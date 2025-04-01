package com.scudata.ide.common.swing;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.ChangeEvent;

import com.scudata.util.Variant;

/**
 * JTable单元格的文本编辑器
 *
 */
public class JTextAreaEditor extends DefaultCellEditor implements MouseListener {
	private static final long serialVersionUID = 1L;

	/** 普通文本框 */
	public static final int TYPE_TEXT_SIMPLE = 1;
	/** 多行文本框 */
	public static final int TYPE_TEXT_WRAP = 2;
	/** 自然数编辑框 */
	public static final int TYPE_UNSIGNED_INTEGER = 3;
	/** 整数编辑框 */
	public static final int TYPE_SIGNED_INTEGER = 4;
	/** 无符号浮点数编辑框 */
	public static final int TYPE_UNSIGNED_DOUBLE = 5;
	/** 浮点数编辑框 */
	public static final int TYPE_SIGNED_DOUBLE = 6;
	/** 只读文本框 */
	public static final int TYPE_TEXT_READONLY = 7;

	/**
	 * 编辑类型
	 */
	private int editorType;
	/**
	 * 滚动面板控件
	 */
	private JScrollPane jsp;
	/**
	 * 表格控件
	 */
	private JTableExListener parent;

	/**
	 * 普通文本框控件
	 */
	private JTextField textSimple = null;

	/**
	 * 多行文本框控件
	 */
	private JTextPane textWrap = null;

	/**
	 * Spinner控件中输入的数值，目前没法获取到失去焦点，然后接受当前编辑值，改用JTextField 2020年12月30日
	 */
	/**
	 * 自然数编辑框
	 */
	private JTextField uInteger = null;
	/**
	 * 整数编辑框
	 */
	private JTextField sInteger = null;
	/**
	 * 浮点数编辑框
	 */
	private JTextField sNumber = null;
	/**
	 * 编辑值
	 */
	private Object oldValue = null;

	/**
	 * 构造函数
	 * 
	 * @param parent
	 *            表格控件
	 */
	public JTextAreaEditor(JTableExListener parent) {
		this(parent, TYPE_TEXT_SIMPLE);
	}

