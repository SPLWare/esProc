package com.scudata.ide.common;

import java.awt.Cursor;
import java.awt.Font;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenuBar;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import com.scudata.common.Logger;
import com.scudata.common.StringUtils;
import com.scudata.expression.FunctionLib;

/**
 * The base class of the IDE main interface
 *
 */
public abstract class AppFrame extends JFrame implements IAppFrame {
	private static final long serialVersionUID = 1L;

	/**
	 * Multiple document container
	 */
	protected JDesktopPane desk;

	/**
	 * Constructor
	 */
	public AppFrame() {
		super("");
		/*
		 * CTRL-F9 in swing is meaningful. In order to give the shortcut key to
		 * execute the function, log out the default function of swing.
		 */
		try {
			UIManager.put("Desktop.ancestorInputMap",
					new UIDefaults.LazyInputMap(
							new Object[] { "ctrl F9", null }));
		} catch (Exception ex) {
			Logger.error(ex);
		}
		/* Load custom functions */
		// 改为在raqsoftConfig.xml中配置和加载了
		// String relativePath = "/config/customFunctions.properties";
		// try {
		// InputStream is = GM.getFileInputStream(relativePath);
		// if (is != null) {
		// FunctionLib.loadCustomFunctions(is);
		// }
		// } catch (Exception x) {
		// Logger.error(x);
		// }
		/*
		 * When the execution of the grid was stopped, a FunctionLib class
		 * initialization error occurred occasionally. So it is initialized when
		 * the IDE starts.
		 */
		FunctionLib.isFnName("");
	}

	/**
	 * Take over the console
	 */
	public static void holdConsole() {
		Font font;
		if (StringUtils.isValidString(ConfigOptions.sFontName)) {
			font = new Font(ConfigOptions.sFontName, Font.PLAIN,
					ConfigOptions.iFontSize.intValue());
		} else {
			font = GC.font;
		}
		if (GV.console == null) {
			JTextArea jta = new JTextArea();
			jta.setFont(font);
			GV.console = new Console(jta);
		} else {
			GV.console.getTextArea().setFont(font);
		}
	}

	/**
	 * Get product name
	 * 
	 * @return
	 */
	public abstract String getProductName();

	/**
	 * Close IDE
	 */
	public abstract boolean exit();

	/**
	 * Exit the IDE and call exit() after closing all editing windows
	 */
	public abstract void quit();

	/**
	 * Open an edit file
	 */
	public abstract JInternalFrame openSheetFile(String fileName)
			throws Exception;

	/**
	 * Add a new method to open the input stream file, without using abstract
	 * methods to facilitate inheritance, overwriting and rewriting
	 * 
	 * @param in
	 *            Input stream
	 * @param fileName
	 *            file name
	 * @param type
	 *            SheetSplC.TYPE_REMOTE,SheetSplC.TYPE_CLOUD 
	 * @return
	 */
	public JInternalFrame openSheetFile(InputStream in, String fileName,
			byte sheetType) throws Exception {
		return null;
	}

	/**
	 * Switch menu and toolbar
	 * 
	 * @param menu
	 * @param toolBar
	 */
	public abstract void changeMenuAndToolBar(JMenuBar menu, JToolBar toolBar);

	/**
	 * Close the editing window
	 * 
	 * @param sheet
	 * @return
	 */
	public abstract boolean closeSheet(Object sheet);

	/**
	 * Close all editing windows
	 */
	public abstract boolean closeAll();

	/**
	 * After the environment variable changes, reset the running state of the
	 * application
	 */
	public void resetRunStatus() {
	}

	/**
	 * Get multiple document container
	 * 
	 * @return
	 */
	public JDesktopPane getDesk() {
		return desk;
	}

	/**
	 * Set product type
	 * 
	 * @param part
	 */
	ImageIcon frameIcon = null;

	public void setProgramPart() {
		String fixTitle = getFixTitle();
		setTitle(fixTitle);
		frameIcon = GM.getLogoImage(true);
		if (frameIcon != null) {
			setIconImage(frameIcon.getImage());
		}
	}

	/**
	 * Get application image icon
	 * @return
	 */
	public ImageIcon getFrameIcon() {
		return frameIcon;
	}

	/**
	 * Refresh title bar
	 * 
	 * @return
	 */
	public String resetTitle() {
		String fixTitle = getFixTitle();
		IPrjxSheet sheet = GV.appSheet;
		if (sheet != null) {
			String sheetTitle = sheet.getSheetTitle();
			fixTitle += "  [" + sheetTitle + "]";
		}
		setTitle(fixTitle);
		return fixTitle;
	}

	/**
	 * 取发布时间
	 * 
	 * @return
	 */
	public String getReleaseDate() {
		return "";
	}

