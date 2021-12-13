package com.scudata.dw.pseudo;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.Operable;
import com.scudata.expression.Expression;

public interface IPseudo extends Operable {
	public void addColNames(String []nameArray);
	public void addColName(String name);
	public ICursor cursor(Expression []exps, String []names);
	public void addPKeyNames();
	public boolean isColumn(String col);
	public Context getContext();
	public Object clone(Context ctx) throws CloneNotSupportedException;
	public void append(ICursor cursor, String option);
	public Sequence update(Sequence data, String opt);
	public Sequence delete(Sequence data, String opt);
}
