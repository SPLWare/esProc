package com.scudata.dm.sql;

import java.util.HashMap;
import java.util.Map;

/**
 * 函数参数固定的标准函数信息
 * @author RunQian
 *
 */
public class FixedParamFunInfo extends FunInfo {
	public static final String NONSUPPORT = "N/A";

	private HashMap<Integer, String> infoMap = new HashMap<Integer, String>();

	public FixedParamFunInfo() {
	}

	public FixedParamFunInfo(String name, int pcount) {
		super(name, pcount);
	}

	/*
	 * info值有以下3种情况： 为null，表示与标准函数一样 为N/A(不区分大小写)，表示不支持此函数
	 * 否则表示数据库SQL表达式，以?n表示标准函数的第n个参数；
	 */

	public String getInfo(int dbType) {
		return infoMap.get(new Integer(dbType));
	}

	public void setInfo(int dbType, String info) {
		infoMap.put(new Integer(dbType), info);
	}

	public Map<Integer, String> getInfos() {
		return infoMap;
	}
}
