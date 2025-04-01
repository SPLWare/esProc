package com.scudata.ide.spl.etl.element;

import java.util.ArrayList;

import com.scudata.chart.Consts;
import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.ObjectElement;
import com.scudata.ide.spl.etl.ParamInfo;
import com.scudata.ide.spl.etl.ParamInfoList;


/**
 * 辅助函数编辑 A.group()
 * 函数名前缀A表示序表
 * 
 * @author Joancy
 *
 */
public class AGroup extends ObjectElement {
	public ArrayList<String> groupFields;
	
	public boolean o;
	public boolean one;
	public boolean n;
	public boolean u;
	public boolean i;
	public boolean zero;
	public boolean s;
	public boolean p;
	public boolean h;
	
	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(AGroup.class, this);

		paramInfos.add(new ParamInfo("groupFields",EtlConsts.INPUT_STRINGLIST));
		
		String group = "options";
		paramInfos.add(group, new ParamInfo("o", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("one", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("n", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("u", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("i", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("zero", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("s", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("p", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("h", Consts.INPUT_CHECKBOX));

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
		if(o){
			options.append("o");
		}
		if(one){
			options.append("1");
		}
		if(n && !o){//o n互斥
			options.append("n");
		}
		if(u && !(o || n)){//与o,n互斥
			options.append("u");
		}
		if(i){
			options.append("i");
		}
		if(zero){
			options.append("0");
		}
		if(s){
			options.append("s");
		}
		if(p){
			options.append("p");
		}
		if(h){
			options.append("h");
		}
		return options.toString();
	}
	
	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "group";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		return getStringListExp(groupFields,",");
	}


	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		if(funcBody.indexOf(";")>0){
			return false;
		}
		groupFields = getStringList(funcBody,",");
		return true;
	}

}
