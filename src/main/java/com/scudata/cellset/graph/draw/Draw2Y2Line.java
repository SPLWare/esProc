package com.scudata.cellset.graph.draw;

import java.util.ArrayList;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.scudata.cellset.graph.*;
import com.scudata.chart.Consts;
import com.scudata.chart.Utils;
/**
 * 双轴折线图的实现
 * @author Joancy
 *
 */
public class Draw2Y2Line extends DrawBase {
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
		int VALUE_RADIUS = db.getPointRadius();
		ArrayList<ValuePoint> pointList = db.pointList;
		
		double seriesWidth;
		double coorWidth;
		double categorySpan;
		double dely;
		double tmpInt;
		double x, y;
		Point2D.Double beginPoint[];
		Point2D.Double lastPoint[];
		gp.coorWidth = 0;

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
				* 1.0
				/ (((gp.catNum + 1) * gp.categorySpan / 100.0) + gp.coorWidth
						/ 200.0 + gp.catNum * gp.serNum);

		coorWidth = seriesWidth * 1.0 * (gp.coorWidth / 200.0);
		categorySpan = seriesWidth * 1.0 * (gp.categorySpan / 100.0);

		tmpInt = (gp.catNum + 1) * categorySpan + coorWidth + gp.catNum
				* gp.serNum * seriesWidth;
		gp.graphRect.x += (gp.graphRect.width - tmpInt) / 2;
		gp.graphRect.width = tmpInt;

		dely = (gp.graphRect.height - coorWidth) / gp.tickNum * 1.0;
		tmpInt =  (dely * gp.tickNum + coorWidth);
		gp.graphRect.y += (gp.graphRect.height - tmpInt) / 2;
		gp.graphRect.height = tmpInt;

		gp.gRect1 = (Rectangle2D.Double)gp.graphRect.clone();
		gp.gRect2 = (Rectangle2D.Double)gp.graphRect.clone();

		/* 画坐标轴 */
		db.drawGraphRect();
		g.setStroke(new BasicStroke(0.00001f));

		for (int i = 0; i <= gp.tickNum; i++) {
			db.drawGridLine(dely, i);
			Number coory = (Number) gp.coorValue.get(i);
			String scoory = db.getFormattedValue(coory.doubleValue());

			x = gp.gRect1.x - gp.tickLen;// - TR.width
			y = gp.gRect1.y + gp.gRect1.height - i * dely;
														
			gp.GFV_YLABEL.outText(x, y, scoory);
			// 设置基线
			if (coory.doubleValue() == gp.baseValue + gp.minValue) {
				gp.valueBaseLine = gp.gRect1.y + gp.gRect1.height - i
						* dely;
			}
		}

		// 画警戒线,双轴图只警戒左边的轴
		db.drawWarnLine();

		/* 画X轴 */
		beginPoint = new Point2D.Double[gp.serNum];
		lastPoint = new Point2D.Double[gp.serNum];
		ArrayList cats = egp.categories;
		int cc = cats.size();
		Color c;
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			double posx = DrawLine.getPosX(gp,i,cc,categorySpan,seriesWidth);
			boolean valvis = (i % (gp.graphXInterval + 1) == 0);//柱顶是否显示值跟画Table分开
			boolean vis = valvis && !gp.isDrawTable;
			if (vis) {
				c = egp.getAxisColor(GraphProperty.AXIS_BOTTOM);
				Utils.setStroke(g, c, Consts.LINE_SOLID, 1.0f);
				db.drawLine(posx, gp.gRect1.y + gp.gRect1.height,
						    posx, gp.gRect1.y + gp.gRect1.height
								+ gp.tickLen,c);
				// 画背景虚线
				db.drawGridLineCategoryV(posx);
			}

			String value = egc.getNameString();
			// TR.setBounds(gp.GFV_XLABEL.getTextSize(value));
			x = posx;
			y = gp.gRect1.y + gp.gRect1.height + gp.tickLen;// + TR.height;
			gp.GFV_XLABEL.outText(x, y, value, vis);

