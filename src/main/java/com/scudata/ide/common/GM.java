package com.scudata.ide.common;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.accessibility.AccessibleContext;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;

import com.scudata.app.common.Section;
import com.scudata.app.config.ConfigUtil;
import com.scudata.cellset.datamodel.NormalCell;
import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.DBConfig;
import com.scudata.common.DBInfo;
import com.scudata.common.DBSession;
import com.scudata.common.IntArrayList;
import com.scudata.common.Logger;
import com.scudata.common.Matrix;
import com.scudata.common.MessageManager;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.dm.DBObject;
import com.scudata.dm.Env;
import com.scudata.dm.KeyWord;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.ide.common.dialog.DialogInputText;
import com.scudata.ide.common.dialog.DialogMaxmizable;
import com.scudata.ide.common.function.FuncInfo;
import com.scudata.ide.common.function.FuncOption;
import com.scudata.ide.common.function.FuncParam;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.AllPurposeEditor;
import com.scudata.ide.common.swing.AllPurposeRenderer;
import com.scudata.ide.common.swing.DateChooser;
import com.scudata.ide.common.swing.JComboBoxEx;
import com.scudata.ide.common.swing.JOptionPaneEx;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.util.Variant;

/**
 * Global Method
 *
 */
public class GM {

	/**
	 * Set dock icon for Mac OS
	 * 
	 * @param image
	 * @throws Exception
	 */
	public static void setMacOSDockIcon(Image image) throws Exception {
		if (image == null) {
			return;
		}
		Class clz = Class.forName("com.apple.eawt.Application");
		java.lang.reflect.Method m = clz.getMethod("getApplication");
		Object obj = m.invoke(null);
		m = clz.getMethod("setDockIconImage", Image.class);
		m.invoke(obj, image);
	}

	/**
	 * Set the flag bit by position
	 * 
	 * @param value   long The long integer to set the flag
	 * @param pos     byte The bit to be set, the range is 0 ~ 63
	 * @param enabled boolean Whether to set to 1. True is set to 1, otherwise it is
	 *                set to 0.
	 * @return long
	 */
	public static long setBitByPos(long value, byte pos, boolean enabled) {
		if (enabled) {
			value |= (long) 0x01 << pos;
		} else {
			value &= ~((long) 0x01 << pos);
		}
		return value;
	}

	/**
	 * Get whether the flag is set by position.
	 * 
	 * @param value long The long integer to set the flag
	 * @param pos   byte The bit to be set, the range is 0 ~ 63
	 * @return boolean whether the flag is set by position
	 */
	public static boolean getBitByPos(long value, byte pos) {
		return (value & ((long) 0x01 << pos)) != 0;
	}

	/**
	 * Given code comparison table, find out the display value of codeKey
	 * 
	 * @param codeKey byte
	 * @param code    Vector
	 * @param disp    Vector
	 * @return String
	 */
	public static String getDispText(byte codeKey, Vector code, Vector disp) {
		for (int i = 0; i < code.size(); i++) {
			byte codeVal = ((Byte) code.get(i)).byteValue();
			if (codeKey == codeVal) {
				return (String) disp.get(i);
			}
		}
		return "";
	}

	/**
	 * Get the string with the trailing \0 removed.
	 * 
	 * @param val
	 * @return
	 */
	public static Object getOptionTrimChar0Value(Object val) {
		if (!ConfigOptions.bAutoTrimChar0) {
			return val;
		}
		if (val != null && val instanceof String) {
			val = getOptionTrimChar0String((String) val);
		}
		return val;
	}

	/**
	 * Get the string with the trailing \0 removed.
	 * 
	 * @param str
	 * @return
	 */
	public static String getOptionTrimChar0String(String str) {
		if (!ConfigOptions.bAutoTrimChar0) {
			return str;
		}
		if (str == null || str.length() == 0)
			return str;
		if ('\0' == str.charAt(str.length() - 1)) {
			return str.substring(0, str.length() - 1);
		}
		return str;
	}

	/**
	 * Convert sequence, pmt, record and other types of objects into displayed
	 * string
	 * 
	 * @param val Object
	 * @return String
	 */
	public static String renderValueText(Object val) {
		if (val == null) {
			return "";
		}
		/*
		 * In order to distinguish between null values and byte[], modify the
		 * display
		 */
		if (val instanceof byte[]) {
			return "(blob)";
		}
		try {
			return renderText(val);
		} catch (Exception x) {
			return "Error#" + x.getMessage();
		}
	}

	/**
	 * Convert sequence, pmt, record and other types of objects into displayed
	 * string
	 * 
	 * @param val
	 * @return
	 */
	private static String renderText(Object val) {
		if (val == null) {
			return "";
		}
		String dispText;
		if (val instanceof Sequence) {
			Sequence s = (Sequence) val;
			int c = Math.min(s.length(), ConfigOptions.iSequenceDispMembers);
			StringBuffer sb = new StringBuffer();
			sb.append("[");
			for (int i = 1; i <= c; i++) {
				if (i > 1) {
					sb.append(",");
				}
				Object v = s.get(i);
				sb.append(renderText(v)); // , objects
			}
			if (s.length() > ConfigOptions.iSequenceDispMembers) {
				sb.append(", ...");
			}
			sb.append("]");
			dispText = sb.toString();
		} else if (val instanceof Record) {
			Record r = (Record) val;
			/* r.getPKValue may return null, call r.value() instead */
			return renderText(r.value());
		} else {
			dispText = Variant.toString(val);
		}
		return dispText;
	}

	/**
	 * Get the displayed value of the record
	 * 
	 * @param r       Record
	 * @param context
	 * @return
	 */
	public static String getRecordDispName(Record r, Context context) {
		int mkIndex[] = r.dataStruct().getPKIndex();
		if (mkIndex == null || mkIndex.length == 0) {
			int fieldCount = r.dataStruct().getFieldCount();
			if (fieldCount == 0)
				return null;
			Object temp = r.getFieldValue(0);
			if (temp == null) {
				return null;
			} else if (temp instanceof Record) {
				return getRecordDispName((Record) temp, context);
			} else {
				return renderValueText(temp);
			}
		}
		String dispName = "";
		for (int i = 0; i < mkIndex.length; i++) {
			Object temp = r.getFieldValue(mkIndex[i]);
			if (temp == null) {
				continue;
			}
			if (StringUtils.isValidString(dispName)) {
				dispName += ",";
			}
			if (temp instanceof Record) {
				dispName += getRecordDispName((Record) temp, context);
			} else {
				dispName += renderValueText(temp);
			}
		}
		return dispName;
	}

