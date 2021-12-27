package com.scudata.vdb;

import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;
import com.scudata.util.Variant;

/**
 * 过滤条件
 * @author RunQian
 *
 */
class Filter {
	private String []filterDirs; // 目录名数组
	private DirFilter []filters; // 目录过滤
	private String []selFields; // 选出字段
	private Expression exp;
	private Context ctx;
	//private Expression filter; // 不能识别的表达式
	
	private int filterCount = 0; // 条件总数
	private int filterIndex = 0; // 下一个路径对应的条件索引
	
	private int capacity = 16; // 目录数容量
	private int dirIndex = 0; // 下一个要加入的目录在数组中的位置
	private String []dirNames = new String[capacity]; // 路径名
	private Object []dirValues = new Object[capacity]; // 路径值
	
	private int selDirCount = 0;
	private String []resultFields; // 结果集数据结构，如果selFields为空则只包含选出的目录字段
	
	// retrive使用
	public Filter(String []dirNames, Object []dirValues, boolean []valueSigns, String []selFields, Expression exp, Context ctx) {
		this.filterDirs = dirNames;
		this.selFields = selFields;
		this.exp = exp;
		this.ctx = ctx;
		
		if (dirNames != null) {
			filterCount = dirNames.length;
			filters = new DirFilter[filterCount];
			for (int i = 0; i < filterCount; ++i) {
				filters[i] = new DirFilter(dirValues[i], valueSigns[i]);
				if (dirNames[i] != null) {
					selDirCount++;
				}
			}
			
			if (selFields != null) {
				resultFields = new String[selDirCount + selFields.length];
				for (int i = 0, j = 0; i < filterCount; ++i) {
					if (dirNames[i] != null) {
						resultFields[j++] = dirNames[i];
					}
				}
				
				System.arraycopy(selFields, 0, resultFields, selDirCount, selFields.length);
			} else {
				if (selDirCount == filterCount) {
					resultFields = dirNames;
				} else {
					resultFields = new String[selDirCount];
					for (int i = 0, j = 0; i < filterCount; ++i) {
						if (dirNames[i] != null) {
							resultFields[j++] = dirNames[i];
						}
					}
				}
			}
		} else {
			resultFields = selFields;
		}
	}
	
	// update使用
	public Filter(String []dirNames, Object []dirValues, boolean []valueSigns, Expression exp, Context ctx) {
		this.filterDirs = dirNames;
		this.exp = exp;
		this.ctx = ctx;
		
		if (dirNames != null) {
			filterCount = dirNames.length;
			filters = new DirFilter[filterCount];
			for (int i = 0; i < filterCount; ++i) {
				filters[i] = new DirFilter(dirValues[i], valueSigns[i]);
			}
		}
	}
	
	// 是否有过滤表达式
	public boolean hasExpression() {
		return exp != null;
	}
	
	private static boolean isEqualField(String filterName, String fname) {
		if (filterName == null) {
			return true;
		} else {
			return fname != null && fname.equals(filterName);
		}
	}
	
	// 如果不满足过滤条件则返回false，并不添加
	public boolean pushDir(String name, Object value) {
		if (filterIndex < filterCount && isEqualField(filterDirs[filterIndex], name)) {
			if (filters[filterIndex].match(value)) {
				filterIndex++;
			} else {
				return false;
			}
		}
		
		int dirIndex = this.dirIndex;
		if (dirIndex == capacity) {
			capacity *= 2;
			String []newNames = new String[capacity];
			Object []newValues = new Object[capacity];
			System.arraycopy(dirNames, 0, newNames, 0, dirIndex);
			System.arraycopy(dirValues, 0, newValues, 0, dirIndex);
			dirNames = newNames;
			dirValues = newValues;
		}
		
		dirNames[dirIndex] = name;
		dirValues[dirIndex] = value;
		this.dirIndex++;
		return true;
	}
	
	public void popDir() {
		dirIndex--;
		int f = filterIndex - 1;
		if (f >= 0 && isEqualField(filterDirs[f], dirNames[dirIndex])) {
			filterIndex = f;
		}
	}
	
	private static Table getFields(Sequence seq, DataStruct ds, String []feilds) {
		int len = seq.length();
		int newCount = feilds.length;
		int []index = new int[newCount];

		for (int i = 0; i < newCount; ++i) {
			index[i] = ds.getFieldIndex(feilds[i]);
		}

		Table table = new Table(feilds, len);
		for (int i = 1; i <= len; ++i) {
			Record nr = table.newLast();
			Record r = (Record)seq.getMem(i);
			for (int f = 0; f < newCount; ++f) {
				if (index[f] != -1) {
					nr.setNormalFieldValue(f, r.getFieldValue(index[f]));
				}
			}
		}
		
		return table;
	}
	
