package com.scudata.chart.graph;

import java.awt.Color;

import com.scudata.cellset.graph.PublicProperty;
import com.scudata.cellset.graph.config.AlarmLine;
import com.scudata.cellset.graph.config.GraphFont;
import com.scudata.cellset.graph.config.GraphFonts;
import com.scudata.cellset.graph.config.IGraphProperty;
import com.scudata.chart.Consts;
import com.scudata.chart.Para;
import com.scudata.chart.edit.*;
import com.scudata.dm.*;
import com.scudata.util.Variant;

//图形图元
public class GraphElement extends GraphBase {
	/** 横轴标题 */
	public String xTitle;
	public String xTitleFont;// = "Dialog";
	public boolean xTitleBold = false;
	public boolean xTitleVertical = false;
	public int xTitleSize = 12;
	public Color xTitleColor = Color.black;
	public int xTitleAngle = 0;

	/** 纵轴标题 */
	public String yTitle;
	public String yTitleFont;// = "Dialog";
	public boolean yTitleBold = false;
	public boolean yTitleVertical = false;
	public int yTitleSize = 12;
	public Color yTitleColor = Color.black;
	public int yTitleAngle;

	/** 横轴标签 */
	public String xLabelFont;// = "Dialog";
	public boolean xLabelBold = false;
	public boolean xLabelVertical = false;
	public int xLabelSize = 12;
	public Color xLabelColor = Color.black;
	public int xLabelAngle = 0;
	public int xLabelInterval = 0;

	/** 纵轴 */
	public String yLabelFont;// = "Dialog";
	public boolean yLabelBold = false;
	public boolean yLabelVertical = false;
	public int yLabelSize = 12;
	public Color yLabelColor = Color.black;
	public int yLabelAngle=0;
	public int yLabelInterval=0;
	public double yStartValue=0;
	public double yEndValue=0;
	public double dataUnit = IGraphProperty.UNIT_ORIGIN;
	public int yMinMarks = 2;

	// 网格线
	/** 网格线类型 */
	public int gridLineType = Consts.LINE_DASHED;
	/** 网格线颜色 */
	public Color gridLineColor = Color.lightGray;

	/* 警戒线定义 */
	public Para warnLineStyle = new Para(new Integer(Consts.LINE_DASHED));
	public Para warnLineWeight = new Para(new Float(1));
	public Para warnLineColor = new Para(Color.red);
	public Sequence warnLineData = null;

	/** 坐标轴颜色 */
	public Color axisTopColor = Color.white;
	public Color axisBottomColor = Color.lightGray;
	public Color axisLeftColor = Color.lightGray;
	public Color axisRightColor = Color.white;

	/** 图形区背景颜色 */
	public Color graphBackColor = Color.white;

	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos =new ParamInfoList();
		ParamInfo.setCurrent(GraphElement.class, this);

