package com.raqsoft.expression.mfn;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.IResource;
import com.raqsoft.expression.MemberFunction;
import com.raqsoft.vdb.VDB;

/**
 * 关闭资源
 * db.close() T.close() cs.close()等
 * @author RunQian
 *
 */
public class Close extends MemberFunction {
	protected IResource resource;
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof IResource;
	}

	public void setDotLeftObject(Object obj) {
		resource = (IResource)obj;
	}

	public Object calculate(Context ctx) {
		if (option != null && option.indexOf('p') != -1 && resource instanceof VDB) {
			((VDB)resource).getLibrary().stop();
		} else {
			resource.close();
		}

		return null;
	}
}
