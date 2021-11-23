package com.scudata.cellset.graph.draw;

import java.awt.*;
import java.util.*;
import java.awt.geom.*;

import com.scudata.cellset.graph.*;
import com.scudata.chart.ChartColor;
import com.scudata.chart.Consts;
import com.scudata.chart.Utils;
import com.scudata.common.*;
import com.scudata.resources.EngineMessage;
/**
 * 饼图实现
 * @author Joancy
 *
 */

public class DrawPie extends DrawBase {
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
		double x, y, w, h;
		double radiusx = 0;
		double radiusy = 0;
		double dely;
		double tmpInt1, tmpInt2;

		gp.coorWidth = 0;
		gp.pieHeight = 0;
		gp.pieRotation = 100;

		db.initGraphInset();

		if (gp.minValue < 0) {
			g.setColor(Color.black);
			g.setFont(gp.GFV_TITLE.font);
			
		   MessageManager mm = EngineMessage.get();
			g.drawString(mm.getMessage("drawpie.negativedata",gp.minValue) ,
					50, 50);
			return;
		}

		if (gp.pieHeight > 100) {
			gp.pieHeight = 100;
		}
		if (gp.pieHeight < 0) {
			gp.pieHeight = 0;

		}
		db.drawLegend(htmlLink);
		db.drawTitle();
		db.drawLabel();
		gp.gRect2 = new Rectangle2D.Double(gp.leftInset, gp.topInset, gp.graphWidth
				- gp.leftInset - gp.rightInset, gp.graphHeight - gp.topInset
				- gp.bottomInset);
		gp.gRect1 = (Rectangle2D.Double)gp.gRect2.clone();
		db.keepGraphSpace();

		gp.graphRect = new Rectangle2D.Double(gp.leftInset,
				gp.topInset, // + 20,
				gp.graphWidth - gp.leftInset - gp.rightInset,
				gp.graphHeight - gp.topInset - gp.bottomInset);

		if (gp.graphRect.width < 10 || gp.graphRect.height < 10) {
			return;
		}
		if (gp.serNum == 0) {
			radiusx =  (gp.graphRect.width / 2);
		} else {
			radiusx =  (gp.graphRect.width / (2 * gp.serNum));
		}
		radiusy =  (gp.graphRect.height / (2 * gp.serNum + (gp.pieHeight / 100.0)));
		if (gp.pieRotation < 10) {
			gp.pieRotation = 10;
		}
		if (gp.pieRotation > 100) {
			gp.pieRotation = 100;
		}
		if (radiusx * (gp.pieRotation / 100.0) > radiusy) {
			radiusx =  (radiusy / (gp.pieRotation / 100.0));
		} else {
			radiusy =  (radiusx * gp.pieRotation / 100.0);
		}
		dely =  (radiusy * (gp.pieHeight / 100.0));
		tmpInt1 = radiusx * (2 * gp.serNum);
		tmpInt2 = radiusy * (2 * gp.serNum)
				+  (radiusy * (gp.pieHeight / 100.0));
		gp.graphRect = new Rectangle2D.Double(gp.graphRect.x
				+ (gp.graphRect.width - tmpInt1) / 2, gp.graphRect.y
				+ (gp.graphRect.height - tmpInt2) / 2, tmpInt1, tmpInt2);

		double orgx = gp.graphRect.x + gp.graphRect.width / 2;
		double orgy = gp.graphRect.y + dely + (gp.graphRect.height - dely) / 2;
		boolean cut = gp.serNum == 1 && egp.isCutPie();

