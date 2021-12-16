package com.scudata.ide.spl.etl.element;

import com.scudata.chart.Consts;
import com.scudata.ide.spl.etl.ParamInfo;
import com.scudata.ide.spl.etl.ParamInfoList;

/**
 * 辅助函数编辑 A.derive()
 * 函数名前缀A表示序表
 * 
 * @author Joancy
 *
 */
public class ADerive extends ANew {
	public boolean x;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = super.getParamInfoList();
		ParamInfo.setCurrent(ADerive.class, this);

		String group = "options";
		paramInfos.add(group, new ParamInfo("x", Consts.INPUT_CHECKBOX));

		return paramInfos;
	}
	
	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName(){
		return "derive";
	}

	/**
	 * 获取用于生成SPL表达式的选项串
	 */
	public String optionString(){
		StringBuffer options = new StringBuffer();
		options.append(super.optionString());
		if(x){
			options.append("x");
		}
		return options.toString();
	}
}
