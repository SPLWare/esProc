package com.scudata.ide.dfx.etl.element;

import java.util.StringTokenizer;

import com.scudata.chart.Consts;
import com.scudata.common.StringUtils;
import com.scudata.ide.dfx.etl.EtlConsts;
import com.scudata.ide.dfx.etl.ParamInfo;
import com.scudata.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 CS.groupx()
 * 函数名前缀Cs表示游标
 * 
 * @author Joancy
 *
 */
public class CsGroupx extends CsGroups {
	public String bufferN;
	
	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(AGroups.class, this);
		paramInfos.add(new ParamInfo("groupExps",EtlConsts.INPUT_FIELDDEFINE_EXP_FIELD));
		paramInfos.add(new ParamInfo("aggregateExps",EtlConsts.INPUT_FIELDDEFINE_EXP_FIELD));
		ParamInfo.setCurrent(CsGroupx.class, this);
		paramInfos.add(new ParamInfo("bufferN"));
		
		ParamInfo.setCurrent(AGroups.class, this);
		String group = "options";
		paramInfos.add(group, new ParamInfo("n", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("u", Consts.INPUT_CHECKBOX));

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
		if(n){
			options.append("n");
		}
		if(u && !n){//u,n互斥
			options.append("u");
		}
		return options.toString();
	}
	
	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append( getFieldDefineExp(groupExps) );
		String aggregates = getFieldDefineExp(aggregateExps);
		if(!aggregates.isEmpty()){
			sb.append(";");
			sb.append(aggregates);
		}
		if(StringUtils.isValidString(bufferN)){
			sb.append(";");
			sb.append(getExpressionExp(bufferN));
		}
		
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		StringTokenizer st = new StringTokenizer(funcBody,";");
		String tmp = st.nextToken();
		groupExps = getFieldDefine(tmp);
		if(st.hasMoreTokens()){
			tmp = st.nextToken();
			aggregateExps = getFieldDefine(tmp);
		}
		if(st.hasMoreTokens()){
			tmp = st.nextToken();
			bufferN = getExpression(tmp);
		}
		return true;
	}

}
