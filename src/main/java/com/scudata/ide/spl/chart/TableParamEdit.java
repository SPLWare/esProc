package com.scudata.ide.spl.chart;

import java.awt.Dialog;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;

import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.chart.ChartColor;
import com.scudata.chart.Consts;
import com.scudata.chart.edit.ParamInfo;
import com.scudata.chart.edit.ParamInfoList;
import com.scudata.common.StringUtils;
import com.scudata.dm.Sequence;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.dialog.DialogInputText;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.ide.common.swing.JTextAreaEditor;
import com.scudata.ide.spl.chart.box.DefaultParamTableRender;
import com.scudata.ide.spl.chart.box.EachRowEditor;
import com.scudata.ide.spl.chart.box.EachRowRenderer;
import com.scudata.ide.spl.resources.ChartMessage;
import com.scudata.util.Variant;

/**
 * 参数编辑窗口
 */
public class TableParamEdit extends JTableEx {
	private static final long serialVersionUID = 1L;

	private String FOLDERCOL = " "; // "图标列";
	public String NAMECOL = ChartMessage.get().getMessage("label.propname"); // "属性名称";
	public String VALUECOL = ChartMessage.get().getMessage("label.propvalue"); // "属性值";
	public String EXPCOL = ChartMessage.get().getMessage("label.propexp"); // "属性值表达式";
	public String AXISCOL = ChartMessage.get().getMessage("label.axis"); // "轴名称";
	private String EDITSTYLECOL = "editstyle";
	private String OBJCOL = "objcol"; // ParamInfo对象，如果是null则代表分组行
	private String ARROWCOL = new String(new char[] { 0 });

	public static int iFOLDERCOL = 0;
	public static int iNAMECOL = 1;
	public static int iVALUECOL = 2;
	public static int iEXPCOL = 3;
	public static int iAXISCOL = 4;
	public static int iEDITSTYLECOL = 5;
	public static int iOBJCOL = 6;
	public static int iARROWCOL = 7;

	private int COLCOUNT = 8;

	private HashMap<String, ArrayList<Object[]>> hiddenMap = new HashMap<String, ArrayList<Object[]>>(); // 隐藏行的数据
																											// 类型ArrayList
																											// 每个对象Object[]
	public TableInputSeries seriesTable;

	boolean isAutoHide = false;
	private Dialog owner;

