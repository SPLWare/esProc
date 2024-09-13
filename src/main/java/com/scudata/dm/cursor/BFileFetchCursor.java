package com.scudata.dm.cursor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BFileReader;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.KeyWord;
import com.scudata.dm.LineImporter;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 读取集文件(把每条记录的读为字节数组)
 * 用于集文件排序
 * @author LW
 *
 */
public class BFileFetchCursor extends ICursor {
	private static final String BYTES_FIELD_NAME = "BFILE_BYTES_FIELD";
	private BFileReader reader;
	private BFileReader reader2;
	private ObjectReader in;
	
	private LineImporter importer;
	private String []selFields; // 选出字段名数组
	private byte []types; // 字段类型
//	private String []fmts; // 字段值格式，用于日期时间
	private int []selIndex; // 选出字段在源结构中的序号
	private String opt; // 选项
	private byte [] colSeparator; // 列分割符
	private boolean isTitle; // 文件是否有标题，如果有将作为结构名
	private boolean isSingleField; // 是否返回单列组成的序列
	
	private int []fields;
	private int bytesIndex;
	private DataStruct fileDataStruct;
	private byte[] titleBytes;
	
	/**
	 * @param reader
	 * @param fields
	 */
	public BFileFetchCursor(FileObject file, String []fields) {
		reader = new BFileReader(file, fields, null);
		BFileReader reader2 = new BFileReader(file, fields, null);
		
		try {
			reader.open();
			this.fields = reader.getReadIndex();
			
			int len = fields.length;
			bytesIndex = len;
			
			String[] dsFields = Arrays.copyOf(fields, len + 1);
			dsFields[len] = BYTES_FIELD_NAME;
			dataStruct = new DataStruct(dsFields);
			fileDataStruct = reader.getFileDataStruct();
			
			reader2.open();
			in = reader2.getImporter();
			
		} catch (IOException e) {
			try {
				reader.close();
				reader2.close();
				in.close();
			} catch (IOException e1) {
			}
		}
	}
	
	//文本文件
	public BFileFetchCursor(FileObject fileObject, String []fields, byte []types, String s, String opt, Context ctx) {
		this.types = types;
		this.opt = opt;
		this.ctx = ctx;
		
		if (fields != null) {
			selFields = new String[fields.length];
			System.arraycopy(fields, 0, selFields, 0, fields.length);
			int len = fields.length;
			bytesIndex = len;
			String[] dsFields = Arrays.copyOf(fields, len + 1);
			dsFields[len] = BYTES_FIELD_NAME;
			dataStruct = new DataStruct(dsFields);
		}
		
		boolean isCsv = false;
		if (opt != null) {
			if (opt.indexOf('t') != -1) isTitle = true;
			if (opt.indexOf('c') != -1) isCsv = true;
			if (opt.indexOf('i') != -1) isSingleField = true;
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
		
		InputStream is = fileObject.getBlockInputStream(Env.FILE_BUFSIZE);
		in = new ObjectReader(is, Env.FILE_BUFSIZE);
		
		importer = open(fileObject, fields);
	}
	
	private LineImporter open(FileObject fileObject, String[] selFields) {
		if (importer != null) {
			return importer;
		} else if (fileObject == null) {
			return null;
		}
		
		InputStream in = null;
		
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
				
				int titleLen = (int) importer.getCurrentPosition();
				titleBytes = new byte[titleLen];
				this.in.read(titleBytes);

				int fcount = line.length;
				String []fieldNames = new String[fcount];
				for (int f = 0; f < fcount; ++f) {
					fieldNames[f] = Variant.toString(line[f]);
				}

				DataStruct ds = fileDataStruct = new DataStruct(fieldNames);
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
							
							if (q > maxSeq) {
								maxSeq = q;
							}
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException(selFields[i] + mm.getMessage("ds.fieldNotExist"));
						}
					}
//
//					this.selDs = new DataStruct(selFields);
//					setDataStruct(selDs);
					
					maxSeq++;
					if (maxSeq < fcount) {
						int []tmp = new int[maxSeq];
						System.arraycopy(index, 0, tmp, 0, maxSeq);
						index = tmp;
					}
					
