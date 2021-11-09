package com.raqsoft.ide.common.swing;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JTextField;

import com.raqsoft.cellset.datamodel.PgmNormalCell;
import com.raqsoft.ide.common.EditListener;

/**
 * 只读的文本框
 *
 */
public class JTextFieldReadOnly extends JTextField {
	private static final long serialVersionUID = 1L;

	/**
	 * 构造函数
	 */
	public JTextFieldReadOnly() {
		this(0);
	}

	/**
	 * 构造函数
	 * 
	 * @param columns
	 *            列数
	 */
	public JTextFieldReadOnly(int columns) {
		this(new String(), columns);
	}

	/**
	 * 构造函数
	 * 
	 * @param s
	 *            字符串
	 */
	public JTextFieldReadOnly(String s) {
		this(s, 0);
	}

	/**
	 * 构造函数
	 * 
	 * @param s
	 *            字符串
	 * @param columns
	 *            列数
	 */
	public JTextFieldReadOnly(String s, int columns) {
		this(s, columns, null);
	}

	/**
	 * 构造函数
	 * 
	 * @param s
	 *            字符串
	 * @param columns
	 *            列数
	 * @param el
	 *            监听器，不为null时可以编辑
	 */
	public JTextFieldReadOnly(String s, int columns, final EditListener el) {
		super(s, columns);
		setEditable(el != null);
		if (el != null) {
			KeyListener kl = new KeyListener() {
				public void keyTyped(KeyEvent e) {
				}

				public void keyPressed(KeyEvent e) {
				}

				public void keyReleased(KeyEvent e) {
					if (e != null) {
						Object src = e.getSource();
						if (src instanceof JTextField) {
							// 不可editable的TextField仍然能接收到按键事件
							JTextField txt = (JTextField) src;
							if (!txt.isEditable())
								return;
							if (e.isControlDown() || e.isAltDown()
									|| e.isShiftDown()) {
								return;
							}
							if (e.isActionKey()) {
								return;
							}
							if (e.getKeyCode() < 32) {
								return;
							}

							String newTxt = txt.getText();
							Object newVal = PgmNormalCell
									.parseConstValue(newTxt);
							el.editChanged(newVal);
						}
					}
				}
			};
			this.addKeyListener(kl);
		}

		addFocusListener(new FocusListener() {
			int caretPosition = 0;

			public void focusGained(FocusEvent e) {
				if (!getCaret().isVisible()) {
					getCaret().setVisible(true);
					setCaretPosition(caretPosition);
				}
			}

			public void focusLost(FocusEvent e) {
				if (getCaretPosition() > 0)
					caretPosition = getCaretPosition();
			}
		});

	}

}
