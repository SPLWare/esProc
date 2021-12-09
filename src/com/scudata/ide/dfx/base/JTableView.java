package com.scudata.ide.dfx.base;

import java.awt.HeadlessException;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Stack;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.DBConfig;
import com.scudata.common.DBInfo;
import com.scudata.common.Matrix;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.dm.DBObject;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.ide.common.AppendDataThread;
import com.scudata.ide.common.DBTypeEx;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.control.CellRect;
import com.scudata.ide.common.control.CellSelection;
import com.scudata.ide.common.dialog.DialogCellFormat;
import com.scudata.ide.common.swing.AllPurposeEditor;
import com.scudata.ide.common.swing.AllPurposeRenderer;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.ide.dfx.resources.IdeDfxMessage;
import com.scudata.util.Variant;

public abstract class JTableView extends JTableEx {
	private static final long serialVersionUID = 1;
	/**
	 * 集算器资源管理器
	 */
	private MessageManager mm = IdeDfxMessage.get();

	/**
	 * 成员
	 */
	private final String COL_SERIES = mm.getMessage("jtablevalue.menber");

	/** 缺省 */
	private final byte TYPE_DEFAULT = 0;
	/** 序表 */
	private final byte TYPE_TABLE = 1;
	/** 序列 */
	private final byte TYPE_SERIES = 2;
	/** 记录 */
	private final byte TYPE_RECORD = 3;
	/** 纯排列 */
	private final byte TYPE_PMT = 4;
	/** 排列，暂时弃用，按照普通序列进行显示 */
	private final byte TYPE_SERIESPMT = 5;
	/** DBInfo对象 */
	private final byte TYPE_DB = 6;
	/** FileObject对象 */
	private final byte TYPE_FILE = 7;
	/**
	 * 值类型
	 */
	private byte m_type = TYPE_DEFAULT;

	/**
	 * 第一列
	 */
	private final int COL_FIRST = 0;

	/**
	 * 值
	 */
	private Object value;

	/**
	 * 用于数据钻取和返回的堆栈
	 */
	private Stack<Object> undo = new Stack<Object>();
	private Stack<Object> redo = new Stack<Object>();

	/** 复制值 */
	private final short iCOPY = 11;
	/** 设置列格式 */
	private final short iFORMAT = 17;

	/**
	 * 构造函数
	 */
	public JTableView() {
		super();
	}

	/**
	 * 鼠标双击时钻取数据
	 */
	public void doubleClicked(int xpos, int ypos, int row, int col, MouseEvent e) {
		drillValue(row, col);
	}

	/**
	 * 鼠标右键菜单
	 */
	public void rightClicked(int xpos, int ypos, int row, int col, MouseEvent e) {
		JPopupMenu pm = new JPopupMenu();
		JMenuItem mItem;
		int selectedRow = getSelectedRow();
		int selectedCol = getSelectedColumn();
		boolean selectCell = selectedRow != -1 && selectedCol != -1;
		mItem = new JMenuItem(mm.getMessage("jtablevalue.copy")); // 复制
		mItem.setIcon(GM.getMenuImageIcon("copy"));
		mItem.setName(String.valueOf(iCOPY));
		mItem.addActionListener(popAction);
		mItem.setEnabled(selectCell);
		pm.add(mItem);

		if (selectedCol > -1 && (m_type == TYPE_TABLE || m_type == TYPE_PMT || m_type == TYPE_SERIESPMT)) {
			mItem = new JMenuItem(mm.getMessage("jtablevalue.editformat")); // 列格式编辑
			mItem.setIcon(GM.getMenuImageIcon("blank"));
			mItem.setName(String.valueOf(iFORMAT));
			mItem.addActionListener(popAction);
			pm.addSeparator();
			pm.add(mItem);
		}

		pm.show(e.getComponent(), e.getX(), e.getY());
	}

