package com.scudata.chart;

import com.scudata.cellset.IStyle;

/**
 * 绘图的各种常量值定义
 * @author Joancy
 *
 */
public class Consts {
//	数值轴的数值变换类型
  public final static int TRANSFORM_NONE = 0;//不变换
  public final static int TRANSFORM_SCALE = 1;//按比例变换
  public final static int TRANSFORM_LOG = 2;//按对数变换
  public final static int TRANSFORM_EXP = 3;//按指数变换

//  图形属性的编辑类型
  public final static int INPUT_NORMAL = 1;//普通的字符串
  public final static int INPUT_EXP = 2;//普通的表达式
  public final static int INPUT_COLOR = 3;//颜色
  public final static int INPUT_LINESTYLE = 4;//线型
  public final static int INPUT_FONT = 5;//字体
  public final static int INPUT_TEXTURE = 6;//纹理，填充
  public final static int INPUT_POINTSTYLE = 7;//点的形状
  public final static int INPUT_FONTSTYLE = 8;//字体风格，包含粗体，斜体，下划线
  public final static int INPUT_COLUMNSTYLE = 9;//柱图类型
  public final static int INPUT_CHECKBOX = 10;//复选框
  public final static int INPUT_DROPDOWN = 11;//下拉列表
  public final static int INPUT_CHARTCOLOR = 12;//填充颜色，可以定义渐变色，是否炫颜色
  public final static int INPUT_DATE = 13;//日期编辑
  public final static int INPUT_ARROW = 14;//箭头
  public final static int INPUT_TICKS = 15;//刻度
  public final static int INPUT_ANGLE = 16;//角度
  public final static int INPUT_UNIT = 17;//单位
  public final static int INPUT_COORDINATES = 18;//坐标系
  public final static int INPUT_AXISLOCATION = 19;//轴位置，横轴、纵轴，极轴、角轴
  public final static int INPUT_FONTSIZE = 20;//字号
  public final static int INPUT_HALIGN = 21;//水平对齐
  public final static int INPUT_VALIGN = 22;//垂直对齐
  public final static int INPUT_LEGENDICON = 23;//图例图标
  public final static int INPUT_INTEGER = 24;//整数
  public final static int INPUT_DOUBLE = 25;//实数
  public final static int INPUT_DATEUNIT = 26;//日期
  public final static int INPUT_TRANSFORM = 27;//变换
  
  //以下是图形参数增加的输入方式，added by sjr
  public final static int INPUT_URLTARGET = 28;//url链接
  public final static int INPUT_STACKTYPE = 29;//堆积类型
  
  public final static int INPUT_DISPLAYDATA = 40;//显示数据
  public final static int INPUT_LEGENDLOCATION = 41;//图例方位
  public final static int INPUT_COLUMNTYPE = 42;//柱图类型
  public final static int INPUT_LINETYPE = 43;//折线类型
  public final static int INPUT_PIETYPE = 44;//饼图类型
  public final static int INPUT_2AXISTYPE = 45;//双轴图类型
  public final static int INPUT_IMAGEMODE = 46;//图像格式
  public final static int INPUT_SIMPLE_ARROW = 47;//简易箭头
  
  /** 自定义下拉 */
  public final static int INPUT_CUSTOMDROPDOWN = 50;
  /** 选择文件名 */
  public final static int INPUT_FILE = 51;
  /**图元类型*/
  public final static int INPUT_POINTERTYPE = 52;
  
  
//  条形码相关属性
  public final static int INPUT_BARTYPE = 60;
  public final static int INPUT_CHARSET = 61;
  public final static int INPUT_RECERROR = 62;
  

  public final static int COORDINATES_CARTESIAN = 0; //笛卡尔坐标系，即直角坐标系
  public final static int COORDINATES_POLAR = 1; //极坐标系
  public final static int COORDINATES_CARTE_3D = 2; //立体展现直角坐标系
  public final static int COORDINATES_CARTE_VIRTUAL_3D = 3; //立体效果的平面直角坐标系
  public final static int COORDINATES_POLAR_3D = 4; //立体展现极坐标系
  public final static int COORDINATES_POLAR_VIRTUAL_3D = 5; //立体效果的平面极坐标系
  public final static int COORDINATES_LEGEND = 6; //图例坐标系，用于自定义图例绘制
  public final static int COORDINATES_FREE = 9;

  public final static int AXIS_LOC_H = 1;
  public final static int AXIS_LOC_V = 2;
  public final static int AXIS_LOC_POLAR = 3;
  public final static int AXIS_LOC_ANGLE = 4;
  public final static int AXIS_LOC_3D = 5;

  /** 网格线位置 -- 数值轴 */
  public static final int GRID_VALUE = 0;
  /** 网格线位置 -- 分类轴 */
  public static final int GRID_CATEGORY = 1;
  /** 网格线位置 -- 全部*/
  public static final int GRID_BOTH = 2;

