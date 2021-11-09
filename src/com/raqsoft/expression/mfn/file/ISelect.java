package com.raqsoft.expression.mfn.file;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.BFileReader;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.IFile;
import com.raqsoft.dm.LineImporter;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.FileCursor;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.cursor.MemoryCursor;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.FileFunction;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * 从对表达式有序的文件（文本文件或集文件）中查找记录
 * f.iselect(A,x;Fi,…;s) 读出x在序列A中的记录构成游标
 * f.iselect(a:b,x;Fi,…;s) 读出x在[a,b]区间的记录构成游标
 * @author RunQian
 *
 */
public class ISelect extends FileFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("iselect" + mm.getMessage("function.missingParam"));
		}

		IParam valParam;
		String []selFields = null;
		String s = null;
		if (param.getType() == IParam.Semicolon) {
			valParam = param.getSub(0);
			IParam fieldParam = param.getSub(1);
			if (fieldParam == null) {
			} else if (fieldParam.isLeaf()) {
				selFields = new String[]{fieldParam.getLeafExpression().getIdentifierName()};
			} else {
				int size = fieldParam.getSubSize();
				selFields = new String[size];
				for (int i = 0; i < size; ++i) {
					IParam sub = fieldParam.getSub(i);
					if (sub == null || !sub.isLeaf()) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("iselect" + mm.getMessage("function.invalidParam"));
					}

					selFields[i] = sub.getLeafExpression().getIdentifierName();
				}
			}

			if (param.getSubSize() > 2) {
				IParam sParam = param.getSub(2);
				if (sParam != null) {
					Object obj = sParam.getLeafExpression().calculate(ctx);
					if (!(obj instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("iselect" + mm.getMessage("function.paramTypeError"));
					}

					s = (String)obj;
				}
			}
		} else {
			valParam = param;
		}

		if (valParam.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("iselect" + mm.getMessage("function.invalidParam"));
		}

		IParam sub0 = valParam.getSub(0);
		IParam sub1 = valParam.getSub(1);
		if (sub0 == null || sub1 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("iselect" + mm.getMessage("function.invalidParam"));
		}

		if (sub0.isLeaf()) {
			Object key = sub0.getLeafExpression().calculate(ctx);
			Sequence values;
			if (key == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("iselect" + mm.getMessage("function.invalidParam"));
			}
			if (key instanceof Sequence) {
				values = (Sequence)key;
			} else {
				values = new Sequence(1);
				values.add(key);
			}
			
			Expression exp = sub1.getLeafExpression();
			if (null == exp){
				MessageManager mm = EngineMessage.get();
				throw new RQException("iselect" + mm.getMessage("function.paramTypeError"));
			}
				
			return search(file, exp, values, selFields, s, option, ctx);
		} else {
			if (sub0.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("iselect" + mm.getMessage("function.invalidParam"));
			}
			
			Object startVal = null;
			Object endVal = null;
			IParam startParam = sub0.getSub(0);
			if (startParam != null) {
				startVal = startParam.getLeafExpression().calculate(ctx);
			}
			
			IParam endParam = sub0.getSub(1);
			if (endParam != null) {
				endVal = endParam.getLeafExpression().calculate(ctx);
			}
			
			Expression exp = sub1.getLeafExpression();
			if (null == exp){
				MessageManager mm = EngineMessage.get();
				throw new RQException("iselect" + mm.getMessage("function.paramTypeError"));
			}
				
			return search(file, exp, startVal, endVal, selFields, s, option, ctx);
		}
	}	
	
	private static ICursor search(FileObject fo, Expression exp, Sequence values,
								 String []selFields, String s, String opt, Context ctx) {
		boolean isCsv = false;
		boolean isMultiId = false;
		boolean isExist = true;
		if (opt != null) {
			if (opt.indexOf('b') != -1) {
				BFileReader reader = new BFileReader(fo);
				return reader.iselect(exp, values, selFields, ctx);
			}
			
			if (opt.indexOf('c') != -1) isCsv = true;
			if (opt.indexOf('e') != -1) isExist = false;
			if (opt.indexOf('r') != -1) isMultiId = true;
		}

		String charset = fo.getCharset();
		byte[] separator;
		if (s != null && s.length() > 0) {
			try {
				separator = s.getBytes(charset);
			} catch (UnsupportedEncodingException e) {
				throw new RQException(e.getMessage(), e);
			}
		} else if (isCsv) {
			separator = new byte[]{(byte)','};
		} else {
			separator = FileObject.COL_SEPARATOR;
		}


		Table table = iselect_t(fo, exp, values, separator, opt, isMultiId, ctx);
		
		if (selFields != null) {
			DataStruct ds = table.dataStruct();
			int fcount = selFields.length;
			int []index = new int[fcount];
			String []names = ds.getFieldNames();
			for (int f = 0; f < fcount; ++f) {
				index[f] = ds.getFieldIndex(selFields[f]);
				if (index[f] < 0) {
					if (isExist) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(selFields[f] + mm.getMessage("ds.fieldNotExist"));
					}
				} else {
					selFields[f] = names[index[f]];
				}
			}

			int len = table.length();
			Table selTable = new Table(selFields, len);
			for (int i = 1; i <= len; ++i) {
				Record nr = selTable.newLast();
				Record r = (Record)table.get(i);
				for (int f = 0; f < fcount; ++f) {
					if (index[f] >= 0) {
						nr.setNormalFieldValue(f, r.getFieldValue(index[f]));
					}
				}
			}

			table = selTable;
		}
		
		if (table != null && table.length() > 0) {
			return new MemoryCursor(table);
		} else {
			return null;
		}
	}

	/**
	 * 根据对比数据和对比表达式从文件中查找结果，并变成cursor输出
	 * 
	 * @param fo			文件对象
	 * @param fieldName		对比表达式
	 * @param startVal		起始值
	 * @param endVal		结束值
	 * @param selFields		组成结果的字段
	 * @param s				
	 * @param opt			读取文件参数
	 * @param ctx			上下文变量
	 * @return				返回结果cursor
	 */
	private static ICursor search(FileObject fo, Expression exp, Object startVal,
			 Object endVal, String []selFields, String s, String opt, Context ctx) {
		boolean isCsv = false;
		if (opt != null) {
			// 如果是二进制文件，生成二进制文件的cursor
			if (opt.indexOf('b') != -1) {
				BFileReader reader = new BFileReader(fo);
				return reader.iselect(exp, startVal, endVal, selFields, ctx);
			}
			
			if (opt.indexOf('c') != -1) isCsv = true;
		}

		String charset = fo.getCharset();
		byte[] separator;
		if (s != null && s.length() > 0) {
			try {
				separator = s.getBytes(charset);
			} catch (UnsupportedEncodingException e) {
				throw new RQException(e.getMessage(), e);
			}
		} else if (isCsv) {
			separator = new byte[]{(byte)','};
		} else {
			separator = FileObject.COL_SEPARATOR;
		}

		LineImporter importer = null;
		Object []line = null;

		try {
			importer = new LineImporter(fo.getInputStream(), charset, separator, opt, 1024);
			line = importer.readLine();
		} catch(IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (importer != null) {
					importer.close();
				}
			} catch (IOException ie) {
			}
		}

		if (line == null || line.length == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(exp.getIdentifierName() + mm.getMessage("ds.fieldNotExist"));
		}
		
		long start = 0;
		if (startVal != null) {
			start = iselect_t(fo, exp, startVal, separator, opt, true, ctx);
			if (start < 0) {
				start = -start;
			}
		}
		
		long end;
		if (endVal != null) {
			end = iselect_t(fo, exp, endVal, separator, opt, false, ctx);
			if (end < 0) {
				end = -end;
			}
		} else {
			end = fo.size();
		}
		
		FileCursor cursor = new FileCursor(fo, 0, 0, selFields, null, s, opt, ctx);
		cursor.setStart(start);
		cursor.setEnd(end);
		return cursor;
	}
		
	/**
	 * 从数据文件中，取得记录表达式的值，在values中的记录。
	 * 
	 * @param fo			数据文件对象
	 * @param exp			表达式
	 * @param values		参考值
	 * @param separator		若fo为文本文件，该变量为分隔符.
	 * @param opt			文件读写参数
	 * @param isMultiId		是否多ID
	 * 
	 * @return				用取得的记录组成的table
	 */
	private static Table iselect_t(FileObject fo, Expression exp, Sequence values, 
			byte[] separator, String opt, boolean isMultiId, Context ctx) {
		boolean isTitle = false;
		if (opt != null) {
			if (opt.indexOf('t') != -1) isTitle = true;
		}
		
		IFile file = fo.getFile();
		String charset = fo.getCharset();
		LineImporter importer = null;
		Object []line = null;
		long start = 0;
		
		Record rec = null;

		try {
			importer = new LineImporter(file.getInputStream(), charset, separator, opt, 1024);
			line = importer.readLine();
			if (line == null) {
				return null;
			}
			
			// 初始化记录变量
			String[] fields = new String[line.length];
			for (int i = 0; i < line.length; i++) {
				fields[i] = line[i].toString();
			}
			
			byte []colTypes = new byte[line.length];
			importer.setColTypes(colTypes, null);

			if (isTitle) {
				start = importer.getCurrentPosition() - 2;
			}
		} catch(IOException e) {
			try {
				if (importer != null) {
					importer.close();
					importer = null;
				}
			} catch (IOException ie) {
			}
			
			throw new RQException(e.getMessage(), e);
		}

		if (line == null || line.length == 0) {
			try {
				if (importer != null) {
					importer.close();
					importer = null;
				}
			} catch (IOException e) {
			}

			MessageManager mm = EngineMessage.get();
			throw new RQException(exp.getIdentifierName() + mm.getMessage("ds.fieldNotExist"));
		}

		int fcount = line.length;
		DataStruct ds;
		if (isTitle) {
			String[] items = new String[fcount];
			for (int f = 0; f < fcount; ++f) {
				items[f] = Variant.toString(line[f]);
			}

			ds = new DataStruct(items);
		} else {
			String[] items = new String[fcount];
			ds = new DataStruct(items);
		}
		
		rec = new Record(ds);
		int valCount = values.length();
		Table table = new Table(ds, valCount);
		long size = file.size();

		try {
			for (int i = 1; i <= valCount; ++i) {
				long low = start;
				long high = size;
				Object key = values.get(i);

				boolean isExist = false;
				while (low <= high) {
					long mid = (low + high) >> 1;
					long pos = importer.getCurrentPosition();
					if (pos > mid) {
						importer.close();
						LineImporter tmp = new LineImporter(file.getInputStream(), charset, separator, opt, 1024);
						tmp.copyProperty(importer);
						importer = tmp;
					}
					
					if (isMultiId && low>=high) break;
					importer.seek(mid);
					Object []objs = importer.readLine();
					if (objs == null) { // 到达结尾
						high = mid - 1;
					} else {
						rec.setStart(0, objs);
						int cmp = 0;
						try {
							cmp = Variant.compare(rec.calc(exp, ctx), key);
						} catch (RQException e) {
							low = mid + 1;
							start = low;
							continue;
						}
						if (isMultiId){
							if (cmp < 0) {
								low = mid + 1;
								start = low;
							} else if (cmp > 0) {
								high = mid;
							} else {
								high = mid;
								isExist = true;
							}
						} else {
							if (cmp < 0) {
								low = mid + 1;
								start = low;
							} else if (cmp > 0) {
								high = mid - 1;
							} else {
								if (objs.length <= fcount) {
									table.newLast(objs);
								} else {
									Record r = table.newLast();
									for (int f = 0; f < fcount; ++f) {
										r.setNormalFieldValue(f, objs[f]);
									}
								}
	
								pos = importer.getCurrentPosition();
								if (pos < size) {
									start = pos - 2;
								} else {
									start = size;
								}
	
								break;
							}
						}
					}/* else {
						low = mid + 1;
						start = low;
					}*/
				}
				if (isMultiId && isExist){
					start = iselect_i(importer, table,
							low, exp, rec, key, fcount, size, ctx);
				}

				if (start >= size) {
					break;
				}
			}
		} catch(IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (importer != null) importer.close();
			} catch (Exception e) {
			}
		}

		return table;
	}
	
	private static long iselect_i(LineImporter importer, Table table,long pos,
			Expression exp, Record rec, Object key, int fcount, long size, Context ctx)
					throws IOException {
		long lastpos;
		while (true)
		{
			lastpos = pos;
			if(pos+2 >= size) break;
			importer.seek(pos);			
			Object []objs = importer.readLine();
			pos = importer.getCurrentPosition()-2;
			if (objs == null) { // 到达结尾
				return importer.getCurrentPosition();
			}
			rec.setStart(0, objs);

			//if (field >= objs.length) return importer.getCurrentPosition();
			try {
				if (0 == Variant.compare(rec.calc(exp, ctx), key)){
					if (objs.length <= fcount) {
						table.newLast(objs);
					} else {
						Record r = table.newLast();
						for (int f = 0; f < fcount; ++f) {
							r.setNormalFieldValue(f, objs[f]);
						}
					}
					
				} else {
					break;
				}
			} catch (RQException e) {
				return importer.getCurrentPosition();
			}
		}
		return lastpos;
	}
		
	/**
	 * 在一个文本文件中，定位表达式exp的结果最接近value的记录的位置
	 * 
	 * @param fo		文本文件的文件对象
	 * @param exp		表达式
	 * @param value		参考值
	 * @param separator	文本文件的分隔符
	 * @param opt		文件读取参数
	 * @param isStart	true	小于value的记录
	 * 					false	大于value的记录
	 * 
	 * @return			返回对应结果的位置
	 * 
	 */
	private static long iselect_t(FileObject fo, Expression exp, Object value, 
			byte[] separator, String opt, boolean isStart, Context ctx) {
		boolean isTitle = false;
		if (opt != null) {
			if (opt.indexOf('t') != -1) isTitle = true;
		}

		IFile file = fo.getFile();
		String charset = fo.getCharset();
		LineImporter importer = new LineImporter(file.getInputStream(), charset, separator, opt, 1024);

		long low = 0;
		long high = file.size();

		try {
			Object []line = importer.readLine();
			if (line == null) {
				return -1;
			}
			
			byte []colTypes = new byte[line.length];
			importer.setColTypes(colTypes, null);
			
			if (isTitle) {
				low = importer.getCurrentPosition() - 2;
			}
			
			// 初始化记录变量
			String[] fields = new String[line.length];
			for (int i = 0; i < line.length; i++) {
				fields[i] = line[i].toString();
			}
			DataStruct ds = new DataStruct(fields);
			Record rec = new Record(ds);
			
			while (low <= high) {
				long mid = (low + high) >> 1;
				long pos = importer.getCurrentPosition();
				if (pos > mid) {
					importer.close();
					LineImporter tmp = new LineImporter(file.getInputStream(), charset, separator, opt, 1024);
					tmp.copyProperty(importer);
					importer = tmp;
				}
				
				importer.seek(mid);
				Object []objs = importer.readLine();
				if (objs == null) { // 到达结尾
					high = mid - 1;
				} else {
					rec.setStart(0, objs);
					
					int cmp = Variant.compare(rec.calc(exp, ctx), value);					
					if (cmp < 0) {
						low = mid + 1;
					} else if (cmp > 0) {
						high = mid - 1;
					} else {
						if (isStart) {
							return mid;
						} else {
							return importer.getCurrentPosition() - 2;
						}
					}
				}
			}
		} catch(IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (importer != null) importer.close();
			} catch (IOException e) {
			}
		}

		return -low;
	}
}
