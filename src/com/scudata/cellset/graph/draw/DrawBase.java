package com.scudata.cellset.graph.draw;

import java.awt.*;
import java.util.*;
import java.text.*;
import java.awt.geom.*;

import com.scudata.cellset.IColCell;
import com.scudata.cellset.INormalCell;
import com.scudata.cellset.IRowCell;
import com.scudata.cellset.IStyle;
import com.scudata.cellset.datamodel.CellSet;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.cellset.graph.GraphProperty;
import com.scudata.cellset.graph.PublicProperty;
import com.scudata.cellset.graph.config.*;
import com.scudata.chart.ChartColor;
import com.scudata.chart.Consts;
import com.scudata.chart.CubeColor;
import com.scudata.chart.Utils;
import com.scudata.common.*;
import com.scudata.ide.common.*;
import com.scudata.ide.common.control.*;
import com.scudata.util.*;
/**
 * 所有绘图类的抽象基础类
 * 封装了图形的公共属性以及一些公共画图函数
 * @author Joancy
 *
 */
public abstract class DrawBase implements IGraph {
	public ExtGraphProperty egp;
	public Graphics2D g;
	public GraphParam gp;
	public Palette palette;
	public int VALUE_RADIUS = 2; // 直线图画值标识的小矩形或圆圈的半径
	public int SHADE_SPAN = 4;
	public Rectangle TR = new Rectangle(); // Text Rectangle 文本输出时的矩形宽高，x,y无意义
	public ArrayList<ValueLabel> labelList = new ArrayList<ValueLabel>();
	public ArrayList<ValuePoint> pointList = new ArrayList<ValuePoint>();

	private boolean disableLink = true;
	private transient Point2D.Double vShift, hShift;// 纵轴因为透明或者3D平台时，因为位置不同的x,y向偏移量，纵向总是减去偏移；横向总是加上偏移
	// 标题加上对齐属性后，需要先保留出空间，最后根据gp.gRect2来绘制对齐
	private ValueLabel vlTitle, vlYTitle1, vlYTitle2, vlXTitle;

	protected boolean isSVG() {
		return egp.getImageFormat() == IGraphProperty.IMAGE_SVG;
	}

	Point2D.Double getVTickPoint(double shift) {
		double x = gp.gRect1.x - vShift.x;
		double y = gp.gRect1.y + gp.gRect1.height - shift - vShift.y;
		return new Point2D.Double(x, y);
	}

	Point2D.Double getHTickPoint(double shift) {
		double x = gp.gRect1.x + shift + hShift.x;
		double y = gp.gRect1.y + gp.gRect1.height + hShift.y;
		return new Point2D.Double(x, y);
	}
/**
 * 设置禁止超链接
 * @param b 禁止超链接
 */
	public void setDisableLink(boolean b) {
		disableLink = b;
	}

	private boolean disableLink() {
		return disableLink;
	}

	/**
	 * 绘图中的文本收集到缓冲后，最后集中绘制
	 * 防止文本被覆盖
	 */
	public void outLabels() {
		double x, y;
		String text;
		byte direction;
		Color c;
		for (int i = 0; i < labelList.size(); i++) {
			ValueLabel vl = (ValueLabel) labelList.get(i);
			x = vl.p.x;
			y = vl.p.y;
			text = vl.text;
			direction = vl.direction;
			c = vl.c;
			if (gp.GFV_VALUE.color.getAlpha() == 0) {
				gp.GFV_VALUE.outText(x, y, text, direction, c);
			} else {
				gp.GFV_VALUE.outText(x, y, text, direction);
			}
		}
		labelList.clear();

		double[] buf;
		if (vlTitle != null) {
			buf = getHTitleX(gp.gRect2, egp.getGraphTitleAlign());
			TR = gp.GFV_TITLE.getTextSize();
			x = buf[0];
			y = vlTitle.p.y - TR.height;
			text = vlTitle.text;
			direction = (byte) buf[1];
			gp.GFV_TITLE.outText(x, y, text, direction);
			vlTitle = null;//这里要清掉，下同。原因为：双轴图时，绘制两遍，位置稍有偏移，造成加粗。
		}

		if (vlXTitle != null) {
			buf = getHTitleX(gp.gRect1, egp.getXTitleAlign());
			TR = gp.GFV_XTITLE.getTextSize();
			x = buf[0];
			y = vlXTitle.p.y - TR.height/2;
			text = vlXTitle.text;
			direction = (byte) buf[1];
			gp.GFV_XTITLE.outText(x, y, text, direction);
			vlXTitle = null;
		}

		if (vlYTitle1 != null) {
			x = vlYTitle1.p.x;
			y = getVTitleY(gp.gRect1, egp.getYTitleAlign());
			text = vlYTitle1.text;
			direction = GraphFontView.TEXT_ON_RIGHT;
			gp.GFV_YTITLE.outText(x, y, text, direction);
			vlYTitle1 = null;
		}

		if (vlYTitle2 != null) {
			x = vlYTitle2.p.x;
			y = getVTitleY(gp.gRect2, egp.getYTitleAlign());
			text = vlYTitle2.text;
			direction = GraphFontView.TEXT_ON_LEFT;
			gp.GFV_YTITLE.outText(x, y, text, direction);
			vlYTitle2 = null;
		}
	}

	private double[] getHTitleX(Rectangle2D.Double rect,byte align) {
		double x[] = new double[2];
		if (align == IStyle.HALIGN_LEFT) {
			x[0] = rect.x;
			x[1] = GraphFontView.TEXT_ON_RIGHT;
		} else if (align == IStyle.HALIGN_RIGHT) {
			x[0] = rect.x + gp.gRect2.width;
			x[1] = GraphFontView.TEXT_ON_LEFT;
		} else {
			x[0] = rect.x + gp.gRect2.width / 2;
			x[1] = GraphFontView.TEXT_ON_CENTER;
		}
		return x;
	}

	private double getVTitleY(Rectangle2D.Double rect, byte align) {
		double y;
		if (align == IStyle.VALIGN_TOP) {
			y = rect.y;
		} else if (align == IStyle.VALIGN_BOTTOM) {
			y = rect.y + gp.gRect2.height;
		} else {
			y = rect.y + gp.gRect2.height / 2;
		}
		return y;
	}

	protected int getPointRadius() {
		int radius = gp.getLineThickByte();
		if (radius < 2)
			return 2;
		return radius;
	}

	/**
	 * 类似于文本，数据点也在绘制过程中置于缓存
	 * 用该方法集中绘制数据点
	 */
	public void outPoints() {
		int bs = Consts.LINE_SOLID;
		float bw = 1;
		boolean drawShade = false;// 折线图绘制时，不再画点的阴影，否则后绘制的阴影会盖住先画的直线
		float transparent = getTransparent();

		for (int i = 0; i < pointList.size(); i++) {
			ValuePoint vp = pointList.get(i);
			Point2D.Double p = vp.p;
			int radius;
			if (vp.radius < 0) {
				radius = getPointRadius();
			} else {
				radius = vp.radius;
			}
			Color fillColor;
			if (vp.fillColor == null) {
				fillColor = egp.getAxisColor(GraphProperty.AXIS_COLBORDER);
			} else {
				fillColor = vp.fillColor;
			}

			if (drawShade) {
				Utils.drawCartesianPoint1(g, p, vp.shape, radius, radius,
						radius, bs, bw, transparent);
			}
			Utils.drawCartesianPoint2(g, p, vp.shape, radius, radius, radius,
					bs, bw, getChartColor(fillColor), vp.borderColor,
					transparent);
		}
	}

	/**
	 * 设置绘图的图形设备
	 * @param g 图形设备
	 */
	public void setGraphics2D(Graphics2D g) {
		this.g = g;
	}

	/**
	 * 设置统计图的大小
	 * @param w 图形宽度
	 * @param h 图形高度
	 */
	public void setGraphWH(int w, int h) { 
		gp.setGraphWH(w, h);
	}

	/**
	 * 根据扩展图形属性的设置，产生绘图的后代实例
	 * @param egp 扩展图形属性
	 * @return 后代绘图实例
	 */
	public static Object getInstance(ExtGraphProperty egp) {
		DrawBase graph = null;
		switch (egp.getType()) {
		case GraphTypes.GT_BAR:
			graph = new DrawBar();
			break;
		case GraphTypes.GT_BAR3D:
		case GraphTypes.GT_BAR3DOBJ:
			graph = new DrawBar3DObj();
			break;
		case GraphTypes.GT_BARSTACKED:
			graph = new DrawBarStacked();
			break;
		case GraphTypes.GT_BARSTACKED3DOBJ:
			graph = new DrawBarStacked3DObj();
			break;
		case GraphTypes.GT_COL:
			graph = new DrawCol();
			break;
		case GraphTypes.GT_COL3D:
			graph = new DrawCol3D();
			break;
		case GraphTypes.GT_COL3DOBJ:
			graph = new DrawCol3DObj();
			break;
		case GraphTypes.GT_COLSTACKED:
			graph = new DrawColStacked();
			break;
		case GraphTypes.GT_COLSTACKED3DOBJ:
			graph = new DrawColStacked3DObj();
			break;
		case GraphTypes.GT_LINE:
			graph = new DrawLine();
			break;
		case GraphTypes.GT_CURVE:
			graph = new DrawCurve();
			break;
		case GraphTypes.GT_LINE3DOBJ:
			graph = new DrawLine3DObj();
			break;
		case GraphTypes.GT_PIE:
			graph = new DrawPie();
			break;
		case GraphTypes.GT_PIE3DOBJ:
			graph = new DrawPie3DObj();
			break;
		case GraphTypes.GT_AREA:
			graph = new DrawArea();
			break;
		case GraphTypes.GT_AREA3D:
			graph = new DrawArea3D();
			break;
		case GraphTypes.GT_SCATTER:
			graph = new DrawDot();
			break;
		case GraphTypes.GT_2YCOLLINE:
			graph = new Draw2YColLine();
			break;
		case GraphTypes.GT_2YCOLSTACKEDLINE:
			graph = new Draw2YColStackedLine();
			break;
		case GraphTypes.GT_2Y2LINE:
			graph = new Draw2Y2Line();
			break;
		case GraphTypes.GT_RADAR:
			graph = new DrawRadar();
			break;
		default:
			graph = new DrawCol();
		}
		graph.transProperty(egp);
		graph.VALUE_RADIUS = graph.getPointRadius();

		return graph;
	}

	/**
	 * 根据属性是否使用渐变色，将颜色c转换为填充颜色类
	 * @param c 颜色
	 * @return 填充颜色ChartColor对象
	 */
	public ChartColor getChartColor(Color c) {
		ChartColor cc = new ChartColor(c);
		if (egp.isGradientColor()) {// 如果是渐变色，设置为ChartColor的炫模式
			cc.setColor1(c);
			cc.setColor2(c);
			cc.setGradient(true);
		} else {
			cc.setGradient(false);
		}
		return cc;
	}

	/**
	 * 获取调色板中的第index序号颜色值
	 * @param index 序号
	 * @return 颜色值
	 */
	public Color getColor(int index) {
		return new Color(palette.getColor(index));
	}

	/**
	 * 取当前图形的透明度
	 * @return
	 */
	public float getTransparent() {
		if (egp.isGraphTransparent())
			return 0.6f;
		return 1f;
	}

	/**
	 * 统计图根据gp属性进行绘图，将扩展图形属性egp转换为
	 * 绘图的属性
	 * @param egp 扩展图形属性
	 */
	public void transProperty(ExtGraphProperty egp) {
		this.egp = egp;
		this.gp = new GraphParam();
		palette = egp.getPlatte();
		gp.catNames = egp.getCategoryNames(); // 转换字体的时候要用到catNames,得先转换
		gp.serNames = egp.getSeriesNames(egp.categories);
		gp.catNum = gp.catNames.size();
		gp.serNum = gp.serNames.size();

		String text = egp.getGraphTitle();
		GraphFonts fonts = egp.getFonts();
		GraphFont gf = fonts.getTitleFont();
		gp.GFV_TITLE = getGraphFontView(gf, text, GraphFontView.FONT_TITLE);

		text = "";
		gf = fonts.getLegendFont();
		gp.GFV_LEGEND = getGraphFontView(gf, text, GraphFontView.FONT_LEGEND);

		gf = fonts.getDataFont();
		gp.GFV_VALUE = getGraphFontView(gf, text, GraphFontView.FONT_VALUE);

		text = egp.getXTitle();
		gf = fonts.getXTitleFont();
		gp.GFV_XTITLE = getGraphFontView(gf, text, GraphFontView.FONT_XTITLE);

		text = egp.getYTitle();
		gf = fonts.getYTitleFont();
		gp.GFV_YTITLE = getGraphFontView(gf, text, GraphFontView.FONT_YTITLE);

		text = "";
		gf = fonts.getXLabelFont();
		gp.GFV_XLABEL = getGraphFontView(gf, text, GraphFontView.FONT_XLABEL);

		gf = fonts.getYLabelFont();
		gp.GFV_YLABEL = getGraphFontView(gf, text, GraphFontView.FONT_YLABEL);

		gp.imageFormat = egp.getImageFormat(); // 在设置如下背景色之前要求先设置格式属性,透明色跟图形格式有关系
		gp.setBackColor(egp.getGraphBackColor());
		gp.dataMarkFormat = egp.getDisplayDataFormat1();
		gp.dataMarkFormat2 = egp.getDisplayDataFormat2();
		if (!StringUtils.isValidString(gp.dataMarkFormat2)) {
			gp.dataMarkFormat2 = gp.dataMarkFormat;
		}

		if (!egp.isUserSetYEndValue1()) { // end
			gp.maxValue = egp.getMaxValue(egp.getCategories());
		} else {
			gp.maxValue = egp.getYEndValue1();
		}
		if (!egp.isUserSetYEndValue2()) {
			gp.maxValue2 = egp.getMaxValue(egp.category2);
		} else {
			gp.maxValue2 = egp.getYEndValue2();
		}

		if (egp.isUserSetYInterval1()) { // interval
			gp.interval = egp.getYInterval1();
		}
		if (egp.isUserSetYInterval2()) {
			gp.interval2 = egp.getYInterval2();
		}

		if (egp.isUserSetYMinMarks()) {
			gp.minTicknum = egp.getYMinMarks();
			gp.minTicknum2 = egp.getYMinMarks();
		}

		if (!egp.isUserSetYStartValue1()) { // start
			gp.minValue = egp.getMinValue(egp.getCategories());
		} else {
			gp.minValue = egp.getYStartValue1();
			gp.baseValue = gp.minValue;
			gp.minValue -= gp.baseValue;
			gp.maxValue -= gp.baseValue;
		}

		if (!egp.isUserSetYStartValue2()) {
			gp.minValue2 = egp.getMinValue(egp.category2);
		} else {
			gp.minValue2 = egp.getYStartValue2();
			gp.baseValue2 = gp.minValue2;
			gp.minValue2 -= gp.baseValue2;
			gp.maxValue2 -= gp.baseValue2;
		}

		if (egp.isUserSetTitleMargin()) {
			gp.graphMargin = (int) egp.getTitleMargin();
		}
		if (egp.isUserSetBarDistance()) {
			gp.barDistance = egp.getBarDistance();
		}
		if (egp.isUserSetXInterval()) {
			gp.graphXInterval = (int) egp.getXInterval();
		}
		if (egp.isUserSetStatusBarWidth()) {
			gp.statusBarHeight = egp.getStatusBarWidth();
		}

		if (egp.is2YGraph()) {
			gp.serNames2 = egp.getSeriesNames(egp.category2);
			gp.serNum2 = gp.serNames2.size();
		}
		gp.dispValueType = egp.getDisplayData();
		gp.dispValueOntop = (gp.dispValueType == IGraphProperty.DISPDATA_VALUE
				|| gp.dispValueType == IGraphProperty.DISPDATA_NAME_VALUE || gp.dispValueType == IGraphProperty.DISPDATA_TITLE);
		gp.dispValueType2 = egp.getDisplayData2();
		gp.dispValueOntop2 = (gp.dispValueType2 == IGraphProperty.DISPDATA_VALUE
				|| gp.dispValueType2 == IGraphProperty.DISPDATA_NAME_VALUE || gp.dispValueType2 == IGraphProperty.DISPDATA_TITLE);

		gp.dispStackSumValue = egp.isDispStackSumValue();
		gp.scaleMark = egp.getDataUnit();
		gp.drawLineDot = egp.isDrawLineDot();
		gp.isOverlapOrigin = egp.isOverlapOrigin();
		gp.drawLineTrend = egp.isDrawLineTrend();
		gp.setLineThick(egp.getLineThick());
		gp.cutPie = egp.isCutPie();
		gp.dispIntersectValue = egp.isShowOverlapText();
		gp.gradientColor = egp.isGradientColor();
		gp.coorColor = new Color(egp.getAxisColor());
		gp.gridColor = new Color(egp.getGridLineColor());
		gp.gridLineLocation = egp.getGridLocation();
		gp.gridLineStyle = egp.getGridLineType();
		gp.graphTransparent = egp.isGraphTransparent();
		gp.timeScale = egp.getStatusTimeType();
		gp.isDrawTable = egp.isDrawDataTable();
		gp.isDataCenter = egp.isDataCenter();
		
		gp.meter3DEdge = egp.getMeter3DEdge();
		gp.meterRainbowEdge = egp.getMeterRainbowEdge();
		gp.pieLine = egp.getPieLine();

		IGraphProperty p = egp.getIGraphProperty();
		gp.leftMargin = p.getLeftMargin();
		gp.rightMargin = p.getRightMargin();
		gp.topMargin = p.getTopMargin();
		gp.bottomMargin = p.getBottomMargin();
		gp.tickLen = p.getTickLen();
		gp.coorWidth = p.getCoorWidth();
		gp.categorySpan = p.getCategorySpan();
		gp.seriesSpan = p.getSeriesSpan();
		gp.pieRotation = p.getPieRotation();
		gp.pieHeight = p.getPieHeight();

		if (egp.isStackedGraph(this)) {
			gp.maxPositive = egp.getStackedMaxValue();
			gp.minNegative = egp.getStackedMinValue();
		}
		gp.isMultiSeries = (gp.serNum+gp.serNum2 > 1) || egp.isLegendOnSery();
	}
	
