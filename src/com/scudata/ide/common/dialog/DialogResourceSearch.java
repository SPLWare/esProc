package com.scudata.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.scudata.common.StringUtils;
import com.scudata.ide.common.GM;

/**
 * 搜索资源，内部调试使用，用于查找指定类从哪个包加载的
 *
 */
public class DialogResourceSearch extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	/**
	 * 类名文本框
	 */
	private JTextField textClass = new JTextField();
	/**
	 * 搜索按钮
	 */
	private JButton buttonSearch = new JButton();
	/**
	 * 取消按钮
	 */
	private JButton buttonCancel = new JButton();
	/**
	 * 结果文本框
	 */
	private JTextArea textResults = new JTextArea();

	/**
	 * 构造函数
	 * 
	 * @param frame
	 *            父组件
	 */
	public DialogResourceSearch(JFrame frame) {
		super(frame, "Resource search", true);
		setSize(600, 300);
		init();
		GM.setDialogDefaultButton(this, buttonSearch, buttonCancel);
	}

	/**
	 * 初始化
	 */
	private void init() {
		this.getContentPane().setLayout(new BorderLayout());
		JPanel panelCenter = new JPanel(new GridBagLayout());
		this.getContentPane().add(panelCenter, BorderLayout.CENTER);
		panelCenter.add(new JLabel("Class name"), GM.getGBC(1, 1));
		panelCenter.add(textClass, GM.getGBC(1, 2, true));
		panelCenter.add(buttonSearch, GM.getGBC(1, 3));
		panelCenter.add(new JLabel("Search results"), GM.getGBC(2, 1));
		GridBagConstraints gbc = GM.getGBC(3, 1, true, true);
		gbc.gridwidth = 3;
		panelCenter.add(new JScrollPane(textResults), gbc);
		buttonSearch.setText("Search");
		buttonSearch.setMnemonic('S');
		buttonSearch.addActionListener(this);
		buttonCancel.addActionListener(this);
		textResults.setLineWrap(true);
	}

	/**
	 * 执行查找
	 */
	public void actionPerformed(ActionEvent e) {
		Object c = e.getSource();
		if (buttonSearch == c) {
			textResults.setText(null);
			String clzName = textClass.getText();
			if (!StringUtils.isValidString(clzName))
				return;
			try {
				String result = searchResource(clzName);
				textResults.setText(result);
			} catch (Exception ex) {
				GM.showException(ex);
				ex.printStackTrace();
			}
		} else if (buttonCancel == c) {
			dispose();
		}
	}

	/**
	 * 查找类的包路径
	 * 
	 * @param clzName
	 * @return
	 * @throws IOException
	 */
	public static String searchResource(String clzName) throws IOException {
		if (!StringUtils.isValidString(clzName)) {
			return null;
		}
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		List<String> resources = new ArrayList<String>();
		searchResource(cl, clzName, resources);
		searchResource(cl, clzName.concat(".class"), resources);
		searchResource(cl, clzName.replace('.', '/'), resources);
		try {
			Class clz = Class.forName(clzName);
			String src = searchResource(clz);
			if (!resources.contains(src))
				resources.add(src);
		} catch (Exception ex) {
		}

		try {
			Class clz = Class.forName(clzName.concat(".class"));
			String src = searchResource(clz);
			if (!resources.contains(src))
				resources.add(src);
		} catch (Exception ex) {
		}

		StringBuffer buf = new StringBuffer();
		for (String src : resources) {
			if (buf.length() > 0)
				buf.append("\r\n");
			buf.append(src);
		}
		return buf.toString();
	}

	/**
	 * 查找类的包路径
	 * 
	 * @param cl
	 * @param clzName
	 * @param resources
	 * @throws IOException
	 */
	private static void searchResource(ClassLoader cl, String clzName,
			List<String> resources) throws IOException {
		Enumeration<URL> urls = cl.getResources(clzName);
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			String path = url.getPath();
			if (StringUtils.isValidString(path)) {
				if (!resources.contains(path))
					resources.add(path);
			}
		}
	}

	/**
	 * 获取类所有的路径
	 * 
	 * @param cls
	 * @return
	 */
	public static String searchResource(final Class cls) {
		URL result = null;
		final String clsAsResource = cls.getName().replace('.', '/')
				.concat(".class");
		final ProtectionDomain pd = cls.getProtectionDomain();
		if (pd != null) {
			final CodeSource cs = pd.getCodeSource();
			if (cs != null)
				result = cs.getLocation();
			if (result != null) {
				if ("file".equals(result.getProtocol())) {
					try {
						if (result.toExternalForm().endsWith(".jar")
								|| result.toExternalForm().endsWith(".zip"))
							result = new URL("jar:"
									.concat(result.toExternalForm())
									.concat("!/").concat(clsAsResource));
						else if (new File(result.getFile()).isDirectory())
							result = new URL(result, clsAsResource);
					} catch (MalformedURLException ignore) {
					}
				}
			}
		}
		if (result == null) {
			final ClassLoader clsLoader = cls.getClassLoader();
			result = clsLoader != null ? clsLoader.getResource(clsAsResource)
					: ClassLoader.getSystemResource(clsAsResource);
		}
		return result.getPath();
	}
}