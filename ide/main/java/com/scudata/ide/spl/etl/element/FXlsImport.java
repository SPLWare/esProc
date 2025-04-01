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
 * 辅助函数编辑 f.xlsimport()
 * 函数名前缀F表示文件对象
 * 
 * @author Joancy
 *
 */
public class FXlsImport extends ObjectElement {
	public ArrayList<String> fields;
	public String sheetName;
	
	public String argb,arge;

	public String password;

	public boolean t;
	public boolean c;
	public boolean b;
	public boolean w;
	public boolean p;
	public boolean n;
	public boolean s;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(FXlsImport.class, this);

		paramInfos.add(new ParamInfo("fields", EtlConsts.INPUT_STRINGLIST));
		paramInfos.add(new ParamInfo("sheetName"));
		paramInfos.add(new ParamInfo("argb"));
		paramInfos.add(new ParamInfo("arge"));
		paramInfos.add(new ParamInfo("password"));
		
		String group = "options";
		paramInfos.add(group, new ParamInfo("t", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("c", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("b", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("w", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("p", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("n", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("s", Consts.INPUT_CHECKBOX));

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
	 * @return 根据选项返回相应类型
	 */
	public byte getReturnType() {
		if(c){
			return EtlConsts.TYPE_CURSOR;
		}
		if(s){
			return EtlConsts.TYPE_STRING;
		}
		return EtlConsts.TYPE_SEQUENCE;
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName(){
		return "xlsimport";
	}
	
	/**
	 * 获取用于生成SPL表达式的选项串
	 */
	public String optionString(){
		StringBuffer options = new StringBuffer();
		if(t){
			options.append("t");
		}
		if(c){
			options.append("c");
		}else if(b){
			options.append("b");//@c时不支持
		}
		boolean existTCB = t|c|b;
		if(!existTCB & w){
			options.append("w");//与@t@c@b互斥
			if(p){
				options.append("p");//w时的子项
			}
		}
		if(n){
			options.append("n");
		}
		if(s){
			options.append("s");
		}
		
		return options.toString();
	}
	
	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		String sfields = getStringListExp(fields,",");
		if(StringUtils.isValidString(sfields)){
			sb.append(sfields);
		}
		
		StringBuffer suffix = new StringBuffer();
		if(StringUtils.isValidString(sheetName)){
			suffix.append( getParamExp(sheetName) );
		}
		
		String be="";
		if(StringUtils.isValidString(argb)){
			be = getNumberExp(argb);
		}
		if(StringUtils.isValidString(arge)){
			be += ":";
			be += getNumberExp(arge);
		}
		
		if(StringUtils.isValidString(be)){
			suffix.append(",");
			suffix.append(be);
		}
		
		if(StringUtils.isValidString(password)){
			suffix.append(";");
			suffix.append(getParamExp(password));
		}

		if(suffix.length()>0){
			sb.append(";");
			sb.append(suffix.toString());
		}

		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody( String funcBody ) {
		StringTokenizer st = new StringTokenizer(funcBody,";");
		fields = getStringList(st.nextToken(),",");
		String buf;
		if(st.hasMoreTokens()){
			buf = st.nextToken();
			if(isValidString(buf)){
				StringTokenizer st2 = new StringTokenizer(buf,",");
				sheetName = getParam(st2.nextToken());
				if(st2.hasMoreTokens()){
					buf = st2.nextToken();
					st2 = new StringTokenizer(buf,":");
					buf = st2.nextToken();
					if(isValidString(buf)){
						argb = getNumber( buf );
					}
					if(st2.hasMoreTokens()){
						arge = getNumber(st2.nextToken());
					}
				}
			}
		}
		
		if( st.hasMoreTokens()){
			buf = st.nextToken();
			password = getParam( buf );
		}
		return true;
	}

}
