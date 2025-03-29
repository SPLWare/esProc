package com.scudata.ide.spl.base;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.scudata.app.common.Section;
import com.scudata.common.DBConfig;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.DataSource;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.control.IconTreeNode;
import com.scudata.ide.common.control.IconTreeRender;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.spl.GVSpl;
import com.scudata.ide.spl.SheetSpl;
import com.scudata.ide.spl.ToolBarProperty;
import com.scudata.ide.spl.control.ContentPanel;

public class PanelSelectField extends JPanel {
	private static final long serialVersionUID = 1L;

	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/**
	 * 复制按钮
	 */
	private JButton jBCopy = new JButton();

	/**
	 * 增加按钮
	 */
	private JButton jBAdd = new JButton();

	/**
	 * 为字段名增加表名前缀
	 */
	private JCheckBox jCBOpt = new JCheckBox(mm.getMessage("panelselectfield.tablepre"));

	/**
	 * 根节点
	 */
	private IconTreeNode root = new IconTreeNode(mm.getMessage("panelselectfield.dsname"));

	/**
	 * 树模型
	 */
	private DefaultTreeModel treeModel = new DefaultTreeModel(root);

	/**
	 * 树控件
	 */
	private JTree mTree = new JTree(treeModel);

	/**
	 * 字段结点的级别
	 */
	private final byte LEVEL_FIELD = 3;

	/**
	 * 显示信息的标签控件
	 */
	private JLabel labelMsg = new JLabel();

	/**
	 * 数据库取结构的线程
	 */
	private Thread dbThread = null;

	/**
	 * 线程是否停止
	 */
	private boolean stoped = false;

	/**
	 * 构造函数
	 */
	public PanelSelectField() {
		init();
		resetEnv();
	}

