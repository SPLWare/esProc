package com.raqsoft.ide.common.function;

import com.raqsoft.common.ICloneable;

/**
 * 函数选项
 *
 */
public class FuncOption implements ICloneable {
	/**
	 * 选项字符
	 */
	String optionChar;
	/**
	 * 描述
	 */
	String description;
	/**
	 * 是否缺省选中
	 */
	boolean defaultSelect;

	/**
	 * 是否选中
	 */
	transient boolean select;

	/**
	 * 构造函数
	 */
	public FuncOption() {
	}

	/**
	 * 设置选项字符
	 * 
	 * @param c
	 */
	public void setOptionChar(String c) {
		optionChar = c;
	}

	/**
	 * 取选项字符
	 * 
	 * @return
	 */
	public String getOptionChar() {
		return optionChar;
	}

	/**
	 * 设置描述
	 * 
	 * @param desc
	 */
	public void setDescription(String desc) {
		description = desc;
	}

	/**
	 * 取描述
	 * 
	 * @return
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * 设置缺省选中
	 * 
	 * @param select
	 */
	public void setDefaultSelect(boolean select) {
		this.defaultSelect = select;
	}

	/**
	 * 取缺省选中
	 * 
	 * @return
	 */
	public boolean isDefaultSelect() {
		return defaultSelect;
	}

	/**
	 * 设置是否选中
	 * 
	 * @param select
	 */
	public void setSelect(boolean select) {
		this.select = select;
	}

	/**
	 * 取是否选中
	 * 
	 * @return
	 */
	public boolean isSelect() {
		return select;
	}

	/**
	 * deepClone
	 * 
	 * @return Object
	 */
	public Object deepClone() {
		FuncOption fo = new FuncOption();
		fo.setOptionChar(optionChar);
		fo.setDescription(description);
		fo.setDefaultSelect(defaultSelect);
		return fo;
	}
}
