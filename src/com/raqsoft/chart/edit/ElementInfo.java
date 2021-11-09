package com.raqsoft.chart.edit;

import java.util.*;
import com.raqsoft.chart.*;
import com.raqsoft.expression.*;
import com.raqsoft.common.*;

/**
 * 图元信息登记类，通过登记后的图元会自动列出到编辑界面的下拉列表里面
 * @author Joancy
 *
 */
public class ElementInfo {
	private String name;
	private String title;
	private Class elementClass;

	private ArrayList chartParams;

	/**
	 * 缺省构造函数
	 */
	public ElementInfo() {
	}

	/**
	 * 根据指定值构造图元信息
	 * @param name 名称
	 * @param title 标题
	 * @param elementClass 图元的实现类
	 */
	public ElementInfo(String name, String title, Class elementClass) {
		this.name = name;
		this.title = title;
		this.elementClass = elementClass;
	}

	private ParamInfoList listParamInfoList() {
		try {
			IElement element = (IElement) elementClass.newInstance();
			ParamInfoList pil = element.getParamInfoList();
			return pil;
		} catch (Exception x) {
			x.printStackTrace();
		}
		return null;
	}

	/**
	 * 实例化一个图元
	 * @return 返回基类图元对象
	 */
	public ObjectElement getInstance() {
		try {
			return (ObjectElement) elementClass.newInstance();
		} catch (Exception x) {
			x.printStackTrace();
		}
		return null;
	}

	/**
	 * 从plotString内容读取编辑信息
	 * @param plotString plot格式的文本串
	 */
	public void setPlotString(String plotString) {
		int len = plotString.length();
		String paramString = plotString.substring(5, len - 1);
		ArgumentTokenizer at = new ArgumentTokenizer(paramString, ',');
		name = Escape.removeEscAndQuote(at.next());
		ElementInfo ei = ElementLib.getElementInfo(name);
		this.title = ei.getTitle();
		this.elementClass = ei.getElementClass();

		chartParams = new ArrayList();
		while (at.hasNext()) {
			ChartParam cp = new ChartParam();
			cp.setPlotString(at.next());
			chartParams.add(cp);
		}
	}

	/**
	 * 将编辑文本所在的参数列表内容转换为plot格式文本串
	 * @param pil 编辑好参数的参数信息列表
	 * @return plot格式文本串
	 */
	public String toPlotString(ParamInfoList pil) {
		StringBuffer sb = new StringBuffer("plot(");
		sb.append("\"" + name + "\"");
		List allParams = pil.getAllParams();
		int size = allParams.size();
		for (int i = 0; i < size; i++) {
			ParamInfo pi = (ParamInfo) allParams.get(i);
			String paramPlot = pi.toPlotString(pi.getDefValue());
			if (paramPlot == null)
				continue;
			sb.append(",");
			sb.append(paramPlot);
		}
		sb.append(")");
		return sb.toString();
	}

	public void setProperties(String elementName, HashMap<String,Object> properties) {
		name = elementName;
		ElementInfo ei = ElementLib.getElementInfo(name);
		this.title = ei.getTitle();
		this.elementClass = ei.getElementClass();

		chartParams = new ArrayList();
		Iterator it = properties.keySet().iterator();
		while (it.hasNext()) {
			String pName = (String)it.next();
			Object pValue = properties.get(pName);
			ChartParam cp = new ChartParam(pName,pValue);
			chartParams.add(cp);
		}
	}
	
	public HashMap<String,Object> getProperties(ParamInfoList pil) {
		HashMap<String,Object> properties = new HashMap<String,Object>();
		List allParams = pil.getAllParams();
		int size = allParams.size();
		for (int i = 0; i < size; i++) {
			ParamInfo pi = (ParamInfo) allParams.get(i);
			String paramPlot = pi.toPlotString(pi.getDefValue());
			if (paramPlot == null)
				continue;
			properties.put(pi.getName(),pi.getValue());
		}
		return properties;
	}

	public void setChartParams(ArrayList chartParams) {
		this.chartParams = chartParams;
	}

	/**
	 * 将图元的参数内容转为编辑用的参数信息列表
	 * @return 编辑用参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = listParamInfoList();
		if (chartParams == null)
			return paramInfos;
		int chartSize = chartParams.size();
		for (int i = 0; i < chartSize; i++) {
			ChartParam cp = (ChartParam) chartParams.get(i);
			ParamInfo pi = paramInfos.getParamInfoByName(cp.getName());
			if (pi == null) {// 被废弃了的参数
				continue;
			}
			pi.setChartParam(cp);
		}
		return paramInfos;
	}

	/**
	 * 获取图元名称
	 * @return 名称
	 */
	public String getName() {
		return name;
	}

	/**
	 * 获取图元标题
	 * @return 标题
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * 获取图元的实现类
	 * @return 图元类
	 */
	public Class getElementClass() {
		return elementClass;
	}

}
