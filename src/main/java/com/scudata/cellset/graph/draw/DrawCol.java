package com.scudata.cellset.graph.draw;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

import com.scudata.cellset.graph.*;
import com.scudata.chart.Consts;
import com.scudata.chart.Utils;

/**
 * 柱图实现
 * @author Joancy
 *
 */
public class DrawCol extends DrawBase {
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
	public static int drawing(DrawBase db,StringBuffer htmlLink) {
		//少改动代码，同名引出要用到的实例
		GraphParam gp = db.gp;
		ExtGraphProperty egp = db.egp;
		Graphics2D g = db.g;
		ArrayList<ValueLabel> labelList = db.labelList;
		double seriesWidth;
		double coorWidth;
		double categorySpan;
		double dely;
		double x, y;

		gp.coorWidth = 0;
		db.initGraphInset();

		String sval = "";
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
			return -1;
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
		dely = (gp.graphRect.height - coorWidth) / gp.tickNum;
		gp.gRect1 = (Rectangle2D.Double)gp.graphRect.clone();
		gp.gRect2 = (Rectangle2D.Double)gp.graphRect.clone();
		/* 画坐标轴 */
		db.drawGraphRect();
		/* 画Y轴 */
		for (int i = 0; i <= gp.tickNum; i++) {
			// 画背景虚线
			db.drawGridLine(dely, i);
			// 写y轴标签
			Number coory = (Number) gp.coorValue.get(i);
			String scoory = db.getFormattedValue(coory.doubleValue());
			x = gp.gRect1.x - gp.tickLen;
			y = gp.gRect1.y + gp.gRect1.height - i * dely;
			gp.GFV_YLABEL.outText(x, y, scoory);
			// 设置基线
			if (coory.doubleValue() == gp.baseValue + gp.minValue) {
				gp.valueBaseLine = (gp.gRect1.y + gp.gRect1.height - i* dely);
			}
		}

		// 画警戒线
		db.drawWarnLine();

		/* 画X轴 */
		ArrayList cats = egp.categories;
		Color c;
		for (int i = 0; i < gp.catNum; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			double delx = (i + 1) * categorySpan + i * seriesWidth
					* gp.serNum + seriesWidth * gp.serNum / 2.0;
			boolean valvis = (i % (gp.graphXInterval + 1) == 0);//柱顶是否显示值跟画Table分开
			boolean vis = valvis && !gp.isDrawTable;

			if (vis) {
				c = egp.getAxisColor(GraphProperty.AXIS_BOTTOM);
				Utils.setStroke(g, c, Consts.LINE_SOLID, 1.0f);
				db.drawLine(gp.gRect1.x + delx, gp.gRect1.y + gp.gRect1.height,
						gp.gRect1.x + delx, gp.gRect1.y + gp.gRect1.height
								+ gp.tickLen,c);
				// 画背景虚线
				db.drawGridLineCategoryV(gp.gRect1.x + delx);
			}

			String value = egc.getNameString();
			x = gp.gRect1.x + delx;
			y = gp.gRect1.y + gp.gRect1.height + gp.tickLen;
			gp.GFV_XLABEL.outText(x, y, value, vis);

			for (int j = 0; j < gp.serNum; j++) {
				ExtGraphSery egs = egc.getExtGraphSery(gp.serNames.get(j));
				if (egs.isNull()) {
					continue;
				}
				double val = egs.getValue();
				double tmp = val - gp.baseValue;
				double len = dely * gp.tickNum * (tmp - gp.minValue) / (gp.maxValue * gp.coorScale);
				double lb;
				if( gp.isDrawTable && gp.serNum==1){
					lb = db.getDataTableX( i )-seriesWidth/2;
				}else{
					lb = gp.gRect1.x + (i + 1) * categorySpan + (i
							* gp.serNum + j)
							* seriesWidth;
				}
				
				// 画柱子
				if (len >= 0) {
					Color tmpc;
					if (!gp.isMultiSeries) {
						tmpc = db.getColor(i);
					} else {
						tmpc = db.getColor(j);
					}
					Color bc = egp.getAxisColor(GraphProperty.AXIS_COLBORDER);
					int bs = Consts.LINE_SOLID;
					float bw = 1.0f;
					Utils.draw2DRect(g, lb, gp.valueBaseLine - len,
							seriesWidth, len, bc, bs, bw,
							egp.isDrawShade(), egp.isRaisedBorder(),
							db.getTransparent(), db.getChartColor(tmpc), true);
					db.htmlLink(lb, gp.valueBaseLine - len, seriesWidth,
							len, htmlLink, egc.getNameString(), egs);
				} else {
					Color tmpc;
					if (!gp.isMultiSeries) {
						tmpc = db.getColor(i);
					} else {
						tmpc = db.getColor(j);
					}
					Color bc = egp.getAxisColor(GraphProperty.AXIS_COLBORDER);
					int bs = Consts.LINE_SOLID;
					float bw = 1.0f;
					Utils.draw2DRect(g, lb, gp.valueBaseLine,
							seriesWidth, Math.abs(len), bc, bs, bw,
							egp.isDrawShade(), egp.isRaisedBorder(),
							db.getTransparent(), db.getChartColor(tmpc), true);
					db.htmlLink(lb, gp.valueBaseLine, seriesWidth,
							Math.abs(len), htmlLink, egc.getNameString(), egs);
				}

				// 在柱顶显示数值
				if (gp.dispValueOntop && !egs.isNull() && valvis) {
					sval = db.getDispValue(egc,egs,gp.serNum);
					x = lb + seriesWidth / 2;
					y = gp.valueBaseLine - len;

					if (!gp.isMultiSeries) {
						c = db.getColor(i);
					} else {
						c = db.getColor(j);
					}
					ValueLabel vl;
					if (len < 0) {
						vl = new ValueLabel(sval, new Point2D.Double(x, y), c,
								GraphFontView.TEXT_ON_BOTTOM);
					} else {
						vl = new ValueLabel(sval, new Point2D.Double(x, y), c,
								GraphFontView.TEXT_ON_TOP);
					}
					labelList.add(vl);
				}
			}
		}
		db.outLabels();

		/* 重画一下基线 */
		db.drawLine(gp.gRect1.x, gp.valueBaseLine, gp.gRect1.x + gp.gRect1.width,
				gp.valueBaseLine, egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
		db.drawLine(gp.gRect1.x + gp.gRect1.width, gp.valueBaseLine,
				 (gp.gRect1.x + gp.gRect1.width + coorWidth),
				 (gp.valueBaseLine - coorWidth),
				egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
		if (!gp.isMultiSeries) {
			return gp.catNum;
		}else{
			return gp.serNum;
		}
	}

}
