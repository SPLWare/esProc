package com.scudata.ide.common.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.event.ChangeListener;
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

import com.scudata.app.common.Section;
import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.MessageManager;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.resources.IdeCommonMessage;

/**
 * 扩展的JTable。注意: 由于通过jTable.getValueAt不能访问被隐藏了的列,所以 一般用 jTable.data.getValueAt
 */

public class JTableEx extends JTable implements MouseListener,
		JTableExListener, ChangeListener {
	private static final long serialVersionUID = 1L;

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/**
	 * 表格模型
	 */
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

	/**
	 * 是否激活行焦点变化
	 */
	private boolean ifFireRowfocusChangedEvent = true;

	/**
	 * 鼠标点击监听器容器
	 */
	public HashSet<JTableExListener> clickedListener = new HashSet<JTableExListener>();

	/**
	 * 构造函数
	 */
	public JTableEx() {
		setModel(data);
		addMouseListener(this);
		addTableExListener(this);
		getColumnModel().addColumnModelListener(new TableColListener(this));
		// 默认不支持挪动列，否则双击事件设置不对
		getTableHeader().setReorderingAllowed(false);

	}

	/**
	 * 构造函数
	 * 
	 * @param colNames
	 *            列名数组
	 */
	public JTableEx(String[] colNames) {
		this();
		for (int i = 0; i < colNames.length; i++) {
			addColumn(colNames[i]);
			setColumnDefaultEditor(i);
		}
		holdColumnNames();
		oldRow = -1;
	}

	/**
	 * 构造函数
	 * 
	 * @param columnNames
	 *            逗号分隔的列名
	 */
	public JTableEx(String columnNames) {
		this(new Section(columnNames).toStringArray());
	}

	/**
	 * 鼠标右键点击事件
	 */
	public void rightClicked(int xpos, int ypos, int row, int col, MouseEvent e) {
	}

	/**
	 * 鼠标双击事件
	 */
	public void doubleClicked(int xpos, int ypos, int row, int col, MouseEvent e) {
	}

	/**
	 * 鼠标点击事件
	 */
	public void clicked(int xpos, int ypos, int row, int col, MouseEvent e) {
	}

	/**
	 * 行焦点变化事件
	 */
	public void rowfocusChanged(int oldRow, int newRow) {
	}

	/**
	 * 行焦点正在变化事件
	 * 
	 * @param oldRow
	 * @param newRow
	 */
	public void rowfocusChanging(int oldRow, int newRow) {
	}

	/**
	 * 鼠标点击事件
	 */
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

	/**
	 * 鼠标按键事件
	 */
	public void mousePressed(MouseEvent e) {
	}

	/**
	 * 鼠标光标进入事件
	 */
	public void mouseEntered(MouseEvent e) {
	}

	/**
	 * 鼠标释放事件
	 */
	public void mouseReleased(MouseEvent e) {
		// 部分选中行动作，没有重画界面
		repaint();
	}

	/**
	 * 鼠标退出事件
	 */
	public void mouseExited(MouseEvent e) {
	}

	/**
	 * 管理列名
	 */
	void holdColumnNames() {
		if (columnKeeper == null) {
			return;
		}
		columnKeeper.clear();

		TableColumnModel cm = getColumnModel();
		Enumeration cols = cm.getColumns();
		TableColumn tmpCol;
		Object tmpName;
		while (cols.hasMoreElements()) {
			tmpCol = (TableColumn) cols.nextElement();
			tmpCol.setCellEditor(new SimpleEditor(new JTextField(), this));
			tmpName = tmpCol.getIdentifier();
			columnKeeper.put(tmpName, tmpCol);
		}
	}

	/**
	 * 设置点击几次进入编辑状态
	 * 
	 * @param startCount
	 */
	public void setClickCountToStart(int startCount) {
		int rows = getRowCount();
		int cols = getColumnCount();
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				CellEditor editor = getCellEditor(row, col);
				if (editor != null && editor instanceof DefaultCellEditor) {
					((DefaultCellEditor) editor)
							.setClickCountToStart(startCount);
				}
			}
		}
	}

	/**
	 * 值是否发生变化了
	 * 
	 * @param row
	 *            行号
	 * @param column
	 *            列号
	 * @param newValue
	 *            新值
	 * @return
	 */
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

	/**
	 * 导出数据
	 * 
	 * @param exportTitle
	 *            是否导出标题
	 * @param fw
	 *            FileWriter
	 * @param dosFormat
	 *            格式
	 * @return
	 * @throws Exception
	 */
	public boolean exportData(boolean exportTitle, FileWriter fw,
			boolean dosFormat) throws Exception {
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

	/**
	 * 设置表模型
	 */
	public void setModel(TableModel dataModel) {
		data = (DefaultTableModel) dataModel;
		super.setModel(data);
		holdColumnNames();
		oldRow = -1;
	}

	/**
	 * 重设列名
	 * 
	 * @param newColNames
	 *            列名列表
	 */
	public void resetColumns(ArrayList newColNames) {
		data.setColumnCount(0);
		for (int i = 0; i < newColNames.size(); i++) {
			data.addColumn(newColNames.get(i));
		}
		holdColumnNames();
	}

	/**
	 * 接受提交数据
	 */
	public void acceptText() {
		if (getCellEditor() != null && getRowCount() > 0) {
			int r = getSelectedRow();
			int c = getRowCount();
			int e = getEditingRow();
			if (e >= c) {
				return;
			}
			getCellEditor().stopCellEditing();
		}
	}

	/**
	 * 增加监听器
	 * 
	 * @param jcl
	 */
	public void addTableExListener(JTableExListener jcl) {
		clickedListener.add(jcl);
	}

	/**
	 * 删除监听器
	 * 
	 * @param jcl
	 */
	public void removeClickedListener(JTableExListener jcl) {
		clickedListener.remove(jcl);
	}

	/**
	 * 触发鼠标点击事件
	 * 
	 * @param xpos
	 *            X坐标
	 * @param ypos
	 *            Y坐标
	 * @param e
	 *            鼠标事件
	 */
	public void fireClicked(int xpos, int ypos, MouseEvent e) {
		Iterator it = clickedListener.iterator();
		int row, col;
		row = this.getSelectedRow();
		col = this.getSelectedColumn();

		if (row == -1) {
			row = this.rowAtPoint(new java.awt.Point(e.getX(), e.getY()));
		}
		int pcol = this.columnAtPoint(new java.awt.Point(e.getX(), e.getY()));
		if (e.getButton() == MouseEvent.BUTTON3) {
			if (col != pcol && pcol > -1) {
				col = pcol;
				setColumnSelectionInterval(col, col);
			}
		} else {
			if (col == -1) {
				col = pcol;
				setColumnSelectionInterval(col, col);
			}
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

	/**
	 * 增加列
	 * 
	 * @param colName
	 */
	public void addColumn(String colName) {
		data.addColumn(colName);
		holdColumnNames();
	}

	/**
	 * 真正删除了指定的列，包括数据Model
	 * 
	 * @param aColumn
	 *            TableColumn列对象
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
			Vector cData = getColumnData(i);
			dataModel.addColumn(tmpName, cData);
		}
		setModel(dataModel);
	}

	/**
	 * 取列对象
	 * 
	 * @param columnIndex
	 *            列号
	 * @return
	 */
	public TableColumn getColumn(int columnIndex) {
		return getColumnModel().getColumn(columnIndex);
	}

	/**
	 * 取列对象
	 * 
	 * @param colName
	 *            列名
	 * @return
	 */
	public int getColumnIndex(String colName) {
		return getColumnIndex(colName, false);
	}

	/**
	 * 取列序号
	 * 
	 * @param colName
	 *            列名
	 * @param includeHideColumns
	 *            是否包含隐藏列
	 * @return
	 */
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

	/**
	 * 删除所有行
	 */
	public void removeAllRows() {
		data.setRowCount(0);
		oldRow = -1;
	}

	/**
	 * 取列数据
	 * 
	 * @param colIndex
	 *            列号
	 * @return
	 */
	public Vector<Object> getColumnData(int colIndex) {
		Vector<Object> cData = new Vector<Object>();
		for (int r = 0; r < data.getRowCount(); r++) {
			cData.add(data.getValueAt(r, colIndex));
		}
		return cData;
	}

	/**
	 * 最小化的列集合
	 */
	private transient HashMap<Integer, String> minimizedColumn = new HashMap<Integer, String>();

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
		String oldConfig = tc.getMaxWidth() + "|" + tc.getMinWidth() + "|"
				+ tc.getPreferredWidth();
		minimizedColumn.put(columnIndex, oldConfig);
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
		String oldConfig = minimizedColumn.get(columnIndex);
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

	/**
	 * 隐藏列
	 * 
	 * @param columnName
	 *            列名
	 */
	public void hideColumn(String columnName) {
		if (!isColumnVisible(columnName)) {
			return;
		}
		TableColumn col = getColumn(columnName);
		Object id = col.getIdentifier();
		columnKeeper.put(id, col);
		removeColumn(col);
	}

	/**
	 * 设置列是否可用
	 * 
	 * @param columnIndex
	 *            列号
	 * @param enable
	 *            是否可用
	 */
	public void setColumnEnable(int columnIndex, boolean enable) {
		setColumnEnable(getColumnName(columnIndex), enable);
	}

	/**
	 * 设置列是否可用
	 * 
	 * @param columnName
	 *            列名
	 * @param enable
	 *            是否可编辑
	 */
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

	/**
	 * 取列编辑器
	 * 
	 * @param columnIndex
	 *            列号
	 * @return
	 */
	public TableCellEditor getColumnEditor(int columnIndex) {
		TableColumn tc = getColumn(getColumnName(columnIndex));
		return tc.getCellEditor();
	}

	/**
	 * 设置列缺省编辑控件
	 * 
	 * @param columnIndex
	 *            列号
	 */
	public void setColumnDefaultEditor(int columnIndex) {
		setColumnDefaultEditor(getColumnName(columnIndex));
	}

	/**
	 * 设置列缺省编辑控件
	 * 
	 * @param columnName
	 *            列名
	 */
	public void setColumnDefaultEditor(String columnName) {
		TableColumn col = getColumn(columnName);
		col.setCellEditor(new SimpleEditor(new JTextField(), this));

		DefaultTableCellRenderer dr = new DefaultTableCellRenderer();
		col.setCellRenderer(dr);
	}

	/**
	 * 设置列数值编辑控件
	 * 
	 * @param columnIndex
	 *            列号
	 */
	public void setColumnSpinner(int columnIndex) {
		setColumnSpinner(getColumnName(columnIndex));
	}

	/**
	 * 设置列数值编辑控件
	 * 
	 * @param columnName
	 *            列名
	 */
	public void setColumnSpinner(String columnName) {
		DefaultCellEditor integerEditor = new JTextAreaEditor(this,
				JTextAreaEditor.TYPE_UNSIGNED_INTEGER);
		TableColumn tc = getColumn(columnName);
		tc.setCellEditor(integerEditor);
	}

	/**
	 * 设置列复选框控件
	 * 
	 * @param columnIndex
	 *            列号
	 */
	public void setColumnCheckBox(int columnIndex) {
		setColumnCheckBox(getColumnName(columnIndex));
	}

	/**
	 * 设置列复选框控件
	 * 
	 * @param columnIndex
	 *            列号
	 * @param enable
	 *            是否可编辑
	 */
	public void setColumnCheckBox(int columnIndex, boolean enable) {
		setColumnCheckBox(getColumnName(columnIndex), enable);
	}

	/**
	 * 设置列复选框控件
	 * 
	 * @param columnName
	 *            列名
	 */
	public void setColumnCheckBox(String columnName) {
		setColumnCheckBox(columnName, true);
	}

	/**
	 * 设置列复选框控件
	 * 
	 * @param columnName
	 *            列名
	 * @param enable
	 *            是否可编辑
	 */
	public void setColumnCheckBox(String columnName, final boolean enable) {
		JCheckBox checkBoxEditor = new JCheckBox();
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stateChanged(new ChangeEvent(e.getSource()));
			}
		};
		checkBoxEditor.addActionListener(al);
		checkBoxEditor.setEnabled(enable);
		checkBoxEditor.setHorizontalAlignment(JLabel.CENTER);

		TableCellEditor cellEditor = new DefaultCellEditor(checkBoxEditor) {
			private static final long serialVersionUID = 1L;

			public boolean isCellEditable(EventObject anEvent) {
				return enable;
			}

			public Component getTableCellEditorComponent(JTable table,
					Object value, boolean isSelected, int row, int column) {
				Component c = super.getTableCellEditorComponent(table, value,
						isSelected, row, column);
				if (isSelected) {
					setForeground(table.getSelectionForeground());
					setBackground(table.getSelectionBackground());
				} else {
					setForeground(table.getForeground());
					setBackground(table.getBackground());
				}
				return c;
			}
		};

		TableCellRenderer cellRenderer = new CheckBoxRenderer();

		TableColumn col = getColumn(columnName);
		col.setCellEditor(cellEditor);
		col.setCellRenderer(cellRenderer);
	}

	/**
	 * 设置列下拉控件
	 * 
	 * @param columnIndex
	 *            列号
	 * @param codeItems
	 *            代码值
	 * @param dispItems
	 *            显示值
	 * @return
	 */
	public JComboBoxEx setColumnDropDown(int columnIndex, Vector codeItems,
			Vector dispItems) {
		return setColumnDropDown(getColumnName(columnIndex), codeItems,
				dispItems);
	}

	/**
	 * 设置列下拉控件
	 * 
	 * @param columnIndex
	 *            列号
	 * @param codeItems
	 *            代码值
	 * @param dispItems
	 *            显示值
	 * @param editable
	 *            是否可编辑
	 * @return
	 */
	public JComboBoxEx setColumnDropDown(int columnIndex, Vector codeItems,
			Vector dispItems, boolean editable) {
		// 多次反复设置下拉列时，当前的下拉控件要先清除掉，否则不能立刻刷新
		acceptText();

		return setColumnDropDown(getColumnName(columnIndex), codeItems,
				dispItems, editable);
	}

	/**
	 * 设置列下拉控件
	 * 
	 * @param columnName
	 *            列名
	 * @param codeItems
	 *            代码值
	 * @param dispItems
	 *            显示值
	 * @return
	 */
	public JComboBoxEx setColumnDropDown(String columnName, Vector codeItems,
			Vector dispItems) {
		return setColumnDropDown(columnName, codeItems, dispItems, false);
	}

	/**
	 * 设置列下拉控件
	 * 
	 * @param columnName
	 *            列名
	 * @param codeItems
	 *            代码值
	 * @param dispItems
	 *            显示值
	 * @param editable
	 *            是否可编辑
	 * @return
	 */
	public JComboBoxEx setColumnDropDown(final String columnName,
			Vector codeItems, Vector dispItems, boolean editable) {
		JComboBoxEx combo = new JComboBoxEx();
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ChangeEvent ce = new ChangeEvent(e.getSource());
				stateChanged(ce);
			}
		};
		combo.addActionListener(al);
		combo.x_setData(codeItems, dispItems);
		combo.setEditable(editable);
		TableColumn col = getColumn(columnName);
		TableCellEditor cellEditor = new JComboBoxExEditor(combo);
		TableCellRenderer cellRenderer = new JComboBoxExRenderer(combo);
		col.setCellEditor(cellEditor);
		col.setCellRenderer(cellRenderer);
		return combo;
	}

	/**
	 * 设置列是否可编辑
	 * 
	 * @param columnIndex
	 *            列号
	 * @param allowEdit
	 *            是否可编辑
	 */
	public void setColumnEditable(int columnIndex, boolean allowEdit) {
		setColumnEditable(getColumnName(columnIndex), allowEdit);
	}

	/**
	 * 设置列是否可编辑
	 * 
	 * @param columnIndex
	 *            列号
	 * @param allowEdit
	 *            是否可编辑
	 * @param centerAlign
	 *            是否居中显示
	 */
	public void setColumnEditable(int columnIndex, boolean allowEdit,
			boolean centerAlign) {
		setColumnEditable(getColumnName(columnIndex), allowEdit, centerAlign);
	}

	/**
	 * 设置列是否可编辑
	 * 
	 * @param columnName
	 *            列名
	 * @param allowEdit
	 *            是否可编辑
	 */
	public void setColumnEditable(String columnName, boolean allowEdit) {
		setColumnEditable(columnName, allowEdit, false);
	}

	/**
	 * 设置列是否可编辑
	 * 
	 * @param columnName
	 *            列名
	 * @param allowEdit
	 *            是否可编辑
	 * @param centerAlign
	 *            是否居中显示
	 */
	public void setColumnEditable(String columnName, boolean allowEdit,
			final boolean centerAlign) {
		if (!isColumnVisible(columnName)) {
			return;
		}
		if (!allowEdit) {
			TableColumn col = getColumn(columnName);
			col.setCellEditor(new JTextAreaEditor(this,
					JTextAreaEditor.TYPE_TEXT_READONLY) {
				private static final long serialVersionUID = 1L;

				public boolean isCellEditable(EventObject anEvent) {
					return false;
				}

				public Component getTableCellEditorComponent(JTable table,
						Object value, boolean isSelected, int row, int column) {
					Component c = super.getTableCellEditorComponent(table,
							value, isSelected, row, column);
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

				public Component getTableCellRendererComponent(JTable table,
						Object value, boolean isSelected, boolean hasFocus,
						int row, int column) {
					Component c = super.getTableCellRendererComponent(table,
							value, isSelected, hasFocus, row, column);
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

	/**
	 * 设置列的最大宽度
	 * 
	 * @param columnIndex
	 *            列号
	 * @param width
	 *            宽度
	 */
	public void setColumnFixedWidth(int columnIndex, int width) {
		String columnName = getColumnName(columnIndex);
		if (!isColumnVisible(columnName)) {
			return;
		}
		TableColumn col = getColumn(columnName);
		col.setPreferredWidth(width);
		col.setMaxWidth(width);
	}

	/**
	 * 设置列宽度
	 * 
	 * @param columnIndex
	 *            列号
	 * @param width
	 *            宽度
	 */
	public void setColumnWidth(int columnIndex, int width) {
		setColumnWidth(getColumnName(columnIndex), width);
	}

	/**
	 * 设置列宽度
	 * 
	 * @param columnName
	 *            列名
	 * @param width
	 *            宽度
	 */
	public void setColumnWidth(String columnName, int width) {
		if (!isColumnVisible(columnName)) {
			return;
		}
		TableColumn col = getColumn(columnName);
		col.setPreferredWidth(width);
	}

	/** 横向左对齐 */
	static public int ALIGN_LEFT = JLabel.LEFT;
	/** 横向居中对齐 */
	static public int ALIGN_CENTER = JLabel.CENTER;
	/** 横向右对齐 */
	static public int ALIGN_RIGHT = JLabel.RIGHT;

	/**
	 * 设置列的横向对齐
	 * 
	 * @param columnIndex
	 *            列号
	 * @param alignment
	 *            对齐方式
	 */
	public void setColumnAlign(int columnIndex, int alignment) {
		setColumnAlign(getColumnName(columnIndex), alignment);
	}

	/**
	 * 设置列的横向对齐
	 * 
	 * @param columnName
	 *            列名
	 * @param alignment
	 *            对齐方式
	 */
	public void setColumnAlign(String columnName, int alignment) {
		if (!isColumnVisible(columnName)) {
			return;
		}
		TableColumn col = getColumn(columnName);
		Object oldRender = col.getCellRenderer();
		if (oldRender == null
				|| (oldRender != null && oldRender instanceof DefaultTableCellRenderer)) {
			DefaultTableCellRenderer cbRender = new DefaultTableCellRenderer();
			cbRender.setHorizontalAlignment(alignment);
			col.setCellRenderer(cbRender);
		}
		Object oldEditor = col.getCellEditor();
		if (oldEditor == null
				|| (oldEditor != null && oldEditor instanceof SimpleEditor)) {
			JTextField tf = new JTextField();
			tf.setHorizontalAlignment(alignment);
			col.setCellEditor(new SimpleEditor(tf, this));
		}
	}

	/**
	 * 取行数据
	 * 
	 * @param row
	 *            行号
	 * @return 行数据数组
	 */
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

	/**
	 * 取行数据的字符串，由\t分隔
	 * 
	 * @param row
	 *            行号
	 * @return
	 */
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

	/**
	 * 取数据块，由\t和\n分隔
	 * 
	 * @return
	 */
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

	/**
	 * 设置数据块的字符串
	 * 
	 * @param ls_data
	 *            由\t和\n分隔
	 * @return
	 */
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

	/**
	 * 设置序号列
	 * 
	 * @param indexCol
	 */
	public void setIndexCol(int indexCol) {
		this.indexCol = indexCol;
		setColumnAlign(indexCol, ALIGN_CENTER);
		setColumnEditable(indexCol, false, true);
		setColumnFixedWidth(indexCol, 40);
	}

	/**
	 * 重置序号列
	 */
	public void resetIndex() {
		if (indexCol == -1) {
			return;
		}

		int c = getRowCount();
		for (int i = 0; i < c; i++) {
			data.setValueAt(new Integer(i + 1), i, indexCol);
		}
	}

	/**
	 * 检查列数据
	 * 
	 * @param colIndex
	 *            列序号
	 * @param colDesc
	 *            列描述
	 * @return
	 */
	public boolean checkColumnData(int colIndex, String colDesc) {
		return verifyColumnData(colIndex, colDesc);
	}

	/**
	 * 检查列数据
	 * 
	 * @param colIndex
	 *            列序号
	 * @param colDesc
	 *            列描述
	 * @return
	 */
	public boolean verifyColumnData(int colIndex, String colDesc) {
		return verifyColumnData(colIndex, colDesc, true);
	}

	/**
	 * 检查列数据
	 * 
	 * @param colIndex
	 *            列序号
	 * @param colDesc
	 *            列描述
	 * @param caseRepeat
	 *            是否检查列数据重复
	 * @return
	 */
	public boolean verifyColumnData(int colIndex, String colDesc,
			boolean caseRepeat) {
		return verifyColumnData(colIndex, colDesc, caseRepeat, GV.appFrame);
	}

	/**
	 * 检查列数据
	 * 
	 * @param colIndex
	 *            列序号
	 * @param colDesc
	 *            列描述
	 * @param caseRepeat
	 *            是否检查列数据重复
	 * @param parent
	 *            父组件
	 * @return
	 */
	public boolean verifyColumnData(int colIndex, String colDesc,
			boolean caseRepeat, Component parent) {
		acceptText();

		HashSet<String> keys = new HashSet<String>();
		int r = getRowCount();
		String key;
		for (int i = 0; i < r; i++) {
			key = (String) data.getValueAt(i, colIndex);
			if (!StringUtils.isValidString(key)) {
				JOptionPane
						.showMessageDialog(
								parent,
								mm.getMessage("jtableex.null",
										String.valueOf((i + 1)), colDesc),
								mm.getMessage("public.note"),
								JOptionPane.WARNING_MESSAGE); // 第：{0}行的{1}为空。
				return false;
			}
			if (caseRepeat && keys.contains(key)) {
				JOptionPane
						.showMessageDialog(parent,
								mm.getMessage("jtableex.repeat") + colDesc
										+ ": " + key,
								mm.getMessage("public.note"),
								JOptionPane.WARNING_MESSAGE);
				return false;
			}
			keys.add(key);
		}
		return true;
	}

	/**
	 * 设置列是否可视
	 * 
	 * @param colName
	 *            列名
	 * @param vis
	 *            是否可视
	 */
	public void setColumnVisible(String colName, boolean vis) {
		if (vis) {
			showColumn(colName);
		} else {
			hideColumn(colName);
		}
	}

	/**
	 * 上移选择行
	 * 
	 * @return
	 */
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
		int start,end;
		if (row < 0) {
			int[] rows = getSelectedRows();
			start = rows[0];
			end = rows[rows.length-1];
		} else {
			start = row;
			end = row;
		}

		if (start <= 0) {
			return -1;
		}
		data.moveRow(start, end, start - 1);
		selectRows(start - 1, end - 1);
		resetIndex();
		return start - 1;
	}

	/**
	 * 下移选择行
	 * 
	 * @return
	 */
	public int shiftDown() {
		return shiftRowDown(-1);
	}

	/**
	 * 下移指定行
	 * 
	 * @param row
	 *            行号
	 * @return
	 */
	public int shiftRowDown(int row) {
		int start,end;
		if (row < 0) {
			int[] rows = getSelectedRows();
			start = rows[0];
			end = rows[rows.length-1];
		} else {
			start = row;
			end = row;
		}
		if (start < 0 || end >= getRowCount() - 1) {
			return -1;
		}
		data.moveRow(start, end, start + 1);
		selectRows(start + 1,end+1);
		resetIndex();
		return start + 1;
	}

	/**
	 * 显示列
	 * 
	 * @param columnName
	 *            列名
	 */
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
		if (cr < r) {
			r = cr;
		}
		selectRow(r);
		resetIndex();
		return true;
	}

	/**
	 * 删除所有选择的行
	 * 
	 * @return
	 */
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

	/**
	 * 删除指定行
	 * 
	 * @param row
	 *            行号
	 * @return
	 */
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

	/**
	 * 选中指定行
	 * 
	 * @param row
	 *            行号
	 */
	public void selectRow(int row) {
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
	 * 补充的多行移动用
	 * @param start
	 * @param end
	 */
	void selectRows(int start, int end) {
		DefaultListSelectionModel selectModel = new DefaultListSelectionModel();
		selectModel.addSelectionInterval(start, end);
		setSelectionModel(selectModel);
	}
	
	public void selectRows(int[] rows) {
		if(rows==null || rows.length==0) {
			return;
		}
		DefaultListSelectionModel selectModel = new DefaultListSelectionModel();
		for(int i=0;i<rows.length;i++) {
			selectModel.addSelectionInterval(rows[i], rows[i]);
		}
		setSelectionModel(selectModel);
	}

	/**
	 * 列选择只能是连续的区间
	 * @param cols
	 */
	public void selectCols(int[] cols) {
		if(cols==null || cols.length==0) {
			return;
		}
		setColumnSelectionInterval(cols[0], cols[cols.length-1]);
	}
	public void selectCol(int col) {
		setColumnSelectionInterval(col, col);
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

	/**
	 * 追加行
	 * 
	 * @return
	 */
	public int addRow() {
		return addRow(true);
	}

	/**
	 * 追加行
	 * 
	 * @param resetIndex
	 *            是否重置序号列
	 * @return
	 */
	public int addRow(boolean resetIndex) {
		return insertRow(-1, null, resetIndex);
	}

	/**
	 * 追加行
	 * 
	 * @param oa
	 *            追加的行数据数组
	 * @return
	 */
	public int addRow(Object[] oa) {
		return addRow(oa, true);
	}

	/**
	 * 追加行
	 * 
	 * @param oa
	 *            追加的行数据数组
	 * @param resetIndex
	 *            是否重置序号列
	 * @return
	 */
	public int addRow(Object[] oa, boolean resetIndex) {
		return insertRow(-1, oa, resetIndex);
	}

	/**
	 * 插入行
	 * 
	 * @param row
	 *            行号
	 * @param rowData
	 *            行数据数组
	 * @return
	 */
	public int insertRow(int row, Object[] rowData) {
		return insertRow(row, rowData, true);
	}

	/**
	 * 插入行
	 * 
	 * @param row
	 *            行号
	 * @param rowData
	 *            行数据数组
	 * @param resetIndex
	 *            是否重置序号列
	 * @return
	 */
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

	/**
	 * 列是否可视
	 * 
	 * @param column
	 *            列标识符
	 * @return
	 */
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

	/**
	 * 行光标变化
	 * 
	 * @param oldRow
	 *            之前选择的行
	 * @param newRow
	 *            新选择的行
	 */
	private void fireRowfocusChanged(int oldRow, int newRow) {
		if (!ifFireRowfocusChangedEvent) {
			return;
		}
		Iterator it = clickedListener.iterator();
		if (oldRow >= this.getRowCount()) {
			oldRow = -1;
		}
		while (it.hasNext()) {
			JTableExListener lis = (JTableExListener) it.next();
			lis.rowfocusChanged(oldRow, newRow);
		}
		this.resizeAndRepaint();
	}

	/**
	 * 取消行焦点变化
	 */
	public void disableRowfocusChanged() {
		ifFireRowfocusChangedEvent = false;
	}

	/**
	 * 激活行焦点变化
	 */
	public void enableRowfocusChanged() {
		ifFireRowfocusChangedEvent = true;
	}

	/**
	 * 选择行变化
	 */
	public void valueChanged(ListSelectionEvent e) {
		int r = getSelectedRow();
		if (r < 0) {
			return;
		}
		if (e.getValueIsAdjusting()) {
			return;
		}
		int n = r;
		if (n == oldRow) {
			return;
		}
		fireRowfocusChanged(oldRow, r);
		oldRow = n;
	}

	/**
	 * 之前选择的行
	 */
	private int oldRow = -1;

	/**
	 * 不可用的背景色
	 */
	private Color disabledBackColor = Color.lightGray;

	/**
	 * 不可用的前景色
	 */
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
	 * 
	 * 不可用的单元格编辑器
	 */
	class DisabledEditor extends DefaultCellEditor {
		private static final long serialVersionUID = 1L;

		public DisabledEditor(JTextField jTxt) {
			super(jTxt);
			jTxt.setBackground(disabledBackColor);
			jTxt.setEditable(false);
		}

		public boolean isCellEditable(EventObject anEvent) {
			return true;
		}
	}

	/**
	 * 取缺省的单元格编辑器
	 * 
	 * @param tf
	 * @param parent
	 * @return
	 */
	public DefaultCellEditor getDefaultCellEditor(JTextField tf, JTableEx parent) {
		return new SimpleEditor(tf, parent);
	}

	/**
	 * 缺省的单元格编辑器
	 *
	 */
	class SimpleEditor extends DefaultCellEditor implements KeyListener,
			MouseListener, FocusListener {

		private static final long serialVersionUID = 1L;
		JTableEx parent;

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
			int absoluteX = e.getX() + editor.getX(), absoluteY = e.getY()
					+ editor.getY();
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
			if (e != null) {
				Object src = e.getSource();
				if (src instanceof JTextField) {
					// 不可editable的TextField仍然能接收到按键事件
					JTextField txt = (JTextField) src;
					if (!txt.isEditable())
						return;
				}
				stateChanged(new ChangeEvent(src));
			}
		}

		/**
		 * keyTyped
		 * 
		 * @param e
		 *            KeyEvent
		 */
		public void keyTyped(KeyEvent e) {
		}
	}

	/**
	 * 表格列监听器
	 *
	 */
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

	/**
	 * 表格内的编辑动作将触发该事件,方便上层的编辑状态联动
	 */
	public void stateChanged(ChangeEvent e) {
	}

}
