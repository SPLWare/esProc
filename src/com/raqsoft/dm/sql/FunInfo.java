package com.raqsoft.dm.sql;

/**
 * 标准函数信息(标准函数名+参数个数唯一)
 * @author RunQian
 *
 */
public class FunInfo implements Comparable<FunInfo> {
	private String name; // 标准函数名
	private int pcount; // 参数个数，-1表示不定参数个数

	public FunInfo() {
	}

	public FunInfo(String name, int pcount) {
		this.name = name;
		this.pcount = pcount;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getParamCount() {
		return pcount;
	}

	public void setParamCount(int pc) {
		this.pcount = pc;
	}

	public int hashCode() {
		return (name.hashCode() << 24) + pcount;
	}

	public boolean equals(Object o) {
		if (o instanceof FunInfo) {
			return compareTo((FunInfo)o) == 0;
		} else {
			return false;
		}
	}

	// 用作map的key
	public int compareTo(FunInfo o) {
		FunInfo funInfo = (FunInfo)o;
		int cmp = name.compareToIgnoreCase(funInfo.name);
		if (cmp == 0) {
			return pcount - funInfo.pcount;
		} else {
			return cmp;
		}
	}
}
