package com.raqsoft.dm;

import java.io.IOException;
import java.io.InputStream;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.expression.Expression;
import com.raqsoft.resources.EngineMessage;

/**
 * 集文件写对象
 * @author WangXiaoJun
 *
 */
public class BFileWriter {
	public static int TYPE_BLOCK = 0x10; // 可分段读取的集文件
	public static int TYPE_NORMAL = 0x50; // 普通集文件没有分段信息
	public static int TYPE_GROUP = 0x70; // 按某个字段的值分段的集文件，同值的不会被拆到两段
	
	public static final int BLOCKCOUNT = 1024; // 最大块数
	public static final int MINBLOCKRECORDCOUNT = 1024; // 每块最小记录数
	public static final String S_FIELDNAME = "_1"; // 导出序列时默认的字段名
	
	private FileObject file; // 文件对象
	private boolean isAppend; // 是否追加写，如果false则会覆盖已有的文件
	private boolean isBlock; // 是否生成有分段信息的集文件
	private RandomOutputStream ros;
	private RandomObjectWriter writer; // 用于写出对象
	private DataStruct ds; // 文件的数据结构
	
	private long []blocks; // 每一块的结束位置
	private int lastBlock; // 最后一块的索引
	private long totalRecordCount; // 总记录数
	private long blockRecordCount; // 每块的记录数，按组导出时则是每块的组数
	private long lastRecordCount; // 最后一块的记录数
	
	private long oldFileSize; // 追加写时源文件大小，如果出错时的文件恢复
	
	/**
	 * 构造集文件写对象
	 * @param file 文件对象
	 * @param opt 选项，a：追加写，z：生成有分段信息的集文件
	 */
	public BFileWriter(FileObject file, String opt) {
		this.file = file;
		if (opt != null) {
			if (opt.indexOf('a') != -1) isAppend = true;
			if (opt.indexOf('z') != -1) isBlock = true;
		}
	}
	
	/**
	 * 导出时是否是生成集文件的选项
	 * @param opt 选项里有b或者z时生成集文件
	 * @return
	 */
	public static boolean isBtxOption(String opt) {
		return opt != null && (opt.indexOf('b') != -1 || opt.indexOf('z') != -1);
	}
	
	/**
	 * 返回文件对象
	 * @return FileObject
	 */
	public FileObject getFile() {
		return file;
	}
	
	// 写文件头
	private void writeHeader(boolean isGroup) throws IOException {
		RandomObjectWriter writer = this.writer;
		writer.position(0);
		writer.write('r');
		writer.write('q');
		writer.write('t');
		writer.write('b');
		writer.write('x');
		
		if (isGroup) {
			writer.write(TYPE_GROUP);
			writer.writeInt32(1); // 保留
			writer.writeLong64(totalRecordCount);
			writer.writeLong64(blockRecordCount);
			writer.writeLong64(lastRecordCount);
			writer.writeInt32(lastBlock);
			
			long []blocks = this.blocks;
			writer.writeInt32(blocks.length);
			for (long b : blocks) {
				writer.writeLong64(b);
			}
		} else if (isBlock) {
			writer.write(TYPE_BLOCK);
			writer.writeInt32(0); // 保留
			writer.writeLong64(totalRecordCount);
			writer.writeLong64(blockRecordCount);
			writer.writeLong64(lastRecordCount);
			writer.writeInt32(lastBlock);
			
			long []blocks = this.blocks;
			writer.writeInt32(blocks.length);
			for (long b : blocks) {
				writer.writeLong64(b);
			}
		} else {
			writer.write(TYPE_NORMAL);
			writer.writeInt32(0); // 保留
			writer.writeLong64(totalRecordCount);
		}
		
		writer.writeStrings(ds.getFieldNames());
	}
	
	// 读文件头
	private void readHeader(boolean isGroup) throws IOException {
		InputStream is = ros.getInputStream(0);
		if (is == null) {
			is = file.getInputStream();
		} 
		
		ObjectReader in = new ObjectReader(is);
		
		try {
			if (in.read() != 'r' || in.read() != 'q' || in.read() != 't' || 
					in.read() != 'b' || in.read() != 'x') {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
			
			int type = in.read();
			int ver = in.readInt32();
			if (type == TYPE_NORMAL) {
				if (isGroup || isBlock) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("license.fileFormatError"));
				}
				
				totalRecordCount = in.readLong64();
			} else if (type == TYPE_BLOCK) {
				if (isGroup) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("license.fileFormatError"));
				}

