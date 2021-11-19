package com.raqsoft.dw.pseudo;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.expression.Expression;

public interface IPseudo {
	public void addColNames(String []nameArray);
	public void addColName(String name);
	public ICursor cursor(Expression []exps, String []names);
	public void addPKeyNames();
	public boolean isColumn(String col);
	public Context getContext();
	public Object clone(Context ctx) throws CloneNotSupportedException;
	public void append(ICursor cursor, String option);
	public Sequence update(Sequence data, String opt);
}
