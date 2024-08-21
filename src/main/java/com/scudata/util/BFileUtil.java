package com.scudata.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BFileReader;
import com.scudata.dm.BFileWriter;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.RandomObjectWriter;
import com.scudata.dm.RandomOutputStream;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.BFileCursor;
import com.scudata.dm.cursor.BFileFetchCursor;
import com.scudata.dm.cursor.BFileSortxCursor;
import com.scudata.dm.cursor.ConjxCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;

/**
 * 集文件的工具类
 * @author LW
 *
 */
public class BFileUtil {
	/**
	 * 对集文件进行外存排序
	 * @param file 文件
	 * @param outFile 输出文件
	 * @param fields 排序字段数组
	 * @param ctx 计算上下文
	 * @param opt 选项(暂时无用)
	 * @return 排好序的游标
	 */
	public static Object sortx(FileObject file, FileObject outFile, String[] fields, Context ctx, String opt) {
		int fcount = fields.length;
		BFileFetchCursor cursor = new BFileFetchCursor(file, fields);
		DataStruct ds  = cursor.getFileDataStruct();
		
		Expression[] tempExps = new Expression[fcount];
		for (int i = 0; i < fcount; i++) {
			tempExps[i] = new Expression(fields[i]);
		}
		int[] findex = isAllRecordFields(ds, tempExps);

		double backup = EnvUtil.getMaxUsedMemoryPercent();
		EnvUtil.setMaxUsedMemoryPercent(0.2);
		ICursor cs = sortx(cursor, tempExps, ctx, 0, opt, findex);
		EnvUtil.setMaxUsedMemoryPercent(backup);
		
		if (outFile == null) {
			return new BFileSortxCursor(cs, fcount, ds);
		}
		
		BFileWriter writer = new BFileWriter(outFile, null);
		writer.exportBinary(cs, ds, fcount, ctx);
		writer.close();
		
		return Boolean.TRUE;
	}
	
	/**
	 * 检查比较是否都是记录的字段
	 * @return
	 */
	private static int[] isAllRecordFields(DataStruct ds, Expression[] exps) {
		int fcount = exps.length;
		int[] findex = new int[fcount];
		for (int i = 0; i < fcount; i++) {
			findex[i] = ds.getFieldIndex(exps[i].getIdentifierName());
			if (findex[i] == -1) {
				return null;
			}
		}
		return findex;
	}
	
	private static Thread newExportThread(final FileObject fo, final Sequence sequence, 
			final Context ctx, final ArrayList<ICursor> cursorList) {
		return new Thread() {
			public void run() {
				fo.exportSeries(sequence, "b", null);
				BFileCursor bfc = new BFileCursor(fo, null, "x", ctx);
				synchronized(cursorList) {
					cursorList.add(bfc);
				}
			}
		};
	}
	
	/**
	 * 对游标进行外存排序
	 */
	private static ICursor sortx(ICursor cursor, Expression[] exps, Context ctx, int capacity, String opt, int[] findex) {
		int fcount = exps.length;
		ArrayList<ICursor> cursorList = new ArrayList<ICursor>();
		
		Sequence table;
		if (capacity <= 1) {
			// 尽可能的多取数据，这样可以减少临时文件的数量
			// 之后每次取数的数量都用这个数
			table = CursorUtil.tryFetch(cursor);
			if (table != null) {
				capacity = table.length();
			}
		} else {
			table = cursor.fetch(capacity);
		}
		
		MessageManager mm = EngineMessage.get();
		String msg = mm.getMessage("engine.createTmpFile");
		Expression[] tempExps = exps.clone();
		Thread exportThread = null;
		
		while (table != null && table.length() > 0) {
			// 字段表达式做运算时为了性能优化会保留记录的指针
			// 为了在下次取数前能够释放前一次的数据，先复制下表达式，排好序后再释放表达式
			for (int i = 0, len = tempExps.length; i < len; i++) {
				tempExps[i] = exps[i].newExpression(ctx);
			}
			
			Sequence sequence;
			if (findex != null) {
				opt = opt == null ? "o" : opt + "o";
				sequence = table.sort(tempExps, null, opt, findex, ctx);
			} else if (fcount == 1) {
				sequence = table.sort(tempExps[0], null, opt, ctx);
			} else {
				sequence = table.sort(tempExps, null, opt, ctx);
			}

			// 是否源表和表达式
			table = null;
			for (int i = 0, len = tempExps.length; i < len; i++) {
				tempExps[i] = null;
			}
			
			// 创建临时文件
			FileObject fo = FileObject.createTempFileObject();
			Logger.info(msg + fo.getFileName());
			
			// 把排好序的排列写出临时集文件
			if (exportThread != null) {
				try {
					exportThread.join();
				} catch (InterruptedException e) {
					throw new RQException(e);
				}
			}
			exportThread = newExportThread(fo, sequence, ctx, cursorList);
			exportThread.start();
			sequence = null;
			
			// 继续取数据
			table = cursor.fetch(capacity);
		}

		if (exportThread != null) {
			try {
				exportThread.join();
			} catch (InterruptedException e) {
				throw new RQException(e);
			}
		}
		
		int size = cursorList.size();
		if (size == 0) {
			//return null;
			return new MemoryCursor(null);
		} else if (size == 1) {
			return (ICursor)cursorList.get(0);
		} else {
			// 对临时文件做归并
			int bufSize = Env.getMergeFileBufSize(size);
			for (int i = 0; i < size; ++i) {
				BFileCursor bfc = (BFileCursor)cursorList.get(i);
				bfc.setFileBufferSize(bufSize);
			}
			
			ICursor []cursors = new ICursor[size];
			cursorList.toArray(cursors);
			return CursorUtil.merge(cursors, exps, opt, ctx);
		}
	}
	
