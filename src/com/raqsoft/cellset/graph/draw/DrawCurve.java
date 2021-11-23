package com.raqsoft.cellset.graph.draw;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

import com.raqsoft.cellset.graph.*;
import com.raqsoft.chart.Consts;
import com.raqsoft.chart.Utils;

/**
 * 曲线图实现
 * 
 * @author Joancy
 *
 */

public class DrawCurve extends DrawBase {
	/**
	 * 实现绘图功能
	 */
	public void draw(StringBuffer htmlLink) {
		drawing(this, htmlLink);
	}

	/**
	 * 根据绘图基类db绘图，并将画图后的超链接存入htmlLink
	 * 
	 * @param db
	 *            抽象的绘图基类
	 * @param htmlLink
	 *            超链接缓存
	 */
	public static void drawing(DrawBase db, StringBuffer htmlLink) {
		GraphParam gp = db.gp;
		ExtGraphProperty egp = db.egp;
		Graphics2D g = db.g;
		ArrayList<ValueLabel> labelList = db.labelList;
		int VALUE_RADIUS = db.VALUE_RADIUS;
		ArrayList<ValuePoint> pointList = db.pointList;
		double seriesWidth;
		double coorWidth;
		double categorySpan;
		double dely;
		double tmpInt;
		double x, y;

		gp.coorWidth = 0;

		Point2D.Double beginPoint[];
		double beginVal[];
		ArrayList catPoints[];

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
		beginVal = new double[gp.serNum];
		catPoints = new ArrayList[gp.serNum];
		for (int j = 0; j < gp.serNum; j++) {
			ArrayList catList = new ArrayList();
			catPoints[j] = catList;
		}

		ArrayList cats = egp.categories;
		int cc = cats.size();
		Color c;
		for (int i = 0; i < cc; i++) {
			ExtGraphCategory egc = (ExtGraphCategory) cats.get(i);
			double posx = DrawLine.getPosX(gp, i, cc, categorySpan, seriesWidth);

			boolean valvis = (i % (gp.graphXInterval + 1) == 0);// 柱顶是否显示值跟画Table分开
			boolean vis = valvis && !gp.isDrawTable;
			if (vis) {
				c = egp.getAxisColor(GraphProperty.AXIS_BOTTOM);
				Utils.setStroke(g, c, Consts.LINE_SOLID, 1.0f);
				db.drawLine(posx, gp.gRect1.y + gp.gRect1.height, posx,
						gp.gRect1.y + gp.gRect1.height + gp.tickLen, c);
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
					String sval = db.getDispValue(egc, egs, gp.serNum); // getFormattedValue(val);
					x = endPoint.x;
					y = endPoint.y;
					if (!gp.isMultiSeries) {
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
					double xx, yy, ww, hh;
					xx = endPoint.x - VALUE_RADIUS;
					yy = endPoint.y - VALUE_RADIUS;
					ww = 2 * VALUE_RADIUS;
					hh = ww;
					Color backColor;
					if (!gp.isMultiSeries) {
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
					DrawLine.drawVTrendLine(db, beginPoint[j], endPoint, val
							- beginVal[j]);
				}
				DrawLine.drawHTrendLine(db, beginPoint[j]);
				beginPoint[j] = endPoint;
				if (endPoint != null) {
					ArrayList catList = catPoints[j];
					catList.add(endPoint);
				}
				beginVal[j] = val;
			}
		}

		Stroke stroke = db.getLineStroke();
		if (stroke != null) {
			g.setStroke(stroke);
		}

		for (int j = 0; j < gp.serNum; j++) {
			ArrayList serPoints = (ArrayList) catPoints[j];
			if (serPoints.size() == 0) {
				continue;
			}

			g.setColor(db.getColor(j));

			Point p1;
			Point p2;
			int n;
			double x1, y1, x2, y2;
			double[] xs;
			double[] ys;
			byte curveType = egp.getCurveType();
			Line2D.Double dLine;
			switch (curveType) {
			case GraphProperty.CURVE_LAGRANGE:
				p1 = (Point) serPoints.get(0);
				p2 = (Point) serPoints.get(serPoints.size() - 1);
				x1 = p1.x;
				y1 = p1.y;
				double delta = 0.2;
				for (x2 = p1.x + delta; x2 <= p2.x; x2 += delta) {
					y2 = Lagrange(serPoints, x2);
					dLine = new Line2D.Double(x1, y1, x2, y2);
					g.draw(dLine);
					x1 = x2;
					y1 = y2;
				}
				break;
			case GraphProperty.CURVE_AKIMA:
				n = serPoints.size();
				xs = new double[n];
				ys = new double[n];
				for (int i = 0; i < n; i++) {
					Point pt = (Point) serPoints.get(i);
					xs[i] = pt.x;
					ys[i] = pt.y;
				}
				for (int k = 0; k < serPoints.size() - 1; k++) {
					p1 = (Point) serPoints.get(k);
					x1 = p1.x;
					y1 = p1.y;
					p2 = (Point) serPoints.get(k + 1);
					double[] s = s(xs, ys, k); // , t
					for (x2 = p1.x + 1; x2 <= p2.x; x2 += 1) {
						y2 = akima(p1.x, x2, s);
						dLine = new Line2D.Double(x1, y1, x2, y2);
						g.draw(dLine);
						x1 = x2;
						y1 = y2;
					}
				}
				break;
			case GraphProperty.CURVE_3SAMPLE:
				n = serPoints.size();
				xs = new double[n];
				ys = new double[n];
				for (int i = 0; i < n; i++) {
					Point pt = (Point) serPoints.get(i);
					xs[i] = pt.x;
					ys[i] = pt.y;
				}

				p1 = (Point) serPoints.get(0);
				p2 = (Point) serPoints.get(serPoints.size() - 1);
				x1 = p1.x;
				y1 = p1.y;
				for (x2 = p1.x + 1; x2 <= p2.x; x2 += 1) {
					y2 =  sample(xs, ys, x2);
					dLine = new Line2D.Double(x1, y1, x2, y2);
					g.draw(dLine);
					x1 = x2;
					y1 = y2;
				}
				break;
			}
		}

		db.outPoints();
		db.outLabels();
		/* 重画一下基线 */
		g.setStroke(new BasicStroke());
		db.drawLine(gp.gRect1.x, gp.valueBaseLine, gp.gRect1.x
				+ gp.gRect1.width, gp.valueBaseLine,
				egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
		db.drawLine(gp.gRect1.x + gp.gRect1.width, gp.valueBaseLine,
				 (gp.gRect1.x + gp.gRect1.width + coorWidth),
				 (gp.valueBaseLine - coorWidth),
				egp.getAxisColor(GraphProperty.AXIS_BOTTOM));
	}

	/**
	 * 拉格朗日插值算法
	 * 
	 * @param points
	 *            ArrayList，所有点
	 * @param deltaX
	 *            int， 插值的X
	 * @return int，插值朗日后的Y
	 */
	private static double Lagrange(ArrayList points, double deltaX) {
		double sum = 0;
		double L;
		for (int i = 0; i < points.size(); i++) {
			L = 1;
			Point pi = (Point) points.get(i);
			for (int j = 0; j < points.size(); j++) {
				Point pj = (Point) points.get(j);
				if (j != i) {
					L = L * (deltaX - pj.x) / (pi.x - pj.x);
				}
			}
			sum = sum + L * pi.y;
		}
		return sum;
	}

	private static double akima(double xk, double t, double[] s) {
		double d = t - xk;
		double dd = d * d;
		return s[0] + s[1] * d + s[2] * dd + s[3] * d * dd;
	}

	private static double[] s(double[] x, double[] y, int k) {
		double t = 0;
		int kk, m, l;
		double[] u = new double[5];
		double[] s = new double[5];
		double p, q;
		s[4] = 0.0;
		s[0] = 0.0;
		s[1] = 0.0;
		s[2] = 0.0;
		s[3] = 0.0;
		int n = x.length;
		if (n < 1) {
			return s;
		}
		if (n == 1) {
			s[0] = y[0];
			s[4] = y[0];
			return s;
		}
		if (n == 2) {
			s[0] = y[0];
			s[1] = (y[1] - y[0]) / (x[1] - x[0]);
			if (k < 0) {
				s[4] = (y[0] * (t - x[1]) - y[1] * (t - x[0])) / (x[0] - x[1]);
			}
			return s;
		}
		if (k < 0) {
			if (t <= x[1]) {
				kk = 0;
			} else if (t >= x[n - 1]) {
				kk = n - 2;
			} else {
				kk = 1;
				m = n;
				while (((kk - m) != 1) && ((kk - m) != -1)) {
					l = (kk + m) / 2;
					if (t < x[l - 1]) {
						m = l;
					} else {
						kk = l;
					}
				}
				kk = kk - 1;
			}
		} else {
			kk = k;
		}
		if (kk >= n - 1) {
			kk = n - 2;
		}
		u[2] = (y[kk + 1] - y[kk]) / (x[kk + 1] - x[kk]);
		if (n == 3) {
			if (kk == 0) {
				u[3] = (y[2] - y[1]) / (x[2] - x[1]);
				u[4] = 2.0 * u[3] - u[2];
				u[1] = 2.0 * u[2] - u[3];
				u[0] = 2.0 * u[1] - u[2];
			} else {
				u[1] = (y[1] - y[0]) / (x[1] - x[0]);
				u[0] = 2.0 * u[1] - u[2];
				u[3] = 2.0 * u[2] - u[1];
				u[4] = 2.0 * u[3] - u[2];
			}
		} else {
			if (kk <= 1) {
				u[3] = (y[kk + 2] - y[kk + 1]) / (x[kk + 2] - x[kk + 1]);
				if (kk == 1) {
					u[1] = (y[1] - y[0]) / (x[1] - x[0]);
					u[0] = 2.0 * u[1] - u[2];
					if (n == 4) {
						u[4] = 2.0 * u[3] - u[2];
					} else {
						u[4] = (y[4] - y[3]) / (x[4] - x[3]);
					}
				} else {
					u[1] = 2.0 * u[2] - u[3];
					u[0] = 2.0 * u[1] - u[2];
					u[4] = (y[3] - y[2]) / (x[3] - x[2]);
				}
			} else if (kk >= (n - 3)) {
				u[1] = (y[kk] - y[kk - 1]) / (x[kk] - x[kk - 1]);
				if (kk == (n - 3)) {
					u[3] = (y[n - 1] - y[n - 2]) / (x[n - 1] - x[n - 2]);
					u[4] = 2.0 * u[3] - u[2];
					if (n == 4) {
						u[0] = 2.0 * u[1] - u[2];
					} else {
						u[0] = (y[kk - 1] - y[kk - 2])
								/ (x[kk - 1] - x[kk - 2]);
					}
				} else {
					u[3] = 2.0 * u[2] - u[1];
					u[4] = 2.0 * u[3] - u[2];
					u[0] = (y[kk - 1] - y[kk - 2]) / (x[kk - 1] - x[kk - 2]);
				}
			} else {
				u[1] = (y[kk] - y[kk - 1]) / (x[kk] - x[kk - 1]);
				u[0] = (y[kk - 1] - y[kk - 2]) / (x[kk - 1] - x[kk - 2]);
				u[3] = (y[kk + 2] - y[kk + 1]) / (x[kk + 2] - x[kk + 1]);
				u[4] = (y[kk + 3] - y[kk + 2]) / (x[kk + 3] - x[kk + 2]);
			}
		}
		s[0] = fabs(u[3] - u[2]);
		s[1] = fabs(u[0] - u[1]);
		if ((s[0] + 1.0 == 1.0) && (s[1] + 1.0 == 1.0)) {
			p = (u[1] + u[2]) / 2.0;
		} else {
			p = (s[0] * u[1] + s[1] * u[2]) / (s[0] + s[1]);
		}
		s[0] = fabs(u[3] - u[4]);
		s[1] = fabs(u[2] - u[1]);
		if ((s[0] + 1.0 == 1.0) && (s[1] + 1.0 == 1.0)) {
			q = (u[2] + u[3]) / 2.0;
		} else {
			q = (s[0] * u[2] + s[1] * u[3]) / (s[0] + s[1]);
		}
		s[0] = y[kk];
		s[1] = p;
		s[3] = x[kk + 1] - x[kk];
		s[2] = (3.0 * u[2] - 2.0 * p - q) / s[3];
		s[3] = (q + p - 2.0 * u[2]) / (s[3] * s[3]);
		if (k < 0) {
			p = t - x[kk];
			s[4] = s[0] + s[1] * p + s[2] * p * p + s[3] * p * p * p;
		}
		return s;
	}

	private static double fabs(double d) {
		return Math.abs(d);
	}

	// 三次样条函数插值
	private static double sample(double[] x, double[] y, double tx) {
		int n = x.length, m;
		double dy[], ddy[], t[], z[], dz[], ddz[];
		dy = new double[n];
		dy[0] = 1;
		dy[n - 1] = -1;
		ddy = new double[n];

		t = new double[] { tx };
		m = 1;
		z = new double[1];
		dz = new double[1];
		ddz = new double[1];
		int i, j;
		double h0, h1, alpha, beta, g;
		double[] s = new double[n];
		s[0] = dy[0];
		dy[0] = 0.0;
		h0 = x[1] - x[0];
		for (j = 1; j <= n - 2; j++) {
			h1 = x[j + 1] - x[j];
			alpha = h0 / (h0 + h1);
			beta = (1.0 - alpha) * (y[j] - y[j - 1]) / h0;
			beta = 3.0 * (beta + alpha * (y[j + 1] - y[j]) / h1);
			dy[j] = -alpha / (2.0 + (1.0 - alpha) * dy[j - 1]);
			s[j] = (beta - (1.0 - alpha) * s[j - 1]);
			s[j] = s[j] / (2.0 + (1.0 - alpha) * dy[j - 1]);
			h0 = h1;
		}
		for (j = n - 2; j >= 0; j--) {
			dy[j] = dy[j] * dy[j + 1] + s[j];
		}
		for (j = 0; j <= n - 2; j++) {
			s[j] = x[j + 1] - x[j];
		}
		for (j = 0; j <= n - 2; j++) {
			h1 = s[j] * s[j];
			ddy[j] = 6.0 * (y[j + 1] - y[j]) / h1 - 2.0
					* (2.0 * dy[j] + dy[j + 1]) / s[j];
		}
		h1 = s[n - 2] * s[n - 2];
		ddy[n - 1] = 6. * (y[n - 2] - y[n - 1]) / h1 + 2.
				* (2. * dy[n - 1] + dy[n - 2]) / s[n - 2];
		g = 0.0;
		for (i = 0; i <= n - 2; i++) {
			h1 = 0.5 * s[i] * (y[i] + y[i + 1]);
			h1 = h1 - s[i] * s[i] * s[i] * (ddy[i] + ddy[i + 1]) / 24.0;
			g = g + h1;
		}
		for (j = 0; j <= m - 1; j++) {
			if (t[j] >= x[n - 1]) {
				i = n - 2;
			} else {
				i = 0;
				while (t[j] > x[i + 1]) {
					i = i + 1;
				}
			}
			h1 = (x[i + 1] - t[j]) / s[i];
			h0 = h1 * h1;
			z[j] = (3.0 * h0 - 2.0 * h0 * h1) * y[i];
			z[j] = z[j] + s[i] * (h0 - h0 * h1) * dy[i];
			dz[j] = 6.0 * (h0 - h1) * y[i] / s[i];
			dz[j] = dz[j] + (3.0 * h0 - 2.0 * h1) * dy[i];
			ddz[j] = (6.0 - 12.0 * h1) * y[i] / (s[i] * s[i]);
			ddz[j] = ddz[j] + (2.0 - 6.0 * h1) * dy[i] / s[i];
			h1 = (t[j] - x[i]) / s[i];
			h0 = h1 * h1;
			z[j] = z[j] + (3.0 * h0 - 2.0 * h0 * h1) * y[i + 1];
			z[j] = z[j] - s[i] * (h0 - h0 * h1) * dy[i + 1];
			dz[j] = dz[j] - 6.0 * (h0 - h1) * y[i + 1] / s[i];
			dz[j] = dz[j] + (3.0 * h0 - 2.0 * h1) * dy[i + 1];
			ddz[j] = ddz[j] + (6.0 - 12.0 * h1) * y[i + 1] / (s[i] * s[i]);
			ddz[j] = ddz[j] - (2.0 - 6.0 * h1) * dy[i + 1] / s[i];
		}
		return z[0];
	}

}
