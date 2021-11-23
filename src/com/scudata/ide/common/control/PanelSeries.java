package com.scudata.ide.common.control;

import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.dm.Param;
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
	private JTableEx tableParam = new JTableEx(
			mm.getMessage("panelseries.tableparam")) { // 序号,值
		private static final long serialVersionUID = 1L;

		/**
		 * 双击弹出文本对话框编辑值
		 */
		public void doubleClicked(int xpos, int ypos, int row, int col,
				MouseEvent e) {
			GM.dialogEditTableText(tableParam, row, col);
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
				series.set(row + 1, aValue);
			} catch (Exception e) {
				GM.showException(e);
				return;
			}
		}
	};

	/**
	 * 常序列对象
	 */
	private Sequence series;

	/**
	 * 常量对象
	 */
	private Param param;
	/**
	 * 是否阻止变化
	 */
	private boolean preventChange = false;

	/**
	 * 构造函数
	 */
	public PanelSeries() {
		try {
			rqInit();
			initTable();
		} catch (Exception ex) {
			GM.showException(ex);
		}
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void rqInit() throws Exception {
		this.setLayout(new GridBagLayout());
		this.add(new JScrollPane(tableParam), GM.getGBC(0, 0, true, true));
	}

	/**
	 * 设置常量对象
	 * 
	 * @param param
	 */
	public void setParam(Param param) {
		this.param = param;
		if (param.getValue() == null) {
			tableParam.data.setRowCount(0);
			this.series = new Sequence();
		} else {
			this.series = (Sequence) param.getValue();
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
	public Param getParam() {
		param.setValue(series);
		return param;
	}

	/**
	 * 刷新
	 */
	private void refresh() {
		tableParam.removeAllRows();
		tableParam.data.setRowCount(0);
		int rowCount = series.length();
		if (rowCount < 1) {
			return;
		}
		for (int i = 1; i <= rowCount; i++) {
			int r = tableParam.addRow();
			Object value = series.get(i);
			tableParam.data.setValueAt(value, r, COL_VALUE);
		}
	}

	/**
	 * 全选
	 */
	public void selectAll() {
		tableParam.acceptText();
		tableParam.selectAll();
	}

	/**
	 * 行上移
	 */
	public void rowUp() {
		tableParam.acceptText();
		int row = tableParam.getSelectedRow();
		if (row < 0) {
			return;
		}
		tableParam.shiftRowUp(row);
	}

	/**
	 * 行下移
	 */
	public void rowDown() {
		tableParam.acceptText();
		int row = tableParam.getSelectedRow();
		if (row < 0) {
			return;
		}
		tableParam.shiftRowDown(row);
	}

	/**
	 * 增加行
	 */
	public void addRow() {
		tableParam.acceptText();
		series.add(null);
		refresh();
	}

	/**
	 * 插入行
	 */
	public void insertRow() {
		tableParam.acceptText();
		int row = tableParam.getSelectedRow();
		if (row < 0) {
			return;
		}
		series.insert(row + 1, null);
		refresh();
		tableParam.setRowSelectionInterval(row, row);
	}

	/**
	 * 检查数据
	 * 
	 * @return
	 */
	public boolean checkData() {
		tableParam.acceptText();
		return true;
	}

	/**
	 * 将数据复制到剪贴板
	 */
	public void clipBoard() {
		String blockData = tableParam.getBlockData();
		GM.clipBoard(blockData);
	}

	/**
	 * 删除选中的行
	 */
	public void deleteRows() {
		tableParam.acceptText();
		int rows[] = tableParam.getSelectedRows();
		if (rows.length == 0) {
			return;
		}
		for (int i = rows.length - 1; i >= 0; i--) {
			series.delete(rows[i] + 1);
		}
		refresh();
	}

	/**
	 * 初始化表控件
	 */
	private void initTable() {
		preventChange = true;
		tableParam.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tableParam.setRowHeight(20);
		tableParam.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableParam.getTableHeader().setReorderingAllowed(false);
		tableParam.setClickCountToStart(1);
		tableParam.setIndexCol(COL_INDEX);
		tableParam.setColumnWidth(COL_VALUE, 250);
		preventChange = false;
	}
}
