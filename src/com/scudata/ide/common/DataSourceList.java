package com.scudata.ide.common;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.StringTokenizer;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import com.scudata.common.MessageManager;
import com.scudata.ide.common.resources.IdeCommonMessage;

/**
 * Data source list
 *
 */
public class DataSourceList extends JList<DataSource> {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 * 
	 * @param model
	 */
	public DataSourceList(DataSourceListModel model) {
		this.setModel(model);
		try {
			setCellRenderer(new DataSourceRenderer());
		} catch (Exception x) {
			GM.showException(x);
		}
	}
}

/**
 * Custom cell renderer extends from ListCellRenderer
 *
 */
class DataSourceRenderer extends JLabel implements ListCellRenderer {
	private static final long serialVersionUID = 1L;
	/**
	 * Empty border
	 */
	private static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

	/**
	 * Constructor
	 */
	public DataSourceRenderer() {
		setOpaque(true);
		setBorder(noFocusBorder);
	}

	/**
	 * Override the getListCellRendererComponent method
	 */
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		DataSource ds = (DataSource) value;
		String dispText;
		dispText = ds.getDBInfo().getName();
		MessageManager mm = IdeCommonMessage.get();
		if (ds.isClosed()) {
			setForeground(Color.black);
			if (ds.isSystem()) {
				setText(mm.getMessage("datasourcelist.system") + dispText
						+ mm.getMessage("datasourcelist.notconnect"));
			} else if (ds.isRemote()) {
				setText(mm.getMessage("datasourcelist.remote") + dispText
						+ mm.getMessage("datasourcelist.notconnect"));
			} else {
				setText(dispText + mm.getMessage("datasourcelist.notconnect"));
			}

			if (isSelected) {
				setBackground(list.getSelectionBackground());
			} else {
				setBackground(list.getBackground());
			}
		} else {
			setForeground(Color.magenta);
			if (ds.isSystem()) {
				setText(mm.getMessage("datasourcelist.system") + dispText
						+ mm.getMessage("datasourcelist.connect"));
			} else if (ds.isRemote()) {
				setText(mm.getMessage("datasourcelist.remote") + dispText
						+ mm.getMessage("datasourcelist.connect"));
			} else {
				setText(dispText + mm.getMessage("datasourcelist.connect"));
			}

			if (isSelected) {
				setBackground(list.getSelectionBackground());
			} else {
				setBackground(list.getBackground());
			}
		}

		setBorder((cellHasFocus) ? UIManager
				.getBorder("List.focusCellHighlightBorder") : noFocusBorder);

		return this;
	}

	/**
	 * Calculate the width
	 * 
	 * @param index
	 * @return
	 */
	private int getTab(int index) {
		int defaultTab = 50;
		return defaultTab * index;
	}

	/**
	 * Override the paintComponent method
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Color colorRetainer = g.getColor();
		FontMetrics fm = g.getFontMetrics();

		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		getBorder().paintBorder(this, g, 0, 0, getWidth(), getHeight());

		g.setColor(getForeground());
		g.setFont(getFont());
		Insets insets = getInsets();
		int x = insets.left;
		int y = insets.top + fm.getAscent();

		StringTokenizer st = new StringTokenizer(getText(), "\t");
		while (st.hasMoreTokens()) {
			String sNext = st.nextToken();
			g.drawString(sNext, x, y);
			x += fm.stringWidth(sNext);
			if (!st.hasMoreTokens()) {
				break;
			}
			int index = 0;
			while (x >= getTab(index)) {
				index++;
			}
			x = getTab(index);
		}

		g.setColor(colorRetainer);
	}
}
