package com.scudata.ide.vdb.commonvdb;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.plaf.ComponentUI;

import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.IDialogDimensionListener;
import com.scudata.ide.common.dialog.DialogInputText;
import com.scudata.ide.vdb.VDB;
import com.scudata.ide.vdb.config.ConfigOptions;
import com.scudata.ide.vdb.control.FileChooserUICN;
import com.scudata.ide.vdb.control.ImageFileView;
import com.scudata.ide.vdb.resources.IdeMessage;

public class GM {
	/**
	 * 取图标
	 * 
	 * @param filePath
	 * @return
	 */
	public static ImageIcon getImageIcon(String filePath) {
		return getImageIcon(filePath, true);
	}

	public static ImageIcon getImageIcon(String filePath, boolean showException) {
		InputStream is = null;
		try {
			byte[] bt;
			filePath = Sentence.replace(filePath, "\\", "/", 0);
			if (!filePath.startsWith("/")) {
				filePath = "/" + filePath;
			}
			is = GM.class.getResourceAsStream(filePath);
			// 先用文件名找，找不到再用小写的找
			if (is == null) {
				is = GM.class.getResourceAsStream(filePath.toLowerCase());
			}
			if (is == null) {
				throw new Exception("Failed to get image file: " + filePath);
			}
			bt = inputStream2Bytes(is);
			is.close();
			return new ImageIcon(bt);
		} catch (Exception e) {
			if (showException) {
				showException(e);
			} else {
				writeLog(e);
			}
			return null;
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}
	}

	/**
	 * 取菜单图标
	 * 
	 * @param menuText
	 * @return
	 */
	public static ImageIcon getMenuImageIcon(String menuText) {
		String iconText;
		int dot = menuText.indexOf(".");
		if (dot > 0) {
			iconText = menuText.substring(dot + 1);
		} else {
			iconText = menuText;
		}
		String iconPath = GC.IMAGES_PATH + "m_" + iconText.toLowerCase() + ".gif";
		return GM.getImageIcon(iconPath);
	}

	/**
	 * 获得输入流中的字节数组
	 * 
	 * @param is
	 *            输入流对象
	 * @throws Exception
	 *             输入输出错误
	 * @return 字节数组
	 */
	public static byte[] inputStream2Bytes(InputStream is) throws Exception {
		if (is == null) {
			return new byte[] {};
		}
		byte[] bytes = new byte[1024 * 10];
		int len;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while ((len = is.read(bytes)) != -1) {
			baos.write(bytes, 0, len);
		}
		return baos.toByteArray();
	}

	/**
	 * 异常处理
	 * 
	 * @param oMsg
	 */
	public static void showException(Object oMsg) {
		showException(oMsg, true);
	}

	/**
	 * 异常处理
	 * 
	 * @param e
	 *            Throwable
	 */
	public static void showException(Object oMsg, boolean canCopyMsg) {
		String msg;
		Throwable e = null;
		if (oMsg instanceof Throwable) {
			e = (Throwable) oMsg;
			if (!StringUtils.isValidString(e.getMessage())) {
				msg = e.toString();
			} else {
				msg = e.getMessage();
			}
		} else {
			msg = oMsg.toString();
		}
		if (canCopyMsg) {
			// 错误信息
			DialogInputText dit = new DialogInputText(false);
			dit.setTitle(IdeMessage.get().getMessage("gm.exinfo"));
			dit.setText(msg);
			dit.setVisible(true);
			if (e != null) {
				e.printStackTrace();
			}
		} else {
			JOptionPane.showMessageDialog(VDB.getInstance(), msg, IdeMessage.get().getMessage("gm.exinfo"),
					JOptionPane.INFORMATION_MESSAGE);
		}

		writeLog(e);
	}

