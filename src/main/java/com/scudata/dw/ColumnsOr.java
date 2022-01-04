package com.scudata.dw;

import java.util.ArrayList;
import java.util.Arrays;

import com.scudata.expression.Node;

public class ColumnsOr {
	private ArrayList<IFilter> filterList = new ArrayList<IFilter>();

	private Node node;
	public boolean sign;
	
	public ArrayList<IFilter> getFilterList() {
		return filterList;
	}

	public void setNode(Node node) {
		this.node= node;
	}
	
	public Node getNode() {
		return node;
	}
	
	public void addFilter(IFilter filter) {
		int size = filterList.size();
		for (int i = 0; i < size; ++i) {
			IFilter f = filterList.get(i);
			if (f.isSameColumn(filter)) {
				filterList.set(i, new LogicOr(f, filter));
				return;
			}
		}
		
		filterList.add(filter);
	}
	
	public void combineColumnsOr(ColumnsOr colsOr) {
		ArrayList<IFilter> list = colsOr.getFilterList();
		for (IFilter f : list) {
			addFilter(f);
		}
		colsOr = null;
	}
	
	public IFilter[] toArray() {
		int size = filterList.size();
		IFilter[] filters = new IFilter[size];
		filterList.toArray(filters);
		Arrays.sort(filters);
		for (int i = 0; i < size; ++i) {
			ColumnOr colOr = new ColumnOr(this, filters[i]);
			colOr.setI(i);
			filters[i] = colOr;
		}
		((ColumnOr)filters[size - 1]).setLastCol(true);
		return filters;
	}
}