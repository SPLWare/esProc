package com.scudata.ide.common.function;

import java.util.ArrayList;

import com.scudata.common.ICloneable;
import com.scudata.expression.Expression;

/**
 * 函数信息
 *
 */
public class FuncInfo implements ICloneable {
	/**
	 * 函数名称
	 */
	String name;

	/**
	 * 函数描述
	 */
	String desc;

	/**
	 * 函数显示的字符串
	 */
	String displayStr;

	/**
	 * 后缀，用来区分同名函数。
	 */
	String postfix;

	/**
	 * 主对象类型
	 */
	byte majorType = Expression.TYPE_UNKNOWN;

	/**
	 * 返回值类型
	 */
	byte returnType = Expression.TYPE_UNKNOWN;

	/**
	 * 函数选项
	 */
	ArrayList<FuncOption> options = null;

	/**
	 * 参数配置
	 */
	ArrayList<FuncParam> params = null;

	/**
	 * 构造函数
	 */
	public FuncInfo() {
	}

	/**
	 * 设置函数名称
	 * 
	 * @param name
	 *            函数名称
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 取函数名称
	 * 
	 * @return 函数名称
	 */
	public String getName() {
		return name;
	}

	/**
	 * 设置函数描述
	 * 
	 * @param desc
	 *            函数描述
	 */
	public void setDesc(String desc) {
		this.desc = desc;
	}

	/**
	 * 取函数描述
	 * 
	 * @return 函数描述
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * 设置函数显示的字符串
	 * 
	 * @param displayStr
	 *            函数显示的字符串
	 */
	public void setDisplayStr(String displayStr) {
		this.displayStr = displayStr;
	}

	/**
	 * 取函数显示的字符串
	 * 
	 * @return 函数显示的字符串
	 */
	public String getDisplayStr() {
		return displayStr;
	}

	/**
	 * 设置后缀，用来区分同名函数。
	 * 
	 * @param postfix
	 */
	public void setPostfix(String postfix) {
		this.postfix = postfix;
	}

	/**
	 * 取后缀，用来区分同名函数。
	 * 
	 * @return
	 */
	public String getPostfix() {
		return postfix;
	}

	/**
	 * 设置主对象类型
	 * 
	 * @param type
	 */
	public void setMajorType(byte type) {
		majorType = type;
	}

	/**
	 * 取主对象类型
	 * 
	 * @return
	 */
	public byte getMajorType() {
		return majorType;
	}

	/**
	 * 设置返回值类型
	 * 
	 * @param type
	 */
	public void setReturnType(byte type) {
		returnType = type;
	}

	/**
	 * 取返回值类型
	 * 
	 * @return
	 */
	public byte getReturnType() {
		return returnType;
	}

	/**
	 * 设置函数选项
	 * 
	 * @param options
	 */
	public void setOptions(ArrayList<FuncOption> options) {
		this.options = options;
	}

	/**
	 * 取函数选项
	 * 
	 * @return
	 */
	public ArrayList<FuncOption> getOptions() {
		return options;
	}

	/**
	 * 设置参数配置
	 * 
	 * @param params
	 */
	public void setParams(ArrayList<FuncParam> params) {
		this.params = params;
	}

	/**
	 * 取参数配置
	 * 
	 * @return
	 */
	public ArrayList<FuncParam> getParams() {
		return params;
	}

	/**
	 * deepClone
	 *
	 * @return Object
	 */
	public Object deepClone() {
		FuncInfo fi = new FuncInfo();
		fi.setName(name);
		fi.setDesc(desc);
		fi.setDisplayStr(displayStr);
		fi.setPostfix(postfix);
		fi.setMajorType(majorType);
		fi.setReturnType(returnType);
		if (options != null) {
			ArrayList<FuncOption> cloneOptions = new ArrayList<FuncOption>(
					options.size());
			for (int i = 0; i < options.size(); i++) {
				FuncOption fo = options.get(i);
				cloneOptions.add((FuncOption) fo.deepClone());
			}
			fi.setOptions(cloneOptions);
		}
		if (params != null) {
			ArrayList<FuncParam> cloneParams = new ArrayList<FuncParam>(
					params.size());
			for (int i = 0; i < params.size(); i++) {
				FuncParam fp = params.get(i);
				cloneParams.add((FuncParam) fp.deepClone());
			}
			fi.setParams(cloneParams);
		}
		return fi;
	}
}
