package com.scudata.cellset.graph.draw;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.text.*;

import com.scudata.cellset.graph.config.*;
import com.scudata.chart.Consts;

/**
 * 图形绘制时，所有属性计算，或者扩展后的图形参数对象
 * @author Joancy
 *
 */
public class GraphParam {
	public GraphFontView GFV_TITLE;
	public GraphFontView GFV_VALUE;
	public GraphFontView GFV_LEGEND;
	public GraphFontView GFV_XTITLE;
	public GraphFontView GFV_XLABEL;
	public GraphFontView GFV_YTITLE;
	public GraphFontView GFV_YLABEL;

	public Color graphBackColor = new Color(255, 255, 255); /* 图象背景颜色 */
	public Color coorColor = new Color(0, 0, 0); /* 坐标轴颜色 */
	public Color gridColor = new Color(0, 0, 0); /* 网格颜色 */
	public byte imageFormat = 0;
	public Vector catNames; /* 类别名称 */
	public Vector serNames; /* 序列名称 */
	public Vector serNames2; /* 序列2名称 */
	public int catNum; // 分类的catNames.size(),程序中大量用到该值,干脆给个地方存着
	public int serNum;
	public int serNum2;
	public double maxValue = 0.0; /* 最大值 */
	public double minValue = 0.0; /* 最小值 */
	public double interval = 0.0; /* 统计值间隔 */
	public double maxValue2 = 0; /* 最大值 */
	public double minValue2 = 0; /* 最小值 */
	public double interval2 = 0.0; /* 统计值间隔 */
	public int minTicknum = 1; /* 最小值轴标度数 */
	public int minTicknum2 = 1; /* 最小值轴标度数 */
	public int graphXInterval = 0;
	public byte timeScale = 0;
	public double scaleMark = 1; /* 值坐标比例标注 */
	public boolean drawLineDot = true; /* 是否标注直线图的矩形方框 */
	public boolean isOverlapOrigin = false; /* 原点重合 */
	public boolean drawLineTrend = false; /* 是否画直线图的前后趋势 */
	private byte lineThick = 1; /* 折线图的粗细度 */

	public boolean cutPie = true; /* 是否切割饼图的一块出来 */
	public boolean isMeterColorEnd = true; /* 仪表盘刻度位于颜色末端 */

	public int graphMargin = -1; /* 统计图与标题或者图样的边框，主要用于标注值标签的时候留出位置,-1表示没有设置边距 */
	public double barDistance = 0.0; /* 柱形图或条形图间距 */
	public int gridLineLocation = Consts.GRID_VALUE; /* 网格线位置 */
	public int gridLineStyle; /* 网格线风格 */
	public int dispValueType = 0; // 是否在柱形图顶部显示值标识
	public int dispValueType2 = 0; // 是否在柱形图顶部显示值标识
	public boolean graphTransparent = false; /* 立体图像是否透明 */

	public String dataMarkFormat = ""; // 图中数据值标识的格式
	public String dataMarkFormat2 = ""; // 图中数据值标识的格式2
	public boolean dispValueOntop = false; // 是否在柱形图顶部显示值标识
	public boolean dispValueOntop2 = false; // 是否在柱形图顶部显示值标识
	public boolean dispStackSumValue = false; // 是否在堆栈图顶部显示统计值
	public boolean dispIntersectValue = true; /* 显示重叠的数值 */
	public boolean gradientColor = true; /* 颜色渐变 */
	public double maxPositive = 0.0; /* 最大值 */
	public double minNegative = 0.0; /* 最小值 */

	public int leftMargin = 10; // 左边距 常量,将来可以扩充为属性
	public int rightMargin = 10; /* 右边距 */
	public int topMargin = 10; /* 上边距 */
	public int bottomMargin = 10; /* 下边距 */
	public int tickLen = 4; /* 刻度长度 */
	public int coorWidth = 100; /* 3D轴宽度占序列宽度的百分比 */
	public double categorySpan = 190; /* 类别间的间隔占序列宽度的百分比 */
	public int seriesSpan = 100; /* 序列间的间隔占序列深度的百分比 */
	public int pieRotation = 50; /* 纵轴占横轴的长度百分比 */
	public int pieHeight = 70; /* 饼型图的高度占半径的百分比<=100 */
	public boolean isDrawTable = false,isDataCenter=false; /* 绘制数据表 */
	public int meterRainbowEdge = 16; /* 仪表盘着色区占半径比值，范围0~100 */
	public int meter3DEdge = 8; /* 3D仪表盘边框占半径比值，范围0~100 */
	public int pieLine = 8; /* 饼图连接线占半径比值，范围0~100 */

