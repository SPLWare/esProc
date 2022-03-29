package com.scudata.ide.common;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

import com.scudata.app.config.ConfigUtil;
import com.scudata.cellset.IStyle;
import com.scudata.common.Logger;
import com.scudata.common.StringUtils;
import com.scudata.dm.Env;
import com.scudata.dm.cursor.ICursor;

/**
 * IDE options
 *
 */
public class ConfigOptions {
	/**
	 * Configuration file
	 */
	protected static ConfigFile cf = null;
	/**
	 * Option container
	 */
	protected static HashMap<String, Object> options = new HashMap<String, Object>();

	/** Normal */

	/** Whether to take over the console */
	public static Boolean bIdeConsole = Boolean.TRUE;
	/** Automatically open (recent files) */
	public static Boolean bAutoOpen = Boolean.FALSE;
	/** Automatic backup when saving */
	public static Boolean bAutoBackup = Boolean.TRUE;
	/** Write the exception to the log file */
	// public static Boolean bLogException = Boolean.FALSE;
	/** Automatically connect (recently connected) */
	public static Boolean bAutoConnect = Boolean.FALSE;
	/** Memory window position size */
	public static Boolean bWindowSize = Boolean.FALSE;
	/** Show window list */
	public static Boolean bViewWinList = Boolean.TRUE;
	/** Whether to use the undo */
	public static Boolean bUseUndo = Boolean.TRUE;
	/** Automatically clear the \0 at the end of the string */
	public static Boolean bAutoTrimChar0 = Boolean.TRUE;
	/** Check for updates at startup */
	// public static Boolean bCheckUpdate = Boolean.TRUE;
	/** Whether to change the cell in the comment cell */
	public static Boolean bAdjustNoteCell = Boolean.TRUE;
	/** App appearance */
	public static Byte iLookAndFeel = new Byte(
			LookAndFeelManager.LNF_OFFICE_SILVER);
	/** Longest wait while connecting to the database */
	public static Integer iConnectTimeout = new Integer(10);

	/** 是否自动保存 */
	public static Boolean bAutoSave = Boolean.FALSE;
	/** 自动保存时间间隔（分钟） */
	public static Integer iAutoSaveMinutes = new Integer(10);
	/** 新建文件备份到目录 */
	public static String sBackupDirectory = GM.getAbsolutePath("backup");
	/** 自动打开的文件数量 */
	public static String sAutoOpenFileNames = null;

	/** Parallel number */
	public static Integer iParallelNum = new Integer(1);
	/** Cursor Parallel number */
	public static Integer iCursorParallelNum = new Integer(1);
	/** Automatically clean the console */
	public static Boolean bAutoCleanOutput = Boolean.FALSE;
	/** The content is out of the cell display */
	public static Boolean bDispOutCell = Boolean.TRUE;
	/** Automatically adjust the line height after entering text */
	public static Boolean bAutoSizeRowHeight = Boolean.FALSE;
	/** Whether to use a multi-line editor */
	public static Boolean bMultiLineExpEditor = Boolean.TRUE;
	/** The cursor follows when single stepping */
	public static Boolean bStepLastLocation = Boolean.FALSE;
	/** DEMO is displayed in the file tree */
	public static Boolean bFileTreeDemo = Boolean.TRUE;
	/** Log file name */
	public static String sLogFileName = null;
	// GM.getAbsolutePath(GC.PATH_TMP + File.separator + "esproc.log")
	/** Paths for spl files */
	public static String sPaths = null;
	/** Main path */
	public static String sMainPath = null;
	/** Temporary path */
	public static String sTempPath = "temp";
	/** External library path */
	public static String sExtLibsPath = null;
	/** The spl file used for initialization */
	public static String sInitSpl = null;
	/** The path of the custom functions file */
	public static String sCustomFunctionFile = null;
	/** The installation directory of slimerjs */
	public static String sSlimerjsDirectory = null;
	/** Date format */
	public static String sDateFormat = Env.getDateFormat();
	/** Time format */
	public static String sTimeFormat = Env.getTimeFormat();
	/** Date time format */
	public static String sDateTimeFormat = Env.getDateTimeFormat();
	/** Default character set name件 */
	public static String sDefCharsetName = Env.getDefaultCharsetName();
	/** Local host */
	public static String sLocalHost = "";
	/** Local port */
	public static Integer iLocalPort = null;
	/** File buffer size (bytes) */
	public static String sFileBuffer = Env.FILE_BUFSIZE + "";
	/** Group table block size (bytes) */
	public static String sBlockSize = Env.BLOCK_SIZE + "";
	/** Missing value format (comma separated) */
	public static String sNullStrings = null;
	/** The number of records fetched from the cursor each time */
	public static Integer iFetchCount = new Integer(ICursor.FETCHCOUNT);
	/**
	 * Whether to automatically pop up the TipsOfDay dialog box. It is not
	 * configured in the options window.
	 */
	public static Boolean bAutoShowTip = Boolean.TRUE;
	/** The current number of the TipsOfDay dialog box. */
	public static Integer iAutoShowTip = new Integer(0);
	/** Whether to automatically pop up the http setting dialog box */
	public static Boolean bShowHttpConfig = Boolean.TRUE;
	/** HTTP port */
	public static Integer iHttpPort = new Integer(8503);

