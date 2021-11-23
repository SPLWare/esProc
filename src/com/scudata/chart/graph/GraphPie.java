package com.scudata.chart.graph;

import java.awt.Color;

import com.scudata.cellset.graph.PublicProperty;
import com.scudata.cellset.graph.config.GraphTypes;
import com.scudata.chart.*;
import com.scudata.chart.edit.*;

/**
 * 饼图
 * 
 * @author Joancy
 *
 */
public class GraphPie extends GraphBase {
	/** 饼图中是否分离出一扇显示 */
	public boolean pieSpacing = true;
	public int pieRotation = 50; /* 纵轴占横轴的长度百分比 */
	public int pieHeight = 70; /* 饼型图的高度占半径的百分比<=100 */
	
	public byte pieType = GraphTypes.GT_PIE; 
	public Color pieJoinLineColor = Color.lightGray;

	/**
	 * 缺省参数构造函数
	 */
	public GraphPie() {
	}

	protected PublicProperty getPublicProperty() {
		PublicProperty pp = super.getPublicProperty();
		pp.setPieSpacing(pieSpacing);
		pp.setPieRotation(pieRotation);
		pp.setPieHeight(pieHeight);
		pp.setAxisColor(PublicProperty.AXIS_PIEJOIN,pieJoinLineColor );

		pp.setType( pieType );
		return pp;
	}

	/**
	 * 获取编辑参数信息列表
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();

		ParamInfo.setCurrent(GraphPie.class, this);
		paramInfos.add(new ParamInfo("pieType", Consts.INPUT_PIETYPE));
		
		paramInfos.add(new ParamInfo("pieSpacing", Consts.INPUT_CHECKBOX));
		paramInfos.add(new ParamInfo("pieRotation", Consts.INPUT_INTEGER));
		paramInfos.add(new ParamInfo("pieHeight", Consts.INPUT_INTEGER));
		paramInfos.add(new ParamInfo("pieJoinLineColor", Consts.INPUT_COLOR));

		paramInfos.addAll(super.getParamInfoList());
		return paramInfos;
	}
	
}