		/* 开始循环画饼 */
		for (int j = 0; j < gp.serNum; j++) {
			String serName = (String) gp.serNames.get(j);
			double totAmount = 0.0;
			double totAngle = 0;
			/* 算出当前序列的半径 */
			double radx = (gp.serNum - j) * radiusx;
			double rady = (gp.serNum - j) * radiusy;
			w = 2 * radx;
			h = 2 * rady;
			// 超链接时需要得到下一系列的半径矩形，以确定环状链接坐标
			double rxx = (gp.serNum - j - 1) * radiusx;
			double ryy = (gp.serNum - j - 1) * radiusy;
			double xx, yy;
			double ww = 2 * rxx;
			double hh = 2 * ryy;

			double max = 0;
			double maxi = -1, maxX = 0, maxY = 0; // 最大块饼的坐标
			ExtGraphCategory maxEgc = null;
			ExtGraphSery maxEgs = null;


			double maxAngle = 0; // 最大块所处的饼图角度

			double movex = 0;
			double movey = 0;
			/* 先算出总和 */
			ArrayList cats = egp.categories;
			int cc = cats.size();
			for (int i = 0; i < cc; i++) {
				ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
				ExtGraphSery egs = egc.getExtGraphSery(serName);
				double amount = egs.getValue();
				if (amount > max) {
					max = amount;
					maxi = i;
				}
				totAmount += amount;
			}

			if (totAmount == 0.0) {
				continue;
			}

			/* 逐项画扇形,先画阴影 */
			totAngle = 0;
			double cumulativeAmount = 0.0;
			if (egp.isDrawShade()) {
				for (int i = 0; i < cc; i++) {
					ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
					ExtGraphSery egs = egc.getExtGraphSery(serName);

					double amount = 0.0;
					double angle = 0;
					amount = egs.getValue();
					angle = 360.0 * amount / totAmount;
					if (i == gp.catNum - 1) {
						angle = 360 - totAngle;
						/* 如果当前扇区被切出，则圆心作一位移，沿对角线 */
					}
					if (cut && maxi == i) {
						movex =  (20 * Math.cos(Math.toRadians(totAngle
								+ angle / 2)));
						movey =  (-20 * Math.sin(Math.toRadians(totAngle
								+ angle / 2)));
					} else {
						movex = 0;
						movey = 0;
					}
					x = orgx - radx + movex;
					y = orgy - rady - dely + movey;
					Arc2D.Double ddd;
					g.setColor(Color.lightGray);
					ddd = new Arc2D.Double(x + db.SHADE_SPAN, y + db.SHADE_SPAN, w,
							h, totAngle, angle, Arc2D.PIE);
					g.fill(ddd);

					totAngle += angle;
				}
			}

			/* 逐项画扇形 */
			totAngle = 0;
			cumulativeAmount = 0.0;
			for (int i = 0; i < cc; i++) {
				ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
				ExtGraphSery egs = egc.getExtGraphSery(serName);

				double amount = 0.0;
				double angle = 0;

				amount = egs.getValue();
				angle = 360.0 * amount / totAmount;
				if (i == gp.catNum - 1) {
					angle = 360 - totAngle;
					/* 如果当前扇区被切出，则圆心作一位移，沿对角线 */
				}
				if (cut && maxi == i) {
					movex =  (20 * Math.cos(Math.toRadians(totAngle
							+ angle / 2)));
					movey =  (-20 * Math.sin(Math.toRadians(totAngle
							+ angle / 2)));
				} else {
					movex = 0;
					movey = 0;
				}
				x = orgx - radx + movex;
				y = orgy - rady - dely + movey;

				Color bc = egp.getAxisColor(GraphProperty.AXIS_TOP);
				int bs = Consts.LINE_SOLID;
				float bw = 1.0f;
				Color c = db.getColor(i);
				ChartColor tmpc = db.getChartColor(c);
				int dazzelCount;
				if (j != (gp.serNum-1) || gp.serNum>1){
					dazzelCount = 2;
				}else{
					dazzelCount = 8;
				}
				Rectangle2D ellipseBounds = new Rectangle2D.Double(x,y,w,h);
				Utils.draw2DPie(g, ellipseBounds,totAngle, angle, 
						bc,bs,bw, db.getTransparent(),
						tmpc, dazzelCount);
				
				Arc2D.Double ddd = new Arc2D.Double(x, y, w, h, totAngle,
						angle, Arc2D.PIE);

				xx = orgx - rxx + movex;
				yy = orgy - ryy - dely + movey;
				Arc2D.Double ddd2 = null;
				if (ww != 0) {
					ddd2 = new Arc2D.Double(xx, yy, ww, hh, totAngle, angle,
							Arc2D.PIE);
				}
				db.htmlLink(ddd, htmlLink, egc.getNameString(), egs, ddd2);
				totAngle += angle;
			}

			// 输出文字
			totAngle = 0;
			for (int i = 0; i < cc; i++) {
				ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
				ExtGraphSery egs = egc.getExtGraphSery(serName);

				double amount = 0.0;
				double angle = 0;

				amount = egs.getValue();
				angle = 360.0 * amount / totAmount;
				if (i == gp.catNum - 1) {
					angle = 360 - totAngle;
					/* 如果当前扇区被切出，则圆心作一位移，沿对角线 */
				}
				if (cut && maxi == i) {
					movex =  (20 * Math.cos(Math.toRadians(totAngle
							+ angle / 2)));
					movey =  (-20 * Math.sin(Math.toRadians(totAngle
							+ angle / 2)));
				} else {
					movex = 0;
					movey = 0;
				}

				if (gp.serNum == 1) { // 画百分比标识
					double tAngle = Math.toRadians(totAngle + angle / 2);
					double x1 = orgx
							+  Math.round((radx) * Math.cos(tAngle))
							+ movex;
					double y1 = orgy
							-  Math.round((rady) * Math.sin(tAngle))
							+ movey;
					double shiftx = radx*(gp.pieLine/100f);
					double shifty = shiftx;
					if(shiftx<5){
						shiftx=5;
					}
					if(shifty<5){
						shifty=5;
					}
					double x2 = orgx
							+  Math.round((radx + shiftx) * Math.cos(tAngle))
							+ movex;
					double y2 = orgy
							-  Math.round((rady + shifty) * Math.sin(tAngle))
							+ movey;
					g.setColor(gp.coorColor);

					String fmt;
					String text = "";
					// 显示数值标示
					double tmpAngle = totAngle + angle / 2;
					db.g.setStroke(new BasicStroke());
					switch (gp.dispValueType) {
					case GraphProperty.DISPDATA_NONE: // 不显示
						text = "";
						break;
					case GraphProperty.DISPDATA_VALUE: // 显示数值
					case GraphProperty.DISPDATA_NAME_VALUE:
						if (StringUtils.isValidString(gp.dataMarkFormat)) {
							fmt = gp.dataMarkFormat;
						} else {
							fmt = "";
						}
						db.drawLine(x1, y1, x2, y2,
								egp.getAxisColor(GraphProperty.AXIS_PIEJOIN));
						text = db.getFormattedValue(amount, fmt);
						if(gp.dispValueType==GraphProperty.DISPDATA_NAME_VALUE){
							text = getDispName(egc,egs,gp.serNum)+","+text;
						}						
						break;
					case GraphProperty.DISPDATA_TITLE: // 标题
						db.drawLine(x1, y1, x2, y2,
								egp.getAxisColor(GraphProperty.AXIS_PIEJOIN));
						text = egs.getTips();
						break;
					default: // 显示百分比
						db.drawLine(x1, y1, x2, y2,
								egp.getAxisColor(GraphProperty.AXIS_PIEJOIN));
						if (i != maxi) {
							if (StringUtils.isValidString(gp.dataMarkFormat)) {
								fmt = gp.dataMarkFormat;
							} else {
								fmt = "0.00%";
							}
							text = db.getFormattedValue(amount / totAmount, fmt);
							String tmp = text.substring(0, text.length() - 1);
							if (tmp.equals(".")
									|| !StringUtils.isValidString(tmp)) {
								tmp = "0";
							}
							cumulativeAmount += Double.parseDouble(tmp);
							if(gp.dispValueType==GraphProperty.DISPDATA_NAME_PERCENTAGE){
								text = getDispName(egc,egs,gp.serNum)+","+text;
							}
						} else {
							maxAngle = tmpAngle;
							maxEgc = egc;
							maxEgs = egs;
							maxX = x2;
							maxY = y2;
						}
						break;
					}

					db.drawOutCircleText(gp.GFV_VALUE, text, tmpAngle, x2, y2);
				}
				totAngle += angle;
			}

			if (gp.serNum > 1) { // 画序列标识
				int angle = Math.round(360 / gp.serNum) * j;
				double tAngle = Math.toRadians(angle);
				g.setColor(gp.coorColor);
				double x1 = orgx
						+  Math.round((radx - radiusx / 2)
								* Math.cos(tAngle));
				double y1 = orgy
						-  Math.round((rady - radiusy / 2)
								* Math.sin(tAngle));
				double x2 = orgx
						+  Math.round((radiusx * gp.serNum + 5)
								* Math.cos(tAngle));
				double y2 = orgy
						-  Math.round((radiusy * gp.serNum + 5)
								* Math.sin(tAngle));
				db.drawLine(x1, y1, x2, y2,
						egp.getAxisColor(GraphProperty.AXIS_PIEJOIN));
				db.drawOutCircleText(gp.GFV_XLABEL, serName, angle, x2, y2);
			} else {
				if (gp.dispValueType == GraphProperty.DISPDATA_PERCENTAGE
						|| gp.dispValueType == GraphProperty.DISPDATA_NAME_PERCENTAGE) {
					String fmt;
					if (StringUtils.isValidString(gp.dataMarkFormat)) {
						fmt = gp.dataMarkFormat;
					} else {
						fmt = "0.00%";
					}
					String text = db.getFormattedValue(
							(100.0 - cumulativeAmount) / 100, fmt);
					if(gp.dispValueType==GraphProperty.DISPDATA_NAME_PERCENTAGE){
						text = getDispName(maxEgc,maxEgs,gp.serNum)+","+text;
					}
					db.drawOutCircleText(gp.GFV_VALUE, text, maxAngle, maxX, maxY);
				}
			}
			orgy -= dely;
		}
		db.outLabels();
	}
}