	/**
	 * 写异常到日志文件
	 * 
	 * @param e
	 */
	public static void writeLog(Throwable e) {
		if (ConfigOptions.bLogException.booleanValue() && StringUtils.isValidString(ConfigOptions.sLogFileName)) {
			File f = new File(ConfigOptions.sLogFileName);
			if (!f.exists()) {
				try {
					f.createNewFile();
				} catch (Exception xf) {
					return;
				}
			}
			try {
				FileOutputStream fos = new FileOutputStream(f, true);
				PrintWriter pw = new PrintWriter(fos);
				pw.println(new java.util.Date());
				e.printStackTrace(pw);
				fos.flush();
				pw.close();
			} catch (Exception logError) {
				return;
			}
		}
	}

	/**
	 * 获取相对于start.home到绝对路径
	 * 
	 * @param path
	 * @return
	 */
	public static String getAbsolutePath(String path) {
		String home = System.getProperty("start.home");
		return getAbsolutePath(path, home);
	}

	/**
	 * 获取相对于home的绝对路径
	 * 
	 * @param path
	 * @param home
	 * @return
	 */
	public static String getAbsolutePath(String path, String home) {
		File f = new File(path);
		if (f.exists()) {
			return path;
		}
		f = new File(home, path);
		return f.getAbsolutePath();
	}

	public static void initDialog(Window dlg, JButton okButton, JButton cancelButton) {
		setDialogDefaultButton(dlg, okButton, cancelButton);
		centerWindow(dlg);
	}

