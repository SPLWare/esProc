package com.scudata.ide.dfx.etl.element;

import java.util.StringTokenizer;

import com.scudata.common.StringUtils;
import com.scudata.ide.dfx.etl.EtlConsts;
import com.scudata.ide.dfx.etl.ObjectElement;
import com.scudata.ide.dfx.etl.ParamInfo;
import com.scudata.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 f.xlswrite()
 * 函数名前缀F表示文件对象
 * 
 * @author Joancy
 *
 */
public class FXlsWrite extends ObjectElement {
	public String xls;
	public String password;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(FXlsWrite.class, this);

		paramInfos.add(new ParamInfo("xls",EtlConsts.INPUT_CELLXLS,true));
		paramInfos.add(new ParamInfo("password"));
		
		return paramInfos;
	}

	/**
	 * 获取父类型
	 * 类型的常量定义为
	 * EtlConsts.TYPE_XXX
	 * @return EtlConsts.TYPE_FILE
	 */
	public byte getParentType() {
		return EtlConsts.TYPE_FILE;
	}

	/**
	 * 获取该函数的返回类型
	 * @return EtlConsts.TYPE_EMPTY
	 */
	public byte getReturnType() {
		return EtlConsts.TYPE_EMPTY;
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName(){
		return "xlswrite";
	}
	
	/**
	 * 获取用于生成SPL表达式的选项串
	 */
	public String optionString(){
		return null;
	}
	
	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append(getExpressionExp(xls));
		if(StringUtils.isValidString(password)){
			sb.append(",");
			sb.append( getParamExp(password) );
		}
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		StringTokenizer st = new StringTokenizer(funcBody,",");
		xls = getExpression( st.nextToken() );
		if( st.hasMoreTokens()){
			password = getParam(st.nextToken());
		}
		return true;
	}

}
