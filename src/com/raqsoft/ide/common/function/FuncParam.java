package com.raqsoft.ide.common.function;

import java.util.ArrayList;

import com.raqsoft.common.ICloneable;

/**
 * 函数参数
 *
 */
public class FuncParam implements ICloneable {
	/**
	 * 说明
	 */
	String desc;
	/**
	 * 前导符
	 */
	char preSign;
	/**
	 * 是否子参数
	 */
	boolean isSubParam;
	/**
	 * 是否可重复
	 */
	boolean isRepeatable;
	/**
	 * 是否标识符
	 */
	boolean isIdentifierOnly;
	/**
	 * 选择指向类型
	 */
	byte filterType = FuncConst.FILTER_NULL;
	/**
	 * 选项列表
	 */
	ArrayList<FuncOption> options = null;

	/**
	 * 参数值
	 */
	transient String paramValue = "";

	/**
	 * 构造函数
	 */
	public FuncParam() {
	}

	/**
	 * 设置说明
	 * 
	 * @param desc
	 */
	public void setDesc(String desc) {
		this.desc = desc;
	}

	/**
	 * 取说明
	 * 
	 * @return
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * 设置前导符
	 * 
	 * @param sign
	 */
	public void setPreSign(char sign) {
		preSign = sign;
	}

	/**
	 * 取前导符
	 * 
	 * @return
	 */
	public char getPreSign() {
		return preSign;
	}

	/**
	 * 设置是否子参数
	 * 
	 * @param isSub
	 */
	public void setSubParam(boolean isSub) {
		isSubParam = isSub;
	}

	/**
	 * 取是否子参数
	 * 
	 * @return
	 */
	public boolean isSubParam() {
		return isSubParam;
	}

	/**
	 * 设置是否可重复
	 * 
	 * @param repeatable
	 */
	public void setRepeatable(boolean repeatable) {
		isRepeatable = repeatable;
	}

	/**
	 * 取是否可重复
	 */
	public boolean isRepeatable() {
		return isRepeatable;
	}

	/**
	 * 设置是否标识符
	 * 
	 * @param identifierOnly
	 */
	public void setIdentifierOnly(boolean identifierOnly) {
		isIdentifierOnly = identifierOnly;
	}

	/**
	 * 取是否标识符
	 * 
	 * @return
	 */
	public boolean isIdentifierOnly() {
		return isIdentifierOnly;
	}

	/**
	 * 设置选择指向类型
	 * 
	 * @param type
	 */
	public void setFilterType(byte type) {
		filterType = type;
	}

	/**
	 * 取选择指向类型
	 * 
	 * @return
	 */
	public byte getFilterType() {
		return filterType;
	}

	/**
	 * 设置参数值
	 * 
	 * @param value
	 */
	public void setParamValue(String value) {
		paramValue = value;
	}

	/**
	 * 取参数值
	 * 
	 * @return
	 */
	public String getParamValue() {
		return paramValue;
	}

	/**
	 * 设置选项列表
	 * 
	 * @param options
	 */
	public void setOptions(ArrayList<FuncOption> options) {
		this.options = options;
	}

	/**
	 * 取选项列表
	 * 
	 * @return
	 */
	public ArrayList<FuncOption> getOptions() {
		return options;
	}

	/**
	 * 克隆
	 */
	public Object deepClone() {
		FuncParam fp = new FuncParam();
		fp.setDesc(desc);
		fp.setPreSign(preSign);
		fp.setSubParam(isSubParam);
		fp.setRepeatable(isRepeatable);
		fp.setIdentifierOnly(isIdentifierOnly);
		fp.setFilterType(filterType);
		if (options != null) {
			ArrayList<FuncOption> cloneOptions = new ArrayList<FuncOption>();
			for (int i = 0; i < options.size(); i++) {
				cloneOptions.add((FuncOption) options.get(i).deepClone());
			}
			fp.setOptions(cloneOptions);
		}
		return fp;
	}
}
