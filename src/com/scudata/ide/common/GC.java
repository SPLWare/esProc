package com.scudata.ide.common;

import java.awt.Color;
import java.awt.Font;
import java.util.Locale;

import com.scudata.ide.common.resources.IdeCommonMessage;

/**
 * Global Constants
 * 
 */
public class GC {
	/**
	 * Chinese
	 */
	public static final byte ASIAN_CHINESE = 0;
	/**
	 * Traditional Chinese
	 */
	public static final byte ASIAN_CHINESE_TRADITIONAL = 1;
	/**
	 * Japanese
	 */
	public static final byte ASIAN_JAPANESE = 2;
	/**
	 * Korea
	 */
	public static final byte ASIAN_KOREA = 3;
	/**
	 * English
	 */
	public static final byte ENGLISH = 4;
	/**
	 * Current language
	 */
	public static byte LANGUAGE = ENGLISH;

	/**
	 * Initialize Locale
	 */
	static {
		initLocale();
	}

	/**
	 * Initialize Locale
	 */
	public static void initLocale() {
		Locale local = Locale.getDefault();
		if (local.equals(Locale.PRC) || local.equals(Locale.CHINA)
				|| local.equals(Locale.CHINESE)
				|| local.equals(Locale.SIMPLIFIED_CHINESE)
				|| local.getLanguage().equalsIgnoreCase("zh")) {
			LANGUAGE = ASIAN_CHINESE;
		} else if (local.equals(Locale.TAIWAN)
				|| local.equals(Locale.TRADITIONAL_CHINESE)
				|| local.getLanguage().equalsIgnoreCase("tw")) {
			LANGUAGE = ASIAN_CHINESE_TRADITIONAL;
		} else if (local.equals(Locale.JAPAN) || local.equals(Locale.JAPANESE)) {
			LANGUAGE = ASIAN_JAPANESE;
		} else if (local.equals(Locale.KOREA) || local.equals(Locale.KOREAN)) {
			LANGUAGE = ASIAN_KOREA;
		} else {
			LANGUAGE = 4;
		}
	}

	/**
	 * Configuration file path
	 */
	public final static String PATH_CONFIG = "config";
	/**
	 * Logo file path
	 */
	public final static String PATH_LOGO = "logo";
	/**
	 * Temporary file path
	 */
	public final static String PATH_TMP = "log";
	/**
	 * Ratio of tool window to screen
	 */
	public static final double TOOL_SCALE = 0.7;

	/**
	 * Picture path
	 */
	public final static String IMAGES_PATH = "/com/raqsoft/ide/common/resources/";

	/**
	 * Windows
	 */
	public static final byte OS_WINDOWS = 0;
	/**
	 * MAC OS
	 */
	public static final byte OS_MAC = 1;
	/**
	 * Other OS
	 */
	public static final byte OS_OTHER = 2;

	/**
	 * Ways to find menus
	 */
	/** Search by menu name */
	public static final byte SEARCHMENU_BYNAME = 0;
	/** Search by menu text */
	public static final byte SEARCHMENU_BYTEXT = 1;
	/** Search by menu label */
	public static final byte SEARCHMENU_BYLABLE = 2;

	/**
	 * Default row height
	 */
	public final static int DEFAULT_ROW_HEIGHT = 25;
	/**
	 * Default row header width
	 */
	public final static int DEFAULT_ROWHEADER_WIDTH = 40;
	/**
	 * Minimum row height
	 */
	public final static int MIN_ROW_HEIGHT = 20;
	/**
	 * Minimum column width
	 */
	public final static int MIN_COL_WIDTH = 30;

	/**
	 * Mouse state of the edit control
	 */
	/** Normal */
	public final static int STATUS_NORMAL = 0;
	/** Selecting */
	public final static int STATUS_SELECTING = 1;
	/** Moving */
	public final static int STATUS_MOVE = 2;
	/** Header Selecting */
	public final static int STATUS_HEADERSELECTING = 3;
	/** Change cell size */
	public final static int STATUS_CELLRESIZE = 4;

	/**
	 * The selected state of the edit control
	 */
	/** Unselected */
	public static final byte SELECT_STATE_NONE = 0;
	/** Cell selected */
	public static final byte SELECT_STATE_CELL = 1;
	/** Cellset selected */
	public static final byte SELECT_STATE_DM = 2;
	/** Row selected */
	public static final byte SELECT_STATE_ROW = 3;
	/** Column selected */
	public static final byte SELECT_STATE_COL = 4;

