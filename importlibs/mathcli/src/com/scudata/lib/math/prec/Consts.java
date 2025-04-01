package com.scudata.lib.math.prec;

import com.scudata.dm.DataStruct;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Table;

/**
 * 常数
 *
 */
public class Consts {
	/**
	 * 变量类型
	 */

	/** 缺省，自动检测 **/
	public static final byte F_DEFAULT = 0;
	/** 二值变量：变量取值在{0,1}内，即只有两种可能性 **/
	public static final byte F_TWO_VALUE = 1;
	/** 单值变量 **/
	public static final byte F_SINGLE_VALUE = 2;
	/** 枚举变量：不是二值和序数的其他离散变量 **/
	public static final byte F_ENUM = 3;

	/** 数值变量：变量取值为实数 **/
	public static final byte F_NUMBER = 11;
	/** 计数变量：变量取值为整数，也属于数值变量 **/
	public static final byte F_COUNT = 12;
	/** 日期 **/
	public static final byte F_DATE = 13;
	/** 不作为数值或计数处理的连续变量 **/
	public static final byte F_CONTINUITY = 20;
	/** 长文本类型，不参与建模 **/
	public static final byte F_LONG_TEXT = 21;

	/** 该变量在预处理中由于空值过多或者枚举值过多被删除，并不会生成BI,MI等 **/
	public static final byte F_DEL_I = 31;
	/** 该变量在二值变量选择中被删除，与MVP无关 **/
	public static final byte F_DEL_B = 32;
	/** 该变量在离散变量选择中被删除，可以不再执行Normalize以及Standardize **/
	public static final byte F_DEL_N = 33;

	/** t_type: target type. 0 binary, 1 categorical 2 numerical **/
	public static final byte TARGET_TYPE_BINARY = 0;
	public static final byte TARGET_TYPE_CATEGORICAL = 1;
	public static final byte TARGET_TYPE_NUMERICAL = 2;

	public static final String PARAM_NULL = "None";

	public static final byte DCT_DATETIME = 1;
	public static final byte DCT_DATE = 2;
	public static final byte DCT_TIME = 3;
	public static final byte DCT_UDATE = 4;
	
	/** 数据挖掘中变换记录表P的数据结构 **/
	public final static DataStruct DSP = new DataStruct(new String[] { "TYPE",
			"MERGE", "ITEM", "FREQ", "AVG", "SE", "MEDIAN", "UP", "LOW", "MIN",
			"PT", "VALUE", "OP", "MIMA", "IST", "NEWNAME", "MIINDEX", "BI",
			"MVP", "DTYPE" });

	/** 变化表P中变量属性字段号 **/
	public static final int P_TYPE = 0;
	public static final int P_MERGE = 1;
	public static final int P_ITEM = 2;
	public static final int P_FREQ = 3;
	public static final int P_AVG = 4;
	public static final int P_SE = 5;
	public static final int P_MEDIAN = 6;
	public static final int P_UP = 7;
	public static final int P_LOW = 8;
	public static final int P_MIN = 9;
	public static final int P_PT = 10;
	public static final int P_VALUE = 11;
	public static final int P_OP = 12;
	public static final int P_MIMA = 13;
	public static final int P_IST = 14;
	public static final int P_NEWNAME = 15;
	public static final int P_MIINDEX = 16;
	public static final int P_BI = 17;
	public static final int P_MVP = 18;
	public static final int P_DATETYPE = 19;
	// 对于日期型字段，处理模式和其它类型字段过程不一致，它们复用前面的记录位置，使用时先判断有无DTYPE即可
	public static final int P_DATE_HOUR = 1;
	public static final int P_DATE_AM = 2;
	public static final int P_DATE_NIGHT = 3;
	public static final int P_DATE_MONTH = 4;
	public static final int P_DATE_SEASON = 5;
	public static final int P_DATE_WEEK = 6;
	public static final int P_DATE_TOTODAY = 7;
	public static final int P_DATE_REL = 8;

	public static final Integer CONST_YES = new Integer(1);
	public static final Integer CONST_NO = new Integer(0);

	public static final String CONST_OTHERS = "others";
	public static final String CONST_NULL = "missing";
	public static final Integer CONST_OTHERNUMS = Integer.MAX_VALUE;
	public static final Integer CONST_NULLNUM = new Integer(Integer.MAX_VALUE - 1);

	public static Table ptt(int cols) {
		Table P = new Table(DSP);
		for (int i = 0; i < cols; i++) {
			P.newLast();
		}
		return P;
	}

	public static synchronized void set(Table P, int ci, int prop, Object pv) {
		if (P == null) {
			return;
		}
		BaseRecord r = P.getRecord(ci + 1);
		r.set(prop, pv);
	}

	public static void set(BaseRecord r, int prop, Object pv) {
		r.set(prop, pv);
	}

	public static Object get(Table P, int ci, int prop) {
		if (P == null) {
			return null;
		}
		BaseRecord r = P.getRecord(ci + 1);
		return r.getFieldValue(prop);
	}

	public static Object get(BaseRecord r, int prop) {
		return r.getFieldValue(prop);
	}

	public static boolean isDispersed(byte type) {
		return (type == F_TWO_VALUE || type == F_ENUM);
	}

	public static boolean isContinuous(byte type) {
		return (type == F_NUMBER || type == F_COUNT);
	}

	public static double getPLevel(int size) {
		if (size > 256000) {
			return 0.005;
		} else if (size > 64000) {
			return 0.01;
		} else if (size > 16000) {
			return 0.02;
		} else if (size > 4000) {
			return 0.03;
		} else if (size > 1000) {
			return 0.04;
		} else {
			return 0.05;
		}
	}

	public static double getSmoothFactor(int size) {
		if (size >= 2000000) {
			return 50d;
		} else if (size > 1000000) {
			return size * 0.000025;
		} else if (size >= 50000) {
			return 25d;
		} else if (size > 10000) {
			return size * 0.0005;
		} else {
			return 5d;
		}
	}
}
