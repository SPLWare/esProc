package com.raqsoft.chart.element;

import java.awt.*;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import com.raqsoft.chart.edit.*;
import com.raqsoft.chart.*;
import com.raqsoft.common.ImageUtils;

/**
 * 背景图元
 * 先画背景颜色，再画背景图，如果背景图是透明的，两者才会都生效。
 * 否则图片会盖住颜色 
 * @author Joancy
 *
 */
public class BackGround extends ObjectElement {
	public ChartColor backColor = new ChartColor(Color.white);
	public float transparent = 1f;
	public boolean visible = true;
	// 图片背景
	public Para imageValue = new Para();
	// 图片填充模式
	public int imageMode = Consts.MODE_NONE;

	/**
	 * 图元是否可见
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * 背景图会最先绘制，该层忽略
	 */
	public void beforeDraw() {
	}

	/**
	 * 背景图会最先绘制，该层忽略
	 */
	public void draw() {
	}
	
	/**
	 * 获取图像的原始宽高
	 * @return 宽和高的像素值
	 */
	public int[] getOrginalWH(){
		Object imgVal = imageValue.getValue();
		byte[] imageBytes = Utils.getFileBytes(imgVal);

		if (imageBytes == null)
			return null;
		Image image = new ImageIcon(imageBytes).getImage();
		int iw = image.getWidth(null);
		int ih = image.getHeight(null);
		return new int[]{iw,ih};
	}

	/**
	 * 背景层绘制
	 */
	public void drawBack() {
		if (!isVisible()) {
			return;
		}
		Graphics2D g = e.getGraphics();
		int w = e.getW();
		int h = e.getH();
		Rectangle rect = new Rectangle(0, 0, w, h);
		if (Utils.setPaint(g, 0, 0, w, h, backColor)) {
			Utils.fillPaint(g, rect, transparent);
		}
		Object imgVal = imageValue.getValue();
		byte[] imageBytes = Utils.getFileBytes(imgVal);

		if (imageBytes == null)
			return;
		Image image = new ImageIcon(imageBytes).getImage();

		switch (imageMode) {
		case Consts.MODE_NONE:
			ImageUtils.drawFixImage(g, image, 0, 0, w, h);
			break;
		case Consts.MODE_FILL:
			g.drawImage(image, 0, 0, w, h, null);
			break;
		case Consts.MODE_TILE:
			int iw = image.getWidth(null);
			int ih = image.getHeight(null);
			int x = 0,
			y = 0;
			while (x < w) {
				y = 0;
				while (y < h) {
					ImageUtils.drawFixImage(g, image, x, y, w, h);
					y += ih;
				}
				x += iw;
			}
			break;
		}

	}

	/**
	 * 前景层绘制，忽略该层
	 */
	public void drawFore() {
	}

	/**
	 * 每个图元都需要事先的可编辑参数信息列表
	 * @return ParamInfoList 当前图元的参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(BackGround.class, this);

		paramInfos.add(new ParamInfo("backColor", Consts.INPUT_CHARTCOLOR));
		paramInfos.add(new ParamInfo("transparent", Consts.INPUT_DOUBLE));
		paramInfos.add(new ParamInfo("visible", Consts.INPUT_CHECKBOX));

		paramInfos.add(new ParamInfo("imageValue"));
		paramInfos.add(new ParamInfo("imageMode", Consts.INPUT_IMAGEMODE));

		return paramInfos;
	}

	/**
	 * 图元绘制完成后，产生的链接shape
	 * 当前图元无意义
	 * @return Shape null
	 */
	public ArrayList<Shape> getShapes() {
		return null;
	}

	/**
	 * 图元绘制完成后，产生的超链接列表
	 * 当前图元无意义
	 * @return null
	 */
	public ArrayList<String> getLinks() {
		return null;
	}

	protected transient Engine e;

	/**
	 * 设置图形引擎
	 */
	public void setEngine(Engine e) {
		this.e = e;
		Utils.setParamsEngine(this);
	}

	/**
	 * 获取当前图形引擎
	 * @return 引擎
	 */
	public Engine getEngine() {
		return e;
	}
}
