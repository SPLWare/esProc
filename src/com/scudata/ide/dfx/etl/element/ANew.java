package com.scudata.ide.dfx.etl.element;

import java.util.ArrayList;

import com.scudata.chart.Consts;
import com.scudata.ide.dfx.etl.EtlConsts;
import com.scudata.ide.dfx.etl.FieldDefine;
import com.scudata.ide.dfx.etl.ObjectElement;
import com.scudata.ide.dfx.etl.ParamInfo;
import com.scudata.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 A.new()
 * 函数名前缀A表示序表
 * 
 * @author Joancy
 *
 */
public class ANew extends ObjectElement {
	public ArrayList<FieldDefine> newFields;//不使用第三列， 表达式，字段名
	
	public boolean i;
	public boolean m;
	
	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(ANew.class, this);

		paramInfos.add(new ParamInfo("newFields",EtlConsts.INPUT_FIELDDEFINE_EXP_FIELD,true));
		
		String group = "options";
		paramInfos.add(group, new ParamInfo("i", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("m", Consts.INPUT_CHECKBOX));

		return paramInfos;
	}


	/**
	 * 获取父类型
	 * 类型的常量定义为
	 * EtlConsts.TYPE_XXX
	 * @return 前缀A开头的函数，均返回EtlConsts.TYPE_SEQUENCE
	 */
	public byte getParentType() {
		return EtlConsts.TYPE_SEQUENCE;
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
		StringBuffer options = new StringBuffer();
		if(i){
			options.append("i");
		}
		if(m){
			options.append("m");
		}
		return options.toString();
	}
	
	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "new";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		return getFieldDefineExp(newFields);
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String body) {
		newFields = getFieldDefine(body);
		return true;
	}
}