	/**
	 * File extension
	 */
	/** DFX file extension */
	public static final String FILE_DFX = "dfx";
	/** SPL file extension */
	public static final String FILE_SPL = "spl";
	/** TXT file extension */
	public static final String FILE_TXT = "txt";
	/** CSV file extension */
	public static final String FILE_CSV = "csv";
	/** XLS file extension */
	public static final String FILE_XLS = "xls";
	/** XLSX file extension */
	public static final String FILE_XLSX = "xlsx";
	/** HTML file extension */
	public static final String FILE_HTML = "html";
	/** XML file extension */
	public static final String FILE_XML = "xml";
	/** LOG file extension */
	public static final String FILE_LOG = "log";
	/** Binary file extension */
	public static final String FILE_BTX = "btx";
	/** Group table file extension */
	public static final String FILE_CTX = "ctx";

	/**
	 * Tree node status
	 */
	/** Empty */
	public static final byte TYPE_EMPTY = -1;
	/** Collapsed */
	public static final byte TYPE_PLUS = 0;
	/** Expanded */
	public static final byte TYPE_MINUS = 1;
	/** Node */
	public static final byte TYPE_NODE = 2;
	/** Last collapsed */
	public static final byte TYPE_LASTPLUS = 3;
	/** Last expanded */
	public static final byte TYPE_LASTMINUS = 4;
	/** Last node */
	public static final byte TYPE_LASTNODE = 5;
	/**
	 * Tree node status icon
	 */
	/** Collapsed icon */
	public static final String FILE_PLUS = "plus.gif";
	/** Expanded icon */
	public static final String FILE_MINUS = "minus.gif";
	/** Node icon */
	public static final String FILE_NODE = "node.gif";
	/** Last collapsed icon */
	public static final String FILE_LASTPLUS = "lastplus.gif";
	/** Last expanded icon */
	public static final String FILE_LASTMINUS = "lastminus.gif";
	/** Last node icon */
	public static final String FILE_LASTNODE = "lastnode.gif";

	/**
	 * General menus 0-1000
	 */
	public static final String MENU = "menu.";

	/**
	 * File menus 0-100
	 */
	/** File */
	public static final String FILE = "file";
	/** New */
	public static final String NEW = "file.new";
	/** Open */
	public static final String OPEN = "file.open";
	/** Close */
	public static final String FILE_CLOSE = "file.close";
	/** Close all */
	public static final String FILE_CLOSE_ALL = "file.closeall";
	/** Save */
	public static final String SAVE = "file.save";
	/** Save as */
	public static final String SAVEAS = "file.saveas";
	/** Save all */
	public static final String SAVEALL = "file.saveall";
	/** Recent files */
	public static final String RECENT_FILES = "file.recentfiles";
	/** Recent main path */
	public static final String RECENT_MAINPATH = "file.recentmainpaths";
	/** Recent connections */
	public static final String RECENT_CONNS = "file.recentconns";
	/** Quit */
	public static final String QUIT = "file.quit"; // 退出
	/** New */
	public static final short iNEW = 5;
	/** Open */
	public static final short iOPEN = 10;

	/** Close */
	public static final short iFILE_CLOSE = 25;
	/**
	 * Because the KeyEvent.VK_F4 key conflicts, add a hidden menu for this
	 * shortcut key.
	 */
	public static final short iFILE_CLOSE1 = 27;
	/** Close all */
	public static final short iFILE_CLOSE_ALL = 30;
	/** Save */
	public static final short iSAVE = 50;
	/** Save as */
	public static final short iSAVEAS = 55;
	/** Save all */
	public static final short iSAVEALL = 60;
	/** Quit */
	public static final short iQUIT = 80;

	/**
	 * Tool menus 100-200
	 */
	public static final String TOOL = "tool";
	/** Data source */
	public static final String DATA_SOURCE = "env.datasource";
	/** Options */
	public static final String OPTIONS = "configure.options";
	/** Console */
	public static final String CONSOLE = "configure.console";
	/** Cellset description */
	public static final String PROPERTY1 = "edit.property1";
	/** Cellset password */
	public static final String PASSWORD = "file.password";
	/** Unlock advanced permissions */
	public static final String PASSWORD2 = "file.password2";

	/** Data source */
	public static final short iDATA_SOURCE = 105;
	/** Options */
	public static final short iOPTIONS = 110;
	/** Console */
	public static final short iCONSOLE = 115;
	/** Cellset description */
	public static final short iPROPERTY = 130;
	/** Cellset password */
	public static final short iPASSWORD = 141;

