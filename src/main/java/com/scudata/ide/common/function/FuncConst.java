package com.scudata.ide.common.function;

/**
 * 函数常量
 */
public class FuncConst {

	/** 未知。计算不出时，函数返回值不确定。 */
	public static final byte FILTER_NULL = 0;
	/** 主对象的字段 */
	public static final byte FILTER_MAJOR_FIELD = 1;
	/** 第一个参数的字段 */
	public static final byte FILTER_FIRSTPARA_FIELD = 2;
	/** 常数集 */
	public static final byte FILTER_SORT = 3;

	/**
	 * 选择指向类型，值为以上常量
	 */
	private byte funcFilterId;
	/**
	 * 值
	 */
	private String value;
	/**
	 * 标题
	 */
	private String title;

	/**
	 * 构造函数
	 * 
	 * @param funcFilterId
	 *            选择指向类型
	 * @param value
	 *            值
	 * @param title
	 *            标题
	 */
	public FuncConst(byte funcFilterId, String value, String title) {
		this.funcFilterId = funcFilterId;
		this.value = value;
		this.title = title;
	}

	/**
	 * 取选择指向类型
	 * 
	 * @return
	 */
	public byte getFuncFilterId() {
		return funcFilterId;
	}

	/**
	 * 取值
	 * 
	 * @return
	 */
	public String getValue() {
		return value;
	}

	/**
	 * 取标题
	 * 
	 * @return
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * 取常数集
	 * 
	 * @return
	 */
	public static FuncConst[] listAllConsts() {
		return null;
	}
}
