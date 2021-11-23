package com.raqsoft.cellset.graph.draw;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

import com.raqsoft.cellset.graph.*;
import com.raqsoft.chart.Consts;
import com.raqsoft.chart.Utils;
/**
 * 折线图实现
 * @author Joancy
 *
 */

public class DrawLine extends DrawBase {
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
	public static void drawing(DrawBase db, StringBuffer htmlLink) {
		GraphParam gp = db.gp;
		ExtGraphProperty egp = db.egp;
		Graphics2D g = db.g;
		ArrayList<ValueLabel> labelList = db.labelList;
		int VALUE_RADIUS = db.getPointRadius();
		ArrayList<ValuePoint> pointList = db.pointList;
		double seriesWidth;
		double coorWidth;
		double categorySpan;
		double dely;
		double tmpInt;
		double x, y;

		gp.coorWidth = 0;

		Point2D.Double beginPoint[];
		Point2D.Double lastPoint[];
		double beginVal[];

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

			x = gp.gRect1.x - gp.tickLen;
			y =  (gp.gRect1.y + gp.gRect1.height - i * dely); 
			gp.GFV_YLABEL.outText(x, y, scoory);
			// 设置基线
			if (coory.doubleValue() == gp.baseValue + gp.minValue) {
				gp.valueBaseLine =  (gp.gRect1.y + gp.gRect1.height - i
						* dely);
			}
		}
		// 画警戒线
		db.drawWarnLine();

		/* 画X轴 */
		beginPoint = new Point2D.Double[gp.serNum];
		lastPoint = new Point2D.Double[gp.serNum];
		beginVal = new double[gp.serNum];
		ArrayList cats = egp.categories;
		int cc = cats.size();
		Color c;
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			double posx = getPosX(gp,i,cc,categorySpan,seriesWidth);
			
			boolean valvis = (i % (gp.graphXInterval + 1) == 0);//柱顶是否显示值跟画Table分开
			boolean vis = valvis && !gp.isDrawTable;
			if (vis) {
				c = egp.getAxisColor(GraphProperty.AXIS_BOTTOM);
				Utils.setStroke(g, c, Consts.LINE_SOLID, 1.0f);
				db.drawLine(posx, gp.gRect1.y + gp.gRect1.height,
						    posx, gp.gRect1.y + gp.gRect1.height
								+ gp.tickLen, c);
				// 画背景虚线
				db.drawGridLineCategoryV(posx);
			}

			String value = egc.getNameString();
			x = posx;
			y = gp.gRect1.y + gp.gRect1.height + gp.tickLen; 
			gp.GFV_XLABEL.outText(x, y, value, vis);

