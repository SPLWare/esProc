package com.scudata.chart.edit;

import java.util.*;

import com.scudata.chart.resources.*;
import com.scudata.common.MessageManager;

import java.io.*;

/**
 * 图元库管理类，每一个新实现的图元都需要在loadSystemElements()中登记
 * 登记后的图元才会在界面自动列出，以及对参数进行编辑
 * @author Joancy
 *
 */
public class ElementLib {
	private static ArrayList<String> groupList = new ArrayList<String>(20);
	private static ArrayList<ArrayList<ElementInfo>> elementList = new ArrayList<ArrayList<ElementInfo>>(
			20);
	private static MessageManager mm = ChartMessage.get();

	static {
		loadSystemElements();
	}

	private static int indexof(ArrayList<ElementInfo> al, String name) {
		int size = al.size();
		for (int i = 0; i < size; i++) {
			ElementInfo ei = al.get(i);
			if (ei.getName().equalsIgnoreCase(name))
				return i;
		}
		return -1;
	}

	private static ArrayList<ElementInfo> getElementList(String group) {
		int size = groupList.size();
		for (int i = 0; i < size; i++) {
			String groupTitle = (String) groupList.get(i);
			if (groupTitle.equalsIgnoreCase(group))
				return elementList.get(i);
		}
		ArrayList<ElementInfo> newElement = new ArrayList<ElementInfo>();
		groupList.add(group);
		elementList.add(newElement);
		return newElement;
	}

	/**
	 * 根据图元名称获取对应的图元信息类
	 * @param name 名称
	 * @return 图元信息类
	 */
	public static ElementInfo getElementInfo(String name) {
		for (int i = 0; i < elementList.size(); i++) {
			ArrayList<ElementInfo> al = elementList.get(i);
			ElementInfo ei = getElementInfo(al, name);
			if (ei != null)
				return ei;
		}
		throw new RuntimeException(mm.getMessage("ElementLib.badelement",name));
	}

	/**
	 * 在指定的图元信息列表中查找图元信息
	 * @param al 图元信息列表
	 * @param name 图元名称
	 * @return 图元信息类
	 */
	public static ElementInfo getElementInfo(ArrayList<ElementInfo> al,
			String name) {
		int i = indexof(al, name);
		if (i >= 0)
			return al.get(i);
		return null;
	}

	/**
	 * 在程序中添加一个图元信息
	 * @param group 图元所属的分组
	 * @param name 名称
	 * @param className 类的全路径名称
	 */
	public static void addElement(String group, String name, String className) {
		try {
			String groupTitle = mm.getMessage(group);
			ArrayList<ElementInfo> al = getElementList(groupTitle);

			Class elemClass = Class.forName(className);
			String title = mm.getMessage(name);

			ElementInfo ei = new ElementInfo(name, title, elemClass);
			int i = indexof(al, name);
			if (i >= 0) {
				al.add(i, ei);
			} else {
				al.add(ei);
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * 获取当前环境下的全部图元信息列表
	 * @return 图元信息列表
	 */
	public static ArrayList<ArrayList<ElementInfo>> getElementInfoList() {
		return elementList;
	}

	/**
	 * 获取当前环境下全部图元组标题
	 * @return 全部组标题列表
	 */
	public static ArrayList<String> getElementTitleList() {
		return groupList;
	}

	/**
	 * 登记全部实现的系统图元
	 */
	public static void loadSystemElements() {
		String group = "axis";
		addElement(group, "MapAxis", "com.raqsoft.chart.element.MapAxis");
		addElement(group, "NumericAxis",
				"com.raqsoft.chart.element.NumericAxis");
		addElement(group, "EnumAxis", "com.raqsoft.chart.element.EnumAxis");
		addElement(group, "DateAxis", "com.raqsoft.chart.element.DateAxis");
		addElement(group, "TimeAxis", "com.raqsoft.chart.element.TimeAxis");

		group = "element";
		addElement(group, "Dot", "com.raqsoft.chart.element.Dot");
		addElement(group, "Line", "com.raqsoft.chart.element.Line");
		addElement(group, "Column", "com.raqsoft.chart.element.Column");
		// addElement(group,"Polygon","com.raqsoft.chart.element.Polygon");
		addElement(group, "Sector", "com.raqsoft.chart.element.Sector");
		addElement(group, "Text", "com.raqsoft.chart.element.Text");

		group = "Graph";
		addElement(group, "GraphColumn", "com.raqsoft.chart.graph.GraphColumn");
		addElement(group, "GraphLine", "com.raqsoft.chart.graph.GraphLine");
		addElement(group, "GraphPie", "com.raqsoft.chart.graph.GraphPie");
		addElement(group, "Graph2Axis", "com.raqsoft.chart.graph.Graph2Axis");

		group = "other";
		addElement(group, "BackGround", "com.raqsoft.chart.element.BackGround");
		addElement(group, "Legend", "com.raqsoft.chart.element.Legend");
	}

	/**
	 * 加载自定义图元信息
	 * @param is 自定义信息的配置文件流
	 */
	public static void loadCustomElements(InputStream is) {
		try {
			Properties pt = new Properties();
			pt.load(is);
			int c = 0;

			for (Enumeration e = pt.propertyNames(); e.hasMoreElements();) {
				Object key = e.nextElement();
				String value = (String) pt.get(key);
				value = new String(value.getBytes("ISO-8859-1"), "gbk");
				String[] items = value.split(",");
				int pos = value.indexOf(',');
				String type = items[0];
				String title = items[1];
				String cls = items[2];
			} // for
		} catch (IOException e) {
			// Logger.error(e.getMessage());
		}
	}

}
