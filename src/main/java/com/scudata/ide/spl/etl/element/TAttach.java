package com.scudata.ide.spl.etl.element;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.scudata.common.StringUtils;
import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.FieldDefine;
import com.scudata.ide.spl.etl.ObjectElement;
import com.scudata.ide.spl.etl.ParamInfo;
import com.scudata.ide.spl.etl.ParamInfoList;

/**
 * 辅助函数编辑 t.attach()
 * 函数名前缀T表示CTX实表
 * 
 * @author Joancy
 *
 */
public class TAttach extends ObjectElement {
	public String tableName;	
	public ArrayList<FieldDefine> fields;
	

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(TAttach.class, this);

		paramInfos.add(new ParamInfo("tableName",true));
		paramInfos.add(new ParamInfo("fields",EtlConsts.INPUT_FIELDDEFINE_FIELD_DIM));
		
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
	 * @return EtlConsts.TYPE_SEQUENCE
	 */
	public byte getReturnType() {
		return EtlConsts.TYPE_SEQUENCE;
	}

	
	/**
	 * 获取用于生成SPL表达式的选项串
	 */
	public String optionString(){
		return "";
	}

	private String getFieldsExp(){
		if(fields==null || fields.isEmpty()){
			return "";
		}
		StringBuffer sb = new StringBuffer();
		if(fields!=null){
			for(FieldDefine fd:fields){
				sb.append(",");
				
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
	
	private void getFields(String fieldstr){
		fields = new ArrayList<FieldDefine>();
		StringTokenizer st = new StringTokenizer(fieldstr,",");
		while( st.hasMoreTokens()){
			FieldDefine fd = new FieldDefine();
			String buf = st.nextToken();
			if(buf.startsWith("#")){
				fd.setTwo("true");
				buf = buf.substring(1);
			}
			fd.setOne(buf);
			fields.add(fd);
		}
	}
	
	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "attach";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append(getExpressionExp(tableName));
		sb.append(getFieldsExp());
		
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		StringTokenizer st = new StringTokenizer(funcBody,",");
		tableName = getExpression(st.nextToken());
		if(st.hasMoreTokens()){
			String buf = st.nextToken(";");//使用不同的符号，取出后面所有逗号串
			getFields(buf);
		}
		return true;
	}


}
