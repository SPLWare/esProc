package com.raqsoft.cellset.graph.draw;

import java.awt.*;
import java.util.*;
import java.awt.geom.*;

import com.raqsoft.cellset.graph.*;
import com.raqsoft.chart.Consts;
import com.raqsoft.chart.Utils;

/**
 * 雷达图实现
 * @author Joancy
 *
 */
public class DrawRadar extends DrawBase {
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
		int VALUE_RADIUS = db.VALUE_RADIUS;
		ArrayList<ValuePoint> pointList = db.pointList;

		double r, thick = 2.5f;
		double tmpInt, tmpInt1, tmpInt2;
		double cx, cy, x, y, x2, y2;

		db.initGraphInset();
		db.createCoorValue();
		db.drawLegend(htmlLink);
		db.drawTitle();
		db.drawLabel();
		db.keepGraphSpace();

		tmpInt1 = gp.graphWidth - gp.leftInset - gp.rightInset;
		tmpInt2 = gp.graphHeight - gp.topInset - gp.bottomInset;
		if (tmpInt1 < tmpInt2) {
			tmpInt = tmpInt1;
			gp.topInset += (tmpInt2 - tmpInt1) / 2;
		} else {
			tmpInt = tmpInt2;
			gp.leftInset += (tmpInt1 - tmpInt2) / 2;
		}

		gp.graphRect = new Rectangle2D.Double(gp.leftInset, gp.topInset, tmpInt, tmpInt);

		r = tmpInt / 2.0f;
		cx = gp.leftInset + r;
		cy = gp.topInset + r;

		if (gp.graphRect.width < 10 || gp.graphRect.height < 10) {
			return;
		}
		gp.gRect1 = (Rectangle2D.Double)gp.graphRect.clone();
		gp.gRect2 = (Rectangle2D.Double)gp.graphRect.clone();


		Color backColor = gp.graphBackColor;
		Color foreColor = egp.getAxisColor(GraphProperty.AXIS_TOP);
		int style = Consts.LINE_NONE;//draw2DPie画的是饼图，画边框时会包含圆心到半径的边，这里不绘制边框
		float weight = (float)thick;
		Rectangle2D ellipseBounds = new Rectangle2D.Double(cx - r,cy - r, 2*r, 2*r);
		double startAngle =0,extentAngle = 360;
		Utils.draw2DPie(g, ellipseBounds, startAngle, extentAngle, 
				foreColor, style, weight, db.getTransparent(), db.getChartColor(backColor), 1);
		Arc2D.Double a2dd = null;
		a2dd = new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, 0, 360,Arc2D.CHORD);
		g.setStroke(new BasicStroke(weight));
		db.drawShape(a2dd, foreColor);

		/* 坐标轴 */
		tmpInt = gp.catNum;
		double deltaAngle = 360 * 1.0f / tmpInt;
		ArrayList cats = egp.categories;
		int cc = cats.size();
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			String cat = egc.getNameString();
			double angle = deltaAngle * i + 90;
			double rAngle = Math.toRadians(angle);

			x2 = cx +  ((r + thick) * Math.cos(rAngle));
			y2 = cy -  ((r + thick) * Math.sin(rAngle));
			g.setStroke(new BasicStroke(1.0f));
			db.drawLine( cx,  cy,  x2,  y2,
					egp.getAxisColor(GraphProperty.AXIS_PIEJOIN));
			db.drawOutCircleText(gp.GFV_XLABEL, cat, angle,  x2,  y2);
		}

		/* 刻度,网格线 */
		double deltaR, dr;
		deltaR = r / gp.tickNum;

		BasicStroke gridStroke = db.getLineStroke(gp.gridLineStyle, 0.1f);

		for (int i = 0; i < gp.tickNum; i++) {
			dr = i * deltaR;
			x = cx -  dr;
			y = cy -  dr;
			g.setColor(gp.gridColor);

			if (gridStroke != null) {
				g.setStroke(gridStroke);
				g.drawOval( (int)x,  (int)y,  (int)dr * 2,  (int)dr * 2);
			}

			Number coory = (Number) gp.coorValue.get(i);
			if (i == 0) {
				gp.baseValue = coory.doubleValue();
			}
			String scoory = db.getFormattedValue(coory.doubleValue());
			x = cx;
			y = cy -  dr;
			gp.GFV_YLABEL.outText( x,  y, scoory);
		}

		//值输出
		Point2D.Double prePoints[] = new Point2D.Double[gp.serNum];
		Point2D.Double lastPoint[] = new Point2D.Double[gp.serNum];
		Point2D.Double startPoints[] = new Point2D.Double[gp.serNum];
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);

			for (int j = 0; j < gp.serNum; j++) {
				ExtGraphSery egs = egc.getExtGraphSery(gp.serNames.get(j));
				double val = egs.getValue();
				double tmp = val - gp.baseValue;
				double len =  (deltaR * gp.tickNum * (tmp - gp.minValue) / (gp.maxValue * gp.coorScale));

				double angle = deltaAngle * i + 90;
				double rAngle = Math.toRadians(angle);
				r = len;

				x = cx +  (r * Math.cos(rAngle));
				y = cy -  (r * Math.sin(rAngle));

				Point2D.Double pt;
				if (egs.isNull()) {
					pt = null;
				} else {
					pt = new Point2D.Double( x,  y);
				}

				if (gp.drawLineDot && pt != null) {
					backColor = db.getColor(j);
					double xx, yy, ww, hh;
					xx = pt.x - VALUE_RADIUS;
					yy = pt.y - VALUE_RADIUS;
					ww = 2 * VALUE_RADIUS;
					hh = ww;
					ValuePoint vp = new ValuePoint(pt, backColor);
					pointList.add( vp );
					db.htmlLink(xx, yy, ww, hh, htmlLink, egc.getNameString(), egs);
				}

				if (i == 0) {
					startPoints[j] = pt;
				} else if (i == gp.catNum - 1) {
					g.setColor(db.getColor(j));
					g.setStroke(new BasicStroke(1f));
					db.drawLine(prePoints[j], pt);
					db.drawLine(startPoints[j], pt);
				} else {
					g.setColor(db.getColor(j));
					g.setStroke(new BasicStroke(1f));
					if (egp.isIgnoreNull()) {
						db.drawLine(lastPoint[j], pt);
					} else {
						db.drawLine(prePoints[j], pt);
					}
				}
				prePoints[j] = pt;
				if (pt != null) {
					lastPoint[j] = pt;
				}
			}
		}
		db.outPoints();
		db.outLabels();
	}

}