	/**
	 * 获取当前图形是否多个系列绘制，由于双轴图分为左轴系列和右轴系列，
	 * 双轴图时为多系列图形
	 * @return 是多系列图形返回true，否则返回false
	 */
	public boolean isMultiSeries(){
		if(egp.is2YGraph()){
			return true;
		}
		return gp.isMultiSeries;
	}

	protected boolean isLegendOnCategory() {
		byte type = egp.getType();
		return (type == GraphTypes.GT_PIE || type == GraphTypes.GT_PIE3DOBJ || !isMultiSeries());
	}

	// 是否画圆形图例
	protected boolean isCircleLegend() {
		byte type = egp.getType();
		// 双轴两线图形时，图例仍然得有方，有圆，区分左右轴
		boolean isCircle = ( type == GraphTypes.GT_LINE || type == GraphTypes.GT_SCATTER || type == GraphTypes.GT_DOT3D);
		return isCircle;
	}

	protected boolean isShowMeterTick(double tick, Vector values) {
		for (int i = 0; i < values.size(); i++) {
			Double val = (Double) values.get(i);
			if (val.doubleValue() == tick) {
				return true;
			}
		}
		return false;
	}

	protected void drawLegendRect(int seriesNo, int series1Count, int x, int y,
			int fontHeight, int nFontHeight, int textWidth,
			boolean verticalPillar, boolean isGongZi, String stmp,
			StringBuffer htmlBuffer) {
		int xx, yy, ww, hh = fontHeight;

		if (isCircleLegend()
				|| (egp.is2YGraph() && gp.isMultiSeries && seriesNo >= series1Count)) { // 第二轴的系列
			g.setStroke(new BasicStroke(0.00001f));
			yy = y - nFontHeight;
			setPaint(x, yy, fontHeight, fontHeight, seriesNo, verticalPillar);
			fillOval(x, yy, fontHeight, fontHeight);
			g.setColor(gp.coorColor);
			g.setStroke(new BasicStroke(0.1f));
			g.drawOval(x, yy, fontHeight, fontHeight);
		} else {
			xx = x;
			yy = y - nFontHeight;
			ww = fontHeight;
			hh = ww;

			Color tmpc = getColor(seriesNo);
			Color bc = egp.getAxisColor(IGraphProperty.AXIS_LEGEND);
			int bs = Consts.LINE_SOLID;
			float bw = 0.1f;
			Utils.draw2DRect(g, xx, yy, ww, hh, bc, bs, bw, egp.isDrawShade(),
					egp.isRaisedBorder(), getTransparent(),
					getChartColor(tmpc), !egp.isBarGraph(this));

		}

		// 超连接处理
		String coordx = x + "," + (y - nFontHeight) + ",";
		x += fontHeight + egp.getLegendHorizonGap();
		if (isGongZi) {
			stmp = stmp.substring(stmp.indexOf(".") + 1);
		}
		if (fontHeight != nFontHeight) { // 只要有折行,则需要重定位Y
			y = y - nFontHeight + fontHeight;
		}

		// 方位定位:图例定义为右侧文字,纵向居中,中心点往上挪半个图例格子高度
		y -= hh / 2;

		int wrapWidth = textWidth + 2; //折行算法要去掉两个可能是边线的点，我这不用去，多给两点
		// 折行算法减掉了wrapInchingWidth,这里不用减，得补回来

		ArrayList al = ControlUtilsBase.wrapString(stmp,
				g.getFontMetrics(gp.GFV_LEGEND.font), wrapWidth);
		for (int i = 0; i < al.size(); i++) {
			String sectText = (String) al.get(i);
			gp.GFV_LEGEND.outText(x, y, sectText);
			y += fontHeight;
		}
		coordx += (x + textWidth) + "," + y;
		if (StringUtils.isValidString(egp.getLegendLink())) {
			htmlBuffer.append(getStgLinkHtml(egp.getLegendLink(), "rect",
					coordx, egp.getLinkTarget(), stmp));
		}
	}

	protected boolean isLegendOnSide() {
		return egp.getLegendLocation() == IGraphProperty.LEGEND_LEFT
				|| egp.getLegendLocation() == IGraphProperty.LEGEND_RIGHT;
	}

	protected Rectangle getLegendTextRect(String text) {
		TR = gp.GFV_LEGEND.getTextSize(text);
		int limitWidth = 0;
		if (isLegendOnSide()) {
			limitWidth = gp.graphWidth / 3;
		} else {
			limitWidth = (int) (gp.graphWidth * 0.75f);
		}
		if (TR.getWidth() > limitWidth) {
			FontMetrics fm = g.getFontMetrics(gp.GFV_LEGEND.font);
			ArrayList al = ControlUtilsBase.wrapString(text, fm, limitWidth);
			TR.setBounds(0, 0, limitWidth, (int) (al.size() * TR.getHeight()));
		}
		return TR;
	}

	/**
	 * 绘制图形的图例，并将图例产生的超链接写入超链缓存htmlBuffer
	 * @param htmlBuffer 超链接缓存对象
	 */
	public void drawLegend(StringBuffer htmlBuffer) {
		if (egp.getLegendLocation() == IGraphProperty.LEGEND_NONE) {
			return;
		}
		int row, col;
		int x, y;
		int nFontHeight = 0, fontHeight = 0;
		int maxWidth = 0;
		int maxHeight = 0;
		int legendWidth = 0;
		int legendHeight = 0;
		int intTmp = 0;
		int borderx, bordery, borderwidth, borderheight;
		int totalWidth = 0;
		int totalHeight = 0;
		boolean isGongZi = egp.getType() == GraphTypes.GT_GONGZI;

		boolean verticalPillar = !egp.isBarGraph(this);
		/* 找出最大图例宽度 */
		if (isLegendOnCategory()) {
			int cc = gp.catNames.size();
			for (int i = 0; i < cc; i++) {
				Object o = gp.catNames.get(i);
				String tmp = Variant.toString(o);
				if (!StringUtils.isValidString(tmp)) { // 有无聊人士仅给一空串，此时仍然要保证一个字符的宽高
					tmp = "A";
				}
				TR = getLegendTextRect(tmp);
				intTmp = TR.width;
				if (maxWidth < intTmp) {
					maxWidth = intTmp;
					maxHeight = TR.height;
				}
			}
		} else {
			int ss = gp.serNames.size();
			for (int i = 0; i < ss; i++) {
				Object o = gp.serNames.get(i);
				String tmp = Variant.toString(o);
				if (isGongZi) {
					tmp = tmp.substring(tmp.indexOf(".") + 1);
				}
				if (!StringUtils.isValidString(tmp)) {
					tmp = "A";
				}
				TR = getLegendTextRect(tmp);
				intTmp = TR.width;
				if (maxWidth < intTmp) {
					maxWidth = intTmp;
					maxHeight = TR.height;
				}
			}

			if (egp.is2YGraph() || egp.isNormalStacked()) {
				if(gp.serNames2!=null){
					int ss2 = gp.serNames2.size();
					for (int i = 0; i < ss2; i++) { // 2轴的第2组系列
						Object o = gp.serNames2.get(i);
						TR = getLegendTextRect(Variant.toString(o));
						intTmp = TR.width;
						if (maxWidth < intTmp) {
							maxWidth = intTmp;
							maxHeight = TR.height;
						}
					}
				}
			}
		}

		/* 图例个数 */
		int size = 0;
		if (isLegendOnCategory()) {
			size = gp.catNames.size();
		} else {
			if (egp.is2YGraph() || (egp.isNormalStacked() && gp.serNames2!=null)) {
				size = gp.serNames.size() + gp.serNames2.size(); // 加上了第二组系列
			} else {
				size = gp.serNames.size();
			}
		}

		int series1Count = gp.serNames.size();
		int seriesNo;

		/* 如果图例位于上方或下方 */
		nFontHeight = maxHeight;
		fontHeight = gp.GFV_LEGEND.getTextSize("ABC").height;

		if (!isLegendOnSide()) {
			legendWidth = fontHeight + 3 * egp.getLegendHorizonGap() + maxWidth;
			totalWidth = gp.graphWidth - gp.leftMargin - gp.rightMargin;
			col = totalWidth / legendWidth;
			if (col < 1) {
				col = 1;
			}
			row = size / col;
			if (row * col < size) {
				row++;
			}
			int tmp = gp.graphHeight - gp.bottomInset
					- ((row + 1) * egp.getLegendVerticalGap() + row * nFontHeight);

			if (row > 1) {
				borderx = gp.leftMargin + (totalWidth - legendWidth * col) / 2;
				borderwidth = legendWidth * col;
			} else {
				borderx = gp.leftMargin + (totalWidth - legendWidth * size) / 2;
				borderwidth = legendWidth * size;
			}

			if (egp.getLegendLocation() == IGraphProperty.LEGEND_TOP) {
				bordery = gp.topInset;
			} else {
				bordery = tmp;
			}
			borderheight = nFontHeight * row + (row + 1) * egp.getLegendVerticalGap();

			drawRect(borderx, bordery, borderwidth, borderheight,
					egp.getAxisColor(IGraphProperty.AXIS_LEGEND)); // 系列外围矩形框

			gp.legendBoxWidth = borderwidth;
			gp.legendBoxHeight = borderheight;

			for (int i = 0; i < row; i++) {
				if (row > 1) {
					x = gp.leftMargin + (totalWidth - legendWidth * col) / 2;
				} else {
					x = gp.leftMargin + (totalWidth - legendWidth * size) / 2;
				}
				for (int j = 0; j < col; j++) {
					if ((i * col) + j >= size) {
						break;
					}

					if (egp.getLegendLocation() == IGraphProperty.LEGEND_TOP) {
						y = gp.topInset + (i + 1) * (egp.getLegendVerticalGap() + nFontHeight);
					} else {
						y = tmp + (i + 1) * (egp.getLegendVerticalGap() + nFontHeight);
					}

					Object o = null;
					seriesNo = i * col + j;
					if (isLegendOnCategory()) {
						o = gp.catNames.get(seriesNo);
					} else {
						if (seriesNo >= series1Count) { // 第二轴的系列
							o = gp.serNames2.get(seriesNo - series1Count);
						} else {
							o = gp.serNames.get(seriesNo);
						}

					}
					x += egp.getLegendHorizonGap();
					drawLegendRect(seriesNo, series1Count, x, y, fontHeight,
							nFontHeight, maxWidth, verticalPillar, isGongZi,
							Variant.toString(o), htmlBuffer);
					x += nFontHeight + egp.getLegendHorizonGap();
					x += maxWidth + egp.getLegendHorizonGap();
				}
			}

			if (egp.getLegendLocation() == IGraphProperty.LEGEND_TOP) {
				gp.topInset += (row + 1) * egp.getLegendVerticalGap() + row * nFontHeight + egp.getLegendVerticalGap();
			} else {
				gp.bottomInset += (row + 1) * egp.getLegendVerticalGap() + row * nFontHeight + egp.getLegendVerticalGap();
			}
		} else { /* 如果图例位于左边或右边 */
			legendWidth = fontHeight + 3 * egp.getLegendHorizonGap() + maxWidth;
			legendHeight = nFontHeight + 2 * egp.getLegendVerticalGap();
			totalHeight = gp.graphHeight - gp.topMargin - gp.bottomMargin;
			row = totalHeight / legendHeight;
			if (row < 1) {
				row = 1;
			}
			col = size / row;
			if (row * col < size) {
				col++;

			}
			int tmp = gp.graphWidth - gp.rightInset
					- ((col + 1) * egp.getLegendHorizonGap() + col * legendWidth);

			if (col > 1) {
				bordery = gp.topMargin + (totalHeight - legendHeight * row) / 2;
				borderheight = legendHeight * row;
			} else {
				bordery = gp.topMargin + (totalHeight - legendHeight * size)
						/ 2;
				borderheight = legendHeight * size;
			}
			if (egp.getLegendLocation() == IGraphProperty.LEGEND_LEFT) {
				borderx = gp.leftInset;
			} else {
				borderx = tmp;
			}
			borderwidth = legendWidth * col;

			drawRect(borderx, bordery, borderwidth, borderheight,
					egp.getAxisColor(IGraphProperty.AXIS_LEGEND));

			gp.legendBoxWidth = borderwidth;
			gp.legendBoxHeight = borderheight;

			for (int j = 0; j < col; j++) {
				if (col > 1) {
					y = gp.topMargin + (totalHeight - legendHeight * row) / 2
							+ nFontHeight + egp.getLegendVerticalGap();
				} else {
					y = gp.topMargin + (totalHeight - legendHeight * size) / 2
							+ nFontHeight + egp.getLegendVerticalGap();
				}
				for (int i = 0; i < row; i++) {
					if ((j * row) + i >= size) {
						break;
					}

					if (egp.getLegendLocation() == IGraphProperty.LEGEND_LEFT) {
						x = gp.leftInset + j * legendWidth;
					} else {
						x = tmp + j * legendWidth;
					}

					Object o = null;
					seriesNo = j * row + i;
					if (isLegendOnCategory()) {
						o = gp.catNames.get(seriesNo);
					} else {
						if (seriesNo >= series1Count) { // 第二轴系列
							o = gp.serNames2.get(seriesNo - series1Count);
						} else {
							o = gp.serNames.get(seriesNo);
						}
					}
					x += egp.getLegendHorizonGap();
					drawLegendRect(seriesNo, series1Count, x, y, fontHeight,
							nFontHeight, maxWidth, verticalPillar, isGongZi,
							Variant.toString(o), htmlBuffer);
					x += nFontHeight + egp.getLegendVerticalGap();
					y += legendHeight;
				}
			}

			if (egp.getLegendLocation() == IGraphProperty.LEGEND_LEFT) {
				gp.leftInset += (col + 1) * egp.getLegendHorizonGap() + col * legendWidth + egp.getLegendHorizonGap();
			} else {
				gp.rightInset += (col + 1) * egp.getLegendHorizonGap() + col * legendWidth + egp.getLegendHorizonGap();
			}
		}
	}

