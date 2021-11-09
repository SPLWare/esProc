package com.raqsoft.chart.element;

import java.util.*;
import java.awt.Shape;

import com.raqsoft.chart.*;
import com.raqsoft.dm.*;
import com.raqsoft.chart.edit.*;
import com.raqsoft.common.RQException;

/**
 * 枚举轴
 * 枚举轴的数值通常为字符串，且要表述分类跟系列时，采用英文逗号分隔的串表示
 * @author Joancy
 *
 */
public class EnumAxis extends TickAxis {

	// 枚举轴属性：枚举值,分类和系列的值都只能是串，因为数据值的录入格式为 "分类值,系列值"，从中提取的分类值直接
	// 当串使用，不再parseValue.
	public Sequence categories;
	public Sequence series;

	// 枚举轴间隙与系列宽度占比
	public double gapRatio = 1.50;

	/**
	 * 缺省参数构造的枚举轴
	 */
	public EnumAxis() {
	}

	/*
	 * getPhyValue 数据的格式为 分类,系列 角轴的系列位置总被忽略，角轴总是堆的情形；
	 */
	double getValueLength(Object val, boolean isAbsolute) {
		double len = 0;
		if (isAbsolute) {
			len = ((Number) val).doubleValue();
			len = getSeriesWidth() * len;
		} else {
			Object cat = Utils.parseCategory(val);
			Object ser = Utils.parseSeries(val);
			int catIndex = categories.firstIndexOf(cat);
			if (catIndex == 0)
				throw new RuntimeException(Dot.NOT_IN_DEFINE + ":" + cat);
			double j = 0;
			double serCount = t_serNum;
			if (ser == null) { // 此时计算的是分类标签的位置
				j = serCount / 2f; // 标签位置相对于系列宽度，居中
			} else {
				int serIndex = series.firstIndexOf(ser);
				if (serIndex == 0)
					throw new RuntimeException(Dot.NOT_IN_DEFINE + ":" + ser);
				j = (serIndex - 1) + 0.5; // 位置为柱子顶部中间，所以再加
											// 0.5个系列宽度
											// ,跟只有分类时统一，点坐标为柱子的顶部中间点
			}

			switch (location) {
			case Consts.AXIS_LOC_H:
			case Consts.AXIS_LOC_V:
			case Consts.AXIS_LOC_POLAR:
				// 对于直角坐标系，累积是相同分类的各系列累加，所以计算枚举坐标时都按分类来计算
				len = catIndex * t_categorySpan
						+ ((catIndex - 1) * serCount + j) * t_seriesWidth;// getLeftX()
																			// +
				break;
			case Consts.AXIS_LOC_ANGLE:
				// 对于直角坐标系，累积是相同分类的各系列累加，所以计算枚举坐标时都按分类来计算
				len = catIndex * t_categorySpan
						+ ((catIndex - 1) * serCount + j) * t_seriesWidth;// getLeftX()
				if(isCircleAngle()){
//					角轴范围是整圆时，减掉第一个分类的宽度，让第一个分类落在极轴位置
					double tmp = t_categorySpan+ (serCount / 2f) * t_seriesWidth;
					len -= tmp;
				}
				break;
			}
		}
		return len;
	}

	private static void putAData(Sequence container, Object data, boolean putCategory) {
		if (data == null) {
			return;
		}
		Object tmp = null;
		Object cat = Utils.parseCategory(data);
		Object ser = Utils.parseSeries(data);

		if (putCategory) {
			tmp = cat;
		} else if (ser != null) {
			tmp = ser;
		}

		if (tmp != null && container.firstIndexOf(tmp) == 0) {
			container.add(tmp);
		}
	}

	/**
	 * 绘图前准备工作
	 */
	public void beforeDraw() {
		double length = getAxisLength();
		// 角轴的角域是圆时，首跟尾是同一个分类，要少一个分类Gap
		if (location == Consts.AXIS_LOC_ANGLE && isCircleAngle()) {
			t_seriesWidth = length
					/ ((t_catNum * gapRatio) + t_catNum * t_serNum);
		} else {
			t_seriesWidth = length
					/ (((t_catNum + 1) * gapRatio) + t_catNum * t_serNum);
		}
		t_categorySpan = t_seriesWidth * (gapRatio);

	}

	/**
	 * 从数据序列data中抽取所有分类的名称
	 * @param data 符合分类描述的数据串序列
	 * @return 分类名构成的序列
	 */
	public static Sequence extractCatNames(Sequence data){
		int dSize = data.length();
		Sequence catNames = new Sequence();
		for (int j = 1; j <= dSize; j++) {
			Object one = data.get(j);
			putAData(catNames, one, true);
		}
		return catNames;
	}
	
