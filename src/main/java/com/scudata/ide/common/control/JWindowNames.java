package com.scudata.ide.common.control;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.ListCellRenderer;

import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.function.FuncInfo;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.JListEx;

/**
 * 用于显示名称列表
 *
 */
public abstract class JWindowNames extends JWindow {
	private static final long serialVersionUID = 1L;
	/**
	 * 列表控件
	 */
	private JListEx listWindow = new JListEx();
	/**
	 * 背景色
	 */
	private final Color BACK_COLOR = new Color(255, 255, 214);
	/**
	 * 选中的背景色
	 */
	private final Color SELECTED_COLOR = BACK_COLOR.darker();
	/**
	 * 名称
	 */
	private Vector<String> nameList;

	/**
	 * 滚动面板控件
	 */
	private JScrollPane jSPWin;

	/**
	 * 函数信息列表
	 */
	private List<FuncInfo> funcs;

	/**
	 * 字段数量
	 */
	private int fieldSize = 0;

	/**
	 * 构造函数
	 */
	public JWindowNames(String[] fieldNames, final List<FuncInfo> funcs) {
		super(GV.appFrame);
		this.funcs = funcs;
		jSPWin = new JScrollPane(listWindow);
		getContentPane().add(jSPWin, BorderLayout.CENTER);
		setFocusable(true);
		fieldNames = sortNames(fieldNames);
		Collections.sort(funcs, new Comparator<FuncInfo>() {

			public int compare(FuncInfo o1, FuncInfo o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		nameList = new Vector<String>();
		Vector<String> dispList = new Vector<String>();
		if (fieldNames != null)
			for (String s : fieldNames) {
				nameList.add(s);
				dispList.add(IdeCommonMessage.get().getMessage(
						"jwindownames.fieldname", s));
			}
		if (funcs != null)
			for (FuncInfo fi : funcs) {
				nameList.add(fi.getName());
			}
		listWindow.x_setData(nameList, nameList);
		fieldSize = fieldNames == null ? 0 : fieldNames.length;
		ListCellRenderer cellRenderer = new ListCellRenderer() {
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				final JPanel panel = new JPanel(new BorderLayout());
				String iconName;
				String tooltip;
				String funcName;
				if (index < fieldSize) {
					funcName = nameList.get(index);
					iconName = "treecolumn.gif";
					tooltip = IdeCommonMessage.get().getMessage(
							"jwindownames.fieldname");
				} else {
					iconName = "func.gif";
					int funcIndex = index - fieldSize;
					FuncInfo fi = funcs.get(funcIndex);
					funcName = FuncWindow.getFuncString(fi, null, null, -1);
					tooltip = fi.getDesc();
				}
				JLabel label = new JLabel(GM.getImageIcon(GC.IMAGES_PATH
						+ iconName));
				panel.add(label, BorderLayout.WEST);
				final JTextField text = new JTextField(funcName);
				if (isSelected)
					text.setBackground(SELECTED_COLOR);
				else
					text.setBackground(BACK_COLOR);
				text.setBorder(null);
				panel.add(text, BorderLayout.CENTER);

				panel.setToolTipText(tooltip);
				text.setToolTipText(tooltip);
				label.setToolTipText(tooltip);
				panel.setMinimumSize(new Dimension(0, 22));
				panel.setPreferredSize(new Dimension(0, 22));
				return panel;
			}
		};
		listWindow.setCellRenderer(cellRenderer);
		listWindow.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				e.consume();
			}

			public void mouseReleased(MouseEvent e) {
				e.consume();
			}

			public void mouseClicked(MouseEvent e) {
				if (e.getButton() != MouseEvent.BUTTON1)
					return;
				if (e.getClickCount() == 2) {
					if (listWindow.isSelectionEmpty())
						return;
					try {
						selectName();
						e.consume();
					} catch (Exception e1) {
						GM.showException(e1);
					}
				}
			}
		});
		this.setBackground(BACK_COLOR);
		listWindow.setBackground(BACK_COLOR);
		jSPWin.setBackground(BACK_COLOR);

		KeyListener kl = new KeyAdapter() {

			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					e.consume();
					dispose();
				} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					if (listWindow.isSelectionEmpty()) {
						return;
					}
					selectName();
					e.consume();
				}
			}

		};

		listWindow.addKeyListener(kl);
		jSPWin.addKeyListener(kl);
		this.addKeyListener(kl);
		listWindow.setSelectedIndex(0);
		this.addWindowListener(new WindowAdapter() {

			public void windowClosed(WindowEvent e) {
			}

			public void windowOpened(WindowEvent e) {
				listWindow.requestFocusInWindow();
			}
		});
	}

	private String[] sortNames(String[] names) {
		if (names == null)
			return null;
		String[] cloneNames = new String[names.length];
		System.arraycopy(names, 0, cloneNames, 0, names.length);
		Arrays.sort(cloneNames);
		return cloneNames;
	}

	/**
	 * 设置焦点
	 */
	public void setFocused() {
		this.requestFocus();
		listWindow.requestFocus();
	}

	/**
	 * 获取选中的名称
	 * 
	 * @return
	 */
	public String getSelectedName() {
		return (String) listWindow.getSelectedValue();
	}

	/**
	 * 选择名称
	 */
	public void selectName() {
		int index = listWindow.getSelectedIndex();
		if (index < 0)
			return;
		String name;
		if (index < fieldSize) {
			name = (String) listWindow.getSelectedValue();
		} else {
			int funcIndex = index - fieldSize;
			FuncInfo fi = funcs.get(funcIndex);
			name = FuncWindow.getFuncString(fi, null, null, -1, false);
		}
		selectName(name);
	}

	/**
	 * 选择前一个
	 */
	public void selectBefore() {
		int index = listWindow.getSelectedIndex();
		if (index < 0) {
			index = 1;
		}
		if (index > 0) {
			listWindow.setSelectedIndex(index - 1);
			listWindow.requestFocus();
		}
	}

	/**
	 * 选择下一个
	 */
	public void selectNext() {
		int index = listWindow.getSelectedIndex();
		if (index < 0) {
			index = 0;
		}
		if (index < nameList.size() - 1) {
			listWindow.setSelectedIndex(index + 1);
			listWindow.requestFocus();
		}
	}

	/**
	 * 搜索名称
	 * 
	 * @param pre
	 *            搜索的名称前缀
	 * @return
	 */
	public boolean searchName(String pre) {
		if (pre == null)
			return false;
		pre = pre.toLowerCase();
		for (int i = 0; i < nameList.size(); i++) {
			String val = nameList.get(i);
			if (val != null) {
				if (val.toLowerCase().startsWith(pre)) {
					listWindow.setSelectedIndex(i);
					int max = jSPWin.getVerticalScrollBar().getMaximum();
					int value = max * i / nameList.size();
					jSPWin.getVerticalScrollBar().setValue(value);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 选中了名称
	 * 
	 * @param name
	 *            选中的名称
	 */
	public abstract void selectName(String name);
}