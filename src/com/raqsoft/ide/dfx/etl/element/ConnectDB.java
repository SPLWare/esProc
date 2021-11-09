package com.raqsoft.ide.dfx.etl.element;

import com.raqsoft.chart.Consts;
import com.raqsoft.common.StringUtils;
import com.raqsoft.ide.dfx.etl.EtlConsts;
import com.raqsoft.ide.dfx.etl.ObjectElement;
import com.raqsoft.ide.dfx.etl.ParamInfo;
import com.raqsoft.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 connect(db)
 * 
 * @author Joancy
 *
 */
public class ConnectDB extends ObjectElement {
	public String db;
	public String level;
	
	public boolean l;
	public boolean e;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(ConnectDB.class, this);

		paramInfos.add(new ParamInfo("db",EtlConsts.INPUT_DB));
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
		if(!StringUtils.isValidString(db)){//没有指定db时，用于文件连接，此时选项没意义
			return null;
		}
		
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
		if(!StringUtils.isValidString(db)){
			return null;
		}
		return getParamExp(db);
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
//		为了跟ConnectDriver区别，只要包含有逗号，就不是当前函数
		if(funcBody.indexOf(",")>0){
			return false;
		}
		db = getParam(funcBody);
		return true;
	}

}
