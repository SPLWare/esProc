package com.scudata.chart.element;

import java.awt.*;
import java.awt.geom.*;

import com.scudata.chart.*;
import com.scudata.chart.edit.*;
import com.scudata.common.*;
import com.scudata.dm.*;

/**
 * 柱和扇的基类，环
 * @author Joancy
 *
 */
public abstract class Ring extends DataElement {
	public int stackType = Consts.STACK_NONE;

	// 透明度
	public float transparent = 1f;

	// 边框类型
	public Para borderStyle = new Para(new Integer(Consts.LINE_SOLID));

	// 边框宽度
	public Para borderWeight = new Para(new Integer(0));

	// 边框颜色
	public Para borderColor = new Para(new Integer(Color.DARK_GRAY.getRGB()),Consts.LEGEND_P_LINECOLOR);

	// 填充颜色
	public Para fillColor = new Para(Consts.LEGEND_P_FILLCOLOR);

	// 标示文字
	public Para text = new Para(null);

	// 标示文字字体
	public Para textFont = new Para();//"Dialog");

	// 标示文字字型
	public Para textStyle = new Para(new Integer(0));

	// 标示文字大小
	public Para textSize = new Para(new Integer(12));

	// 标示文字颜色
	public Para textColor = new Para(Color.black);

	// 文字重叠显示
	public boolean textOverlapping = true;


	/**
	 * 缺省值的构造函数
	 */
	public Ring() {
	}

	/**
	 * 是否堆积图形
	 * @return 如果是堆积类型返回true，否则返回false
	 */
	public boolean isStacked() {
		return (stackType>Consts.STACK_NONE);
	}

	/**
	 * 获取图元中的最大数值，堆积图时按分类汇总计算
	 * @param de 数据第一
	 * @param numericAxis 数值轴名称
	 * @return 最大值
	 */
	public static Double getMaxValue(DataElement de, String numericAxis) {
		Sequence numData = de.getAxisData(numericAxis);
		Sequence enumData = de.getOppositeAxisData(numericAxis);
		double max = 0;
		int stackType = Consts.STACK_NONE;
		if( de instanceof Ring){
			stackType = ((Ring)de).stackType;
		}
		if( de instanceof Line){
			stackType = ((Line)de).stackType;
		}
		if (stackType==Consts.STACK_VALUE) {
//			枚举值直接从图元自身找，轴上的是所有图元的合并值
			int catSize = de.categories.length();
			for (int c = 1; c <= catSize; c++) {
				String catName = (String) de.categories.get(c);
				double d = Utils.sumCategory(catName, enumData, numData);
				if (d > max) {
					max = d;
				}
			}
		}else if (stackType==Consts.STACK_PERCENT) {
			max = 1;
		}
		else {
			max = ((Number)numData.max()).doubleValue();
			if(de instanceof Column){
				Column col = (Column)de;
				Sequence data3 = col.getData3();
				if(data3!=null){
					double d = ((Number)data3.max()).doubleValue();
					max = Math.max(max, d);
				}
			}
		}

		return new Double(max);
	}

	protected String getText(int index){
		return text.stringValue(index);
	}
	
