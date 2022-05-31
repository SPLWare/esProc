package com.scudata.dm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.cursor.BFileCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.PFileCursor;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 集文件读取对象
 * @author WangXiaoJun
 *
 */
public class BFileReader {
	private FileObject file; // 集文件对应的文件对象
	private int type; // 集文件类型
	private long []blocks; // 每一块的结束位置
	private int lastBlock; // 最后一块的索引
	private long totalRecordCount;	// 记录总数
	private long blockRecordCount;	// 区块总数
	private long firstRecordPos; // 第一条记录的位置
	
	private DataStruct ds; // 集文件数据结构
	private DataStruct readDs; // 选出字段组成的数据结构
	private String []readFields; // 选出字段
	private int []readIndex; // 选出字段对应的序号
	private boolean isSingleField; // 是否返回单列组成的序列
	private boolean isSequenceMember; // 是否返回序列组成的序列
	private boolean isExist = true; // 字段是否都在文件中

	private int segSeq; // 分段序号，从1开始计数
	private int segCount; // 分段数
	private long endPos = -1; // 读取的结束位置，用于多线程分段读取
	
	private ObjectReader importer; // 对象读取类
	
	/**
	 * 由文件对象创建集文件读取类
	 * 
	 * @param file	文件对象
	 */
	public BFileReader(FileObject file) {
		this(file, null, null);
	}
	
	/**
	 * 用文件对象、列名和读写字符串创建二进制文件
	 * @param file 文件对象
	 * @param fields 选出字段
	 * @param opt 选项，i：结果集只有1列时返回成序列，e：在文件中不存在时将生成null，缺省将报错，w：把每行读成序列
	 */
	public BFileReader(FileObject file, String []fields, String opt) {
		this(file, fields, 1, 1, opt);
	}

