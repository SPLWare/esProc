package com.scudata.chart.graph;

import com.scudata.cellset.graph.PublicProperty;
import com.scudata.cellset.graph.config.GraphTypes;
import com.scudata.chart.*;
import com.scudata.chart.edit.*;

/**
 * 折线图形
 * 
 * @author Joancy
 *
 */
public class GraphLine extends GraphElement {
	public boolean drawLineDot = false;
	public boolean drawLineTrend = false;
	public boolean drawShade = false;
	public boolean ignoreNull = true;
	/** 折线图直线粗细度 */
	public int lineThick = 2;
	public int lineStyle = Consts.LINE_SOLID;
	public byte lineType = GraphTypes.GT_LINE; 

	/**
	 * 缺省参数构造函数
	 */
	public GraphLine() {
	}

	protected PublicProperty getPublicProperty() {
		PublicProperty pp = super.getPublicProperty();
		pp.setDrawLineDot(drawLineDot);
		pp.setDrawLineTrend(drawLineTrend);
		pp.setDrawShade(drawShade);
		pp.setIgnoreNull(ignoreNull);
		pp.setLineThick((byte)lineThick);
		pp.setLineStyle((byte)lineStyle);
		
		pp.setType( lineType );
		return pp;
	}

	/**
	 * 获取编辑参数信息列表
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(GraphLine.class, this);
		paramInfos.add( new ParamInfo("lineType", Consts.INPUT_LINETYPE));
		String group  ="line";
		paramInfos.add(group, new ParamInfo("drawLineDot", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("drawLineTrend", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("drawShade", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("ignoreNull", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("lineThick", Consts.INPUT_INTEGER));
		paramInfos.add(group,new ParamInfo("lineStyle", Consts.INPUT_LINESTYLE));

		paramInfos.addAll(super.getParamInfoList());
		return paramInfos;
	}
	
}