	/** esProc */
	/** Row count */
	public static Integer iRowCount = new Integer(20);
	/** Column count */
	public static Integer iColCount = new Integer(5);
	/** Row height */
	public static Float fRowHeight = new Float(25);
	/** Column width */
	public static Float fColWidth = new Float(150);
	/** Constant cell foreground color */
	public static Color iConstFColor = new Color(255, 0, 255);
	/** Constant cell background color */
	public static Color iConstBColor = Color.white;
	/** Comment cell foreground color */
	public static Color iNoteFColor = new Color(51, 153, 0);
	/** Comment cell background color */
	public static Color iNoteBColor = Color.white;
	/** Valued cell foreground color */
	public static Color iValueFColor = Color.black;
	/** Valued cell background color */
	public static Color iValueBColor = new Color(255, 255, 153);
	/** Unvalued cell foreground color */
	public static Color iNValueFColor = Color.black;
	/** Unvalued cell background color */
	public static Color iNValueBColor = Color.white;
	/** Font name */
	public static String sFontName = "Dialog";
	/** Font size */
	public static Short iFontSize = new Short((short) 12);
	/** Is bold */
	public static Boolean bBold = Boolean.FALSE;
	/** Is italic */
	public static Boolean bItalic = Boolean.FALSE;
	/** Is underline */
	public static Boolean bUnderline = Boolean.FALSE;
	/** Horizontal alignment */
	public static Byte iHAlign = new Byte(IStyle.HALIGN_LEFT);
	/** Vertical alignment */
	public static Byte iVAlign = new Byte(IStyle.VALIGN_TOP);
	/** Indents */
	public static Integer iIndent = new Integer(3);
	/** The upper limit of the members of the sequence display */
	public static Integer iSequenceDispMembers = new Integer(3);
	/**
	 * Display the tables and fields in the database
	 */
	public static Boolean bShowDBStruct = Boolean.TRUE;
	/**
	 * Number of records in the database. Discard.
	 */
	public static Integer iDBRecordCount = new Integer(-1);
	/**
	 * The current locale:
	 * GC.ASIAN_CHINESE,GC.ASIAN_CHINESE_TRADITIONAL,GC.ASIAN_JAPANESE,GC
	 * .ASIAN_KOREA,GC.ENGLISH
	 */
	public static Byte iLocale = null;

	/**
	 * The location of the output window
	 */
	public static Integer iConsoleLocation = new Integer(-1);

	/**
	 * The color of the cell value type
	 */
	/** Decimal */
	public static Color COLOR_DECIMAL = Color.RED;
	/** Integer */
	public static Color COLOR_INTEGER = Color.BLUE;
	/** Double */
	public static Color COLOR_DOUBLE = Color.PINK.darker();
	/** NULL */
	public static Color COLOR_NULL = new Color(255, 0, 255);