	/**
	 * 用文件对象、列名和读写字符串创建二进制文件
	 * @param file 文件对象
	 * @param fields 选出字段
	 * @param segSeq 要读取的段号，从1开始计数
	 * @param segCount 分段数
	 * @param opt 选项，i：结果集只有1列时返回成序列，e：在文件中不存在时将生成null，缺省将报错，w：把每行读成序列
	 */
	public BFileReader(FileObject file, String []fields, int segSeq, int segCount, String opt) {
		this.file = file;
		this.segSeq = segSeq;
		this.segCount = segCount;
		
		if (fields != null) {
			readFields = new String[fields.length];
			System.arraycopy(fields, 0, readFields, 0, fields.length);
		}
		
		if (opt != null) {
			if (opt.indexOf('i') != -1) isSingleField = true;
			if (opt.indexOf('e') != -1) isExist = false;
			if (opt.indexOf('w') != -1) isSequenceMember = true;
		}
		
		if (segCount > 1) {
			if (segSeq < 0 || segSeq > segCount) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(segSeq + mm.getMessage("function.invalidParam"));
			}
		}
	}
	
	/**
	 * 返回列结构
	 * @return DataStruct
	 */
	public DataStruct getFileDataStruct() {
		return ds;
	}
	
	/**
	 * 对文件进行分段
	 * 
	 * @throws IOException
	 */
	private void doSegment() throws IOException {
		int segSeq = this.segSeq - 1;
		int avg = (lastBlock + 1) / segCount;
		
		if (avg < 1) {
			// 每段不足一块
			if (segSeq > lastBlock) {
				endPos = 0;
			} else {
				endPos = blocks[segSeq];
				if (segSeq > 0) {
					importer.seek(blocks[segSeq - 1]);
				}
			}
		} else {
			if (segSeq > 0) {
				int s = segSeq * avg - 1;
				int e = s + avg;
				
				// 剩余的块后面的每段多一块
				int mod = (lastBlock + 1) % segCount;
				int n = mod - (segCount - segSeq - 1);
				if (n > 0) {
					e += n;
					s += n - 1;
				}
				
				endPos = blocks[e];
				importer.seek(blocks[s]);
			} else {
				endPos = blocks[avg - 1];
			}
		}
	}
	
	/**
	 * 当前读到的流的位置
	 * @return	返回当前读到的位置
	 */
	public long position() {
		return importer.position();
	}
	
	/**
	 * 定位、到给定位置。
	 * 支持向后定位。向前定位的话，若超出缓冲区范围，就会抛异常。
	 * @param pos	要定位的位置
	 * @throws IOException
	 */
	public void seek(long pos) throws IOException {
		importer.seek(pos);
	}
	
	/**
	 * 当前读取类是否打开
	 * 
	 * @return
	 */
	public boolean isOpen() {
		return importer != null;
	}
	
	/**
	 * 
	 * @throws IOException
	 */
	public void open() throws IOException {
		open(Env.FILE_BUFSIZE);
	}
	
	/**
	 * 重新打开
	 * 
	 * @param bufSize	缓冲区的大小
	 */
	private void reopen(int bufSize) {
		InputStream in = file.getBlockInputStream(bufSize);
		ObjectReader importer = new ObjectReader(in, bufSize);
		this.importer = importer;
	}
	
	/**
	 * 打开一个二进制文件，并用bufSize初始化文件读取类的缓冲区大小。
	 * 
	 * @param bufSize	缓冲区的大小
	 * @throws IOException
	 */
	public void open(int bufSize) throws IOException {
		InputStream in = file.getBlockInputStream(bufSize);
		ObjectReader importer = new ObjectReader(in, bufSize);
		this.importer = importer;
		
		if (importer.read() != 'r' || importer.read() != 'q' || importer.read() != 't' || 
				importer.read() != 'b' || importer.read() != 'x') {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("license.fileFormatError"));
		}
		
		type = importer.read();
		int ver = importer.readInt32();
		
		if (type == BFileWriter.TYPE_NORMAL) {
			if (segCount > 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needZFile"));
			}
			
			totalRecordCount = importer.readLong64();
			ds = new DataStruct(importer.readStrings());
			firstRecordPos = position();
		} else if (type == BFileWriter.TYPE_BLOCK) {
			totalRecordCount = importer.readLong64();
			blockRecordCount = importer.readLong64();
			importer.readLong64(); // lastRecordCount
			lastBlock = importer.readInt32();
			
			int count = importer.readInt32();
			long []blocks = new long[count];
			this.blocks = blocks;
			for (int i = 0; i < count; ++i) {
				blocks[i] = importer.readLong64();
			}
			
			ds = new DataStruct(importer.readStrings());
			firstRecordPos = position();
			
			if (segCount > 1) {
				doSegment();
			}
		} else if (type == BFileWriter.TYPE_GROUP) {
			totalRecordCount = importer.readLong64();
			if (ver > 0) {
				blockRecordCount = importer.readLong64();
				importer.readLong64(); // lastRecordCount
			}
			
			lastBlock = importer.readInt32();
			int count = importer.readInt32();
			long []blocks = new long[count];
			this.blocks = blocks;
			for (int i = 0; i < count; ++i) {
				blocks[i] = importer.readLong64();
			}
			
			ds = new DataStruct(importer.readStrings());
			firstRecordPos = position();
			
			if (segCount > 1) {
				doSegment();
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("license.fileFormatError"));
		}
		
		String []fields = ds.getFieldNames();
		int fcount = fields.length;
		
		if (readFields != null) {
			if (isSingleField) {
				isSingleField = readFields.length == 1;
			}
			
			readIndex = new int[fcount];
			for (int i = 0; i < fcount; ++i) {
				readIndex[i] = -1;
			}

			for (int i = 0, count = readFields.length; i < count; ++i) {
				int q = ds.getFieldIndex(readFields[i]);
				if (q >= 0) {
					if (readIndex[q] != -1) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(readFields[i] + mm.getMessage("ds.colNameRepeat"));
					}

					readIndex[q] = i;
					readFields[i] = fields[q];
				} else if (isExist) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(readFields[i] + mm.getMessage("ds.fieldNotExist"));
				}
			}

			readDs = new DataStruct(readFields);
		} else {
			if (isSingleField) isSingleField = fcount == 1;
		}
	}
	
	/**
	 * 关闭文件，关闭流
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (importer != null) {
			importer.close();
			importer = null;
		}
	}
	
	/**
	 * 读取文件中所有的记录，并组成序列返回。
	 * 
	 * @return
	 * @throws IOException
	 */
	public Sequence readAll() throws IOException {
		return read(ICursor.MAXSIZE);
	}
	
	/**
	 * 读取指定条数的记录
	 * @param n	要读取的记录数
	 * @return	用序列保存的记录集
	 * @throws IOException
	 */
	public Sequence read(int n) throws IOException {
		long endPos = this.endPos;
		ObjectReader importer = this.importer;
		if (n < 1 || (endPos != -1 && importer.position() >= endPos)) {
			return null;
		}

		int fcount = ds.getFieldCount();
		int initSize;
		if (n <= ICursor.FETCHCOUNT) {
			initSize = n;
		} else if (n >= totalRecordCount && totalRecordCount > 0) {
			// 分段二进制记了记录总数;
			initSize = (int)totalRecordCount;
		} else if (n < ICursor.MAXSIZE) {
			initSize = n;
		} else {
			initSize = ICursor.INITSIZE;
		}
		
		if (isSingleField) {
			Sequence seq = new Sequence(initSize);
			if (readFields == null) {
				for (int i = 0; i < n; ++i) {
					if (importer.hasNext() && (endPos == -1 || importer.position() < endPos)) {
						seq.add(importer.readObject());
					} else {
						break;
					}
				}
			} else {
				int []readIndex = this.readIndex;
				for (int i = 0; i < n; ++i) {
					if (importer.hasNext() && (endPos == -1 || importer.position() < endPos)) {
						for (int f = 0; f < fcount; ++f) {
							if (readIndex[f] != -1) {
								seq.add(importer.readObject());
							} else {
								importer.skipObject();
							}
						}
					} else {
						break;
					}
				}
			}

			if (seq.length() != 0) {
				return seq;
			} else {
				return null;
			}
		} else if (isSequenceMember) {
			Sequence seq = new Sequence(initSize);
			if (readFields == null) {
				Sequence tmp = new Sequence(ds.getFieldNames());
				seq.add(tmp);
				Object []values = new Object[fcount];
				for (int i = 0; i < n; ++i) {
					if (importer.hasNext() && (endPos == -1 || importer.position() < endPos)) {
						for (int f = 0; f < fcount; ++f) {
							values[f] = importer.readObject();
						}
						
						seq.add(new Sequence(values));
					} else {
						break;
					}
				}
			} else {
				Sequence tmp = new Sequence(readFields);
				seq.add(tmp);
				Object []values = new Object[readFields.length];
				int []readIndex = this.readIndex;
				for (int i = 0; i < n; ++i) {
					if (importer.hasNext() && (endPos == -1 || importer.position() < endPos)) {
						for (int f = 0; f < fcount; ++f) {
							if (readIndex[f] != -1) {
								values[readIndex[f]] = importer.readObject();
							} else {
								importer.skipObject();
							}
						}
						
						seq.add(new Sequence(values));
					} else {
						break;
					}
				}
			}

			if (seq.length() != 0) {
				//seq.trimToSize();
				return seq;
			} else {
				return null;
			}
		} else {
			Table table;
			if (readFields == null) {
				table = new Table(ds, initSize);
				for (int i = 0; i < n; ++i) {
					if (importer.hasNext() && (endPos == -1 || importer.position() < endPos)) {
						Record cur = table.newLast();
						for (int f = 0; f < fcount; ++f) {
							cur.setNormalFieldValue(f, importer.readObject());
						}
					} else {
						break;
					}
				}
			} else {
				int []readIndex = this.readIndex;
				table = new Table(readDs, initSize);

				for (int i = 0; i < n; ++i) {
					if (importer.hasNext() && (endPos == -1 || importer.position() < endPos)) {
						Record cur = table.newLast();
						for (int f = 0; f < fcount; ++f) {
							if (readIndex[f] != -1) {
								cur.setNormalFieldValue(readIndex[f], importer.readObject());
							} else {
								importer.skipObject();
							}
						}
					} else {
						break;
					}
				}
			}

			if (table.length() != 0) {
				//table.trimToSize();
				return table;
			} else {
				return null;
			}
		}
	}
	
	/**
	 * 跳过，跳过指定的记录数
	 * 
	 * @param n	要跳过的记录数
	 * @return	返回实际的跳过的记录数
	 * @throws IOException
	 */
	public long skip(long n) throws IOException {
		if (totalRecordCount > 0 && segCount <= 1 && firstRecordPos == position()) {
			if (totalRecordCount <= n) {
				seek(file.size());
				return totalRecordCount;
			}

			if (type == BFileWriter.TYPE_BLOCK && blockRecordCount < n) {
				int i = (int)(n / blockRecordCount);
				seek(blocks[i - 1]);
				skip(n - blockRecordCount * i);
				return n;
			}
		}
		
		ObjectReader importer = this.importer;
		if (n < 1 || (endPos != -1 && importer.position() >= endPos)) {
			return 0;
		}

		int fcount = ds.getFieldCount();
		for (long i = 0; i < n; ++i) {
			if (importer.hasNext() && (endPos == -1 || importer.position() < endPos)) {
				for (int f = 0; f < fcount; ++f) {
					importer.skipObject();
				}
			} else {
				return i;
			}
		}

		return n;
	}
	
	/**
	 * 取当前记录的指定字段
	 * 
	 * @param fields	字段标记，值为-1的不取数。
	 * @param values	结果数组
	 * @return		true	取数成功
	 * 				false	取数失败
	 * @throws IOException
	 */
	public boolean readRecord(int []fields, Object []values) throws IOException {
		ObjectReader importer = this.importer;
		if (importer.hasNext()) {
			for (int f = 0, fcount = fields.length; f < fcount; ++f) {
				if (fields[f] != -1) {
					values[fields[f]] = importer.readObject();
				} else {
					importer.skipObject();
				}
			}
			
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 读取一个一条记录的某个字段。
	 * @param field	指定的字段索引。
	 * @return	返回值读取的结果
	 * @throws IOException
	 */
	private Object readRecordField(int field) throws IOException {
		ObjectReader importer = this.importer;
		for (int f = 0; f < field; ++f) {
			importer.skipObject();
		}
		
		return importer.readObject();
	}
	
	// 取当前记录的字段
	/**
	 * 读取一条记录
	 * 
	 * @param values	保存记录的对象数组
	 * @return			true	取数成功
	 * 					false	取数失败
	 * @throws IOException
	 */
	public boolean readRecord(Object []values) throws IOException {
		ObjectReader importer = this.importer;
		if (importer.hasNext()) {
			for (int f = 0, fcount = values.length; f < fcount; ++f) {
				values[f] = importer.readObject();
			}
			
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 跳过记录，如果到文件尾则返回false
	 * @return
	 * @throws IOException
	 */
	public boolean skipRecord() throws IOException {
		ObjectReader importer = this.importer;
		if (importer.hasNext()) {
			for (int f = 0, fcount = ds.getFieldCount(); f < fcount; ++f) {
				importer.skipObject();
			}
			
			return true;
		} else {
			return false;
		}
	}
			
	/**
	 * 从对x有序的文件f读出x在序列A中的记录构成游标
	 * 从当前数据集中，选出key值在values中的记录，选取fields字段值构成新表。并返回新表的游标。
	 * @param key		已经排好序的字段的字段名。
	 * @param values	参考值，由key字段与这些值做对比
	 * @param fields	字段名列表，最终得结果表，由这些字段组成
	 * @param ctx		上下文变量
	 * @return			返回筛选出的数据集的游标
	 */
	public ICursor iselect(String key, Sequence values, String []fields, Context ctx) {
		int count = values.length();
		if (count == 0) {
			//return null; 改成返回空游标，这样cs.groups@t会返回空序表
			return new MemoryCursor(null);
		}
		
		try {
			// 打开二进制文件，并设置缓冲区大小
			open(1024);
			long []blocks = this.blocks;
			if (blocks == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
			
			// 取得字段索引
			int keyField = ds.getFieldIndex(key);
			if (keyField < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(key + mm.getMessage("ds.fieldNotExist"));
			}
			
			int lastBlock = this.lastBlock;
			int fcount = ds.getFieldCount();
			Object []vals = new Object[fcount];
			
			// 定义分段
			LongArray posArray = new LongArray(count > 5 ? count * 2 : 10);
			long prevEnd = position();
			int nextBlock = 0;
			Object nextBlockVal = null;
			if (lastBlock > 0) {
				seek(blocks[0]);
				readRecord(vals);
				nextBlockVal = vals[keyField];
			}
			
			int i = 1;
			while (i <= count && nextBlock < lastBlock) {
				Object val = values.getMem(i);
				int cmp = Variant.compare(val, nextBlockVal);
				
				// 因为文件可能有重复的值，相等的时候需要从前一块开始找
				if (cmp <= 0) {
					if (position() > prevEnd) {
						close();
						reopen(1024);
						seek(prevEnd);
					}
					
					while (true) {
						readRecord(vals);
						cmp = Variant.compare(val, vals[keyField]);
						if (cmp > 0) {
							prevEnd = position();
							if (prevEnd == blocks[nextBlock]) {
								nextBlock++;
								if (nextBlock < lastBlock) {
									seek(blocks[nextBlock]);
									readRecord(vals);
									nextBlockVal = vals[keyField];
								}
								
								break;
							}
						} else if (cmp == 0) {
							posArray.add(prevEnd);
							prevEnd = position();
							if (prevEnd == blocks[nextBlock]) {
								nextBlock++;
								if (nextBlock < lastBlock) {
									seek(blocks[nextBlock]);
									readRecord(vals);
									nextBlockVal = vals[keyField];
								}
								
								break;
							}

							// 文件可能有重复的值，继续找有没有和当前值重复的
							continue;
						} else {
							i++;
							while (i <= count) {
								val = values.getMem(i);
								cmp = Variant.compare(val, vals[keyField]);
								if (cmp > 0) {
									break;
								} else if (cmp == 0) {
									posArray.add(prevEnd);
									break;
								} else {
									i++;
								}
							}
							
							prevEnd = position();
							if (prevEnd == blocks[nextBlock]) {
								nextBlock++;
								if (nextBlock < lastBlock) {
									seek(blocks[nextBlock]);
									readRecord(vals);
									nextBlockVal = vals[keyField];
								}
								
								break;
							}
							
							break;
						}
					}
				} else {
					prevEnd = blocks[nextBlock];					
					nextBlock++;
					if (nextBlock < lastBlock) {
						seek(blocks[nextBlock]);
						readRecord(vals);
						nextBlockVal = vals[keyField];
					}
				}
			}
			
			if (i <= count) {
				if (position() > prevEnd) {
					close();
					reopen(1024);
					seek(prevEnd);
				}
				
				Object val = values.getMem(i);
				while (i <= count && readRecord(vals)) {
					int cmp = Variant.compare(val, vals[keyField]);
					if (cmp > 0) {
						prevEnd = position();
					} else if (cmp == 0) {
						posArray.add(prevEnd);
						prevEnd = position();
					} else {
						i++;
						while (i <= count) {
							val = values.getMem(i);
							cmp = Variant.compare(val, vals[keyField]);
							if (cmp > 0) {
								break;
							} else if (cmp == 0) {
								posArray.add(prevEnd);
								break;
							} else {
								i++;
							}
						}
						
						prevEnd = position();
					}
				}
			}
			
			if (posArray.size() == 0) {
				//return null; 改成返回空游标，这样cs.groups@t会返回空序表
				return new MemoryCursor(null);
			}
						
			return new PFileCursor(file, posArray.toArray(), 1024, fields, null, ctx);
		} catch (IOException e) {
			throw new RQException(e);
		} finally {
			try {
				close();
			} catch (IOException e) {
			}
		}
	}
	
	/**
	 * 从对x有序的文件f读出x在[a,b]区间的记录构成游标
	 * 从当前数据集中，选出key值在startVal和endVal之间的记录，选取fields字段值构成新表。并返回新表的游标。
	 * @param key		已经排好序的字段的字段名。
	 * @param startVal	筛选数据的起始值
	 * @param endVal	筛选数据的结束值
	 * @param fields	字段名列表，最终得结果表，由这些字段组成
	 * @param ctx		上下文变量
	 * @return
	 */
	public ICursor iselect(String key, Object startVal, Object endVal, String []fields, Context ctx) {
		int startBlock;
		int endBlock ;
		int keyField;
		long firstPos;
		
		try {
			open(1024);
			firstPos = position();
			long []blocks = this.blocks;
			if (blocks == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
			
			keyField = ds.getFieldIndex(key);
			if (keyField < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(key + mm.getMessage("ds.fieldNotExist"));
			}
			
			int lastBlock = this.lastBlock;
			startBlock = lastBlock;
			endBlock = lastBlock;
			
			for (int i = 0; i < lastBlock; ++i) {
				seek(blocks[i]);
				Object val = readRecordField(keyField);
				if (Variant.compare(val, startVal) >= 0) {
					startBlock = i;
					if (endVal != null && Variant.compare(val, endVal) > 0) {
						endBlock = i;
					}
					
					break;
				}
			}
			
			if (endVal != null && endBlock != startBlock) {
				for (int i = startBlock + 1; i < lastBlock; ++i) {
					seek(blocks[i]);
					Object val = readRecordField(keyField);
					if (Variant.compare(val, endVal) > 0) {
						endBlock = i;
						break;
					}
				}
			}
		} catch (IOException e) {
			throw new RQException(e);
		} finally {
			try {
				close();
			} catch (IOException e) {
			}
		}
		
		try {
			reopen(1024);
			seek(firstPos);
			
			long []blocks = this.blocks;
			long startPos = blocks[startBlock];
			int fcount = ds.getFieldCount();
			Object []vals = new Object[fcount];
			
			if (startBlock > 0) {
				seek(blocks[startBlock - 1]);
			}
			
			long pos = firstPos;
			while (pos < startPos) {
				readRecord(vals);
				if (Variant.compare(vals[keyField], startVal) >= 0) {
					if (endVal != null && Variant.compare(vals[keyField], endVal) > 0) {
						//return null; 改成返回空游标，这样cs.groups@t会返回空序表
						return new MemoryCursor(null);
					}
					
					startPos = pos;
					break;
				}
				
				pos = position();
			}
			
			long endPos = blocks[endBlock];
			if (endVal != null) {
				if (endBlock > 0 && position() < blocks[endBlock - 1]) {
					seek(blocks[endBlock - 1]);
				}
				
				pos = position();
				while (pos < endPos) {
					readRecord(vals);
					if (Variant.compare(vals[keyField], endVal) > 0) {
						endPos = pos;
						break;
					}
					
					pos = position();
				}
			}
			
			if (startPos < endPos) {
				BFileCursor cursor = new BFileCursor(file, fields, null, ctx);
				cursor.setPosRange(startPos, endPos);
				return cursor;
			} else {
				//return null; 改成返回空游标，这样cs.groups@t会返回空序表
				return new MemoryCursor(null);
			}
		} catch (IOException e) {
			throw new RQException(e);
		} finally {
			try {
				close();
			} catch (IOException e) {
			}
		}
	}
	
	/**
	 * 设置读取的结束位置，用于多线程分段读取
	 * @param pos 位置
	 */
	public void setEndPos(long pos) {
		this.endPos = pos;
	}
	
	/**
	 * 取结果集数据结构
	 * @return
	 */
	public DataStruct getResultSetDataStruct() {
		if (readDs == null) {
			return ds;
		} else {
			return readDs;
		}
	}
	
	/**
	 * 对比多个字段的值
	 * 
	 * @param fieldsValue	字段值
	 * @param refValues		参考值
	 * @return	1	fieldsValue的值比较大
	 * 			0	两个参数一样大
	 * 			-1	refValues的值比较大
	 */
	private int	compareFields(Object[] fieldsValue, Object refValues ) {
		Object refObj = null;
		for (int i = 0; i < fieldsValue.length; i++) {
			if (refValues instanceof Sequence) {
				refObj = ((Sequence)refValues).get(i+1);
			} else
				refObj = refValues;
			
			int res = Variant.compare(fieldsValue[i], refObj);
			if (res > 0)
				return 1;
			else if (res < 0)
				return -1;
		}
		return 0;
	}
	
	/**
	 * 对比多个字段的值
	 * 
	 * @param fieldsValue	字段值
	 * @param refValues		参考值
	 * @return	1	refValues的值比较大
	 * 			0	两个参数一样大
	 * 			-1	fieldsValue的值比较大
	 */	
	private int compareFields(Object refValues, Object[] fieldsValue) {
		Object refObj = null;
		for (int i = 0; i < fieldsValue.length; i++) {
			if (refValues instanceof Sequence) {
				refObj = ((Sequence)refValues).get(i+1);
			} else
				refObj = refValues;
			
			int res = Variant.compare(fieldsValue[i], refObj);
			if (res > 0)
				return -1;
			else if (res < 0)
				return 1;
		}
		return 0;		
	}
	
	/**
	 * 选出表达式的计算结果在values中的记录
	 * @param exp		表达式
	 * @param values	对比结果
	 * @param fields	组成结果的字段
	 * @param ctx		上下文变量
	 * @return			返回筛选出的记录
	 */
	public ICursor iselect(Expression exp, Sequence values,
			String []fields, Context ctx) {
		if (exp == null) {
			//return null; 改成返回空游标，这样cs.groups@t会返回空序表
			return new MemoryCursor(null);
		}
		
		
		// 多字段表达式，走另外的流程，可以大幅提高效率
		//  输入表达式是否式多字符串
		String[] fieldNames = exp.toFields();
		if (fieldNames != null) {
			try {
				open(1024);
				for (String name : fieldNames) {
					if (ds.getFieldIndex(name) == -1) {
						fieldNames = null;
						break;
					}
				}
			} catch (IOException e) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("file.fileNotExist", file.getFileName()));
			} finally {
				try {
					close();
				} catch (IOException e) {
				}
			}
		}
				
		if (fieldNames != null) {
			return iselectFields(fieldNames, values, fields, ctx);
		} else {
			return iselectExpression(exp, values, fields, ctx);
		}
	}
	
	/**
	 * 单字段、多字段从当前数据集中选出在 values中的记录
	 * 
	 * @param	refFields	参考字段
	 * @param	values		参考字段的参考值
	 * @param	fields      构成新表的字段，可以不包括参考字段
	 * @param	ctx			上下文变量
	 * 
	 * @return	返回筛选出的数据集cursor
	 * 
	*/
	public ICursor iselectFields(String[] refFields, Sequence values,
			String []fields, Context ctx) {
		// 查找对应列的索引
		int fcount = ds.getFieldCount();
		int[] selFields = new int[fcount];
		for (int i = 0; i < fcount; i++) {
			selFields[i] = -1;
		}

		int fcou = 0;
		for (int i = 0; i < refFields.length; i++) {
			int index = ds.getFieldIndex(refFields[i]);
			if (0 > index ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(refFields[i] + mm.getMessage("ds.fieldNotExist"));
			}
			selFields[index] = fcou;
			fcou++;
		}
		
		// 取得记录长度
		int count = values.length();
		if (count == 0) {
			//return null; 改成返回空游标，这样cs.groups@t会返回空序表
			return new MemoryCursor(null);
		}
		
		try {
			// 打开文件
			open(1024);
			long []blocks = this.blocks;
			if (blocks == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
					
			int lastBlock = this.lastBlock;
			Object []vals = new Object[fcou];
			
			LongArray posArray = new LongArray(count > 5 ? count * 2 : 10);
			long prevEnd = position();
			int nextBlock = 0;
			Object[] nextBlockVal = null;
			if (lastBlock > 0) {
				seek(blocks[0]);
				readRecord(selFields, vals);
				nextBlockVal = vals.clone();
			}
			
			int i = 1;
			while (i <= count && nextBlock < lastBlock) {
				// 取得一个参考值
				Object val = values.getMem(i);
				int cmp = compareFields(val, nextBlockVal);
				if (cmp <= 0) {
					if (position() > prevEnd) {
						close();
						reopen(1024);
						seek(prevEnd);
					}
					
					while (true) {
						readRecord(selFields, vals);
						cmp = compareFields(val, vals);
						if (cmp > 0) {
							prevEnd = position();
							if (prevEnd == blocks[nextBlock]) {
								nextBlock++;
								if (nextBlock < lastBlock) {
									seek(blocks[nextBlock]);
									readRecord(selFields, vals);
									nextBlockVal = vals.clone();
								}
								
								break;
							}
						} else if (cmp == 0) {
							posArray.add(prevEnd);
							prevEnd = position();
							if (prevEnd == blocks[nextBlock]) {
								nextBlock++;
								if (nextBlock < lastBlock) {
									seek(blocks[nextBlock]);
									readRecord(selFields, vals);
									nextBlockVal = vals.clone();
								}
								
								break;
							}

							continue;
						} else {
							i++;
							while (i <= count) {
								// 取得一个参考值
								val = values.getMem(i);
								cmp = compareFields(val, vals);
								if (cmp > 0) {
									break;
								} else if (cmp == 0) {
									posArray.add(prevEnd);
									break;
								} else {
									i++;
								}
							}
							
							prevEnd = position();
							if (prevEnd == blocks[nextBlock]) {
								nextBlock++;
								if (nextBlock < lastBlock) {
									seek(blocks[nextBlock]);
									readRecord(selFields, vals);
									nextBlockVal = vals.clone();
								}
								
								break;
							}
							
							break;
						}
					}
				} else {
					prevEnd = blocks[nextBlock];					
					nextBlock++;
					if (nextBlock < lastBlock) {
						seek(blocks[nextBlock]);
						readRecord(selFields, vals);
						nextBlockVal = vals.clone();
					}
				}
			}
			
			if (i <= count) {
				if (position() > prevEnd) {
					close();
					reopen(1024);
					seek(prevEnd);
				}
				
				Object val = values.getMem(i);
				while (i <= count && readRecord(selFields, vals)) {
					int cmp = compareFields(val, vals);
					if (cmp > 0) {
						prevEnd = position();
					} else if (cmp == 0) {
						posArray.add(prevEnd);
						prevEnd = position();
					} else {
						i++;
						while (i <= count) {
							val = values.getMem(i);
							cmp = compareFields(val, vals);
							if (cmp > 0) {
								break;
							} else if (cmp == 0) {
								posArray.add(prevEnd);
								break;
							} else {
								i++;
							}
						}
						
						prevEnd = position();
					}
				}
			}
			
			if (posArray.size() == 0) {
				//return null; 改成返回空游标，这样cs.groups@t会返回空序表
				return new MemoryCursor(null);
			}
						
			return new PFileCursor(file, posArray.toArray(), 1024, fields, null, ctx);
		} catch (IOException e) {
			throw new RQException(e);
		} finally {
			try {
				close();
			} catch (IOException e) {
			}
		}
	}
	
	private ICursor iselectExpression(Expression exp, Sequence values, String []fields, Context ctx) {
		int count = values.length();
		if (count == 0) {
			//return null; 改成返回空游标，这样cs.groups@t会返回空序表
			return new MemoryCursor(null);
		}
		
		try {
			open(1024);
			long []blocks = this.blocks;
			if (blocks == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
			
			int lastBlock = this.lastBlock;
			int fcount = ds.getFieldCount();
			Object []vals = new Object[fcount];
			Record rec = new Record(ds);
			
			LongArray posArray = new LongArray(count > 5 ? count * 2 : 10);
			long prevEnd = position();
			int nextBlock = 0;
			Object nextBlockVal = null;
			if (lastBlock > 0) {
				seek(blocks[0]);
				readRecord(vals);
				rec.values = vals;
				nextBlockVal = rec.calc(exp, ctx);
			}
			
			int i = 1;
			while (i <= count && nextBlock < lastBlock) {
				Object val = values.getMem(i);
				int cmp = Variant.compare(val, nextBlockVal);
				if (cmp <= 0) {
					if (position() > prevEnd) {
						close();
						reopen(1024);
						seek(prevEnd);
					}
					
					while (true) {
						readRecord(vals);
						rec.values = vals;
						Object reCal = rec.calc(exp, ctx);
						cmp = Variant.compare(val, reCal);
						if (cmp > 0) {
							prevEnd = position();
							if (prevEnd == blocks[nextBlock]) {
								nextBlock++;
								if (nextBlock < lastBlock) {
									seek(blocks[nextBlock]);
									readRecord(vals);
									rec.values = vals;
									nextBlockVal = rec.calc(exp, ctx);
								}
								
								break;
							}
						} else if (cmp == 0) {
							posArray.add(prevEnd);
							prevEnd = position();
							if (prevEnd == blocks[nextBlock]) {
								nextBlock++;
								if (nextBlock < lastBlock) {
									seek(blocks[nextBlock]);
									readRecord(vals);
									rec.values = vals;
									nextBlockVal = rec.calc(exp, ctx);
								}
								
								break;
							}

							continue;
						} else {
							i++;
							while (i <= count) {
								val = values.getMem(i);
								cmp = Variant.compare(val, rec.calc(exp, ctx));
								if (cmp > 0) {
									break;
								} else if (cmp == 0) {
									posArray.add(prevEnd);
									break;
								} else {
									i++;
								}
							}
							
							prevEnd = position();
							if (prevEnd == blocks[nextBlock]) {
								nextBlock++;
								if (nextBlock < lastBlock) {
									seek(blocks[nextBlock]);
									readRecord(vals);
									rec.values = vals;
									nextBlockVal = rec.calc(exp, ctx);
								}
								
								break;
							}
							
							break;
						}
					}
				} else {
					prevEnd = blocks[nextBlock];					
					nextBlock++;
					if (nextBlock < lastBlock) {
						seek(blocks[nextBlock]);
						readRecord(vals);
						rec.values = vals;
						nextBlockVal = rec.calc(exp, ctx);
					}
				}
			}
			
			if (i <= count) {
				if (position() > prevEnd) {
					close();
					reopen(1024);
					seek(prevEnd);
				}
				
				Object val = values.getMem(i);
				while (i <= count && readRecord(vals)) {
					rec.values = vals;
					int cmp = Variant.compare(val, rec.calc(exp, ctx));
					if (cmp > 0) {
						prevEnd = position();
					} else if (cmp == 0) {
						posArray.add(prevEnd);
						prevEnd = position();
					} else {
						i++;
						while (i <= count) {
							val = values.getMem(i);
							cmp = Variant.compare(val, rec.calc(exp, ctx));
							if (cmp > 0) {
								break;
							} else if (cmp == 0) {
								posArray.add(prevEnd);
								break;
							} else {
								i++;
							}
						}
						
						prevEnd = position();
					}
				}
			}
			
			if (posArray.size() == 0) {
				//return null; 改成返回空游标，这样cs.groups@t会返回空序表
				return new MemoryCursor(null);
			}
			
			return new PFileCursor(file, posArray.toArray(), 1024, fields, null, ctx);
		} catch (IOException e) {
			throw new RQException(e);
		} finally {
			try {
				close();
			} catch (IOException e) {
			}
		}
	}
	
	/**
	 * 表达式的值，在startVal和endVal之间的记录
	 * @param	exp			表达式。若e为null, 则结果也为null
	 * @param	startVal	起始值
	 * @param	endVal		结束值
	 * @param	fields      构成新表的字段，可以不包括参考字段
	 * @param	ctx			上下文变量
	 * 
	 * @return	返回对应的游标
	*/
	public ICursor iselect(Expression exp, Object startVal,
			Object endVal, String []fields, Context ctx) {
		if (exp == null) {
			//return null; 改成返回空游标，这样cs.groups@t会返回空序表
			return new MemoryCursor(null);
		}
		
		try {
			open(1024);
		} catch (IOException e) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("file.fileNoExist", file.getFileName()));
		}

		// 多字段表达式，走另外的流程，可以大幅提高效率
		//  输入表达式是否式多字符串
		String[] fieldNames = exp.toFields();
		if (null != fieldNames) {
			boolean multi = true;
			// 判断是否式多字段
			loop:for (int i = 0; i < fieldNames.length; i++) {
				for (int j = 0; j < ds.getFieldCount(); j ++) {
					if (fieldNames[i].equals(ds.getFieldName(j)))
						continue loop;
				}
				
				multi = false;
				break;
			}
			
			if (multi) {
				return iselectFields(fieldNames, startVal, endVal, fields, ctx);
			}
		}
		
		// 走普通的流程
		
		return iselectExpression(exp, startVal, endVal, fields, ctx);
	}
	/**
	 * 单字段、多字段值，在startVal和endVal之间的记录
	 * 
	 * @param	refFields	参考字段
	 * @param	startVal	起始值
	 * @param	endVal		结束值
	 * @param	fields      构成新表的字段，可以不包括参考字段
	 * @param	ctx			上下文变量
	 * 
	 * @return	返回对应的游标
	*/
	private ICursor iselectFields(String[] refFields, Object startVal, Object endVal, String []fields, Context ctx) {
		int startBlock;
		int endBlock ;
		long firstPos;
		Object[] vals = null; // 读取的记录数据,可能是单字段或多字段
		int fcount = ds.getFieldCount();
		int[] selFields = new int[fcount]; // 要读取的字段
		
		// 查找对应列的索引
		for (int i = 0; i < fcount; i++) {
			selFields[i] = -1;
		}

		int fcou = 0;
		for (int i = 0; i < refFields.length; i++) {
			int index = ds.getFieldIndex(refFields[i]);
			if (0 > index ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(refFields[i] + mm.getMessage("ds.fieldNotExist"));
			}
			selFields[index] = fcou;
			fcou++;
		}
				
		try {
			open(1024);
			firstPos = position();
			long []blocks = this.blocks;
			if (blocks == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
			
			int lastBlock = this.lastBlock;
			startBlock = lastBlock;
			endBlock = lastBlock;
			vals = new Object[fcou];
			
			for (int i = 0; i < lastBlock; ++i) {
				seek(blocks[i]);
				readRecord(selFields, vals);
				if (compareFields(vals, startVal) >= 0) {
					startBlock = i;
					if (endVal != null && compareFields(vals, endVal) > 0) {
						endBlock = i;
					}
					
					break;
				}
			}
			
			if (endVal != null && endBlock != startBlock) {
				for (int i = startBlock + 1; i < lastBlock; ++i) {
					seek(blocks[i]);
					readRecord(selFields, vals);
					if (compareFields(vals, startVal) > 0) {
						endBlock = i;
						break;
					}
				}
			}
		} catch (IOException e) {
			throw new RQException(e);
		} finally {
			try {
				close();
			} catch (IOException e) {
			}
		}
		
		try {
			reopen(1024);
			seek(firstPos);
			
			long []blocks = this.blocks;
			long startPos = blocks[startBlock];
			
			if (startBlock > 0) {
				seek(blocks[startBlock - 1]);
			}
			
			long pos = firstPos;
			while (pos < startPos) {
				readRecord(selFields, vals);
				if (compareFields(vals, startVal) >= 0) {
					if (endVal != null && compareFields(vals, endVal) > 0) {
						//return null; 改成返回空游标，这样cs.groups@t会返回空序表
						return new MemoryCursor(null);
					}
					
					startPos = pos;
					break;
				}
				
				pos = position();
			}
			
			long endPos = blocks[endBlock];
			if (endVal != null) {
				if (endBlock > 0 && position() < blocks[endBlock - 1]) {
					seek(blocks[endBlock - 1]);
				}
				
				pos = position();
				while (pos < endPos) {
					readRecord(selFields, vals);
					if (compareFields(vals, endVal) > 0) {
						endPos = pos;
						break;
					}
					
					pos = position();
				}
			}
			
			if (startPos < endPos) {
				BFileCursor cursor = new BFileCursor(file, fields, null, ctx);
				cursor.setPosRange(startPos, endPos);
				return cursor;
			} else {
				//return null; 改成返回空游标，这样cs.groups@t会返回空序表
				return new MemoryCursor(null);
			}
		} catch (IOException e) {
			throw new RQException(e);
		} finally {
			try {
				close();
			} catch (IOException e) {
			}
		}
	}
	
	/**
	 * 表达式，在startVal和endVal之间的记录
	 * 
	 * @param	e			计算表达式
	 * @param	startVal	起始值
	 * @param	endVal		结束值
	 * @param	fields      构成新表的字段，可以不包括参考字段
	 * @param	ctx			上下文变量
	 * 
	 * @return	返回对应的游标
	*/
	private ICursor iselectExpression(Expression exp, Object startVal, Object endVal, String []fields, Context ctx) {
		int startBlock;
		int endBlock ;
		long firstPos;
		Object[] vals = null; // 读取的记录数据,可能是单字段或多字段
		Record rec = new Record(ds);
		
		// 与起始值做比较
		try {
			open(1024);
			firstPos = position();
			long []blocks = this.blocks;
			if (blocks == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
			
			int lastBlock = this.lastBlock;
			startBlock = lastBlock;
			endBlock = lastBlock;
			vals = new Object[ds.getFieldCount()];
			
			for (int i = 0; i < lastBlock; ++i) {
				seek(blocks[i]);
				readRecord(vals);
				rec.values = vals;
				if (Variant.compare(rec.calc(exp, ctx), startVal) >= 0) {
					startBlock = i;
					if (endVal != null && Variant.compare(rec.calc(exp, ctx), endVal) > 0) {
						endBlock = i;
					}
					
					break;
				}
			}
			
			if (endVal != null && endBlock != startBlock) {
				for (int i = startBlock + 1; i < lastBlock; ++i) {
					seek(blocks[i]);
					readRecord(vals);
					rec.values = vals;
					if (Variant.compare(rec.calc(exp, ctx), startVal) > 0) {
						endBlock = i;
						break;
					}
				}
			}
		} catch (IOException e) {
			throw new RQException(e);
		} finally {
			try {
				close();
			} catch (IOException e) {
			}
		}
		
		// 与结束值做比较
		try {
			reopen(1024);
			seek(firstPos);
			
			long []blocks = this.blocks;
			long startPos = blocks[startBlock];
			
			if (startBlock > 0) {
				seek(blocks[startBlock - 1]);
			}
			
			long pos = firstPos;
			while (pos < startPos) {
				readRecord(vals);
				rec.values = vals;
				if (Variant.compare(rec.calc(exp, ctx), startVal) >= 0) {
					if (endVal != null && Variant.compare(rec.calc(exp, ctx), endVal) > 0) {
						//return null; 改成返回空游标，这样cs.groups@t会返回空序表
						return new MemoryCursor(null);
					}
					
					startPos = pos;
					break;
				}
				
				pos = position();
			}
			
			long endPos = blocks[endBlock];
			if (endVal != null) {
				if (endBlock > 0 && position() < blocks[endBlock - 1]) {
					seek(blocks[endBlock - 1]);
				}
				
				pos = position();
				while (pos < endPos) {
					readRecord(vals);
					rec.values = vals;
					if (Variant.compare(rec.calc(exp, ctx), endVal) > 0) {
						endPos = pos;
						break;
					}
					
					pos = position();
				}
			}
			
			if (startPos < endPos) {
				BFileCursor cursor = new BFileCursor(file, fields, null, ctx);
				cursor.setPosRange(startPos, endPos);
				return cursor;
			} else {
				//return null; 改成返回空游标，这样cs.groups@t会返回空序表
				return new MemoryCursor(null);
			}
		} catch (IOException e) {
			throw new RQException(e);
		} finally {
			try {
				close();
			} catch (IOException e) {
			}
		}
	}
	
	/**
	 * 根据n返回分段点值和每段条数
	 * @param list 返回的每段条数
	 * @param values 返回分段点值
	 * @param n 期望的每段条数
	 * @throws IOException 
	 */
	public void getSegmentInfo(ArrayList<Integer> list, Sequence values, int n) throws IOException {
		open();
		long []blocks = this.blocks;
		if (blocks == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("license.fileFormatError"));
		}
		
		int blockCount = lastBlock + 1;
		long blockRecordCount = this.blockRecordCount;
		int sum = (int) blockRecordCount;
		int colCount = readFields.length;
		
		for (int i = 1; i < blockCount; ++i) {
			if (sum + blockRecordCount > n) {
				list.add(sum);
				sum = (int) blockRecordCount;
				seek(blocks[i - 1]);
				Object []vals = new Object[colCount];
				readRecord(readIndex, vals);
				values.add(vals);
			} else {
				sum += blockRecordCount;
			}
		}
		
		list.add(sum);//最后一个分段条数，有可能只有这一个
		close();
	}
}