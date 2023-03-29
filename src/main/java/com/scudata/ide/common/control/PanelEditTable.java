package com.scudata.ide.common.control;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.util.HashSet;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;

import com.scudata.app.common.Section;
import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Param;
import com.scudata.dm.Table;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.util.Variant;

/**
 * 常序表编辑面板
 */
public class PanelEditTable extends JPanel {
	private static final long serialVersionUID = 1L;

	/**
	 * 常规页
	 */
	private final byte TAB_NORMAL = 0;
	/**
	 * 数据页
	 */
	private final byte TAB_DATA = 1;

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/** 序号列 */
	private final byte COL_INDEX = 0;
	/** 名称列 */
	private final byte COL_NAME = 1;
	/** 主键列 */
	private final byte COL_PK = 2;

	/** 序号列标题 */
	private final String STR_INDEX = mm.getMessage("paneledittable.index");

	/**
	 * 常规表对象。序号,名称,主键
	 */
	private JTableEx tableNormal = new JTableEx(
			mm.getMessage("paneledittable.tablenormal")) {
		private static final long serialVersionUID = 1L;

		/**
		 * 双击列名格，弹出文本编辑对话框
		 */
		public void doubleClicked(int xpos, int ypos, int row, int col,
				MouseEvent e) {
			switch (col) {
			case COL_NAME:
				GM.dialogEditTableText(tableNormal, row, col);
				break;
			}
		}

		/**
		 * 提交列名时，表结构变化
		 */
		public void setValueAt(Object aValue, int row, int column) {
			if (!isItemDataChanged(row, column, aValue)) {
				return;
			}
			String oldName = null;
			if (column == COL_NAME && getValueAt(row, column) != null) {
				oldName = (String) data.getValueAt(row, column);
			}

			if (preventChange) {
				super.setValueAt(aValue, row, column);
				return;
			}
			if (column == COL_NAME) {
				String newName = aValue == null ? null : (String) aValue;
				if (!alterTable(newName, oldName)) {
					return;
				}
				super.setValueAt(aValue, row, column);
				tableStructChanged();
			} else {
				super.setValueAt(aValue, row, column);
			}
		}
	};

	/**
	 * JTabbedPane对象
	 */
	private JTabbedPane jTabMain = new JTabbedPane();

	/**
	 * 数据表对象
	 */
	private JTableEx tableData;
	/**
	 * 是否阻止变化
	 */
	private boolean preventChange = false;

	/**
	 * 常量对象
	 */
	private Param param;

	/**
	 * 常序表对象
	 */
	private Table table;

	/**
	 * 构造函数
	 * 
	 * @param param
	 *            常量对象
	 */
	public PanelEditTable(Param param) {
		try {
			preventChange = true;
			this.param = param;
			rqInit();
			this.table = (Table) param.getValue();
			initConstTable();
		} catch (Exception ex) {
			GM.showException(ex);
		} finally {
			preventChange = false;
		}
	}

	/**
	 * 获取常量对象
	 * 
	 * @return
	 */
	public Param getParam() {
		table.dataStruct().setPrimary(getPrimary());
		param.setValue(table);
		return param;
	}

	/**
	 * 修改列名
	 * 
	 * @param newName
	 *            新列名
	 * @param oldName
	 *            旧列名
	 * @return
	 */
	private boolean alterTable(String newName, String oldName) {
		return alterTable(newName, oldName, -1);
	}