	/**
	 * Increase the background color configuration of some positions in the designer
	 * in the system configuration file
	 */
	/** Reserved, not used in esProc for now */
	public static String fileColor;
	/** Reserved, not used in esProc for now */
	public static String fileColorOpacity;
	/** Select the background color of the first column of the table row */
	public static String headerColor;
	/**
	 * Select the background color transparency at the first column of the table row
	 */
	public static String headerColorOpacity;
	/**
	 * Select the cell and the background color in the upper right corner of the
	 * designer
	 */
	public static String cellColor;
	/**
	 * Select the cell and the background color transparency in the upper right
	 * corner of the designer
	 */
	public static String cellColorOpacity;

	/** Save the expanded state of the file tree */
	public static String sFileTreeExpand;
	/**
	 * Whether the multi-text editor automatically wraps
	 */
	public static Boolean bTextEditorLineWrap = Boolean.FALSE;

	/** HTML */
	public static final int COPY_HTML = 0;
	/** TEXT */
	public static final int COPY_TEXT = 1;
	/** Copy the present code type. There are HTML and text. */
	public static Byte iCopyPresentType = COPY_HTML;
	/** Whether to copy the row and column number */
	public static Boolean bCopyPresentHeader = Boolean.TRUE;
	/** Column separator */
	public static String sCopyPresentSep = "\t";
	/** The max of undo and redo */
	public static Integer iUndoCount = new Integer(20);

	/**
	 * Static loading options
	 */
	static {
		putOptions();
	}

	/**
	 * Put option default values in the container
	 */
	public static void putOptions() {
		options.put("bIdeConsole", bIdeConsole);
		options.put("bAutoOpen", bAutoOpen);
		options.put("bAutoBackup", bAutoBackup);
		// options.put("bLogException", bLogException);
		options.put("bAutoConnect", bAutoConnect);
		options.put("bWindowSize", bWindowSize);
		options.put("bViewWinList", bViewWinList);
		options.put("bUseUndo", bUseUndo);
		options.put("bTextEditorLineWrap", bTextEditorLineWrap);
		options.put("bAutoTrimChar0", bAutoTrimChar0);
		options.put("bAutoCleanOutput", bAutoCleanOutput);
		// options.put("bCheckUpdate", bCheckUpdate);
		options.put("bAdjustNoteCell", bAdjustNoteCell);
		options.put("iLookAndFeel", iLookAndFeel);
		options.put("iConnectTimeout", iConnectTimeout);
		options.put("iFontSize", iFontSize);
		options.put("bDispOutCell", bDispOutCell);
		options.put("bAutoSizeRowHeight", bAutoSizeRowHeight);
		options.put("bMultiLineExpEditor", bMultiLineExpEditor);
		options.put("bStepLastLocation", bStepLastLocation);
		options.put("bFileTreeDemo", bFileTreeDemo);
		options.put("bAutoSave", bAutoSave);
		options.put("iAutoSaveMinutes", iAutoSaveMinutes);
		options.put("sBackupDirectory", sBackupDirectory);
		options.put("sAutoOpenFileNames", sAutoOpenFileNames);
		options.put("iConstFColor", new Integer(iConstFColor.getRGB()));
		options.put("iConstBColor", new Integer(iConstBColor.getRGB()));
		options.put("iNoteFColor", new Integer(iNoteFColor.getRGB()));
		options.put("iNoteBColor", new Integer(iNoteBColor.getRGB()));
		options.put("iValueFColor", new Integer(iValueFColor.getRGB()));
		options.put("iValueBColor", new Integer(iValueBColor.getRGB()));
		options.put("iNValueFColor", new Integer(iNValueFColor.getRGB()));
		options.put("iNValueBColor", new Integer(iNValueBColor.getRGB()));
		options.put("iRowCount", iRowCount);
		options.put("iColCount", iColCount);
		options.put("fRowHeight", fRowHeight);
		options.put("fColWidth", fColWidth);
		options.put("sFontName", sFontName);
		options.put("iFontSize", iFontSize);
		options.put("bBold", bBold);
		options.put("bItalic", bItalic);
		options.put("bUnderline", bUnderline);
		options.put("iHAlign", iHAlign);
		options.put("iVAlign", iVAlign);
		options.put("iIndent", iIndent);
		options.put("iSequenceDispMembers", iSequenceDispMembers);
		options.put("sLogFileName", sLogFileName);
		options.put("bShowDBStruct", bShowDBStruct);
		options.put("bAutoShowTip", bAutoShowTip);
		options.put("iDBRecordCount", iDBRecordCount);
		options.put("iConsoleLocation", iConsoleLocation);
		options.put("bShowHttpConfig", bShowHttpConfig);
		options.put("iHttpPort", iHttpPort);
		options.put("iLocale", iLocale);
		options.put("sSlimerjsDirectory", sSlimerjsDirectory);
		options.put("sFileTreeExpand", sFileTreeExpand);
		options.put("iUndoCount", iUndoCount);

	}

