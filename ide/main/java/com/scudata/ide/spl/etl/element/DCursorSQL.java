package com.scudata.ide.spl.etl.element;

import java.util.ArrayList;

import com.scudata.chart.Consts;
import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.ObjectElement;
import com.scudata.ide.spl.etl.ParamInfo;
import com.scudata.ide.spl.etl.ParamInfoList;

/**
 * 辅助函数编辑 db.cursor()
 * 函数名前缀D表示数据库连接
 * 
 * @author Joancy
 *
 */
public class DCursorSQL extends ObjectElement {
	public String sql;
	
	public ArrayList<String> args;
	
	public boolean i;
	public boolean d;
	public boolean x;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(DCursorSQL.class, this);

		paramInfos.add(new ParamInfo("sql",true));
		paramInfos.add(new ParamInfo("args", EtlConsts.INPUT_STRINGLIST));
		String group = "options";
		paramInfos.add(group, new ParamInfo("i", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("d", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("x", Consts.INPUT_CHECKBOX));

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
		if(i){
			options.append("i");
		}
		if(d){
			options.append("d");
		}
		if(x){
			options.append("x");
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
		sb.append(getParamExp(sql));
		String argStr = getStringListExp(args,",");
		if(!argStr.isEmpty()){
			sb.append(",");
			sb.append(argStr);	
		}
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		int index = funcBody.indexOf(",");
		if(index>0){
			sql = getParam(funcBody.substring(0,index));
			String tmp = funcBody.substring(index+1);
			args = getStringList(tmp,",");
		}else{
			sql = getParam(funcBody);
		}
		return true;
	}

}
