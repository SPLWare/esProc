package com.scudata.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BFileReader;
import com.scudata.dm.BFileWriter;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.BFileFetchCursor;
import com.scudata.dm.cursor.BFileSortxCursor;
import com.scudata.dm.cursor.ConjxCursor;
import com.scudata.dm.cursor.ICursor;
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
		
		//复制第一个文件
		FileObject first = fileArray[0];
		if (!first.move(outFile.getFileName(), "c")) {
			return null;
		}
		
		//只有一个文件则返回
		if (len == 1) {
			return Boolean.TRUE;
		}
		
		//从第二个文件开始拼接到输出
		for (int i = 1; i < len; i++) {
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
		
		//调整分块、总记录数信息
		BFileWriter writer = new BFileWriter(outFile, null);
		writer.close();
		
		return Boolean.TRUE; 
	}
}
