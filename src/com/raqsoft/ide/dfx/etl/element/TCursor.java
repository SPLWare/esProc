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
 * 辅助函数编辑 t.close()
 * 函数名前缀T表示CTX实表
 * 
 * @author Joancy
 *
 */
public class TCursor extends ObjectElement {
	public ArrayList<FieldDefine> fields;
	public String where;
	public String argk;
	public String argn;
	public String cursorN;
	
	public boolean m;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(TCursor.class, this);

		paramInfos.add(new ParamInfo("fields", EtlConsts.INPUT_FIELDDEFINE_EXP_FIELD));
		paramInfos.add(new ParamInfo("where",EtlConsts.INPUT_ONLYPROPERTY));
		paramInfos.add(new ParamInfo("argk"));
		paramInfos.add(new ParamInfo("argn"));
		paramInfos.add(new ParamInfo("cursorN"));
		
		String group = "options";
		paramInfos.add(group, new ParamInfo("m", Consts.INPUT_CHECKBOX));

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
		if(m){
			options.append("m");
		}
		return options.toString();
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "cursor";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append(getFieldDefineExp(fields));
		
		StringBuffer bufKN = new StringBuffer();
		if(m){
			if(StringUtils.isValidString(cursorN)){
				bufKN.append(getNumberExp(cursorN));	
			}
		}else{
			if(StringUtils.isValidString(argk)){
				bufKN.append(getNumberExp(argk));
				bufKN.append(":");
				bufKN.append(getNumberExp(argn));
			}
		}
		if(bufKN.length()>0){
			sb.append(";");
			if(StringUtils.isValidString(where)){
				sb.append(where);
			}
			sb.append(";");
			sb.append(bufKN.toString());
		}else{
			if(StringUtils.isValidString(where)){
				sb.append(";");
				sb.append(where);
			}
		}
		
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		StringTokenizer st = new StringTokenizer(funcBody,";");
		fields = getFieldDefine(st.nextToken());
		String buf;
		if(st.hasMoreTokens()){
			buf = st.nextToken();
			if(isValidString(buf)){
				where = buf;
			}
		}
		if(st.hasMoreTokens()){
			buf = st.nextToken();
			int maohao = buf.indexOf(":");
			if(maohao>0){
				m = false;
				argk = getNumber(buf.substring(0,maohao));
				argn = getNumber(buf.substring(maohao+1));
			}else{
				m = true;
				cursorN = getNumber(buf);
			}
		}
		return true;
	}


}