	/**
	 * 设置对话框的默认确定和取消按钮
	 * 
	 * @param dlg
	 *            JDialog，要设置的对话框
	 * @param okButton
	 *            JButton，回车时执行的按钮
	 * @param cancelButton
	 *            JButton，Escape时执行的按钮
	 */
	public static void setDialogDefaultButton(Window dlg, final JButton okButton, final JButton cancelButton) {
		JRootPane pane = null;
		if (dlg instanceof JDialog) {
			pane = ((JDialog) dlg).getRootPane();
			((JDialog) dlg).setResizable(false);
		} else if (dlg instanceof JFrame) {
			pane = ((JFrame) dlg).getRootPane();
		}
		pane.setDefaultButton(okButton);
		okButton.requestFocus();

		AbstractAction cancelAction = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent event) {
				cancelButton.doClick();
			}
		};

		Object o = new String("esc");
		pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), o);
		pane.getActionMap().put(o, cancelAction);
		dlg.setSize(dlg.getWidth() + 10, dlg.getHeight() + 10);

	}

	/**
	 * 将窗口居中
	 * 
	 * @param w
	 *            Window 要居中的窗口
	 */
	public static void centerWindow(Component w) {
		if (!loadWindowDimension(w)) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			Dimension winSize = w.getSize();
			if (winSize.height > screenSize.height) {
				winSize.height = screenSize.height;
			}
			if (winSize.width > screenSize.width) {
				winSize.width = screenSize.width;
			}
			w.setLocation((screenSize.width - winSize.width) / 2, (screenSize.height - winSize.height) / 2);
		}
	}

	private final static String STRING_DIMENSION = ".dimension";
	private final static String CLASS_NAME_SEP = "$";
	private static IDialogDimensionListener ddListener = null;
	public static IDialogDimensionListener getDDListener(){
		if(ddListener!=null){
			return ddListener;
		}
		ddListener = new IDialogDimensionListener(){
			public void saveWindowDimension(Component dlg) {
				try {
					Dimension d = dlg.getSize();
					String width = String.valueOf(d.getWidth());
					String height = String.valueOf(d.getHeight());
					int index;
					index = width.indexOf(".");
					if (index > 0) {
						width = width.substring(0, index);
					}
					index = height.indexOf(".");
					if (index > 0) {
						height = height.substring(0, index);
					}
					String className = dlg.getClass().getName();
					index = className.indexOf(CLASS_NAME_SEP);
					if (index > -1) {
						className = className.substring(0, index);
					}
					ConfigOptions.dimensions.put(className + STRING_DIMENSION,
							width + "," + height + "," + String.valueOf(dlg.getX()) + "," + String.valueOf(dlg.getY()));
					ConfigOptions.save();
				} catch (Throwable ex) {
				}
			}

			public boolean loadWindowDimension(Component dlg) {
				String className = dlg.getClass().getName();
				int index = className.indexOf(CLASS_NAME_SEP);
				if (index > -1) {
					className = className.substring(0, index);
				}
				try {
					if (ConfigOptions.bWindowSize.booleanValue()) {
						String key = className + STRING_DIMENSION;
						String dimension = ConfigOptions.dimensions.get(key);
						if (StringUtils.isValidString(dimension)) {
							String size[] = dimension.split(",");
							dlg.setSize(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
							dlg.setLocation(Integer.parseInt(size[2]), Integer.parseInt(size[3]));
							return true;
						}
					}
				} catch (Throwable ex) { // 读不到无所谓了
				}
				return false;
			}};
		return ddListener;
	}
	
	public static boolean loadWindowDimension(Component jc) {
		return getDDListener().loadWindowDimension(jc);
	}

	/**
	 * 保存窗口的位置和大小信息
	 * 
	 * @param dlg
	 *            JDialog
	 */
	public static void saveWindowDimension(Component dlg) {
		getDDListener().saveWindowDimension(dlg);
	}

	/**
	 * 获取GridBag布局对象
	 * 
	 * @param row
	 *            int, 对象所处行号
	 * @param col
	 *            int, 对象所处列号
	 * @param hFill
	 *            boolean,该对象是否行填充
	 * @return GridBagConstraints
	 */
	public static GridBagConstraints getGBC(int row, int col, boolean hFill, boolean vFill, int hGap) {
		return getGBC(row, col, hFill, vFill, hGap, 3);
	}

	public static GridBagConstraints getGBC(int row, int col, boolean hFill, boolean vFill, int hGap, int vGap) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = col;
		gbc.gridy = row;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(vGap, hGap, vGap, hGap);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		if (hFill) {
			gbc.weightx = 1;
		}
		if (vFill) {
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weighty = 1;
		}
		return gbc;
	}

	public static GridBagConstraints getGBC(int row, int col, boolean hFill, boolean vFill) {
		return getGBC(row, col, hFill, vFill, 8);
	}

	public static GridBagConstraints getGBC(int row, int col, boolean hFill) {
		return getGBC(row, col, hFill, false);
	}

	public static GridBagConstraints getGBC(int row, int col) {
		return getGBC(row, col, false);
	}

	/**
	 * 获取控件在窗口中的绝对坐标
	 * 
	 * @param c
	 *            Component
	 * @param isGetX
	 *            boolean
	 * @return int
	 */
	public static int getAbsolutePos(Component c, boolean isGetX) {
		if (c == null) {
			return 0;
		} else {
			return isGetX ? c.getX() : c.getY() + getAbsolutePos(c.getParent(), isGetX);
		}
	}

	/**
	 * 打开文件选择对话框，获取指定扩展名类型的文件
	 * 
	 * @param fileExts
	 *            String，初始的扩展名类型，用逗号分开多种类型
	 * @param currentDirectory
	 *            String，初始的文件路径
	 * @return File
	 */
	public static File dialogSelectFile(String fileExts) {
		return dialogSelectFile(fileExts, ConfigOptions.sLastDirectory, "", "");
	}

	public static File dialogSelectFile(String fileExts, String currentDirectory, String title, String oldFileName) {
		return (File) dialogSelectFiles(fileExts, currentDirectory, title, new File(oldFileName), false);
	}

	public static File[] dialogSelectFiles(String fileExts) {
		return dialogSelectFiles(fileExts, ConfigOptions.sLastDirectory, "", null);
	}

	public static File[] dialogSelectFiles(String fileExts, String currentDirectory, String title, File[] oldFiles) {
		return (File[]) dialogSelectFiles(fileExts, currentDirectory, title, oldFiles, true);
	}

	private static Object dialogSelectFiles(String fileExts, String currentDirectory, String title, Object oldFiles,
			boolean multiSelect) {
		return dialogSelectFiles(fileExts, currentDirectory, title, oldFiles, multiSelect, null,
				VDB.getInstance());
	}

	public static Object dialogSelectFiles(String fileExts, String currentDirectory, String buttonText, Object oldFiles,
			boolean multiSelect, String dialogTitle, Component parent) {
		if (currentDirectory == null) {
			currentDirectory = ConfigOptions.sLastDirectory;
		}
		fileExts = fileExts.toLowerCase();

		JFileChooser chooser = new JFileChooser(currentDirectory) {
			private static final long serialVersionUID = 1L;

			protected void setUI(ComponentUI newUI) {
				super.setUI(new FileChooserUICN(this));
			}
		};
		chooser.setFileView(new ImageFileView());
		chooser.setMultiSelectionEnabled(multiSelect);
		String[] exts = fileExts.split(",");
		for (int i = exts.length - 1; i >= 0; i--) {
			chooser.setFileFilter(getFileFilter("." + exts[i], "*." + exts[i]));
		}

		if (multiSelect) {
			if (oldFiles != null) {
				chooser.setSelectedFiles((File[]) oldFiles);
			}
		} else {
			chooser.setSelectedFile((File) oldFiles);
		}

		if (StringUtils.isValidString(dialogTitle)) {
			chooser.setDialogTitle(dialogTitle);
		}

		int r;
		if (StringUtils.isValidString(buttonText)) {
			r = chooser.showDialog(parent, buttonText);
		} else {
			r = chooser.showOpenDialog(parent);
		}

		if (r == JFileChooser.APPROVE_OPTION) {
			ConfigOptions.sLastDirectory = chooser.getSelectedFile().getParent();
			if (multiSelect) {
				return chooser.getSelectedFiles();
			} else {
				String fileExt = chooser.getFileFilter().getDescription();
				int dot = fileExt.indexOf(".");
				if (dot < 0) {
					fileExt = "";
				} else {
					fileExt = fileExt.substring(dot);
				}

				String path = chooser.getSelectedFile().getAbsolutePath();
				// System.out.println("PATH: " + path);
				boolean fileHasExt = path.toLowerCase().endsWith(fileExt);

				if (!fileHasExt && fileExt.startsWith(".")) {
					File fWithExt = new File(path + fileExt);
					return fWithExt;
				}
				// System.out.println("FILE: " +
				// chooser.getSelectedFile().getName());
				return chooser.getSelectedFile();
			}
		}
		return null;
	}

	public static javax.swing.filechooser.FileFilter getFileFilter(final String extName, final String desc) {
		return new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				String s = f.getName().toLowerCase();
				return f.isDirectory() || s.endsWith(extName);
			}

			public String getDescription() {
				return desc;
			}
		};
	}

	/**
	 * 弹出对话框，选择一个文件路径
	 * 
	 * @param currentDirectory
	 *            String，对话框初始的路径
	 * @return String,用户选择的路径，取消返回null
	 */
	public static String dialogSelectDirectory(String currentDirectory) {
		JFileChooser chooser = new JFileChooser(currentDirectory);
		javax.swing.filechooser.FileFilter dirFilter = new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory();
			}

			public String getDescription() {
				return "";
			}
		};
		chooser.setFileFilter(dirFilter);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (chooser.showOpenDialog(VDB.getInstance()) == JFileChooser.APPROVE_OPTION) {
			ConfigOptions.sLastDirectory = chooser.getSelectedFile().getAbsolutePath();
			return chooser.getSelectedFile().getAbsolutePath();
		}
		return null;
	}

	public static boolean canSaveAsFile(String otherFile) {
		if (!StringUtils.isValidString(otherFile)) {
			JOptionPane.showMessageDialog(VDB.getInstance(), IdeMessage.get().getMessage("file.inputfilename"));
			return false;
		}

		File saveFile = new File(otherFile);
		if (saveFile.exists()) {
			int r = JOptionPane.showConfirmDialog(VDB.getInstance(),
					IdeMessage.get().getMessage("file.coverexistfile", otherFile),
					IdeMessage.get().getMessage("public.prompt"), JOptionPane.OK_CANCEL_OPTION);
			if (r == JOptionPane.CANCEL_OPTION) {
				return false;
			}
		}
		return true;
	}

	public static String getFileExts() {
		String fileExts = GC.FILE_VDB;
		return fileExts;
	}

}
