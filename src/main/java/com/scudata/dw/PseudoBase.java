package com.scudata.dw;

import com.scudata.dm.Context;
import com.scudata.dm.IResource;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.Operable;
import com.scudata.dm.op.Operation;

/**
 * 虚表的基础类
 * @author LW
 *
 */
public class PseudoBase extends Operable implements IResource {

	public void close() {
		throw new RuntimeException();
	}

	public Operable addOperation(Operation op, Context ctx) {
		throw new RuntimeException();
	}

	public Sequence toSequence() {
		throw new RuntimeException();
	}
	
	public ICursor toCursor() {
		throw new RuntimeException();
	}
}
