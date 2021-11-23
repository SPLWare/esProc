package com.scudata.dm.cursor;

import java.io.IOException;
import java.io.InputStream;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.KeyWord;
import com.scudata.dm.LineImporter;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 文件游标，用于读取文本文件
 * @author WangXiaoJun
 *
 */
public class FileCursor extends ICursor {
	private FileObject fileObject; // 文件对象
	private LineImporter importer; // 文本分析类，用于把文本按行读成字段数组
	private DataStruct ds; // 文件对应的数据结构
	
	private long start; // 读取的起始位置，要做掐头去尾处理，用于并行读文件
	private long end = -1; // 读取的结束位置，要做掐头去尾处理，用于并行读文件

	private String []selFields; // 选出字段名数组
	private byte []types; // 字段类型
	private String []fmts; // 字段值格式，用于日期时间
	private int []selIndex; // 选出字段在源结构中的序号
	private DataStruct selDs; // 结果集数据结构
	private String opt; // 选项
	
	private byte [] colSeparator; // 列分割符
	private boolean isTitle; // 文件是否有标题，如果有将作为结构名
	private boolean isDeleteFile; // 读完后是否删除文件
	private boolean isSingleField; // 是否返回单列组成的序列
	private boolean isSequenceMember; // 是否返回序列组成的序列
	private int sigleFieldIndex; // 单列时的字段索引
	private boolean isExist = true; // 字段是否都在文件中
	private boolean isEnd = false;
	
	private boolean optimize = true; // ide需要用，parse时是否先判断能不能转成上一条记录的类型
	
	/**
	 * 产生一个文本文件的游标
	 * @param fileObject 文本文件
	 * @param segSeq 段号，从1开始计数
	 * @param segCount 分段数
	 * @param s 列分隔符
	 * @param opt 选项  t：第一行为标题，b：二进制文件，c：写成逗号分隔的csv文件
	 * 	s：不拆分字段，读成单字段串构成的序表，i：结果集只有1列时返回成序列
	 * 	q：如果字段串外有引号则先剥离，包括标题部分，k：保留数据项两端的空白符，缺省将自动做trim
	 * 	e：Fi在文件中不存在时将生成null，缺省将报错
	 * @param ctx
	 */
	public FileCursor(FileObject fileObject, int segSeq, int segCount, 
			String s, String opt, Context ctx) {
		this(fileObject, segSeq, segCount, null, null, s, opt, ctx);
	}

	/**
	 * 产生一个文本文件的游标
	 * @param fileObject 文本文件
	 * @param segSeq 段号，从1开始计数
	 * @param segCount 分段数
	 * @param fields 选出字段名数组
	 * @param types 选出字段类型数组（可空），参照com.raqsoft.common.Types
	 * @param s 列分隔符
	 * @param opt 选项  t：第一行为标题，b：二进制文件，c：写成逗号分隔的csv文件
	 * 	s：不拆分字段，读成单字段串构成的序表，i：结果集只有1列时返回成序列
	 * 	q：如果字段串外有引号则先剥离，包括标题部分，k：保留数据项两端的空白符，缺省将自动做trim
	 * 	e：Fi在文件中不存在时将生成null，缺省将报错
	 * @param ctx
	 */
	public FileCursor(FileObject fileObject, int segSeq, int segCount, 
			String []fields, byte []types, String s, String opt, Context ctx) {
		if (segCount > 1) {
			if (segSeq < 1 || segSeq > segCount) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(segSeq + mm.getMessage("function.invalidParam"));
			}

			long size = fileObject.size();
			long blockSize = size / segCount;
			if (segSeq == segCount) {
				end = size;
				start = blockSize * (segSeq - 1);
			} else {
				end = blockSize * segSeq;
				start = blockSize * (segSeq - 1);
			}
		}

		this.fileObject = fileObject;
		this.types = types;
		this.opt = opt;
		this.ctx = ctx;
		
