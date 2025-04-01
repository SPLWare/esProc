package com.scudata.cellset.graph;

import java.awt.*;
import java.awt.image.*;

import com.scudata.app.common.*;
import com.scudata.cellset.*;
import com.scudata.cellset.graph.config.*;
import com.scudata.cellset.graph.draw.*;
import com.scudata.common.*;
import com.scudata.dm.*;
import java.io.*;
import javax.swing.*;

/**
 * 整体统计图的计算实现
 * 整体统计图相对于图元不同，整体统计图按照常见统计图配置好所有属性，默认坐标系
 * 图元相当于积木，可以用不同图元以及周组合出各种类似整体统计图
 * @author Joancy
 *
 */
public class StatisticGraph {

	/**
	 * 将共有属性转换为绘图前的扩展图形属性
	 * @param prop 共有属性
	 * @return 扩展图形属性
	 */
	public static ExtGraphProperty calc1(PublicProperty prop) {
		ExtGraphProperty catMap = new ExtGraphProperty(prop);
		catMap.setXTitle(prop.getXTitle());
		catMap.setYTitle(prop.getYTitle());
		catMap.setGraphTitle(prop.getGraphTitle());
		catMap.setBarDistance(prop.getBarDistance());
		catMap.setTopData(prop.getTopData());
		catMap.setBackGraphConfig(prop.getBackGraphConfig());

		String str = prop.getDisplayDataFormat();
		if (StringUtils.isValidString(str)) {
			ArgumentTokenizer st = new ArgumentTokenizer(str, ';');
			if (st.hasMoreTokens()) {
				catMap.setDisplayDataFormat1(st.nextToken());
			}
			if (st.hasMoreTokens()) {
				catMap.setDisplayDataFormat2(st.nextToken());
			}
		}

		catMap.setLink(prop.getLink());
		catMap.setLinkTarget(prop.getLinkTarget());

		/** 统计图的配色方案名 */
		Palette pltt = Palette.getDefaultPalette();
		str = prop.getColorConfig();
		if (StringUtils.isValidString(str)) {
			Palette pl = Palette.readColor(str);
			if (pl != null) {
				pltt = pl;
			}
		}
		catMap.setPalette(pltt);

		/** 统计值起始值 */
		str = prop.getYStartValue();
		if (StringUtils.isValidString(str)) {
			ArgumentTokenizer st = new ArgumentTokenizer(str, ';');
			if (st.hasMoreTokens()) {
				catMap.setYStartValue1(Double.parseDouble(st.nextToken()));
			}
			if (st.hasMoreTokens()) {
				catMap.setYStartValue2(Double.parseDouble(st.nextToken()));
			}
		}

		/** 统计值结束值 */
		str = prop.getYEndValue();
		if (StringUtils.isValidString(str)) {
			ArgumentTokenizer st = new ArgumentTokenizer(str, ';');
			if (st.hasMoreTokens()) {
				catMap.setYEndValue1(Double.parseDouble(st.nextToken()));
			}
			if (st.hasMoreTokens()) {
				catMap.setYEndValue2(Double.parseDouble(st.nextToken()));
			}
		}
		/** 统计值标签间隔 */
		str = prop.getYInterval();
		if (StringUtils.isValidString(str)) {
			ArgumentTokenizer st = new ArgumentTokenizer(str, ';');
			if (st.hasMoreTokens()) {
				catMap.setYInterval1(Double.parseDouble(st.nextToken()));
			}
			if (st.hasMoreTokens()) {
				catMap.setYInterval2(Double.parseDouble(st.nextToken()));
			}
		}

		/** 统计值最少刻度数 */
		catMap.setYMinMarks(prop.getYMinMarks());
		catMap.setTitleMargin(prop.getTitleMargin());
		catMap.setXInterval(prop.getXInterval());
		return catMap;
	}

	/**
	 * 将缓冲图像按照指定格式转为图像数据
	 * @param bi 缓冲图像
	 * @param imageFmt 图片格式
	 * @return 图像数据
	 * @throws Exception
	 */
	public static byte[] getImageBytes(BufferedImage bi, byte imageFmt)
			throws Exception {
		byte[] bytes = null;
		switch (imageFmt) {
		case GraphProperty.IMAGE_GIF:
			bytes = ImageUtils.writeGIF(bi);
			break;
		case GraphProperty.IMAGE_JPG:
			bytes = ImageUtils.writeJPEG(bi);
			break;
		case GraphProperty.IMAGE_PNG:
			bytes = ImageUtils.writePNG(bi);
			break;
		}
		return bytes;
	}

	private static byte[] getFileBytes(String picFile) {
		if (picFile == null || picFile.trim().length() == 0) {
			return null;
		}
		InputStream fis = null;
		try {
			File file = new File(picFile);
			if (file.exists()) { // 绝对路径表示的文件
				fis = new FileInputStream(picFile);
			} else {
				String paths[] = Env.getPaths();
				if (paths != null && paths.length > 0) {
					file = new File(paths[0], picFile);
					fis = new FileInputStream(file);
				}
			}

			if (fis == null) {
				return null;
			}
			return AppUtil.getStreamBytes(fis);
		} catch (Exception e) {
			return null;
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 执行背景图绘制
	 * @param g 图形设备
	 * @param egp 扩展图形属性
	 * @param w 宽度
	 * @param h 高度
	 */
	public static void drawBackGraph(Graphics2D g, ExtGraphProperty egp,
			int w, int h) {
		BackGraphConfig bgc = egp.getBackGraphConfig();
		if (bgc == null) {
			return;
		}
		byte[] b = getFileBytes(bgc.getValue());
		if (b != null) {
			bgc.setImageBytes(b);
		}

		byte[] backImage = bgc.getImageBytes();
		if (backImage == null) {
			return;
		}
		Image image = new ImageIcon(backImage).getImage();
		g.drawImage(image, 1, 1, w, h, null);
	}

}
