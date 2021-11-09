package com.raqsoft.ide.dfx.etl.element;

import com.raqsoft.chart.Consts;
import com.raqsoft.ide.dfx.etl.EtlConsts;
import com.raqsoft.ide.dfx.etl.ObjectElement;
import com.raqsoft.ide.dfx.etl.ParamInfo;
import com.raqsoft.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 t.append()
 * 函数名前缀T表示CTX实表
 * 
 * @author Joancy
 *
 */
public class TAppend extends ObjectElement {
	public String cursorName;
	
	public boolean i;
	public boolean a;
	public boolean x;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(TAppend.class, this);

		paramInfos.add(new ParamInfo("cursorName",EtlConsts.INPUT_CELLCURSOR,true));
		String group = "options";
		paramInfos.add(group, new ParamInfo("i", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("a", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("x", Consts.INPUT_CHECKBOX));

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
	 * @return EtlConsts.TYPE_CTX;
	 */
	public byte getReturnType() {
		return EtlConsts.TYPE_CTX;
	}

	
	/**
	 * 获取用于生成SPL表达式的选项串
	 */
	public String optionString(){
		StringBuffer sb = new StringBuffer();
		if(i){
			sb.append("i");
		}
		if(a){
			sb.append("a");
		}
		if(x){
			sb.append("x");
		}
		return sb.toString();
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "append";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append(getExpressionExp(cursorName));
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		cursorName = getExpression( funcBody );
		return true;
	}

}
