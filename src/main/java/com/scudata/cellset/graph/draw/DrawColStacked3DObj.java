package com.scudata.cellset.graph.draw;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

import com.scudata.cellset.graph.*;
import com.scudata.cellset.graph.config.IGraphProperty;
import com.scudata.chart.Consts;
import com.scudata.chart.Utils;
import com.scudata.common.*;
/**
 * 三维堆积立体柱图实现
 * @author Joancy
 *
 */

public class DrawColStacked3DObj extends DrawBase {
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
		double seriesWidth;
		double coorWidth;
		double categorySpan;
		double dely;
		double tmpInt;

		gp.maxValue = gp.maxPositive;
		gp.minValue = gp.minNegative;

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


		int serNum = 1;
		if (egp.category2 != null) {
			serNum = 2;
		}
		
		if (gp.barDistance > 0) {
			double maxCatSpan = (gp.graphRect.width - serNum * gp.catNum * 1.0f)
					/ (gp.catNum + 1.0f);
			if (gp.barDistance <= maxCatSpan) {
				categorySpan = gp.barDistance;
			} else {
				categorySpan = maxCatSpan;
				Logger.warning("Category span :"+
				gp.barDistance+" is too large. Use max limit span:"+maxCatSpan);
			}
			seriesWidth = (gp.graphRect.width - (gp.catNum + 1) * categorySpan)
			/ (serNum * gp.catNum);
		} else {
			seriesWidth = (gp.graphRect.width / (((gp.catNum + 1)
					* gp.categorySpan / 100.0)
					+ gp.coorWidth / 200.0 + gp.catNum * serNum));

			categorySpan = (seriesWidth * (gp.categorySpan / 100.0));
		}
		
		coorWidth = (seriesWidth * (gp.coorWidth / 200.0));
		tmpInt = (gp.catNum + 1) * categorySpan + coorWidth + gp.catNum
				* serNum * seriesWidth;
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
		Point2D.Double p;
		/* 画Y轴 */
		for (int i = 0; i <= gp.tickNum; i++) {
			db.drawGridLine(dely, i);
			Number coory = (Number) gp.coorValue.get(i);
			String scoory = db.getFormattedValue(coory.doubleValue());

			p = db.getVTickPoint(i * dely);
			gp.GFV_YLABEL.outText(p.x - gp.tickLen, p.y, scoory);
			// 设置基线
			if (coory.doubleValue() == gp.baseValue + gp.minValue) {
				gp.valueBaseLine =  (gp.gRect1.y + gp.gRect1.height - i
						* dely);
			}
		}

		
		/* 先画负的柱子 */
		ArrayList<Desc3DRect> negativeRects = new ArrayList<Desc3DRect>();
		ArrayList cats = egp.categories;
		int cc = cats.size();
		Color c;
		
//		柱子有正和负，先画背景网格线
		for (int i = 0; i < cc; i++) {
			double delx = (i + 1) * categorySpan + i * seriesWidth
					* serNum + seriesWidth * serNum / 2.0;
			boolean valvis = (i % (gp.graphXInterval + 1) == 0);//柱顶是否显示值跟画Table分开
			boolean vis = valvis && !gp.isDrawTable;
			p = db.getHTickPoint(delx);
			if (vis) {
				db.drawGridLineCategoryV(gp.gRect2.x + delx);
			}
		}
		