					this.selIndex = index;
					importer.setColSelectIndex(index);
					types = new byte[maxSeq];
					importer.setColTypes(types, null);
				} else {
					setDataStruct(ds);
					if (isSingleField && fcount != 1) {
						isSingleField = false;
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
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException(selFields[i] + mm.getMessage("ds.fieldNotExist"));
						}
					}
	
					int []index = new int[fcount];
					for (int i = 0; i < fcount; ++i) {
						index[i] = -1;
					}

					String[] fieldNames = new String[fcount];
					DataStruct ds = fileDataStruct = new DataStruct(fieldNames);
					for (int i = 0, count = selFields.length; i < count; ++i) {
						int q = ds.getFieldIndex(selFields[i]);
						if (q >= 0) {
							if (index[q] != -1) {
								MessageManager mm = EngineMessage.get();
								throw new RQException(selFields[i] + mm.getMessage("ds.colNameRepeat"));
							}
			
							index[q] = i;
							selFields[i] = ds.getFieldName(q);
						}
					}

//					this.selDs = new DataStruct(selFields);
					this.selIndex = index;
					importer.setColSelectIndex(index);
					types = new byte[fcount];
					importer.setColTypes(types, null);
				}
			}

//			importer.seek(start);
//			if (end != -1 && importer.getCurrentPosition() > end) {
//				return null;
//			}

			this.in.seek(importer.getCurrentPosition());
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
	
	public DataStruct getFileDataStruct() {
		return fileDataStruct;
	}

	protected Sequence get(int n) {
		if (importer != null) {
			return getTxt(n);
		}
		
		if (reader == null || n < 1) {
			return null;
		}
		
		int count = 0;
		Sequence result = new Sequence(n);
		
		BFileReader reader = this.reader;
		ObjectReader in = this.in;
		int []fields = this.fields;
		DataStruct ds = this.dataStruct;
		int bytesIndex = this.bytesIndex;
		
		long lastPos = reader.position();
		
		try {
			while (count < n) {
				Record rec = new Record(ds);
				Object[] values = rec.getFieldValues();
				if (!reader.readRecord(fields, values)) {
					break;
				}
				long pos = reader.position();
				int length = (int) (pos - lastPos);
				
				byte[] bytes = new byte[length];
				
				in.read(bytes);
				values[bytesIndex] = bytes;
				result.add(rec);
				lastPos = pos;
				count++;
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		if (result.length() == 0)
			return null;
		else
			return result;
	}

	private Sequence getTxt(int n) {
		int count = 0;
		Sequence result = new Sequence(n);
		
		LineImporter importer = this.importer;
		ObjectReader in = this.in;
		int []selIndex = this.selIndex;
		DataStruct ds = this.dataStruct;
		int bytesIndex = this.bytesIndex;
		
		long lastPos = importer.getCurrentPosition();
		
		try {
			while (count < n) {
				Record rec = new Record(ds);
				Object[] values = importer.readLine();
				if (values == null) {
					break;
				}
				int curLen = values.length;
				for (int f = 0; f < curLen; ++f) {
					if (selIndex[f] != -1) rec.setNormalFieldValue(selIndex[f], values[f]);
				}
				
				long pos = importer.getCurrentPosition();
				int length = (int) (pos - lastPos);
				
				byte[] bytes = new byte[length];
				
				in.read(bytes);
				rec.setNormalFieldValue(bytesIndex, bytes);
				result.add(rec);
				lastPos = pos;
				count++;
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		if (result.length() == 0)
			return null;
		else
			return result;
	}
	
	protected long skipOver(long n) {
		throw new RuntimeException();
	}
	
	public void close() {
		super.close();
		try {
			if (importer != null) {
				if (ctx != null) ctx.removeResource(this);
				try {
					importer.close();
				} catch (IOException e) {
				}
				importer = null;
			}
			
			if (in != null) {
				in.close();
				in = null;
			}
			if (reader != null) {
				reader.close();
				reader = null;
			}
			if (reader2 != null) {
				reader2.close();
				reader2 = null;
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	public byte[] getTitleBytes() {
		return titleBytes;
	}
}
