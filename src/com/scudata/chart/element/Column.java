package com.scudata.chart.element;

import java.awt.*;
import java.awt.geom.*;

import com.scudata.chart.*;
import com.scudata.chart.edit.*;
import com.scudata.common.*;
import com.scudata.dm.*;

/**
 * 柱图元
 * 柱图元可以展现为所有条状，包含直方图，立体直方图，圆柱图
 * 如果枚举轴在纵轴时则表现为系列条形图
 * 特别地，柱图元在极坐标系下表现为一条弯曲的环
 * @author Joancy
 *
 */
public class Column extends Ring {
	// 柱子高度轴的另一个逻辑坐标，格式[w1,w2,...,wn]或w
	public Sequence data3 = null;
	// 柱子宽度,如果是枚举轴上：<=1表示跟系列占宽的比例，>1表示绝对像素宽度。如果是日期或者数值轴时，<=1表示跟轴长的比例，>1表示绝对像素宽度
	public Para columnWidth = new Para(new Double(0.9));

	// 柱子类型
	public Para columnShape = new Para(new Integer(Consts.COL_COBOID));

	// 标示文字横向对齐
	public Para horizontalAlign = new Para(new Integer(Consts.HALIGN_CENTER));

	// 标示文字纵向对齐
	public Para verticalAlign = new Para(new Integer(Consts.VALIGN_TOP));

	// 是否有阴影
	public boolean shadow = true;

	// 突出边框
	public boolean convexEdge = false;

	/**
	 * 缺省参数的构造函数
	 */
	public Column() {
	}

	/**
	 * 图元计算前准备，检查数据完整性
	 * 数据不完整，不匹配时扔出异常
	 */
	public void prepare() {
		super.prepare();
		if (data3 != null && data1.length() != data3.length()) {
			throw new RuntimeException(
					"Column property 'data3' is not match to 'data1': data1 length="
							+ data1.length() + " data3 length="
							+ data3.length());
		}
	}

	/**
	 * 获取第index个柱子的宽度
	 * 枚举值的柱宽比照系列宽度；
	 * 日期或者数值轴直接按照像素返回
	 * @param ia 刻度轴
	 * @param index 柱子序号
	 * @return 返回按像素的柱子宽度
	 */
	public double getColumnWidth(TickAxis ia, int index) {
		double colWidth = columnWidth.doubleValue(index);
		if (ia instanceof EnumAxis) {
			colWidth = ia.getValueRadius(colWidth);
		}
//		如果柱宽为0则按0返回，否则最小得1像素，要不图形绘制不出来
		if(colWidth==0){
			return 0;
		}else if(colWidth<1){
			colWidth=1;
		}
		return colWidth;
	}

	/**
	 * 柱子可以绘制从数据3到数值数据的一段自由柱
	 * 这种自由柱时使用data3
	 * @return 逻辑数据3的序列
	 */
	public Sequence getData3() {
		return data3;
	}

