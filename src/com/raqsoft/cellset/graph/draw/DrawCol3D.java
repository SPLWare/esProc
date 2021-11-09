package com.raqsoft.cellset.graph.draw;

import java.awt.*;
import java.util.*;

import com.raqsoft.cellset.graph.*;
import com.raqsoft.chart.Consts;
import com.raqsoft.chart.Utils;
/**
 * 三维柱图实现
 * @author Joancy
 *
 */

public class DrawCol3D extends DrawBase {
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
		double seriesDeep;
		double coorWidth;
		double categorySpan;
		double seriesSpan;
		double dely = 0;
		int tmpInt;
		int x, y;

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

		gp.gRect1 = new Rectangle(gp.graphRect);
		gp.gRect2 = new Rectangle(gp.graphRect);

		coorWidth = (Math.min(gp.graphRect.width, gp.graphRect.height)) / 2;
		gp.gRect1.y += coorWidth;
		gp.gRect1.width -= coorWidth;
		gp.gRect1.height -= coorWidth;

		dely = gp.gRect1.height / gp.tickNum;
		gp.gRect1.y += (gp.gRect1.height - dely * gp.tickNum) / 2;
		gp.gRect1.height = (int) (dely * gp.tickNum);

		seriesDeep = (coorWidth / (((gp.serNum + 1) * gp.seriesSpan / 100.0) + gp.serNum));
		seriesSpan = (seriesDeep * (gp.seriesSpan / 100.0));

		tmpInt = (int) ((gp.serNum + 1) * seriesSpan + gp.serNum * seriesDeep);
		tmpInt = (int) ((coorWidth - tmpInt) / 2);
		gp.gRect1.y += tmpInt;

		if (gp.barDistance > 0) {
			double maxCatSpan = (gp.gRect1.width - gp.catNum * 1.0f)
					/ (gp.catNum + 1.0f);
			if (gp.barDistance <= maxCatSpan) {
				categorySpan = gp.barDistance;
			} else {
				categorySpan = maxCatSpan;
			}
			seriesWidth = (gp.gRect1.width - (gp.catNum + 1) * categorySpan)
					/ gp.catNum;
		} else {
			seriesWidth = (gp.gRect1.width / (((gp.catNum + 1)
					* gp.categorySpan / 100.0)
					+ gp.coorWidth / 200.0 + gp.catNum));

			categorySpan = (seriesWidth * (gp.categorySpan / 100.0));

		}
		gp.gRect2.x = (int) (gp.gRect1.x + coorWidth);
		gp.gRect2.width = gp.gRect1.width;
		gp.gRect2.y = (int) (gp.gRect1.y - coorWidth);
		gp.gRect2.height = gp.gRect1.height;

		/* 画坐标轴 */
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
		ArrayList cats = egp.categories;
		int cc = cats.size();
		Color c;
		
		/* 画负数柱子 */
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			double delx = (i + 1) * categorySpan + i * seriesWidth + seriesWidth / 2.0;

			g.setColor(gp.gridColor);
			float dashes[] = { 2 };
			g.setStroke(new BasicStroke(0.1f, BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND, 1, dashes, 0));
			g.drawLine(gp.gRect1.x + (int)delx, gp.valueBaseLine, (int) (gp.gRect1.x
					+ delx + coorWidth), (int) (gp.valueBaseLine - coorWidth));

			boolean vis = (i % (gp.graphXInterval + 1) == 0);
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

			for (int j = gp.serNum - 1; j >= 0; j--) {
				ExtGraphSery egs = egc.getExtGraphSery(gp.serNames.get(j));
				if (egs.isNull()) {
					continue;
				}
				double val = egs.getValue();
				double tmp = val - gp.baseValue;
				int len = (int) Math.round(dely * gp.tickNum
						* (tmp - gp.minValue) / (gp.maxValue * gp.coorScale));

				int lb = (int) Math.round(gp.gRect1.x + (i + 1) * categorySpan
						+ i * seriesWidth);
				int br = (int) Math
						.round((j + 1) * seriesSpan + j * seriesDeep);
				if (len > 0) {
					continue;
				}

				int cIndex;
				if (!gp.isMultiSeries) {
					cIndex = i;
				} else {
					cIndex = j;
				}
				db.drawRectCube(lb, (int) seriesWidth, len, (int) seriesDeep, br,
						cIndex, htmlLink, egc.getNameString(), egs);
				// 最后输出文字，否则会被图形覆盖
				if (gp.dispValueOntop && !egs.isNull() && vis) {
					String sval = db.getDispValue(egc,egs,gp.serNum);
					x = lb + br + (int) seriesWidth / 2;// - TR.width / 2;
					y = gp.valueBaseLine - len - br;

					if (!gp.isMultiSeries) {
						c = db.getColor(i);
					} else {
						c = db.getColor(j);
					}
					ValueLabel vl;
					if (len < 0) {
						vl = new ValueLabel(sval, new Point(x, y), c,
								GraphFontView.TEXT_ON_BOTTOM);
					} else {
						vl = new ValueLabel(sval, new Point(x, y), c,
								GraphFontView.TEXT_ON_TOP);
					}
					labelList.add(vl);
				}

			}

		}