		if (fields != null) {
			selFields = new String[fields.length];
			System.arraycopy(fields, 0, selFields, 0, fields.length);
		}
		
		boolean isCsv = false;
		if (opt != null) {
			if (opt.indexOf('t') != -1) isTitle = true;
			if (opt.indexOf('c') != -1) isCsv = true;
			if (opt.indexOf('i') != -1) isSingleField = true;
			if (opt.indexOf('e') != -1) isExist = false;
						
			if (opt.indexOf('x') != -1) {
				isDeleteFile = true;
				if (ctx != null) ctx.addResource(this);
			}
			
			if (opt.indexOf('w') != -1) isSequenceMember = true;
		}

		if (s != null && s.length() > 0) {
			String charset = fileObject.getCharset();
			try {
				colSeparator = s.getBytes(charset);
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		} else if (isCsv) {
			colSeparator = new byte[]{(byte)','};
		} else {
			colSeparator = FileObject.COL_SEPARATOR;
		}
	}

	/**
	 * 设置读文件的起始位置
	 * @param startPos 起始位置，会做掐头去尾处理
	 */
	public void setStart(long start) {
		this.start = start;
	}
	
	/**
	 * 设置读文件的结束位置
	 * @param endPos 结束位置，会做掐头去尾处理
	 */
	public void setEnd(long end) {
		this.end = end;
	}
	
	/**
	 * 设置日期时间字段的格式
	 * @param fmts 日期时间格式数组
	 */
	public void setFormats(String []fmts) {
		this.fmts = fmts;
	}
	
	private LineImporter open() {
		if (importer != null) {
			return importer;
		} else if (fileObject == null || isEnd) {
			return null;
		}

		if (!isDeleteFile && ctx != null) {
			ctx.addResource(this);
		}
		
		InputStream in = null;
		String []selFields = this.selFields;
		
		try {
			in = fileObject.getBlockInputStream();
			String charset = fileObject.getCharset();
			importer = new LineImporter(in, charset, colSeparator, opt);
			
			if (isTitle) {
				// 第一行是标题
				Object []line = importer.readFirstLine();
				if (line == null) {
					return null;
				}

				int fcount = line.length;
				String []fieldNames = new String[fcount];
				for (int f = 0; f < fcount; ++f) {
					fieldNames[f] = Variant.toString(line[f]);
				}

				ds = new DataStruct(fieldNames);
				if (selFields != null) {
					if (isSingleField) isSingleField = selFields.length == 1;
					
					int maxSeq = 0;
					int []index = new int[fcount];
					for (int i = 0; i < fcount; ++i) {
						index[i] = -1;
					}

					for (int i = 0, count = selFields.length; i < count; ++i) {
						int q = ds.getFieldIndex(selFields[i]);
						if (q >= 0) {
							if (index[q] != -1) {
								MessageManager mm = EngineMessage.get();
								throw new RQException(selFields[i] + mm.getMessage("ds.colNameRepeat"));
							}
			
							index[q] = i;
							selFields[i] = ds.getFieldName(q);
							sigleFieldIndex = q;
							
							if (q > maxSeq) {
								maxSeq = q;
							}
						} else if (isExist) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(selFields[i] + mm.getMessage("ds.fieldNotExist"));
						}
					}

					this.selDs = new DataStruct(selFields);
					setDataStruct(selDs);
					
					maxSeq++;
					if (maxSeq < fcount) {
						int []tmp = new int[maxSeq];
						System.arraycopy(index, 0, tmp, 0, maxSeq);
						index = tmp;
					}
					
					this.selIndex = index;
					importer.setColSelectIndex(index);
					
					if (optimize) {
						byte []colTypes = new byte[maxSeq];
						String []colFormats = new String[maxSeq];
						if (types != null) {
							for (int i = 0; i < maxSeq; ++i) {
								if (index[i] != -1) {
									colTypes[i] = types[index[i]];
								}
							}
						}
						
						if (fmts != null) {
							for (int i = 0; i < maxSeq; ++i) {
								if (index[i] != -1) {
									colFormats[i] = fmts[index[i]];
								}
							}
						}
						
						importer.setColTypes(colTypes, colFormats);
					}
				} else {
					setDataStruct(ds);
					if (isSingleField && fcount != 1) {
						isSingleField = false;
					}
					
					if (optimize) {
						byte []colTypes = new byte[fcount];
						importer.setColTypes(colTypes, fmts);
					}
				}
			} else {
				if (selFields != null) {
					if (isSingleField) {
						isSingleField = selFields.length == 1;
					}
					
					int fcount = 0;
					for (int i = 0, count = selFields.length; i < count; ++i) {
						if (KeyWord.isFieldId(selFields[i])) {
							int f =  KeyWord.getFiledId(selFields[i]);
							if (f > fcount) {
								fcount = f;
							}
						} else if (isExist) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(selFields[i] + mm.getMessage("ds.fieldNotExist"));
						}
					}
	
					int []index = new int[fcount];
					for (int i = 0; i < fcount; ++i) {
						index[i] = -1;
					}

					String[] fieldNames = new String[fcount];
					ds = new DataStruct(fieldNames);
					for (int i = 0, count = selFields.length; i < count; ++i) {
						int q = ds.getFieldIndex(selFields[i]);
						if (q >= 0) {
							if (index[q] != -1) {
								MessageManager mm = EngineMessage.get();
								throw new RQException(selFields[i] + mm.getMessage("ds.colNameRepeat"));
							}
			
							index[q] = i;
							selFields[i] = ds.getFieldName(q);
							sigleFieldIndex = q;
						}
					}

					this.selDs = new DataStruct(selFields);
					this.selIndex = index;
					importer.setColSelectIndex(index);
					
					if (optimize) {
						byte []colTypes = new byte[fcount];
						String []colFormats = new String[fcount];
						if (types != null) {
							for (int i = 0; i < fcount; ++i) {
								if (index[i] != -1) {
									colTypes[i] = types[index[i]];
								}
							}
						}
						
						if (fmts != null) {
							for (int i = 0; i < fcount; ++i) {
								if (index[i] != -1) {
									colFormats[i] = fmts[index[i]];
								}
							}
						}
						
						importer.setColTypes(colTypes, colFormats);
					}
				}
			}