	/**
	 * Save options
	 * 
	 * @throws Throwable
	 */
	public static void save() throws Throwable {
		save(true);
	}

	/**
	 * Save options
	 * 
	 * @param holdConsole
	 * @throws Throwable
	 */
	public static void save(boolean holdConsole) throws Throwable {
		putOptions();
		cf = ConfigFile.getConfigFile();
		cf.setConfigNode(ConfigFile.NODE_OPTIONS);
		Iterator<String> it = options.keySet().iterator();
		String option;
		Object optionVar;
		while (it.hasNext()) {
			option = it.next();
			optionVar = options.get(option);
			cf.setAttrValue(option, optionVar);
		}
		cf.save();
		applyOptions(holdConsole);
	}

	/**
	 * Loading options
	 * 
	 * @throws Throwable
	 */
	public static void load() throws Throwable {
		load2(true);
	}

	/**
	 * Loading options
	 * 
	 * @param holdConsole
	 * @throws Throwable
	 */
	public static void load2(boolean holdConsole) throws Throwable {
		load2(holdConsole, true);
	}

	/**
	 * Loading options
	 * 
	 * @param holdConsole
	 * @param applyOptions
	 * @throws Throwable
	 */
	public static void load2(boolean holdConsole, boolean applyOptions)
			throws Throwable {
		cf = ConfigFile.getConfigFile();
		cf.setConfigNode(ConfigFile.NODE_OPTIONS);
		Iterator<String> it = options.keySet().iterator();
		while (it.hasNext()) {
			loadOption(it.next());
		}
		if (applyOptions)
			applyOptions(holdConsole);
	}

