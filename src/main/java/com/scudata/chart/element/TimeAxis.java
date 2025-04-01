package com.scudata.chart.element;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.scudata.chart.Consts;
import com.scudata.chart.DataElement;
import com.scudata.chart.Engine;
import com.scudata.chart.IAxis;
import com.scudata.chart.ICoor;
import com.scudata.chart.ObjectElement;
import com.scudata.chart.Para;
import com.scudata.chart.Utils;
import com.scudata.chart.edit.ParamInfo;
import com.scudata.chart.edit.ParamInfoList;
import com.scudata.common.StringUtils;
import com.scudata.dm.Sequence;
import com.scudata.util.Variant;

public class TimeAxis extends ObjectElement implements IAxis{
	// 轴名称
	public String name;
	// 自动计算最大小值的范围
	public boolean autoCalcValueRange = true;
	// 保留轨迹
	public boolean keepTrack = true;

	// 起始时间， 时间可以为日期，数值
	public Object beginTime = 0;
	// 结束时间
	public Object endTime = 10;


	// 图片左下角显示标注
	public boolean displayMark = true;
	public String textFont = "Dialog";
	public int textStyle = new Integer(0);
	public int textSize = new Integer(14);
	public Color textColor = Color.RED;
	public Color backColor = null;
	public double markX = 0.1;
	public double markY = 0.9;
	public String format = null;
	
	private transient double t_maxDate=0, t_minDate=Long.MAX_VALUE;
	private transient boolean isDateType = false;//当前时间轴的数据是否日期类型，不同类型使用不同的format
	public TimeAxis() {
	}

	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(TimeAxis.class, this);

		paramInfos.add(new ParamInfo("name"));
		String group = "axisTime";
		paramInfos.add(group, new ParamInfo("autoCalcValueRange",
				Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("keepTrack",Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("beginTime"));
		paramInfos.add(group, new ParamInfo("endTime"));
		
		group = "marker";
		paramInfos.add(group, new ParamInfo("displayMark", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("textFont", Consts.INPUT_FONT));
		paramInfos.add(group,
				new ParamInfo("textStyle", Consts.INPUT_FONTSTYLE));
		paramInfos.add(group, new ParamInfo("textSize", Consts.INPUT_FONTSIZE));
		paramInfos.add(group, new ParamInfo("textColor", Consts.INPUT_COLOR));
		paramInfos.add(group, new ParamInfo("backColor", Consts.INPUT_COLOR));
		paramInfos.add(group, new ParamInfo("format"));
		paramInfos.add(group, new ParamInfo("markX", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("markY", Consts.INPUT_DOUBLE));

		return paramInfos;
	}

	public void prepare(ArrayList<DataElement> dataElements) {
		if (autoCalcValueRange) {
			for (int i = 0; i < dataElements.size(); i++) {
				DataElement de = dataElements.get(i);
				Sequence data = de.getAxisData(name);
				t_minDate = DateAxis.min(t_minDate, data);
				t_maxDate = DateAxis.max(t_maxDate, data);
			}
		} else {
			double begin = DateAxis.getDoubleDate(beginTime);
			double end = DateAxis.getDoubleDate(endTime);
			t_maxDate = Math.max(begin, end);
			t_minDate = Math.min(begin, end);
		}
//有时间轴时，需要事先将相关数据图元的数据按时间排序
		for(DataElement de:dataElements){
			Sequence posIndex = de.dataTime.psort(null);
			de.dataTime = de.dataTime.get( posIndex );
			Object tmp = de.dataTime.get(1);
			if (tmp instanceof Date) {
				isDateType = true;
			} else {
				Object obj = Variant.parseDate(tmp.toString());
				if (obj instanceof Date) {
					isDateType = true;
				}
			}

			de.data1 = de.data1.get( posIndex );
			de.data2 = de.data2.get( posIndex );
		}
	}

	public double getMaxDate(){
		return t_maxDate;
	}
	public double getMinDate(){
		return t_minDate;
	}
	
	public boolean isKeepTrack(){
		return keepTrack;
	}
	
	public boolean isVisible() {
		return false;
	}

	public void beforeDraw() {
	}

	public void drawBack() {
	}

	public void draw() {
	}

	public void drawFore() {
	}

	public ArrayList<Shape> getShapes() {
		return null;
	}

	public ArrayList<String> getLinks() {
		return null;
	}

	public Point2D getBasePoint(ICoor coor) {
		return null;
	}

	public String getName() {
		return name;
	}

	public int getLocation() {
		return 0;
	}

	public void setEngine(Engine e) {
		this.e = e;
	}

	public Engine getEngine() {
		return e;
	}

	public Text getMarkElement(double timeLocation){
		Object obj;
		if( isDateType ){
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis((long)timeLocation);
			obj = calendar.getTime();
			if(!StringUtils.isValidString(format)){
				format="yyyy/MM/dd";
			}
		}else{
			obj = timeLocation;
			if(!StringUtils.isValidString(format)){
				format="###";
			}
		}
		String mark = Utils.format(obj, format);
		Text txt = new Text();
		txt.setEngine(e);
		txt.text = new Para(mark);
		txt.textFont = new Para(textFont);
		txt.textSize = new Para(textSize);
		txt.textStyle = new Para(textStyle);
		txt.textColor = new Para(textColor);
		txt.backColor = new Para(backColor);
		txt.data1 = new Sequence(new Double[]{markX});
		txt.data2 = new Sequence(new Double[]{markY});
		return txt;
	}
	
	public double animateDoubleValue(Object val){
		return DateAxis.getDoubleDate(val);
	}
	
	protected transient Engine e;
}
