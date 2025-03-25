package com.scudata.ide.common.swing;

import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.Logger;
import com.scudata.util.Variant;

/**
 * 数据规则，显示值要求必须为String类型，方便显示程序 数据值可以是任意类型，此要求跟JListEx相同
 *
 */
public class JComboBoxEx extends JComboBox {
	private static final long serialVersionUID = 1L;

	/**
	 * 是否可以修改值
	 */
	private boolean isItemChangeable = true;
	/**
	 * 下拉列表模型
	 */
	public DefaultComboBoxModel data = new DefaultComboBoxModel();
	/**
	 * 代码值
	 */
	public Vector<Object> codeData = new Vector<Object>();

	/**
	 * 构造函数
	 */
	public JComboBoxEx() {
		super.setModel(data);
	}

	/**
	 * 构造函数
	 * 
	 * @param items
	 *            由逗号分隔的下拉项字符串
	 */
	public JComboBoxEx(String items) {
		this();
		setListData(items, ',');
	}

	/**
	 * 构造函数
	 * 
	 * @param items
	 *            由delim分隔的下拉项字符串
	 * @param delim
	 *            分隔符
	 */
	public JComboBoxEx(String items, char delim) {
		this();
		setListData(items, delim);
	}

	/**
	 * 构造函数
	 * 
	 * @param items
	 *            下拉项数组
	 */
	public JComboBoxEx(Object[] items) {
		this();
		setListData(items);
	}

	/**
	 * 构造函数
	 * 
	 * @param items
	 *            下拉项集合
	 */
	public JComboBoxEx(Vector items) {
		this();
		setListData(items);
	}

	/**
	 * 构造函数
	 * 
	 * @param model
	 *            下拉框模型
	 */
	public JComboBoxEx(DefaultComboBoxModel model) {
		this();
		if (model == null) {
			return;
		}
		setModel(model);
	}

	/**
	 * 取成员是否可修改
	 * 
	 * @return
	 */
	public boolean isItemChangeable() {
		return isItemChangeable;
	}

	/**
	 * 设置成员是否可修改
	 * 
	 * @param changeable
	 */
	public void setItemChageable(boolean changeable) {
		isItemChangeable = changeable;
	}

	/**
	 * 设置当前的数据为items
	 *
	 * @param items
	 *            delim分隔的下拉项字符串
	 * @param delim
	 *            用于分开items数据的分割符号
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
	 *            用','分开的数据列表
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
	public void setListData(Vector listData) {
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

	/**
	 * 设置当前选择的数据
	 */
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
	public void x_setData(Vector codeData, Vector dispData) {
		isItemChangeable = false;
		// 必须得先设置codeData,否则该控件的ActionPerformed事件中得到的x_getSelectedItem()的类型有错

		this.codeData.removeAllElements();
		this.codeData.addAll(codeData);

		data.removeAllElements();
		setListData(dispData);
		isItemChangeable = true;
	}

	/**
	 * 排序
	 * 
	 * @param sortByDisp
	 *            是否用显示值排序
	 * @param ascend
	 *            正序
	 * @return
	 */
	public boolean x_sort(boolean sortByDisp, boolean ascend) {
		int i, j;
		Comparable ci, cj;
		boolean lb_exchange;
		if (sortByDisp) {
			for (i = 0; i < data.getSize(); i++) {
				if (!(data.getElementAt(i) instanceof Comparable)) {
					return false;
				}
			}
		} else {
			for (i = 0; i < codeData.size(); i++) {
				if (!(codeData.get(i) instanceof Comparable)) {
					return false;
				}
			}
		}

		for (i = 0; i < data.getSize() - 1; i++) {
			for (j = i + 1; j < data.getSize(); j++) {
				if (sortByDisp) {
					ci = (Comparable) data.getElementAt(i);
					cj = (Comparable) data.getElementAt(j);
				} else {
					ci = (Comparable) codeData.get(i);
					cj = (Comparable) codeData.get(j);
				}
				if (ascend) {
					lb_exchange = ci.compareTo(cj) > 0;
				} else {
					lb_exchange = ci.compareTo(cj) < 0;
				}
				if (lb_exchange) {
					Object o, o2;
					o = data.getElementAt(i);
					o2 = data.getElementAt(j);
					data.removeElementAt(j);
					data.insertElementAt(o, j);

					data.removeElementAt(i);
					data.insertElementAt(o2, i);

					o = codeData.get(i);
					o2 = codeData.get(j);
					codeData.setElementAt(o2, i);
					codeData.setElementAt(o, j);
				}
			}
		}
		return true;
	}

	/**
	 * 打印
	 * 
	 * @param v
	 */
	public void prints(Object v) {
		Vector vc = null;
		DefaultComboBoxModel vd = null;
		if (v instanceof Vector) {
			vc = (Vector) v;
		} else {
			vd = (DefaultComboBoxModel) v;
		}
		if (vc != null) {
			if (vc.size() < 50) {
				return;
			}
			for (int i = 0; i < vc.size(); i++) {
				Logger.debug(i + "-" + vc.get(i).toString());
			}
		}

		if (vd != null) {
			for (int i = 0; i < vd.getSize(); i++) {
				Logger.debug(vd.getElementAt(i).toString());
			}

		}
	}

	/**
	 * 根据显示值取代码值
	 * 
	 * @param dispItem
	 *            显示值
	 * @return
	 */
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

	/**
	 * 根据代码值取显示值
	 * 
	 * @param codeItem
	 *            代码值
	 * @return
	 */
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

	/**
	 * 设置代码值和显示模型
	 * 
	 * @param codeData
	 *            代码值
	 * @param dispModel
	 *            显示模型
	 */
	public void x_setModel(Vector codeData, DefaultComboBoxModel dispModel) {
		data = dispModel;
		super.setModel(dispModel);
		this.codeData = codeData;
	}

	/**
	 * 删除全部成员
	 */
	public void x_removeAllElements() {
		data.removeAllElements();
		codeData.removeAllElements();
	}

	/**
	 * 按序号删除成员
	 * 
	 * @param i
	 *            序号
	 */
	public void x_removeElement(int i) {
		data.removeElementAt(i);
		codeData.removeElementAt(i);
	}

	/**
	 * 增加成员
	 * 
	 * @param code
	 *            代码值
	 * @param disp
	 *            显示值
	 */
	public void x_addElement(Object code, String disp) {
		codeData.addElement(code);
		data.addElement(disp);
	}

	/**
	 * 取当前选择的代码值
	 * 
	 * @return
	 */
	public Object x_getSelectedItem() {
		return x_getCodeItem(getSelectedItem());
	}

	/**
	 * 设置选择的显示值
	 * 
	 * @param dispItem
	 *            显示值
	 */
	public void x_setSelectedDispItem(String dispItem) {
		data.setSelectedItem(dispItem);
	}

	/**
	 * 设置选择的代码值
	 * 
	 * @param codeItem
	 *            代码值
	 */
	public void x_setSelectedCodeItem(Object codeItem) {
		x_setSelectedDispItem(x_getDispItem(codeItem));
	}
}
