package com.scudata.chart;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Writer;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.encoder.SymbolShapeHint;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.*;
import java.io.InputStream;
import java.lang.reflect.*;
import java.text.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.ImageIcon;

import com.scudata.cellset.graph.draw.Desc3DRect;
import com.scudata.chart.element.*;
import com.scudata.chart.resources.ChartMessage;
import com.scudata.common.*;
import com.scudata.dm.*;
import com.scudata.expression.*;
import com.scudata.util.*;

/**
 * 画图的一些基础公用方法
 * 凡是方法后面带上1.2.3步骤的，均为同一图元需要分三步绘制的分解动作
 * 且每一个动作都需要带上独立的风格，颜色
 * 步骤1为图像的阴影，在drawBack中调用；
 * 步骤2为图像的填充色，在draw中调用；
 * 步骤3为图像的边框，在drawFore中调用。 
 * 不带数字的方法均为简单函数，不需要分步的图像，且风格在上层程序已经设置
 * 
 */

public class Utils {
	public static int SHADE_SPAN = 3;
	private static ArrayList globalFonts = new ArrayList();
	private static int[] shadowColors = { 203, 208, 198, 201, 205, 196, 181,
			186, 178, 155, 158, 151, 135, 138, 131 };
	static MessageManager mm = ChartMessage.get();

	private static Shape getLine1ShapeArea(double shift, Point2D b, Point2D e,
			float weight) {
		double x1 = b.getX() + shift;// SHADE_SPAN;
		double y1 = b.getY() + shift;// SHADE_SPAN;
		double x2 = e.getX() + shift;// SHADE_SPAN;
		double y2 = e.getY() + shift;// SHADE_SPAN;
		// 计算草图参考手写记事本 第 直线阴影计算图形 页
		// Point2D P1 = new Point2D.Double(x1, y1);
		// Point2D P2 = new Point2D.Double(x2, y2);
		double P2e = Math.abs(y2 - y1);
		double P1e = Math.abs(x2 - x1);
		double P1P2 = Math.sqrt(P2e * P2e + P1e * P1e);
		double AP1 = weight / 2f;
		double sina2 = P1e / P1P2;
		double Af = AP1 * sina2;
		double cosa2 = P2e / P1P2;
		double fP1 = AP1 * cosa2;
		Point2D A = new Point2D.Double(x1 - fP1, y1 - Af);
		Point2D D = new Point2D.Double(x1 + fP1, y1 + Af);
		Point2D B = new Point2D.Double(x2 - fP1, y2 - Af);
		Point2D C = new Point2D.Double(x2 + fP1, y2 + Af);
		Shape polygon = newPolygon2DShape(
				new double[] { A.getX(), B.getX(), C.getX(), D.getX() },
				new double[] { A.getY(), B.getY(), C.getY(), D.getY() });
		return polygon;
	}

	/**
	 * 画直线的阴影
	 * @param g 图形设备
	 * @param b 线段起点
	 * @param e 线段终点
	 * @param style 线的风格
	 * @param weight 线的粗度
	 * @return 线段的超链接形状
	 */
	public static Shape drawLine1(Graphics2D g, Point2D b, Point2D e,
			int style, float weight) {
		if (b == null || e == null) {
			return null;
		}
		double shift = weight / 2 + 1;
		if (shift < 2)
			shift = 2;
		Shape s = getLine1ShapeArea(shift, b, e, weight);

		double x1 = b.getX() + shift;// SHADE_SPAN;
		double y1 = b.getY() + shift;// SHADE_SPAN;
		double x2 = e.getX() + shift;// SHADE_SPAN;
		double y2 = e.getY() + shift;// SHADE_SPAN;
		double P2e = Math.abs(y2 - y1);
		double P1e = Math.abs(x2 - x1);
		double P1P2 = Math.sqrt(P2e * P2e + P1e * P1e);
		double AP1 = weight / 2f;
		double sina2 = P1e / P1P2;
		double Af = AP1 * sina2;
		double cosa2 = P2e / P1P2;
		double fP1 = AP1 * cosa2;
		Point2D A = new Point2D.Double(x1 - fP1, y1 - Af);
		Point2D D = new Point2D.Double(x1 + fP1, y1 + Af);

		Color bright = new Color(243, 248, 239);
		Color dark = getShadeColor(1);
		GradientPaint gp = new GradientPaint(A, dark, D, bright);
		g.setPaint(gp);
		setTransparent(g, 0.8f);
		g.fill(s);
		setTransparent(g, 1);

		return getLine1ShapeArea(0, b, e, weight);// 返回没有位移的多边形直线状区域
	}

	/**
	 * 绘制一条直线
	 * @param g 图形设备
	 * @param b 线的起点
	 * @param e 线的终点
	 * @param color 颜色
	 * @param style 线型
	 * @param weight 粗度
	 */
	public static void drawLine2(Graphics2D g, Point2D b, Point2D e,
			Color color, int style, float weight) {
		if (b == null || e == null) {
			return;
		}
		if (setStroke(g, color, style, weight)) {
			Utils.drawLine(g, b, e);
		}
	}

	public static String xToChinese(double dd) {
		try {
			String s = "零壹贰叁肆伍陆柒捌玖";
			// String s1 = "拾佰仟万拾佰仟亿拾佰仟万";
			String s1 = "十百千万十百千亿十百千万";
			String m;
			int j;
			StringBuffer k = new StringBuffer();
			m = String.valueOf(Math.round(dd));
			for (j = m.length(); j >= 1; j--) {
				char n = s.charAt(Integer.parseInt(m.substring(m.length() - j,
						m.length() - j + 1)));
				if (n == '零' && k.charAt(k.length() - 1) == '零') {
					continue;
				}
				k.append(n);
				if (n == '零') {
					continue;
				}
				int u = j - 2;
				if (u >= 0) {
					k.append(s1.charAt(u));
				}
				if (u > 3 && u < 7) {
					k.append('万');
				}
				if (u > 7) {
					k.append('亿');
				}
			}
			if (k.length() > 0 && k.charAt(k.length() - 1) == '零') {
				k.deleteCharAt(k.length() - 1);
			}
			if (k.length() > 0 && k.charAt(0) == '壹') {
				k.deleteCharAt(0);
			}
			return k.toString();
		} catch (Exception x) {
			NumberFormat df = new DecimalFormat("###,#.#");
			return df.format(dd);
		}
	}

	/**
	 * 对列表里面的数据排序
	 * @param list 列表数据，要求值为可比较的
	 * @param ascend 是否升序
	 * @return 排序完成返回true，否则返回false
	 */
	public static boolean sort(AbstractList list, boolean ascend) {
		Comparable ci, cj;
		int i, j;
		boolean lb_exchange;
		for (i = 0; i < list.size(); i++) {
			Object o = list.get(i);
			if (o != null && !(o instanceof Comparable)) {
				return false;
			}
		}

		for (i = 0; i < list.size() - 1; i++) {
			for (j = i + 1; j < list.size(); j++) {
				ci = (Comparable) list.get(i);
				cj = (Comparable) list.get(j);
				if (ascend) {
					if (ci == null || cj == null) {
						lb_exchange = (cj == null);
					} else {
						lb_exchange = ci.compareTo(cj) > 0;
					}
				} else {
					if (ci == null || cj == null) {
						lb_exchange = (ci == null);
					} else {
						lb_exchange = ci.compareTo(cj) < 0;
					}
				}
				if (lb_exchange) {
					Object o, o2;
					o = list.get(i);
					o2 = list.get(j);
					list.set(i, o2);
					list.set(j, o);
				}
			}
		}
		return true;
	}

	/**
	 * 从中心点 cx,cy按c1到c2的颜色环状发散梯度填充形状s
	 * 
	 * @param s
	 *            Shape
	 * @param c1
	 *            Color
	 * @param c2
	 *            Color
	 */
	public static synchronized void fillRadioGradientShape(Graphics2D g,
			Shape s, Color c1, Color c2, float transparent) {
		// 简单灰色填充好了
		setTransparent(g, transparent);
		g.setColor(Utils.getShadeColor(1));
		g.fill(s);
		setTransparent(g, 1);
	}

	/**
	 * 该算法使用矩形的对角线为填充梯度的长度；角度angle为对角线从底边逆时针的旋转角度；
	 * 
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param color1
	 * @param color2
	 * @param angle
	 * @return
	 */
	private static synchronized Paint getGradientPaint(double x, double y, double width,
			double height, Color color1, Color color2, int angle) {
		double x1 = 0, y1 = 0, x2 = 0, y2 = 0, h = 0;
		h = height;
		double antiAngleLen = Math.sqrt(width * width + h * h);// 对角线长度
		double rad = angle * Math.PI / 180;// 角度的弧度数
		if (angle >= 0 && angle <= 90) {
			if (angle == 0) {
				x1 = x;
				y1 = y + h / 2;
				x2 = x + width;
				y2 = y1;
			} else if (angle == 90) {
				x1 = x + width / 2;
				y1 = y + h;
				x2 = x1;
				y2 = y;
			} else {
				x1 = x;
				y1 = y + h;
				x2 = x1 + antiAngleLen * Math.cos(rad);
				y2 = y1 - antiAngleLen * Math.sin(rad);
			}
		} else if (angle > 90 && angle <= 180) {
			if (angle == 180) {
				x1 = x + width;
				y1 = y + h / 2;
				x2 = x;
				y2 = y1;
			} else {
				x1 = x + width;
				y1 = y + h;
				x2 = x1 + antiAngleLen * Math.cos(rad);
				y2 = y1 - antiAngleLen * Math.sin(rad);
			}
		} else if (angle > 180 && angle <= 270) {
			if (angle == 270) {
				x1 = x + width / 2;
				y1 = y;
				x2 = x1;
				y2 = y + h;
			} else {
				x1 = x + width;
				y1 = y;
				x2 = x1 + antiAngleLen * Math.cos(rad);
				y2 = y1 - antiAngleLen * Math.sin(rad);
			}
		} else if (angle > 270 && angle <= 360) {
			if (angle == 360) {
				x1 = x;
				y1 = y + h / 2;
				x2 = x + width;
				y2 = y1;
			} else {
				x1 = x;
				y1 = y;
				x2 = x1 + antiAngleLen * Math.cos(rad);
				y2 = y1 - antiAngleLen * Math.sin(rad);
			}
		}

		return new GradientPaint((int)x1, (int)y1, color1, (int)x2, (int)y2, color2, false);
	}

