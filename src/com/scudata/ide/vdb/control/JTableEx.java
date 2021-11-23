package com.scudata.ide.vdb.control;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.CellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.ByteMap;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.ide.vdb.VDB;
import com.scudata.ide.vdb.resources.IdeMessage;

/**
 * 注意: 由于通过jTable.getValueAt不能访问被隐藏了的列,所以 一般用 jTable.data.getValueAt
 */

public class JTableEx extends JTable implements MouseListener, JTableExListener {
	private static final long serialVersionUID = 1L;

	public Object tag;

	public DefaultTableModel data = new DefaultTableModel();

	/**
	 * 提示，禁止使用data.setRowCount(0);而用this.removeAllRows()
	 */
	private int indexCol = -1;

	/**
	 * columnKeeper 用于保存住列表中的列对象；使得hideColumn()以及 showColumn()方法得以实现
	 * 注意动态增加Table中的列的时候，只能使用Table.AddColumn(col); 注意!!!!!!!!!!!!!!!!!!!!!!
	 * 而不要使用Table.data.AddColumn(col)
	 */
	private HashMap<Object, TableColumn> columnKeeper = new HashMap<Object, TableColumn>();

	private boolean ifFireRowfocusChangedEvent = true;

	public HashSet<JTableExListener> clickedListener = new HashSet<JTableExListener>();

	public void rightClicked(int xpos, int ypos, int row, int col, MouseEvent e) {
	}

	public void doubleClicked(int xpos, int ypos, int row, int col, MouseEvent e) {
	}

	public void clicked(int xpos, int ypos, int row, int col, MouseEvent e) {
	}

	public void rowfocusChanged(int oldRow, int newRow) {
	}

	public void rowfocusChanging(int oldRow, int newRow) {
	}

	public void mouseClicked(final MouseEvent e) {
		java.awt.Container p = getParent();
		java.awt.Container ct = getTopLevelAncestor();
		int absoluteX = e.getX(), absoluteY = e.getY();
		while (p != ct) {
			absoluteX += p.getX();
			absoluteY += p.getY();
			p = p.getParent();
		}
		fireClicked(absoluteX, absoluteY, e);
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
		// 部分选中行动作，没有重画界面
		repaint();
	}

	public void mouseExited(MouseEvent e) {
	}

	public JTableEx() {
		setModel(data);
		addMouseListener(this);
		addTableExListener(this);
		// addFocusListener(this);
		getColumnModel().addColumnModelListener(new TableColListener(this));

		getTableHeader().setReorderingAllowed(false); // 默认不支持挪动列，否则双击事件设置不对
		// setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// setColumnSelectionAllowed(false); //列可选
		// setRowSelectionAllowed(false); //行可选
	}

	public JTableEx(String[] colNames) {
		this();
		for (int i = 0; i < colNames.length; i++) {
			addColumn(colNames[i]);
			setColumnDefaultEditor(i);
		}
		holdColumnNames();
		oldRow = -1;
	}

	public JTableEx(String columnNames) {
		this(columnNames.split(","));
	}

	void holdColumnNames() {
		if (columnKeeper == null) {
			return;
		}
		columnKeeper.clear();

		TableColumnModel cm = getColumnModel();
		Enumeration<TableColumn> cols = cm.getColumns();
		TableColumn tmpCol;
		Object tmpName;
		while (cols.hasMoreElements()) {
			tmpCol = (TableColumn) cols.nextElement();
			tmpCol.setCellEditor(new SimpleEditor(new JTextField(), this));
			tmpName = tmpCol.getIdentifier();
			columnKeeper.put(tmpName, tmpCol);
		}
	}

