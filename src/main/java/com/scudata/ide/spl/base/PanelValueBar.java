package com.scudata.ide.spl.base;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.spl.GCSpl;
import com.scudata.ide.spl.GVSpl;
import com.scudata.ide.spl.MenuSpl;
import com.scudata.ide.spl.resources.IdeSplMessage;

/**
 * 值面板的工具条
 *
 */
public class PanelValueBar extends JPanel {
	private static final long serialVersionUID = 1L;

	/**
	 * 撤回
	 */
	private JButton jBUndo = new JButton();
	/**
	 * 重做
	 */
	private JButton jBRedo = new JButton();
	/**
	 * 字段内容
	 */
	private JButton jBCell = new JButton();
	/**
	 * 复制数据
	 */
	private JButton jBCopy = new JButton();
	/**
	 * 复制字段名
	 */
	private JButton jBCopyColNames = new JButton();

	/**
	 * 绘制图形
	 */
	private JButton jBDrawChart = new JButton();

	/**
	 * 显示长文本
	 */
	private JButton jBShowText = new JButton();

	/**
	 * 单元格坐标
	 */
	private JLabel labelCell = new JLabel();

	/**
	 * 锁定按钮
	 */
	private JToggleButton jBPin = new JToggleButton();

	/**
	 * 集算器资源管理器
	 */
	private MessageManager splMM = IdeSplMessage.get();

	/** 显示固定格值 */
	private final String PIN_ON = splMM.getMessage("panelvaluebar.pin1");
	/** 显示焦点格值 */
	private final String PIN_OFF = splMM.getMessage("panelvaluebar.pin2");

	/** 撤回 */
	private final short iUNDO = 1;
	/** 重做 */
	private final short iREDO = 3;
	/** 固定显示单元格值 */
	private final short iPIN = 5;
	/** 字段内容 */
	private final short iDRILLCELL = 6;
	/** 绘制图形 */
	private final short iDRAWCHART = 7;
	/** 复制数据 */
	private final short iCOPY = 8;
	/** 复制列名 */
	private final short iCOPY_COLNAMES = 9;
	/** 显示长文本 */
	private final short iSHOW_TEXT = 10;

	/**
	 * 方便集文件浏览集成时，不需要Pin属性，允许灰掉该按钮
	 */
	private boolean disablePin = false;

	/**
	 * 方便集文件浏览集成时，不需要Pin属性，允许灰掉该按钮
	 */
	public void disablePin() {
		disablePin = true;
	}