	/**
	 * 对集文件的序列进行外存排序
	 * @param files 文件序列
	 * @param outFile 输出文件
	 * @param fields 排序字段数组
	 * @param ctx 计算上下文
	 * @param opt 选项(暂时无用)
	 * @return 排好序的游标
	 */
	public static Object sortx(Sequence files, FileObject outFile, String[] fields, Context ctx, String opt) {
		int fcount = fields == null ? 0 : fields.length;
		
		if (fcount == 0) {
			return conj(files, outFile);
		}
		
		int len = files.length();
		BFileFetchCursor[] cursors = new BFileFetchCursor[len];
		for (int i = 1; i <= len; i++) {
			Object obj = files.get(i);
			if (obj instanceof FileObject) {
				FileObject file = (FileObject) obj;
				cursors[i - 1] = new BFileFetchCursor(file, fields);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sortx" + mm.getMessage("function.invalidParam"));
			}
		}
		ConjxCursor cursor = new ConjxCursor(cursors);
		
		DataStruct ds  = cursors[0].getFileDataStruct(); 
		Expression[] tempExps = new Expression[fcount];
		for (int i = 0; i < fcount; i++) {
			tempExps[i] = new Expression(fields[i]);
		}

		double backup = EnvUtil.getMaxUsedMemoryPercent();
		EnvUtil.setMaxUsedMemoryPercent(0.2);
		ICursor cs = CursorUtil.sortx(cursor, tempExps, ctx, 0, opt);
		EnvUtil.setMaxUsedMemoryPercent(backup);
		
		if (outFile == null) {
			return new BFileSortxCursor(cs, fcount, ds);
		}
		
		BFileWriter writer = new BFileWriter(outFile, null);
		writer.exportBinary(cs, ds, fcount, ctx);
		writer.close();
		
		return Boolean.TRUE;
	}
	
	private static Object conj(Sequence files, FileObject outFile) {
		int len = files.length();
		FileObject[] fileArray = new FileObject[len];
		long[][] blocks = new long[len][];
		long[] startPosArray = new long[len];
		long[] lengthArray = new long[len];
		long totalRecordCount = 0;
		DataStruct ds = null;
		
		if (outFile.isExists()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("file.fileAlreadyExist", outFile.getFileName()));
		}
		
		//读取所有文件的块地址、长度、记录总数信息
		for (int i = 1; i <= len; i++) {
			Object obj = files.get(i);
			if (obj instanceof FileObject) {
				FileObject file = (FileObject) obj;
				
				long start;
				long length;
				try {
					BFileReader reader = new BFileReader(file);
					reader.open();
					start = reader.getFirstRecordPos();
					length = file.size() - start + 1;
					blocks[i - 1] = reader.getBlocks();
					totalRecordCount += reader.getTotalRecordCount();
					ds = reader.getDataStruct();
					reader.close();
				} catch (IOException e) {
					return null;
				}
				
				fileArray[ i - 1] = file;
				startPosArray[i - 1] = start;
				lengthArray[i - 1] = length;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sortx" + mm.getMessage("function.invalidParam"));
			}
		}
		
		//调整分块、总记录数信息
		RandomOutputStream ros = outFile.getRandomOutputStream(true);
		RandomObjectWriter writer = new RandomObjectWriter(ros);
		try {
			writer.position(0);
			writer.write('r');
			writer.write('q');
			writer.write('t');
			writer.write('b');
			writer.write('x');
			writer.write(BFileWriter.TYPE_NORMAL);
			writer.writeInt32(0); // 保留
			writer.writeLong64(totalRecordCount);
			writer.writeStrings(ds.getFieldNames());
			writer.close();
			ros.close();
		} catch (IOException e) {
			return null;
		} finally {
			try {
				writer.close();
				ros.close();
			} catch (IOException e) {
			}
		}

		
		//拼接输出
		for (int i = 0; i < len; i++) {
			FileObject file = fileArray[i];
			FileInputStream fis = null;
			FileOutputStream fos = null;
			long start = startPosArray[i];
			long length = lengthArray[i];
			
			try {
				fis = new FileInputStream(file.getLocalFile().getFile());
				fos = new FileOutputStream(outFile.getLocalFile().getFile(), true);
				FileChannel in = fis.getChannel();
				FileChannel out = fos.getChannel();
				in.transferTo(start, length, out); // 连接两个通道，并且从in通道读取，然后写入out通道
			} catch (IOException e) {
				try {
					fos.close();
				} catch (IOException e1) {
				}
				outFile.delete();
				return null;
			} finally {
				IOException ie = null;
				try {
					fis.close();
				} catch (IOException e) {
					ie = e;
				}
				try {
					fos.close();
				} catch (IOException e) {
					ie = e;
				}
				
				if (ie != null) {
					throw new RQException(ie);
				}
			}
		}
		
		return Boolean.TRUE; 
	}
}
