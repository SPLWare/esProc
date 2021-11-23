package com.scudata.ide.dfx.etl.element;

import java.util.StringTokenizer;

import com.scudata.chart.Consts;
import com.scudata.ide.dfx.etl.EtlConsts;
import com.scudata.ide.dfx.etl.ObjectElement;
import com.scudata.ide.dfx.etl.ParamInfo;
import com.scudata.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 CS.fetch()
 * 函数名前缀Cs表示游标
 * 
 * @author Joancy
 *
 */
public class CsFetch extends ObjectElement {
	public String fetchN;
	public String groupX;
	
	public boolean zero;
	public boolean x;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(CsFetch.class, this);

		paramInfos.add(new ParamInfo("fetchN"));
		paramInfos.add(new ParamInfo("groupX", EtlConsts.INPUT_ONLYPROPERTY));
		String group = "options";
		paramInfos.add(group, new ParamInfo("zero", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("x", Consts.INPUT_CHECKBOX));

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
		if(zero){
			options.append("0");
		}else if(x){
			options.append("x");
		}
		return options.toString();
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "fetch";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		String buf = getNumberExp(fetchN);
		if(!buf.isEmpty()){
			sb.append(buf);
		}
		buf = getExpressionExp(groupX);
		if(!buf.isEmpty()){
			sb.append(";");
			sb.append(buf);
		}
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		StringTokenizer st = new StringTokenizer(funcBody,";");
		String buf = st.nextToken(); 
		fetchN = getNumber( buf );
		
		if(st.hasMoreTokens()){
			buf = st.nextToken();
			groupX = getExpression(buf);
		}
		return true;
	}

}