			for (int j = 0; j < gp.serNum; j++) {
				ExtGraphSery egs = egc.getExtGraphSery(gp.serNames.get(j));
				double val = egs.getValue();
				double tmp = val - gp.baseValue;
				double len =  (dely * gp.tickNum * (tmp - gp.minValue) / (gp.maxValue * gp.coorScale));

				if (gp.isDrawTable) {
					posx =  db.getDataTableX(i);
				}

				Point2D.Double endPoint;
				if (egs.isNull()) {
					endPoint = null;
				} else {
					endPoint = new Point2D.Double(posx, gp.valueBaseLine - len);
				}

				// 输出文字
				if (gp.dispValueOntop && !egs.isNull() && valvis) {
					String sval = db.getDispValue(egc, egs, gp.serNum);
					x = endPoint.x;
					y = endPoint.y;
					if (!db.isMultiSeries()) {
						c = db.getColor(i);
					} else {
						c = db.getColor(j);
					}
					ValueLabel vl = new ValueLabel(sval, new Point2D.Double(x, y
							- VALUE_RADIUS), c);
					labelList.add(vl);
				}

				boolean vis2 = (i % (gp.graphXInterval + 1) == 0);
				if (!egs.isNull() && gp.drawLineDot && vis2) {
					Color backColor;
					double xx, yy, ww, hh;
					xx = endPoint.x - VALUE_RADIUS;
					yy = endPoint.y - VALUE_RADIUS;
					ww = 2 * VALUE_RADIUS;
					hh = ww;
					if (!db.isMultiSeries()) {
						backColor = db.getColor(i);
					} else {
						backColor = db.getColor(j);
					}

					ValuePoint vp = new ValuePoint(endPoint, backColor);
					pointList.add(vp);
					db.htmlLink(xx, yy, ww, hh, htmlLink, egc.getNameString(),
							egs);
				} // 线条上的小方块

				if (i > 0) {
					g.setColor(db.getColor(j));
					if (egp.isIgnoreNull()) {
						db.drawLine(lastPoint[j], endPoint);
					} else {
						db.drawLine(beginPoint[j], endPoint);
					}
					drawVTrendLine(db, beginPoint[j], endPoint, val
							- beginVal[j]);
				}
				drawHTrendLine(db, beginPoint[j]);
				beginPoint[j] = endPoint;
				if (endPoint != null) {
					lastPoint[j] = endPoint;
				}
				beginVal[j] = val;
			}
		}

		// 最后输出值标签，文字置顶
		db.outPoints();
		db.outLabels();

		/* 重画一下基线 */
		db.drawLine(gp.gRect1.x, gp.valueBaseLine, gp.gRect1.x
				+ gp.gRect1.width, gp.valueBaseLine,
				egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
		db.drawLine(gp.gRect1.x + gp.gRect1.width, gp.valueBaseLine,
				gp.gRect1.x + gp.gRect1.width + coorWidth,
				gp.valueBaseLine - coorWidth,
				egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
	}
	
	/**
	 * 原点重合时，不用按照分类累加，直接用平均空间
	 * @param gp 绘图属性
	 * @param i 序号
	 * @param cc 
	 * @param categorySpan 分类间距
	 * @param seriesWidth 系列间距
	 * @return
	 */
	public static double getPosX(GraphParam gp,int i,int cc,double categorySpan,double seriesWidth){
		double lb = (i + 1) * categorySpan + (2 * i + 1)
				* gp.serNum * seriesWidth / 2;
		if( gp.isOverlapOrigin ){
			if(cc==1){
				lb = gp.gRect1.width;
			}else{
				lb = i*gp.gRect1.width*1.0f/(cc-1);
			}
		}
		return gp.gRect1.x +lb;
	}

	/**
	 * 画横向趋势线
	 * @param db 绘图实例
	 * @param p 坐标点
	 */
	public static void drawHTrendLine(DrawBase db, Point2D.Double p) {
		if (p == null || !db.gp.drawLineTrend) {
			return;
		}
		BasicStroke bs = db.getLineStroke(GraphProperty.LINE_SHORT_DASH, 0.1f);
		db.g.setColor(Color.darkGray);
		db.g.setStroke(bs);
		Utils.drawLine(db.g, db.gp.gRect2.x, p.y, db.gp.gRect2.x + db.gp.gRect2.width
				- 1, p.y);
	}

	/**
	 * 画纵向趋势线
	 * @param db 绘图实例
	 * @param begin 起点
	 * @param end 终点
	 * @param cha 差
	 */
	public static void drawVTrendLine(DrawBase db, Point2D.Double begin, Point2D.Double end,
			double cha) {
		if (begin == null || end == null || !db.gp.drawLineTrend) {
			return;
		}
		Stroke stroke = db.getLineStroke(GraphProperty.LINE_LONG_DASH,
				db.gp.getLineThick());
		if (stroke == null) {
			return;
		}
		Stroke old = db.g.getStroke();
		if (end.y < begin.y) {
			db.g.setColor(Color.green.darker());
		} else {
			db.g.setColor(Color.red.darker());
		}
		db.g.setStroke(stroke);
		Utils.drawLine(db.g,end.x, begin.y, end.x, end.y);
		double h = end.y - begin.y;
		double textX = end.x, textY = end.y;

		if (Math.abs(h) > 3) {
			db.g.setStroke(db.getLineStroke(GraphProperty.LINE_SOLID, 0.1f));
			double[] xx = new double[3];
			double[] yy = new double[3];
			if (h > 0) {
				textY = begin.y + h / 2;
				xx[0] = end.x - 3;
				yy[0] = end.y - 12;
				xx[1] = end.x;
				yy[1] = end.y;
				xx[2] = end.x + 3;
				yy[2] = end.y - 12;
			} else {
				textY = end.y - h / 2;
				xx[0] = end.x - 3;
				yy[0] = end.y + 12;
				xx[1] = end.x;
				yy[1] = end.y;
				xx[2] = end.x + 3;
				yy[2] = end.y + 12;
			}
			Utils.fillPolygon(db.g,xx, yy);
		}

		if (cha != 0) {
			String sval = db.getFormattedValue(Math.abs(cha));
			db.gp.GFV_VALUE.outText(textX, textY, sval); 
		}

		db.g.setStroke(old);
	}

}
