package com.scudata.expression;

import com.scudata.dw.pseudo.IPseudo;

public abstract class PseudoFunction extends MemberFunction {
	protected IPseudo pseudo;
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof IPseudo;
	}

	public void setDotLeftObject(Object obj) {
		pseudo = (IPseudo)obj;
	}

	
	/**
	 * 释放节点引用的点操作符左侧的对象
	 */
	public void releaseDotLeftObject() {
		pseudo = null;
	}
}
