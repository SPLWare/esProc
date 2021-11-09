package com.raqsoft.ide.common.function;

/**
 * 正在编辑的文本
 *
 */
public class EditingText {

	/** 普通文本 */
	public static String STYLE_NORMAL = "N";
	/** 高亮文本 */
	public static String STYLE_HIGHLIGHT = "H";
	/** 选择文本 */
	public static String STYLE_SELECTED = "S";

	/**
	 * 文本
	 */
	private String text;
	/**
	 * 样式
	 */
	private String style = STYLE_NORMAL;

	/**
	 * 构造函数
	 * 
	 * @param text
	 *            文本
	 */
	public EditingText(String text) {
		this.text = text;
	}

	/**
	 * 构造函数
	 * 
	 * @param text
	 *            文本
	 * @param style
	 *            样式
	 */
	public EditingText(String text, String style) {
		this.text = text;
		this.style = style;
	}

	/**
	 * 取文本
	 * 
	 * @return
	 */
	public String getText() {
		return text;
	}

	/**
	 * 设置样式
	 * 
	 * @param style
	 */
	public void setStyle(String style) {
		this.style = style;
	}

	/**
	 * 取样式
	 * 
	 * @return
	 */
	public String getStyle() {
		return style;
	}
}
