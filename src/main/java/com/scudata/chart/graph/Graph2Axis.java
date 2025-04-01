package com.scudata.chart.graph;

import com.scudata.cellset.graph.PublicProperty;
import com.scudata.cellset.graph.config.GraphTypes;
import com.scudata.chart.*;
import com.scudata.chart.edit.*;
import com.scudata.dm.Sequence;

/**
 * 双轴柱线图
 * @author Joancy
 *
 */
public class Graph2Axis extends GraphLine {
	public int yLabelInterval2 = 0;
	public double yStartValue2 = 0;
	public double yEndValue2 = 0;
	
	public Para leftSeries = null;
	public Para rightSeries = null;


	public byte type = GraphTypes.GT_2YCOLLINE;

	/**
	 * 缺省构造函数
	 */
	public Graph2Axis() {
	}

	protected PublicProperty getPublicProperty() {
		PublicProperty pp = super.getPublicProperty();
		if (yLabelInterval + yLabelInterval2 != 0) {
			pp.setYInterval(yLabelInterval + ";" + yLabelInterval2);
		}
		if (yStartValue + yStartValue2 != 0) {
			pp.setYStartValue(yStartValue + ";" + yStartValue2);
		}
		if (yEndValue + yEndValue2 != 0) {
			pp.setYEndValue(yEndValue + ";" + yEndValue2);
		}

		pp.setType(type);
		return pp;
	}

	/**
	 * 获取编辑参数信息列表
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = super.getParamInfoList();//new ParamInfoList();
		paramInfos.delete("lineType");//折线的类型，双轴里面不支持
		paramInfos.delete("line","drawLineTrend");

		ParamInfo.setCurrent(Graph2Axis.class, this);
		paramInfos.add(new ParamInfo("type", Consts.INPUT_2AXISTYPE));
		paramInfos.add(new ParamInfo("leftSeries", Consts.INPUT_NORMAL));
		paramInfos.add(new ParamInfo("rightSeries", Consts.INPUT_NORMAL));

		String group = "YAxisLabels";
		paramInfos.add(group,new ParamInfo("yLabelInterval2", Consts.INPUT_INTEGER));
		paramInfos.add(group,new ParamInfo("yStartValue2", Consts.INPUT_DOUBLE));
		paramInfos.add(group,new ParamInfo("yEndValue2", Consts.INPUT_DOUBLE));

		return paramInfos;
	}

//	是否指定了系列所在轴
	protected boolean isSplitByAxis(){
		return leftSeries!=null || rightSeries!=null;
	}

	private boolean containsName(Para series, String serName){
		Object val = series.getValue();
		if(val instanceof String){
			String str = ","+(String)val+",";
			if(str.indexOf(serName)>0){
				return true;
			}
			return false;
		}else if(val instanceof Sequence){
			Sequence seq = series.sequenceValue();
			Object pos = seq.pos(serName, null);
			if (pos==null){
				return false;
			}else{
				return true;
			}
		}
		throw new RuntimeException("Invalid series name:"+series);
	}
	
	
//	左轴为1， 右轴为2 双轴图覆盖父类该方法，指定系列到正确的轴
//	通常系列只需指定一个轴的所属，剩下的就全是另一个轴，两者都指定时，相当于右轴没意义，全部按照左轴判断，满足左轴的为1，其他的都为2，根是否在右轴的定义无关
	protected byte getSeriesAxis(String serName){
		if(leftSeries!=null){
			if(containsName(leftSeries,serName)){
				return Consts.AXIS_LEFT;
			}else{
				return Consts.AXIS_RIGHT;
			}
		}

		if(rightSeries!=null){
			if(!containsName(rightSeries,serName)){
				return Consts.AXIS_LEFT;
			}else{
				return Consts.AXIS_RIGHT;
			}
		}
//		一个轴都没指定时，使用缺省的左轴值
		return Consts.AXIS_LEFT;
	}

}
