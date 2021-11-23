package com.scudata.ide.dfx.etl.element;

import com.scudata.ide.dfx.etl.EtlConsts;
import com.scudata.ide.dfx.etl.ParamInfoList;
/**
 * 辅助函数编辑 CS.new()
 * 函数名前缀Cs表示游标
 * 
 * @author Joancy
 *
 */
public class CsNew extends ANew {

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

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = super.getParamInfoList();
		
//		游标没有m选项
		String group = "options";
		paramInfos.delete(group, "m");

		return paramInfos;
	}
	
	/**
	 * 获取用于生成SPL表达式的选项串
	 */
	public String optionString(){
		StringBuffer options = new StringBuffer();
		if(i){
			options.append("i");
		}
		return options.toString();
	}
	
}