			importer.seek(start);
			if (end != -1 && importer.getCurrentPosition() > end) {
				return null;
			}

			return importer;
		} catch (Exception e) {
			// importer产生过程中可能出异常
			if (in != null && importer == null) {
				try {
					in.close();
				} catch (IOException ie) {
				}
			}
			
			close();
			
			if (e instanceof RQException) {
				throw (RQException)e;
			} else {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	// 取所有字段
	private Sequence fetchAll(LineImporter importer, int n) throws IOException {
		Object []line;
		long end = this.end;
		int initSize = n > INITSIZE ? INITSIZE : n;
		if (isSequenceMember) {
			Sequence seq = new Sequence(initSize);
			for (int i = 0; i < n; ++i) {
				if (end != -1 && importer.getCurrentPosition() > end) {
					break;
				}

				line = importer.readLine();
				if (line == null) {
					break;
				} else {
					seq.add(new Sequence(line));
				}
			}

			if (seq.length() != 0) {
				return seq;
			} else {
				return null;
			}
		}
		
		int fcount;
		if (ds == null) {
			// 首次读且没有标题
			line = importer.readFirstLine();
			if (line == null) {
				return null;
			}

			fcount = line.length;
			String []fieldNames = new String[fcount];
			ds = new DataStruct(fieldNames);
			
			if (isSingleField && fcount != 1) {
				isSingleField = false;
			}
			
			if (optimize) {
				byte []colTypes = new byte[fcount];
				for (int i = 0; i < fcount; ++i) {
					colTypes[i] = Variant.getObjectType(line[i]);
				}
	
				importer.setColTypes(colTypes, fmts);
			}
		} else {
			fcount = ds.getFieldCount();
			line = importer.readLine();
			if (line == null) {
				return null;
			}
		}
		
		if (isSingleField) {
			Sequence seq = new Sequence(initSize);
			seq.add(line[0]);

			for (int i = 1; i < n; ++i) {
				if (end != -1 && importer.getCurrentPosition() > end) {
					break;
				}

				line = importer.readLine();
				if (line == null) {
					break;
				}

				seq.add(line[0]);
			}

			return seq;
		} else {
			Table table = new Table(ds, initSize);
			Record r = table.newLast();
			int curLen = line.length;
			if (curLen > fcount) curLen = fcount;
			for (int f = 0; f < curLen; ++f) {
				r.setNormalFieldValue(f, line[f]);
			}

			for (int i = 1; i < n; ++i) {
				if (end != -1 && importer.getCurrentPosition() > end) {
					break;
				}

				line = importer.readLine();
				if (line == null) {
					break;
				}

				r = table.newLast();
				curLen = line.length;
				if (curLen > fcount) curLen = fcount;
				for (int f = 0; f < curLen; ++f) {
					r.setNormalFieldValue(f, line[f]);
				}
			}

			return table;
		}
	}

	// 有选出字段时的取数
	private Sequence fetchFields(LineImporter importer, int n) throws IOException {
		Object []line;
		long end = this.end;
		int initSize = n > INITSIZE ? INITSIZE : n;
		int []selIndex = this.selIndex;
		
		if (isSingleField) {
			int index = this.sigleFieldIndex;
			Sequence seq = new Sequence(initSize);
			for (int i = 0; i < n; ++i) {
				if (end != -1 && importer.getCurrentPosition() > end) {
					break;
				}

				line = importer.readLine();
				if (line == null) {
					break;
				}

				if (index < line.length) {
					seq.add(line[index]);
				} else {
					seq.add(null);
				}
			}

			if (seq.length() != 0) {
				return seq;
			} else {
				return null;
			}
		} else {
			int curLen;
			Record r;
			Table table = new Table(selDs, initSize);
			
			for (int i = 0; i < n; ++i) {
				if (end != -1 && importer.getCurrentPosition() > end) {
					break;
				}

				line = importer.readLine();
				if (line == null) {
					break;
				}

				r = table.newLast();
				curLen = line.length;
				for (int f = 0; f < curLen; ++f) {
					if (selIndex[f] != -1) r.setNormalFieldValue(selIndex[f], line[f]);
				}
			}

			if (table.length() != 0) {
				return table;
			} else {
				return null;
			}
		}
	}

	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (n < 1) return null;
		LineImporter importer = open();
		if (importer == null) return null;

		try {
			if (selFields == null) {
				return fetchAll(importer, n);
			} else {
				return fetchFields(importer, n);
			}
		} catch (IOException e) {
			close();
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		if (n < 1) return 0;

		LineImporter importer = open();
		if (importer == null) return 0;

		try {
			long end = this.end;
			for (long i = 0; i < n; ++i) {
				if (end != -1 && importer.getCurrentPosition() > end) {
					break;
				}

				if (!importer.skipLine()) {
					return i;
				}
			}
		} catch (IOException e) {
			close();
			throw new RQException(e.getMessage(), e);
		}

		return n;
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		if (fileObject != null) {
			isEnd = true;
			if (importer != null) {
				if (ctx != null) ctx.removeResource(this);
				try {
					importer.close();
				} catch (IOException e) {
				}
			}

			if (isDeleteFile) {
				fileObject.delete();
				fileObject = null;
			}

			importer = null;
			ds = null;
			selDs = null;
		}
	}

	protected void finalize() throws Throwable {
		close();
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		close();
		
		if (fileObject != null) {
			isEnd = false;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 是否对字段parse做优化，优化时会先按上一条记录的字段类型转
	 * @param optimize true：优化，false：不优化
	 */
	public void setOptimize(boolean optimize) {
		this.optimize = optimize;
	}
}