  //注意：线型由线形与箭头相加得来
  //线形
  public final static int LINE_NONE = IStyle.LINE_NONE; //无
  public final static int LINE_SOLID = IStyle.LINE_SOLID; //实线
  public final static int LINE_DASHED = IStyle.LINE_DASHED; //虚线
  public final static int LINE_DOTTED = IStyle.LINE_DOT; //点线
  public final static int LINE_DOUBLE = IStyle.LINE_DOUBLE; //双实线
  public final static int LINE_DOTDASH = IStyle.LINE_DOTDASH; //点划线

  //线的箭头只能按百的整数倍定义，后面有取byte动作，会截掉十位及个位
  public final static int LINE_ARROW_NONE = 0x0; //无
  public final static int LINE_ARROW = 0x100; //单箭头
  public final static int LINE_ARROW_BOTH = 0x200; //双箭头
  public final static int LINE_ARROW_HEART = 0x300; //心状头
  public final static int LINE_ARROW_CIRCEL = 0x400; //圆状头
  public final static int LINE_ARROW_DIAMOND = 0x500; //菱状头
  public final static int LINE_ARROW_L = 0x600; //左单箭头

  /**----------------------刻度线位置----------------------------*/
  public final static int TICK_RIGHTUP = 0; // 靠右或上
  public final static int TICK_LEFTDOWN = 1; // 靠左或下
  public final static int TICK_CROSS = 2; // 压轴
  public final static int TICK_NONE = 3; // 无刻度线

  //点型
  public final static int PT_NONE = 0; //无
  public final static int PT_CIRCLE = 1; //圆
  public final static int PT_SQUARE = 2; //正方形
  public final static int PT_TRIANGLE = 3; //三角形
  public final static int PT_RECTANGLE = 4; //长方形
  public final static int PT_STAR = 5; //星形
  public final static int PT_DIAMOND = 6; //菱形
  public final static int PT_CORSS = 7; //叉形
  public final static int PT_PLUS = 8; //加号
  public final static int PT_D_CIRCEL = 9; //双圆
  public final static int PT_D_SQUARE = 10; //双正方形
  public final static int PT_D_TRIANGLE = 11; //双三角形
  public final static int PT_D_RECTANGLE = 12; //双长方形
  public final static int PT_D_DIAMOND = 13; //双菱形
  public final static int PT_CIRCLE_PLUS = 14; //圆内加号
  public final static int PT_SQUARE_PLUS = 15; //方内加号
  public final static int PT_TRIANGLE_PLUS = 16; //三角内加号
  public final static int PT_RECTANGLE_PLUS = 17; //长方形内加号
  public final static int PT_DIAMOND_PLUS = 18; //菱内加号
  public final static int PT_DOT = 19; //实心点,不使用填充色，只用前景色

  //柱型
  public final static int COL_COBOID = 1; //方柱
  public final static int COL_CUBE = 2; //立体方柱
  public final static int COL_CYLINDER = 3; //圆柱
//  public final static int COL_CONE = 4; //圆锥


  public final static int PATTERN_DEFAULT = 0; //填充图案，全填充
  public final static int PATTERN_H_THIN_LINE = 1; //填充图案，水平细线
  public final static int PATTERN_H_THICK_LINE = 2; //填充图案，水平粗线
  public final static int PATTERN_V_THIN_LINE = 3; //填充图案，垂直细线
  public final static int PATTERN_V_THICK_LINE = 4; //填充图案，垂直粗线
  public final static int PATTERN_THIN_SLASH = 5; //填充图案，细斜线
  public final static int PATTERN_THICK_SLASH = 6; //填充图案，粗斜线
  public final static int PATTERN_THIN_BACKSLASH = 7; //填充图案，细反斜线
  public final static int PATTERN_THICK_BACKSLASH = 8; //填充图案，粗反斜线
  public final static int PATTERN_THIN_GRID = 9; //填充图案，细网格
  public final static int PATTERN_THICK_GRID = 10; //填充图案，粗网格
  public final static int PATTERN_THIN_BEVEL_GRID = 11; //填充图案，细斜网格
  public final static int PATTERN_THICK_BEVEL_GRID = 12; //填充图案，粗斜网格
  public final static int PATTERN_DOT_1 = 13; //填充图案，稀疏点
  public final static int PATTERN_DOT_2 = 14; //填充图案，较稀点
  public final static int PATTERN_DOT_3 = 15; //填充图案，较密点
  public final static int PATTERN_DOT_4 = 16; //填充图案，稠密点
  public final static int PATTERN_SQUARE_FLOOR = 17; //填充图案，正方块地板砖
  public final static int PATTERN_DIAMOND_FLOOR = 18; //填充图案，菱形地板砖
  public final static int PATTERN_BRICK_WALL = 19; //填充图案，砖墙

  //字体风格，各位为1为表示是，为0表示否
  public final static int FONT_BOLD = 1; //黑体
  public final static int FONT_ITALIC = 2; //斜体
  public final static int FONT_UNDERLINE = 4; //下划线
  public final static int FONT_VERTICAL = 8; //竖排


