package com.scudata.ide.spl.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.VFlowLayout;
import com.scudata.ide.spl.GMSpl;
import com.scudata.ide.spl.base.TableExtLibs;
import com.scudata.ide.spl.resources.IdeSplMessage;
import com.scudata.resources.AppMessage;

/**
 * 外部库设置对话框
 *
 */
public class DialogExtLibs extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	/**
	 * 集算器资源管理器
	 */
	private MessageManager mm = IdeSplMessage.get();
	/**
	 * 确认按钮
	 */
	private JButton jBOK = new JButton();
	/**
	 * 取消按钮
	 */
	private JButton jBCancel = new JButton();
	/**
	 * 外部库路径编辑框
	 */
	private JTextField jTFExtLibsPath = new JTextField();
	/**
	 * 外部库路径选择按钮
	 */
	private JButton jBExtLibsPath = new JButton();
	/**
	 * 外部库表滚动面板控件
	 */
	private TableExtLibs tableNames;
	/**
	 * 退出选项
	 */
	private int m_option = JOptionPane.CANCEL_OPTION;
	/**
	 * 已经存在的外部库名称列表
	 */
	private Vector<String> existNames = new Vector<String>();
	/**
	 * 外部库目录名称列表
	 */
	private Vector<String> dirNames = new Vector<String>();

	/**
	 * 构造函数
	 * 
	 * @param config
	 *            集算器配置
	 * @param frame
	 *            父窗口
	 * @param extLibsPath
	 *            外部库根路径
	 * @param extLibs
	 *            配置的外部库列表
	 */
	public DialogExtLibs(RaqsoftConfig config, Frame frame, String extLibsPath,
			List<String> extLibs) {
		super(frame, IdeSplMessage.get().getMessage("dialogselectnames.title"),
				true);
		tableNames = new TableExtLibs();
		try {
			init();
			setConfig(config, extLibsPath, extLibs);
			setSize(500, 450);
			resetText();
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
			setResizable(true);
		} catch (Exception e) {
			GM.showException(e);
		}
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
	 * 取外部库的根目录
	 * 
	 * @return
	 */
	public String getExtLibsPath() {
		return jTFExtLibsPath.getText();
	}

	/**
	 * 外部库列表
	 * 
	 * @return
	 */
	public List<String> getExtLibs() {
		String[] selectedNames = tableNames.getSelectedNames(null);
		if (selectedNames != null && selectedNames.length == 0)
			return null;
		List<String> extLibs = new ArrayList<String>();
		for (String name : selectedNames) {
			extLibs.add(name);
		}
		return extLibs;
	}

	/**
	 * 设置外部库配置
	 * 
	 * @param config
	 *            集算器配置
	 * @param extLibsPath
	 *            外部库根目录
	 * @param extLibs
	 *            外部库列表
	 */
	private void setConfig(RaqsoftConfig config, String extLibsPath,
			List<String> extLibs) {
		jTFExtLibsPath.setText(extLibsPath);
		List<String> libs = extLibs;
		if (libs != null)
			existNames.addAll(libs);
		setExtLibsPath(false);
	}

	/**
	 * 设置外部库根目录
	 * 
	 * @param showException
	 */
	private synchronized void setExtLibsPath(boolean showException) {
		dirNames.clear();
		String extLibsPath = jTFExtLibsPath.getText();
		if (!StringUtils.isValidString(extLibsPath)) {
			tableNames.setExistNames(null);
			tableNames.setExistColor(true);
			tableNames.setNames(null, false, true);
			return;
		}
		extLibsPath = GMSpl.getAbsolutePath(extLibsPath);
		File extLibsDir = new File(extLibsPath);
		if (!extLibsDir.exists() || !extLibsDir.isDirectory()) {
			if (showException)
				JOptionPane.showMessageDialog(GV.appFrame, AppMessage.get()
						.getMessage("configutil.noextpath"));
			else
				return;
		}
		File[] subDirs = extLibsDir.listFiles();
		if (subDirs != null) {
			for (File sd : subDirs) {
				if (sd.isDirectory()) {
					if (isExtLibrary(sd)) {
						dirNames.add(sd.getName());
					}
				}
				// 根目录下的jar已经在根目录加载过，这里不处理了
			}
		}
		int existSize = existNames.size();
		for (int i = existSize - 1; i >= 0; i--) {
			if (!dirNames.contains(existNames.get(i))) {
				existNames.remove(i);
			}
		}

		tableNames.setExistNames(existNames);
		tableNames.setExistColor(true);
		tableNames.setNames(dirNames, false, true);
	}

	/**
	 * 是否外部库
	 * 
	 * @param dir
	 * @return
	 */
	private boolean isExtLibrary(File dir) {
		File[] fs = dir.listFiles();
		List<File> jars = new ArrayList<File>();
		for (File f : fs) {
			if (f.getName().endsWith(".jar")) {
				jars.add(f);
			}
		}
		Pattern p = Pattern
				.compile("com/scudata/lib/(\\w+)/functions.properties");
		for (File f : jars) {
			JarFile jf = null;
			try {
				jf = new JarFile(f);
				Enumeration<JarEntry> jee = jf.entries();
				while (jee.hasMoreElements()) {
					JarEntry je = jee.nextElement();
					Matcher m = p.matcher(je.getName());
					if (m.matches()) {
						return true;
					}
				}
			} catch (Exception e) {
				continue;
			} finally {
				if (jf != null)
					try {
						jf.close();
					} catch (IOException e) {
					}
			}
		}
		return false;
	}

	/**
	 * 重置语言资源
	 */
	private void resetText() {
		jBOK.setText(mm.getMessage("button.ok")); // 确定(O)
		jBCancel.setText(mm.getMessage("button.cancel")); // 取消(C)
		jBExtLibsPath.setText(IdeCommonMessage.get().getMessage(
				"dialogoptions.select")); // 选择
	}

	/**
	 * 初始化控件
	 */
	private void init() {
		this.getContentPane().setLayout(new BorderLayout());
		JPanel panelEast = new JPanel();
		VFlowLayout vf = new VFlowLayout();
		vf.setAlignment(VFlowLayout.TOP);
		vf.setHorizontalFill(true);
		panelEast.setLayout(vf);
		panelEast.add(jBOK);
		panelEast.add(jBCancel);
		jBOK.addActionListener(this);
		jBCancel.addActionListener(this);
		jBOK.setText("确定(O)");
		jBOK.setMnemonic('O');
		jBCancel.setText("取消(C)");
		jBCancel.setMnemonic('C');
		this.getContentPane().add(panelEast, BorderLayout.EAST);
		JPanel panelNorth = new JPanel(new GridBagLayout());
		JLabel jLExtLibsPath = new JLabel(IdeCommonMessage.get().getMessage(
				"dialogoptions.extlibspath"));
		panelNorth.add(jLExtLibsPath, GM.getGBC(0, 0));
		panelNorth.add(jTFExtLibsPath, GM.getGBC(0, 1, true));
		panelNorth.add(jBExtLibsPath, GM.getGBC(0, 2));
		JPanel panelCenter = new JPanel(new BorderLayout());
		panelCenter.add(panelNorth, BorderLayout.NORTH);
		panelCenter.add(tableNames, BorderLayout.CENTER);
		JLabel labelTips = new JLabel(IdeSplMessage.get().getMessage(
				"dialogselectnames.tips"));
		labelTips.setForeground(Color.BLUE);
		panelCenter.add(labelTips, BorderLayout.SOUTH);
		this.getContentPane().add(panelCenter, BorderLayout.CENTER);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				close();
			}
		});

		jTFExtLibsPath.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					e.consume();
					jBOK.requestFocusInWindow();
					setExtLibsPath(true);
				}
			}
		});
		jBExtLibsPath.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String oldDir = jTFExtLibsPath.getText();
				if (StringUtils.isValidString(oldDir)) {
					File f = new File(oldDir);
					if (f != null && f.exists())
						oldDir = f.getParent();
				}
				if (!StringUtils.isValidString(oldDir))
					oldDir = GV.lastDirectory;
				String newPath = GM.dialogSelectDirectory(oldDir, getParent());
				if (newPath != null) {
					jTFExtLibsPath.setText(newPath);
					setExtLibsPath(true);
				}
			}
		});
	}

	/**
	 * 关闭窗口
	 */
	private void close() {
		GM.setWindowDimension(this);
		dispose();
	}

	/**
	 * 按钮事件
	 */
	public void actionPerformed(ActionEvent e) {
		Object c = e.getSource();
		if (c == null) {
			return;
		}
		if (c.equals(jBOK)) {
			m_option = JOptionPane.OK_OPTION;
			close();
		} else if (c.equals(jBCancel)) {
			close();
		}
	}
}