	// colorIndex,在枚举轴下，colorIndex为系列值，同系列，颜色统一
	private void drawData(int index, double halfColWidth, int step,
			int seriesIndex) {
		String title = getTipTitle(index);
		
		if(isPhysicalCoor()){
			Point2D p = getScreenPoint(index);
			Point2D p1, p2;
			Point2D basePoint;
			if (getData3() == null) {
				basePoint = new Point2D.Double(0,0);
			} else {
				Object val3 = getData3().get(index);
				double vx = ((Number)data1.get(index)).doubleValue();
				double vy = ((Number)val3).doubleValue();
				double px = e.getXPixel(vx);
				double py = e.getYPixel(vy);
				basePoint = new Point2D.Double(px,py);
			}

			p1 = new Point2D.Double(p.getX() - halfColWidth,
					basePoint.getY()); // 左下角
			p2 = new Point2D.Double(p.getX() + halfColWidth, p.getY()); // 右上角
			Shape dataLinkShape = drawFreeColumn(index, p1, p2, step, true, seriesIndex);
			if(dataLinkShape!=null){
				addLink(dataLinkShape, htmlLink.stringValue(index), title,linkTarget.stringValue(index));
			}
			return;
		}
		Object val1 = data1.get(index);
		Object val2 = data2.get(index);
		String txt = text.stringValue(index);
		Shape dataLinkShape = null;
		
		if(stackType==Consts.STACK_PERCENT){
			if(val2 instanceof Number){
				val2 = getPercentValue(val1,val2,data1,data2);
			}else{
				val1 = getPercentValue(val2,val1,data2,data1);
			}
		}
		
		ICoor coor = getCoor();
		Point2D p;
		TickAxis t1 = coor.getAxis1();
		if (coor.isCartesianCoor()) {
			p = coor.getScreenPoint(val1, val2);
			Point2D p1, p2;
			boolean isVerticalColumn = (t1.getLocation() == Consts.AXIS_LOC_H);
			Point2D basePoint;
			if (getData3() == null) {
				basePoint = t1.getBasePoint(coor);
			} else {
				Object val3 = getData3().get(index);
				basePoint = coor.getScreenPoint(val1, val3);
			}

			if (isVerticalColumn) {
				p1 = new Point2D.Double(p.getX() - halfColWidth,
						basePoint.getY()); // 左下角
				p2 = new Point2D.Double(p.getX() + halfColWidth, p.getY()); // 右上角
			} else {
				p1 = new Point2D.Double(basePoint.getX(), p.getY()
						+ halfColWidth); // 左下角
				p2 = new Point2D.Double(p.getX(), p.getY() - halfColWidth); // 右上角
			}
			dataLinkShape = drawFreeColumn(index, p1, p2, step, isVerticalColumn, seriesIndex);
		} else {
			PolarCoor pc = (PolarCoor) coor;
			p = pc.getPolarPoint(val1, val2);
			Graphics2D g = e.getGraphics();
			TickAxis angleAxis = pc.getAngleAxis();
			double start, extent;
			Color bc = borderColor.colorValueNullAsDef(seriesIndex);
			int bs = borderStyle.intValue(seriesIndex);
			float bw = borderWeight.floatValue(seriesIndex);
			ChartColor fc = fillColor.chartColorValue(seriesIndex);
			if (t1.getLocation() == Consts.AXIS_LOC_POLAR) {// 垂向轴是极轴时，为一段环
				if (getData3() == null) {
					start = angleAxis.startAngle;
					extent = p.getY();
				} else {
					Object val3 = getData3().get(index);
					double angle3 = angleAxis.getValueLen(val3);
					start = angleAxis.startAngle + Math.min(p.getY(), angle3);
					extent = Math.abs(p.getY() - angle3);
				}
				switch (step) {
				case 1: // 画环
					Rectangle2D bigBounds = pc.getEllipseBounds(p.getX()
							+ halfColWidth);
					Rectangle2D smallBounds = pc.getEllipseBounds(p.getX()
							- halfColWidth);
					dataLinkShape = Utils.draw2DRing(g, bigBounds, smallBounds, start, extent,
							bc, bs, bw, transparent, fc);
					break;
				case 2:
					if (!StringUtils.isValidString(txt)) {
						return;
					}
					double angle = start + extent / 2;
					Point2D txtP = new Point2D.Double(p.getX(),angle);
					String fontName = textFont.stringValue(index);
					int fontStyle = textStyle.intValue(index);
					int fontSize = textSize.intValue(index);
					Color c = textColor.colorValue(index);

					Utils.drawPolarPointText(e, txt, pc, txtP, fontName,
							fontStyle, fontSize, c, textOverlapping);
					break;
				}
			} else {// 垂向轴为角轴时，为一段扇；
				TickAxis polarAxis = pc.getPolarAxis();
				double r3 = 0;
				if (getData3() != null) {
					Object val3 = getData3().get(index);
					r3 = polarAxis.getValueLen(val3);
				}
				Rectangle2D bigBounds = pc.getEllipseBounds(Math.max(p.getX(),
						r3));
				Rectangle2D smallBounds = pc.getEllipseBounds(Math.min(
						p.getX(), r3));
				start = angleAxis.startAngle + p.getY() - halfColWidth;
				extent = halfColWidth * 2;
				switch (step) {
				case 1: // 画扇
						dataLinkShape = Utils.draw2DRing(g, bigBounds, smallBounds, start,
								extent, bc, bs, bw, transparent, fc,isFillPie());
					break;
				case 2:
					if (!StringUtils.isValidString(txt)) {
						return;
					}
					String fontName = textFont.stringValue(index);
					int fontStyle = textStyle.intValue(index);
					int fontSize = textSize.intValue(index);
					Color c = textColor.colorValue(index);
						Utils.drawPolarPointText(e, txt, pc, p, fontName,
								fontStyle, fontSize, c, textOverlapping,
								Consts.LOCATION_CM);
					break;
				}
			}

		}
		if(dataLinkShape!=null){
			addLink(dataLinkShape, htmlLink.stringValue(index), title,linkTarget.stringValue(index));
		}
	}

