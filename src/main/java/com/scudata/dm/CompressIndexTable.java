package com.scudata.dm;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.dw.compress.Column;
import com.scudata.dw.compress.ColumnList;
import com.scudata.dw.compress.IntColumn;
import com.scudata.util.HashUtil;
import com.scudata.util.Variant;

public class CompressIndexTable extends IndexTable {
	private ColumnList mems;
	private Column []columns;
	private int findex[];
	private int fcount;
	//private Object keys[];
	private int size;
	private boolean isInteger;
	private boolean isOrder;//findex里的顺序就是原序
	
	private int []hashCol;
	protected HashUtil hashUtil;
	
	public CompressIndexTable(ColumnList mems, int findex[]) {
		this.mems = mems;
		this.columns = mems.getColumns();
		this.findex = findex;
		//keys = new Object[1];
		size = mems.size();

		//判断是否findex里的顺序是否就是自然顺序
		isOrder = true;
		fcount = findex.length;
		for (int f = 0; f < fcount; ++f) {
			if (f != findex[f]) {
				isOrder = false;
				break;
			}
		}
		
		//判断是否都是int列
		isInteger = true;
		for (int f = 0; f < fcount; ++f) {
			Column col = columns[findex[f]];
			if (!(col instanceof IntColumn)) {
				isInteger = false;
				break;
			}
		}
		
		if (findex.length == 1) {
			if (isInteger) {
				hashUtil = new HashUtil(size);
				IntColumn col = (IntColumn) columns[findex[0]];
				hashCol = col.makeHashCode(hashUtil);
			} else {
				hashUtil = new HashUtil(size);
				hashCol = new int[hashUtil.getCapacity()];
				int idx = findex[0];
				Column col = columns[idx];
				for (int i = 1; i <= size; ++i) {
					int hash = hashUtil.hashCode(col.getData(i));
					hashCol[hash] = i;
				}
			}
		}
		
	}
	
	public Object find(Object key) {
		int idx = pfindByField(key);
		if (idx == 0) {
			return null;
		} else {
			return mems.get(idx);
		}
	}

	public Object find(Object[] keys) {
		int idx = pfindByFields(keys);
		if (idx == 0) {
			return null;
		} else {
			return mems.get(idx);
		}
	}


	public int pfindByField(Object val) {
		Column column;
		int low = 1, high = this.size;

		if (hashCol != null) {
			int hash = hashUtil.hashCode(val);
			int idx = hashCol[hash];
			if (idx > 0) {
				Object obj = this.columns[0].getData(idx);
				if (Variant.compare(obj, val, true) == 0) {
					return idx;
				}
			}
		}
		
		if (isOrder && isInteger) {
			IntColumn intCol = (IntColumn) this.columns[0];
			if (val == null) return 0;
			int right = ((Integer)val);

			//这是对连续自然数时的优化
//			int target = right - firstVal + 1;
//			if(target > 0 && target <= high) {
//				int left = intCol.getValue(target);
//				if (right == left) {
//					return target;
//				}
//			}
		
			
			NEXT:
			while (low <= high) {
				int mid = (low + high) >> 1;

//				if (right == null) {
//					high = mid - 1;
//					continue NEXT;
//				} 
//				else 
				{
					int left = intCol.getValue(mid);
					if (left < right) {
						low = mid + 1;
						continue NEXT;
					} else if (left > right) {
						high = mid - 1;
						continue NEXT;
					}
				}
		
				return mid;
			}
			return 0;			
		}
		
		int []findex = this.findex;
		if (isInteger) {
			column = this.columns[findex[0]];
			Integer right = ((Integer)val);
			
			NEXT:
			while (low <= high) {
				int mid = (low + high) >> 1;

				if (right == null) {
					high = mid - 1;
					continue NEXT;
				} else {
					int left = ((IntColumn)column).getValue(mid);
					if (left < right) {
						low = mid + 1;
						continue NEXT;
					} else if (left > right) {
						high = mid - 1;
						continue NEXT;
					}
				}
		
				return mid;
			}
			return 0;
		}
		
		column = this.columns[findex[0]];
		int cmp = 0;
		while (low <= high) {
			int mid = (low + high) >> 1;

			Object obj = column.getData(mid);
			cmp = Variant.compare(obj, val, true);
			if (cmp != 0) break;
		
			if (cmp < 0) {
				low = mid + 1;
			} else if (cmp > 0) {
				high = mid - 1;
			} else { // key found
				return mid;
			}
		}

		return 0;//-low;
	}

