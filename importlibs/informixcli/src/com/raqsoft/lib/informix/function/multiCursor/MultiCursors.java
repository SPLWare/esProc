package com.raqsoft.lib.informix.function.multiCursor;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.cursor.MultipathCursors;

public class MultiCursors extends  MultipathCursors
{
	public MultiCursors(ICursor[] cursors, Context ctx){
		super(cursors, ctx);
	}
	
	public Sequence get(int n){
		return super.get(n);
	}
}
