package com.scudata.ide.common.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboBoxEditor;

import com.scudata.cellset.graph.config.Palette;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.resources.IdeCommonMessage;

/**
 * 颜色编辑下拉列表控件
 */
public class ColorComboBox extends JComboBox<Integer> {
	private static final long serialVersionUID = 1L;

	/**
	 * 是否可以编辑表达式
	 */
	private boolean canEditExp = false;
	/**
	 * 透明色
	 */
	public static Integer transparentColor = new Integer(0xffffff);

	/**
	 * 缺省的颜色列表
	 */
	final static Integer[] defColors = new Integer[] {
			new Integer(Color.black.getRGB()),
			new Integer(Color.gray.getRGB()),
			new Integer(Color.orange.getRGB()),
			new Integer(Color.red.getRGB()), new Integer(Color.blue.getRGB()),
			new Integer(Color.yellow.getRGB()),
			new Integer(Color.magenta.getRGB()),
			new Integer(Color.green.getRGB()),
			new Integer(Color.lightGray.getRGB()),
			new Integer(Color.white.getRGB()), transparentColor };

	/**
	 * 构造函数
	 */
	public ColorComboBox() {
		this(true);
	}

	/**
	 * 构造函数
	 * 
	 * @param palette
	 */
	public ColorComboBox(Palette palette) {
		super();
		if (palette == null)
			return;
		this.removeAllItems();
		for (int i = 0; i < palette.size(); i++) {
			int c = palette.getColor(i);
			addItem(c);
		}
	}

	/**
	 * 初始化
	 * 
	 * @param withTransparentColor
	 */
	private void init(boolean withTransparentColor) {
		if (!withTransparentColor) {
			this.removeItemAt(defColors.length - 1);
		}
		setEditor(new ColorComboBoxEditor(this));
		setRenderer(new ColorRendererer());
		setEditable(true);
		setPreferredSize(new Dimension(70, 25));
	}

	/**
	 * @param withTransparentColor
	 *            boolean 是否有 透明色 下拉颜色
	 */
	public ColorComboBox(boolean withTransparentColor) {
		super(defColors);
		init(withTransparentColor);
	}

	/**
	 * 设置是否包含透明色
	 * 
	 * @param withTransparentColor
	 *            true时包含透明色
	 */
	public void setWithTransparentColor(boolean withTransparentColor) {
		if (withTransparentColor) {
			Object lastColor = this.getItemAt(this.getItemCount() - 1);
			if (lastColor != transparentColor) {
				addItem(transparentColor);
			}
		} else {
			Object lastColor = this.getItemAt(this.getItemCount() - 1);
			if (lastColor == transparentColor) {
				removeItem(transparentColor);
				setSelectedIndex(getItemCount() - 1);
			}
		}
	}

	/**
	 * 取颜色的int值
	 * 
	 * @return
	 */
	public Integer getColor() {
		return (Integer) this.getSelectedItem();
	}

	/**
	 * 是否可以编辑表达式
	 * 
	 * @return
	 */
	boolean isCanEditExp() {
		return canEditExp;
	}

	/**
	 * 设置可以编辑表达式
	 */
	public void setAllowEditExp() {
		canEditExp = true;
	}

