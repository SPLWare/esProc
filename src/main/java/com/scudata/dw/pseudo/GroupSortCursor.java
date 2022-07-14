package com.scudata.dw.pseudo;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;

public class GroupSortCursor extends ICursor {
	private ICursor cs;
	private Expression groupExp;
	private Expression sortExp;
	private Sequence cache;
	
	public GroupSortCursor(ICursor cs, String group, String sort, Context ctx) {
		this.cs = cs;
		this.ctx = ctx;
		groupExp = new Expression(group);
		sortExp = new Expression(sort);
	}
	
	protected Sequence get(int n) {
		Sequence cache = this.cache;
		if (cache != null) {
			int len = cache.length();
			if (len > n) {
				this.cache = cache.split(n + 1);
				return cache;
			} else if (len == n) {
				this.cache = null;
				return cache;
			}
		} else {
			Sequence data = cs.fetchGroup(groupExp,ctx);
			if (data == null) return null;
			data = data.sort(sortExp, null, null, ctx);
			if (data.length() == n) return data;
			if (data.length() > n) {
				this.cache = data.split(n + 1);
				return data;
			}
			cache = data;
		}
		
		int count = n - cache.length();
		while (count > 0) {
			Sequence data = cs.fetchGroup(groupExp,ctx);
			if (data == null) {
				return cache;
			}
			data = data.sort(sortExp, null, null, ctx);
			
			int len = data.length(); 
			if (len <= count) {
				cache.addAll(data);
				count -= len;
			} else {
				this.cache = data.split(count + 1);
				cache.addAll(data);
				return cache;
			}
		}
		return cache;
	}

	protected long skipOver(long n) {
		Sequence data = get((int) n);
		return data == null ? 0 : data.length();
	}

}
