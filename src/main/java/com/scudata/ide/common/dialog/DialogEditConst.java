package com.scudata.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import java.util.Date;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.table.TableColumn;

import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.DateFactory;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.dm.Env;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.AllPurposeEditor;
import com.scudata.ide.common.swing.AllPurposeRenderer;
import com.scudata.ide.common.swing.DateChooser;
import com.scudata.ide.common.swing.DatetimeChooser;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.ide.common.swing.VFlowLayout;
import com.scudata.util.Variant;

/**
 * 常量编辑
 *
 */
public class DialogEditConst extends DialogMaxmizable {
	private static final long serialVersionUID = 1L;

	/**
	 * 确认按钮
	 */
	private JButton jBOK = new JButton();
	/**
	 * 取消按钮
	 */
	private JButton jBCancel = new JButton();

	/**
	 * 日期格式
	 */
	private java.text.SimpleDateFormat dateFormatter = new java.text.SimpleDateFormat(
			Env.getDateFormat());

	/**
	 * 事件格式
	 */
	private java.text.SimpleDateFormat timeFormatter = new java.text.SimpleDateFormat(
			Env.getDateTimeFormat());

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();
	/** 字符串 */
	private final String STR_STR = mm.getMessage("dialogeditconst.str");
	/** 整数 */
	private final String STR_INT = mm.getMessage("dialogeditconst.int");
	/** 浮点数 */
	private final String STR_DOUBLE = mm.getMessage("dialogeditconst.double");
	/** 日期 */
	private final String STR_DATE = mm.getMessage("dialogeditconst.date");
	/** 日期时间 */
	private final String STR_DATE_TIME = mm
			.getMessage("dialogeditconst.datetime");
	/** 序列 */
	private final String STR_SERIES = mm.getMessage("dialogeditconst.series");
	/** 序表 */
	private final String STR_TABLE = mm.getMessage("dialogeditconst.table");
	/** 表达式 */
	private final String STR_EXP = mm.getMessage("dialogeditconst.exp");