	/**
	 * 按照指定参数设置当前填充模式
	 * @param x 横坐标x
	 * @param y 纵坐标y
	 * @param w 填充宽度
	 * @param h 填充高度
	 * @param c 填充颜色
	 * @param verticalPillar 是否竖向圆柱
	 */
	public void setPaint(double x, double y, double w, double h, Color c,
			boolean verticalPillar) {
		setPaint(x, y, w, h, c, verticalPillar, Palette.PATTERN_DEFAULT);
	}

	/**
	 * 按照指定参数设置当前填充模式
	 * @param x 横坐标x
	 * @param y 纵坐标y
	 * @param w 填充宽度
	 * @param h 填充高度
	 * @param pIndex 颜色序号
	 * @param verticalPillar 是否竖向圆柱
	 */
	public void setPaint(double x, double y, double w, double h, int pIndex,
			boolean verticalPillar) {
		Color c = getColor(pIndex);
		byte p = palette.getPattern(pIndex);
		setPaint(x, y, w, h, c, verticalPillar, p);
	}

	protected void setPaint(double x, double y, double w, double h, Color c,
			boolean verticalPillar, byte fillPattern) {
		GradientPaint paint;
		if (gp.gradientColor) {
			Color cc = c;
			Color dd = c;

			int cValue = c.getRGB();
			if (cValue < -13410000) { // 黑色
				cc = Color.lightGray;
				dd = c;
			} else if (cValue < -8355712) { // 如果是深色背景
				cc = c.brighter();
				dd = cc.darker().darker();
			} else {
				cc = c;
				dd = cc.darker().darker().darker();
			}

			if (verticalPillar) { // 纵向柱子
				paint = new GradientPaint((float)x, (float)y, cc, (float)(x + w), (float)y, dd);
			} else {
				paint = new GradientPaint((float)x, (float)y, cc, (float)x, (float)(y + h), dd);
			}
			g.setPaint(paint);
		} else if (egp.isRaisedBorder()
				|| fillPattern == Palette.PATTERN_DEFAULT) {
			g.setColor(c);
		} else {
			g.setPaint(Palette.getPatternPaint(c, fillPattern));
		}
	}

	protected String getStgLinkHtml(String link, String shape, String coords,
			Object target, Object legendValue) {
		return getStgLinkHtml(link, shape, coords, target, legendValue, null,
				null, null);
	}

	protected String replaceAll(String src, String sold, String snew) {
		return Sentence.replace(src, 0, sold, snew, Sentence.IGNORE_CASE
				+ Sentence.IGNORE_PARS + Sentence.IGNORE_QUOTE
				+ Sentence.ONLY_PHRASE);

	}

	protected String getStgLinkHtml(String link, String shape, String coords,
			Object target, Object legendValue, Object categoryValue,
			String fmtedVal, String tips) {
		if (link == null) {
			return null;
		}
		if (StringUtils.isValidString(link)) {
			// 改成先替换后计算，同时吧把category当作参数
			if (StringUtils.isValidString(legendValue)) {
				link = replaceAll(link, "@legend", legendValue.toString());
			}
			if (StringUtils.isValidString(categoryValue)) {
				link = replaceAll(link, "@category", categoryValue.toString());
			}
			if (StringUtils.isValidString(tips)) {
				link = replaceAll(link, "@title", tips);
			}
		}
		StringBuffer sb = new StringBuffer(128);
		if (isSVG()) {
			if (!StringUtils.isValidString(link)) {
				return null;
			}
			sb.append("<a xlink:href=\"").append(link).append("\" target=\"");
			sb.append(target).append("\">\n");

			sb.append("<").append(shape);
			sb.append(" ").append(coords);
			sb.append(" style=\"fill:transparent;stroke:transparent\"/>\n");

			sb.append("</a>\n");
		} else {
			sb.append("<area shape=\"").append(shape).append("\" coords=\"");
			sb.append(coords);
			if (StringUtils.isValidString(link)) {
				sb.append("\" href=\"").append(link).append("\" target=\"")
						.append(target);
			}
			String title = "";
			if (StringUtils.isValidString(tips)) {
				title = tips;
			} else {
				if (StringUtils.isValidString(categoryValue)) {
					title = categoryValue.toString();
				}
				if (StringUtils.isValidString(legendValue)) {
					title += " " + legendValue.toString();
				}
				if (fmtedVal != null) {
					title += " " + fmtedVal;
				}
			}
			title = replaceAll(title, "\"", "&quot;");

			if (StringUtils.isValidString(title)) {
				sb.append("\" title=\"").append(title);
			}
			sb.append("\">\n");
		}
		return sb.toString();
	}

	/**
	 * 在指定位置画立方体的轮廓线
	 * @param x 横坐标x
	 * @param y 纵坐标y
	 * @param w 柱子宽度
	 * @param h 柱子高度
	 * @param z 柱子进深
	 */
	public void drawRectCubeLine(int x, int y, int w, int h, int z) {
		g.setColor(gp.coorColor);
		int hx, hy; // 立方体看不到的那个点的坐标
		hx = x + z;
		hy = y + h - z;

		g.drawLine(hx, hy, hx - z, hy + z);
		g.drawLine(hx, hy, hx, hy - h);
		g.drawLine(hx, hy, hx + w, hy);

	}

	/**
	 * 指定位置处画一个矩形，c为null时当透明处理，不绘制
	 * @param x 横坐标x
	 * @param y 纵坐标y
	 * @param w 宽度
	 * @param h 高度
	 * @param c 边框颜色
	 */
	public void drawRect(double x, double y, double w, double h, Color c) {
		if (c == null) {
			return;
		}
		g.setColor(c);
		Utils.drawRect(g, x, y, w, h);
	}

	/**
	 * 在指定的两个点间绘制直线
	 * 调用该函数之前必须先调用 Utils.setStroke(); 确保当前的线有正确的填充方案
	 * @param x1 点1横坐标
	 * @param y1 点1纵坐标
	 * @param x2 点2横坐标
	 * @param y2 点2纵坐标
	 * @param c 颜色，null时不绘制
	 */
	public void drawLine(double x1, double y1, double x2, double y2, Color c) {
		if (c == null) {
			return;
		}
		g.setColor(c);
		Utils.drawLine(g,x1, y1, x2, y2);
	}

	/**
	 * 在指定的两个点间绘制一段弧
	 * @param x 横坐标
	 * @param y 纵坐标
	 * @param w 宽度
	 * @param h 高度
	 * @param startAngle 起始角度
	 * @param arcAngle 弧的角度长
	 * @param c 颜色，null时不绘制
	 */
	public void drawArc(int x, int y, int w, int h, int startAngle,
			int arcAngle, Color c) {
		if (c == null) {
			return;
		}
		g.setColor(c);
		g.drawArc(x, y, w, h, startAngle, arcAngle);
	}

	/**
	 * 按照指定颜色绘制形状s
	 * @param s 形状对象
	 * @param c 颜色，null时不绘制
	 */
	public void drawShape(Shape s, Color c) {
		if (c == null) {
			return;
		}
		g.setColor(c);
		g.draw(s);
	}

	/**
	 * 按照指定位置绘制多边形
	 * @param x 点的x坐标数值
	 * @param y 点的y坐标数值
	 * @param n 点的个数
	 * @param c 颜色，null时不绘制
	 */
	public void drawPolygon(double[] x, double[] y, int n, Color c) {
		if (c == null) {
			return;
		}
		g.setColor(c);
		Shape poly = Utils.newPolygon2D(x, y);
		g.draw(poly);
	}

	public void drawPolygon(int[] x, int[] y, int n, Color c) {
		if (c == null) {
			return;
		}
		g.setColor(c);
		Polygon poly = new Polygon(x, y, n);
		g.draw(poly);
	}

	/**
	 * 按照指定位置绘制椭圆
	 * @param x x坐标
	 * @param y y坐标
	 * @param w 宽度
	 * @param h 高度
	 * @param c 颜色，null时不绘制
	 */
	public void drawOval(int x, int y, int w, int h, Color c) {
		if (c == null) {
			return;
		}
		g.setColor(c);
		g.drawOval(x, y, w, h);
	}

	/**
	 * 根据图形属性配置在指定位置填充椭圆
	 * @param x x坐标
	 * @param y y坐标
	 * @param w 宽度
	 * @param h 高度
	 */
	public void fillOval(int x, int y, int w, int h) {
		if (egp.isDrawShade()) {
			Paint p = g.getPaint();
			Color c = g.getColor();
			g.setColor(Color.lightGray);
			g.fillOval(x + SHADE_SPAN, y + SHADE_SPAN, w, h);
			g.setColor(c);
			g.setPaint(p);
		}
		g.fillOval(x, y, w, h);
	}

	/**
	 * 根据图形属性配置在指定位置填充矩形
	 * @param x x坐标
	 * @param y y坐标
	 * @param w 宽度
	 * @param h 高度
	 */
	public void fillRect(double x, double y, double w, double h) {
		Color bc = null;
		int bs = 0;
		float bw = 0;
		ChartColor fillColor = getChartColor(g.getColor());
		Utils.draw2DRect(g, x, y, w, h, bc, bs, bw, egp.isDrawShade(),
				egp.isRaisedBorder(), getTransparent(), fillColor,
				!egp.isBarGraph(this));
	}

	/**
	 * 从中心点 cx,cy按c1到c2的颜色环状发散梯度填充形状s 目前仅支持cx，cy在形状s内的填充
	 * @param s
	 *            Shape 形状对象
	 * @param c1
	 *            Color 颜色1
	 * @param c2
	 *            Color 颜色2
	 * @param cx
	 *            int 中心点x
	 * @param cy 
	 *            int 中心点y
	 */
	public void fillDotGradientShape(Shape s, Color c1, Color c2, double cx, double cy) {
		Rectangle sBounds = s.getBounds();
		double w = sBounds.getWidth();
		double h = sBounds.getHeight();
		int r = (int) Math.sqrt(w * w + h * h);
		int dAngle = 5;
		int sAngle = 0;

		Arc2D.Double a2dd = new Arc2D.Double(cx - r, cy - r, r * 2, r * 2,
				sAngle, dAngle + 3, Arc2D.PIE);
		java.awt.geom.Area sA = new java.awt.geom.Area(s);
		Point2D.Double p1 = new Point2D.Double(cx, cy);
		while (sAngle <= 360) {
			java.awt.geom.Area da = new java.awt.geom.Area(a2dd);
			java.awt.geom.Area a1 = new java.awt.geom.Area(s);
			a1.add(da);
			a1.subtract(sA);

			da.subtract(a1);
			Point2D p2 = a2dd.getStartPoint();
			p2.setLocation((p1.getX() + p2.getX()) / 2,
					(p1.getY() + p2.getY()) / 2);
			g.setPaint(new GradientPaint(p1, c1, p2, c2));
			g.fill(da);
			sAngle += dAngle;
			a2dd.setAngleStart(sAngle);
		}
	}


	/**
	 * 根据指定参数绘制一个长方体
	 * 
	 * @param x
	 *            int 正面矩形x 正面矩形y 根据h的正负，和基线来计算得出
	 * @param w
	 *            int 正面矩形w
	 * @param h
	 *            int 正面矩形h
	 * @param z
	 *            int 立方的z深度
	 * @param dz
	 *            int 立方的z向位移
	 * @param colorIndex
	 *            int 颜色序号
	 * @param sb
	 *            StringBuffer 生成超连接的缓冲
	 * @param cat
	 *            Object 当前分类值
	 * @param egs
	 *            ExtGraphSery 系列
	 */
	public void drawRectCube(double x, double w, double h, double z, double dz,
			int colorIndex, StringBuffer sb, Object cat, ExtGraphSery egs) {
		ChartColor chartColor = getChartColor(getColor(colorIndex));
		double xx, yy, ww, hh;
		if (h >= 0) {
			xx = x + dz;
			yy = gp.valueBaseLine - h - dz;
			ww = w;
			hh = h;
		} else {
			xx = x + dz;
			yy = gp.valueBaseLine - dz;
			ww = w;
			hh = Math.abs(h);
		}
		Color bc = egp.getAxisColor(PublicProperty.AXIS_COLBORDER);
		int bs = Consts.LINE_SOLID;
		float bw = 1;
		double coorShift = z;
		Utils.draw3DRect(g, xx, yy, ww, hh, bc, bs, bw, egp.isDrawShade(),
				egp.isRaisedBorder(), getTransparent(), chartColor,
				!egp.isBarGraph(this), coorShift);
		htmlLink(xx, yy, ww, hh, sb, cat, egs);

	}