	protected Shape drawFreeColumn(int index, Point2D p1, Point2D p2, int step,
			boolean isVertical, int seriesIndex) {
		// 只有第一步计算时才会返回数据链接的形状
		Shape linkShape = null;

		double leftX = Math.min(p1.getX(), p2.getX());
		double rightX = Math.max(p1.getX(), p2.getX());
		double topY = Math.min(p1.getY(), p2.getY());
		double bottomY = Math.max(p1.getY(), p2.getY());

		int x = (int) leftX;
		int y = (int) topY;
		int w = (int) (rightX) - x;
		int h = (int) (bottomY) - y;

		// Graphics2D g = e.getGraphics();
		switch (step) {
		case 1: // 画柱子
			int style = columnShape.intValue();
			switch (style) {
			case Consts.COL_CUBE:
				if (isPhysicalCoor()) {
					throw new RuntimeException(
							"Physical coordinates do not support 3D column.");
				}
				linkShape = draw3DColumn(x, y, w, h, seriesIndex, isVertical);
				break;
			case Consts.COL_CYLINDER:
				if (isPhysicalCoor()) {
					throw new RuntimeException(
							"Physical coordinates do not support cylinder column.");
				}
				linkShape = drawCylinder(x, y, w, h, seriesIndex, isVertical);
				break;
			default:
				linkShape = draw2DColumn(x, y, w, h, seriesIndex, isVertical);
			}
			break;
		case 2: // 画柱顶文字
			String txt = text.stringValue(index);
			if (!StringUtils.isValidString(txt)) {
				return null;
			}

			String fontName = textFont.stringValue(index);
			int fontStyle = textStyle.intValue(index);
			int fontSize = textSize.intValue(index);
			Font font = Utils.getFont(fontName, fontStyle, fontSize);
			Color tc = textColor.colorValue(index);
			int hAlign = horizontalAlign.intValue(index);
			int vAlign = verticalAlign.intValue(index);
			switch (hAlign) {
			case Consts.HALIGN_LEFT:
				break;
			case Consts.HALIGN_CENTER:
				x = x + w / 2;
				break;
			case Consts.HALIGN_RIGHT:
				x = x + w;
				break;
			}
			switch (vAlign) {
			case Consts.VALIGN_TOP:
				break;
			case Consts.VALIGN_MIDDLE:
				y = y + h / 2;
				break;
			case Consts.VALIGN_BOTTOM:
				y = y + h;
				break;
			}
			int location = hAlign + vAlign;
			int coorShift = 0;
			if( !isPhysicalCoor() ){
				style = columnShape.intValue();
				switch (style) {
				case Consts.COL_CYLINDER:
					ICoor coor = getCoor();
					coorShift = ((CartesianCoor) coor).get3DShift() / 2;
					break;
				default:
				}
			}
			
			Utils.drawText(e, txt, x + coorShift, y - coorShift, font, tc,
					fontStyle, 0, location, textOverlapping);
			break;
		}
		return linkShape;
	}

	private Shape draw2DColumn(int x, int y, int w, int h, int index,
			boolean isVertical) {
		Graphics2D g = e.getGraphics();
		ChartColor cc = fillColor.chartColorValue(index);
		Color bc = borderColor.colorValue(index);
		int bs = borderStyle.intValue(index);
		float bw = borderWeight.floatValue(index);

		Utils.draw2DRect(g, x, y, w, h, bc, bs, bw, shadow, convexEdge,
				transparent, cc, isVertical);
		return new java.awt.Rectangle(x, y, w, h);
	}

	private Shape draw3DColumn(int x, int y, int w, int h, int index,
			boolean isVertical) {
		Graphics2D g = e.getGraphics();
		CartesianCoor cc = (CartesianCoor) getCoor();
		int coorShift = cc.get3DShift();
		Color c = borderColor.colorValue(index);
		int style = borderStyle.intValue(index);
		float weight = borderWeight.floatValue(index);
		ChartColor chartColor = fillColor.chartColorValue(index);

		Utils.draw3DRect(g, x, y, w, h, c, style, weight, shadow, convexEdge,
				transparent, chartColor, isVertical, coorShift);

		int[] shapeX = new int[] { x, x + coorShift, x + coorShift + w,
				x + coorShift + w, x + w, x };
		int[] shapeY = new int[] { y, y - coorShift, y - coorShift,
				y - coorShift + h, y + h, y + h };

		return new java.awt.Polygon(shapeX, shapeY, shapeX.length);
	}