	/** 序号列 */
	private final byte COL_INDEX = 0;
	/** 名称列 */
	private final byte COL_NAME = 1;
	/** 类型列 */
	private final byte COL_KIND = 2;
	/** 值列 */
	private final byte COL_VALUE = 3;
	/** 配置对象列 */
	private final byte COL_CONFIG = 4;
	/** 配置对象列名 */
	private final String STR_PARAM = "COL_CONFIG";
	/**
	 * 常量表格控件。序号,名称,类型,值,COL_CONFIG
	 */
	private JTableEx tableConst = new JTableEx(
			mm.getMessage("dialogeditconst.tableconst") + STR_PARAM) {
		private static final long serialVersionUID = 1L;

		/**
		 * 设置值的事件
		 */
		public void setValueAt(Object aValue, int row, int column) {
			if (!isItemDataChanged(row, column, aValue)) {
				return;
			}
			super.setValueAt(aValue, row, column);
			switch (column) {
			case COL_KIND:
				data.setValueAt(null, row, COL_VALUE);
				data.setValueAt(null, row, COL_CONFIG);
				acceptText();
				break;
			}
		}

		/**
		 * 双击事件
		 */
		public void doubleClicked(int xpos, int ypos, int row, int col,
				MouseEvent e) {
			switch (col) {
			case COL_NAME:
				GM.dialogEditTableText(DialogEditConst.this, tableConst, row,
						col);
				break;
			case COL_VALUE:
				Object val = data.getValueAt(row, COL_VALUE);
				byte kind = ((Byte) data.getValueAt(row, COL_KIND)).byteValue();
				Param p = new Param();
				if (StringUtils.isValidString(tableConst.data.getValueAt(row,
						COL_NAME))) {
					p.setName(tableConst.data.getValueAt(row, COL_NAME) == null ? ""
							: (String) tableConst.data
									.getValueAt(row, COL_NAME));
				}
				p.setKind(getParamKind(kind));
				p.setValue(val);
				acceptText();
				switch (kind) {
				case GC.KIND_STR:
				case GC.KIND_INT:
				case GC.KIND_DOUBLE:
				case GC.KIND_EXP:
					String title = p.getName();
					if (StringUtils.isValidString(title)) {
						title += " : ";
					}
					if (kind == GC.KIND_STR) {
						title += STR_STR;
					} else if (kind == GC.KIND_INT) {
						title += STR_INT;
					} else if (kind == GC.KIND_DOUBLE) {
						title += STR_DOUBLE;
					} else {
						title += STR_EXP;
					}
					DialogInputText dit = new DialogInputText(
							DialogEditConst.this, true);
					dit.setText(val == null ? null : (String) val);
					dit.setTitle(title);
					dit.setVisible(true);
					if (dit.getOption() == JOptionPane.OK_OPTION) {
						data.setValueAt(dit.getText(), row, COL_VALUE);
						acceptText();
					}
					break;
				case GC.KIND_DATE:
					title = p.getName();
					if (StringUtils.isValidString(title)) {
						title += " : ";
					}
					title += STR_DATE;
					DateChooser dc = new DateChooser(DialogEditConst.this, true);
					dc.setTitle(title);
					java.util.Calendar selectedCalendar = java.util.Calendar
							.getInstance();
					try {
						if (StringUtils.isValidString(data.getValueAt(row,
								COL_VALUE))) {
							selectedCalendar.setTime(dateFormatter
									.parse((String) data.getValueAt(row,
											COL_VALUE)));
						}
						dc.initDate(selectedCalendar);
					} catch (Exception x) {
					}
					GM.centerWindow(dc);
					dc.setVisible(true);
					if (dc.getSelectedDate() != null) {
						selectedCalendar = dc.getSelectedDate();
					} else {
						selectedCalendar = null;
					}
					if (selectedCalendar == null) {
						dc.dispose();
						return;
					}
					long time = selectedCalendar.getTimeInMillis();
					java.util.Date date = new java.sql.Date(time);
					data.setValueAt(dateFormatter.format(date), row, COL_VALUE);
					dc.dispose();
					break;
				case GC.KIND_DATE_TIME:
					title = p.getName();
					if (StringUtils.isValidString(title)) {
						title += " : ";
					}
					title += STR_DATE_TIME;
					DatetimeChooser dtc = new DatetimeChooser(
							DialogEditConst.this, true);
					dtc.setTitle(title);
					selectedCalendar = java.util.Calendar.getInstance();
					try {
						if (StringUtils.isValidString(data.getValueAt(row,
								COL_VALUE))) {
							selectedCalendar.setTime(timeFormatter
									.parse((String) data.getValueAt(row,
											COL_VALUE)));
						}
						dtc.initDate(selectedCalendar);
					} catch (Exception x) {
					}
					GM.centerWindow(dtc);
					dtc.setVisible(true);
					if (dtc.getSelectedDatetime() != null) {
						selectedCalendar = dtc.getSelectedDatetime();
					} else {
						selectedCalendar = null;
					}
					if (selectedCalendar == null) {
						dtc.dispose();
						return;
					}
					time = selectedCalendar.getTimeInMillis();
					java.sql.Time datetime = new java.sql.Time(time);
					data.setValueAt(timeFormatter.format(datetime), row,
							COL_VALUE);
					dtc.dispose();
					break;
				case GC.KIND_SERIES:
					DialogEditSeries des = new DialogEditSeries(
							DialogEditConst.this);
					des.setSequence(val == null ? null : (Sequence) val);
					title = p.getName();
					if (StringUtils.isValidString(title)) {
						title += " : ";
					}
					title += STR_SERIES;
					des.setTitle(title);
					des.setVisible(true);
					if (des.getOption() == JOptionPane.OK_OPTION) {
						p.setValue(des.getSequence());
						tableConst.data
								.setValueAt(p.getValue(), row, COL_VALUE);
						acceptText();
					}
					break;
				case GC.KIND_TABLE:
					DialogEditTable det = new DialogEditTable(
							DialogEditConst.this, p);
					title = p.getName();
					if (StringUtils.isValidString(title)) {
						title += " : ";
					}
					title += STR_TABLE;
					det.setTitle(title);
					det.setVisible(true);
					if (det.getOption() == JOptionPane.OK_OPTION) {
						p = det.getParam();
						tableConst.data
								.setValueAt(p.getValue(), row, COL_VALUE);
						acceptText();
					}
					break;
				}
				break;
			}
		}
	};

