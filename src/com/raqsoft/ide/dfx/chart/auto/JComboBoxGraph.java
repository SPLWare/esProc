package com.raqsoft.ide.dfx.chart.auto;

import java.awt.*;

import javax.swing.*;

import com.raqsoft.common.*;
import com.raqsoft.cellset.graph.config.*;
import com.raqsoft.ide.common.GM;
import com.raqsoft.ide.dfx.resources.ChartMessage;

/**
 * 统计图类型下拉选择框 
 */
 public class JComboBoxGraph extends JComboBox<Object> {
	private static final long serialVersionUID = 1L;

	public JComboBoxGraph() {
		MessageManager mm = ChartMessage.get();
		setRenderer(new TypeSelectBoxRenderer());

		addItem(new TypeItem(mm.getMessage("GraphType.COL"), GraphTypes.GT_COL)); // 柱形图
		addItem(new TypeItem(mm.getMessage("GraphType.PIE"), GraphTypes.GT_PIE)); // "饼型图"
		addItem(new TypeItem(mm.getMessage("GraphType.LINE"),
				GraphTypes.GT_LINE));
	}

	/**
	 * 获取下拉值
	 * @return 图形类型
	 */
	public byte getValue() {
		return ((TypeItem) getSelectedItem()).getType();
	}

	/**
	 * 设置图形类型
	 * @param type 类型
	 */
	public void setValue(byte type) {
		for (int i = 0; i < getItemCount(); i++) {
			if (((TypeItem) getItemAt(i)).getType() == type) {
				setSelectedIndex(i);
			}
		}
	}

}

class TypeSelectBoxRenderer implements ListCellRenderer<Object> {
	public Component getListCellRendererComponent(JList<?> list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		JLabel label = ((TypeItem) value).getItem();
		if (isSelected) {
			label.setBorder(BorderFactory.createRaisedBevelBorder());
		} else {
			label.setBorder(null);
		}
		return label;
	}
}

class TypeItem {
	private JLabel label;
	private byte type;

	public TypeItem(String title, byte type) {
		String graphIcon = getGraphIcon(type);
		java.io.InputStream is = this.getClass().getResourceAsStream(graphIcon);
		byte[] b;
		ImageIcon icon;
		try {
			b = GM.inputStream2Bytes(is);
			icon = new ImageIcon(b);
		} catch (Exception exc) {
			return;
		}
		Image image = icon.getImage().getScaledInstance(30, 20,
				Image.SCALE_SMOOTH);
		icon.setImage(image);
		label = new JLabel(title, icon, JLabel.HORIZONTAL);
		label.setHorizontalAlignment(JLabel.LEFT);
		this.type = type;
	}

	public JLabel getItem() {
		return label;
	}

	public byte getType() {
		return type;
	}

	public String getGraphIcon(byte graphType) {
		String name = "";
		switch (graphType) {
		case GraphTypes.GT_AREA:
			name = "area";
			break;
		case GraphTypes.GT_AREA3D:
			name = "area3d";
			break;
		case GraphTypes.GT_BAR:
			name = "bar";
			break;
		case GraphTypes.GT_BAR3D:
			name = "bar3d";
			break;
		case GraphTypes.GT_BAR3DOBJ:
			name = "bar3dobj";
			break;
		case GraphTypes.GT_BARSTACKED:
			name = "barstacked";
			break;
		case GraphTypes.GT_BARSTACKED3DOBJ:
			name = "barstacked3dobj";
			break;
		case GraphTypes.GT_COL:
			name = "col";
			break;
		case GraphTypes.GT_COL3D:
			name = "col3d";
			break;
		case GraphTypes.GT_COL3DOBJ:
			name = "col3dobj";
			break;
		case GraphTypes.GT_COLSTACKED:
			name = "colstacked";
			break;
		case GraphTypes.GT_COLSTACKED3DOBJ:
			name = "colstacked3dobj";
			break;
		case GraphTypes.GT_LINE:
			name = "line";
			break;
		case GraphTypes.GT_CURVE:
			name = "curve";
			break;
		case GraphTypes.GT_LINE3DOBJ:
			name = "line3d";
			break;
		case GraphTypes.GT_PIE:
			name = "pie";
			break;
		case GraphTypes.GT_PIE3DOBJ:
			name = "pie3d";
			break;
		case GraphTypes.GT_SCATTER:
			name = "scatter";
			break;
		case GraphTypes.GT_DOT3D:
			name = "dot3d";
			break;
		case GraphTypes.GT_TIMESTATE:
			name = "timestate";
			break;
		case GraphTypes.GT_TIMETREND:
			name = "timetrend";
			break;
		case GraphTypes.GT_2YCOLLINE:
			name = "2ycolline";
			break;
		case GraphTypes.GT_2YCOLSTACKEDLINE:
			name = "2ycolstackedline";
			break;
		case GraphTypes.GT_2Y2LINE:
			name = "2y2line";
			break;
		case GraphTypes.GT_RADAR:
			name = "radar";
			break;
		case GraphTypes.GT_GANTT:
			name = "gantt";
			break;
		case GraphTypes.GT_METER:
			name = "meter";
			break;
		case GraphTypes.GT_METER3D:
			name = "meter3d";
			break;
		case GraphTypes.GT_MILEPOST:
			name = "milepost";
			break;
		case GraphTypes.GT_RANGE:
			name = "range";
			break;
		case GraphTypes.GT_GONGZI:
			name = "gongzi";
			break;
		}
		return "/com/raqsoft/ide/dfx/chart/image/graph_" + name + ".gif";
	}
	
}