	/**
	 * 抽取分隔串val中的分类值部分
	 * @param val 数据值
	 * @return 分类值
	 */
	public static Object discardSeries(Object val) {
		if (val instanceof Number) {
			return val;
		}
		return Utils.parseCategory(val);
	}

	/**
	 * 算出分类catName中，numVal占分类统计值的百分比
	 * @param catName 分类名称
	 * @param numVal 当前数值
	 * @param enumData 分类序列
	 * @param numData 对应的数值序列
	 * @return 实数精度的百分比值
	 */
	public static Double getPercentValue(Object catName,Object numVal,Sequence enumData,Sequence numData){
		double catSum = Utils.sumCategory(catName.toString(), enumData, numData);
		double val = ((Number)numVal).doubleValue()/catSum;
		return new Double(val);
	}
	
	private Point2D drawStackedData(int index, double halfColWidth, int step,
			int seriesIndex, Point2D lastPoint) {
		Object val1 = discardSeries(data1.get(index));
		Object val2 = discardSeries(data2.get(index));

		String title = getTipTitle(index);
		Shape linkShape = null;
		if(stackType==Consts.STACK_PERCENT){
			if(val2 instanceof Number){
				val2 = getPercentValue(val1,val2,data1,data2);
			}else{
				val1 = getPercentValue(val2,val1,data2,data1);
			}
		}
		ICoor coor = getCoor();
		TickAxis ta1 = coor.getAxis1();
		Point2D p, p1, p2 = null;
		if (coor.isCartesianCoor()) {
			p = coor.getScreenPoint(val1, val2);
			double columnLength = 0;
			boolean isVerticalColumn = (ta1.getLocation() == Consts.AXIS_LOC_H);
			Point2D basePoint = ta1.getBasePoint(coor);
			if (isVerticalColumn) {
				p1 = new Point2D.Double(p.getX() - halfColWidth,
						lastPoint.getY()); // 左下角
				columnLength = basePoint.getY() - p.getY();
				p2 = new Point2D.Double(p.getX() + halfColWidth,
						lastPoint.getY() - columnLength); // 右上角
			} else {
				p1 = new Point2D.Double(lastPoint.getX(), p.getY()
						+ halfColWidth); // 左下角
				columnLength = p.getX() - basePoint.getX();
				p2 = new Point2D.Double(lastPoint.getX() + columnLength,
						p.getY() - halfColWidth); // 右上角
			}
			linkShape = drawFreeColumn(index, p1, p2, step, isVerticalColumn, seriesIndex);
		} else {
			PolarCoor pc = (PolarCoor) coor;
			p = pc.getPolarPoint(val1, val2);
			Graphics2D g = e.getGraphics();
			TickAxis angleAxis = pc.getAngleAxis();
			double start, extent;
			Color bc = borderColor.colorValueNullAsDef(seriesIndex);
			int bs = borderStyle.intValue(seriesIndex);
			float bw = borderWeight.floatValue(seriesIndex);
			ChartColor fc = fillColor.chartColorValue(seriesIndex);
			if (ta1.getLocation() == Consts.AXIS_LOC_POLAR) {// 垂向轴是极轴时，为一段环
				if (lastPoint == null) {
					start = angleAxis.startAngle;
				} else {
					start = lastPoint.getY();
				}
				extent = p.getY();
				switch (step) {
				case 1: // 画环
					Rectangle2D bigBounds = pc.getEllipseBounds(p.getX()
							+ halfColWidth);
					Rectangle2D smallBounds = pc.getEllipseBounds(p.getX()
							- halfColWidth);
					linkShape = Utils.draw2DRing(g, bigBounds, smallBounds, start, extent,
							bc, bs, bw, transparent, fc, isFillPie());
					break;
				case 2:
					String txt = text.stringValue(index);
					if (!StringUtils.isValidString(txt)) {
						break;
					}
					double angle = start + extent / 2;
					Point2D txtP = new Point2D.Double(p.getX(),angle);
					String fontName = textFont.stringValue(index);
					int fontStyle = textStyle.intValue(index);
					int fontSize = textSize.intValue(index);
					Color c = textColor.colorValue(index);
					Utils.drawPolarPointText(e, txt, pc, txtP, fontName,
							fontStyle, fontSize, c, textOverlapping,
							Consts.LOCATION_CM);
					break;
				}
				p2 = new Point2D.Double(p.getX(), start + extent);
			} else {// 垂向轴为角轴时，为一段扇；
				double innerR, outerR;
				if (lastPoint == null) {
					innerR = 0;
					outerR = p.getX();
				} else {
					innerR = lastPoint.getX();
					outerR = innerR + p.getX();
				}
				start = angleAxis.startAngle + p.getY() - halfColWidth;
				extent = halfColWidth * 2;
				switch (step) {
				case 1: // 画环
					Rectangle2D bigBounds = pc.getEllipseBounds(outerR);
					Rectangle2D smallBounds = pc.getEllipseBounds(innerR);
					linkShape = Utils.draw2DRing(g, bigBounds, smallBounds, start, extent,
							bc, bs, bw, transparent, fc);
					break;
				case 2:
					String txt = text.stringValue(index);
					if (!StringUtils.isValidString(txt)) {
						break;
					}
					String fontName = textFont.stringValue(index);
					int fontStyle = textStyle.intValue(index);
					int fontSize = textSize.intValue(index);
					Color c = textColor.colorValue(index);
					p1 = new Point2D.Double(innerR + p.getX() / 2, p.getY());
					Utils.drawPolarPointText(e, txt, pc, p1, fontName,
							fontStyle, fontSize, c, textOverlapping,
							Consts.LOCATION_CM);
					break;
				}
				p2 = new Point2D.Double(outerR, p.getY());
			}
		}
		if(linkShape!=null){
			addLink(linkShape, htmlLink.stringValue(index), title,linkTarget.stringValue(index));
		}
		
		return p2;
	}

