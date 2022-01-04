package com.scudata.cellset.graph.draw;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import com.scudata.cellset.graph.*;
import com.scudata.chart.Consts;
import com.scudata.chart.Utils;
/**
 * 条形图的实现
 * @author Joancy
 *
 */

public class DrawBar extends DrawBase {
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
		
		double seriesWidth;
		double coorWidth;
		double categorySpan;
		double delx;
		double x, y;

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

		if (gp.barDistance > 0) {
			double maxCatSpan = (gp.graphRect.height - gp.serNum * gp.catNum
					* 1.0f)
					/ (gp.catNum + 1.0f);
			if (gp.barDistance <= maxCatSpan) {
				categorySpan = gp.barDistance;
			} else {
				categorySpan = maxCatSpan;
			}
			seriesWidth = (gp.graphRect.height - (gp.catNum + 1) * categorySpan)
					/ (gp.serNum * gp.catNum);
		} else {
			seriesWidth = (gp.graphRect.height / (((gp.catNum + 1)
					* gp.categorySpan / 100.0) + gp.catNum * gp.serNum));
			categorySpan = (seriesWidth * (gp.categorySpan / 100.0));
		}

		coorWidth = (seriesWidth * (gp.coorWidth / 200.0));

		delx = (gp.graphRect.width - coorWidth) / gp.tickNum;
		gp.gRect1 = (Rectangle2D.Double)gp.graphRect.clone();
		gp.gRect2 = (Rectangle2D.Double)gp.graphRect.clone();
		/* 画坐标轴 */
		db.drawGraphRect();

		/* 画X轴 */
		for (int i = 0; i <= gp.tickNum; i++) {
			db.drawGridLineV(delx, i);

			// 画x轴标签
			Number coorx = (Number) gp.coorValue.get(i);
			String scoorx = db.getFormattedValue(coorx.doubleValue());

			x = gp.gRect1.x + i * delx;
			y = gp.gRect1.y + gp.gRect1.height + gp.tickLen;
			gp.GFV_XLABEL.outText(x, y, scoorx);

			// 设置基线
			if (coorx.doubleValue() == gp.baseValue + gp.minValue) {
				gp.valueBaseLine = gp.gRect1.x + i * delx;
			}
		}

		// 画警戒线
		db.drawWarnLineH();

		/* 画X轴,柱子 */
		ArrayList cats = egp.categories;
		int cc = cats.size();
		Color c,tmpc;
		Color bc = egp.getAxisColor(GraphProperty.AXIS_COLBORDER);
		int bs = Consts.LINE_SOLID;
		float bw = 1.0f;
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			double dely = (i + 1) * categorySpan + i * seriesWidth
					* gp.serNum + seriesWidth * gp.serNum / 2.0;
			boolean vis = i % (gp.graphXInterval + 1) == 0;
			if (vis) {
				c = egp.getAxisColor(GraphProperty.AXIS_LEFT);
				Utils.setStroke(g, c, Consts.LINE_SOLID, 1.0f);
				db.drawLine(gp.gRect1.x, gp.gRect1.y + gp.gRect1.height - dely,
						gp.gRect1.x - gp.tickLen, gp.gRect1.y
								+ gp.gRect1.height - dely,c);
				db.drawGridLineCategory( gp.gRect1.y + dely );
			}

			String scoory = egc.getNameString();
			x = gp.gRect1.x - gp.tickLen;
			y = gp.gRect1.y + dely;
			gp.GFV_YLABEL.outText(x, y, scoory, vis);

			for (int j = 0; j < gp.serNum; j++) {
				ExtGraphSery egs = egc.getExtGraphSery(gp.serNames.get(j));
				if (egs.isNull()) {
					continue;
				}
				double val = egs.getValue();
				double len = 0;
				double tmp = val - gp.baseValue;
				len = delx * gp.tickNum * (tmp - gp.minValue) / (gp.maxValue * gp.coorScale);
				double lb = (gp.gRect1.y + (i + 1) * categorySpan + (i
						* gp.serNum + j + 1)
						* seriesWidth);
				int cIndex;
				if (!gp.isMultiSeries) {
					cIndex = i;
				} else {
					cIndex = j;
				}
				tmpc = db.getColor(cIndex);
				if (len >= 0) {
					Utils.draw2DRect(g, gp.valueBaseLine, lb - seriesWidth, len,
							seriesWidth, bc, bs, bw,
							egp.isDrawShade(), egp.isRaisedBorder(),
							db.getTransparent(), db.getChartColor(tmpc), false);

					db.htmlLink(gp.valueBaseLine, lb - seriesWidth, len,
							seriesWidth, htmlLink, egc.getNameString(),
							egs);
				} else {
					Utils.draw2DRect(g, gp.valueBaseLine + len, lb - seriesWidth,
							Math.abs(len), seriesWidth, bc, bs, bw,
							egp.isDrawShade(), egp.isRaisedBorder(),
							db.getTransparent(), db.getChartColor(tmpc), false);
					db.htmlLink(gp.valueBaseLine + len, lb - seriesWidth,
							Math.abs(len), seriesWidth, htmlLink,
							egc.getNameString(), egs);
				}

				// 在柱顶显示数值
				if (gp.dispValueOntop && !egs.isNull() && vis) {
					String sval = db.getDispValue(egc,egs,gp.serNum);
					y = lb - (seriesWidth) / 2;

					if (len < 0) {
						len = len - 3; // 留3个点空隙
					} else {
						len = len + 3;
					}

					x = gp.valueBaseLine + len;

					if (!gp.isMultiSeries) {
						c = db.getColor(i);
					} else {
						c = db.getColor(j);
					}
					ValueLabel vl;
					if (len < 0) {
						vl = new ValueLabel(sval, new Point2D.Double(x, y), c,
								GraphFontView.TEXT_ON_LEFT);
					} else {
						vl = new ValueLabel(sval, new Point2D.Double(x, y), c,
								GraphFontView.TEXT_ON_RIGHT);
					}
					labelList.add(vl);
				}
			}
		}
		db.outLabels();
		db.drawLine(gp.valueBaseLine, gp.gRect1.y, gp.valueBaseLine, gp.gRect1.y
				+ gp.gRect1.height, egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
		db.drawLine(gp.valueBaseLine, gp.gRect1.y, gp.valueBaseLine
				+ coorWidth, gp.gRect1.y - coorWidth,
				egp.getAxisColor(GraphProperty.AXIS_BOTTOM));

	}
}
