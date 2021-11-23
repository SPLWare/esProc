package com.raqsoft.chart.element;

import java.awt.geom.*;
import java.util.*;

import com.raqsoft.dm.*;
import com.raqsoft.chart.*;
import com.raqsoft.chart.edit.*;
import com.raqsoft.util.*;
/**
 * 日期轴
 * 数据类型为Date的轴，为了易用性，数据也可以写成串类型的日期
 * @author Joancy
 *
 */
public class DateAxis extends TickAxis {
	// 自动计算最大小值的范围
	public boolean autoCalcValueRange = true;
	// 值轴属性，最大值
	public Date endDate = GregorianCalendar.getInstance().getTime();

	// 值轴属性，最小值
	public Date beginDate = endDate;

	// 值轴属性，刻度单位
	public int scaleUnit = Consts.DATEUNIT_DAY;

	// 值轴属性，刻度显示格式
	public String format = "yyyy/MM/dd";

	/**
	 * 缺省值构造函数
	 */
	public DateAxis() {
	}

	/**
	 * 日期轴图元的编辑参数信息列表
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(DateAxis.class, this);

		String group = "dateaxis";
		paramInfos.add(group, new ParamInfo("autoCalcValueRange",
				Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("endDate", Consts.INPUT_DATE));
		paramInfos.add(group, new ParamInfo("beginDate", Consts.INPUT_DATE));
		paramInfos
				.add(group, new ParamInfo("scaleUnit", Consts.INPUT_DATEUNIT));
		paramInfos.add(group, new ParamInfo("format"));

		paramInfos.addAll(super.getParamInfoList());
		return paramInfos;
	}

	/**
	 * 将日期按getTime()取整数，以及数值类型的值，返回实数
	 * 该函数的作用主要用于计算日期以及数值的绝对区间
	 * @param date 可以为数值，日期，或者串描述的日期类型
	 * @return 实数精度的绝对值
	 */
	public static double getDoubleDate(Object date) {
		if (date instanceof Number) {
			return ((Number) date).doubleValue();
		} else if (date instanceof Date) {
			return ((Date) date).getTime();
		} else {
			Object obj = Variant.parseDate(date.toString());
			if (obj instanceof Date) {
				return ((Date) obj).getTime();
			} else {
				throw new RuntimeException("Wrong data: " + date
						+ " on calculating on axis. ");// + axisName);
			}
		}
	}

	/*
	 * @return Object，结果为到轴原点的长度 如果是极轴则为极轴长度 如果是角轴则为绝对角度
	 */
	double getValueLength(Object val, boolean isAbsolute) {
		double len = 0;
		double axisLen = getAxisLength();
		if (isAbsolute) {
			len = ((Number) val).doubleValue();
			long valMillSecs = (long) (len * 24 * 60 * 60 * 1000);// 将val代表的天数转换为毫秒数
			len = axisLen * (valMillSecs / (t_maxDate - t_minDate));
		} else {
			double tmp = getDoubleDate(val);
			len = axisLen * (tmp - t_minDate) / (t_maxDate - t_minDate);
		}

		return len;
	}

	/**
	 * 获取基值的坐标点
	 * 对于柱图元的绘制，一般是从基值画到另一高度
	 * @return Point 坐标点
	 */
	public Point2D getRootPoint() {
		switch (location) {
		case Consts.AXIS_LOC_H:
		case Consts.AXIS_LOC_POLAR:
			return new Point2D.Double(t_valueBaseLine, getBottomY());
		case Consts.AXIS_LOC_V:
			return new Point2D.Double(getLeftX(), t_valueBaseLine);
		}
		return null;
	}


	/**
	 * 获取v跟序列d中所有值比较后的最大值
	 * @param v 值序列
	 * @param d 待比较的值
	 * @return 最大值
	 */
	public static  double max(double v, Sequence d) {
		Sequence al = (Sequence) d;
		double max = getDoubleDate(al.max());
		if (v > max) {
			return v;
		}
		return max;
	}

	/**
	 * 获取v跟序列d中所有值比较后的最小值
	 * @param v 值序列
	 * @param d 待比较的值
	 * @return 最小值
	 */
	public static double min(double v, Sequence d) {
		Sequence al = (Sequence) d;
		double min = getDoubleDate(al.min());
		if (v < min) {
			return v;
		}
		return min;
	}

