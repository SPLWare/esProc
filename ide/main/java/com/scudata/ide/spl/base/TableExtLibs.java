package com.scudata.ide.spl.base;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.plaf.metal.MetalBorders.TableHeaderBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import com.scudata.app.common.Section;
import com.scudata.common.IntArrayList;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.control.TransferableObject;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.ide.spl.resources.IdeSplMessage;

/**
 * 
 * 外部库表滚动面板控件
 *
 */
public class TableExtLibs extends JScrollPane {

	private static final long serialVersionUID = 1L;

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/** 名称列 */
	public static final byte COL_NAME = 1;
	/** 选择列 */
	private final byte COL_SELECT = 2;

	/** 序号 */
	private final String TITLE_INDEX = mm.getMessage("tableselectname.index");
	/** 目录名 */
	private final String TITLE_NAME = IdeSplMessage.get().getMessage(
			"tableselectname.dirname");
	/** 选出 */
	private final String TITLE_SELECT = mm.getMessage("tableselectname.select");

	/**
	 * 表控件
	 */
	private JTableEx tableNames;

	/**
	 * 是否全选
	 */
	private boolean selectAll = false;

	/**
	 * 已存在的名称
	 */
	private Vector<String> existNames;

	/**
	 * 已存在的名称显示的颜色
	 */
	private boolean existColor = true;

	/**
	 * 是否阻止变化
	 */
	private boolean preventChange = false;

	/**
	 * 过滤条件
	 */
	private String filter = null;

	/**
	 * 选择的名称映射
	 */
	private Map<String, Boolean> nameSelected = new HashMap<String, Boolean>();

	/**
	 * 夫组件
	 */
	private Component parent;

	/**
	 * 构造函数
	 */
	public TableExtLibs(JDialog parent) {
		this.parent = parent;
		tableNames = new JTableEx(TITLE_INDEX + "," + TITLE_NAME + ","
				+ TITLE_SELECT) {

			private static final long serialVersionUID = 1L;

			public void setValueAt(Object aValue, int row, int column) {
				if (!isItemDataChanged(row, column, aValue)) {
					return;
				}
				super.setValueAt(aValue, row, column);
				if (preventChange) {
					return;
				}
				setDataChanged();
				if (column == COL_SELECT) {
					boolean selected = aValue != null
							&& ((Boolean) aValue).booleanValue();
					rowSelectedChanged(row, selected);
					String name = (String) data.getValueAt(row, COL_NAME);
					nameSelected.put(name, new Boolean(selected));
					Object tmp;
					boolean rSelected;
					for (int i = 0; i < getRowCount(); i++) {
						tmp = data.getValueAt(i, COL_SELECT);
						rSelected = tmp != null
								&& ((Boolean) tmp).booleanValue();
						if (selected != rSelected) {
							selectAll = false;
							tableNames.getTableHeader().repaint();
							return;
						}
					}
					selectAll = selected;
					allRowsSelected(selectAll);
					tableNames.getTableHeader().repaint();
				}
			}

			public void rowfocusChanged(int oldRow, int newRow) {
				rowChanged(oldRow, newRow);
			}

			public void doubleClicked(int xpos, int ypos, int row, int col,
					MouseEvent e) {
				doubleClick(row, col);
			}
		};
		init();
	}

	/**
	 * 设置已存在的名称显示的颜色
	 * 
	 * @param existColor
	 */
	public void setExistColor(boolean existColor) {
		this.existColor = existColor;
	}

	/**
	 * 取选择的行
	 * 
	 * @return
	 */
	public int getSelectedRow() {
		return tableNames.getSelectedRow();
	}

	/**
	 * 行光标变化了
	 * 
	 * @param oldRow
	 *            旧行号
	 * @param newRow
	 *            新行号
	 */
	public void rowChanged(int oldRow, int newRow) {
	}

	/**
	 * 鼠标双击
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 */
	public void doubleClick(int row, int col) {
	}

	/**
	 * 行选择状态变化
	 * 
	 * @param row
	 *            行号
	 * @param selected
	 *            是否选择行
	 */
	public void rowSelectedChanged(int row, boolean selected) {
	}

	/**
	 * 设置全选/全不选所有行
	 * 
	 * @param allSelected
	 *            是否选择
	 */
	public void allRowsSelected(boolean allSelected) {
	}