	/**
	 * Get the operating system of the current IDE.
	 * 
	 * @return byte GC.OS_WINDOWS,GC.OS_MAC,GC.OS_OTHER
	 */
	public static byte getOperationSytem() {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.indexOf("windows") > -1) {
			return GC.OS_WINDOWS;
		} else if (osName.indexOf("mac") > -1) {
			return GC.OS_MAC;
		}
		return GC.OS_OTHER;
	}

	/**
	 * Whether the current operating system is windows
	 * 
	 * @return
	 */
	public static boolean isWindowsOS() {
		return getOperationSytem() == GC.OS_WINDOWS;
	}

	/**
	 * Whether the current operating system is MAC
	 * 
	 * @return
	 */
	public static boolean isMacOS() {
		return getOperationSytem() == GC.OS_MAC;
	}

	/**
	 * Whether the current locale is Chinese.
	 * 
	 * @return
	 */
	public static boolean isChineseLanguage() {
		Locale local = Locale.getDefault();
		return "zh".equalsIgnoreCase(local.getLanguage());
	}

	/**
	 * File filter based on file suffix names
	 * 
	 * @param extNames
	 * @param desc
	 * @return
	 */
	public static javax.swing.filechooser.FileFilter getFileFilter(
			final String[] extNames, final String desc) {
		return new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				String s = f.getName().toLowerCase();
				if (f.isDirectory())
					return true;
				for (String extName : extNames)
					if (s.endsWith(extName))
						return true;
				return false;
			}

			public String getDescription() {
				return desc;
			}
		};
	}

	/**
	 * Get the names of all connected data sources
	 * 
	 * @return
	 */
	public static Vector<String> getActiveDSNames() {
		DataSourceListModel dsList = GV.dsModel;
		Vector<String> names = dsList.listNames();
		DataSource ds;
		Vector<String> actives = new Vector<String>();
		for (int i = 0; i < names.size(); i++) {
			ds = dsList.getDataSource((String) names.get(i));
			if (!ds.isClosed()) {
				actives.add(ds.getName());
			}
		}
		return actives;
	}

	/**
	 * Commonly used buttons
	 */
	/** Open file **/
	public static final byte B_OPEN = 0;
	/** Add **/
	public static final byte B_ADD = 1;
	/** Delete **/
	public static final byte B_DEL = 2;
	/** Move up **/
	public static final byte B_UP = 3;
	/** Move down **/
	public static final byte B_DOWN = 4;
	/** Copy **/
	public static final byte B_COPY = 5;
	/** Paste **/
	public static final byte B_PASTE = 6;

	/**
	 * Get frequently used buttons
	 * 
	 * @param type Constants defined above
	 * @return
	 */
	public static JButton getCommonIconButton(byte type) {
		String tip, sIcon;
		switch (type) {
		case B_OPEN:
			tip = IdeCommonMessage.get().getMessage("gm.open");
			sIcon = "m_open.gif";
			break;
		case B_ADD:
			tip = IdeCommonMessage.get().getMessage("gm.add");
			sIcon = "m_addrecord.gif";
			break;
		case B_DEL:
			tip = IdeCommonMessage.get().getMessage("gm.delete");
			sIcon = "m_deleterecord.gif";
			break;
		case B_UP:
			tip = IdeCommonMessage.get().getMessage("gm.rowup");
			sIcon = "m_shiftup.gif";
			break;
		case B_DOWN:
			tip = IdeCommonMessage.get().getMessage("gm.rowdown");
			sIcon = "m_shiftdown.gif";
			break;
		case B_COPY:
			tip = IdeCommonMessage.get().getMessage("gm.copy");
			sIcon = "m_copy.gif";
			break;
		case B_PASTE:
			tip = IdeCommonMessage.get().getMessage("gm.paste");
			sIcon = "m_paste.gif";
			break;
		default:
			return null;
		}
		return getIconButton(null, tip, sIcon);
	}

	/**
	 * Get an instance of the button
	 * 
	 * @param text     Button text
	 * @param tip      Tooltip text of the button
	 * @param iconName Button icon name
	 * @return
	 */
	public static JButton getIconButton(String text, String tip, String iconName) {
		JButton b = new JButton(GM.getImageIcon(GC.IMAGES_PATH + iconName));
		b.setToolTipText(tip);
		Dimension d = new Dimension(24, 24);
		b.setMinimumSize(d);
		b.setMaximumSize(d);
		b.setPreferredSize(d);
		b.setOpaque(false);
		b.setMargin(new Insets(0, 0, 0, 0));
		return b;
	}

	/**
	 * Select file dialog
	 * 
	 * @param fileExts
	 * @return
	 */
	public static File dialogSelectFile(String fileExts) {
		return dialogSelectFile(fileExts, GV.lastDirectory, "", "", GV.appFrame);
	}

	/**
	 * Select file dialog
	 * 
	 * @param fileExts
	 * @param owner
	 * @return
	 */
	public static File dialogSelectFile(String fileExts, Component owner) {
		return dialogSelectFile(fileExts, GV.lastDirectory, "", "", owner);
	}

	/**
	 * Select file dialog
	 * 
	 * @param fileExts
	 * @param useAllFileFilter
	 * @return
	 */
	public static File dialogSelectFile(String fileExts,
			boolean useAllFileFilter) {
		return (File) dialogSelectFiles(fileExts, GV.lastDirectory, "", null,
				false, null, GV.appFrame, useAllFileFilter);
	}

	/**
	 * Select file dialog
	 * 
	 * @param fileExts
	 * @param useAllFileFilter
	 * @param image
	 * @return
	 */
	public static File dialogSelectFile(String fileExts,
			boolean useAllFileFilter, Image image) {
		return (File) dialogSelectFiles(fileExts, GV.lastDirectory, "", null,
				false, null, GV.appFrame, useAllFileFilter, image);
	}

	/**
	 * Select file dialog
	 * 
	 * @param fileExts
	 * @param currentDirectory
	 * @param title
	 * @param oldFileName
	 * @param owner
	 * @return
	 */
	public static File dialogSelectFile(String fileExts,
			String currentDirectory, String title, String oldFileName,
			Component owner) {
		return dialogSelectFile(fileExts, currentDirectory, title, oldFileName,
				owner, true);
	}

	/**
	 * Select file dialog
	 * 
	 * @param fileExts
	 * @param currentDirectory
	 * @param title
	 * @param oldFileName
	 * @param owner
	 * @param useAllFileFilter Whether to use the "All files" drop-down item for the
	 *                         file type. The default is true to use.
	 * @return
	 */
	public static File dialogSelectFile(String fileExts,
			String currentDirectory, String title, String oldFileName,
			Component owner, boolean useAllFileFilter) {
		return (File) dialogSelectFiles(fileExts, currentDirectory, title,
				new File(oldFileName), false, null, owner, useAllFileFilter);
	}

	/**
	 * Select file dialog
	 * 
	 * @param fileExts
	 * @return
	 */
	public static File[] dialogSelectFiles(String fileExts) {
		return dialogSelectFiles(fileExts, GV.lastDirectory, "", null,
				GV.appFrame);
	}

	public static File[] dialogSelectFiles(String fileExts, Image image) {
		return (File[]) dialogSelectFiles(fileExts, GV.lastDirectory, "", null,
				true, null, GV.appFrame, true, image);
	}

	/**
	 * Select file dialog
	 * 
	 * @param fileExts
	 * @param currentDirectory
	 * @param title
	 * @param oldFiles
	 * @param owner
	 * @return
	 */
	public static File[] dialogSelectFiles(String fileExts,
			String currentDirectory, String title, File[] oldFiles,
			Component owner) {
		return (File[]) dialogSelectFiles(fileExts, currentDirectory, title,
				oldFiles, true, null, owner);
	}

	/**
	 * Select file dialog
	 * 
	 * @param fileExts
	 * @param currentDirectory
	 * @param buttonText
	 * @param oldFiles
	 * @param multiSelect
	 * @param dialogTitle
	 * @param parent
	 * @return
	 */
	public static Object dialogSelectFiles(String fileExts,
			String currentDirectory, String buttonText, Object oldFiles,
			boolean multiSelect, String dialogTitle, Component parent) {
		return dialogSelectFiles(fileExts, currentDirectory, buttonText,
				oldFiles, multiSelect, null, GV.appFrame, true);
	}

	/**
	 * Select file dialog
	 * 
	 * @param fileExts
	 * @param currentDirectory
	 * @param buttonText
	 * @param oldFiles
	 * @param multiSelect
	 * @param dialogTitle
	 * @param parent
	 * @param useAllFileFilter
	 * @return
	 */
	public static Object dialogSelectFiles(String fileExts,
			String currentDirectory, String buttonText, Object oldFiles,
			boolean multiSelect, String dialogTitle, Component parent,
			boolean useAllFileFilter) {
		return dialogSelectFiles(fileExts, currentDirectory, buttonText,
				oldFiles, multiSelect, dialogTitle, parent, useAllFileFilter,
				null);
	}

	public static String saveAsExt = null;

	/**
	 * Select file dialog
	 * 
	 * @param fileExts
	 * @param currentDirectory
	 * @param buttonText
	 * @param oldFiles
	 * @param multiSelect
	 * @param dialogTitle
	 * @param parent
	 * @param useAllFileFilter
	 * @param image
	 * @return
	 */
	public static Object dialogSelectFiles(String fileExts,
			String currentDirectory, String buttonText, Object oldFiles,
			boolean multiSelect, String dialogTitle, Component parent,
			boolean useAllFileFilter, final Image image) {
		if (currentDirectory == null) {
			currentDirectory = GV.lastDirectory;
		}
		if (fileExts == null)
			fileExts = "";
		fileExts = fileExts.toLowerCase();

		JFileChooser chooser = new JFileChooser(currentDirectory) {

			private static final long serialVersionUID = 1L;

			protected void setUI(ComponentUI newUI) {
				super.setUI(new FileChooserUICN(this));
			}

			/**
			 * Override part of the createDialog method in JFileChooser
			 */
			public JDialog createDialog(Component parent)
					throws HeadlessException {
				String title = getUI().getDialogTitle(this);
				putClientProperty(
						AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY,
						title);

				JDialog dialog;
				Window window = JOptionPaneEx.getJWindowForComponent(parent);
				if (window instanceof Frame) {
					dialog = new JDialog((Frame) window, title, true);
				} else {
					dialog = new JDialog((Dialog) window, title, true);
				}
				/*
				 * AppFrame is null when it is just started and needs to be set
				 * manually.
				 */
				if (image != null) {
					dialog.setIconImage(image);
				}

				dialog.setComponentOrientation(this.getComponentOrientation());
				Container contentPane = dialog.getContentPane();
				contentPane.setLayout(new BorderLayout());
				contentPane.add(this, BorderLayout.CENTER);
				if (JDialog.isDefaultLookAndFeelDecorated()) {
					boolean supportsWindowDecorations = UIManager
							.getLookAndFeel().getSupportsWindowDecorations();
					if (supportsWindowDecorations) {
						dialog.getRootPane().setWindowDecorationStyle(
								JRootPane.FILE_CHOOSER_DIALOG);
					}
				}
				dialog.pack();
				dialog.setLocationRelativeTo(parent);
				return dialog;
			}
		};
		chooser.setFileView(new ImageFileView());
		chooser.setMultiSelectionEnabled(multiSelect);
		chooser.setAcceptAllFileFilterUsed(useAllFileFilter);

		String[] extArr;
		boolean isMuiltExts = false;

		if (fileExts.startsWith("\"") && fileExts.endsWith("\"")) {
			String ext = fileExts.substring(1, fileExts.length() - 1);
			extArr = new String[] { ext };
			isMuiltExts = ext.split(",").length > 1;
		} else {
			extArr = fileExts.split(",");
			final String firstExt = saveAsExt;

			if (firstExt != null) {
				for (int i = 0; i < extArr.length; i++) {
					String ext = extArr[i];
					if (firstExt.equals(ext)) {
						extArr[i] = extArr[0];
						extArr[0] = ext;
						break;
					}
				}
			} else if (!multiSelect && extArr.length > 1) {
				// 如果有文件名，在下拉列表中选择该后缀
				if (oldFiles != null) {
					File oldFile = (File) oldFiles;
					if (oldFile != null) {
						for (int i = 0; i < extArr.length; i++) {
							String ext = extArr[i];
							if (oldFile.getName().toLowerCase()
									.endsWith("." + ext)) {
								extArr[i] = extArr[0];
								extArr[0] = ext;
								break;
							}
						}

					}
				}
			}
		}
		saveAsExt = null;

		for (int i = extArr.length - 1; i >= 0; i--) {
			String ext = extArr[i];
			// chooser.setFileFilter(getFileFilter("." + ext, "*." + ext));
			if (StringUtils.isValidString(ext)) {
				String[] exts = ext.split(",");
				StringBuffer desc = new StringBuffer();
				for (int j = 0; j < exts.length; j++) {
					exts[j] = "." + exts[j];
					if (desc.length() > 0)
						desc.append(",");
					desc.append("*" + exts[j]);
				}
				chooser.setFileFilter(GM.getFileFilter(exts, desc.toString()));
			}
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
			GV.lastDirectory = chooser.getSelectedFile().getParent();
			if (multiSelect) {
				return chooser.getSelectedFiles();
			} else {
				String fileExt = chooser.getFileFilter().getDescription();
				int dot = fileExt.lastIndexOf(".");
				if (dot < 0) {
					fileExt = "";
				} else {
					fileExt = fileExt.substring(dot);
				}

				String path = chooser.getSelectedFile().getAbsolutePath();
				// if (!path.toLowerCase().endsWith(fileExt)) { // 切换了后缀
				// if (AppUtil.isSPLFile(path)) { // 仅限SPL网格文件的类型
				// // 去掉后缀
				// int index = path.lastIndexOf(".");
				// path = path.substring(0, index);
				// }
				// }
				if (!isMuiltExts) {
					boolean fileHasExt = path.toLowerCase().endsWith(fileExt);
					if (!fileHasExt && fileExt.startsWith(".")) {
						File fWithExt = new File(path + fileExt);
						return fWithExt;
					}
				}
				return chooser.getSelectedFile();
			}
		}
		return null;
	}

	/**
	 * Pop up a dialog box to select a date
	 * 
	 * @param initDate Initialization date
	 * @return
	 */
	public static String dialogSelectDate(String initDate) {
		return dialogSelectDate(initDate, GV.appFrame);
	}

	/**
	 * Pop up a dialog box to select a date
	 * 
	 * @param initDate Initialization date
	 * @param parent   Parent window
	 * @return
	 */
	public static String dialogSelectDate(String initDate, JFrame parent) {
		String val = null;

		DateChooser dc = new DateChooser(parent, true);
		java.util.Calendar cal = java.util.Calendar.getInstance();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		centerWindow(dc);
		try {
			val = initDate;
			cal.setTime(formatter.parse(val));
			dc.initDate(cal);
		} catch (Exception x) {
		}

		dc.setVisible(true);
		cal = dc.getSelectedDate();
		if (cal == null) {
			val = null;
		} else {
			val = formatter.format(cal.getTime());
		}
		return val;
	}

	/**
	 * Dialog to select directory
	 * 
	 * @param currentDirectory Initial path
	 * @return
	 */
	public static String dialogSelectDirectory(String currentDirectory) {
		return dialogSelectDirectory(currentDirectory, GV.appFrame);
	}

	/**
	 * Dialog to select directory
	 * 
	 * @param currentDirectory Initial path
	 * @param parent           Parent window
	 * @return
	 */
	public static String dialogSelectDirectory(String currentDirectory,
			Component parent) {
		return dialogSelectDirectory(currentDirectory, null, null, parent);
	}

	/**
	 * Dialog to select directory
	 * 
	 * @param currentDirectory Initial path
	 * @param buttonText       Button text
	 * @param title            The title of the dialog
	 * @param parent           Parent window
	 * @return
	 */
	public static String dialogSelectDirectory(String currentDirectory,
			String buttonText, String title, Component parent) {
		JFileChooser chooser = new JFileChooser(currentDirectory);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		try {
			resetFileNameLabel(chooser);
		} catch (Throwable t) {
		}
		if (StringUtils.isValidString(buttonText)) {
			chooser.setApproveButtonText(buttonText);
		}
		if (StringUtils.isValidString(title)) {
			chooser.setDialogTitle(title);
		}

		if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			GV.lastDirectory = chooser.getSelectedFile().getAbsolutePath();
			return chooser.getSelectedFile().getAbsolutePath();
		}
		return null;
	}

	/**
	 * Listener for saving window position and size information
	 * 
	 * @param dlg JDialog
	 */
	private static IDialogDimensionListener iDDListener = null;

	/**
	 * Set the listener for saving window position and size information
	 * 
	 * @param listener
	 */
	public static void setDialogDimensionListener(
			IDialogDimensionListener listener) {
		iDDListener = listener;
	}

	/**
	 * Set the position and size of the window
	 * 
	 * @param dlg
	 */
	public static void setWindowDimension(Component dlg) {
		if (iDDListener != null) {
			iDDListener.saveWindowDimension(dlg);
			return;
		}
		if (ConfigOptions.bWindowSize.booleanValue()) {
			try {
				ConfigFile cf = ConfigFile.getConfigFile();
				if (cf != null) {
					String oldNode = cf.getConfigNode();
					Dimension d = dlg.getSize();
					cf.setConfigNode(ConfigFile.NODE_DIMENSION);
					String nodeName = dlg.getClass().getName()
							+ STRING_DIMENSION;
					String nodeValue = d.width + "," + d.height + ","
							+ dlg.getX() + "," + dlg.getY();
					nodeName = removeXmlKeyWords(nodeName);
					nodeValue = removeXmlKeyWords(nodeValue);

					cf.setAttrValue(nodeName, nodeValue);
					cf.setConfigNode(oldNode);
				}
			} catch (Throwable ex) {
			}
		}
	}

	/**
	 * 去掉xml的特殊字符，不转换，仅给保存窗口位置尺寸方法使用
	 * 
	 * @param str
	 * @return
	 */
	public static String removeXmlKeyWords(String str) {
		str = str.replaceAll("\\$", "");
		str = str.replaceAll("&", "");
		str = str.replaceAll("<", "");
		str = str.replaceAll(">", "");
		str = str.replaceAll("'", "");
		str = str.replaceAll("\"", "");
		return str;
	}

	/**
	 * Center the window
	 * 
	 * @param w Window The window to be centered
	 */
	public static void centerWindow(Component w) {
		boolean b = loadWindowSize(w);

		if (!b) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			Dimension winSize = w.getSize();
			if (winSize.height > screenSize.height) {
				winSize.height = screenSize.height;
			}
			if (winSize.width > screenSize.width) {
				winSize.width = screenSize.width;
			}
			w.setLocation((screenSize.width - winSize.width) / 2,
					(screenSize.height - winSize.height) / 2);
		}
	}

	/**
	 * Node name suffix
	 */
	private final static String STRING_DIMENSION = ".dimension";

	/**
	 * Load window position and size
	 * 
	 * @param jc
	 * @return
	 */
	public static boolean loadWindowSize(Component jc) {
		if (iDDListener != null) {
			return iDDListener.loadWindowDimension(jc);
		}

		String str = jc.getClass().getName() + STRING_DIMENSION;
		str = removeXmlKeyWords(str);
		boolean b = false;
		try {
			if (ConfigOptions.bWindowSize.booleanValue()) {
				ConfigFile cf = ConfigFile.getConfigFile();
				if (cf != null) {
					String oldNode = cf.getConfigNode();
					cf.setConfigNode(ConfigFile.NODE_DIMENSION);
					String dimension = cf.getAttrValue(str);
					if (StringUtils.isValidString(dimension)) {
						dimension = removeXmlKeyWords(dimension);
						Section s = new Section(dimension);
						String size[] = s.toStringArray();
						jc.setSize(Integer.parseInt(size[0]),
								Integer.parseInt(size[1]));
						jc.setLocation(Integer.parseInt(size[2]),
								Integer.parseInt(size[3]));
						b = true;
					}
					cf.setConfigNode(oldNode);
				}
			}
		} catch (Throwable ex) {
		}
		return b;
	}

	/**
	 * Save column format
	 * 
	 * @param colName
	 * @param format
	 */
	public static void saveFormat(String colName, String format) {
		ConfigFile cf = null;
		try {
			cf = ConfigFile.getConfigFile();
			String oldNode = cf.getConfigNode();

			XMLFile xml = cf.xmlFile();
			if (!xml.isPathExists(ConfigFile.NODE_FORMAT)) {
				xml.newElement("RAQSOFT", ConfigFile.NODE_FORMAT);
			}
			cf.setConfigNode(ConfigFile.NODE_FORMAT);
			cf.setAttrValue(colName, format);
			cf.setConfigNode(oldNode);
		} catch (Throwable t) {
		}
	}

	/**
	 * Get column format by column name
	 * 
	 * @param colName
	 * @return
	 */
	public static String getColumnFormat(String colName) {
		if (colName == null) {
			return null;
		}
		try {
			ConfigFile cf = ConfigFile.getConfigFile();
			String oldNode = cf.getConfigNode();
			cf.setConfigNode(ConfigFile.NODE_FORMAT);
			String format = cf.getAttrValue(colName);
			cf.setConfigNode(oldNode);
			return format;
		} catch (Throwable ex) {
		}
		return null;
	}

	/**
	 * The select file dialog shows an error.It should be a bug in substance.
	 * 
	 * @param p
	 */
	public static void resetFileNameLabel(Container p) {
		int cc = p.getComponentCount();
		for (int i = 0; i < cc; i++) {
			Component c = p.getComponent(i);
			if (c instanceof JLabel) {
				String text = ((JLabel) c).getText();
				if (!StringUtils.isValidString(text)) {
					Dimension size = c.getPreferredSize();
					if (size.getHeight() < 21) {
						size.setSize(size.getWidth(), 21);
					}
					((JLabel) c).setText(IdeCommonMessage.get().getMessage(
							"gm.labelfoldername"));
					c.setPreferredSize(size);
				} else {
					String labelFileName = IdeCommonMessage.get().getMessage(
							"gm.labelfilename");
					String labelFolderName = IdeCommonMessage.get().getMessage(
							"gm.labelfoldername");
					String fileNamePre = labelFileName.substring(0,
							labelFileName.length() - 1).toLowerCase();
					String folderNamePre = labelFolderName.substring(0,
							labelFolderName.length() - 1).toLowerCase();
					if (text.toLowerCase().startsWith(fileNamePre)
							|| text.toLowerCase().startsWith(folderNamePre)) {
						JLabel newLabel = new JLabel(labelFolderName);
						newLabel.setPreferredSize(c.getPreferredSize());
						p.remove(c);
						p.add(newLabel, i);
						return;
					}
				}
			} else if (c instanceof Container) {
				resetFileNameLabel((Container) c);
			}
		}
	}

	/**
	 * Possible to paste
	 */
	private static boolean canPaste = false;

	/**
	 * Reset the clipboard
	 */
	public static void resetClipBoard() {
		canPaste = StringUtils.isValidString(GM.clipBoard(false));
	}

	/**
	 * Possible to paste
	 * 
	 * @return
	 */
	public static boolean canPaste() {
		if (GV.cellSelection != null) {
			return true;
		}
		return canPaste;
	}

	/**
	 * Copy text to the system clipboard
	 * 
	 * @param data String The string to put on the system clipboard
	 */
	public static void clipBoard(String data) {
		try {
			Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection ss = new StringSelection(data);
			clip.setContents(ss, null);
			canPaste = StringUtils.isValidString(data);
		} catch (Exception e) {
		}
	}

	/**
	 * Get text from the system clipboard
	 * 
	 * @return String Text of the system clipboard
	 */
	public static String clipBoard() {
		return clipBoard(true);
	}

	/**
	 * Get text from the system clipboard
	 * 
	 * @param caseHtml Whether to consider HTML formatted text
	 * @return
	 */
	public static String clipBoard(boolean caseHtml) {
		try {
			Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable tf = clip.getContents(null);
			if (caseHtml) {
				DataFlavor dfs[] = tf.getTransferDataFlavors();
				if (dfs == null) {
					return "";
				}
				String mineType;
				Object obj = null;
				for (int i = 0; i < dfs.length; i++) {
					mineType = dfs[i].getMimeType();
					if (mineType.indexOf("text/html") > -1) {
						obj = tf.getTransferData(dfs[i]);
						if (StringUtils.isValidString(obj)) {
							break;
						}
					}
				}
				if (StringUtils.isValidString(obj)) {
					String htmlStr = (String) obj;
					Pattern p;
					Matcher m;
					p = Pattern.compile("<table", Pattern.CASE_INSENSITIVE);
					m = p.matcher(htmlStr);
					if (m.find()) {
						/* Office can correctly sort out the ranks */
						p = Pattern.compile("microsoft",
								Pattern.CASE_INSENSITIVE);
						m = p.matcher(htmlStr);
						if (!m.find()) {
							return htmlStr;
						}
						p = Pattern.compile("office", Pattern.CASE_INSENSITIVE);
						m = p.matcher(htmlStr);
						if (!m.find()) {
							return htmlStr;
						}
					}
				}
			}
			String str = (String) tf.getTransferData(DataFlavor.stringFlavor);
			return str;
		} catch (Exception ex) {
			return "";
		}
	}

	/**
	 * Get the byte array in the input stream
	 * 
	 * @param is Input stream
	 * @throws Exception
	 * @return Byte array
	 */
	public static byte[] inputStream2Bytes(InputStream is) throws Exception {
		if (is == null) {
			return new byte[] {};
		}
		ArrayList<byte[]> al = new ArrayList<byte[]>();
		int totalBytes = 0;
		byte[] b = new byte[102400];
		int readBytes = 0;
		while ((readBytes = is.read(b)) > 0) {
			byte[] bb = new byte[readBytes];
			System.arraycopy(b, 0, bb, 0, readBytes);
			al.add(bb);
			totalBytes += readBytes;
		}
		b = new byte[totalBytes];
		int pos = 0;
		for (int i = 0; i < al.size(); i++) {
			byte[] bb = (byte[]) al.get(i);
			System.arraycopy(bb, 0, b, pos, bb.length);
			pos += bb.length;
		}
		return b;
	}

	/**
	 * Read the picture according to the path
	 * 
	 * @param filePath String Relative file path
	 * @return ImageIcon Generated image
	 */
	public static ImageIcon getImageIcon(String filePath) {
		return getImageIcon(filePath, true);
	}

	/**
	 * Read the picture according to the path
	 * 
	 * @param filePath
	 * @param showException Whether to display exception information
	 * @return
	 */
	public static ImageIcon getImageIcon(String filePath, boolean showException) {
		try {
			File f = new File(filePath);
			InputStream is = null;
			if (f.exists()) {
				is = new FileInputStream(f);
			} else {
				filePath = Sentence.replace(filePath, "\\", "/", 0);
				if (!filePath.startsWith("/")) {
					filePath = "/" + filePath;
				}
				is = GM.class.getResourceAsStream(filePath);
				/*
				 * First use the file name to find it, and then use lowercase to
				 * find it if you cannot find it.
				 */
				if (is == null) {
					is = GM.class.getResourceAsStream(filePath.toLowerCase());
				}
			}
			if (is == null) {
				throw new Exception("Get image file: " + filePath + " failure!");
			}

			byte[] bt = inputStream2Bytes(is);
			is.close();
			return new ImageIcon(bt);
		} catch (Exception e) {
			// Logger.info(e.getMessage());
			if (showException) {
				showException(e);
			}
		}
		return null;
	}

	/**
	 * Display exception information
	 * 
	 * @param oMsg Exception or error message
	 */
	public static void showException(Object oMsg) {
		showException(oMsg, true);
	}

	/**
	 * Display exception information
	 * 
	 * @param oMsg       Exception or error message
	 * @param canCopyMsg Is it possible to copy the exception information.
	 */
	public static void showException(Object oMsg, boolean canCopyMsg) {
		showException(oMsg, canCopyMsg, null);
	}

	/**
	 * Display exception information
	 * 
	 * @param oMsg       Exception or error message
	 * @param canCopyMsg Is it possible to copy the exception information.
	 * @param logo       Window logo
	 */
	public static void showException(Object oMsg, boolean canCopyMsg,
			ImageIcon logo) {
		showException(oMsg, canCopyMsg, logo, null, GV.appFrame);
	}

	/**
	 * Display exception information
	 * 
	 * @param oMsg  Exception or error message
	 * @param frame Parent component
	 */
	public static void showException(Object oMsg, Frame frame) {
		showException(oMsg, true, null, null, frame);
	}

	/**
	 * Display exception information
	 * 
	 * @param oMsg       Exception or error message
	 * @param canCopyMsg Is it possible to copy the exception information.
	 * @param logo       Window logo
	 * @param pre        The prefix of the exception information
	 */
	public static void showException(Object oMsg, boolean canCopyMsg,
			ImageIcon logo, String pre) {
		showException(oMsg, canCopyMsg, logo, pre, GV.appFrame);
	}

	/**
	 * Display exception information
	 * 
	 * @param oMsg       Exception or error message
	 * @param canCopyMsg Is it possible to copy the exception information.
	 * @param logo       Window logo
	 * @param pre        The prefix of the exception information
	 * @param frame      Parent component
	 */
	public static void showException(Object oMsg, boolean canCopyMsg,
			ImageIcon logo, String pre, Frame frame) {
		String msg;
		Throwable e = null;
		if (oMsg instanceof Throwable) {
			e = (Throwable) oMsg;
			if (e != null) {
				if (e instanceof ThreadDeath)
					return;
				Throwable cause = e.getCause();
				int i = 0;
				while (cause != null) {
					if (cause instanceof ThreadDeath)
						return;
					cause = cause.getCause();
					i++;
					if (i > 10) {
						break;
					}
				}
			}
			if (!StringUtils.isValidString(e.getMessage())) {
				msg = e.toString();
			} else {
				msg = e.getMessage();
			}
			Throwable cause = e.getCause();
			if (cause != null) {
				if (StringUtils.isValidString(cause.getMessage())) {
					if (msg.indexOf(cause.getMessage()) < 0)
						msg = msg
								+ com.scudata.ide.common.GM.getLineSeparator()
								+ cause.getMessage();
				} else {
					cause = cause.getCause();
					if (cause != null)
						if (StringUtils.isValidString(cause.getMessage())) {
							if (msg.indexOf(cause.getMessage()) < 0)
								msg = msg
										+ com.scudata.ide.common.GM
												.getLineSeparator()
										+ cause.getMessage();
						}
				}
			}
		} else {
			msg = oMsg == null ? null : oMsg.toString();
		}
		if (pre != null) {
			msg = pre + "\n" + msg;
		}
		if (canCopyMsg) {
			DialogInputText dit = new DialogInputText(frame, IdeCommonMessage
					.get().getMessage("gm.errorprompt"), false);
			dit.setText(msg);
			dit.setMessageMode();
			if (logo != null) {
				dit.setIconImage(logo.getImage());
			}
			dit.setVisible(true);
		} else {
			JOptionPane.showMessageDialog(frame, msg, IdeCommonMessage.get()
					.getMessage("gm.errorprompt"),
					JOptionPane.INFORMATION_MESSAGE);
		}

		writeLog(e != null ? e : msg);
	}

	/**
	 * Write the exception information to the log file.
	 * 
	 * @param e Throwable or String
	 */
	public static void writeLog(Object e) {
		if (e == null)
			return;
		// if (ConfigOptions.bLogException.booleanValue()) {
		Logger.error(e);
		// File f = new
		// File(GM.getAbsolutePath(ConfigOptions.sLogFileName));
		// if (!f.exists()) {
		// try {
		// f.createNewFile();
		// } catch (Exception xf) {
		// xf.printStackTrace();
		// return;
		// }
		// }
		// try {
		// FileOutputStream fos = new FileOutputStream(f, true);
		// PrintWriter pw = new PrintWriter(fos);
		// pw.println(new java.util.Date());
		// if (e instanceof Throwable) {
		// ((Throwable) e).printStackTrace(pw);
		// } else {
		// pw.write((String) e);
		// }
		// fos.flush();
		// pw.close();
		// } catch (Exception logError) {
		// logError.printStackTrace();
		// return;
		// }
		// }
	}

	/**
	 * 输出异常信息到控制台和IDE日志文件
	 * 
	 * @param e Throwable or String
	 */
	public static void outputMessage(Object e) {
		// 现在Logger同时输出到控制台和日志了，直接调用writeLog
		writeLog(e);
	}

	/**
	 * Find menu item
	 * 
	 * @param menu Menu object
	 * @param str  Menu name/text/label
	 * @param type Constants defined in GC.
	 *             E.g:SEARCHMENU_BYNAME,SEARCHMENU_BYTEXT,SEARCHMENU_BYLABLE
	 * @return
	 */
	private static Object extractMenuObject(Object menu, String str, short type) {
		int c;
		String sTmp = "";
		if (menu instanceof Menu) {
			Menu pMenu = (Menu) menu;
			MenuItem sItem;
			c = pMenu.getItemCount();
			for (int i = 0; i < c; i++) {
				sItem = pMenu.getItem(i);
				if (sItem == null) {
					continue;
				}
				if (sItem instanceof Menu) {
					MenuItem tmpItem;
					tmpItem = (MenuItem) extractMenuObject(sItem, str, type);
					if (tmpItem != null) {
						return tmpItem;
					}
				}
				if (type == GC.SEARCHMENU_BYNAME) {
					sTmp = sItem.getName();
				} else if (type == GC.SEARCHMENU_BYLABLE) {
					sTmp = sItem.getLabel();
				}
				if (sTmp == null) {
					continue;
				}
				if (sTmp.equalsIgnoreCase(str)) {
					return sItem;
				}
			}
		}
		if (menu instanceof JMenu) {
			JMenu pMenu = (JMenu) menu;
			JMenuItem sItem;
			c = pMenu.getItemCount();
			for (int i = 0; i < c; i++) {
				sItem = pMenu.getItem(i);
				if (sItem == null) {
					continue;
				}
				if (sItem instanceof JMenu) {
					JMenuItem tmpItem;
					tmpItem = (JMenuItem) extractMenuObject(sItem, str, type);
					if (tmpItem != null) {
						return tmpItem;
					}
				}
				if (type == GC.SEARCHMENU_BYNAME) {
					sTmp = sItem.getName();
				} else if (type == GC.SEARCHMENU_BYTEXT) {
					sTmp = sItem.getText();
				}
				if (sTmp == null) {
					continue;
				}
				if (sTmp.equalsIgnoreCase(str)) {
					return sItem;
				}
			}
		}
		return null;
	}

	/**
	 * Find menu item
	 * 
	 * @param menuBar JMenuBar
	 * @param str     Menu name/text/label
	 * @param type    Constants defined in GC.
	 *                E.g:SEARCHMENU_BYNAME,SEARCHMENU_BYTEXT,SEARCHMENU_BYLABLE
	 * @return
	 */
	private static JMenuItem findMenuItem(JMenuBar menuBar, String str,
			short searchType) {
		int c;
		c = menuBar.getMenuCount();
		JMenuItem sItem;

		for (int i = 0; i < c; i++) {
			sItem = (JMenuItem) extractMenuObject(menuBar.getMenu(i), str,
					searchType);
			if (sItem != null) {
				return sItem;
			}
		}
		return null;
	}

	/**
	 * Find the menu item according to the name of the menu.
	 * 
	 * @param menuBar JMenuBar
	 * @param name    The name of the menu item. Set by the method setName(String
	 *                name).
	 * @return If found, return the JMenuItem menu item object. Otherwise, it
	 *         returns null.
	 */
	public static JMenuItem getMenuByName(JMenuBar menuBar, String name) {
		return findMenuItem(menuBar, name, GC.SEARCHMENU_BYNAME);
	}

	/**
	 * Find the menu item according to the text of the menu.
	 * 
	 * @param menuBar JMenuBar
	 * @param name    The text of the menu item. Set by the method setText(String
	 *                text).
	 * @return If found, return the JMenuItem menu item object. Otherwise, it
	 *         returns null.
	 */
	public static JMenuItem getMenuByText(JMenuBar menuBar, String text) {
		return findMenuItem(menuBar, text, GC.SEARCHMENU_BYTEXT);
	}

	/**
	 * Set the size of the tool window.
	 * 
	 * @param w The window object to be set
	 */
	public static void setWindowToolSize(Window w) {
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		w.setSize((int) (d.getWidth() * 0.75), (int) (d.getHeight() * 0.75));
	}

	/**
	 * Set the default OK and Cancel buttons of the dialog.
	 * 
	 * @param dlg          JDialog Dialog to be set
	 * @param okButton     JButton Button to be executed when enter
	 * @param cancelButton JButton Button to be executed when escape
	 */
	public static void setDialogDefaultButton(Window dlg,
			final JButton okButton, final JButton cancelButton) {
		JRootPane pane = null;

		if (dlg instanceof DialogMaxmizable) {
			pane = ((JDialog) dlg).getRootPane();
			DialogMaxmizable dm = (DialogMaxmizable) dlg;
			dm.oldSize = dm.getSize();
			dlg.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						DialogMaxmizable f = (DialogMaxmizable) e.getSource();
						Dimension screenSize = Toolkit.getDefaultToolkit()
								.getScreenSize();
						if (f.isMaxized) {
							f.setSize(f.oldSize);
							int newX = (screenSize.width - f.oldSize.width) / 2;
							int newY = (screenSize.height - f.oldSize.height) / 2;
							f.setLocation(newX, newY);
							f.isMaxized = false;
						} else {
							f.setLocation(0, 0);
							f.setSize(screenSize);
							f.isMaxized = true;
						}
					}
				}
			});
		} else if (dlg instanceof JDialog) {
			pane = ((JDialog) dlg).getRootPane();
			((JDialog) dlg).setResizable(true);
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
		pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), o);
		pane.getActionMap().put(o, cancelAction);
		dlg.setSize(dlg.getWidth() + 10, dlg.getHeight() + 10);
		centerWindow(dlg);

	}

	/**
	 * Font map. Key:Font name, Value:Font object
	 */
	private static HashMap<String, Font> fontMap = new HashMap<String, Font>();
	/**
	 * Statically load the font map
	 */
	static {
		String[] fonts = GM.getFontNames();
		for (int i = 0; i < fonts.length; i++) {
			String font = fonts[i];
			fontMap.put(font, new Font(font, Font.PLAIN, 9));
		}
	}

	/**
	 * Return the font object based on the font name.
	 * 
	 * @param fontName Font name
	 * @return
	 */
	public static Font getFont(String fontName) {
		return (Font) fontMap.get(fontName);
	}

	/**
	 * Get the font of the current user's machine environment. Return according to
	 * the current language.
	 * 
	 * @return String[] Font names
	 */
	public static String[] getFontNames() {
		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		return ge.getAvailableFontFamilyNames(GV.language);
	}

	/**
	 * Get a list of word sizes. Non-Chinese countries are displayed as numbers
	 * 
	 * @return JComboBoxEx
	 */
	public static JComboBoxEx getFontSizes() {
		JComboBoxEx combo = new JComboBoxEx();
		Section ss = new Section(GC.FONTSIZECODE);
		Vector code = ss.toVector();
		if (GV.language.equals(Locale.CHINA)
				|| GV.language.equals(Locale.CHINESE)
				|| GC.LANGUAGE == GC.ASIAN_CHINESE) {
			ss = new Section(GC.FONTSIZEDISP);
		} else {
			ss = new Section();
			for (int i = 0; i < code.size(); i++) {
				ss.addSection(code.get(i).toString());
			}
		}
		Vector disp = ss.toVector();
		combo.x_setData(code, disp);
		return combo;
	}

	/**
	 * Pop up a text dialog to edit the table
	 * 
	 * @param table JTableEx The table to be edited
	 * @param r     int Row
	 * @param c     int Column
	 */
	public static boolean dialogEditTableText(JTableEx table, int r, int c) {
		Object o = table.getCellEditor(r, c);
		if (o instanceof DefaultCellEditor) {
			DefaultCellEditor editor = (DefaultCellEditor) o;
			Component cp = editor.getComponent();
			if (cp instanceof JCheckBox) {
				return false;
			}
		}

		Object sData = table.getValueAt(r, c); // data.
		try {
			sData = sData.toString();
		} catch (Exception ex) {
			sData = "";
		}
		DialogInputText dit = new DialogInputText(GV.appFrame, true);
		dit.setText(sData.toString());
		dit.setVisible(true);
		table.acceptText();
		if (dit.getOption() == JOptionPane.OK_OPTION) {
			table.setValueAt(dit.getText(), r, c);
			table.acceptText();
			return true;
		}
		return false;
	}

	/**
	 * Find a unique name starting with namePrefix in the column of the table
	 * 
	 * @param table      JTableEx
	 * @param column     int
	 * @param namePrefix String
	 * @return String
	 */
	public static String getTableUniqueName(JTableEx table, int column,
			String namePrefix) {
		int r, m, t;
		r = 0;
		t = table.getRowCount();
		String tmpName;
		while (true) {
			r++;
			for (m = 0; m < t; m++) {
				tmpName = (String) table.getValueAt(m, column);
				if (tmpName == null) {
					continue;
				}
				if (tmpName.equalsIgnoreCase(namePrefix + Integer.toString(r))) {
					break;
				}
			}
			if (m >= t) {
				break;
			}
		}
		m = r;
		return namePrefix + Integer.toString(m);
	}

	/**
	 * Get the GridBagLayout object
	 * 
	 * @param row int Row
	 * @param col int Column
	 * @return
	 */
	public static GridBagConstraints getGBC(int row, int col) {
		return getGBC(row, col, false);
	}

	/**
	 * Get the GridBagLayout object
	 * 
	 * @param row   int Row
	 * @param col   int Column
	 * @param hFill boolean Whether the object is filled horizontally
	 * @return
	 */
	public static GridBagConstraints getGBC(int row, int col, boolean hFill) {
		return getGBC(row, col, hFill, false);
	}

	/**
	 * Get the GridBagLayout object
	 * 
	 * @param row   int Row
	 * @param col   int Column
	 * @param hFill boolean Whether the object is filled horizontally
	 * @param vFill boolean Whether the object is filled vertically
	 * @return
	 */
	public static GridBagConstraints getGBC(int row, int col, boolean hFill,
			boolean vFill) {
		return getGBC(row, col, hFill, vFill, 8);
	}

	/**
	 * Get the GridBagLayout object
	 * 
	 * @param row   int Row
	 * @param col   int Column
	 * @param hFill boolean Whether the object is filled horizontally
	 * @param vFill boolean Whether the object is filled vertically
	 * @param hGap  Horizontal interval
	 * @return GridBagConstraints
	 */
	public static GridBagConstraints getGBC(int row, int col, boolean hFill,
			boolean vFill, int hGap) {
		return getGBC(row, col, hFill, vFill, hGap, 3);
	}

	/**
	 * Get the GridBagLayout object
	 * 
	 * @param row   int Row
	 * @param col   int Column
	 * @param hFill boolean Whether the object is filled horizontally
	 * @param vFill boolean Whether the object is filled vertically
	 * @param hGap  Horizontal interval
	 * @param vGap  Vertical interval
	 * @return
	 */
	public static GridBagConstraints getGBC(int row, int col, boolean hFill,
			boolean vFill, int hGap, int vGap) {
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

	/**
	 * Sort the data
	 * direct use  Collections.sort instead
	 * @param v2Sort AbstractList, List to sort
	 * @param ascend boolean, Whether to sort in ascending order
	 * @return boolean, Return true if the sort is successful, otherwise false
	 */
	// public static boolean sort(AbstractList list, boolean ascend) {
	// Comparable ci, cj;
	// int i, j;
	// boolean lb_exchange;
	// for (i = 0; i < list.size(); i++) {
	// Object o = list.get(i);
	// if (o != null && !(o instanceof Comparable)) {
	// return false;
	// }
	// }
	//
	// for (i = 0; i < list.size() - 1; i++) {
	// for (j = i + 1; j < list.size(); j++) {
	// ci = (Comparable) list.get(i);
	// cj = (Comparable) list.get(j);
	// if (ascend) {
	// if (ci == null || cj == null) {
	// lb_exchange = (cj == null);
	// } else {
	// lb_exchange = ci.compareTo(cj) > 0;
	// }
	// } else {
	// if (ci == null || cj == null) {
	// lb_exchange = (ci == null);
	// } else {
	// lb_exchange = ci.compareTo(cj) < 0;
	// }
	// }
	// if (lb_exchange) {
	// Object o, o2;
	// o = list.get(i);
	// o2 = list.get(j);
	// list.set(i, o2);
	// list.set(j, o);
	// }
	// }
	// }
	// return true;
	// }

	/**
	 * Whether it can be saved as the other file.
	 * 
	 * @param saveAsFile File name to save as
	 * @return
	 */
	public static boolean canSaveAsFile(String saveAsFile) {
		if (!StringUtils.isValidString(saveAsFile)) {
			JOptionPane.showMessageDialog(GV.appFrame, IdeCommonMessage.get()
					.getMessage("gm.inputfilename"));
			return false;
		}

		File saveFile = new File(saveAsFile);
		if (saveFile.exists()) {
			int r = JOptionPane.showConfirmDialog(GV.appFrame, IdeCommonMessage
					.get().getMessage("gm.existfile", saveAsFile),
					IdeCommonMessage.get().getMessage("public.note"),
					JOptionPane.OK_CANCEL_OPTION);
			if (r == JOptionPane.CANCEL_OPTION) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Get the icon of the menu
	 * 
	 * @param menuId ID of the menu
	 * @return
	 */
	public static ImageIcon getMenuImageIcon(String menuId) {
		String iconText;
		int dot = menuId.indexOf(".");
		if (dot > 0) {
			iconText = menuId.substring(dot + 1);
		} else {
			iconText = menuId;
		}
		String iconPath = GC.IMAGES_PATH + "m_" + iconText.toLowerCase()
				+ ".gif";
		return GM.getImageIcon(iconPath);
	}

	/**
	 * Reset the title. Used after the data source connection status changes.
	 * 
	 * @param dsName   Data source name
	 * @param userName User name
	 */
	public static void resetFrameTitle(String dsName, String userName) {
		Iterator it = GV.allFrames.iterator();
		while (it.hasNext()) {
			AppFrame af = (AppFrame) it.next();
			String tmpTitle = af.resetTitle();
			if (StringUtils.isValidString(dsName)) {
				tmpTitle += " - "
						+ IdeCommonMessage.get().getMessage("gm.connect",
								dsName);
			}
			if (StringUtils.isValidString(userName)) {
				tmpTitle += " - "
						+ IdeCommonMessage.get().getMessage("gm.activeuser",
								userName);
			}
			af.setTitle(tmpTitle);
		}
	}

	/**
	 * First go to the resource folder to find it. Then go to the classpath to find
	 * it.
	 * 
	 * @param productId The product ID defined in Sequence
	 * @param isGetIcon
	 * @return
	 */
	public static ImageIcon getLogoImage(boolean isGetIcon) {
		String relativeFile = GC.PATH_LOGO;
		String pLogo;
		/* Whether to display a popup window when the logo is not found */
		boolean isShowException = false;
		pLogo = GC.DEFAULT_LOGO;
		if (isGetIcon) {
			int lastDot = pLogo.lastIndexOf(".");
			if (lastDot > -1) {
				pLogo = pLogo.substring(0, lastDot) + "_ico."
						+ pLogo.substring(lastDot + 1);
			} else {
				pLogo += "_ico";
			}
		}
		if (pLogo.startsWith("/")) {
			relativeFile += pLogo.toLowerCase();
		} else {
			relativeFile += "/" + pLogo.toLowerCase();
		}
		String logoPath = getAbsolutePath(relativeFile);
		File f = new File(logoPath);
		ImageIcon ii = null;
		if (f.exists()) {
			ii = new ImageIcon(logoPath);
		} else {
			if (isGetIcon) {
				InputStream is = GM.class.getResourceAsStream(relativeFile);
				if (is == null) {
					return getLogoImage(false);
				}
			}
			ii = getImageIcon(relativeFile, isShowException);
		}
		return ii;

	}

	/**
	 * Get the absolute path relative to start.home.
	 * 
	 * @param path Absolute path or relative path
	 * @return Return the absolute path of path
	 */
	public static String getAbsolutePath(String path) {
		String home = System.getProperty("start.home");
		return ConfigUtil.getPath(home, path);
	}

	/**
	 * Generate JMenu object
	 * 
	 * @param menuText   Menu text
	 * @param mneKey     The Mnemonic
	 * @param isMainMenu Whether the menu is the main menu
	 * @return
	 */
	public static JMenu getMenuItem(String menuText, char mneKey,
			boolean isMainMenu) {
		JMenu mItem = new JMenu(menuText);
		if (!isMainMenu) {
			mItem.setIcon(getMenuImageIcon("blank"));
		}
		if (StringUtils.isValidString(String.valueOf(mneKey))) {
			mItem.setMnemonic(mneKey);
		}
		return mItem;
	}

	/**
	 * Generate JMenu object
	 * 
	 * @param cmdId    short, Menu constant defined in GC
	 * @param menuId   String, Menu name
	 * @param mneKey   char, The Mnemonic
	 * @param mask     int, Because ActionEvent.META_MASK is almost not used. This
	 *                 key seems to be only available on Macintosh keyboards. It is
	 *                 used here instead of no accelerator key.
	 * @param hasIcon  boolean, Whether the menu item has an icon
	 * @param menuText String, The menu text
	 * @return JMenuItem
	 */
	public static JMenuItem getMenuItem(short cmdId, String menuId,
			char mneKey, int mask, boolean hasIcon, String menuText) {
		JMenuItem mItem = null;
		mItem = new JMenuItem(menuText, mneKey);
		mItem.setName(Short.toString(cmdId));
		if (hasIcon) {
			mItem.setIcon(getMenuImageIcon(menuId));
		} else {
			mItem.setIcon(getMenuImageIcon("blank"));
		}
		if (mask != GC.NO_MASK) {
			/* Replace CTRL with META in Apple system. */
			if (GM.isMacOS()) {
				if ((mask & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK) {
					mask = mask - ActionEvent.CTRL_MASK + ActionEvent.META_MASK;
				}
				if (mneKey == KeyEvent.VK_DELETE) {
					mneKey = KeyEvent.VK_BACK_SPACE;
				}
			}
			KeyStroke ks = KeyStroke.getKeyStroke(mneKey, mask);
			mItem.setAccelerator(ks);
		}
		return mItem;
	}

	/**
	 * All in the schema name. All means null.
	 */
	public static final String SCHEMA_ALL = IdeCommonMessage.get().getMessage(
			"public.all");

	/**
	 * Get the real schema name. All means null.
	 * 
	 * @param schema
	 * @return
	 */
	public static String getRealSchema(Object schema) {
		if (!StringUtils.isValidString(schema)) {
			return null;
		}
		if (schema.equals(SCHEMA_ALL)) {
			return null;
		}
		return (String) schema;
	}

	/**
	 * Load the schema names into the JComboBox component.
	 * 
	 * @param ds       Data source object
	 * @param cbSchema The JComboBox component
	 */
	public static void loadSchemas(DataSource ds, JComboBox<String> cbSchema) {
		cbSchema.removeAllItems();
		if (ds == null) {
			cbSchema.addItem(SCHEMA_ALL);
		} else {
			DBInfo dbInfo = ds.getDBInfo();
			String dbcs = dbInfo.getDBCharset();
			String ccs = dbInfo.getClientCharset();
			boolean convert = dbcs != null && !dbcs.equals(ccs);
			try {
				Vector<String> schemas = ds.listSchemas();
				String schema;
				for (int i = 0; i < schemas.size(); i++) {
					schema = schemas.get(i);
					if (convert) {
						schema = convertDBString(ds, schema);
					}
					cbSchema.addItem(schema);
				}
			} catch (Throwable x) {
				/* Do nothing */
			}
			cbSchema.addItem(SCHEMA_ALL);
		}
	}

	/**
	 * Handle the exception informations of the data source
	 * 
	 * @param ds The data source
	 * @param x  Throwable
	 * @return Return the exception information of the data source after it is
	 *         concretized
	 */
	public static Object handleDSException(DataSource ds, Throwable x) {
		boolean noClass = false;
		if (x instanceof ClassNotFoundException) {
			noClass = true;
		} else if (x instanceof SQLException) {
			if (x.getMessage() != null
					&& x.getMessage().equals("No suitable driver")) {
				noClass = true;
			}
		}
		if (noClass) {
			StringBuffer msg = new StringBuffer();
			String errorMsg = DBTypeEx.getErrorMessage(ds.getName());
			msg.append("Class [");
			msg.append(((DBConfig) ds.getDBInfo()).getDriver());
			msg.append("] is not found.");
			if (errorMsg != null) {
				msg.append("\r\n");
				msg.append("\r\n");
				msg.append(errorMsg);
			}
			return msg.toString();
		}
		return x;
	}

	/**
	 * Set the data source configuration to Env
	 * 
	 * @param dsModel
	 */
	public static void resetEnvDataSource(DataSourceListModel dsModel) {
		Env.clearDBSessionFactories();

		if (dsModel != null) {
			List<DBConfig> dbList = new ArrayList<DBConfig>();
			GV.config.setDBList(dbList);
			for (int i = 0; i < dsModel.size(); i++) {
				DataSource ds = (DataSource) dsModel.get(i);
				try {
					if (ds.isLocal())
						dbList.add(new DBConfig((DBConfig) ds.getDBInfo()));
					Env.setDBSessionFactory(ds.getName(), ds.getDBInfo()
							.createSessionFactory());
				} catch (Throwable x) {
					Logger.debug(x);
				}
			}
		} else {
			if (GV.config != null)
				GV.config.setDBList(null);
		}
	}

	/**
	 * Get the list of table names in the schema
	 * 
	 * @param ds     Data source
	 * @param schema The schema name
	 * @return
	 */
	public static Vector<String> listSchemaTables(DataSource ds, String schema) {
		return listSchemaTables(ds, schema, true, true);
	}

	/**
	 * Get the list of table names in the schema
	 * 
	 * @param ds      Data source
	 * @param schema  The schema name
	 * @param showMsg Whether to display exception information
	 * @return
	 */
	public static Vector<String> listSchemaTables(DataSource ds, String schema,
			boolean showMsg) {
		return listSchemaTables(ds, schema, true, showMsg);
	}

	/**
	 * Get the list of table names in the schema
	 * 
	 * @param ds           Data source
	 * @param schema       The schema name
	 * @param schemaPrefix The prefix of the schema name
	 * @param showMsg      Whether to display exception information
	 * @return
	 */
	public static Vector<String> listSchemaTables(DataSource ds, String schema,
			boolean schemaPrefix, boolean showMsg) {
		try {
			if (ds == null || ds.isOLAP()) {
				return new Vector<String>();
			}
			return listTableNames(ds, false, schema, schemaPrefix);
		} catch (Throwable e) {
			if (showMsg) {
				GM.showException(e);
			} else {
				Logger.debug(e);
			}
		}
		return new Vector<String>();
	}

	/**
	 * Get the list of table names in the data source
	 * 
	 * @param dsName The data source name
	 * @return
	 */
	public static Vector<String> listTableNames(String dsName) {
		if (StringUtils.isValidString(dsName)) {
			if (GV.dsModel != null) {
				try {
					DataSource ds = GV.dsModel.getDataSource(dsName);
					if (ds == null || ds.isClosed()) {
						return null;
					}
					return GM.listTableNames(ds, false, null, true);
				} catch (Throwable ex) {
				}
			}
		}
		return null;
	}

	/**
	 * Get the list of table names in the schema
	 * 
	 * @param dataSource       Data source
	 * @param showSystemTables Whether to display system tables
	 * @param schema           The schema name
	 * @param schemaPrefix     The prefix of the schema name
	 * @return
	 * @throws Throwable
	 */
	public static Vector<String> listTableNames(DataSource ds,
			boolean showSystemTables, String schema, boolean schemaPrefix)
			throws Throwable {
		Vector<String> tableNames = new Vector<String>();
		DBSession session = ds.getDBSession();
		if (session.isClosed())
			return tableNames;
		boolean addTilde = ds.getDBConfig().isAddTilde();

		Connection con = (Connection) session.getSession();
		DatabaseMetaData md = con.getMetaData();

		String types[] = null;
		if (showSystemTables) {
			types = new String[] { "TABLE", "VIEW", "SYSTEM TABLE" };
		} else {
			types = new String[] { "TABLE", "VIEW" };
		}
		String catalog = con.getCatalog();
		String productName = md.getDatabaseProductName();
		if (productName != null) {
			if (productName.toLowerCase().indexOf(NAME_ANY_WHERE) > -1) {
				catalog = null;
			}
		}
		DBInfo dbInfo = ds.getDBInfo();
		String dbcs = dbInfo.getDBCharset();
		String ccs = dbInfo.getClientCharset();
		boolean convert = dbcs != null && !dbcs.equals(ccs);
		if (convert) {
			catalog = convertDBSearchString(ds, catalog);
			schema = convertDBSearchString(ds, schema);
		}
		ResultSet rs = md.getTables(catalog, schema, null, types);
		String tilde = md.getIdentifierQuoteString();
		String name, tableName, schemaName;
		while (rs.next()) {
			tableName = rs.getString("TABLE_NAME");
			if (convert)
				tableName = convertDBString(ds, tableName);
			if (ds.isUseSchema() && schemaPrefix) {
				schemaName = rs.getString("TABLE_SCHEM");
				if (convert)
					schemaName = convertDBString(ds, schemaName);
				if (addTilde) {
					name = tildeString(schemaName, tilde) + "."
							+ tildeString(tableName, tilde);
				} else {
					name = schemaName + "." + tableName;
				}
			} else {
				schemaName = null;
				if (addTilde) {
					name = tildeString(tableName, tilde);
				} else {
					name = tableName;
				}
			}

			tableNames.addElement(name);
		}
		rs.close();
		return tableNames;
	}

	public static String tildeString(String name, String tilde) {
		return tilde + name + tilde;
	}

	/**
	 * The name of anywhere
	 */
	private final static String NAME_ANY_WHERE = "anywhere";

	/**
	 * Get all the column names of the table in the data source.
	 * 
	 * @param dsName    The data source name
	 * @param tableName The table name
	 * @return
	 */
	public static Vector<String> listColumnNames(String dsName, String tableName) {
		Vector<String> colNames = new Vector<String>();
		if (!StringUtils.isValidString(dsName)
				|| !StringUtils.isValidString(tableName)) {
			return colNames;
		}
		try {
			DataSource ds = GV.dsModel.getDataSource(dsName);
			if (ds == null || ds.isClosed()) {
				return colNames;
			}
			String[] names = listColumnNames(ds, null, (String) tableName);
			if (names != null) {
				for (String name : names) {
					colNames.add(name);
				}
			}
		} catch (Throwable e) {
		}
		return colNames;
	}

	/**
	 * Get all the column names of the table in the data source.
	 * 
	 * @param ds     Data source
	 * @param schema The schema name
	 * @param table  The table name
	 * @return Returns the table names. When an error occurs, null is returned.
	 * @throws Throwable
	 */
	public static String[] listColumnNames(DataSource ds, String schema,
			String table) throws Throwable {
		return listColumnInfo(ds, schema, table, "COLUMN_NAME");
	}

	/**
	 * Get the information of the column in the table.
	 * 
	 * @param ds         Data source
	 * @param schema     The schema name
	 * @param table      The table name
	 * @param columnName The column name
	 * @throws Exception
	 * @return Vector
	 */
	private static String[] listColumnInfo(DataSource ds, String schema,
			String table, String columnName) throws Throwable {
		DBObject dbo = new DBObject(ds.getDBSession());
		Object session = dbo.getDbSession().getSession();
		if (!(session instanceof Connection)
				|| ((Connection) session).isClosed()) {
			return null;
		}
		DBInfo dbInfo = ds.getDBInfo();
		String dbcs = dbInfo.getDBCharset();
		String ccs = dbInfo.getClientCharset();
		boolean convert = dbcs != null && !dbcs.equals(ccs);
		if (convert) {
			schema = convertDBSearchString(ds, schema);
			table = convertDBSearchString(ds, table);
		}
		Connection con = (Connection) session;
		DatabaseMetaData md = con.getMetaData();
		String[] schemaTable = getRealSchemaTable(con, schema, table);
		Section columnNames = new Section();
		ResultSet rs = null;
		try {
			rs = md.getColumns(null, schemaTable[0], schemaTable[1], null);
			while (rs.next()) {
				String colName = rs.getString(columnName);
				if (convert) {
					colName = convertDBString(ds, colName);
				}
				columnNames.addSection(colName);
			}
		} finally {
			rs.close();
		}
		return columnNames.toStringArray();
	}

	/**
	 * Convert the searching string according to the database encoding
	 * 
	 * @param ds  Data source
	 * @param str The string to be converted
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String convertDBSearchString(DataSource ds, String str)
			throws UnsupportedEncodingException {
		if (str == null) {
			return null;
		}
		DBInfo dbInfo = ds.getDBInfo();
		if (!dbInfo.getNeedTranSentence()) {
			return str;
		}
		String dbcs = dbInfo.getDBCharset();
		String ccs = dbInfo.getClientCharset();
		return new String(str.getBytes(ccs), dbcs);
	}

	/**
	 * Convert the string content according to the database encoding
	 * 
	 * @param ds  Data source
	 * @param str The string to be converted
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String convertDBString(DataSource ds, String str)
			throws UnsupportedEncodingException {
		if (str == null) {
			return null;
		}
		DBInfo dbInfo = ds.getDBInfo();
		if (!dbInfo.getNeedTranContent()) {
			return str;
		}
		String dbcs = dbInfo.getDBCharset();
		String ccs = dbInfo.getClientCharset();
		return new String(str.getBytes(dbcs), ccs);
	}

	/**
	 * Get the primary key in the database table
	 * 
	 * @param dbName    The name of the database
	 * @param tableName The name of the table
	 * @return
	 */
	public static String[] getPrimaryKeys(Object dbName, Object tableName) {
		if (!StringUtils.isValidString(dbName)) {
			return null;
		}
		if (!StringUtils.isValidString(tableName)) {
			return null;
		}
		try {
			DataSource ds = GV.dsModel.getDataSource((String) dbName);
			if (ds == null || ds.isClosed()) {
				return null;
			}
			DBObject dbo = new DBObject(ds.getDBSession());
			return GM.getPrimaryKeys(dbo, null, (String) tableName);
		} catch (Throwable e) {
		}
		return null;
	}

	/**
	 * 
	 * Get the primary key in the database table
	 * 
	 * @param dbo    The DBObject
	 * @param schema The schema name
	 * @param table  The table name
	 * @return
	 * @throws Exception
	 */
	public static String[] getPrimaryKeys(DBObject dbo, String schema,
			String table) throws Exception {
		Object session = dbo.getDbSession().getSession();
		if (!(session instanceof Connection)
				|| ((Connection) session).isClosed()) {
			return null;
		}
		Connection con = (Connection) session;
		DatabaseMetaData md = con.getMetaData();
		String[] schemaTable = getRealSchemaTable(con, schema, table);
		ResultSet rs = null;
		Section columnNames = new Section();
		try {
			rs = md.getPrimaryKeys(null, schemaTable[0], schemaTable[1]);
			while (rs.next()) {
				String columnName = rs.getString("COLUMN_NAME");
				columnNames.addSection(columnName);
			}
		} finally {
			rs.close();
		}
		return columnNames.toStringArray();
	}

	/**
	 * Get the actual schema name and table name
	 * 
	 * @param con    The connection
	 * @param schema The schema name
	 * @param table  The table name
	 * @return Returns String[2]. Schema name is at index 0, Table name is at index
	 *         1.
	 * @throws SQLException
	 */
	public static String[] getRealSchemaTable(Connection con, String schema,
			String table) throws SQLException {
		String newSchema = null, newTable;
		DatabaseMetaData md = con.getMetaData();
		String tilde = md.getIdentifierQuoteString();
		int index = table.indexOf('.');
		if (index > -1) {
			newSchema = table.substring(0, index);
			if (newSchema.startsWith(tilde)) {
				newSchema = newSchema.substring(tilde.length(),
						newSchema.length() - tilde.length());
			}
			newTable = table.substring(index + 1, table.length());
			if (newTable.startsWith(tilde)) {
				newTable = newTable.substring(tilde.length(), newTable.length()
						- tilde.length());
			}
		} else {
			newTable = table;
			if (newTable.startsWith(tilde)) {
				newTable = newTable.substring(tilde.length(), newTable.length()
						- tilde.length());
			}
		}
		if (!StringUtils.isValidString(newSchema)) {
			newSchema = schema;
		}
		return new String[] { newSchema, newTable };
	}

	/**
	 * Set the editing style of the table
	 * 
	 * @param tableEx Table to be set
	 */
	public static void setEditStyle(JTableEx tableEx) {
		for (int c = 0; c < tableEx.getColumnCount(); c++) {
			TableColumn tc = tableEx.getColumn(c);
			TableCellEditor tce = tc.getCellEditor();
			if (tce == null) {
				continue;
			}
			if (tce.getClass()
					.getName()
					.equals("com.scudata.ide.common.swing.JTableEx$SimpleEditor")) {
				tc.setCellEditor(new AllPurposeEditor(new JTextField(), tableEx));
				tc.setCellRenderer(new AllPurposeRenderer());
			}
		}
	}

	/**
	 * Get the dragged cursor icon
	 * 
	 * @return
	 */
	public static Cursor getDndCursor() {
		String path = GC.IMAGES_PATH + "dnd_cursor.gif";
		Image im = GM.getImageIcon(path).getImage();
		return Toolkit.getDefaultToolkit().createCustomCursor(im,
				new Point(0, 0), "cur");
	}

	/**
	 * Parse the html format string and return the Matrix object.
	 * 
	 * @param htmlStr The html format string
	 * @return
	 */
	public static Matrix string2Matrix(String htmlStr) {
		return string2Matrix(htmlStr, false);
	}

	/**
	 * Parse the html format string and return the Matrix object.
	 * 
	 * @param htmlStr The html format string
	 * @param parse   Whether to parse value
	 * @return
	 */
	public static Matrix string2Matrix(String htmlStr, boolean parse) {
		if (!StringUtils.isValidString(htmlStr))
			return null;
		/* Initialize htmlStr */
		/* Remove the white space between tags */
		htmlStr = replaceHtmlTag(htmlStr, ">\\s+<", "><");
		/* Replace <p>...</p> with spaces */
		htmlStr = replaceHtmlTag(htmlStr, "<p\\s*/?>", " ");
		/* Replace <p> or img with spaces */
		htmlStr = replaceHtmlTag(htmlStr, "&lt;(?i)(p|img)\\s*/?&gt;", " ");
		/* Replace <br><br/> with \r */
		htmlStr = replaceHtmlTag(htmlStr, "<br\\s*/?>", "\r");
		/* Filter head tags */
		htmlStr = replaceHtmlTag(htmlStr, "<head\\s*>[\\s\\S]*?</\\s*head>");
		/* Filter script tags */
		htmlStr = replaceHtmlTag(htmlStr, "<script[^>]*?>[\\s\\S]*?<\\/script>");
		/* Filter style tags */
		htmlStr = replaceHtmlTag(htmlStr, "<style[^>]*?>[\\s\\S]*?<\\/style>");

		Matrix matrix = null, tmp;
		String[] tableTag = splitTableTag(htmlStr);
		while (tableTag != null) {
			String pre = tableTag[0];
			if (StringUtils.isValidString(pre)) {
				/* Processing normal html */
				pre = replaceHtmlTag(pre, "<+^[>]+>/g");
				pre = replaceHtmlTag(pre, "&lt;[^>]+/?&gt;");
				if (StringUtils.isValidString(pre)) {
					tmp = getStringMatrix(pre, parse);
					if (matrix == null) {
						matrix = tmp;
					} else {
						matrix = unionMatrix(matrix, tmp);
					}
				}
			}
			String center = tableTag[1];
			if (center == null)
				break;
			if (StringUtils.isValidString(center)) {
				/* Processing table tags */
				tmp = getTableTagMatrix(center);
				if (matrix == null) {
					matrix = tmp;
				} else {
					matrix = unionMatrix(matrix, tmp);
				}
			}
			if (tableTag[2] == null)
				break;
			/* Continue to process the following html */
			tableTag = splitTableTag(tableTag[2]);
		}
		/* Remove the blank line at the end */
		int rowSize = matrix.getRowSize();
		int colSize = matrix.getColSize();
		for (int i = rowSize - 1; i >= 0; i--) {
			boolean isEmptyRow = true;
			for (int j = 0; j < colSize; j++) {
				Object val = matrix.get(i, j);
				if (val == null)
					continue;
				if (!(val instanceof String)) {
					isEmptyRow = false;
					break;
				}
				if (StringUtils.isValidString(val)) {
					isEmptyRow = false;
					break;
				}
			}
			if (isEmptyRow)
				matrix.deleteRow(i);
			else
				break;
		}
		return matrix;
	}

	/**
	 * Divide the html into 3 sections according to the table tag. Return null means
	 * that there is no table tag.
	 */
	private static String[] splitTableTag(String htmlStr) {
		if (!StringUtils.isValidString(htmlStr))
			return null;
		Pattern p;
		Matcher m;
		p = Pattern.compile("<\\s*table[^>]*>", Pattern.CASE_INSENSITIVE);
		m = p.matcher(htmlStr);
		if (!m.find())
			return new String[] { htmlStr, null, null };
		int startIndex = m.start();
		String pre = htmlStr.substring(0, startIndex);
		int end = m.end();
		p = Pattern.compile("</\\s*table\\s*>", Pattern.CASE_INSENSITIVE);
		m = p.matcher(htmlStr);
		int endIndex;
		if (m.find(end)) {
			endIndex = m.end();
		} else {
			endIndex = htmlStr.length() - 1;
		}
		String center = htmlStr.substring(startIndex, endIndex);
		String suffix = null;
		if (htmlStr.length() > endIndex + 1)
			suffix = htmlStr.substring(endIndex);
		return new String[] { pre, center, suffix };
	}

	/**
	 * Parse the Table tag and return Matrix.
	 * 
	 * @param tableTag Table tag
	 * @return
	 */
	private static Matrix getTableTagMatrix(String tableTag) {
		String[] trTags = splitTableRowTag(tableTag);
		if (trTags == null || trTags.length == 0)
			return null;
		Matrix matrix = new Matrix(trTags.length, 1);
		for (int r = 0; r < trTags.length; r++) {
			String tr = trTags[r];
			String[] rowData = splitTableDataTag(tr);
			if (rowData != null && rowData.length > 0) {
				if (rowData.length > matrix.getColSize())
					matrix.addCols(rowData.length - matrix.getColSize());
				for (int c = 0; c < rowData.length; c++)
					matrix.set(r, c, rowData[c]);
			}
		}
		if (matrix.getRowSize() == 0 || matrix.getColSize() == 0)
			return null;
		return matrix;
	}

	/**
	 * Divide the table into 2 segments according to tr tags. Returning null means
	 * that there is no tr tag.
	 * 
	 * @param tableTag
	 * @return
	 */
	private static String[] splitTableRowTag(String tableTag) {
		if (!StringUtils.isValidString(tableTag))
			return null;
		List<String> dataList = new ArrayList<String>();
		int index = 0;
		Pattern p1 = Pattern.compile("<\\s*tr[^>]*>", Pattern.CASE_INSENSITIVE);
		Pattern p2 = Pattern.compile("</\\s*tr\\s*>", Pattern.CASE_INSENSITIVE);
		Matcher m1 = p1.matcher(tableTag);
		Matcher m2 = p2.matcher(tableTag);
		int startIndex, endIndex;
		while (index < tableTag.length() && m1.find(index)) {
			startIndex = m1.start();
			index = m1.end();
			if (index >= tableTag.length())
				break;
			if (m2.find(index)) {
				endIndex = m2.end();
				index = m2.end();
			} else {
				endIndex = tableTag.length() - 1;
				index = endIndex;
			}
			dataList.add(tableTag.substring(startIndex, endIndex));
		}
		if (dataList.isEmpty())
			return null;
		String[] datas = new String[dataList.size()];
		for (int i = 0; i < datas.length; i++) {
			Object data = dataList.get(i);
			datas[i] = data == null ? null : (String) data;
		}
		return datas;
	}

	/**
	 * Separate a row (tr) of the table according to the th/td label. Returning null
	 * means that there is no tr tag.
	 * 
	 * @param trTag Table row tag
	 * @return
	 */
	private static String[] splitTableDataTag(String trTag) {
		if (!StringUtils.isValidString(trTag))
			return null;
		List<String> dataList = new ArrayList<String>();
		int index = 0;
		Pattern p1 = Pattern.compile("<\\s*t[hd][^>]*>",
				Pattern.CASE_INSENSITIVE);
		Pattern p2 = Pattern.compile("</\\s*t[hd]\\s*>",
				Pattern.CASE_INSENSITIVE);
		Matcher m1 = p1.matcher(trTag);
		Matcher m2 = p2.matcher(trTag);
		int startIndex, endIndex;
		while (index < trTag.length() && m1.find(index)) {
			startIndex = m1.end();
			index = m1.end();
			if (index >= trTag.length() - 1)
				break;
			if (m2.find(index)) {
				endIndex = m2.start();
				index = m2.end();
			} else {
				endIndex = trTag.length() - 1;
				index = endIndex;
			}
			String data = trTag.substring(startIndex, endIndex);
			data = replaceHtmlTag(data, "<[^>]+>");
			data = replaceHtmlTag(data, "&lt;[^>]+/?&gt;");
			dataList.add(data);
		}
		if (dataList.isEmpty())
			return null;
		String[] datas = new String[dataList.size()];
		for (int i = 0; i < datas.length; i++) {
			Object data = dataList.get(i);
			datas[i] = data == null ? null : (String) data;
		}
		return datas;
	}

	/**
	 * Remove the tag in html
	 * 
	 * @param htmlStr Html format string
	 * @param regEx   The regular expression to delete
	 * @return
	 */
	private static String replaceHtmlTag(String htmlStr, String regEx) {
		htmlStr = replaceHtmlTag(htmlStr, regEx, "");
		if (htmlStr != null)
			htmlStr.trim();
		return htmlStr;
	}

	/**
	 * Replace the tag in html
	 * 
	 * @param htmlStr    Html format string
	 * @param regEx      The regular expression to replace
	 * @param replaceStr Replace with
	 * @return
	 */
	private static String replaceHtmlTag(String htmlStr, String regEx,
			String replaceStr) {
		Pattern p = Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(htmlStr);
		return m.replaceAll(replaceStr);
	}

	/**
	 * Union two Matrix
	 * 
	 * @param m1 Matrix
	 * @param m2 Matrix
	 * @return The merged Matrix
	 */
	private static Matrix unionMatrix(Matrix m1, Matrix m2) {
		if (m1 == null)
			return m2;
		if (m2 == null)
			return m1;
		int colSize1 = m1.getColSize(), colSize2 = m2.getColSize();
		if (colSize2 > colSize1)
			m1.addCols(colSize2 - colSize1);
		int rowSize1 = m1.getRowSize(), rowSize2 = m2.getRowSize();
		m1.addRows(rowSize2);
		for (int r = 0; r < rowSize2; r++) {
			for (int c = 0; c < colSize2; c++) {
				m1.set(rowSize1 + r, c, m2.get(r, c));
			}
		}
		return m1;
	}

	/**
	 * Parse string into Matrix
	 * 
	 * @param data  String separated by \n and \t
	 * @param parse
	 * @return
	 */
	public static Matrix getStringMatrix(String data, boolean parse) {
		int r = 0, c = 0;
		String ls_row;
		try {
			data = data.replaceAll("\r\n", "\r");
			data = data.replaceAll("\n", "\r");
		} catch (Exception x) {
		}
		Matrix matrix = new Matrix(1, 1);

		ArgumentTokenizer rows = new ArgumentTokenizer(data, '\r', true, true,
				true, true);
		while (rows.hasMoreTokens()) {
			ls_row = rows.nextToken();
			ArgumentTokenizer items = new ArgumentTokenizer(ls_row, '\t', true,
					true, true, true);
			String item;
			c = 0;
			if (r >= matrix.getRowSize()) {
				matrix.addRow();
			}
			while (items.hasMoreTokens()) {
				if (c >= matrix.getColSize()) {
					matrix.addCol();
				}
				item = items.nextToken();
				Object val = item;
				if (parse) {
					if (item.startsWith(KeyWord.CONSTSTRINGPREFIX)
							&& !item.endsWith(KeyWord.CONSTSTRINGPREFIX)) { // 字符串常数'
						val = item.substring(1);
					} else {
						val = Variant.parseCellValue(item);
					}
				}
				matrix.set(r, c, val);
				c++;
			}
			r++;
		}

		return matrix;
	}

	/**
	 * Maximum number of columns in the cellset
	 */
	private static final int MAX_COLUMN = 999;

	/**
	 * Letters in column names
	 */
	private static final String[] STR_COLID = { "A", "B", "C", "D", "E", "F",
			"G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S",
			"T", "U", "V", "W", "X", "Y", "Z" };

	/**
	 * Convert the row and column values into the string identifier of the cell.
	 * Convert the column value as a 26-base label.
	 * 
	 * @param iRow Row number
	 * @param iCol Column number
	 * @return cell的字符串标识,如果列值大于最大列值，将cell设为第一列
	 */
	public static String getCellID(int iRow, int iCol) {
		String retVal = "";
		--iCol;
		if (iCol > MAX_COLUMN) {
			retVal += "A" + iRow;
		} else {
			while (iCol >= 0) {
				retVal = STR_COLID[iCol % 26] + retVal;
				iCol = iCol / 26 - 1;
			}
			retVal += iRow;
		}
		return retVal;
	}

	/**
	 * Convert column labels to column number.
	 * 
	 * @param colName The column name
	 * @return
	 */
	public static int getColByName(String colName) {
		if (!StringUtils.isValidString(colName)) {
			return -1;
		}
		int col = 0;
		char c;
		for (int i = colName.length() - 1; i >= 0; i--) {
			c = colName.charAt(i);
			int j = 0;
			for (; j < STR_COLID.length; j++) {
				if (STR_COLID[j].equals(c + ""))
					break;
			}
			col += j + Math.pow(STR_COLID.length, i);
		}
		return col;
	}

	/**
	 * Convert the matrix to a string.
	 * 
	 * @param matrix   Matrix
	 * @param useValue Whether to copy the value. Use the cell value when true, and
	 *                 use the expression when false.
	 * @return
	 */
	public static String getCellSelectionString(Matrix matrix, boolean useValue) {
		if (matrix == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		NormalCell nc;
		String exp;
		boolean isSingleCell = matrix.getRowSize() == 1
				&& matrix.getColSize() == 1;
		for (int i = 0; i < matrix.getRowSize(); i++) {
			for (int j = 0; j < matrix.getColSize(); j++) {
				if (j != 0) {
					sb.append("\t");
				}
				nc = (NormalCell) matrix.get(i, j);
				if (nc != null) {
					if (useValue) {
						try {
							if (Variant.canConvertToString(nc.getValue())) {
								exp = Variant.toExportString(nc.getValue());
							} else {
								throw new Exception(IdeCommonMessage.get()
										.getMessage(
												"gm.canttostr",
												getCellID(nc.getRow(),
														nc.getCol())));
							}
						} catch (Exception ex) {
							showException(ex);
							exp = null;
						}
					} else {
						exp = nc.getExpString();
					}
				} else {
					exp = "";
				}
				if (StringUtils.isValidString(exp)) {
					if (!isSingleCell) {
						exp = exp.replaceAll("\t", " ");
						exp = exp.replaceAll("\r", "");
						exp = exp.replaceAll("\n", " ");
					}
					sb.append(exp);
				}

			}
			if (i != matrix.getRowSize() - 1) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	/**
	 * Get addressing paths
	 * 
	 * @return
	 */
	public static String[] getPaths() {
		if (StringUtils.isValidString(ConfigOptions.sPaths)) {
			String[] paths = ConfigOptions.sPaths.split(";");
			if (paths != null) {
				for (int i = 0; i < paths.length; i++) {
					paths[i] = ConfigUtil.getPath(
							System.getProperty("start.home"), paths[i]);
				}
				return paths;
			}
		}
		return null;
	}

	/**
	 * Set up a temporary main path
	 * 
	 * @param filePath
	 */
	public static void setCurrentPath(String filePath) {
		try {
			/*
			 * When the main path is not set in the IDE, the directory where the
			 * current spl file is located is used.
			 */
			if (!StringUtils.isValidString(ConfigOptions.sMainPath)) {
				if (StringUtils.isValidString(filePath)) {
					File f = new File(filePath);
					if (f.isFile() && f.exists()) {
						Env.setMainPath(f.getParent());
						return;
					}
				}
				/*
				 * When the current file is not saved, the current path of the
				 * system is used.
				 */
				Env.setMainPath(System.getProperty("user.dir"));
			}
		} catch (Throwable ex) {
		}
	}

	/**
	 * Load files in the order of absolute path and class path.
	 * 
	 * @param filePath The file path
	 * @return File input stream
	 * @throws Exception
	 */
	public static InputStream getFileInputStream(String filePath)
			throws Exception {
		InputStream is = null;
		try {
			is = new FileInputStream(filePath);
		} catch (Exception e) {
		}

		if (is == null) {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			if (cl != null) {
				try {
					is = cl.getResourceAsStream(filePath);
				} catch (Exception e) {
				}
			}
		}
		if (is == null) {
			try {
				is = GM.class.getResourceAsStream(filePath);
			} catch (Exception e) {
			}
		}
		return is;
	}

	/**
	 * Get the absolute coordinates of the control in the window
	 * 
	 * @param c      Component
	 * @param isGetX boolean
	 * @return int
	 */
	public static int getAbsolutePos(Component c, boolean isGetX) {
		if (c == null) {
			return 0;
		} else {
			return (isGetX ? c.getX() : c.getY())
					+ getAbsolutePos(c.getParent(), isGetX);
		}
	}

	/**
	 * Whether the data source name exists.
	 * 
	 * @param ds The data source
	 * @return
	 */
	public static boolean isExistDataSource(DataSource ds) {
		if (GV.dsModel.existDSName(ds.getName())) {
			JOptionPane.showMessageDialog(GV.appFrame, IdeCommonMessage.get()
					.getMessage("dialogdatasource.existdsname", ds.getName()),
					IdeCommonMessage.get().getMessage("public.note"),
					JOptionPane.ERROR_MESSAGE);
			return true;
		}
		return false;
	}

	/**
	 * Set the text into the text component
	 * 
	 * @param textEditor JTextComponent
	 * @param text       Entered text
	 */
	public static void addText(JTextComponent textEditor, String text) {
		if (text == null) {
			return;
		}
		if (!textEditor.isEnabled()) {
			return;
		}
		try {
			String exp = textEditor.getText();
			int pos = textEditor.getCaretPosition();
			String select = textEditor.getSelectedText();
			int length = 0;
			if (select != null && !select.equals("")) {
				length = select.length();
			}
			if (pos + length <= exp.length()
					&& exp.substring(pos, pos + length).equals(select)) {
				exp = exp.substring(0, pos) + exp.substring(pos + length);
			} else if (pos - length >= 0
					&& exp.substring(pos - length, pos).equals(select)) {
				exp = exp.substring(0, pos - length) + exp.substring(pos);
				pos = pos - length;
			}
			if (pos == 0) {
				exp = text + exp;
			} else if (pos >= exp.length()) {
				exp += text;
			} else {
				exp = exp.substring(0, pos) + text + exp.substring(pos);
			}
			textEditor.setText(exp);
			textEditor.requestFocus();
			textEditor.setCaretPosition(pos + text.length());
		} catch (Throwable t) {
		}
	}

	/**
	 * Get the description of the function
	 * 
	 * @param fi FuncInfo
	 * @return
	 */
	public static String getFuncDesc(FuncInfo fi) {
		return getFuncDesc(fi, null, null, -1);
	}

	/**
	 * Get the description of the function
	 * 
	 * @param fi          FuncInfo
	 * @param efo         The option under the cursor
	 * @param activeParam Current parameter
	 * @param paramPos    The position of the cursor in the parameter
	 * @return
	 */
	public static String getFuncDesc(FuncInfo fi, String efo,
			FuncParam activeParam, int paramPos) {
		StringBuffer desc = new StringBuffer();
		final String blankStr = "&nbsp;&nbsp;";
		desc.append(fi.getDesc());
		ArrayList<FuncOption> options = fi.getOptions();
		FuncOption fo;
		if (options != null && options.size() > 0) {
			desc.append("<br>");
			desc.append(IdeCommonMessage.get().getMessage("gm.funcopt"));
			desc.append(":");
			String optDesc;
			for (int i = 0; i < options.size(); i++) {
				fo = options.get(i);
				optDesc = "(" + fo.getOptionChar() + ") " + fo.getDescription();
				if (StringUtils.isValidString(efo)) {
					if (efo.indexOf(fo.getOptionChar()) > -1) {
						/* Bold current option */
						optDesc = "<b>" + optDesc + "</b>";
					}
				}
				desc.append("<br>");
				desc.append(blankStr);
				desc.append(optDesc);
			}
		}
		ArrayList<FuncParam> params = fi.getParams();
		if (params != null) {
			desc.append("<br>");
			desc.append(IdeCommonMessage.get().getMessage("gm.funcparam") + ":");
			FuncParam fp;
			boolean isActiveParam;
			String paramDesc, paramValue;
			int optPos;
			char activeOptChar;
			for (int i = 0; i < params.size(); i++) {
				fp = params.get(i);
				isActiveParam = activeParam != null
						&& fp.getDesc().equals(activeParam.getDesc());
				desc.append("<br>");
				desc.append(blankStr);
				paramDesc = fp.getDesc();
				activeOptChar = '@';
				if (isActiveParam) {
					/* Bold current param */
					paramDesc = "<b>" + paramDesc + "</b>";
					paramValue = activeParam.getParamValue();
					optPos = paramValue.indexOf("@");
					if (optPos > -1 && paramPos > optPos) {
						/* The cursor is on the parameter option */
						activeOptChar = paramValue.charAt(paramPos - 1);
					}
				}
				desc.append((i + 1) + ". " + paramDesc);
				options = fp.getOptions();
				if (options != null) {
					desc.append("<br>");
					desc.append(blankStr);
					desc.append(blankStr);
					desc.append(IdeCommonMessage.get().getMessage("gm.funcopt")
							+ ":");
					String charDesc;
					for (int j = 0; j < options.size(); j++) {
						fo = (FuncOption) options.get(j);
						charDesc = "(" + fo.getOptionChar() + ") "
								+ fo.getDescription();
						if (fo.getOptionChar().equals(activeOptChar + "")) {
							/* Bold current option */
							charDesc = "<b>" + charDesc + "</b>";
						}
						desc.append("<br>");
						desc.append(blankStr);
						desc.append(blankStr);
						desc.append(blankStr);
						desc.append(charDesc);
					}
				}
			}
		}
		return desc.toString();
	}

	/**
	 * Convert the tips into a html format string for display.
	 * 
	 * @param tips
	 * @return
	 */
	public static String transTips(String tips) {
		return transTips(tips, null);
	}

	/**
	 * Convert the tips into a html format string for display.
	 * 
	 * @param tips
	 * @param list Container for storing information such as width and height
	 * @return
	 */
	public static String transTips(String tips, IntArrayList list) {
		return transTips(tips, list, new JTextArea().getFontMetrics(GC.font),
				GC.TIP_WIDTH);
	}

	/**
	 * Convert the tips into a html format string for display.
	 * 
	 * @param tips
	 * @param list     Container for storing information such as width and height
	 * @param fm       FontMetrics
	 * @param maxWidth Maximum width
	 * @return
	 */
	public static String transTips(String tips, IntArrayList list,
			FontMetrics fm, int maxWidth) {
		if (!StringUtils.isValidString(tips)) {
			return "";
		}
		if (tips.toLowerCase().trim().startsWith("<html>")) {
			return tips;
		}
		tips = tips.replaceAll("<br>", "\r");
		tips = tips.replaceAll("\r\n", "\r");
		tips = tips.replaceAll("\n", "\r");
		tips = tips.replaceAll("\t", " ");
		ArgumentTokenizer rows = new ArgumentTokenizer(tips, '\r', true, true,
				true);
		String rowTips;
		boolean first = true;
		StringBuffer htmlTips = new StringBuffer();
		int colCount = 0;
		int tipWidth = 0;
		while (rows.hasMoreTokens()) {
			rowTips = rows.nextToken();
			int width = 0;
			for (int i = 0; i < rowTips.length(); i++) {
				width += fm.charWidth(rowTips.charAt(i));
				if (width > maxWidth) {
					if (first) {
						first = false;
					} else {
						htmlTips.append("<br>");
					}
					htmlTips.append(rowTips.substring(0, i));
					colCount++;
					rowTips = rowTips.substring(i);
					i = -1;
					width = 0;
				} else {
					if (width > tipWidth) {
						tipWidth = width;
					}
				}
			}
			if (first) {
				first = false;
			} else {
				htmlTips.append("<br>");
			}
			htmlTips.append(rowTips);
			colCount++;
		}
		if (colCount * fm.getHeight() > maxWidth) {
			return transTips(tips, list,
					new JTextArea().getFontMetrics(GC.font),
					(int) (maxWidth * 1.5));
		}
		if (list != null) {
			list.addInt(tipWidth);
			list.addInt(colCount * fm.getHeight());
		}
		tips = htmlTips.toString();
		tips = tips.replaceAll("&", "&amp;");
		tips = tips.replaceAll(" ", "&nbsp;");
		tips = tips.replaceAll("\"", "&quot;");
		return "<html>" + "&nbsp;" + tips + "</html>";
	}

	/**
	 * Prepare the parent context
	 * 
	 * @return
	 */
	public static Context prepareParentContext() {
		Context context = new Context();
		for (int c = 0; c < GV.dsModel.size(); c++) {
			DataSource ds = (DataSource) GV.dsModel.getElementAt(c);
			if (ds == null || ds.isClosed()) {
				continue;
			}
			try {
				context.setDBSession(ds.getName(), ds.getDBSession());
			} catch (Throwable ex) {
				GM.showException(ex);
			}
		}
		return context;
	}

	/**
	 * Whether it is a newly created cellset
	 * 
	 * @param filePath The path of the cellset
	 * @param pre      Name prefix
	 * @return
	 */
	public static boolean isNewGrid(String filePath, String pre) {
		if (filePath.equals(pre)) {
			return true;
		}
		if (filePath.startsWith(pre)) {
			String end = filePath.substring(pre.length());
			if (end.startsWith("_"))
				end = end.substring(1);
			try {
				Integer.parseInt(end);
				return true;
			} catch (Exception ex) {
			}
		}
		return false;
	}

	/**
	 * 删除文件或目录
	 * @param f 文件或目录
	 * @return 是否删除成功
	 */
	public static boolean deleteFile(File f) {
		if (f == null || !f.exists())
			return true;
		if (f.isFile()) {
			return f.delete();
		} else if (f.isDirectory()) {
			File[] subFiles = f.listFiles();
			if (subFiles != null)
				for (File sub : subFiles) {
					deleteFile(sub);
				}
			return f.delete();
		}
		return false;
	}

	/**
	 * Get the language suffix
	 * 
	 * @return
	 */
	public static String getLanguageSuffix() {
		switch (GC.LANGUAGE) {
		case GC.ASIAN_CHINESE:
			return "_zh";
		case GC.ASIAN_CHINESE_TRADITIONAL:
			return "_zh_TW";
		default:
			return "_en";
		}
	}

	/**
	 * Whether Data Logic data source
	 * 
	 * @param ds
	 * @return
	 */
	public static boolean isDataLogicDS(DataSource ds) {
		if (ds.getDBInfo() instanceof DBConfig) {
			String driver = ((DBConfig) ds.getDBInfo()).getDriver();
			if (driver.equalsIgnoreCase("com.datalogic.jdbc.LogicDriver")) {
				return true;
			} else if (driver
					.equalsIgnoreCase("com.datasphere.httpjdbc.client.HttpDriver")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Reset the style of all sheets.
	 */
	public static void resetAllSheetStyle() {
		JInternalFrame[] sheets = GV.appFrame.getAllSheets();
		if (sheets == null)
			return;
		for (int i = 0; i < sheets.length; i++) {
			((IPrjxSheet) sheets[i]).resetSheetStyle();
		}
	}

	/**
	 * Use a browser to access the URL
	 * 
	 * @param url URL to visit
	 * @throws Exception
	 */
	public static void browse(String url) throws Exception {
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.startsWith("mac os")) {
			Class fileMgr = Class.forName("com.apple.eio.FileManager");
			Method openURL = fileMgr.getDeclaredMethod("openURL",
					new Class[] { String.class });
			openURL.invoke(null, new Object[] { url });
		} else if (osName.startsWith("windows")) {
			Runtime.getRuntime().exec(
					"rundll32 url.dll,FileProtocolHandler " + url);
		} else {
			/* Assume Unix or Linux */
			try {
				URI uri = new URI(url);
				Desktop desktop = null;
				if (Desktop.isDesktopSupported()) {
					desktop = Desktop.getDesktop();
				}
				if (desktop != null) {
					desktop.browse(uri);
					return;
				}
			} catch (Throwable t) {
			}
			String[] browsers = { "google-chrome", "firefox", "opera",
					"konqueror", "epiphany", "mozilla", "netscape" };
			String browser = null;
			for (int count = 0; count < browsers.length && browser == null; count++)
				if (Runtime.getRuntime()
						.exec(new String[] { "which", browsers[count] })
						.waitFor() == 0)
					browser = browsers[count];
			if (browser == null)
				throw new NoSuchMethodException("Could not find web browser");
			else {
				Logger.info("cmd: " + browser + ", " + url);
				Runtime.getRuntime().exec(new String[] { browser, url });
			}
		}
	}

	/**
	 * Get line separator
	 * 
	 * @return
	 */
	public static String getLineSeparator() {
		return isWindowsOS() ? "\n" : System.getProperties().getProperty(
				"line.separator");
	}

	/**
	 * Open a file using the command line
	 * 
	 * @param filePath The file path
	 * @throws IOException
	 */
	public static void openFile(String filePath) throws IOException {
		Runtime.getRuntime().exec("cmd.exe /c start \"\" \"" + filePath + "\"");
	}

	/**
	 * Get the type of picture
	 * 
	 * @param imageBytes
	 * @return
	 * @throws IOException
	 */
	public static String getImageType(final byte[] imageBytes)
			throws IOException {
		ByteArrayInputStream input = new ByteArrayInputStream(imageBytes);
		ImageInputStream imageInput = ImageIO.createImageInputStream(input);
		Iterator<ImageReader> iterator = ImageIO.getImageReaders(imageInput);
		String type = null;
		if (iterator.hasNext()) {
			ImageReader reader = iterator.next();
			type = reader.getFormatName().toUpperCase();
		}

		try {
			return type;
		} finally {
			if (imageInput != null) {
				imageInput.close();
				imageInput = null;
			}
		}
	}

	/**
	 * The display value of the Locale that can be supported.
	 * 
	 * @return
	 */
	public static Vector<String> getDispLocale() {
		MessageManager mm = IdeCommonMessage.get();
		Vector<String> v = new Vector<String>();
		v.add(mm.getMessage("gm.english"));
		v.add(mm.getMessage("gm.simplechinese"));
		return v;
	}

	/**
	 * The code value of the Locale that can be supported.
	 * 
	 * @return
	 */
	public static Vector<Byte> getCodeLocale() {
		Vector<Byte> v = new Vector<Byte>();
		v.add(new Byte(GC.ENGLISH));
		v.add(new Byte(GC.ASIAN_CHINESE));
		return v;
	}

	/**
	 * Get the height of the page
	 * 
	 * @param scale
	 * @return
	 */
	public static int getPageHeight(float scale) {
		int height = GV.appSheet.getHeight();
		height -= GV.appSheet.getTitleHeight();
		height -= GC.DEFAULT_ROW_HEIGHT * scale;
		return new Double(height).intValue() - 5;
	}

	/**
	 * Get the width of the page
	 * 
	 * @param scale
	 * @return
	 */
	public static int getPageWidth(float scale) {
		int width = GV.appSheet.getWidth();
		width -= 10; /* JScrollBar */
		width -= GC.DEFAULT_ROWHEADER_WIDTH * scale;
		return new Double(width).intValue() - 5;
	}

	/**
	 * Whether to display the group table block size
	 * 
	 * @return
	 */
	public static boolean isBlockSizeEnabled() {
		/* Open at 2018-06-11 */
		return true;
	}

	/**
	 * 获取SQL的部分
	 * 
	 * @param sql
	 * @param part
	 * @return
	 */
	public static String getPartOfSql(String sql, String part) {
		sql = removeSqlNote(sql);
		Object fromObj = com.scudata.dm.sql.SQLUtil.parse(sql, part);
		if (fromObj == null) // null转为空串，方便IDE拼SQL
			return "";
		if (fromObj instanceof Sequence) {
			StringBuffer buf = new StringBuffer();
			Sequence seq = (Sequence) fromObj;
			// 这里不加引号，也不加中括号，所以不使用Sequence转串方法
			for (int i = 1, len = seq.length(); i <= len; i++) {
				if (i > 1)
					buf.append(",");
				buf.append(seq.get(i));
			}
			return buf.toString();
		} else {
			return (String) fromObj;
		}
	}

	/**
	 * 替换部分SQL
	 * 
	 * @param sql
	 * @param replace
	 * @param part
	 * @return
	 */
	public static String modifySql(String sql, String replace, String part) {
		sql = removeSqlNote(sql);
		return com.scudata.dm.sql.SQLUtil.replace(sql, replace, part);
	}

	/**
	 * 使用正则表达式去除SQL注释
	 */
	public static String removeSqlNote(String sql) {
		if (sql == null)
			return null;
		sql = sql.replaceAll("(/\\*.*?\\*/)|(--.*)", "");
		return sql;
	}

	/**
	 * 取当前选择的词起始位置
	 * @param str
	 * @param p
	 * @return
	 */
	public static int[] getCurrentWordPosition(String str, int p) {
		if (p > 0) {
			if (isSymbol(str.charAt(p))) { // 光标位置是符号
				if (isSymbol(str.charAt(p - 1))) { // 光标前一位也是符号
					return null;
				} else { // 移动到光标前一位，使p位是字符
					p = p - 1;
				}
			}
		} else if (p == 0) { // 光标在第一个位置
			if (isSymbol(str.charAt(p))) { // 第一位不是字符
				return null;
			}
		} else {
			return null;
		}
		int start = p;
		int end = p;
		for (int i = p - 1; i >= 0; i--) { // 从字符位置向前找
			char c = str.charAt(i);
			if (isSymbol(c)) {
				start = i + 1;
				break;
			}
		}
		for (int i = p + 1; i < str.length(); i++) { // 从字符位置向后找
			char c = str.charAt(i);
			if (isSymbol(c)) {
				end = i;
				break;
			}
		}
		if (start >= end)
			return null;
		return new int[] { start, end };
	}
	
	private static boolean isSymbol(char c) {
		return KeyWord.isSymbol(c) || c == KeyWord.OPTION;
	}
}