	/**
	 * 构造函数
	 */
	public PanelValueBar() {
		setLayout(new GridBagLayout());
		add(labelCell, getGBC(1, 1, true));
		jBUndo.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "m_pmtundo.gif"));
		jBUndo.setToolTipText(splMM.getMessage("panelvaluebar.undo")); // 后退
		initButton(jBUndo, iUNDO);
		add(jBUndo, getGBC(1, 2));
		jBRedo.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "m_pmtredo.gif"));
		jBRedo.setToolTipText(splMM.getMessage("panelvaluebar.redo")); // 前进
		initButton(jBRedo, iREDO);
		add(jBRedo, getGBC(1, 3));
		jBCell.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "m_pmtcell.gif"));
		jBCell.setToolTipText(splMM.getMessage("panelvaluebar.cell")); // 字段内容
		initButton(jBCell, iDRILLCELL);
		add(jBCell, getGBC(1, 4));
		jBCopy.setIcon(GM.getMenuImageIcon("copy"));
		jBCopy.setToolTipText(TIP_COPY_CONTENT); // 复制
		initButton(jBCopy, iCOPY);
		add(jBCopy, getGBC(1, 5));

		jBCopyColNames.setIcon(GM.getMenuImageIcon("coldefine"));
		jBCopyColNames.setToolTipText(TIP_COPY_COLNAMES); // 复制列名
		initButton(jBCopyColNames, iCOPY_COLNAMES);
		add(jBCopyColNames, getGBC(1, 6));

		jBDrawChart.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "m_drawchart.gif"));
		jBDrawChart.setToolTipText(splMM.getMessage("panelvaluebar.chart")); // 图形绘制
		initButton(jBDrawChart, iDRAWCHART);
		add(jBDrawChart, getGBC(1, 7));

		jBShowText.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "b_showtext.gif"));
		jBShowText.setToolTipText(JTableValue.LABEL_VIEW_TEXT); // 查看长文本
		initButton(jBShowText, iSHOW_TEXT);
		add(jBShowText, getGBC(1, 8));

		jBPin.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "m_pin.gif"));
		jBPin.setToolTipText(PIN_OFF);
		initButton(jBPin, iPIN);
		jBPin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setLocked(jBPin.isSelected());
			}
		});
		add(jBPin, getGBC(1, 9));
	}

	/**
	 * 设置是否固定单元格值
	 * 
	 * @param isLocked
	 */
	public void setLocked(boolean isLocked) {
		if (isLocked) {
			jBPin.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "m_pin2.gif"));
			jBPin.setToolTipText(PIN_ON);
		} else {
			jBPin.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "m_pin.gif"));
			jBPin.setToolTipText(PIN_OFF);
		}
	}

	/** 复制数据 */
	private final String TIP_COPY_CONTENT = splMM.getMessage("panelvaluebar.copy");
	/** 复制列名 */
	private final String TIP_COPY_COLNAMES = splMM.getMessage("panelvaluebar.copycolnames");

	/**
	 * 取GridBagConstraints对象
	 * 
	 * @param r
	 * @param c
	 * @return
	 */
	private GridBagConstraints getGBC(int r, int c) {
		return getGBC(r, c, false);
	}

	/**
	 * 取GridBagConstraints对象
	 * 
	 * @param r
	 * @param c
	 * @param b
	 * @return
	 */
	private GridBagConstraints getGBC(int r, int c, boolean b) {
		GridBagConstraints gbc = GM.getGBC(r, c, b);
		gbc.insets = new Insets(3, 3, 3, 3);
		return gbc;
	}

	/**
	 * 刷新
	 */
	public void refresh() {
		JTableValue table = GVSpl.panelValue.tableValue;
		String id = table.getCellId();
		if (StringUtils.isValidString(id)) {
			labelCell.setText(id);
		} else {
			labelCell.setText(null);
		}

		jBCell.setEnabled(table.getSelectedRow() > -1 && table.getSelectedColumn() > -1);
		jBUndo.setEnabled(table.canUndo());
		jBRedo.setEnabled(table.canRedo());
		jBCopy.setEnabled(!table.valueIsNull());
		jBCopyColNames.setEnabled(!table.valueIsNull());
		jBDrawChart.setEnabled(table.canDrawChart());
		jBShowText.setEnabled(table.canShowText());
		if (disablePin) {
			jBPin.setEnabled(false);
		} else {
			jBPin.setEnabled(StringUtils.isValidString(id));
		}
		if (GV.appMenu instanceof MenuSpl) {
			((MenuSpl) GV.appMenu).setMenuEnabled(GCSpl.iDRAW_CHART, table.canDrawChart());
		}
	}

	/**
	 * 设置是否可用
	 */
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		jBCell.setEnabled(enabled);
		jBUndo.setEnabled(enabled);
		jBRedo.setEnabled(enabled);
		jBCopy.setEnabled(enabled);
		jBDrawChart.setEnabled(enabled);
		jBPin.setEnabled(enabled);
	}

	/**
	 * 初始化按钮
	 * 
	 * @param button
	 * @param cmd
	 */
	private void initButton(AbstractButton button, short cmd) {
		Dimension bSize = new Dimension(25, 25);
		button.setMinimumSize(bSize);
		button.setMaximumSize(bSize);
		button.setPreferredSize(bSize);
		button.setName(String.valueOf(cmd));
		button.addActionListener(popupAction);
		button.setEnabled(false);
	}

	/**
	 * 右键弹出菜单监听
	 */
	ActionListener popupAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			AbstractButton button = (AbstractButton) e.getSource();
			short cmd = Short.parseShort(button.getName());
			JTableValue table = GVSpl.panelValue.tableValue;
			switch (cmd) {
			case iUNDO:
				table.undo();
				break;
			case iREDO:
				table.redo();
				break;
			case iDRILLCELL:
				table.dispCellValue();
				break;
			case iCOPY:
				table.copyValue(e.getModifiers() == 17);
				break;
			case iCOPY_COLNAMES:
				table.copyColumnNames();
				break;
			case iPIN:
				table.setLocked(!table.isLocked());
				if (!table.isLocked()) { // 解锁
					GVSpl.appSheet.refresh();
				}
				break;
			case iDRAWCHART:
				table.drawChart();
				break;
			case iSHOW_TEXT:
				table.showText();
				break;
			}
		}
	};

}
