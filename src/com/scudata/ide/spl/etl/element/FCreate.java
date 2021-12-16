package com.scudata.ide.spl.etl.element;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.scudata.chart.Consts;
import com.scudata.common.StringUtils;
import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.FieldDefine;
import com.scudata.ide.spl.etl.ObjectElement;
import com.scudata.ide.spl.etl.ParamInfo;
import com.scudata.ide.spl.etl.ParamInfoList;

/**
 * 辅助函数编辑 f.create()
 * 函数名前缀F表示文件对象
 * 
 * @author Joancy
 *
 */
public class FCreate extends ObjectElement {
	public ArrayList<FieldDefine> fields;
	public String x;
	
	public boolean u;
	public boolean r;
	public boolean y;
	public boolean p;
	
	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(FCreate.class, this);

		paramInfos.add(new ParamInfo("fields",EtlConsts.INPUT_FIELDDEFINE_FIELD_DIM));
		paramInfos.add(new ParamInfo("x"));
		
		String group = "options";
		paramInfos.add(group, new ParamInfo("u", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("r", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("y", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("p", Consts.INPUT_CHECKBOX));
		
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
		if(u){
			sb.append("u");
		}
		if(r){
			sb.append("r");
		}
		if(y){
			sb.append("y");
		}
		if(p){
			sb.append("p");
		}
		return sb.toString();
	}

//	private String toPwd(){
//		StringBuffer sb = new StringBuffer();
//		if(StringUtils.isValidString(writePassword)){
//			sb.append(getParamExp(writePassword));
//		}
//		if(StringUtils.isValidString(readPassword)){
//			sb.append(":");
//			sb.append(getParamExp(readPassword));
//		}
//		return sb.toString();
//	}
	
//	private void setPwd(String buf){
//		StringTokenizer st = new StringTokenizer(buf,":");
//		String tmp;
//		if(st.hasMoreTokens()){
//			tmp = st.nextToken();
//			if(isValidString(tmp)){
//				writePassword = getParam( tmp );
//			}
//		}
//		if(st.hasMoreTokens()){
//			tmp = st.nextToken();
//			if(isValidString(tmp)){
//				readPassword = getParam( tmp );
//			}
//		}
//	}
	
	private String getFieldsExp(){
		if(fields==null || fields.isEmpty()){
			return "";
		}
		StringBuffer sb = new StringBuffer();
		if(fields!=null){
			for(FieldDefine fd:fields){
				if(sb.length()>0){
					sb.append(",");
				}
				
				if(StringUtils.isValidString(fd.getTwo())){
					if(Boolean.parseBoolean(fd.getTwo())){
						sb.append("#");
					}
				}
				sb.append(fd.getOne());
			}
		}
		return sb.toString();
		
	}
	private void setFields(String buf){
		fields = new ArrayList<FieldDefine>();
		StringTokenizer st = new StringTokenizer(buf,",");
		while( st.hasMoreTokens() ){
			FieldDefine fd = new FieldDefine();
			String tmp = st.nextToken();
			if(tmp.startsWith("#")){
				fd.setTwo("true");
				fd.setOne(tmp.substring(1));
			}else{
				fd.setOne(tmp);
			}
			fields.add(fd);
		}
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "create";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append(getFieldsExp());
		
		if(StringUtils.isValidString(x)){
			sb.append(";");
			sb.append( x );
		}
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		StringTokenizer st = new StringTokenizer(funcBody,";");
		if(st.hasMoreTokens()){
			setFields( st.nextToken() );
		}
		if(st.hasMoreTokens()){
			x = st.nextToken();
		}
		return true;
	}


}
