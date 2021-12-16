package com.scudata.ide.spl.etl.element;

import com.scudata.chart.Consts;
import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.ObjectElement;
import com.scudata.ide.spl.etl.ParamInfo;
import com.scudata.ide.spl.etl.ParamInfoList;

/**
 * 辅助函数编辑 A.select()
 * 函数名前缀A表示序表
 * 
 * @author Joancy
 *
 */
public class ASelect extends ObjectElement {
	public String filter;
	
	public boolean one;//1
	public boolean z;
	public boolean b;
	public boolean m;
	public boolean c;
	public boolean r;
	public boolean t;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(ASelect.class, this);

		paramInfos.add(new ParamInfo("filter"));
		String group = "options";
		paramInfos.add(group, new ParamInfo("one", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("z", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("b", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("m", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("c", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("r", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("t", Consts.INPUT_CHECKBOX));

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
//		1bzm
		if(m){//与@1bz选项互斥
			return "m";
		}
		StringBuffer options = new StringBuffer();
		if(one){
			options.append("1");
		}
		if(b){
			options.append("b");
		}
		if(z){
			options.append("z");
		}
		if(c){
			options.append("c");
		}
		if(r){
			options.append("r");
		}
		if(t){
			options.append("t");
		}
		return options.toString();
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "select";
	}


	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		return getExpressionExp(filter);
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		filter = getExpression(funcBody);
		return true;
	}
}
