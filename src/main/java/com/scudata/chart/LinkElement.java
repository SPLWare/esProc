package com.scudata.chart;

import java.awt.Shape;
import java.util.ArrayList;

import com.scudata.chart.edit.ParamInfo;
import com.scudata.chart.edit.ParamInfoList;
import com.scudata.common.StringUtils;

/**
 * 超链接图元的抽象类
 * @author Joancy
 *
 */
public abstract class LinkElement extends ObjectElement{
	//超链接不再使用宏，改为由dfx计算出每个data的图元的对应链接序列，不够时，循环使用；
	public Para htmlLink = new Para(null);
	public Para tipTitle = new Para(null);// 当tipTitle没有指定时，默认使用data的数据
	public Para linkTarget = new Para("_blank");

	private transient ArrayList<Shape> shapes = new ArrayList<Shape>();
	private transient ArrayList<String> links = new ArrayList<String>();
	private transient ArrayList<String> titles = new ArrayList<String>();// 鼠标指向区域时，弹出的提示消息
	private transient ArrayList<String> targets = new ArrayList<String>();
	/**
	 * 获取图元的链接边界形状
	 * 
	 * @return Shape 边界形状列表
	 */
	public ArrayList<Shape> getShapes() {
		return shapes;
	}

	/**
	 * 获取超链接列表
	 * @return 超链接
	 */
	public ArrayList<String> getLinks() {
		return links;
	}

	/**
	 * 获取提示信息列表
	 * @return 提示信息
	 */
	public ArrayList<String> getTitles() {
		return titles;
	}

	/**
	 * 获取超链接目标打开方式列表
	 * @return 目标打开方式
	 */
	public ArrayList<String> getTargets() {
		return targets;
	}

	protected String getTipTitle(int index) {
		if (tipTitle != null) {
			String t = tipTitle.stringValue(index);
			if (t != null) {
				return t;//.toString();
			}
		}
		return null;
	}

	protected void addLink(Shape shape, String link, String title,String target) {
		shapes.add(shape);
		if (StringUtils.isValidString(link)) {
			links.add(link);
			titles.add(title);
			targets.add(target);
		}
	}

	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		
		ParamInfo.setCurrent(LinkElement.class, this);
		paramInfos.add(new ParamInfo("htmlLink", Consts.INPUT_NORMAL));
		paramInfos.add(new ParamInfo("tipTitle", Consts.INPUT_NORMAL));
		paramInfos.add(new ParamInfo("linkTarget", Consts.INPUT_NORMAL));

		return paramInfos;
	}

}