	public int pfindByFields(Object []fvals) {
		Column []columns = this.columns;
		int fcount = this.fcount;
		int low = 1, high = this.size;

		if (isOrder && isInteger) {

//			if (fcount == 1) {
//				int right = ((Integer)fvals[0]);
//				int target = right - firstVal + 1;
//				if(target > 0 && target <= high) {
//					int left = ((IntColumn)columns[0]).getValue(target);
//					if (right == left) {
//						return target;
//					}
//				}
//			}
			
			NEXT:
			while (low <= high) {
				int mid = (low + high) >> 1;
				for (int f = 0; f < fcount; ++f) {
					Integer right = ((Integer)fvals[f]);
					if (right == null) {
						high = mid - 1;
						continue NEXT;
					} else {
						int left = ((IntColumn)columns[f]).getValue(mid);
						if (left < right) {
							low = mid + 1;
							continue NEXT;
						} else if (left > right) {
							high = mid - 1;
							continue NEXT;
						}
					}
				}
				return mid;
			}
			return 0;			
		}
		
		int []findex = this.findex;
		if (isInteger) {
			
//			if (fcount == 1) {
//				int right = ((Integer)fvals[0]);
//				int target = right - firstVal + 1;
//				if(target > 0 && target <= high) {
//					int left = ((IntColumn)columns[findex[0]]).getValue(target);
//					if (right == left) {
//						return target;
//					}
//				}
//			}
			
			NEXT:
			while (low <= high) {
				int mid = (low + high) >> 1;
				for (int f = 0; f < fcount; ++f) {
					Integer right = ((Integer)fvals[f]);
					if (right == null) {
						high = mid - 1;
						continue NEXT;
					} else {
						int left = ((IntColumn)columns[findex[f]]).getValue(mid);
						if (left < right) {
							low = mid + 1;
							continue NEXT;
						} else if (left > right) {
							high = mid - 1;
							continue NEXT;
						}
					}
				}
				return mid;
			}
			return 0;
		}
		
		int cmp = 0;
		while (low <= high) {
			int mid = (low + high) >> 1;
			for (int f = 0; f < fcount; ++f) {
				Object obj = columns[findex[f]].getData(mid);
				cmp = Variant.compare(obj, fvals[f], true);
				if (cmp != 0) break;
			}
			
			if (cmp < 0) {
				low = mid + 1;
			} else if (cmp > 0) {
				high = mid - 1;
			} else { // key found
				return mid;
			}
		}

		return 0;//-low;
	}

	public int[] findAllPos(IArray key) {
		if (key == null) {
			return null;
		}
		int len = key.size();
		int[] pos = new int[len + 1];
		for (int i = 1; i <= len; i++) {
			Object obj = key.get(i);
			pos[i] = pfindByField(obj);
		}
		return pos;
	}

	public int[] findAllPos(IArray[] keys) {
		if (keys == null) {
			return null;
		}
		
		int keyCount = keys.length;
		int len = keys[0].size();
		int[] pos = new int[len + 1];
		Object[] objs = new Object[keyCount];
		for (int i = 1; i <= len; i++) {
			for (int c = 0; c < keyCount; c++) {
				objs[c] = keys[c].get(i);
			}
			pos[i] = pfindByFields(objs);
		}
		return pos;
	}

	public int findPos(Object key) {
		return pfindByField(key);
	}

	public int findPos(Object[] keys) {
		return pfindByFields(keys);
	}

	public int[] findAllPos(IArray key, BoolArray signArray) {
		if (key == null) {
			return null;
		}
		int len = key.size();
		int[] pos = new int[len + 1];
		for (int i = 1; i <= len; i++) {
			if (signArray.isFalse(i)) {
				continue;
			}
			Object obj = key.get(i);
			pos[i] = pfindByField(obj);
		}
		return pos;
	}

	public int[] findAllPos(IArray[] keys, BoolArray signArray) {
		if (keys == null) {
			return null;
		}
		
		int keyCount = keys.length;
		int len = keys[0].size();
		int[] pos = new int[len + 1];
		Object[] objs = new Object[keyCount];
		for (int i = 1; i <= len; i++) {
			if (signArray.isFalse(i)) {
				continue;
			}
			for (int c = 0; c < keyCount; c++) {
				objs[c] = keys[c].get(i);
			}
			pos[i] = pfindByFields(objs);
		}
		return pos;
	}
	

//	public void create() {
//		int []findex = this.findex;
//		Column []columns = this.columns;
//		int fcount = findex.length;
//		HashUtil hashUtil = this.hashUtil;
//		int []hashCol1 = new int[hashUtil.getCapacity()];
//		int []hashCol2 = new int[hashUtil.getCapacity()];
//		this.hashCol1 = hashCol1;
//		this.hashCol2 = hashCol2;
//		
//		Object objs[] = new Object[fcount];
//		
//		int size = this.size;
//		for (int i = 1, len = size; i <= len; ++i) {
//			for (int f = 0; f < fcount; ++f) {
//				objs[f] = columns[findex[f]].getData(i);
//			}
//
//			int hash = hashUtil.hashCode(objs, fcount);
//			int i1 = hashCol1[hash];
//			if (i1 == 0) {
//				hashCol1[hash] = i;
//			} else {
//				hashCol2[hash] = i;
//			}
//		}
//	}

//	public static void lr_test(ICursor cs) {
//	}
}