	/**
	 * 设置绘图的填充风格
	 * @param g 图形设备
	 * @param x 填充的x坐标
	 * @param y 填充的y坐标
	 * @param width 填充的宽度
	 * @param height 填充的高度
	 * @param cc 定义了填充风格的 填充颜色类
	 * @return 填充设置完成返回true，否则返回false
	 */
	public static boolean setPaint(Graphics2D g, double x, double y, double width,
			double height, ChartColor cc) {
		Rectangle2D.Double rect = null;
		BufferedImage tempbi = null;
		Graphics2D tempG = null;
		Paint paint = null;
		int pattern = cc.getType();
		Color c1 = cc.getColor1();
		if (c1 == null)
			return false;
		Color c2 = cc.getColor2();
		if (c2 == null)
			return false;

		switch (pattern) {
		case Consts.PATTERN_DEFAULT: // 填充图案，全填充
			if (cc.isGradient()) {
				paint = getGradientPaint(x, y, width, height, c1, c2,
						cc.getAngle());
			} else {
				g.setColor(c1);
				return true;
			}
			break;
		case Consts.PATTERN_H_THIN_LINE: // 填充图案，水平细线
			rect = new Rectangle2D.Double(x + 1, y + 1, 6, 6);
			tempbi = new BufferedImage(6, 6, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 6, 6);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(0.1f));
			tempG.drawLine(0, 1, 6, 1);
			tempG.drawLine(0, 3, 6, 3);
			tempG.drawLine(0, 5, 6, 5);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_H_THICK_LINE: // 填充图案，水平粗线
			rect = new Rectangle2D.Double(x + 1, y + 1, 6, 6);
			tempbi = new BufferedImage(6, 6, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 6, 6);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(1.5f));
			tempG.drawLine(0, 2, 6, 2);
			tempG.drawLine(0, 5, 6, 5);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_V_THIN_LINE: // 填充图案，垂直细线
			rect = new Rectangle2D.Double(x + 1, y + 1, 6, 6);
			tempbi = new BufferedImage(6, 6, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 6, 6);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(0.1f));
			tempG.drawLine(1, 0, 1, 6);
			tempG.drawLine(3, 0, 3, 6);
			tempG.drawLine(5, 0, 5, 6);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_V_THICK_LINE: // 填充图案，垂直粗线
			rect = new Rectangle2D.Double(x + 1, y + 1, 6, 6);
			tempbi = new BufferedImage(6, 6, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 6, 6);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(1.5f));
			tempG.drawLine(2, 0, 2, 6);
			tempG.drawLine(5, 0, 5, 6);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_THIN_SLASH: // 填充图案，细斜线
			rect = new Rectangle2D.Double(x + 1, y + 1, 3, 3);
			tempbi = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 3, 3);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(0.1f));
			tempG.drawLine(0, 0, 3, 3);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_THICK_SLASH: // 填充图案，粗斜线
			rect = new Rectangle2D.Double(x + 1, y + 1, 4, 4);
			tempbi = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			setGraphAntiAliasingOn(tempG);
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 4, 4);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, null, 0.0f));
			tempG.drawLine(0, 0, 4, 4);
			tempG.drawLine(3, -1, 5, 1);
			tempG.drawLine(-1, 3, 1, 5);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_THIN_BACKSLASH: // 填充图案，细反斜线
			rect = new Rectangle2D.Double(x + 1, y + 1, 3, 3);
			tempbi = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 3, 3);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(0.1f));
			tempG.drawLine(2, 0, -1, 3);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_THICK_BACKSLASH: // 填充图案，粗反斜线
			rect = new Rectangle2D.Double(x + 1, y + 1, 4, 4);
			tempbi = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			setGraphAntiAliasingOn(tempG);
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 4, 4);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, null, 0.0f));
			tempG.drawLine(4, 0, 0, 4);
			tempG.drawLine(-1, 1, 1, -1);
			tempG.drawLine(3, 5, 5, 3);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_THIN_GRID: // 填充图案，细网格
			rect = new Rectangle2D.Double(x + 1, y + 1, 3, 3);
			tempbi = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 3, 3);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(0.1f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_MITER, 10.0f, null, 0.0f));
			tempG.drawLine(1, 0, 1, 3);
			tempG.drawLine(0, 1, 3, 1);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_THICK_GRID: // 填充图案，粗网格
			rect = new Rectangle2D.Double(x + 1, y + 1, 5, 5);
			tempbi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 5, 5);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_MITER, 10.0f, null, 0.0f));
			tempG.drawLine(3, 0, 3, 5);
			tempG.drawLine(0, 3, 5, 3);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_THIN_BEVEL_GRID: // 填充图案，细斜网格
			rect = new Rectangle2D.Double(x + 1, y + 1, 5, 5);
			tempbi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 5, 5);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_MITER, 10.0f, null, 0.0f));
			tempG.drawLine(0, 0, 5, 5);
			tempG.drawLine(0, 5, 5, 0);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_THICK_BEVEL_GRID: // 填充图案，粗斜网格
			rect = new Rectangle2D.Double(x + 1, y + 1, 6, 6);
			tempbi = new BufferedImage(6, 6, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			setGraphAntiAliasingOn(tempG);
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 6, 6);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, null, 0.0f));
			tempG.drawLine(0, 0, 6, 6);
			tempG.drawLine(0, 6, 6, 0);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_DOT_1: // 填充图案，稀疏点
			rect = new Rectangle2D.Double(x, y, 12, 12);
			tempbi = new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			setGraphAntiAliasingOn(tempG);
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 12, 12);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, null, 0.0f));
			tempG.drawLine(2, 3, 2, 3);
			tempG.drawLine(8, 9, 8, 9);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_DOT_2: // 填充图案，较稀点
			rect = new Rectangle2D.Double(x, y, 12, 12);
			tempbi = new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 12, 12);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, null, 0.0f));
			tempG.drawLine(2, 3, 2, 3);
			tempG.drawLine(6, 11, 6, 11);
			tempG.drawLine(10, 7, 10, 7);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_DOT_3: // 填充图案，较密点
			rect = new Rectangle2D.Double(x, y, 9, 9);
			tempbi = new BufferedImage(9, 9, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			setGraphAntiAliasingOn(tempG);
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 9, 9);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, null, 0.0f));
			tempG.drawLine(2, 2, 2, 2);
			tempG.drawLine(5, 8, 5, 8);
			tempG.drawLine(8, 5, 8, 5);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_DOT_4: // 填充图案，稠密点
			rect = new Rectangle2D.Double(x, y, 4, 4);
			tempbi = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 4, 4);
			tempG.setColor(c2);
			tempG.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, null, 0.0f));
			tempG.drawLine(1, 3, 1, 3);
			tempG.drawLine(3, 1, 3, 1);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_SQUARE_FLOOR: // 填充图案，正方块地板砖
			rect = new Rectangle2D.Double(0, 0, 8, 8);
			tempbi = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 8, 8);
			tempG.setColor(c2);
			tempG.fillRect(0, 0, 4, 4);
			tempG.fillRect(4, 4, 4, 4);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_DIAMOND_FLOOR: // 填充图案，菱形地板砖
			rect = new Rectangle2D.Double(x + 1, y + 1, 8, 8);
			tempbi = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 8, 8);
			tempG.setColor(c2);
			int[] xs = { 4, 0, 4, 8 };
			int[] ys = { 0, 4, 8, 4 };
			tempG.fillPolygon(xs, ys, 4);
			paint = new TexturePaint(tempbi, rect);
			break;
		case Consts.PATTERN_BRICK_WALL: // 填充图案，砖墙
			rect = new Rectangle2D.Double(x + 1, y + 1, 12, 12);
			tempbi = new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB);
			tempG = (Graphics2D) tempbi.getGraphics();
			tempG.setColor(c1);
			tempG.fillRect(0, 0, 12, 12);
			tempG.setStroke(new BasicStroke(0.1f));
			tempG.setColor(c2);
			tempG.drawLine(0, 0, 12, 0);
			tempG.drawLine(0, 3, 12, 3);
			tempG.drawLine(0, 6, 12, 6);
			tempG.drawLine(0, 9, 12, 9);
			tempG.drawLine(2, 0, 2, 3);
			tempG.drawLine(8, 3, 8, 6);
			tempG.drawLine(2, 6, 2, 9);
			tempG.drawLine(8, 9, 8, 12);
			paint = new TexturePaint(tempbi, rect);
			break;
		}
		g.setPaint(paint);
		if(tempG!=null){
			tempG.dispose();
		}
		return true;
	}

	private static ArrayList solidStrokes = null;
	private static ArrayList dashedStrokes = null;
	private static ArrayList dottedStrokes = null;
	private static ArrayList dotdashStrokes = null;

	private static ArrayList doubleStrokes = null;

	/**
	 * 设置绘制直线的风格
	 * @param g
	 *            Graphics2D
	 * @param c
	 *            Color,为null不设置，表示用当前的颜色 备注：null到底是表示当前画笔颜色还是透明色？
	 * @param style 风格
	 * @param weight 粗度
	 * @return boolean 设置成功返回true，否则返回false
	 */
	public static boolean setStroke(Graphics2D g, Color c, int style,
			float weight) {
		float w2 = weight;
		if (w2 == 0) {
			return false;
		}
		if (w2 < 1) {
			w2 = 1;
		}
		if (c != null) {
			g.setColor(c);
		}
		style = style & 0x0f;
		ListIterator li = null;
		BasicStroke stroke;
		switch (style) {
		case Consts.LINE_SOLID:
			if (solidStrokes == null) {
				solidStrokes = new ArrayList();
			} else {
				li = solidStrokes.listIterator();
				while (li.hasNext()) {
					BasicStroke bs = (BasicStroke) li.next();
					if (bs.getLineWidth() == weight) {
						g.setStroke(bs);
						return true;
					}
				}
			}
			stroke = new BasicStroke(weight);
			g.setStroke(stroke);
			solidStrokes.add(stroke);
			break;
		case Consts.LINE_DASHED:
			if (dashedStrokes == null) {
				dashedStrokes = new ArrayList();
			} else {
				li = dashedStrokes.listIterator();
				while (li.hasNext()) {
					BasicStroke bs = (BasicStroke) li.next();
					if (bs.getLineWidth() == weight) {
						g.setStroke(bs);
						return true;
					}
				}
			}
			float[] dashes1 = { 6 * w2, 6 * w2 };
			stroke = new BasicStroke(weight, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, dashes1, 0.0f);
			g.setStroke(stroke);
			dashedStrokes.add(stroke);
			break;
		case Consts.LINE_DOTTED:
			if (dottedStrokes == null) {
				dottedStrokes = new ArrayList();
			} else {
				li = dottedStrokes.listIterator();
				while (li.hasNext()) {
					BasicStroke bs = (BasicStroke) li.next();
					if (bs.getLineWidth() == weight) {
						g.setStroke(bs);
						return true;
					}
				}
			}
			float[] dashes2 = { w2, 3 * w2 };
			stroke = new BasicStroke(weight, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, dashes2, 0.0f);
			g.setStroke(stroke);
			dottedStrokes.add(stroke);
			break;
		case Consts.LINE_DOTDASH:
			if (dotdashStrokes == null) {
				dotdashStrokes = new ArrayList();
			} else {
				li = dotdashStrokes.listIterator();
				while (li.hasNext()) {
					BasicStroke bs = (BasicStroke) li.next();
					if (bs.getLineWidth() == weight) {
						g.setStroke(bs);
						return true;
					}
				}
			}
			float[] lp1 = { 6 * w2, 2 * w2, w2, 2 * w2 };
			stroke = new BasicStroke(weight, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, lp1, 0.0f);
			g.setStroke(stroke);
			dotdashStrokes.add(stroke);
			break;
		case Consts.LINE_DOUBLE:
			if (doubleStrokes == null) {
				doubleStrokes = new ArrayList();
			} else {
				li = doubleStrokes.listIterator();
				while (li.hasNext()) {
					BasicStroke bs = (BasicStroke) li.next();
					if (bs.getLineWidth() == weight) {
						g.setStroke(bs);
						return true;
					}
				}
			}
			float[] dashes3 = { w2, 2 * w2, w2, 2 * w2, 8 * w2, 2 * w2 };
			stroke = new BasicStroke(weight, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_BEVEL, 10.0f, dashes3, 0.0f);
			g.setStroke(stroke);
			doubleStrokes.add(stroke);
			break;
		default:

			// stroke = new BasicStroke(0f);
			return false;
		}
		g.setStroke(stroke);
		return true;
	}

	public static int getArrow(int style) {
		return style & 0xF00;
	}

	/**
	 * 以弧度角度radian，画一个箭头起始于dx,dy的箭头
	 * @param g 图形设备
	 * @param dx 实数精度的x坐标
	 * @param dy y坐标
	 * @param radian 弧度制的角度值
	 * @param style 箭头风格
	 */
	public static void drawLineArrow(Graphics2D g, double dx, double dy,
			double radian, int style) {
		style = getArrow(style);
		if (style == Consts.LINE_ARROW_NONE) {
			return;
		}
		int x = (int) dx;
		int y = (int) dy;
		AffineTransform at = g.getTransform();
		AffineTransform at1 = AffineTransform.getRotateInstance(radian, x, y);
		g.transform(at1);
		switch (style) {
		case Consts.LINE_ARROW_NONE: // 无箭头
			break;
		case Consts.LINE_ARROW:
			x += 8;
			int[] xs_arr = { x - 8, x - 12, x, x - 12 };
			int[] ys_arr = { y, y - 4, y, y + 4 };
			g.fillPolygon(xs_arr, ys_arr, 4);
			break;
		case Consts.LINE_ARROW_L://左箭头
			x -= 8;
			int[] xl_arr = { x, x + 12, x+8, x + 12 };
			int[] yl_arr = { y, y - 4, y, y + 4 };
			g.fillPolygon(xl_arr, yl_arr, 4);
			break;
		case Consts.LINE_ARROW_BOTH: // 双箭头
			x += 8;
			int[] xs_bot = { x - 8, x - 12, x, x - 12 };
			int[] ys_bot = { y, y - 4, y, y + 4 };
			g.fillPolygon(xs_bot, ys_bot, 4);
			int[] xs_bot2 = { x - 14, x - 18, x - 6, x - 18 };
			int[] ys_bot2 = { y, y - 4, y, y + 4 };
			g.fillPolygon(xs_bot2, ys_bot2, 4);
			break;
		case Consts.LINE_ARROW_HEART: // 心形箭头
			int r_h = 4;
			int cdy = (int) (r_h * 1.732d);
			int cdx = 3 * r_h;
			x += cdx;
			int[] xs_heart = { x - cdx, x - cdx, x };
			int[] ys_heart = { y - cdy, y + cdy, y };
			g.fillPolygon(xs_heart, ys_heart, 3);
			int d2y = (int) (r_h * 1.732d / 2);
			int d2x = (int) 3.5 * r_h;
			g.fillOval(x - d2x - r_h, y - d2y - r_h, 2 * r_h - 1, 2 * r_h - 1);
			g.fillOval(x - d2x - r_h, y + d2y - r_h, 2 * r_h - 1, 2 * r_h - 1);
			// 所谓心形，实际上就是一个三角形加上两个和两腰相切的圆，在这里用正三角形来计算
			break;
		case Consts.LINE_ARROW_CIRCEL: // 圆形箭头
			x += 8;
			g.fillOval(x - 8, y - 4, 8, 8);
			break;
		case Consts.LINE_ARROW_DIAMOND: // 菱形箭头
			x += 14;
			int[] xs_dia = { x - 14, x - 7, x, x - 7 };
			int[] ys_dia = { y, y - 4, y, y + 4 };
			g.fillPolygon(xs_dia, ys_dia, 4);
			break;
		}
		g.setTransform(at);
	}

	/**
	 * 根据fontStyle判断是否为竖排文字
	 * @param fontStyle 字体风格
	 * @return 竖排返回true，否则返回false
	 */
	public static boolean isVertical(int fontStyle) {
		return (fontStyle & Consts.FONT_VERTICAL) != 0;
	}

	/**
	 * 根据fontStyle判断是否为下划线文字
	 * @param fontStyle 字体风格
	 * @return 有下划线返回true，否则返回false
	 */
	public static boolean isUnderline(int fontStyle) {
		return (fontStyle & Consts.FONT_UNDERLINE) != 0;
	}

	/**
	 * 根据fontStyle判断是否为粗体文字
	 * @param fontStyle 字体风格
	 * @return 有粗体返回true，否则返回false
	 */
	public static boolean isBold(int fontStyle) {
		return (fontStyle & Consts.FONT_BOLD) != 0;
	}

	/**
	 * 获取文本占用的宽高，使用Rectangle的宽和高存储信息
	 * @param text 要测量的文本
	 * @param g 图形设备
	 * @param fontStyle 风格
	 * @param angle 文本的旋转角度
	 * @param font 字体
	 * @return 矩形存储的宽高信息
	 */
	public static Rectangle getTextSize(String text, java.awt.Graphics g,
			int fontStyle, int angle, Font font) {
		boolean vertical = isVertical(fontStyle);
		return getTextSize(text, g, vertical, angle, font);
	}

	/**
	 * 获取文本占用的宽高，使用Rectangle的宽和高存储信息
	 * @param text 要测量的文本
	 * @param g 图形设备
	 * @param vertical 是否垂直绘制
	 * @param angle 旋转角度
	 * @param font 字体
	 * @return 矩形存储的宽高信息
	 */
	public static Rectangle getTextSize(String text, java.awt.Graphics g,
			boolean vertical, int angle, Font font) {
		if (text == null) {
			return new Rectangle();
		}
		Rectangle rect = null;
		if (vertical) {
			rect = getVerticalArea(text, g, angle, font);
		} else if (angle == 0) {
			rect = getHorizonArea(text, g, font);
		} else {
			rect = getRotationArea(text, g, angle, font);
		}
		// added by bdl, 2009.5.8, 在旋转角度不为0时，rect的宽度或者高度有可能产生负值，给留空等操作带来麻烦，全设为正值
		if (rect.width < 0) {
			rect.width = -rect.width;
		}
		if (rect.height < 0) {
			rect.height = -rect.height;
		}
		return rect;
	}

	private static String getChar(String text) {
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c > 255) {
				return "汉";
			}
		}
		return "A";
	}

	/**
	 * 文本竖排排版时的宽高
	 * @param text 要测量的文本
	 * @param g 图形设备
	 * @param angle 旋转角度
	 * @param font 字体
	 * @return 矩形存储的宽高信息
	 */
	public static Rectangle getVerticalArea(String text, java.awt.Graphics g,
			int angle, Font font) {
		if (!StringUtils.isValidString(text)) {
			text = "A";
		}
		Rectangle area = new Rectangle();
		if (angle == 0) {
			FontMetrics fm = g.getFontMetrics(font);
			int hh = fm.getAscent();
			area.width = fm.stringWidth(getChar(text));
			area.height = hh * text.length();
		} else {
			angle = angle % 360;
			if (angle < 0) {
				angle += 360;
			}
			Rectangle area0 = getVerticalArea(text, g, 0, font);
			double sin = Math.sin(angle * Math.PI / 180);
			double cos = Math.cos(angle * Math.PI / 180);
			if (sin < 0) {
				sin = -sin;
			}
			if (cos < 0) {
				cos = -cos;
			}
			int aw = (int) (area0.height * sin + area0.width * cos);
			int ah = (int) (area0.width * sin + area0.height * cos);
			area.width = aw;
			area.height = ah;
		}
		return area;
	}

	/**
	 * 文本水平排版时的宽高
	 * @param text 要测量的文本
	 * @param g 图形设备
	 * @param font 字体
	 * @return 矩形存储的宽高信息
	 */
	public static Rectangle getHorizonArea(String text, java.awt.Graphics g,
			Font font) {
		Rectangle area = new Rectangle();
		FontMetrics fm = g.getFontMetrics(font);
		int hw = fm.stringWidth(text);
		int hh = fm.getAscent() + fm.getDescent(); // .getAscent();
		area.width = hw;
		area.height = hh;
		return area;
	}

	/**
	 * 文本旋转角度排版时占用的宽高
	 * @param text 要测量的文本
	 * @param g 图形设备
	 * @param angle 旋转角度
	 * @param font 字体
	 * @return 矩形存储的宽高信息
	 */
	public static Rectangle getRotationArea(String text, java.awt.Graphics g,
			int angle, Font font) {
		Rectangle area = new Rectangle();
		angle = angle % 360;
		if (angle < 0) {
			angle += 360;
		}
		Rectangle area0 = getTextSize(text, g, 0, 0, font);
		double sin = Math.sin(angle * Math.PI / 180);
		double cos = Math.cos(angle * Math.PI / 180);
		if (sin < 0) {
			sin = -sin;
		}
		if (cos < 0) {
			cos = -cos;
		}
		int aw = (int) (area0.height * sin + area0.width * cos);
		int ah = (int) (area0.width * sin + area0.height * cos);
		area.width = aw;
		area.height = ah;
		return area;
	}

	/**
	 * 根据指定参数和位置绘制一串文本
	 * @param g 图形设备
	 * @param txt 要绘制的文本
	 * @param dx 横坐标x
	 * @param dy 纵坐标y
	 * @param font 字体
	 * @param c 前景色
	 * @param fontStyle 风格
	 * @param angle 旋转角度
	 * @param location 相对于文本中心点的偏移位置，值参考Consts.LOCATION_XX
	 */
	public static void drawText(Graphics2D g, String txt, double dx, double dy,
			Font font, Color c, int fontStyle, int angle, int location) {
		drawText(g, txt, dx, dy, font, c, null, fontStyle, angle, location);
	}

	/**
	 * 根据指定参数绘制一段文本
	 * @param g 图形设备
	 * @param txt 要绘制的文本
	 * @param dx 横坐标x
	 * @param dy 纵坐标y
	 * @param font 字体
	 * @param c 前景色
	 * @param backC 背景色
	 * @param fontStyle 风格
	 * @param angle 旋转角度
	 * @param location 相对于文本中心点的偏移位置，值参考Consts.LOCATION_XX
	 */
	public static void drawText(Graphics2D g, String txt, double dx, double dy,
			Font font, Color c, Color backC, int fontStyle, int angle,
			int location) {
		drawText(null, txt, dx, dy, font, c, backC, fontStyle, angle, location,
				true, g);
	}

	/**
	 * 根据指定参数绘制一段文本
	 * @param e 绘图引擎
	 * @param txt 要绘制的文本
	 * @param dx 横坐标x
	 * @param dy 纵坐标y
	 * @param font 字体
	 * @param c 前景色
	 * @param fontStyle 风格
	 * @param angle 旋转角度
	 * @param location 相对于文本中心点的偏移位置，值参考Consts.LOCATION_XX
	 * @param allowIntersect 是否允许文字重叠(不允许重叠时，后面的重叠文本将忽略)
	 */
	public static void drawText(Engine e, String txt, double dx, double dy,
			Font font, Color c, int fontStyle, int angle, int location,
			boolean allowIntersect) {
		drawText(e, txt, dx, dy, font, c, null, fontStyle, angle, location,
				allowIntersect);
	}

	/**
	 * 根据指定参数绘制一段文本
	 * @param e 绘图引擎
	 * @param txt 要绘制的文本
	 * @param dx 横坐标x
	 * @param dy 纵坐标y
	 * @param font 字体
	 * @param c 前景色
	 * @param fontStyle 风格
	 * @param angle 旋转角度
	 * @param location 相对于文本中心点的偏移位置，值参考Consts.LOCATION_XX
	 * @param allowIntersect 是否允许文字重叠(不允许重叠时，后面的重叠文本将忽略)
	 */
	public static void drawText(Engine e, String txt, double dx, double dy,
			Font font, Color c, Color backC, int fontStyle, int angle,
			int location, boolean allowIntersect) {
		drawText(e, txt, dx, dy, font, c, backC, fontStyle, angle, location,
				allowIntersect, e.getGraphics());
	}

	/**
	 * 由于描述文本位置时，采用的是中心点；而实际绘制文本或者图片时，
	 * isImage时，要变换为左上角， 文本时变换为左下角；
	 * 因为g在左下角绘制文本， 左上角绘制图形。
	 * @param posDesc 根据中心点描述的文本位置
	 * @param location 文本相对中心点的方位
	 * @param isImage 是否绘制图形，否则按文本算法
	 * @return 图形设备直接输出的实际坐标
	 */
	public static Point getRealDrawPoint(Rectangle posDesc, int location,boolean isImage) {
		Rectangle rect = posDesc;
		// 绘图中心点
		int xloc = rect.x;
		int yloc = rect.y;

		if (location == Consts.LOCATION_LT || location == Consts.LOCATION_CT
				|| location == Consts.LOCATION_RT) {
			// 所给点在上边，需要求到左下角的y坐标
			if (isImage) {
				yloc -= rect.height;
			} else {
				yloc += rect.height;
			}
		} else if (location == Consts.LOCATION_LM
				|| location == Consts.LOCATION_CM
				|| location == Consts.LOCATION_RM) {
			// 所给参考点在中间，需要求到左下角y坐标
			if (isImage) {
				yloc -= rect.height / 2;
			} else {
				yloc += rect.height / 2;
			}
		} else {
			yloc -= 1;
		}
		if (location == Consts.LOCATION_RT || location == Consts.LOCATION_RM
				|| location == Consts.LOCATION_RB) {
			// 所给点在右边，需要求到左下角的x坐标
			xloc -= rect.width;
		} else if (location == Consts.LOCATION_CT
				|| location == Consts.LOCATION_CM
				|| location == Consts.LOCATION_CB) {
			// 所给参考点在中间，需要求到左下角x坐标
			xloc -= rect.width / 2;
		} else {
			xloc += 1;
		}
		return new Point(xloc, yloc);
	}

	/**
	 * 根据指定参数绘制一段文本
	 * @param e 绘图引擎
	 * @param txt 要绘制的文本
	 * @param dx 横坐标x
	 * @param dy 纵坐标y
	 * @param font 字体
	 * @param c 前景色
	 * @param backC 背景色
	 * @param fontStyle 风格
	 * @param angle 旋转角度
	 * @param location 相对于文本中心点的偏移位置，值参考Consts.LOCATION_XX
	 * @param allowIntersect 是否允许文字重叠(不允许重叠时，后面的重叠文本将忽略)
	 * @param g 图形设备
	 */
	public static void drawText(Engine e, String txt, double dx, double dy,
			Font font, Color c, Color backC, int fontStyle, int angle,
			int location, boolean allowIntersect, Graphics2D g) {
		if (txt == null || txt.trim().length() < 1 || font.getSize() == 0) {
			return;
		}
		int x = (int) dx;
		int y = (int) dy;
		boolean vertical = isVertical(fontStyle);
		FontMetrics fm = g.getFontMetrics(font);

		// 文字不重叠
		Rectangle rect = getTextSize(txt, g, vertical, angle, font);
		rect.x = x;
		rect.y = y;
		if (e != null) {
			if (!allowIntersect && e.intersectTextArea(rect)) {
				return;
			} else {
				e.addTextArea(rect);
			}
		}

		g.setFont(font);
		g.setColor(c);

		Point drawPoint = getRealDrawPoint(rect, location, false);
		int xloc = drawPoint.x;
		int yloc = drawPoint.y;

		Utils.setGraphAntiAliasingOff(g);

		if (!vertical) {
			// 非竖排文字
			if (angle != 0) {
				AffineTransform at = g.getTransform();
				Rectangle rect2 = getTextSize(txt, g, vertical, 0, font);
				rect2.setLocation(xloc, yloc - rect2.height);
				int delx = 0, dely = 0;
				angle = angle % 360;
				if (angle < 0) {
					angle += 360;
				}
				if (angle >= 0 && angle < 90) {
					delx = 0;
					dely = (int) (rect2.width * Math.sin(angle * Math.PI / 180));
				} else if (angle < 180) {
					dely = rect.height;
					delx = (int) (rect2.width * Math.cos(angle * Math.PI / 180));
				} else if (angle < 270) {
					delx = -rect.width;
					dely = (int) (-rect2.height * Math.sin(angle * Math.PI
							/ 180));
				} else {
					dely = 0;
					delx = (int) (rect2.height * Math
							.sin(angle * Math.PI / 180));
				}
				AffineTransform at1 = AffineTransform.getRotateInstance(angle
						* Math.PI / 180, xloc - delx, yloc - dely);
				g.transform(at1);

				if (backC != null) {
					g.setColor(backC);
					g.fillRect(xloc - delx, yloc - dely - fm.getAscent(),
							rect2.width, rect2.height);
				}
				g.setColor(c);
				g.drawString(txt, xloc - delx, yloc - dely);

				g.setTransform(at);
			} else {
				if (backC != null) {
					g.setColor(backC);
					g.fillRect(xloc, yloc - fm.getAscent(), rect.width,
							rect.height);
				}

				g.setColor(c);
				g.drawString(txt, xloc, yloc);
			}
		} else {
			// 竖排文字，竖排文字如果有旋转角度的话，属于比较奇怪的使用
			// 由于每个字去旋转然后竖排的话，有可能产生遮挡，所以这里采用支持竖排了再旋转的方式
			// 在考察竖排旋转的文字时，为了效率，只使用第一个字符计算，如果非要竖排中英混排的文字，那么乱了也没有办法
			AffineTransform at = g.getTransform();
			Rectangle rect2 = getTextSize(txt, g, vertical, 0, font);
			rect2.setLocation(xloc, yloc - rect2.height);
			// this.addShapeInfo(rect2, si);
			Rectangle rect3 = getTextSize(txt.substring(0, 1), g, vertical, 0,
					font);
			int delx = 0, dely = 0;
			angle = angle % 360;
			if (angle < 0) {
				angle += 360;
			}
			if (angle >= 0 && angle < 90) {
				delx = 0;
				dely = (int) (rect2.width * Math.sin(angle * Math.PI / 180));
			} else if (angle < 180) {
				dely = rect.height;
				delx = (int) (rect2.width * Math.cos(angle * Math.PI / 180));
			} else if (angle < 270) {
				delx = -rect.width;
				dely = (int) (-rect2.height * Math.sin(angle * Math.PI / 180));
			} else {
				dely = 0;
				delx = (int) (-rect2.height * Math.cos(angle * Math.PI / 180));
			}
			AffineTransform at1 = AffineTransform.getRotateInstance(angle
					* Math.PI / 180, xloc - delx, yloc - dely);
			g.transform(at1);
			int length = txt.length();
			for (int i = length; i > 0; i--) {
				g.drawString(txt.substring(i - 1, i), xloc - delx, yloc - dely
						- rect3.height * (length - i));
			}
			g.setTransform(at);
		}
		if (isUnderline(fontStyle)) {
			// 需要下划线，画一条（目前只在横排文字，且不旋转的时候画）
			// /至于横排或者是旋转个角度的话怎么处理，置后。
			if (!vertical && angle == 0) {
				setStroke(g, null, Consts.LINE_SOLID, 0.5f);
				drawLine(g, xloc, yloc + 2, xloc + rect.width, yloc + 2);
			}
		}
		Utils.setGraphAntiAliasingOn(g);
	}

	/**
	 * 大量绘制文本时，由于字体大多相同，为了绘制性能，避免大量new字体对象
	 * 采用字体缓存获取对应字体
	 * @param fontName 字体名称
	 * @param fontStyle 风格
	 * @param fontSize 字号
	 * @return 字体对象
	 */
	public synchronized static Font getFont(String fontName, int fontStyle,
			int fontSize) {
		if (fontName == null || fontName.trim().length() < 1) {
			fontName = "dialog";
		}
		ListIterator li = globalFonts.listIterator();
		fontStyle = fontStyle & 0x03;
		while (li.hasNext()) {
			Font f = (Font) li.next();
			if (f.getFontName().equalsIgnoreCase(fontName)
					&& f.getStyle() == fontStyle && f.getSize() == fontSize) {
				return f;
			}
		}
		Font f = new Font(fontName, fontStyle, fontSize);
		globalFonts.add(f);
		return f;
	}

	/**
	 * 根据文本的旋转角度计算出实际绘制文本时，
	 * 文本的相对中心点的方位
	 * @param angle 旋转角度
	 * @return 相对中心点的方位，值为Consts.LOCATION_XX
	 */
	public static int getAngleTextLocation(double angle) {
		if (angle == 0) {
			return Consts.LOCATION_LM;
		} else if (angle < 90) {
			return Consts.LOCATION_LB;
		} else if (angle == 90) {
			return Consts.LOCATION_CB;
		} else if (angle < 180) {
			return Consts.LOCATION_RB;
		} else if (angle == 180) {
			return Consts.LOCATION_RM;
		} else if (angle < 270) {
			return Consts.LOCATION_RT;
		} else if (angle == 270) {
			return Consts.LOCATION_CT;
		} else if (angle < 360) {
			return Consts.LOCATION_LT;
		} else if (angle == 360) {
			return Consts.LOCATION_LM;
		}
		return 0;
	}

	/**
	 * 判断数据图元列表里面是否含有堆积类型属性，堆积图的坐标范围需要累计计算
	 * @param dataElements 数据图元列表
	 * @return 如果有堆积类型返回true，否则返回false
	 */
	public static boolean isStackedGraph(ArrayList dataElements) {
		for (int i = 0; i < dataElements.size(); i++) {
			DataElement de = (DataElement) dataElements.get(i);
			if (de instanceof Column) {
				if (((Column) de).isStacked()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 将实数o按照format格式化为文本
	 * @param o 数值
	 * @param format 格式信息
	 * @return 格式化后的文本
	 */
	public static String format(double o, String format) {
		return format(new java.lang.Double(o), format);
	}

	/**
	 * 将数据o按照format格式化为文本
	 * @param o 任意类型的数据(常用的日期，数值，以及列表等)
	 * @param format 格式信息
	 * @return 格式化后的文本
	 */
	public static String format(Object o, String format) {
		if (o instanceof Date) {
			DateFormat sdf = new SimpleDateFormat(format);
			return sdf.format(o);
		} else if (o instanceof Number) {
			NumberFormat nf = new DecimalFormat(format);
			return nf.format(o);
		} else if (o instanceof ArrayList) {
			ArrayList series = (ArrayList) o;
			StringBuffer sb = new StringBuffer();
			for (int i = 0, size = series.size(); i <= size; ++i) {
				if (i > 1) {
					sb.append(',');
				}
				sb.append(format(series.get(i), format));
			}
			return sb.toString();
		} else {
			return Variant.toString(o);
		}
	}

	public static void drawRect(Graphics2D g,double x, double y,double w,double h) {
		Rectangle2D.Double rect = new Rectangle2D.Double(x, y, w, h);
		g.draw(rect);
	}

	public static void fillRect(Graphics2D g,double x, double y,double w,double h) {
		Rectangle2D.Double rect = new Rectangle2D.Double(x, y, w, h);
		g.fill(rect);
	}
	
	public static void fillPolygon(Graphics2D g,double[] x, double[] y) {
		Shape s = newPolygon2D(x,y);
		g.fill(s);
	};
	
	public static void drawPolygon(Graphics2D g,double[] x, double[] y) {
		Shape s = newPolygon2D(x,y);
		g.draw(s);
	};

	/**
	 * 按照指定参数给形状设置填充
	 * @param g 图形设备
	 * @param shape 需要填充的形状
	 * @param transparent 填充透明度
	 * @param c 填充颜色，如果该值为null，则表示当前形状透明，不做填充。
	 */
	public static void fill(Graphics2D g, Shape shape, float transparent,
			Color c) {
		if (c == null) {// 透明色，不填充
			return;
		}
		g.setColor(c);
		setTransparent(g, transparent);
		g.fill(shape);
		setTransparent(g, 1);
	}

	/**
	 * 使用图形设备当前颜色，再按照指定参数给形状设置填充
	 * @param g 图形设备
	 * @param shape 需要填充的形状
	 * @param transparent 填充透明度
	 */
	public static void fillPaint(Graphics2D g, Shape shape, float transparent) {
		setTransparent(g, transparent);
		g.fill(shape);
		setTransparent(g, 1);
	}

	/**
	 * 设置图形设备当前的透明度
	 * @param g 图形设备
	 * @param transparent 透明度，取值范围为区间[0,1]，越界的数值当0或1处理
	 */
	public static void setTransparent(Graphics2D g, float transparent) {
		if (transparent > 1) {
			transparent = 1f;
		} else if (transparent < 0) {
			transparent = 0f;
		}
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				transparent));
	}

	/**
	 * 在当前的图形设备环境下绘制一条线段
	 * @param g 图形设备
	 * @param b 起始点
	 * @param e 结束点
	 */
	public static void drawLine(Graphics2D g, Point2D b, Point2D e) {
		drawLine(g,b,e,Consts.LINE_ARROW_NONE);
	}
	
	/**
	 * 在当前的图形设备环境下绘制一条带箭头的线段
	 * @param g 图形设备
	 * @param b 起始点
	 * @param e 结束点
	 * @param arrow 箭头形状，值仅支持
	 * Consts.LINE_ARROW 结束点出绘制右箭头
	 * Consts.LINE_ARROW_L 起始点处绘制左箭头 
	 */
	public static void drawLine(Graphics2D g, Point2D b, Point2D e, int arrow) {
		if (b == null || e == null) {
			return;
		}
		drawLine(g, b.getX(), b.getY(), e.getX(), e.getY(), arrow);
	}

	/**
	 * 按照指定参数绘制一个填充的矩形，对应于柱图元
	 * @param g 图形设备
	 * @param x x坐标
	 * @param y y坐标
	 * @param w 宽度
	 * @param h 高度
	 * @param borderColor 边框颜色
	 * @param borderStyle 边框风格
	 * @param borderWeight 边框粗度
	 * @param drawShade 绘制阴影
	 * @param convexEdge 是否凸出边框
	 * @param transparent 填充透明度
	 * @param fillColor 填充颜色
	 * @param isVertical 是否竖向柱子(柱子的横纵不同时，采用的填充渐变色不同)
	 */
	public static void draw2DRect(Graphics2D g, double x, double y, double w, double h,
			Color borderColor, int borderStyle, float borderWeight,
			boolean drawShade, boolean convexEdge, float transparent,
			ChartColor fillColor, boolean isVertical) {
		if (drawShade && fillColor.getColor1() != null) {
			drawRectShadow(g, x, y, w, h);
		}

		setTransparent(g, transparent);
		if(fillColor.getType()!=Consts.PATTERN_DEFAULT){
			Utils.setPaint(g, x, y, w, h, fillColor);
			fillRect(g, x, y, w, h);
		}else if (fillColor.getColor1() != null) {
			// 炫效果
			if (fillColor.isDazzle()) {
				CubeColor ccr = new CubeColor(fillColor.getColor1());
				Color c1 = ccr.getR1(), c2 = ccr.getT1();
				double x1, y1, x2, y2;
				if (isVertical) {
					x1 = x;
					y1 = y;
					x2 = x1 + w / 2;
					y2 = y1;
				} else {
					x1 = x;
					y1 = y;
					x2 = x1;
					y2 = y1 + h / 2;
				}
				if (c1 != null && c2 != null) {
					GradientPaint paint = new GradientPaint((int)x1, (int)y1, c1, (int)x2, (int)y2,
							c2, true);
					g.setPaint(paint);
					fillRect(g,x1, y1, w, h);
				}
			} else if (fillColor.isGradient()) {
				if (setPaint(g, x, y, w, h, fillColor)) {
					fillRect(g,x, y, w, h);
				}
			} else {
				if (convexEdge) {
					Color change = CubeColor.getDazzelColor(fillColor
							.getColor1());
					g.setColor(change);
				} else {
					g.setColor(fillColor.getColor1());
				}
				fillRect(g,x, y, w, h);
			}
		}

		setTransparent(g, 1);

		if (convexEdge && !fillColor.isGradient() && w > 10 && h > 10) {
			drawRaisedBorder(g, x, y, w, h, fillColor.getColor1());
		}
		if (setStroke(g, borderColor, borderStyle, borderWeight)) {
			if (borderColor != null) {// 边框color为null表示用当前使用中的画笔
				g.setColor(borderColor);
			}
			drawRect(g,x, y, w, h);
		}
		setTransparent(g, transparent);
	}

	/**
	 * 获取多个坐标点的绘制路径(点比较少时准确，点多且密集时，java有bug，绘制的线错误)
	 * 避免使用该方法绘制多条线段
	 * @param points 点坐标
	 * @param closePath 是否封闭路径
	 * @return 绘制路径
	 */
	public static Path2D getPath2D(ArrayList<Point2D> points, boolean closePath) {
		Path2D.Double path2D = new Path2D.Double();
		for (int i = 0; i < points.size(); i++) {
			Point2D p = points.get(i);
			if (i == 0) {
				path2D.moveTo(p.getX(), p.getY());
			} else {
				path2D.lineTo(p.getX(), p.getY());
			}
		}
		if (points.size() > 0 && closePath) {
			path2D.closePath();
		}
		return path2D;
	}

	public static Shape newPolygon2D(double[] x, double[] y) {
		return newPolygon2DShape(x,y);
	}
	/**
	 * 采用路径依次连接给定的点坐标
	 * @param x x坐标
	 * @param y y坐标
	 * @return 封闭路径的形状
	 */
	public static Shape newPolygon2DShape(double[] x, double[] y) {
		Path2D.Double polygon2D = new Path2D.Double();
		polygon2D.moveTo(x[0], y[0]);
		for (int i = 1; i < x.length; i++) {
			polygon2D.lineTo(x[i], y[i]);
		}
		polygon2D.closePath();
		return polygon2D;
	}

	private static void drawRaisedBorder(Graphics2D g, double x, double y, double w,
			double h, Color borderColor) {
		// 太细的柱子也不绘制凸出边框
		// T1最亮，R1相对暗.渐变色的凸出边框不好看，渐变色时忽略凸出边框属性；
		Color dazzel = CubeColor.getDazzelColor(borderColor);
		CubeColor ccr = new CubeColor(dazzel);
		int d = 5;
		for (int i = 0; i < d; i++) {
			Color tmp = ccr.getLight((i + 1) * 0.2f);
			Utils.setStroke(g, tmp, Consts.LINE_SOLID, 1);
			drawLine(g,x + i, y + i, x + i, y + h - i);
			drawLine(g,x + i, y + i, x + w - i, y + i);
		}
		for (int i = 0; i < d; i++) {
			Color tmp = ccr.getDark((i + 1) * 0.2f);
			Utils.setStroke(g, tmp, Consts.LINE_SOLID, 1);
			drawLine(g,x + w - i, y + i, x + w - i, y + h - i);
			drawLine(g,x + i, y + h - i, x + w - i, y + h - i);
		}
	}

	/**
	 * 在指定区域绘制矩形阴影
	 * @param g 图形设备
	 * @param x x坐标
	 * @param y y坐标
	 * @param w 宽度
	 * @param h 高度
	 * @return 阴影通常需要偏移，返回这个偏移量，单位像素
	 */
	public static int drawRectShadow(Graphics2D g, double x, double y, double w, double h) {
		if (w == 0 || h == 0)
			return 0;
		int dShadow = 4;
		w -= dShadow;
		h -= dShadow;
		double x1, y1, x2, y2;
		int z = 0;
		Color cz = new Color(shadowColors[z * 3], shadowColors[z * 3 + 1],
				shadowColors[z * 3 + 2]);
		z = 1;
		cz = new Color(shadowColors[z * 3], shadowColors[z * 3 + 1],
				shadowColors[z * 3 + 2]);
		Utils.setStroke(g, cz, Consts.LINE_SOLID, 1);
		x1 = x + w + dShadow;
		y1 = y - 1;
		x2 = x1;
		y2 = y + h - 1;
		drawLine(g,x1, y1, x2, y2);
		x1 = x + w + 3;
		y1 = y - 2;
		x2 = x1;
		y2 = y1;
		drawLine(g,x1, y1, x2, y2);

		z = 2;
		cz = new Color(shadowColors[z * 3], shadowColors[z * 3 + 1],
				shadowColors[z * 3 + 2]);
		Utils.setStroke(g, cz, Consts.LINE_SOLID, 1);
		x1 = x + 1;
		y1 = y - 2;
		x2 = x + w + 2;
		y2 = y1;
		drawLine(g,x1, y1, x2, y2);
		x1 = x + w + 3;
		y1 = y - 1;
		x2 = x1;
		y2 = y + h - 1;
		drawLine(g,x1, y1, x2, y2);

		z = 3;
		cz = new Color(shadowColors[z * 3], shadowColors[z * 3 + 1],
				shadowColors[z * 3 + 2]);
		Utils.setStroke(g, cz, Consts.LINE_SOLID, 1);
		x1 = x + 1;
		y1 = y - 1;
		x2 = x + w + 2;
		y2 = y1;
		drawLine(g,x1, y1, x2, y2);
		x1 = x + w + 2;
		y1 = y - 1;
		x2 = x1;
		y2 = y + h - 1;
		drawLine(g,x1, y1, x2, y2);

		z = 4;
		cz = new Color(shadowColors[z * 3], shadowColors[z * 3 + 1],
				shadowColors[z * 3 + 2]);
		Utils.setStroke(g, cz, Consts.LINE_SOLID, 2);
		x1 = x + w;
		y1 = y;
		fillRect(g,x1, y1, 2, h);
		return dShadow;
	}

	/**
	 * 获取合适的3D坐标平台的厚度，不能太厚，也不能太薄
	 * @param coorShift 3D坐标偏移量
	 * @return 调整后的适中偏移量
	 */
	public static double getPlatformH(double coorShift) {
		double h = coorShift;
		if (h < 2)
			h = 2;
		if (h > 6)
			h = 10;
		return h;
	}

	/**
	 * 将指定参数封装为三维立体柱描述对象，使代码更简洁
	 * @param x x坐标
	 * @param y y坐标
	 * @param w 宽度
	 * @param h 高度
	 * @param borderColor 边框颜色
	 * @param borderStyle 边框风格
	 * @param borderWeight 边框粗度
	 * @param drawShade 是否绘制阴影
	 * @param convexEdge 是否凸出边框
	 * @param transparent 填充透明度
	 * @param fillColor 填充颜色
	 * @param isVertical 是否竖向柱子
	 * @param coorShift 三维厚度
	 * @return 包含上述参数的封装类
	 */
	public static Desc3DRect get3DRect(double x, double y, double w, double h,
			Color borderColor, int borderStyle, float borderWeight,
			boolean drawShade, boolean convexEdge, float transparent,
			ChartColor fillColor, boolean isVertical, double coorShift) {
		Desc3DRect d3 = new Desc3DRect();
		d3.x = x;
		d3.y = y;
		d3.w = w;
		d3.h = h;
		d3.borderColor = borderColor;
		d3.borderStyle = borderStyle;
		d3.borderWeight = borderWeight;
		d3.drawShade = drawShade;
		d3.convexEdge = convexEdge;
		d3.transparent = transparent;
		d3.fillColor = fillColor;
		d3.isVertical = isVertical;
		d3.coorShift = coorShift;
		return d3;
	}

	/**
	 * 使用三维描述的立体柱，绘制一个立方体
	 * @param g 图形设备
	 * @param d3 封装了立方体参数的描述对象
	 */
	public static void draw3DRect(Graphics2D g, Desc3DRect d3) {
		draw3DRect(g, d3.x, d3.y, d3.w, d3.h, d3.borderColor, d3.borderStyle,
				d3.borderWeight, d3.drawShade, d3.convexEdge, d3.transparent,
				d3.fillColor, d3.isVertical, d3.coorShift);
	}

	/**
	 * 在指定位置按照配置绘制一个立体柱子 3D柱子画上边框就不好看了，所以borderColor为null是为透明色，
	 * 不解释为使用当前颜色
	 * 返回超链接的区域形状 isDrawTop,isDrawRight,
	 * 是否绘制顶边（纵向柱子），或者右边（横向条状），该选项用于有负值累积时
	 * 由于累积方向相反，所以，除第一个靠近
	 * 轴的柱子全画，其余的都不绘制顶部，否则会造成柱子间覆盖
	 */
	public static void draw3DRect(Graphics2D g, double x, double y, double w, double h,
			Color borderColor, int borderStyle, float borderWeight,
			boolean drawShade, boolean convexEdge, float transparent,
			ChartColor fillColor, boolean isVertical, double coorShift) {
		Shape poly;
		if (drawShade && fillColor.getColor1() != null) {
			drawRectShadow(g, x + coorShift, y - coorShift, w, h);
		}
		CubeColor ccr = new CubeColor(fillColor.getColor1());
		if (transparent < 1) {
			// 1:被遮挡的背面，透明时才能看到；
			// g.setColor(fillColor.getColor1());
			Utils.fill(g, new Rectangle2D.Double(x + coorShift, y - coorShift, w, h),
					transparent, fillColor.getColor1());
			if (Utils.setStroke(g, borderColor, borderStyle, borderWeight)
					&& borderColor != null) {
				drawRect(g,x + coorShift, y - coorShift, w, h);
			}
			// 2:被遮挡的底面
			double[] xPointsB = { x, x + w, x + w + coorShift, x + coorShift };
			double[] yPointsB = { y + h, y + h, y + h - coorShift,y + h - coorShift };
			poly = newPolygon2D(xPointsB, yPointsB);
			// g.setColor(fillColor.getColor1());
			Utils.fill(g, poly, transparent, fillColor.getColor1());
			if (Utils.setStroke(g, borderColor, borderStyle, borderWeight)
					&& borderColor != null) {
				g.draw(poly);
			}

			// 3:被遮挡的左侧面
			double[] xPointsL = { x, x, x + coorShift, x + coorShift };
			double[] yPointsL = { y, y + h, y + h - coorShift, y - coorShift };
			// g.setColor(fillColor.getColor1());
			poly = newPolygon2D(xPointsL, yPointsL);
			Utils.fill(g, poly,transparent, fillColor.getColor1());
			if (Utils.setStroke(g, borderColor, borderStyle, borderWeight)
					&& borderColor != null) {
				g.draw( poly );
			}
		}
		Rectangle bound;
		// 4:右边侧面
		// if (isDrawRight) {
		double[] xPointsR = { x + w, x + w, x + w + coorShift, x + w + coorShift };
		double[] yPointsR = { y, y + h, y + h - coorShift, y - coorShift };
		poly = newPolygon2D(xPointsR, yPointsR);
		bound = poly.getBounds();
		if (fillColor.isGradient()) {
			ChartColor tmpcc = new ChartColor();
			tmpcc.setGradient(true);
			tmpcc.setAngle(270);
			if (isVertical) {
				tmpcc.setColor1(ccr.getR1());
				tmpcc.setColor2(ccr.getR2());
			} else {
				tmpcc.setColor1(ccr.getT1());
				tmpcc.setColor2(ccr.getT2());
			}
			if (Utils.setPaint(g, bound.x, bound.y, bound.width, bound.height,
					tmpcc)) {
				Utils.fillPaint(g, poly, transparent);
			}
		} else {
			// g.setColor(ccr.getR1());
			Utils.fill(g, poly, transparent, ccr.getR1());
		}

		if (Utils.setStroke(g, borderColor, borderStyle, borderWeight)
				&& borderColor != null) {
			g.draw(poly);
		}
		// }

		// 5:顶面
		// if (isDrawTop) {
		double[] xPointsT = { x, x + w, x + w + coorShift, x + coorShift };
		double[] yPointsT = { y, y, y - coorShift, y - coorShift };
		poly = newPolygon2D(xPointsT, yPointsT);
		bound = poly.getBounds();
		if (fillColor.isGradient()) {// ，右面和顶面都使用渐变时就炫
			ChartColor tmpcc = new ChartColor();
			tmpcc.setGradient(true);
			tmpcc.setAngle(180);
			if (isVertical) {
				tmpcc.setColor1(ccr.getT1());
				tmpcc.setColor2(ccr.getT2());
			} else {
				tmpcc.setColor1(ccr.getR1());
				tmpcc.setColor2(ccr.getR2());
			}
			if (Utils.setPaint(g, bound.x, bound.y, bound.width, bound.height,
					tmpcc)) {
				Utils.fillPaint(g, poly, transparent);
			}
		} else {
			// g.setColor(ccr.getT2());
			Utils.fill(g, poly, transparent, ccr.getT2());
		}

		if (Utils.setStroke(g, borderColor, borderStyle, borderWeight)
				&& borderColor != null) {
			g.draw(poly);
		}
		// }

		boolean isSet = false;
		if (fillColor.isDazzle()) {// 只有前面得区分是否炫效果
			// 前面
			ChartColor tmpcc = new ChartColor();
			if (isVertical) {
				tmpcc.setColor1(ccr.getF1());
				tmpcc.setColor2(ccr.getF2());
				tmpcc.setAngle(270);
			} else {
				tmpcc.setColor1(ccr.getF1());
				tmpcc.setColor2(ccr.getF2());
				tmpcc.setAngle(180);
			}
			isSet = Utils.setPaint(g, x, y, w, h, tmpcc);
		} else {
			isSet = Utils.setPaint(g, x, y, w, h, fillColor);
		}
		if (isSet) {
			Utils.fillPaint(g, new Rectangle2D.Double(x, y, w, h), transparent);
		}

		if (convexEdge && !fillColor.isGradient() && w > 10 && h > 10) {
			drawRaisedBorder(g, x, y, w, h, fillColor.getColor1());
		}

		if (Utils.setStroke(g, borderColor, borderStyle, borderWeight)
				&& borderColor != null) {
			drawRect(g,x, y, w, h);
		}
	}

	private static Color getShadeColor(int z) {
		if (z > 4)
			z = 4;
		return new Color(shadowColors[z * 3], shadowColors[z * 3 + 1],
				shadowColors[z * 3 + 2]);
	}

	private static Rectangle2D getSmallerBounds(Rectangle2D orginal,
			int deltaSize) {
		if (deltaSize == 0)
			return orginal;
		double x = orginal.getX();
		double y = orginal.getY();
		double w = orginal.getWidth();
		double h = orginal.getHeight();
		double scale;
		if (w >= h) {
			scale = h / w;
			w -= deltaSize * 2;
			h -= deltaSize * 2 * scale;
			x += deltaSize;
			y += deltaSize * scale;
		} else {
			scale = w / h;
			w -= deltaSize * 2 * scale;
			h -= deltaSize * 2;
			x += deltaSize * scale;
			y += deltaSize;
		}
		return new Rectangle2D.Double(x, y, w, h);
	}

	/**
	 * 按照指定参数绘制一个平面饼图
	 * @param g 图形设备
	 * @param ellipseBounds 饼图或扇形的边界
	 * @param startAngle 扇形的起始角度
	 * @param extentAngle 角度长度范围(比如起始5度，绘制长度45度，表示从5度画到50度的一个扇形)
	 * @param borderColor 边框颜色
	 * @param borderStyle 边框风格
	 * @param borderWeight 边框粗度
	 * @param transparent 透明度
	 * @param fillColor 填充颜色
	 * @param dazzelCount 边框炫的厚度
	 */
	public static void draw2DPie(Graphics2D g, Rectangle2D ellipseBounds,
			double startAngle, double extentAngle, Color borderColor,
			int borderStyle, float borderWeight, float transparent,
			ChartColor fillColor, int dazzelCount) {
		draw2DArc(g, ellipseBounds, startAngle, extentAngle, borderColor,
				borderStyle, borderWeight, transparent, fillColor, dazzelCount,
				Arc2D.PIE);
	}

	/**
	 * 按照指定参数绘制扇形的实现
	 * @param g 图形设备
	 * @param ellipseBounds 饼图或扇形的编辑
	 * @param startAngle 起始角度
	 * @param extentAngle 角度范围
	 * @param borderColor 边框颜色
	 * @param borderStyle 边框风格
	 * @param borderWeight 边框粗度
	 * @param transparent 透明度
	 * @param fillColor 填充颜色
	 * @param dazzelCount 边框炫的厚度
	 * @param arcType 弧的类型，值参考：Arc2D.XXX
	 */
	public static void draw2DArc(Graphics2D g, Rectangle2D ellipseBounds,
			double startAngle, double extentAngle, Color borderColor,
			int borderStyle, float borderWeight, float transparent,
			ChartColor fillColor, int dazzelCount, int arcType) {
		Arc2D arc = new Arc2D.Double(ellipseBounds, startAngle, extentAngle,
				arcType);
		Rectangle rect = ellipseBounds.getBounds();
		ChartColor cc = fillColor;
		if (cc.isDazzle()) {
			Color dazzel = CubeColor.getDazzelColor(cc.getColor1());
			CubeColor ccr = new CubeColor(dazzel);
			GradientPaint gp;
			// 由外及里面颜色渐变画出边框，最后一圈小的使用渐变色填充突出炫
			// setTransparent(g, transparent);
			if (dazzelCount > 10) {// 该数值不能太大，没有足够的颜色以及性能上
				dazzelCount = 10;
			}
			for (int k = 0; k < dazzelCount; k++) {
				Rectangle2D tmpBounds = getSmallerBounds(ellipseBounds, k);
				Arc2D tmpArc = new Arc2D.Double(tmpBounds, startAngle,
						extentAngle, arcType);
				Color tmp = ccr.getDark(0.5f + k * 0.07f);
				rect = tmpBounds.getBounds();
				if (k == (dazzelCount - 1)) {
					if (ccr.getF2() != null && ccr.getF1() != null) {
						gp = new GradientPaint(rect.x, rect.y + rect.height,
								ccr.getF2(), rect.x + rect.width / 2, rect.y
										+ rect.height / 2, ccr.getF1(), true);
						g.setPaint(gp);
						Utils.fillPaint(g, tmpArc, transparent);
					}
				} else {
					// g.setColor(tmp);
					Utils.fill(g, tmpArc, transparent, tmp);
				}
			}
		} else if (cc.color1 != null) {// 如果不是透明色
			if (Utils.setPaint(g, rect.x, rect.y, rect.width, rect.height, cc)) {
				Utils.fillPaint(g, arc, transparent);
			}
		}// 否则为透明色，不用绘制
		if (Utils.setStroke(g, borderColor, borderStyle, borderWeight)) {
			g.draw(arc);
		}

	}

	/**
	 * 按照给定坐标以及当前图形设备的环境绘制一条线段
	 * drawLine跟drawLineShade之所以要分开，是因为绘图时需要先把阴影画完，然后才能画线，不能交错画；
	 * @param g 图形设备
	 * @param x1 起点x坐标
	 * @param y1 起点y坐标
	 * @param x2 终点x坐标
	 * @param y2 终点y坐标
	 */
	public static void drawLine(Graphics2D g, double x1, double y1, double x2,
			double y2) {
		drawLine(g,x1,y1,x2,y2,Consts.LINE_ARROW_NONE);
	}
	
	/**
	 * 按照给定坐标以及当前图形设备的环境绘制一条带箭头的线段
	 * @param g 图形设备
	 * @param x1 起点x坐标
	 * @param y1 起点y坐标
	 * @param x2 终点x坐标
	 * @param y2 终点y坐标
	 * @param arrow 箭头位置，取值只能为
	 * 无 Consts.LINE_ARROW_NONE (即普通线段)
	 * 左 Consts.LINE_ARROW_L(起点处)
	 * 右 Consts.LINE_ARROW  (终点处)
	 */
	public static void drawLine(Graphics2D g, double x1, double y1, double x2,
			double y2, int arrow) {
		Line2D line = new Line2D.Double(x1, y1, x2, y2);
		g.draw(line);
//		线的箭头暂时只支持普通的左右箭头
		if(arrow==Consts.LINE_ARROW || arrow==Consts.LINE_ARROW_L){
//			弧度表示的两点之间的斜率
			double radian = Math.atan2(y2-y1,x2-x1);
			double x = x2, y = y2;
			if(arrow==Consts.LINE_ARROW_L){//左箭头时，需要给定x1
				x = x1;
				y = y1;
			}
			Utils.drawLineArrow(g, x, y, radian, arrow);
		}
	}

	/**
	 * 设置图形设备的当前画笔颜色
	 * @param g 图形设备
	 * @param c 画笔颜色，颜色为null时，不改变当前画笔颜色
	 */
	public static void setColor(Graphics2D g, Color c) {
		if (c != null) {
			g.setColor(c);
		}
	}

	private static boolean setPointPaint(Graphics2D g, ChartColor cc,
			Shape pShape) {
		if (cc.getColor1() == null) {
			return false;
		}
		Rectangle2D bound = pShape.getBounds2D();

		boolean isCircle = (pShape instanceof Ellipse2D.Double);
		if (cc.isDazzle()) {
			Paint paint = null;
			if (isCircle) {
				Ball ball = new Ball(cc.getColor1(), 0);
				double x, y, w, h, L = 2;
				x = bound.getX() - L;
				y = bound.getY() - L;
				w = bound.getWidth() + L * 2;
				h = bound.getHeight() + L * 2;

				Rectangle2D bd = new Rectangle2D.Double(x, y, w, h);
				paint = new TexturePaint(ball.imgs[4], bd);
			} else {
				CubeColor cuc = new CubeColor(cc.getColor1());
				paint = new GradientPaint((int) bound.getX(),
						(int) (bound.getY() + bound.getHeight()), cuc.getF2(),
						(int) (bound.getX() + bound.getWidth() / 2),
						(int) (bound.getY() + bound.getHeight() / 2),
						cuc.getF1(), true);
			}
			g.setPaint(paint);
		} else {
			setPaint(g, (int) bound.getX(), (int) bound.getY(),
					(int) bound.getWidth(), (int) bound.getHeight(), cc);
		}
		return true;
	}

	private static Shape getCPoint1ShapeArea(int shape, double x, double y,
			double radiusx, double radiusy) {
		Shape pShape = null;
		switch (shape) {
		case Consts.PT_NONE: // 无
			break;
		case Consts.PT_CIRCLE: // 圆
			pShape = new Ellipse2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			break;
		case Consts.PT_SQUARE: // 正方形
			pShape = new Rectangle2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			break;
		case Consts.PT_TRIANGLE: // 三角形
			double[] xs_tr = new double[] {
					x - radiusx * Math.cos(Math.toRadians(30)), x,
					x + radiusx * Math.cos(Math.toRadians(30)) };
			double[] ys_tr = new double[] { y + radiusy / 2, y - radiusy,
					y + radiusy / 2 };
			pShape = newPolygon2DShape(xs_tr, ys_tr);
			break;
		case Consts.PT_RECTANGLE: // 长方形
			pShape = new Rectangle2D.Double(x - radiusx, y - radiusy * 7 / 10,
					radiusx * 2, radiusy * 7 / 5);
			break;
		case Consts.PT_STAR: // 星形
			pShape = new Ellipse2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			break;
		case Consts.PT_DIAMOND: // 菱形
			double[] xs_di,
			ys_di;
			xs_di = new double[] { x - radiusx, x, x + radiusx, x };
			ys_di = new double[] { y, y - radiusy, y, y + radiusy };
			pShape = newPolygon2DShape(xs_di, ys_di);
			break;
		case Consts.PT_CORSS: // 叉形
			pShape = new Rectangle2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			break;
		case Consts.PT_PLUS: // 加号
			pShape = new Rectangle2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			break;
		case Consts.PT_D_CIRCEL: // 双圆
			pShape = new Ellipse2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			break;
		case Consts.PT_D_SQUARE: // 双正方形
			pShape = new Rectangle2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			break;
		case Consts.PT_D_TRIANGLE: // 双三角形
			double delta = radiusx * Math.cos(Math.toRadians(30));
			double[] xs_dtr1,
			ys_dtr1;
			xs_dtr1 = new double[] { x - delta, x, x + delta };
			ys_dtr1 = new double[] { y + radiusy / 2, y - radiusy,
					y + radiusy / 2 };
			pShape = newPolygon2DShape(xs_dtr1, ys_dtr1);
			break;
		case Consts.PT_D_RECTANGLE: // 双长方形
			pShape = new Rectangle2D.Double(x - radiusx, y - radiusy * 7 / 10,
					radiusx * 2, radiusy * 7 / 5);
			break;
		case Consts.PT_D_DIAMOND: // 双菱形
			double[] xs_ddi2,
			ys_ddi2;
			xs_ddi2 = new double[] { x - radiusx, x, x + radiusx, x };
			ys_ddi2 = new double[] { y, y - radiusy, y, y + radiusy };
			pShape = newPolygon2DShape(xs_ddi2, ys_ddi2);

			break;
		case Consts.PT_CIRCLE_PLUS: // 圆内加号
			pShape = new Ellipse2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			break;
		case Consts.PT_SQUARE_PLUS: // 方内加号
			pShape = new Rectangle2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			break;
		case Consts.PT_TRIANGLE_PLUS: // 三角内加号
			double[] xs_trp,
			ys_trp;
			xs_trp = new double[] { x - radiusx * Math.cos(Math.toRadians(30)),
					x, x + radiusx * Math.cos(Math.toRadians(30)) };
			ys_trp = new double[] { y + radiusy / 2, y - radiusy,
					y + radiusy / 2 };
			pShape = newPolygon2DShape(xs_trp, ys_trp);
			break;
		case Consts.PT_RECTANGLE_PLUS: // 长方形内加号
			pShape = new Rectangle2D.Double(x - radiusx, y - radiusy * 7 / 10,
					radiusx * 2, radiusy * 7 / 5);
			break;
		case Consts.PT_DIAMOND_PLUS: // 菱内加号
			double[] xs_dip,
			ys_dip;
			xs_dip = new double[] { x - radiusx, x, x + radiusx, x };
			ys_dip = new double[] { y, y - radiusy, y, y + radiusy };
			pShape = newPolygon2DShape(xs_dip, ys_dip);
			break;
		}
		return pShape;
	}

	/**
	 * 绘制直角坐标系下一个点的步骤1(图元之所以分步骤绘制用以防止背景色覆盖别的前景文字)
	 * @param g 图形设备
	 * @param point 点坐标
	 * @param shape 点的形状，取值参考Consts.PT_XXX
	 * @param radiusx x轴上的业务半径计算过后的像素单位，俗称为轴上的业务半径
	 * @param radiusy y轴上的业务半径计算过后的像素单位
	 * @param rw x，y轴上的业务半径都为0时，使用对称的半径像素单位，俗称为业务无关的像素半径
	 * @param style 边框风格
	 * @param weight 边框粗度 
	 * @param transparent 透明度
	 * @return 点的形状对象(用于计算超链接)
	 */
	public static Shape drawCartesianPoint1(Graphics2D g, Point2D point,
			int shape, double radiusx, double radiusy, double rw, int style,
			float weight, float transparent) {
		double x = point.getX();
		double y = point.getY();
		// 没有指定坐标值业务半径时，使用对称的像素半径rw radiusWeight:半径厚度，无业务半径时使用的对称半径
		if (radiusx == 0 && radiusy == 0) {
			radiusx = rw;
			radiusy = rw;
		}
		if (radiusx + radiusy == 0)
			return null;// 无论业务半径还是像素半径都为零时，不画点
		Point2D p1, p2;
		if (radiusx == 0) {// x坐标为0时，点型无效，画纵向直线
			p1 = new Point2D.Double(x, y - radiusy);
			p2 = new Point2D.Double(x, y + radiusy);
			return drawLine1(g, p1, p2, style, weight);
		}
		if (radiusy == 0) {// 画横向直线
			p1 = new Point2D.Double(x - radiusx, y);
			p2 = new Point2D.Double(x + radiusx, y);
			return drawLine1(g, p1, p2, style, weight);
		}

		Shape linkShape = null, pShape = null;
		int shadowShift = (int) (radiusx * 0.2);
		if (shadowShift < SHADE_SPAN) {
			shadowShift = SHADE_SPAN;
		}
		linkShape = getCPoint1ShapeArea(shape, x, y, radiusx, radiusy);
		x += shadowShift;
		y += shadowShift;
		pShape = getCPoint1ShapeArea(shape, x, y, radiusx, radiusy);
		if (pShape != null) {
			fillRadioGradientShape(g, pShape, Color.darkGray, Color.white,
					transparent / 2);// 阴影的透明度减半
		}
		return linkShape;
	}

	/**
	 * 获取镂空的形状
	 * 
	 * @param outer
	 *            ,大形状
	 * @param inner
	 *            ,镂空掉的小形状
	 * @return
	 */
	private static Shape getLouKongShape(Shape outer, Shape inner) {
		java.awt.geom.Area area = new java.awt.geom.Area(outer);
		java.awt.geom.Area sArea = new java.awt.geom.Area(inner);
		area.subtract(sArea);
		return area;
	}

	/**
	 * 绘制直角坐标系下一个点的步骤2
	 * @param g 图形设备
	 * @param point 点坐标
	 * @param shape 点的形状，取值参考Consts.PT_XXX
	 * @param radiusx x轴上的业务半径计算过后的像素单位，俗称为轴上的业务半径
	 * @param radiusy y轴上的业务半径计算过后的像素单位
	 * @param rw x，y轴上的业务半径都为0时，使用对称的半径像素单位，俗称为业务无关的像素半径
	 * @param style 边框风格
	 * @param weight 边框粗度 
	 * @param ccr 填充颜色 
	 * @param foreColor 前景色，边框颜色 
	 * @param transparent 透明度
	 * @return 点的形状对象(用于计算超链接)
	 */
	public static Shape drawCartesianPoint2(Graphics2D g, Point2D point,
			int shape, double radiusx, double radiusy, double rw, int style,
			float weight, ChartColor ccr, Color foreColor, float transparent) {
		double x = point.getX();
		double y = point.getY();
		if (rw == 0) {// rw为0表示采用线粗半径画点
			rw = weight / 2 + 1;
			if (rw < 4)
				rw = 3;
		}

		// 没有指定坐标值业务半径时，使用对称的像素半径radius
		if (radiusx == 0 && radiusy == 0) {
			radiusx = rw;
			radiusy = rw;
		}
		if (radiusx == radiusy) {// 对称半径时，不使用线粗属性，真正的线粗使用1
			weight = 1;
		}

		Point2D p1, p2;
		if (radiusx == 0) {// x坐标为0时，点型无效，画纵向直线
			p1 = new Point2D.Double(x, y - radiusy);
			p2 = new Point2D.Double(x, y + radiusy);
			drawLine2(g, p1, p2, ccr.color1, style, weight);
			return new Rectangle((int)(x-2),(int)(y-radiusy),4,(int)(2*radiusy));
		}
		if (radiusy == 0) {
			p1 = new Point2D.Double(x - radiusx, y);
			p2 = new Point2D.Double(x + radiusx, y);
			drawLine2(g, p1, p2, ccr.color1, style, weight);
			return new Rectangle((int)(x- radiusx),(int)(y-2),(int)(2*radiusx),4);
		}

		Shape fillShape = null, outerShape, innerShape;
		ArrayList<Shape> drawShapes = new ArrayList<Shape>();
		switch (shape) {
		case Consts.PT_NONE: // 无
			break;
		case Consts.PT_CIRCLE: // 圆
			fillShape = new Ellipse2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			break;
		case Consts.PT_DOT: // 实心点
			fillShape = new Ellipse2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			g.setColor(foreColor);
			fillPaint(g, fillShape, transparent);
			return fillShape;
		case Consts.PT_SQUARE: // 正方形
			fillShape = new Rectangle2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			break;
		case Consts.PT_TRIANGLE: // 三角形
			double[] xs_tr,
			ys_tr;
			xs_tr = new double[] { x - radiusx * Math.cos(Math.toRadians(30)),
					x, x + radiusx * Math.cos(Math.toRadians(30)) };
			ys_tr = new double[] { y + radiusy / 2, y - radiusy,
					y + radiusy / 2 };
			fillShape = newPolygon2DShape(xs_tr, ys_tr);
			break;
		case Consts.PT_RECTANGLE: // 长方形
			fillShape = new Rectangle2D.Double(x - radiusx, y - radiusy * 7
					/ 10, radiusx * 2, radiusy * 7 / 5);
			break;
		case Consts.PT_STAR: // 星形
			fillShape = new Ellipse2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			if (setStroke(g, foreColor, style, weight)) {
				double deltax = radiusx * Math.cos(Math.toRadians(45));
				double deltay = radiusy * Math.cos(Math.toRadians(45));
				drawShapes
						.add(new Line2D.Double(x, y - radiusy, x, y + radiusy));
				drawShapes.add(new Line2D.Double(x - deltax, y - deltay, x
						+ deltax, y + deltay));
				drawShapes.add(new Line2D.Double(x - deltax, y + deltay, x
						+ deltax, y - deltay));
			}
			break;
		case Consts.PT_DIAMOND: // 菱形
			double[] xs_di,
			ys_di;
			xs_di = new double[] { x - radiusx, x, x + radiusx, x };
			ys_di = new double[] { y, y - radiusy, y, y + radiusy };
			fillShape = newPolygon2DShape(xs_di, ys_di);
			break;
		case Consts.PT_CORSS: // 叉形
			fillShape = new Rectangle2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			if (setStroke(g, foreColor, style, weight)) {
				double deltax = radiusx * Math.cos(Math.toRadians(45));
				double deltay = radiusy * Math.cos(Math.toRadians(45));
				drawShapes.add(new Line2D.Double(x - deltax, y - deltay, x
						+ deltax, y + deltay));
				drawShapes.add(new Line2D.Double(x - deltax, y + deltay, x
						+ deltax, y - deltay));
			}
			break;
		case Consts.PT_PLUS: // 加号
			fillShape = new Rectangle2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			if (setStroke(g, foreColor, style, weight)) {
				drawShapes
						.add(new Line2D.Double(x - radiusx, y, x + radiusx, y));
				drawShapes
						.add(new Line2D.Double(x, y + radiusy, x, y - radiusy));
			}
			break;
		case Consts.PT_D_CIRCEL: // 双圆
			outerShape = new Ellipse2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			innerShape = new Ellipse2D.Double(x - radiusx / 2, y - radiusy / 2,
					radiusx, radiusy);
			fillShape = getLouKongShape(outerShape, innerShape);
			break;
		case Consts.PT_D_SQUARE: // 双正方形
			outerShape = new Rectangle2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			innerShape = new Rectangle2D.Double(x - radiusx / 2, y - radiusy
					/ 2, radiusx, radiusy);
			fillShape = getLouKongShape(outerShape, innerShape);
			break;
		case Consts.PT_D_TRIANGLE: // 双三角形
			double delta = radiusx * Math.cos(Math.toRadians(30));
			double[] xs_dtr1,
			ys_dtr1;
			xs_dtr1 = new double[] { x - delta, x, x + delta };
			ys_dtr1 = new double[] { y + radiusy / 2, y - radiusy,
					y + radiusy / 2 };
			outerShape = newPolygon2DShape(xs_dtr1, ys_dtr1);

			double[] xs_dtr2 = {
					x - radiusx / 2 * Math.cos(Math.toRadians(30)), x,
					x + radiusx / 2 * Math.cos(Math.toRadians(30)) };
			double[] ys_dtr2 = { y + radiusy / 4, y - radiusy / 2,
					y + radiusy / 4 };
			innerShape = newPolygon2DShape(xs_dtr2, ys_dtr2);
			fillShape = getLouKongShape(outerShape, innerShape);
			break;
		case Consts.PT_D_RECTANGLE: // 双长方形
			outerShape = new Rectangle2D.Double(x - radiusx, y - radiusy * 7
					/ 10, radiusx * 2, radiusy * 7 / 5);
			innerShape = new Rectangle2D.Double(x - radiusx / 2, y - radiusy
					* 7 / 20, radiusx, radiusy * 7 / 10);
			fillShape = getLouKongShape(outerShape, innerShape);
			break;
		case Consts.PT_D_DIAMOND: // 双菱形
			double[] xs_ddi2,
			ys_ddi2;
			xs_ddi2 = new double[] { x - radiusx, x, x + radiusx, x };
			ys_ddi2 = new double[] { y, y - radiusy, y, y + radiusy };
			outerShape = newPolygon2DShape(xs_ddi2, ys_ddi2);
			double[] xs_ddi1 = { x - radiusx / 2, x, x + radiusx / 2, x };
			double[] ys_ddi1 = { y, y - radiusy / 2, y, y + radiusy / 2 };
			innerShape = newPolygon2DShape(xs_ddi1, ys_ddi1);
			fillShape = getLouKongShape(outerShape, innerShape);
			break;
		case Consts.PT_CIRCLE_PLUS: // 圆内加号
			fillShape = new Ellipse2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			if (setStroke(g, foreColor, style, weight)) {
				drawShapes.add(fillShape);
				drawShapes
						.add(new Line2D.Double(x, y - radiusy, x, y + radiusy));
				drawShapes
						.add(new Line2D.Double(x - radiusx, y, x + radiusx, y));
			}
			break;
		case Consts.PT_SQUARE_PLUS: // 方内加号
			fillShape = new Rectangle2D.Double(x - radiusx, y - radiusy,
					radiusx * 2, radiusy * 2);
			if (setStroke(g, foreColor, style, weight)) {
				drawShapes.add(fillShape);
				drawShapes
						.add(new Line2D.Double(x, y - radiusy, x, y + radiusy));
				drawShapes
						.add(new Line2D.Double(x - radiusx, y, x + radiusx, y));
			}
			break;
		case Consts.PT_TRIANGLE_PLUS: // 三角内加号
			double[] xs_trp,
			ys_trp;
			xs_trp = new double[] { x - radiusx * Math.cos(Math.toRadians(30)),
					x, x + radiusx * Math.cos(Math.toRadians(30)) };
			ys_trp = new double[] { y + radiusy / 2, y - radiusy,
					y + radiusy / 2 };
			fillShape = newPolygon2DShape(xs_trp, ys_trp);
			if (setStroke(g, foreColor, style, weight)) {
				drawShapes.add(fillShape);
				drawShapes.add(new Line2D.Double(x, y - radiusy, x, y + radiusy
						/ 2));
				drawShapes.add(new Line2D.Double(x - radiusx
						* Math.tan(Math.toRadians(30)), y, x + radiusx
						* Math.tan(Math.toRadians(30)), y));
			}
			break;
		case Consts.PT_RECTANGLE_PLUS: // 长方形内加号
			fillShape = new Rectangle2D.Double(x - radiusx, y - radiusy * 7
					/ 10, radiusx * 2, radiusy * 7 / 5);
			if (setStroke(g, foreColor, style, weight)) {
				drawShapes.add(fillShape);
				drawShapes.add(new Line2D.Double(x, y - radiusy * 7 / 10, x, y
						+ radiusy * 7 / 10));
				drawShapes
						.add(new Line2D.Double(x - radiusx, y, x + radiusx, y));
			}
			break;
		case Consts.PT_DIAMOND_PLUS: // 菱内加号
			double[] xs_dip,
			ys_dip;
			xs_dip = new double[] { x - radiusx, x, x + radiusx, x };
			ys_dip = new double[] { y, y - radiusy, y, y + radiusy };
			fillShape = newPolygon2DShape(xs_dip, ys_dip);
			if (setStroke(g, foreColor, style, weight)) {
				drawShapes.add(fillShape);
				drawShapes
						.add(new Line2D.Double(x - radiusx, y, x + radiusx, y));
				drawShapes
						.add(new Line2D.Double(x, y - radiusy, x, y + radiusy));
			}
			break;
		}
		if (fillShape != null) {
			if (setPointPaint(g, ccr, fillShape)) {
				fillPaint(g, fillShape, transparent);
			}
			if (setStroke(g, foreColor, style, weight)) {
				if (drawShapes.isEmpty()) {
					g.draw(fillShape);
				} else {
					for (int i = 0; i < drawShapes.size(); i++) {
						Shape s = drawShapes.get(i);
						g.draw(s);
					}
				}
			}
		}
		return fillShape;
	}

	private static Shape getPPoint1ShapeArea(int shape, PolarCoor pc, double r,
			double a, double radiusR, double radiusA, int shadowShift) {
		Shape pShape = null;
		Rectangle2D bound;
		Arc2D arc;
		Point2D polarDot, p1, p2;

		switch (shape) {
		case Consts.PT_NONE: // 无
			break;
		case Consts.PT_RECTANGLE: // 长方形,极坐标下，长方形，正方形同样处理
		case Consts.PT_SQUARE: // 正方形,极坐标下演变为一段环
			bound = pc.getEllipseBounds(r + radiusR);
			arc = new Arc2D.Double(bound.getX() + shadowShift, bound.getY()
					+ shadowShift, bound.getWidth(), bound.getHeight(), a
					- radiusA, radiusA * 2, Arc2D.PIE);
			java.awt.geom.Area bigArea = new java.awt.geom.Area(arc);
			if (r > radiusR) {
				bound = pc.getEllipseBounds(r - radiusR);
				arc = new Arc2D.Double(bound.getX() + shadowShift, bound.getY()
						+ shadowShift, bound.getWidth(), bound.getHeight(), a
						- radiusA - 5, radiusA * 2 + 10, Arc2D.PIE);// 小扇形多10度，扇形误差后仍能减干净
				java.awt.geom.Area sArea = new java.awt.geom.Area(arc);
				bigArea.subtract(sArea);
			}// else 极轴坐标小于等于半径时，直接画扇形
			pShape = bigArea;
			break;
		case Consts.PT_DIAMOND: // 菱形，此时的菱形为环中点的变形菱形
			double[] xs_di,
			ys_di;
			xs_di = new double[4];
			ys_di = new double[4];
			polarDot = new Point2D.Double(r + radiusR, a);
			p1 = pc.getScreenPoint(polarDot);
			xs_di[0] = p1.getX();
			ys_di[0] = p1.getY();

			polarDot = new Point2D.Double(r, a + radiusA);
			p1 = pc.getScreenPoint(polarDot);
			xs_di[1] = p1.getX();
			ys_di[1] = p1.getY();

			polarDot = new Point2D.Double(r - radiusR, a);
			p1 = pc.getScreenPoint(polarDot);
			xs_di[2] = p1.getX();
			ys_di[2] = p1.getY();

			polarDot = new Point2D.Double(r, a - radiusA);
			p1 = pc.getScreenPoint(polarDot);
			xs_di[3] = p1.getX();
			ys_di[3] = p1.getY();

			pShape = newPolygon2DShape(xs_di, ys_di);
			break;
		case Consts.PT_CIRCLE: // 圆
		case Consts.PT_TRIANGLE: // 三角形
		case Consts.PT_STAR: // 星形
		case Consts.PT_CORSS: // 叉形
		case Consts.PT_PLUS: // 加号
		case Consts.PT_D_CIRCEL: // 双圆
		case Consts.PT_D_SQUARE: // 双正方形
		case Consts.PT_D_TRIANGLE: // 双三角形
		case Consts.PT_D_RECTANGLE: // 双长方形
		case Consts.PT_D_DIAMOND: // 双菱形
		case Consts.PT_CIRCLE_PLUS: // 圆内加号
		case Consts.PT_SQUARE_PLUS: // 方内加号
		case Consts.PT_TRIANGLE_PLUS: // 三角内加号
		case Consts.PT_RECTANGLE_PLUS: // 长方形内加号
		case Consts.PT_DIAMOND_PLUS: // 菱内加号
			throw new RuntimeException("Unsupportted dot shape:" + shape
					+ " in polar coordinate system.");
		}
		return pShape;
	}

	/**
	 * 绘制极坐标系下一个点的步骤1
	 * @param g 图形设备
	 * @param point 点坐标(极坐标)
	 * @param shape 点的形状，取值参考Consts.PT_XXX
	 * @param radiusR 极轴上的业务半径计算过后的像素单位，俗称为轴上的业务半径
	 * @param radiusA 角轴上的业务半径
	 * @param rw 没有指定坐标值业务半径时，使用对称的像素半径rw，无业务半径时使用的对称半径
	 * 			  极坐标下无业务半径时，点绘制同直角坐标系。
	 * @param style 边框风格
	 * @param weight 边框粗度 
	 * @param pc 极坐标系 
	 * @param transparent 透明度
	 * @return 点的形状对象(用于计算超链接)
	 */
	public static Shape drawPolarPoint1(Graphics2D g, Point2D point, int shape,
			double radiusR, double radiusA, double rw, int style, float weight,
			PolarCoor pc, float transparent) {
		if (radiusR == 0 && radiusA == 0) {
			return drawCartesianPoint1(g, point, shape, 0, 0, rw, style,
					weight, transparent);
		}
		double r = point.getX();
		double a = point.getY();
		Rectangle2D bound;
		Point2D polarDot, p1, p2;
		Arc2D arc, arcSmall;
		java.awt.geom.Area arcArea, arcSmallArea;
		int shadowShift = (int) (radiusR * 0.2), dLinkShift = 1;
		if (shadowShift < SHADE_SPAN) {
			shadowShift = SHADE_SPAN;
		}
		if (radiusR == 0) {// radiusR为0时，点型无效，为一段弧线
			Color dark = getShadeColor(1);
			bound = pc.getEllipseBounds(r);
			arc = new Arc2D.Double(bound.getX() + shadowShift, bound.getY()
					+ shadowShift, bound.getWidth(), bound.getHeight(), a
					- radiusA, radiusA * 2, Arc2D.OPEN);
			if (setStroke(g, dark, style, weight)) {
				g.draw(arc);
			}
			arc = new Arc2D.Double(bound.getX() - dLinkShift, bound.getY()
					- dLinkShift, bound.getWidth() + dLinkShift * 2,
					bound.getHeight() + dLinkShift * 2, a - radiusA,
					radiusA * 2, Arc2D.PIE);

			arcSmall = new Arc2D.Double(bound.getX() + dLinkShift, bound.getY()
					+ dLinkShift, bound.getWidth() - dLinkShift * 2,
					bound.getHeight() - dLinkShift * 2, a - radiusA - 10,
					radiusA * 2 + 5, Arc2D.PIE);
			arcArea = new java.awt.geom.Area(arc);
			arcSmallArea = new java.awt.geom.Area(arcSmall);
			arcArea.subtract(arcSmallArea);
			return arcArea;
		} else if (radiusA == 0) {// radiusA为0时，为一段半径直线
			polarDot = new Point2D.Double(r + radiusR, a);
			p1 = pc.getScreenPoint(polarDot);
			polarDot = new Point2D.Double(r - radiusR, a);
			p2 = pc.getScreenPoint(polarDot);
			return drawLine1(g, p1, p2, style, weight);
		}

		Shape pShape = getPPoint1ShapeArea(shape, pc, r, a, radiusR, radiusA,
				shadowShift);
		if (pShape != null) {
			fillRadioGradientShape(g, pShape, Color.darkGray, Color.white,
					transparent / 2);
		}

		Shape linkShape = getPPoint1ShapeArea(shape, pc, r, a, radiusR,
				radiusA, 0);
		return linkShape;
	}

	/**
	 * 绘制极坐标系下一个点的步骤2
	 * @param g 图形设备
	 * @param point 点坐标(数值坐标，此处为极坐标)
	 * @param shape 点的形状，取值参考Consts.PT_XXX
	 * @param radiusR 极轴上的业务半径计算过后的像素单位，俗称为轴上的业务半径
	 * @param radiusA 角轴上的业务半径
	 * @param rw 没有指定坐标值业务半径时，使用对称的像素半径rw，无业务半径时使用的对称半径
	 * 			  极坐标下无业务半径时，点绘制同直角坐标系。
	 * @param style 边框风格
	 * @param weight 边框粗度 
	 * @param pc 极坐标系 
	 * @param ccr 填充颜色 
	 * @param foreColor 边框颜色 
	 * @param transparent 透明度
	 * @return 点的形状对象(用于计算超链接)
	 */
	public static void drawPolarPoint2(Graphics2D g, Point2D point, int shape,
			double radiusR, double radiusA, double rw, int style, float weight,
			PolarCoor pc, ChartColor ccr, Color foreColor, float transparent) {
		// 没有指定坐标值业务半径时，使用对称的像素半径rw radiusWeight:半径厚度，无业务半径时使用的对称半径
		// 极坐标下无业务半径时，点绘制同直角坐标系。
		if (radiusR == 0 && radiusA == 0) {
			point = pc.getScreenPoint(point);
			drawCartesianPoint2(g, point, shape, 0, 0, rw, style, weight, ccr,
					foreColor, transparent);
			return;
		}
		double r = point.getX();
		double a = point.getY();
		Rectangle2D bound;
		Point2D polarDot, p1, p2;
		Arc2D arc;
		if (radiusR == 0) {// radiusR为0时，点型无效，为一段弧线
			bound = pc.getEllipseBounds(r);
			arc = new Arc2D.Double(bound.getX(), bound.getY(),
					bound.getWidth(), bound.getHeight(), a - radiusA,
					radiusA * 2, Arc2D.OPEN);
			if (setStroke(g, foreColor, style, weight)) {
				g.draw(arc);
			}
			return;
		} else if (radiusA == 0) {// radiusA为0时，为一段半径直线
			polarDot = new Point2D.Double(r + radiusR, a);
			p1 = pc.getScreenPoint(polarDot);
			polarDot = new Point2D.Double(r - radiusR, a);
			p2 = pc.getScreenPoint(polarDot);
			drawLine2(g, p1, p2, foreColor, style, weight);
			return;
		}

		Shape pShape = null;
		switch (shape) {
		case Consts.PT_NONE: // 无
			break;
		case Consts.PT_RECTANGLE: // 长方形,极坐标下，长方形，正方形同样处理
		case Consts.PT_SQUARE: // 正方形,极坐标下演变为一段环
			Rectangle2D bigBounds = pc.getEllipseBounds(r + radiusR);
			Rectangle2D smallBounds = pc.getEllipseBounds(r - radiusR);
			draw2DRing(g, bigBounds, smallBounds, a - radiusA, radiusA * 2,
					foreColor, style, weight, transparent, ccr);
			return;
		case Consts.PT_DIAMOND: // 菱形，此时的菱形为环中点的变形菱形
			double[] xs_di,
			ys_di;
			xs_di = new double[4];
			ys_di = new double[4];
			polarDot = new Point2D.Double(r + radiusR, a);
			p1 = pc.getScreenPoint(polarDot);
			xs_di[0] = p1.getX();
			ys_di[0] = p1.getY();

			polarDot = new Point2D.Double(r, a + radiusA);
			p1 = pc.getScreenPoint(polarDot);
			xs_di[1] = p1.getX();
			ys_di[1] = p1.getY();

			polarDot = new Point2D.Double(r - radiusR, a);
			p1 = pc.getScreenPoint(polarDot);
			xs_di[2] = p1.getX();
			ys_di[2] = p1.getY();

			polarDot = new Point2D.Double(r, a - radiusA);
			p1 = pc.getScreenPoint(polarDot);
			xs_di[3] = p1.getX();
			ys_di[3] = p1.getY();

			pShape = newPolygon2DShape(xs_di, ys_di);
			break;
		case Consts.PT_CIRCLE: // 圆
		case Consts.PT_TRIANGLE: // 三角形
		case Consts.PT_STAR: // 星形
		case Consts.PT_CORSS: // 叉形
		case Consts.PT_PLUS: // 加号
		case Consts.PT_D_CIRCEL: // 双圆
		case Consts.PT_D_SQUARE: // 双正方形
		case Consts.PT_D_TRIANGLE: // 双三角形
		case Consts.PT_D_RECTANGLE: // 双长方形
		case Consts.PT_D_DIAMOND: // 双菱形
		case Consts.PT_CIRCLE_PLUS: // 圆内加号
		case Consts.PT_SQUARE_PLUS: // 方内加号
		case Consts.PT_TRIANGLE_PLUS: // 三角内加号
		case Consts.PT_RECTANGLE_PLUS: // 长方形内加号
		case Consts.PT_DIAMOND_PLUS: // 菱内加号
			throw new RQException("Unsupportted dot shape:" + shape
					+ " in polar coordinate system.");
		}
		if (pShape != null) {
			Rectangle bd = pShape.getBounds();
			if (setPointPaint(g, ccr, pShape)) {
				g.fill(pShape);
			}
			if (setStroke(g, foreColor, style, weight)) {
				g.draw(pShape);
			}
		}
	}

	/**
	 * 图元的参数如果是Para类型时，也需要设置引擎环境
	 * @param chartElement 图元
	 */
	public static void setParamsEngine(IElement chartElement) {
		try {
			Field[] fields = chartElement.getClass().getFields();
			int size = fields.length;
			for (int i = 0; i < size; i++) {
				Field f = fields[i];
				Object param = f.get(chartElement);
				if (!(param instanceof Para)) {
					continue;
				}
				((Para) param).setEngine(chartElement.getEngine());
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	private static ThreadLocal<Boolean> tlIsGif = new ThreadLocal<Boolean>() {
		protected synchronized Boolean initialValue() {
			return Boolean.FALSE;
		}
	};

	/**
	 * 线程安全下的静态值，GIF格式颜色不能太多，不能使用文本的光滑效果
	 * 用此开关来控制图形的绘制模式
	 */
	public static void setIsGif(boolean isGif) {
		tlIsGif.set( isGif );
	}

	/**
	 * 是否线程安全下的GIF格式
	 */
	public static boolean isGif() {
		return (tlIsGif.get()).booleanValue();
	}

	// 为了保证圆弧的圆滑，以及文字的不使用锯齿的清晰状态，使用如下开关来区分画文字和圆弧的条件；
	// 但是gif格式时，不能使用抗锯齿属性，会造成颜色过多。
//	目前的用法是绘图前就将平滑打开，然后在绘制文本时，先关掉，画完文本后，再打开，保证图形平滑，文字清晰。
	public static void setGraphAntiAliasingOn(Graphics2D g) {
		// gif格式时不能打开锯齿开关，以防颜色过多；
		if (isGif()) {//gif格式时，总是关闭，此处强行关闭，以防同线程中别的格式已经设为on
			setGraphAntiAliasingOff(g);
			return;
		}
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
	}

	public static void setGraphAntiAliasingOff(Graphics2D g) {
//		if (isGif())
//			return;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF);
	}

	/**
	 * 将序列seq对象描述的填充颜色转换为对应的 类实例
	 * @param seq 属性序列
	 * @return ChartColor，填充颜色实例，转换失败(比如错误的格式)时返回null
	 */
	public static ChartColor sequenceToChartColor(Sequence seq) {
		if (seq.length() < 1)
			return null;
		Object prefix = seq.get(1);
		if (StringUtils.isValidString(prefix)) {
			prefix = Escape.removeEscAndQuote((String) prefix);
			if (prefix.equals("ChartColor")) {
				return ChartColor.getInstance(seq);
			}
		}
		return null;
	}

	/**
	 * 文本串描述的逻辑数据，采用逗号 (必须为英文逗号) 分隔分类和系列
	 * 该方法获取其中的分类值
	 * (比如值为   "张三,语文" 时，返回分类值  "张三")
	 * @param data 带格式的逻辑数据
	 * @return 分类值
	 */
	public static Object parseCategory(Object data) {
		if (data == null) {
			return null;
		}
		if (!(data instanceof String)) {
			return data;
		}
		String val = (String) data;
		int commaIndex = val.indexOf(",");
		if (commaIndex > -1) {
			return val.substring(0, commaIndex);
		}
		return val;
	}

	/**
	 * 文本串描述的逻辑数据，采用逗号 (必须为英文逗号) 分隔分类和系列
	 * 该方法获取其中的系列值，如果没有逗号分隔的数据，则表示没有系列
	 * (比如值为   "张三,语文" 时，返回系列值  "语文")
	 * @param data 带格式的逻辑数据
	 * @return 系列值
	 */
	public static Object parseSeries(Object data) {
		if (data == null) {
			return null;
		}
		if (!(data instanceof String)) {
			return data;
		}

		String val = (String) data;
		int commaIndex = val.indexOf(",");
		if (commaIndex > -1) {
			return val.substring(commaIndex + 1);
		}

		if (val.indexOf(",") > -1) {
			StringTokenizer st = new StringTokenizer(val, ",");
			st.nextToken();
			String ser = st.nextToken();
			return ser;
		}
		return null;
	}

	/**
	 * 找出当前的分类catName和系列serName在枚举enumData序列中的索引序号
	 * @param enumData 带格式的逻辑数据
	 * @param catName 分类名称
	 * @param serName 系列名称
	 * @return int 序号(从1开始)
	 */
	public static int indexOf(Sequence enumData, String catName, String serName) {
		if (catName == null)
			return 0;
		String tmp = catName;
		if (serName != null) {
			tmp += "," + serName;
		}
		return enumData.firstIndexOf(tmp);
	}

	/**
	 * 统计分类catName的汇总值
	 * @param catName 分类名称
	 * @param enumData 枚举值的数据序列
	 * @param numData 数值的数据序列
	 * @return 汇总值
	 */
	public static double sumCategory(String catName, Sequence enumData,
			Sequence numData) {
		double sum = 0;
		int size = enumData.length();
		for (int i = 1; i <= size; i++) {
			Object enumObj = enumData.get(i);
			Object cat = parseCategory(enumObj);
			if (catName.equals(cat)) {
				double d = ((Number) numData.get(i)).doubleValue();
				// 累积值不处理负数
				if (d > 0) {
					sum += d;
				}
			}
		}
		return sum;
	}

	/**
	 * 按照指定参数绘制圆柱体的顶面
	 * @param g 图形设备
	 * @param topOval 顶面圆
	 * @param bc 边框颜色
	 * @param bs 边框风格
	 * @param bw 边框粗度
	 * @param transparent 透明度
	 * @param chartColor 填充颜色
	 * @param isVertical 是否竖向圆柱
	 */
	public static void drawCylinderTop(Graphics2D g, Arc2D.Double topOval,
			Color bc, int bs, float bw, float transparent,
			ChartColor chartColor, boolean isVertical) {
		Rectangle bound = topOval.getBounds();
		CubeColor ccr = new CubeColor(chartColor.getColor1());
		// 顶面圆只要渐变色就使用炫效果
		if (chartColor.isDazzle()) {
			if (ccr.getT1() != null && ccr.getT2() != null) {
				GradientPaint gp;
				if (isVertical) {
					gp = new GradientPaint(bound.x, bound.y, ccr.getT2(),
							bound.x + bound.width / 2, bound.y + bound.height
									/ 2, ccr.getT1(), true);
				} else {
					gp = new GradientPaint(bound.x + bound.width, bound.y,
							ccr.getT2(), bound.x + bound.width / 2, bound.y
									+ bound.height / 2, ccr.getT1(), true);
				}
				g.setPaint(gp);
				Utils.fillPaint(g, topOval, transparent);
			}
		} else if (chartColor.isGradient()) {
			ChartColor tmpcc = new ChartColor();
			tmpcc.setAngle(180);
			tmpcc.setGradient(true);
			tmpcc.setColor1(ccr.getT1());
			tmpcc.setColor2(ccr.getT2());
			if (Utils.setPaint(g, bound.x, bound.y, bound.width, bound.height,
					tmpcc)) {
				Utils.fillPaint(g, topOval, transparent);
			}
		} else {
			// g.setColor(ccr.getOrigin());// ccr.getF1());
			Utils.fill(g, topOval, transparent, ccr.getOrigin());
		}
		if (Utils.setStroke(g, bc, bs, bw)) {
			g.draw(topOval);
		}
	}

	/**
	 * 按照指定参数绘制圆柱体的正面
	 * @param g 图形设备
	 * @param front 正面区域
	 * @param bc 边框颜色
	 * @param bs 边框风格
	 * @param bw 边框粗度
	 * @param transparent 透明度
	 * @param chartColor 填充颜色
	 * @param isVertical 是否竖向圆柱
	 */
	public static void drawCylinderFront(Graphics2D g,
			java.awt.geom.Area front, Color bc, int bs, float bw,
			float transparent, ChartColor chartColor, boolean isVertical) {
		drawCylinderFront(g, front, bc, bs, bw, transparent, chartColor,
				isVertical, null, false);
	}

	/**
	 * 按照指定参数绘制圆柱体的正面
	 * @param g 图形设备
	 * @param front 正面区域
	 * @param bc 边框颜色
	 * @param bs 边框风格
	 * @param bw 边框粗度
	 * @param transparent 透明度
	 * @param chartColor 填充颜色
	 * @param isVertical 是否竖向圆柱
	 * @param shinningRange 炫效果区域
	 * @param isCurve 是否曲面
	 */
	public static void drawCylinderFront(Graphics2D g,
			java.awt.geom.Area front, Color bc, int bs, float bw,
			float transparent, ChartColor chartColor, boolean isVertical,
			Rectangle2D shinningRange, boolean isCurve) {
		double x1, y1, x2, y2;
		CubeColor ccr = new CubeColor(chartColor.getColor1());
		Rectangle bound = front.getBounds();
		GradientPaint paint;
		Color c1, c2;
		// 炫效果
		if (chartColor.isDazzle()) {
			Rectangle2D r2d = shinningRange;
			if (r2d == null) {
				r2d = bound.getBounds2D();
			}
			java.awt.geom.Area tmp;
			if (isVertical) {
				tmp = new java.awt.geom.Area(new Rectangle2D.Double(r2d.getX(),
						r2d.getY(), r2d.getWidth() * 2 / 3, r2d.getHeight()));

			} else {
				tmp = new java.awt.geom.Area(new Rectangle2D.Double(r2d.getX(),
						r2d.getY(), r2d.getWidth(), r2d.getHeight() * 2 / 3));

			}
			java.awt.geom.Area leftOrTop = (java.awt.geom.Area) front.clone();
			leftOrTop.intersect(tmp);
			if (!leftOrTop.isEmpty()) {
				bound = tmp.getBounds();
				if (isVertical) {
					x1 = bound.x;
					y1 = bound.y;
					x2 = bound.x + bound.width / 2;
					y2 = y1;
				} else {
					x1 = bound.x;
					y1 = bound.y;
					x2 = x1;
					y2 = y1 + bound.height / 2;
				}
				if (isCurve) {
					c1 = ccr.getRelativeBrighter("T2", 1);
					c2 = ccr.getRelativeBrighter("T1", 1);
				} else {
					c1 = ccr.getT2();
					c2 = ccr.getT1();
				}
				if (c1 != null && c2 != null) {
					paint = new GradientPaint((int) x1, (int) y1, c1, (int) x2,
							(int) y2, c2, true);

					g.setPaint(paint);
					Utils.fillPaint(g, leftOrTop, transparent);
				}
			}

			java.awt.geom.Area rightOrBottom = (java.awt.geom.Area) front
					.clone();
			double d;
			if (isVertical) {
				d = r2d.getWidth() * 2 / 3;
				tmp = new java.awt.geom.Area(new Rectangle2D.Double(r2d.getX()
						+ d - 1, r2d.getY(), r2d.getBounds().width - d + 1,
						r2d.getHeight()));// 由于四舍五入的误差，让右边往左多包含一个像素
			} else {
				d = r2d.getHeight() * 2 / 3;
				tmp = new java.awt.geom.Area(new Rectangle2D.Double(r2d.getX(),
						r2d.getY() + d - 1, r2d.getWidth(),
						r2d.getBounds().height - d + 1));
			}

			rightOrBottom.intersect(tmp);
			if (!rightOrBottom.isEmpty()) {
				bound = tmp.getBounds();
				if (isVertical) {
					x1 = bound.x;
					y1 = bound.y;
					x2 = bound.x + bound.width * 0.6f;
					y2 = y1;
				} else {
					x1 = bound.x;
					y1 = bound.y;
					x2 = x1;
					y2 = bound.y + bound.height * 0.6f;
				}
				if (isCurve) {
					c1 = ccr.getRelativeBrighter("T2", 1);
					c2 = ccr.getRelativeBrighter("F2", 1);
				} else {
					c1 = ccr.getT2();
					c2 = ccr.getF2();
				}
				if (c1 != null && c2 != null) {
					paint = new GradientPaint((int) x1, (int) y1, c1, (int) x2,
							(int) y2, c2, false);
					g.setPaint(paint);
					Utils.fillPaint(g, rightOrBottom, transparent);
				}
			}
		} else {
			if (Utils.setPaint(g, bound.x, bound.y, bound.width, bound.height,
					chartColor)) {
				Utils.fillPaint(g, front, transparent);
			}
		}

		// 柱子的边框
		if (Utils.setStroke(g, bc, bs, bw)) {
			g.draw(front);
		}
	}

	/**
	 * 绘制极坐标系下的文本
	 * @param e 绘图引擎
	 * @param txt 文本
	 * @param pc 极坐标系
	 * @param txtPolarPoint 极坐标点
	 * @param fontName 字体
	 * @param fontStyle 风格
	 * @param fontSize 字号
	 * @param c 前景色
	 * @param textOverlapping 允许文本重叠
	 */
	public static void drawPolarPointText(Engine e, String txt, PolarCoor pc,
			Point2D txtPolarPoint, String fontName, int fontStyle,
			int fontSize, Color c, boolean textOverlapping) {
		drawPolarPointText(e, txt, pc, txtPolarPoint, fontName, fontStyle,
				fontSize, c, textOverlapping, 0);

	}

	/**
	 * 绘制极坐标系下的文本
	 * @param e 绘图引擎
	 * @param txt 文本
	 * @param pc 极坐标系
	 * @param txtPolarPoint 极坐标点
	 * @param fontName 字体
	 * @param fontStyle 风格
	 * @param fontSize 字号
	 * @param c 前景色
	 * @param textOverlapping 允许文本重叠
	 * @param specifiedLocation 相对中心点方位
	 */
	public static void drawPolarPointText(Engine e, String txt, PolarCoor pc,
			Point2D txtPolarPoint, String fontName, int fontStyle,
			int fontSize, Color c, boolean textOverlapping,
			int specifiedLocation) {
		if (!StringUtils.isValidString(txt)) {
			return;
		}
		double angle = txtPolarPoint.getY();
		int locationType = Consts.LOCATION_CM;
		if (specifiedLocation > 0) {
			locationType = specifiedLocation;
		} else {
			locationType = getAngleTextLocation(angle);
		}
		Font font = getFont(fontName, fontStyle, fontSize);
		Point2D txtP = pc.getScreenPoint(txtPolarPoint);
		drawText(e, txt, txtP.getX(), txtP.getY(), font, c, null, fontStyle, 0,
				locationType, textOverlapping);

	}

	/**
	 * 指定参数绘制平面环
	 * @param g 图形设备
	 * @param bigBounds 外环边界
	 * @param smallBounds 内环边界
	 * @param start 起始角度
	 * @param extent 角度范围
	 * @param borderColor 边框颜色
	 * @param borderStyle 边框风格
	 * @param borderWeight 边框粗度
	 * @param transparent 透明度
	 * @param fillColor 填充颜色
	 * @return 对应环弧的形状
	 */
	public static Shape draw2DRing(Graphics2D g, Rectangle2D bigBounds,
			Rectangle2D smallBounds, double start, double extent,
			Color borderColor, int borderStyle, float borderWeight,
			float transparent, ChartColor fillColor) {
		return draw2DRing(g, bigBounds, smallBounds, start, extent,
				borderColor, borderStyle, borderWeight, transparent, fillColor,
				false);
	}

	/**
	 * 指定参数绘制平面环
	 * @param g 图形设备
	 * @param bigBounds 外环边界
	 * @param smallBounds 内环边界
	 * @param start 起始角度
	 * @param extent 角度范围
	 * @param borderColor 边框颜色
	 * @param borderStyle 边框风格
	 * @param borderWeight 边框粗度
	 * @param transparent 透明度
	 * @param fillColor 填充颜色
	 * @param fillAsPie 内半径为0,且只有一个分类时，环退化为扇，直接画扇形，由上层程序告诉是否只有一个分类
	 * @return 对应环弧的形状
	 */
	public static Shape draw2DRing(Graphics2D g, Rectangle2D bigBounds,
			Rectangle2D smallBounds, double start, double extent,
			Color borderColor, int borderStyle, float borderWeight,
			float transparent, ChartColor fillColor, boolean fillAsPie) {
		double x, y, w, h;
		x = (bigBounds.getX() + smallBounds.getX()) / 2;
		y = (bigBounds.getY() + smallBounds.getY()) / 2;
		w = (bigBounds.getWidth() + smallBounds.getWidth()) / 2;
		h = (bigBounds.getHeight() + smallBounds.getHeight()) / 2;
		Rectangle2D midBounds = new Rectangle2D.Double(x, y, w, h);

		// 小的圆弧角域增加10度，防止角域相同时，计算精度问题会减不净，留下一条直线等状态
		Arc2D smallSector = new Arc2D.Double(smallBounds, start - 5,
				extent + 10, Arc2D.PIE);
		Arc2D bigSector = new Arc2D.Double(bigBounds, start, extent, Arc2D.PIE);
		java.awt.geom.Area ring = new java.awt.geom.Area(bigSector);
		ring.subtract(new java.awt.geom.Area(smallSector));

		Rectangle rect = bigBounds.getBounds();
		ChartColor cc = fillColor;
		double endAngle = start + extent;

		if (cc.isDazzle()) {
			Color dazzel = CubeColor.getDazzelColor(cc.getColor1());
			CubeColor ccr = new CubeColor(dazzel);
			if (fillAsPie) {
				if (ccr.getF1() != null && ccr.getF2() != null) {
					GradientPaint gp = new GradientPaint(rect.x, rect.y
							+ rect.height, ccr.getF2(),
							rect.x + rect.width / 2, rect.y + rect.height / 2,
							ccr.getF1(), true);
					g.setPaint(gp);
					Utils.fillPaint(g, ring, transparent);
				}
			} else {// 圆环,donut
				double dAngle = 3;
				double sAngle = start;
				while (sAngle <= endAngle) {
					smallSector = new Arc2D.Double(smallBounds, sAngle - 5,
							dAngle + 10, Arc2D.PIE);
					double tmps = sAngle, tmpa = dAngle;
					if (tmps != start) {// 让每一个dring彼此重叠，避免delta之间的空隙
						tmps -= dAngle;
						tmpa += dAngle * 2;
					}
					if (tmps + tmpa > endAngle) {
						tmpa = endAngle - tmps;
					}
					bigSector = new Arc2D.Double(bigBounds, tmps, tmpa,
							Arc2D.PIE);
					java.awt.geom.Area dRing = new java.awt.geom.Area(bigSector);
					dRing.subtract(new java.awt.geom.Area(smallSector));
					smallSector = new Arc2D.Double(smallBounds, sAngle,
							dAngle / 2f, Arc2D.PIE);
					bigSector = new Arc2D.Double(midBounds, sAngle,
							dAngle / 2f, Arc2D.PIE);
					// delta环的环中点
					Point2D dp1 = smallSector.getEndPoint();
					Point2D dp2 = bigSector.getEndPoint();
					if (ccr.getF1() != null && ccr.getF2() != null) {
						g.setPaint(new GradientPaint(dp1, ccr.getF2(), dp2, ccr
								.getF1(), true));
						g.fill(dRing);
					}
					sAngle += dAngle;
				}

			}
		} else {
			if (Utils.setPaint(g, rect.x, rect.y, rect.width, rect.height,
					fillColor)) {
				Utils.fillPaint(g, ring, transparent);
			}
		}

		int style = borderStyle;
		float weight = borderWeight;
		if (Utils.setStroke(g, borderColor, style, weight)) {
			bigSector = new Arc2D.Double(bigBounds, start, extent, Arc2D.OPEN);
			g.draw(bigSector);
			Line2D line;
			Point2D spb, spe;
			if (smallBounds.getWidth() == 0) {
				spe = new Point2D.Double(bigBounds.getCenterX(),
						bigBounds.getCenterY());
				spb = spe;
			} else {
				smallSector = new Arc2D.Double(smallBounds, start, extent,
						Arc2D.OPEN);
				g.draw(smallSector);
				spb = smallSector.getStartPoint();
				spe = smallSector.getEndPoint();
			}
			line = new Line2D.Double(spb, bigSector.getStartPoint());
			g.draw(line);
			line = new Line2D.Double(spe, bigSector.getEndPoint());
			g.draw(line);
		}

		return ring;
	}

	/**
	 * 封装简易替换操作，忽略引号括号等转义符内容，也就是彻底替换
	 * @param src 源文本串
	 * @param sold 需要替换掉的文本
	 * @param snew 新文本
	 * @return 替换完成的目标串
	 */
	public static String replaceAll(String src, String sold, String snew) {
		return Sentence.replace(src, 0, sold, snew, Sentence.IGNORE_CASE
				+ Sentence.IGNORE_PARS + Sentence.IGNORE_QUOTE
				+ Sentence.ONLY_PHRASE);
	}

	/**
	 * 处理超链接文本串，将内容替换掉相应的参数格式
	 * @param link 要替换的包含参数格式的串
	 * @param data1  替换格式"@data1"内容
	 * @param data2  替换格式"@data2"内容
	 * @param text  替换格式"@text"内容
	 * @param legend  替换格式"@legend"内容
	 * @return 替换后的串
	 */
	public static String getHtmlLink(String link, Object data1, Object data2,
			String text, String legend) {
		if (!StringUtils.isValidString(link))
			return null;

		if (data1 != null) {
			link = replaceAll(link, "@data1", data1.toString());
		}
		if (data2 != null) {
			link = replaceAll(link, "@data2", data2.toString());
		}
		if (StringUtils.isValidString(text)) {
			link = replaceAll(link, "@text", text);
		}
		if (StringUtils.isValidString(legend)) {
			link = replaceAll(link, "@legend", legend);
		}
		return link;
	}

	/**
	 * 文本输出为条形码时，绘制为缓冲图像的条码
	 * @param value 文本图元
	 * @param index 文本数据为序列时的文本序号
	 * @param fore 前景色
	 * @param back 背景色
	 * @return 缓冲图像
	 */
	public static BufferedImage calcBarcodeImage(Text value, int index, Color fore,
			Color back) {
		int TRANSPARENT = 0x00FFFFFF;// 不能是用全0，会造成导出pdf失败；
		int BLACK = 0xFF000000;
		int WHITE = 0xFFFFFFFF;

		int w = value.getWidth(index);
		int h = value.getHeight(index);
		if (w <= 0) {
			w = 40;
		}
		if (h <= 0) {
			h = 40;
		}
		int backColor = WHITE;
		int foreColor = BLACK;
		if (back != null) {
			backColor = back.getRGB();
		}
		if (fore != null) {
			foreColor = fore.getRGB();
		}

		if (value.barType == Consts.TYPE_QRCODE) {
			// 如果是二维码，用正方形区域来绘图，且由于有QuietZone的白边，多给30像素来绘制，然后取数据区域
			int minWH = w < h ? w : h;
			w = h = (minWH + 30);// 留出30边框以便裁剪数据区域后，更接近于实际宽高
		} else if (value.dispText) {
			// 如果是条形码，且显示文字，留出文字空间
			Rectangle rect = getTextRect(value,index);
			h -= (rect.height + 10);
		}

		MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
		Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
		// 说是字符集名称小写后，某些扫描枪才正常，否则有多余字符 xq 2016年12月1日
		hints.put(EncodeHintType.CHARACTER_SET, value.charSet.toLowerCase());
		char errLevel = value.recError.charAt(0);
		ErrorCorrectionLevel ecl;
		if (errLevel == 'L') {
			ecl = ErrorCorrectionLevel.L;
		} else if (errLevel == 'M') {
			ecl = ErrorCorrectionLevel.M;
		} else if (errLevel == 'Q') {
			ecl = ErrorCorrectionLevel.Q;
		} else {
			ecl = ErrorCorrectionLevel.H;
		}
		hints.put(EncodeHintType.ERROR_CORRECTION, ecl);
		if(value.barType==Consts.TYPE_QRCODE) {
//			二维码改成默认为正方形
			hints.put(EncodeHintType.DATA_MATRIX_SHAPE, SymbolShapeHint.FORCE_SQUARE);
		}
		try {
			BarcodeFormat bf = convertBarcodeFormat(value.barType);
			BitMatrix bitMatrix;
			if(isCode128ABC(value.barType)) {
		         Writer writer = new Code128ABC(value.barType);
		         bitMatrix = writer.encode(value.getDispText(index), bf, w,h, hints);
			}else {
				bitMatrix = multiFormatWriter.encode(value.getDispText(index), bf, w,
						h, hints);
			}
			if (value.barType == Consts.TYPE_QRCODE) {
				return toBufferedQRImage(value, bitMatrix, foreColor, backColor);
			} else {
				return toBufferedImage(value,index, bitMatrix, foreColor, backColor);
			}
		} catch (Exception x) {
			String val = value.toString();
			String upper = val.toUpperCase();
			boolean isUpper = upper.equals(val);
			if (value.barType == Consts.TYPE_CODE39 && !isUpper) {
				throw new RQException(
						"Only upper case characters were supported by Code39!");
			}
			throw new RQException(x.getMessage(), x);
		}
	}

	private static Point get417Margin(BitMatrix matrix) {
		Point p = new Point();
		int width = matrix.getWidth();
		int height = matrix.getHeight();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (matrix.get(x, y))
					return new Point(x - 1, y - 1);
			}
		}
		return p;
	}

	/**
	 * 计算文本图元中第index个文本的空间位置
	 * @param value 文本图元
	 * @param index 文字序号
	 * @return 矩形描述的空间
	 */
	public static Rectangle getTextRect(Text value,int index) {
		Font font = new Font(value.textFont.stringValue(index), Font.PLAIN, value.textSize.intValue(index));
		Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
				.createGraphics();
		FontMetrics fm = g.getFontMetrics(font);
		int txtHeight = fm.getHeight();
		g.dispose();
		return new Rectangle(0, 0, fm.stringWidth(value.getDispText(index)), txtHeight);
	}

	private static BufferedImage toBufferedImage(Text value, int index,BitMatrix matrix,
			int foreColor, int backColor) {
		int width = matrix.getWidth();
		int height = matrix.getHeight();
		int x1 = 0, x2 = width;
		int y1 = 0, y2 = height;
		// pdf417, 条码尺寸会自动撑大
		if (value.barType == Consts.TYPE_PDF417) {
			Point leftUpCorner = get417Margin(matrix);
			x1 = leftUpCorner.x;
			y1 = leftUpCorner.y;
			x2 = width - x1;
			y2 = height - y1;
		}

		int dw = x2 - x1;
		int dh = y2 - y1;
		Font font = new Font(value.textFont.stringValue(index), Font.PLAIN, value.textSize.intValue(index));
		if (value.dispText) {
			Rectangle rect = getTextRect(value,index);
			dh = dh + rect.height;
		}
		BufferedImage image = new BufferedImage(dw, dh,
				BufferedImage.TYPE_INT_RGB);// 不能使用ARGB，否则导出到Excel会有半透明效果 xq
											// 2016年11月22日
		Graphics2D g = image.createGraphics();
		g.setBackground(new Color(backColor));
		g.clearRect(0, 0, dw, dh);
		FontMetrics fm = g.getFontMetrics(font);
		int txtHeight = 0;

		if (value.dispText) {
			// 上述代码自动增加文字高度后，不需要判断高度空间了
			// txtHeight = fm.getHeight();
			boolean isSpaceEnough = (txtHeight < dh - 10);
			int txtWidth = fm.stringWidth(value.getDispText(index));
			int x = (dw - txtWidth) / 2;
			isSpaceEnough = x >= 0;
			if (!isSpaceEnough) {
				throw new RQException(
						"Barcode is too narrow to draw text on it.Please set smaller font size or hide text.");
			}

		}

		for (int x = x1; x < x2; x++) {
			for (int y = y1; y < y2; y++) {
				image.setRGB(x - x1, y - y1, matrix.get(x, y) ? foreColor
						: backColor);// BLACK:NULL);
			}
		}

		if (value.dispText) {
			g.setColor(new Color(foreColor));
			g.setFont(font);
			int txtWidth = fm.stringWidth(value.getDispText(index));
			txtHeight = fm.getHeight();
			int x = (dw - txtWidth) / 2;
			int y = dh - fm.getDescent();
			g.drawString(value.getDispText(index), x, y);
		}
		g.dispose();
		return image;
	}

	/**
	 * 将二维码信息matrix融合文本内容，比如打印logo到二维码
	 * @param text 文本图元
	 * @param matrix 二维码信息矩阵
	 * @param foreColor 前景色
	 * @param backColor 背景色
	 * @return 融合文本信息的二维码缓冲图像
	 */
	public static BufferedImage toBufferedQRImage(Text text, BitMatrix matrix,
			int foreColor, int backColor) {
		int width = matrix.getWidth();

		int margin = getMarginIndex(matrix);
		int dataSize = width - margin * 2;
		int dataEnd = margin + dataSize;
		BufferedImage image = new BufferedImage(dataSize, dataSize,
				BufferedImage.TYPE_INT_RGB);
		for (int x = margin; x < dataEnd; x++) {
			for (int y = margin; y < dataEnd; y++) {
				image.setRGB(x - margin, y - margin,
						matrix.get(x, y) ? foreColor : backColor);
			}
		}
		int scale = (int) ((width - 30) * 100 / dataSize);
		image = scaleImage(image, scale);
		image = printLogo(image, text);
		return image;
	}

	private static int getMarginIndex(BitMatrix matrix) {
		int width = matrix.getWidth();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < width; y++) {
				if (matrix.get(x, y))
					return x - 1;
			}
		}
		return 0;
	}

	private static BufferedImage scaleImage(BufferedImage src, int scale) {
		if (scale == 100) {
			return src;
		}
		float width = src.getWidth();
		int scaleWidth = (int) (width * scale * 0.01);
		Image image = src.getScaledInstance(scaleWidth, scaleWidth,
				Image.SCALE_DEFAULT);

		BufferedImage buffer = new BufferedImage(scaleWidth, scaleWidth,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = buffer.createGraphics();
		g2.drawImage(image, 0, 0, null);
		g2.dispose();
		return buffer;
	}

	/**
	 * 读出环境配置下文件的内容，比如位于搜索路径下的文件
	 * @param fileValue 文件名，用于兼容调用，文件名本身可以已经为字节内容
	 * @return 文件内容的字节数组
	 */
	public static byte[] getFileBytes(Object fileValue) {
		Object imgVal = fileValue;
		byte[] imageBytes = null;
		if (imgVal instanceof byte[]) {
			imageBytes = (byte[]) imgVal;
		} else if (imgVal instanceof String) {
			try {
				String path = (String) imgVal;
				FileObject fo = new FileObject(path, "s");
				int size = (int) fo.size();
				imageBytes = new byte[size];
				InputStream is = fo.getInputStream();
				is.read(imageBytes);
				is.close();
			} catch (Exception x) {
				throw new RQException(x.getMessage(),x);
			}
		}else if(imgVal!=null){
			throw new RQException(mm.getMessage("Utils.invalidfile",fileValue));
		}
		return imageBytes;
	}

	private static BufferedImage printLogo(BufferedImage src, Text value) {
		Para logoValue = value.logoValue;
		byte[] bytes = getFileBytes(logoValue.getValue());
		if(bytes==null) return src;
		
		ImageIcon icon = new ImageIcon(bytes);
		Image image = icon.getImage();

		int width = src.getWidth();
		double center = width / 2;
		double logoPercent = value.logoSize / 100.0f;
		int size = (int) (width * logoPercent);
		if (size % 2 != 0) {
			size += 1;
		}
		double shift = size / 2;
		image = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
		image = new ImageIcon(image).getImage();

		Graphics2D g2 = src.createGraphics();
		int x = (int) (center - shift);
		if (value.logoFrame) {
			g2.setColor(Color.white);
			int edge = 2;
			Rectangle rect = new Rectangle(x - edge, x - edge, size + edge * 2
					- 1, size + edge * 2 - 1);
			g2.fill(rect);
			g2.setColor(Color.lightGray);
			g2.draw(rect);
		}

		boolean b = g2.drawImage(image, x, x, null);
		g2.dispose();
		return src;
	}
	
	/**
	 * zxing的code28只有auto类型，抄了一个类Code128ABC
	  *  使用强制的ABC类型
	 * @param raqType
	 * @return
	 */
	public static boolean isCode128ABC(int raqType) {
		return raqType==Consts.TYPE_CODE128A ||
				raqType==Consts.TYPE_CODE128B ||
				raqType==Consts.TYPE_CODE128C;
	}

	private static BarcodeFormat convertBarcodeFormat(int raqType) {
		switch (raqType) {
		case Consts.TYPE_CODABAR:
			return BarcodeFormat.CODABAR;
		case Consts.TYPE_CODE128:
		case Consts.TYPE_CODE128A:
		case Consts.TYPE_CODE128B:
		case Consts.TYPE_CODE128C:
			return BarcodeFormat.CODE_128;
		case Consts.TYPE_CODE39:
			// Code 39只接受如下43个有效输入字符： 　　26个大写字母（A - Z）， 　　十个数字（0 - 9）
			// 注意小写字母会报错
			return BarcodeFormat.CODE_39;
		case Consts.TYPE_EAN13:// ean13对编码有严格规则，不能是随便的数字,示例值：7501054530107
			return BarcodeFormat.EAN_13;
		case Consts.TYPE_EAN8:
			return BarcodeFormat.EAN_8;
		case Consts.TYPE_ITF:
			return BarcodeFormat.ITF;
		case Consts.TYPE_PDF417:
			return BarcodeFormat.PDF_417;
		case Consts.TYPE_UPCA:
			return BarcodeFormat.UPC_A;
		}

		return BarcodeFormat.QR_CODE;
	}

	/**
	 * 求出经过点p1和p2的直线上的点
	 * @param p1 端点1
	 * @param p2 端点2
	 * @param x, 经过两点的直线，待求点的x坐标
	 * @return x对应的y坐标
	 */
	public static double calcLineY(Point2D.Double p1, Point2D.Double p2, double x){
//		直线的方程为y=kx+b
//		y1=kx1+b;y2=kx2+b;
//		k=(y2-y1)/(x2-x1)
//		b = y1-kx1
		double x1, y1, x2, y2;
		x1 = p1.x;
		y1 = p1.y;
		x2 = p2.x;
		y2 = p2.y;
		double k, b;
		k = (y2-y1)/(x2-x1);
		b = y1-k*x1;
		double y = k*x+b;
		return y;
	}
	
	public static void main(String[] args) {
		IParam param = ParamParser.parse("a,b:c,d", null, null);
		ArrayList al = new ArrayList();
		param.getAllLeafExpression(al);

		for (int i = 0; i < al.size(); i++) {
			System.out.println(al.get(i).toString());
		}
	}

}
