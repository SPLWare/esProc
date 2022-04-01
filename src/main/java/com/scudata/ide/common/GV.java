package com.scudata.ide.common;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import com.scudata.app.config.RaqsoftConfig;
import com.scudata.ide.common.control.CellSelection;
import com.scudata.ide.common.control.FuncWindow;
import com.scudata.ide.common.dialog.DialogMemory;
import com.scudata.ide.custom.IResourceTreeBase;

/**
 * Global Variant
 *
 */
public class GV {
	/**
	 * IDE current main frame object
	 */
	public static AppFrame appFrame = null;

	/**
	 * IDE current main menu object
	 */
	public static AppMenu appMenu = null;

	/**
	 * File path opened last time
	 */
	public static String lastDirectory = System.getProperty("user.home");

	/**
	 * The locale of the designer
	 */
	public static Locale language = Locale.getDefault();

	/**
	 * Clipboard
	 */
	public static CellSelection cellSelection = null;

	/**
	 * Console
	 */
	public static Console console = null;

	/**
	 * 通过参数传递的启动文件名（双击打开时传递的）
	 */
	public static String directOpenFile = "";

	/**
	 * Clean up memory dialog
	 */
	public static DialogMemory dialogMemory = null;

	/**
	 * Data source model
	 */
	public static DataSourceListModel dsModel = null;

	/**
	 * Open application object container
	 */
	public static HashSet<AppFrame> allFrames = new HashSet<AppFrame>();

	/**
	 * Esproc IDE user ID
	 */
	public static String userID = null;

	/**
	 * Number of times used
	 */
	public static long usedTimes;

	/**
	 * Active sheet
	 */
	public static IPrjxSheet appSheet = null;

	/**
	 * Active toolbar
	 */
	public static AppToolBar appTool = null;

	/**
	 * Active property toolbar
	 */
	public static ToolBarPropertyBase toolBarProperty = null;

	/**
	 * Window toolbar
	 */
	public static ToolBarWindow toolWin = null;

	/**
	 * The object that issued the command. Used to prevent recursive triggering
	 * of commands.
	 */
	public static Object cmdSender = null;

	/**
	 * Whether the cell is being edited
	 */
	public static boolean isCellEditing = false;

	/**
	 * Function window
	 */
	private static FuncWindow funcWindow = null;

	/**
	 * Get function window
	 * 
	 * @return
	 */
	public static FuncWindow getFuncWindow() {
		if (funcWindow == null) {
			funcWindow = new FuncWindow();
		}
		return funcWindow;
	}

	/**
	 * RaqsoftConfig object
	 */
	public static RaqsoftConfig config = null;

	/**
	 * EsProc Resource Tree
	 */
	public static IResourceTreeBase fileTree;

	/**
	 * EsProc remote service data sources
	 */
	public static Map<String, DataSourceListModel> dsModelRemote;

}
