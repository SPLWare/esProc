package com.raqsoft.cellset.graph.draw;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import com.raqsoft.cellset.graph.*;
import com.raqsoft.chart.Consts;
import com.raqsoft.chart.Utils;

/**
 * 三维面积图的实现
 * @author Joancy
 *
 */
public class DrawArea3D extends DrawBase {
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
		//少改动代码，同名引出要用到的实例
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

		tmpInt = (gp.catNum + 1) * categorySpan + coorWidth + gp.catNum
				* gp.serNum * seriesWidth;
		gp.graphRect.x += (gp.graphRect.width - tmpInt) / 2;
		gp.graphRect.width = tmpInt;

		dely = (gp.graphRect.height - coorWidth) / gp.tickNum;
		tmpInt = dely * gp.tickNum + coorWidth;
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

		db.drawGraphRect();
		Point2D.Double p;
		/* 画Y轴 */
		for (int i = 0; i <= gp.tickNum; i++) {
			db.drawGridLine(dely, i);

			Number coory = (Number) gp.coorValue.get(i);
			String scoory = db.getFormattedValue(coory.doubleValue());
			p = db.getVTickPoint(i*dely);
			gp.GFV_YLABEL.outText(p.x-gp.tickLen, p.y, scoory);

			// 设置基线
			if (coory.doubleValue() == gp.baseValue + gp.minValue) {
				gp.valueBaseLine = gp.gRect1.y + gp.gRect1.height - i
						* dely;
			}
		}
		// 画警戒线
		db.drawWarnLine();
		if (gp.graphTransparent) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					0.60F));
		}

		/* 画X轴 */
		prePoints1 = new Point2D.Double[gp.serNum];
		prePoints2 = new Point2D.Double[gp.serNum];
		ArrayList cats = egp.categories;
		int cc = cats.size();
		Color c;
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			double posx = DrawLine.getPosX(gp,i,cc,categorySpan,seriesWidth);
			double delx = posx - gp.gRect1.x;
			
			boolean vis = i % (gp.graphXInterval + 1) == 0;
			p = db.getHTickPoint(delx);
			if (vis) {
				c = egp.getAxisColor(GraphProperty.AXIS_BOTTOM);
				Utils.setStroke(g, c, Consts.LINE_SOLID, 1.0f);
				db.drawLine(p.x, p.y,p.x, p.y + gp.tickLen,c);
				// 画背景虚线
				db.drawGridLineCategoryV(gp.gRect2.x + delx);
			}

			String value = egc.getNameString();
			gp.GFV_XLABEL.outText(p.x, p.y+gp.tickLen, value, vis);

			for (int j = 0; j < gp.serNum; j++) {
				ExtGraphSery egs = egc.getExtGraphSery(gp.serNames.get(j));
				if (egs.isNull()) {
					continue;
				}
				double val = egs.getValue();
				double tmp = val - gp.baseValue;
				double len = dely * gp.tickNum * (tmp - gp.minValue) / (gp.maxValue * gp.coorScale);

				Point2D.Double pt1, pt2;
				if (egs.isNull()) {
					pt1 = null;
					pt2 = null;
				} else {
					pt1 = new Point2D.Double(posx, gp.valueBaseLine - len);
					pt2 = new Point2D.Double(posx + coorWidth,
							gp.valueBaseLine - len - coorWidth);
				}

				if (gp.coorWidth == 0) {
					if (gp.serNum == 1) {
						g.setColor(db.getColor(i));
					} else {
						g.setColor(db.getColor(j));
					}
					db.fillRect(pt1.x - 2, pt1.y - 2, 4, 4);
					db.fillRect(pt2.x - 2, pt2.y - 2, 4, 4);
					if (seriesWidth > 3) {
						db.drawRect(pt1.x - 2, pt1.y - 2, 4, 4,
								egp.getAxisColor(GraphProperty.AXIS_COLBORDER));
						db.drawRect(pt2.x - 2, pt2.y - 2, 4, 4,
								egp.getAxisColor(GraphProperty.AXIS_COLBORDER));
					}
				}
				// 画带子
				if (i > 0 && prePoints1[j] != null && pt1 != null) {
					double ptx1[] = { prePoints1[j].x, prePoints2[j].x, pt2.x,
							pt1.x };
					double pty1[] = { prePoints1[j].y, prePoints2[j].y, pt2.y,
							pt1.y };
					g.setColor(Color.gray);
					Utils.fillPolygon(g,ptx1, pty1);
					g.setColor(db.getColor(j));
					Utils.fillPolygon(g,ptx1, pty1);
					if (gp.coorWidth != 0) {
						g.setColor(gp.coorColor);
					} else {
						g.setColor(db.getColor(j));
					}
					if (seriesWidth > 3) {
						Utils.drawPolygon(g,ptx1, pty1);
						// 画区域
					}
					double ptx2[] = { prePoints1[j].x, prePoints1[j].x, pt1.x,
							pt1.x };
					double pty2[] = { prePoints1[j].y, gp.valueBaseLine,
							gp.valueBaseLine, pt1.y };

					db.setPaint(prePoints1[j].x, prePoints1[j].y, pt1.x
							- prePoints1[j].x, pt1.y - gp.valueBaseLine, j,
							true);

					Utils.fillPolygon(g,ptx2, pty2);
					if (gp.coorWidth != 0) {
						g.setColor(gp.coorColor);
					} else {
						g.setColor(db.getColor(j));
					}
					if (seriesWidth > 3) {
						Utils.drawPolygon(g,ptx2, pty2);
					}
				}

				// 输出文字
				if (gp.dispValueOntop && !egs.isNull() && vis) {
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
				if (!egs.isNull() && gp.drawLineDot && vis) {
					if (gp.serNum == 1) {
						g.setColor(db.getColor(i));
					} else {
						g.setColor(db.getColor(j));
					}
					db.fillRect(pt1.x - VALUE_RADIUS, pt1.y - VALUE_RADIUS,
							VALUE_RADIUS * 2, VALUE_RADIUS * 2);
					db.fillRect(pt2.x - VALUE_RADIUS, pt2.y - VALUE_RADIUS,
							VALUE_RADIUS * 2, VALUE_RADIUS * 2);
					if (seriesWidth > 3) {
						db.drawRect(pt1.x - VALUE_RADIUS, pt1.y - VALUE_RADIUS,
								VALUE_RADIUS * 2, VALUE_RADIUS * 2,
								egp.getAxisColor(GraphProperty.AXIS_COLBORDER));
						db.drawRect(pt2.x - VALUE_RADIUS, pt2.y - VALUE_RADIUS,
								VALUE_RADIUS * 2, VALUE_RADIUS * 2,
								egp.getAxisColor(GraphProperty.AXIS_COLBORDER));
						db.htmlLink(pt2.x - VALUE_RADIUS, pt2.y - VALUE_RADIUS,
								VALUE_RADIUS * 2, VALUE_RADIUS * 2, htmlLink,
								egc.getNameString(), egs);
					}
				}

				prePoints1[j] = pt1;
				prePoints2[j] = pt2;
				// 画最后一条垂直的带子
				if (len >= 0 && i == gp.catNum - 1) {
					double ptx1[] = { prePoints1[j].x, prePoints1[j].x,
							prePoints2[j].x, prePoints2[j].x };
					double pty1[] = { prePoints1[j].y, gp.valueBaseLine,
							gp.valueBaseLine - coorWidth,
							prePoints2[j].y };
					g.setColor(Color.gray);
					Utils.fillPolygon(g,ptx1, pty1);
					g.setColor(db.getColor(j).darker());
					Utils.fillPolygon(g,ptx1, pty1);
					if (gp.coorWidth != 0) {
						g.setColor(gp.coorColor);
					} else {
						g.setColor(db.getColor(j));
					}
					if (seriesWidth > 3) {
						Utils.drawPolygon(g,ptx1, pty1);
					}
				}
			}
		}

		// 最后输出值标签，文字置顶
		db.outLabels();

		/* 重画一下基线 */
		db.drawLine(gp.gRect1.x, gp.valueBaseLine, gp.gRect1.x + gp.gRect1.width,
				gp.valueBaseLine, egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
		db.drawLine(gp.gRect1.x + gp.gRect1.width, gp.valueBaseLine,
				gp.gRect1.x + gp.gRect1.width + coorWidth,
				gp.valueBaseLine - coorWidth,
				egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
	}
}
