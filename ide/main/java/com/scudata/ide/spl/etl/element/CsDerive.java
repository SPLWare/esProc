package com.scudata.ide.spl.etl.element;

/**
 * 辅助函数编辑 CS.derive()
 * 函数名前缀Cs表示游标
 * 
 * @author Joancy
 *
 */
public class CsDerive extends CsNew {

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName(){
		return "derive";
	}

}
