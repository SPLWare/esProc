package com.scudata.chart;

import java.awt.geom.Point2D;
import java.util.*;

import com.scudata.chart.edit.*;
import com.scudata.chart.element.Column;
import com.scudata.chart.element.DateAxis;
import com.scudata.chart.element.EnumAxis;
import com.scudata.chart.element.Line;
import com.scudata.chart.element.TickAxis;
import com.scudata.chart.element.TimeAxis;
import com.scudata.common.RQException;
import com.scudata.dm.*;

/**
 * 数据图元抽象类
 * @author Joancy
 *
 */
public abstract class DataElement extends LinkElement {
	// 使用的坐标轴1
	public String axis1 = null;

	// 使用的坐标轴2
	public String axis2 = null;

	// 使用的时间轴
	public String axisTime = null;

	
	// 轴1的逻辑坐标，格式[v1,v2,...,vn]或v, 没有映射逻辑轴时， data1为物理的x坐标
	public Sequence data1 = null;

	// 轴2的逻辑坐标，格式[w1,w2,..,wn]或w，物理y坐标
	public Sequence data2 = null;

	// 时间轴的坐标，格式[t1,t2,..,tn],如果使用了时间轴， 则data只能是序列
	public Sequence dataTime = null;

	public boolean visible = true;

	/**
	 * 图元是否可见
	 * @return 可见返回true，否则false
	 */
	public boolean isVisible() {
		return visible;
	}

	//图元可以用于组合，组合轴上的分类和系列，是所有图元的合并值；
//	但是图元自身的绘制不能使用合并值，得用各自自己的分类和系列值
	// 当不同图元的分类和系列不一致时，用合并值导致缺少系列的图元画不出
	public transient Sequence categories = null;
	public transient Sequence series = null;

	protected abstract String getText(int index);

	protected String getTipTitle(int index) {
		String superTitle = super.getTipTitle(index);
		if (superTitle != null) {
			return superTitle;
		}

		if (getText(index) != null) {
			return getText(index);
		}
		Object val1, val2;
		val1 = data1.get(index);
		val2 = data2.get(index);

		return val1.toString() + " " + val2.toString();
	}

	/**
	 * 获取图元所在的坐标系
	 * @return 坐标系接口
	 */
	public ICoor getCoor() {
		ArrayList<ICoor> coorList = e.getCoorList();
		int size = coorList.size();
		for (int i = 0; i < size; i++) {
			ICoor coor = coorList.get(i);
			if (coor.getAxis1().getName().equals(axis1)
					&& coor.getAxis2().getName().equals(axis2)) {
				return coor;
			}
		}
		return null;
	}

	/**
	 * 用于定义生成编辑面板的参数列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(DataElement.class, this);
		paramInfos.add(new ParamInfo("visible", Consts.INPUT_CHECKBOX));
		String group = "data";
		paramInfos.add(group, new ParamInfo("axis1", Consts.INPUT_NORMAL));
		paramInfos.add(group, new ParamInfo("data1", Consts.INPUT_NORMAL));
		paramInfos.add(group, new ParamInfo("axis2", Consts.INPUT_NORMAL));
		paramInfos.add(group, new ParamInfo("data2", Consts.INPUT_NORMAL));

		paramInfos.add(group, new ParamInfo("axisTime", Consts.INPUT_NORMAL));
		paramInfos.add(group, new ParamInfo("dataTime", Consts.INPUT_NORMAL));

		paramInfos.addAll(super.getParamInfoList());
		return paramInfos;
	}

	/**
	 * 获取1轴的名称
	 * @return 轴名字
	 */
	public String getAxis1Name() {
		return axis1;
	}

	/**
	 * 获取2轴的名称
	 * @return 轴名字
	 */
	public String getAxis2Name() {
		return axis2;
	}
	
	/**
	 * 获取时间轴名称
	 * @return 时间轴
	 */
	public String getAxisTimeName() {
		return axisTime;
	}

	private Object parseNumber(Object o) {
		if (o instanceof Number) {
			return o;
		} else {
			return new Double(o.toString());
		}
	}

	private void normalizeNumber(Sequence seq, String desc) {
		int size = seq.length();
		for (int i = 1; i <= size; i++) {
			Object o = seq.get(i);
			if (o == null) {
				throw new RuntimeException(desc
						+ " can not contain null values.");
			} else {
				seq.set(i, parseNumber(o));
			}
		}
	}

	/**
	 * 做数据兼容性，允许用串来表示数值
	 * 
	 * @param numericAxis 数值轴名称
	 */
	public void parseNumericAxisData(String numericAxis) {
		if (isPhysicalCoor()) {
			return;
		}
		Sequence seq = getAxisData(numericAxis);
		normalizeNumber(seq, numericAxis + " Data");
		if (this instanceof Column) {
			Column col = (Column) this;
			Sequence data3 = col.getData3();
			if (data3 != null) {
				normalizeNumber(data3, numericAxis + " Data3");
			}
		}
	}

