package com.scudata.ide.common.dialog;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.scudata.app.common.Section;
import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.DBInfo;
import com.scudata.common.Escape;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.DataSource;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.JComboBoxEx;
import com.scudata.ide.common.swing.JListEx;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.ide.common.swing.VFlowLayout;

/**
 * SQL编辑器
 *
 */
public class DialogSQLEditor extends JDialog {
	private static final long serialVersionUID = 1L;

	/**
	 * SELECT 成绩.学生ID, 成绩.语文, sum(成绩.数学), 成绩.英语,学生表.籍贯 FROM 成绩 // LEFT OUTER
	 * JOIN 学生表 ON 成绩.学生ID = 学生表.id WHERE (成绩.英语>60) GROUP BY 成绩.学生ID HAVING
	 * sum(成绩.数学)>300 ORDER BY 成绩.姓名
	 * 
	 * where是先按照该条件检索数据， having是检索完成并且分组计算后，把结果集过滤
	 */

	/** FROM页 */
	public static final int TAB_FROM = 0;
	/** SELECT页 */
	public static final int TAB_SELECT = 1;
	/** WHERE页 */
	public static final int TAB_WHERE = 2;
	/** JOIN页 */
	public static final int TAB_JOIN = 3;
	/** GROUP BY页 */
	public static final int TAB_GROUP = 4;
	/** HAVING页 */
	public static final int TAB_HAVING = 5;
	/** ORDER BY页 */
	public static final int TAB_ORDER = 6;
	/** SQL页 */
	public static final int TAB_SQL = 7;

	/**
	 * 序号列，字段列，选项列，值列，连接列
	 */
	private static final int COL_ID = 0, COL_FIELD = 1, COL_OPT = 2,
			COL_VAL = 3, COL_CONNECT = 4;

	/**
	 * 表发生变化
	 */
	private TableChanged tableChanged = new TableChanged(this);

	/**
	 * 表名列表
	 */
	private Vector codeTable = new Vector();

	/**
	 * 表名显示值
	 */
	private DefaultComboBoxModel dispTable = new DefaultComboBoxModel();

	/**
	 * 字段名列表
	 */
	private Vector codeFields = new Vector();
	/**
	 * 字段名显示值列表
	 */
	private DefaultListModel dispFields = new DefaultListModel();

	/**
	 * 操作符号列表
	 */
	private Vector<String> operateCodeItems = new Vector<String>(),
			operateDispItems = new Vector<String>();

	/**
	 * 连接符号列表
	 */
	private Vector<String> connectCodeItems = new Vector<String>(),
			connectDispItems = new Vector<String>();

	/**
	 * SQL TAB面板
	 */
	private JTabbedPane jTabbedPaneSql = new JTabbedPane();

	/**
	 * SQL编辑框
	 */
	private JTextPane jTextPaneSql = new JTextPane();

	/**
	 * 增加表按钮
	 */
	private JButton fromButtonRight = new JButton();
	/**
	 * 删除表按钮
	 */
	private JButton fromButtonLeft = new JButton();

	/**
	 * 确认按钮
	 */
	private JButton jBOK = new JButton();
	/**
	 * 取消按钮
	 */
	private JButton jBCancel = new JButton();

	/**
	 * 增加字段按钮
	 */
	private JButton selectButtonRight = new JButton();
	/**
	 * 删除字段按钮
	 */
	private JButton selectButtonLeft = new JButton();

	/**
	 * 增加分组按钮
	 */
	private JButton groupButtonRight = new JButton();
	/**
	 * 删除分组按钮
	 */
	private JButton groupButtonLeft = new JButton();

	/**
	 * 增加排序按钮
	 */
	private JButton orderButtonRight = new JButton();
	/**
	 * 删除排序按钮
	 */
	private JButton orderButtonLeft = new JButton();

	/**
	 * 聚合方式控件
	 */
	private JComboBoxEx selectComboSum = new JComboBoxEx();
	/**
	 * FROM面板
	 */
	private JPanel fromPanel = new JPanel();

	/**
	 * 选出下拉控件
	 */
	private JComboBoxEx selectComboLeft = new JComboBoxEx();
	/**
	 * 分组下拉控件
	 */
	private JComboBoxEx groupComboLeft = new JComboBoxEx();
	/**
	 * 排序下拉控件
	 */
	private JComboBoxEx orderComboLeft = new JComboBoxEx();

	/**
	 * 退出选项
	 */
	protected int m_option = JOptionPane.CLOSED_OPTION;

	/**
	 * 增加条件
	 */
	private JButton whereButtonAdd = new JButton();

	/**
	 * 删除条件
	 */
	private JButton whereButtonDel = new JButton();

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/**
	 * 序号,字段,运算符,值,连接符
	 */
	private final String TABLE_COLS = mm
			.getMessage("dialogsqleditor.tablecols");

	/**
	 * 条件表格控件
	 */
	private JTableEx whereTable = new JTableEx(TABLE_COLS);

	/**
	 * 增加连接
	 */
	private JButton joinButtonAdd = new JButton();

	/**
	 * 删除连接
	 */
	private JButton joinButtonDel = new JButton();

	/**
	 * 连接表格控件
	 */
	private JTableEx joinTable = new JTableEx(TABLE_COLS);

	/**
	 * 增加HAVING条件
	 */
	private JButton havingButtonAdd = new JButton();
	/**
	 * 删除HAVING条件
	 */
	private JButton havingButtonDel = new JButton();

	/**
	 * HAVING条件控件
	 */
	private JTableEx havingTable = new JTableEx(TABLE_COLS);

	/**
	 * 模式名控件
	 */
	private JComboBox<String> jCBSchema = new JComboBox<String>();

	/**
	 * 当前的表
	 */
	private String currentFrom = null;

	/**
	 * 手动修改
	 */
	private boolean bEditByHand = false;

	/**
	 * 是否初始化后
	 */
	private boolean afterInit = false;

	/**
	 * 数据源对象
	 */
	private DataSource ds;

	/**
	 * 待选FROM列表
	 */
	private JListEx fromListLeft = new JListEx();
	/**
	 * 选出FROM列表
	 */
	private JListEx fromListRight = new JListEx();
	/**
	 * 待选SELECT列表
	 */
	private JListEx selectListLeft = new JListEx();
	/**
	 * 选出SELECT列表
	 */
	private JListEx selectListRight = new JListEx();
	/**
	 * 待选分组列表
	 */
	private JListEx groupListLeft = new JListEx();
	/**
	 * 选出分组列表
	 */
	private JListEx groupListRight = new JListEx();
	/**
	 * 待选排序列表
	 */
	private JListEx orderListLeft = new JListEx();
	/**
	 * 选出排序列表
	 */
	private JListEx orderListRight = new JListEx();

	/**
	 * 待选的表名
	 */
	private final String AVAILABLE_TABLE = mm
			.getMessage("dialogsqleditor.availabletable");

	/**
	 * 选出表名
	 */
	private final String SELECTED_TABLE = mm
			.getMessage("dialogsqleditor.selectedtable");

	/**
	 * 选出字段名
	 */
	private final String SELECTED_FIELD = mm
			.getMessage("dialogsqleditor.selectedfield");

	/**
	 * 无
	 */
	private final String OPT_NUL = "";
	/**
	 * 求和
	 */
	private final String OPT_SUM = "SUM";
	/**
	 * 最大值
	 */
	private final String OPT_MAX = "MAX";

