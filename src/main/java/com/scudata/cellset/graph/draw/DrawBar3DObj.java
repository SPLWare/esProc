package com.scudata.cellset.graph.draw;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

import com.scudata.cellset.graph.*;
import com.scudata.chart.ChartColor;
import com.scudata.chart.Consts;
import com.scudata.chart.Utils;
/**
 * 三维条形图的实现
 * @author Joancy
 *
 */

public class DrawBar3DObj extends DrawBase {
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
		double tmpInt;
		double x, y;

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
					* gp.categorySpan / 100.0)
					+ gp.coorWidth / 200.0 + gp.catNum * gp.serNum));
			categorySpan = (seriesWidth * (gp.categorySpan / 100.0));
		}

		coorWidth = (seriesWidth * (gp.coorWidth / 200.0));

		tmpInt = (gp.catNum + 1) * categorySpan + coorWidth + gp.catNum
				* gp.serNum * seriesWidth;
		gp.graphRect.y += (gp.graphRect.height - tmpInt) / 2;
		gp.graphRect.height = tmpInt;

		delx = (gp.graphRect.width - coorWidth) / gp.tickNum;
		tmpInt = delx * gp.tickNum + coorWidth;
		gp.graphRect.x += (gp.graphRect.width - tmpInt) / 2;
		gp.graphRect.width = tmpInt;

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
		Point2D.Double p;
		/* 画X轴 */
		for (int i = 0; i <= gp.tickNum; i++) {
			db.drawGridLineV(delx, i);
			// 画x轴标签
			Number coorx = (Number) gp.coorValue.get(i);
			String scoorx = db.getFormattedValue(coorx.doubleValue());
			p = db.getHTickPoint(i * delx);
			gp.GFV_XLABEL.outText(p.x, p.y + gp.tickLen, scoorx);

			// 设置基线
			if (coorx.doubleValue() == gp.baseValue + gp.minValue) {
				gp.valueBaseLine =  (gp.gRect1.x + i * delx);
			}
		}

		// 画警戒线
		db.drawWarnLineH();

		/* 画柱子 */
		ArrayList cats = egp.categories;
		int cc = cats.size();
		Color c;
		for (int i = cc-1; i >=0 ; i--) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			double dely = (i + 1) * categorySpan + i * seriesWidth * gp.serNum
					+ seriesWidth * gp.serNum / 2.0;
			boolean vis = i % (gp.graphXInterval + 1) == 0;
			p = db.getVTickPoint(dely);
			if (vis) {
				c = egp.getAxisColor(GraphProperty.AXIS_LEFT);
				Utils.setStroke(g, c, Consts.LINE_SOLID, 1.0f);
				db.drawLine(p.x - gp.tickLen, p.y, p.x, p.y, c);
				db.drawGridLineCategory( gp.gRect2.y + dely );
			}

			String value = egc.getNameString();
			x = gp.gRect1.x - gp.tickLen;
			y = gp.gRect1.y + dely;
			gp.GFV_YLABEL.outText(x, y, value, vis);
			
			for (int j = gp.serNum - 1; j >= 0; j--) {
				ExtGraphSery egs = egc.getExtGraphSery(gp.serNames.get(j));
				if (egs.isNull()) {
					continue;
				}
				double len = 0, orginLen = 0;
				double val = egs.getValue();
				double tmp = val - gp.baseValue;
				len = delx * gp.tickNum * (tmp - gp.minValue) / (gp.maxValue * gp.coorScale);

				orginLen = len;
				double lb = gp.gRect1.y + (i + 1) * categorySpan + (i
						* gp.serNum + j + 1)
						* seriesWidth;
				double xx, yy, ww, hh;
				ChartColor chartColor;
				if (!gp.isMultiSeries) {
					chartColor = db.getChartColor(db.getColor(i));
				} else {
					chartColor = db.getChartColor(db.getColor(j));
				}

				if (len >= 0) {
					xx = gp.valueBaseLine;
					yy = lb - seriesWidth;
					ww = len;
					hh = seriesWidth;
				} else {
					xx = gp.valueBaseLine + len;
					yy = lb - seriesWidth;
					ww = Math.abs(len);
					hh = seriesWidth;
				}

				Color bc = null;
				int bs = 0;
				float bw = 0;
				double coorShift = coorWidth;
				Utils.draw3DRect(g, xx, yy, ww, hh, bc, bs, bw,
						egp.isDrawShade(), egp.isRaisedBorder(),
						db.getTransparent(), chartColor, !egp.isBarGraph(db),
						coorShift);
				db.htmlLink(xx, yy, ww, hh, htmlLink, egc.getNameString(), egs);

				// 在柱顶显示数值
				if (gp.dispValueOntop && !egs.isNull() && vis) {
					String sval = db.getDispValue(egc,egs,gp.serNum);
					y = lb - seriesWidth / 2;

					if (orginLen < 0) {
						orginLen = orginLen - 3; // 留3个点空隙
					} else {
						orginLen = orginLen + 3;
					}

					x = gp.valueBaseLine + orginLen;
					if (!gp.isMultiSeries) {
						c = db.getColor(i);
					} else {
						c = db.getColor(j);
					}
					ValueLabel vl;
					if (orginLen < 0) {
						vl = new ValueLabel(sval, new Point2D.Double(x, y), c,
								GraphFontView.TEXT_ON_LEFT);
					} else {
						vl = new ValueLabel(sval, new Point2D.Double(x, y), c,
								GraphFontView.TEXT_ON_RIGHT);
					}
					labelList.add(vl);
				}
			}
			if (gp.graphTransparent) {
				g.setComposite(AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, 1.0F));
			}
		}
		db.outLabels();
		/* 画一下基线 */
		if (gp.valueBaseLine != gp.gRect1.x) {
			db.drawLine(gp.valueBaseLine, gp.gRect1.y, gp.valueBaseLine,
					gp.gRect1.y + gp.gRect1.height,
					egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
			db.drawLine(gp.valueBaseLine, gp.gRect1.y, gp.valueBaseLine
					+ coorWidth, gp.gRect1.y - coorWidth,
					egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
		}
	}
}