	/**
	 *获取数据轴名字对应的逻辑数据序列 
	 * @param axisName 数据轴名称 
	 * @return 对应的逻辑数据序列
	 */
	public Sequence getAxisData(String axisName) {
		if (axisName.equals(axis1)) {
			return data1;
		}
		if (axisName.equals(axis2)) {
			return data2;
		}
		return dataTime;
	}

	/**
	 * 设置相应轴的逻辑数据序列
	 * @param axisName 数据轴名称
	 * @param data 逻辑数据序列
	 */
	public void setAxisData(String axisName, Sequence data) {
		if (data == null)
			return;
		if (axisName.equals(axis1)) {
			data1 = data;
		} else if (axisName.equals(axis2)) {
			data2 = data;
		} else {
			dataTime = data;
		}
	}

	/**
	 * 获取指定轴的另一轴的逻辑数据序列
	 * @param axisName 轴名称
	 * @return 坐标系中另一轴的逻辑数据序列
	 */
	public Sequence getOppositeAxisData(String axisName) {
		if (axisName.equals(axis1)) {
			return data2;
		}
		return data1;
	}

	/**
	 * 准备绘图前的数据检查等准备工作
	 */
	public void beforeDraw() {
		if (isPhysicalCoor()) {
			return;
		}
		ICoor coor = getCoor();
		EnumAxis ea = coor.getEnumAxis();
		if (ea != null) {
			Sequence enumData = getAxisData(ea.getName());
			Sequence idED = enumData.id(null);
			if (enumData.length() != idED.length()) {
				throw new RQException("EnumAxis [ " + ea.getName()
						+ " ]'s data exists duplicate item!");
			}
		}
	}

	/**
	 * 返回数据图元是否用到了渐变色，数据图元所在的轴根据是否有渐变色，对轴的部分着色自动渐变配色 轴本身不设置渐变颜色
	 * @return
	 */
	public abstract boolean hasGradientColor();

	protected transient Engine e;

/**
 * 设置绘图引擎
 */
	public void setEngine(Engine e) {
		this.e = e;
	}

	/**
	 * 获取绘图引擎
	 * @return 绘图引擎
	 */
	public Engine getEngine() {
		return e;
	}

	/**
	 * 当前图元是否为物理坐标系
	 * 
	 * @return 物理坐标系时返回true，逻辑坐标系时返回false
	 */
	public boolean isPhysicalCoor() {
		return axis1 == null && axis2 == null;
	}

	/**
	 * 数据图元的准备工作主要是检查属性的合法性， Para参数的引擎环境设置
	 */
	public void prepare() {
		// 由于数据图元本身没有支持Para的对象，故只需下述调用一次，即可完成所有子类的Para类型属性的引擎设置
		Utils.setParamsEngine(this);
		String msg = null;

		if (data1 == null) {
			msg = mm.getMessage("data1") + " can not be empty!";
		} else if (data2 == null) {
			msg = mm.getMessage("data2") + " can not be empty!";
		} else if (data1.length() != data2.length()) {
			msg = "DataElement property 'data' is not match: data1 length="
					+ data1.length() + " data2 length=" + data2.length();
		} else if (dataTime != null && data1.length() != dataTime.length()) {
			msg = "DataElement property 'data' is not match: data1 length="
					+ data1.length() + " dataTime length=" + dataTime.length();
		} else if (this instanceof Column) {
			Column col = (Column) this;
			Sequence data3 = col.getData3();
			if (data3 != null) {
				if (data1.length() != data3.length()) {
					msg = "DataElement property 'data' is not match: data1 length="
							+ data1.length()
							+ " data3 length="
							+ data3.length();
				}
			}
		}

		if (dataTime != null && dataTime.length() < 2) {
			msg = "Animate chart requires at least 2 data.";
		}
		if (msg != null) {
			throw new RuntimeException(msg);
		}
		// 物理坐标系时，后续检查没必要
		if (isPhysicalCoor()) {
			return;
		}

		TickAxis ta = e.getAxisByName(axis1);
		Sequence enumData = null;
		if (ta instanceof EnumAxis) {
			enumData = data1;
		} else {
			ta = e.getAxisByName(axis2);
			if (ta instanceof EnumAxis) {
				enumData = data2;
			}
		}
		if (enumData != null) {
			categories = EnumAxis.extractCatNames(enumData);
			series = EnumAxis.extractSerNames(enumData);
			// 如果有枚举轴，则总是将枚举轴置为axis1，
			// 后续的绘图代码都基于axis1来绘制，改动太麻烦，在这里，将axis1和axis2对调即可
			if (enumData == data2) {
				ICoor ic = getCoor();
				ta = ic.getAxis2();
				ic.setAxis2(ic.getAxis1());
				ic.setAxis1(ta);

				data2 = data1;
				data1 = enumData;

				String tmp = axis2;
				axis2 = axis1;
				axis1 = tmp;
			}
		}
		// 检查轴跟对应数据是否匹配
		ICoor ic = getCoor();
		ic.getAxis1().checkDataMatch(data1);
		ic.getAxis2().checkDataMatch(data2);
	}

	protected int pointSize() {
		return data1.length();
	}