		if (gp.minNegative < 0) {
			for (int i = cc-1; i >= 0; i--) {
				ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
				double delx = (i + 1) * categorySpan + i * seriesWidth
						* serNum + seriesWidth * serNum / 2.0;
				
				boolean vis = (i % (gp.graphXInterval + 1) == 0);
				p = db.getHTickPoint(delx);
				if (vis) {
					c = egp.getAxisColor(GraphProperty.AXIS_BOTTOM);
					Utils.setStroke(g, c, Consts.LINE_SOLID, 1.0f);
					db.drawLine(p.x, p.y, p.x, p.y + gp.tickLen, c);
				}

				String value = egc.getNameString();
				gp.GFV_XLABEL.outText(p.x, p.y + gp.tickLen, value, vis);
				double negativeBase = gp.valueBaseLine;
				double lb = gp.gRect1.x + (i + 1) * categorySpan + (i
						* serNum + 0)
						* seriesWidth;

				if (egp.category2 == null) {
					drawNegativeSeries(0, gp.serNames,egc,
							dely, db, lb,
							seriesWidth, htmlLink, negativeBase,
							coorWidth, vis,negativeRects);
				}else{
					drawNegativeSeries(0, gp.serNames,egc,
							dely, db, lb, 
							seriesWidth, htmlLink, negativeBase,
							coorWidth, vis,negativeRects);
					lb = gp.gRect1.x + (i + 1) * categorySpan + (i
							* serNum + 1)
							* seriesWidth;
					
					egc = (ExtGraphCategory) egp.category2.get(i);
					drawNegativeSeries(gp.serNames.size(), gp.serNames2,egc,
							dely, db, lb, 
							seriesWidth, htmlLink, negativeBase,
							coorWidth, vis,negativeRects);
				}

			}

			for (int i = negativeRects.size() - 1; i >= 0; i--) {
				Desc3DRect d3 = negativeRects.get(i);
				Utils.draw3DRect(g, d3);
			}
		}

		// 绘制基线透明平面，
		if (gp.valueBaseLine != gp.gRect1.y + gp.gRect1.height) {
			double xx[] = { gp.gRect1.x, gp.gRect1.x + coorWidth,
					gp.gRect1.x + gp.gRect1.width + coorWidth,
					gp.gRect1.x + gp.gRect1.width };
			double yy[] = { gp.valueBaseLine,
					gp.valueBaseLine - coorWidth,
					gp.valueBaseLine - coorWidth, gp.valueBaseLine };
			Shape poly = Utils.newPolygon2D(xx, yy);

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

		// 画警戒线
		db.drawWarnLine();

		/* 画正数柱子 */
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			double delx = (i + 1) * categorySpan + i * seriesWidth
					* serNum + seriesWidth * serNum / 2.0;
			boolean vis = (i % (gp.graphXInterval + 1) == 0);
			
			p = db.getHTickPoint(delx);
			if (vis) {
				c = egp.getAxisColor(GraphProperty.AXIS_BOTTOM);
				Utils.setStroke(g, c, Consts.LINE_SOLID, 1.0f);
				db.drawLine(p.x, p.y, p.x, p.y + gp.tickLen, c);
			}

			String value = egc.getNameString();
			gp.GFV_XLABEL.outText(p.x, p.y + gp.tickLen, value, vis);
			
			if (gp.graphTransparent) {
				g.setComposite(AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, 0.60F));
			}
			double positiveBase = gp.valueBaseLine;
			double lb = gp.gRect1.x + (i + 1) * categorySpan + (i
					* serNum + 0)
					* seriesWidth;

