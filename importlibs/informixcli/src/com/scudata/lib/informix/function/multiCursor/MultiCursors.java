package com.scudata.lib.informix.function.multiCursor;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MultipathCursors;

public class MultiCursors extends  MultipathCursors
{
	public MultiCursors(ICursor[] cursors, Context ctx){
		super(cursors, ctx);
	}
	
	public Sequence get(int n){
		return super.get(n);
	}
}
