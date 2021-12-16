package com.scudata.ide.spl.etl.element;

import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.ObjectElement;
import com.scudata.ide.spl.etl.ParamInfoList;

/**
 * 辅助函数编辑 t.close()
 * 函数名前缀T表示CTX实表
 * 
 * @author Joancy
 *
 */
public class TClose extends ObjectElement {

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		return paramInfos;
	}

	/**
	 * 获取父类型
	 * 类型的常量定义为
	 * EtlConsts.TYPE_XXX
	 * @return EtlConsts.TYPE_CTX
	 */
	public byte getParentType() {
		return EtlConsts.TYPE_CTX;
	}

	/**
	 * 获取该函数的返回类型
	 * @return EtlConsts.TYPE_EMPTY
	 */
	public byte getReturnType() {
		return EtlConsts.TYPE_EMPTY;
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
		return "close";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		return null;
	}
	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		return true;
	}


}