	/**
	 * 绘图前的数据准备工作
	 */
	public void prepare(ArrayList<DataElement> dataElements) {
		super.prepare(dataElements);

		if (autoCalcValueRange) {
			for (int i = 0; i < dataElements.size(); i++) {
				DataElement de = dataElements.get(i);
				if(de.isPhysicalCoor()){
					continue;
				}
				
				Sequence data = de.getAxisData(name);
				t_minDate = min(t_minDate, data);
				t_maxDate = max(t_maxDate, data);
				if(de instanceof Column){
					Column col = (Column)de;
					data = col.getData3();
					if(data!=null && data.length()>0){
						Object one = data.get(1);
						if(one instanceof Date){
							t_minDate = min(t_minDate, data);
							t_maxDate = max(t_maxDate, data);}
					}
				}
			}
		} else {
			t_maxDate = Math.max(endDate.getTime(), beginDate.getTime());
			t_minDate = Math.min(endDate.getTime(), beginDate.getTime());
		}

		Date start = new Date((long)t_minDate);
		Date end = new Date((long)t_maxDate);

		// 日期标签tick已经按照tickStep过滤了刻度，所以产生刻度后，tickStep重置为1
		createCoorValue(start, end, scaleUnit, displayStep, t_coorValue);
		displayStep = 1;
	}

	private void createCoorValue(Date start, Date end, int scale, int step,
			Sequence v) {
		if (step < 1) {
			step = 1;
		}
		GregorianCalendar gc = new GregorianCalendar();
		switch (scale) {
		case Consts.DATEUNIT_YEAR:
			gc.setTime(end);
			int endY = gc.get(GregorianCalendar.YEAR);
			gc.setTime(start);
			int startY = gc.get(GregorianCalendar.YEAR);
			while (startY <= endY) {
				Date d = gc.getTime();
				v.add(d);
				gc.add(GregorianCalendar.YEAR, step);
				startY = gc.get(GregorianCalendar.YEAR);
			}
			break;
		case Consts.DATEUNIT_MONTH:
			gc.setTime(end);
			endY = gc.get(GregorianCalendar.YEAR);
			int endM = gc.get(GregorianCalendar.MONTH);
			gc.setTime(start);
			startY = gc.get(GregorianCalendar.YEAR);
			int startM = gc.get(GregorianCalendar.MONTH);
			while (startY < endY || (startY == endY && startM <= endM)) {
				Date d = gc.getTime();
				v.add(d);
				gc.add(GregorianCalendar.MONTH, step);
				startY = gc.get(GregorianCalendar.YEAR);
				startM = gc.get(GregorianCalendar.MONTH);
			}
			break;
		case Consts.DATEUNIT_DAY:
			gc.setTime(end);
			endY = gc.get(GregorianCalendar.YEAR);
			endM = gc.get(GregorianCalendar.MONTH);
			int endD = gc.get(GregorianCalendar.DAY_OF_MONTH);
			gc.setTime(start);
			startY = gc.get(GregorianCalendar.YEAR);
			startM = gc.get(GregorianCalendar.MONTH);
			int startD = gc.get(GregorianCalendar.DAY_OF_MONTH);
			while (startY < endY
					|| (startY == endY && (startM < endM || (startM == endM && startD <= endD)))) {
				Date d = gc.getTime();
				v.add(d);
				gc.add(GregorianCalendar.DAY_OF_MONTH, step);
				startY = gc.get(GregorianCalendar.YEAR);
				startM = gc.get(GregorianCalendar.MONTH);
				startD = gc.get(GregorianCalendar.DAY_OF_MONTH);
			}
			break;
		case Consts.DATEUNIT_HOUR:
			gc.setTime(end);
			endY = gc.get(GregorianCalendar.YEAR);
			endM = gc.get(GregorianCalendar.MONTH);
			endD = gc.get(GregorianCalendar.DAY_OF_MONTH);
			int endH = gc.get(GregorianCalendar.HOUR_OF_DAY);
			gc.setTime(start);
			startY = gc.get(GregorianCalendar.YEAR);
			startM = gc.get(GregorianCalendar.MONTH);
			startD = gc.get(GregorianCalendar.DAY_OF_MONTH);
			int startH = gc.get(GregorianCalendar.HOUR_OF_DAY);
			while (startY < endY
					|| (startY == endY && (startM < endM || (startM == endM && (startD < endD || (startD == endD && startH <= endH)))))) {
				Date d = gc.getTime();
				v.add(d);
				gc.add(GregorianCalendar.HOUR_OF_DAY, step);
				startY = gc.get(GregorianCalendar.YEAR);
				startM = gc.get(GregorianCalendar.MONTH);
				startD = gc.get(GregorianCalendar.DAY_OF_MONTH);
				startH = gc.get(GregorianCalendar.HOUR_OF_DAY);
			}
			break;
		case Consts.DATEUNIT_MINUTE:
			gc.setTime(end);
			endY = gc.get(GregorianCalendar.YEAR);
			endM = gc.get(GregorianCalendar.MONTH);
			endD = gc.get(GregorianCalendar.DAY_OF_MONTH);
			endH = gc.get(GregorianCalendar.HOUR_OF_DAY);
			int endMM = gc.get(GregorianCalendar.MINUTE);
			gc.setTime(start);
			startY = gc.get(GregorianCalendar.YEAR);
			startM = gc.get(GregorianCalendar.MONTH);
			startD = gc.get(GregorianCalendar.DAY_OF_MONTH);
			startH = gc.get(GregorianCalendar.HOUR_OF_DAY);
			int startMM = gc.get(GregorianCalendar.MINUTE);
			while (startY < endY
					|| (startY == endY && (startM < endM || (startM == endM && (startD < endD || (startD == endD
							&& startH < endH || (startH == endH && startMM <= endMM))))))) {
				Date d = gc.getTime();
				v.add(d);
				gc.add(GregorianCalendar.MINUTE, step);
				startY = gc.get(GregorianCalendar.YEAR);
				startM = gc.get(GregorianCalendar.MONTH);
				startD = gc.get(GregorianCalendar.DAY_OF_MONTH);
				startMM = gc.get(GregorianCalendar.MINUTE);
				startH = gc.get(GregorianCalendar.HOUR_OF_DAY);
			}
			break;
		case Consts.DATEUNIT_SECOND:
			gc.setTime(end);
			endY = gc.get(GregorianCalendar.YEAR);
			endM = gc.get(GregorianCalendar.MONTH);
			endD = gc.get(GregorianCalendar.DAY_OF_MONTH);
			endH = gc.get(GregorianCalendar.HOUR_OF_DAY);
			endMM = gc.get(GregorianCalendar.MINUTE);
			int endS = gc.get(GregorianCalendar.SECOND);
			gc.setTime(start);
			startY = gc.get(GregorianCalendar.YEAR);
			startM = gc.get(GregorianCalendar.MONTH);
			startD = gc.get(GregorianCalendar.DAY_OF_MONTH);
			startH = gc.get(GregorianCalendar.HOUR_OF_DAY);
			startMM = gc.get(GregorianCalendar.MINUTE);
			int startS = gc.get(GregorianCalendar.SECOND);
			while (startY < endY
					|| (startY == endY && (startM < endM || (startM == endM && (startD < endD || (startD == endD
							&& startH < endH || (startH == endH && (startMM < endMM || (startMM == endMM && startS <= endS))))))))) {
				Date d = gc.getTime();
				v.add(d);
				gc.add(GregorianCalendar.SECOND, step);
				startY = gc.get(GregorianCalendar.YEAR);
				startM = gc.get(GregorianCalendar.MONTH);
				startD = gc.get(GregorianCalendar.DAY_OF_MONTH);
				startMM = gc.get(GregorianCalendar.MINUTE);
				startH = gc.get(GregorianCalendar.HOUR_OF_DAY);
				startS = gc.get(GregorianCalendar.SECOND);
			}
			break;
		}
	}

