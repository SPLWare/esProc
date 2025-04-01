package com.scudata.ide.spl.etl.element;

import com.scudata.chart.Consts;
import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.ParamInfo;
import com.scudata.ide.spl.etl.ParamInfoList;

/**
 * 辅助函数编辑 db.query()
 * 函数名前缀D表示数据库连接
 * 
 * @author Joancy
 *
 */
public class DQuerySQL extends DCursorSQL {
	public boolean one;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = super.getParamInfoList();
		ParamInfo.setCurrent(DQuerySQL.class, this);

		String group = "options";
		paramInfos.add(group, new ParamInfo("one", Consts.INPUT_CHECKBOX));

		return paramInfos;
	}

	/**
	 * 获取父类型
	 * 类型的常量定义为
	 * EtlConsts.TYPE_XXX
	 * @return EtlConsts.TYPE_DB
	 */
	public byte getParentType() {
		return EtlConsts.TYPE_DB;
	}

	/**
	 * 获取该函数的返回类型
	 * @return EtlConsts.TYPE_SEQUENCE
	 */
	public byte getReturnType() {
		return EtlConsts.TYPE_SEQUENCE;
	}

	/**
	 * 获取用于生成SPL表达式的选项串
	 */
	public String optionString(){
		StringBuffer sb = new StringBuffer();
		sb.append(  super.optionString() );
		if(one){
			sb.append("1");
		}
		return sb.toString();
	}
	
	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName(){
		return "query";
	}

}
