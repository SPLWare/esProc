package com.scudata.cellset.graph;

import java.io.*;
import java.awt.*;

import com.scudata.cellset.*;
import com.scudata.cellset.graph.config.*;
import com.scudata.common.*;

/**
 * 
 * 所有图形的公共属性
 * 
 * @author Joancy
 *
 */
public class PublicProperty implements IGraphProperty, ICloneable,
		Externalizable, IRecord {
//	图形颜色16777215为透明，编辑的透明色为null对象；所以所有设置接口都改为Color入口
	/** 统计图类型 */
	private byte type = GraphTypes.GT_COL;
	private byte curveType = CURVE_LAGRANGE;
	private byte borderStyle = IStyle.LINE_SOLID;
	private float borderWidth = 0.75f;
	private int borderColor = Color.black.getRGB();
	private boolean borderShadow = true;

	/** 坐标轴颜色 */
	private int axisColor = Color.black.getRGB();

	/** 全图背景颜色 */
	private int canvasColor = Color.white.getRGB();

	/** 图形区背景颜色 */
	private int graphBackColor = Color.white.getRGB();

	/** 横轴标题 */
	private String xTitle;

	/** 纵轴标题 */
	private String yTitle;

	/** 统计图标题 */
	private String graphTitle;

	/** 网格线位置*/
	private byte gridLineLocation = GRID_VALUE;
	/** 网格线类型 */
	private byte gridLineType = LINE_NONE;

	/** 网格线颜色 */
	private int gridLineColor = Color.lightGray.getRGB();

	/** 柱形图或条形图间距 */
	private int barDistance;

	/** 图形格式 */
	private byte imageFormat = IMAGE_JPG;

	/** 图形是否透明 */
	private boolean graphTransparent = false;

	/** 绘制数据表 */
	private boolean isDrawDataTable = false,isDataCenter=false;
	
	/** 是否渐变色,注意该属性与raisedBorder是互斥的 */
	private boolean gradientColor = true;

	/** 用前N条数据画图 */
	private int topData;

	/** 时序状态图、甘特图或里程碑图开始时间表达式 */
	private String statusStartTimeExp;

	/** 时序状态图、甘特图或里程碑图结束时间表达式 */
	private String statusEndTimeExp;

	/** 时序状态图分类表达式或甘特图和里程碑图的项目表达式 */
	private String statusCategoryExp;

	/** 时序状态图或甘特图状态表达式 */
	private String statusStateExp;

	/** 时序状态图或甘特图状态条宽度 */
	private String statusBarWidth;

	/** 时序状态图或甘特图时间刻度类型 */
	private byte statusTimeType = TIME_HOUR;

	/** 统计图中的字体 */
	private GraphFonts fonts = new GraphFonts();

	/** 警戒线定义 */
	private AlarmLine[] alarms;

	/** 图中显示数据定义 */
	private byte displayData = DISPDATA_NONE;
	private byte displayData2 = DISPDATA_NONE;

	/** 图中显示数据格式定义 */
	private String displayDataFormat;

	/** 统计图超链接 */
	private String link;

	/** 统计图图例超链接 2009.5.13 xq add */
	private String legendLink;

	/** 统计图超链接目标窗口 */
	private String linkTarget;

	/** 统计图的图例位置 */
	private byte legendLocation = LEGEND_NONE;

	/** 统计图的图例纵向间距 */
	private int legendVerticalGap = 4;
	/** 统计图的图例横向间距 */
	private int legendHorizonGap = 4;

	/** 总按系列画图例 */
	private boolean drawLegendBySery = false;

	/** 统计图的配色方案名 */
	private String colorConfig = "";

	/** 统计值起始值 */
	private String yStartValue;

	/** 统计值结束值 */
	private String yEndValue;

	/** 统计值标签间隔 */
	private String yInterval;

	/** 统计值数量单位 */
	private double dataUnit = UNIT_ORIGIN;

	/** 统计值最少刻度数 */
	private int yMinMarks = 2;

	/** 标题与图形之间的间距 */
	private int titleMargin = 20;

	/** 折线图是否标注数据点 */
	private boolean drawLineDot = true;
	private boolean isOverlapOrigin = false;

	/** 折线图是否画趋势线 */
	private boolean drawLineTrend = false;

	/** 折线图直线粗细度 */
	private byte lineThick = 1;
	private byte lineStyle = LINE_SOLID;

	/** 相邻数值或标签重叠时是否显示后一数值或标签 */
	private boolean showOverlapText = true;

	/** 饼图中是否分离出一扇显示 */
	private boolean pieSpacing = false;
	private boolean isMeterColorEnd = true;
	private boolean isMeterTick = false;
	private int meter3DEdge = 8;
	private int meterRainbowEdge = 16;
	private int pieLine = 8;

	private boolean isDispStackSumValue = false;
	/** 分类轴标签间隔 */
	private int xInterval;

	/** 折线图是否忽略空值 只对于折线图有效 */
	private boolean ignoreNull;

	/** 自定义图形类名 */
	private String customClass;

	/** 自定义图形相关参数 */
	private String customParam;

	/** 平面折线图,平面饼图，平面柱图是否画阴影 */
	private boolean drawShade = true;

	/** 平面柱图的突出边框 */
	private boolean raisedBorder = false;

	/** 图形的背景图 */
	private BackGraphConfig backGraph = null;

	/** topN 时是否丢掉other数据 */
	private long flag = 0;

	/** 轴线颜色以及图例边框颜色，可扩充,次序可以引用本类定义的常量 AXIS_xxx */
	/** 颜色定义，代替原来轴颜色，轴矩形分开为四根线的颜色，顺序依次为上，下，左，右，图例 */
	private int[] axisColors = new int[20];

	private int leftMargin = 10; // 左边距
	private int rightMargin = 10; /* 右边距 */
	private int topMargin = 10; /* 上边距 */
	private int bottomMargin = 10; /* 下边距 */
	private int tickLen = 4; /* 刻度长度 */
	private int coorWidth = 100; /* 3D轴宽度占序列宽度的百分比 */
	private double categorySpan = 190; /* 类别间的间隔占序列宽度的百分比 */
	private int seriesSpan = 100; /* 序列间的间隔占序列深度的百分比 */
	private int pieRotation = 50; /* 纵轴占横轴的长度百分比 */
	private int pieHeight = 70; /* 饼型图的高度占半径的百分比<=100 */

	private String otherStackedSeries=null;
	
	/**
	 * 缺省值构造函数
	 */
	public PublicProperty() {
		/* Init Property 注意该初始化动作同步一下 ReportGraphProperty*/
		for (int i = 0; i < axisColors.length; i++) {
			axisColors[i] = 16777215;//透明色
		}
		axisColors[this.AXIS_BOTTOM]=Color.lightGray.getRGB();
		type = GraphTypes.GT_COL3DOBJ;
		graphBackColor = new Color(187,192,181).getRGB();
		this.gridLineType = LINE_SOLID;
		this.gridLineColor = new Color(172,187,153).getRGB();
		this.gradientColor = true;
		this.imageFormat = IMAGE_SVG;
		this.pieSpacing = false;
	}

	/**
	 * 取统计图类型
	 * 
	 * @return byte 统计图类型，由GraphTypes中的常量定义
	 */
	public byte getType() {
		return type;
	}

	/**
	 * 设置统计图类型
	 * 
	 * @param type 统计图类型，由GraphTypes中的常量定义
	 */
	public void setType(byte type) {
		this.type = type;
	}

	/**
	 * 设置曲线类型
	 * @param curveType 类型值，参考	IGraphProperty.CURVE_XXX
	 */
	public void setCurveType(byte curveType) {
		this.curveType = curveType;
	}

	/**
	 * 获取曲线类型
	 * @return 曲线的类型
	 */
	public byte getCurveType() {
		return curveType;
	}

	/**
	 * 取自定义图形类名称
	 * 
	 * @return String 自定义图类名称
	 */
	public String getCustomClass() {
		return customClass;
	}

	/**
	 * 设置自定义图形类名称
	 */
	public void setCustomClass(String customClass) {
		this.customClass = customClass;
	}

	/**
	 * 取自定义图形类参数
	 * 
	 * @return String 自定义图类参数
	 */
	public String getCustomParam() {
		return customParam;
	}

	/**
	 * 设置自定义图形类参数
	 */
	public void setCustomParam(String customParam) {
		this.customParam = customParam;
	}

	/**
	 * 取坐标轴颜色
	 * 
	 * @return int RGB值表示的颜色
	 */
	public int getAxisColor() {
		return axisColor;
	}

	/**
	 * 设置坐标轴颜色
	 * 该方法已经废弃不用，由数组axisColors
	 * @param color 坐标轴颜色
	 */
	public void setAxisColor(int color) {
		this.axisColor = color;
	}

	/**
	 * 设置指定序号轴的颜色
	 * @param index 序号
	 * @param c 颜色
	 */
	public void setAxisColor(int index, Color c) {
		this.axisColors[index] = color(c);
	}

	private int color(Color c){
		if( c==null ) return 16777215;
		return c.getRGB();
	}
	
	/**
	 * 设置边框的属性
	 * @param borderStyle 边框风格
	 * @param borderWidth 边框粗度
	 * @param borderColor 边框颜色
	 * @param borderShadow 边框阴影
	 */
	public void setBorder(byte borderStyle, float borderWidth, Color borderColor,
			boolean borderShadow) {
		this.borderStyle = borderStyle;
		this.borderWidth = borderWidth;
		this.borderColor = color(borderColor);
		this.borderShadow = borderShadow;
	}

	/**
	 * 去边框风格
	 * @return 风格值
	 */
	public byte getBorderStyle() {
		return borderStyle;
	}

	/**
	 * 取边框粗度
	 * @return 粗度值
	 */
	public float getBorderWidth() {
		return borderWidth;
	}

	/**
	 * 取RGB值表示的颜色对应绘图用的Color
	 * 颜色值为16777215时表示透明色，返回null
	 * @param c RGB表示的颜色值
	 * @return 颜色对象
	 */
	public static Color getColorObject(int c){
		if(c==16777215) return null;
		return new Color(c);
	}
	
	/**
	 * 取边框颜色
	 * @return RGB表示的整数颜色
	 */
	public int getBorderColor() {
		return borderColor;
	}

	/**
	 * 边框是否绘制阴影
	 * @return 有阴影返回true，否则返回false
	 */
	public boolean getBorderShadow() {
		return borderShadow;
	}

	/**
	 * 取全图背景颜色
	 * 
	 * @return int　全图背景颜色
	 */
	public int getCanvasColor() {
		return canvasColor;
	}

	/**
	 * 设置全图背景颜色
	 * 
	 * @param color
	 *            全图背景颜色
	 */
	public void setCanvasColor(int color) {
		this.canvasColor = color;
	}
	
	/**
	 * 设置全图背景色
	 * @param c 颜色对象
	 */
	public void setCanvasColor(Color c) {
		this.canvasColor = color(c);
	}

	/**
	 * 取图形区背景颜色
	 * 
	 * @return int　图形区背景颜色
	 */
	public int getGraphBackColor() {
		return this.graphBackColor;
	}

	/**
	 * 设置图形区背景颜色
	 * 
	 * @param color 图形区背景颜色
	 */
	public void setGraphBackColor(int color) {
		this.graphBackColor = color;
	}
	/**
	 * 设置图形区背景颜色
	 * @param c 颜色对象
	 */
	public void setGraphBackColor(Color c) {
		this.graphBackColor = color(c);
	}

	/**
	 * 取横轴标题
	 * 
	 * @return String 横轴标题
	 */
	public String getXTitle() {
		return xTitle;
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
	 * 取纵轴标题
	 * 
	 * @return String 纵轴标题
	 */
	public String getYTitle() {
		return yTitle;
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
	 * 取统计图标题
	 * 
	 * @return String　统计图标题
	 */
	public String getGraphTitle() {
		return graphTitle;
	}

	/**
	 * 设置统计图标题
	 * 
	 * @param title
	 *            统计图标题
	 */
	public void setGraphTitle(String title) {
		this.graphTitle = title;
	}

	/**
	 * 取网格线类型
	 * 
	 * @return byte　网格线类型，值为IGraphProperty.LINE_NONE, LINE_SOLID, LINE_LONG_DASH,
	 *         LINE_SHORT_DASH, LINE_DOT_DASH, LINE_2DOT_DASH
	 */
	public byte getGridLineType() {
		return gridLineType;
	}

	/**
	 * 设置网格线类型
	 * 
	 * @param type
	 *            网格线类型, 取值为LINE_NONE, LINE_SOLID, LINE_LONG_DASH,
	 *            LINE_SHORT_DASH, LINE_DOT_DASH, LINE_2DOT_DASH
	 */
	public void setGridLineType(byte type) {
		this.gridLineType = type;
	}

	/**
	 * 取网格线颜色
	 * 
	 * @return int　网格线颜色
	 */
	public int getGridLineColor() {
		return gridLineColor;
	}

	/**
	 * 设置网格线颜色
	 * 
	 * @param color
	 *            网格线颜色
	 */
	public void setGridLineColor(int color) {
		this.gridLineColor = color;
	}

	/**
	 * 取柱形图或条形图间距
	 * 
	 * @return String　柱形图或条形图间距
	 */
	public int getBarDistance() {
		return barDistance;
	}

	/**
	 * 设置柱形图或条形图间距
	 * 
	 * @param distance
	 *            柱形图或条形图间距
	 */
	public void setBarDistance(int distance) {
		this.barDistance = distance;
	}

	/**
	 * 取图形格式
	 * 
	 * @return byte　图形格式, 值为IMAGE_JPG, IMAGE_GIF, IMAGE_PNG
	 */
	public byte getImageFormat() {
		return imageFormat;
	}

	/**
	 * 设置图形格式
	 * 
	 * @param format
	 *            图形格式，取值为IMAGE_JPG, IMAGE_GIF, IMAGE_PNG
	 */
	public void setImageFormat(byte format) {
		this.imageFormat = format;
	}

	/**
	 * 取图形是否透明
	 * 
	 * @return boolean
	 */
	public boolean isGraphTransparent() {
		return graphTransparent;
	}

	/**
	 * 是否在图形下方绘制数据表
	 * @return 绘制时返回true，否则返回false
	 */
	public boolean isDrawDataTable(){
		return isDrawDataTable;
	}
	
	/**
	 * 设置是否绘制数据表
	 * @param b 是否绘制
	 */
	public void setDrawDataTable(boolean b){
		this.isDrawDataTable = b;
	}
	/**
	 * 如果绘制数据表，数据在格子中间是否居中
	 * @return 如果居中绘制返回true，否则返回false
	 */
	public boolean isDataCenter(){
		return isDataCenter;
	}
	/**
	 * 设置数据表的数据是否居中绘制
	 * 如果不居中的话，就是左对齐
	 * @param b 居中显示
	 */
	public void setDataCenter(boolean b){
		this.isDataCenter = b;
	}
	/**
	 * 设置图形是否透明
	 * 
	 * @param b 是否透明
	 */
	public void setGraphTransparent(boolean b) {
		this.graphTransparent = b;
	}

	/**
	 * 取是否渐变色
	 * 
	 * @return boolean 是否渐变色
	 */
	public boolean isGradientColor() {
		return gradientColor;
	}

	/**
	 * 设置是否渐变色
	 * 
	 * @param b 是否使用渐变色
	 */
	public void setGradientColor(boolean b) {
		this.gradientColor = b;
	}

	/**
	 * 取用前N条数据画图
	 * 
	 * @return int　用前N条数据画图
	 */
	public int getTopData() {
		return topData;
	}

	/**
	 * 设置用前N条数据画图
	 * 
	 * @param n  数据条数
	 */
	public void setTopData(int n) {
		this.topData = n;
	}

	/**
	 * 取时序状态图、甘特图或里程碑图开始时间表达式
	 * 
	 * @return String　时序状态图、甘特图或里程碑图开始时间表达式
	 */
	public String getStatusStartTimeExp() {
		return statusStartTimeExp;
	}

	/**
	 * 设置时序状态图、甘特图或里程碑图开始时间表达式
	 * 
	 * @param exp 时序状态图、甘特图或里程碑图开始时间表达式
	 */
	public void setStatusStartTimeExp(String exp) {
		this.statusStartTimeExp = exp;
	}

	/**
	 * 取时序状态图、甘特图或里程碑图结束时间表达式
	 * 
	 * @return String　时序状态图、甘特图或里程碑图结束时间表达式
	 */
	public String getStatusEndTimeExp() {
		return statusEndTimeExp;
	}

	/**
	 * 设置时序状态图、甘特图或里程碑图结束时间表达式
	 * 
	 * @param exp 时序状态图、甘特图或里程碑图结束时间表达式
	 */
	public void setStatusEndTimeExp(String exp) {
		this.statusEndTimeExp = exp;
	}

	/**
	 * 取时序状态图分类表达式或甘特图和里程碑图的项目表达式
	 * 
	 * @return String　时序状态图分类表达式或甘特图和里程碑图的项目表达式
	 */
	public String getStatusCategoryExp() {
		return statusCategoryExp;
	}

	/**
	 * 设置时序状态图分类表达式或甘特图和里程碑图的项目表达式
	 * 
	 * @param exp 时序状态图分类表达式或甘特图和里程碑图的项目表达式
	 */
	public void setStatusCategoryExp(String exp) {
		this.statusCategoryExp = exp;
	}

	/**
	 * 取时序状态图或甘特图状态表达式
	 * 
	 * @return String　时序状态图或甘特图状态表达式
	 */
	public String getStatusStateExp() {
		return statusStateExp;
	}

	/**
	 * 设置时序状态图或甘特图状态表达式
	 * 
	 * @param exp 时序状态图或甘特图状态表达式
	 */
	public void setStatusStateExp(String exp) {
		this.statusStateExp = exp;
	}

	/**
	 * 取时序状态图或甘特图状态条宽度
	 * 
	 * @return String　时序状态图或甘特图状态条宽度
	 */
	public String getStatusBarWidth() {
		return statusBarWidth;
	}

	/**
	 * 设置时序状态图或甘特图状态条宽度
	 * 
	 * @param width 时序状态图或甘特图状态条宽度
	 */
	public void setStatusBarWidth(String width) {
		this.statusBarWidth = width;
	}

	/**
	 * 取时序状态图或甘特图时间刻度类型
	 * 
	 * @return byte　时序状态图或甘特图时间刻度类型，值为TIME_YEAR, TIME_MONTH, TIME_DAY,
	 *         TIME_HOUR, TIME_MINUTE, TIME_SECOND
	 */
	public byte getStatusTimeType() {
		return statusTimeType;
	}

	/**
	 * 设置时序状态图或甘特图时间刻度类型
	 * 
	 * @param type
	 *            时序状态图或甘特图时间刻度类型，取值为TIME_YEAR, TIME_MONTH, TIME_DAY, TIME_HOUR,
	 *            TIME_MINUTE, TIME_SECOND
	 */
	public void setStatusTimeType(byte type) {
		this.statusTimeType = type;
	}

	/**
	 * 取统计图字体
	 * 
	 * @return GraphFonts　统计图字体
	 */
	public GraphFonts getFonts() {
		return fonts;
	}

	/**
	 * 设置统计图字体
	 * 
	 * @param fonts 统计图字体
	 */
	public void setFonts(GraphFonts fonts) {
		this.fonts = fonts;
	}

	/**
	 * 取警戒线定义
	 * 
	 * @return AlarmLine[]　警戒线定义
	 */
	public AlarmLine[] getAlarmLines() {
		return alarms;
	}

	/**
	 * 设置警戒线定义
	 * 
	 * @param alarms
	 *            警戒线定义
	 */
	public void setAlarmLines(AlarmLine[] alarms) {
		this.alarms = alarms;
	}

	/**
	 * 取图中显示数据定义
	 * 
	 * @return byte　图中显示数据定义，值为DISPDATA_NONE, DISPDATA_VALUE,
	 *         DISPDATA_PERCENTAGE
	 */
	public byte getDisplayData() {
		return displayData;
	}
	public byte getDisplayData2() {
		return displayData2;
	}

	/**
	 * 设置图中显示数据定义
	 * 
	 * @param displayData
	 *            图中显示数据定义，取值为DISPDATA_NONE, DISPDATA_VALUE, DISPDATA_PERCENTAGE
	 */
	public void setDisplayData(byte displayData) {
		this.displayData = displayData;
	}
	public void setDisplayData2(byte displayData) {
		this.displayData2 = displayData;
	}

	/**
	 * 取图中显示数据格式定义
	 * 
	 * @return String　图中显示数据格式定义
	 */
	public String getDisplayDataFormat() {
		return displayDataFormat;
	}

	/**
	 * 设置图中显示数据格式定义
	 * 
	 * @param format
	 *            图中显示数据格式定义，双轴图时用分号隔开
	 */
	public void setDisplayDataFormat(String format) {
		this.displayDataFormat = format;
	}

	/**
	 * 取统计图超链接
	 * 
	 * @return String　统计图超链接
	 */
	public String getLink() {
		return link;
	}

	/**
	 * 取统计图中的图例超链接
	 * @return String 图例超链接
	 */
	public String getLegendLink() {
		return legendLink;
	}

	/**
	 * 设置统计图超链接
	 * 
	 * @param link 统计图超链接
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * 设置图例超链接
	 * @param link 图例超链接
	 */
	public void setLegendLink(String link) {
		this.legendLink = link;
	}

	/**
	 * 取统计图超链接目标窗口
	 * 
	 * @return String　统计图超链接目标窗口
	 */
	public String getLinkTarget() {
		return linkTarget;
	}

	/**
	 * 设置统计图超链接目标窗口
	 * 
	 * @param target 统计图超链接目标窗口
	 */
	public void setLinkTarget(String target) {
		this.linkTarget = target;
	}

	/**
	 * 取统计图的图例位置
	 * 
	 * @return byte　统计图的图例位置，值为LEGEND_LEFT, LEGEND_RIGHT, LEGEND_TOP,
	 *         LEGEND_BOTTOM, LEGEND_NONE
	 */
	public byte getLegendLocation() {
		return legendLocation;
	}

	/**
	 * 设置统计图的图例位置
	 * 
	 * @param location
	 *            统计图的图例位置, 取值为LEGEND_LEFT, LEGEND_RIGHT, LEGEND_TOP,
	 *            LEGEND_BOTTOM, LEGEND_NONE
	 */
	public void setLegendLocation(byte location) {
		this.legendLocation = location;
	}

	/**
	 * 设置图例布局的纵向间隙
	 * @param gap 间隙值
	 */
	public void setLegendVerticalGap(int gap){
		this.legendVerticalGap = gap;
	}
	/**
	 * 取图例的纵向间隙值
	 * @return int 间隙值
	 */
	public int getLegendVerticalGap(){
		return legendVerticalGap;
	}
	
	/**
	 * 设置图例的横向间隙值
	 * @param gap 间隙值
	 */
	public void setLegendHorizonGap(int gap){
		this.legendHorizonGap = gap;
	}
	
	/**
	 * 去图例横向间隙值
	 * @return 间隙值
	 */
	public int getLegendHorizonGap(){
		return legendHorizonGap;
	}

	/**
	 * 图例是否按照系列绘制
	 * @return 按系列绘制时返回true，否则返回false
	 */
	public boolean getDrawLegendBySery() {
		return drawLegendBySery;
	}

	/**
	 * 设置是否按照系列值绘制图例
	 * @param b 是否按系列绘图例
	 */
	public void setDrawLegendBySery(boolean b) {
		this.drawLegendBySery = b;
	}

	/**
	 * 取统计图的配色方案名
	 * 
	 * @return String　统计图的配色方案名
	 */
	public String getColorConfig() {
		return colorConfig;
	}

	/**
	 * 设置统计图的配色方案名
	 * 
	 * @param config
	 *            统计图的配色方案名
	 */
	public void setColorConfig(String config) {
		this.colorConfig = config;
	}

	/**
	 * 取统计值起始值
	 * 
	 * @return String　统计值起始值
	 */
	public String getYStartValue() {
		return yStartValue;
	}

	/**
	 * 设置统计值起始值
	 * 
	 * @param value
	 *            统计值起始值, 双轴图时用分号隔开
	 */
	public void setYStartValue(String value) {
		this.yStartValue = value;
	}

	/**
	 * 取统计值结束值
	 * 
	 * @return String　统计值结束值
	 */
	public String getYEndValue() {
		return yEndValue;
	}

	/**
	 * 设置统计值结束值
	 * 
	 * @param value
	 *            统计值结束值, 双轴图时用分号隔开
	 */
	public void setYEndValue(String value) {
		this.yEndValue = value;
	}

	/**
	 * 取统计值标签间隔
	 * 
	 * @return String　统计值标签间隔
	 */
	public String getYInterval() {
		return yInterval;
	}

	/**
	 * 设置统计值标签间隔
	 * 
	 * @param interval
	 *            统计值标签间隔，双轴图时用分号隔开
	 */
	public void setYInterval(String interval) {
		this.yInterval = interval;
	}

	/**
	 * 取统计值数量单位
	 * 
	 * @return double　统计值数量单位，值为UNIT_ORIGIN, UNIT_AUTO, UNIT_THOUSAND,
	 *         UNIT_10THOUSAND, UNIT_MILLION, UNIT_10MILLION, UNIT_100MILLION,
	 *         UNIT_BILLION, UNIT_001, UNIT_0001, UNIT_00001, UNIT_0000001
	 */
	public double getDataUnit() {
		return dataUnit;
	}

	/**
	 * 设置统计值数量单位
	 * 
	 * @param unit
	 *            统计值数量单位，取值为UNIT_ORIGIN, UNIT_AUTO, UNIT_THOUSAND,
	 *            UNIT_10THOUSAND, UNIT_MILLION, UNIT_10MILLION,
	 *            UNIT_100MILLION, UNIT_BILLION, UNIT_001, UNIT_0001,
	 *            UNIT_00001, UNIT_0000001
	 */
	public void setDataUnit(double unit) {
		this.dataUnit = unit;
	}

	/**
	 * 取统计值最少刻度数
	 * 
	 * @return String　统计值最少刻度数
	 */
	public int getYMinMarks() {
		return yMinMarks;
	}

	/**
	 * 设置统计值最少刻度数
	 * 
	 * @param marks
	 *            统计值最少刻度数
	 */
	public void setYMinMarks(int marks) {
		this.yMinMarks = marks;
	}

	/**
	 * 取标题与图形之间的间距
	 * 
	 * @return String　标题与图形之间的间距
	 */
	public int getTitleMargin() {
		return titleMargin;
	}

	/**
	 * 设置标题与图形之间的间距
	 * 
	 * @param margin
	 *            标题与图形之间的间距
	 */
	public void setTitleMargin(int margin) {
		this.titleMargin = margin;
	}

	/**
	 * 取折线图是否标注数据点
	 * 
	 * @return boolean 绘制数据点时返回true，否则false
	 */
	public boolean isDrawLineDot() {
		return drawLineDot;
	}

	/**
	 * 折线图是否画趋势线
	 * 
	 * @return boolean 绘制趋势线时返回true，否则返回false
	 */
	public boolean isDrawLineTrend() {
		return drawLineTrend;
	}

	/**
	 * 折线图的粗细度
	 * 
	 * @return byte 粗度
	 */
	public byte getLineThick() {
		return lineThick;
	}

	/**
	 * 取折线的线型风格
	 * @return 风格
	 */
	public byte getLineStyle() {
		return lineStyle;
	}

	/**
	 * 设置折线图是否标注数据点
	 * 
	 * @param b 是否标注
	 */
	public void setDrawLineDot(boolean b) {
		this.drawLineDot = b;
	}

	/**
	 * 设置原点重合
	 * @param b 是否重合
	 */
	public void setOverlapOrigin(boolean b) {
		this.isOverlapOrigin = b;
	}
	
	/**
	 * 取原点是否重合
	 * @return 重合时返回true，否则返回false
	 */
	public boolean isOverlapOrigin() {
		return isOverlapOrigin;
	}
	
	/**
	 * 设置折线图是否画趋势线
	 * 
	 * @param b 是否画趋势线
	 */
	public void setDrawLineTrend(boolean b) {
		this.drawLineTrend = b;
	}

	/**
	 * 设置折线图粗细度
	 * 
	 * @byte thick 粗度
	 */
	public void setLineThick(byte thick) {
		this.lineThick = thick;
	}

	/**
	 * 设置折线图的线型
	 * @param style  线型
	 */
	public void setLineStyle(byte style) {
		this.lineStyle = style;
	}

	/**
	 * 取相邻数值或标签重叠时是否显示后一数值或标签
	 * 
	 * @return boolean 允许文本重叠时返回true，否则返回false
	 */
	public boolean isShowOverlapText() {
		return showOverlapText;
	}

	/**
	 * 设置相邻数值或标签重叠时是否显示后一数值或标签
	 * 
	 * @param b 是否允许文本重叠绘制
	 */
	public void setShowOverlapText(boolean b) {
		this.showOverlapText = b;
	}

	/**
	 * 取饼图中是否分离出一扇显示
	 * 
	 * @return boolean 是否分离
	 */
	public boolean isPieSpacing() {
		return pieSpacing;
	}

	/**
	 * 仪表盘着色是否将刻度绘制到颜色末端
	 * 比如定义20度为绿色表示舒适，30度为红色表示炎热
	 * 则按照末端绘制则为区间0到20画绿色，20到30画红色
	 * 而非末端时指刻度位于颜色中间，此时15到25画绿色，让20位于颜色中间；而25到30才画红色。
	 */
	public boolean isMeterColorEnd() {
		return isMeterColorEnd;
	}
	/**
	 * 是否绘制仪表盘刻度标签
	 * 不画刻度时仅标出定义的标签
	 */
	public boolean isMeterTick() {
		return isMeterTick;
	}
	/**
	 * 设置是否绘制仪表盘刻度标签
	 * @param b 是否绘制
	 */
	public void setMeterTick(boolean b){
		isMeterTick = b;
	}
	
	/**
	 * 取仪表盘3d边框厚度
	 */
	public int getMeter3DEdge() {
		return meter3DEdge;
	}
	
	/**
	 * 取仪表盘彩虹边厚度
	 */
	public int getMeterRainbowEdge() {
		return meterRainbowEdge;
	}
	/**
	 * 取饼图的连接线
	 */
	public int getPieLine() {
		return pieLine;
	}
	
	/**
	 * 堆积图形时，是否显示汇总值
	 */
	public boolean isDispStackSumValue(){
		return isDispStackSumValue;
	}
	/**
	 * 设置堆积图时是否绘制汇总值
	 * @param b 是否绘制
	 */
	public void setDispStackSumValue(boolean b){
		isDispStackSumValue = b;
	}
	 
	/**
	 * 设置饼图中是否分离出一扇显示
	 * 
	 * @param b
	 *            饼图中是否分离出一扇显示
	 */
	public void setPieSpacing(boolean b) {
		this.pieSpacing = b;
	}

	/**
	 * 取分类轴标签间隔
	 * 
	 * @return String　分类轴标签间隔
	 */
	public int getXInterval() {
		return xInterval;
	}

	/**
	 * 设置分类轴标签间隔
	 * 
	 * @param interval
	 *            分类轴标签间隔
	 */
	public void setXInterval(int interval) {
		this.xInterval = interval;
	}

	/**
	 * 折线图是否忽略空值
	 * 
	 * @return boolean
	 */
	public boolean ignoreNull() {
		return ignoreNull;
	}

	/**
	 * 设置折线图是否忽略空值
	 * 
	 * @param b
	 */
	public void setIgnoreNull(boolean b) {
		this.ignoreNull = b;
	}

	/**
	 * 是否画阴影
	 */
	public boolean isDrawShade() {
		return drawShade;
	}

	/**
	 * 设置是否画阴影
	 * @param isDrawShade 是否画阴影
	 */
	public void setDrawShade(boolean isDrawShade) {
		drawShade = isDrawShade;
	}

	/**
	 * 柱图形式，是否画凸出边框
	 */
	public boolean isRaisedBorder() {
		return raisedBorder;
	}

	/**
	 * 设置柱图是否绘制凸出边框
	 * @param isRaisedBorder 是否凸出边框
	 */
	public void setRaisedBorder(boolean isRaisedBorder) {
		raisedBorder = isRaisedBorder;
	}

	/**
	 * 可用于扩充的标志位，整体获取时无意义
	 * 参考getFlag(prop)
	 * @return
	 */
	public long getFlag() {
		return flag;
	}

	/**
	 * 设置标志位
	 * @param newFlag 标志位
	 */
	public void setFlag(long newFlag) {
		flag = newFlag;
	}

	/**
	 * 获取标志位属性，可扩充
	 * 目前只实现FLAG_DISCARDOTHER
	 */
	public boolean getFlag(byte prop) {
		return (this.flag & (0x01 << prop)) != 0;
	}

	/**
	 * 设置标志位的布尔值
	 * @param prop 标志位
	 * @param isOn 布尔状态
	 */
	public void setFlag(byte prop, boolean isOn) {
		if (isOn) {
			this.flag |= 0x01 << prop;
		} else {
			this.flag &= ~(0x01 << prop);
		}
	}

	/**
	 * 取背景图配置
	 * @return 背景图配置
	 */
	public BackGraphConfig getBackGraphConfig() {
		return backGraph;
	}

	/**
	 * 设置背景图配置
	 * @param backGraphConfig 背景图对象
	 */
	public void setBackGraphConfig(BackGraphConfig backGraphConfig) {
		backGraph = backGraphConfig;
	}

	/**
	 * 取图形四边以及图例的颜色数组
	 */
	public int[] getAxisColors() {
		return axisColors;
	}

	/**
	 * 设置图形四边轴以及图例的颜色数组
	 */
	public void setAxisColors(int[] colors) {
		axisColors = colors;
	}

	/**
	 * 取图形左边距
	 */
	public int getLeftMargin() {
		return leftMargin;
	}

	/**
	 * 设置图形的左边距
	 * @param leftMargin
	 */
	public void setLeftMargin(int leftMargin) {
		this.leftMargin = leftMargin;
	}

	/**
	 * 取图形的右边距
	 */
	public int getRightMargin() {
		return rightMargin;
	}

	/**
	 * 设置图形的右边距
	 * @param rightMargin 右边距
	 */
	public void setRightMargin(int rightMargin) {
		this.rightMargin = rightMargin;
	}

	/**
	 * 取图形的顶边距
	 */
	public int getTopMargin() {
		return topMargin;
	}

	/**
	 * 设置图形的顶边距
	 * @param topMargin 顶边距
	 */
	public void setTopMargin(int topMargin) {
		this.topMargin = topMargin;
	}

	/**
	 * 取图形的底边距
	 */
	public int getBottomMargin() {
		return bottomMargin;
	}

	/**
	 * 设置图形的底边距
	 * @param bottomMargin 底边距
	 */
	public void setBottomMargin(int bottomMargin) {
		this.bottomMargin = bottomMargin;
	}

	/**
	 * 取刻度的绘制长度
	 */
	public int getTickLen() {
		return tickLen;
	}

	/**
	 * 设置刻度的绘制长度
	 * @param tickLen 长度
	 */
	public void setTickLen(int tickLen) {
		this.tickLen = tickLen;
	}

	/**
	 * 取坐标系厚度
	 */
	public int getCoorWidth() {
		return coorWidth;
	}

	/**
	 * 获取坐标系厚度
	 * @param coorWidth 厚度值
	 */
	public void setCoorWidth(int coorWidth) {
		this.coorWidth = coorWidth;
	}

	/**
	 * 取图形分类的间隙值
	 */
	public double getCategorySpan() {
		return categorySpan;
	}

	/**
	 * 设置图形分类值之间的间隙
	 * @param categorySpan 间隙值
	 */
	public void setCategorySpan(double categorySpan) {
		this.categorySpan = categorySpan;
	}

	/**
	 * 取相邻系列之间的间隙值
	 */
	public int getSeriesSpan() {
		return seriesSpan;
	}

	/**
	 * 设置系列之间的间隙值
	 * @param seriesSpan 间隙值
	 */
	public void setSeriesSpan(int seriesSpan) {
		this.seriesSpan = seriesSpan;
	}

	/**
	 * 取饼图的旋转角度
	 */
	public int getPieRotation() {
		return pieRotation;
	}

	/**
	 * 设置饼图的旋转角度
	 * @param pieRotation 角度
	 */
	public void setPieRotation(int pieRotation) {
		this.pieRotation = pieRotation;
	}

	/**
	 * 取立体饼图的高度
	 */
	public int getPieHeight() {
		return pieHeight;
	}

	/**
	 * 设置立体饼图的高度
	 * @param pieHeight 高度值
	 */
	public void setPieHeight(int pieHeight) {
		this.pieHeight = pieHeight;
	}

	/**
	 * 设置共有属性
	 * @param pp 共有属性
	 */
	public void setPublicProperty(PublicProperty pp){
		type=pp.getType();
		axisColor = pp.getAxisColor();
		canvasColor = pp.getCanvasColor();
		graphBackColor = pp.getGraphBackColor();
		xTitle = pp.getXTitle();
		yTitle = pp.getYTitle();
		graphTitle = pp.getGraphTitle();
		gridLineType = pp.getGridLineType();
		gridLineColor = pp.getGridLineColor();
		barDistance = pp.getBarDistance();
		imageFormat = pp.getImageFormat();
		graphTransparent = pp.isGraphTransparent();
		isDrawDataTable = pp.isDrawDataTable();
		isDataCenter = pp.isDataCenter();
		
		gradientColor = pp.isGradientColor();
		topData = pp.getTopData();
		statusStartTimeExp = pp.getStatusStartTimeExp();
		statusEndTimeExp = pp.getStatusEndTimeExp();
		statusCategoryExp = pp.getStatusCategoryExp();
		statusStateExp = pp.getStatusStateExp();
		statusBarWidth = pp.getStatusBarWidth();
		statusTimeType = pp.getStatusTimeType();
		fonts = (GraphFonts)pp.getFonts().deepClone();
		if (pp.getAlarmLines() != null) {
			AlarmLine[] aline = new AlarmLine[pp.getAlarmLines().length];
			for (int i = 0; i < aline.length; i++) {
				aline[i] = (AlarmLine) pp.getAlarmLines()[i].deepClone();
			}
			alarms = aline;
		}
		displayData = pp.getDisplayData();
		displayData2 = pp.getDisplayData2();
		displayDataFormat = pp.getDisplayDataFormat();
		link = pp.getLink();
		linkTarget = pp.getLinkTarget();
		legendLocation = pp.getLegendLocation();
		legendVerticalGap = pp.getLegendVerticalGap();
		legendHorizonGap = pp.getLegendHorizonGap();
		drawLegendBySery = pp.getDrawLegendBySery();
		colorConfig = pp.getColorConfig();
		yStartValue = pp.getYStartValue();
		yEndValue = pp.getYEndValue();
		yInterval = pp.getYInterval();
		dataUnit = pp.getDataUnit();
		yMinMarks = pp.getYMinMarks();
		titleMargin = pp.getTitleMargin();
		drawLineDot = pp.isDrawLineDot();
		drawLineTrend = pp.isDrawLineTrend();
		lineThick = pp.getLineThick();
		showOverlapText = pp.isShowOverlapText();
		xInterval = pp.getXInterval();
		pieSpacing = pp.isPieSpacing();
		ignoreNull = pp.ignoreNull();
		customClass = pp.getCustomClass();
		customParam = pp.getCustomParam();
		drawShade = pp.isDrawShade();
		raisedBorder = pp.isRaisedBorder();
		backGraph = pp.getBackGraphConfig();
		axisColors = pp.getAxisColors();
		flag = pp.getFlag();
		leftMargin = pp.getLeftMargin();
		rightMargin = pp.getRightMargin();
		topMargin = pp.getTopMargin();
		bottomMargin = pp.getBottomMargin();
		tickLen = pp.getTickLen();
		coorWidth = pp.getCoorWidth();
		categorySpan = pp.getCategorySpan();
		seriesSpan = pp.getSeriesSpan();
		pieRotation = pp.getPieRotation();
		pieHeight = pp.getPieHeight();
		legendLink = pp.getLegendLink();
		curveType = pp.getCurveType();
		lineStyle = pp.getLineStyle();
		setBorder(pp.getBorderStyle(), pp.getBorderWidth(),
				getColorObject(pp.getBorderColor()), pp.getBorderShadow());
		isDispStackSumValue = pp.isDispStackSumValue();
		otherStackedSeries = pp.getOtherStackedSeries();
		isOverlapOrigin = pp.isOverlapOrigin();
	}
	
	/**
	 * 深度克隆
	 * 
	 * @return Object 克隆的图形属性
	 */
	public Object deepClone() {
		GraphProperty gp = new GraphProperty();
		gp.setPublicProperty(this);
		return gp;
	}

	/**
	 * 按版本序列化
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(9);
		out.writeByte(type);
		out.writeInt(axisColor);
		out.writeInt(canvasColor);
		out.writeInt(graphBackColor);
		out.writeObject(xTitle);
		out.writeObject(yTitle);
		out.writeObject(graphTitle);
		out.writeByte(gridLineType);
		out.writeInt(gridLineColor);
		out.writeInt(barDistance);
		out.writeByte(imageFormat);
		out.writeBoolean(graphTransparent);
		out.writeBoolean(gradientColor);
		out.writeInt(topData);
		out.writeObject(statusStartTimeExp);
		out.writeObject(statusEndTimeExp);
		out.writeObject(statusCategoryExp);
		out.writeObject(statusStateExp);
		out.writeObject(statusBarWidth);
		out.writeByte(statusTimeType);
		out.writeObject(fonts);
		out.writeObject(alarms);
		out.writeByte(displayData);
		out.writeObject(displayDataFormat);
		out.writeObject(link);
		out.writeObject(linkTarget);
		out.writeByte(legendLocation);
		out.writeBoolean(drawLegendBySery);
		out.writeObject(colorConfig);
		out.writeObject(yStartValue);
		out.writeObject(yEndValue);
		out.writeObject(yInterval);
		out.writeDouble(dataUnit);
		out.writeInt(yMinMarks);
		out.writeInt(titleMargin);
		out.writeBoolean(drawLineDot);
		out.writeBoolean(showOverlapText);
		out.writeBoolean(pieSpacing);
		out.writeInt(xInterval);
		out.writeByte(lineThick);
		out.writeBoolean(drawLineTrend);
		out.writeBoolean(ignoreNull);
		out.writeObject(customClass);
		out.writeObject(customParam);
		out.writeBoolean(drawShade);
		out.writeBoolean(raisedBorder);
		out.writeObject(backGraph);
		out.writeObject(axisColors);
		out.writeLong(flag);
		out.writeInt(leftMargin);
		out.writeInt(rightMargin);
		out.writeInt(topMargin);
		out.writeInt(bottomMargin);
		out.writeInt(tickLen);
		out.writeInt(coorWidth);
		out.writeDouble(categorySpan);
		out.writeInt(seriesSpan);
		out.writeInt(pieRotation);
		out.writeInt(pieHeight);
		out.writeObject(legendLink);
		out.writeByte(curveType);
		out.writeByte(lineStyle);
		out.writeByte(borderStyle);
		out.writeFloat(borderWidth);
		out.writeInt(borderColor);
		out.writeBoolean(borderShadow);
		out.writeBoolean(isDispStackSumValue);
		out.writeBoolean(isDrawDataTable);
		out.writeObject(otherStackedSeries);
		out.writeBoolean(isOverlapOrigin);
		out.writeInt(legendVerticalGap);
		out.writeInt(legendHorizonGap);
		out.writeBoolean(isDataCenter);
		out.writeByte(displayData2);

	}

	/**
	 * 按版本反序列化
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		byte ver = in.readByte();
		type = in.readByte();
		canvasColor = in.readInt();
		graphBackColor = in.readInt();
		xTitle = (String) in.readObject();
		yTitle = (String) in.readObject();
		graphTitle = (String) in.readObject();
		gridLineType = in.readByte();
		gridLineColor = in.readInt();
		barDistance = in.readInt();
		imageFormat = in.readByte();
		graphTransparent = in.readBoolean();
		gradientColor = in.readBoolean();
		topData = in.readInt();
		statusStartTimeExp = (String) in.readObject();
		statusEndTimeExp = (String) in.readObject();
		statusCategoryExp = (String) in.readObject();
		statusStateExp = (String) in.readObject();
		statusBarWidth = (String) in.readObject();
		statusTimeType = in.readByte();
		fonts = (GraphFonts) in.readObject();
		alarms = (AlarmLine[]) in.readObject();
		displayData = in.readByte();
		displayDataFormat = (String) in.readObject();
		link = (String) in.readObject();
		linkTarget = (String) in.readObject();
		legendLocation = in.readByte();
		drawLegendBySery = in.readBoolean();
		colorConfig = (String) in.readObject();
		yStartValue = (String) in.readObject();
		yEndValue = (String) in.readObject();
		yInterval = (String) in.readObject();
		dataUnit = in.readDouble();
		yMinMarks = in.readInt();
		titleMargin = in.readInt();
		drawLineDot = in.readBoolean();
		showOverlapText = in.readBoolean();
		pieSpacing = in.readBoolean();
		xInterval = in.readInt();
		lineThick = in.readByte();
		drawLineTrend = in.readBoolean();
		ignoreNull = in.readBoolean();
		customClass = (String) in.readObject();
		customParam = (String) in.readObject();
		drawShade = in.readBoolean();
		raisedBorder = in.readBoolean();
		backGraph = (BackGraphConfig) in.readObject();
		axisColors = (int[]) in.readObject();
		flag = in.readLong();
		leftMargin = in.readInt();
		rightMargin = in.readInt();
		topMargin = in.readInt();
		bottomMargin = in.readInt();
		tickLen = in.readInt();
		coorWidth = in.readInt();
		categorySpan = in.readDouble();
		seriesSpan = in.readInt();
		pieRotation = in.readInt();
		pieHeight = in.readInt();
		legendLink = (String) in.readObject();
		curveType = in.readByte();
		lineStyle = in.readByte();
		if (ver > 1) {
			borderStyle = in.readByte();
			borderWidth = in.readFloat();
			borderColor = in.readInt();
			borderShadow = in.readBoolean();
		}
		if(ver>2){
			isDispStackSumValue = in.readBoolean();
		}
		if(ver>3){
			isDrawDataTable = in.readBoolean();
		}
		if(ver>4){
			otherStackedSeries = (String)in.readObject();
		}
		if(ver>5){
			isOverlapOrigin = in.readBoolean();
		}
		if(ver>6){
			legendVerticalGap = in.readInt();
			legendHorizonGap = in.readInt();
		}
		if(ver>7){
			isDataCenter = in.readBoolean();
		}
		if(ver>8){
			displayData2 = in.readByte();
		}
		
	}

	/**
	 * 实现IRecord接口
	 */
	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeByte(type);
		out.writeInt(axisColor);
		out.writeInt(canvasColor);
		out.writeInt(graphBackColor);
		out.writeString(xTitle);
		out.writeString(yTitle);
		out.writeString(graphTitle);
		out.writeByte(gridLineType);
		out.writeInt(gridLineColor);
		out.writeInt(barDistance);
		out.writeByte(imageFormat);
		out.writeBoolean(graphTransparent);
		out.writeBoolean(gradientColor);
		out.writeInt(topData);
		out.writeString(statusStartTimeExp);
		out.writeString(statusEndTimeExp);
		out.writeString(statusCategoryExp);
		out.writeString(statusStateExp);
		out.writeString(statusBarWidth);
		out.writeByte(statusTimeType);
		out.writeRecord(fonts);
		if (alarms == null) {
			out.writeShort((short) 0);
		} else {
			int size = alarms.length;
			out.writeShort((short) size);
			for (int i = 0; i < size; i++) {
				out.writeRecord(alarms[i]);
			}
		}
		out.writeByte(displayData);
		out.writeString(displayDataFormat);
		out.writeString(link);
		out.writeString(linkTarget);
		out.writeByte(legendLocation);
		out.writeBoolean(drawLegendBySery);
		out.writeString(colorConfig);
		out.writeString(yStartValue);
		out.writeString(yEndValue);
		out.writeString(yInterval);
		out.writeDouble(dataUnit);
		out.writeInt(yMinMarks);
		out.writeInt(titleMargin);
		out.writeBoolean(drawLineDot);
		out.writeBoolean(showOverlapText);
		out.writeBoolean(pieSpacing);
		out.writeInt(xInterval);
		out.writeByte(lineThick);
		out.writeBoolean(drawLineTrend);
		out.writeBoolean(ignoreNull);
		out.writeString(customClass);
		out.writeString(customParam);
		out.writeBoolean(drawShade);
		out.writeBoolean(raisedBorder);
		out.writeRecord(backGraph);

		if (axisColors == null) {
			out.writeShort((short) 0);
		} else {
			int size = axisColors.length;
			out.writeShort((short) size);
			for (int i = 0; i < size; i++) {
				out.writeInt(axisColors[i]);
			}
		}
		out.writeLong(flag);
		out.writeInt(leftMargin);
		out.writeInt(rightMargin);
		out.writeInt(topMargin);
		out.writeInt(bottomMargin);
		out.writeInt(tickLen);
		out.writeInt(coorWidth);
		out.writeDouble(categorySpan);
		out.writeInt(seriesSpan);
		out.writeInt(pieRotation);
		out.writeInt(pieHeight);
		out.writeString(legendLink);
		out.writeByte(curveType);
		out.writeByte(lineStyle);
		out.writeByte(borderStyle);
		out.writeFloat(borderWidth);
		out.writeInt(borderColor);
		out.writeBoolean(borderShadow);
		out.writeBoolean(isDispStackSumValue);
		out.writeBoolean(isDrawDataTable);
		out.writeString(otherStackedSeries);
		
		out.writeBoolean(isOverlapOrigin);
		out.writeInt(legendVerticalGap);
		out.writeInt(legendHorizonGap);
		out.writeBoolean(isDataCenter);
		out.writeByte(displayData2);
		return out.toByteArray();
	}

	/**
	 * 实现IRecord接口
	 */
	public void fillRecord(byte[] buf) throws IOException,
			ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		type = in.readByte();
		axisColor = in.readInt();
		canvasColor = in.readInt();
		graphBackColor = in.readInt();
		xTitle = in.readString();
		yTitle = in.readString();
		graphTitle = in.readString();
		gridLineType = in.readByte();
		gridLineColor = in.readInt();
		barDistance = in.readInt();
		imageFormat = in.readByte();
		graphTransparent = in.readBoolean();
		gradientColor = in.readBoolean();
		topData = in.readInt();
		statusStartTimeExp = in.readString();
		statusEndTimeExp = in.readString();
		statusCategoryExp = in.readString();
		statusStateExp = in.readString();
		statusBarWidth = in.readString();
		statusTimeType = in.readByte();
		fonts = (GraphFonts) in.readRecord(new GraphFonts());
		short an = in.readShort();
		if (an > 0) {
			alarms = new AlarmLine[an];
			for (int i = 0; i < an; i++) {
				alarms[i] = (AlarmLine) in.readRecord(new AlarmLine());
			}
		}
		displayData = in.readByte();
		displayDataFormat = in.readString();
		link = in.readString();
		linkTarget = in.readString();
		legendLocation = in.readByte();
		drawLegendBySery = in.readBoolean();
		colorConfig = in.readString();
		yStartValue = in.readString();
		yEndValue = in.readString();
		yInterval = in.readString();
		dataUnit = in.readDouble();
		yMinMarks = in.readInt();
		titleMargin = in.readInt();
		drawLineDot = in.readBoolean();
		showOverlapText = in.readBoolean();
		pieSpacing = in.readBoolean();
		xInterval = in.readInt();
		lineThick = in.readByte();
		drawLineTrend = in.readBoolean();
		ignoreNull = in.readBoolean();
		customClass = in.readString();
		customParam = in.readString();
		drawShade = in.readBoolean();
		raisedBorder = in.readBoolean();
		backGraph = (BackGraphConfig) in.readRecord(new BackGraphConfig());
		an = in.readShort();
		if (an > 0) {
			axisColors = new int[an];
			for (int i = 0; i < an; i++) {
				axisColors[i] = in.readInt();
			}
		}
		flag = in.readLong();
		leftMargin = in.readInt();
		rightMargin = in.readInt();
		topMargin = in.readInt();
		bottomMargin = in.readInt();
		tickLen = in.readInt();
		coorWidth = in.readInt();
		categorySpan = in.readDouble();
		seriesSpan = in.readInt();
		pieRotation = in.readInt();
		pieHeight = in.readInt();
		legendLink = in.readString();
		curveType = in.readByte();
		lineStyle = in.readByte();
		if (in.available() > 0) {
			borderStyle = in.readByte();
			borderWidth = in.readFloat();
			borderColor = in.readInt();
			borderShadow = in.readBoolean();
		}
		if(in.available()>0){
			isDispStackSumValue = in.readBoolean();
		}
		if(in.available()>0){
			isDrawDataTable = in.readBoolean();
		}
		if(in.available()>0){
			otherStackedSeries = in.readString();
		}
		if(in.available()>0){
			isOverlapOrigin = in.readBoolean();
		}
		if(in.available()>0){
			legendVerticalGap = in.readInt();
			legendHorizonGap = in.readInt();
		}
		if(in.available()>0){
			isDataCenter = in.readBoolean();
		}
		if(in.available()>0){
			displayData2 = in.readByte();
		}
	}

	/**
	 * 设置其他堆积系列
	 * 比如共有语文数学英语等7门课，现在只关注语文数学，其他的课程可以汇总在一起，列在其他系列
	 */
	public void setOtherStackedSeries(String other) {
		this.otherStackedSeries = other;
	}

	/**
	 * 取其他堆积序列
	 */
	public String getOtherStackedSeries() {
		return otherStackedSeries;
	}

	/**
	 * 横轴标题的水平对齐方式
	 */
	public byte getXTitleAlign() {
		return IStyle.HALIGN_CENTER;
	}
	
	/**
	 * 纵轴标题的垂直对齐方式
	 */
	public byte getYTitleAlign() {
		return IStyle.VALIGN_MIDDLE;
	}

	/**
	 * 图形标题的水平对齐方式
	 */
	public byte getGraphTitleAlign() {
		return IStyle.HALIGN_CENTER;
	}

	/**
	 * 网格线位置
	 */
	public byte getGridLocation() {
		return gridLineLocation;
	}

	/**
	 * 设置网格线位置
	 */
	public void setGridLocation(byte loc) {
		gridLineLocation = loc;
	}

}