	/**
	 * 分步绘制图形
	 * @param step 要绘制的步骤
	 */
	public void drawStep(int step) {
		if (!isVisible()) {
			return;
		}
		TickAxis ia1 = null;
		if(!isPhysicalCoor()){
			ICoor coor = getCoor();
			ia1 = coor.getAxis1();
			if (ia1.isEnumAxis()) {
				drawEnumBasedRing(step);
				return;
			}
		}

		int size = pointSize();
		Sequence sort = null;
		sort = data1.psort(null);
		//当有3D柱时，要从小到大顺序画，否则会有遮盖不正常；
		for (int i = 1; i <= size; i++) {
			int index = ((Number)sort.get(i)).intValue();
			double colWidth = getColumnWidth(ia1, index);
			double halfColWidth = colWidth / 2;
			drawData(index, halfColWidth, step, index);
		}
	}

	private void drawEnumBasedRing(int step) {
		ICoor coor = getCoor();
		EnumAxis ea = (EnumAxis) coor.getAxis1();
		Sequence enumData = data1;// getAxisData(ea.getName());

		int catCount = categories.length();
		for (int c = 1; c <= catCount; c++) { // 挨个分类画柱子
			String catName = (String) categories.get(c);

			int index = 0;
			int serCount = series.length();
			if (serCount == 0) {
				index = Utils.indexOf(enumData, catName, null);
				if (index == 0) { // 某个分类和系列的数值缺少
					continue;
				}
				double colWidth = getColumnWidth(ea, index);
				double halfColWidth = colWidth / 2;
				drawData(index, halfColWidth, step, index);
			} else {
				if (isStacked()) {
					Point2D lastPoint = null;
					if (coor.isCartesianCoor()) {
						lastPoint = ea.getBasePoint(coor);
					}
					for (int s = 1; s <= serCount; s++) {
						String serName = (String) series.get(s);
						index = Utils.indexOf(enumData, catName, serName);
						if (index == 0) { // 某个分类和系列的数值缺少
							continue;
						}
						double colWidth = getColumnWidth(ea, index);
						double halfColWidth = colWidth / 2;
						lastPoint = drawStackedData(index, halfColWidth, step,
								s, lastPoint);
					}
				} else {
					for (int s = 1; s <= serCount; s++) {
						String serName = (String) series.get(s);
						index = Utils.indexOf(enumData, catName, serName);
						if (index == 0) { // 某个分类和系列的数值缺少
							continue;
						}
						double colWidth = getColumnWidth(ea, index);
						double halfColWidth = colWidth / 2;
						drawData(index, halfColWidth, step, s);
					}
				}
			}
		}

	}
	