		String group;
		group = "XAxisTitle";
		paramInfos.add(group, new ParamInfo("xTitle", Consts.INPUT_NORMAL));
		paramInfos.add(group, new ParamInfo("xTitleFont", Consts.INPUT_FONT));
		paramInfos.add(group, new ParamInfo("xTitleBold", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("xTitleVertical", Consts.INPUT_CHECKBOX));
		paramInfos.add(group,
				new ParamInfo("xTitleSize", Consts.INPUT_FONTSIZE));
		paramInfos.add(group, new ParamInfo("xTitleColor", Consts.INPUT_COLOR));
		paramInfos.add(group,
				new ParamInfo("xTitleAngle", Consts.INPUT_INTEGER));

		group = "YAxisTitle";
		paramInfos.add(group, new ParamInfo("yTitle", Consts.INPUT_NORMAL));
		paramInfos.add(group, new ParamInfo("yTitleFont", Consts.INPUT_FONT));
		paramInfos.add(group, new ParamInfo("yTitleBold", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("yTitleVertical", Consts.INPUT_CHECKBOX));
		paramInfos.add(group,
				new ParamInfo("yTitleSize", Consts.INPUT_FONTSIZE));
		paramInfos.add(group, new ParamInfo("yTitleColor", Consts.INPUT_COLOR));
		paramInfos.add(group,
				new ParamInfo("yTitleAngle", Consts.INPUT_INTEGER));

		group = "XAxisLabels";
		paramInfos.add(group, new ParamInfo("xLabelFont", Consts.INPUT_FONT));
		paramInfos.add(group, new ParamInfo("xLabelBold", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("xLabelVertical", Consts.INPUT_CHECKBOX));
		paramInfos.add(group,
				new ParamInfo("xLabelSize", Consts.INPUT_FONTSIZE));
		paramInfos.add(group, new ParamInfo("xLabelColor", Consts.INPUT_COLOR));
		paramInfos.add(group,
				new ParamInfo("xLabelAngle", Consts.INPUT_INTEGER));
		paramInfos.add(group, new ParamInfo("xLabelInterval",
				Consts.INPUT_INTEGER));

		group = "YAxisLabels";
		paramInfos.add(group, new ParamInfo("yLabelFont", Consts.INPUT_FONT));
		paramInfos.add(group, new ParamInfo("yLabelBold", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("yLabelVertical", Consts.INPUT_CHECKBOX));
		paramInfos.add(group,
				new ParamInfo("yLabelSize", Consts.INPUT_FONTSIZE));
		paramInfos.add(group, new ParamInfo("yLabelColor", Consts.INPUT_COLOR));
		paramInfos.add(group,
				new ParamInfo("yLabelAngle", Consts.INPUT_INTEGER));
		paramInfos.add(group, new ParamInfo("yLabelInterval",
				Consts.INPUT_INTEGER));
		paramInfos
				.add(group, new ParamInfo("yStartValue", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("yEndValue", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("yMinMarks", Consts.INPUT_INTEGER));

		group = "GridLine";
		paramInfos.add(group, new ParamInfo("gridLineType",
				Consts.INPUT_LINESTYLE));
		paramInfos.add(group,
				new ParamInfo("gridLineColor", Consts.INPUT_COLOR));

		group = "WarnLines";
		paramInfos.add(group,
				new ParamInfo("warnLineData", Consts.INPUT_NORMAL));
		paramInfos.add(group, new ParamInfo("warnLineStyle",
				Consts.INPUT_LINESTYLE));
		paramInfos.add(group, new ParamInfo("warnLineWeight",
				Consts.INPUT_DOUBLE));
		paramInfos.add(group,
				new ParamInfo("warnLineColor", Consts.INPUT_COLOR));

		group = "axisColor";
		paramInfos.add(group,new ParamInfo("axisTopColor", Consts.INPUT_COLOR));
		paramInfos.add(group,new ParamInfo("axisBottomColor", Consts.INPUT_COLOR));
		paramInfos.add(group,new ParamInfo("axisLeftColor", Consts.INPUT_COLOR));
		paramInfos.add(group,new ParamInfo("axisRightColor", Consts.INPUT_COLOR));

		group = "graphColor";
		paramInfos.add(group, new ParamInfo("graphBackColor",
				Consts.INPUT_COLOR));
		
		paramInfos.addAll(super.getParamInfoList());
		return paramInfos;
	}

	protected GraphFonts getGraphFonts() {
		
		GraphFonts gfs = super.getGraphFonts();
		GraphFont gf  = getGraphFont(xTitleFont, xTitleBold, xTitleVertical, xTitleSize, xTitleColor,
				xTitleAngle);
		gfs.setXTitleFont(gf);
		gf = getGraphFont(yTitleFont, yTitleBold, yTitleVertical, yTitleSize, yTitleColor,
				yTitleAngle);
		gfs.setYTitleFont(gf);
		gf = getGraphFont(xLabelFont, xLabelBold, xLabelVertical, xLabelSize, xLabelColor,
				xLabelAngle);
		gfs.setXLabelFont(gf);
		gf = getGraphFont(yLabelFont, yLabelBold, yLabelVertical, yLabelSize, yLabelColor,
				yLabelAngle);
		gfs.setYLabelFont(gf);
		return gfs;
	}

	protected AlarmLine[] getWarnLines() {
		if (warnLineData == null)
			return null;

		AlarmLine[] als = new AlarmLine[warnLineData.length()];
		for (int i = 1; i <= warnLineData.length(); i++) {
			AlarmLine al = new AlarmLine();
			al.setAlarmValue(Variant.toString(warnLineData.get(i)));
			al.setLineType((byte) warnLineStyle.intValue(i));
			al.setLineThick((byte) warnLineWeight.floatValue(i));
			al.setColor(warnLineColor.colorValue(i).getRGB());
			als[i - 1] = al;
		}
		return als;
	}

	// 子图形根据自己的属性，构建PublicProperty对象返回给GraphElement.
	protected PublicProperty getPublicProperty() {
		PublicProperty pp = super.getPublicProperty();
		pp.setXTitle(xTitle);
		pp.setYTitle(yTitle);
		pp.setXInterval(xLabelInterval);
		String buf = yLabelInterval + "";
		if(yLabelInterval!=0){
			pp.setYInterval(yLabelInterval+"");
		}
		buf = yStartValue + "";
		if(yStartValue!=0){
			pp.setYStartValue(buf);
		}
		buf = yEndValue + "";
		if(yEndValue!=0){
		pp.setYEndValue(buf);
		}
		pp.setYMinMarks(yMinMarks);

		pp.setGridLineType((byte) gridLineType);
		pp.setGridLineColor(gridLineColor.getRGB());

		pp.setAxisColor(PublicProperty.AXIS_TOP,axisTopColor );
		pp.setAxisColor(PublicProperty.AXIS_BOTTOM,axisBottomColor );
		pp.setAxisColor(PublicProperty.AXIS_LEFT,axisLeftColor );
		pp.setAxisColor(PublicProperty.AXIS_RIGHT,axisRightColor );
		pp.setGraphBackColor(graphBackColor);

		return pp;
	}

}
