package com.scudata.ide.common.swing;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ListModel;
import javax.swing.TransferHandler;

import com.scudata.app.common.Section;
import com.scudata.common.ArgumentTokenizer;
import com.scudata.ide.common.GM;

/**
 * 扩充的JList
 * 
 * 将数据封装在该类 数据规则，显示值要求必须为String类型，方便显示程序 数据值可以是任意类型，此要求跟JComboBoxEx相同
 * 
 */

public class JListEx extends JList {
	private static final long serialVersionUID = 1L;

	/**
	 * 列表模型
	 */
	public DefaultListModel data = new DefaultListModel();

	/**
	 * 代码值
	 */
	private Vector<Object> codeData = new Vector<Object>();

	/**
	 * 构造函数
	 */
	public JListEx() {
		super.setModel(data);
		setTransferHandler(new JListExHandler(this));
		addKeyListener(new KeyListener() {
			private StringBuffer buf = new StringBuffer();

			private int lastTypeTime = -1;

			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
			}

			public void keyTyped(KeyEvent e) {
				JList list = (JList) e.getSource();
				ListModel model = list.getModel();
				char key = e.getKeyChar();
				int now = (int) (System.currentTimeMillis() / 1000);
				if (lastTypeTime > 0) {
					if (now - lastTypeTime > 2) {
						buf.delete(0, buf.length());
					}
				}
				lastTypeTime = now;
				buf.append(key);
				int i = 0, size = model.getSize();
				boolean doubleSearch = false;
				for (; i < size; i++) {
					String s = (String) model.getElementAt(i);
					if (s != null && s.length() > 0
							&& s.startsWith(buf.toString())) {
						list.setSelectedIndex(i);
						list.ensureIndexIsVisible(i);
						break;
					}
					if (!doubleSearch && i == size - 1) {
						doubleSearch = true;
						buf.delete(0, buf.length());
						buf.append(key);
						i = 0;
					}
				}
			}
		});
	}

	/**
	 * 构造函数
	 * 
	 * @param items
	 *            由delim分隔的列表值字符串
	 * @param delim
	 *            分隔符
	 */
	public JListEx(String items, char delim) {
		this();
		setListData(items, delim);
	}

	/**
	 * 构造函数
	 * 
	 * @param items
	 *            由逗号分隔的列表值字符串
	 */
	public JListEx(String items) {
		this(items, ',');
	}

	/**
	 * 构造函数
	 * 
	 * @param items
	 *            列表值数组
	 */
	public JListEx(Object[] items) {
		this();
		setListData(items);
	}

	/**
	 * 构造函数
	 * 
	 * @param items
	 *            列表值集合
	 */
	public JListEx(Vector items) {
		this();
		setListData(items);
	}

	/**
	 * 构造函数
	 * 
	 * @param model
	 *            列表模型
	 */
	public JListEx(DefaultListModel model) {
		this();
		if (model == null) {
			return;
		}
		setModel(model);
	}

	/**
	 * 移动数据的处理方法，默认情况用户不用管该方法，程序自动将数据drop到所在控件，
	 * 如果用户有必要自己处理数据，比如还要作其他操作，责覆盖该方法，然后返回true就 可以
	 * 
	 * @param moveData
	 *            String 逗号分割的选中的JList的选项
	 * @param dropedControl
	 *            JComponent 鼠标Drop的控件
	 * @return boolean 用户是否自己处理了该方法，是的话应该返回true，否则false。
	 */
	public boolean moveDropTarget(String moveData, JComponent dropedControl) {
		return false;
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
		data.removeAllElements();
		ArgumentTokenizer at = new ArgumentTokenizer(items, delim);
		while (at.hasMoreTokens()) {
			data.addElement(at.nextToken());
		}
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
		data.removeAllElements();
		for (int i = 0; i < listData.length; i++) {
			data.addElement(listData[i]);
		}
	}

	/**
	 * 设置当前的数据为listData
	 * 
	 * @param listData
	 *            包含数据的Vector对象
	 */
	public void setListData(List listData) {
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
	public void setModel(DefaultListModel model) {
		if (model == null) {
			return;
		}
		data = model;
		super.setModel(model);
	}

	/**
	 * 删除当前列表框中的所有选中的项目
	 */
	public void removeSelectedItems() {
		if (this.data.size() == 0) {
			return;
		}
		int[] iSelects = getSelectedIndices();
		for (int i = iSelects.length - 1; i >= 0; i--) {
			data.removeElementAt(iSelects[i]);
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
			sb.append(data.get(i) + ",");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * 获得当前列表框中的所有选中的项目名
	 * 
	 * @return 字符串形式的项目列表，列表之间用‘，’分开
	 */
	public String selectedItems() {
		StringBuffer sb = new StringBuffer();
		Object[] sItems = this.getSelectedValues();
		if (sItems.length == 0) {
			return "";
		}
		for (int i = 0; i < sItems.length; i++) {
			sb.append(sItems[i] + ",");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * 取逗号分隔的列表值字符串
	 * 
	 * @return
	 */
	public String getSelectedItems() {
		return selectedItems();
	}

	/**
	 * 如果当前数据实现了Comparable，则对数据按照指定的顺序排序
	 * 
	 * @param asc
	 *            ascend，是否按照升序排列
	 * @return 如果可以排序返回true,否则false
	 */
	public boolean sort(boolean ascend) {
		Comparable ci, cj;
		int i, j;
		boolean lb_exchange;
		for (i = 0; i < data.size(); i++) {
			if (!(data.get(i) instanceof Comparable)) {
				return false;
			}
		}

		for (i = 0; i < data.size() - 1; i++) {
			for (j = i + 1; j < data.size(); j++) {
				ci = (Comparable) data.get(i);
				cj = (Comparable) data.get(j);
				if (ascend) {
					lb_exchange = ci.compareTo(cj) > 0;
				} else {
					lb_exchange = ci.compareTo(cj) < 0;
				}
				if (lb_exchange) {
					data.setElementAt(cj, i);
					data.setElementAt(ci, j);
				}
			}
		}
		return true;
	}

	/**
	 * x_ 为前缀的函数用于显示值和真实值显示，所有的函数都得用相应的 x_...
	 * 
	 * @param codeData
	 * @param dispData
	 */
	public synchronized void x_setData(List codeData, List dispData) {
		data.removeAllElements();
		setListData(dispData);
		this.codeData.removeAllElements();
		this.codeData.addAll(codeData);
	}

	/**
	 * 排序
	 * 
	 * @param sortByDisp
	 *            是否通过显示值排序
	 * @param ascend
	 *            是否正序
	 * @return
	 */
	public boolean x_sort(boolean sortByDisp, boolean ascend) {
		Comparable ci, cj;
		int i, j;
		boolean lb_exchange;
		if (sortByDisp) {
			for (i = 0; i < data.size(); i++) {
				if (!(data.get(i) instanceof Comparable)) {
					return false;
				}
			}
		} else {
			for (i = 0; i < data.size(); i++) {
				if (!(codeData.get(i) instanceof Comparable)) {
					return false;
				}
			}
		}

		for (i = 0; i < data.size() - 1; i++) {
			for (j = i + 1; j < data.size(); j++) {
				if (sortByDisp) {
					ci = (Comparable) data.get(i);
					cj = (Comparable) data.get(j);
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
					o = data.get(i);
					o2 = data.get(j);
					data.setElementAt(o2, i);
					data.setElementAt(o, j);

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
	 * 根据显示值取代码值
	 * 
	 * @param dispItem
	 *            显示值
	 * @return
	 */
	public Object x_getCodeItem(String dispItem) {
		if (codeData == null || data == null) {
			return dispItem;
		}
		String disp;
		int i = 0;
		for (i = 0; i < data.size(); i++) {
			disp = (String) data.get(i);
			if (disp.equalsIgnoreCase(dispItem)) {
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
		if (codeData == null || data == null) {
			return codeItem.toString();
		}
		Object code;
		int i = 0;
		for (i = 0; i < codeData.size(); i++) {
			code = codeData.get(i);
			if (code.equals(codeItem)) {
				break;
			}
		}
		if (i >= data.size()) {
			return codeItem.toString();
		}
		return (String) data.get(i);
	}

	/**
	 * 取选择的列表值
	 * 
	 * @return
	 */
	public String x_SelectedItems() {
		StringBuffer sb = new StringBuffer();
		Object[] sItems = this.getSelectedValues();
		if (sItems.length == 0) {
			return "";
		}
		for (int i = 0; i < sItems.length; i++) {
			sb.append(x_getCodeItem((String) sItems[i]) + ",");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * 设置显示模型和代码值
	 * 
	 * @param codeData
	 * @param dispModel
	 */
	public void x_setModel(Vector codeData, DefaultListModel dispModel) {
		data = dispModel;
		super.setModel(dispModel);
		this.codeData = codeData;
	}

	/**
	 * 删除所有成员
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
	 * 删除选择的成员
	 */
	public void x_removeSelectedItems() {
		int[] ii = getSelectedIndices();
		for (int i = ii.length - 1; i >= 0; i--) {
			x_removeElement(ii[i]);
		}
	}

	/**
	 * 取选择的值
	 * 
	 * @return
	 */
	public Object[] x_getSelectedValues() {
		Object[] d = getSelectedValues();
		Object[] c = new Object[d.length];
		for (int i = 0; i < d.length; i++) {
			c[i] = x_getCodeItem((String) d[i]);
		}
		return c;
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
	 * 插入成员
	 * 
	 * @param index
	 *            序号
	 * @param code
	 *            代码值
	 * @param disp
	 *            显示值
	 */
	public void x_insertElement(int index, Object code, String disp) {
		codeData.insertElementAt(code, index);
		data.insertElementAt(disp, index);
	}

	/**
	 * 按序号设置成员
	 * 
	 * @param index
	 *            序号
	 * @param code
	 *            代码值
	 * @param disp
	 *            显示值
	 */
	public void x_setElementAt(int index, Object code, String disp) {
		codeData.setElementAt(code, index);
		data.setElementAt(disp, index);
	}

	/**
	 * 移动成员
	 * 
	 * @param index
	 *            序号
	 * @param shiftUp
	 *            是否上移
	 */
	public void x_shiftElement(int index, boolean shiftUp) {
		if (shiftUp && index <= 0) {
			return;
		}
		if (!shiftUp && index >= data.size() - 1) {
			return;
		}
		Object tmp;
		if (shiftUp) {
			tmp = codeData.get(index - 1);
			codeData.set(index - 1, codeData.get(index));
			codeData.set(index, tmp);
			tmp = data.get(index - 1);
			data.set(index - 1, data.get(index));
			data.set(index, tmp);
			this.setSelectedIndex(index - 1);
		} else {
			tmp = codeData.get(index + 1);
			codeData.set(index + 1, codeData.get(index));
			codeData.set(index, tmp);
			tmp = data.get(index + 1);
			data.set(index + 1, data.get(index));
			data.set(index, tmp);
			this.setSelectedIndex(index + 1);
		}

	}
}

/**
 * 列表的句柄
 *
 */
class JListExHandler extends TransferHandler {

	/**
	 * 列表控件
	 */
	JListEx home;

	/**
	 * 构造函数
	 * 
	 * @param home
	 *            列表控件
	 */
	public JListExHandler(JListEx home) {
		this.home = home;
	}

	/**
	 * 是否可以导入
	 */
	public boolean canImport(JComponent c, DataFlavor[] flavors) {
		return (c instanceof JListEx || c instanceof JTextArea
				|| c instanceof JList || c instanceof JTextPane);
	}

	/**
	 * 在接口listData中找到对象item的序号
	 * 
	 * @param listData
	 * @param item
	 * @return
	 */
	public static int getItemIndex(ListModel listData, String item) {
		int c = listData.getSize();
		for (int i = 0; i < c; i++) {
			if (item.equalsIgnoreCase(listData.getElementAt(i).toString())) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 导入数据
	 */
	public boolean importData(JComponent c, Transferable t) {
		try {
			String items = (String) t.getTransferData(DataFlavor.stringFlavor);
			if (home.moveDropTarget(items, c)) {
				return true;
			}
			Section sItems = new Section(items);
			int i;
			if (c instanceof JListEx) {
				JListEx tList = (JListEx) c;
				for (i = 0; i < sItems.size(); i++) {
					if (getItemIndex(tList.data, sItems.get(i)) == -1) {
						tList.data.addElement(sItems.get(i));
					}
				}
			} else if (c instanceof JList) {
				JList list = (JList) c;
				DefaultListModel lm = (DefaultListModel) list.getModel();
				for (i = 0; i < sItems.size(); i++) {
					if (getItemIndex(lm, sItems.get(i)) == -1) {
						lm.addElement(sItems.get(i));
					}
				}
			} else if (c instanceof JTextArea) {
				JTextArea tta = (JTextArea) c;
				tta.setText(tta.getText() + " " + items);
			} else if (c instanceof JTextPane) {
				JTextPane ttp = (JTextPane) c;
				ttp.setText(ttp.getText() + " " + items);
			} else {
				return false;
			}
			return true;
		} catch (Exception x) {
			GM.showException(x);
		}
		return false;
	}

	protected void exportDone(JComponent c, Transferable data, int action) {
	}

	/**
	 * 取源命令
	 */
	public int getSourceActions(JComponent c) {
		return COPY_OR_MOVE;
	}

	/**
	 * 创建移动的Transferable对象
	 */
	protected Transferable createTransferable(JComponent c) {
		JListEx list = (JListEx) c;
		Object[] items = list.getSelectedValues();
		Section sItems = new Section(items);
		return new StringSelection(sItems.toString());
	}
}