	/**
	 * 加载选项
	 * 
	 * @param option
	 */
	public static void loadOption(String option) {
		String val = cf.getAttrValue(option);
		if (!StringUtils.isValidString(val)) {
			return;
		}
		String type = option.substring(0, 1);
		if (type.equalsIgnoreCase("i")) {
			Integer ii = Integer.valueOf(val);
			if (option.equalsIgnoreCase("iLookAndFeel")) {
				iLookAndFeel = new Byte(ii.byteValue());
			} else if (option.equalsIgnoreCase("iConnectTimeout")) {
				iConnectTimeout = ii;
			} else if (option.equalsIgnoreCase("iRowCount")) {
				iRowCount = ii;
			} else if (option.equalsIgnoreCase("iColCount")) {
				iColCount = ii;
			} else if (option.equalsIgnoreCase("iConstFColor")) {
				iConstFColor = new Color(ii.intValue());
			} else if (option.equalsIgnoreCase("iConstBColor")) {
				iConstBColor = new Color(ii.intValue());
			} else if (option.equalsIgnoreCase("iNoteFColor")) {
				iNoteFColor = new Color(ii.intValue());
			} else if (option.equalsIgnoreCase("iNoteBColor")) {
				iNoteBColor = new Color(ii.intValue());
			} else if (option.equalsIgnoreCase("iValueFColor")) {
				iValueFColor = new Color(ii.intValue());
			} else if (option.equalsIgnoreCase("iValueBColor")) {
				iValueBColor = new Color(ii.intValue());
			} else if (option.equalsIgnoreCase("iNValueFColor")) {
				iNValueFColor = new Color(ii.intValue());
			} else if (option.equalsIgnoreCase("iNValueBColor")) {
				iNValueBColor = new Color(ii.intValue());
			} else if (option.equalsIgnoreCase("iFontSize")) {
				iFontSize = new Short(ii.shortValue());
			} else if (option.equalsIgnoreCase("iHAlign")) {
				iHAlign = new Byte(ii.byteValue());
			} else if (option.equalsIgnoreCase("iVAlign")) {
				iVAlign = new Byte(ii.byteValue());
			} else if (option.equalsIgnoreCase("iIndent")) {
				iIndent = ii;
			} else if (option.equalsIgnoreCase("iSequenceDispMembers")) {
				iSequenceDispMembers = ii;
			} else if (option.equalsIgnoreCase("iAutoShowTip")) {
				iAutoShowTip = ii;
			} else if (option.equalsIgnoreCase("iDBRecordCount")) {
				iDBRecordCount = ii;
			} else if (option.equalsIgnoreCase("iConsoleLocation")) {
				iConsoleLocation = ii;
			} else if (option.equalsIgnoreCase("iHttpPort")) {
				iHttpPort = ii;
			} else if (option.equalsIgnoreCase("iLocale")) {
				iLocale = new Byte(ii.byteValue());
			} else if (option.equalsIgnoreCase("iUndoCount")) {
				iUndoCount = ii;
			} else if (option.equalsIgnoreCase("iAutoSaveMinutes")) {
				iAutoSaveMinutes = ii;
			}
		} else if (type.equalsIgnoreCase("f")) {
			Float ii = Float.valueOf(val);
			if (option.equalsIgnoreCase("fRowHeight")) {
				fRowHeight = ii;
			} else if (option.equalsIgnoreCase("fColWidth")) {
				fColWidth = ii;
			}
		} else if (type.equalsIgnoreCase("b")) {
			Boolean ii = Boolean.valueOf(val);
			if (option.equalsIgnoreCase("bIdeConsole")) {
				bIdeConsole = ii;
			} else if (option.equalsIgnoreCase("bAutoOpen")) {
				bAutoOpen = ii;
			} else if (option.equalsIgnoreCase("bAutoBackup")) {
				bAutoBackup = ii;
				// } else if (option.equalsIgnoreCase("bLogException")) {
				// bLogException = ii;
			} else if (option.equalsIgnoreCase("bAutoConnect")) {
				bAutoConnect = ii;
			} else if (option.equalsIgnoreCase("bWindowSize")) {
				bWindowSize = ii;
			} else if (option.equalsIgnoreCase("bViewWinList")) {
				bViewWinList = ii;
			} else if (option.equalsIgnoreCase("bShowHttpConfig")) {
				bShowHttpConfig = ii;
			} else if (option.equalsIgnoreCase("bUseUndo")) {
				bUseUndo = ii;
			} else if (option.equalsIgnoreCase("bTextEditorLineWrap")) {
				bTextEditorLineWrap = ii;
			} else if (option.equalsIgnoreCase("bAutoTrimChar0")) {
				bAutoTrimChar0 = ii;
			} else if (option.equalsIgnoreCase("bAutoCleanOutput")) {
				bAutoCleanOutput = ii;
				// } else if (option.equalsIgnoreCase("bCheckUpdate")) {
				// bCheckUpdate = ii;
			} else if (option.equalsIgnoreCase("bAdjustNoteCell")) {
				bAdjustNoteCell = ii;
			} else if (option.equalsIgnoreCase("bDispOutCell")) {
				bDispOutCell = ii;
			} else if (option.equalsIgnoreCase("bAutoSizeRowHeight")) {
				bAutoSizeRowHeight = ii;
			} else if (option.equalsIgnoreCase("bAutoCalc")) {
				// bAutoCalc = ii;
			} else if (option.equalsIgnoreCase("bMultiLineExpEditor")) {
				bMultiLineExpEditor = ii;
			} else if (option.equalsIgnoreCase("bStepLastLocation")) {
				bStepLastLocation = ii;
			} else if (option.equalsIgnoreCase("bBold")) {
				bBold = ii;
			} else if (option.equalsIgnoreCase("bItalic")) {
				bItalic = ii;
			} else if (option.equalsIgnoreCase("bUnderline")) {
				bUnderline = ii;
			} else if (option.equalsIgnoreCase("bShowDBStruct")) {
				bShowDBStruct = ii;
			} else if (option.equalsIgnoreCase("bAutoShowTip")) {
				bAutoShowTip = ii;
			} else if (option.equalsIgnoreCase("bFileTreeDemo")) {
				bFileTreeDemo = ii;
			} else if (option.equalsIgnoreCase("bAutoSave")) {
				bAutoSave = ii;
			}
		} else if (StringUtils.isValidString(val)) {
			if (option.equalsIgnoreCase("sLogFileName")) {
				sLogFileName = val;
			} else if (option.equalsIgnoreCase("sFontName")) {
				sFontName = val;
			} else if (option.equalsIgnoreCase("sSlimerjsDirectory")) {
				sSlimerjsDirectory = val;
			} else if (option.equalsIgnoreCase("sFileTreeExpand")) {
				sFileTreeExpand = val;
			} else if (option.equalsIgnoreCase("sBackupDirectory")) {
				sBackupDirectory = val;
			} else if (option.equalsIgnoreCase("sAutoOpenFileNames")) {
				sAutoOpenFileNames = val;
			}
		}
	}