	/**
	 * 下拉框的单元格颜色编辑器
	 *
	 */
	class ColorComboBoxEditor extends BasicComboBoxEditor implements
			FocusListener {
		/**
		 * 颜色下拉框
		 */
		ColorComboBox parentContainer = null;
		/**
		 * 颜色图标
		 */
		ColorIcon editorIcon = new ColorIcon();
		/**
		 * 编辑标签
		 */
		JLabel editorLabel = new JLabel(editorIcon);
		/**
		 * 边框
		 */
		Border lowerBorder, etchedBorder;
		/**
		 * 编辑的颜色
		 */
		Object color;
		/**
		 * 对话框
		 */
		Dialog dialog = null;
		/**
		 * 颜色表达式、值和HEX值
		 */
		JTextField colorExpField, colorValueDec, colorValueHex;
		/**
		 * 颜色选择器
		 */
		JColorChooser colorChooser = new JColorChooser();

		/**
		 * 构造函数
		 * 
		 * @param p
		 */
		public ColorComboBoxEditor(ColorComboBox p) {
			parentContainer = p;
			lowerBorder = BorderFactory.createLoweredBevelBorder();
			etchedBorder = BorderFactory.createEtchedBorder();
			editorLabel.setBorder(etchedBorder);

			editorLabel.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					editorLabel.setBorder(lowerBorder);
				}

				public void mouseReleased(MouseEvent e) {
					editorLabel.setBorder(etchedBorder);
					if (parentContainer.isEnabled()) {
						if (dialog == null) {

							if (parentContainer.isCanEditExp()) {
								colorExpField = new JTextField();
							}

							final ActionListener okListener = new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									if (parentContainer.isCanEditExp()
											&& StringUtils
													.isValidString(colorExpField
															.getText())) {
										setColor(colorExpField.getText());
									} else {
										setColor(colorChooser.getColor());
									}
								}
							};

							dialog = JColorChooser.createDialog(
									parentContainer,
									IdeCommonMessage.get().getMessage(
											"colorcombobox.selectcolor"), true,
									colorChooser, okListener, null);
							if (parentContainer.isCanEditExp()) {
								JRootPane rp = (JRootPane) dialog
										.getComponent(0);
								JPanel tmp = new JPanel(new GridBagLayout());
								tmp.add(new JLabel("Expression: "),
										GM.getGBC(1, 1));
								tmp.add(colorExpField,
										GM.getGBC(1, 2, true, false, 0));
								JButton bt = new JButton("C");
								bt.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										colorExpField.setText("");
									}
								});
								tmp.add(bt, GM.getGBC(1, 3));
								rp.getContentPane()
										.add(tmp, BorderLayout.NORTH);
							}
						}

						if (color instanceof Integer) {
							colorChooser.setColor(new Color(((Integer) color)
									.intValue(), true));
						} else if (parentContainer.isCanEditExp()) {
							if (color instanceof String) {
								colorExpField.setText((String) color);
							}
						}

						dialog.setVisible(true);
						dialog.dispose();
						dialog = null;
					}
				}
			});
		}

		/**
		 * 取编辑控件
		 */
		public Component getEditorComponent() {
			return editorLabel;
		}

		/**
		 * 取颜色
		 */
		public Object getItem() {
			return color;
		}

		/**
		 * 设置项目
		 */
		public void setItem(Object itemToSet) {
			editorLabel.setText("");
			color = itemToSet;
			editorIcon.setColor(itemToSet);
		}

		/**
		 * 设置颜色
		 * 
		 * @param itemToSet
		 */
		public void setColor(Object itemToSet) {
			if (itemToSet instanceof Color) {
				itemToSet = new Integer(((Color) itemToSet).getRGB());
			}
			setItem(itemToSet);
			((JComboBox) parentContainer).setSelectedItem(itemToSet);
		}

		public void selectAll() {
		}

		public void focusGained(FocusEvent e) {
		}

		public void focusLost(FocusEvent e) {
		}
	}
}

/**
 * JList单元格渲染器
 *
 */
class ColorRendererer extends JLabel implements ListCellRenderer {
	private static final long serialVersionUID = 1L;
	/**
	 * 颜色图标
	 */
	private ColorIcon icon = new ColorIcon();

	/**
	 * 构造函数
	 */
	public ColorRendererer() {
		setOpaque(true);
		setIcon(icon);
		setHorizontalAlignment(CENTER);
		setVerticalAlignment(CENTER);
		setPreferredSize(new Dimension(38, 18));
	}

	/**
	 * 返回显示的组件
	 */
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		Object color = value;
		if (value instanceof Integer) {
			int rgb = ((Integer) value).intValue();
			color = new Color(rgb, true);
		}
		icon.setColor(color);
		setText("");

		if (isSelected) {
			setForeground(list.getSelectionForeground());
			setBackground(list.getSelectionBackground());
		} else {
			setForeground(list.getForeground());
			setBackground(list.getBackground());
		}
		return this;
	}

}
