package com.scudata.expression.mfn;

import com.scudata.dm.Context;
import com.scudata.dm.IResource;
import com.scudata.expression.MemberFunction;
import com.scudata.vdb.VDB;

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
	
	/**
	 * 释放节点引用的点操作符左侧的对象
	 */
	public void releaseDotLeftObject() {
		resource = null;
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
