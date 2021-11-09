package com.raqsoft.chart.graph;

import java.awt.Color;

import com.raqsoft.cellset.graph.PublicProperty;
import com.raqsoft.cellset.graph.config.GraphTypes;
import com.raqsoft.chart.*;
import com.raqsoft.chart.edit.*;

/** 
 * 柱形图或条形图
 * 
 */
public class GraphColumn extends GraphElement {

	public int barDistance;

	public byte columnType = GraphTypes.GT_COL; 
	public Color columnBorderColor = null;//颜色设置为null表示为透明色；

	/**
	 * 缺省参数构造函数
	 */
	public GraphColumn() {
	}

	protected PublicProperty getPublicProperty() {
		PublicProperty pp = super.getPublicProperty();
		pp.setBarDistance(barDistance);
		pp.setType( columnType );
		pp.setAxisColor(PublicProperty.AXIS_COLBORDER,columnBorderColor );

		return pp;
	}

	/**
	 * 获取编辑参数信息列表
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(GraphColumn.class, this);
		
		paramInfos.add(new ParamInfo("columnType", Consts.INPUT_COLUMNTYPE));
		paramInfos.add(new ParamInfo("barDistance", Consts.INPUT_INTEGER));
		paramInfos.add(new ParamInfo("columnBorderColor", Consts.INPUT_COLOR));

		paramInfos.addAll(super.getParamInfoList());
		return paramInfos;
	}
	
}