	/**
	 * 构造一个参数编辑表
	 * 
	 * @param owner
	 *            父窗口
	 * @param list
	 *            参数信息列表
	 */
	public TableParamEdit(Dialog owner, ParamInfoList list) {
		this.owner = owner;
		String[] colNames = new String[] { FOLDERCOL, NAMECOL, VALUECOL,
				EXPCOL, AXISCOL, EDITSTYLECOL, OBJCOL, ARROWCOL };
		data.setColumnIdentifiers(colNames);

		// 生成数据
		ArrayList<ParamInfo> root = list.getRootParams();
		for (int i = 0; i < root.size(); i++) {
			ParamInfo pi = (ParamInfo) root.get(i);
			Object row[] = new Object[COLCOUNT];
			row[iFOLDERCOL] = null;
			row[iNAMECOL] = pi.getTitle();
			Object value = pi.getValue();
			if (value instanceof String) {
				if (value.toString().startsWith("=")) {
					row[iEXPCOL] = value.toString().substring(1);
					row[iVALUECOL] = pi.getDefValue();
				} else
					row[iVALUECOL] = value;
			} else {
				if (value instanceof ArrayList) {
					row[iVALUECOL] = arrayList2Series((ArrayList) value);
					pi.setValue(row[iVALUECOL]);
				} else
					row[iVALUECOL] = value;
			}
			row[iAXISCOL] = pi.getAxis();
			row[iEDITSTYLECOL] = new Integer(pi.getInputType());
			row[iOBJCOL] = pi;
			data.addRow(row);
		}
		java.util.List<String> groups = list.getGroupNames();
		if (groups != null) {
			for (int i = 0; i < groups.size(); i++) {
				String groupName = (String) groups.get(i);
				Object[] grow = new Object[COLCOUNT];
				if (i == groups.size() - 1)
					grow[iFOLDERCOL] = new Byte(GC.TYPE_LASTMINUS);
				else
					grow[iFOLDERCOL] = new Byte(GC.TYPE_MINUS);
				grow[iNAMECOL] = groupName;
				grow[iEDITSTYLECOL] = new Integer(Consts.INPUT_NORMAL);
				data.addRow(grow);
				ArrayList<Object[]> grows = new ArrayList<Object[]>();
				ArrayList<ParamInfo> glist = list.getParams(groupName);
				for (int j = 0; j < glist.size(); j++) {
					ParamInfo pi = (ParamInfo) glist.get(j);
					Object row[] = new Object[COLCOUNT];
					if (j == glist.size() - 1)
						row[iFOLDERCOL] = new Byte(GC.TYPE_LASTNODE);
					else
						row[iFOLDERCOL] = new Byte(GC.TYPE_NODE);
					row[iNAMECOL] = pi.getTitle();
					Object value = pi.getValue();
					if (value instanceof String) {
						if (value.toString().startsWith("=")) {
							row[iEXPCOL] = value.toString().substring(1);
							row[iVALUECOL] = pi.getDefValue();
						} else
							row[iVALUECOL] = value;
					} else {
						if (value instanceof ArrayList) {
							row[iVALUECOL] = arrayList2Series((ArrayList<Object>) value);
							pi.setValue(row[iVALUECOL]);
						} else
							row[iVALUECOL] = value;
					}
					row[iAXISCOL] = pi.getAxis();
					row[iEDITSTYLECOL] = new Integer(pi.getInputType());
					row[iOBJCOL] = pi;
					data.addRow(row);
					grows.add(row);
				}
				hiddenMap.put(groupName, grows);
			}
		}

		this.setRowHeight(25);
		DefaultParamTableRender render = new DefaultParamTableRender();
		TableColumn tc;
		tc = getColumn(iFOLDERCOL);
		tc.setMaxWidth(20);
		tc.setMinWidth(20);
		tc.setCellEditor(new ImageEditor());
		tc.setCellRenderer(new ImageRenderer());

		tc = getColumn(iNAMECOL);
		tc.setCellRenderer(render);
		tc.setPreferredWidth(100);
		tc.setMaxWidth(200);

		tc = getColumn(iVALUECOL);
		tc.setCellEditor(new EachRowEditor(this, iEDITSTYLECOL, owner));
		tc.setCellRenderer(new EachRowRenderer(iEDITSTYLECOL));

		tc = getColumn(iEXPCOL);
		tc.setCellEditor(new JTextAreaEditor(this));
		tc.setCellRenderer(render);

		tc = getColumn(iAXISCOL);
		tc.setCellEditor(new JTextAreaEditor(this));
		tc.setCellRenderer(render);

		tc = getColumn(iARROWCOL);
		tc.setMaxWidth(25);
		tc.setMinWidth(25);
		tc.setCellEditor(new SeriesEditor(owner));
		tc.setCellRenderer(new SeriesEditRender());

		setColumnVisible(EDITSTYLECOL, false);
		setColumnVisible(OBJCOL, false);

		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setColumnSelectionAllowed(true); // 列可选
		setRowSelectionAllowed(true); // 行可选
		this.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				if (seriesTable != null) {
					seriesTable.updateParams();
				}
			}