	// **********************此标注以上的属性均需要从ExtGraphProperty初始化过来,以下为画图过程中用到的中间变量
	public Rectangle2D.Double graphRect, gRect1, gRect2; /* 坐标区域 */
	public double coorScale = 1.0; /* 上述Mark为Auto时,计算实际缩放比例且放到这个变量 */
	public double coorScale2 = 1.0; /* 值坐标比例 */
	public Vector coorValue = new Vector(); /* 值数据,如果是时间序列图，则为x轴时间刻度值 */
	public Vector coorValue2 = new Vector(); /* 值数据,如果是时间序列图，则为x轴时间刻度值 */
	public int tickNum = 10; /* 值轴标度数 */
	public int tickNum2 = 10; /* 值轴标度数 */
	public double valueBaseLine = 0; /* 值轴基线 */
	public double baseValue = 0; /* 如果以最小值作为画图的基线，则该值用于存储最小值 */
	public double baseValue2 = 0; /* 如果以最小值作为画图的基线，则该值用于存储最小值 */
	public int graphWidth = 640; /* 图象宽度 */
	public int graphHeight = 480; /* 图象高度 */
	public int legendBoxWidth = 0; /* 图例宽度 */
	public int legendBoxHeight = 0; /* 图例高度 */

	public boolean isMultiSeries = false; /* 是否有多个系列值 */
	public Date stateBegin = java.sql.Timestamp.valueOf("2999-01-01 00:00:00");
	public Date stateEnd = java.sql.Timestamp.valueOf("1900-01-01 00:00:00");

	public int topInset = 0; // 图象上边距
	public int bottomInset = 0; /* 图象下边距 */
	public int leftInset = 0; /* 图象左边距 */
	public int rightInset = 0; /* 图象右边距 */
	public int statusBarHeight = 10; /* 状态图条条高度 */

	//此标注以下为没有编辑的属性,将来可能扩充为属性
	public Color lineColor = Color.lightGray; /* 轮郭线颜色 */

	/**
	 * 设置统计图背景色
	 * 
	 * @param rgb
	 *            统计图背景色
	 */

	public void setBackColor(int rgb) {
		// 透明
		if (rgb == 16777215 && imageFormat != IGraphProperty.IMAGE_JPG) {
			graphBackColor = null;
		} else {
			graphBackColor = new Color(rgb);
		}
	}

	/**
	 * 设置折线图的粗度
	 * @param thick 粗度
	 */
	public void setLineThick(byte thick) {
		this.lineThick = thick;
	}

	/**
	 * 去直线粗度
	 * @return 粗度值
	 */
	public float getLineThick() {
		return getLineThick(lineThick);
	}

	/**
	 * 将线粗转换为byte类型
	 * @return byte类型的粗度值
	 */
	public byte getLineThickByte() {
		return lineThick;
	}

	/**
	 * 将报表中定义的thickDefine转换为实际画图时的线粗度
	 * 
	 * @param thickDefine
	 *            byte
	 * @return float
	 */
	public static float getLineThick(byte thickDefine) {
		float thick = 0.1f;
		switch (thickDefine) {
		case 0:
			return 0;
		case 1:
			thick = 0.1f;
			break;
		default:
			thick = thickDefine - 1.0f;
		}
		return thick;
	}

	/**
	 * 设置统计图的宽高
	 * @param w 宽度
	 * @param h 高度
	 */
	public void setGraphWH(int w, int h) {
		if (w > 0) {
			graphWidth = w;
		}
		if (h > 0) {
			graphHeight = h;
		}
	}

	/**
	 * 将读数转换为中文大写读法
	 * @param dd 数据
	 * @return 中文读数
	 */
	public static String xToChinese(double dd) {
		try {
			String s = "零壹贰叁肆伍陆柒捌玖";
			String s1 = "拾佰仟万拾佰仟亿拾佰仟万";
			String m;
			int j;
			StringBuffer k = new StringBuffer();
			m = String.valueOf(Math.round(dd));
			for (j = m.length(); j >= 1; j--) {
				char n = s.charAt(Integer.parseInt(m.substring(m.length() - j,
						m.length() - j + 1)));
				if (n == '零' && k.charAt(k.length() - 1) == '零') {
					continue;
				}
				k.append(n);
				if (n == '零') {
					continue;
				}
				int u = j - 2;
				if (u >= 0) {
					k.append(s1.charAt(u));
				}
				if (u > 3 && u < 7) {
					k.append('万');
				}
				if (u > 7) {
					k.append('亿');
				}
			}
			if (k.length() > 0 && k.charAt(k.length() - 1) == '零') {
				k.deleteCharAt(k.length() - 1);
			}
			if (k.length() > 0 && k.charAt(0) == '壹') {
				k.deleteCharAt(0);
			}
			return k.toString();
		} catch (Exception x) {
			DecimalFormatSymbols dfs = new DecimalFormatSymbols(
					Locale.getDefault());
			DecimalFormat df = new DecimalFormat("#.#E0", dfs);
			return df.format(dd);
		}
	}

}