			for (int j = 0; j < gp.serNum; j++) {
				ExtGraphSery egs = egc.getExtGraphSery(gp.serNames.get(j));
				double val = egs.getValue();
				double tmp = val - gp.baseValue;
				double len = dely * gp.tickNum * (tmp - gp.minValue) / (gp.maxValue * gp.coorScale);
				if( gp.isDrawTable ){
					posx = db.getDataTableX( i );
				}

				Point2D.Double endPoint;
				if (egs.isNull()) {
					endPoint = null;
				} else {
					endPoint = new Point2D.Double(posx, gp.valueBaseLine - len);
				}

				// 显示值标示
				if (gp.dispValueOntop && !egs.isNull() && valvis) {
					String sval = db.getDispValue(egc,egs,gp.serNum);
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
				if (!egs.isNull() && gp.drawLineDot && vis2) { // 线条上的小方块
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

					ValuePoint vp = new ValuePoint(endPoint, backColor,null, Consts.PT_SQUARE,-1);
					pointList.add( vp );
					db.htmlLink(xx, yy, ww, hh, htmlLink, egc.getNameString(), egs);
				}
				if (i > 0) {
					g.setColor(db.getColor(j));
					if (egp.isIgnoreNull()) {
						db.drawLine(lastPoint[j], endPoint);
					} else {
						db.drawLine(beginPoint[j], endPoint);
					}
				}
				beginPoint[j] = endPoint;
				if (endPoint != null) {
					lastPoint[j] = endPoint;
				}
			}
		}

		/* 画基线 */
		db.drawLine(gp.gRect1.x, gp.valueBaseLine, gp.gRect1.x + gp.gRect1.width,
				gp.valueBaseLine, egp.getAxisColor(GraphProperty.AXIS_BOTTOM));