	/**
	 * 从数据序列data中抽取所有系列的名称
	 * @param data 符合分类系列描述的数据串序列
	 * @return 系列名构成的序列
	 */
	public static Sequence extractSerNames(Sequence data){
		int dSize = data.length();
		Sequence serNames = new Sequence();
		for (int j = 1; j <= dSize; j++) {
			Object one = data.get(j);
			putAData(serNames, one, false);
		}
		return serNames;
	}
	
	/**
	 * 绘图前准备工作，数据校验
	 * 枚举轴对应的图元数据格式为两种： 1， 分类值； 2, 分类值,系列值
	 * @param dataElements 数据图元列表
	 */
	public void prepare(ArrayList<DataElement> dataElements) {
		super.prepare(dataElements);

		if (categories == null) {
			categories = new Sequence();
			for (int i = 0; i < dataElements.size(); i++) {
				DataElement de = dataElements.get(i);
				if(de.isPhysicalCoor()){
					continue;
				}
				
				Sequence data = de.getAxisData(name);
				if (data == null) {
					continue;
				}
				Sequence catNames = extractCatNames(data);
				for( int n=1; n<=catNames.length(); n++){
					Object cat = catNames.get(n);
					if( categories.contains(cat, false)) continue;
					categories.add(cat);
				}
			}
		}

		if (series == null) {
			series = new Sequence();
			for (int i = 0; i < dataElements.size(); i++) {
				DataElement de = (DataElement) dataElements.get(i);
				if(de.isPhysicalCoor()){
					continue;
				}
				
				Sequence data = de.getAxisData(name);
				if (data == null) {
					continue;
				}
				Sequence serNames = extractSerNames(data);
				for( int n=1; n<=serNames.length(); n++){
					Object ser = serNames.get(n);
					if( series.contains(ser, false)) continue;
					series.add(ser);
				}
			}
		}

		t_catNum = categories.length();
		if( t_catNum==0 )throw new RQException("Empty categories data of EnumAxis:[ "+name+" ]!");
		Object catVal = categories.get(1);
		if (!(catVal instanceof String)) {
			throw new RQException(
					"Category value must be 'String' type,current value is: "+catVal+",  and it's type is: "
							+ catVal.getClass().getName());
		}
		t_serNum = series.length() == 0 ? 1 : series.length();// 没有系列时，系列数目为1计算
		if (series.length() > 0) {
			Object serVal = series.get(1);
			if (!(serVal instanceof String)) {
				throw new RQException(
						"Series value must be 'String' type,current value is: "+serVal+",  and it's type is: "
								+ serVal.getClass().getName());
			}
		}

		t_coorValue.addAll(categories);
	}

	/**
	 * 获取图元绘制后对应超链接的空间形状
	 * 
	 * @return Shape 无意义，返回null
	 */
	public Shape getShape() {
		return null;
	}

	/**
	 * 获取系列的宽度，单位像素，为了防止误差，图元计算时
	 * 即使是像素值，也都采用double实数
	 * @return 系列宽度
	 */
	public double getSeriesWidth() {
		return t_seriesWidth;
	}

	// 枚举轴的枚举个数
	private transient int t_catNum = 0;

	// 轴上系列数
	private transient int t_serNum = 1;

	private transient double t_categorySpan = 190, t_seriesWidth = 0;

	/**
	 * 获取编辑参数信息列表
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		
		ParamInfo.setCurrent(EnumAxis.class, this);

		String group = "enumaxis";
		paramInfos.add(group, new ParamInfo("categories"));
		paramInfos.add(group, new ParamInfo("series"));
		paramInfos.add(group, new ParamInfo("gapRatio", Consts.INPUT_DOUBLE));
		// paramInfos.add(group, new ParamInfo("coorWidthRate",
		// Consts.INPUT_DOUBLE));

		paramInfos.addAll(super.getParamInfoList());
		return paramInfos;
	}

	
	/**
	 * 是否枚举轴
	 * @return true
	 */
	public boolean isEnumAxis() {
		return true;
	}

	/**
	 * 是否日期轴
	 * @return false
	 */
	public boolean isDateAxis() {
		return false;
	}

	/**
	 * 是否数值轴
	 * @return false
	 */
	public boolean isNumericAxis() {
		return false;
	}

	public void checkDataMatch(Sequence data){
		if(data!=null && data.length()>1){
			Object one = data.get(1);
			if(!(one instanceof String)){
				throw new RuntimeException("Axis "+name+" is enumeration axis, error data got:" + one);
			}
		}
	}
	
	public double animateDoubleValue(Object val){
		throw new RuntimeException("Enumeration axis does not support animate double value.");
	}
}