	protected Rectangle2D.Double rightTop, rightBottom, leftTop, leftBottom; // 四周相应已经占用了的位置

	/**
	 * 画圆形外的数值，根据角度调整Text的坐标。雷达图以及饼图用到
	 * 
	 * @param gfv
	 *            GraphFontView 字体定义视图
	 * @param text
	 *            String 文本值
	 * @param angle
	 *            double 当前数值所处圆上的角度位置
	 * @param x
	 *            int 文本的x
	 * @param y
	 *            int 文本的y
	 */
	public void drawOutCircleText(GraphFontView gfv, String text, double angle,
			double x, double y) {
		if (!StringUtils.isValidString(text)) {
			return;
		}
		if (egp.isShowOverlapText()) {
			drawOutCircleText(gfv, text, angle, x, y, false);
		} else {
			if (!drawOutCircleText(gfv, text, angle, x, y, false)) {
				drawOutCircleText(gfv, text, angle, x, y, true);
			}
		}
	}

	/**
	 * 画圆形内的数值，根据角度调整Text的坐标。雷达图以及饼图用到
	 * 
	 * @param gfv
	 *            GraphFontView 字体定义视图
	 * @param text
	 *            String 文本值
	 * @param angle
	 *            double 当前数值所处圆上的角度位置
	 * @param x
	 *            int 文本的x
	 * @param y
	 *            int 文本的y
	 */
	public void drawInnerCircleText(GraphFontView gfv, String text,
			double angle, double x, double y) {
		if (!StringUtils.isValidString(text)) {
			return;
		}
		gfv.outText(x, y, text,
				GraphFontView.reverseDirection(angleToPosition(gfv, angle)));
	}

	protected byte angleToPosition(GraphFontView gfv, double angle) {
		if (angle >= 360) {
			angle -= 360;
		}
		if (gfv.vertical || gfv.angle == 90) {
			if (angle < 5) {
				return GraphFontView.TEXT_ON_RIGHT;
			} else if (angle < 175) {
				return GraphFontView.TEXT_ON_TOP;
			} else if (angle < 185) {
				return GraphFontView.TEXT_ON_LEFT;
			} else if (angle < 355) {
				return GraphFontView.TEXT_ON_BOTTOM;
			} else {
				return GraphFontView.TEXT_ON_RIGHT;
			}
		} else if (gfv.angle == 0) {
			if (angle < 85) {
				return GraphFontView.TEXT_ON_RIGHT;
			} else if (angle < 95) {
				return GraphFontView.TEXT_ON_TOP;
			} else if (angle < 265) {
				return GraphFontView.TEXT_ON_LEFT;
			} else if (angle < 275) {
				return GraphFontView.TEXT_ON_BOTTOM;
			} else {
				return GraphFontView.TEXT_ON_RIGHT;
			}
		} else {
			if (angle < 85) {
				return GraphFontView.TEXT_ON_RIGHT;
			} else if (angle < 95) {
				return GraphFontView.TEXT_ON_TOP;
			} else if (angle < 180 + gfv.angle) {
				return GraphFontView.TEXT_ON_LEFT;
			} else if (angle < 360 - gfv.angle) {
				return GraphFontView.TEXT_ON_BOTTOM;
			} else {
				return GraphFontView.TEXT_ON_RIGHT;
			}
		}
	}

	protected boolean drawOutCircleText(GraphFontView gfv, String text,
			double angle, double x, double y, boolean drawExtend) {
		double tmpAngle = angle;
		int w = gfv.getTextSize(text).width;
		int h = gfv.getTextSize(text).height;

		while (tmpAngle < 0) {
			tmpAngle += 360;
		}
		while (tmpAngle > 360) {
			tmpAngle -= 360;
		}

		boolean isLeft = tmpAngle > 90 && tmpAngle < 270;
		boolean isTop = !(tmpAngle > 180 && tmpAngle < 360);
		if (drawExtend) {
			double x1 = x, y1 = y;
			if (isLeft && isTop) { // 左上 - 左边 //
				if (leftTop == null) {
					leftTop = new Rectangle2D.Double(gp.gRect2.x, gp.gRect2.y, 0, 0);
				}
				x = leftTop.x;
				y = leftTop.y + leftTop.height + 5;
				leftTop = new Rectangle2D.Double(leftTop.x, y, w, h);

				Utils.drawLine(g,x1, y1, x1, y);
				Utils.drawLine(g,x1, y, x, y);
				y += h / 2;
			} else if (isLeft && !isTop) { // 左下 － 下边
				if (leftBottom == null) {
					leftBottom = new Rectangle2D.Double(gp.gRect2.x, gp.gRect2.y
							+ gp.gRect2.height, 0, 0);
				}
				x = leftBottom.x;
				y = leftBottom.y - h;
				leftBottom = new Rectangle2D.Double(x, leftBottom.y, w, h);
				Utils.drawLine(g,x1, y1, x1, y);
				Utils.drawLine(g,x1, y, x, y);
				w /= 2;
			} else if (!isLeft && isTop) { // 右上 － 上边
				if (rightTop == null) {
					rightTop = new Rectangle2D.Double(gp.gRect2.x + gp.gRect2.width,
							gp.gRect2.y, 0, 0);
				}
				x = rightTop.x - w - 2;
				x = x1 > x ? rightTop.x : x;

				y = rightTop.y + h;
				rightTop = new Rectangle2D.Double(x, rightTop.y, w, h);
				Utils.drawLine(g,x1, y1, x1, y);
				Utils.drawLine(g,x1, y, x, y);
				x -= w / 2;
			} else { // 右下 － 右边
				if (rightBottom == null) {
					rightBottom = new Rectangle2D.Double(gp.gRect2.x + gp.gRect2.width,
							gp.gRect2.y + gp.gRect2.height, 0, 0);
				}
				x = rightBottom.x - w;
				x = x1 > x ? rightBottom.x : x;

				y = rightBottom.y - h - 5;
				rightBottom = new Rectangle2D.Double(rightBottom.x, y, w, h);
				Utils.drawLine(g,x1, y1, x1, y);
				Utils.drawLine(g,x1, y, x, y);
				h /= 2;
			}

			isLeft = x1 > x;
		} else {
			byte direction = angleToPosition(gfv, angle);

			if (!(this instanceof DrawRadar)) {// 雷达图时，不使用延长线
				int shiftX = 10;
				double x1 = x, y1 = y, x2 = x, y2 = y;
				if (direction == GraphFontView.TEXT_ON_LEFT) {
					x1 = x - shiftX;
					x = x1;
				} else if (direction == GraphFontView.TEXT_ON_RIGHT) {
					x2 = x + shiftX;
					x = x2;
				}
				boolean isOutText = gfv.outText(x, y, text, direction);
				if (isOutText) {
					Color tmpc = egp.getAxisColor(GraphProperty.AXIS_PIEJOIN);
					Line2D.Double tmpShape = new Line2D.Double(x1, y1, x2, y2);
					g.setStroke(new BasicStroke(1.0f));
					drawShape(tmpShape, tmpc);
				}
				return isOutText;
			}

			return gfv.outText(x, y, text, direction);
		}
		if (isLeft) {
			x -= w;
		}
		if (!isTop) {
			y += h;
		}

		return gfv.outText(x, y, text, GraphFontView.TEXT_FIXED); /**/
	}

	/**
	 * 根据参数绘制文本
	 * @param text 文本
	 * @param x 横坐标
	 * @param y 纵坐标
	 * @param gf 字体定义
	 */
	public void drawText(String text, int x, int y, GraphFont gf) {
		if (!StringUtils.isValidString(text)) {
			return;
		}
		g.setColor(new Color(gf.getColor()));
	}

	/**
	 * 画纵向网格线
	 * @param delx 网格线之间的宽度
	 * @param ci 网格线序号
	 */
	public void drawGridLineV(double delx, int ci) {
		if (ci == 0 || ci == gp.tickNum
				|| gp.gridLineStyle == IGraphProperty.LINE_NONE) {
			return;
		}
		if(gp.gridLineLocation==IGraphProperty.GRID_CATEGORY){
			return;
		}
		BasicStroke stroke = getLineStroke(gp.gridLineStyle, 1f);
		if (stroke == null) {
			return;
		}
		g.setColor(gp.gridColor);
		g.setStroke(stroke);

		Utils.drawLine(g,gp.gRect2.x + ci * delx, gp.gRect2.y
				+ gp.gRect2.height, gp.gRect2.x + ci * delx,
				gp.gRect2.y + 1);
		g.setStroke(new BasicStroke(0.00001f));
		Utils.drawLine(g,
				// 斜边线太短，只用直线
				gp.gRect1.x + ci * delx,
				gp.gRect1.y + gp.gRect1.height,
				gp.gRect2.x + ci * delx, gp.gRect2.y + gp.gRect2.height);
	}

	/**
	 * 画横向网格线
	 * @param dely 网格线之间的高度
	 * @param ci 网格线序号
	 */
	public void drawGridLine(double dely, int ci) {
		Color c = egp.getAxisColor(IGraphProperty.AXIS_LEFT);
		double tmpi = gp.gRect1.y + gp.gRect1.height - ci * dely;
		if (c != null) {
			Utils.setStroke(g, c, Consts.LINE_SOLID, 1.0f);
			drawLine(gp.gRect1.x - vShift.x - gp.tickLen, tmpi, gp.gRect1.x
					- vShift.x, tmpi, c); // Y坐标轴的小短线
		}

		// 由于网格线顶线跟外边框重合，目前是外边框颜色优先；
		if (ci == 0 || ci == gp.tickNum
				|| gp.gridLineStyle == IGraphProperty.LINE_NONE) {
			return;
		}
		if(gp.gridLineLocation==IGraphProperty.GRID_CATEGORY){
			return;
		}
		BasicStroke bs = getLineStroke(gp.gridLineStyle, 1f);
		if (bs == null) {
			return;
		}
		g.setColor(gp.gridColor);
		g.setStroke(bs);
		Utils.drawLine(g,gp.gRect2.x, gp.gRect2.y + gp.gRect2.height - ci
				* dely, gp.gRect2.x + gp.gRect2.width - 1, gp.gRect2.y
				+ gp.gRect2.height - ci * dely);

		if (c != null) {// 如果纵轴不为透明色时，需要绘制斜边网格
			// 斜边线太短，只用直线 ;
			Utils.drawLine(g,gp.gRect1.x, gp.gRect1.y + gp.gRect1.height - ci
					* dely, gp.gRect2.x,
					gp.gRect2.y + gp.gRect2.height - ci * dely);
		}
	}

	
	/**
	 * 画纵向分类网格线
	 * @param catX 分类的横坐标
	 */
	public void drawGridLineCategoryV(double catX) {
		if (gp.gridLineStyle == IGraphProperty.LINE_NONE) {
			return;
		}
		if(gp.gridLineLocation==IGraphProperty.GRID_VALUE){
			return;
		}
		BasicStroke stroke = getLineStroke(gp.gridLineStyle, 1f);
		if (stroke == null) {
			return;
		}
		g.setColor(gp.gridColor);
		g.setStroke(stroke);

		Utils.drawLine(g,catX, gp.gRect2.y+ gp.gRect2.height, 
				catX,gp.gRect2.y + 1);
	}

	/**
	 * 画横向分类网格线
	 * @param catY 分类的纵坐标
	 */
	public void drawGridLineCategory(double catY) {
		if (gp.gridLineStyle == IGraphProperty.LINE_NONE) {
			return;
		}
		if(gp.gridLineLocation==IGraphProperty.GRID_VALUE){
			return;
		}
		BasicStroke stroke = getLineStroke(gp.gridLineStyle, 1f);
		if (stroke == null) {
			return;
		}
		g.setColor(gp.gridColor);
		g.setStroke(stroke);

		Utils.drawLine(g,gp.gRect2.x+1, catY,gp.gRect2.x+gp.gRect2.width,catY);
	}
	
	/**
	 * 让出饼图的外围空间
	 */
	public void keepGraphSpace() {
		boolean isPie = (this instanceof DrawPie)
				|| (this instanceof DrawPie3DObj);
		int space = gp.graphMargin;
		if (isPie) {
			if (space > -1) {
				gp.leftInset += space;
				gp.rightInset += space;
				gp.topInset += space;
				gp.bottomInset += space;
				return;
			}

			ArrayList cats = egp.categories;
			int cc = cats.size();
			double maxW = 0;
			double maxH = 0;
			for (int i = 0; i < cc; i++) {
				ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
				ArrayList sers = egc.getSeries();
				for (int n = 0; n < sers.size(); n++) {
					String fmt;
					String text = "";
					ExtGraphSery egs = (ExtGraphSery) sers.get(n);
					// 显示数值标示
					switch (gp.dispValueType) {
					case 1: // 不显示
						text = "";
						break;
					case 2: // 显示数值
						if (StringUtils.isValidString(gp.dataMarkFormat)) {
							fmt = gp.dataMarkFormat;
						} else {
							fmt = "";
						}
						text = getFormattedValue(egs.getValue(), fmt);
						break;
					default: // 显示百分比
						if (StringUtils.isValidString(gp.dataMarkFormat)) {
							fmt = gp.dataMarkFormat;
						} else {
							fmt = "0.00%";
						}
						text = getFormattedValue(egs.getValue(), fmt);
						break;
					}

					Rectangle rec = gp.GFV_VALUE.getTextSize(text);
					if (rec.getWidth() > maxW) {
						maxW = rec.getWidth();
					}
					if (rec.getHeight() > maxH) {
						maxH = rec.getHeight();
					}
				}
			}
			gp.leftInset += maxW;
			gp.rightInset += maxW;
			gp.topInset += maxH;
			gp.bottomInset += maxH;
		} else {
			if (space == -1) {
				space = 5;
			}
			gp.leftInset += space;
			gp.rightInset += space;
			gp.topInset += space;
			gp.bottomInset += space;
		}
	}

	/**
	 * 画统计图标题 
	 */
	public void drawTitle() {
		if (!StringUtils.isValidString(gp.GFV_TITLE.text)) {
			return;
		}
		int y = 0;
		TR = gp.GFV_TITLE.getTextSize();
		y = gp.topInset + TR.height;

		Point2D.Double p = new Point2D.Double(0, y);
		vlTitle = new ValueLabel(gp.GFV_TITLE.text, p, null);
		gp.topInset += TR.height + 4;
	}

