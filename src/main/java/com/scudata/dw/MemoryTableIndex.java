package com.scudata.dw;

import java.util.ArrayList;
import java.util.HashMap;

import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.HashIndexTable;
import com.scudata.dm.IndexTable;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.op.New;
import com.scudata.dm.op.Select;
import com.scudata.expression.Expression;
import com.scudata.expression.FieldId;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.Operator;
import com.scudata.expression.UnknownSymbol;
import com.scudata.expression.fn.string.Like;
import com.scudata.expression.mfn.sequence.Contain;
import com.scudata.expression.operator.And;
import com.scudata.expression.operator.DotOperator;
import com.scudata.expression.operator.Equals;
import com.scudata.expression.operator.Greater;
import com.scudata.expression.operator.NotGreater;
import com.scudata.expression.operator.NotSmaller;
import com.scudata.expression.operator.Or;
import com.scudata.expression.operator.Smaller;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 内表索引类
 * @author LW
 *
 */
public class MemoryTableIndex {
	private static final int NULL = -1;
	private static final int EQ = 0; // 等于
	private static final int GE = 1; // 大于等于
	private static final int GT = 2; // 大于
	private static final int LE = 3; // 小于等于
	private static final int LT = 4; // 小于
	
	private static final int LIMIT = 1000000;//高频词限制数，超过这个值就抛弃
	public static final int TEMP_FILE_SIZE = 100 * 1024 * 1024;//排序时的缓冲文件大小
	private static final int MIN_HASH_SIZE = 4096;
	public static final int TYPE_SORT = 0;
	public static final int TYPE_HASH = 1;
	public static final int TYPE_FULLTEXT = 2;
	private static final String SORT_FIELD_NAME = "SORT_FIELD_NAME";
	
	private Table srcTable;
	private String name;// 本索引的命名
	private String[] ifields;// 索引字段
	private int type;
	
	private int avgNums;//平均每个key的记录数
	
	private Table indexData;// 排序后的数据
	private IntArray[] recordNums;// 跟排序后的数据相对应的记录号 
	private IndexTable indexTable;//索引表（用于等值查找）
	
	public MemoryTableIndex(String name, Table srcTable, String[] fields, Expression filter, 
			int capacity, int type, Context ctx) {
		this.name = name;
		if (filter != null) {
			this.srcTable = ((Sequence)srcTable.select(filter, null, ctx)).derive(null);
		} else {
			this.srcTable = srcTable;
		}
		if (type == TYPE_SORT) {
			if (fields.length == 1) {
				createSortIndex(fields[0], ctx);
			} else {
				createSortIndex(fields, ctx);
			}
		} else if (type == TYPE_HASH) {
			createHashIndex(fields, capacity, ctx);
		} else {
			createFullTextIndex(fields[0], capacity, ctx);
		}
		if (avgNums == 0) avgNums = 8;
	}
	
