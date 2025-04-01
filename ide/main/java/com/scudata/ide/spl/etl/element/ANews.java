package com.scudata.ide.spl.etl.element;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.FieldDefine;
import com.scudata.ide.spl.etl.ObjectElement;
import com.scudata.ide.spl.etl.ParamInfo;
import com.scudata.ide.spl.etl.ParamInfoList;

/**
 * 辅助函数编辑 A.news()
 * 函数名前缀A表示序表
 * 
 * @author Joancy
 *
 */
public class ANews extends ObjectElement {
	public String bigX;
	public ArrayList<FieldDefine> newFields;//不使用第三列， 表达式，字段名
	
	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(ANews.class, this);

		paramInfos.add(new ParamInfo("bigX"));
		paramInfos.add(new ParamInfo("newFields",EtlConsts.INPUT_FIELDDEFINE_EXP_FIELD));
		
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
	public String optionString() {
		return null;
	}
	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "news";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append( getExpressionExp(bigX) );
		sb.append(";");
		sb.append( getFieldDefineExp(newFields));
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		StringTokenizer st = new StringTokenizer(funcBody,";");
		String tmp = st.nextToken();
		bigX = getExpression(tmp);
		
		tmp = st.nextToken();
		newFields = getFieldDefine(tmp);
		return true;
	}

}