	/**
	 * 画平面图形区域的矩形
	 */
	public void drawGraphRect() {
		if (gp.graphBackColor != null) {
			double bx, by, bw, bh;
			bx = gp.gRect2.x;
			by = gp.gRect2.y;
			bw = gp.gRect2.width;
			bh = gp.gRect2.height;
			if (egp.isGradientColor()) {
				CubeColor ccr = new CubeColor(gp.graphBackColor);
				ChartColor tmpcc = new ChartColor();
				tmpcc.setColor1(ccr.getLight(0.95f));
				tmpcc.setColor2(ccr.getLight(0.65f));
				tmpcc.setAngle(0);
				Utils.setPaint(g, bx, by, bw, bh, tmpcc);
			} else {
				g.setColor(gp.graphBackColor);
			}
			Utils.fillRect(g,bx, by, bw, bh);
		}

		double vx = 0, vy = 0, hx = 0, hy = 0;
		// 纵轴的x和y偏移以及横轴的

		g.setStroke(new BasicStroke(1f));
		Color c;
		double x1 = gp.gRect2.x;
		double y1 = gp.gRect2.y;
		double x2 = x1 + gp.gRect2.width;
		double y2 = y1;
		c = egp.getAxisColor(IGraphProperty.AXIS_TOP);
		drawLine(x1, y1, x2, y2, c);

		x1 = x2;
		y1 = y2 + gp.gRect2.height;
		c = egp.getAxisColor(IGraphProperty.AXIS_RIGHT);
		drawLine(x1, y1, x2, y2, c);

		double x, y, w, h;
		double coorShift = gp.gRect2.x - gp.gRect1.x;
		c = egp.getAxisColor(IGraphProperty.AXIS_BOTTOM);
		if (c != null) {
			if (coorShift != 0) {
				if (egp.isGradientColor()) {
					x = gp.gRect1.x;
					y = gp.gRect1.y + gp.gRect1.height;
					w = gp.gRect1.width;
					h = Utils.getPlatformH(coorShift);
					hy = h;
					ChartColor fillColor = new ChartColor(c);
					Utils.draw3DRect(g, x, y, w, h, null, 0, 0, false, false,
							1, fillColor, true, coorShift);
				} else {
					x2 = x1 - gp.gRect2.width;
					y2 = y1;
					drawLine(x1, y1, x2, y2, c);
					drawLine(gp.gRect1.x, gp.gRect1.y + gp.gRect1.height,
							gp.gRect1.x + gp.gRect1.width, gp.gRect1.y
									+ gp.gRect1.height, c);
					drawLine(gp.gRect1.x, gp.gRect1.y + gp.gRect1.height,
							gp.gRect2.x, gp.gRect2.y + gp.gRect2.height, c);
					drawLine(gp.gRect1.x + gp.gRect1.width, gp.gRect1.y
							+ gp.gRect1.height, gp.gRect2.x + gp.gRect2.width,
							gp.gRect2.y + gp.gRect2.height, c);
				}
			} else {
				x1 = gp.gRect1.x;
				y1 = gp.gRect1.y + gp.gRect1.height;
				x2 = x1 + gp.gRect1.width;
				y2 = y1;
				drawLine(x1, y1, x2, y2, c);
			}
		} else {
			hx = coorShift;
			hy = -coorShift;
		}
		hShift = new Point2D.Double(hx, hy);

		c = egp.getAxisColor(IGraphProperty.AXIS_LEFT);
		if (c != null) {
			if (coorShift != 0) {
				if (egp.isGradientColor()) {
					w = Utils.getPlatformH(coorShift);
					x = gp.gRect1.x - w;
					y = gp.gRect1.y;
					h = gp.gRect1.height;
					vx = w;
					ChartColor fillColor = new ChartColor(c);
					Utils.draw3DRect(g, x, y, w, h, null, 0, 0, false, false,
							1, fillColor, false, coorShift);
				} else {
					x1 = gp.gRect2.x;
					y1 = gp.gRect2.y;
					drawLine(x1, y1, x2, y2, c);
					drawLine(gp.gRect1.x, gp.gRect1.y, gp.gRect1.x, gp.gRect1.y
							+ gp.gRect1.height, c);
					drawLine(gp.gRect1.x, gp.gRect1.y + gp.gRect1.height,
							gp.gRect2.x, gp.gRect2.y + gp.gRect2.height, c);
					drawLine(gp.gRect1.x, gp.gRect1.y, gp.gRect2.x,
							gp.gRect2.y, c);
				}
			} else {
				x1 = gp.gRect1.x;
				y1 = gp.gRect1.y;
				x2 = x1;
				y2 = y1 + gp.gRect1.height;
				drawLine(x1, y1, x2, y2, c);
			}
		} else {
			vx = -coorShift;
			vy = coorShift;
		}
		vShift = new Point2D.Double(vx, vy);
	}

	private String getLeftText() {
		String label = "";
		if (egp.isBarGraph(this)) {
			label = gp.GFV_YTITLE.text;
		} else {
			label = gp.GFV_XTITLE.text;
		}
		return label;
	}

	/**
	 * 画坐标轴标题
	 */
	public void drawLabel() {
		/* 画X轴标题 */
		String label = getLeftText();

		int x = 0;
		int y = 0;
		if (StringUtils.isValidString(label)) {
			TR = gp.GFV_XTITLE.getTextSize(label);
			y = gp.graphHeight - gp.bottomInset;
			vlXTitle = new ValueLabel(label, new Point2D.Double(0, y), null);
			gp.bottomInset += TR.height + 2;
		}

		/* 画Y轴标题 */
		if (egp.isBarGraph(this)) {
			label = gp.GFV_XTITLE.text;
		} else {
			label = gp.GFV_YTITLE.text;
		}

		if (StringUtils.isValidString(label)) {
			// Y1轴标题
			TR = gp.GFV_YTITLE.getTextSize(label);
			x = gp.leftInset;

			// 绘制数据表时，数据表的位置会侵占纵轴位置，挪到后面去画
			vlYTitle1 = new ValueLabel(label, new Point2D.Double(x, 0), null);
			gp.leftInset += TR.width + 4;
		}

		if (egp.is2YGraph() && StringUtils.isValidString(gp.GFV_YTITLE.text2)) { // Y2轴标题
			TR = gp.GFV_YTITLE.getTextSize(gp.GFV_YTITLE.text2);
			x = gp.graphWidth - gp.rightInset;

			vlYTitle2 = new ValueLabel(gp.GFV_YTITLE.text2, new Point2D.Double(x, 0),
					null);
			gp.rightInset += TR.width + 4;
		}

	}

	/**
	 * 在指定位置画一个数据点
	 * @param pt 点的坐标
	 * @param shape 点的形状
	 * @param radius 半径
	 * @param borderStyle 边框风格
	 * @param borderWeight 边框粗度
	 * @param backColor 背景色
	 * @param foreColor 边框颜色
	 */
	public void drawPoint(Point2D.Double pt, int shape, double radius, int borderStyle,
			float borderWeight, Color backColor, Color foreColor) {
		if (egp.isDrawShade()) {
			Utils.drawCartesianPoint1(g, pt, shape, radius, radius, radius,
					borderStyle, borderWeight, getTransparent());
		}
		Utils.drawCartesianPoint2(g, pt, shape, radius, radius, radius,
				borderStyle, borderWeight, getChartColor(backColor), foreColor,
				getTransparent());
	}

	/**
	 * 在指定矩形区域产生超链接内容，并写入缓冲sb
	 * @param x1 横坐标
	 * @param y1 纵坐标
	 * @param w 宽度
	 * @param h 高度
	 * @param sb html缓冲
	 * @param category 分类值
	 * @param egs 系列值
	 */
	public void htmlLink(double x1, double y1, double w, double h, StringBuffer sb,
			Object category, ExtGraphSery egs) {
		if (disableLink()) {
			return;
		}
		// 超连接处理
		int minimum = 10;
		if (w < minimum) {
			w = minimum;
		}
		if (h < minimum) {
			h = minimum;
		}
		String coordx;
		if (isSVG()) {
			if (!StringUtils.isValidString(egp.getLink())) {
				Logger.debug("SVG graph must specify hyper link to generate dynamic links.");
				return;
			}

			coordx = "x=\"" + (int)x1 + "\" y=\"" + (int)y1 + "\" width=\"" + (int)w
					+ "\" height=\"" + (int)h + "\"";
		} else {
			coordx = (int)x1 + "," + (int)y1 + "," + (int)(x1 + w) + "," + (int)(y1 + h);
		}
		sb.append(getStgLinkHtml(egp.getLink(), "rect", coordx,
				egp.getLinkTarget(), egs.getName(), category,
				getDispValue(egs), egs.getTips()));
	}

	/**
	 * 在指定区域产生超链接内容，并写入缓冲sb，方法废弃
	 * @param x1 横坐标
	 * @param y1 纵坐标
	 * @param w 宽度
	 * @param h 高度
	 * @param sb html缓冲
	 * @param category 分类值
	 * @param egs 系列值
	 */
	public void htmlLink2(double x1, double y1, double w, double h, StringBuffer sb,
			Object category, ExtGraphSery egs) {
		// 超连接处理
		if (disableLink()) {
			return;
		}
		int minimum = 10;
		if (w < minimum) {
			w = minimum;
		}
		if (h < minimum) {
			h = minimum;
		}
		String coordx;
		if (isSVG()) {
			if (!StringUtils.isValidString(egp.getLink())) {
				return;
			}
			coordx = "x=\"" + (int)x1 + "\" y=\"" + (int)y1 + "\" width=\"" + (int)w
					+ "\" height=\"" + (int)h + "\"";
		} else {
			coordx = (int)x1 + "," + (int)y1 + "," + (int)(x1 + w) + "," + (int)(y1 + h);
		}
		sb.append(getStgLinkHtml(egp.getLink(), "rect", coordx,
				egp.getLinkTarget(), egs.getName(), category,
				getDispValue2(egs), egs.getTips()));
	}

	private boolean debuglink = true;

	/**
	 * 生成一段环形状的超链接，并将内容写入缓冲sb
	 * @param ddd 外围弧
	 * @param sb html缓冲
	 * @param category 分类值
	 * @param egs 系列值
	 * @param ddd2 内围弧
	 */
	public void htmlLink(Arc2D.Double ddd, StringBuffer sb, Object category,
			ExtGraphSery egs, Arc2D.Double ddd2) {
		if (disableLink()) {
			return;
		}
		String shape = "";
		if (isSVG()) {
			if (!StringUtils.isValidString(egp.getLink())) {
				if(debuglink){
					Logger.debug("SVG graph must specify link to generate hyper links.");
					debuglink = false;
				}
				return;
			}
			shape = "polygon";
		} else {
			shape = "poly";
		}
		StringBuffer coords = new StringBuffer();
		int cx = (int) ddd.getCenterX();
		int cy = (int) ddd.getCenterY();
		double start = ddd.getAngleStart();
		double end = ddd.getAngleStart() + ddd.getAngleExtent();
		double delta = 5;
		if (isSVG()) {
			coords.append("points=\"");
		}
		String seperator = ",";
		if (isSVG()) {
			seperator = " ";
		}
		if (ddd2 == null) {
			coords.append(cx);
			coords.append(seperator + cy + ",");
		}

		coords.append((int) ddd.getStartPoint().getX());
		coords.append(seperator + (int) ddd.getStartPoint().getY());

		start += delta;
		while (start < end) {
			Arc2D.Double tmp = new Arc2D.Double(ddd.getBounds(), start, delta,
					Arc2D.PIE);
			coords.append("," + (int) tmp.getEndPoint().getX());
			coords.append(seperator + (int) tmp.getEndPoint().getY());
			start += delta;
		}

		coords.append("," + (int) ddd.getEndPoint().getX());
		coords.append(seperator + (int) ddd.getEndPoint().getY());

		if (ddd2 != null) { // 小环坐标得反过来
			coords.append("," + (int) ddd2.getEndPoint().getX());
			coords.append(seperator + (int) ddd2.getEndPoint().getY());

			double start2 = ddd2.getAngleStart();
			double end2 = ddd2.getAngleStart() + ddd2.getAngleExtent() - delta;

			while (end2 > start2) {
				Arc2D.Double tmp = new Arc2D.Double(ddd2.getBounds(), end2,
						delta, Arc2D.PIE);
				coords.append("," + (int) tmp.getStartPoint().getX());
				coords.append(seperator + (int) tmp.getStartPoint().getY());
				end2 -= delta;
			}
			coords.append("," + (int) ddd2.getStartPoint().getX());
			coords.append(seperator + (int) ddd2.getStartPoint().getY());
		}

		// 饼图的htmllink数据不再使用格式化，因为饼图本身显示数据为百分比时，link的数据为原值，不能使用定义的格式
		String dispVal = null;
		if (!egs.isNull()) {
			double scaledVal = getScaledValue(egs.getValue(), true);
			dispVal = getDispValue(scaledVal, egs.getTips(), gp.dataMarkFormat);
		}
		if (isSVG()) {
			coords.append("\"");
		}
		sb.append(getStgLinkHtml(egp.getLink(), shape, coords.toString(),
				egp.getLinkTarget(), egs.getName(), category, dispVal,
				egs.getTips()));
	}

	/**
	 * 取折线stroke
	 * @return stroke
	 */
	public Stroke getLineStroke() {
		return getLineStroke(egp.getLineStyle(), gp.getLineThick());
	}

	/**
	 * 根据定义产生直线stroke
	 * @param style 线型
	 * @param thick 粗度
	 * @return stroke
	 */
	public BasicStroke getLineStroke(int style, float thick) {
		if (thick == 0) {
			return null;
		}
		float dashes[] = { 2 };
		switch (style) {
		case IGraphProperty.LINE_SOLID:
			return new BasicStroke(thick);
		case IGraphProperty.LINE_DOT_DASH:
			dashes = new float[] { 2, 2, 6, 2 };
			break;
		case IGraphProperty.LINE_2DOT_DASH:
			dashes = new float[] { 2, 2, 2, 2, 6, 2 }; // 一节的定义
														// 实2,空2,实2,空2,实6,空2
			break;
		case IGraphProperty.LINE_LONG_DASH:
			dashes = new float[] { 5 };
			break;
		case IGraphProperty.LINE_SHORT_DASH:
			dashes = new float[] { 2 };
			break;
		default:
			return null;
		}
		return new BasicStroke(thick, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_BEVEL, thick, dashes, 0);
	}

	/**
	 * 由于警戒线坐标为整体计算的，而坐标标签则是用间隔乘以空距累算的，所以部分值有累积误差
	 * 该函数解决当警戒值包含在坐标标签中时，由子类重新按照空距累算覆盖该方法以保证两者的坐标一致
	 * 
	 * @param alarmVal
	 *            double 警戒值
	 * @return Point null
	 */
	public Point2D.Double getCoorPoint(double alarmVal) {
		return null;
	}