				isBlock = true;
				totalRecordCount = in.readLong64();
				blockRecordCount = in.readLong64();
				lastRecordCount = in.readLong64();
				lastBlock = in.readInt32();
				
				int count = in.readInt32();
				long []blocks = new long[count];
				this.blocks = blocks;
				for (int i = 0; i < count; ++i) {
					blocks[i] = in.readLong64();
				}
			} else if (type == TYPE_GROUP) {
				if (!isGroup || ver == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("license.fileFormatError"));
				}

				totalRecordCount = in.readLong64();
				blockRecordCount = in.readLong64();
				lastRecordCount = in.readLong64();
				lastBlock = in.readInt32();
				
				int count = in.readInt32();
				long []blocks = new long[count];
				this.blocks = blocks;
				for (int i = 0; i < count; ++i) {
					blocks[i] = in.readLong64();
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
			
			ds = new DataStruct(in.readStrings());
		} finally {
			in.close();
		}
	}
	
	/**
	 * 打开文件准备写
	 * @param ds 要写入的数据的数据结构
	 * @param isGroup 是否有分组字段
	 * @throws IOException
	 */
	public void prepareWrite(DataStruct ds, boolean isGroup) throws IOException {
		if (isAppend ) {
			// 先锁定再判断大小
			ros = file.getRandomOutputStream(true);
			writer = new RandomObjectWriter(ros);
			oldFileSize = file.size();
			
			if (oldFileSize > 0) {
				readHeader(isGroup);
				writer.position(oldFileSize);
			} else {
				if (isBlock) {
					blocks = new long[BLOCKCOUNT];
					if (isGroup) {
						blockRecordCount = 1;
					} else {
						blockRecordCount = MINBLOCKRECORDCOUNT;
					}
				}
				
				this.ds = ds; 
				writeHeader(isGroup);
			}
		} else {
			ros = file.getRandomOutputStream(false);
			writer = new RandomObjectWriter(ros);
			oldFileSize = 0;
			if (isBlock) {
				blocks = new long[BLOCKCOUNT];
				if (isGroup) {
					blockRecordCount = 1;
				} else {
					blockRecordCount = MINBLOCKRECORDCOUNT;
				}
			}
			
			this.ds = ds; 
			writeHeader(isGroup);
		}
	}
	
	/**
	 * 写结束，关闭文件
	 */
	public void close() {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				throw new RQException(e);
			}
		}
	}
	
	// 取数据的数据结构
	private DataStruct getDataStruct(Sequence seq, Expression []exps, String []names) {
		int fcount = exps.length;
		String []tmps = new String[fcount];
		if (names != null) {
			System.arraycopy(names, 0, tmps, 0, fcount);
		}
		
		seq.getNewFieldNames(exps, tmps, "export");
		return new DataStruct(tmps);
	}
	
	// 调整字段的顺序，跟之前的结构对齐
	private Expression[] adjustDataStruct(DataStruct ds, Expression []exps, String []names) {
		DataStruct srcDs = this.ds;
		if (srcDs.isCompatible(ds)) {
			return exps;
		}
		
		int fcount = srcDs.getFieldCount();
		if (fcount != ds.getFieldCount()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}
		
		if (exps == null) {
			exps = new Expression[fcount];
			String []fields = srcDs.getFieldNames();
			for (int i = 0; i < fcount; ++i) {
				int index = ds.getFieldIndex(fields[i]);
				if (index < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.dsNotMatch"));
				}
				
				exps[i] = new Expression("#" + (index + 1));
			}
			
			return exps;
		} else if (names != null) {
			Expression []tmp = new Expression[fcount];
			for (int i = 0; i < fcount; ++i) {
				if (names[i] != null) {
					int index = ds.getFieldIndex(names[i]);
					if (index < 0 || tmp[index] != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.dsNotMatch"));
					}
					
					tmp[index] = exps[i];
				} else {
					if (tmp[i] != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.dsNotMatch"));
					}
					
					tmp[i] = exps[i];
				}
			}
			
			return tmp;
		} else {
			return exps;
		}
	}

	// 改变文件存储格式为可分段文件
	private void changeToSegmentFile(Context ctx) throws IOException {
		Sequence seq = null;
		BFileReader reader = new BFileReader(file);
		try {
			reader.open();
			seq = reader.readAll();
		} finally {
			reader.close();
		}
		
		BFileWriter writer = new BFileWriter(file, "z");
		writer.export(seq, null, null, ctx);
	}
	
	/**
	 * 导出数据
	 * @param data 排列
	 * @param exps 导出的字段表达式数组，省略则导出所有字段
	 * @param names 字段名数组
	 * @param ctx 计算上下文
	 */
	public void export(Sequence data, Expression []exps, String []names, Context ctx) {
		if (data == null || data.length() == 0) {
			if (!isAppend) file.delete();
			return;
		}
		
		if (!isAppend && data.length() > MINBLOCKRECORDCOUNT) {
			isBlock = true;
		}
		
		DataStruct ds;
		if (exps != null) {
			ds = getDataStruct(data, exps, names);
		} else {
			ds = data.dataStruct();
			if (ds == null) {
				ds = new DataStruct(new String[]{S_FIELDNAME});
			}
		}
		
		try {
			prepareWrite(ds, false);
			if (isAppend && !isBlock) {
				if (data.length() + totalRecordCount > MINBLOCKRECORDCOUNT) {
					close();
					changeToSegmentFile(ctx);
					isBlock = true;
					prepareWrite(ds, false);
				}
			}
			
			adjustDataStruct(ds, exps, names);
			if (isBlock) {
				exportBlock(data, exps, ctx);
			} else {
				exportNormal(data, exps, ctx);
			}
			
			writer.flush();
			writeHeader(false);
		} catch (Exception e) {
			file.setFileSize(oldFileSize);
			if (e instanceof RQException) {
				throw (RQException)e;
			} else {
				throw new RQException(e);
			}
		} finally {
			close();
		}
	}
	
	/**
	 * 导出数据
	 * @param cursor 游标
	 * @param exps 导出的字段表达式数组，省略则导出所有字段
	 * @param names 字段名数组
	 * @param ctx 计算上下文
	 */
	public void export(ICursor cursor, Expression []exps, String []names, Context ctx) {
		Sequence data = cursor.fetch(ICursor.FETCHCOUNT);
		if (data == null || data.length() == 0) {
			if (!isAppend) file.delete();
			return;
		}
		
		if (!isAppend) {
			isBlock = true;
		}
		
		DataStruct ds;
		if (exps != null) {
			ds = getDataStruct(data, exps, names);
		} else {
			ds = data.dataStruct();
			if (ds == null) {
				ds = new DataStruct(new String[]{S_FIELDNAME});
			}
		}
		
		try {
			prepareWrite(ds, false);
			if (isAppend && !isBlock) {
				if (data.length() + totalRecordCount > MINBLOCKRECORDCOUNT) {
					close();
					changeToSegmentFile(ctx);
					isBlock = true;
					prepareWrite(ds, false);
				}
			}
			
			adjustDataStruct(ds, exps, names);
			if (isBlock) {
				while (data != null && data.length() > 0) {
					exportBlock(data, exps, ctx);
					data = cursor.fetch(ICursor.FETCHCOUNT);
				}
			} else {
				while (data != null && data.length() > 0) {
					exportNormal(data, exps, ctx);
					data = cursor.fetch(ICursor.FETCHCOUNT);
				}
			}
			
			writer.flush();
			writeHeader(false);
		} catch (Exception e) {
			file.setFileSize(oldFileSize);
			if (e instanceof RQException) {
				throw (RQException)e;
			} else {
				throw new RQException(e);
			}
		} finally {
			close();
		}
	}
	
	// 有分段信息的导出
	private void exportBlock(Sequence data, Expression []exps, Context ctx) throws IOException {
		RandomObjectWriter writer = this.writer;
		long []blocks = this.blocks;
		int blockCount = blocks.length;
		int lastBlock = this.lastBlock;
		long blockRecordCount = this.blockRecordCount;
		long lastRecordCount = this.lastRecordCount;
		int fcount = ds.getFieldCount();
		int len = data.length();
		
		if (exps == null) {
			boolean isTable = data.getMem(1) instanceof Record;
			for (int i = 1; i <= len; ++i) {
				if (lastRecordCount == blockRecordCount) {
					blocks[lastBlock++] = writer.position();
					lastRecordCount = 0;
					if (lastBlock == blockCount) {
						blockRecordCount += blockRecordCount;
						lastBlock = blockCount / 2;
						for (int b = 0, j = 1; b < lastBlock; ++b, j += 2) {
							blocks[b] = blocks[j];
						}
					}
				}
				
				lastRecordCount++;
				if (isTable) {
					Record r = (Record)data.getMem(i);
					Object []vals = r.getFieldValues();
					for (int f = 0; f < fcount; ++f) {
						writer.writeObject(vals[f]);
					}
				} else {
					writer.writeObject(data.getMem(i));
				}
			}
		} else {
			ComputeStack stack = ctx.getComputeStack();
			Sequence.Current current = data.new Current();
			stack.push(current);
			
			try {
				for (int i = 1; i <= len; ++i) {
					if (lastRecordCount == blockRecordCount) {
						blocks[lastBlock++] = writer.position();
						lastRecordCount = 0;
						if (lastBlock == blockCount) {
							blockRecordCount += blockRecordCount;
							lastBlock = blockCount / 2;
							for (int b = 0, j = 1; b < lastBlock; ++b, j += 2) {
								blocks[b] = blocks[j];
							}
						}
					}
					
					lastRecordCount++;
					current.setCurrent(i);
					for (int f = 0; f < fcount; ++f) {
						writer.writeObject(exps[f].calculate(ctx));
					}
				}
			} finally {
				stack.pop();
			}
		}
		
		blocks[lastBlock] = writer.position();
		this.totalRecordCount += len;
		this.lastBlock = lastBlock;
		this.blockRecordCount = blockRecordCount;
		this.lastRecordCount = lastRecordCount;
	}
	
	// 有分组信息的导出
	private void exportGroup(Sequence data, Expression []exps, Context ctx) throws IOException {
		RandomObjectWriter writer = this.writer;
		int fcount = ds.getFieldCount();
		int len = data.length();
		
		if (exps == null) {
			boolean isTable = data.getMem(1) instanceof Record;
			for (int i = 1; i <= len; ++i) {
				if (isTable) {
					Record r = (Record)data.getMem(i);
					Object []vals = r.getFieldValues();
					for (int f = 0; f < fcount; ++f) {
						writer.writeObject(vals[f]);
					}
				} else {
					writer.writeObject(data.getMem(i));
				}
			}
		} else {
			ComputeStack stack = ctx.getComputeStack();
			Sequence.Current current = data.new Current();
			stack.push(current);
			
			try {
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					for (int f = 0; f < fcount; ++f) {
						writer.writeObject(exps[f].calculate(ctx));
					}
				}
			} finally {
				stack.pop();
			}
		}
		
		this.totalRecordCount += len;
		long []blocks = this.blocks;
		
		if (lastRecordCount == blockRecordCount) {
			lastBlock++;
			lastRecordCount = 0;
			if (lastBlock == blocks.length) {
				blockRecordCount += blockRecordCount;
				lastBlock = blocks.length / 2;
				for (int b = 0, j = 1; b < lastBlock; ++b, j += 2) {
					blocks[b] = blocks[j];
				}
			}
		}
		
		lastRecordCount++;
		blocks[lastBlock] = writer.position();
	}
	
	// 没有分段信息的导出
	private void exportNormal(Sequence data, Expression []exps, Context ctx) throws IOException {
		RandomObjectWriter writer = this.writer;
		int fcount = ds.getFieldCount();
		int len = data.length();
		
		if (exps == null) {
			boolean isTable = data.getMem(1) instanceof Record;
			if (isTable) {
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)data.getMem(i);
					Object []vals = r.getFieldValues();
					for (int f = 0; f < fcount; ++f) {
						writer.writeObject(vals[f]);
					}
				}
			} else {
				for (int i = 1; i <= len; ++i) {
					writer.writeObject(data.getMem(i));
				}
			}
		} else {
			ComputeStack stack = ctx.getComputeStack();
			Sequence.Current current = data.new Current();
			stack.push(current);
			
			try {
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					for (int f = 0; f < fcount; ++f) {
						writer.writeObject(exps[f].calculate(ctx));
					}
				}
			} finally {
				stack.pop();
			}
		}

		this.totalRecordCount += len;		
	}

	/**
	 * 导出数据
	 * @param cursor 游标
	 * @param exps 导出的字段表达式数组，省略则导出所有字段
	 * @param names 字段名数组
	 * @param gexp 分组表达式，使该表达式返回结果相同的记录不会被拆成两段
	 * @param ctx 计算上下文
	 */
	public void export(ICursor cursor, Expression []exps, String []names, Expression gexp, Context ctx) {
		Sequence data = cursor.fetchGroup(gexp, ctx);
		if (data == null || data.length() == 0) {
			if (!isAppend) file.delete();
			return;
		}
		
		if (!isAppend) {
			isBlock = true;
		}
		
		DataStruct ds;
		if (exps != null) {
			ds = getDataStruct(data, exps, names);
		} else {
			ds = data.dataStruct();
			if (ds == null) {
				ds = new DataStruct(new String[]{S_FIELDNAME});
			}
		}
		
		try {
			prepareWrite(ds, true);
			adjustDataStruct(ds, exps, names);
			
			while (data != null && data.length() > 0) {
				exportGroup(data, exps, ctx);
				data = cursor.fetchGroup(gexp, ctx);
			}
			
			writer.flush();
			writeHeader(true);
		} catch (Exception e) {
			file.setFileSize(oldFileSize);
			if (e instanceof RQException) {
				throw (RQException)e;
			} else {
				throw new RQException(e);
			}
		} finally {
			close();
		}
	}

	// 写不分块文件头
	private static void writeHeader(ObjectWriter writer, DataStruct ds) throws IOException {
		writer.write('r');
		writer.write('q');
		writer.write('t');
		writer.write('b');
		writer.write('x');
		
		writer.write(TYPE_NORMAL);
		writer.writeInt32(0); // 保留
		writer.writeLong64(0);
		
		if (ds != null) {
			writer.writeStrings(ds.getFieldNames());
		} else {
			writer.writeStrings(new String[] {S_FIELDNAME});
		}
	}

	/**
	 * 导出不分块序列
	 * @param writer 写对象
	 * @param data 数据
	 * @param ds 数据的数据结构，空则当序列写出
	 * @param writeHeader 是否写文件头
	 * @throws IOException
	 */
	public static void export(ObjectWriter writer, Sequence data, 
			DataStruct ds, boolean writeHeader) throws IOException {
		if (writeHeader) {
			writeHeader(writer, ds);
		}
		
		int len = data.length();
		if (ds != null) {
			int fcount = ds.getFieldCount();
			for (int i = 1; i <= len; ++i) {
				Record r = (Record)data.getMem(i);
				Object []vals = r.getFieldValues();
				for (int f = 0; f < fcount; ++f) {
					writer.writeObject(vals[f]);
				}
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				writer.writeObject(data.getMem(i));
			}
		}
	}

	/**
	 * 导出不分块游标
	 * @param writer 写对象
	 * @param cursor 游标
	 * @param ds 数据的数据结构，空则当序列写出
	 * @param writeHeader 是否写文件头
	 * @throws IOException
	 */
	public static void export(ObjectWriter writer, ICursor cursor, boolean writeHeader) throws IOException {
		Sequence data = cursor.fetch(ICursor.FETCHCOUNT);
		if (data == null || data.length() == 0) {
			return;
		}
		
		DataStruct ds = data.dataStruct();
		if (writeHeader) {
			writeHeader(writer, ds);
		}

		while (data != null && data.length() > 0) {
			export(writer, data, ds, false);
			data = cursor.fetch(ICursor.FETCHCOUNT);
		}
	}
	
	/**
	 * 不分段导出序表，由外部控制打开关闭文件
	 * @param data
	 * @throws IOException
	 */
	public void write(Sequence data) throws IOException {
		RandomObjectWriter writer = this.writer;
		int fcount = ds.getFieldCount();
		int len = data.length();
		
		for (int i = 1; i <= len; ++i) {
			Record r = (Record)data.getMem(i);
			Object []vals = r.getFieldValues();
			for (int f = 0; f < fcount; ++f) {
				writer.writeObject(vals[f]);
			}
		}

		this.totalRecordCount += len;		
	}
}