	/**
	 * Window menus 200-300
	 */
	public static final String WINDOW = "window";
	/** Cascade */
	public static final String CASCADE = "window.cascade";
	/** Tile horizontally */
	public static final String TILEHORIZONTAL = "window.tilehorizontal";
	/** Tile Vertically */
	public static final String TILEVERTICAL = "window.tilevertical";
	/** Filling */
	public static final String LAYER = "window.layer";
	/** Cascade */
	public static final short iCASCADE = 205;
	/** Tile horizontally */
	public static final short iTILEHORIZONTAL = 210;
	/** Tile Vertically */
	public static final short iTILEVERTICAL = 215;
	/** Filling */
	public static final short iLAYER = 220;

	/** Show/hide window list */
	public static final String SHOW_WINLIST = "edit.showwinlist";
	/** Show/hide console */
	public static final String VIEW_CONSOLE = "edit.viewconsole";
	/** Show/hide the right panel */
	public static final String VIEW_RIGHT = "edit.viewright";
	/** Show/hide window list */
	public static final short iSHOW_WINLIST = 251;
	/** Show/hide console */
	public static final short iVIEW_CONSOLE = 253;
	/** Show/hide the right panel */
	public static final short iVIEW_RIGHT = 255;

	/**
	 * Help menus 300-400
	 */
	public static final String HELP = "help";
	/** About dialog */
	public static final String ABOUT = "help.about";
	/** Tips of the day */
	public static final String TIPSOFDAY = "help.tipsofday";
	/** Clean up memory */
	public static final String MEMORYTIDY = "help.memorytidy";
	/** Check for updates */
	public static final String CHECK_UPDATE = "help.update";

	/** About dialog */
	public static final short iABOUT = 305;
	/** Clean up memory */
	public static final short iMEMORYTIDY = 310;
	/** Check for updates */
	public static final short iCHECK_UPDATE = 355;

	/** Edit menus 400-500 */
	/** Tips */
	public static final String TIPS = "edit.tips";

	public static final short iTIPS = 401;

	/**
	 * Font size code
	 */
	public static Short[] FONTSIZECODE = { new Short((short) 8),
			new Short((short) 9), new Short((short) 11), new Short((short) 12),
			new Short((short) 14), new Short((short) 15),
			new Short((short) 16), new Short((short) 18),
			new Short((short) 22), new Short((short) 24),
			new Short((short) 26), new Short((short) 36), new Short((short) 42) };

	/**
	 * Font size display
	 */
	public static String[] FONTSIZEDISP = new String[] {
			IdeCommonMessage.get().getMessage("gc.fontsix"),
			IdeCommonMessage.get().getMessage("gc.fontsmallfive"),
			IdeCommonMessage.get().getMessage("gc.fontfive"),
			IdeCommonMessage.get().getMessage("gc.fontsmallfour"),
			IdeCommonMessage.get().getMessage("gc.fontfour"),
			IdeCommonMessage.get().getMessage("gc.fontsmallthree"),
			IdeCommonMessage.get().getMessage("gc.fontthree"),
			IdeCommonMessage.get().getMessage("gc.fontsmalltwo"),
			IdeCommonMessage.get().getMessage("gc.fonttwo"),
			IdeCommonMessage.get().getMessage("gc.fontsmallone"),
			IdeCommonMessage.get().getMessage("gc.fontone"),
			IdeCommonMessage.get().getMessage("gc.fontsmallzero"),
			IdeCommonMessage.get().getMessage("gc.fontzero") };

	/**
	 * NULL String. Used to set attribute values and expressions. Allows
	 * AtomicCell to set only one value at a time
	 */
	public final static String NULL = new String();
	/**
	 * IDE Font
	 */
	public static Font font = new Font("Dialog", Font.PLAIN, 12);
	/**
	 * For clarity, use spaces to increase indentation.
	 */
	public static final String STR_INDENT = " ";
	/**
	 * The color of the pseudo field column
	 */
	public static final Color ALIAS_COLOR = new Color(235, 235, 255);
	/**
	 * Date formats used for display
	 */
	public static final String[] DATE_FORMATS = LANGUAGE == ASIAN_CHINESE ? new String[] {
			"yyyy-MM-dd", "yyyy年MM月dd日", "yyyy/MM/dd", "yy-MM-dd", "yy年MM月dd日",
			"yy/MM/dd" }
			: new String[] { "yyyy-MM-dd", "yyyy/MM/dd", "yy-MM-dd", "yy/MM/dd" };
	/**
	 * Time formats used for display
	 */
	public static final String[] TIME_FORMATS = new String[] { "HH:mm:ss",
			"HH:mm:ssS", "kk:mm:ss", "kk:mm:ssS", "hh:mm:ss", "hh:mm:ssS",
			"KK:mm:ss", "KK:mm:ssS" };
	/**
	 * Date time formats used for display
	 */
	public static final String[] DATE_TIME_FORMATS = LANGUAGE == ASIAN_CHINESE ? new String[] {
			"yyyy-MM-dd HH:mm:ss", "yyyy年MM月dd日 HH:mm:ss",
			"yyyy/MM/dd HH:mm:ss", "yy-MM-dd HH:mm:ss", "yy年MM月dd日 HH:mm:ss",
			"yy/MM/dd HH:mm:ss" }
			: new String[] { "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss",
					"yy-MM-dd HH:mm:ss", "yy/MM/dd HH:mm:ss" };

