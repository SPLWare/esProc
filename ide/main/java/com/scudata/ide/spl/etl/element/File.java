package com.scudata.ide.spl.etl.element;

import com.scudata.chart.Consts;
import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.StringUtils;
import com.scudata.ide.spl.etl.EtlConsts;
import com.scudata.ide.spl.etl.ObjectElement;
import com.scudata.ide.spl.etl.ParamInfo;
import com.scudata.ide.spl.etl.ParamInfoList;

/**
 * 辅助函数编辑 file()
 * 
 * @author Joancy
 *
 */
public class File extends ObjectElement {
	public String fn;

	public String cs;

	public boolean s;

	/**
	 * 获取用于界面编辑的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(File.class, this);

		paramInfos.add(new ParamInfo("fn", Consts.INPUT_FILE,true));
		paramInfos.add(new ParamInfo("cs", Consts.INPUT_CHARSET));
		
		String group = "options";
		paramInfos.add(group, new ParamInfo("s", Consts.INPUT_CHECKBOX));

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
	 * @return EtlConsts.TYPE_FILE
	 */
	public byte getReturnType() {
		return EtlConsts.TYPE_FILE;
	}


	/**
	 * 获取用于生成SPL表达式的选项串
	 */
	public String optionString(){
		if(s){
			return "s";
		}
		return "";
	}

	/**
	 * 获取用于生成SPL表达式的函数名
	 */
	public String getFuncName() {
		return "file";
	}

	/**
	 * 获取用于生成SPL表达式的函数体
	 * 跟setFuncBody是逆函数，然后表达式的赋值也总是互逆的
	 */
	public String getFuncBody() {
		StringBuffer sb = new StringBuffer();
		sb.append(getParamExp(fn));
		if(StringUtils.isValidString(cs)){
			sb.append(":");
			sb.append(getParamExp(cs));
		}
		return sb.toString();
	}

	/**
	 * 设置函数体
	 * @param funcBody 函数体
	 */
	public boolean setFuncBody(String funcBody) {
		ArgumentTokenizer at = new ArgumentTokenizer(funcBody,':');
		fn = getParam(at.nextToken());
		if(at.hasMoreTokens()){
			cs = getParam(at.nextToken());
		}
		return true;
	}

}
