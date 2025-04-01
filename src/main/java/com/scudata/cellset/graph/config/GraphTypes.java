package com.scudata.cellset.graph.config;

/**
 * 定义统计图类型常量
 */
public class GraphTypes {
	/**
	 * 区域图
	 */
	public static final byte GT_AREA = ( byte ) 1;

	/**
	 * 条形图
	 */
	public static final byte GT_BAR = ( byte ) 2;

	/**
	 * 三维条形图
	 */
	public static final byte GT_BAR3D = ( byte ) 3;

	/**
	 * 三维簇状条形图
	 */
	public static final byte GT_BAR3DOBJ = ( byte ) 4;

	/**
	 * 堆积条形图
	 */
	public static final byte GT_BARSTACKED = ( byte ) 5;

	/**
	 * 三维堆积条形图
	 */
	public static final byte GT_BARSTACKED3DOBJ = ( byte ) 6;

	/**
	 * 柱形图
	 */
	public static final byte GT_COL = ( byte ) 7;

	/**
	 * 三维柱形图
	 */
	public static final byte GT_COL3D = ( byte ) 8;

	/**
	 * 三维簇状柱形图
	 */
	public static final byte GT_COL3DOBJ = ( byte ) 9;

	/**
	 * 堆积柱形图
	 */
	public static final byte GT_COLSTACKED = ( byte ) 10;

	/**
	 * 三维堆积柱形图
	 */
	public static final byte GT_COLSTACKED3DOBJ = ( byte ) 11;

	/**
	 * 折线图
	 */
	public static final byte GT_LINE = ( byte ) 12;

	/**
	 * 饼型图
	 */
	public static final byte GT_PIE = ( byte ) 13;

	/**
	 * 散列图
	 */
	public static final byte GT_SCATTER = ( byte ) 14;

	/**
	 * 三维区域图
	 */
	public static final byte GT_AREA3D = ( byte ) 15;

	/**
	 * 三维折线图
	 */
	public static final byte GT_LINE3DOBJ = ( byte ) 16;

	/**
	 * 三维饼型图
	 */
	public static final byte GT_PIE3DOBJ = ( byte ) 17;

	/**
	 * 时序状态图
	 */
	public static final byte GT_TIMESTATE = ( byte ) 18;

	/**
	 * 时间走势图
	 */
	public static final byte GT_TIMETREND = ( byte ) 19;

	/**
	 * 双轴柱线图
	 */
	public static final byte GT_2YCOLLINE = ( byte ) 21;

	/**
	 * 双轴折线图
	 */
	public static final byte GT_2Y2LINE = ( byte ) 20;

	/**
	 * 雷达图
	 */
	public static final byte GT_RADAR = ( byte ) 22;

	/**
	 * 甘特图
	 */
	public static final byte GT_GANTT = ( byte ) 23;

	/**
	 * 仪表盘
	 */
	public static final byte GT_METER = ( byte ) 24;

	/**
	 * 里程碑
	 */
	public static final byte GT_MILEPOST = ( byte ) 25;

	/**
	 * 全距图
	 */
	public static final byte GT_RANGE = ( byte ) 26;

	/**
	 * 工字图
	 */
	public static final byte GT_GONGZI = ( byte ) 27;

	/**
	 * 3D仪表盘
	 */
	public static final byte GT_METER3D = ( byte ) 28;

	/**
	 * 曲线图
	 */
	public static final byte GT_CURVE = ( byte ) 29;
	
	/**
	 * 双轴堆积柱线图
	 */
	public static final byte GT_2YCOLSTACKEDLINE = ( byte ) 30;
	
	/**
	 * 三维点图，气泡图
	 */
	public static final byte GT_DOT3D = ( byte ) 31;
	
	/**
	 * 自定义统计图
	 */
//	public static final byte GT_CUSTOM = ( byte )0xFF;
}
