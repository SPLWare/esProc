package com.scudata.cellset.graph.config;

import com.scudata.chart.Consts;

/**
 * 图形属性接口，以及图形常量定义
 * @author Joancy
 *
 */
public interface IGraphProperty{
  /** 网格线位置 -- 数值轴 */
  public static final byte GRID_VALUE = (byte)Consts.GRID_VALUE;
  /** 网格线位置 -- 分类轴 */
  public static final byte GRID_CATEGORY = (byte)Consts.GRID_CATEGORY;
  /** 网格线位置 -- 全部*/
  public static final byte GRID_BOTH = (byte)Consts.GRID_BOTH;

  /** 线类型 -- 没有线 */
  public static final byte LINE_NONE = (byte)Consts.LINE_NONE;

  /** 线类型 -- 实线 */
  public static final byte LINE_SOLID = (byte)Consts.LINE_SOLID;

  /** 线类型 -- 长虚线 */
  public static final byte LINE_LONG_DASH = (byte)Consts.LINE_DASHED;

  /** 线类型 -- 短虚线 */
  public static final byte LINE_SHORT_DASH = (byte)Consts.LINE_DOTTED;

  /** 线类型 -- 点划线 */
  public static final byte LINE_DOT_DASH = (byte)Consts.LINE_DOTDASH;

  /** 线类型 -- 双点划线 */
  public static final byte LINE_2DOT_DASH = (byte)Consts.LINE_DOUBLE;

  /** 图形格式 -- JPG  */
  public static final byte IMAGE_JPG = Consts.IMAGE_JPG;

  /** 图形格式 -- GIF  */
  public static final byte IMAGE_GIF = Consts.IMAGE_GIF;

  /** 图形格式 -- PNG  */
  public static final byte IMAGE_PNG = Consts.IMAGE_PNG;

  /** 图形格式 -- FLASH*/
  public static final byte IMAGE_FLASH = Consts.IMAGE_FLASH;

  /** 图形格式 -- SVG */
  public static final byte IMAGE_SVG = Consts.IMAGE_SVG;

  /** 时间刻度类型 -- 年  */
  public static final byte TIME_YEAR = (byte) 1;

  /** 时间刻度类型 -- 月  */
  public static final byte TIME_MONTH = (byte) 2;

  /** 时间刻度类型 -- 日  */
  public static final byte TIME_DAY = (byte) 3;

  /** 时间刻度类型 -- 时  */
  public static final byte TIME_HOUR = (byte) 4;

  /** 时间刻度类型 -- 分  */
  public static final byte TIME_MINUTE = (byte) 5;

  /** 时间刻度类型 -- 秒  */
  public static final byte TIME_SECOND = (byte) 6;

  /** 图中显示数据 -- 无  */
  public static final byte DISPDATA_NONE = (byte) 1;

  /** 图中显示数据 -- 统计值  */
  public static final byte DISPDATA_VALUE = (byte) 2;

  /** 图中显示数据 -- 百分比  */
  public static final byte DISPDATA_PERCENTAGE = (byte) 3;

  /** 图中显示数据 -- 显示系列标题  */
  public static final byte DISPDATA_TITLE = (byte) 4;
  
  /** 图中显示数据 -- 名称和统计值  */
  public static final byte DISPDATA_NAME_VALUE = (byte) 5;
  
  /** 图中显示数据 -- 名称和百分比  */
  public static final byte DISPDATA_NAME_PERCENTAGE = (byte) 6;

  /** 图例位置 -- 左边  */
  public static final byte LEGEND_LEFT = (byte) 1;

  /** 图例位置 -- 右边  */
  public static final byte LEGEND_RIGHT = (byte) 2;

  /** 图例位置 -- 上边  */
  public static final byte LEGEND_TOP = (byte) 3;

  /** 图例位置 -- 下边  */
  public static final byte LEGEND_BOTTOM = (byte) 4;

  /** 图例位置 -- 无  */
  public static final byte LEGEND_NONE = (byte) 5;

  /** 统计值数量单位 -- 无缩放 */
  public static final double UNIT_ORIGIN = 1;

  /** 统计值数量单位 -- 自动计算 */
  public static final double UNIT_AUTO = 2;

  /** 统计值数量单位 -- 千 */
  public static final double UNIT_THOUSAND = 1000;

