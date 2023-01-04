package com.scudata.dw.pseudo;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.Operable;
import com.scudata.expression.Expression;

public abstract class IPseudo extends Operable {
	public abstract void addColNames(String []nameArray);
	public abstract void addColName(String name);
	public abstract Sequence Import(Expression []exps, String []names);
	public abstract ICursor cursor(Expression []exps, String []names);
	public abstract ICursor cursor(Expression []exps, String []names, boolean isColumn);
	public abstract void addPKeyNames();
	public abstract boolean isColumn(String col);
	public abstract Context getContext();
	public abstract Object clone(Context ctx) throws CloneNotSupportedException;
	public abstract void append(ICursor cursor, String option);
	public abstract Sequence update(Sequence data, String opt);
	public abstract Sequence delete(Sequence data, String opt);
	public abstract void setCache(Sequence cache);
	public abstract Sequence getCache();
}