	/**
	 * 构造函数
	 * 
	 * @param parent
	 *            表格控件
	 * @param editorType
	 *            编辑框类型
	 */
	public JTextAreaEditor(final JTableExListener parent, int editorType) {
		super(new JTextField(""));
		this.editorType = editorType;
		this.parent = parent;

		KeyAdapter kl = new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				if (e != null) {
					Object src = e.getSource();
					if (src instanceof JTextField) {
						// 不可editable的TextField仍然能接收到按键事件
						JTextField txt = (JTextField) src;
						if (!txt.isEditable())
							return;
					}
					parent.stateChanged(new ChangeEvent(src));
				}
			}
		};

		switch (editorType) {
		case TYPE_TEXT_WRAP:
			textWrap = new JTextPane();
			textWrap.addMouseListener(this);
			textWrap.addKeyListener(kl);
			textWrap.setBorder(BorderFactory.createEmptyBorder());
			jsp = new JScrollPane(textWrap);
			break;
		case TYPE_SIGNED_INTEGER:
			sInteger = new JTextField();
			sInteger.addKeyListener(kl);
			sInteger.addMouseListener(this);
			sInteger.setBorder(BorderFactory.createEmptyBorder());
			break;
		case TYPE_UNSIGNED_INTEGER:
			uInteger = new JTextField();
			uInteger.addMouseListener(this);
			uInteger.addKeyListener(kl);
			uInteger.setBorder(BorderFactory.createEmptyBorder());
			break;
		case TYPE_UNSIGNED_DOUBLE:
			textSimple = new JTextField("");
			textSimple.addKeyListener(kl);
			textSimple.setBorder(BorderFactory.createEmptyBorder());
			break;
		case TYPE_SIGNED_DOUBLE:
			sNumber = new JTextField();
			sNumber.addMouseListener(this);
			sNumber.addKeyListener(kl);
			sNumber.setBorder(BorderFactory.createEmptyBorder());
			break;
		case TYPE_TEXT_READONLY:
			textSimple = new JTextFieldReadOnly("");
			textSimple.addMouseListener(this);
			textSimple.setBorder(BorderFactory.createEmptyBorder());
			break;
		default:
			textSimple = new JTextField("");
			textSimple.addKeyListener(kl);
			textSimple.addMouseListener(this);
			textSimple.setBorder(BorderFactory.createEmptyBorder());
			break;
		}
		this.setClickCountToStart(1);
	}

	/**
	 * 设置值
	 * 
	 * @param value
	 *            值
	 * @return
	 */
	public Component setValue(Object value) {
		Component o;
		oldValue = value;

		switch (editorType) {
		case TYPE_TEXT_WRAP:
			if (value == null) {
				textWrap.setText("");
			} else {
				textWrap.setText(value.toString());
			}
			o = jsp;
			break;
		case TYPE_SIGNED_INTEGER:
			if (value == null || !(value instanceof Integer)) {
				sInteger.setText("0");
			} else {
				sInteger.setText(value.toString());
			}
			o = sInteger;
			break;
		case TYPE_UNSIGNED_INTEGER:
			if (value == null || !(value instanceof Integer)) {
				uInteger.setText("0");
			} else {
				uInteger.setText(value.toString());
			}
			o = uInteger;
			break;
		case TYPE_UNSIGNED_DOUBLE:
			if (value == null) {
				textSimple.setText("");
			} else {
				textSimple.setText(value.toString());
			}
			o = textSimple;
			break;
		case TYPE_SIGNED_DOUBLE:
			if (value == null) {
				sNumber.setText("0");
			} else {
				sNumber.setText(value.toString());
			}
			o = sNumber;
			break;
		default:
			if (value == null) {
				textSimple.setText("");
			} else {
				textSimple.setText(value.toString());
			}
			o = textSimple;
			break;
		}
		return o;
	}

	/**
	 * 取编辑值
	 */
	public Object getCellEditorValue() {
		Object value = null;
		String tmp;
		switch (editorType) {
		case TYPE_TEXT_WRAP:
			value = textWrap.getText();
			break;
		case TYPE_SIGNED_INTEGER:
			tmp = sInteger.getText();
			value = Variant.parse(tmp);
			if (!(value instanceof Integer)) {
				value = oldValue;
			}
			break;
		case TYPE_UNSIGNED_INTEGER:
			tmp = uInteger.getText();
			value = Variant.parse(tmp);
			if (!(value instanceof Integer)) {
				value = oldValue;
			}
			break;
		case TYPE_UNSIGNED_DOUBLE:
			value = textSimple.getText();
			break;
		case TYPE_SIGNED_DOUBLE:
			tmp = sNumber.getText();
			value = Variant.parse(tmp);
			if (!(value instanceof Number)) {
				value = oldValue;
			}

			break;
		default:
			value = textSimple.getText();
			break;
		}
		return value;
	}

	/**
	 * 取单元格编辑控件
	 */
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		return setValue(value);
	}

	/**
	 * 鼠标点击事件
	 */
	public void mouseClicked(MouseEvent e) {
		JComponent editor = (JComponent) e.getSource();

		java.awt.Container p;
		java.awt.Container ct = editor.getTopLevelAncestor();
		int absoluteX = e.getX() + editor.getX(), absoluteY = e.getY()
				+ editor.getY();
		p = editor.getParent();
		while (p != ct) {
			absoluteX += p.getX();
			absoluteY += p.getY();
			p = p.getParent();
		}
		if (parent != null) {
			parent.fireClicked(absoluteX, absoluteY, e);
		}
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void setArrange(int min, int max, int step) {
	}

	public void setArrange(double min, double max, double step) {
	}

}
