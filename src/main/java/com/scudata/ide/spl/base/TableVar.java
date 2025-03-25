package com.scudata.ide.spl.base;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumn;

import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.control.TransferableObject;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.AllPurposeEditor;
import com.scudata.ide.common.swing.AllPurposeRenderer;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.ide.spl.resources.IdeSplMessage;
import com.scudata.util.Variant;

/**
 * 变量表控件
 *
 */
public abstract class TableVar extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/**
	 * 是否阻止变化
	 */
	private boolean preventChange = false;

	/**
	 * 变量列表
	 */
	private ParamList vl;

	/**
	 * 构造函数
	 */
	public TableVar() {
		super(new BorderLayout());
		this.setMinimumSize(new Dimension(1, 1));
		init();
	}

	/**
	 * 选择变量
	 * 
	 * @param val
	 *            值
	 * @param varName
	 *            变量名
	 */
	public abstract void select(Object val, String varName);

	/**
	 * 设置变量列表
	 * 
	 * @param pl
	 *            变量列表
	 */
	public synchronized void setParamList(ParamList pl) {
		setParamList(pl, false);
	}

	/**
	 * 设置变量列表
	 * 
	 * @param pl
	 *            变量列表
	 * @param isReset
	 *            是否修改显示值后刷新的
	 */
	public synchronized void setParamList(ParamList pl, boolean isRefresh) {
		if (setParamThread != null) {
			setParamThread.stopThread();
			try {
				setParamThread.join();
			} catch (Exception e) {
			}
		}
		setParamThread = null;
		if (!isRefresh) {
			vl = null;
		}
		try {
			preventChange = true;
			tableVar.acceptText();
			tableVar.removeAllRows();
			tableVar.clearSelection();
		} finally {
			preventChange = false;
		}
		if (pl == null) {
			if (jPSouth.isVisible())
				jPSouth.setVisible(false);
			return;
		}
		if (isRefresh) {
			vl = pl;
		} else {
			vl = new ParamList();
			pl.getAllVarParams(vl);
		}
		int dispRows = getDispRows();

		if (vl.count() > DEFAULT_ROW_COUNT) {
			if (!jPSouth.isVisible())
				jPSouth.setVisible(true);
		} else {
			if (jPSouth.isVisible())
				jPSouth.setVisible(false);
		}

		setParamThread = new SetParamThread(vl, dispRows);
		SwingUtilities.invokeLater(setParamThread);
	}

	/**
	 * 设置参数的线程
	 */
	private SetParamThread setParamThread = null;

	class SetParamThread extends Thread {
		private ParamList pl;

		private int dispRows;
		/**
		 * 是否停止了
		 */
		boolean isStoped = false;

		/**
		 * 构造函数
		 * 
		 * @param pl
		 */
		public SetParamThread(ParamList pl, int dispRows) {
			this.pl = pl;
			this.dispRows = dispRows;
		}

		/**
		 * 执行
		 */
		public void run() {
			try {
				preventChange = true;
				Param p;
				int count = pl.count();
				count = Math.min(count, dispRows);
				for (int i = 0; i < count; i++) {
					if (isStoped) {
						break;
					}
					p = pl.get(i);
					int r = tableVar.addRow();
					tableVar.data.setValueAt(p.getName(), r, COL_NAME);
					tableVar.data.setValueAt(p.getValue(), r, COL_VALUE);
				}
			} finally {
				preventChange = false;
			}
		}

		/**
		 * 停止线程
		 */
		void stopThread() {
			isStoped = true;
		}
	}

	/**
	 * 初始化
	 */
	private void init() {
		JScrollPane jSPTable = new JScrollPane(tableVar);
		this.add(jSPTable, BorderLayout.CENTER);
		this.add(jPSouth, BorderLayout.SOUTH);
		jPSouth.setVisible(false);

		jPSouth.add(jLDispRows1, GM.getGBC(0, 2, false, false, 2));
		jPSouth.add(jSDispRows, GM.getGBC(0, 3, false, false, 0));
		jPSouth.add(jLDispRows2, GM.getGBC(0, 4, false, false, 2));
		jPSouth.add(new JPanel(), GM.getGBC(0, 5, true));

		jSDispRows.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				setParamList(vl, true);
			}

		});

		tableVar.setIndexCol(COL_INDEX);
		tableVar.setRowHeight(20);

		TableColumn tc = tableVar.getColumn(COL_VALUE);
		tc.setCellEditor(new AllPurposeEditor(new JTextField(), tableVar));
		tc.setCellRenderer(new AllPurposeRenderer());

		DragGestureListener dgl = new DragGestureListener() {
			public void dragGestureRecognized(DragGestureEvent dge) {
				try {
					int row = tableVar.getSelectedRow();
					if (!StringUtils.isValidString(tableVar.data.getValueAt(
							row, COL_NAME))) {
						return;
					}
					String name = (String) tableVar.data.getValueAt(row,
							COL_NAME);
					Object data = null;
					if (dge.getTriggerEvent().isControlDown()) {
						data = name;
					} else {
						data = "=" + name;
					}
					Transferable tf = new TransferableObject(data);
					if (tf != null) {
						dge.startDrag(GM.getDndCursor(), tf);
					}
				} catch (Exception x) {
					GM.showException(GV.appFrame, x);
				}
			}
		};
		DragSource ds = DragSource.getDefaultDragSource();
		ds.createDefaultDragGestureRecognizer(tableVar,
				DnDConstants.ACTION_COPY, dgl);
	}

	/**
	 * 取显示行
	 * 
	 * @return
	 */
	private int getDispRows() {
		if (!jPSouth.isVisible())
			return DEFAULT_ROW_COUNT;
		int dispRows = ((Number) jSDispRows.getValue()).intValue();
		return dispRows;
	}

	/** 序号列 */
	private final byte COL_INDEX = 0;
	/** 名称列 */
	private final byte COL_NAME = 1;
	/** 值列 */
	private final byte COL_VALUE = 2;

	/**
	 * 变量表控件。序号,名称,值。
	 */
	private JTableEx tableVar = new JTableEx(
			mm.getMessage("jtabbedparam.tableconst")) {

		private static final long serialVersionUID = 1L;

		public void rowfocusChanged(int oldRow, int newRow) {
			if (preventChange) {
				return;
			}
			if (newRow != -1) {
				select(data.getValueAt(newRow, COL_VALUE),
						data.getValueAt(newRow, COL_NAME) == null ? ""
								: (String) data.getValueAt(newRow, COL_NAME));
			}
		}

		public void setValueAt(Object value, int row, int col) {
			if (!isItemDataChanged(row, col, value)) {
				return;
			}
			super.setValueAt(value, row, col);
			if (preventChange) {
				return;
			}
			// ParamList varList = new ParamList();
			// vl.getAllVarParams(varList);
			if (vl != null) {
				Param p = vl.get(row);
				if (col == COL_NAME) {
					p.setName(value == null ? null : (String) value);
				} else {
					if (value == null) {
						p.setValue(null);
					} else if (StringUtils.isValidString(value)) {
						String str = value.toString();
						Object val = Variant.parse(str);
						p.setValue(val);
						preventChange = true;
						data.setValueAt(val, row, col);
						preventChange = false;
					} else {
						p.setValue(value);
					}
				}
			}
		}

		public void mousePressed(MouseEvent e) {
			if (e == null) {
				return;
			}
			Point p = e.getPoint();
			if (p == null) {
				return;
			}
			int row = rowAtPoint(p);
			if (row != -1) {
				select(data.getValueAt(row, COL_VALUE),
						data.getValueAt(row, COL_NAME) == null ? ""
								: (String) data.getValueAt(row, COL_NAME));
			}
		}

		public void doubleClicked(int xpos, int ypos, int row, int col,
				MouseEvent e) {
			if (row != -1) {
				select(data.getValueAt(row, COL_VALUE),
						data.getValueAt(row, COL_NAME) == null ? ""
								: (String) data.getValueAt(row, COL_NAME));
			}
		}
	};

	private JLabel jLDispRows1 = new JLabel(IdeSplMessage.get().getMessage(
			"panelvalue.disprows1"));
	private JLabel jLDispRows2 = new JLabel(IdeSplMessage.get().getMessage(
			"tablevar.dispvar"));

	private static final int DEFAULT_ROW_COUNT = 100;

	/**
	 * 显示的最大行数面板
	 */
	private JSpinner jSDispRows = new JSpinner(new SpinnerNumberModel(
			DEFAULT_ROW_COUNT, 1, Integer.MAX_VALUE, 1));

	private JPanel jPSouth = new JPanel(new GridBagLayout());
}
