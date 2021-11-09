package com.raqsoft.cellset.graph.draw;

import java.awt.*;
import java.util.ArrayList;
import com.raqsoft.cellset.graph.*;
import com.raqsoft.chart.Consts;
import com.raqsoft.chart.Utils;
/**
 * 面积图的实现
 * @author Joancy
 *
 */

public class DrawArea extends DrawBase {
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
		int tmpInt;
		int x, y;
		gp.coorWidth = 0;
		Point headPoint[];

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

		/* 画坐标轴 */
		db.drawGraphRect();

		/* 画Y轴 */
		for (int i = 0; i <= gp.tickNum; i++) {
			db.drawGridLine(dely, i);
			Number coory = (Number) gp.coorValue.get(i);
			String scoory = db.getFormattedValue(coory.doubleValue());
			x = gp.gRect1.x - gp.tickLen; // - TR.width
			y = (int) (gp.gRect1.y + gp.gRect1.height - i * dely); // +
																	// TR.height
																	// / 2
			gp.GFV_YLABEL.outText(x, y, scoory);
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
		headPoint = new Point[gp.serNum];
		ArrayList cats = egp.getCategories();
		int cc = cats.size();
		Color c;
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			int posx = DrawLine.getPosX(gp,i,cc,categorySpan,seriesWidth);
			
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
			x = posx;
			y = gp.gRect1.y + gp.gRect1.height + gp.tickLen; // + TR.height;
			gp.GFV_XLABEL.outText(x, y, value, vis);

			for (int j = 0; j < gp.serNum; j++) {
				ExtGraphSery egs = egc.getExtGraphSery(gp.serNames.get(j));
				if (egs.isNull()) {
					continue;
				}
				double val = egs.getValue();
				double tmp = val - gp.baseValue;
				int len = (int) (dely * gp.tickNum * (tmp - gp.minValue) / (gp.maxValue * gp.coorScale));

				if( gp.isDrawTable ){
					posx = (int)db.getDataTableX( i );
				}

				Point endPoint;
				if (egs.isNull()) {
					endPoint = null;
				} else {
					endPoint = new Point(posx, gp.valueBaseLine - len);
				}

				// 画带子
				if (headPoint[j] != null && endPoint != null) {
					int ptx1[] = { headPoint[j].x, headPoint[j].x, endPoint.x,
							endPoint.x };
					int pty1[] = { headPoint[j].y, headPoint[j].y, endPoint.y,
							endPoint.y };
					g.setColor(db.getColor(j));
					fillPolygon(db,ptx1, pty1, 4);
					db.drawPolygon(ptx1, pty1, 4,
							egp.getAxisColor(GraphProperty.AXIS_COLBORDER));
					// 画区域
					int ptx2[] = { headPoint[j].x, headPoint[j].x, endPoint.x,
							endPoint.x };
					int pty2[] = { headPoint[j].y, gp.valueBaseLine,
							gp.valueBaseLine, endPoint.y };
					db.setPaint(headPoint[j].x, headPoint[j].y, endPoint.x
							- headPoint[j].x, endPoint.y - gp.valueBaseLine, j,
							true);
					fillPolygon(db,ptx2, pty2, 4);
					if (seriesWidth > 3) {
						db.drawPolygon(ptx2, pty2, 4,
								egp.getAxisColor(GraphProperty.AXIS_COLBORDER));
					}
				}

				// 输出文字
				if (gp.dispValueOntop && !egs.isNull() && valvis) {
					String sval = db.getDispValue(egc,egs,gp.serNum); // getFormattedValue(val);
					x = endPoint.x;
					y = endPoint.y;
					if (!gp.isMultiSeries) {
						c = db.getColor(i);
					} else {
						c = db.getColor(j);
					}
					ValueLabel vl = new ValueLabel(sval, new Point(x, y
							- VALUE_RADIUS), c);
					labelList.add(vl);
				}
				boolean vis2 = (i % (gp.graphXInterval + 1) == 0);
				if (!egs.isNull() && gp.drawLineDot && vis2) {
					int xx, yy, ww, hh;
					xx = endPoint.x - VALUE_RADIUS;
					yy = endPoint.y - VALUE_RADIUS;
					ww = 2 * VALUE_RADIUS;
					hh = ww;
					if (!gp.isMultiSeries) {
						db.setPaint(xx, yy, ww, hh, db.getColor(i), true);
					} else {
						db.setPaint(xx, yy, ww, hh, db.getColor(j), true);
					}

					db.fillRect(xx, yy, ww, hh);
					db.drawRect(xx, yy, ww, hh,
							egp.getAxisColor(GraphProperty.AXIS_COLBORDER));
					db.htmlLink(xx, yy, ww, hh, htmlLink, egc.getNameString(), egs);
				} // 线条上的小方块

				headPoint[j] = endPoint;
			}
		}

		// 最后输出值标签，文字置顶
		db.outLabels();

		/* 重画一下基线 */
		db.drawLine(gp.gRect1.x, gp.valueBaseLine, gp.gRect1.x + gp.gRect1.width,
				gp.valueBaseLine, egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
		db.drawLine(gp.gRect1.x + gp.gRect1.width, gp.valueBaseLine,
				(int) (gp.gRect1.x + gp.gRect1.width + coorWidth),
				(int) (gp.valueBaseLine - coorWidth),
				egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
	}

	private static void fillPolygon(DrawBase db,int[] x, int[] y, int n) {
		//少改动代码，同名引出要用到的实例
		GraphParam gp = db.gp;
		ExtGraphProperty egp = db.egp;
		Graphics2D g = db.g;
		ArrayList<ValueLabel> labelList = db.labelList;
		int VALUE_RADIUS = db.VALUE_RADIUS;
		ArrayList<ValuePoint> pointList = db.pointList;

		if (egp.isDrawShade()) {
			Color c = g.getColor();
			Paint p = g.getPaint();
			int[] xx = new int[n];
			int[] yy = new int[n];
			for (int i = 0; i < n; i++) {
				xx[i] = x[i] + db.SHADE_SPAN;
				yy[i] = y[i] + db.SHADE_SPAN;
			}
			g.setColor(Color.lightGray);
			g.fillPolygon(xx, yy, n);
			g.setColor(c);
			g.setPaint(p);
		}
		g.fillPolygon(x, y, n);
	}

}
