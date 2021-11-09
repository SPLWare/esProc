package com.raqsoft.ide.dfx.etl.element;

import java.util.ArrayList;
import com.raqsoft.ide.dfx.etl.EtlConsts;
import com.raqsoft.ide.dfx.etl.ObjectElement;
import com.raqsoft.ide.dfx.etl.ParamInfo;
import com.raqsoft.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 create()
 * 
 * @author Joancy
 *
 */
public class Create extends ObjectElement {
	public ArrayList<String> fields;
	
	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(Create.class, this);

		paramInfos.add(new ParamInfo("fields",EtlConsts.INPUT_STRINGLIST));
		return paramInfos;
	}

	/**
	 * 获取父类型
	 * 类型的常量定义为
	 * EtlConsts.TYPE_XXX
	 * @return EtlConsts.TYPE_EMPTY
	 */
	public byte getParentType() {
		return EtlConsts.TYPE_EMPTY;
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
		return "";
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "create";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		return getStringListExp(fields,",");
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody( String funcBody ) {
		fields = getStringList(funcBody,",");
		return true;
	}

}
