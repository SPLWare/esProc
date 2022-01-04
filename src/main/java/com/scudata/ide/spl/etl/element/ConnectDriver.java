package com.scudata.ide.spl.etl.element;

import java.util.StringTokenizer;

import com.scudata.chart.Consts;
import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.ObjectElement;
import com.scudata.ide.spl.etl.ParamInfo;
import com.scudata.ide.spl.etl.ParamInfoList;

/**
 * 辅助函数编辑 connect(driver,url)
 * 
 * @author Joancy
 *
 */
public class ConnectDriver extends ObjectElement {
	public String driver;
	public String URL;
	public String level;
	
	public boolean l;
	public boolean e;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(ConnectDriver.class, this);

		paramInfos.add(new ParamInfo("driver",true));
		paramInfos.add(new ParamInfo("URL",true));
		paramInfos.add(new ParamInfo("level",EtlConsts.INPUT_ISOLATIONLEVEL));
		String group = "options";
		paramInfos.add(group, new ParamInfo("l", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("e", Consts.INPUT_CHECKBOX));

		return paramInfos;
	}


	/**
	 * 获取父类型
	 * 类型的常量定义为
	 * EtlConsts.TYPE_XXX
	 * @return EtlConsts.TYPE_EMPTY
	 */
	public byte getParentType() {
		return EtlConsts.TYPE_EMPTY;
	}

	/**
	 * 获取该函数的返回类型
	 * @return EtlConsts.TYPE_DB
	 */
	public byte getReturnType() {
		return EtlConsts.TYPE_DB;
	}

	/**
	 * 获取用于生成SPL表达式的选项串
	 */
	public String optionString(){
		StringBuffer sb = new StringBuffer();
		if(e){
			sb.append("e");
		}
		if(l){
			sb.append("l");
		}
		if(level!=null){
			sb.append(level);
		}
		return sb.toString();
	}
	
	/**
	 * 覆盖父类的设置函数的选项
	 * 该类中的level是用字符描述的隔离级别，跟boolean选项值不通用，所以需要特殊处理
	 * @param options 选项
	 */
	public void setOptions(String options){
		if(options==null){
			return;
		}
		boolean isE = options.indexOf("e")>-1;
		if(isE){
			e = true;
			options = options.replace('e', ' ');
		}
		boolean isL = options.indexOf("l")>-1;
		if(isL){
			l = true;
			options = options.replace('l', ' ');
		}
		level = options.trim();
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "connect";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append(getParamExp(driver));
		sb.append(",");
		sb.append(getParamExp(URL));
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		StringTokenizer st = new StringTokenizer(funcBody,",");
		driver = getParam( st.nextToken() );
		URL = getParam( st.nextToken() );
		return true;
	}
}