	public void setClickCountToStart(int startCount) {
		int rows = getRowCount();
		int cols = getColumnCount();
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				CellEditor editor = getCellEditor(row, col);
				if (editor != null && editor instanceof DefaultCellEditor) {
					((DefaultCellEditor) editor).setClickCountToStart(startCount);
				}
			}
		}
	}

	protected boolean isItemDataChanged(int row, int column, Object newValue) {
		if (column == indexCol) {
			return false;
		}
		Object oldValue = getValueAt(row, column);
		if (oldValue instanceof String && newValue instanceof String) {
			if (StringUtils.isValidString(oldValue)) {
				return !oldValue.equals(newValue);
			} else {
				return StringUtils.isValidString(newValue);
			}
		} else {
			if (oldValue != null) {
				return !oldValue.equals(newValue);
			} else {
				return newValue != null && !newValue.equals("");
			}
		}
	}

	public boolean exportData(boolean exportTitle, FileWriter fw, boolean dosFormat) throws Exception {
		String sRow = "", sTmp = "";
		int rc, cc, r, c;
		cc = getColumnCount();
		rc = getRowCount();
		String rowSep;
		if (dosFormat) {
			rowSep = "\r\n";
		} else {
			rowSep = "\r";
		}

		if (exportTitle) {
			if (cc == 0) {
				return false;
			}
			for (c = 0; c < cc; c++) {
				sTmp += "\t" + getColumnName(c);
			}
			sRow = rowSep + sTmp.substring(1);
		}

		for (r = 0; r < rc; r++) {
			sTmp = "";
			for (c = 0; c < cc; c++) {
				sTmp += "\t" + data.getValueAt(r, c);
			}
			sRow += rowSep + sTmp.substring(1);
		}

		fw.write(sRow.substring(rowSep.length()));
		return true;
	}

	public void setModel(TableModel dataModel) {
		data = (DefaultTableModel) dataModel;
		super.setModel(data);
		holdColumnNames();
		oldRow = -1;
	}

	public void resetColumns(ArrayList<Object> newColNames) {
		data.setColumnCount(0);
		for (int i = 0; i < newColNames.size(); i++) {
			data.addColumn(newColNames.get(i));
		}
		holdColumnNames();
	}

	public void acceptText() {
		if (getCellEditor() != null && getRowCount() > 0) {
			int c = getRowCount();
			int e = getEditingRow();
			if (e >= c) {
				return;
			}
			getCellEditor().stopCellEditing();
		}
	}

	public void addTableExListener(JTableExListener jcl) {
		clickedListener.add(jcl);
	}

	public void removeClickedListener(JTableExListener jcl) {
		clickedListener.remove(jcl);
	}

	public void fireClicked(int xpos, int ypos, MouseEvent e) {
		Iterator<JTableExListener> it = clickedListener.iterator();
		int row, col;
		row = this.getSelectedRow();
		col = this.getSelectedColumn();
		if (row == -1) {
			row = this.rowAtPoint(new java.awt.Point(e.getX(), e.getY()));
		}
		if (col == -1) {
			col = this.columnAtPoint(new java.awt.Point(e.getX(), e.getY()));
		}
		while (it.hasNext()) {
			JTableExListener lis = (JTableExListener) it.next();
			if (e.getButton() == MouseEvent.BUTTON3) {
				lis.rightClicked(xpos, ypos, row, col, e);
				continue;
			} else if (e.getButton() == MouseEvent.BUTTON1) {
				switch (e.getClickCount()) {
				case 1:
					lis.clicked(xpos, ypos, row, col, e);
					break;
				case 2:
					lis.doubleClicked(xpos, ypos, row, col, e);
					break;
				default:
					break;
				}
			}
		}
	}

	public void addColumn(String colName) {
		data.addColumn(colName);
		holdColumnNames();
	}

	/**
	 * 添加几列，保持原有外观不变
	 * 
	 * @param colName
	 *            String[]
	 */
	public void addColumn(String colName[]) {
		// this.getColumnModel()
	}

	/**
	 * 真正删除了指定的列，包括数据Model
	 * 
	 * @param aColumn
	 *            TableColumn
	 */
	public void deleteColumn(TableColumn aColumn) {
		String colName = (String) aColumn.getIdentifier();

		int cc = data.getColumnCount();
		DefaultTableModel dataModel = new DefaultTableModel();

		for (int i = 0; i < cc; i++) {
			String tmpName = (String) data.getColumnName(i);
			if (tmpName.equalsIgnoreCase(colName)) {
				continue;
			}
			Vector<Object> cData = getColumnData(i);
			dataModel.addColumn(tmpName, cData);
		}
		setModel(dataModel);
	}

	public TableColumn getColumn(int columnIndex) {
		return getColumnModel().getColumn(columnIndex);
	}

	public int getColumnIndex(String colName) {
		return getColumnIndex(colName, false);
	}

	public int getColumnIndex(String colName, boolean includeHideColumns) {
		if (includeHideColumns) {
			for (int i = 0; i < data.getColumnCount(); i++) {
				String name = data.getColumnName(i);
				if (name.equals(colName)) {
					return i;
				}
			}
		} else {
			for (int i = 0; i < this.getColumnCount(); i++) {
				String name = getColumnName(i);
				if (name.equals(colName)) {
					return i;
				}
			}
		}
		return -1;
	}

	public void removeAllRows() {
		data.setRowCount(0);
		oldRow = -1;
	}

	public Vector<Object> getColumnData(int colIndex) {
		Vector<Object> cData = new Vector<Object>();
		for (int r = 0; r < data.getRowCount(); r++) {
			cData.add(data.getValueAt(r, colIndex));
		}
		return cData;
	}

	private transient ByteMap minimizedColumn = new ByteMap();

	/**
	 * 最小化列，即把列的宽度设为0，以至于看不见
	 * 
	 * @param colIndex
	 *            int
	 */
	public void minimizeColumn(int columnIndex) {
		TableColumn tc = getColumn(columnIndex);
		if (tc.getPreferredWidth() == 0) {
			return;
		}
		String oldConfig = tc.getMaxWidth() + "|" + tc.getMinWidth() + "|" + tc.getPreferredWidth();
		minimizedColumn.put((byte) columnIndex, oldConfig);
		tc.setMaxWidth(0);
		tc.setMinWidth(0);
		tc.setPreferredWidth(0);
		tc.setResizable(false);
	}

	/**
	 * 还原最小化的列
	 * 
	 * @param colIndex
	 *            int
	 */
	public void recoverColumn(int columnIndex) {
		TableColumn tc = getColumn(columnIndex);
		tc.setResizable(true);
		String oldConfig = (String) minimizedColumn.get((byte) columnIndex);
		if (oldConfig == null) {
			return;
		}
		ArgumentTokenizer at = new ArgumentTokenizer(oldConfig, '|');
		try {
			tc.setMaxWidth(Integer.parseInt(at.nextToken()));
		} catch (Exception e) {
		}
		try {
			tc.setMinWidth(Integer.parseInt(at.nextToken()));
		} catch (Exception e) {
		}
		try {
			tc.setPreferredWidth(Integer.parseInt(at.nextToken()));
		} catch (Exception e) {
		}
	}

	public void hideColumn(String columnName) {
		if (!isColumnVisible(columnName)) {
			return;
		}
		TableColumn col = getColumn(columnName);
		Object id = col.getIdentifier();
		columnKeeper.put(id, col);
		removeColumn(col);
	}

	public void setColumnEnable(int columnIndex, boolean enable) {
		setColumnEnable(getColumnName(columnIndex), enable);
	}

	public void setColumnEnable(String columnName, boolean enable) {
		if (!isColumnVisible(columnName)) {
			return;
		}
		TableColumn col = getColumn(columnName);
		Object o = col.getCellRenderer();

		int align = ALIGN_LEFT;
		if (o != null && o instanceof DefaultTableCellRenderer) {
			align = ((DefaultTableCellRenderer) o).getHorizontalAlignment();
		}
		JTextField jtf = new JTextField();
		jtf.setHorizontalAlignment(align);

		if (enable) {
			col.setCellEditor(new DefaultCellEditor(jtf));
			DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();
			dtcr.setHorizontalAlignment(align);
			col.setCellRenderer(dtcr);
		} else {
			col.setCellEditor(new DisabledEditor(jtf));
			DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();
			dtcr.setHorizontalAlignment(align);
			dtcr.setForeground(disabledForeColor);
			dtcr.setBackground(disabledBackColor);
			col.setCellRenderer(dtcr);
		}
	}

	public void setColumnSpinner(int columnIndex) {
		setColumnSpinner(getColumnName(columnIndex));
	}

	public void setColumnSpinner(String columnName) {
		DefaultCellEditor integerEditor = new JTextAreaEditor(this, JTextAreaEditor.TYPE_UNSIGNED_INTEGER);
		TableColumn tc = getColumn(columnName);
		tc.setCellEditor(integerEditor);
	}

	public void setColumnCheckBox(int columnIndex) {
		setColumnCheckBox(getColumnName(columnIndex));
	}

	public void setColumnCheckBox(String columnName) {
		JCheckBox checkBoxEditor = new JCheckBox();
		checkBoxEditor.setHorizontalAlignment(JLabel.CENTER);

		TableCellEditor cellEditor = new DefaultCellEditor(checkBoxEditor);
		TableCellRenderer cellRenderer = new CheckBoxRenderer();

		TableColumn col = getColumn(columnName);
		col.setCellEditor(cellEditor);
		col.setCellRenderer(cellRenderer);
	}

	public JComboBoxEx setColumnDropDown(int columnIndex, Vector<Object> codeItems, Vector<String> dispItems) {
		return setColumnDropDown(getColumnName(columnIndex), codeItems, dispItems);
	}

	public JComboBoxEx setColumnDropDown(int columnIndex, Vector<Object> codeItems, Vector<String> dispItems,
			boolean editable) {
		// 多次反复设置下拉列时，当前的下拉控件要先清除掉，否则不能立刻刷新 xq 2015.12.11
		acceptText();

		return setColumnDropDown(getColumnName(columnIndex), codeItems, dispItems, editable);
	}

	public void setColumnDefaultEditor(int columnIndex) {
		setColumnDefaultEditor(getColumnName(columnIndex));
	}

	public void setColumnDefaultEditor(String columnName) {
		TableColumn col = getColumn(columnName);

		// col.setCellEditor(new JTextAreaEditor(this));
		col.setCellEditor(new SimpleEditor(new JTextField(), this));

		DefaultTableCellRenderer dr = new DefaultTableCellRenderer();
		col.setCellRenderer(dr);
	}

	public JComboBoxEx setColumnDropDown(String columnName, Vector<Object> codeItems, Vector<String> dispItems) {
		return setColumnDropDown(columnName, codeItems, dispItems, false);
	}

	public JComboBoxEx setColumnDropDown(final String columnName, Vector<Object> codeItems, Vector<String> dispItems,
			boolean editable) {
		JComboBoxEx combo = new JComboBoxEx();
		combo.x_setData(codeItems, dispItems);
		combo.setEditable(editable);
		TableColumn col = getColumn(columnName);
		TableCellEditor cellEditor = new JComboBoxExEditor(combo);
		TableCellRenderer cellRenderer = new JComboBoxExRenderer(combo);
		col.setCellEditor(cellEditor);
		col.setCellRenderer(cellRenderer);
		return combo;
	}

	public void setColumnEditable(int columnIndex, boolean allowEdit) {
		setColumnEditable(getColumnName(columnIndex), allowEdit);
	}

	public void setColumnEditable(int columnIndex, boolean allowEdit, boolean centerAlign) {
		setColumnEditable(getColumnName(columnIndex), allowEdit, centerAlign);
	}

	public void setColumnEditable(String columnName, boolean allowEdit) {
		setColumnEditable(columnName, allowEdit, false);
	}

	public void setColumnEditable(String columnName, boolean allowEdit, final boolean centerAlign) {
		if (!isColumnVisible(columnName)) {
			return;
		}
		if (!allowEdit) {
			TableColumn col = getColumn(columnName);
			col.setCellEditor(new JTextAreaEditor(this, JTextAreaEditor.TYPE_TEXT_READONLY) {
				private static final long serialVersionUID = 1L;

				public boolean isCellEditable(EventObject anEvent) {
					return false;
				}

				public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
						int column) {
					Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
					if (isSelected) {
						setForeground(table.getSelectionForeground());
						setBackground(table.getSelectionBackground());
					} else {
						setForeground(table.getForeground());
						setBackground(table.getBackground());
					}
					if (centerAlign) {
						if (c instanceof JLabel) {
							JLabel text = (JLabel) c;
							text.setHorizontalAlignment(JTextField.CENTER);
						}
					}
					return c;
				}
			});

			TableCellRenderer tcr = new DefaultTableCellRenderer() {
				private static final long serialVersionUID = 1L;

				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
						boolean hasFocus, int row, int column) {
					Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					if (isSelected) {
						setForeground(table.getSelectionForeground());
						setBackground(table.getSelectionBackground());
					} else {
						setForeground(table.getForeground());
						setBackground(table.getBackground());
					}
					if (centerAlign) {
						if (c instanceof JLabel) {
							JLabel text = (JLabel) c;
							text.setHorizontalAlignment(JTextField.CENTER);
						}
					}
					return c;
				}
			};
			col.setCellRenderer(tcr);
		} else {
			this.setColumnDefaultEditor(columnName);
		}
	}

	public void setColumnFixedWidth(int columnIndex, int width) {
		String columnName = getColumnName(columnIndex);
		if (!isColumnVisible(columnName)) {
			return;
		}
		TableColumn col = getColumn(columnName);
		col.setPreferredWidth(width);
		col.setMaxWidth(width);
	}

	public void setColumnWidth(int columnIndex, int width) {
		setColumnWidth(getColumnName(columnIndex), width);
	}

	public void setColumnWidth(String columnName, int width) {
		if (!isColumnVisible(columnName)) {
			return;
		}
		TableColumn col = getColumn(columnName);
		col.setPreferredWidth(width);
	}

	static public int ALIGN_LEFT = JLabel.LEFT;

	static public int ALIGN_CENTER = JLabel.CENTER;

	static public int ALIGN_RIGHT = JLabel.RIGHT;

	public void setColumnAlign(int columnIndex, int alignment) {
		setColumnAlign(getColumnName(columnIndex), alignment);
	}

	public void setColumnAlign(String columnName, int alignment) {
		if (!isColumnVisible(columnName)) {
			return;
		}
		TableColumn col = getColumn(columnName);
		Object oldRender = col.getCellRenderer();
		if (oldRender == null || (oldRender != null && oldRender instanceof DefaultTableCellRenderer)) {
			DefaultTableCellRenderer cbRender = new DefaultTableCellRenderer();
			cbRender.setHorizontalAlignment(alignment);
			col.setCellRenderer(cbRender);
		}
		Object oldEditor = col.getCellEditor();
		if (oldEditor == null || (oldEditor != null && oldEditor instanceof SimpleEditor)) {
			JTextField tf = new JTextField();
			tf.setHorizontalAlignment(alignment);
			col.setCellEditor(new SimpleEditor(tf, this));
		}
	}

	public Object[] getRowDataArray(int row) {
		int colCount = data.getColumnCount();
		Object[] rowData = new Object[colCount];

		if (row >= 0 && row < data.getRowCount()) {
			for (int i = 0; i < colCount; i++) {
				rowData[i] = data.getValueAt(row, i);
			}
		}
		return rowData;
	}

	public String getRowData(int row) {
		StringBuffer rowData = new StringBuffer(1024);
		Object[] rowVals = getRowDataArray(row);
		for (int i = 0; i < rowVals.length; i++) {
			Object val = rowVals[i];
			if (val == null) {
				val = "";
			}
			rowData.append(val.toString());
			if (i < rowVals.length - 1) {
				rowData.append("\t");
			}
		}
		return rowData.toString();
	}

	public String getBlockData() {
		StringBuffer sb = new StringBuffer(1024);
		acceptText();
		for (int i = 0; i < getRowCount(); i++) {
			if (this.isRowSelected(i)) {
				String row = getRowData(i);
				sb.append(row);
				if (i < getRowCount() - 1) {
					sb.append("\n");
				}
			}
		}
		return sb.toString();
	}

	public int setBlockData(String ls_data) {
		acceptText();
		if (!StringUtils.isValidString(ls_data)) {
			return -1;
		}
		int row, col, r = 0;
		String ls_row;
		row = getSelectedRow();
		col = getSelectedColumn();
		if (row < 0) {
			row = getRowCount() + 1;
		}
		if (col < 0) {
			col = 0;
		}
		ls_data = Sentence.replace(ls_data, "\r\n", "\r", Sentence.IGNORE_CASE);
		ls_data = Sentence.replace(ls_data, "\n", "\r", Sentence.IGNORE_CASE);

		ArgumentTokenizer rows = new ArgumentTokenizer(ls_data, '\r');
		while (rows.hasMoreTokens()) {
			ls_row = rows.nextToken();
			if (!StringUtils.isValidString(ls_row)) {
				continue;
			}
			if (row >= getRowCount()) {
				row = addRow();
			}

			int li_col = col, colCount = data.getColumnCount();
			ArgumentTokenizer items = new ArgumentTokenizer(ls_row, '\t');
			String item;
			while (items.hasMoreTokens()) {
				item = items.nextToken();
				data.setValueAt(item, row, li_col);
				li_col++;
				if (li_col == colCount) {
					break;
				}
			}
			row++;
			r++;
		}
		return r;
	}

	// 由于隐藏列后,列的序号发生了变化,所以不使用序号来设置列的可视
	// public void showColumn(int colIndex) {
	// showColumn(getColumnName(colIndex));
	// }

	public void setIndexCol(int indexCol) {
		this.indexCol = indexCol;
		setColumnAlign(indexCol, ALIGN_CENTER);
		setColumnEditable(indexCol, false, true);
		setColumnFixedWidth(indexCol, 40);
	}

	public void resetIndex() {
		if (indexCol == -1) {
			return;
		}

		int c = getRowCount();
		for (int i = 0; i < c; i++) {
			data.setValueAt(new Integer(i + 1), i, indexCol);
		}
	}

	public boolean checkColumnData(int colIndex, String colDesc) {
		return verifyColumnData(colIndex, colDesc);
	}

	public boolean verifyColumnData(int colIndex, String colDesc) {
		return verifyColumnData(colIndex, colDesc, true);
	}

	public boolean verifyColumnData(int colIndex, String colDesc, boolean caseRepeat) {
		acceptText();

		HashSet<Object> keys = new HashSet<Object>();
		int r = getRowCount();
		String key;
		for (int i = 0; i < r; i++) {
			key = (String) data.getValueAt(i, colIndex);
			if (!StringUtils.isValidString(key)) {
				JOptionPane.showMessageDialog(VDB.getInstance(),
						IdeMessage.get().getMessage("jtableex.emptyvalue", String.valueOf(i + 1), colDesc),
						IdeMessage.get().getMessage("public.prompt"), JOptionPane.WARNING_MESSAGE); // 第：{0}行的{1}为空。
				return false;
			}
			if (caseRepeat && keys.contains(key)) {
				JOptionPane.showMessageDialog(VDB.getInstance(),
						IdeMessage.get().getMessage("jtableex.dupvalue", colDesc, key),
						IdeMessage.get().getMessage("public.prompt"), JOptionPane.WARNING_MESSAGE); // 重复的{0}：{1}
				return false;
			}
			keys.add(key);
		}
		return true;
	}

	public void setColumnVisible(String colName, boolean vis) {
		if (vis) {
			showColumn(colName);
		} else {
			hideColumn(colName);
		}
	}

	public int shiftUp() {
		return shiftRowUp(-1);
	}

	/**
	 * 上移指定的记录行
	 * 
	 * @param row
	 *            int，要移动的行号，如果为小于0的数字则移动当前选中的行
	 * @return int，移动后的新行号,返回-1表示没有移动
	 */
	public int shiftRowUp(int row) {
		int cr;
		if (row < 0) {
			cr = getSelectedRow();
		} else {
			cr = row;
		}

		if (cr <= 0) {
			return -1;
		}
		data.moveRow(cr, cr, cr - 1);
		selectRow(cr - 1);
		resetIndex();
		return cr - 1;
	}

	public int shiftDown() {
		return shiftRowDown(-1);
	}

	public int shiftRowDown(int row) {
		int cr;
		if (row < 0) {
			cr = getSelectedRow();
		} else {
			cr = row;
		}
		if (cr < 0 || cr >= getRowCount() - 1) {
			return -1;
		}
		data.moveRow(cr, cr, cr + 1);
		selectRow(cr + 1);
		resetIndex();
		return cr + 1;
	}

	public void showColumn(String columnName) {
		if (isColumnVisible(columnName)) {
			return;
		}
		TableColumn col = (TableColumn) columnKeeper.get(columnName);
		if (col == null) {
			return;
		}
		addColumn(col);
	}

	/**
	 * 删除选中的行集，删除后并且选中有效记录
	 */
	public boolean deleteSelectedRows() {
		acceptText();
		int cr = getSelectedRow();
		if (cr < 0) {
			return false;
		}
		removeCurrentRow();
		int r = getRowCount() - 1;
		clearSelection();
		// oldRow = -1;
		if (cr < r) {
			r = cr;
		}
		selectRow(r);
		resetIndex();
		return true;
	}

	public boolean deleteSelectedRow() {
		acceptText();
		int cr = getSelectedRow();
		if (cr < 0) {
			return false;
		}
		data.removeRow(getSelectedRow());
		int r = getRowCount() - 1;
		clearSelection();

		if (cr < r) {
			r = cr;
		}
		selectRow(r);
		resetIndex();
		return true;
	}

	/**
	 * 删除当前选中的记录行集 但是不影响选中的行集合
	 * 
	 * @return 删除的行数
	 */
	public int removeCurrentRow() {
		acceptText();
		int cr = 0;
		for (int i = this.getRowCount(); i >= 0; i--) {
			if (this.isRowSelected(i)) {
				data.removeRow(i);
				cr++;
			}
		}
		return cr;
	}

	public int removeRow(int row) {
		if (row < 0) {
			return -1;
		}
		int c = data.getRowCount();
		if (row >= c) {
			return -1;
		}
		data.removeRow(row);
		int t = getRowCount();
		if (t == row) {
			row--;
		}
		selectRow(row);
		setEditingRow(row);
		return row;
	}

	public void selectRow(int row) {
		// if (row < 0) {
		// return;
		// }

		DefaultListSelectionModel selectModel = new DefaultListSelectionModel();
		selectModel.addSelectionInterval(row, row);
		setSelectionModel(selectModel);
		setEditingRow(row);
		int n;
		n = row;
		// if (n == oldRow) {
		// return;
		// }
		fireRowfocusChanged(oldRow, row);
		oldRow = n;
	}

	/**
	 * 在表格的SearchColumn列里面查找对象value
	 * 
	 * @param value
	 *            Object
	 * @param searchColumn
	 *            int
	 * @return int, 找到该对象返回对象所在的行号,否则返回-1
	 */
	public int searchValue(Object value, int searchColumn) {
		Object o;
		for (int i = 0; i < getRowCount(); i++) {
			o = getValueAt(i, searchColumn);
			if (value.equals(o)) {
				return i;
			}
		}
		return -1;
	}

	public int addRow() {
		return addRow(true);
	}

	public int addRow(boolean resetIndex) {
		return insertRow(-1, null, resetIndex);
	}

	public int addRow(Vector<Object> v) {
		return addRow(v, true);
	}

	public int addRow(Vector<Object> v, boolean resetIndex) {
		return insertRow(-1, v.toArray(), resetIndex);
	}

	public int addRow(Object[] oa) {
		return addRow(oa, true);
	}

	public int addRow(Object[] oa, boolean resetIndex) {
		return insertRow(-1, oa, resetIndex);
	}

	public int insertRow(int row, Object[] rowData) {
		return insertRow(row, rowData, true);
	}

	public int insertRow(int row, Object[] rowData, boolean resetIndex) {
		acceptText();
		int r;
		if (row > -1 && row < data.getRowCount()) {
			data.insertRow(row, rowData);
			r = row;
		} else {
			data.addRow(rowData);
			r = data.getRowCount() - 1;
		}

		if (resetIndex) {
			setEditingRow(r);
			resetIndex();
			selectRow(r);
		}
		return r;
	}

	public boolean isColumnVisible(Object column) {
		TableColumnModel cm = this.getColumnModel();
		TableColumn tc;
		for (int i = 0; i < cm.getColumnCount(); i++) {
			tc = cm.getColumn(i);
			if (tc.getIdentifier().equals(column)) {
				return true;
			}
		}
		return false;
	}

	private void fireRowfocusChanged(int oldRow, int newRow) {
		if (!ifFireRowfocusChangedEvent) {
			return;
		}
		Iterator<JTableExListener> it = clickedListener.iterator();
		if (oldRow >= this.getRowCount()) {
			oldRow = -1;
		}
		while (it.hasNext()) {
			JTableExListener lis = it.next();
			lis.rowfocusChanged(oldRow, newRow);
		}
		this.resizeAndRepaint();
	}

	public void disableRowfocusChanged() {
		ifFireRowfocusChangedEvent = false;
	}

	public void enableRowfocusChanged() {
		ifFireRowfocusChangedEvent = true;
	}

	public void valueChanged(ListSelectionEvent e) {
		// if(e!=null) super.valueChanged(e);
		int r = getSelectedRow();
		if (r < 0) {
			return;
		}
		if (e.getValueIsAdjusting()) { // e!=null &&
			return;
		}
		n = r;
		if (n == oldRow) {
			return;
		}
		fireRowfocusChanged(oldRow, r);
		oldRow = n;
	}

	private int oldRow = -1, n;

	private Color disabledBackColor = Color.lightGray;

	private Color disabledForeColor = Color.black;

	/**
	 * 设置禁止列的颜色
	 * 
	 * @param c
	 *            Color
	 */
	public void setDisabledColor(Color foreColor, Color backColor) {
		disabledForeColor = foreColor;
		disabledBackColor = backColor;
	}

	/**
	 * focusGained
	 * 
	 * @param e
	 *            FocusEvent
	 */
	public void focusGained(FocusEvent e) {
	}

	public void keyPressed(KeyEvent e) {
	}

	/**
	 * focusLost
	 * 
	 * @param e
	 *            FocusEvent
	 */
	// public void focusLost(FocusEvent e) {
	// acceptText();
	// }
	class DisabledEditor extends DefaultCellEditor {
		private static final long serialVersionUID = 1L;

		public DisabledEditor(JTextField jTxt) {
			super(jTxt);
			jTxt.setBackground(disabledBackColor);
			jTxt.setEditable(false);
			// jTxt.setHorizontalAlignment(JTextField.CENTER);
		}

		public boolean isCellEditable(EventObject anEvent) {
			return true;
		}
	}

	public DefaultCellEditor getDefaultCellEditor(JTextField tf, JTableEx parent) {
		return new SimpleEditor(tf, parent);
	}

	class SimpleEditor extends DefaultCellEditor implements KeyListener, MouseListener, FocusListener {
		private static final long serialVersionUID = 1L;
		JTableEx parent;

		// private boolean edited = false;
		public SimpleEditor(JTextField tf, JTableEx parent) {
			super(tf);
			tf.addKeyListener(this);
			tf.addFocusListener(this);
			tf.addMouseListener(this);
			tf.setBorder(javax.swing.BorderFactory.createEmptyBorder());
			this.parent = parent;
			this.setClickCountToStart(1);
		}

		public void mouseClicked(final MouseEvent e) {
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
			parent.fireClicked(absoluteX, absoluteY, e);
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		/**
		 * focusGained
		 * 
		 * @param e
		 *            FocusEvent
		 */
		public void focusGained(FocusEvent e) {
			parent.focusGained(e);
		}

		/**
		 * focusLost
		 * 
		 * @param e
		 *            FocusEvent
		 */
		public void focusLost(FocusEvent e) {
			// 只有在值改动过时触发接受，否则单击表格不能直接编辑，以及会影响到输入法
			// if( edited ){
			// parent.acceptText();
			// edited = false;
			// }
		}

		/**
		 * keyPressed
		 * 
		 * @param e
		 *            KeyEvent
		 */
		public void keyPressed(KeyEvent e) {
			parent.keyPressed(e);
		}

		/**
		 * keyReleased
		 * 
		 * @param e
		 *            KeyEvent
		 */
		public void keyReleased(KeyEvent e) {
		}

		/**
		 * keyTyped
		 * 
		 * @param e
		 *            KeyEvent
		 */
		public void keyTyped(KeyEvent e) {
			// edited = true;
		}
	}

	class TableColListener implements TableColumnModelListener {
		JTableEx table;

		public TableColListener(JTableEx table) {
			this.table = table;
		}

		public void columnAdded(TableColumnModelEvent e) {
		};

		public void columnMarginChanged(ChangeEvent e) {
			table.acceptText();
		};

		public void columnMoved(TableColumnModelEvent e) {
		};

		public void columnRemoved(TableColumnModelEvent e) {
		};

		public void columnSelectionChanged(ListSelectionEvent e) {
		};
	}

}
