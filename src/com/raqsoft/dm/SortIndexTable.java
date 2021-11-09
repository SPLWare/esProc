package com.raqsoft.dm;

import com.raqsoft.expression.Expression;
import com.raqsoft.util.Variant;

public class SortIndexTable extends IndexTable {

	private Sequence code;
	private int findex[];
	private Object keys[];

	public Object find(Object key) {
		keys[0] = key;
		int idx = pfindByFields(keys, findex);
		if (idx < 0) {
			return null;
		} else {
			return code.getMem(idx);
		}
	}

	public Object find(Object[] keys) {
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
		ListBase1 mems = code.mems;
		int len = mems.size();
		int fcount = findex.length;
		Object []vals = new Object[fcount];

		int low = 1, high = len;
		while (low <= high) {
			int mid = (low + high) >> 1;
			Record r = (Record)mems.get(mid);
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
}
