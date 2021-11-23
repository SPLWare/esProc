package com.scudata.ide.dfx.etl.element;

import com.scudata.chart.Consts;
import com.scudata.ide.dfx.etl.EtlConsts;
import com.scudata.ide.dfx.etl.ParamInfo;
import com.scudata.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 A.group(x:F,...;y:G,…)
 * 函数名前缀A表示序表
 * 
 * @author Joancy
 *
 */

public class AGroup2 extends AGroups {
	
	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(AGroups.class, this);
		paramInfos.add(new ParamInfo("groupExps",EtlConsts.INPUT_FIELDDEFINE_EXP_FIELD));
		paramInfos.add(new ParamInfo("aggregateExps",EtlConsts.INPUT_FIELDDEFINE_EXP_FIELD,true));
		
		String group = "options";
		paramInfos.add(group, new ParamInfo("o", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("n", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("u", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("i", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("zero", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("h", Consts.INPUT_CHECKBOX));

		return paramInfos;
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName(){
		return "group";
	}

}
