package com.scudata.cellset.graph.draw;

import java.util.*;
import java.awt.*;

import com.scudata.cellset.*;
import com.scudata.cellset.graph.config.*;
import com.scudata.chart.Consts;
import com.scudata.common.StringUtils;

/**
 * 扩展图形分类
 * 
 * @author Joancy
 *
 */
public class ExtGraphProperty {
	public ArrayList categories;
	public ArrayList category2 = null;
	private BackGraphConfig bgc = null;
	private boolean isSplitByAxis = false;

	public IGraphProperty getIGraphProperty() {
		return prop;
	}

	/**
	 * 取所有分类的名称
	 * 
	 * @return
	 */
	public Vector getCategoryNames() {
		return listCategoryNames(categories);
	}

	/**
	 * 将数组结构的分类转换为列表对象
	 * 
	 * @param cats
	 *            分类
	 * @return 列表
	 */
	public static ArrayList getArrayList(ExtGraphCategory[] cats) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < cats.length; i++) {
			list.add(cats[i]);
		}
		return list;
	}

	/**
	 * 列出所有分类的名称
	 * 
	 * @param categories
	 *            分类
	 * @return 名称
	 */
	public Vector listCategoryNames(ExtGraphCategory[] categories) {
		return listCategoryNames(getArrayList(categories));
	}

	/**
	 * 列出所有分类的名称
	 * 
	 * @param categories
	 *            分类
	 * @return 名称
	 */
	public Vector listCategoryNames(ArrayList categories) {
		Vector v = new Vector();
		if (categories == null) {
			return v;
		}
		for (int i = 0; i < categories.size(); i++) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);
			v.add(egc.getName()); // 趋势图由于输出时有格式,所以该值只能是原始对象,不能用串串
		}
		return v;
	}

	/**
	 * 列出分类属性中所有系列的名字
	 * 
	 * @param cats
	 *            分类属性(包含系列)
	 * @return 系列的名字
	 */
	public Vector listSeriesNames(ExtGraphCategory[] cats) {
		return listSeriesNames(getArrayList(cats));
	}

	protected String getReportASeriesName(Object series) {
		return null;
	}

	/**
	 * 列出分类属性中所有系列的名字
	 * 
	 * @param cats
	 *            分类属性(包含系列)
	 * @return 系列的名字
	 */
	public Vector listSeriesNames(ArrayList cats) {
		Vector names = new Vector();
		for (int c = 0; c < cats.size(); c++) {
			ArrayList series = ((ExtGraphCategory) cats.get(c)).getSeries();
			for (int i = 0; i < series.size(); i++) {
				Object ser = series.get(i);
				String name = "";
				if (_instanceof(ser, "ExtGraphSery")) {
					ExtGraphSery egs = (ExtGraphSery) ser;
					name = egs.getName();
				} else {
					name = getReportASeriesName(ser);
				}
				if (names.contains(name)) {
					continue;
				}
				names.add(name);
			}
		}
		return names;
	}

	/**
	 * 当需要指定系列所在轴时，在分类对象中找到第一个同名的系列对象即可 该函数只能在指定系列轴中调用，否则当甘特等图形时，系列对象不是
	 * ExtGraphSery
	 * 
	 * @param sname
	 *            系列名字
	 * @return 系列对象
	 */
	public ExtGraphSery getEGS(String sname) {
		ArrayList cats = categories;
		for (int c = 0; c < cats.size(); c++) {
			ArrayList series = ((ExtGraphCategory) cats.get(c)).getSeries();
			for (int i = 0; i < series.size(); i++) {
				Object ser = series.get(i);
				String name = "";
				if (_instanceof(ser, "ExtGraphSery")) {
					ExtGraphSery egs = (ExtGraphSery) ser;
					name = egs.getName();
				} else {
					name = getReportASeriesName(ser);
				}
				if (sname.equals(name)) {
					return (ExtGraphSery) ser;
				}
			}
		}
		return null;
	}

	protected Vector getReportSeriesNames(ArrayList cats) {
		return null;
	}

	/**
	 * 获取分类属性中所有系列的名字,不同于listSeriesNames,
	 * 该函数兼容报表中系列的格式
	 * @param cats
	 *            分类属性(包含系列)
	 * @return 系列的名字
	 */
	public Vector getSeriesNames(ArrayList cats) {
		if (cats == null) {
			return new Vector();
		}
		Vector v = getReportSeriesNames(cats);
		if (v != null)
			return v;
		return listSeriesNames(cats);
	}

	/**
	 * 构造函数
	 * @param graph 图形属性接口，该值为null时方便报表中的子类创建一个空的实例。
	 * 这个空实例用于Option对象调用其中的listCategory以及listSeries方法，从而不需要调整这些方法的继承执行顺序
	 */
	public ExtGraphProperty(IGraphProperty graph) {
		if( graph==null )return;
		prop = graph;
		graphType = prop.getType();
	}

	/**
	 * 参见PublicProperty同名方法
	 */
	public byte getCurveType() {
		return prop.getCurveType();
	}

	/**
	 * 饼图时是否将最大一块分离显示
	 * @return 是返回true，否则false
	 */
	public boolean isCutPie() {
		return prop.isPieSpacing();
	}

	/**
	 * 参见PublicProperty同名方法
	 */
	public boolean isMeterColorEnd() {
		return prop.isMeterColorEnd();
	}

	/**
	 * 参见PublicProperty同名方法
	 */
	public boolean isMeterTick() {
		return prop.isMeterTick();
	}

	/**
	 * 参见PublicProperty同名方法
	 */
	public int getMeter3DEdge() {
		return prop.getMeter3DEdge();
	}

	/**
	 * 参见PublicProperty同名方法
	 */
	public int getMeterRainbowEdge() {
		return prop.getMeterRainbowEdge();
	}

	/**
	 * 参见PublicProperty同名方法
	 */
	public int getPieLine() {
		return prop.getPieLine();
	}

	/**
	 * 判断当前图形是否双轴图
	 * @return 双轴图形返回true，否则返回false
	 */
	public boolean is2YGraph() {
		byte type = this.getType();
		return type == GraphTypes.GT_2Y2LINE || type == GraphTypes.GT_2YCOLLINE
				|| type == GraphTypes.GT_2YCOLSTACKEDLINE;
	}

	/**
	 * 判断当前图形是否仅为普通的堆积图，而不包含双轴的堆积
	 * @return 普通堆积图返回true，否则返回false
	 */
	public boolean isNormalStacked() {
		return isNormalStacked(getType());
	}

	/**
	 * 判断type图形是否仅为普通的堆积图，而不包含双轴的堆积
	 * @param type 图类型
	 * @return 普通堆积图返回true，否则返回false
	 */
	public static boolean isNormalStacked(byte type) {
		return type == GraphTypes.GT_BARSTACKED
				|| type == GraphTypes.GT_BARSTACKED3DOBJ
				|| type == GraphTypes.GT_COLSTACKED
				|| type == GraphTypes.GT_COLSTACKED3DOBJ;
	}

	/**
	 * 判断当前图形instance是否为堆积图形
	 * @param instance 图形实现的实例
	 * @return 堆积图形时返回true，否则返回false
	 */
	public boolean isStackedGraph(DrawBase instance) {
		if (instance == null) {
			byte type = this.getType();
			return type == GraphTypes.GT_BARSTACKED
					|| type == GraphTypes.GT_BARSTACKED3DOBJ
					|| type == GraphTypes.GT_COLSTACKED
					|| type == GraphTypes.GT_2YCOLSTACKEDLINE
					|| type == GraphTypes.GT_COLSTACKED3DOBJ;
		} else {
			String className = instance.getClass().getName();
			boolean isStacked = className.indexOf("Stacked") > 0;
			return isStacked;
		}
	}

	/**
	 * 判断当前图形instance是否为条形图，即横向的柱形图
	 * @param instance 图形实现的实例
	 * @return 条形图时返回true，否则返回false
	 */
	public boolean isBarGraph(DrawBase instance) {
		if (instance == null) {
			byte type = this.getType();
			return (type == GraphTypes.GT_BAR || type == GraphTypes.GT_BAR3D
					|| type == GraphTypes.GT_BAR3DOBJ
					|| type == GraphTypes.GT_BARSTACKED || type == GraphTypes.GT_BARSTACKED3DOBJ);
		} else {
			String className = instance.getClass().getName();
			boolean isBar = className.indexOf("Bar") > 0;
			return isBar;
		}
	}

	/**
	 * 根据分类名称catName获取扩展图形分类对象
	 * @param catName 分类的名称
	 * @return 扩展图形分类
	 */
	public ExtGraphCategory getExtGraphCategory(Object catName) {
		for (int i = 0; i < categories.size(); i++) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);
			if (catName.equals(egc.getName())) {
				return egc;
			}
		}
		return null;
	}

	/**
	 * 是否正常统计图,即时间走势图,甘特图,里程碑,时间状态图以外的所有图形
	 * 
	 * @return boolean 常规图形返回true，否则返回false
	 */
	public static boolean isNormalGraph(byte type) {
		return (type != GraphTypes.GT_TIMETREND
				&& type != GraphTypes.GT_TIMESTATE
				&& type != GraphTypes.GT_GANTT && type != GraphTypes.GT_GONGZI
				&& type != GraphTypes.GT_RANGE && type != GraphTypes.GT_MILEPOST);
	}

	protected void reportRecalcProperty() {
	}

	/**
	 * 重新按照属性值的设置来调整相应数据
	 */
	public void recalcProperty() {
		discardNoNameData();
		if (isNormalGraph(getType()) && topN > 0) {
			extractTopNCat();
		}
		if (is2YGraph()) {
			split2YCat();
		}
		reportRecalcProperty();
	}

	protected void splitCategory(Vector seriesName1, Vector seriesName2) {
		ArrayList newCat1 = new ArrayList();
		ArrayList newCat2 = new ArrayList();

		for (int i = 0; i < categories.size(); i++) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);

			ExtGraphCategory egc1 = new ExtGraphCategory();
			egc1.setName(egc.getName());
			ArrayList ser1 = new ArrayList();
			for (int j = 0; j < seriesName1.size(); j++) {
				String name = (String) seriesName1.get(j);
				ExtGraphSery egs = egc.getExtGraphSery(name);
				ser1.add(egs);
			}
			egc1.setSeries(ser1);
			newCat1.add(egc1);

			ExtGraphCategory egc2 = new ExtGraphCategory();
			egc2.setName(egc.getName());
			ArrayList ser2 = new ArrayList();
			for (int j = 0; j < seriesName2.size(); j++) {
				String name = (String) seriesName2.get(j);
				ExtGraphSery egs = egc.getExtGraphSery(name);
				// if (egs != null) {
				ser2.add(egs);
				// }
			}
			egc2.setSeries(ser2);
			newCat2.add(egc2);
		}
		this.categories = newCat1;
		this.category2 = newCat2;
	}

	/**
	 * 设置根据轴分割数据
	 * @param set 是否轴分割
	 */
	public void setSplitByAxis(boolean set) {
		isSplitByAxis = set;
	}

	protected boolean isSplitByAxis() {
		return isSplitByAxis;
	}

	private void split2YCat() {
		if (isSplitByAxis()) {
			Vector allSeriesName = getSeriesNames(categories);
			int total;
			total = allSeriesName.size();

			Vector seriesName1 = new Vector();
			Vector seriesName2 = new Vector();
			for (int i = 0; i < total; i++) {
				Object sName = allSeriesName.get(i);
				ExtGraphSery egs = getEGS(sName.toString());
				if (egs.getAxis() == Consts.AXIS_RIGHT) {
					seriesName2.add(sName);
				} else {
					seriesName1.add(sName);
				}
			}
			splitCategory(seriesName1, seriesName2);
		} else {
			autoSplit2YCat();
		}
	}

	private void autoSplit2YCat() {
		Vector allSeriesName = getSeriesNames(categories);
		int total, s;
		total = allSeriesName.size();

		Vector seriesName1 = new Vector();
		Vector seriesName2 = new Vector();
		s = (int) ((total + 1) / 2);
		for (int i = 0; i < allSeriesName.size(); i++) {
			if (i >= s) {
				seriesName2.add(allSeriesName.get(i));
			} else {
				seriesName1.add(allSeriesName.get(i));
			}
		}
		ArrayList newCat1 = new ArrayList();
		ArrayList newCat2 = new ArrayList();

		for (int i = 0; i < categories.size(); i++) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);

			ExtGraphCategory egc1 = new ExtGraphCategory();
			egc1.setName(egc.getName());
			ArrayList ser1 = new ArrayList();
			for (int j = 0; j < seriesName1.size(); j++) {
				String name = (String) seriesName1.get(j);
				ExtGraphSery egs = egc.getExtGraphSery(name);
				ser1.add(egs);
			}
			egc1.setSeries(ser1);
			newCat1.add(egc1);

			ExtGraphCategory egc2 = new ExtGraphCategory();
			egc2.setName(egc.getName());
			ArrayList ser2 = new ArrayList();
			for (int j = 0; j < seriesName2.size(); j++) {
				String name = (String) seriesName2.get(j);
				ExtGraphSery egs = egc.getExtGraphSery(name);
				ser2.add(egs);
			}
			egc2.setSeries(ser2);
			newCat2.add(egc2);
		}
		this.categories = newCat1;
		this.category2 = newCat2;
	}

	private void discardNoNameData() {
		if (categories == null || categories.size() == 0) {
			throw new RuntimeException(
					"Error！Graph does not define categories!");
		}
		for (int i = categories.size() - 1; i >= 0; i--) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);
			if (!StringUtils.isValidString(egc.getNameString())) {
				categories.remove(i);
				continue;
			}
			ArrayList series = egc.getSeries();
			for (int j = series.size() - 1; j >= 0; j--) {
				Object objSer = series.get(j);
				String className = objSer.getClass().getName();
				if (_instanceof(objSer, "ExtGraphSery")) {
					ExtGraphSery egs = (ExtGraphSery) objSer;
					if (!StringUtils.isValidString(egs.getName())
							&& egs.isNull()) { // 当且仅当系列名称和值都为空的时候，忽略该系列
						series.remove(j);
						continue;
					}
				}
			}
		}
	}

	/**
	 * 封装一下实例判断的写法，简便代码的书写
	 * @param ins 实例对象
	 * @param className 类的名称
	 * @return 如果是当前类的实例则返回true，否则返回false
	 */
	public static boolean _instanceof(Object ins, String className) {
		return ins.getClass().getName().endsWith(className);
	}

	private void extractTopNCat() {
		int originCatNum = categories.size();
		if (topN <= 0 || topN > (originCatNum - 2)) { // 如果Other只有一个分类甚至没有时,没有意义
			return;
		}

//		categories的成员ExtGraphproperty按照sumSeries系列和实现了Compare方法，可以使用如下sort
//		但是取前三，必须先sort为正序，然后再反转，光reverse仅仅是将座位反一下
//		xq 2025年1月15日
		Collections.sort(categories);
		Collections.reverse(categories);
//		com.scudata.ide.common.GM.sort(categories, false); // 按照每个分类的系列和排序,取前TopN个

		ArrayList dataCategory = new ArrayList();
		for (int i = 0; i < topN; i++) {
			dataCategory.add(categories.get(i));
		}
		if (getFlag(IGraphProperty.FLAG_DISCARDOTHER)) {
			this.categories = dataCategory;
			return;
		}
		ExtGraphCategory otherCategoryData = new ExtGraphCategory();
		if (DrawBase.isChinese()) {
			otherCategoryData.setName("其他");
		} else {
			otherCategoryData.setName("Other");
		}

		HashMap otherSeriesData = new HashMap();
		for (int i = topN; i < categories.size(); i++) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);
			ArrayList series = egc.getSeries();
			for (int j = 0; j < series.size(); j++) {
				ExtGraphSery egs = (ExtGraphSery) series.get(j);
				Double seriesValue = (Double) otherSeriesData
						.get(egs.getName());
				if (seriesValue == null) {
					otherSeriesData.put(egs.getName(),
							new Double(egs.getValue()));
				} else {
					otherSeriesData.put(
							egs.getName(),
							new Double(seriesValue.doubleValue()
									+ egs.getValue()));
				}
			}
		}
		ArrayList otherSeries = new ArrayList();
		Iterator it = otherSeriesData.keySet().iterator();
		while (it.hasNext()) {
			String sName = (String) it.next();
			ExtGraphSery sd = new ExtGraphSery();
			sd.setName(sName);
			sd.setValue((Number) otherSeriesData.get(sName));
			otherSeries.add(sd);
		}
		otherCategoryData.setSeries(otherSeries);
		dataCategory.add(otherCategoryData);
		this.categories = dataCategory;
	}

	/**
	 * 取统计图类型
	 * 
	 * @return byte 统计图类型，由GraphTypes中的常量定义
	 */
	public byte getType() {
		return graphType;
	}

	/**
	 * 是否根据系列画图例数据
	 * @return 如果是则按系列画，否则按照分类画
	 */
	public boolean isLegendOnSery() {
		return prop.getDrawLegendBySery();
	}

	/**
	 * 取坐标轴颜色
	 * 
	 * @return int 坐标轴颜色
	 */
	public int getAxisColor() {
		return prop.getAxisColor();
	}

	/**
	 * 取全图背景颜色
	 * 
	 * @return int　全图背景颜色
	 */
	public int getCanvasColor() {
		return prop.getCanvasColor();
	}

	/**
	 * 取图形区背景颜色
	 * 
	 * @return int　图形区背景颜色
	 */
	public int getGraphBackColor() {
		return prop.getGraphBackColor();
	}

	/**
	 * 取统计图分类定义
	 * 
	 * @return ArrayList (GraphCategory)　统计图分类定义
	 */
	public ArrayList getCategories() {
		return this.categories;
	}

	/**
	 * 取横轴标题
	 * 
	 * @return String 横轴标题
	 */
	public String getXTitle() {
		return this.xTitle;
	}

	/**
	 * 取横轴标题的对齐方式
	 * @return 对齐方式
	 */
	public byte getXTitleAlign() {
		return prop.getXTitleAlign();
	}

	/**
	 * 取纵轴标题
	 * 
	 * @return String 纵轴标题
	 */
	public String getYTitle() {
		return this.yTitle;
	}

	/**
	 * 取纵轴标题的对齐方式
	 * @return 对齐方式
	 */
	public byte getYTitleAlign() {
		return prop.getYTitleAlign();
	}

	/**
	 * 取统计图标题
	 * 
	 * @return String　统计图标题
	 */
	public String getGraphTitle() {
		return this.graphTitle;
	}

	/**
	 * 取统计图标题的对齐方式
	 * @return 对齐方式
	 */
	public byte getGraphTitleAlign() {
		return prop.getGraphTitleAlign();
	}

	/**
	 * 取网格线类型
	 * 
	 * @return byte　网格线类型，值为LINE_NONE, LINE_SOLID, LINE_LONG_DASH,
	 *         LINE_SHORT_DASH, LINE_DOT_DASH, LINE_2DOT_DASH
	 */
	public byte getGridLineType() {
		return prop.getGridLineType();
	}

	/**
	 * 参考PublicProperty的同名方法
	 * @return
	 */
	public byte getGridLocation() {
		return prop.getGridLocation();
	}

	/**
	 * 取网格线颜色
	 * 
	 * @return int　网格线颜色
	 */
	public int getGridLineColor() {
		return prop.getGridLineColor();
	}

	/**
	 * 取柱形图或条形图间距
	 * 
	 * @return double　柱形图或条形图间距
	 */
	public double getBarDistance() {
		return this.barDistance;
	}

	/**
	 * 取图形格式
	 * 
	 * @return byte　图形格式, 值为IMAGE_JPG, IMAGE_GIF, IMAGE_PNG
	 */
	public byte getImageFormat() {
		return prop.getImageFormat();
	}

	/**
	 * 参考PublicProperty的同名方法
	 * @return
	 */
	public boolean isGraphTransparent() {
		return prop.isGraphTransparent();
	}

	/**
	 * 参考PublicProperty的同名方法
	 * @return
	 */
	public boolean isDrawDataTable() {
		return prop.isDrawDataTable();
	}

	/**
	 * 参考PublicProperty的同名方法
	 * @return
	 */
	public boolean isDataCenter() {
		return prop.isDataCenter();
	}

	/**
	 * 取是否渐变色
	 * 
	 * @return boolean
	 */
	public boolean isGradientColor() {
		return prop.isGradientColor();
	}

	/**
	 * 取统计图字体
	 * 
	 * @return GraphFonts　统计图字体
	 */
	public GraphFonts getFonts() {
		return prop.getFonts();
	}

	/**
	 * 取警戒线定义
	 * 
	 * @return ArrayList (ExtAlarmLine)　警戒线定义
	 */
	public ArrayList getAlarmLines() {
		if (alarms == null) {
			AlarmLine[] als = prop.getAlarmLines();
			if (als == null || als.length == 0) {
				return null;
			}
			alarms = new ArrayList();
			for (int i = 0; i < als.length; i++) {
				ExtAlarmLine eal = new ExtAlarmLine();
				eal.setAlarmValue(Double.parseDouble(als[i].getAlarmValue()));
				eal.setColor(als[i].getColor());
				eal.setLineThick(GraphParam.getLineThick(als[i].getLineThick()));
				eal.setLineType(als[i].getLineType());
				eal.setName(als[i].getName());
				eal.setDrawAlarmValue(als[i].isDrawAlarmValue());
				alarms.add(eal);
			}
		}
		return this.alarms;
	}

	public void setAlarmLines(ArrayList alarm) {
		this.alarms = alarm;
	}

	/**
	 * 取图中显示数据定义
	 * 
	 * @return byte　图中显示数据定义，值为DISPDATA_NONE, DISPDATA_VALUE,
	 *         DISPDATA_PERCENTAGE
	 */
	public byte getDisplayData() {
		return prop.getDisplayData();
	}
	public byte getDisplayData2() {
		return prop.getDisplayData2();
	}

	/**
	 * 参考PublicProperty的同名方法
	 * @return
	 */
	public boolean isDispStackSumValue() {
		return prop.isDispStackSumValue();
	}

	/**
	 * 取图中显示数据格式定义
	 * 
	 * @return String　图中显示数据格式定义
	 */
	public String getDisplayDataFormat1() {
		return this.dataFormat1;
	}

	/**
	 * 取二轴图中显示数据格式定义
	 * 
	 * @return String　图中显示数据格式定义
	 */
	public String getDisplayDataFormat2() {
		return this.dataFormat2;
	}

	/**
	 * 取统计图超链接
	 * 
	 * @return String　统计图超链接
	 */
	public String getLink() {
		return this.link;
	}

	/**
	 * 取图例的超链接
	 * @return 链接串
	 */
	public String getLegendLink() {
		return prop.getLegendLink();
	}

	/**
	 * 取统计图超链接目标窗口
	 * 
	 * @return String　统计图超链接目标窗口
	 */
	public String getLinkTarget() {
		return this.linkTarget;
	}

	/**
	 * 取统计图的图例位置
	 * 
	 * @return byte　统计图的图例位置，值为LEGEND_LEFT, LEGEND_RIGHT, LEGEND_TOP,
	 *         LEGEND_BOTTOM, LEGEND_NONE
	 */
	public byte getLegendLocation() {
		return prop.getLegendLocation();
	}

	/**
	 * 参考PublicProperty同名方法
	 * @return
	 */
	public int getLegendVerticalGap() {
		return prop.getLegendVerticalGap();
	}

	/**
	 * 参考PublicProperty同名方法
	 * @return
	 */
	public int getLegendHorizonGap() {
		return prop.getLegendHorizonGap();
	}

	/**
	 * 取统计图的配色方案名
	 * 
	 * @return String　统计图的配色方案名
	 */
	public Palette getPlatte() {
		return this.palette;
	}

	/**
	 * 取统计值起始值
	 * 
	 * @return String　统计值起始值
	 */
	public double getYStartValue1() {
		return this.yStartValue1;
	}

	/**
	 * 取双轴图第二轴的起始值
	 * @return 统计起始值
	 */
	public double getYStartValue2() {
		return this.yStartValue2;
	}

	/**
	 * 取统计值结束值
	 * 
	 * @return String　统计值结束值
	 */
	public double getYEndValue1() {
		return this.yEndValue1;
	}

	/**
	 * 取双轴图第二轴的结束值
	 * @return 结束值
	 */
	public double getYEndValue2() {
		return this.yEndValue2;
	}

	/**
	 * 取统计值标签间隔
	 * 
	 * @return double　统计值标签间隔
	 */
	public double getYInterval1() {
		return this.yInterval1;
	}

	/**
	 * 取双轴图第二轴的标签间隔
	 * @return 标签间隔
	 */
	public double getYInterval2() {
		return this.yInterval2;
	}

	/**
	 * 取统计值数量单位
	 * 
	 * @return double　统计值数量单位，值为UNIT_ORIGIN, UNIT_AUTO, UNIT_THOUSAND,
	 *         UNIT_10THOUSAND, UNIT_MILLION, UNIT_10MILLION, UNIT_100MILLION,
	 *         UNIT_BILLION, UNIT_001, UNIT_0001, UNIT_00001, UNIT_0000001
	 */
	public double getDataUnit() {
		return prop.getDataUnit();
	}

	/**
	 * 取得统计值最少刻度数
	 * 
	 * @param int 统计值最少刻度数
	 */
	public int getYMinMarks() {
		return this.yMinMarks;
	}

	/**
	 * 取标题与图形之间的间距
	 * 
	 * @return double　标题与图形之间的间距
	 */
	public double getTitleMargin() {
		return this.titleMargin;
	}

	/**
	 * 取折线图是否标注数据点
	 * 
	 * @return boolean
	 */
	public boolean isDrawLineDot() {
		return prop.isDrawLineDot();
	}

	/**
	 * 参考PublicProperty同名方法
	 * @return
	 */
	public boolean isOverlapOrigin() {
		return prop.isOverlapOrigin();
	}

	/**
	 * 取折线图是否画趋势线
	 * 
	 * @return boolean
	 */
	public boolean isDrawLineTrend() {
		return prop.isDrawLineTrend();
	}

	/**
	 * 折线图是否忽略空值
	 * 
	 * @return boolean
	 */
	public boolean isIgnoreNull() {
		return prop.ignoreNull();
	}

	/**
	 * 自定义图形类名
	 * 
	 * @return String
	 */
	public String getCustomClass() {
		return prop.getCustomClass();
	}

	/**
	 * 自定义图形参数
	 * 
	 * @return String
	 */
	public String getCustomParam() {
		return prop.getCustomParam();
	}

	/**
	 * 取折线图粗细度
	 * 
	 * @return boolean
	 */
	public byte getLineThick() {
		return prop.getLineThick();
	}

	/**
	 * 参考PublicProperty同名方法
	 * @return
	 */
	public byte getLineStyle() {
		return prop.getLineStyle();
	}

	/**
	 * 取相邻数值或标签重叠时是否显示后一数值或标签
	 * 
	 * @return boolean
	 */
	public boolean isShowOverlapText() {
		return prop.isShowOverlapText();
	}

	/**
	 * 取分类轴标签间隔
	 * 
	 * @return double　分类轴标签间隔
	 */
	public double getXInterval() {
		return this.xInterval;
	}

	/**
	 * 取时间走势图横轴
	 * 
	 * @return ArrayList　(ExtTimeTrendXValue) 时间走势图横轴
	 */
	public ArrayList getTimeTrendXValues() {
		return this.ttXValues;
	}

	/**
	 * 取时序状态图或甘特图状态条宽度
	 * 
	 * @return int　时序状态图或甘特图状态条宽度
	 */
	public int getStatusBarWidth() {
		return this.statusBarWidth;
	}

	/**
	 * 取时序状态图或甘特图时间刻度类型
	 * 
	 * @return byte　时序状态图或甘特图时间刻度类型，值为TIME_YEAR, TIME_MONTH, TIME_DAY,
	 *         TIME_HOUR, TIME_MINUTE, TIME_SECOND
	 */
	public byte getStatusTimeType() {
		return prop.getStatusTimeType();
	}

	/**
	 *  用户是否设置柱形图或条形图间距
	 * @return 设置了属性返回true，否则false
	 */
	public boolean isUserSetBarDistance() {
		return ((userSetStatus & BAR_DISTANCE) == BAR_DISTANCE);
	}

	/**
	 * 用户是否设置用前N条数据画图 
	 * @return 设置了属性返回true，否则false
	 */
	public boolean isUserSetTopData() {
		return ((userSetStatus & TOP_DATA_N) == TOP_DATA_N);
	}

	/**
	 * 用户是否设置统计值起始值
	 * @return 设置了属性返回true，否则false
	 */
	public boolean isUserSetYStartValue1() {
		return ((userSetStatus & Y_START_VALUE1) == Y_START_VALUE1);
	}

	/**
	 * 用户是否设置第二轴的统计值起始值
	 * @return 设置了属性返回true，否则false
	 */
	public boolean isUserSetYStartValue2() {
		return ((userSetStatus & Y_START_VALUE2) == Y_START_VALUE2);
	}

	/**
	 * 用户是否设置统计值结束值
	 * @return 设置了属性返回true，否则false
	 */
	public boolean isUserSetYEndValue1() {
		return ((userSetStatus & Y_END_VALUE1) == Y_END_VALUE1);
	}

	/**
	 * 用户是否设置了第二轴统计值结束值
	 * @return 设置了属性返回true，否则false
	 */
	public boolean isUserSetYEndValue2() {
		return ((userSetStatus & Y_END_VALUE2) == Y_END_VALUE2);
	}

	/**
	 * 用户是否设置统计值标签间隔
	 * @return 设置了属性返回true，否则false
	 */
	public boolean isUserSetYInterval1() {
		return ((userSetStatus & Y_INTERVAL1) == Y_INTERVAL1);
	}

	/**
	 * 用户是否设置了第二轴统计值标签间隔
	 * @return 设置了属性返回true，否则false
	 */
	public boolean isUserSetYInterval2() {
		return ((userSetStatus & Y_INTERVAL2) == Y_INTERVAL2);
	}

	/**
	 * 用户是否设置统计值最少刻度数
	 * @return 设置了属性返回true，否则false
	 */
	public boolean isUserSetYMinMarks() {
		return ((userSetStatus & Y_MIN_MARK) == Y_MIN_MARK);
	}

	/**
	 * 用户是否设置标题与图形之间的间距
	 * @return 设置了属性返回true，否则false
	 */
	public boolean isUserSetTitleMargin() {
		return ((userSetStatus & TITLE_MARGIN) == TITLE_MARGIN);
	}

	/**
	 * 用户是否设置分类轴标签间隔
	 * @return 设置了属性返回true，否则false
	 */
	public boolean isUserSetXInterval() {
		return ((userSetStatus & X_INTERVAL) == X_INTERVAL);
	}

	/**
	 * 用户是否设置时序状态图或甘特图状态条宽度
	 * @return 设置了属性返回true，否则false
	 */
	public boolean isUserSetStatusBarWidth() {
		return ((userSetStatus & STATUS_BAR_WIDTH) == STATUS_BAR_WIDTH);
	}

	/**
	 * 是否绘制阴影
	 * @return 绘制阴影返回true，否则返回false
	 */
	public boolean isDrawShade() {
		if (isStackedGraph(null)) {
			return false;
		}
		if (graphType == GraphTypes.GT_COL3D)
			return false;
		// 柱子在平台中央时也不绘制阴影
		// 堆积图画阴影有重叠，不好画
		return prop.isDrawShade();
	}

	/**
	 * 参考PublicProperty同名方法
	 * @return
	 */
	public boolean isRaisedBorder() {
		return prop.isRaisedBorder();
	}

	/**
	 * 参考PublicProperty同名方法
	 * @return
	 */
	public boolean getFlag(byte key) {
		return prop.getFlag(key);
	}

	/**
	 * 取背景图配置
	 * @return 配置对象
	 */
	public BackGraphConfig getBackGraphConfig() {
		return bgc;
	}

	/**
	 * 设置背景图配置
	 * @param bgc 配置对象
	 */
	public void setBackGraphConfig(BackGraphConfig bgc) {
		this.bgc = bgc;
	}

	/**
	 * 图形的版本到了7时，允许图形矩形的4边分别设置不同的颜色 此时原来的AxisColor则作废，顺序依次为上下左右 AXIS_ID
	 * 为GraphProperty中定义的AXIS_开头的常量
	 * 
	 * @return Color, 如果为Null则表示为透明色
	 */
	public Color getAxisColor(int AXIS_ID) {
		int c = prop.getAxisColors()[AXIS_ID];
		if (c == 16777215) {
			return null;
		} else {
			return new Color(c);
		}
	}

	/**
	 * 设置统计图类型
	 * 
	 * @param type
	 *            统计图类型，由GraphTypes中的常量定义
	 */
	public void setType(byte type) {
		graphType = type;
	}

	/**
	 * 设置坐标轴颜色
	 * 
	 * @param color
	 *            坐标轴颜色
	 */
	public void setAxisColor(int color) {
		prop.setAxisColor(color);
	}

	/**
	 * 设置全图背景颜色
	 * 
	 * @param color
	 *            　全图背景颜色
	 */
	public void setCanvasColor(int color) {
		prop.setCanvasColor(color);
	}

	/**
	 * 设置图形区背景颜色
	 * 
	 * @param color
	 *            　图形区背景颜色
	 */
	public void setGraphBackColor(int color) {
		prop.setGraphBackColor(color);
	}

	/**
	 * 设置统计图分类
	 * 
	 * @param categorys
	 *            　(ExtGraphProperty ) 统计图分类
	 */
	public void setCategories(ArrayList categorys) {
		this.categories = categorys;
	}

	/**
	 * 设置横轴标题
	 * 
	 * @param title
	 *            横轴标题
	 */
	public void setXTitle(String title) {
		this.xTitle = title;
	}

	/**
	 * 设置纵轴标题
	 * 
	 * @param title
	 *            纵轴标题
	 */
	public void setYTitle(String title) {
		this.yTitle = title;
	}

	/**
	 * 设置统计图标题
	 * 
	 * @return title　统计图标题
	 */
	public void setGraphTitle(String title) {
		this.graphTitle = title;
	}

	/**
	 * 设置网格线类型
	 * 
	 * @param type
	 *            　网格线类型，值为LINE_NONE, LINE_SOLID, LINE_LONG_DASH,
	 *            LINE_SHORT_DASH, LINE_DOT_DASH, LINE_2DOT_DASH
	 */
	public void setGridLineType(byte type) {
		prop.setGridLineType(type);
	}

	/**
	 * 设置网格线颜色
	 * 
	 * @param color
	 *            　网格线颜色
	 */
	public void setGridLineColor(int color) {
		prop.setGridLineColor(color);
	}

	/**
	 * 设置柱形图或条形图间距
	 * 
	 * @param distance
	 *            　柱形图或条形图间距
	 */
	public void setBarDistance(double distance) {
		userSetStatus |= BAR_DISTANCE;
		this.barDistance = distance;
	}

	/**
	 * 设置图形格式
	 * 
	 * @param format
	 *            　图形格式, 值为IMAGE_JPG, IMAGE_GIF, IMAGE_PNG
	 */
	public void setImageFormat(byte format) {
		prop.setImageFormat(format);
	}

	/**
	 * 设置图形是否透明
	 * 
	 * @param b
	 */
	public void setGraphTransparent(boolean b) {
		prop.setGraphTransparent(b);
	}

	/**
	 * 设置是否渐变色
	 * 
	 * @param b
	 */
	public void setGradientColor(boolean b) {
		prop.setGradientColor(b);
	}

	/**
	 * 设置用前N条数据画图
	 * 
	 * @param n
	 *            　前N条数据
	 */
	public void setTopData(int n) {
		userSetStatus |= TOP_DATA_N;
		this.topN = n;
	}

	/**
	 * 设置统计图字体
	 * 
	 * @param font
	 *            　统计图字体
	 */
	public void setFonts(GraphFonts font) {
		prop.setFonts(font);
	}

	/**
	 * 设置图中显示数据
	 * 
	 * @param data
	 *            　图中显示数据，值为DISPDATA_NONE, DISPDATA_VALUE, DISPDATA_PERCENTAGE
	 */
	public void setDisplayData(byte data) {
		prop.setDisplayData(data);
	}
	public void setDisplayData2(byte data) {
		prop.setDisplayData2(data);
	}

	/**
	 * 设置图中显示数据格式
	 * 
	 * @param format
	 *            　图中显示数据格式
	 */
	public void setDisplayDataFormat1(String format) {
		this.dataFormat1 = format;
	}

	public void setDisplayDataFormat2(String format) {
		this.dataFormat2 = format;
	}

	/**
	 * 设置统计图超链接
	 * 
	 * @param link
	 *            　统计图超链接
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * 设置统计图超链接目标窗口
	 * 
	 * @param target
	 *            　统计图超链接目标窗口
	 */
	public void setLinkTarget(String target) {
		this.linkTarget = target;
	}

	/**
	 * 设置统计图的图例位置
	 * 
	 * @param location
	 *            　统计图的图例位置，值为LEGEND_LEFT, LEGEND_RIGHT, LEGEND_TOP,
	 *            LEGEND_BOTTOM, LEGEND_NONE
	 */
	public void setLegendLocation(byte location) {
		prop.setLegendLocation(location);
	}

	/**
	 * 设置统计图的配色方案名
	 * 
	 * @param config
	 *            　统计图的配色方案名
	 */
	public void setPalette(Palette palette) {
		this.palette = palette;
	}

	/**
	 * 设置统计值起始值
	 * 
	 * @param value
	 *            　统计值起始值
	 */
	public void setYStartValue1(double value) {
		userSetStatus |= Y_START_VALUE1;
		this.yStartValue1 = value;
	}

	public void setYStartValue2(double value) {
		userSetStatus |= Y_START_VALUE2;
		this.yStartValue2 = value;
	}

	/**
	 * 设置统计值结束值
	 * 
	 * @param value
	 *            　统计值结束值
	 */
	public void setYEndValue1(double value) {
		userSetStatus |= Y_END_VALUE1;
		this.yEndValue1 = value;
	}

	public void setYEndValue2(double value) {
		userSetStatus |= Y_END_VALUE2;
		this.yEndValue2 = value;
	}

	/**
	 * 设置统计值标签间隔
	 * 
	 * @param interval
	 *            　统计值标签间隔
	 */
	public void setYInterval1(double interval) {
		userSetStatus |= Y_INTERVAL1;
		this.yInterval1 = interval;
	}

	public void setYInterval2(double interval) {
		userSetStatus |= Y_INTERVAL2;
		this.yInterval2 = interval;
	}

	/**
	 * 设置统计值数量单位
	 * 
	 * @param unit
	 *            　统计值数量单位，值为UNIT_ORIGIN, UNIT_AUTO, UNIT_THOUSAND,
	 *            UNIT_10THOUSAND, UNIT_MILLION, UNIT_10MILLION,
	 *            UNIT_100MILLION, UNIT_BILLION, UNIT_001, UNIT_0001,
	 *            UNIT_00001, UNIT_0000001
	 */
	public void setDataUnit(double unit) {
		prop.setDataUnit(unit);
	}

	/**
	 * 设置统计值最少刻度数
	 * 
	 * @param mark
	 *            统计值最少刻度数
	 */
	public void setYMinMarks(int mark) {
		userSetStatus |= Y_MIN_MARK;
		this.yMinMarks = mark;
	}

	/**
	 * 设置标题与图形之间的间距
	 * 
	 * @param margin
	 *            　标题与图形之间的间距
	 */
	public void setTitleMargin(double margin) {
		userSetStatus |= TITLE_MARGIN;
		this.titleMargin = margin;
	}

	/**
	 * 设置折线图是否标注数据点
	 * 
	 * @param b
	 */
	public void setDrawLineDot(boolean b) {
		prop.setDrawLineDot(b);
	}

	/**
	 * 设置相邻数值或标签重叠时是否显示后一数值或标签
	 * 
	 * @param b
	 */
	public void setShowOverlapText(boolean b) {
		prop.setShowOverlapText(b);
	}

	/**
	 * 设置分类轴标签间隔
	 * 
	 * @param interval
	 *            　分类轴标签间隔
	 */
	public void setXInterval(double interval) {
		userSetStatus |= X_INTERVAL;
		this.xInterval = interval;
	}

	/**
	 * 设置时间走势图横轴
	 * 
	 * @param value
	 *            (TimeTrendXValue )　时间走势图横轴
	 */
	public void setTimeTrendXValues(ArrayList value) {
		this.ttXValues = value;
	}

	/**
	 * 设置时序状态图或甘特图状态条宽度
	 * 
	 * @param width
	 *            时序状态图或甘特图状态条宽度
	 */
	public void setStatusBarWidth(int width) {
		userSetStatus |= STATUS_BAR_WIDTH;
		this.statusBarWidth = width;
	}

	/**
	 * 设置时序状态图或甘特图时间刻度类型
	 * 
	 * @param type
	 *            时序状态图或甘特图时间刻度类型，取值为TIME_YEAR, TIME_MONTH, TIME_DAY, TIME_HOUR,
	 *            TIME_MINUTE, TIME_SECOND
	 */
	public void setStatusTimeType(byte type) {
		prop.setStatusTimeType(type);
	}

	public double getStackedMaxValue() {
		if (categories == null) {
			return 0;
		}
		double val = 0;
		for (int i = 0; i < categories.size(); i++) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);
			double stackedVal = egc.getPositiveSumSeries();
			if (stackedVal > val) {
				val = stackedVal;
			}
		}
		if (is2YGraph()) {// 双轴图时，仅有左轴堆积，所以只计算分类1
			return val;
		}
		if (category2 != null) {
			for (int i = 0; i < category2.size(); i++) {
				ExtGraphCategory egc = (ExtGraphCategory) category2.get(i);
				double stackedVal = egc.getPositiveSumSeries();
				if (stackedVal > val) {
					val = stackedVal;
				}
			}
		}
		return val;
	}

	public double getStackedMinValue() {
		if (categories == null) {
			return 0;
		}
		double val = 0;
		for (int i = 0; i < categories.size(); i++) {
			ExtGraphCategory egc = (ExtGraphCategory) categories.get(i);
			double stackedVal = egc.getNegativeSumSeries();
			if (stackedVal < val) {
				val = stackedVal;
			}
		}
		if (category2 != null) {
			for (int i = 0; i < category2.size(); i++) {
				ExtGraphCategory egc = (ExtGraphCategory) category2.get(i);
				double stackedVal = egc.getNegativeSumSeries();
				if (stackedVal < val) {
					val = stackedVal;
				}
			}
		}
		return val;
	}

	/**
	 * 找出分类定义中的最大值
	 * @param cats 分类
	 * @return 最大值
	 */
	public double getMaxValue(ArrayList cats) {
		return getTerminalValue(true, cats);
	}

	/**
	 * 找出分类定义中的最小值
	 * @param cats 分类
	 * @return 最小值
	 */
	public double getMinValue(ArrayList cats) {
		return getTerminalValue(false, cats);
	}

	/**
	 * 将other设置为其他系列
	 * @param other 系列名称
	 */
	public void setOtherStackedSeries(String other) {
		prop.setOtherStackedSeries(other);
	}
