package com.scudata.ide.spl.etl.element;

import com.scudata.chart.Consts;
import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.ObjectElement;
import com.scudata.ide.spl.etl.ParamInfo;
import com.scudata.ide.spl.etl.ParamInfoList;

/**
 * 辅助函数编辑 t.update()
 * 函数名前缀T表示CTX实表
 * 
 * @author Joancy
 *
 */
public class TUpdate extends ObjectElement {
	public String tableName;

	public boolean i;
	public boolean u;
	public boolean n;
	
	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(TUpdate.class, this);

		paramInfos.add(new ParamInfo("tableName",EtlConsts.INPUT_CELLA,true));
		String group = "options";
		paramInfos.add(group, new ParamInfo("i", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("u", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("n", Consts.INPUT_CHECKBOX));
		
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
	 * @return EtlConsts.TYPE_CTX
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
		if(u){
			sb.append("u");
		}
		if(n){
			sb.append("n");
		}
		return sb.toString();
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "update";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append(getExpressionExp(tableName));
		
		return sb.toString();
	}


	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody( String funcBody ) {
		tableName = getExpression( funcBody );
		return true;
	}


}
