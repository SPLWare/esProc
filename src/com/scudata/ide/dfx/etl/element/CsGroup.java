package com.scudata.ide.dfx.etl.element;

import java.util.ArrayList;

import com.scudata.chart.Consts;
import com.scudata.ide.dfx.etl.EtlConsts;
import com.scudata.ide.dfx.etl.ObjectElement;
import com.scudata.ide.dfx.etl.ParamInfo;
import com.scudata.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 CS.group()
 * 函数名前缀Cs表示游标
 * 
 * @author Joancy
 *
 */
public class CsGroup extends ObjectElement {
	public ArrayList<String> groupFields;
	
	public boolean i;
	public boolean one;
	public boolean q;
	public boolean s;
	
	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(CsGroup.class, this);

		paramInfos.add(new ParamInfo("groupFields",EtlConsts.INPUT_STRINGLIST));
		
		String group = "options";
		paramInfos.add(group, new ParamInfo("i", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("one", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("q", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("s", Consts.INPUT_CHECKBOX));

		return paramInfos;
	}

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
	 * 获取用于生成SPL表达式的选项串
	 */
	public String optionString(){
		StringBuffer options = new StringBuffer();
		if(i){
			options.append("i");
		}
		if(one){
			options.append("1");
		}
		if(q){
			options.append("q");
			if(s){//仅排序，不分组，必须要与@q配合使用
				options.append("s");
			}
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
		groupFields = getStringList(funcBody,",");
		return true;
	}
}