	public String[] getIfields() {
		return ifields;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean getByName(String name) {
		if (this.name.equals(name)) {
			return true;
		}
		return false;
	}
	
	private class FieldFilter {
		private Object startVal;
		private Object endVal;
		private int startSign = NULL;
		private int endSign = NULL;
	}
	
	private void createHashIndex(String[] fields, int capacity, Context ctx) {
		type = TYPE_HASH;
		int len = srcTable.length();
		if (capacity == 0) {
			capacity = len / 2;
		}
		if (capacity < MIN_HASH_SIZE) {
			capacity = MIN_HASH_SIZE;
		}
		if (capacity > len) {
			capacity = len;
		}
		
		int flen = fields.length;
		DataStruct ds = srcTable.dataStruct();
		int[] findex = new int[flen];
		for (int i = 0; i < flen; i++) {
			findex[i] = ds.getFieldIndex(fields[i]);
		}
		
		if (flen == 1) {
			HashIndexTable it = new HashIndexTable(capacity, "m");
			it.create_i(srcTable, findex[0]);
			this.indexTable = it;
		} else {
			this.indexTable = srcTable.newIndexTable(findex, capacity, "mU");
		}
		this.ifields = fields;
		this.avgNums = avgNums / len;
	}
	
	private void createSortIndex(String field, Context ctx) {
		type = TYPE_SORT;
		Expression exp = new Expression(field);
		Expression[] exps = new Expression[] {exp, new Expression("#")};
		String[] names = new String[] {field, SORT_FIELD_NAME};
		Sequence table = srcTable.newTable(names, exps, null, ctx);
		table = table.sort(exp, null, "o", ctx);
		table = table.group(exp, "o", ctx);

		int len = table.length();
		names = new String[] {field};
		Table indexData = new Table(names, len);
		IntArray[] recordNums = new IntArray[len + 1]; 
		int avgNums = 0;
		for (int i = 1; i <= len; i++) {
			Sequence seq = (Sequence) table.getMem(i);
			BaseRecord rec = (BaseRecord) seq.getMem(1);
			int size = seq.length();
			IntArray recNum = new IntArray(size);
			for (int j = 1; j <= size; j++) {
				BaseRecord record = (BaseRecord) seq.getMem(j);
				Integer value = (Integer) record.getNormalFieldValue(1);
				recNum.pushInt(value);
			}
			Object[] objs = new Object[] {rec.getNormalFieldValue(0)};
			indexData.newLast(objs);
			recordNums[i] = recNum;
			avgNums += recNum.size();
		}
		indexData.dataStruct().setPrimary(names);
		indexData.createIndexTable(len, "b");
		this.indexData = indexData;
		this.indexTable = indexData.getIndexTable();
		this.recordNums = recordNums;
		this.ifields = names;
		this.avgNums = avgNums / len;
	}
	
	private void createSortIndex(String[] fields, Context ctx) {
		type = TYPE_SORT;
		int flen = fields.length;
		Expression[] exp = new Expression[flen];
		Expression[] exps = new Expression[flen + 1];
		String[] names = new String[flen + 1];
		String[] names2 = new String[flen];
		for (int i = 0; i < flen; i++) {
			exp[i] = exps[i] = new Expression(fields[i]);
			names[i] = names2[i] = fields[i];
		}
		exps[flen] = new Expression("#");
		names[flen] = SORT_FIELD_NAME;
		
		Sequence table = srcTable.newTable(names, exps, null, ctx);
		table = table.sort(exp, null, "o", ctx);
		table = table.group(exp, "o", ctx);
		
		int len = table.length();
		int avgNums = 0;
		Table indexData = new Table(names2, len);
		IntArray[] recordNums = new IntArray[len + 1]; 
		
		for (int i = 1; i <= len; i++) {
			Sequence seq = (Sequence) table.getMem(i);
			BaseRecord rec = (BaseRecord) seq.getMem(1);
			int size = seq.length();
			IntArray recNum = new IntArray(size);
			for (int j = 1; j <= size; j++) {
				BaseRecord record = (BaseRecord) seq.getMem(j);
				Integer value = (Integer) record.getNormalFieldValue(flen);
				recNum.pushInt(value);
			}
			Object[] objs = new Object[flen];
			for (int f = 0; f < flen; f++) {
				objs[f] = rec.getNormalFieldValue(f);
			}
			indexData.newLast(objs);
			recordNums[i] = recNum;
			avgNums += recNum.size();
		}
		indexData.dataStruct().setPrimary(names2);
		indexData.createIndexTable(len, "b");
		this.indexData = indexData;
		this.indexTable = indexData.getIndexTable();
		this.recordNums = recordNums;
		this.ifields = names2;
		this.avgNums = avgNums / len;
	}
	
	private void createFullTextIndex(String field, int capacity, Context ctx) {
		type = TYPE_FULLTEXT;
		Expression exp = new Expression(field);
		Expression[] exps = new Expression[] {exp, new Expression("#")};
		String[] names = new String[] {field, SORT_FIELD_NAME};
		Sequence table = srcTable.newTable(names, exps, null, ctx);
		table = fullTextSort(table, field);//table.sort(exp, null, "o", ctx);
		table = table.group(exp, "o", ctx);

		int len = table.length();
		names = new String[] {field};
		Table indexData = new Table(names, len);
		IntArray[] recordNums = new IntArray[len + 1]; 
		for (int i = 1; i <= len; i++) {
			Sequence seq = (Sequence) table.getMem(i);
			BaseRecord rec = (BaseRecord) seq.getMem(1);
			int size = seq.length();
			IntArray recNum = new IntArray(size);
			for (int j = 1; j <= size; j++) {
				BaseRecord record = (BaseRecord) seq.getMem(j);
				Integer value = (Integer) record.getNormalFieldValue(1);
				recNum.pushInt(value);
			}
			Object[] objs = new Object[] {rec.getNormalFieldValue(0)};
			indexData.newLast(objs);
			recordNums[i] = recNum;
		}
		table = null;
		indexData.dataStruct().setPrimary(names);
		indexData.createIndexTable(len, "b");
		this.indexData = indexData;
		this.indexTable = indexData.getIndexTable();
		this.recordNums = recordNums;
		this.ifields = names;
		this.avgNums = 8;
	}
	
	/**
	 * 检查字符key出现的次数
	 * @param strCounters
	 * @param key
	 * @return
	 */
	private boolean checkStringCount(HashMap<String, Long> strCounters, String key) {
		
		if (strCounters.containsKey(key)) {
			Long  cnt = strCounters.get(key) + 1;
			if (cnt >= LIMIT) {
				return true;
			}
			strCounters.put(key, cnt);
		} else {
			strCounters.put(key, (long) 1);
		}
		return false;
	}
	
	private boolean checkAlpha(char word) {
		if (word >= '0' && word <= 'z') {
			return true;
		}
		return false;
	}
	
	private Sequence fullTextSort(Sequence indexData, String field) {
		DataStruct ds = indexData.dataStruct();
		
		//check field
		int id = ds.getFieldIndex(field);
		if (id == -1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(field + mm.getMessage("ds.fieldNotExist"));
		}

		HashMap<String, Long> strCounters = new HashMap<String, Long>();//记录每个字符出现的次数
		int fieldsCount = ds.getFieldCount();
		ArrayList<String> list = new ArrayList<String>();
		Sequence table;
		Table subTable;		

		table = indexData;
		if (table.length() <= 0) return indexData;

		ds = table.dataStruct();
		subTable = new Table(ds);
		IArray mems = table.getMems();
		int length = table.length();
		for (int i = 1; i <= length; i++) {
			Record r = (Record) mems.get(i);
			Object []objs = r.getFieldValues();
			if (objs[0] == null) {
				continue;
			}
			if (!(objs[0] instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("index" + mm.getMessage("function.paramTypeError"));
			}
			String ifield = (String) objs[0];
			
			list.clear();//用于判断重复的字符，例如"宝宝巴士"，重复的"宝"字不能被重复索引
			int strLen = ifield.length();
			for (int j = 0; j < strLen; j++) {
				char ch1 = ifield.charAt(j);
				if (ch1 == ' ') {
					continue;//空格
				}
				
				if (checkAlpha(ch1)) {
					//英文要连续取3个、4个字母
					if (j + 2 < strLen) {
						char ch2 = ifield.charAt(j + 1);
						char ch3 = ifield.charAt(j + 2);
						if (checkAlpha(ch2) && checkAlpha(ch3)) {
							Object []vals = new Object[fieldsCount];
							for (int f = 1; f < fieldsCount; f++) {
								vals[f] = objs[f];
							}
							String str3 = new String("" + ch1 + ch2 + ch3);
							if (!list.contains(str3) && !checkStringCount(strCounters, str3)) {
								vals[0] = str3;
								subTable.newLast(vals);
								list.add(str3);
							}
							
							if (j + 3 < strLen) {
								char ch4 = ifield.charAt(j + 3);
								if (checkAlpha(ch4)) {
									String str4 =  new String(str3 + ch4);
									if (!list.contains(str4)) {
										vals = new Object[fieldsCount];
										for (int f = 1; f < fieldsCount; f++) {
											vals[f] = objs[f];
										}
										vals[0] = str4;
										subTable.newLast(vals);
										list.add(str4);
									}
								}
							}
						}
					}
				} else if (ch1 > 255) {
					String str = new String("" + ch1);
					if (list.contains(str)) {
						continue;//已经存在
					}				
					//处理计数器
					if (checkStringCount(strCounters, str)) {
						continue;
					}
					
					Object []vals = new Object[fieldsCount];
					for (int f = 1; f < fieldsCount; f++) {
						vals[f] = objs[f];
					}
					vals[0] = str;
					subTable.newLast(vals);
					list.add(str);
				}
				
			}
		}

		if (subTable != null && subTable.length() != 0) {
			subTable.sortFields(new int[] {0});
		}
		
		return subTable;
	}
	
	private boolean equalField(int fieldIndex, Node node) {
		if (node instanceof UnknownSymbol) {
			if (ifields[fieldIndex].equals(((UnknownSymbol)node).getName())) {
				return true;
			}
		} else if (node instanceof FieldId) {
			return ((FieldId)node).getFieldIndex() == fieldIndex;
		}
		
		return false;
	}
	
	private boolean getFieldFilters(Node home, FieldFilter []filters, Context ctx) {
		if (!(home instanceof Operator)) return false;
		
		Node left = home.getLeft();
		Node right = home.getRight();
		if (home instanceof And) {
			if (!getFieldFilters(left, filters, ctx)) return false;
			return getFieldFilters(right, filters, ctx);
		} else if (home instanceof Equals) { // ==
			for (int i = 0, icount = ifields.length; i < icount; ++i) {
				if (equalField(i, left)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else {
						return false;
					}
					
					filters[i].startSign = EQ;
					filters[i].startVal = right.calculate(ctx);
					return true;
				} else if (equalField(i, right)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else {
						return false;
					}

					filters[i].startSign = EQ;
					filters[i].startVal = left.calculate(ctx);
					return true;
				}
			}
		} else if (home instanceof NotSmaller) { // >=
			for (int i = 0, icount = ifields.length; i < icount; ++i) {
				if (equalField(i, left)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].startSign != NULL) {
						return false;
					}
					
					filters[i].startSign = GE;
					filters[i].startVal = right.calculate(ctx);
					return true;
				} else if (equalField(i, right)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].endSign != NULL) {
						return false;
					}

					filters[i].endSign = LE;
					filters[i].endVal = left.calculate(ctx);
					return true;
				}
			}
		} else if (home instanceof Greater) { // >
			for (int i = 0, icount = ifields.length; i < icount; ++i) {
				if (equalField(i, left)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].startSign != NULL) {
						return false;
					}
					
					filters[i].startSign = GT;
					filters[i].startVal = right.calculate(ctx);
					return true;
				} else if (equalField(i, right)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].endSign != NULL) {
						return false;
					}

					filters[i].endSign = LT;
					filters[i].endVal = left.calculate(ctx);
					return true;
				}
			}
		} else if (home instanceof NotGreater) { // <=
			for (int i = 0, icount = ifields.length; i < icount; ++i) {
				if (equalField(i, left)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].endSign != NULL) {
						return false;
					}
					
					filters[i].endSign = LE;
					filters[i].endVal = right.calculate(ctx);
					return true;
				} else if (equalField(i, right)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].startSign != NULL) {
						return false;
					}

					filters[i].startSign = GE;
					filters[i].startVal = left.calculate(ctx);
					return true;
				}
			}
		} else if (home instanceof Smaller) { // <
			for (int i = 0, icount = ifields.length; i < icount; ++i) {
				if (equalField(i, left)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].endSign != NULL) {
						return false;
					}
					
					filters[i].endSign = LT;
					filters[i].endVal = right.calculate(ctx);
					return true;
				} else if (equalField(i, right)) {
					if (filters[i] == null) {
						filters[i] = new FieldFilter();
					} else if (filters[i].startSign != NULL) {
						return false;
					}

					filters[i].startSign = GT;
					filters[i].startVal = left.calculate(ctx);
					return true;
				}
			}
		}

		return false;
	}
	
	private boolean getFieldFilters(Node home, ArrayList<Object> objs, Context ctx) {
		if (!(home instanceof Operator)) return false;
		
		Node left = home.getLeft();
		Node right = home.getRight();
		if (home instanceof Or) {
			if (!getFieldFilters(left, objs, ctx)) return false;
			return getFieldFilters(right, objs, ctx);
		} else if (home instanceof Equals) { // ==
			for (int i = 0, icount = ifields.length; i < icount; ++i) {
				if (equalField(i, left)) {
					objs.add(right.calculate(ctx));
					return true;
				} else if (equalField(i, right)) {
					objs.add(left.calculate(ctx));
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 找出索引字段的区间
	 * @param fieldIndex
	 * @param home
	 * @param filter
	 * @param ctx
	 */
	private void getFieldFilter(int fieldIndex, Node home, FieldFilter filter, Context ctx) {
		if (!(home instanceof Operator)) return;
		
		Node left = home.getLeft();
		Node right = home.getRight();
		if (home instanceof And) {
			getFieldFilter(fieldIndex, left, filter, ctx);
			getFieldFilter(fieldIndex, right, filter, ctx);
		} else if (home instanceof Equals) { // ==
			if (equalField(fieldIndex, left)) {
				filter.startSign = EQ;
				filter.startVal = right.calculate(ctx);
			} else if (equalField(fieldIndex, right)) {
				filter.startSign = EQ;
				filter.startVal = left.calculate(ctx);
			}
		} else if (home instanceof NotSmaller) { // >=
			if (equalField(fieldIndex, left)) {
				filter.startSign = GE;
				filter.startVal = right.calculate(ctx);
			} else if (equalField(fieldIndex, right)) {
				filter.endSign = LE;
				filter.endVal = left.calculate(ctx);
			}
		} else if (home instanceof Greater) { // >
			if (equalField(fieldIndex, left)) {
				filter.startSign = GT;
				filter.startVal = right.calculate(ctx);
			} else if (equalField(fieldIndex, right)) {
				filter.endSign = LT;
				filter.endVal = left.calculate(ctx);
			}
		} else if (home instanceof NotGreater) { // <=
			if (equalField(fieldIndex, left)) {
				filter.endSign = LE;
				filter.endVal = right.calculate(ctx);
			} else if (equalField(fieldIndex, right)) {
				filter.startSign = GE;
				filter.startVal = left.calculate(ctx);
			}
		} else if (home instanceof Smaller) { // <
			if (equalField(fieldIndex, left)) {
				filter.endSign = LT;
				filter.endVal = right.calculate(ctx);
			} else if (equalField(fieldIndex, right)) {
				filter.startSign = GT;
				filter.startVal = left.calculate(ctx);
			}
		} // 忽略or和其它运算符
	}
	
	private int binarySearch(Object key) {
		IArray mems = indexData.getMems();
		int len = mems.size();
		
		int low = 1;
		int high = len;
		while (low <= high) {
			int mid = (low + high) >> 1;
			BaseRecord r = (BaseRecord)mems.get(mid);
			Object obj = r.getNormalFieldValue(0);
			int cmp = Variant.compare(obj, key, true);

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		
		if (low < len + 1) {
			return low;
		} else {
			return -1;
		}
	}
	
	private int binarySearchArray(Object []keys, boolean isStart) {
		IArray mems = indexData.getMems();
		int len = mems.size();
		
		int keyCount = keys.length;
		Object[] vals = new Object[keyCount];
		int low = 1;
		int high = len;
		while (low <= high) {
			int mid = (low + high) >> 1;
			BaseRecord r = (BaseRecord)mems.get(mid);
			for (int f = 0; f < keyCount; ++f) {
				vals[f] = r.getNormalFieldValue(f);
			}
			int cmp = Variant.compareArrays(vals, keys, keyCount);

			if (cmp < 0) {
				low = mid + 1;
			} else if (cmp > 0) {
				high = mid - 1;
			} else {
				// 只对部分索引字段提条件时可能有重复的
				if (isStart) { // 找起始位置
					for (int i = mid - 1; i >= 0; --i) {
						r = (BaseRecord)mems.get(i);
						for (int f = 0; f < keyCount; ++f) {
							vals[f] = r.getNormalFieldValue(f);
						}
						if (Variant.compareArrays(vals, keys, keyCount) == 0) {
							mid = i;
						} else {
							break;
						}
					}
				} else { // 找结束位置
					for (int i = mid + 1; i <= high; ++i) {
						r = (BaseRecord)mems.get(i);
						for (int f = 0; f < keyCount; ++f) {
							vals[f] = r.getNormalFieldValue(f);
						}
						if (Variant.compareArrays(vals, keys, keyCount) == 0) {
							mid = i;
						} else {
							break;
						}
					}
					
					if (mid < len) mid++;
				}
				
				return mid; // key found
			}
		}
		
		if (low < len + 1) {
			return low;
		} else {
			return -1;
		}
	}

	//根据值查找块号和位置，两个索引区都查找
	//key[] 要查找的值
	//icount 字段个数
	//isStart 是否是找开始
	//index[] 输出找到的记录号
	private int searchValue(Object[] key, int icount, boolean isStart) {
		int i;
		
		int index = -1;
		
		while (true) {
			if (icount == 1) {
				i = binarySearch(key[0]);
				if (i < 0) {
					break;
				}
			} else {
				i = binarySearchArray(key, isStart);
				if (i < 0) {
					break;
				}
			}
			index = i;
			break;
		}
		return index;
	}
	
	/**
	 * 按表达式exp查询
	 */
	public ICursor select(Expression exp, String []fields, String opt, Context ctx) {
		IntArray recNums = null;
		ICursor cs;
		if (type == TYPE_FULLTEXT) {
			cs = select_fulltext(exp, fields, opt, ctx);
		} else {
			if (type == TYPE_SORT)
				recNums = select_sort(exp, opt, ctx);
			else if (type == TYPE_HASH)
				recNums = select_hash(exp, opt, ctx);
			
			if (recNums == null || recNums.size() == 0) {
				return new MemoryCursor(null);
			}
			
			Table srcTable = this.srcTable;
			Table result = new Table(srcTable.dataStruct());
			if (type == TYPE_HASH) {
				for (int i = recNums.size(); i > 0; i--) {
					BaseRecord rec = srcTable.getRecord(recNums.getInt(i));
					result.add(rec);
				}
			} else {
				for (int i = 1, len = recNums.size(); i <= len; i++) {
					BaseRecord rec = srcTable.getRecord(recNums.getInt(i));
					result.add(rec);
				}
			}
			
			cs = new MemoryCursor(result);
		}
		
		if (fields != null) {
			int len = fields.length;
			Expression[] exps = new Expression[len];
			for (int i = 0; i < len; i++) {
				exps[i] = new Expression(fields[i]);
			}
			New op = new New(exps, fields, null);
			cs.addOperation(op, ctx);
		}
		return cs;
	}
	
	/**
	 * 按表达式查询
	 * @param exp
	 * @param opt
	 * @param ctx
	 * @return	记录号
	 */
	private IntArray select_sort(Expression exp, String opt, Context ctx) {
		int icount = ifields.length;
		if (icount == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + exp.toString());
		}
		
		//处理contain表达式
		Node home = exp.getHome();
		if (home instanceof DotOperator) {
			Node left = home.getLeft();
			Node right = home.getRight();
			if (!(right instanceof Contain)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
			Sequence series;
			Object obj = left.calculate(ctx);
			if (obj instanceof Sequence) {
				series = (Sequence) obj;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
			
			String str = ((Contain)right).getParamString();
			str = str.replaceAll("\\[", "");
			str = str.replaceAll("\\]", "");
			str = str.replaceAll(" ", "");
			String[] split = str.split(",");
			if (icount != split.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.paramCountNotMatch"));
			}
			if (0 != Variant.compareArrays(ifields, split)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
			series.sort("o");
			return select(series, opt, ctx);
		}
		
		//处理like(F,"xxx*")表达式
		if (home instanceof Like) {
			if (((Like) home).getParam().getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
			IParam sub1 = ((Like) home).getParam().getSub(0);
			IParam sub2 = ((Like) home).getParam().getSub(1);
			String f = (String) sub1.getLeafExpression().getIdentifierName();
			if (!f.equals(ifields[0])) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
			String fmtExp = (String) sub2.getLeafExpression().calculate(ctx);
			int idx = fmtExp.indexOf("*");
			if (idx > 0) {
				fmtExp = fmtExp.substring(0, idx);
				return select(new String[]{fmtExp}, exp, opt, ctx);
			}
		}
				
		FieldFilter []filters = new FieldFilter[icount];
		if (getFieldFilters(exp.getHome(), filters, ctx)) {
			int last = icount - 1;
			for (; last >= 0; --last) {
				if (filters[last] != null) break;
			}
			
			// 如果左面的都是相等比较则可以优化成[a,b...v1]:[a,b...v2]
			boolean canOpt = true;
			for (int i = 0; i < last; ++i) {
				if (filters[i] == null || filters[i].startSign != EQ) {
					canOpt = false;
					break;
				}
			}
			
			if (canOpt) {
				if (filters[last].startSign == EQ) {
					Object []vals = new Object[last + 1];
					for (int i = 0; i <= last; ++i) {
						vals[i] = filters[i].startVal;
					}
					
					if (icount == last + 1) {
						//如果是所有字段的等于
						Sequence seq = new Sequence();
						seq.addAll(vals);
						if (icount == 1) {
							return select(seq, opt, ctx);
						}
						Sequence series = new Sequence();
						series.add(seq);
						return select(series, opt, ctx);
					}
					return select(vals, opt, ctx);
				} else if (filters[last].startSign != NULL && filters[last].endSign != NULL) {
					Object []startVals = new Object[last + 1];
					Object []endVals = new Object[last + 1];
					for (int i = 0; i <= last; ++i) {
						startVals[i] = filters[i].startVal;
						endVals[i] = filters[i].startVal;
					}
					
					endVals[last] = filters[last].endVal;
					if (opt == null) opt = "";
					if (filters[last].startSign == GT) opt += "l";
					if (filters[last].endSign == LT) opt += "r";
					
					return select(startVals, endVals, opt, ctx);
				}
			}
		}

		Sequence vals = new Sequence(icount); // 前面做相等判断的字段的值
		FieldFilter ff = null; // 第一个做非相等判断的字段的信息
		
		for (int i = 0; i < icount; ++i) {
			FieldFilter filter = new FieldFilter();
			getFieldFilter(i, exp.getHome(), filter, ctx);
			if (filter.startSign == EQ) {
				vals.add(filter.startVal);
			} else {
				ff = filter;
				break;
			}
		}
		
		int start;
		int end;
		
		start = 1;
		end = indexData.length();
		int eqCount = vals.length();
		
		if (eqCount == 0) {
			if (ff != null && ff.startSign != NULL) {
				Object []keys = new Object[]{ff.startVal};
				start = searchValue(keys, icount, true);
				if (start < 0) return null;
			}
			
			if (ff != null && ff.endSign != NULL) {
				Object []keys = new Object[]{ff.endVal};
				end = searchValue(keys, icount, false);
				if (end < 0) end = indexData.length();
			}
		} else {
			if (ff == null || ff.startSign == NULL) {
				Object []keys = vals.toArray();
				start = searchValue(keys, icount, true);
			} else {
				Object []keys = new Object[eqCount + 1];
				vals.toArray(keys);
				keys[eqCount] = ff.startVal;
				start = searchValue(keys, eqCount + 1, true);
			}
			
			if (start < 0) return null;
			
			if (ff == null || ff.endSign == NULL) {
				if (icount == 1) {
					end = start;
				} else {
					Object []keys = vals.toArray();
					end = searchValue(keys, icount, false);
				}
			} else {
				Object []keys = new Object[eqCount + 1];
				vals.toArray(keys);
				keys[eqCount] = ff.endVal;
				end = searchValue(keys, icount, false);
			}

			if (end < 0) end = indexData.length();//(int) (this.internalBlockCount - 1);
		}
		
		IntArray recNum = null;
		if (start >= 0) {
			recNum = select(start, end, exp, ctx);
		}
		return recNum;
	}
	
	/**
	 * 按表达式查询
	 */
	private IntArray select_hash(Expression exp, String opt, Context ctx) {
		int icount = ifields.length;
		if (icount == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + exp.toString());
		}

		Node home = exp.getHome();
		if (home instanceof DotOperator) {
			Node left = home.getLeft();
			Node right = home.getRight();
			if (!(right instanceof Contain)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
			Sequence series;
			Object obj = left.calculate(ctx);
			if (obj instanceof Sequence) {
				series = (Sequence) obj;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}

			String str = ((Contain) right).getParamString();
			str = str.replaceAll("\\[", "");
			str = str.replaceAll("\\]", "");
			str = str.replaceAll(" ", "");
			String[] split = str.split(",");
			if (icount != split.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.paramCountNotMatch"));
			}
			if (0 != Variant.compareArrays(ifields, split)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
			// series.sort("o");
			return select_hash(series, opt, ctx);
		}

		ArrayList<Object> objs = new ArrayList<Object>();
		if (getFieldFilters(exp.getHome(), objs, ctx)) {
			Object[] vals;
			int size = objs.size();
			if (size == 0) {
				return null;
			} else {
				vals = new Object[size];
				objs.toArray(vals);
			}

			return select_hash(vals, opt, ctx);
		}
		MessageManager mm = EngineMessage.get();
		throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
	}
	
 	/**
 	 * 按值区间查询（多字段）
 	 * @param startVals
 	 * @param endVals
 	 * @param opt
 	 * @param ctx
 	 * @return
 	 */
	private IntArray select(Object []startVals, Object []endVals, String opt, Context ctx) {
		boolean le = opt == null || opt.indexOf('l') == -1;
		boolean re = opt == null || opt.indexOf('r') == -1;
				
		int icount = ifields.length;
		IntArray srcPos = null;
		if (startVals == null) {
			throw new RQException("icursor: never run to here!");
		} else if (endVals == null) {
			throw new RQException("icursor: never run to here!");
		} else {
			if (startVals.length > ifields.length || endVals.length > ifields.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));			
			}

			if (startVals.length != endVals.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("psort" + mm.getMessage("function.paramCountNotMatch"));			
			}
			
			int cmp = Variant.compareArrays(startVals, endVals);
			if (cmp > 0) {
				return new IntArray();
			} else if (cmp == 0 && (!le || !re)) {
				return new IntArray();
			}

			int start;
			int end;
			
			start = searchValue(startVals, icount, true);
			if (start < 0) return new IntArray();
			end = searchValue(endVals, icount, false);
			
			if (start >= 0) {
				if (end < 0) {
					srcPos = readPos(startVals, start, le ? GE : GT);
				} else {
					srcPos = readPos(startVals, start, le, endVals, end, re);
				}
			}
		}

		return srcPos;
	}
	
	/**
	 * 一次查询多个值
	 * @param vals
	 * @param opt
	 * @param ctx
	 * @return
	 */
	private IntArray select(Sequence vals, String opt, Context ctx) {
		if (vals == null || vals.length() == 0) return null;
		
		IArray mems = vals.getMems();
		int len = vals.length();
		IntArray recNum = new IntArray(len * avgNums);
		IntArray[] recordNums = this.recordNums;
		
		for (int i = 1; i <= len; i++) {
			Object srcVal = mems.get(i);
			int pos = indexTable.findPos(srcVal);
			if (pos != 0) {
				recNum.addAll(recordNums[pos]);
			}
		}
	
		return recNum;
	}
	
	private IntArray select_hash(Sequence vals, String opt, Context ctx) {
		if (vals == null || vals.length() == 0) return null;
		
		IArray mems = vals.getMems();
		int len = vals.length();
		IntArray recNum = new IntArray(len * avgNums);
		
		for (int i = len; i > 0; i--) {
			Object srcVal = mems.get(i);
			indexTable.findPos(srcVal, recNum);
		}
	
		return recNum;
	}
	
	private IntArray select(Object key, boolean isFirst, Context ctx) {
		if (key == null) return null;
		IntArray recNum = new IntArray(avgNums);
		
		int pos;
		if (key instanceof Object[]) {
			pos = indexTable.findPos((Object[])key);
		} else {
			pos = indexTable.findPos(key);
		}
		if (pos > 0) {
			IntArray[] recordNums = this.recordNums;
			if (isFirst) {
				recNum.add(recordNums[pos].get(1));
			} else {
				recNum.addAll(recordNums[pos]);
			}
			return recNum;
		} else {
			return null;
		}
	}
	
	private IntArray select_hash(Object key, boolean isFirst, Context ctx) {
		if (key == null) return null;
		IntArray recNum = new IntArray(avgNums);
		
		indexTable.findPos(key, recNum);
		int size = recNum.size();
		if (size == 0) return null;
		if (isFirst && size != 1) {
			recNum.setSize(1);
			return recNum;
		} else {
			return recNum;
		}
	}
	
	private IntArray select(Object[] vals, String opt, Context ctx) {
		if (vals == null || vals.length == 0) return null;

		int len = vals.length;
		IntArray recNum = new IntArray(len * avgNums);
		IntArray[] recordNums = this.recordNums;
		for (int i = 0; i < len; i++) {
			int pos = indexTable.findPos(vals[i]);
			if (pos != 0) {
				recNum.addAll(recordNums[pos]);
			}
		}
		
		return recNum;
	}
	
	private IntArray select_hash(Object[] vals, String opt, Context ctx) {
		if (vals == null || vals.length == 0) return null;

		int len = vals.length;
		IntArray recNum = new IntArray(len * avgNums);
		for (int i = len - 1; i >= 0; i--) {
			indexTable.findPos(vals[i], recNum);
		}
		
		return recNum;
	}
	
	/**
	 * 
	 * @param start 不包含
	 * @param end 不包含
	 * @return
	 */
	private IntArray select(int start, int end, Expression exp, Context ctx) {
		if (start > end) return null;

		int len = end - start + 1;
		IntArray resultNum = new IntArray(len * avgNums);
		IntArray[] recordNums = this.recordNums;
		for (int i = start; i <= end; i++) {
			IntArray recNum = recordNums[i];
			int size = recNum.size();
			for (int j = 1; j <= size; j++) {
				int seq = recNum.getInt(j);
				BaseRecord rec = srcTable.getRecord(seq);
				Object b = rec.calc(exp, ctx);
				if (Variant.isTrue(b)) {
					resultNum.addInt(seq);
				}
			}
		}
		

		
		return resultNum;
	}
	
	/**
	 * 查找以key[0]开头的
	 * @param key	key[0]是String
	 * @param exp	like表达式
	 * @param ctx
	 * @return	地址(伪号)数组
	 */
	private IntArray select(String []key, Expression exp, String opt, Context ctx) {
		int start = 0;
		IntArray srcPos = null;
		
		start = searchValue(key, 1, true);
		if (start < 0) return new IntArray();
		if (start >= 0) {
			readPos_like(start, exp, ctx);
		}
		return srcPos;
	}
	
	private IntArray readPos_like(int start, Expression exp, Context ctx) {
		IntArray recNum = recordNums[start];
		Table srcTable = this.srcTable;
		IntArray resultNum = new IntArray();
		DataStruct ds = new DataStruct(ifields);
		Record r = new Record(ds);
		ComputeStack stack = ctx.getComputeStack();
		stack.push(r);
		
		try {
			for (int i = 1, len = recNum.size(); i <= len; i++) {
				int seq = recNum.getInt(i);
				BaseRecord rec = srcTable.getRecord(seq);
				Object cur = rec.getNormalFieldValue(0);
				r.setNormalFieldValue(0, cur);
				Object b = exp.calculate(ctx);
				if (Variant.isTrue(b)) {
					resultNum.addInt(seq);
				}
			}
		} finally {
			stack.pop();
		}
		return resultNum;
	}
	
	//判断是否与indexData的指定值相等
	private boolean isEqualToIndexData(int seq, Object[] vals) {
		BaseRecord rec = indexData.getRecord(seq);
		Object[] cur = rec.getFieldValues();
		int keyCount = vals.length;
		int cmp = Variant.compareArrays(vals, cur, keyCount);
		return cmp == 0;
	}
	
	private int compareToIndexData(int seq, Object[] vals) {
		BaseRecord rec = indexData.getRecord(seq);
		Object[] cur = rec.getFieldValues();
		int keyCount = vals.length;
		int cmp = Variant.compareArrays(cur, vals, keyCount);
		return cmp;
	}
	
	private IntArray readPos(Object[] startVals, int start, int type) {
		IntArray[] recordNums = this.recordNums;
		int end = recordNums.length;
		int len = end - start + 1;
		IntArray recNum;
		switch (type) {
		case EQ:
			if (isEqualToIndexData(start, startVals))
				return recordNums[start];
			else 
				return null;
		case GE:
			recNum = new IntArray(len * avgNums);
			if (compareToIndexData(start, startVals) >= 0)
				recNum.addAll(recordNums[start]);
			start++;
			for (int i = start; i <= end; i++) {
				recNum.addAll(recordNums[i]);
			}
			return recNum;
		case GT:
			recNum = new IntArray(len * avgNums);
			if (compareToIndexData(start, startVals) > 0)
				recNum.addAll(recordNums[start]);
			start++;
			for (int i = start; i <= end; i++) {
				recNum.addAll(recordNums[i]);
			}
			return recNum;
		case LE:
			recNum = new IntArray(start * avgNums);
			for (int i = 1; i < start; i++) {
				recNum.addAll(recordNums[i]);
			}
			if (compareToIndexData(start, startVals) <= 0)
				recNum.addAll(recordNums[start]);
			return recNum;
		case LT:
			recNum = new IntArray(start * avgNums);
			for (int i = 1; i < start; i++) {
				recNum.addAll(recordNums[i]);
			}
			if (compareToIndexData(start, startVals) < 0)
				recNum.addAll(recordNums[start]);
			return recNum;
		}
		throw new RuntimeException();
	}
	
	private IntArray readPos(Object[] startVals, int start, boolean le, Object[] endVals, int end, boolean re) {
		IntArray[] recordNums = this.recordNums;
		int len = end - start + 1;
		IntArray recNum = new IntArray(len * avgNums);
		
		int i = start;
		if (le) {
			if (compareToIndexData(i, startVals) >= 0)
				recNum.addAll(recordNums[i]);
		} else {
			if (compareToIndexData(i, startVals) > 0)
				recNum.addAll(recordNums[i]);
		}
		i++;
		for (; i < end; i++) {
			recNum.addAll(recordNums[i]);
		}
		
		if (re) {
			if (compareToIndexData(i, endVals) <= 0)
				recNum.addAll(recordNums[i]);
		} else {
			if (compareToIndexData(i, endVals) < 0)
				recNum.addAll(recordNums[i]);
		}
		return recNum;
	}
	
	private ICursor toCursor(Sequence srcTable, Expression exp, Context ctx) {
		return (ICursor) new MemoryCursor(srcTable).select(null, exp, null, ctx);
	}
	private ICursor select_fulltext(Expression exp, String []fields, String opt, Context ctx) {
		int icount = ifields.length;
		if (icount == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + exp.toString());
		}

		Node home = exp.getHome();
		//处理like(F,"*xxx*")表达式
		while (home instanceof Like) {
			if (((Like) home).getParam().getSubSize() != 2) {
				break;
			}
			IParam sub1 = ((Like) home).getParam().getSub(0);
			IParam sub2 = ((Like) home).getParam().getSub(1);
			String f = (String) sub1.getLeafExpression().getIdentifierName();
			if (!f.equals(ifields[0])) {
				break;
			}
			
			//必须是like("*关键字*")格式的。否则按照普通的处理
			String fmtExp = (String) sub2.getLeafExpression().calculate(ctx);
			int idx = fmtExp.indexOf("*");
			if (idx != 0) {
				//return srcTable.cursor(fields, exp, ctx);
				return toCursor(srcTable, exp, ctx);
			}
			
			fmtExp = fmtExp.substring(1);
			idx = fmtExp.indexOf("*");
			if (idx != fmtExp.length() - 1) {
				//return srcTable.cursor(fields, exp, ctx);
				return toCursor(srcTable, exp, ctx);
			}
			
			fmtExp = fmtExp.substring(0, fmtExp.length() - 1);
			idx = fmtExp.indexOf("*");
			if (idx >= 0) {
				//return srcTable.cursor(fields, exp, ctx);
				return toCursor(srcTable, exp, ctx);
			}
			
			String regex = "[a-zA-Z0-9]+";
			if (fmtExp.matches(regex) && fmtExp.length() < 3) {
				//return srcTable.cursor(fields, exp, ctx);
				return toCursor(srcTable, exp, ctx);
			}
			IntArray recNums = select_fulltext(exp, opt, ctx);
			
			if (recNums != null && recNums.size() > 0) {
				Table srcTable = this.srcTable;
				Table result = new Table(srcTable.dataStruct());
				for (int i = 1, len = recNums.size(); i <= len; i++) {
					BaseRecord rec = srcTable.getRecord(recNums.getInt(i));
					result.add(rec);
				}
				ICursor cs = new MemoryCursor(result);
				Select select = new Select(exp, null);
				cs.addOperation(select, ctx);
				return cs;
			} else {
				return null;
			}
		}
		return toCursor(srcTable, exp, ctx);//return srcTable.cursor(fields, exp, ctx);
	}
	
	private static IntArray intArrayUnite(IntArray a, IntArray b) {
		int lenB = b.size();
		
		if (a == null) {
			IntArray c = new IntArray(lenB);
			c.addAll(b);
			return c;
		}
		
		int lenA = a.size();
		if (lenB == 0) {
			return a;
		}

		IntArray c = new IntArray(Math.min(lenA, lenB));
		int i = 1, j = 1;
		while (i <= lenA && j <= lenB) {
			int longA = a.getInt(i);
			int longB = a.getInt(j);
			if (longA < longB) {
				i++;
			} else if (longB < longA) {
				j++;
			} else {
				c.add(a.getInt(i));
				i++;
				j++;	
			}
		}
		return c;
	}
	
	private IntArray select_fulltext(Expression exp, String opt, Context ctx) {
		String f = ifields[0];
		IParam sub2 = ((Like) exp.getHome()).getParam().getSub(1);
		String fmtExp = (String) sub2.getLeafExpression().calculate(ctx);
		fmtExp = fmtExp.substring(1, fmtExp.length() - 1);
		
//		boolean isRow = srcTable instanceof RowPhyTable;
//		long recCountOfSegment[] = null;
//		if (!isRow) {
//			recCountOfSegment = ((ColPhyTable)srcTable).getSegmentInfo();
//		}
		
		//对每个关键字符进行过滤，求交集
		String regex = "[a-zA-Z0-9]+";
		String search = "";
		IntArray tempPos = null;
		int strLen = fmtExp.length();
		int j;
		int p = 0;//表示处理到的位置
		for (j = 0; j < strLen; ) {
			String str = fmtExp.substring(j, j + 1);
			p = j + 1;
			if (str.matches(regex)) {
				//英文
				//尝试连续取4个字母
				if (j + 3 < strLen) {
					String str4 = fmtExp.substring(j, j + 4);
					if (str4.matches(regex)) {
						str = str4;
						p = j + 4;
					}
				} else if (j + 2 < strLen) {//尝试连续取3个字母
					String str3 = fmtExp.substring(j, j + 3);
					if (str3.matches(regex)) {
						str = str3;
						p = j + 3;
					}
				}
			}
			j++;
			if (search.indexOf(str) >= 0) {
				continue;//重复的不再查询
			}
			search = fmtExp.substring(0, p);
			Expression tempExp = new Expression(f + "==\"" + str + "\"");
			IntArray srcPos =  select_sort(tempExp, opt, null);
			if (srcPos == null || srcPos.size() == 0) {
				tempPos = null;
				break;
			}
			
			//排序，归并求交集
			tempPos = intArrayUnite(tempPos, srcPos);
		}
		return tempPos;
	}
	
	/**
	 * 根据KEY值查询记录号
	 * @param key key值
	 * @param opt 选项
	 * @param ctx
	 * @return 记录(号)序列
	 */
	public Object ifind(Object key, String opt, Context ctx) {
		IntArray recNums = null;
		boolean hasOpt1 = false;
		boolean hasOptP = false;
		if (opt != null) {
			hasOpt1 = opt.indexOf('1') != -1;
			hasOptP = opt.indexOf('p') != -1;
		}
		
		if (type == TYPE_FULLTEXT) {
			String option = "p";
			if (hasOpt1) option += "z";
			return (Sequence) srcTable.pos(key, option);
		} else {
			if (type == TYPE_SORT)
				recNums = select(key, hasOpt1, ctx);
			else if (type == TYPE_HASH)
				recNums = select_hash(key, hasOpt1, ctx);
		}
		
		if (recNums == null || recNums.size() == 0) return null;
		
		if (hasOptP) {
			//返回序号
			if (hasOpt1)
				return recNums.getInt(1);
			else
				return new Sequence(recNums);
		} else {
			//返回记录
			if (hasOpt1) {
				return srcTable.getRecord(recNums.getInt(1));
			}
			Table srcTable = this.srcTable;
			Table result = new Table(srcTable.dataStruct());
			for (int i = 1, len = recNums.size(); i <= len; i++) {
				BaseRecord rec = srcTable.getRecord(recNums.getInt(i));
				result.add(rec);
			}
			
			return result;
		}
	}
	
	public int getType() {
		return type;
	}
	
	public IndexTable getIndexTable() {
		return indexTable;
	}
}
