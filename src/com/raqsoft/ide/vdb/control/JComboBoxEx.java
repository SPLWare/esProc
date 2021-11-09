package com.raqsoft.ide.vdb.control;

import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import com.raqsoft.common.ArgumentTokenizer;
import com.raqsoft.util.Variant;

/**
 * 数据规则，显示值要求必须为String类型，方便显示程序 数据值可以是任意类型，此要求跟JListEx相同
 *
 * @version 1.0
 */

public class JComboBoxEx extends JComboBox {
	private static final long serialVersionUID = 1L;
	boolean isItemChangeable = true;
	public Object tag;
	public DefaultComboBoxModel data = new DefaultComboBoxModel();
	public Vector<Object> codeData = new Vector<Object>();

	public JComboBoxEx() {
		super.setModel(data);
	}

	public JComboBoxEx(String items, char delim) {
		this();
		setListData(items, delim);
	}

	public JComboBoxEx(String items) {
		this();
		setListData(items, ',');
	}

	public JComboBoxEx(Object[] items) {
		this();
		setListData(items);
	}

	public JComboBoxEx(Vector<Object> items) {
		this();
		setListData(items);
	}

	public JComboBoxEx(DefaultComboBoxModel model) {
		this();
		if (model == null) {
			return;
		}
		setModel(model);
	}

	public boolean isItemChangeable() {
		return isItemChangeable;
	}

	public void setItemChageable(boolean changeable) {
		isItemChangeable = changeable;
	}

	/**
	 * 设置当前的数据为items
	 *
	 * @param items
	 *            数据列表
	 * @param delim
	 *            用用于分开items数据的分割符号
	 */
	public void setListData(String items, char delim) {
		if (items == null) {
			return;
		}
		isItemChangeable = false;
		data.removeAllElements();
		ArgumentTokenizer at = new ArgumentTokenizer(items, delim);
		while (at.hasMoreTokens()) {
			data.addElement(at.nextToken());
		}
		isItemChangeable = true;
	}

	/**
	 * 设置当前的数据为items
	 *
	 * @param items
	 *            用‘，’分开的数据列表
	 */
	public void setListData(String items) {
		setListData(items, ',');
	}

	/**
	 * 设置当前的数据为listData
	 *
	 * @param listData
	 *            包含数据的对象数组
	 */
	public void setListData(Object[] listData) {
		if (listData == null) {
			return;
		}
		isItemChangeable = false;
		data.removeAllElements();
		for (int i = 0; i < listData.length; i++) {
			data.addElement(listData[i]);
		}
		isItemChangeable = true;
	}

	/**
	 * 设置当前的数据为listData
	 *
	 * @param listData
	 *            包含数据的Vector对象
	 */
	public void setListData(Vector<?> listData) {
		if (listData == null) {
			return;
		}
		setListData(listData.toArray());
	}

	/**
	 * 设置当前的数据为model
	 *
	 * @param model
	 *            包含数据的ListModel对象
	 */
	public void setModel(DefaultComboBoxModel model) {
		if (model == null) {
			return;
		}
		data = model;
		super.setModel(model);
	}

	public void setSelectedItem(Object anObject) {
		if (anObject == null) {
			return;
		}
		if (codeData.size() > 0) {
			x_setSelectedCodeItem(anObject);
		} else {
			x_setSelectedDispItem(anObject.toString());
		}
	}

	/**
	 * 获得当前列表框中的所有项目名
	 *
	 * @return 字符串形式的项目列表，列表之间用‘，’分开
	 */
	public String totalItems() {
		if (data.getSize() == 0) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < data.getSize(); i++) {
			sb.append(data.getElementAt(i) + ",");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * x_ 为前缀的函数用于显示值和真实值显示，所有的函数都得用相应的 x_... 显示值必须是字符串形式， 真实值可以是任何值
	 *
	 * @param codeData
	 * @param dispData
	 */
	public void x_setData(Vector<Object> codeData, Vector<String> dispData) {
		isItemChangeable = false;
		// 必须得先设置codeData,否则该控件的ActionPerformed事件中得到的x_getSelectedItem()的类型有错

		this.codeData.removeAllElements();
		this.codeData.addAll(codeData);

		data.removeAllElements();
		setListData(dispData);
		isItemChangeable = true;
	}

	public Object x_getCodeItem(Object dispItem) {
		if (codeData == null || data == null) {
			return dispItem;
		}
		Object disp;
		int i = 0;
		for (i = 0; i < data.getSize(); i++) {
			disp = data.getElementAt(i);
			if (disp == null) {
				continue;
			}
			if (disp.equals(dispItem)) {
				break;
			}
		}
		if (i >= codeData.size()) {
			return dispItem;
		}
		return codeData.get(i);
	}

	public String x_getDispItem(Object codeItem) {
		if (codeItem == null) {
			return null;
		}
		if (codeData == null || data == null) {
			return codeItem.toString();
		}
		Object code;
		int i = 0;
		for (i = 0; i < codeData.size(); i++) {
			code = codeData.get(i);
			if (Variant.isEquals(code, codeItem)) { // code.equals(codeItem)) {
				break;
			}
		}
		if (i >= data.getSize()) {
			return codeItem.toString();
		}
		return String.valueOf(data.getElementAt(i));
	}

	public void x_setModel(Vector<Object> codeData, DefaultComboBoxModel dispModel) {
		data = dispModel;
		super.setModel(dispModel);
		this.codeData = codeData;
	}

	public void x_removeAllElements() {
		data.removeAllElements();
		codeData.removeAllElements();
	}

	public void x_removeElement(int i) {
		data.removeElementAt(i);
		codeData.removeElementAt(i);
	}

	public void x_addElement(Object code, String disp) {
		codeData.addElement(code);
		data.addElement(disp);
	}

	public Object x_getSelectedItem() {
		return x_getCodeItem(getSelectedItem());
	}

	public void x_setSelectedDispItem(String dispItem) {
		data.setSelectedItem(dispItem);
	}

	public void x_setSelectedCodeItem(Object codeItem) {
		x_setSelectedDispItem(x_getDispItem(codeItem));
	}

	/*
	 * public String x_totalItems() { if (codeData.size() == 0) { return ""; }
	 * StringBuffer sb = new StringBuffer(); for (int i = 0; i <
	 * codeData.size(); i++) { sb.append((String)codeData.get(i) + ","); }
	 * sb.deleteCharAt(sb.length() - 1); return sb.toString(); }
	 */
}