	/**
	 * 绘制警戒线
	 */
	public void drawWarnLine() {
		ArrayList warnLines = egp.getAlarmLines();
		if (warnLines == null) {
			return;
		}

		double x1, y1, x2, y2;
		Stroke s = g.getStroke();
		int cc = warnLines.size();

		double topTickVal = ((Number) gp.coorValue.get(gp.tickNum))
				.doubleValue();
		double bottomTickVal = ((Number) gp.coorValue.get(0)).doubleValue();
		double tickArea = Math.abs(bottomTickVal - topTickVal);

		for (int i = 0; i < cc; i++) {
			ExtAlarmLine eal = (ExtAlarmLine) warnLines.get(i);
			byte lineType = eal.getLineType();
			float thick = eal.getLineThick();
			Stroke bs = getLineStroke(lineType, thick);

			if (bs == null) {
				continue;
			}
			g.setStroke(bs);

			int lineColor = eal.getColor();
			g.setColor(new Color(lineColor));

			double value = eal.getAlarmValue() / gp.scaleMark;
			y1 = gp.gRect2.y + (Math.abs(value - topTickVal) / tickArea)
					* gp.gRect2.height;

			x1 = gp.gRect2.x;
			x2 = x1 + gp.gRect2.width;
			y2 = y1;
			Utils.drawLine(g,x1, y1, x2, y2);
			if(!eal.isDrawAlarmValue()){
				continue;
			}
			String scoory = getFormattedValue(value);
			TR.setBounds(gp.GFV_YLABEL.getTextSize(scoory));
			x1 = gp.gRect1.x - gp.tickLen; // - TR.width
			y1 += gp.gRect1.y - gp.gRect2.y;
			Color tmp = gp.GFV_YLABEL.color;
			gp.GFV_YLABEL.color = new Color(lineColor);
			gp.GFV_YLABEL.outText(x1, y1, scoory, GraphFontView.TEXT_ON_LEFT);
			gp.GFV_YLABEL.color = tmp;
		}
		g.setStroke(s);
	}

	/**
	 * 绘制横向坐标轴的警戒线
	 */
	public void drawWarnLineH() {
		ArrayList warnLines = egp.getAlarmLines();
		if (warnLines == null) {
			return;
		}
		double x1, y1, x2, y2;
		Stroke s = g.getStroke();
		int cc = warnLines.size();

		double rightTickVal = ((Number) gp.coorValue.get(gp.tickNum))
				.doubleValue();
		double leftTickVal = ((Number) gp.coorValue.get(0)).doubleValue();
		double tickArea = Math.abs(rightTickVal - leftTickVal);

		for (int i = 0; i < cc; i++) {
			ExtAlarmLine eal = (ExtAlarmLine) warnLines.get(i);
			int lineType = eal.getLineType();
			Stroke bs = getLineStroke(lineType, eal.getLineThick());
			if (bs == null) {
				continue;
			}
			g.setStroke(bs);
			int lineColor = eal.getColor();
			g.setColor(new Color(lineColor));

			double value = eal.getAlarmValue() / gp.scaleMark;
			x1 = gp.gRect2.x + (Math.abs(value - leftTickVal) / tickArea)
					* gp.gRect2.width;

			y1 = gp.gRect2.y;
			x2 = x1;
			y2 = y1 + gp.gRect2.height;
			Utils.drawLine(g,x1, y1, x2, y2);
			if(!eal.isDrawAlarmValue()){
				continue;
			}
			String scoory = getFormattedValue(value);
			TR.setBounds(gp.GFV_YLABEL.getTextSize(scoory));
			x1 -= gp.gRect1.x - gp.gRect2.x; // TR.width / 2;
			y1 = y2 + gp.tickLen; // + TR.height
			Color tmp = gp.GFV_XLABEL.color;
			gp.GFV_XLABEL.color = new Color(lineColor);
			gp.GFV_XLABEL.outText(x1, y1, scoory, GraphFontView.TEXT_ON_BOTTOM);
			gp.GFV_XLABEL.color = tmp;
		}
		g.setStroke(s);
	}