	/**
	 * Accept options
	 */
	public static void applyOptions() {
		applyOptions(true);
	}

	/**
	 * Accept options
	 * 
	 * @param holdConsole
	 */
	public static void applyOptions(boolean holdConsole) {
		applyOptions(holdConsole, false);
	}

	/**
	 * Accept options
	 * 
	 * @param holdConsole
	 * @param isReport
	 */
	public static void applyOptions(boolean holdConsole, boolean isReport) {
		int style = Font.PLAIN;
		if (ConfigOptions.bBold.booleanValue()) {
			style += Font.BOLD;
		}
		if (ConfigOptions.bItalic.booleanValue()) {
			style += Font.ITALIC;
		}
		GC.font = new Font(ConfigOptions.sFontName, style,
				ConfigOptions.iFontSize.intValue());
		Env.setPaths(GM.getPaths());
		String tempPath = ConfigOptions.sTempPath;
		if (tempPath != null)
			if (tempPath.trim().length() == 0)
				tempPath = null;
		Env.setTempPath(null);
		if (StringUtils.isValidString(ConfigOptions.sMainPath)) {
			String mainPath = ConfigUtil.getPath(
					System.getProperty("start.home"), ConfigOptions.sMainPath);
			Env.setMainPath(mainPath);
			if (StringUtils.isValidString(tempPath)) {
				Env.setTempPath(ConfigUtil.getPath(mainPath, tempPath));
			}
		} else {
			GM.setCurrentPath(null);
			if (StringUtils.isValidString(tempPath)) {
				File tempDir = new File(tempPath);
				if (tempDir.isAbsolute()) {
					Env.setTempPath(tempPath);
				}
			}
		}
		if (StringUtils.isValidString(Env.getTempPath())) {
			try {
				File f = new File(Env.getTempPath());
				if (!f.exists()) {
					f.mkdir();
				}
			} catch (Exception ex) {
				Logger.info("Make temp directory failed:");
				ex.printStackTrace();
			}
		}
		if (StringUtils.isValidString(sDateFormat))
			Env.setDateFormat(sDateFormat);
		if (StringUtils.isValidString(sTimeFormat))
			Env.setTimeFormat(sTimeFormat);
		if (StringUtils.isValidString(sDateTimeFormat))
			Env.setDateTimeFormat(sDateTimeFormat);
		if (StringUtils.isValidString(sDefCharsetName))
			Env.setDefaultChartsetName(sDefCharsetName);
		ICursor.FETCHCOUNT = iFetchCount.intValue();
		Env.setLocalHost(sLocalHost);
		if (iLocalPort != null)
			Env.setLocalPort(iLocalPort.intValue());
		if (GM.isBlockSizeEnabled()) {
			Env.setFileBufSize(ConfigUtil.parseBufferSize(sFileBuffer));
			if (StringUtils.isValidString(sBlockSize))
				ConfigUtil.setEnvBlockSize(sBlockSize);
		}
		Env.setNullStrings(ConfigUtil.splitNullStrings(sNullStrings));
		Env.setParallelNum(iParallelNum.intValue());
		Env.setCursorParallelNum(iCursorParallelNum.intValue());
		Env.setAdjustNoteCell(bAdjustNoteCell.booleanValue());
		if (holdConsole && ConfigOptions.bIdeConsole.booleanValue())
			AppFrame.holdConsole();
		if (!isReport) {
			try {
				Logger.setPropertyConfig(getLoggerProperty());
			} catch (Exception e) {
				GM.showException(e);
			}
			DriverManager.setLoginTimeout(iConnectTimeout.intValue());
		}
	}