  /** 统计值数量单位 -- 万 */
  public static final double UNIT_10THOUSAND = 10000;

  /** 统计值数量单位 -- 百万 */
  public static final double UNIT_MILLION = 1000000;

  /** 统计值数量单位 -- 千万 */
  public static final double UNIT_10MILLION = 10000000;

  /** 统计值数量单位 -- 亿 */
  public static final double UNIT_100MILLION = 100000000;

  /** 统计值数量单位 -- 十亿 */
  public static final double UNIT_BILLION = 1000000000;

  /** 统计值数量单位 -- 百分之一 */
  public static final double UNIT_001 = 0.01;

  /** 统计值数量单位 -- 千分之一 */
  public static final double UNIT_0001 = 0.001;

  /** 统计值数量单位 -- 万分之一 */
  public static final double UNIT_00001 = 0.0001;

  /** 统计值数量单位 -- 百万分之一 */
  public static final double UNIT_0000001 = 0.000001;

  public static final int AXIS_TOP = 0;
  public static final int AXIS_BOTTOM = 1;
  public static final int AXIS_LEFT = 2;
  public static final int AXIS_RIGHT = 3;
  public static final int AXIS_LEGEND = 4;
  public static final int AXIS_COLBORDER = 5;
  public static final int AXIS_PIEJOIN = 6;

  public static final byte CURVE_LAGRANGE = 0; //拉格朗日曲线
  public static final byte CURVE_AKIMA = 1; //阿克玛曲线
  public static final byte CURVE_3SAMPLE = 2; //三次样条曲线

  public static byte FLAG_DISCARDOTHER = 0;


  public int getLeftMargin();
  public int getRightMargin();
  public int getTopMargin();
  public int getBottomMargin();
  public int getTickLen();
  public int getCoorWidth();
  public double getCategorySpan();
  public int getSeriesSpan();
  public int getPieRotation();
  public int getPieHeight();
  public byte getType();
  public byte getCurveType();
  public boolean isPieSpacing();
  public boolean isMeterColorEnd();
  public boolean isMeterTick();
  public boolean isGradientColor();
  public GraphFonts getFonts();
  public AlarmLine[] getAlarmLines();
  public byte getDisplayData();
  public byte getDisplayData2();
  public boolean isDispStackSumValue();
  public byte getLegendLocation();
  
  public int getLegendVerticalGap();
  public int getLegendHorizonGap();
  
  public boolean getDrawLegendBySery();
  public int getAxisColor();
  public int getCanvasColor();
  public int getGraphBackColor();
  public byte getGridLocation();
  public byte getGridLineType();
  public int getGridLineColor();
  public byte getImageFormat();
  public boolean isGraphTransparent();
  public String getLegendLink();
  public double getDataUnit();
  public boolean isDrawLineDot();
  public boolean isOverlapOrigin();
  public boolean isDrawLineTrend();
  public boolean ignoreNull();
  public String getCustomClass();
  public String getCustomParam();
  public byte getLineThick();
  public byte getLineStyle();
  public boolean isShowOverlapText();
  public byte getStatusTimeType();
  public boolean isDrawShade();
  public boolean isRaisedBorder();
  public boolean getFlag(byte key);
  public int[] getAxisColors();
  public void setAxisColors(int[] colors);
  public void setAxisColor(int color);
  public void setCanvasColor(int color);
  public void setGraphBackColor(int color);
  public void setGridLocation(byte loc);
  public void setGridLineType(byte type);
  public void setGridLineColor(int color);
  public void setImageFormat(byte format);
  public void setGraphTransparent(boolean b);
  public void setGradientColor(boolean b);
  public void setFonts(GraphFonts font);
  public void setDisplayData(byte data);
  public void setDisplayData2(byte data);
  public void setLegendLocation(byte location);
  public void setDataUnit(double unit);
  public void setDrawLineDot(boolean b);
  public void setShowOverlapText(boolean b);
  public void setStatusTimeType(byte type);
  public boolean isDrawDataTable();
  public boolean isDataCenter();
  
  public int getMeter3DEdge();
  public int getMeterRainbowEdge();
  public int getPieLine();

  public void setOtherStackedSeries(String other);
  public String getOtherStackedSeries();
  
  public byte getXTitleAlign();
  public byte getYTitleAlign();
  public byte getGraphTitleAlign();
  
}