	/**
	 * Multi-window display
	 * 
	 * @param cmd
	 * @throws Exception
	 */
	public void arrangeSheet(short cmd) throws Exception {
		JInternalFrame[] frames = desk.getAllFrames();
		if (frames.length == 0) {
			return;
		}
		int x = 0, y = 0, w = 450, h = 280;
		switch (cmd) {
		case GC.iCASCADE:
			for (int i = 0; i < frames.length; i++) {
				if (!frames[i].isIcon()) {
					frames[i].setMaximum(false);
					frames[i].setBounds(x, y, w, h);
					frames[i].setSelected(true);
					x += 20;
					y += 20;
				}
			}
			break;
		case GC.iTILEHORIZONTAL:
			int deskWidth = desk.getWidth();
			w = deskWidth / frames.length;
			h = desk.getHeight();
			for (int i = 0; i < frames.length; i++) {
				x = i * w;
				if (!frames[i].isIcon()) {
					frames[i].setMaximum(false);
					frames[i].setBounds(x, y, w, h);
					frames[i].update(frames[i].getGraphics());
				}
			}
			break;
		case GC.iTILEVERTICAL:
			int deskHeight = desk.getHeight();
			h = deskHeight / frames.length;
			w = desk.getWidth();
			for (int i = 0; i < frames.length; i++) {
				y = i * h;
				if (!frames[i].isIcon()) {
					frames[i].setMaximum(false);
					frames[i].setBounds(x, y, w, h);
					frames[i].update(frames[i].getGraphics());
				}
			}
			break;
		case GC.iLAYER:
			JInternalFrame f = desk.getSelectedFrame();
			if (f != null) {
				f.setMaximum(true);
				f.setBounds(0, 0, desk.getWidth(), desk.getHeight());
			}
			break;
		}
		desk.invalidate();
		desk.update(desk.getGraphics());
	}

	/**
	 * Displayed title
	 * 
	 * @return
	 */
	public String getFixTitle() {
		String title = getProductName();
		return title;
	}

	/**
	 * Open connection
	 * 
	 * @param dsName
	 */
	public void openConnection(String dsName) {
		try {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			DataSource ds = GV.dsModel.getDataSource(dsName);
			if (ds != null) {
				ds.getDBSession();
			}
		} catch (Throwable e2) {
			GM.showException(e2);
		} finally {
			setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Get edit window object by name
	 * 
	 * @param title
	 * @return
	 */
	public JInternalFrame getSheet(String title) {
		JInternalFrame[] frames = desk.getAllFrames();
		for (int i = 0; i < frames.length; i++) {
			if (frames[i].getTitle().equalsIgnoreCase(title)) {
				return frames[i];
			}
		}
		return null;
	}

	/**
	 * Show editing window
	 * 
	 * @param s
	 * @return
	 * @throws Exception
	 */
	public boolean showSheet(JInternalFrame s) throws Exception {
		return showSheet(s, true);
	}

	/**
	 * Show editing window
	 * 
	 * @param s
	 * @param showIcon
	 *            Whether to display as an icon
	 * @return
	 * @throws Exception
	 */
	public boolean showSheet(JInternalFrame s, boolean showIcon)
			throws Exception {
		if (s == null) {
			return false;
		}
		boolean setMax = false;
		if (GV.appSheet != null) {
			if (!GV.appSheet.submitEditor()) {
				return false;
			}
			if (GV.appSheet.isMaximum() && !GV.appSheet.isIcon()) {
				setMax = true;
				GV.appSheet.resumeSheet();
				if (!s.isMaximum() || (s.isIcon() && s.isMaximum()))
					((IPrjxSheet) s).setForceMax();
			}
		}
		s.toFront();
		s.show();
		s.setSelected(true);
		if (showIcon && s.isIcon())
			s.setIcon(false);
		if (setMax) {
			s.setMaximum(true);
		}
		return true;
	}

	/**
	 * Get the active sheet
	 * 
	 * @return
	 */
	public JInternalFrame getActiveSheet() {
		return desk.getSelectedFrame();
	}

	/**
	 * Initialize IDE directories
	 */
	public static void resetInstallDirectories() {
		String startHome = System.getProperty("start.home");
		if (!StringUtils.isValidString(startHome)) {
			System.setProperty("raqsoft.home", System.getProperty("user.home"));
		} else {
			System.setProperty("raqsoft.home", startHome + "");
		}
		String[] path = new String[] { GC.PATH_CONFIG, GC.PATH_LOGO,
				GC.PATH_TMP, GC.PATH_BACKUP };
		for (int i = 0; i < path.length; i++) {
			File f = new File(GM.getAbsolutePath(path[i]));
			if (!f.exists()) {
				f.mkdirs();
			}
		}
	}

	/**
	 * Get all edit window objects
	 * 
	 * @return
	 */
	public JInternalFrame[] getAllSheets() {
		return desk.getAllFrames();
	}

	/**
	 * Initialize font
	 * 
	 * @param fnt
	 */
	public static void initGlobalFontSetting(Font fnt) {
		FontUIResource fontRes = new FontUIResource(fnt);
		Enumeration<?> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof FontUIResource) {
				UIManager.put(key, fontRes);
			}
		}
	}

	/**
	 * Get the list of sheet names
	 * 
	 * @return
	 */
	public List<String> getSheetNameList() {
		List<String> sheetNameList = new ArrayList<String>();
		JInternalFrame[] frames = desk.getAllFrames();
		for (JInternalFrame jInternalFrame : frames) {
			sheetNameList.add(jInternalFrame.getTitle());
		}
		return sheetNameList;
	}

}