	/**
	 * 创建日志属性对象
	 * @return
	 */
	public static Properties getLoggerProperty() {
		Properties props = new Properties();
		String logName = "IDE_CONSOLE";
		String logPath = null;
		if (StringUtils.isValidString(ConfigOptions.sLogFileName)) {
			logName += ",IDE_LOGFILE";
			logPath = GM.getAbsolutePath(ConfigOptions.sLogFileName);
		}
		props.put("Logger", logName);
		String sLogLevel = GV.config.getLogLevel();
		if (logPath != null) {
			props.put("IDE_LOGFILE", logPath);
			props.put("IDE_LOGFILE.Level", sLogLevel);
			props.put("IDE_LOGFILE.Encoding", "UTF-8");
		}
		props.put("IDE_CONSOLE", "Console");
		props.put("IDE_CONSOLE.Level", sLogLevel);
		props.put("IDE_CONSOLE.Encoding", "UTF-8");
		return props;
	}

	/**
	 * Display value of log level
	 * 
	 * @return
	 */
	public static Vector<String> dispLevels() {
		String[] levelNames = Logger.listLevelNames();
		Vector<String> levels = new Vector<String>();
		if (levelNames != null)
			for (String levelName : levelNames)
				levels.add(levelName);
		return levels;
	}

	/**
	 * Get Locale based on display name
	 * 
	 * @param dispName
	 * @return
	 */
	public static Locale getLocaleByName(String dispName) {
		if (dispName == null) {
			return null;
		}
		Locale[] locales = Locale.getAvailableLocales();
		if (locales != null) {
			for (int i = 0; i < locales.length; i++) {
				if (dispName.equals(locales[i].getDisplayName())) {
					return locales[i];
				}
			}
		}
		return null;
	}

	/**
	 * Display color of the file tree
	 * 
	 * @return
	 */
	public static Color getFileColor() {
		String color = fileColor;
		if (StringUtils.isValidString(color) && color.length() == 7) {
			int red = Integer.parseInt(color.substring(1, 3), 16);
			int green = Integer.parseInt(color.substring(3, 5), 16);
			int blue = Integer.parseInt(color.substring(5, 7), 16);
			int opacity;
			if (StringUtils.isValidString(fileColorOpacity)) {
				opacity = Math.round(255 * Float.parseFloat(fileColorOpacity));
			} else {
				opacity = 255;
			}
			return new Color(red, green, blue, opacity);
		}
		return null;
	}

	/**
	 * Get the color of the header
	 * 
	 * @return
	 */
	public static Color getHeaderColor() {
		String color = headerColor;
		if (StringUtils.isValidString(color) && color.length() == 7) {
			int red = Integer.parseInt(color.substring(1, 3), 16);
			int green = Integer.parseInt(color.substring(3, 5), 16);
			int blue = Integer.parseInt(color.substring(5, 7), 16);
			int opacity;
			if (StringUtils.isValidString(headerColorOpacity)) {
				opacity = Math
						.round(255 * Float.parseFloat(headerColorOpacity));
			} else {
				opacity = 255;
			}
			return new Color(red, green, blue, opacity);
		}
		return null;
	}

	/**
	 * Get the cell color
	 * 
	 * @return
	 */
	public static Color getCellColor() {
		String color = cellColor;
		if (StringUtils.isValidString(color) && color.length() == 7) {
			int red = Integer.parseInt(color.substring(1, 3), 16);
			int green = Integer.parseInt(color.substring(3, 5), 16);
			int blue = Integer.parseInt(color.substring(5, 7), 16);
			int opacity;
			if (StringUtils.isValidString(cellColorOpacity)) {
				opacity = Math.round(255 * Float.parseFloat(cellColorOpacity));
			} else {
				opacity = 255;
			}
			return new Color(red, green, blue, opacity);
		}
		return null;
	}
}