	/**
	 * 绘制背景层
	 */
	public void drawBack() {
		drawStep(1);
	}


	/**
	 * 绘制中间层，当前忽略
	 */
	public void draw() {
	}

	/**
	 * 绘制前景层
	 */
	public void drawFore() {
		drawStep(2);
	}

	/**
	 * 获取编辑参数信息列表
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(Ring.class, this);

		paramInfos.add(new ParamInfo("stackType", Consts.INPUT_STACKTYPE));
		paramInfos.add(new ParamInfo("textOverlapping", Consts.INPUT_CHECKBOX));
		paramInfos.add(new ParamInfo("transparent", Consts.INPUT_DOUBLE));

		String group = "appearance";
		paramInfos.add(group, new ParamInfo("borderStyle",
				Consts.INPUT_LINESTYLE));
		paramInfos.add(group, new ParamInfo("borderWeight",
				Consts.INPUT_INTEGER));
		paramInfos.add(group, new ParamInfo("borderColor", Consts.INPUT_COLOR));
		paramInfos.add(group, new ParamInfo("fillColor",
				Consts.INPUT_CHARTCOLOR));

		group = "text"; // source here
		paramInfos.add(group, new ParamInfo("text"));
		paramInfos.add(group, new ParamInfo("textFont", Consts.INPUT_FONT));
		paramInfos.add(group,
				new ParamInfo("textStyle", Consts.INPUT_FONTSTYLE));
		paramInfos.add(group, new ParamInfo("textSize", Consts.INPUT_FONTSIZE));
		paramInfos.add(group, new ParamInfo("textColor", Consts.INPUT_COLOR));

		// group = "链接";
		// paramInfos.add(group, new ParamInfo("tip"));
		// paramInfos.add(group, new ParamInfo("url"));
		// paramInfos.add(group, new ParamInfo("target"));
		
		ParamInfoList superParams = super.getParamInfoList();
		superParams.delete("data","axisTime");
		superParams.delete("data","dataTime");
		paramInfos.addAll( superParams);
		return paramInfos;
	}
	
	/**
	 * 图形绘制前的准备工作
	 */
	public void prepare() {
		super.prepare();
		checkStackProperties(this);
	}

	/**
	 * 检查数据图元的堆积属性是否合法
	 * @param de 数据图元
	 */
	public static void checkStackProperties(DataElement de) {
		boolean isStacked = false;
		Object data3 = null;
		if( de instanceof Ring){
			Ring r = (Ring)de;
			isStacked = r.isStacked();
			data3 = r.getData3();
		}
		if( de instanceof Line){
			Line l = (Line)de;
			isStacked = l.isStacked();
		}

		if (isStacked) { // 堆积图不允许负数
			if(de.isPhysicalCoor()){
				throw new RuntimeException("Stacked graph does not support physical coordinates.");
			}
			
			ICoor coor = de.getCoor();
			 if (!coor.isEnumBased()){
					throw new RuntimeException(
							"Stacked graph must be based on EnumAxis, and it must be spedified by 'axis1' property.");
			 }
			 
			 if (data3 != null) {
				throw new RuntimeException(
						"Floating column can not be stacked.");
			}

			NumericAxis na = coor.getNumericAxis();
			Sequence numData = de.getAxisData(na.getName());
			double min = ((Number) numData.min()).doubleValue();
			if (min < 0) {
				throw new RuntimeException(
						"Stacked graph does not support netagive data:" + min);
			}
			if(na.transform==Consts.TRANSFORM_EXP || na.transform == Consts.TRANSFORM_LOG ){
				throw new RuntimeException(
						"Stacked graph does not support exponent or log transform.");
			}
		}

	}

	/**
	 * 是否定义了渐变填充色
	 * @return 如果有渐变返回true，否则返回false
	 */
	public boolean hasGradientColor() {
		return fillColor.hasGradientColor();
	}

	/**
	 * 枚举都是比系列宽度；日期轴都是按天算长度，数值轴则按数字算；不分大小与1的情况了；
	 */
	protected Shape drawFreeColumn(int index, Point2D p1, Point2D p2, int step,
			boolean isVertical, int seriesIndex){return null;}
	protected Sequence getData3(){return null;}
	protected boolean isFillPie(){return false;}//填充环的炫颜色时，是否使用扇状填充，否则为环状
	public abstract double getColumnWidth(TickAxis ia, int index);
}
