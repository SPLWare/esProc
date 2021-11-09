package com.raqsoft.cellset.graph.draw;

import java.awt.*;
import java.util.*;

import com.raqsoft.cellset.graph.*;
import com.raqsoft.chart.Consts;
import com.raqsoft.chart.Utils;
/**
 * 三维立体柱图实现
 * @author Joancy
 *
 */

public class DrawCol3DObj extends DrawBase {
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
		
		double seriesWidth;
		double coorWidth;
		double categorySpan;
		double dely;
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

		if (gp.coorWidth < 0 || gp.coorWidth > 10000) {
			gp.coorWidth = 0;
		}

		if (gp.barDistance > 0) {
			double maxCatSpan = (gp.graphRect.width - gp.serNum * gp.catNum
					* 1.0f)
					/ (gp.catNum + 1.0f);
			if (gp.barDistance <= maxCatSpan) {
				categorySpan = gp.barDistance;
			} else {
				categorySpan = maxCatSpan;
			}
			seriesWidth = (gp.graphRect.width - (gp.catNum + 1) * categorySpan)
					/ (gp.serNum * gp.catNum);
		} else {
			seriesWidth = (gp.graphRect.width / (((gp.catNum + 1)
					* gp.categorySpan / 100.0)
					+ gp.coorWidth / 200.0 + gp.catNum * gp.serNum));

			categorySpan = (seriesWidth * (gp.categorySpan / 100.0));
		}
		coorWidth = (seriesWidth * (gp.coorWidth / 200.0));
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
		Point p;
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
		
		/* 先画负数柱子 */
		if (gp.graphTransparent) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					0.60F));
		}
		ArrayList cats = egp.categories;
		int cc = cats.size();
		Color c;
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			double delx = (i + 1) * categorySpan + i * seriesWidth
					* gp.serNum + seriesWidth * gp.serNum / 2.0;
			
			boolean vis = i % (gp.graphXInterval + 1) == 0;
			p = db.getHTickPoint(delx);
			if (vis) {
				c = egp.getAxisColor(GraphProperty.AXIS_BOTTOM);
				Utils.setStroke(g, c, Consts.LINE_SOLID, 1.0f);
				db.drawLine(p.x, p.y,p.x, p.y + gp.tickLen,c);
				// 画背景虚线
				db.drawGridLineCategoryV(gp.gRect2.x + (int)delx);
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
				int len = (int) (dely * gp.tickNum * (tmp - gp.minValue) / (gp.maxValue * gp.coorScale));
				int lb = (int) (gp.gRect1.x + (i + 1) * categorySpan + (i
						* gp.serNum + j)
						* seriesWidth);
				if (len > 0) {
					continue;
				}

				int cIndex;
				if (!gp.isMultiSeries) {
					cIndex = i;
				} else {
					cIndex = j;
				}
				db.drawRectCube(lb, (int) seriesWidth, len, (int) coorWidth, 0,
						cIndex, htmlLink, egc.getNameString(), egs);
				if (gp.dispValueOntop && !egs.isNull() && vis) { // 柱顶显示数值
					String sval = db.getDispValue(egc,egs,gp.serNum);
					x = lb + (int) seriesWidth / 2; // - TR.width / 2;
					y = gp.valueBaseLine - len;
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

			Utils.fill(g, poly, trans, ccc);
		}
		
//再画正数柱子
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			boolean vis = i % (gp.graphXInterval + 1) == 0;


			for (int j = 0; j < gp.serNum; j++) {
				ExtGraphSery egs = egc.getExtGraphSery(gp.serNames.get(j));
				if (egs.isNull()) {
					continue;
				}
				double val = egs.getValue();
				double tmp = val - gp.baseValue;
				int len = (int) (dely * gp.tickNum * (tmp - gp.minValue) / (gp.maxValue * gp.coorScale));
				int lb = (int) (gp.gRect1.x + (i + 1) * categorySpan + (i
						* gp.serNum + j)
						* seriesWidth);
				if (len < 0) {
					continue;
				}

				int cIndex;
				if (!gp.isMultiSeries) {
					cIndex = i;
				} else {
					cIndex = j;
				}
				db.drawRectCube(lb, (int) seriesWidth, len, (int) coorWidth, 0,
						cIndex, htmlLink, egc.getNameString(), egs);
				if (gp.dispValueOntop && !egs.isNull() && vis) { // 柱顶显示数值
					String sval = db.getDispValue(egc,egs,gp.serNum);
					x = lb + (int) seriesWidth / 2; // - TR.width / 2;
					y = gp.valueBaseLine - len;
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
		
		/* 当基线跟底座不重合时，才需要绘制 */
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
