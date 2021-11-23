package com.scudata.ide.dfx.etl.element;

import com.scudata.chart.Consts;
import com.scudata.ide.dfx.etl.EtlConsts;
import com.scudata.ide.dfx.etl.ParamInfo;
import com.scudata.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 f.cursor()
 * 函数名前缀F表示文件对象
 * 
 * @author Joancy
 *
 */
public class FCursor extends FImport {
	public boolean x;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = super.getParamInfoList();
		ParamInfo.setCurrent(FCursor.class, this);

		String group = "options";
		paramInfos.add(group, new ParamInfo("x", Consts.INPUT_CHECKBOX));

		return paramInfos;
	}
	
	/**
	 * 获取该函数的返回类型
	 * @return EtlConsts.TYPE_CURSOR
	 */
	public byte getReturnType() {
		return EtlConsts.TYPE_CURSOR;
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName(){
		return "cursor";
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