  public final static int NUNIT_NONE = 0; //无，1
  public final static int NUNIT_HUNDREDS = 2; //百，10^2
  public final static int NUNIT_THOUSANDS = 3; //千，10^3
  public final static int NUNIT_TEN_THOUSANDS = 4; //万，10^4
  public final static int NUNIT_HUNDRED_THOUSANDS = 5; //十万，10^5
  public final static int NUNIT_MILLIONS = 6; //百万，10^6
  public final static int NUNIT_TEN_MILLIONS = 7; //千万，10^7
  public final static int NUNIT_HUNDRED_MILLIONS = 8; //亿，10^8
  public final static int NUNIT_THOUSAND_MILLIONS = 9; //十亿，10^9
  public final static int NUNIT_BILLIONS = 12; //万亿，10^12

  public final static int LEGEND_RECT = 1;
  public final static int LEGEND_POINT = 2;
  public final static int LEGEND_LINE = 3;
  public final static int LEGEND_LINEPOINT = 4;
  public final static int LEGEND_NONE = 5;

  /**----------------------水平对齐取值----------------------------*/
  public final static int HALIGN_LEFT = IStyle.HALIGN_LEFT; // 左对齐
  public final static int HALIGN_CENTER = IStyle.HALIGN_CENTER; // 中对齐
  public final static int HALIGN_RIGHT = IStyle.HALIGN_RIGHT; // 右对齐

  /**----------------------垂直对齐取值----------------------------*/
  public final static int VALIGN_TOP = IStyle.VALIGN_TOP; // 靠上
  public final static int VALIGN_MIDDLE = IStyle.VALIGN_MIDDLE; // 居中
  public final static int VALIGN_BOTTOM = IStyle.VALIGN_BOTTOM; // 靠下

  public final static int LOCATION_LT = HALIGN_LEFT + VALIGN_TOP; //左上角
  public final static int LOCATION_LM = HALIGN_LEFT + VALIGN_MIDDLE; //左边中心点
  public final static int LOCATION_LB = HALIGN_LEFT + VALIGN_BOTTOM; //左下角
  public final static int LOCATION_CT = HALIGN_CENTER + VALIGN_TOP; //上边中心点
  public final static int LOCATION_CM = HALIGN_CENTER + VALIGN_MIDDLE; //中心点
  public final static int LOCATION_CB = HALIGN_CENTER + VALIGN_BOTTOM; //下边中心点
  public final static int LOCATION_RT = HALIGN_RIGHT + VALIGN_TOP; //右上角
  public final static int LOCATION_RM = HALIGN_RIGHT + VALIGN_MIDDLE; //右边中心点
  public final static int LOCATION_RB = HALIGN_RIGHT + VALIGN_BOTTOM; //右下角


  //日期单位
  public final static int DATEUNIT_YEAR = 1; //年
  public final static int DATEUNIT_MONTH = 2; //月
  public final static int DATEUNIT_DAY = 3; //日
  public final static int DATEUNIT_HOUR = 4; //时
  public final static int DATEUNIT_MINUTE = 5; //分
  public final static int DATEUNIT_SECOND = 6; //秒
  public final static int DATEUNIT_MILLISECOND = 7; //毫秒

  /** 图形格式 -- JPG  */
  public static final byte IMAGE_JPG = (byte) 1;
  public static final byte IMAGE_GIF = (byte) 2;
  public static final byte IMAGE_PNG = (byte) 3;
  public static final byte IMAGE_FLASH = (byte) 4;
  public static final byte IMAGE_SVG = (byte) 5;
  public static final byte IMAGE_TIFF = (byte) 6;
  
  //堆积类型
  public final static int STACK_NONE = 0; //不堆积
  public final static int STACK_PERCENT = 1; //百分比堆积
  public final static int STACK_VALUE = 2; //原值堆积
  
  
//条形码类型
	public static final int TYPE_NONE = 0;
	public static final int TYPE_CODABAR = 1;
	public static final int TYPE_CODE39 = 2;
	public static final int TYPE_CODE128 = 3;
	public static final int TYPE_CODE128A = 4;
	public static final int TYPE_CODE128B = 5;
	public static final int TYPE_CODE128C = 6;
	
	public static final int TYPE_EAN13 = 7;
	public static final int TYPE_EAN8 = 8;
	public static final int TYPE_UPCA = 9;
	public static final int TYPE_ITF = 23;
	public static final int TYPE_PDF417 = 25;
	public static final int TYPE_QRCODE = 256;

//	背景图元填充模式
	public static final int MODE_NONE = 0; // 缺省
	public static final int MODE_FILL = 1; // 填充
	public static final int MODE_TILE = 2; // 平铺
	
	//双轴图形时，可以指定系列在哪个轴上
	public static byte AXIS_LEFT = 1;
	public static byte AXIS_RIGHT = 2;

	//属性对应图例绘制类型
	public static final byte LEGEND_P_LINECOLOR = 1;//线颜色
	public static final byte LEGEND_P_MARKERSTYLE = 2;//形状
	public static final byte LEGEND_P_FILLCOLOR = 3;//填充色
	
  }