	/**
	 * 增加按钮
	 */
	private JButton jBAdd = new JButton();

	/**
	 * 删除按钮
	 */
	private JButton jBDel = new JButton();

	/**
	 * 参数列表对象
	 */
	private ParamList pl;

	/**
	 * 退出选项
	 */
	private int m_option = JOptionPane.CANCEL_OPTION;

	/**
	 * 已经存在的名称
	 */
	private Vector<String> usedNames = new Vector<String>();

	/**
	 * 其他参数名称
	 */
	private Vector<String> otherNames;

	/**
	 * 构造函数
	 * 
	 * @param isGlobal
	 *            是否全局变量
	 */
	public DialogEditConst(boolean isGlobal) {
		super(GV.appFrame, "常量编辑", true);
		try {
			initUI();
			init(isGlobal);
			setSize(450, 300);
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
			resetText(isGlobal);
		} catch (Exception ex) {
			GM.showException(this, ex);
		}
	}

	/**
	 * 重设语言资源
	 * 
	 * @param isGlobal
	 */
	private void resetText(boolean isGlobal) {
		setTitle(isGlobal ? mm.getMessage("dialogeditconst.title") : mm
				.getMessage("dialogeditconst.title1")); // 常量编辑
		jBOK.setText(mm.getMessage("button.ok"));
		jBCancel.setText(mm.getMessage("button.cancel"));
		jBAdd.setText(mm.getMessage("button.add"));
		jBDel.setText(mm.getMessage("button.delete"));
	}

	/**
	 * 取退出选项
	 * 
	 * @return
	 */
	public int getOption() {
		return m_option;
	}

	/**
	 * 设置已经存在的名称
	 * 
	 * @param usedNames
	 */
	public void setUsedNames(Vector<String> usedNames) {
		this.otherNames = usedNames;
	}

	/**
	 * 设置参数列表对象
	 * 
	 * @param pl
	 */
	public void setParamList(ParamList pl) {
		if (pl == null) {
			return;
		}
		this.pl = pl;
		setParamList2Table(pl);
		ParamList otherList = new ParamList(); // Param.ARG 数据网
		if (pl != null) {
			pl.getAllVarParams(otherList);
			int count = otherList.count();
			for (int i = 0; i < count; i++) {
				usedNames.add(otherList.get(i).getName());
			}
		}
	}

	/**
	 * 设置参数列表到表格控件
	 * 
	 * @param pl
	 */
	private void setParamList2Table(ParamList pl) {
		ParamList constList = new ParamList();
		pl.getAllConsts(constList);
		int count = constList.count();
		Param p;
		byte kind;
		Object val;
		int row;
		for (int i = 0; i < count; i++) {
			p = constList.get(i);
			row = tableConst.addRow();
			tableConst.data.setValueAt(p.getName(), row, COL_NAME);
			kind = getKind(p);
			tableConst.data.setValueAt(new Byte(kind), row, COL_KIND);
			val = p.getValue();
			switch (kind) {
			case GC.KIND_INT:
			case GC.KIND_DOUBLE:
			case GC.KIND_DATE:
				val = Variant.toString(val);
				break;
			case GC.KIND_DATE_TIME:
				val = timeFormatter.format((Date) val);
				break;
			}
			tableConst.data.setValueAt(val, row, COL_VALUE);
		}
	}

