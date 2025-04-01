package com.scudata.ide.common.control;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.dm.Sequence;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.JTableEx;

/**
 * 常序列编辑面板
 *
 */
public class PanelSeries extends JPanel {
	private static final long serialVersionUID = 1L;

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/**
	 * 序号列
	 */
	private final int COL_INDEX = 0;
	/**
	 * 值列
	 */
	private final int COL_VALUE = 1;
	/**
	 * 常序列值的表对象
	 */
	private JTableEx tableSeq = new JTableEx(
			mm.getMessage("panelseries.tableparam")) { // 序号,值
		private static final long serialVersionUID = 1L;

		/**
		 * 双击弹出文本对话框编辑值
		 */
		public void doubleClicked(int xpos, int ypos, int row, int col,
				MouseEvent e) {
			GM.dialogEditTableText(parent, tableSeq, row, col);
		}

		/**
		 * 值提交时转换成相应的对象
		 */
		public void setValueAt(Object aValue, int row, int column) {
			if (!isItemDataChanged(row, column, aValue)) {
				return;
			}
			super.setValueAt(aValue, row, column);
			if (preventChange) {
				return;
			}
			try {
				if (StringUtils.isValidString(aValue)) {
					aValue = PgmNormalCell.parseConstValue((String) aValue);
				}
				seq.set(row + 1, aValue);
			} catch (Exception e) {
				GM.showException(parent, e);
				return;
			}
		}
	};

	/**
	 * 常序列对象
	 */
	private Sequence seq;

	/**
	 * 是否阻止变化
	 */
	private boolean preventChange = false;

	/**
	 * 夫组件
	 */
	private Component parent;

	/**
	 * 构造函数
	 */
	public PanelSeries(Component parent) {
		try {
			this.parent = parent;
			rqInit();
			initTable();
		} catch (Exception ex) {
			GM.showException(parent, ex);
		}
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void rqInit() throws Exception {
		this.setLayout(new BorderLayout());
		this.add(new JScrollPane(tableSeq), BorderLayout.CENTER);
	}

	/**
	 * 设置常量对象
	 * 
	 * @param param
	 */
	public void setSequence(Sequence seq) {
		if (seq == null) {
			tableSeq.data.setRowCount(0);
			this.seq = new Sequence();
		} else {
			this.seq = seq;
			preventChange = true;
			refresh();
			preventChange = false;
		}
	}

	/**
	 * 取常量对象
	 * 
	 * @return
	 */
	public Sequence getSequence() {
		return seq;
	}

	/**
	 * 刷新
	 */
	private void refresh() {
		tableSeq.removeAllRows();
		tableSeq.data.setRowCount(0);
		int rowCount = seq.length();
		if (rowCount < 1) {
			return;
		}
		for (int i = 1; i <= rowCount; i++) {
			int r = tableSeq.addRow();
			Object value = seq.get(i);
			tableSeq.data.setValueAt(value, r, COL_VALUE);
		}
	}

	/**
	 * 全选
	 */
	public void selectAll() {
		tableSeq.acceptText();
		tableSeq.selectAll();
	}

	/**
	 * 行上移
	 */
	public void rowUp() {
		tableSeq.acceptText();
		int row = tableSeq.getSelectedRow();
		if (row < 0) {
			return;
		}
		tableSeq.shiftRowUp(row);
	}

	/**
	 * 行下移
	 */
	public void rowDown() {
		tableSeq.acceptText();
		int row = tableSeq.getSelectedRow();
		if (row < 0) {
			return;
		}
		tableSeq.shiftRowDown(row);
	}

	/**
	 * 增加行
	 */
	public void addRow() {
		tableSeq.acceptText();
		seq.add(null);
		refresh();
	}

	/**
	 * 插入行
	 */
	public void insertRow() {
		tableSeq.acceptText();
		int row = tableSeq.getSelectedRow();
		if (row < 0) {
			return;
		}
		seq.insert(row + 1, null);
		refresh();
		tableSeq.setRowSelectionInterval(row, row);
	}

	/**
	 * 检查数据
	 * 
	 * @return
	 */
	public boolean checkData() {
		tableSeq.acceptText();
		return true;
	}

	/**
	 * 将数据复制到剪贴板
	 */
	public void clipBoard() {
		String blockData = tableSeq.getBlockData();
		GM.clipBoard(blockData);
	}

	/**
	 * 删除选中的行
	 */
	public void deleteRows() {
		tableSeq.acceptText();
		int rows[] = tableSeq.getSelectedRows();
		if (rows.length == 0) {
			return;
		}
		for (int i = rows.length - 1; i >= 0; i--) {
			seq.delete(rows[i] + 1);
		}
		refresh();
	}

	/**
	 * 初始化表控件
	 */
	private void initTable() {
		preventChange = true;
		tableSeq.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tableSeq.setRowHeight(20);
		tableSeq.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableSeq.getTableHeader().setReorderingAllowed(false);
		tableSeq.setClickCountToStart(1);
		tableSeq.setIndexCol(COL_INDEX);
		tableSeq.setColumnWidth(COL_VALUE, 250);
		preventChange = false;
	}
}