	protected Point2D getScreenPoint(int index) {
		return getScreenPoint(index, false);
	}

	protected Point2D getScreenPoint(int index, boolean discardSeries) {
		Point2D p;
		if (isPhysicalCoor()) {
			double vx = ((Number) data1.get(index)).doubleValue();
			double vy = ((Number) data2.get(index)).doubleValue();
			double px = e.getXPixel(vx);
			double py = e.getYPixel(vy);
			p = new Point2D.Double(px, py);
		} else {
			ICoor coor = getCoor();
			Object v1 = data1.get(index);
			Object v2 = data2.get(index);
			if (discardSeries) {
				v1 = Column.discardSeries(v1);
				v2 = Column.discardSeries(v2);
			}

			if (coor.isCartesianCoor()) {
				p = coor.getScreenPoint(v1, v2);
			} else {
				PolarCoor pc = (PolarCoor) coor;
				p = pc.getScreenPoint(v1, v2);
			}
		}
		return p;
	}

	/**
	 * 采用线性插值算法
	 * @param frameTime
	 * @return
	 */
	public DataElement getFrame(double frameTime) {
		DataElement de = (DataElement) deepClone();
		de.setEngine(e);
		int size = dataTime.length();
		int index = 1;
		for (int i = 1; i <= size; i++) {
			double tmp = DateAxis.getDoubleDate(dataTime.get(i));
			if (tmp > frameTime) {
				break;
			}
			index = i;
		}
		int index1,index2;
		if(index==size){
			index1=index-1;
			index2=index;
		}else{
			index1=index;
			index2=index+1;
		}
		Point2D.Double p1 = new Point2D.Double();
		Point2D.Double p2 = new Point2D.Double();
		double x,y;
		TickAxis ta1=null,ta2=null;
		if( isPhysicalCoor() ){
//			物理坐标系时，直接使用逻辑像素坐标
			x = ((Number)data1.get(index1)).doubleValue();
			y = ((Number)data2.get(index1)).doubleValue();
			p1.setLocation(x, y);
			x = ((Number)data1.get(index2)).doubleValue();
			y = ((Number)data2.get(index2)).doubleValue();
			p2.setLocation(x, y);
		}else{
//			暂时不支持极坐标系
			ta1 = e.getAxisByName(axis1);
			ta2 = e.getAxisByName(axis2);
			Object val;
			if(ta1.getLocation()==Consts.AXIS_LOC_H){
				val = data1.get(index1);
				x = ta1.animateDoubleValue(val);
				val = data2.get(index1);
				y = ta2.animateDoubleValue(val);
				p1.setLocation(x,y);
				
				val = data1.get(index2);
				x = ta1.animateDoubleValue(val);
				val = data2.get(index2);
				y = ta2.animateDoubleValue(val);
				p2.setLocation(x,y);
			}else{
				val = data1.get(index1);
				y = ta1.animateDoubleValue(val);
				val = data2.get(index1);
				x = ta2.animateDoubleValue(val);
				p1.setLocation(x,y);
				
				val = data1.get(index2);
				y = ta1.animateDoubleValue(val);
				val = data2.get(index2);
				x = ta2.animateDoubleValue(val);
				p2.setLocation(x,y);
			}
		}
		
		TimeAxis ta = e.getTimeAxis(getAxisTimeName());
		double time1 = ta.animateDoubleValue( dataTime.get(index1) );
		double time2 = ta.animateDoubleValue( dataTime.get(index2) );
		double timeLength = time2-time1;
		double ratio = (frameTime-time1)/timeLength;//时间点相对于点1的时间长度比值
		double xLength = p2.x - p1.x;

		double cx,cy;//插值逻辑坐标
		cx = p1.x+ ratio*xLength;
		cy = Utils.calcLineY(p1, p2, cx);

		Sequence d1 = new Sequence();
		Sequence d2 = new Sequence();
		if (ta.isKeepTrack()) {
			for (int i = 1; i <= index1; i++) {
				d1.add(data1.get(i));
				d2.add(data2.get(i));
			}
//			补上插值点
			if(ta1!=null && ta1.getLocation()==Consts.AXIS_LOC_V){
				d1.add(cy);
				d2.add(cx);
			}else{
				d1.add(cx);
				d2.add(cy);
			}
		} else {
			// 如果是线，则保持时间点两端的数据点
			if (this instanceof Line) {
				d1.add(data1.get(index1));
				d2.add(data2.get(index1));
			}
//				补上插值点
			if(ta1!=null && ta1.getLocation()==Consts.AXIS_LOC_V){
				d1.add(cy);
				d2.add(cx);
			}else{
				d1.add(cx);
				d2.add(cy);
			}
		}
		de.data1 = d1;
		de.data2 = d2;
		return de;
	}

	
	/**
	 * 克隆数据图元
	 * @param de
	 */
	public void clone(DataElement de) {
		de.axis1 = axis1;
		de.axis2 = axis2;
		de.axisTime = axisTime;
		de.data1 = data1;
		de.data2 = data2;
		de.dataTime = dataTime;
		de.visible = visible;
	}

	public abstract Object deepClone();
}
