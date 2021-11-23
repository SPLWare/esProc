package com.scudata.ide.vdb.control;

import java.awt.BorderLayout;
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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.plaf.metal.MetalBorders.TableHeaderBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import com.scudata.common.IntArrayList;
import com.scudata.common.StringUtils;
import com.scudata.ide.vdb.commonvdb.*;
import com.scudata.ide.vdb.resources.IdeMessage;

public class TableSelect extends JTableEx {

	private static final long serialVersionUID = 1L;

	private int COL_NAME = 1;

	private int COL_SELECT = 2;

	private int VALUE_COL = COL_SELECT;

	private boolean selectAll = true;

	public TableSelect(String[] colNames) {
		this(colNames, 1, colNames.length - 1, colNames.length - 1);
	}

	public TableSelect(String[] colNames, int colName, int valueCol, int colSelect) {
		super(colNames);
		COL_NAME = colName;
		COL_SELECT = colSelect;
		VALUE_COL = valueCol;
		init();
	}

	public void rowSelectedChanged(int row, boolean selected) {
	}

	public void allRowsSelected(boolean allSelected) {
	}

	public void setValueAt(Object aValue, int row, int column) {
		if (!isItemDataChanged(row, column, aValue)) {
			return;
		}
		super.setValueAt(aValue, row, column);
		dataChanged();
		if (column == VALUE_COL) {
			boolean selected = aValue != null && ((Boolean) aValue).booleanValue();
			rowSelectedChanged(row, selected);
			Object tmp;
			boolean rSelected;
			for (int i = 0; i < getRowCount(); i++) {
				tmp = data.getValueAt(i, COL_SELECT);
				rSelected = tmp != null && ((Boolean) tmp).booleanValue();
				if (selected != rSelected) {
					selectAll = false;
					getTableHeader().repaint();
					return;
				}
			}
			selectAll = selected;
			allRowsSelected(selectAll);
			getTableHeader().repaint();
			this.repaint();
		}
	}

	public String[] getSelectedNames() {
		return getSelectedNames(null, null);
	}

	public String[] getSelectedNames(String tableName, String sep) {
		acceptText();
		int count = getRowCount();
		List<String> names = new ArrayList<String>();
		Object tmp;
		String name;
		for (int i = 0; i < count; i++) {
			tmp = data.getValueAt(i, COL_SELECT);
			if (tmp == null || !((Boolean) tmp).booleanValue()) {
				continue;
			}
			tmp = data.getValueAt(i, COL_NAME);
			if (StringUtils.isValidString(tmp)) {
				name = (String) tmp;
				if (StringUtils.isValidString(tableName)) {
					if (!name.startsWith(tableName + sep)) {
						name = tableName + sep + name;
					}
				}
				names.add(name);
			}
		}
		if (names.isEmpty())
			return null;
		String[] sNames = new String[names.size()];
		for (int i = 0; i < sNames.length; i++)
			sNames[i] = names.get(i);
		return sNames;
	}

	public int[] getSelectedIndexes() {
		acceptText();
		int count = getRowCount();
		if (count == 0)
			return null;
		IntArrayList list = new IntArrayList();
		for (int i = 0; i < count; i++) {
			if (isSelectedRow(i))
				list.addInt(i);
		}
		if (list.isEmpty())
			return null;
		return list.toIntArray();
	}

	public boolean isSelectedRow(int row) {
		Object s = data.getValueAt(row, COL_SELECT);
		return s != null && ((Boolean) s).booleanValue();
	}

	public void setRowSelected(int row, boolean selected) {
		data.setValueAt(new Boolean(selected), row, COL_SELECT);
		acceptText();
	}

	private void init() {
		setRowHeight(GC.TABLE_ROW_HEIGHT);
		getTableHeader().setReorderingAllowed(false);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JTableHeader header = getTableHeader();
		header.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int col = columnAtPoint(e.getPoint());
				if (col == VALUE_COL) {
					selectAll(!selectAll);
					dataChanged();
				}
			}
		});
		final int selectWidth = 75;
		getColumn(COL_SELECT).setHeaderRenderer(new DefaultTableCellRenderer() {

			private static final long serialVersionUID = 1L;

			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				JCheckBox cb = new JCheckBox(IdeMessage.get().getMessage("public.select")); // TristateCheckBox
				cb.setSelected(selectAll);
				JPanel p = new JPanel(new BorderLayout());
				p.add(cb, BorderLayout.CENTER);
				p.setFont(table.getFont());
				p.setBorder(new TableHeaderBorder());
				p.setPreferredSize(new Dimension(selectWidth, 20));
				cb.setEnabled(TableSelect.this.isEnabled());
				return p;
			}
		});
		setColumnFixedWidth(COL_SELECT, selectWidth);
		setColumnCheckBox(COL_SELECT);

		DragGestureListener dgl = new DragGestureListener() {
			public void dragGestureRecognized(DragGestureEvent dge) {
				try {
					String[] names = getSelectedNames();
					if (names == null) {
						return;
					}
					Transferable tf = new TransferableObject(names);
					if (tf != null) {
						dge.startDrag(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), tf);
					}
				} catch (Exception x) {
					GM.showException(x);
				}
			}
		};
		DragSource ds = DragSource.getDefaultDragSource();
		ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, dgl);
	}

	public void selectAll(boolean selected) {
		selectAll = selected;
		int count = getRowCount();
		for (int r = 0; r < count; r++) {
			data.setValueAt(new Boolean(selectAll), r, COL_SELECT);
		}
		getTableHeader().repaint();
		allRowsSelected(selectAll);
		this.repaint();
	}

	protected void dataChanged() {
	}
}