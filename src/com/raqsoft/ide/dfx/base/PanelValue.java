package com.raqsoft.ide.dfx.base;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import com.raqsoft.cellset.datamodel.PgmCellSet;
import com.raqsoft.ide.common.EditListener;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.dfx.GVDfx;
import com.raqsoft.ide.dfx.resources.IdeDfxMessage;

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
	 * 调试执行时间
	 */
	private JLabel labelTime = new JLabel();

	/**
	 * 构造函数
	 */
	public PanelValue() {
		GVDfx.panelValue = this;
		this.setLayout(new BorderLayout());
		setMinimumSize(new Dimension(0, 0));
		valueBar = new PanelValueBar();
		add(valueBar, BorderLayout.NORTH);
		tableValue = new JTableValue();
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
		JPanel panelSign = new JPanel(new GridBagLayout());
		panelSign.add(labelTime, GM.getGBC(0, 0, true, false, 4));
		panelSouthSign.add(CARD_SIGN, panelSign);
		panelSouthSign.add(CARD_EMPTY, new JPanel());
		cl.show(panelSouthSign, CARD_EMPTY);
		panelSouthSign.setVisible(false);
		this.add(panelSouthSign, BorderLayout.SOUTH);
	}

	/** 签名面板 */
	private static final String CARD_SIGN = "CARD_SIGN";
	/** 空面板 */
	private static final String CARD_EMPTY = "CARD_EMPTY";
	/**
	 * 用于切换是否显示签名的卡片布局
	 */
	private CardLayout cl = new CardLayout();
	/**
	 * 签名面板。不显示签名时切换称空面板
	 */
	private JPanel panelSouthSign = new JPanel(cl);

	/**
	 * 设置网格
	 * 
	 * @param cs
	 *            网格对象
	 */
	public void setCellSet(PgmCellSet cs) {
		if (cs == null) {
			panelSouthSign.setVisible(false);
			cl.show(panelSouthSign, CARD_EMPTY);
		} else {
			cl.show(panelSouthSign, CARD_SIGN);
			panelSouthSign.setVisible(true);
		}
	}

	/**
	 * 设置调试执行时间，单位毫秒
	 * 
	 * @param time
	 */
	public void setDebugTime(Long time) {
		if (time == null) {
			labelTime.setText(null);
		} else {
			labelTime.setText(IdeDfxMessage.get().getMessage(
					"panelvalue.debugtime", time));
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