	/**
	 * 将字体的各类属性封装到字体视图，方便绘图参数传递
	 * @param gf 字体定义
	 * @param text 文字
	 * @param fontType 字体类型
	 * @return 字体视图
	 */
	public GraphFontView getGraphFontView(GraphFont gf, String text,
			int fontType) {
		GraphFontView gfv = new GraphFontView(this);
		gfv.setAngle(gf.getAngle());
		gfv.setVertical(gf.isVerticalText());
		Color c = new Color(gf.getColor());
		if (gf.getColor() == 16777215) {
			c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 0);
		}
		gfv.setColor(c);
		Font font = getFont(gf.getFamily(), gf.getSize(), fontType,
				gf.isAutoResize(), gf.isBold(), false, text);
		gfv.setFont(font);
		gfv.setText(text);
		gfv.setTextPosition(getTextDefaultPosition(fontType));
		return gfv;
	}

	protected Font getFont(String face, int size, int fontType,
			boolean autoSize, boolean isBold, boolean isItalic, String text) {
		int style = Font.PLAIN;
		if (isBold) {
			style = style + Font.BOLD;
		}
		if (isItalic) {
			style = style + Font.ITALIC;
		}
		if (face == null || face.trim().length() < 1) {
			face = "dialog";
		}

		if (!autoSize) {
			return new Font(face, style, size);
		}

		int len = 0;
		if (egp.getCategories().size() == 0) {
			len = 4;
		} else {
			if (gp.catNames.get(0) == null) {
				len = 0;
			} else {
				len = Variant.toString(gp.catNames.get(0)).length();
			}
		}
		switch (fontType) {
		case GraphFontView.FONT_TITLE:
			size = Math.round((gp.graphWidth < gp.graphHeight ? gp.graphWidth
					: gp.graphHeight) / 15);
			size = size > 17 ? 17 : size;
			break;
		case GraphFontView.FONT_LEGEND:
			size = Math.round((gp.graphWidth - gp.leftInset)
					/ (gp.serNames.size() == 0 ? 1 : gp.serNames.size())
					/ (len == 0 ? 1 : len));
			size = size > 13 ? 13 : size;
			size = size < 3 ? 0 : size;
			break;
		case GraphFontView.FONT_XLABEL:
			int tmp = 1;
			size = Math.round((gp.graphWidth - gp.leftInset)
					/ (gp.catNames.size() == 0 ? 1 : gp.catNames.size()) / tmp);
			size = size > 13 ? 13 : size;
			size = size < 3 ? 0 : size;
			break;
		case GraphFontView.FONT_XTITLE:

			size = Math.round((gp.graphWidth - gp.leftInset - gp.rightInset)
					/ ((text == null || text.trim().length() == 0) ? 1 : text
							.length()));
			size = size > 13 ? 13 : size;
			size = size < 3 ? 0 : size;
			break;
		case GraphFontView.FONT_YLABEL:
			size = Math.round((gp.graphHeight - gp.topInset - gp.bottomInset)
					/ (gp.tickNum == 0 ? 1 : gp.tickNum));
			size = size > 13 ? 13 : size;
			size = size < 3 ? 0 : size;
			break;
		case GraphFontView.FONT_YTITLE:
			size = Math.round((gp.graphHeight - gp.topInset - gp.bottomInset)
					/ ((text == null || text.trim().length() == 0) ? 1 : text
							.length()));
			size = size > 13 ? 13 : size;
			size = size < 3 ? 0 : size;
			break;
		case GraphFontView.FONT_VALUE:
			break;
		}
		return new Font(face, style, size);
	}

	protected byte getTextDefaultPosition(int fontType) {
		byte pos = GraphFontView.TEXT_FIXED;
		switch (fontType) {
		case GraphFontView.FONT_TITLE:
			pos = GraphFontView.TEXT_ON_BOTTOM;
			break;
		case GraphFontView.FONT_LEGEND:
			pos = GraphFontView.TEXT_ON_RIGHT;
			break;
		case GraphFontView.FONT_XLABEL:
			pos = GraphFontView.TEXT_ON_BOTTOM;
			break;
		case GraphFontView.FONT_XTITLE:
			pos = GraphFontView.TEXT_ON_TOP;
			break;
		case GraphFontView.FONT_YLABEL:
			pos = GraphFontView.TEXT_ON_LEFT;
			break;
		case GraphFontView.FONT_YTITLE:
			pos = GraphFontView.TEXT_ON_RIGHT;
			break;
		case GraphFontView.FONT_VALUE:
			pos = GraphFontView.TEXT_ON_TOP;
			break;
		}
		return pos;
	}

	/**
	 * 创建纵坐标的值标签,如果是时间序列图，则创建x轴的时间刻度值
	 */
	public void createCoorValue() {
		createCoorValue(true);
	}

	// 堆积图的最大值仍然没法设置，因为有正有负，此处调整为合理最大值
	private double getAutoInterval(double max) {
		if (max > 50) {
			return (10 * gp.coorScale);
		} else if (max > 5) {
			return (5 * gp.coorScale);
		}
		return (1 * gp.coorScale);
	}

	/**
	 * 产生坐标刻度值
	 * @param adjustMaxValue 调整刻度最大值为取整值
	 */
	public void createCoorValue(boolean adjustMaxValue) {
		if (!egp.isStackedGraph(this)) {
			gp.maxPositive = gp.maxValue;
			gp.minNegative = gp.minValue;
		}

		double tmp;
		if (gp.maxValue < gp.minValue && adjustMaxValue) {
			tmp = gp.maxValue;
			gp.maxValue = gp.minValue;
			gp.minValue = tmp;
		}

		if (gp.maxValue != 0) {
			tmp = gp.maxValue * 0.1;
			if (tmp == gp.maxValue) {
				throw new RuntimeException("The max value is infinite.");
			}
		}
		if (gp.minValue != 0) {
			tmp = gp.minValue * 10;
			if (tmp == gp.minValue) {
				throw new RuntimeException("The min value is infinite.");
			}
		}

		if (egp.is2YGraph()) {
			createCoorValue2(adjustMaxValue);
		}

		double doubleTmp = 0.0;
		gp.coorScale = 1;

		if (gp.minValue >= 0.0) {
			if (gp.maxValue == 0.0) {
				gp.maxValue = 1.0;
			}
			while (gp.maxValue > 100.00) {
				gp.coorScale *= 10;
				gp.maxValue *= 0.1;
			}

			while (gp.maxValue <= 1.0) {
				gp.coorScale *= 0.1;
				gp.maxValue *= 10;
			}
			if (gp.interval == 0.0) {
				gp.interval = getAutoInterval(gp.maxValue);
			}
			double veryMin = 0.000001;// 由用户设定的间隔值仍然没法整除，但很接近时，不再调整
			if (gp.maxValue % (gp.interval / gp.coorScale) > veryMin
					&& adjustMaxValue) {
				gp.maxValue = gp.maxValue - gp.maxValue
						% (gp.interval / gp.coorScale)
						+ (gp.interval / gp.coorScale);
			}
			gp.tickNum = (int) (gp.maxValue / (gp.interval / gp.coorScale));

			if (gp.minTicknum > 0 && gp.tickNum < gp.minTicknum) {
				gp.tickNum = gp.minTicknum;
				if (gp.maxValue % gp.tickNum > 0 && adjustMaxValue) {
					gp.maxValue = gp.maxValue - gp.maxValue % gp.tickNum
							+ gp.tickNum;
				}
			}
			if (gp.maxValue == 0.0) {
				gp.maxValue = 1.0;
			}
			for (int i = 0; i <= gp.tickNum; i++) {
				if (gp.scaleMark == IGraphProperty.UNIT_AUTO) {
					doubleTmp = i * gp.maxValue / gp.tickNum;
				} else {
					doubleTmp = i * gp.maxValue * (gp.coorScale / gp.scaleMark)
							/ gp.tickNum;
				}
				gp.coorValue.add(i, new Double(doubleTmp + gp.baseValue));
			}
		} else if (gp.maxValue <= 0) {
			doubleTmp = gp.maxValue;
			gp.maxValue = Math.abs(gp.minValue);
			gp.minValue = Math.abs(doubleTmp);
			if (gp.maxValue == 0.0) {
				gp.maxValue = 1.0;
			}
			while (gp.maxValue > 100.00) {
				gp.coorScale *= 10;
				gp.maxValue *= 0.1;
			}
			while (gp.maxValue < 1.0) {
				gp.coorScale *= 0.1;
				gp.maxValue *= 10;
			}
			if (gp.interval == 0.0) {
				gp.interval = getAutoInterval(gp.maxValue);
			}
			if (gp.maxValue % (gp.interval / gp.coorScale) != 0
					&& adjustMaxValue) {
				gp.maxValue = gp.maxValue - gp.maxValue
						% (gp.interval / gp.coorScale)
						+ (gp.interval / gp.coorScale);
			}
			gp.tickNum = (int) (gp.maxValue / (gp.interval / gp.coorScale));
			if (gp.minTicknum > 0 && gp.tickNum < gp.minTicknum) {
				gp.tickNum = gp.minTicknum;
				if (gp.maxValue % gp.tickNum > 0 && adjustMaxValue) {
					gp.maxValue = gp.maxValue - gp.maxValue % gp.tickNum
							+ gp.tickNum;
				}
			}

			if (gp.maxValue == 0.0) {
				gp.maxValue = 1.0;
			}
			for (int i = 0; i <= gp.tickNum; i++) {
				if (gp.scaleMark == IGraphProperty.UNIT_AUTO) {
					doubleTmp = -(gp.tickNum - i) * gp.maxValue / gp.tickNum;
				} else {
					doubleTmp = -(gp.tickNum - i) * gp.maxValue
							* (gp.coorScale / gp.scaleMark) / gp.tickNum;
				}
				gp.coorValue.add(i, new Double(doubleTmp + gp.baseValue));
			}
		} else {

			double absMax = gp.maxValue;
			double absMin = Math.abs(gp.minValue);
			if (gp.maxValue < Math.abs(gp.minValue)) {
				absMin = absMax;
				absMax = Math.abs(gp.minValue);
			}

			if (absMax == 0.0) {
				absMax = 1.0;
			}
			while (absMax > 100.00) {
				gp.coorScale *= 10;
				absMax *= 0.1;
			}
			while (absMax < 1.0) {
				gp.coorScale *= 0.1;
				absMax *= 10;
			}
			if (gp.interval == 0.0) {
				gp.interval = getAutoInterval(absMax);
			}
			if (absMax % (gp.interval / gp.coorScale) != 0 && adjustMaxValue) {
				absMax = absMax - absMax % (gp.interval / gp.coorScale)
						+ (gp.interval / gp.coorScale);
			}
			gp.tickNum = (int) (absMax / (gp.interval / gp.coorScale));

			if (gp.minTicknum > 0 && gp.tickNum < gp.minTicknum) {
				gp.tickNum = gp.minTicknum;
				if (absMax % gp.tickNum > 0 && adjustMaxValue) {
					absMax = absMax - absMax % gp.tickNum + gp.tickNum;
				}
			}

			if (gp.scaleMark != IGraphProperty.UNIT_AUTO) {
				absMin = absMin * (gp.scaleMark / gp.coorScale);
			}
			boolean addPositive = false;
			boolean addNegative = false;
			int intTmp = gp.tickNum;
			gp.coorValue.add(new Double(0 + gp.baseValue));
			for (int i = 1; i <= intTmp; i++) {
				if (gp.scaleMark == IGraphProperty.UNIT_AUTO) {
					doubleTmp = i * absMax / intTmp;
				} else {
					doubleTmp = i * absMax * (gp.coorScale / gp.scaleMark)
							/ intTmp;
				}
				if (doubleTmp < gp.maxValue) {
					gp.coorValue.add(new Double(doubleTmp + gp.baseValue));
				} else if (!addPositive) {
					addPositive = true;
					gp.coorValue.add(new Double(doubleTmp + gp.baseValue));
				}
				doubleTmp *= -1f;
				if (doubleTmp > gp.minValue) {
					gp.coorValue.add(new Double(doubleTmp + gp.baseValue));
				} else if (!addNegative) {
					addNegative = true;
					gp.coorValue.add(new Double(doubleTmp + gp.baseValue));
				}
			}
			GM.sort(gp.coorValue, true);
			gp.tickNum = gp.coorValue.size() - 1;
			gp.maxValue = Math
					.abs(((Number) gp.coorValue.get(0)).doubleValue())
					+ Math.abs(((Number) gp.coorValue.get(gp.tickNum))
							.doubleValue());
			gp.maxValue *= (gp.scaleMark / gp.coorScale);
		}
		gp.minValue = 0;

		boolean isChinese = GC.LANGUAGE == GC.ASIAN_CHINESE;
		if (isChinese) {
			if (gp.scaleMark == IGraphProperty.UNIT_AUTO && gp.coorScale != 1) {
				if (gp.coorScale > 1000) {
					gp.GFV_YTITLE.text = gp.GFV_YTITLE.text + "("
							+ gp.xToChinese(gp.coorScale) + ")";
				} else {
					gp.GFV_YTITLE.text = gp.GFV_YTITLE.text + "(比例：1:"
							+ gp.coorScale + ")";
				}
			} else if (gp.scaleMark != IGraphProperty.UNIT_ORIGIN) {
				if (gp.scaleMark == IGraphProperty.UNIT_THOUSAND) {
					gp.GFV_YTITLE.text += "(千)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_10THOUSAND) {
					gp.GFV_YTITLE.text += "(万)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_MILLION) {
					gp.GFV_YTITLE.text += "(百万)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_10MILLION) {
					gp.GFV_YTITLE.text += "(千万)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_100MILLION) {
					gp.GFV_YTITLE.text += "(亿)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_BILLION) {
					gp.GFV_YTITLE.text += "(十亿)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_001) {
					gp.GFV_YTITLE.text += "(百分之一)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_0001) {
					gp.GFV_YTITLE.text += "(千分之一)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_00001) {
					gp.GFV_YTITLE.text += "(万分之一)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_0000001) {
					gp.GFV_YTITLE.text += "(百万分之一)";
				}
			}
		} else { // English
			double unit = 1;
			if (gp.scaleMark == IGraphProperty.UNIT_AUTO && gp.coorScale != 1) {
				unit = gp.coorScale;
			} else {
				unit = gp.scaleMark;
			}
			if (unit != 1) {
				gp.GFV_YTITLE.text += "(" + getFormattedValue(unit, "#.#E0")
						+ ")";
			}
		}
	}

	public void createCoorValue2(boolean adjustMaxValue) {
		double doubleTmp = 0.0;
		gp.coorScale2 = 1;

		if (gp.minValue2 >= 0.0) {
			if (gp.maxValue2 == 0.0 && adjustMaxValue) {
				gp.maxValue2 = 1.0;

			}
			while (gp.maxValue2 > 100.00) {
				gp.coorScale2 *= 10;
				gp.maxValue2 *= 0.1;
			}

			while (gp.maxValue2 < 1.0) {
				gp.coorScale2 *= 0.1;
				gp.maxValue2 *= 10;
			}
			if (gp.interval2 == 0.0) {
				if (gp.maxValue2 > 50) {
					gp.interval2 = (10 * gp.coorScale2);
				} else {
					gp.interval2 = (5 * gp.coorScale2);
				}
			}
			if (gp.maxValue2 % (gp.interval2 / gp.coorScale2) != 0
					&& adjustMaxValue) {
				gp.maxValue2 = gp.maxValue2 - gp.maxValue2
						% (gp.interval2 / gp.coorScale2)
						+ (gp.interval2 / gp.coorScale2);
			}
			gp.tickNum2 = (int) (gp.maxValue2 / (gp.interval2 / gp.coorScale2));

			if (gp.minTicknum2 > 0 && gp.tickNum2 < gp.minTicknum2) {
				gp.tickNum2 = gp.minTicknum2;
				if (gp.maxValue2 % gp.tickNum2 > 0 && adjustMaxValue) {
					gp.maxValue2 = gp.maxValue2 - gp.maxValue2 % gp.tickNum2
							+ gp.tickNum2;
				}
			}
			if (gp.maxValue2 == 0.0 && adjustMaxValue) {
				gp.maxValue2 = 1.0;

			}
			for (int i = 0; i <= gp.tickNum2; i++) {
				if (gp.scaleMark == IGraphProperty.UNIT_AUTO) {
					doubleTmp = i * gp.maxValue2 / gp.tickNum2;
				} else {
					doubleTmp = i * gp.maxValue2
							* (gp.coorScale2 / gp.scaleMark) / gp.tickNum2;
				}

				gp.coorValue2.add(i, new Double(doubleTmp + gp.baseValue2));
			}
		} else if (gp.maxValue2 <= 0) {
			doubleTmp = gp.maxValue2;
			gp.maxValue2 = Math.abs(gp.minValue2);
			gp.minValue2 = Math.abs(doubleTmp);
			if (gp.maxValue2 == 0.0) {
				gp.maxValue2 = 1.0;

			}
			while (gp.maxValue2 > 100.00) {
				gp.coorScale2 *= 10;
				gp.maxValue2 *= 0.1;
			}
			while (gp.maxValue2 < 1.0) {
				gp.coorScale2 *= 0.1;
				gp.maxValue2 *= 10;
			}
			if (gp.interval2 == 0.0) {
				if (gp.maxValue2 > 50) {
					gp.interval2 = (10 * gp.coorScale2);
				} else {
					gp.interval2 = (5 * gp.coorScale2);
				}
			}
			if (gp.maxValue2 % (gp.interval2 / gp.coorScale2) != 0
					&& adjustMaxValue) {
				gp.maxValue2 = gp.maxValue2 - gp.maxValue2
						% (gp.interval2 / gp.coorScale2)
						+ (gp.interval2 / gp.coorScale2);
			}
			gp.tickNum2 = (int) (gp.maxValue2 / (gp.interval2 / gp.coorScale2));
			if (gp.minTicknum2 > 0 && gp.tickNum2 < gp.minTicknum2) {
				gp.tickNum2 = gp.minTicknum2;
				if (gp.maxValue2 % gp.tickNum2 > 0 && adjustMaxValue) {
					gp.maxValue2 = gp.maxValue2 - gp.maxValue2 % gp.tickNum2
							+ gp.tickNum2;
				}
			}

			if (gp.maxValue2 == 0.0 && adjustMaxValue) {
				gp.maxValue2 = 1.0;
			}
			for (int i = 0; i <= gp.tickNum2; i++) {
				if (gp.scaleMark == IGraphProperty.UNIT_AUTO) {
					doubleTmp = -(gp.tickNum2 - i) * gp.maxValue2 / gp.tickNum2;
				} else {
					doubleTmp = -(gp.tickNum2 - i) * gp.maxValue2
							* (gp.coorScale2 / gp.scaleMark) / gp.tickNum2;
				}
				gp.coorValue2.add(i, new Double(doubleTmp));
			}
		} else {
			double absMax = gp.maxValue2;
			double absMin = Math.abs(gp.minValue2);
			if (gp.maxValue2 < Math.abs(gp.minValue2)) {
				absMin = absMax;
				absMax = Math.abs(gp.minValue2);
			}

			if (absMax == 0.0) {
				absMax = 1.0;
			}
			while (absMax > 100.00) {
				gp.coorScale2 *= 10;
				absMax *= 0.1;
			}
			while (absMax < 1.0) {
				gp.coorScale2 *= 0.1;
				absMax *= 10;
			}
			if (gp.interval2 == 0.0) {
				if (absMax > 50) {
					gp.interval2 = (10 * gp.coorScale2);
				} else {
					gp.interval2 = (5 * gp.coorScale2);
				}
			}
			if (absMax % (gp.interval2 / gp.coorScale2) != 0 && adjustMaxValue) {
				absMax = absMax - absMax % (gp.interval2 / gp.coorScale2)
						+ (gp.interval2 / gp.coorScale2);
			}
			gp.tickNum2 = (int) (absMax / (gp.interval2 / gp.coorScale2));

			if (gp.minTicknum > 0 && gp.tickNum2 < gp.minTicknum) {
				gp.tickNum2 = gp.minTicknum;
				if (absMax % gp.tickNum2 > 0 && adjustMaxValue) {
					absMax = absMax - absMax % gp.tickNum2 + gp.tickNum2;
				}
			}

			if (gp.scaleMark != IGraphProperty.UNIT_AUTO) {
				absMin = absMin * (gp.scaleMark / gp.coorScale2);
			}
			boolean addPositive = false;
			boolean addNegative = false;
			int intTmp = gp.tickNum2;
			gp.coorValue2.add(new Double(0 + gp.baseValue2));
			for (int i = 1; i <= intTmp; i++) {
				if (gp.scaleMark == IGraphProperty.UNIT_AUTO) {
					doubleTmp = i * absMax / intTmp;
				} else {
					doubleTmp = i * absMax * (gp.coorScale2 / gp.scaleMark)
							/ intTmp;
				}
				if (doubleTmp < gp.maxValue2) {
					gp.coorValue2.add(new Double(doubleTmp + gp.baseValue2));
				} else if (!addPositive) {
					addPositive = true;
					gp.coorValue2.add(new Double(doubleTmp + gp.baseValue2));
				}
				doubleTmp *= -1f;
				if (doubleTmp > gp.minValue2) {
					gp.coorValue2.add(new Double(doubleTmp + gp.baseValue2));
				} else if (!addNegative) {
					addNegative = true;
					gp.coorValue2.add(new Double(doubleTmp + gp.baseValue2));
				}
			}
			GM.sort(gp.coorValue2, true);
			gp.tickNum2 = gp.coorValue2.size() - 1;
			gp.maxValue2 = Math.abs(((Number) gp.coorValue2.get(0))
					.doubleValue())
					+ Math.abs(((Number) gp.coorValue2.get(gp.tickNum2))
							.doubleValue());
			gp.maxValue2 *= (gp.scaleMark / gp.coorScale2);
		}
		gp.minValue2 = 0;
		boolean ISCHINESE = (Locale.getDefault().equals(Locale.CHINA)
				|| Locale.getDefault().equals(Locale.CHINESE) || Locale
				.getDefault().equals(Locale.SIMPLIFIED_CHINESE));
		if (ISCHINESE) {
			if (gp.scaleMark == IGraphProperty.UNIT_AUTO && gp.coorScale2 != 1) {
				if (gp.coorScale2 > 1000) {
					gp.GFV_YTITLE.text2 = gp.GFV_YTITLE.text2 + "("
							+ gp.xToChinese(gp.coorScale2) + ")";
				} else {
					gp.GFV_YTITLE.text2 = gp.GFV_YTITLE.text2 + "(比例：1:"
							+ gp.coorScale2 + ")";
				}
			} else if (gp.scaleMark != IGraphProperty.UNIT_ORIGIN) {
				if (gp.scaleMark == IGraphProperty.UNIT_THOUSAND) {
					gp.GFV_YTITLE.text2 += "(千)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_10THOUSAND) {
					gp.GFV_YTITLE.text2 += "(万)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_MILLION) {
					gp.GFV_YTITLE.text2 += "(百万)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_10MILLION) {
					gp.GFV_YTITLE.text2 += "(千万)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_BILLION) {
					gp.GFV_YTITLE.text2 += "(亿)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_100MILLION) {
					gp.GFV_YTITLE.text2 += "(十亿)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_001) {
					gp.GFV_YTITLE.text2 += "(百分之一)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_0001) {
					gp.GFV_YTITLE.text2 += "(千分之一)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_00001) {
					gp.GFV_YTITLE.text2 += "(万分之一)";
				} else if (gp.scaleMark == IGraphProperty.UNIT_0000001) {
					gp.GFV_YTITLE.text2 += "(百万分之一)";
				}
			}
		} else { // English
			double unit = 1;
			if (gp.scaleMark == IGraphProperty.UNIT_AUTO && gp.coorScale != 1) {
				unit = gp.coorScale;
			} else {
				unit = gp.scaleMark;
			}
			if (unit != 1) {
				gp.GFV_YTITLE.text += "(" + getFormattedValue(unit, "#.#E0")
						+ ")";
			}
		}
	}

	String getFormattedCatVal(Object catVal) {
		return Variant.toString(catVal);
	}

	protected float getDataTableX(int catIndex) {
		float axisLen = gp.graphWidth - gp.leftInset - gp.rightInset;
		float catWidth = axisLen / gp.catNum;
		float pos = gp.leftInset + (catIndex + 1) * catWidth - catWidth / 2;
		return pos;
	}

	/**
	 * 绘制数据表
	 * 目前只考虑在柱状图的下方画数据表
	 */
	public void drawDataTable() {
		float axisWidth = gp.graphWidth - gp.leftInset - gp.rightInset;
		CellSet cs = getDataTable(axisWidth);
		float tableH = getDataTableHeight(cs);

		Utils.setGraphAntiAliasingOff(g);

		IColCell cc1 = cs.getColCell(1);
		float col1W = cc1.getWidth();
		float x = gp.leftInset - col1W;
		float y = gp.graphHeight - gp.bottomInset - tableH;
		// draw table
		int rows = cs.getRowCount();
		int cols = cs.getColCount();
		for (int r = 1; r <= rows; r++) {
			IRowCell rc = cs.getRowCell(r);
			float h = rc.getHeight();
			for (int c = 1; c <= cols; c++) {
				if (c == 1) {
					x = gp.leftInset - col1W;
				}
				IColCell cc = cs.getColCell(c);
				INormalCell nc = cs.getCell(r, c);
				float w = cc.getWidth();
				if (r == 1 && c == 1) {
				} else {
					Color cr = egp.getAxisColor(IGraphProperty.AXIS_BOTTOM);
					Rectangle2D.Double rect = new Rectangle2D.Double(x, y, w, h);
					drawShape(rect, cr);
					boolean isCenter = true;
					if(r == 1 || c == 1){
						isCenter =	true;
					}else{
						isCenter =	gp.isDataCenter;
					}
					 
					Font font;
					Color color;
					if (isCenter) {
						font = gp.GFV_XLABEL.font;
						color = gp.GFV_XLABEL.color;
					} else {
						font = gp.GFV_VALUE.font;
						color = gp.GFV_VALUE.color;
					}
					drawText(nc.getExpString(), (int) x, (int) y, (int) w,
							(int) h, isCenter, font, color);
				}
				x += w;
			}
			y += h;
		}

		gp.bottomInset += tableH;
		Utils.setGraphAntiAliasingOn(g);
	}

	private void drawText(String text, int x, int y, int w, int h,
			boolean isCenter, Font font, Color c) {
		if (text == null || text.trim().length() == 0) {
			return;
		}

		FontMetrics fm = g.getFontMetrics(font);
		int fw = 0;// stringWidth(fm, text);
		int ascent = fm.getAscent();
		int height = fm.getHeight();
		ArrayList<String> al = DrawStringUtils2.wrapString(text, fm, w, false);
		int lineH = DrawStringUtils2.getTextRowHeight(fm);
		if (al.size() < 2) {
			if (lineH > h) {
				lineH = h;
			}
		}
		int yy = y;
		yy = y + (h - lineH * al.size()) / 2;
		if (yy < y) {
			yy = y;
		}
		for (int i = 0; i < al.size(); i++) {
			if (i > 0 && yy + lineH > y + h) { // 第一行总是绘制，其余行如果在框外不画，否则遮盖别的格子文字
				break;
			}

			String wrapedText = (String) al.get(i);
			fw = fm.stringWidth(wrapedText);
			int x1 = x;
			if (isCenter) {
				x1 = x + (w - fw) / 2;
			} else {
				x1 = x + w - fw;
			}
			int y1 = yy + (lineH - height) / 2 + ascent;
			g.setColor(c);
			g.setFont(font);
			g.drawString(wrapedText, x1, y1);
			yy += lineH;
		}

	}

	/**
	 * 调整坐标缩进位置
	 * */
	public void adjustCoorInset() {
		int maxValW = 0;
		int maxValH = 0;
		int maxCatW = 0;
		int maxCatH = 0;
		int intTmp = 0;

		for (int i = 0; i < gp.tickNum; i++) {
			Object coory = gp.coorValue.get(i);
			String scoory = Variant.toString(coory);
			if (coory instanceof Number && gp.dataMarkFormat != null
					&& gp.dataMarkFormat.trim().length() > 0) {
				DecimalFormatSymbols dfs = new DecimalFormatSymbols(
						Locale.getDefault());
				DecimalFormat df = new DecimalFormat(gp.dataMarkFormat, dfs);
				scoory = df.format(((Number) coory).doubleValue());
			}
			TR = gp.GFV_YLABEL.getTextSize(scoory);
			intTmp = TR.width;
			if (maxValW < intTmp) {
				maxValW = intTmp;
			}
			intTmp = TR.height;
			if (maxValH < intTmp) {
				maxValH = intTmp;
			}
		}

		for (int j = 0; j < gp.catNum; j++) {
			Object o = gp.catNames.get(j);
			TR = gp.GFV_XLABEL.getTextSize(getFormattedCatVal(o));
			intTmp = TR.width;
			if (maxCatW < intTmp) {
				maxCatW = intTmp;
			}
			intTmp = TR.height;
			if (maxCatH < intTmp) {
				maxCatH = intTmp;
			}
		}

		if (egp.isBarGraph(this)) {
			gp.leftInset += maxCatW + 4;
			gp.bottomInset += maxValH + 4;
		} else {
			gp.leftInset += maxValW + 4;
			if (!gp.isDrawTable) {// 目前的数据表仅支持在X轴上绘制
				gp.bottomInset += maxCatH + 4;
			}

			if (egp.is2YGraph()) {
				for (int i = 0; i < gp.tickNum2; i++) {
					Object coory = gp.coorValue2.get(i);
					String scoory = Variant.toString(coory);
					if (coory instanceof Number && gp.dataMarkFormat != null
							&& gp.dataMarkFormat.trim().length() > 0) {
						DecimalFormatSymbols dfs = new DecimalFormatSymbols(
								Locale.getDefault());
						DecimalFormat df = new DecimalFormat(gp.dataMarkFormat,
								dfs);
						scoory = df.format(((Number) coory).doubleValue());
					}
					TR = gp.GFV_YLABEL.getTextSize(scoory);
					intTmp = TR.width;
					if (maxValW < intTmp) {
						maxValW = intTmp;
					}
					intTmp = TR.height;
					if (maxValH < intTmp) {
						maxValH = intTmp;
					}
				}
				gp.rightInset += maxValW + 4;
			}

			if (gp.isDrawTable) {// 目前的数据表仅支持在X轴上绘制
				drawDataTable();
			}
		}
	}

	/**
	 * 初始化统计图边距
	 */
	public void initGraphInset() {
		gp.leftInset = gp.leftMargin;
		gp.topInset = gp.topMargin;
		gp.rightInset = gp.rightMargin;
		gp.bottomInset = gp.bottomMargin;
	}

	/**
	 * 指定格式来格式化数据
	 * 
	 * @param value
	 *            double 要格式化的数值
	 * @param fmt
	 *            String 格式
	 * @return String 格式化后的串
	 */
	public String getFormattedValue(double value, String fmt) {
		double tmp = value;
		String sVal = new Double(tmp).toString();
		if (StringUtils.isValidString(fmt)) {
			DecimalFormatSymbols dfs = new DecimalFormatSymbols(
					Locale.getDefault());
			DecimalFormat df = new DecimalFormat(fmt, dfs);
			sVal = df.format(tmp);
		}
		return sVal;
	}

	/**
	 * 使用配置的格式来格式化数值
	 * @param value
	 *            double，要格式化的数值
	 * @return String，格式化完后的串
	 */
	public String getFormattedValue(double value) {
		return getFormattedValue(value, gp.dataMarkFormat);
	}

	/**
	 * 将原值根据单位算出比例值
	 * @param orginalVal 原始数值
	 * @param isFirstAxis 是否第一轴数值
	 * @return 比例化后的数值
	 */
	public double getScaledValue(double orginalVal, boolean isFirstAxis) {
		double scaledVal;
		if (gp.scaleMark == IGraphProperty.UNIT_AUTO) {
			double coorScale = isFirstAxis ? gp.coorScale : gp.coorScale2;
			scaledVal = orginalVal / coorScale;
		} else {
			scaledVal = orginalVal / gp.scaleMark;
		}
		return scaledVal;
	}

	/**
	 * 获取系列定义的tip显示值
	 * @param egs 系列
	 * @return 显示值
	 */
	public String getDispValue(ExtGraphSery egs) {
		return getDispValue(null, egs, 0);
	}

	/**
	 * 获取分类以及系列的超链接tip显示值
	 * @param egc 分类
	 * @param egs 系列
	 * @param serNum 系列个数
	 * @return 格式串显示值
	 */
	public String getDispValue(ExtGraphCategory egc, ExtGraphSery egs,
			int serNum) {
		if (egs.isNull()) {
			return null;
		}
		double scaledVal = getScaledValue(egs.getValue(), true);
		String txt = getDispValue(scaledVal, egs.getTips(), gp.dataMarkFormat);
		if (egc != null
				&& gp.dispValueType == IGraphProperty.DISPDATA_NAME_VALUE) {
			txt = getDispName(egc, egs, serNum) + "," + txt;
		}
		return txt;
	}

	/**
	 * 获取系列定义的tip显示值，双轴图第2轴用
	 * @param egs 系列
	 * @return 显示值
	 */
	public String getDispValue2(ExtGraphSery egs) {
		return getDispValue2(null, egs, 0);
	}

	/**
	 * 获取分类以及系列的超链接tip显示值，双轴图第2轴数据用
	 * @param egc 分类
	 * @param egs 系列
	 * @param serNum 系列个数
	 * @return 格式串显示值
	 */
	public String getDispValue2(ExtGraphCategory egc, ExtGraphSery egs,
			int serNum) {
		if (egs.isNull()) {
			return null;
		}
		double scaledVal = getScaledValue(egs.getValue(), false);
		String txt = getDispValue(scaledVal, egs.getTips(), gp.dataMarkFormat2);
		if (egc != null
				&& gp.dispValueType2 == IGraphProperty.DISPDATA_NAME_VALUE) {
			txt = getDispName(egc, egs, serNum) + "," + txt;
		}
		return txt;
	}

	/**
	 * 根据配置将显示值显示为title还是value的格式化值
	 * @param value 原始数值
	 * @param title 缺省显示标题
	 * @param fmt 格式
	 * @return string 格式串显示值
	 */
	public String getDispValue(double value, String title, String fmt) {
		if (gp.dispValueType == IGraphProperty.DISPDATA_TITLE) {
			return title;
		}
		String txt = getFormattedValue(value, fmt);
		return txt;
	}

	/**
	 * 绘制两点间的直线
	 * @param b 起始点
	 * @param e 结束点
	 */
	public void drawLine(Point2D.Double b, Point2D.Double e) {
		drawLine(b, e, egp.isDrawShade());
	}

	/**
	 * 绘制两点间的直线
	 * @param b 起始点
	 * @param e 结束点
	 * @param drawShade 是否绘制阴影
	 */
	public void drawLine(Point2D.Double b, Point2D.Double e, boolean drawShade) {
		if (b == null || e == null) {
			return;
		}
		Stroke old = g.getStroke();
		Stroke stroke = getLineStroke();
		if (stroke != null) {
			g.setStroke(stroke);
			if (drawShade) {
				Color c = g.getColor();
				g.setColor(Color.lightGray);
				Utils.drawLine(g,b.x + SHADE_SPAN, b.y + SHADE_SPAN,
						e.x + SHADE_SPAN, e.y + SHADE_SPAN);
				g.setColor(c);
			}
			Utils.drawLine(g,b.x, b.y, e.x, e.y);
		}
		g.setStroke(old);
	}

	/**
	 * 当前环境是否为中文环境
	 * @return 如果是返回true，否则返回false
	 */
	public static boolean isChinese() {
		return (Locale.getDefault().equals(Locale.CHINA)
				|| Locale.getDefault().equals(Locale.CHINESE) || Locale
				.getDefault().equals(Locale.SIMPLIFIED_CHINESE));
	}

	/**
	 * 获取分类和系列的显示名
	 * @param egc 分类
	 * @param egs 系列
	 * @param serNum 系类个数
	 * @return 显示名
	 */
	public static String getDispName(ExtGraphCategory egc, ExtGraphSery egs,
			int serNum) {
		if (serNum == 1) {
			return egc.getNameString();
		}
		return egs.getName();
	}

	/**
	 * 计算数据表的高度
	 * @param cs 数据表的网格对象
	 * @return 高度
	 */
	public float getDataTableHeight(CellSet cs) {
		int rows = cs.getRowCount();
		float total = 0;
		for (int r = 1; r <= rows; r++) {
			IRowCell rc = cs.getRowCell(r);
			total += rc.getHeight();
		}
		return total;
	}

	/**
	 * 获取布局好行高，列宽后的的数据表网格
	 * @param axisLen 轴的长度
	 * @return 数据表网格
	 */
	public CellSet getDataTable(double axisLen) {
		int rows = gp.serNum + 1;
		int cols = gp.catNum + 1;

		if (egp.is2YGraph() || egp.category2 != null) {
			rows += gp.serNum2;
		}
		PgmCellSet cs = new PgmCellSet(rows, cols);
		double catWidth = axisLen / gp.catNum;
		IColCell cc = cs.getColCell(1);
		cc.setWidth(gp.leftInset-5);

		ArrayList cats = egp.categories;
		ArrayList cats2 = egp.category2;
		for (int i = 0; i < gp.catNum; i++) {
			cc = cs.getColCell(i + 2);
			cc.setWidth((float) catWidth);

			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			String value = egc.getNameString();
			INormalCell nc = cs.getCell(1, i + 2);
			nc.setExpString(value);

			for (int j = 0; j < gp.serNum; j++) {
				String serName = gp.serNames.get(j).toString();
				if (i == 0) {
					nc = cs.getCell(j + 2, 1);
					nc.setExpString(serName);
				}
				ExtGraphSery egs = egc.getExtGraphSery(serName);
				if (egs.isNull()) {
					continue;
				}
				double scaledVal = getScaledValue(egs.getValue(), true);
				String txt = getFormattedValue(scaledVal, gp.dataMarkFormat);
				nc = cs.getCell(j + 2, i + 2);
				nc.setExpString(txt);
			}

			if (egp.is2YGraph() || cats2 != null) {
				ExtGraphCategory egc2 = (ExtGraphCategory) cats2.get(i);
				for (int j = 0; j < gp.serNum2; j++) {
					String serName = gp.serNames2.get(j).toString();
					if (i == 0) {
						nc = cs.getCell(gp.serNum + j + 2, 1);
						nc.setExpString(serName);
					}
					ExtGraphSery egs = egc2.getExtGraphSery(serName);
					if (egs.isNull()) {
						continue;
					}
					double scaledVal = getScaledValue(egs.getValue(), true);
					String txt = getFormattedValue(scaledVal,
							gp.dataMarkFormat2);
					nc = cs.getCell(gp.serNum + j + 2, i + 2);
					nc.setExpString(txt);
				}
			}
		}
		// 数据表的分类以及系列名称采用 x轴标签字体，自动撑大行高
		for (int r = 1; r <= rows; r++) {
			IRowCell rc = cs.getRowCell(r);
			float rowH = rc.getHeight();
			for (int c = 1; c <= cols; c++) {
				Font font;
				if (r == 1 || c == 1) {
					font = gp.GFV_XLABEL.font;
				} else {
					font = gp.GFV_VALUE.font;
				}
				cc = cs.getColCell(c);
				float w = cc.getWidth();
				INormalCell nc = cs.getCell(r, c);
				float cellH = getCellH(nc, font, w);
				if (cellH > rowH) {
					rowH = cellH;
				}
			}
			rc.setHeight(rowH);
		}
		return cs;
	}

	private float getCellH(INormalCell nc, Font font, float w) {
		FontMetrics fm = g.getFontMetrics(font);
		String text = nc.getExpString();
		ArrayList<String> al = DrawStringUtils2.wrapString(text, fm, w, false);
		int lineH = DrawStringUtils2.getTextRowHeight(fm);
		if (al.size() < 2) {
			return lineH;
		}
		float totalH = lineH * al.size();
		totalH += 2;
		return totalH;
	}

}