	// 判断路径值是否与条件都匹配了
	public boolean isDirMatch() {
		return filterIndex == filterCount;
	}
	
	public Sequence select(Object val) {
		if (filterIndex < filterCount) {
			return null;
		}
		
		if (!(val instanceof Sequence)) {
			return null;
		}
		
		Sequence seq = (Sequence)val;
		if (seq.length() == 0) {
			return null;
		}
		
		DataStruct ds = seq.dataStruct();
		if (ds == null) {
			return null;
		}
		
		if (exp == null && selDirCount == 0) {
			if (selFields == null) {
				return seq;
			} else {
				return getFields(seq, ds, selFields);
			}
		}
		
		String []fields = ds.getFieldNames();
		int dirCount = dirIndex;
		int fcount = fields.length;
		String []totalNames = new String[dirCount + fcount];
		System.arraycopy(dirNames, 0, totalNames, 0, dirCount);
		System.arraycopy(fields, 0, totalNames, dirCount, fcount);
		
		int len = seq.length();
		Table table = new Table(totalNames, len);
		Object []dirValues = this.dirValues;
		
		for (int i = 1; i <= len; ++i) {
			Record r = (Record)seq.getMem(i);
			Record nr = table.newLast();
			nr.setStart(0, dirValues, dirCount);
			nr.setStart(dirCount, r);
		}

		if (exp != null) {
			try {
				table.select(exp, "o", ctx);
				if (table.length() == 0) {
					return null;
				}
			} catch (Exception e) {
				return null;
			}
		}
		
		if (selFields == null) {
			if (resultFields == null) {
				return getFields(table, table.dataStruct(), fields);
			} else if (selDirCount == dirCount) {
				return table;
			} else {
				totalNames = new String[selDirCount + fcount];
				System.arraycopy(resultFields, 0, totalNames, 0, selDirCount);
				System.arraycopy(fields, 0, totalNames, selDirCount, fcount);
				return getFields(table, table.dataStruct(), totalNames);
			}
		} else {
			return getFields(table, table.dataStruct(), resultFields);
		}
	}
	
	private static void update(Sequence seq, Object []fvals, int []findex) {
		int mcount = findex.length;
		for (int i = 1, len = seq.length(); i <= len; ++i) {
			Record r = (Record)seq.getMem(i);
			for (int f = 0; f < mcount; ++f) {
				r.setNormalFieldValue(findex[f], fvals[f]);
			}
		}
	}
	
	public boolean update(Object val, Object []fvals, String []fields) {
		if (filterIndex < filterCount) {
			return false;
		}
		
		if (!(val instanceof Sequence)) {
			return false;
		}
		
		Sequence seq = (Sequence)val;
		if (seq.length() == 0) {
			return false;
		}
		
		DataStruct ds = seq.dataStruct();
		if (ds == null) {
			return false;
		}
		
		int mcount = fields.length;
		int []findex = new int[mcount];
		for (int i = 0; i < mcount; ++i) {
			findex[i] = ds.getFieldIndex(fields[i]);
			if (findex[i] == -1) {
				return false;
			}
		}
		
		Expression exp = this.exp;
		if (exp == null) {
			update(seq, fvals, findex);
			return true;
		}
		
		String []srcFields = ds.getFieldNames();
		int dirCount = dirIndex;
		int fcount = srcFields.length;
		String []totalNames = new String[dirCount + fcount];
		System.arraycopy(dirNames, 0, totalNames, 0, dirCount);
		System.arraycopy(srcFields, 0, totalNames, dirCount, fcount);
		
		DataStruct nds = new DataStruct(totalNames);
		Object []dirValues = this.dirValues;
		
		boolean match = false;
		Context ctx = this.ctx;
		ComputeStack stack = ctx.getComputeStack();
		Record nr = new Record(nds);
		stack.push(nr);
		
		try {
			for (int i = 1, len = seq.length(); i <= len; ++i) {
				Record r = (Record)seq.getMem(i);
				nr.setStart(0, dirValues, dirCount);
				nr.setStart(dirCount, r);
				
				if (Variant.isTrue(exp.calculate(ctx))) {
					match = true;
					for (int f = 0; f < mcount; ++f) {
						r.setNormalFieldValue(findex[f], fvals[f]);
					}
				}
			}
		} catch (Exception e) {
			return false;
		} finally {
			stack.pop();
		}
		
		return match;
	}
}