//		负数的数据先输出，如果下述平面不透明，就应盖住负数的柱顶值
		db.outLabels();
		// 绘制基线透明平面，
		if (gp.valueBaseLine != gp.gRect1.y + gp.gRect1.height) {
			int xx[] = { gp.gRect1.x, (int) (gp.gRect1.x + coorWidth),
					(int) (gp.gRect1.x + gp.gRect1.width + coorWidth),
					(int) (gp.gRect1.x + gp.gRect1.width) };
			int yy[] = { gp.valueBaseLine,
					(int) (gp.valueBaseLine - coorWidth),
					(int) (gp.valueBaseLine - coorWidth), gp.valueBaseLine };
			Polygon poly = new Polygon(xx, yy, 4);

			Color ccc = egp.getAxisColor(GraphProperty.AXIS_BOTTOM);
			if (ccc == null) {// 如果底边为透明色时，使用缺省灰
				ccc = Color.lightGray;
			}
			float trans = 1.0f;
			if (gp.graphTransparent) {
				trans = 0.4f;
			}

			Utils.fill(g, poly, trans,ccc);
		}

		/* 画正数数柱子 */
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			double delx = (i + 1) * categorySpan + i * seriesWidth + seriesWidth / 2.0;
			g.setColor(gp.gridColor);
			float dashes[] = { 2 };
			g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND, 1, dashes, 0));
			g.drawLine(gp.gRect1.x + (int)delx, gp.valueBaseLine, (int) (gp.gRect1.x
					+ delx + coorWidth), (int) (gp.valueBaseLine - coorWidth));

			boolean vis = (i % (gp.graphXInterval + 1) == 0);

			for (int j = gp.serNum - 1; j >= 0; j--) {
				ExtGraphSery egs = egc.getExtGraphSery(gp.serNames.get(j));
				if (egs.isNull()) {
					continue;
				}
				double val = egs.getValue();
				double tmp = val - gp.baseValue;
				int len = (int) Math.round(dely * gp.tickNum
						* (tmp - gp.minValue) / (gp.maxValue * gp.coorScale));

				int lb = (int) Math.round(gp.gRect1.x + (i + 1) * categorySpan
						+ i * seriesWidth);
				int br = (int) Math
						.round((j + 1) * seriesSpan + j * seriesDeep);
				if (len < 0) {
					continue;
				}

				int cIndex;
				if (!gp.isMultiSeries) {
					cIndex = i;
				} else {
					cIndex = j;
				}
				db.drawRectCube(lb, (int) seriesWidth, len, (int) seriesDeep, br,
						cIndex, htmlLink, egc.getNameString(), egs);
				// 最后输出文字，否则会被图形覆盖
				if (gp.dispValueOntop && !egs.isNull() && vis) {
					String sval = db.getDispValue(egc,egs,gp.serNum);
					x = lb + br + (int) seriesWidth / 2;// - TR.width / 2;
					y = gp.valueBaseLine - len - br;

					if (!gp.isMultiSeries) {
						c = db.getColor(i);
					} else {
						c = db.getColor(j);
					}
					ValueLabel vl;
					if (len < 0) {
						vl = new ValueLabel(sval, new Point(x, y), c,
								GraphFontView.TEXT_ON_BOTTOM);
					} else {
						vl = new ValueLabel(sval, new Point(x, y), c,
								GraphFontView.TEXT_ON_TOP);
					}
					labelList.add(vl);
				}

			}

		}

		if (gp.graphTransparent) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					1.00F));
		}
		db.outLabels();
		/* 画一下基线 */
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
}