			public void focusLost(FocusEvent e) {
			}
		});
	}

	/**
	 * 自动生成图形时，隐藏多余列
	 */
	public void autoHide() {
		isAutoHide = true;
		setColumnVisible(EXPCOL, false);
		setColumnVisible(AXISCOL, false);
		setColumnVisible(ARROWCOL, false);
	}

	/**
	 * 实现鼠标点击事件
	 * 
	 * @param e
	 *            鼠标事件
	 */
	public void mouseClicked(MouseEvent e) {
		super.mouseClicked(e);
		if (e.getButton() == MouseEvent.BUTTON1) {
			int row = getSelectedRow();
			int col = getSelectedColumn();
			if (col != iFOLDERCOL) { // 鼠标点击行首列时，收缩与展开属性组
				return;
			}
			if (data.getValueAt(row, col) == null
					|| !(data.getValueAt(row, col) instanceof Byte)) {
				return;
			}
			byte oldType = ((Byte) data.getValueAt(row, col)).byteValue();
			byte newType = oldType;
			ArrayList<Object[]> list = new ArrayList<Object[]>();
			Object rowData[];
			String key = (String) data.getValueAt(row, iNAMECOL);
			acceptText();
			switch (oldType) {
			case GC.TYPE_MINUS:
				newType = GC.TYPE_PLUS;
			case GC.TYPE_LASTMINUS:
				newType = GC.TYPE_LASTPLUS;
				while (row + 1 < data.getRowCount()
						&& data.getValueAt(row + 1, iOBJCOL) != null) { // 找到表尾或下一个分组行为止
					rowData = new Object[data.getColumnCount()];
					for (int c = 0; c < data.getColumnCount(); c++) {
						rowData[c] = data.getValueAt(row + 1, c);
					}
					list.add(rowData);
					data.removeRow(row + 1);
				}
				hiddenMap.put(key, list);
				break;
			case GC.TYPE_PLUS:
				newType = GC.TYPE_MINUS;
			case GC.TYPE_LASTPLUS:
				newType = GC.TYPE_LASTMINUS;
				expand(key, row + 1);
				break;
			}
			data.setValueAt(new Byte(newType), row, col);
			acceptText();
		}
	}

	/**
	 * 展开所有参数
	 */
	public void expandAll() {
		int rowCount = data.getRowCount();
		for (int r = rowCount - 1; r >= 0; r--) {
			Object obj = data.getValueAt(r, iFOLDERCOL);
			if (obj == null) {
				continue;
			}
			byte oldType = ((Byte) obj).byteValue();
			if (oldType != GC.TYPE_LASTPLUS)
				continue;
			String key = (String) data.getValueAt(r, iNAMECOL);
			expand(key, r + 1);
			data.setValueAt(new Byte(GC.TYPE_LASTMINUS), r, iFOLDERCOL);
		}
		acceptText();
	}

	/**
	 * 收起参数
	 * 
	 * @param key
	 *            参数的分组键值
	 * @param row
	 *            行号
	 */
	private void collapse(String key, int row) {
		ArrayList<Object[]> list = new ArrayList<Object[]>();
		Object rowData[];
		while (row + 1 < data.getRowCount()
				&& data.getValueAt(row + 1, iOBJCOL) != null) { // 找到表尾或下一个分组行为止
			rowData = new Object[data.getColumnCount()];
			for (int c = 0; c < data.getColumnCount(); c++) {
				rowData[c] = data.getValueAt(row + 1, c);
			}
			list.add(rowData);
			data.removeRow(row + 1);
		}
		hiddenMap.put(key, list);
	}

	/**
	 * 收起所有参数
	 */
	public void collapseAll() {
		int rowCount = data.getRowCount();
		for (int r = rowCount - 1; r >= 0; r--) {
			Object obj = data.getValueAt(r, iFOLDERCOL);
			if (obj == null) {
				continue;
			}
			byte oldType = ((Byte) obj).byteValue();
			if (oldType != GC.TYPE_MINUS && oldType != GC.TYPE_LASTMINUS)
				continue;
			String key = (String) data.getValueAt(r, iNAMECOL);
			collapse(key, r);
			data.setValueAt(new Byte(GC.TYPE_LASTPLUS), r, iFOLDERCOL);
		}
		acceptText();
	}

	/**
	 * 返回格子是否允许编辑
	 * 
	 * @param row
	 *            行
	 * @param column
	 *            列
	 */
	public boolean isCellEditable(int row, int column) {
		ParamInfo info = (ParamInfo) data.getValueAt(row, iOBJCOL);
		if (info == null) { // 分组行不能编辑
			return false;
		}

		if (!isAutoHide) {
			if (column == iAXISCOL && !info.isAxisEnable())
				return false;
		}

		return column != iNAMECOL;
	}

	/**
	 * 某个属性值被修改后触发
	 * 
	 * @param aValue
	 *            属性值
	 * @param row
	 *            行
	 * @param column
	 *            列
	 * 
	 */
	public void setValueAt(Object aValue, int row, int column) {
		Object oldValue = getValueAt(row, column);
		if (oldValue == null)
			oldValue = "";
		if (column == iEXPCOL) {
			aValue = aValue.toString().trim();
			if (!StringUtils.isValidExpression((String) aValue)) {
				JOptionPane.showMessageDialog(GV.appFrame, ChartMessage.get()
						.getMessage("TableParamEdit.invalidexp", aValue));
				return;
			}
		}
		if (Variant.isEquals(aValue, oldValue)) {
			return;
		}
		super.setValueAt(aValue, row, column);
		ParamInfo info = (ParamInfo) data.getValueAt(row, iOBJCOL);
		if (info != null) {
			if (isAutoHide) {// 自动隐藏相关列后，只有值列可以编辑
				info.setValue(aValue);
				return;
			}
			if (column == iAXISCOL) {
				info.setAxis((String) aValue);
				return;
			}
			if (column == iVALUECOL) { // 输入值，要在表达式列显示下拉等输入方式选择的值
				super.setValueAt(toExpString(info, aValue), row, iEXPCOL);
				super.setValueAt("", row, iAXISCOL); // 将轴清空
			} else if (column == iEXPCOL) { // 输入表达式，要将能解析的值显示成值列对应的显示值
				super.setValueAt(toValueObject(info, aValue.toString()), row,
						iVALUECOL);
				if (aValue.toString().trim().length() == iFOLDERCOL) {
					aValue = info.getDefValue();
					super.setValueAt("", row, iAXISCOL); // 将轴清空
				} else
					aValue = "=" + aValue.toString();
			} else
				return;
			info.setValue(aValue);
		}
	}

	private void expand(String groupName, int row) {
		ArrayList<Object[]> list = hiddenMap.get(groupName);
		for (int i = 0; i < list.size(); i++) {
			data.insertRow(row + i, list.get(i));
		}
	}

	/**
	 * 接受当前的编辑值
	 */
	public void acceptText() {
		if (this.isEditing()) {
			this.getCellEditor().stopCellEditing();
		}
	}

	/**
	 * 将参数value根据参数信息的类型转为表达式串
	 * 
	 * @param info
	 *            参数信息
	 * @param value
	 *            参数值
	 * @return 表达式串
	 */
	public static String toExpString(ParamInfo info, Object value) {
		int inputType = info.getInputType();
		switch (inputType) {
		case Consts.INPUT_COLOR:
		case Consts.INPUT_LINESTYLE:
		case Consts.INPUT_TEXTURE:
		case Consts.INPUT_POINTSTYLE:
		case Consts.INPUT_FONTSTYLE:
		case Consts.INPUT_COLUMNSTYLE:
		case Consts.INPUT_DROPDOWN:
		case Consts.INPUT_CHARTCOLOR:
		case Consts.INPUT_ARROW:
		case Consts.INPUT_TICKS:
		case Consts.INPUT_UNIT:
		case Consts.INPUT_COORDINATES:
		case Consts.INPUT_AXISLOCATION:
		case Consts.INPUT_FONTSIZE:
		case Consts.INPUT_HALIGN:
		case Consts.INPUT_VALIGN:
		case Consts.INPUT_LEGENDICON:
		case Consts.INPUT_CUSTOMDROPDOWN:
		case Consts.INPUT_URLTARGET:
			return value == null ? "" : value.toString();
		default:
			return "";
		}
	}

	/**
	 * 将表达式exp按照参数信息info的类型转换为对应的参数值
	 * 
	 * @param info
	 *            参数信息
	 * @param exp
	 *            表达式
	 * @return 参数值
	 */
	public static Object toValueObject(ParamInfo info, String exp) {
		int inputType = info.getInputType();
		switch (inputType) {
		case Consts.INPUT_NORMAL:
			return Variant.parse(exp, true);
		case Consts.INPUT_EXP:
		case Consts.INPUT_DATE:
		case Consts.INPUT_FILE:
			return "";
		case Consts.INPUT_CHARTCOLOR:
			Object value1 = PgmNormalCell.parseConstValue(exp);
			if (TableInputSeries.isChartColor(value1))
				return ChartColor.getInstance((Sequence) value1);
			return info.getDefValue();
		default:
			Object value = PgmNormalCell.parseConstValue(exp);
			if (isRightType(inputType, value))
				return value;
			return info.getDefValue();
		}
	}

	/**
	 * 返回参数值value是否跟描述的类型type一致
	 * 
	 * @param type
	 *            类型定义
	 * @param value
	 *            参数值
	 * @return 一致的话返回true，否则返回false
	 */
	public static boolean isRightType(int type, Object value) {
		switch (type) {
		case Consts.INPUT_ANGLE:
		case Consts.INPUT_ARROW:
		case Consts.INPUT_AXISLOCATION:
		case Consts.INPUT_COLOR:
		case Consts.INPUT_COLUMNSTYLE:
		case Consts.INPUT_COORDINATES:
		case Consts.INPUT_FONTSIZE:
		case Consts.INPUT_FONTSTYLE:
		case Consts.INPUT_HALIGN:
		case Consts.INPUT_VALIGN:
		case Consts.INPUT_INTEGER:
		case Consts.INPUT_LEGENDICON:
		case Consts.INPUT_LINESTYLE:
		case Consts.INPUT_POINTSTYLE:
		case Consts.INPUT_TEXTURE:
		case Consts.INPUT_TICKS:
		case Consts.INPUT_UNIT:
			return value instanceof Integer;
		case Consts.INPUT_DOUBLE:
			return value instanceof Double;
		case Consts.INPUT_FONT:
		case Consts.INPUT_URLTARGET:
		case Consts.INPUT_NORMAL:
			return value instanceof String;
		case Consts.INPUT_CHECKBOX:
			return value instanceof Boolean;
		case Consts.INPUT_CHARTCOLOR:
			return TableInputSeries.isChartColor(value);
		}
		return false;
	}

	/**
	 * 设置序列编辑表
	 * 
	 * @param table
	 *            序列编辑表
	 */
	public void setSeriesTable(TableInputSeries table) {
		this.seriesTable = table;
	}

	private Sequence arrayList2Series(ArrayList<Object> list) {
		Sequence series = new Sequence();
		for (int i = 0; i < list.size(); i++) {
			Object o = list.get(i);
			if (o instanceof ArrayList) {
				series.add(arrayList2Series((ArrayList) o));
			} else
				series.add(o);
		}
		return series;
	}

	/**
	 * 实现鼠标双击时间
	 */
	public void doubleClicked(int xpos, int ypos, int row, int col, MouseEvent e) {
		if (!isCellEditable(row, col)) {
			return;
		}
		if (row > -1 && col == iEXPCOL) {
			DialogInputText dit = new DialogInputText(owner, true);
			String exp = (String) data.getValueAt(row, col);
			dit.setText(exp);
			dit.setVisible(true);
			if (dit.getOption() == JOptionPane.OK_OPTION) {
				acceptText();
				setValueAt(dit.getText(), row, col);
				acceptText();
			}
		}
	}

}