	/**
	 * 取参数列表
	 * 
	 * @return
	 */
	public ParamList getParamList() {
		return getParamList(true);
	}

	/**
	 * 取参数列表
	 * 
	 * @param containTsx
	 * @return
	 */
	private ParamList getParamList(boolean containTsx) {
		ParamList otherList = new ParamList();
		if (pl == null) {
			pl = new ParamList();
		} else if (containTsx) {
			pl.getAllVarParams(otherList);
		}
		ParamList newList = new ParamList();
		int count = tableConst.getRowCount();
		Param p;
		byte kind;
		Object val;
		for (int i = 0; i < count; i++) {
			p = new Param();
			p.setName((String) tableConst.data.getValueAt(i, COL_NAME));
			kind = ((Byte) tableConst.data.getValueAt(i, COL_KIND)).byteValue();
			p.setKind(getParamKind(kind));
			val = tableConst.data.getValueAt(i, COL_VALUE);
			switch (kind) {
			case GC.KIND_INT:
				p.setValue(new Integer(Integer.parseInt((String) val)));
				break;
			case GC.KIND_DOUBLE:
				p.setValue(new Double(Double.parseDouble((String) val)));
				break;
			case GC.KIND_DATE:
				try {
					p.setValue(DateFactory.parseDate((String) val));
				} catch (ParseException ex) {
				}
				break;
			case GC.KIND_DATE_TIME:
				try {
					p.setValue(new java.sql.Time(timeFormatter.parse(
							(String) val).getTime()));
				} catch (ParseException ex) {
				}
				break;
			case GC.KIND_SERIES:
				if (val instanceof String) {
					val = PgmNormalCell.parseConstValue((String) val);
				}
				p.setValue(val);
				break;
			default:
				p.setValue(val);
				break;
			}
			newList.add(p);
		}
		count = otherList.count();
		for (int i = 0; i < count; i++) {
			newList.add(otherList.get(i));
		}
		return newList;
	}

	/**
	 * 取参数类型
	 * 
	 * @param kind
	 * @return
	 */
	private byte getParamKind(byte kind) {
		switch (kind) {
		case GC.KIND_STR:
		case GC.KIND_INT:
		case GC.KIND_DOUBLE:
		case GC.KIND_DATE:
		case GC.KIND_DATE_TIME:
		case GC.KIND_SERIES:
		case GC.KIND_TABLE:
			return Param.CONST;
		}
		return -1;
	}

	/**
	 * 取类型
	 * 
	 * @param p
	 * @return
	 */
	private byte getKind(Param p) {
		Object val = p.getValue();
		if (val instanceof Integer) {
			return GC.KIND_INT;
		} else if (val instanceof Double) {
			return GC.KIND_DOUBLE;
		} else if (val instanceof java.sql.Time) {
			return GC.KIND_DATE_TIME;
		} else if (val instanceof Date) {
			return GC.KIND_DATE;
		} else if (val instanceof Sequence) {
			Sequence s = (Sequence) val;
			if (s.isPmt()) {
				return GC.KIND_TABLE;
			}
			return GC.KIND_SERIES;
		} else if (val instanceof String) {
			if (p.getKind() == Param.CONST) {
				return GC.KIND_STR;
			}
		}
		return -1;
	}

