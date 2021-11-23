package com.scudata.ide.dfx.etl;

import java.util.Vector;

import com.scudata.ide.common.swing.JComboBoxEx;
import com.scudata.ide.dfx.GVDfx;

/**
 * 先实现了部分跟ETL相关的函数辅助编辑
 * 该类定义辅助编辑环境中用到的常量定义
 * 以及部分工具函数
 * 
 * @author Joancy
 *
 */
public class EtlConsts {
	public final static byte TYPE_EMPTY = 0;
	public final static byte TYPE_DB = 1;
	public final static byte TYPE_FILE = 2;
	public final static byte TYPE_SEQUENCE = 3;
	public final static byte TYPE_CURSOR = 4;
	public final static byte TYPE_CTX = 5;//组表
	public final static byte TYPE_XLS = 6;//XLS对象
	public final static byte TYPE_STRING = 7;

	// 函数参数相关属性
	public final static int INPUT_SEPERATOR = 1002;
	public final static int INPUT_SEPERATOR2 = 1003;//可以选择空的分隔符，代表删除该项
	public final static int INPUT_STRINGLIST = 1004;
	public final static int INPUT_ISOLATIONLEVEL = 1005;//事务孤立级别
	public final static int INPUT_DB = 1006;//下拉数据源名称
	public final static int INPUT_ONLYPROPERTY = 1007;//仅能输入字串属性，不能输入表达式
	
	public final static int INPUT_CELLA = 1011;//序列类型的格子下拉列表 A
	public final static int INPUT_CELLAORCS = 1012;//序列和游标的格子下拉列表 A  or  Cursor
	public final static int INPUT_CELLFILE = 1013;//文件对象 下拉列表 File
	public final static int INPUT_CELLCURSOR = 1014;//下拉列表 Cursor
	public final static int INPUT_CELLBCTX = 1015;//BTX and CTX
	public final static int INPUT_CELLXLS = 1016;//XLS
	
	public final static int INPUT_FIELDDEFINE_NORMAL = 1021;//字段名，类型(下拉),format
	public final static int INPUT_FIELDDEFINE_EXP_FIELD = 1022;//表达式，字段名
	public final static int INPUT_FIELDDEFINE_FIELD_EXP = 1023;//字段名，表达式
	public final static int INPUT_FIELDDEFINE_RENAME_FIELD = 1024;//原列名，新列名
	
	public final static int INPUT_FIELDDEFINE_FIELD_DIM = 1030;//字段名,维(Checkbox)


	/**
	 * 参数值是列表或者复杂的多项时，需要禁止表达式编辑
	 * @param type 编辑类型
	 * @return 需要禁止表达式列时返回true，否则返回false
	 */
	public static boolean isDisableExpEditType(int type){
		if(type==INPUT_STRINGLIST){
			return true;
		}
		if(type==INPUT_FIELDDEFINE_NORMAL){
			return true;
		}
		if(type==INPUT_FIELDDEFINE_EXP_FIELD){
			return true;
		}
		if(type==INPUT_FIELDDEFINE_FIELD_EXP){
			return true;
		}
		if(type==INPUT_FIELDDEFINE_RENAME_FIELD){
			return true;
		}
		if(type==INPUT_FIELDDEFINE_FIELD_DIM){
			return true;
		}
		if(type==INPUT_ONLYPROPERTY){
			return true;
		}
		
		return false;
	}
	
	/**
	 * 获取数据来源type的描述信息
	 * @param type 数据来源类型
	 * @return 文本描述信息
	 */
	public static String getTypeDesc(byte type){
		switch(type){
		case TYPE_DB:
			return "DB";
		case TYPE_FILE:
			return "File";
		case TYPE_SEQUENCE:
			return "Sequence";
		case TYPE_CURSOR:
			return "Cursor";
		case TYPE_CTX:
			return "CTX";
		case TYPE_XLS:
			return "XLS";
		}
		return "";
	}

	/**
	 * 获取数据库的隔离级别下拉列表
	 * @return 下拉列表
	 */
	public static JComboBoxEx getIsolationLevelBox(){
		Vector<String> scodes = new Vector<String>(),sdisps = new Vector<String>();
	    scodes.add("");
	    scodes.add("n");
	    scodes.add("c");
	    scodes.add("u");
	    scodes.add("r");
	    scodes.add("s");

	    sdisps.add("");
	    sdisps.add("None");
	    sdisps.add("Commit");
	    sdisps.add("Uncommit");
	    sdisps.add("Repeatable");
	    sdisps.add("Serializable");
	    JComboBoxEx combo = new JComboBoxEx();
	    combo.x_setData(scodes, sdisps);
		return combo;
	}
	
	/**
	 * 获取当前环境下的所有定义的数据库的名称下拉列表
	 * @return 下拉列表
	 */
	public static JComboBoxEx getDBBox(){
		Vector<String> dbNames = GVDfx.dsModel.listNames();
		dbNames.insertElementAt("", 0);
	    JComboBoxEx combo = new JComboBoxEx();
	    combo.x_setData(dbNames, dbNames);
		return combo;
	}

	/**
	 * 获取文本数据源时，分隔符类型的下拉列表
	 * @param allowEmpty 是否添加一个空值使得用户可以选择空表示没选任何值
	 * @return 下拉列表
	 */
	public static JComboBoxEx getSeperatorComboBox(boolean allowEmpty) {
		String msg = "SPACE";
		String SEP_SPACE = msg;
		String SEP_TAB = "TAB";
		Vector<String> scodes = new Vector<String>(), sdisps = new Vector<String>();
		if (allowEmpty) {
			scodes.add("");
			sdisps.add("");
		}
		// 分隔符
		scodes.add("\t");
		scodes.add(",");
		scodes.add(" ");
		scodes.add("|");
		scodes.add("-");
		scodes.add("_");
		sdisps.add(SEP_TAB);
		sdisps.add(",");
		sdisps.add(SEP_SPACE);
		sdisps.add("|");
		sdisps.add("-");
		sdisps.add("_");
		JComboBoxEx combo = new JComboBoxEx();
		combo.x_setData(scodes, sdisps);
		return combo;
	}
	
}