	/**
	 * 最小值
	 */
	private final String OPT_MIN = "MIN";
	/**
	 * 计数
	 */
	private final String OPT_COUNT = "COUNT";
	/**
	 * 平均值
	 */
	private final String OPT_AVG = "AVG";
	/**
	 * 等号
	 */
	private final String OPT_EQUAL = "=";
	/**
	 * 不等号
	 */
	private final String OPT_NOT_EQUAL = "<>";
	/**
	 * 大于号
	 */
	private final String OPT_MORE = ">";
	/**
	 * 小于号
	 */
	private final String OPT_LESS = "<";
	/**
	 * 并且
	 */
	private final String OPT_AND = "and";
	/**
	 * 或者
	 */
	private final String OPT_OR = "or";

	/**
	 * 是否复制模式
	 */
	private boolean isCopyMode = false;

	/**
	 * 进度窗口
	 */
	public ProcessWindow pw;

	/**
	 * 构造函数
	 * 
	 * @param ds
	 *            数据源
	 */
	public DialogSQLEditor(DataSource ds) {
		super(GV.appFrame);
		this.ds = ds;
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			selectComboLeft.x_setModel(codeTable, dispTable);
			groupComboLeft.x_setModel(codeTable, dispTable);
			orderComboLeft.x_setModel(codeTable, dispTable);

			selectListLeft.x_setModel(codeFields, dispFields);
			groupListLeft.x_setModel(codeFields, dispFields);
			orderListLeft.x_setModel(codeFields, dispFields);

			fromListLeft.x_setData(new Vector<Object>(), new Vector<String>());
			initUI();
			this.setSize(600, 420);
			GM.loadSchemas(ds, jCBSchema);
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
			resetLangText();
			try {
				pw = new ProcessWindow();
				GM.centerWindow(pw);
				pw.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
			this.addWindowListener(new WindowAdapter() {

				public void windowOpened(WindowEvent e) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							try {
								refreshTables();
							} catch (Throwable x) {
								GM.showException(x);
							} finally {
								if (pw != null) {
									try {
										pw.disposeWindow();
									} catch (Exception e) {
									}
									pw = null;
								}
							}
						}
					});
				}
			});
		} catch (Exception e) {
			GM.showException(e);
		} finally {
			afterInit = true;
		}
	}

	/**
	 * 设置复制模式
	 */
	public void setCopyMode() {
		isCopyMode = true;
		jBOK.setText(mm.getMessage("button.copy"));
		jBCancel.setText(mm.getMessage("button.close"));

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
	 * 更新选中的表或视图的列信息
	 * 
	 * @param table
	 *            表或视图名
	 * @throws Exception
	 */
	private void changeSelectTable(String table) throws Exception {
		if (null == table) {
			return;
		}
		dispFields.removeAllElements();
		updateTableFields(table);
	}

	/**
	 * 刷新表名
	 */
	private void refreshTables() {
		String realSchema = GM.getRealSchema(jCBSchema.getSelectedItem());
		Vector<String> v = GM.listSchemaTables(ds, realSchema, false);
		if (v == null || v.isEmpty()) {
			fromListLeft.x_setData(new Vector<Object>(), new Vector<String>());
		} else {
			Object[] tableNames = v.toArray();
			Arrays.sort(tableNames);
			List tableList = Arrays.asList(tableNames);
			fromListLeft.x_setData(tableList, tableList);
		}
	}

	/**
	 * 取SQL
	 * 
	 * @return
	 */
	public String getSQL() {
		return jTextPaneSql.getText();
	}

	/**
	 * 进度窗口
	 *
	 */
	class ProcessWindow extends JWindow {
		private static final long serialVersionUID = 1L;
		/**
		 * 进度跳
		 */
		private JProgressBar bar;

		/**
		 * 构造函数
		 */
		ProcessWindow() {
			super(DialogSQLEditor.this);
			this.setSize(350, 60);
			bar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
			bar.setStringPainted(false);
			bar.setValue(0);
			bar.setMinimumSize(new Dimension(1, 20));
			JPanel panelMain = new JPanel(new BorderLayout());
			panelMain.setBorder(BorderFactory.createLineBorder(Color.black));
			JPanel panelCenter = new JPanel(new GridBagLayout());
			// 正在加载表结构，请稍候。
			panelCenter.add(
					new JLabel(IdeCommonMessage.get().getMessage(
							"dialogsqleditor.loadds")), GM.getGBC(0, 0, true));
			panelMain.add(panelCenter, BorderLayout.CENTER);
			panelMain.add(bar, BorderLayout.NORTH);
			getContentPane().add(panelMain);
			bar.setIndeterminate(true);
		}

		/**
		 * 关闭窗口
		 */
		public void disposeWindow() {
			bar.setIndeterminate(false);
			bar.setValue(100);
			super.dispose();
		}
	}

	/**
	 * 取垂直摆放的面板
	 * 
	 * @param bt1
	 * @param bt2
	 * @param bt3
	 * @return
	 */
	private JPanel getVPanel(Component bt1, Component bt2, Component bt3) {
		VFlowLayout vf = new VFlowLayout();
		vf.setHorizontalFill(true);
		vf.setAlignment(VFlowLayout.CENTER);
		JPanel p = new JPanel(vf);
		p.add(bt1);
		if (bt2 != null) {
			p.add(bt2);
			if (bt3 != null) {
				p.add(bt3);
			}
		}
		return p;
	}

	/**
	 * 重设语言资源
	 */
	private void resetLangText() {
		jBOK.setText(mm.getMessage("button.ok")); // 确定(O)
		jBCancel.setText(mm.getMessage("button.cancel")); // 取消(C)
	}

	/**
	 * 取FROM面板
	 * 
	 * @return
	 */
	private JPanel getFromPanel() {
		JLabel fromLabelTable = new JLabel();
		JLabel fromLabelSelects = new JLabel();
		fromLabelTable.setText(AVAILABLE_TABLE);
		fromLabelSelects.setText(SELECTED_TABLE);
		fromButtonRight.setText(">");
		fromButtonRight
				.addActionListener(new DialogSQLEditor_fromButtonRight_actionAdapter(
						this));
		fromButtonLeft.setText("<");
		fromButtonLeft
				.addActionListener(new DialogSQLEditor_fromButtonLeft_actionAdapter(
						this));

		fromListLeft.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					jButtonFR_actionPerformed(null);
				}
			}
		});

		fromListRight.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					jButtonFL_actionPerformed(null);
				}
			}
		});

		fromPanel.setLayout(new GridBagLayout());
		fromPanel.add(fromLabelTable, GM.getGBC(1, 1));
		fromPanel.add(fromLabelSelects, GM.getGBC(1, 3));
		JScrollPane fromScrollPaneRight = new JScrollPane();
		fromScrollPaneRight.getViewport().add(fromListRight, null);
		JScrollPane fromScrollPaneLeft = new JScrollPane();
		fromScrollPaneLeft.getViewport().add(fromListLeft, null);

		fromPanel.add(fromScrollPaneLeft, GM.getGBC(2, 1, true, true));
		fromPanel.add(getVPanel(fromButtonRight, fromButtonLeft, null),
				GM.getGBC(2, 2, false, true));
		fromPanel.add(fromScrollPaneRight, GM.getGBC(2, 3, true, true));

		return fromPanel;
	}

	/**
	 * 取SELECT面板
	 * 
	 * @return
	 */
	private JPanel getSelectPanel() {
		JPanel selectPanel = new JPanel();
		selectPanel.setLayout(new GridBagLayout());
		selectComboLeft.addItemListener(tableChanged);
		selectButtonRight.setText(">");
		selectButtonRight
				.addActionListener(new DialogSQLEditor_jButtonSR_actionAdapter(
						this));
		selectButtonLeft.setText("<");
		selectButtonLeft
				.addActionListener(new DialogSQLEditor_jButtonSL_actionAdapter(
						this));
		JLabel selectLabelRight = new JLabel();
		selectLabelRight.setText(SELECTED_FIELD);

		selectListLeft.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					jButtonSR_actionPerformed(null);
				}
			}
		});
		selectListRight.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					jButtonSL_actionPerformed(null);
				}
			}
		});
		selectComboSum.setEnabled(false);
		selectListRight.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				selectComboSum.setEnabled(!selectListRight.isSelectionEmpty());
				String exp = (String) selectListRight.getSelectedValue();
				if (exp == null) {
					return;
				}
				String[] exps = parseComputedField(exp);
				selectComboSum.x_setSelectedCodeItem(StringUtils
						.isValidString(exps[0]) ? exps[0] : OPT_NUL);
			}
		});
		Vector<String> codeOpts = new Vector<String>(), dispOpts = new Vector<String>();
		codeOpts.add("");
		codeOpts.add(OPT_SUM);
		codeOpts.add(OPT_MAX);
		codeOpts.add(OPT_MIN);
		codeOpts.add(OPT_COUNT);
		codeOpts.add(OPT_AVG);

		dispOpts.add("");
		dispOpts.add(mm.getMessage("dialogsqleditor.sum")); // SUM
		dispOpts.add(mm.getMessage("dialogsqleditor.max"));// MAX
		dispOpts.add(mm.getMessage("dialogsqleditor.min"));// MIN
		dispOpts.add(mm.getMessage("dialogsqleditor.count"));// COUNT
		dispOpts.add(mm.getMessage("dialogsqleditor.avg"));// AVG
		selectComboSum.x_setData(codeOpts, dispOpts);
		selectComboSum.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (selectListRight.isSelectionEmpty()) {
					return;
				}
				String selectedFunc = (String) selectComboSum
						.x_getSelectedItem();
				String exp = (String) selectListRight.getSelectedValue();
				if (exp == null) {
					return;
				}
				String[] exps = parseComputedField(exp);
				if (StringUtils.isValidString(selectedFunc)) {
					exp = selectedFunc + "(" + exps[1] + ")";
				} else {
					exp = exps[1];
				}
				int index = selectListRight.getSelectedIndex();
				selectListRight.data.set(index, exp);
				selectedFieldChanged();
			}
		});

		selectPanel.add(selectComboLeft, GM.getGBC(1, 1));
		selectPanel.add(selectLabelRight, GM.getGBC(1, 3));
		JScrollPane selectScrollPaneRight = new JScrollPane();
		selectScrollPaneRight.getViewport().add(selectListRight, null);
		JScrollPane selectScrollPaneLeft = new JScrollPane();
		selectScrollPaneLeft.getViewport().add(selectListLeft, null);
		selectPanel.add(selectScrollPaneLeft, GM.getGBC(2, 1, true, true));
		selectPanel.add(
				getVPanel(selectButtonRight, selectButtonLeft, selectComboSum),
				GM.getGBC(2, 2, false, true, 0));
		JPanel selectPanelRight = new JPanel(new BorderLayout());
		selectPanelRight.add(selectScrollPaneRight, BorderLayout.CENTER);
		selectPanelRight.add(selectComboSum, BorderLayout.SOUTH);
		selectPanel.add(selectPanelRight, GM.getGBC(2, 3, true, true));
		return selectPanel;
	}

	/**
	 * 解析计算列
	 * 
	 * @param exp
	 * @return
	 */
	private String[] parseComputedField(String exp) {
		if (exp == null) {
			return null;
		}
		exp = exp.trim();
		int indexL = exp.indexOf("("), indexR = exp.indexOf(")");
		String[] exps = new String[] { null, exp };
		if (indexL > -1 && indexR == exp.length() - 1) {
			String func = exp.substring(0, indexL);
			func = func.trim();
			if (func.equalsIgnoreCase(OPT_SUM)
					|| func.equalsIgnoreCase(OPT_MAX)
					|| func.equalsIgnoreCase(OPT_MIN)
					|| func.equalsIgnoreCase(OPT_COUNT)
					|| func.equalsIgnoreCase(OPT_AVG)) {
				exps[0] = func;
				exps[1] = exp.substring(indexL + 1, indexR);
			}
		}
		return exps;
	}

	/**
	 * 取WHERE面板
	 * 
	 * @return
	 */
	private JPanel getWherePanel() {
		whereButtonAdd.setToolTipText(mm.getMessage("dialogsqleditor.add")); // Add
		whereButtonAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				whereTable.addRow();
			}
		});
		whereButtonAdd.setIcon(GM.getImageIcon(GC.IMAGES_PATH
				+ "m_addrecord.gif"));
		initIconButton(whereButtonAdd);
		whereButtonDel.setToolTipText(mm.getMessage("dialogsqleditor.delete")); // Delete
		whereButtonDel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				whereTable.deleteSelectedRow();
			}
		});
		whereButtonDel.setIcon(GM.getImageIcon(GC.IMAGES_PATH
				+ "m_deleterecord.gif"));
		initIconButton(whereButtonDel);
		whereTable.setRowHeight(20);
		whereTable.setIndexCol(COL_ID);
		whereTable.setColumnDropDown(COL_OPT, operateCodeItems,
				operateCodeItems, true);
		whereTable.setColumnDropDown(COL_CONNECT, connectCodeItems,
				connectCodeItems, true);

		JScrollPane whereScrollPane = new JScrollPane(whereTable);
		JPanel wherePanel = new JPanel();
		wherePanel.setLayout(new GridBagLayout());
		wherePanel.add(new JLabel(), GM.getGBC(1, 1, true));
		wherePanel.add(whereButtonAdd, GM.getGBC(1, 2));
		wherePanel.add(whereButtonDel, GM.getGBC(1, 3));
		GridBagConstraints gbc = GM.getGBC(2, 1, true, true);
		gbc.gridwidth = 3;
		wherePanel.add(whereScrollPane, gbc);
		return wherePanel;
	}

	/**
	 * 取JOIN面板
	 * 
	 * @return
	 */
	private JPanel getJoinPanel() {
		joinButtonAdd.setToolTipText(mm.getMessage("dialogsqleditor.add")); // Add
		joinButtonAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				joinTable.addRow();
			}
		});
		joinButtonAdd.setIcon(GM.getImageIcon(GC.IMAGES_PATH
				+ "m_addrecord.gif"));
		initIconButton(joinButtonAdd);
		joinButtonDel.setToolTipText(mm.getMessage("dialogsqleditor.delete")); // Delete
		joinButtonDel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				joinTable.deleteSelectedRow();
			}
		});
		joinButtonDel.setIcon(GM.getImageIcon(GC.IMAGES_PATH
				+ "m_deleterecord.gif"));
		initIconButton(joinButtonDel);
		joinTable.setRowHeight(20);
		joinTable.setIndexCol(COL_ID);
		joinTable.setColumnDropDown(COL_OPT, operateCodeItems,
				operateCodeItems, true);
		joinTable.setColumnDropDown(COL_CONNECT, connectCodeItems,
				connectCodeItems, true);
		JPanel joinPanel = new JPanel();
		JScrollPane joinScrollPane = new JScrollPane(joinTable);
		joinPanel.setLayout(new GridBagLayout());
		joinPanel.add(new JLabel(), GM.getGBC(1, 1, true));
		joinPanel.add(joinButtonAdd, GM.getGBC(1, 2));
		joinPanel.add(joinButtonDel, GM.getGBC(1, 3));
		GridBagConstraints gbc = GM.getGBC(2, 1, true, true);
		gbc.gridwidth = 3;
		joinPanel.add(joinScrollPane, gbc);
		return joinPanel;
	}

	/**
	 * 初始化图标按钮
	 * 
	 * @param b
	 */
	private void initIconButton(JButton b) {
		Dimension d = new Dimension(22, 22);
		b.setMaximumSize(d);
		b.setMinimumSize(d);
		b.setPreferredSize(d);
	}

	/**
	 * 取分组面板
	 * 
	 * @return
	 */
	private JPanel getGroupPanel() {
		JPanel groupPanel = new JPanel();
		groupPanel.setLayout(new GridBagLayout());
		groupComboLeft.addItemListener(tableChanged);
		groupButtonRight.setText(">");
		groupButtonRight
				.addActionListener(new DialogSQLEditor_groupR_actionAdapter(
						this));
		groupButtonLeft.setText("<");
		groupButtonLeft
				.addActionListener(new DialogSQLEditor_groupL_actionAdapter(
						this));
		JLabel groupLabelRight = new JLabel();
		groupLabelRight.setText(SELECTED_FIELD);

		groupListLeft.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					groupR_actionPerformed(null);
				}
			}
		});
		groupListRight.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					groupL_actionPerformed(null);
				}
			}
		});

		JButton bAuto = new JButton();
		bAuto.setToolTipText(mm.getMessage("dialogsqleditor.autogen")); // Automatic
		// generation
		bAuto.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "m_loaddb.gif"));
		bAuto.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Section s = new Section(selectListRight.totalItems());
				Vector fields = getNormalFields(s);
				Section old = new Section(groupListRight.totalItems());
				String field;
				for (int i = 0; i < fields.size(); i++) {
					field = (String) fields.get(i);
					if (!old.containsSection(field)) {
						old.addSection(field);
					}
				}
				Vector v = old.toVector();
				groupListRight.x_setData(v, v);
			}
		});
		initIconButton(bAuto);

		groupPanel.add(groupComboLeft, GM.getGBC(1, 1));
		groupPanel.add(groupLabelRight, GM.getGBC(1, 3));
		JScrollPane groupScrollPaneRight = new JScrollPane();
		groupScrollPaneRight.getViewport().add(groupListRight, null);
		JScrollPane groupScrollPaneLeft = new JScrollPane();
		groupScrollPaneLeft.getViewport().add(groupListLeft, null);
		groupPanel.add(groupScrollPaneLeft, GM.getGBC(2, 1, true, true));
		groupPanel.add(getVPanel(groupButtonRight, groupButtonLeft, bAuto),
				GM.getGBC(2, 2, false, true));
		groupPanel.add(groupScrollPaneRight, GM.getGBC(2, 3, true, true));

		return groupPanel;
	}

	/**
	 * 取HAVING面板
	 * 
	 * @return
	 */
	private JPanel getHavingPanel() {
		havingButtonAdd.setToolTipText(mm.getMessage("dialogsqleditor.add"));
		havingButtonAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				havingTable.addRow();
			}
		});
		havingButtonAdd.setIcon(GM.getImageIcon(GC.IMAGES_PATH
				+ "m_addrecord.gif"));
		initIconButton(havingButtonAdd);
		havingButtonDel.setToolTipText(mm.getMessage("dialogsqleditor.delete"));
		havingButtonDel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				havingTable.deleteSelectedRow();
			}
		});
		havingButtonDel.setIcon(GM.getImageIcon(GC.IMAGES_PATH
				+ "m_deleterecord.gif"));
		initIconButton(havingButtonDel);
		havingTable.setRowHeight(20);
		havingTable.setIndexCol(COL_ID);
		havingTable.setColumnDropDown(COL_OPT, operateCodeItems,
				operateCodeItems, true);
		havingTable.setColumnDropDown(COL_CONNECT, connectCodeItems,
				connectCodeItems, true);
		JPanel havingPanel = new JPanel();
		JScrollPane havingScrollPane = new JScrollPane(havingTable);
		havingPanel.setLayout(new GridBagLayout());
		havingPanel.add(new JLabel(), GM.getGBC(1, 1, true));
		havingPanel.add(havingButtonAdd, GM.getGBC(1, 2));
		havingPanel.add(havingButtonDel, GM.getGBC(1, 3));
		GridBagConstraints gbc = GM.getGBC(2, 1, true, true);
		gbc.gridwidth = 3;
		havingPanel.add(havingScrollPane, gbc);
		return havingPanel;
	}

	/**
	 * 取排序面板
	 * 
	 * @return
	 */
	private JPanel getOrderPanel() {
		orderButtonRight.setText(">");
		orderButtonRight
				.addActionListener(new DialogSQLEditor_jButtonOR_actionAdapter(
						this));
		orderButtonLeft.setText("<");
		orderButtonLeft
				.addActionListener(new DialogSQLEditor_jButtonOL_actionAdapter(
						this));
		JLabel orderLabelRight = new JLabel();
		orderLabelRight.setText(mm.getMessage("dialogsqleditor.order"));
		orderComboLeft.addItemListener(tableChanged);
		orderListLeft.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					jButtonOR_actionPerformed(null);
				}
			}
		});

		orderListRight.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					int index = orderListRight.getSelectedIndex();
					String direction = "", item = orderListRight.data
							.getElementAt(index).toString();
					StringTokenizer st = new StringTokenizer(item);
					if (st.hasMoreTokens()) {
						item = st.nextToken();
					}
					if (st.hasMoreTokens()) {
						direction = st.nextToken();
					}
					if (direction.equalsIgnoreCase("ASC")) {
						item += " DESC";
					} else {
						item += " ASC";
					}
					orderListRight.data.set(index, item);
				}
			}
		});
		JPanel orderPanel = new JPanel();
		orderPanel.setLayout(new GridBagLayout());
		orderPanel.add(orderComboLeft, GM.getGBC(1, 1));
		orderPanel.add(orderLabelRight, GM.getGBC(1, 3));
		JScrollPane orderScrollPaneRight = new JScrollPane();
		orderScrollPaneRight.getViewport().add(orderListRight, null);
		JScrollPane orderScrollPaneLeft = new JScrollPane();
		orderScrollPaneLeft.getViewport().add(orderListLeft, null);
		orderPanel.add(orderScrollPaneLeft, GM.getGBC(2, 1, true, true));
		orderPanel.add(getVPanel(orderButtonRight, orderButtonLeft, null),
				GM.getGBC(2, 2, false, true));
		orderPanel.add(orderScrollPaneRight, GM.getGBC(2, 3, true, true));

		return orderPanel;
	}

	/**
	 * 取SQL面板
	 * 
	 * @return
	 */
	private JPanel getSqlPanel() {
		JPanel jPanelSql = new JPanel();
		BorderLayout borderLayout3 = new BorderLayout();
		jPanelSql.setLayout(borderLayout3);
		jTextPaneSql
				.addKeyListener(new DialogSQLEditor_jTextPaneSql_keyAdapter(
						this));
		JScrollPane jScrollPaneSql = new JScrollPane();
		jScrollPaneSql.getViewport().add(jTextPaneSql, null);
		jPanelSql.add(jScrollPaneSql, BorderLayout.CENTER);

		return jPanelSql;
	}

	/**
	 * 取外键映射
	 * 
	 * @param ds
	 *            数据源
	 * @param schema
	 *            模式名
	 * @return
	 */
	private FKMap getFKMap(DataSource ds, String schema) {
		String str = fromListRight.totalItems();
		if (!StringUtils.isValidString(str)) {
			return null;
		}
		Section tables = new Section(str);
		if (tables.size() == 0) {
			return null;
		}
		String table;
		FKMap map = new FKMap();
		FKMap tableMap;
		for (int i = 0; i < tables.size(); i++) {
			table = tables.getSection(i);
			try {
				tableMap = getTableFKMap(ds, schema, table, tables);
				map.addAll(tableMap);

			} catch (Throwable e) {
			}

		}
		return map;
	}

	/**
	 * 取外键映射
	 * 
	 * @param ds
	 *            数据源
	 * @param schema
	 *            模式名
	 * @param table
	 *            表名
	 * @param tables
	 *            已选表名
	 * @return
	 * @throws Throwable
	 */
	private FKMap getTableFKMap(DataSource ds, String schema, String table,
			Section tables) throws Throwable {
		Connection con = (Connection) ds.getDBSession().getSession();
		DatabaseMetaData md = con.getMetaData();
		DBInfo dbInfo = ds.getDBInfo();
		String dbcs = dbInfo.getDBCharset();
		String ccs = dbInfo.getClientCharset();
		boolean convert = dbcs != null && !dbcs.equals(ccs);
		String[] schemaTable = GM.getRealSchemaTable(con, schema, table);
		schema = schemaTable[0];
		table = schemaTable[1];
		if (convert) {
			schema = GM.convertDBSearchString(ds, schema);
			table = GM.convertDBSearchString(ds, table);
		}
		ResultSet rs = null;
		FKMap fkMap = new FKMap();
		try {
			rs = md.getImportedKeys(null, schema, table);
			String pkTableName, pkColName, colName;
			while (rs.next()) {
				pkTableName = rs.getString("PKTABLE_NAME");
				if (convert) {
					pkTableName = GM.convertDBString(ds, pkTableName);
				}
				if (tables.containsSection(pkTableName)) {
					pkColName = rs.getString("PKCOLUMN_NAME");
					colName = rs.getString("FKCOLUMN_NAME");
					if (convert) {
						pkColName = GM.convertDBString(ds, pkColName);
						colName = GM.convertDBString(ds, colName);
					}
					if (!colName.startsWith(table + ".")) {
						colName = table + "." + colName;
					}
					if (!pkColName.startsWith(pkTableName + ".")) {
						pkColName = pkTableName + "." + pkColName;
					}
					fkMap.add(colName, pkColName);
				}
			}
		} finally {
			rs.close();
		}
		return fkMap;
	}

	/**
	 * 外键映射对象
	 *
	 */
	class FKMap {
		Section fkNames = new Section(), pkNames = new Section();

		public FKMap() {
		}

		public void add(String fkName, String pkName) {
			int index = fkNames.indexOf(fkName);
			if (index > -1) {
				pkNames.replaceSection(index, pkName);
			} else {
				fkNames.addSection(fkName);
				pkNames.addSection(pkName);
			}
		}

		public String getFKName(int i) {
			return fkNames.getSection(i);
		}

		public String getPKName(int i) {
			return pkNames.getSection(i);
		}

		public int size() {
			return fkNames == null ? 0 : fkNames.size();
		}

		public void addAll(FKMap map) {
			if (map == null) {
				return;
			}
			for (int i = 0; i < map.size(); i++) {
				add(map.getFKName(i), map.getPKName(i));
			}
		}
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		JLabel jLBSchema = new JLabel();
		jLBSchema.setText(mm.getMessage("dialogsqleditor.schema"));
		jCBSchema
				.addActionListener(new DialogSQLEditor_jCBSchema_actionAdapter(
						this));
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);

		JPanel jPanel1 = new JPanel();
		VFlowLayout verticalFlowLayout1 = new VFlowLayout();
		BorderLayout borderLayout2 = new BorderLayout();
		jPanel1.setLayout(verticalFlowLayout1);
		JLabel jLabel1 = new JLabel();
		jLabel1.setText(" ");

		JPanel jPanel2 = new JPanel();
		GridBagLayout gridBagLayout1 = new GridBagLayout();
		jPanel2.setLayout(gridBagLayout1);
		this.setTitle(mm.getMessage("dialogsqleditor.sqleditor"));
		this.getContentPane().setLayout(borderLayout2);

		jBOK.setText("OK");
		jBOK.setMnemonic('O');
		jBOK.addActionListener(new DialogSQLEditor_jBOK_actionAdapter(this));
		jBCancel.setText("Cancel");
		jBCancel.setMnemonic('C');
		jBCancel.addActionListener(new DialogSQLEditor_jBCancel_actionAdapter(
				this));
		jTabbedPaneSql.setTabPlacement(JTabbedPane.TOP);
		JPanel jPanel3 = new JPanel();
		BorderLayout borderLayout4 = new BorderLayout();
		jPanel3.setLayout(borderLayout4);
		jPanel3.add(jPanel2, BorderLayout.SOUTH);

		this.getContentPane().add(jPanel1, BorderLayout.EAST);
		jPanel1.add(jBOK, null);
		jPanel1.add(jBCancel, null);
		jPanel1.add(jLabel1, null);
		jPanel2.add(jLBSchema, GM.getGBC(1, 1));
		jPanel2.add(jCBSchema, GM.getGBC(1, 2, true));
		this.getContentPane().add(jPanel3, BorderLayout.CENTER);
		jPanel3.add(jTabbedPaneSql, BorderLayout.CENTER);

		int i = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
		fromListLeft.setSelectionMode(i);
		selectListLeft.setSelectionMode(i);
		selectListRight.setSelectionMode(i);
		groupListLeft.setSelectionMode(i);
		groupListRight.setSelectionMode(i);
		orderListLeft.setSelectionMode(i);
		orderListRight.setSelectionMode(i);

		operateCodeItems.add(OPT_NUL);
		operateCodeItems.add(OPT_EQUAL);
		operateCodeItems.add(OPT_NOT_EQUAL);
		operateCodeItems.add(OPT_MORE);
		operateCodeItems.add(OPT_LESS);
		operateDispItems.add("");
		operateDispItems.add("=");
		operateDispItems.add("<>");
		operateDispItems.add(">");
		operateDispItems.add("<");

		connectCodeItems.add(OPT_NUL);
		connectCodeItems.add(OPT_AND);
		connectCodeItems.add(OPT_OR);
		connectDispItems.add("");
		connectDispItems.add("and");
		connectDispItems.add("or");

		jTabbedPaneSql.addChangeListener(new TabChangeListener());
		jTabbedPaneSql.add(getFromPanel(),
				mm.getMessage("dialogsqleditor.table")); // Table
		jTabbedPaneSql.add(getSelectPanel(),
				mm.getMessage("dialogsqleditor.field"));// Field
		jTabbedPaneSql.add(getWherePanel(),
				mm.getMessage("dialogsqleditor.where"));// Where
		jTabbedPaneSql.add(getJoinPanel(),
				mm.getMessage("dialogsqleditor.join"));// Join
		jTabbedPaneSql.add(getGroupPanel(),
				mm.getMessage("dialogsqleditor.group"));// Group
		jTabbedPaneSql.add(getHavingPanel(),
				mm.getMessage("dialogsqleditor.having"));// Having
		jTabbedPaneSql.add(getOrderPanel(),
				mm.getMessage("dialogsqleditor.sort"));// Sort
		jTabbedPaneSql.add(getSqlPanel(), mm.getMessage("dialogsqleditor.sql")); // SQL

		jTabbedPaneSql.setSelectedComponent(fromPanel);

		setModal(true);
	}

	/**
	 * 窗口关闭事件
	 */
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			if (pw != null) {
				try {
					pw.disposeWindow();
				} catch (Exception ex) {
				}
				pw = null;
			}
			GM.setWindowDimension(this);
			dispose();
		}
	}

	/**
	 * 更新表名
	 * 
	 * @param tables
	 *            表名
	 * @throws Exception
	 */
	private void updateTableName(String tables) throws Exception {
		codeTable.removeAllElements();
		dispTable.removeAllElements();
		if (tables == null) {
			return;
		}
		Vector v = new Section(tables, ',', false, false).sections;
		codeTable.addAll(v);
		for (int k = 0; k < v.size(); k++) {
			dispTable.addElement(v.get(k).toString());
		}

		if (codeTable.size() > 0) {
			changeSelectTable((String) codeTable.get(0));
		}
	}

	/**
	 * 更新表字段
	 * 
	 * @param tableName
	 * @throws Exception
	 */
	private void updateTableFields(String tableName) throws Exception {
		codeFields.removeAllElements();
		dispFields.removeAllElements();
		Vector v = getListTableColumns(
				GM.getRealSchema(jCBSchema.getSelectedItem()), tableName);
		if (v != null) {
			codeFields.addAll(v);
			for (int k = 0; k < v.size(); k++) {
				dispFields.addElement(v.get(k));
			}
		}
		selectListLeft.x_sort(true, true);
	}

	/**
	 * 取表字段
	 * 
	 * @param schema
	 *            模式名
	 * @param tableName
	 *            表名
	 * @return
	 */
	private Vector<String> getListTableColumns(String schema, String tableName) {
		try {
			if (ds == null) {
				return null;
			}
			if (ds.isOLAP()) {
				return null;
			}
			Vector<String> colNames = new Vector<String>();
			Connection con = (Connection) ds.getDBSession().getSession();
			if (con == null) {
				return null;
			}
			DatabaseMetaData dbmd = con.getMetaData();
			if (dbmd == null) {
				return null;
			}
			String[] schemaTable = GM
					.getRealSchemaTable(con, schema, tableName);
			if (schemaTable != null) {
				schema = schemaTable[0];
				tableName = schemaTable[1];
			}
			DBInfo dbInfo = ds.getDBInfo();
			String dbcs = dbInfo.getDBCharset();
			String ccs = dbInfo.getClientCharset();
			boolean convert = dbcs != null && !dbcs.equals(ccs);
			if (convert) {
				schema = GM.convertDBSearchString(ds, schema);
				tableName = GM.convertDBSearchString(ds, tableName);
			}
			String name;
			String tilde = dbmd.getIdentifierQuoteString();
			int index = tableName.indexOf('.');
			if (index > -1) {
				schema = tableName.substring(0, index);
				if (schema.startsWith(tilde)) {
					schema = schema.substring(tilde.length(), schema.length()
							- tilde.length());
				}
				name = tableName.substring(index + 1, tableName.length());
				if (name.startsWith(tilde)) {
					name = name.substring(tilde.length(),
							name.length() - tilde.length());
				}
			} else {
				name = tableName;
				if (name.startsWith(tilde)) {
					name = name.substring(tilde.length(),
							name.length() - tilde.length());
				}
			}

			boolean isAddTilde = ds.getDBConfig().isAddTilde();
			ResultSet rs = dbmd.getColumns(con.getCatalog(), schema, tableName,
					null);
			String colName;
			while (rs.next()) {
				colName = rs.getString("COLUMN_NAME");
				if (convert) {
					colName = GM.convertDBString(ds, colName);
				}
				if (isAddTilde) {
					colName = GM.tildeString(colName, tilde);
				}
				colNames.addElement(colName);
			}
			rs.close();
			GM.sort(colNames, true);
			return colNames;
		} catch (Throwable ex) {
			GM.showException(ex);
		}
		return null;
	}

	/**
	 * 设置选择的项目
	 * 
	 * @param currentTable
	 *            当前表
	 * @param source
	 *            源列表控件
	 * @param target
	 *            目标列表控件
	 */
	private void setSelectedItems(String currentTable, JListEx source,
			Object target) {
		currentTable = addTableQuote(currentTable);
		Object[] items;
		items = source.x_getSelectedValues();
		if (target instanceof JListEx) {
			JListEx tarList = (JListEx) target;
			for (int i = 0; i < items.length; i++) {
				if (currentTable == null) {
					if (tarList.data.contains(items[i])) {
						continue;
					}
					tarList.data.addElement(items[i]);
				} else {
					if (tarList.data.contains(currentTable + "." + items[i])) {
						continue;
					}
					tarList.data.addElement(currentTable + "." + items[i]);
				}
			}
		} else {
			JTextPane text = (JTextPane) target;
			String str = text.getText();
			String buf = new String();
			for (int i = 0; i < items.length; i++) {
				if (currentTable == null) {
					buf += " " + items[i].toString() + " ";
				} else {
					buf += " " + currentTable + "." + items[i].toString() + " ";
				}
			}
			if (buf.length() == 0) {
				return;
			}
			text.setText(str + buf);
		}
	}

	/**
	 * 设置选出的排序项目
	 * 
	 * @param table
	 *            表名
	 * @param source
	 *            源列表控件
	 * @param target
	 *            目标列表控件
	 */
	private void setSelectedSortItems(String table, JListEx source,
			JListEx target) {
		table = addTableQuote(table);
		Object[] tables;
		tables = source.x_getSelectedValues();
		for (int i = 0; i < tables.length; i++) {
			String tmpSA = table + "." + tables[i].toString() + " ASC";
			String tmpSD = table + "." + tables[i].toString() + " DESC";
			if (target.data.contains(tmpSA) || target.data.contains(tmpSD)) {
				continue;
			}
			target.data.addElement(tmpSA);
		}

	}

	/**
	 * 表名增加引号
	 * 
	 * @param fromStr
	 * @return
	 */
	private String addTableQuote(String fromStr) {
		if (fromStr == null)
			return null;
		if (!StringUtils.isValidString(fromStr)) {
			return "";
		}
		String driver = ds.getDBConfig().getDriver();
		if (!"com.esproc.jdbc.InternalDriver".equalsIgnoreCase(driver)) {
			return fromStr;
		}
		ArgumentTokenizer sections = new ArgumentTokenizer(fromStr, ',', true,
				true, true, true);
		StringBuffer buf = new StringBuffer();
		while (sections.hasMoreTokens()) {
			if (buf.length() > 0)
				buf.append(',');
			String tableName = sections.nextToken();
			if (tableName.indexOf(".") > -1) {
				if (!tableName.startsWith("\"") || !tableName.endsWith("\""))
					tableName = Escape.addEscAndQuote(tableName);
			}
			buf.append(tableName);
		}
		return buf.toString();
	}

	/**
	 * 增加表事件
	 * 
	 * @param e
	 */
	void jButtonFR_actionPerformed(ActionEvent e) {
		setSelectedItems(null, fromListLeft, fromListRight);
		String sSql = jTextPaneSql.getText();
		String tmpFrom = addTableQuote(fromListRight.totalItems());
		if (!sSql.trim().toLowerCase().startsWith("select")) {
			sSql = "SELECT *";
		}
		jTextPaneSql.setText(GM.modifySql(sSql, tmpFrom, "f"));
		selectedTableChanged();
	}

	/**
	 * 删除表事件
	 * 
	 * @param e
	 */
	void jButtonFL_actionPerformed(ActionEvent e) {
		fromListRight.removeSelectedItems();
		String sSql = jTextPaneSql.getText();
		String tmpFrom = addTableQuote(fromListRight.totalItems());
		if (!com.scudata.common.StringUtils.isValidString(tmpFrom)) {
			sSql = "";
		} else {
			sSql = GM.modifySql(sSql, tmpFrom, "f");
		}
		jTextPaneSql.setText(sSql);
		selectedTableChanged();
	}

	/**
	 * 取消按钮事件
	 * 
	 * @param e
	 */
	void jBCancel_actionPerformed(ActionEvent e) {
		GM.setWindowDimension(this);
		m_option = JOptionPane.CANCEL_OPTION;
		dispose();
	}

	/**
	 * 确认按钮事件
	 * 
	 * @param e
	 */
	void jBOK_actionPerformed(ActionEvent e) {
		m_option = JOptionPane.OK_OPTION;
		if (!isCopyMode) {
			generateSql(jTabbedPaneSql.getSelectedIndex());
			String ls_sql = jTextPaneSql.getText();
			if (!com.scudata.common.StringUtils.isValidString(ls_sql)) {
				return;
			}
			ls_sql = ls_sql.trim();
			if (ls_sql.startsWith("FROM")) {
				ls_sql = "SELECT * " + ls_sql;
				jTextPaneSql.setText(ls_sql);
			}
			GM.setWindowDimension(this);
			dispose();
		} else {
			if (jTabbedPaneSql.getSelectedIndex() != TAB_SQL) {
				jTabbedPaneSql.setSelectedIndex(TAB_SQL);
			}
			GM.clipBoard(getSQL());
		}
	}

	/**
	 * 选出字段事件
	 * 
	 * @param e
	 */
	void jButtonSR_actionPerformed(ActionEvent e) {
		setSelectedItems((String) selectComboLeft.x_getSelectedItem(),
				selectListLeft, selectListRight);
		selectedFieldChanged();
	}

	/**
	 * 删除字段事件
	 * 
	 * @param e
	 */
	void jButtonSL_actionPerformed(ActionEvent e) {
		selectListRight.removeSelectedItems();
		selectedFieldChanged();
	}

	/**
	 * 选择的表发生变化
	 */
	private void selectedTableChanged() {
		FKMap map = getFKMap(ds, GM.getRealSchema(jCBSchema.getSelectedItem()));
		joinTable.acceptText();
		joinTable.removeAllRows();
		joinTable.clearSelection();
		if (map == null) {
			return;
		}
		String fkName, pkName;
		int row = -1;
		for (int i = 0; i < map.size(); i++) {
			fkName = map.getFKName(i);
			pkName = map.getPKName(i);
			if (!existWhere(fkName, pkName)) {
				row = joinTable.addRow(false);
				joinTable.data.setValueAt(fkName, row, COL_FIELD);
				joinTable.data.setValueAt(OPT_EQUAL, row, COL_OPT);
				joinTable.data.setValueAt(pkName, row, COL_VAL);
				if (row > 0) {
					joinTable.data.setValueAt(OPT_AND, row - 1, COL_CONNECT);
				}
			}
		}
		if (row > -1) {
			joinTable.setEditingRow(row);
			joinTable.resetIndex();
			joinTable.selectRow(row);
		}
	}

	/**
	 * 是否存在条件
	 * 
	 * @param f1
	 * @param f2
	 * @return
	 */
	private boolean existWhere(String f1, String f2) {
		Object tmp1, tmp2;
		for (int i = 0; i < whereTable.getRowCount(); i++) {
			tmp1 = whereTable.data.getValueAt(i, COL_OPT);
			if (!OPT_EQUAL.equals(tmp1)) {
				continue;
			}
			tmp1 = whereTable.data.getValueAt(i, COL_FIELD);
			tmp2 = whereTable.data.getValueAt(i, COL_VAL);
			if ((f1.equals(tmp1) && f2.equals(tmp2))
					|| (f1.equals(tmp2) && f2.equals(tmp1))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 选出的字段发生变化
	 */
	private void selectedFieldChanged() {
		Section s = new Section(selectListRight.totalItems());
		Vector all = s.toVector();
		whereTable.setColumnDropDown(COL_FIELD, all, all, true);
		Vector<String> computedFields = getComputedFields(s);
		havingTable.setColumnDropDown(COL_FIELD, computedFields,
				computedFields, true);
	}

	/**
	 * 取计算列列表
	 * 
	 * @param s
	 * @return
	 */
	private Vector<String> getComputedFields(Section s) {
		if (s == null || s.size() == 0) {
			return new Vector<String>();
		}
		Vector<String> computedFields = new Vector<String>();
		String exp;
		String[] exps;
		for (int i = 0; i < s.size(); i++) {
			exp = s.getSection(i);
			exps = parseComputedField(exp);
			if (StringUtils.isValidString(exps[0])) {
				computedFields.add(exp);
			}
		}
		return computedFields;
	}

	/**
	 * 取常规字段列表
	 * 
	 * @param s
	 * @return
	 */
	private Vector<String> getNormalFields(Section s) {
		if (s == null || s.size() == 0) {
			return new Vector<String>();
		}
		Vector<String> fields = new Vector<String>();
		String exp;
		String[] exps;
		for (int i = 0; i < s.size(); i++) {
			exp = s.getSection(i);
			exps = parseComputedField(exp);
			if (!StringUtils.isValidString(exps[0])) {
				fields.add(exp);
			}
		}
		return fields;
	}

	/**
	 * 增加排序事件
	 * 
	 * @param e
	 */
	void jButtonOR_actionPerformed(ActionEvent e) {
		setSelectedSortItems((String) orderComboLeft.x_getSelectedItem(),
				orderListLeft, orderListRight);
	}

	/**
	 * 删除排序事件
	 * 
	 * @param e
	 */
	void jButtonOL_actionPerformed(ActionEvent e) {
		orderListRight.removeSelectedItems();
	}

	/**
	 * 弹出提示对话框
	 * 
	 * @param text
	 *            提示信息
	 */
	void messageBox(String text) {
		JOptionPane.showMessageDialog(this, text,
				mm.getMessage("dialogsqlsrcdata.prompt"), // 提示
				JOptionPane.WARNING_MESSAGE);
	}

	/**
	 * 取WHERE条件
	 * 
	 * @param table
	 * @return
	 */
	private String getWhere(JTableEx table) {
		table.acceptText();
		int c = table.getRowCount();
		if (c == 0) {
			return "";
		}
		StringBuffer where = new StringBuffer();
		Object field, opt, val, connect;
		for (int i = 0; i < c; i++) {
			field = table.data.getValueAt(i, COL_FIELD);
			if (field == null) {
				continue;
			}
			opt = table.data.getValueAt(i, COL_OPT);
			if (opt == null) {
				continue;
			}
			val = table.data.getValueAt(i, COL_VAL);
			if (val == null) {
				continue;
			}
			connect = table.data.getValueAt(i, COL_CONNECT);
			if (connect == null && i != c - 1) {
				continue;
			}
			where.append(" ");
			where.append(field);
			where.append(" ");
			where.append(opt);
			where.append(" ");
			where.append(val);
			if (i != c - 1) {
				where.append(" ");
				where.append(connect);
			}
		}
		if (where.length() == 0) {
			return "";
		}
		String whereStr = where.toString().trim();
		String lastOpt = null;
		if (whereStr.endsWith(OPT_AND)) {
			lastOpt = OPT_AND;
		} else if (whereStr.endsWith(OPT_OR)) {
			lastOpt = OPT_OR;
		}
		if (lastOpt != null) {
			whereStr = whereStr.substring(0,
					whereStr.length() - lastOpt.length());
		}
		return whereStr.trim();
	}

	/**
	 * 生成SQL语句
	 * 
	 * @param tabIndex
	 *            当前选择的TAB序号
	 * @return
	 */
	private String generateSql(int tabIndex) {
		String sSql, sTmp;
		Section selects;
		sSql = jTextPaneSql.getText();
		switch (tabIndex) {
		case TAB_FROM:
			break;
		case TAB_SELECT:
			selects = new Section(selectListRight.totalItems(), ',', false,
					false);
			selects.removeSection("*");
			sTmp = selects.toString();
			if (!com.scudata.common.StringUtils.isValidString(sTmp)) {
				sTmp = "*";
			}
			sSql = GM.modifySql(sSql, sTmp, "s");
			break;
		case TAB_WHERE:
		case TAB_JOIN:
			String where1 = getWhere(whereTable); // whereTextPaneRight.getText();
			String where2 = getWhere(joinTable);
			if (StringUtils.isValidString(where1)) {
				if (StringUtils.isValidString(where2)) {
					where1 = "(" + where1 + ") and (" + where2 + ")";
				}
			} else if (StringUtils.isValidString(where2)) {
				where1 = where2;
			}
			sSql = GM.modifySql(sSql, where1, "w");
			break;
		case TAB_GROUP:
			selects = new Section(groupListRight.totalItems(), ',', false,
					false);
			sTmp = selects.toString();
			sSql = GM.modifySql(sSql, sTmp, "g");
			break;
		case TAB_HAVING:
			sTmp = getWhere(havingTable);
			sSql = GM.modifySql(sSql, sTmp, "h");
			break;
		case TAB_ORDER:
			selects = new Section(orderListRight.totalItems(), ',', false,
					false);
			sTmp = selects.toString();
			sSql = GM.modifySql(sSql, sTmp, "o");
			break;
		case TAB_SQL:
			return sSql;
		}
		jTextPaneSql.setText(sSql.trim());
		return sSql;
	}

	/**
	 * 选择的TAB页变化监听器
	 *
	 */
	class TabChangeListener implements ChangeListener {
		int oldIndex = 0;

		public void stateChanged(ChangeEvent e) {
			switch (oldIndex) {
			case TAB_FROM:
				currentFrom = fromListRight.totalItems();

				// 改变选择表
				try {
					updateTableName(currentFrom);
				} catch (Exception x) {
					Logger.debug(x);
				}
				break;
			case TAB_SQL:
				if (bEditByHand) {
					bEditByHand = false;
					try {
						// showSql();
					} catch (Exception x) {
						GM.showException(x);
					}
				}
				break;
			default:
				generateSql(oldIndex);
				break;
			}
			oldIndex = jTabbedPaneSql.getSelectedIndex();
		}
	}

	/**
	 * SQL编辑框按键后
	 * 
	 * @param e
	 */
	void jTextPaneSql_keyReleased(KeyEvent e) {
		bEditByHand = true;
	}

	/**
	 * 模式名变化事件
	 * 
	 * @param e
	 */
	void jCBSchema_actionPerformed(ActionEvent e) {
		if (afterInit) {
			refreshTables();
		}
	}

	/**
	 * 表发生变化
	 *
	 */
	class TableChanged implements ItemListener {
		private DialogSQLEditor executor = null;

		public TableChanged(DialogSQLEditor executor) {
			this.executor = executor;
		}

		public void itemStateChanged(ItemEvent event) {
			int state = event.getStateChange();
			if (state != ItemEvent.SELECTED || executor == null) {
				return;
			}

			JComboBoxEx jcb = (JComboBoxEx) event.getSource();
			try {
				executor.changeSelectTable((String) jcb.x_getSelectedItem());
			} catch (Exception x) {
				GM.showException(x);
			}
		}
	}

	/**
	 * 新增分组
	 * 
	 * @param e
	 */
	void groupR_actionPerformed(ActionEvent e) {
		setSelectedItems((String) groupComboLeft.x_getSelectedItem(),
				groupListLeft, groupListRight);
	}

	/**
	 * 删除分组
	 * 
	 * @param e
	 */
	void groupL_actionPerformed(ActionEvent e) {
		groupListRight.removeSelectedItems();
	}
}

class DialogSQLEditor_fromButtonRight_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSQLEditor adaptee;

	DialogSQLEditor_fromButtonRight_actionAdapter(DialogSQLEditor adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jButtonFR_actionPerformed(e);
	}
}

class DialogSQLEditor_fromButtonLeft_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSQLEditor adaptee;

	DialogSQLEditor_fromButtonLeft_actionAdapter(DialogSQLEditor adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jButtonFL_actionPerformed(e);
	}
}

