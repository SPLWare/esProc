package com.scudata.ide.spl.etl.element;

import com.scudata.ide.spl.etl.EtlConsts;

/**
 * 辅助函数编辑 CS.rename()
 * 函数名前缀Cs表示游标
 * 
 * @author Joancy
 *
 */
public class CsRename extends ARename {
	
	/**
	 * 获取父类型
	 * 类型的常量定义为
	 * EtlConsts.TYPE_XXX
	 * @return EtlConsts.TYPE_CURSOR
	 */
	public byte getParentType() {
		return EtlConsts.TYPE_CURSOR;
	}

	/**
	 * 获取该函数的返回类型
	 * @return EtlConsts.TYPE_CURSOR
	 */
	public byte getReturnType() {
		return EtlConsts.TYPE_CURSOR;
	}

}