	/**
	 * 重置环境。 外层做了判断，一定有可连接的数据源。
	 */
	public void resetEnv() {
		if (dbThread != null) {
			stoped = true;
		}
		root.removeAllChildren();
		treeModel.nodeStructureChanged(root);

		if (!ConfigOptions.bShowDBStruct) // 不加载数据库结构
			return;

		dbThread = new Thread() {
			public void run() {
				stoped = false;
				DataSource ds;
				String dsName, tableName;
				Vector<String> tables, fields;
				final List<String> dsNames = new ArrayList<String>();
				final ArrayList<ArrayList<TableConfig>> dsList = new ArrayList<ArrayList<TableConfig>>();
				try {
					labelMsg.setVisible(true);
					labelMsg.setText(mm.getMessage("panelselectfield.loading") + "...");
					for (int i = 0; i < GV.dsModel.getSize(); i++) {
						if (stoped) {
							return;
						}
						ds = (DataSource) GV.dsModel.get(i);
						if (ds != null && !ds.isClosed()) {
							Connection con = null;
							try {
								con = (Connection) ds.getDBSession().getSession();
							} catch (Throwable e1) {
								continue;
							}
							if (con == null)
								continue;
							if (ds.getDBInfo() instanceof DBConfig) {
								DBConfig dbConfig = ((DBConfig) ds.getDBInfo());
								if (dbConfig.getDriver().toLowerCase().startsWith("com.scudata.datahub")) {
									// datahub
									ResultSet rs = null;
									DatabaseMetaData dbmd = null;
									List<String> splNames = new ArrayList<String>();
									try {
										dbmd = con.getMetaData();
										rs = dbmd.getProcedures(null, null, null);
										while (rs.next()) {
											String splName = rs.getString("PROCEDURE_NAME");
											splNames.add(splName);
										}
									} catch (SQLException e) {
									} finally {
										if (rs != null)
											try {
												rs.close();
											} catch (SQLException e) {
											}
									}

									if (!splNames.isEmpty() && dbmd != null) {
										ArrayList<TableConfig> tableList = new ArrayList<TableConfig>();
										dsList.add(tableList);
										for (int d = 0, splSize = splNames.size(); d < splSize; d++) {
											String splName = (String) splNames.get(d);
											rs = null;
											try {
												// 尚未实现
												rs = dbmd.getProcedureColumns(null, null, splName, null);
												TableConfig tc = new TableConfig();
												tc.tableName = splName;
												tc.fields = null;
												tableList.add(tc);
											} catch (SQLException e) {
											} finally {
												if (rs != null)
													try {
														rs.close();
													} catch (SQLException e) {
													}
											}
										}
									}
									continue;
								}
							}
							dsName = ds.getName();
							dsNames.add(dsName);
							tables = GM.listTableNames(dsName);
							if (tables == null || tables.isEmpty()) {
								continue;
							}
							String[] tableNames = new String[tables.size()];
							for (int s = 0; s < tables.size(); s++) {
								tableNames[s] = (String) tables.get(s);
							}
							Arrays.sort(tableNames);

							Map<String, Vector<String>> tableFields = null;
							if (tables.size() > 300 && !GM.isDataLogicDS(ds)) {
								try {
									tableFields = getTableFields(con);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

							ArrayList<TableConfig> tableList = new ArrayList<TableConfig>();
							dsList.add(tableList);
							TableConfig tc;
							for (int t = 0; t < tableNames.length; t++) {
								if (stoped) {
									return;
								}
								tc = new TableConfig();
								tableList.add(tc);
								tableName = tableNames[t];
								tc.tableName = tableName;
								fields = tableFields == null ? null : (Vector<String>) tableFields.get(tableName);
								if (fields == null) {
									fields = GM.listColumnNames(dsName, tableName);
								}
								tc.fields = fields;
								try {
									tc.fkMap = getTableFKMap(ds, null, tableName, con);
								} catch (Throwable e) {
									tc.fkMap = null;
								}
							}
						}
					}
					if (stoped) {
						return;
					}
				} finally {
					labelMsg.setVisible(false);
				}
				SwingUtilities.invokeLater(new Thread() {
					public void run() {
						rebuildTree(dsNames, dsList);
					}
				});
			}
		};
		dbThread.start();
	}

	/**
	 * 重新构造树控件
	 * 
	 * @param dsNames 数据源名称
	 * @param dsList  表配置列表
	 */
	private synchronized void rebuildTree(List<String> dsNames, List<ArrayList<TableConfig>> dsList) {
		root = new IconTreeNode(mm.getMessage("panelselectfield.dsname"));
		if (dsList != null && dsNames != null) {
			IconTreeNode dsNode, tableNode, fieldNode, pkNode;
			Map<String, String> fkMap;
			TableConfig tc;
			String dsName, fieldName;
			List<TableConfig> tableList;
			Vector<String> fields;
			for (int i = 0; i < dsNames.size(); i++) {
				dsName = (String) dsNames.get(i);
				dsNode = new IconTreeNode(dsName);
				root.add(dsNode);
				if (dsList.size() <= i)
					continue;
				tableList = (ArrayList<TableConfig>) dsList.get(i);
				for (int t = 0; t < tableList.size(); t++) {
					tc = tableList.get(t);
					tableNode = new IconTreeNode(tc.tableName, IconTreeNode.TYPE_TABLE);
					dsNode.add(tableNode);
					fields = tc.fields;
					if (fields != null) {
						fkMap = tc.fkMap;
						for (int c = 0; c < fields.size(); c++) {
							fieldName = fields.get(c) == null ? "" : (String) fields.get(c);
							fieldNode = new IconTreeNode(fieldName, IconTreeNode.TYPE_COLUMN);
							tableNode.add(fieldNode);
							if (fkMap != null) {
								Object pkName = fkMap.get(fieldName);
								if (pkName != null) {
									pkNode = new IconTreeNode((String) pkName, IconTreeNode.TYPE_TABLE);
									fieldNode.add(pkNode);
								}
							}
						}
					}
					treeModel.nodeStructureChanged(tableNode);
					treeModel.nodeStructureChanged(dsNode);
				}
			}
		}
		treeModel = new DefaultTreeModel(root);
		mTree.setModel(treeModel);
		mTree.setCellRenderer(new IconTreeRender());
	}

	/**
	 * 表配置
	 */
	class TableConfig {
		/**
		 * 表名
		 */
		String tableName;
		/**
		 * 字段列表
		 */
		Vector<String> fields;
		/**
		 * 外键映射
		 */
		Map<String, String> fkMap;

		public TableConfig() {
		}
	}

	/**
	 * 取表的字段名列表
	 * 
	 * @param con
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Vector<String>> getTableFields(Connection con) throws Exception {
		DatabaseMetaData md = con.getMetaData();
		ResultSet rs = null;
		Map<String, Vector<String>> tableFields = new LinkedHashMap<String, Vector<String>>();
		try {
			rs = md.getColumns(null, null, null, "%");
			while (rs.next()) {
				String tableName = rs.getString("TABLE_NAME");
				Vector<String> columns = (Vector<String>) tableFields.get(tableName);
				if (columns == null) {
					columns = new Vector<String>();
					tableFields.put(tableName, columns);
				}
				String columnName = rs.getString("COLUMN_NAME");
				columns.add(columnName);
			}
		} finally {
			if (rs != null)
				rs.close();
		}
		if (tableFields.isEmpty()) {
			return null;
		}
		return tableFields;
	}

	/**
	 * 取选择的值
	 * 
	 * @return
	 */
	private String getSelectedValues() {
		Section sec = new Section();
		TreePath[] paths = mTree.getSelectionPaths();
		if (paths == null) {
			return null;
		}
		IconTreeNode node;
		String val;
		for (int i = 0; i < paths.length; i++) {
			node = (IconTreeNode) paths[i].getLastPathComponent();
			val = getNodeValue(node);
			if (val != null) {
				sec.addSection(val);
			}
		}
		if (sec.size() == 0) {
			return null;
		}
		return sec.toString();
	}

	/**
	 * 取结点的值
	 * 
	 * @param node 树结点
	 * @return
	 */
	private String getNodeValue(IconTreeNode node) {
		IconTreeNode pNode;
		String name = node.getName();
		if (StringUtils.isValidString(name)) {
			if (node.getLevel() == LEVEL_FIELD) {
				if (jCBOpt.isSelected()) {
					pNode = (IconTreeNode) node.getParent();
					if (pNode.getType() == IconTreeNode.TYPE_TABLE && StringUtils.isValidString(pNode.getName())) {
						name = pNode.getName() + "." + name;
					}
				}
			}
		}
		return name;
	}

	/**
	 * 初始化
	 */
	private void init() {
		GridBagConstraints gbc;
		this.setLayout(new GridBagLayout());
		gbc = GM.getGBC(1, 1);
		gbc.insets = new Insets(3, 8, 3, 3);
		this.add(jBAdd, gbc);
		gbc = GM.getGBC(1, 2);
		gbc.insets = new Insets(3, 3, 3, 3);
		this.add(jBCopy, gbc);
		gbc = GM.getGBC(1, 3, true);
		gbc.insets = new Insets(3, 3, 3, 3);
		this.add(jCBOpt, gbc);
		gbc = GM.getGBC(4, 1, true, true);
		gbc.gridwidth = 3;
		JScrollPane jScrollPane1 = new JScrollPane();
		this.add(jScrollPane1, gbc);
		gbc = GM.getGBC(5, 1, true);
		gbc.gridwidth = 3;
		this.add(labelMsg, gbc);
		jScrollPane1.getViewport().add(mTree);
		jBAdd.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "m_pmtundo.gif"));
		jBCopy.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "m_copy.gif"));
		jBAdd.setToolTipText(mm.getMessage("panelselectfield.addtf"));
		jBCopy.setToolTipText(mm.getMessage("panelselectfield.copyf"));
		initButton(jBAdd);
		initButton(jBCopy);
		jBAdd.addActionListener(new PanelSelectField_jBAdd_actionAdapter(this));
		jBCopy.addActionListener(new PanelSelectField_jBCopy_actionAdapter(this));

		mTree.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() != 2) {
					return;
				}
				TreePath path = mTree.getSelectionPath();
				if (path == null) {
					return;
				}
				IconTreeNode node = (IconTreeNode) path.getLastPathComponent();
				if (node.isRoot()) {
					return;
				}
				String val = getNodeValue(node);
				if (val != null) {
					add(val);
				}
			}
		});
		labelMsg.setVisible(false);
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
	 * 增加值
	 * 
	 * @param vals
	 */
	private void add(String vals) {
		if (vals == null || GVSpl.splEditor == null || !(GV.appSheet instanceof SheetSpl)) {
			return;
		}
		if (GV.isCellEditing) {
			ContentPanel cp = GVSpl.splEditor.getComponent().getContentPanel();
			if (cp.isEditing()) {
				cp.addText(vals);
			}
		} else {
			((ToolBarProperty) GV.toolBarProperty).addText(vals);
		}
	}

	/**
	 * 取表的外键映射
	 * 
	 * @param ds     数据源
	 * @param schema 模式名
	 * @param table  表名
	 * @param con    连接
	 * @return
	 * @throws Throwable
	 */
	private Map<String, String> getTableFKMap(DataSource ds, String schema, String table, Connection con)
			throws Throwable {
		DatabaseMetaData md = con.getMetaData();
		String[] schemaTable = GM.getRealSchemaTable(con, schema, table);
		schema = schemaTable[0];
		table = schemaTable[1];
		ResultSet rs = null;
		Map<String, String> fkMap = new HashMap<String, String>();
		try {
			rs = md.getImportedKeys(null, schema, table);
			String pkTableName, pkColName, colName;
			while (rs.next()) {
				pkTableName = rs.getString("PKTABLE_NAME");
				pkColName = rs.getString("PKCOLUMN_NAME");
				colName = rs.getString("FKCOLUMN_NAME");
				if (!pkColName.startsWith(pkTableName + ".")) {
					pkColName = pkTableName + "." + pkColName;
				}
				fkMap.put(colName, pkColName);
			}
		} finally {
			rs.close();
		}
		return fkMap;
	}

	/**
	 * 增加事件
	 * 
	 * @param e
	 */
	void jBAdd_actionPerformed(ActionEvent e) {
		add(getSelectedValues());
	}

	/**
	 * 复制事件
	 * 
	 * @param e
	 */
	void jBCopy_actionPerformed(ActionEvent e) {
		String vals = getSelectedValues();
		if (vals == null) {
			return;
		}
		try {
			Clipboard cb = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
			cb.setContents(new StringSelection(vals), null);
		} catch (HeadlessException e1) {
		}
	}
}

class PanelSelectField_jBAdd_actionAdapter implements java.awt.event.ActionListener {
	PanelSelectField adaptee;

	PanelSelectField_jBAdd_actionAdapter(PanelSelectField adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBAdd_actionPerformed(e);
	}
}

class PanelSelectField_jBCopy_actionAdapter implements java.awt.event.ActionListener {
	PanelSelectField adaptee;

	PanelSelectField_jBCopy_actionAdapter(PanelSelectField adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCopy_actionPerformed(e);
	}
}
