package com.scudata.ide.common.function;

import java.util.ArrayList;

import javax.swing.text.JTextComponent;

import com.scudata.common.StringUtils;
import com.scudata.expression.Expression;

/**
 * 正在编辑的函数信息
 *
 */
public class EditingFuncInfo {
	/**
	 * 函数名称
	 */
	private String funcName;
	/**
	 * 函数选项
	 */
	private String funcOption;
	/**
	 * 正在编辑的函数参数
	 */
	private EditingFuncParam funcParam;
	/**
	 * 主对象类型
	 */
	private byte majorType = Expression.TYPE_UNKNOWN;
	/**
	 * 主对象值
	 */
	private Object majorValue;
	/**
	 * 函数信息
	 */
	private FuncInfo funcInfo;
	/**
	 * 编辑框
	 */
	private JTextComponent editor;
	/**
	 * 旧表达式
	 */
	private String oldExp;
	/**
	 * 函数起始点、结束点
	 */
	private int funcStart, funcEnd;

	/**
	 * 构造函数
	 * 
	 * @param editor
	 *            编辑框
	 * @param funcName
	 *            函数名
	 * @param funcOption
	 *            函数选项
	 * @param funcParam
	 *            正在编辑的函数参数
	 * @param funcStart
	 *            函数起始点
	 * @param funcEnd
	 *            函数结束点
	 */
	public EditingFuncInfo(JTextComponent editor, String funcName,
			String funcOption, EditingFuncParam funcParam, int funcStart,
			int funcEnd) {
		this.editor = editor;
		oldExp = editor.getText();
		this.funcName = funcName;
		this.funcOption = funcOption;
		this.funcParam = funcParam;
		this.funcStart = funcStart;
		this.funcEnd = funcEnd;
	}

	/**
	 * 设置函数名
	 * 
	 * @param funcName
	 */
	public void setFuncName(String funcName) {
		this.funcName = funcName;
	}

	/**
	 * 取函数名
	 * 
	 * @return
	 */
	public String getFuncName() {
		return funcName;
	}

	/**
	 * 设置函数选项
	 * 
	 * @param funcOption
	 */
	public void setFuncOption(String funcOption) {
		this.funcOption = funcOption;
		refreshEditor();
	}

	/**
	 * 取函数选项
	 * 
	 * @return
	 */
	public String getFuncOption() {
		return funcOption;
	}

	/**
	 * 设置正在编辑的函数参数
	 * 
	 * @param funcParam
	 */
	public void setFuncParam(EditingFuncParam funcParam) {
		this.funcParam = funcParam;
		refreshEditor();
	}

	/**
	 * 取正在编辑的函数参数
	 * 
	 * @return
	 */
	public EditingFuncParam getFuncParam() {
		return funcParam;
	}

	/**
	 * 设置主对象类型
	 * 
	 * @param type
	 */
	public void setMajorType(byte type) {
		this.majorType = type;
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
	 * 设置主对象值
	 * 
	 * @param value
	 */
	public void setMajorValue(Object value) {
		this.majorValue = value;
	}

	/**
	 * 取主对象值
	 * 
	 * @return
	 */
	public Object getMajorValue() {
		return majorValue;
	}

	/**
	 * 设置函数信息
	 * 
	 * @param funcInfo
	 */
	public void setFuncInfo(FuncInfo funcInfo) {
		this.funcInfo = funcInfo;
	}

	/**
	 * 取函数信息
	 * 
	 * @return
	 */
	public FuncInfo getFuncInfo() {
		return funcInfo;
	}

	/**
	 * 取函数起始点
	 * 
	 * @return
	 */
	public int getFuncStart() {
		return funcStart;
	}

	/**
	 * 取函数选项起始点
	 * 
	 * @return
	 */
	public int getFuncOptionStart() {
		return funcStart + funcName.length() + 1;
	}

	/**
	 * 取函数参数起始点
	 * 
	 * @return
	 */
	public int getFuncParamStart() {
		return funcStart + getPreFuncString().length() + 1;
	}

	/**
	 * 取函数括号前的字符串
	 * 
	 * @return
	 */
	private StringBuffer getPreFuncString() {
		StringBuffer sb = new StringBuffer();
		sb.append(funcName);
		if (StringUtils.isValidString(funcOption)) {
			sb.append("@");
			sb.append(funcOption);
		}
		return sb;
	}

	/**
	 * 转字符串
	 */
	public String toString() {
		StringBuffer sb = getPreFuncString();
		if (StringUtils.isValidString(funcParam)) {
			sb.append("(");
			sb.append(funcParam);
			sb.append(")");
		} else {
			sb.append("()");
		}

		return sb.toString();
	}

	/**
	 * 追加编辑的文本
	 * 
	 * @param container
	 */
	public void appendEditingText(ArrayList<EditingText> container) {
		container.add(new EditingText(getPreFuncString().toString()));
		if (funcParam != null) {
			container.add(new EditingText("(", EditingText.STYLE_HIGHLIGHT));
			funcParam.appendEditingText(container);
			container.add(new EditingText(")", EditingText.STYLE_HIGHLIGHT));
		} else {
			container.add(new EditingText("()"));
		}
	}

	/**
	 * 刷新编辑器
	 */
	public void refreshEditor() {
		int caretPos = editor.getCaretPosition();
		int ss = editor.getSelectionStart();
		int se = editor.getSelectionEnd();

		ArrayList<EditingText> container = new ArrayList<EditingText>();
		container.add(new EditingText(oldExp.substring(0, funcStart)));
		appendEditingText(container);
		container.add(new EditingText(oldExp.substring(funcEnd)));

		editor.setCaretPosition(caretPos);
		editor.setSelectionStart(ss);
		editor.setSelectionEnd(se);
	}

}