	/**
	 * 取行数
	 * 
	 * @return
	 */
	public int getRowCount() {
		return tableNames.getRowCount();
	}

	/**
	 * 取单元格值
	 * 
	 * @param row
	 *            行号
	 * @param col
	 *            列号
	 * @return
	 */
	public Object getValueAt(int row, int col) {
		return tableNames.data.getValueAt(row, col);
	}

	/**
	 * 设置过滤条件
	 * 
	 * @param filter
	 */
	public void setFilter(String filter) {
		this.filter = filter;
	}

	/**
	 * 设置名称列表
	 * 
	 * @param names
	 *            名称
	 * @param reset
	 *            是否重置
	 * @param selectExist
	 *            是否选择已经在的名称
	 */
	public synchronized void setNames(Vector<String> names, boolean reset,
			boolean selectExist) {
		try {
			preventChange = true;
			tableNames.acceptText();
			tableNames.removeAllRows();
			selectAll = false;
			if (!reset)
				nameSelected.clear();
			if (names == null) {
				return;
			}
			int size = names.size();
			String name;
			Pattern p = null;
			if (StringUtils.isValidString(filter)) {
				try {
					p = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);
				} catch (Throwable t) {
				}
			}
			boolean isAllSelected = true;
			Matcher m;
			Boolean selected;
			for (int r = 0; r < size; r++) {
				Object n = names.get(r);
				name = n == null ? null : n.toString();
				if (p != null) {
					m = p.matcher(name);
					if (!m.find())
						continue;
				}
				selected = Boolean.FALSE;
				if (reset) {
					Object tmp = nameSelected.get(name);
					if (tmp != null && tmp instanceof Boolean) {
						selected = (Boolean) tmp;
					}
				} else if (selectExist && existNames != null) {
					selected = new Boolean(existNames.contains(name));
				}
				tableNames.insertRow(-1, new Object[] { new Integer(r + 1),
						name, selected }, false);
				if (!selected.booleanValue())
					isAllSelected = false;
				if (!reset)
					nameSelected.put(name, selected);
			}
			tableNames.resetIndex();
			if (tableNames.getRowCount() > 0) {
				tableNames.selectRow(0);
			}

			if (isAllSelected) {
				selectAll = true;
				tableNames.getTableHeader().repaint();
			}
			repaint();
		} finally {
			preventChange = false;
		}
	}

	/**
	 * 取选择的名称
	 * 
	 * @param tableName
	 *            表名。如果有表名返回tableName.fieldName，无表名时直接返回fieldName。
	 * @return
	 */
	public String[] getSelectedNames(String tableName) {
		return getSelectedNames(tableName, ".");
	}

	/**
	 * 取选择的名称
	 * 
	 * @param tableName
	 *            表名。如果有表名返回tableName.fieldName，无表名时直接返回fieldName。
	 * @param opt
	 *            表名和字段名的连接符号
	 * @return
	 */
	public String[] getSelectedNames(String tableName, String opt) {
		tableNames.acceptText();
		int count = tableNames.getRowCount();
		Section sec = new Section();
		Object tmp;
		String name;
		for (int i = 0; i < count; i++) {
			tmp = tableNames.data.getValueAt(i, COL_SELECT);
			if (tmp == null || !((Boolean) tmp).booleanValue()) {
				continue;
			}
			tmp = tableNames.data.getValueAt(i, COL_NAME);
			if (StringUtils.isValidString(tmp)) {
				name = (String) tmp;
				if (StringUtils.isValidString(tableName)) {
					if (!name.startsWith(tableName + opt)) {
						name = tableName + opt + name;
					}
				}
				sec.addSection(name);
			}
		}
		return sec.toStringArray();
	}

	/**
	 * 取选择的序号
	 * 
	 * @return
	 */
	public int[] getSelectedIndexes() {
		tableNames.acceptText();
		int count = tableNames.getRowCount();
		if (count == 0)
			return null;
		IntArrayList list = new IntArrayList();
		for (int i = 0; i < count; i++) {
			if (isRowSelected(i))
				list.addInt(i);
		}
		if (list.isEmpty())
			return null;
		return list.toIntArray();
	}

	/**
	 * 取行是否选出
	 * 
	 * @param row
	 *            行号
	 * @return
	 */
	public boolean isRowSelected(int row) {
		Object s = tableNames.data.getValueAt(row, COL_SELECT);
		return s != null && ((Boolean) s).booleanValue();
	}

	/**
	 * 设置行是否选出
	 * 
	 * @param row
	 *            行号
	 * @param selected
	 *            是否选出
	 */
	public void setRowSelected(int row, boolean selected) {
		tableNames.data.setValueAt(new Boolean(selected), row, COL_SELECT);
		tableNames.acceptText();
	}

	/**
	 * 设置已经存在的名称
	 * 
	 * @param existNames
	 *            已经存在的名称
	 */
	public void setExistNames(Vector<String> existNames) {
		this.existNames = existNames;
	}

	/**
	 * 初始化
	 */
	private void init() {
		this.getViewport().add(tableNames);
		tableNames.setRowHeight(20);
		tableNames.getTableHeader().setReorderingAllowed(false);
		tableNames.setIndexCol(0);
		tableNames.setColumnEditable(COL_NAME, false);
		tableNames.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JTableHeader header = tableNames.getTableHeader();
		header.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int col = tableNames.columnAtPoint(e.getPoint());
				if (col == COL_SELECT) {
					selectAll(!selectAll);
					setDataChanged();
				}
			}
		});
		final int selectWidth = 75;
		tableNames.getColumn(COL_SELECT).setHeaderRenderer(
				new DefaultTableCellRenderer() {

					private static final long serialVersionUID = 1L;

					public Component getTableCellRendererComponent(
							JTable table, Object value, boolean isSelected,
							boolean hasFocus, int row, int column) {
						JCheckBox cb = new JCheckBox(TITLE_SELECT);
						cb.setSelected(selectAll);
						JPanel p = new JPanel(new BorderLayout());
						p.add(cb, BorderLayout.CENTER);
						p.setFont(table.getFont());
						p.setBorder(new TableHeaderBorder());
						p.setPreferredSize(new Dimension(selectWidth, 20));
						return p;
					}
				});
		tableNames.setColumnFixedWidth(COL_SELECT, selectWidth);
		tableNames.setColumnCheckBox(COL_SELECT);
		tableNames.getColumn(COL_NAME).setCellRenderer(
				new DefaultTableCellRenderer() {

					private static final long serialVersionUID = 1L;

					public Component getTableCellRendererComponent(
							JTable table, Object value, boolean isSelected,
							boolean hasFocus, int row, int column) {
						Component c = super
								.getTableCellRendererComponent(table, value,
										isSelected, hasFocus, row, column);
						if (existColor && existNames != null
								&& existNames.contains(value)) {
							c.setForeground(Color.BLUE);
						} else {
							c.setForeground(Color.BLACK);
						}
						return c;
					}
				});

		DragGestureListener dgl = new DragGestureListener() {
			public void dragGestureRecognized(DragGestureEvent dge) {
				try {
					String[] names = getSelectedNames(null);
					if (names == null) {
						return;
					}
					Transferable tf = new TransferableObject(names);
					if (tf != null) {
						dge.startDrag(
								Cursor.getPredefinedCursor(Cursor.HAND_CURSOR),
								tf);
					}
				} catch (Exception x) {
					GM.showException(parent, x);
				}
			}
		};
		DragSource ds = DragSource.getDefaultDragSource();
		ds.createDefaultDragGestureRecognizer(tableNames,
				DnDConstants.ACTION_COPY, dgl);
	}

	/**
	 * 初始化按钮
	 * 
	 * @param b
	 */
	public void initButton(JButton b) {
		Dimension d = new Dimension(22, 22);
		b.setMaximumSize(d);
		b.setMinimumSize(d);
		b.setPreferredSize(d);
	}

	/**
	 * 全选/全不选
	 * 
	 * @param selected
	 *            是否全选
	 */
	public void selectAll(boolean selected) {
		selectAll = selected;
		int count = tableNames.getRowCount();
		for (int r = 0; r < count; r++) {
			tableNames.data.setValueAt(new Boolean(selectAll), r, COL_SELECT);
			nameSelected.put((String) tableNames.data.getValueAt(r, COL_NAME),
					new Boolean(selectAll));
		}
		tableNames.getTableHeader().repaint();
		allRowsSelected(selectAll);
	}

	/**
	 * 数据变化了
	 */
	protected void setDataChanged() {
	}
}