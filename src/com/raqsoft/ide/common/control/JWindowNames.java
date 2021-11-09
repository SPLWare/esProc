package com.raqsoft.ide.common.control;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JScrollPane;
import javax.swing.JWindow;

import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.common.GV;
import com.raqsoft.ide.common.swing.JListEx;

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
	 * 名称
	 */
	private Vector<String> nameList;
	/**
	 * 光标的位置
	 */
	private int dot;
	/**
	 * 前缀是否"."
	 */
	private boolean isPeriod;
	/**
	 * 滚动面板控件
	 */
	private JScrollPane jSPWin;

	/**
	 * 构造函数
	 * 
	 * @param names
	 *            名称数组
	 * @param dot
	 *            光标的位置
	 * @param isPeriod
	 *            前缀是否"."
	 */
	public JWindowNames(String[] names, int dot, boolean isPeriod) {
		super(GV.appFrame);
		this.dot = dot;
		this.isPeriod = isPeriod;
		jSPWin = new JScrollPane(listWindow);
		getContentPane().add(jSPWin, BorderLayout.CENTER);
		setFocusable(true);
		String[] cloneNames = new String[names.length];
		System.arraycopy(names, 0, cloneNames, 0, names.length);
		names = cloneNames;
		Arrays.sort(names);
		nameList = new Vector<String>();
		for (String s : names) {
			nameList.add(s);
		}
		listWindow.x_setData(nameList, nameList);
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
						selectName((String) listWindow.getSelectedValue());
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
					selectName((String) listWindow.getSelectedValue());
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

	/**
	 * 设置焦点
	 */
	public void setFocused() {
		this.requestFocus();
		listWindow.requestFocus();
	}

	/**
	 * 获取光标位置
	 * 
	 * @return
	 */
	public int getDot() {
		return dot;
	}

	/**
	 * 是否前缀是"."
	 * 
	 * @return
	 */
	public boolean isPeriod() {
		return isPeriod;
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
		selectName(getSelectedName());
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