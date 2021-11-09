package com.raqsoft.ide.dfx.etl.element;

import java.util.ArrayList;
import com.raqsoft.chart.Consts;
import com.raqsoft.common.StringUtils;
import com.raqsoft.ide.dfx.etl.EtlConsts;
import com.raqsoft.ide.dfx.etl.ObjectElement;
import com.raqsoft.ide.dfx.etl.ParamInfo;
import com.raqsoft.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 db.execute()
 * 函数名前缀D表示数据库连接
 * 
 * @author Joancy
 *
 */
public class DExecute extends ObjectElement {
	public String aOrCs;
	public String sql;
	
	public ArrayList<String> args;
	
	public boolean k;
	public boolean s;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(DExecute.class, this);

		paramInfos.add(new ParamInfo("aOrCs", EtlConsts.INPUT_CELLAORCS));
		paramInfos.add(new ParamInfo("sql",true));
		paramInfos.add(new ParamInfo("args", EtlConsts.INPUT_STRINGLIST));
		String group = "options";
		paramInfos.add(group, new ParamInfo("k", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("s", Consts.INPUT_CHECKBOX));

		return paramInfos;
	}

	/**
	 * 获取父类型
	 * 类型的常量定义为
	 * EtlConsts.TYPE_XXX
	 * @return EtlConsts.TYPE_DB
	 */
	public byte getParentType() {
		return EtlConsts.TYPE_DB;
	}

	/**
	 * 获取该函数的返回类型
	 * @return EtlConsts.TYPE_EMPTY
	 */
	public byte getReturnType() {
		return EtlConsts.TYPE_EMPTY;
	}

	/**
	 * 获取用于生成SPL表达式的选项串
	 */
	public String optionString(){
		StringBuffer options = new StringBuffer();
		if(k){
			options.append("k");
		}
		if(s){
			options.append("s");
		}
		return options.toString();
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "execute";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		if(StringUtils.isValidString(aOrCs)){
			sb.append(aOrCs);	
			sb.append(",");	
		}
		sb.append(getParamExp(sql));
		String argStr = getStringListExp(args,",");
		if(!argStr.isEmpty()){
			sb.append(",");
			sb.append(argStr);	
		}
		return sb.toString();
	}

	private boolean isSql(String var){
		var = var.toUpperCase();
		if(var.startsWith("SELECT ")){
			return true;
		}
		if(var.startsWith("UPDATE ")){
			return true;
		}
		if(var.startsWith("INSERT ")){
			return true;
		}
		if(var.startsWith("DELETE ")){
			return true;
		}
		return false;
	}
	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		int index = funcBody.indexOf(",");
		if(index<0){
			sql = getParam(funcBody);
			return true;
		}
		
		String buf = funcBody.substring(0,index);
		if( isSql(buf) ){
			sql = getParam( buf );
		}else{
			aOrCs = buf;
		}
		buf = funcBody.substring(index+1);
		
		if(StringUtils.isValidString(buf)){
			args = getStringList(buf,",");
		}
		return true;
	}
}
