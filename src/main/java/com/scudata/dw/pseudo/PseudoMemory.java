package com.scudata.dw.pseudo;

import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.op.New;
import com.scudata.dm.op.Operation;
import com.scudata.dm.op.Select;
import com.scudata.expression.Expression;

/**
 * 内存虚表；从内存变量产生虚表
 * @author LW
 *
 */
public class PseudoMemory extends Pseudo {
	protected int pathCount;
	public PseudoMemory() {
	}
	
	public PseudoMemory(Record rec, int n, Context ctx) {
		pd = new PseudoDefination(rec, ctx);
		pathCount = n;
		this.ctx = ctx;
	}
	
	public void addColNames(String []nameArray) {
	}
	
	public void addColName(String name) {
	}
	
	public ICursor cursor(Expression []exps, String []names) {
		ICursor cs = addOptionToCursor(new MemoryCursor(pd.getMemoryTable()));
		
		if (filter != null) {
			cs.addOperation(new Select(filter, null), ctx);
		}
		
		if (exps == null && names == null) {
			return cs;
		} else {
			New _new = new New(exps, names, null);
			cs.addOperation(_new, ctx);
			return cs;
		}
	}
	
	public void addPKeyNames() {
	}
	
	public boolean isColumn(String col) {
		DataStruct ds = pd.getMemoryTable().dataStruct();
		return (ds.getFieldIndex(col) >= 0);
	}
	
	public void append(ICursor cursor, String option) {
		
	}
	
	public Sequence update(Sequence data, String opt) {
		return null;
	}
	
	public Sequence delete(Sequence data, String opt) {
		return null;
	}
	
	private ICursor addOptionToCursor(ICursor cursor) {
		if (opList != null) {
			for (Operation op : opList) {
				cursor.addOperation(op, ctx);
			}
		}
		return cursor;
	}
	
	public Sequence getTable() {
		return pd.getMemoryTable();
	}

	public Pseudo setPathCount(int pathCount) {
		this.pathCount = pathCount;
		return this;
	}
	
	public Object clone(Context ctx) throws CloneNotSupportedException {
		PseudoMemory obj = new PseudoMemory();
		cloneField(obj);
		obj.ctx = ctx;
		return obj;
	}
}