			if (egp.category2 == null) {
				drawPositiveSeries(0, gp.serNames,egc,
						dely, db, lb,
						seriesWidth, htmlLink, positiveBase,
						coorWidth, vis);
			}else{
				drawPositiveSeries(0, gp.serNames,egc,
						dely, db, lb, 
						seriesWidth, htmlLink, positiveBase,
						coorWidth, vis);
				lb = gp.gRect1.x + (i + 1) * categorySpan + (i
						* serNum + 1)
						* seriesWidth;
				
				egc = (ExtGraphCategory) egp.category2.get(i);
				drawPositiveSeries(gp.serNames.size(), gp.serNames2,egc,
						dely, db, lb, 
						seriesWidth, htmlLink, positiveBase,
						coorWidth, vis);
			}

		}

		db.outLabels();
		/* 重画一下基线 */
		if (gp.valueBaseLine != gp.gRect1.y + gp.gRect1.height) {
			db.drawLine(gp.gRect1.x, gp.valueBaseLine, gp.gRect1.x
					+ gp.gRect1.width, gp.valueBaseLine,
					egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
			db.drawLine(gp.gRect1.x + gp.gRect1.width, gp.valueBaseLine,
					gp.gRect1.x + gp.gRect1.width + coorWidth,
					gp.valueBaseLine - coorWidth,
					egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
		}
	}

	private static void drawNegativeSeries(int serNumBase, Vector serNames,
			ExtGraphCategory egc, double dely, DrawBase db, double dlb,
			double seriesWidth, StringBuffer htmlLink, double negativeBase,
			double coorWidth, boolean vis, ArrayList<Desc3DRect> negativeRects) {
		GraphParam gp = db.gp;
		ExtGraphProperty egp = db.egp;
		Graphics2D g = db.g;
		double lb = dlb;

		ArrayList<ValueLabel> labelList = db.labelList;
		if (gp.graphTransparent) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					0.60F));
		}

		int serNum = serNames.size();
		for (int j = 0; j < serNum; j++) {
			ExtGraphSery egs = egc.getExtGraphSery(serNames.get(j));
			if (egs.isNull()) {
				continue;
			}
			double val = egs.getValue();
			double tmp = val - gp.baseValue;
			double len = Math.round(dely * gp.tickNum * (tmp - gp.minValue)
					/ (gp.maxValue * gp.coorScale));
			if (len == 0) {
				continue;
			}
			double xx, yy, ww, hh;
			if (len >= 0) {
				continue;
			} else {
				xx = lb;
				yy = negativeBase;
				ww = seriesWidth;
				hh = Math.abs(len);
			}
			Color bc = egp.getAxisColor(GraphProperty.AXIS_COLBORDER);
			int bs = Consts.LINE_SOLID;
			float bw = 1.0f;
			Color tmpc = db.getColor(j + serNumBase);
			double coorShift = coorWidth;

			negativeRects.add(Utils.get3DRect(xx, yy, ww, hh, bc, bs, bw,
					egp.isDrawShade(), egp.isRaisedBorder(),
					db.getTransparent(), db.getChartColor(tmpc), false,
					coorShift));

			String percentFmt = null;
			if (vis) {
				if (gp.dispValueType == GraphProperty.DISPDATA_PERCENTAGE
						|| gp.dispValueType == GraphProperty.DISPDATA_NAME_PERCENTAGE) {
					if (StringUtils.isValidString(gp.dataMarkFormat)) {
						percentFmt = gp.dataMarkFormat;
					} else {
						percentFmt = "0.00%";
					}
				}
			}

			ValueLabel vl = null;
			double x = (lb + seriesWidth / 2);
			if (len > 0) {
				// 上面已经跳过
			} else {
				String sval = null;
				if (percentFmt != null) {
					sval = db.getFormattedValue(
							egs.getValue() / egc.getNegativeSumSeries(),
							percentFmt);
					if (egc != null
							&& gp.dispValueType == IGraphProperty.DISPDATA_NAME_PERCENTAGE) {
						sval = getDispName(egc, egs, serNum) + "," + sval;
					}
				}else{
					sval = db.getDispValue(egc,egs,gp.serNum);
				}
				if (StringUtils.isValidString(sval)) {
					vl = new ValueLabel(sval, new Point2D.Double(x, (negativeBase - len
							/ 2)), gp.GFV_VALUE.color,
							GraphFontView.TEXT_ON_CENTER);
				}

				negativeBase -= len;
			}
			if (vl != null) {
				labelList.add(vl);
			}
		}

		if (gp.graphTransparent) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					1.00F));
		}

		// 在柱顶显示数值
		if (gp.dispStackSumValue && vis) {
			double val = db.getScaledValue(egc.getPositiveSumSeries(), true);
			String sval;
			ValueLabel vl = null;

			val = db.getScaledValue(egc.getNegativeSumSeries(), true);
			if (val < 0) {
				sval = db.getFormattedValue(val);
				double x = (lb + seriesWidth / 2);
				double y = negativeBase + 3;
				vl = new ValueLabel(sval, new Point2D.Double(x, y), gp.GFV_VALUE.color,
						GraphFontView.TEXT_ON_BOTTOM);
			}

			if (vl != null) {
				labelList.add(vl);
			}

		}
	}

	private static void drawPositiveSeries(int serNumBase, Vector serNames,
			ExtGraphCategory egc, double dely, DrawBase db, double dlb,
			double seriesWidth, StringBuffer htmlLink, double positiveBase,
			double coorWidth, boolean vis) {
		GraphParam gp = db.gp;
		ExtGraphProperty egp = db.egp;
		Graphics2D g = db.g;

		double lb = dlb;
		ArrayList<ValueLabel> labelList = db.labelList;
		if (gp.graphTransparent) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					0.60F));
		}

		int serNum = serNames.size();
		for (int j = 0; j < serNum; j++) {

			ExtGraphSery egs = egc.getExtGraphSery(serNames.get(j));
			if (egs.isNull()) {
				continue;
			}
			double val = egs.getValue();
			double tmp = val - gp.baseValue;
			double len = Math.round(dely * gp.tickNum * (tmp - gp.minValue)
					/ (gp.maxValue * gp.coorScale));
			if (len == 0) {
				continue;
			}
			double xx, yy, ww, hh;
			if (len >= 0) {
				xx = lb;
				yy = positiveBase - len;
				ww = seriesWidth;
				hh = len;
			} else {
				continue;
			}
			Color bc = egp.getAxisColor(GraphProperty.AXIS_COLBORDER);
			int bs = Consts.LINE_SOLID;
			float bw = 1.0f;
			Color tmpc = db.getColor(j + serNumBase);
			double coorShift = coorWidth;
			Utils.draw3DRect(g, xx, yy, ww, hh, bc, bs, bw, egp.isDrawShade(),
					egp.isRaisedBorder(), db.getTransparent(),
					db.getChartColor(tmpc), false, coorShift);
			db.htmlLink(xx, yy, ww, hh, htmlLink, egc.getNameString(), egs);

			String percentFmt = null;
			if (vis) {
				if (gp.dispValueType == GraphProperty.DISPDATA_PERCENTAGE
						|| gp.dispValueType == GraphProperty.DISPDATA_NAME_PERCENTAGE) {
					if (StringUtils.isValidString(gp.dataMarkFormat)) {
						percentFmt = gp.dataMarkFormat;
					} else {
						percentFmt = "0.00%";
					}
				}
			}
			
			ValueLabel vl = null;
			double x = (lb + seriesWidth / 2);
			if (len > 0) {
				String sval = null;
				if (percentFmt != null) {// 柱中显示百分比
					sval = db.getFormattedValue(
							egs.getValue() / egc.getPositiveSumSeries(),
							percentFmt);
					if (egc != null
							&& gp.dispValueType == IGraphProperty.DISPDATA_NAME_PERCENTAGE) {
						sval = getDispName(egc, egs, serNum) + "," + sval;
					}
				}else{
					sval = db.getDispValue(egc,egs,gp.serNum);
				}

				if (StringUtils.isValidString(sval)) {
					vl = new ValueLabel(sval, new Point2D.Double(x, (positiveBase - len
							/ 2)), gp.GFV_VALUE.color,
							GraphFontView.TEXT_ON_CENTER);
				}

				positiveBase -= len;
			} else {
				// 没有负数
			}
			if (vl != null) {
				labelList.add(vl);
			}
		}
		if (gp.graphTransparent) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					1.00F));
		}

		// 在柱顶显示数值
		if (gp.dispStackSumValue && vis) {
			double val = db.getScaledValue(egc.getPositiveSumSeries(), true);
			String sval;
			ValueLabel vl = null;

			if (val > 0) {
				sval = db.getFormattedValue(val);
				double x = (lb + seriesWidth / 2);
				double y = positiveBase - 3;
				vl = new ValueLabel(sval, new Point2D.Double(x, y), gp.GFV_VALUE.color,
						gp.GFV_VALUE.textPosition);
			}
			if (vl != null) {
				labelList.add(vl);
			}

		}

	}
}