	/**
	 * 右键菜单事件监听
	 */
	private ActionListener popAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			JMenuItem mItem = (JMenuItem) e.getSource();
			short cmd = Short.parseShort(mItem.getName());
			switch (cmd) {
			case iCOPY:
				copyValue();
				break;
			case iFORMAT:
				colFormat();
				break;
			}
		}
	};

	/**
	 * 列格式编辑
	 */
	private void colFormat() {
		int col = getSelectedColumn();
		if (col < 0) {
			return;
		}
		String colName = null;
		if (!StringUtils.isValidString(colName)) {
			colName = getColumnName(col);
		}
		String format = GM.getColumnFormat(colName);
		DialogCellFormat dcf = new DialogCellFormat();
		if (format != null) {
			dcf.setFormat(format);
		}
		dcf.setVisible(true);
		if (dcf.getOption() == JOptionPane.OK_OPTION) {
			format = dcf.getFormat();
			GM.saveFormat(colName, format);
			setColFormat(col, format);
		}

	}

	/**
	 * 设置列格式
	 * 
	 * @param col    列号
	 * @param format 格式
	 */
	private void setColFormat(int col, String format) {
		TableColumn tc = getColumn(col);
		tc.setCellEditor(new AllPurposeEditor(new JTextField(), this));
		tc.setCellRenderer(new AllPurposeRenderer(format));
		this.repaint();
	}

	/**
	 * 刷新
	 */
	public void refresh() {
		privateAction(value);
	}

	/**
	 * 值是否null
	 * 
	 * @return
	 */
	public boolean valueIsNull() {
		return value == null;
	}

	/**
	 * 刷新按钮状态
	 */
	public abstract void refreshValueButton();

	/**
	 * 设置数据
	 */
	public void setData() {
		if (resetThread != null) {
			resetThread.stopThread();
			try {
				resetThread.join();
			} catch (Exception e) {
			}
		}
		resetThread = null;
		Sequence s;
		switch (m_type) {
		case TYPE_DB:
			s = dbTable;
			break;
		case TYPE_TABLE:
		case TYPE_PMT:
		case TYPE_SERIESPMT:
		case TYPE_SERIES:
			s = (Sequence) value;
			break;
		default:
			return;
		}
		SwingUtilities.invokeLater(resetThread = new ResetDataThread(s));
	}

	/**
	 * 线程实例
	 */
	private ResetDataThread resetThread = null;

	/**
	 * 设置序列(排列)数据的线程
	 *
	 */
	class ResetDataThread extends Thread {
		Sequence seq;
		boolean isStoped = false, isFinished = false;

		ResetDataThread(Sequence seq) {
			this.seq = seq;
		}

		public void run() {
			try {
				if (seq == null) {
					removeAllRows();
					return;
				}
				if (isStoped)
					return;
				Object rowData;
				for (int i = 1, count = seq.length(); i <= count; i++) {
					if (isStoped)
						return;
					rowData = seq.get(i);

					if (rowData instanceof Record) {
						insertRow(-1, getRecordData((Record) seq.get(i), i - 1), false);
					} else {
						insertRow(-1, new Object[] { seq.get(i) }, false);
					}
				}
			} catch (Exception ex) {
			} finally {
				isFinished = true;
			}
		}

		void stopThread() {
			seq = null;
			isStoped = true;
		}

		boolean isFinished() {
			return isFinished;
		}
	}

	/**
	 * 获取记录的数据
	 * 
	 * @param record 记录对象
	 * @param r      行号
	 * @return
	 */
	private Object[] getRecordData(Record record, int r) {
		if (record == null || r < 0)
			return null;
		int colCount = this.getColumnCount();
		Object[] rowData = new Object[colCount];
		if (m_type == TYPE_TABLE) {
			for (int i = 0; i < colCount; i++) {
				rowData[i] = record.getFieldValue(i);
			}
		} else {
			DataStruct ds = record.dataStruct();
			String nNames[] = ds.getFieldNames();
			if (nNames != null) {
				Object val;
				for (int j = 0; j < nNames.length; j++) {
					try {
						val = record.getFieldValue(nNames[j]);
					} catch (Exception e) {
						// 取不到的显示空
						val = null;
					}
					int col = getColumnIndex(nNames[j]);
					if (col > -1)
						rowData[col] = val;
				}
			}
		}
		return rowData;
	}

	/**
	 * 单元格是否可以编辑
	 */
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	/**
	 * 清理
	 */
	public void clear() {
		undo.clear();
		redo.clear();
		value = null;
		initJTable();
	}

	/**
	 * 设置单元格值
	 * 
	 * @param value 单元格值
	 */
	public void setValue(Object value) {
		setValue(value, false);
	}

	/**
	 * 设置单元格值
	 * 
	 * @param value         单元格值
	 * @param privateAction 是否内部设置动作
	 */
	private synchronized void setValue(Object value, boolean privateAction) {
		this.value = value;
		dbTable = null;
		try {
			initJTable();
			if (!privateAction) {
				undo.clear();
				redo.clear();
			}
			refreshValueButton();
			if (value == null) {
				if (resetThread != null) {
					resetThread.stopThread();
					try {
						resetThread.join();
					} catch (Exception e) {
					}
				}
				resetThread = null;
				return;
			}
			m_type = getValueType(value);
			switch (m_type) {
			case TYPE_TABLE:
				initTable((Table) value);
				break;
			case TYPE_PMT:
				initPmt((Sequence) value);
				break;
			case TYPE_RECORD:
				initRecord((Record) value);
				break;
			case TYPE_SERIES:
				initSeries((Sequence) value);
				break;
			case TYPE_SERIESPMT:
				initSeriesPmt((Sequence) value);
				break;
			case TYPE_DB:
				initDB((DBObject) value);
				break;
			case TYPE_FILE:
				initFile((FileObject) value);
				break;
			default:
				initDefault(value);
				break;
			}
		} finally {
			setData();
		}
	}

	/**
	 * 初始化表格控件
	 */
	private void initJTable() {
		removeAllRows();
		data.getDataVector().clear();
		int colCount = getColumnCount();
		if (colCount == 0) {
			return;
		}
		for (int i = colCount - 1; i >= 0; i--) {
			try {
				deleteColumn(getColumn(i));
			} catch (Exception ex) {
			}
		}
	}

	/**
	 * 取值的类型
	 * 
	 * @param value 值
	 * @return
	 */
	private byte getValueType(Object value) {
		if (value instanceof Table) {
			return TYPE_TABLE;
		} else if (value instanceof Sequence) {
			if (((Sequence) value).isPurePmt()) {
				return TYPE_PMT;
			} else if (((Sequence) value).isPmt()) {
				return TYPE_SERIESPMT;
			}
			return TYPE_SERIES;
		} else if (value instanceof Record) {
			return TYPE_RECORD;
		} else if (value instanceof DBObject) {
			return TYPE_DB;
		} else if (value instanceof FileObject) {
			return TYPE_FILE;
		} else {
			return TYPE_DEFAULT;
		}
	}

	/**
	 * 初始化序表
	 * 
	 * @param table 序表
	 * @return
	 */
	private int initTable(Table table) {
		DataStruct ds = table.dataStruct();
		setTableColumns(ds);
		setEditStyle(ds);
		return table.length();
	}

	/**
	 * 初始化纯排列
	 * 
	 * @param pmt 纯排列
	 * @return
	 */
	private int initPmt(Sequence pmt) {
		DataStruct ds = pmt.dataStruct();
		setTableColumns(ds);
		setEditStyle(ds);
		return pmt.length();
	}

	/**
	 * 初始化排列
	 * 
	 * @param pmt
	 * @return
	 */
	private int initSeriesPmt(Sequence pmt) {
		DataStruct ds = ((Record) pmt.ifn()).dataStruct();
		setTableColumns(ds);
		setEditStyle(ds);
		return pmt.length();
	}

	/**
	 * 初始化序列
	 * 
	 * @param series
	 * @return
	 */
	private int initSeries(Sequence series) {
		addColumn(COL_SERIES);
		setColumnWidth(COL_FIRST, 200);
		TableColumn tc = getColumn(COL_FIRST);
		tc.setCellEditor(new AllPurposeEditor(new JTextField(), this));
		tc.setCellRenderer(new AllPurposeRenderer());
		return series.length();
	}

	/**
	 * 初始化记录
	 * 
	 * @param record 记录
	 * @return
	 */
	private int initRecord(Record record) {
		DataStruct ds = record.dataStruct();
		setTableColumns(ds);
		try {
			AppendDataThread.addRecordRow(this, record);
		} catch (Exception ex) {
			GM.showException(ex);
		}
		setEditStyle(ds);
		return 1;
	}

	/**
	 * 设置编辑风格
	 * 
	 * @param ds 数据结构
	 */
	private void setEditStyle(DataStruct ds) {
		String cols[] = ds.getFieldNames();
		TableColumn tc;
		for (int i = 0; i < cols.length; i++) {
			tc = this.getColumn(i);
			String format = GM.getColumnFormat(cols[i]);
			if (StringUtils.isValidString(format)) {
				tc.setCellEditor(new AllPurposeEditor(new JTextField(), this));
				tc.setCellRenderer(new AllPurposeRenderer(format));
			}
		}

		GM.setEditStyle(this);
	}

	/**
	 * 设置表格的列
	 * 
	 * @param ds 数据结构
	 */
	private void setTableColumns(DataStruct ds) {
		String nNames[] = ds.getFieldNames();
		if (nNames != null) {
			for (int i = 0; i < nNames.length; i++) {
				addColumn(nNames[i]);
			}
		}
	}

	/**
	 * DBInfo对象对应的序表
	 */
	private Table dbTable = null;

	/** 属性名 */
	private final String TITLE_NAME = mm.getMessage("jtablevalue.name");
	/** 属性值 */
	private final String TITLE_PROP = mm.getMessage("jtablevalue.property");

	/**
	 * 初始化DBInfo对象
	 * 
	 * @param db DBInfo对象
	 * @return
	 */
	private int initDB(DBObject db) {
		dbTable = new Table(new String[] { TITLE_NAME, TITLE_PROP });
		addColumn(TITLE_NAME); // 名称
		addColumn(TITLE_PROP); // 属性
		for (int i = 0; i < this.getColumnCount(); i++) {
			setColumnEditable(i, false);
		}
		if (db == null || db.getDbSession() == null) {
			return 0;
		}
		initDBTable(db);
		return dbTable.length();
	}

	/** 数据源名称 */
	private final String DB_NAME = mm.getMessage("jtablevalue.dbname");
	/** 用户名 */
	private final String USER = mm.getMessage("jtablevalue.user");
	/** 数据库类型 */
	private final String DB_TYPE = mm.getMessage("jtablevalue.dbtype");
	/** 驱动程序 */
	private final String DRIVER = mm.getMessage("jtablevalue.driver");
	/** 数据源URL */
	private final String URL = mm.getMessage("jtablevalue.url");
	/** 对象名带模式 */
	private final String USE_SCHEMA = mm.getMessage("jtablevalue.useschema");
	/** 对象名带限定符 */
	private final String ADD_TILDE = mm.getMessage("jtablevalue.addtilde");

	private void initDBTable(DBObject db) {
		DBInfo info = db.getDbSession().getInfo();
		dbTable.newLast(new Object[] { DB_NAME, info.getName() });
		if (info instanceof DBConfig) {
			int type = info.getDBType();
			dbTable.newLast(new Object[] { DB_TYPE, DBTypeEx.getDBTypeName(type) });

			DBConfig dc = (DBConfig) info;
			dbTable.newLast(new Object[] { DRIVER, dc.getDriver() });
			dbTable.newLast(new Object[] { URL, dc.getUrl() });
			dbTable.newLast(new Object[] { USER, dc.getUser() });
			dbTable.newLast(new Object[] { USE_SCHEMA, Boolean.toString(dc.isUseSchema()) });
			dbTable.newLast(new Object[] { ADD_TILDE, Boolean.toString(dc.isAddTilde()) });
		}
	}

	/**
	 * 初始化FileObject对象
	 * 
	 * @param file FileObject对象
	 * @return
	 */
	private int initFile(FileObject file) {
		addColumn(mm.getMessage("public.file"));
		return initSingleValue(file);
	}

	/**
	 * 初始化普通值
	 * 
	 * @param value 普通值
	 * @return
	 */
	private int initDefault(Object value) {
		addColumn(mm.getMessage("public.value"));
		return initSingleValue(value);
	}

	/**
	 * 初始化单独的值
	 * 
	 * @param value
	 * @return
	 */
	private int initSingleValue(final Object value) {
		setColumnWidth(COL_FIRST, 200);
		TableColumn tc = getColumn(0);
		tc.setCellEditor(new AllPurposeEditor(new JTextField(), this));
		tc.setCellRenderer(new AllPurposeRenderer());
		if (getRowCount() == 0) {
			addRow();
		}
		data.setValueAt(value, 0, COL_FIRST);
		return 1;
	}

	/**
	 * 显示格值
	 */
	public void dispCellValue() {
		int r = getSelectedRow();
		int c = getSelectedColumn();
		drillValue(r, c);
	}

	/**
	 * 钻取成员值
	 * 
	 * @param row 行号
	 * @param col 列号
	 */
	private void drillValue(int row, int col) {
		Object newValue = null;
		switch (m_type) {
		case TYPE_TABLE:
		case TYPE_PMT:
		case TYPE_SERIES:
		case TYPE_SERIESPMT:
			Sequence s = (Sequence) value;
			Object temp = s.get(row + 1);
			if (temp instanceof Record) {
				Record r = (Record) temp;
				if (r.dataStruct() != null && s.dataStruct() != null && !r.dataStruct().equals(s.dataStruct())) { // 异构排列
					newValue = temp;
				}
			}
			break;
		default:
			break;
		}
		if (newValue == null) {
			newValue = data.getValueAt(row, col);
			if (newValue == null) {
				return;
			}
		}
		if (newValue.equals(value)) { // 钻取的元素是本身时
			return;
		}
		redo.clear();
		undo.push(value);
		value = newValue;
		privateAction(value);
	}

	/**
	 * 内部设置值，不考虑锁的状态
	 * 
	 * @param newValue
	 */
	private void privateAction(Object newValue) {
		setValue(newValue, true);
	}

	/**
	 * 是否可以撤回
	 * 
	 * @return
	 */
	public boolean canUndo() {
		return !undo.empty();
	}

	/**
	 * 是否可以重做
	 * 
	 * @return
	 */
	public boolean canRedo() {
		return !redo.empty();
	}

	/**
	 * 撤回
	 */
	public void undo() {
		redo.push(value);
		value = undo.pop();
		privateAction(value);
	}

	/**
	 * 重做
	 */
	public void redo() {
		undo.push(value);
		value = redo.pop();
		privateAction(value);
	}

	/**
	 * 复制数据
	 * 
	 * @return
	 */
	public boolean copyValue() {
		int rows[] = getSelectedRows();
		if (rows == null || rows.length == 0) {
			return false;
		}
		int cc = getColumnCount();
		Matrix matrix = new Matrix(rows.length, cc);
		CellRect cr = new CellRect(0, (short) 0, rows.length - 1, (short) (cc - 1));
		for (int r = 0; r < rows.length; r++) {
			for (int c = 0; c < getColumnCount(); c++) {
				Object value = data.getValueAt(rows[r], c);
				PgmNormalCell pnc = new PgmNormalCell();
				pnc.setValue(value);
				try {
					pnc.setExpString(Variant.toExportString(value));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				matrix.set(r, c, pnc);
			}
		}
		GV.cellSelection = new CellSelection(matrix, cr, null);
		Clipboard cb = null;
		try {
			cb = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
		} catch (HeadlessException e) {
			cb = null;
		}
		String strCS = GM.getCellSelectionString(matrix, false);
		if (cb != null) {
			cb.setContents(new StringSelection(strCS), null);
		}
		GV.cellSelection.systemClip = strCS;
		return true;
	}

	/**
	 * 取单元格值
	 */
	public Object getValueAt(int row, int col) {
		try {
			return super.getValueAt(row, col);
		} catch (Exception ex) {
			return null;
		}
	}

}
