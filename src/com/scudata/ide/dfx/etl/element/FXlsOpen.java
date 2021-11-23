package com.scudata.ide.dfx.etl.element;

import com.scudata.chart.Consts;
import com.scudata.common.StringUtils;
import com.scudata.ide.dfx.etl.EtlConsts;
import com.scudata.ide.dfx.etl.ObjectElement;
import com.scudata.ide.dfx.etl.ParamInfo;
import com.scudata.ide.dfx.etl.ParamInfoList;

/**
 * 辅助函数编辑 f.xlsopen()
 * 函数名前缀F表示文件对象
 * 
 * @author Joancy
 *
 */
public class FXlsOpen extends ObjectElement {
	public String password;

	public boolean r;
	public boolean w;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(FXlsOpen.class, this);

		paramInfos.add(new ParamInfo("password"));
		
		String group = "options";
		paramInfos.add(group, new ParamInfo("r", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("w", Consts.INPUT_CHECKBOX));

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
	 * @return EtlConsts.TYPE_XLS
	 */
	public byte getReturnType() {
		return EtlConsts.TYPE_XLS;
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName(){
		return "xlsopen";
	}
	
	/**
	 * 获取用于生成SPL表达式的选项串
	 */
	public String optionString(){
		StringBuffer options = new StringBuffer();
		if(r){//r,w互斥
			options.append("r");
		}else if(w){
			options.append("w");
		}
		return options.toString();
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		if(StringUtils.isValidString(password)){
			sb.append( getParamExp(password) );
		}
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		password = getParam( funcBody );
		return true;
	}

}
