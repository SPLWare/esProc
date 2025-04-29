package com.scudata.dm.query;

import com.scudata.dm.query.Select.Exp;

public class SortItem {
	public static final int ASC = 1;
	public static final int DESC = -1;
	
	private Exp sortExp;
	private int order = ASC;
	
	public SortItem(Exp sortExp, String order) {
		this.sortExp = sortExp;
		setOrder(order);
	}

	public Exp getSortExp() {
		return sortExp;
	}

	public void setSortExp(Exp sortExp) {
		this.sortExp = sortExp;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
	
	public void setOrder(String order) {
		if (order == null || order.equals("ASC")) {
			this.order = ASC;
		} else {
			this.order = DESC;
		}
	}
}