class DialogSQLEditor_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSQLEditor adaptee;

	DialogSQLEditor_jBCancel_actionAdapter(DialogSQLEditor adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogSQLEditor_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSQLEditor adaptee;

	DialogSQLEditor_jBOK_actionAdapter(DialogSQLEditor adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogSQLEditor_jButtonSR_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSQLEditor adaptee;

	DialogSQLEditor_jButtonSR_actionAdapter(DialogSQLEditor adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jButtonSR_actionPerformed(e);
	}
}

class DialogSQLEditor_jButtonSL_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSQLEditor adaptee;

	DialogSQLEditor_jButtonSL_actionAdapter(DialogSQLEditor adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jButtonSL_actionPerformed(e);
	}
}

class DialogSQLEditor_groupR_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSQLEditor adaptee;

	DialogSQLEditor_groupR_actionAdapter(DialogSQLEditor adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.groupR_actionPerformed(e);
	}
}

class DialogSQLEditor_groupL_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSQLEditor adaptee;

	DialogSQLEditor_groupL_actionAdapter(DialogSQLEditor adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.groupL_actionPerformed(e);
	}
}

class DialogSQLEditor_jButtonOR_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSQLEditor adaptee;

	DialogSQLEditor_jButtonOR_actionAdapter(DialogSQLEditor adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jButtonOR_actionPerformed(e);
	}
}

class DialogSQLEditor_jButtonOL_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSQLEditor adaptee;

	DialogSQLEditor_jButtonOL_actionAdapter(DialogSQLEditor adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jButtonOL_actionPerformed(e);
	}

}

class DialogSQLEditor_jTextPaneSql_keyAdapter extends java.awt.event.KeyAdapter {
	DialogSQLEditor adaptee;

	DialogSQLEditor_jTextPaneSql_keyAdapter(DialogSQLEditor adaptee) {
		this.adaptee = adaptee;
	}

	public void keyReleased(KeyEvent e) {
		adaptee.jTextPaneSql_keyReleased(e);
	}
}

class DialogSQLEditor_jCBSchema_actionAdapter implements
		java.awt.event.ActionListener {
	DialogSQLEditor adaptee;

	DialogSQLEditor_jCBSchema_actionAdapter(DialogSQLEditor adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jCBSchema_actionPerformed(e);
	}
}
