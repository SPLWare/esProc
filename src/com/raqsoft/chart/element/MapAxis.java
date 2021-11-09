package com.raqsoft.chart.element;

import com.raqsoft.chart.*;
import com.raqsoft.dm.*;

import java.util.ArrayList;

import com.raqsoft.chart.edit.ParamInfoList;

import java.awt.Shape;

import com.raqsoft.chart.edit.*;

//映射轴，只作为逻辑值到物理值的一个映射，不实际绘图
public class MapAxis extends ObjectElement implements IMapAxis{
	// 轴名称
	public String name;

	// 逻辑值序列
	public Sequence logicalData;

	// 物理值，图例轴目前可支持颜色、点型、线型
	public Sequence physicalData;

//	映射轴废弃，兼容保留，不关心映射属性
	public Object getMapValue(Object val,byte mapProperty){
		return getPhyValue(val);
	}

	/**
	 * getName
	 * 
	 * @return String
	 */
	public String getName() {
		return name;
	}

	public static Object getMapValue(Sequence s1,Object v1,Object s2) {
		int index = s1.firstIndexOf(v1);
		if (index < 1)
			return v1;
		if(s2 instanceof Sequence ){
			Sequence map = (Sequence)s2;
			int phySize = map.length();
			index = index % phySize;
			if (index == 0) {
				index = phySize;
			}
			return map.get(index);
		}
		Para para = (Para)s2;
		return para.objectValue(index);
	}
	
	public Object getPhyValue(Object lgcValue) {
		return getMapValue(logicalData,lgcValue,physicalData);
	}

	/**
	 * isVisible
	 * 
	 * @return boolean
	 */
	public boolean isVisible() {
		return false;
	}

	public void beforeDraw() {
	}

	/**
	 * draw
	 */
	public void draw() {
	}

	/**
	 * drawBack
	 */
	public void drawBack() {
	}

	/**
	 * drawFore
	 */
	public void drawFore() {
	}

	/**
	 * getEngine
	 * 
	 * @return Engine
	 */
	public Engine getEngine() {
		return null;
	}

	/**
	 * getParamInfoList
	 * 
	 * @return ParamInfoList
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(MapAxis.class, this);
		paramInfos.add(new ParamInfo("name"));
		paramInfos.add(new ParamInfo("logicalData"));
		paramInfos.add(new ParamInfo("physicalData"));
		return paramInfos;
	}

	/**
	 * getShape
	 * 
	 * @return Shape
	 */
	public ArrayList<Shape> getShapes() {
		return null;
	}

	public ArrayList<String> getLinks() {
		return null;
	}

	/**
	 * setEngine
	 * 
	 * @param e
	 *            Engine
	 */
	public void setEngine(Engine e) {
	}

}