	/**
	 * 初始化
	 * 
	 * @param isGlobal
	 *            是否全局变量
	 */
	private void init(boolean isGlobal) {
		tableConst.setIndexCol(COL_INDEX);
		tableConst.setRowHeight(20);
		Vector<Byte> code = new Vector<Byte>();
		code.add(new Byte(GC.KIND_STR));
		code.add(new Byte(GC.KIND_INT));
		code.add(new Byte(GC.KIND_DOUBLE));
		code.add(new Byte(GC.KIND_DATE));
		code.add(new Byte(GC.KIND_DATE_TIME));
		code.add(new Byte(GC.KIND_SERIES));
		code.add(new Byte(GC.KIND_TABLE));
		Vector<String> disp = new Vector<String>();
		disp.add(STR_STR);
		disp.add(STR_INT);
		disp.add(STR_DOUBLE);
		disp.add(STR_DATE);
		disp.add(STR_DATE_TIME);
		disp.add(STR_SERIES);
		disp.add(STR_TABLE);
		JComboBox combo = tableConst.setColumnDropDown(COL_KIND, code, disp);
		combo.setMaximumRowCount(10);
		tableConst.setColumnVisible(STR_PARAM, false);
		TableColumn tc = tableConst.getColumn(COL_VALUE);
		tc.setCellEditor(new AllPurposeEditor(new JTextField(), tableConst));
		tc.setCellRenderer(new AllPurposeRenderer());

		// tableConst.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableConst.getTableHeader().setReorderingAllowed(false);
		tableConst.setColumnWidth(COL_NAME, 120);
		tableConst.setColumnWidth(COL_KIND, 100);
		tableConst.setColumnWidth(COL_VALUE, 120);

	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		JPanel jPanel2 = new JPanel();
		VFlowLayout vFlowLayout1 = new VFlowLayout();
		jPanel2.setLayout(vFlowLayout1);
		jBOK.setMnemonic('O');
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new DialogEditConst_jBOK_actionAdapter(this));
		jBCancel.setMnemonic('C');
		jBCancel.setText("取消(C)");
		jBCancel.addActionListener(new DialogEditConst_jBCancel_actionAdapter(
				this));
		jBAdd.addActionListener(new DialogEditConst_jBAdd_actionAdapter(this));
		jBDel.addActionListener(new DialogEditConst_jBDel_actionAdapter(this));
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new DialogEditConst_this_windowAdapter(this));
		JScrollPane jScrollPane1 = new JScrollPane();
		jScrollPane1.getViewport().add(tableConst);
		JPanel jPanel3 = new JPanel();
		BorderLayout borderLayout1 = new BorderLayout();
		JButton jButton1 = new JButton();
		jButton1.setMaximumSize(new Dimension(450, 1));
		jButton1.setBorder(null);
		jPanel3.setLayout(borderLayout1);
		jBAdd.setMnemonic('A');
		jBAdd.setText("增加(A)");
		jBDel.setMnemonic('D');
		jBDel.setText("删除(D)");
		this.getContentPane().add(jPanel3, BorderLayout.CENTER);
		this.getContentPane().add(jPanel2, BorderLayout.EAST);
		jPanel2.add(jBOK, null);
		jPanel2.add(jBCancel, null);
		JPanel jPanel1 = new JPanel();
		jPanel2.add(jPanel1, null);
		jPanel2.add(jBAdd, null);
		jPanel2.add(jBDel, null);
		GridBagConstraints gbc = GM.getGBC(2, 1, true, true);
		gbc.gridwidth = 3;
		jPanel3.add(jScrollPane1, BorderLayout.CENTER);
		jPanel3.add(jButton1, BorderLayout.NORTH);
	}

	/**
	 * 窗口关闭事件
	 * 
	 * @param e
	 */
	void this_windowClosing(WindowEvent e) {
		GM.setWindowDimension(this);
		dispose();
	}

	/**
	 * 检查数据
	 * 
	 * @return
	 */
	private boolean checkData() {
		tableConst.acceptText();
		if (!tableConst
				.verifyColumnData(COL_NAME, mm.getMessage("public.name"))) { // 名称
			return false;
		}
		int count = tableConst.getRowCount();
		byte kind;
		String name;
		Object val;
		for (int i = 0; i < count; i++) {
			name = (String) tableConst.data.getValueAt(i, COL_NAME);
			if (usedNames.contains(name)) {
				GM.messageDialog(this, mm.getMessage(
						"dialogeditconst.existname", i + 1 + "", name)); // 第{0}行参数名：{1}已经存在。
				return false;
			}
			kind = ((Byte) tableConst.data.getValueAt(i, COL_KIND)).byteValue();
			val = tableConst.data.getValueAt(i, COL_VALUE);
			if (val == null) {
				GM.messageDialog(this,
						mm.getMessage("dialogeditconst.emptyval", i + 1 + "")); // 第{0}行参数值为空。
				return false;
			}
			String strKind = "";
			String message = mm.getMessage("dialogeditconst.notvalid", i + 1
					+ ""); // 第{0}行参数值类型应该为：
			try {
				switch (kind) {
				case GC.KIND_INT:
					strKind = STR_INT;
					Integer.parseInt((String) val);
					break;
				case GC.KIND_DOUBLE:
					strKind = STR_DOUBLE;
					Double.parseDouble((String) val);
					break;
				case GC.KIND_DATE:
					strKind = STR_DATE;
					DateFactory.parseDate((String) val);
					break;
				case GC.KIND_SERIES:
					strKind = STR_SERIES;
					if (StringUtils.isValidString(val)) {
						val = PgmNormalCell.parseConstValue((String) val);
					}
					if (!(val instanceof Sequence)) {
						GM.messageDialog(this, message + STR_SERIES); // 第{0}行参数值类型应该为：序列
						return false;
					}
					break;
				case GC.KIND_TABLE:
					if (!(val instanceof Table)) {
						GM.messageDialog(this, message + STR_TABLE); // 第{0}行参数值类型应该为：序表
						return false;
					}
					break;
				}
			} catch (Exception e) {
				GM.messageDialog(this, message + strKind);
				return false;
			}
		}
		return true;
	}

	/**
	 * 确认按钮事件
	 * 
	 * @param ae
	 */
	void jBOK_actionPerformed(ActionEvent ae) {
		if (!checkData()) {
			return;
		}
		m_option = JOptionPane.OK_OPTION;
		GM.setWindowDimension(this);
		dispose();
	}

	/**
	 * 取消按钮事件
	 * 
	 * @param e
	 */
	void jBCancel_actionPerformed(ActionEvent e) {
		GM.setWindowDimension(this);
		dispose();
	}

	/**
	 * 增加按钮事件
	 * 
	 * @param e
	 */
	void jBAdd_actionPerformed(ActionEvent e) {
		int row = tableConst.addRow();
		tableConst.acceptText();
		Vector<String> names = new Vector<String>();
		for (int i = 0; i < tableConst.getRowCount(); i++) {
			if (tableConst.data.getValueAt(i, COL_NAME) != null) {
				names.add((String) tableConst.data.getValueAt(i, COL_NAME));
			}
		}
		names.addAll(otherNames);
		int index = 1;
		while (names.contains(GC.PRE_PARAM + index)) {
			index++;
		}
		tableConst.data.setValueAt(GC.PRE_PARAM + index, row, COL_NAME);
		tableConst.data.setValueAt(new Byte(GC.KIND_STR), row, COL_KIND);
	}

	/**
	 * 删除按钮事件
	 * 
	 * @param e
	 */
	void jBDel_actionPerformed(ActionEvent e) {
		tableConst.deleteSelectedRows();
	}

}

class DialogEditConst_this_windowAdapter extends java.awt.event.WindowAdapter {
	DialogEditConst adaptee;

	DialogEditConst_this_windowAdapter(DialogEditConst adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}

class DialogEditConst_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogEditConst adaptee;

	DialogEditConst_jBOK_actionAdapter(DialogEditConst adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogEditConst_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogEditConst adaptee;

	DialogEditConst_jBCancel_actionAdapter(DialogEditConst adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogEditConst_jBAdd_actionAdapter implements
		java.awt.event.ActionListener {
	DialogEditConst adaptee;

	DialogEditConst_jBAdd_actionAdapter(DialogEditConst adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBAdd_actionPerformed(e);
	}
}

class DialogEditConst_jBDel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogEditConst adaptee;

	DialogEditConst_jBDel_actionAdapter(DialogEditConst adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBDel_actionPerformed(e);
	}
}
