package com.scudata.ide.spl.base;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.ide.common.EditListener;
import com.scudata.ide.common.GM;
import com.scudata.ide.spl.GVSpl;
import com.scudata.ide.spl.resources.IdeSplMessage;

/**
 * 值面板
 */
public class PanelValue extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * 滚动面板
	 */
	public JScrollPane spValue;

	/**
	 * 表格控件
	 */
	public JTableValue tableValue;

	/**
	 * 值工具条面板
	 */
	public PanelValueBar valueBar;

	/**
	 * 滚动条
	 */
	public JScrollBar sbValue;

	/**
	 * 阻止变化
	 */
	public boolean preventChange = false;
	/**
	 * 单元格调试执行时间
	 */
	private JLabel jLCellTime = new JLabel();

	/**
	 * 单元格调试执行时间间隔
	 */
	private JLabel jLInterval = new JLabel();

	private JLabel jLDispRows1 = new JLabel(IdeSplMessage.get().getMessage(
			"panelvalue.disprows1"));
	private JLabel jLDispRows2 = new JLabel(IdeSplMessage.get().getMessage(
			"panelvalue.disprows2"));

	/**
	 * 显示的最大行数面板
	 */
	private JSpinner jSDispRows = new JSpinner(new SpinnerNumberModel(100, 1,
			Integer.MAX_VALUE, 1));

	/**
	 * 游标加载数据按钮
	 */
	private JButton jBCursorFetch = new JButton(IdeSplMessage.get().getMessage(
			"panelvalue.cursorfetch")); // 加载数据

	/**
	 * 构造函数
	 */
	public PanelValue() {
		GVSpl.panelValue = this;
		this.setLayout(new BorderLayout());
		setMinimumSize(new Dimension(0, 0));
		valueBar = new PanelValueBar();
		add(valueBar, BorderLayout.NORTH);
		tableValue = new JTableValue(this);
		GM.loadWindowSize(this);
		spValue = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		spValue.getViewport().add(tableValue);
		tableValue.addMWListener(spValue);
		add(spValue, BorderLayout.CENTER);
		sbValue = new JScrollBar();
		sbValue.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				if (preventChange)
					return;
				int index = e.getValue();
				if (index > 0) {
					tableValue.resetData(index);
				}
			}
		});
		this.add(sbValue, BorderLayout.EAST);
		tableValue.setValue(null);

		panelDebug.add(jLCellTime, GM.getGBC(0, 0, true, false, 2));
		panelDebug.add(jLInterval, GM.getGBC(0, 1, true));

		panelCursor.add(jLDispRows1, GM.getGBC(0, 0, false, false, 2));
		panelCursor.add(jSDispRows, GM.getGBC(0, 1, false, false, 0));
		panelCursor.add(jLDispRows2, GM.getGBC(0, 2, false, false, 2));
		panelCursor.add(jBCursorFetch, GM.getGBC(0, 3));
		panelCursor.add(new JPanel(), GM.getGBC(0, 4, true));

		JPanel panelAction = new JPanel(new GridBagLayout());
		panelAction.add(panelCursor, GM.getGBC(0, 0, true));
		panelAction.add(panelDebug, GM.getGBC(1, 0, true));

		panelSouth.add(CARD_DEBUG, panelAction);
		panelSouth.add(CARD_EMPTY, new JPanel());
		cl.show(panelSouth, CARD_EMPTY);
		panelSouth.setVisible(false);
		this.add(panelSouth, BorderLayout.SOUTH);
		jBCursorFetch.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Object value = jSDispRows.getValue();
				if (!(value instanceof Number))
					return;
				int dispRows = ((Number) value).intValue();
				tableValue.cursorFetch(dispRows);
			}
		});
		Dimension dim = new Dimension(100, 25);
		jSDispRows.setPreferredSize(dim);
	}

	public void setCursorValue(boolean isCursor) {
		jLDispRows1.setVisible(isCursor);
		jSDispRows.setVisible(isCursor);
		jLDispRows2.setVisible(isCursor);
		jBCursorFetch.setVisible(isCursor);
		panelCursor.setVisible(isCursor);
	}

	/** 调试面板 */
	private static final String CARD_DEBUG = "CARD_DEBUG";
	/** 空面板 */
	private static final String CARD_EMPTY = "CARD_EMPTY";
	/**
	 * 用于切换是否显示签名的卡片布局
	 */
	private CardLayout cl = new CardLayout();
	/**
	 * 调试面板。不调试时切换为空面板
	 */
	private JPanel panelSouth = new JPanel(cl);

	private JPanel panelDebug = new JPanel(new GridBagLayout());
	private JPanel panelCursor = new JPanel(new GridBagLayout());

	/**
	 * 设置网格
	 * 
	 * @param cs
	 *            网格对象
	 */
	public void setCellSet(PgmCellSet cs) {
		if (cs == null) {
			panelSouth.setVisible(false);
			cl.show(panelSouth, CARD_EMPTY);
		} else {
			cl.show(panelSouth, CARD_DEBUG);
			panelSouth.setVisible(true);
		}
	}

	/**
	 * 设置调试执行时间，单位毫秒
	 * 
	 * @param time
	 */
	public void setDebugTime(String cellId, Long time) {
		if (cellId == null || time == null) {
			jLCellTime.setText(null);
		} else {
			jLCellTime.setText(IdeSplMessage.get().getMessage(
					"panelvalue.debugtime", cellId, time));
		}
	}

	public void setInterval(String cellId1, String cellId2, Long interval) {
		if (cellId1 == null || cellId2 == null) {
			jLInterval.setText(null);
		} else {
			jLInterval.setText(IdeSplMessage.get().getMessage(
					"panelvalue.cellinterval", cellId1, cellId2, interval));
		}
	}

	/**
	 * 设置编辑监听器
	 * 
	 * @param el
	 */
	public void setEditListener(EditListener el) {
		tableValue.setEditListener(el);
	}
}
