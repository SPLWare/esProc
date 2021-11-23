package com.raqsoft.cellset.graph.draw;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

import com.raqsoft.cellset.graph.*;
import com.raqsoft.chart.Consts;
import com.raqsoft.chart.Utils;
/**
 * 点图，气泡图实现
 * @author Joancy
 *
 */

public class DrawDot extends DrawBase {
	/**
	 * 实现绘图功能
	 */
	public void draw(StringBuffer htmlLink) {
		drawing(this, htmlLink);
	}

	/**
	 * 根据绘图基类db绘图，并将画图后的超链接存入htmlLink
	 * @param db 抽象的绘图基类
	 * @param htmlLink 超链接缓存
	 */
	public static void drawing(DrawBase db,StringBuffer htmlLink) {
		GraphParam gp = db.gp;
		ExtGraphProperty egp = db.egp;
		Graphics2D g = db.g;
		ArrayList<ValueLabel> labelList = db.labelList;
		int VALUE_RADIUS = db.VALUE_RADIUS;
		double seriesWidth;
		double coorWidth;
		double categorySpan;
		double dely;
		double tmpInt;
		double x, y;
		gp.coorWidth = 0;

		Point2D.Double prePoints1[];
		Point2D.Double prePoints2[];

		db.initGraphInset();
		db.createCoorValue();
		db.drawLegend(htmlLink);
		db.drawTitle();
		db.drawLabel();
		db.keepGraphSpace();
		db.adjustCoorInset();

		gp.graphRect = new Rectangle2D.Double(gp.leftInset, gp.topInset, gp.graphWidth
				- gp.leftInset - gp.rightInset, gp.graphHeight - gp.topInset
				- gp.bottomInset);

		if (gp.graphRect.width < 10 || gp.graphRect.height < 10) {
			return;
		}

		if (gp.coorWidth < 0 || gp.coorWidth > 10000) {
			gp.coorWidth = 0;
		}
		seriesWidth = gp.graphRect.width
				/ (((gp.catNum + 1) * gp.categorySpan / 100.0) + gp.coorWidth
						/ 200.0 + gp.catNum * gp.serNum);

		coorWidth = seriesWidth * (gp.coorWidth / 200.0);
		categorySpan = seriesWidth * (gp.categorySpan / 100.0);

		tmpInt =  ((gp.catNum + 1) * categorySpan + coorWidth + gp.catNum
				* gp.serNum * seriesWidth);
		gp.graphRect.x += (gp.graphRect.width - tmpInt) / 2;
		gp.graphRect.width = tmpInt;

		dely = (gp.graphRect.height - coorWidth) / gp.tickNum;
		tmpInt =  (dely * gp.tickNum + coorWidth);
		gp.graphRect.y += (gp.graphRect.height - tmpInt) / 2;
		gp.graphRect.height = tmpInt;

		gp.gRect1 = (Rectangle2D.Double)gp.graphRect.clone();
		gp.gRect2 = (Rectangle2D.Double)gp.graphRect.clone();

		gp.gRect1.y += coorWidth;
		gp.gRect1.width -= coorWidth;
		gp.gRect1.height -= coorWidth;
		gp.gRect2.x += coorWidth;
		gp.gRect2.width -= coorWidth;
		gp.gRect2.height -= coorWidth;

		/* 画坐标轴 */
		db.drawGraphRect();
		/* 画Y轴 */
		for (int i = 0; i <= gp.tickNum; i++) {
			db.drawGridLine(dely, i);

			Number coory = (Number) gp.coorValue.get(i);
			String scoory = db.getFormattedValue(coory.doubleValue());
			x = gp.gRect1.x - gp.tickLen;// - TR.width
			y =  (gp.gRect1.y + gp.gRect1.height - i * dely);// + TR.height
																	// / 2
			gp.GFV_YLABEL.outText(x, y, scoory);
			// 设置基线
			if (coory.doubleValue() == gp.baseValue) {
				gp.valueBaseLine =  (gp.gRect1.y + gp.gRect1.height - i
						* dely);
			}
		}
		// 画警戒线
		db.drawWarnLine();
		/* 画X轴 */
		prePoints1 = new Point2D.Double[gp.serNum];
		prePoints2 = new Point2D.Double[gp.serNum];
		ArrayList cats = egp.categories;
		int cc = cats.size();
		Color c;
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			double delx =  ((i + 1) * categorySpan + i * seriesWidth
					* gp.serNum + seriesWidth * gp.serNum / 2.0);
			boolean valvis = (i % (gp.graphXInterval + 1) == 0);//柱顶是否显示值跟画Table分开
			boolean vis = valvis && !gp.isDrawTable;
			if (vis) {
				c = egp.getAxisColor(GraphProperty.AXIS_BOTTOM);
				Utils.setStroke(g, c, Consts.LINE_SOLID, 1.0f);
				db.drawLine(gp.gRect1.x + delx, gp.gRect1.y + gp.gRect1.height,
						gp.gRect1.x + delx, gp.gRect1.y + gp.gRect1.height
								+ gp.tickLen,c);
				// 画背景虚线
				db.drawGridLineCategoryV(gp.gRect1.x + delx);
			}

			String value = egc.getNameString();
			x = gp.gRect1.x + delx;// - TR.width / 2;
			y = gp.gRect1.y + gp.gRect1.height + gp.tickLen;// + TR.height;
			gp.GFV_XLABEL.outText(x, y, value, vis);

			for (int j = 0; j < gp.serNum; j++) {
				ExtGraphSery egs = egc.getExtGraphSery(gp.serNames.get(j));
				if (egs.isNull()) {
					continue;
				}
				double val = egs.getValue();
				double tmp = val - gp.baseValue;
				double len =  (dely * gp.tickNum * (tmp - gp.minValue) / (gp.maxValue * gp.coorScale));

				double lb = gp.gRect1.x + (i + 1) * categorySpan + (2 * i + 1)
						* gp.serNum * seriesWidth / 2;
				if( gp.isDrawTable ){
					lb = db.getDataTableX( i );
				}

				Point2D.Double pt1 = new Point2D.Double( (lb), gp.valueBaseLine - len);
				Point2D.Double pt2 = new Point2D.Double( (lb + coorWidth),
						 (gp.valueBaseLine - len - coorWidth));
				// 显示值标示
				if (gp.dispValueOntop && !egs.isNull() && valvis) {
					String sval = db.getDispValue(egc,egs,gp.serNum);
					x = pt1.x;
					y = pt1.y;
					if (!gp.isMultiSeries) {
						c = db.getColor(i);
					} else {
						c = db.getColor(j);
					}
					ValueLabel vl = new ValueLabel(sval, new Point2D.Double(x, y
							- VALUE_RADIUS), c);
					labelList.add(vl);
				}
				
				if (gp.coorWidth == 0) { // 线条上的小方块
					Color backColor,foreColor;
					if (!gp.isMultiSeries) {
						backColor = db.getColor(i);
					} else {
						backColor = db.getColor(j);
					}

					foreColor = egp.getAxisColor(GraphProperty.AXIS_COLBORDER);
					int shape = Consts.PT_CIRCLE;
					int radius = VALUE_RADIUS;
					int bs = Consts.LINE_SOLID;
					float bw = 1.0f;
					db.drawPoint(pt1,shape,radius,bs,bw,backColor,foreColor);
					db.drawPoint(pt2,shape,radius,bs,bw,backColor,foreColor);
					
					db.htmlLink(pt1.x - VALUE_RADIUS, pt1.y - VALUE_RADIUS,
							2 * VALUE_RADIUS, 2 * VALUE_RADIUS, htmlLink,
							egc.getNameString(), egs);
				}
				prePoints1[j] = pt1;
				prePoints2[j] = pt2;
			}
		}
		db.outLabels();
		/* 重画一下基线 */
		db.drawLine(gp.gRect1.x, gp.valueBaseLine, gp.gRect1.x + gp.gRect1.width,
				gp.valueBaseLine, egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
		db.drawLine(gp.gRect1.x + gp.gRect1.width, gp.valueBaseLine,
				 (gp.gRect1.x + gp.gRect1.width + coorWidth),
				 (gp.valueBaseLine - coorWidth),
				egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
	}
}