	/**
	 * Default parameter name prefix
	 */
	public static final String PRE_PARAM = "param";
	/**
	 * Default variable name prefix
	 */
	public static final String PRE_VAR = "var";

	/**
	 * Constant type
	 */
	/** String */
	public static final byte KIND_STR = 0;
	/** Integer */
	public static final byte KIND_INT = 1;
	/** Double */
	public static final byte KIND_DOUBLE = 2;
	/** Date */
	public static final byte KIND_DATE = 3;
	/** Date time */
	public static final byte KIND_DATE_TIME = 4;
	/** Sequence */
	public static final byte KIND_SERIES = 5;
	/** Table */
	public static final byte KIND_TABLE = 6;
	/** Expression */
	public static final byte KIND_EXP = 7;

	/** Logo file name */
	public static final String DEFAULT_LOGO = "logo.png";

	/**
	 * Custom properties of the cellset
	 */
	/** Cellset title */
	public static final byte CELLSET_TITLE = 0;
	/** Cellset description */
	public static final byte CELLSET_DESC = 1;
	/** Cellset expressions */
	public static final byte CELLSET_EXPS = 11;
	/** The current cell of the Cellset */
	public static final byte CELLSET_ACTIVE_CELL = 12;

	/**
	 * In order to distinguish it from general menus, esProc's menu items need
	 * to add this constant.
	 */
	public static final short MENU_DFX = 4000;
	/**
	 * EsProc’s pop-up menu adds this constant
	 */
	public static final short POPMENU_DFX = 5000;

	/**
	 * Indentation of the tips
	 */
	public static final int TIP_GAP = 15;
	/**
	 * The width of the tips
	 */
	public static final int TIP_WIDTH = 200;

	/**
	 * Size of cell flag
	 */
	/** Small flag */
	public static final int FLAG_SIZE_SMALL = 8;
	/** Big flag */
	public static final int FLAG_SIZE_BIG = 10;
	/**
	 * Minimum file cache size
	 */
	public static final int MIN_BUFF_SIZE = 4096;

	/**
	 * No mask. Any one that does not conflict with Event.CTRL_MASK etc.
	 */
	public static final int NO_MASK = 1 << 6;

	/**
	 * After the locale changes, the language-related constants should be reset.
	 */
	public static void resetLocal() {
		FONTSIZEDISP = new String[] {
				IdeCommonMessage.get().getMessage("gc.fontsix"),
				IdeCommonMessage.get().getMessage("gc.fontsmallfive"),
				IdeCommonMessage.get().getMessage("gc.fontfive"),
				IdeCommonMessage.get().getMessage("gc.fontsmallfour"),
				IdeCommonMessage.get().getMessage("gc.fontfour"),
				IdeCommonMessage.get().getMessage("gc.fontsmallthree"),
				IdeCommonMessage.get().getMessage("gc.fontthree"),
				IdeCommonMessage.get().getMessage("gc.fontsmalltwo"),
				IdeCommonMessage.get().getMessage("gc.fonttwo"),
				IdeCommonMessage.get().getMessage("gc.fontsmallone"),
				IdeCommonMessage.get().getMessage("gc.fontone"),
				IdeCommonMessage.get().getMessage("gc.fontsmallzero"),
				IdeCommonMessage.get().getMessage("gc.fontzero") };
	}

	/**
	 * Upper limit of recent files
	 */
	public static final int RECENT_MENU_COUNT = 20;
	/**
	 * The number of recent login information saved in the remote service login
	 * interface
	 */
	public static final int REMOTESERVER_COUNT = 5;

}