	private Shape drawCylinder(int x, int y, int width, int height, int index,
			boolean isVertical) {
		Graphics2D g = e.getGraphics();
		CartesianCoor cc = (CartesianCoor) getCoor();
		int coorShift = cc.get3DShift();
		double halfShift = coorShift / 2;
		ChartColor chartColor = fillColor.chartColorValue(index);

		double ovalRate = 0.5;
		double xOval;
		double yOval;

		if (isVertical) {
			xOval = x + halfShift;
			yOval = y - halfShift + height - width * ovalRate / 2;
		} else {
			xOval = x + halfShift - height * ovalRate / 2;
			yOval = y - halfShift;
		}
		Color bc = borderColor.colorValue(index);
		int bs = borderStyle.intValue(index);
		float bw = borderWeight.floatValue(index);

		Arc2D.Double bottomOval;
		if (isVertical) {
			bottomOval = new Arc2D.Double(xOval, yOval, width,
					width * ovalRate, 0, 360, Arc2D.OPEN);
		} else {
			bottomOval = new Arc2D.Double(xOval, yOval, height * ovalRate,
					height, 0, 360, Arc2D.OPEN);
		}
		if (transparent < 1) {
			// g.setColor(chartColor.getColor1()); // 底面圆
			Utils.fill(g, bottomOval, transparent, chartColor.getColor1());
		}
		if (Utils.setStroke(g, bc, bs, bw)) {
			g.draw(bottomOval);
		}

		Arc2D.Double topOval;
		if (isVertical) {
			topOval = new Arc2D.Double(xOval, yOval - height, width, width
					* ovalRate, 0, 360, Arc2D.OPEN);
		} else {
			topOval = new Arc2D.Double(xOval + width, yOval, height * ovalRate,
					height, 0, 360, Arc2D.OPEN);
		}
		Utils.drawCylinderTop(g, topOval, bc, bs, bw, transparent, chartColor,
				isVertical);

		double xRect, yRect;
		java.awt.geom.Area sc, sc1, sc2;
		if (isVertical) {
			xRect = x + halfShift;
			yRect = y - halfShift;
			sc = new java.awt.geom.Area(new Rectangle2D.Double(xRect, yRect,
					width, height + coorShift - 2));
			sc1 = new java.awt.geom.Area(new Rectangle2D.Double(xRect, yRect,
					width, height + coorShift - 2));
			sc2 = new java.awt.geom.Area(new Rectangle2D.Double(xRect, yRect,
					width, height));
		} else {
			xRect = x - 2;
			yRect = y - halfShift;
			sc = new java.awt.geom.Area(new Rectangle2D.Double(xRect, yRect,
					width + halfShift, height));
			sc1 = new java.awt.geom.Area(new Rectangle2D.Double(xRect, yRect,
					width + halfShift, height));
			sc2 = new java.awt.geom.Area(new Rectangle2D.Double(xRect
					+ halfShift, yRect, width, height));
		}
		java.awt.geom.Area or1 = new java.awt.geom.Area(bottomOval);
		java.awt.geom.Area or2 = new java.awt.geom.Area(topOval);
		sc2.subtract(or1);
		sc1.subtract(sc2);
		sc1.subtract(or1);
		sc.subtract(sc1);
		sc.subtract(or2);

		Utils.drawCylinderFront(g, sc, bc, bs, bw, transparent, chartColor,
				isVertical);

		java.awt.geom.Area outLine = new java.awt.geom.Area(sc);
		outLine.add(or2);
		return outLine;
	}

	/**
	 * 获取柱图元的编辑参数信息列表
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();

		ParamInfo.setCurrent(Column.class, this);

		paramInfos.add(new ParamInfo("shadow", Consts.INPUT_CHECKBOX));
		paramInfos.add(new ParamInfo("convexEdge", Consts.INPUT_CHECKBOX));
		paramInfos.add(new ParamInfo("data3", Consts.INPUT_NORMAL));

		String group = "appearance";
		paramInfos
				.add(group, new ParamInfo("columnWidth", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("columnShape",
				Consts.INPUT_COLUMNSTYLE));

		group = "text";
		paramInfos.add(group, new ParamInfo("horizontalAlign",
				Consts.INPUT_HALIGN));
		paramInfos.add(group, new ParamInfo("verticalAlign",
				Consts.INPUT_VALIGN));

		paramInfos.addAll(super.getParamInfoList());// 之所以从父类最后加载，是保持子类的属性靠前的顺序
		return paramInfos;
	}

	/**
	 * 克隆柱图元的属性值
	 * @param c
	 */
	public void clone(Column c){
		super.clone(c);
	}
	
	/**
	 * 深度克隆柱图元
	 * @return 克隆柱图元
	 */
	public Object deepClone() {
		Column c = new Column();
		clone(c);
		return c;
	}
}