/**
 * 获取其他系列
 * @return 其他
 */
	public String getOtherStackedSeries() {
		return prop.getOtherStackedSeries();
	}

	private double getAlarmTerminal(boolean getMax) {
		if (alarms == null)
			return 0;
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < alarms.size(); i++) {
			ExtAlarmLine eal = (ExtAlarmLine) alarms.get(i);
			double d = eal.getAlarmValue();
			if (getMax) {
				max = Math.max(max, d);
			} else {
				min = Math.min(min, d);
			}
		}
		if (getMax) {
			return max;
		} else {
			return min;
		}
	}

	private double getTerminalValue(boolean getMax, ArrayList cats) {
		double val = 0;
		if (cats != null) {
			for (int i = 0; i < cats.size(); i++) {
				ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
				ArrayList series = egc.getSeries();
				for (int j = 0; j < series.size(); j++) {
					// if (! (series.get(j) instanceof ExtGraphSery)) {
					if (!_instanceof(series.get(j), "ExtGraphSery")) {
						break;
						// return 0;
					}
					ExtGraphSery egs = (ExtGraphSery) series.get(j);
					if (getMax) {
						if (egs.getValue() > val) {
							val = egs.getValue();
						}
					} else {
						if (egs.getValue() < val) {
							val = egs.getValue();
						}
					}
				}
			}
		}
		double tVal = getAlarmTerminal(getMax);
		if (getMax) {
			return Math.max(tVal, val);
		} else {
			return Math.min(tVal, val);
		}
	}

	/** 横轴标题 */
	private String xTitle;

	/** 纵轴标题 */
	private String yTitle;

	/** 统计图标题 */
	private String graphTitle;

	/** 网格线类型 */
	/** 柱形图或条形图间距 */
	private double barDistance = 0.0;

	/** 用前N条数据画图 */
	private int topN = 0; //

	/** 警戒线定义 */
	private ArrayList alarms = null;

	/** 统计图超链接 */
	private String link;

	/** 统计图超链接目标窗口 */
	private String linkTarget;

	/** 统计图的配色方案名 */
	private Palette palette;

	/** 标题与图形之间的间距 */
	private double titleMargin;

	/** 时间走势图横轴取值 */
	private ArrayList ttXValues;

	/** 时序状态图或甘特图状态条宽度 */
	private int statusBarWidth;

	/** 柱形图或条形图间距 */
	private static final short BAR_DISTANCE = (short) 0x01;

	/** 用前N条数据画图 */
	private static final short TOP_DATA_N = (short) 0x02;

	/** 统计值起始值 */
	private static final short Y_START_VALUE1 = (short) 0x04;
	private static final short Y_START_VALUE2 = (short) 0x08;

	/** 统计值结束值 */
	private static final short Y_END_VALUE1 = (short) 0x10;
	private static final short Y_END_VALUE2 = (short) 0x20;

	/** 统计值标签间隔 */
	private static final short Y_INTERVAL1 = (short) 0x40;
	private static final short Y_INTERVAL2 = (short) 0x80;

	/** 统计值最少刻度数 */
	private static final short Y_MIN_MARK = (short) 0x100;

	/** 标题与图形之间的间距 */
	private static final short TITLE_MARGIN = (short) 0x200;

	/** 分类轴标签间隔 */
	private static final short X_INTERVAL = (short) 0x400;

	/** 时序状态图或甘特图状态条宽度 */
	private static final short STATUS_BAR_WIDTH = (short) 0x800;

	private short userSetStatus = 0; // 用户是否设置该属性

	private double yStartValue1 = 0;
	/** 统计值起始值 */
	private double yStartValue2 = 0;
	private double yEndValue1 = 0;
	/** 统计值结束值 */
	private double yEndValue2 = 0;
	private double yInterval1 = 0;
	/** 统计值标签间隔 */
	private double yInterval2 = 0;
	private int yMinMarks = 0;
	/** 统计值最少刻度数 */
	private double xInterval = 0;
	/** 分类轴标签间隔 */
	private String dataFormat1;
	/** 图中显示数据格式定义 */
	private String dataFormat2;
	/** 图中显示数据格式定义 */
	private byte graphType;
	private IGraphProperty prop;
}
