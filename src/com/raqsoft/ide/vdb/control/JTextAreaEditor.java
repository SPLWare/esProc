package com.raqsoft.ide.vdb.control;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.math.BigDecimal;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;

public class JTextAreaEditor extends DefaultCellEditor implements MouseListener {
	private static final long serialVersionUID = 1L;

	public static final int TYPE_TEXT_SIMPLE = 1;
	public static final int TYPE_TEXT_WRAP = 2;
	public static final int TYPE_UNSIGNED_INTEGER = 3; // 自然数
	public static final int TYPE_SIGNED_INTEGER = 4;
	public static final int TYPE_UNSIGNED_DOUBLE = 5;
	public static final int TYPE_SIGNED_DOUBLE = 6;
	public static final int TYPE_TEXT_READONLY = 7;

	private int editorType;
	private JScrollPane jsp;
	private JTableEx parent;

	private JTextField textSimple = null;
	private JTextPane textWrap = null;
	private JSpinner uInteger = null;
	private JSpinner sInteger = null;
	private JSpinner uNumber = null;
	private JSpinner sNumber = null;

	public JTextAreaEditor(JTableEx parent) {
		this(parent, TYPE_TEXT_SIMPLE);
	}

	public JTextAreaEditor(JTableEx parent, int editorType) {
		super(new JTextField(""));
		this.editorType = editorType;
		this.parent = parent;

		switch (editorType) {
		case TYPE_TEXT_WRAP:
			textWrap = new JTextPane();
			textWrap.addMouseListener(this);
			textWrap.setBorder(BorderFactory.createEmptyBorder());
			jsp = new JScrollPane(textWrap);
			break;
		case TYPE_SIGNED_INTEGER:
			sInteger = new JSpinner();
			sInteger.addMouseListener(this);
			SpinnerNumberModel smodel = new SpinnerNumberModel(1, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
			sInteger.setModel(smodel);
			sInteger.setBorder(BorderFactory.createEmptyBorder());
			break;
		case TYPE_UNSIGNED_INTEGER:
			uInteger = new JSpinner();
			uInteger.addMouseListener(this);
			SpinnerNumberModel model = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1);
			uInteger.setModel(model);
			uInteger.setBorder(BorderFactory.createEmptyBorder());
			break;
		case TYPE_UNSIGNED_DOUBLE:
			textSimple = new JTextField("");
			textSimple.setBorder(BorderFactory.createEmptyBorder());
			break;
		case TYPE_SIGNED_DOUBLE:
			sNumber = new JSpinner();
			sNumber.addMouseListener(this);
			// 构造BigDecimal时要使用串，因为浮点数只能接近0.1，使用BigDecimal可以精确计算小数 2013.8.22 xq
			// System.out.println( new BigDecimal("0.1") );
			// System.out.println( new BigDecimal(0.1d) );
			final BigDecimal minimum = new BigDecimal("-999999999999");
			final BigDecimal maximum = new BigDecimal("999999999999");
			final BigDecimal stepSize = new BigDecimal("1");
			SpinnerNumberModel snmodel = new SpinnerNumberModel(new BigDecimal("1"), minimum, maximum, stepSize) {
				private static final long serialVersionUID = 1L;

				public Object getPreviousValue() {
					return incrValue(-1);
				}

				public Object getNextValue() {
					return incrValue(+1);
				}

				private Number incrValue(int dir) {
					BigDecimal bdir = new BigDecimal(dir);
					BigDecimal value = (BigDecimal) getValue();
					BigDecimal newValue = new BigDecimal(stepSize.toString());
					newValue = newValue.multiply(bdir);
					newValue = newValue.add(value);
					return newValue;
				}
			};

			// SpinnerNumberModel snmodel = new SpinnerNumberModel(1.0d,
			// -999999999999.0d, 999999999999.0d, 1.0d);
			sNumber.setModel(snmodel);
			sNumber.setBorder(BorderFactory.createEmptyBorder());
			break;
		/*
		 * case TYPE_UNSIGNED_DOUBLE: uNumber = new JSpinner();
		 * uNumber.addMouseListener( this ); SpinnerNumberModel unmodel = new
		 * SpinnerNumberModel( 1.0f, 0, Double.MAX_VALUE, 1 ); uNumber.setModel(
		 * unmodel ); uNumber.setBorder( BorderFactory.createEmptyBorder() );
		 * break;
		 */
		case TYPE_TEXT_READONLY:
			textSimple = new JTextFieldReadOnly("");
			textSimple.addMouseListener(this);
			textSimple.setBorder(BorderFactory.createEmptyBorder());
			break;
		default:
			textSimple = new JTextField("");
			textSimple.addMouseListener(this);
			textSimple.setBorder(BorderFactory.createEmptyBorder());
			break;
		}
		this.setClickCountToStart(1);
	}

	/*
	 * public boolean stopCellEditing() { super.stopCellEditing();
	 * 
	 * if (uInteger != null) { Object o = uInteger.getValue();
	 * System.out.println( o ); } return super.stopCellEditing(); }
	 */
	public Component setValue(Object value) {
		Component o;
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
				sInteger.setValue(new Integer(0));
			} else {
				sInteger.setValue(value);
			}
			o = sInteger;
			break;
		case TYPE_UNSIGNED_INTEGER:
			if (value == null || !(value instanceof Integer)) {
				uInteger.setValue(new Integer(0));
			} else {
				uInteger.setValue(value);
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
				sNumber.setValue(new BigDecimal(0));
			} else {
				sNumber.setValue(new BigDecimal(value.toString()));
			}
			// if (value == null || ! (value instanceof Double)) {
			// sNumber.setValue(new Double(0));
			// }
			// else {
			// sNumber.setValue(value);
			// }
			o = sNumber;
			break;
		/*
		 * case TYPE_UNSIGNED_DOUBLE: if ( value == null || ! ( value instanceof
		 * Double ) ){ uNumber.setValue( new Double( 0 ) ); } else{
		 * uNumber.setValue( value ); } o = uNumber; break;
		 */
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

	Object value = null;

	public Object getCellEditorValue() {
		switch (editorType) {
		case TYPE_TEXT_WRAP:
			value = textWrap.getText();
			break;
		case TYPE_SIGNED_INTEGER:
			value = sInteger.getValue();
			break;
		case TYPE_UNSIGNED_INTEGER:
			value = uInteger.getValue();
			break;
		case TYPE_UNSIGNED_DOUBLE:
			value = textSimple.getText();
			break;
		case TYPE_SIGNED_DOUBLE:
			value = sNumber.getValue();
			break;
		/*
		 * case TYPE_UNSIGNED_DOUBLE: value = uNumber.getValue(); break;
		 */
		default:
			value = textSimple.getText();
			break;
		}
		return value;
	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		return setValue(value);
	}

	public void mouseClicked(MouseEvent e) {
		JComponent editor = (JComponent) e.getSource();

		java.awt.Container p;
		java.awt.Container ct = editor.getTopLevelAncestor();
		int absoluteX = e.getX() + editor.getX(), absoluteY = e.getY() + editor.getY();
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
		SpinnerNumberModel smodel = new SpinnerNumberModel(1, min, max, step);
		if (sInteger != null) {
			sInteger.setModel(smodel);
		}
		if (uInteger != null) {
			uInteger.setModel(smodel);
		}
	}

	public void setArrange(double min, double max, double step) {
		SpinnerNumberModel smodel = new SpinnerNumberModel(1, min, max, step);
		if (sNumber != null) {
			sNumber.setModel(smodel);
		}
		if (uNumber != null) {
			uNumber.setModel(smodel);
		}
	}

}