		drawY2Line(db, gp.serNum, htmlLink);
		db.outPoints();
		db.outLabels();
	}
	
	/**
	 * 根据绘图基类db，画双轴柱线图的折线部分，并将画图后的超链接存入htmlLink
	 * @param db 抽象的绘图基类
	 * @param seriesCount 双轴图的系列拆为左轴和右轴，此为左轴的系列个数
	 * @param htmlLink 超链接缓存
	 */
	public static void drawY2Line(DrawBase db, int series1Count,
			StringBuffer htmlLink) {
		int VALUE_RADIUS = db.getPointRadius();
		double seriesWidth;
		double coorWidth;
		double categorySpan;
		double dely;
		double tmpInt;
		double x, y;

		Point2D.Double beginPoint[];
		Point2D.Double lastPoint[];
		db.gp.graphRect = new Rectangle2D.Double(db.gp.leftInset, db.gp.topInset,
				db.gp.graphWidth - db.gp.leftInset - db.gp.rightInset,
				db.gp.graphHeight - db.gp.topInset - db.gp.bottomInset);

		if (db.gp.graphRect.width < 10 || db.gp.graphRect.height < 10) {
			return;
		}

		if (db.gp.coorWidth < 0 || db.gp.coorWidth > 10000) {
			db.gp.coorWidth = 0;
		}
		seriesWidth = db.gp.graphRect.width
				/ (((db.gp.catNum + 1) * db.gp.categorySpan / 100.0)
						+ db.gp.coorWidth / 200.0 + db.gp.catNum * db.gp.serNum);

		coorWidth = seriesWidth * (db.gp.coorWidth / 200.0);
		categorySpan = seriesWidth * (db.gp.categorySpan / 100.0);

		tmpInt = (db.gp.catNum + 1) * categorySpan + coorWidth + db.gp.catNum
				* db.gp.serNum * seriesWidth;
		db.gp.graphRect.x += (db.gp.graphRect.width - tmpInt) / 2;
		db.gp.graphRect.width = tmpInt;

		dely = (db.gp.graphRect.height - coorWidth) / db.gp.tickNum2;
		tmpInt = dely * db.gp.tickNum2 + coorWidth;
		db.gp.graphRect.y += (db.gp.graphRect.height - tmpInt) / 2;
		db.gp.graphRect.height = tmpInt;

		db.gp.gRect1 = (Rectangle2D.Double)db.gp.graphRect.clone();
		db.gp.gRect2 = (Rectangle2D.Double)db.gp.graphRect.clone();

		double tmpi; // 临时存储值，用于减少重复计算的值
		for (int i = 0; i <= db.gp.tickNum2; i++) {
			tmpi =  (db.gp.gRect1.y + db.gp.gRect1.height - i * dely);
			db.g.setStroke(new BasicStroke(1f));
			db.drawLine(db.gp.gRect1.x + db.gp.gRect1.width - db.gp.tickLen,
					tmpi, db.gp.gRect1.x + db.gp.gRect1.width, tmpi,
					db.egp.getAxisColor(GraphProperty.AXIS_RIGHT)); // Y2坐标轴的小短线

			Number coory = (Number) db.gp.coorValue2.get(i);
			String scoory = db.getFormattedValue(coory.doubleValue(),
					db.gp.dataMarkFormat2);
			x = db.gp.gRect1.x + db.gp.gRect1.width + db.gp.tickLen;
			y =  (db.gp.gRect1.y + db.gp.gRect1.height - i * dely);// db.TR.height
																		// / 2
			db.gp.GFV_YLABEL.outText(x, y, scoory, GraphFontView.TEXT_ON_RIGHT);
			// 设置基线
			if (coory.doubleValue() == db.gp.baseValue2 + db.gp.minValue2) {
				db.gp.valueBaseLine =  (db.gp.gRect1.y
						+ db.gp.gRect1.height - i * dely);
			}
		}

		/* 画数值线条 */
		db.g.setStroke(new BasicStroke(0.00001f));

		beginPoint = new Point2D.Double[db.gp.serNames2.size()];
		lastPoint = new Point2D.Double[db.gp.serNames2.size()];
		ArrayList cats2 = db.egp.category2;
		int cc = cats2.size();
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats2.get(i);
			for (int j = 0; j < db.gp.serNum2; j++) {
				ExtGraphSery egs = egc.getExtGraphSery(db.gp.serNames2.get(j));
				double val = egs.getValue();
				double tmp = val - db.gp.baseValue2;
				double len = dely * db.gp.tickNum2
						* (tmp - db.gp.minValue2) / (db.gp.maxValue2 * db.gp.coorScale2);
				double posx = DrawLine.getPosX(db.gp,i,cc,categorySpan,seriesWidth);
				if( db.gp.isDrawTable ){
					posx = db.getDataTableX( i );
				}

				Point2D.Double endPoint;
				if (egs.isNull()) {
					endPoint = null;
				} else {
					endPoint = new Point2D.Double(posx, db.gp.valueBaseLine - len);
				}

				boolean valvis = i % (db.gp.graphXInterval + 1) == 0;
				// 显示值标示
				if (db.gp.dispValueOntop2 && !egs.isNull() && valvis) {
					String sval = db.getDispValue2(egc,egs,db.gp.serNum);
					x = endPoint.x;
					y = endPoint.y;
					Color c = db.getColor(series1Count + j);
					ValueLabel vl = new ValueLabel(sval, new Point2D.Double(x, y
							- VALUE_RADIUS), c);
					db.labelList.add(vl);
				}

				boolean vis2 = (i % (db.gp.graphXInterval + 1) == 0);
				if (!egs.isNull() && db.gp.drawLineDot && vis2) { // 线条上的小圈圈
					Color backColor;
					double xx, yy, ww, hh;
					xx = endPoint.x - VALUE_RADIUS;
					yy = endPoint.y - VALUE_RADIUS;
					ww = 2 * VALUE_RADIUS;
					hh = ww;
					if (!db.isMultiSeries()) {
						backColor = db.getColor(series1Count +i);
					} else {
						backColor = db.getColor(series1Count +j);
					}

					ValuePoint vp = new ValuePoint(endPoint, backColor);
					db.pointList.add( vp );
					db.htmlLink2(xx, yy, ww, hh, htmlLink, egc.getNameString(), egs);
				}
				if (i > 0) {
					db.g.setColor(db.getColor(series1Count + j));
					if (db.egp.isIgnoreNull()) {
						db.drawLine(lastPoint[j], endPoint);
					} else {
						db.drawLine(beginPoint[j], endPoint);
					}
				}
				beginPoint[j] = endPoint;
				if (endPoint != null) {
					lastPoint[j] = endPoint;
				}
			}
		}

		db.drawLine(db.gp.gRect1.x, db.gp.valueBaseLine, db.gp.gRect1.x
				+ db.gp.gRect1.width, db.gp.valueBaseLine,
				db.egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
	}

}
