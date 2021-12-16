package com.scudata.ide.spl.etl.element;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.scudata.chart.Consts;
import com.scudata.common.StringUtils;
import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.ObjectElement;
import com.scudata.ide.spl.etl.ParamInfo;
import com.scudata.ide.spl.etl.ParamInfoList;

/**
 * 辅助函数编辑 CS.sortx()
 * 函数名前缀Cs表示游标
 * 
 * @author Joancy
 *
 */
public class CsSortx extends ObjectElement {
	public ArrayList<String> sortFields;

	public String bufferN;
	
	public boolean zero;
	public boolean n;
	
	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(CsSortx.class, this);

		paramInfos.add(new ParamInfo("sortFields",EtlConsts.INPUT_STRINGLIST));
		paramInfos.add(new ParamInfo("bufferN"));

		String group = "options";			
		paramInfos.add(group, new ParamInfo("zero", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("n", Consts.INPUT_CHECKBOX));
		

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
		if(zero){
			options.append("0");
		}else{
			if(n){
				options.append("n");
			}
		}
		return options.toString();
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "sortx";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append( getStringListExp(sortFields,",") );
		
		if(StringUtils.isValidString( bufferN )){
			sb.append(";");
			String buf = getNumberExp(bufferN);
			sb.append(buf);
		}
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		StringTokenizer st = new StringTokenizer( funcBody,";");
		
		sortFields = getStringList( st.nextToken(),"," );
		
		if(st.hasMoreTokens()){
			bufferN = getNumber(st.nextToken());
		}
		return true;
	}

}
