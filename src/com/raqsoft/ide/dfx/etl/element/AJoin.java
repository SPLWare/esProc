package com.raqsoft.ide.dfx.etl.element;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.raqsoft.chart.Consts;
import com.raqsoft.common.StringUtils;
import com.raqsoft.ide.dfx.etl.EtlConsts;
import com.raqsoft.ide.dfx.etl.FieldDefine;
import com.raqsoft.ide.dfx.etl.ObjectElement;
import com.raqsoft.ide.dfx.etl.ParamInfo;
import com.raqsoft.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 A.join()
 * 函数名前缀A表示序表
 * 
 * @author Joancy
 *
 */
public class AJoin extends ObjectElement {
	public ArrayList<String> foreignKeys;
	public String joinTable;
	public ArrayList<String> joinKeys;//连接的序表设置好主键，A.keys(keys)时，改项可以省略
	
	public ArrayList<FieldDefine> attachFields;//连接后附加在当前A后的表达式以及输出字段名，不使用FieldDefine的第三列
	
	public boolean i;
//	public boolean o;//先不实现o选项
	
	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(AJoin.class, this);

		paramInfos.add(new ParamInfo("foreignKeys",EtlConsts.INPUT_STRINGLIST));
		paramInfos.add(new ParamInfo("joinTable",EtlConsts.INPUT_CELLA,true));
		paramInfos.add(new ParamInfo("joinKeys",EtlConsts.INPUT_STRINGLIST));
		paramInfos.add(new ParamInfo("attachFields",EtlConsts.INPUT_FIELDDEFINE_EXP_FIELD));
		
		String group = "options";
		paramInfos.add(group, new ParamInfo("i", Consts.INPUT_CHECKBOX));

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
	public String optionString(){
		StringBuffer options = new StringBuffer();
		if(i){
			options.append("i");
		}
		return options.toString();
	}
	
	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "join";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append( getStringListExp(foreignKeys,":") );
		sb.append(",");
		sb.append(getExpressionExp(joinTable));
		
		String buf = getStringListExp(joinKeys,":");
		if(StringUtils.isValidString(buf)){
			sb.append(":");
			sb.append(buf);
		}

		
		buf = getFieldDefineExp(attachFields);
		if(!buf.isEmpty()){
			sb.append(",");
			sb.append( buf );
		}
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		StringTokenizer st = new StringTokenizer(funcBody,",");
		String tmp = st.nextToken();
		foreignKeys = getStringList(tmp,":");
		tmp = st.nextToken();
		int index = tmp.indexOf(":");
		joinTable = getExpression(tmp.substring(0,index));
		joinKeys = getStringList(tmp.substring(index+1),":");

		if(st.hasMoreTokens()){
			tmp = st.nextToken();
			attachFields = getFieldDefine(tmp);
		}
		return true;
	}

}