	/**
	 * 修改列名
	 * 
	 * @param newName
	 *            新列名
	 * @param oldName
	 *            旧列名
	 * @param row
	 *            行号
	 * @return
	 */
	private boolean alterTable(String newName, String oldName, int row) {
		if (!StringUtils.isValidString(newName)
				&& !StringUtils.isValidString(oldName)) {
			return false;
		}
		int rowCount = tableNormal.getRowCount();
		if (rowCount < 0) {
			return false;
		}
		if (StringUtils.isValidString(newName)
				&& (table == null || table.dataStruct() == null || table
						.dataStruct().getFieldCount() == 0)) {
			table = new Table(new String[] { newName });
			return true;
		}
		String oldNames[];
		String newNames[];
		String nNames[] = table.dataStruct().getFieldNames();
		if (StringUtils.isValidString(oldName)
				&& StringUtils.isValidString(newName)) { // rename
			oldNames = new String[nNames.length];
			newNames = new String[nNames.length];
			int index = -1;
			for (int i = 0; i < nNames.length; i++) {
				if (oldName.equals(nNames[i])) {
					index = i;
					break;
				}
			}
			System.arraycopy(nNames, 0, oldNames, 0, nNames.length);
			System.arraycopy(nNames, 0, newNames, 0, nNames.length);
			if (index > -1) {
				newNames[index] = newName;
			}
		} else if (StringUtils.isValidString(oldName)) { // delete
			oldNames = new String[nNames.length - 1];
			newNames = new String[nNames.length - 1];
			int index = -1;
			for (int i = 0; i < nNames.length; i++) {
				if (oldName.equals(nNames[i])) {
					index = i;
					break;
				}
			}
			if (index > 0) {
				System.arraycopy(nNames, 0, oldNames, 0, index);
				System.arraycopy(nNames, 0, newNames, 0, index);
			}
			if (index != nNames.length - 1) {
				System.arraycopy(nNames, index + 1, oldNames, index,
						nNames.length - 1 - index);
				System.arraycopy(nNames, index + 1, newNames, index,
						nNames.length - 1 - index);
			}
		} else {
			oldNames = new String[nNames.length + 1];
			newNames = new String[oldNames.length];
			if (row >= 0) { // insert
				if (row > 0) {
					System.arraycopy(nNames, 0, oldNames, 0, row);
					System.arraycopy(nNames, 0, newNames, 0, row);
				}
				newNames[row] = newName;
				System.arraycopy(nNames, row, oldNames, row + 1,
						oldNames.length - row - 1);
				System.arraycopy(nNames, row, newNames, row + 1,
						oldNames.length - row - 1);
			} else { // add
				System.arraycopy(nNames, 0, oldNames, 0, nNames.length);
				System.arraycopy(nNames, 0, newNames, 0, nNames.length);
				newNames[nNames.length] = newName;
			}
		}
		try {
			table.alter(newNames, oldNames);
		} catch (Exception e) {
			GM.showException(e);
			return false;
		}
		return true;
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	void rqInit() throws Exception {
		JPanel panelNormal = new JPanel(new GridBagLayout());
		jTabMain.addChangeListener(new PanelEditTable_this_changeAdapter(this));
		panelNormal.add(new JScrollPane(tableNormal),
				GM.getGBC(0, 0, true, true));

		jTabMain.add(panelNormal, mm.getMessage("paneledittable.normal")); // 常规

		tableData = new JTableEx() {
			private static final long serialVersionUID = 1L;

			public void doubleClicked(int xpos, int ypos, int row, int col,
					MouseEvent e) {
				if (col <= COL_INDEX) {
					return;
				}
				GM.dialogEditTableText(tableData, row, col);
			}

			public void setValueAt(Object aValue, int row, int column) {
				if (!isItemDataChanged(row, column, aValue)) {
					return;
				}
				if (!preventChange) {
					BaseRecord record = table.getRecord(row + 1);
					int nc = record.getFieldCount();
					column--;
					if (column < nc) {
						if (aValue instanceof String) {
							aValue = PgmNormalCell
									.parseConstValue((String) aValue);
						}
						record.set(column, aValue);
					}
					column++;
				}
				super.setValueAt(aValue, row, column);
			}
		};

		tableData.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tableData.setRowHeight(20);
		tableData.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableData.getTableHeader().setReorderingAllowed(false);
		tableData.setClickCountToStart(1);

		jTabMain.add(new JScrollPane(tableData),
				mm.getMessage("paneledittable.data")); // 数据
		initTable(tableNormal);
		tableNormal.setColumnCheckBox(COL_PK);
		tableNormal.getColumn(COL_PK).setMaxWidth(80);
		tableNormal.setColumnWidth(COL_PK, 80);
		tableNormal.getColumn(COL_PK).setMaxWidth(500);

		this.setLayout(new BorderLayout());
		this.add(jTabMain, BorderLayout.CENTER);
	}

	/**
	 * 初始化常量表
	 */
	private void initConstTable() {
		if (table == null) {
			return;
		}
		DataStruct ds = table.dataStruct();
		if (ds == null) {
			return;
		}
		// 设置普通字段
		initNormalTable(ds.getFieldNames(), ds.getPrimary());
		// 常排列数据
		resetTableData();
	}

	/**
	 * 初始化字段和主键
	 * 
	 * @param normalNames
	 *            字段名
	 * @param primarys
	 *            主键名
	 */
	private void initNormalTable(String[] normalNames, String[] primarys) {
		if (normalNames == null) {
			return;
		}
		Section pks = null;
		if (primarys != null) {
			pks = new Section(primarys);
		}
		for (int i = 0; i < normalNames.length; i++) {
			tableNormal.addRow();
			tableNormal.data.setValueAt(normalNames[i], i, COL_NAME);
			Boolean isPk = Boolean.FALSE;
			if (pks != null && pks.containsSection(normalNames[i])) {
				isPk = Boolean.TRUE;
			}
			tableNormal.data.setValueAt(isPk, i, COL_PK);
		}
	}

	/**
	 * 获取当前正在编辑的JTable对象
	 * 
	 * @return
	 */
	private JTableEx getEditingTable() {
		switch (jTabMain.getSelectedIndex()) {
		case TAB_NORMAL:
			return tableNormal;
		case TAB_DATA:
			return tableData;
		}
		return null;
	}

	/**
	 * 检查数据
	 * 
	 * @return
	 */
	public boolean checkData() {
		tableNormal.acceptText();
		tableData.acceptText();

		HashSet<String> keys = new HashSet<String>();
		String key;
		int count = tableNormal.getRowCount();
		if (count > 0) {
			for (int i = 0; i < count; i++) {
				key = (String) tableNormal.data.getValueAt(i, COL_NAME);
				if (!StringUtils.isValidString(key)) {
					jTabMain.setSelectedIndex(TAB_NORMAL);
					GM.messageDialog(
							GV.appFrame,
							mm.getMessage("paneledittable.emptyname",
									String.valueOf(i + 1))); // 第：{0}行字段名为空。
					return false;
				}
				if (keys.contains(key)) {
					jTabMain.setSelectedIndex(TAB_NORMAL);
					GM.messageDialog(
							GV.appFrame,
							mm.getMessage("paneledittable.existname",
									String.valueOf(i + 1))); // 第：{0}行字段名重复。
					return false;
				}
				keys.add(key);
			}
		} else {
			int option = GM.optionDialog(GV.appFrame,
					mm.getMessage("paneledittable.norow"), // 表结构至少得有一个字段，是否增加缺省字段？
					mm.getMessage("public.prompt"), // 提示
					JOptionPane.OK_CANCEL_OPTION);
			switch (option) {
			case JOptionPane.OK_OPTION:
				jTabMain.setSelectedIndex(TAB_NORMAL);
				addRow();
				break;
			case JOptionPane.CANCEL_OPTION:
				return false;
			default:
				return false;
			}
		}
		return true;
	}

	/**
	 * 获取主键
	 * 
	 * @return
	 */
	private String[] getPrimary() {
		Section pks = new Section();
		Object pk;
		for (int i = 0; i < tableNormal.getRowCount(); i++) {
			pk = tableNormal.data.getValueAt(i, COL_PK);
			if (pk != null) {
				if (((Boolean) pk).booleanValue()) {
					pks.addSection((String) tableNormal.data.getValueAt(i,
							COL_NAME));
				}
			}
		}
		return pks.toStringArray();
	}

	/**
	 * 行上移
	 */
	public void rowUp() {
		JTableEx editingTable = getEditingTable();
		if (editingTable == null) {
			return;
		}
		editingTable.acceptText();
		int row = editingTable.getSelectedRow();
		if (row < 1) {
			return;
		}
		editingTable.shiftRowUp(row);
	}

	/**
	 * 行下移
	 */
	public void rowDown() {
		JTableEx editingTable = getEditingTable();
		if (editingTable == null) {
			return;
		}
		int row = editingTable.getSelectedRow();
		if (row < 0 || row == editingTable.getRowCount() - 1) {
			return;
		}
		editingTable.shiftRowDown(row);
	}

	/**
	 * 增加行
	 */
	public void addRow() {
		JTableEx editingTable = getEditingTable();
		if (editingTable == null) {
			return;
		}
		editingTable.acceptText();
		if (editingTable.equals(tableData)) {
			if (table == null) {
				return;
			}
			int count = tableData.getSelectedRowCount();
			if (count == 0) {
				count = 1;
			}
			for (int i = 0; i < count; i++) {
				tableData.addRow();
				table.newLast();
			}
			int rowCount = tableData.getRowCount();
			tableData.setRowSelectionInterval(rowCount - count, rowCount - 1);
		} else {
			String name = null;
			switch (jTabMain.getSelectedIndex()) {
			case TAB_NORMAL:
				name = GM.getTableUniqueName(tableNormal, COL_NAME, "col");
				break;
			}
			int r = editingTable.addRow();
			editingTable.data.setValueAt(name, r, COL_NAME);
			if (jTabMain.getSelectedIndex() == TAB_NORMAL) {
				tableNormal.data.setValueAt(Boolean.FALSE, r, COL_PK);
				alterTable(name, null);
				tableStructChanged();
			}
		}
	}

	/**
	 * 插入行
	 */
	public void insertRow() {
		JTableEx editingTable = getEditingTable();
		if (editingTable == null) {
			return;
		}
		editingTable.acceptText();
		int selectedRow = editingTable.getSelectedRow();
		if (selectedRow < 0) {
			selectedRow = 0;
		}
		if (editingTable.equals(tableData)) {
			if (table == null) {
				return;
			}
			tableData.insertRow(selectedRow,
					new Object[tableData.getColumnCount()]);
			table.insert(selectedRow + 1);
		} else {
			String name = null;
			switch (jTabMain.getSelectedIndex()) {
			case TAB_NORMAL:
				name = GM.getTableUniqueName(tableNormal, COL_NAME, "col");
				break;
			// case TAB_ALIAS:
			// name = GM.getTableUniqueName(tableAlias, COL_NAME, "alias");
			// break;
			}
			editingTable.insertRow(selectedRow,
					new Object[editingTable.getColumnCount()]);
			editingTable.data.setValueAt(name, selectedRow, COL_NAME);
			if (jTabMain.getSelectedIndex() == TAB_NORMAL) {
				tableNormal.data.setValueAt(Boolean.FALSE, selectedRow, COL_PK);
				// tableNormal.data.setValueAt(new Integer(ORDER_NONE),
				// selectedRow, COL_ORDER);
				// if (isConst) {
				alterTable(name, null, selectedRow);
				// }
				tableStructChanged();
			}
			// else if (jTabMain.getSelectedIndex() == TAB_ALIAS) {
			// tableAlias.data.setValueAt("exp", selectedRow, COL_EXP);
			// if (isConst) {
			// dscTable();
			// }
			// tableStructChanged();
			// }
		}
		// resetExp();
	}

	public void deleteRows() {
		JTableEx editingTable = getEditingTable();
		if (editingTable == null) {
			return;
		}
		if (editingTable.getSelectedRowCount() == 0) {
			return;
		}

		if (editingTable.equals(tableNormal)) {
			if (tableNormal.getRowCount() == 1) {
				GM.messageDialog(GV.appFrame,
						mm.getMessage("paneledittable.atleastonerow")); // 表结构至少得有一个字段。
				// 数据结构的字段数目不能为0
				return;
			}

			int row = tableNormal.getSelectedRow();
			String oldName = null;
			if (tableNormal.data.getValueAt(row, COL_NAME) != null) {
				oldName = (String) tableNormal.data.getValueAt(row, COL_NAME);
			}
			alterTable(null, oldName);
			editingTable.deleteSelectedRow();
			tableStructChanged();
		} else {
			editingTable.deleteSelectedRow();
			tableStructChanged();
			if (editingTable.equals(tableData)) {
				int rows[] = tableData.getSelectedRows();
				for (int i = rows.length - 1; i >= 0; i--) {
					table.delete(rows[i] + 1);
				}
			}
		}

	}

	/**
	 * 初始化JTable控件
	 * 
	 * @param table
	 */
	private void initTable(JTableEx table) {
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(20);
		table.getTableHeader().setReorderingAllowed(false);
		table.setIndexCol(COL_INDEX);
		table.setClickCountToStart(1);
	}

	/**
	 * 表结构发生变化
	 */
	private void tableStructChanged() {
		if (preventChange) {
			return;
		}
		resetTableData();
	}

	/**
	 * 重置表数据
	 */
	private void resetTableData() {
		tableData.data.setRowCount(0);
		int colCount = tableData.getColumnCount();
		for (int i = colCount - 1; i >= 0; i--) {
			tableData.deleteColumn(tableData.getColumn(i));
		}
		if (table == null) {
			return;
		}
		DataStruct ds = table.dataStruct();
		if (ds == null) {
			return;
		}
		String[] colNames = ds.getFieldNames();
		if (colNames != null) {
			tableData.addColumn(STR_INDEX);
			for (int i = 0; i < colNames.length; i++) {
				tableData.addColumn(colNames[i]);
			}
		}
		if (tableData.getColumnCount() > 0) {
			tableData.setIndexCol(COL_INDEX);
		}
		resetTableDataStyle();

		int rowCount = table.length();
		if (rowCount == 0) {
			return;
		}
		for (int i = 1; i <= rowCount; i++) {
			BaseRecord record = table.getRecord(i);
			int r = tableData.addRow();
			for (int j = 0; j < colNames.length; j++) {
				Object val = null;
				try {
					val = record.getFieldValue(colNames[j]);
					val = Variant.toString(val);
				} catch (Exception e) {
					continue;
				}
				if (val instanceof BaseRecord) {
					BaseRecord tmp = (BaseRecord) val;
					val = GM.getRecordDispName(tmp, new Context());
				}
				tableData.data.setValueAt(val, r, j + 1);
			}
		}
		tableData.repaint();
	}

	/**
	 * 重置数据表的编辑风格
	 */
	private void resetTableDataStyle() {
		if (table == null) {
			return;
		}
		DataStruct ds = table.dataStruct();
		if (ds == null) {
			return;
		}
		for (int i = 1; i <= ds.getFieldCount(); i++) {
			tableData.setColumnDefaultEditor(i);
		}
		GM.setEditStyle(tableData);
		tableData.repaint();
	}

	/**
	 * 选择的TAB发生改变
	 * 
	 * @param e
	 */
	void this_stateChanged(ChangeEvent e) {
		if (preventChange) {
			return;
		}
		tableNormal.acceptText();
		if (jTabMain.getSelectedIndex() == TAB_DATA) {
			resetTableData();
		}
	}

}

class PanelEditTable_this_changeAdapter implements
		javax.swing.event.ChangeListener {
	PanelEditTable adaptee;

	PanelEditTable_this_changeAdapter(PanelEditTable adaptee) {
		this.adaptee = adaptee;
	}

	public void stateChanged(ChangeEvent e) {
		adaptee.this_stateChanged(e);
	}
}
