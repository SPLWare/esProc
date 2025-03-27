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
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumn;

import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.dm.Param;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.control.TransferableObject;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.AllPurposeEditor;
import com.scudata.ide.common.swing.AllPurposeRenderer;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.ide.spl.SheetSpl;
import com.scudata.ide.spl.resources.IdeSplMessage;
import com.scudata.util.Variant;

/**
 * 任务空间表控件
 */
public abstract class JTableJobSpace extends JPanel {
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
	 * 构造函数
	 */
	public JTableJobSpace() {
		super(new BorderLayout());
		this.setMinimumSize(new Dimension(1, 1));
		init();
	}

	/**
	 * 选择了参数
	 * 
	 * @param val
	 * @param varName
	 */
	public abstract void select(Object val, String varName, String spaceName);

	public void selectParam(int row) {
		if (row < 0)
			return;
		select(tableVar.data.getValueAt(row, COL_VALUE), getVarName(row),
				getSpaceName(row));
	}

	private HashMap<String, Param[]> paramMap;

	/**
	 * 重置任务空间
	 */
	public synchronized void setJobSpaces(HashMap<String, Param[]> paramMap) {
		this.paramMap = paramMap;
		tableVar.acceptText();
		tableVar.clearSelection();
		tableVar.removeAllRows();
		if (paramMap != null) {
			preventChange = true;

			int dispRows = getDispRows();
			boolean southVisible = false;
			Iterator<String> it = paramMap.keySet().iterator();
			while (it.hasNext()) {
				String jsId = it.next();
				if (GV.appSheet != null) {
					String spaceId = ((SheetSpl) GV.appSheet).getSpaceId();
					if (spaceId == null || !spaceId.equals(jsId)) {
						continue;
					}
				}
				
				Param[] paras = paramMap.get(jsId);
				if (!addJobSpaceRow(jsId, paras, dispRows)) {
					southVisible = true;
					break;
				}
			}
			if (southVisible) {
				if (!jPSouth.isVisible())
					jPSouth.setVisible(true);
			} else {
				if (jPSouth.isVisible())
					jPSouth.setVisible(false);
			}
			preventChange = false;
		} else {
			if (jPSouth.isVisible())
				jPSouth.setVisible(false);
		}
	}

	/**
	 * 增加任务空间到表格行
	 * 
	 * @param id
	 * @param params
	 * @param dispRows
	 *            显示行数
	 * @return 是否继续增加
	 */
	private boolean addJobSpaceRow(String id, Param[] params, int dispRows) {
		for (int j = 0; j < params.length; j++) {
			int row = tableVar.addRow();
			tableVar.data.setValueAt(id, row, COL_SPACE);
			tableVar.data.setValueAt(params[j].getName(), row, COL_NAME);
			tableVar.data.setValueAt(params[j].getValue(), row, COL_VALUE);
			if (row + 1 >= dispRows && j != params.length - 1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 取任务空间名
	 * 
	 * @param row
	 * @return
	 */
	private String getSpaceName(int row) {
		return tableVar.data.getValueAt(row, COL_SPACE) == null ? ""
				: (String) tableVar.data.getValueAt(row, COL_SPACE);
	}

	/**
	 * 取变量名
	 * 
	 * @param row
	 * @return
	 */
	private String getVarName(int row) {
		return tableVar.data.getValueAt(row, COL_NAME) == null ? ""
				: (String) tableVar.data.getValueAt(row, COL_NAME);
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
				setJobSpaces(paramMap);
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
		tableVar.setColumnVisible(TITLE_VAR, false);

		tableVar.setColumnVisible(tableVar.getColumnName(COL_SPACE), false);
	}

	/** 序号列 */
	private final byte COL_INDEX = 0;
	/** 任务空间ID */
	private final byte COL_SPACE = 1;
	/** 参数名 */
	private final byte COL_NAME = 2;
	/** 参数值 */
	private final byte COL_VALUE = 3;
	/** 参数对象，隐藏列 */
	private final byte COL_VAR = 4;

	/** 参数对象列标签，隐藏列 */
	private final String TITLE_VAR = "TITLE_VAR";

	/**
	 * 参数表控件。 序号,空间名,变量名,变量值,TITLE_VAR
	 */
	private JTableEx tableVar = new JTableEx(
			mm.getMessage("jtablejobspace.tablenames") + "," + TITLE_VAR) {
		private static final long serialVersionUID = 1L;

		public void rowfocusChanged(int oldRow, int newRow) {
			if (preventChange) {
				return;
			}
			if (newRow != -1) {
				selectParam(newRow);
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
			Param p = (Param) data.getValueAt(row, COL_VAR);

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
				selectParam(row);
			}
		}

		public void doubleClicked(int xpos, int ypos, int row, int col,
				MouseEvent e) {
			if (row != -1) {
				selectParam(row);
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
