package com.scudata.dm;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.expression.Expression;
import com.scudata.util.Variant;

public class SortIndexTable extends IndexTable {
	private Sequence code;
	private int []findex;
	private Object []keys;

	public SortIndexTable(Sequence code, int []fields) {
		this.code = code;
		this.findex = fields;
		this.keys = new Object[1];
	}
	
	public Object find(Object key) {
		keys[0] = key;
		int idx = pfindByFields(keys, findex);
		if (idx < 0) {
			return null;
		} else {
			return code.getMem(idx);
		}
	}

	public Object find(Object []keys) {
		int idx = pfindByFields(keys, findex);
		if (idx < 0) {
			return null;
		} else {
			return code.getMem(idx);
		}
	}
	
	public void create(Sequence code, Expression []exps, Context ctx) {
		this.code = code;
		DataStruct ds = code.dataStruct();
		int keyCount = exps.length;
		findex = new int[keyCount];
		for (int i = 0; i < keyCount; ++i) {
			String field = exps[i].getIdentifierName();
			findex[i] = ds.getFieldIndex(field);
		}
		keys = new Object[1];
	}
	
	public void create(Sequence code, int []findex) {
		this.code = code;
		this.findex = findex;
		keys = new Object[1];
	}
	
	public int pfindByFields(Object []fvals, int []findex) {
		IArray mems = code.getMems();
		int len = mems.size();
		int fcount = findex.length;
		Object []vals = new Object[fcount];

		int low = 1, high = len;
		while (low <= high) {
			int mid = (low + high) >> 1;
			BaseRecord r = (BaseRecord)mems.get(mid);
			for (int f = 0; f < fcount; ++f) {
				vals[f] = r.getNormalFieldValue(findex[f]);
			}

			int value = Variant.compareArrays(vals, fvals);
			if (value < 0) {
				low = mid + 1;
			} else if (value > 0) {
				high = mid - 1;
			} else { // key found
				return mid;
			}
		}

		return -low;
	}
	
	public int findPos(Object key) {
		keys[0] = key;
		int idx = pfindByFields(keys, findex);
		if (idx < 0) {
			return 0;
		} else {
			return idx;
		}
	}

	public int findPos(Object []keys) {
		int idx = pfindByFields(keys, findex);
		if (idx < 0) {
			return -1;
		} else {
			return idx;
		}
	}
	
	public int[] findAllPos(IArray key) {
		if (key == null) {
			return null;
		}
		int len = key.size();
		int[] pos = new int[len + 1];
		for (int i = 1; i <= len; i++) {
			Object obj = key.get(i);
			pos[i] = findPos(obj);
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
			pos[i] = findPos(objs);
		}
		return pos;
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
			pos[i] = findPos(obj);
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
			pos[i] = findPos(objs);
		}
		return pos;
	}
}