	String getCoorText(Object coorValue) {
		Date coory = (Date) coorValue;
		return Utils.format(coory, format);
	}

	/**
	 * 每次重绘前根据当前画布的尺寸，计算相关临时绘制参数
	 */
	public void beforeDraw() {
		switch (location) {
		case Consts.AXIS_LOC_H:
		case Consts.AXIS_LOC_POLAR:
			t_valueBaseLine = (int) getLeftX();
			break;
		case Consts.AXIS_LOC_V:
			t_valueBaseLine = (int) getBottomY();
			break;
		case Consts.AXIS_LOC_ANGLE:
			// nothing to do
			break;
		}
	}

	/**
	 * 是否枚举轴
	 * @return false
	 */
	public boolean isEnumAxis(){
		return false;
	}

	/**
	 * 是否日期轴
	 * @return true
	 */
	public boolean isDateAxis(){
		return true;
	}
	
	/**
	 * 是否数值轴
	 * @return false
	 */
	public boolean isNumericAxis(){
		return false;
	}

	public void checkDataMatch(Sequence data){
		if(data!=null && data.length()>1){
			Object one = data.get(1);
			getDoubleDate(one);
		}
	}
	/**
	 * 将逻辑值val转换为时间轴数值
	 */
	public double animateDoubleValue(Object val){
		return getDoubleDate( val );
	}

	// 所有绘制图形相关的中间计算变量都由transient限定词指出，并且变量名由t_作为前缀
	// 数量单位
	private transient double t_maxDate=0, t_minDate=Long.MAX_VALUE;
	private transient int t_valueBaseLine = 0;
}
