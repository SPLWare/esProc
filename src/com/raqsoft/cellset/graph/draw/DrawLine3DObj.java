package com.raqsoft.cellset.graph.draw;

import java.awt.*;
import java.util.*;

import com.raqsoft.cellset.graph.*;
import com.raqsoft.chart.Consts;
import com.raqsoft.chart.Utils;

/**
 * 三维折线图实现
 * @author Joancy
 *
 */
public class DrawLine3DObj extends DrawBase {
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
		ArrayList<ValuePoint> pointList = db.pointList;

		double seriesWidth;
		double coorWidth;
		double categorySpan;
		double dely;
		int tmpInt;
		int x, y;
		Point prePoints1[];
		Point prePoints2[];
		Point lastPoint1[];
		Point lastPoint2[];

		db.initGraphInset();

		db.createCoorValue();

		db.drawLegend(htmlLink);
		db.drawTitle();
		db.drawLabel();
		db.keepGraphSpace();

		db.adjustCoorInset();
		gp.graphRect = new Rectangle(gp.leftInset, gp.topInset, gp.graphWidth
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

		tmpInt = (int) ((gp.catNum + 1) * categorySpan + coorWidth + gp.catNum
				* gp.serNum * seriesWidth);
		gp.graphRect.x += (gp.graphRect.width - tmpInt) / 2;
		gp.graphRect.width = tmpInt;

		dely = (gp.graphRect.height - coorWidth) / gp.tickNum;
		tmpInt = (int) (dely * gp.tickNum + coorWidth);
		gp.graphRect.y += (gp.graphRect.height - tmpInt) / 2;
		gp.graphRect.height = tmpInt;

		gp.gRect1 = new Rectangle(gp.graphRect);
		gp.gRect2 = new Rectangle(gp.graphRect);

		gp.gRect1.y += coorWidth;
		gp.gRect1.width -= coorWidth;
		gp.gRect1.height -= coorWidth;
		gp.gRect2.x += coorWidth;
		gp.gRect2.width -= coorWidth;
		gp.gRect2.height -= coorWidth;

		db.drawGraphRect();
		Point p;
		/* 画Y轴 */
		for (int i = 0; i <= gp.tickNum; i++) {
			db.drawGridLine(dely, i);
			Number coory = (Number) gp.coorValue.get(i);
			String scoory = db.getFormattedValue(coory.doubleValue());

			p = db.getVTickPoint(i*dely);
			gp.GFV_YLABEL.outText(p.x-gp.tickLen, p.y, scoory);
			// 设置基线
			if (coory.doubleValue() == gp.baseValue + gp.minValue) {
				gp.valueBaseLine = (int) (gp.gRect1.y + gp.gRect1.height - i
						* dely);
			}
		}
		// 画警戒线
		db.drawWarnLine();
		if (gp.graphTransparent) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					0.60F));
		}

		/* 画X轴 */
		prePoints1 = new Point[gp.serNum];
		prePoints2 = new Point[gp.serNum];
		lastPoint1 = new Point[gp.serNum];
		lastPoint2 = new Point[gp.serNum];
		ArrayList cats = egp.categories;
		int cc = cats.size();
		Color c;
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			int posx = DrawLine.getPosX(gp,i,cc,categorySpan,seriesWidth);
			double delx = posx - gp.gRect1.x;
			
			boolean vis = i % (gp.graphXInterval + 1) == 0;
			p = db.getHTickPoint(delx);
			if (vis) {
				c = egp.getAxisColor(GraphProperty.AXIS_BOTTOM);
				Utils.setStroke(g, c, Consts.LINE_SOLID, 1.0f);
				db.drawLine(p.x,p.y,p.x,p.y+gp.tickLen,c);
				// 画背景虚线
				db.drawGridLineCategoryV(gp.gRect2.x + (int)delx);
			}

			String value = egc.getNameString();
			gp.GFV_XLABEL.outText(p.x, p.y+gp.tickLen, value, vis);

			for (int j = 0; j < gp.serNum; j++) {
				ExtGraphSery egs = egc.getExtGraphSery(gp.serNames.get(j));
				double val = egs.getValue();
				double tmp = val - gp.baseValue;
				int len = (int) (dely * gp.tickNum * (tmp - gp.minValue) / (gp.maxValue * gp.coorScale));

				Point pt1 = new Point(posx, gp.valueBaseLine - len);
				Point pt2 = new Point(posx + (int)coorWidth,
						(int) (gp.valueBaseLine - len - coorWidth));
				if (egs.isNull()) {
					pt1 = null;
					pt2 = null;
				}

				
				Point p1, p2;
				if (i > 0) {
					if (!gp.isMultiSeries) {
						c = db.getColor(i);
					} else {
						c = db.getColor(j);
					}
					if (egp.isIgnoreNull()) {
						p1 = lastPoint1[j];
						p2 = lastPoint2[j];
					} else {
						p1 = prePoints1[j];
						p2 = prePoints2[j];
					}
					drawLineArea(db,p1, p2, pt1, pt2, c, seriesWidth);
				}

				if (!egs.isNull() && gp.drawLineDot && vis) {
					Color backColor;
					if (gp.serNum == 1) {
						backColor = db.getColor(i);
					} else {
						backColor = db.getColor(j);
					}

					Color foreColor = db.getColor(j);
					ValuePoint vp = new ValuePoint(pt1, backColor);//,foreColor);
					pointList.add( vp );
					db.htmlLink(pt1.x - VALUE_RADIUS, pt1.y - VALUE_RADIUS,
							VALUE_RADIUS * 2, VALUE_RADIUS * 2, htmlLink,
							egc.getNameString(), egs);
				}

				// 输出文字
				if (gp.dispValueOntop && !egs.isNull() && vis) {
					String sval = db.getDispValue(egc,egs,gp.serNum);
					x = pt2.x;
					y = pt2.y;
					if (!gp.isMultiSeries) {
						c = db.getColor(i);
					} else {
						c = db.getColor(j);
					}
					ValueLabel vl = new ValueLabel(sval, new Point(x, y
							- VALUE_RADIUS), c);
					labelList.add(vl);
				}

				prePoints1[j] = pt1;
				prePoints2[j] = pt2;
				if (pt1 != null) {
					lastPoint1[j] = pt1;
					lastPoint2[j] = pt2;
				}
			}
		}

		db.outPoints();
		db.outLabels();
		/* 重画一下基线 */
		if (gp.valueBaseLine != gp.gRect1.y + gp.gRect1.height) {
			db.drawLine(gp.gRect1.x, gp.valueBaseLine, gp.gRect1.x
					+ gp.gRect1.width, gp.valueBaseLine,
					egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
			db.drawLine(gp.gRect1.x + gp.gRect1.width, gp.valueBaseLine,
					(int) (gp.gRect1.x + gp.gRect1.width + coorWidth),
					(int) (gp.valueBaseLine - coorWidth),
					egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
		}
	}

	private static void drawLineArea(DrawBase db,Point p1, Point p2, Point e1, Point e2, Color c,
			double seriesWidth) {
		if (p1 == null || e1 == null) {
			return;
		}
		int ptx1[] = { p1.x, p2.x, e2.x, e1.x };
		int pty1[] = { p1.y, p2.y, e2.y, e1.y };
		db.setPaint(ptx1[0], pty1[0], ptx1[3] - ptx1[0], pty1[3] - pty1[0], c,
				true);
		db.g.fillPolygon(ptx1, pty1, 4);

		if (db.gp.coorWidth != 0) {
			db.g.setColor(db.gp.coorColor);
		} else {
			db.g.setColor(c);
		}

		if (seriesWidth > 3) {
			db.drawPolygon(ptx1, pty1, 4,
					db.egp.getAxisColor(GraphProperty.AXIS_COLBORDER));
		}
	}
}
