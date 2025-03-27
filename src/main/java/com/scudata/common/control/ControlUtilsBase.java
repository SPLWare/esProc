package com.scudata.common.control;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.HashMap;

import com.scudata.app.common.StringUtils2;
import com.scudata.common.StringUtils;

/**
 * The basic tool class of the control
 *
 */
public class ControlUtilsBase {
	/**
	 * 记住字符的宽度，加速绘图速度
	 */
	private static HashMap<String, Integer> fmWidthBuf = new HashMap<String, Integer>();
	/**
	 * 空列表
	 */
	private static ArrayList<String> emptyArrayList = new ArrayList<String>();
	/**
	 * 缓存文本折行的映射表。KEY是字符串，VALUE是折行后的文本列表
	 */
	public static HashMap<String, ArrayList<String>> wrapStringBuffer = new HashMap<String, ArrayList<String>>();

	/**
	 * 折行
	 * 
	 * @param text
	 *            要折行的文本
	 * @param fm
	 *            FontMetrics
	 * @param w
	 *            宽度
	 * @return
	 */
	public static ArrayList<String> wrapString(String text, FontMetrics fm,
			int w) {
		return wrapString(text, fm, w, -1);
	}

	/**
	 * 获取字符串的显示宽度
	 * 
	 * @param fm
	 *            FontMetrics
	 * @param text
	 *            要显示的字符串
	 * @return 宽度
	 */
	public static int stringWidth(FontMetrics fm, String text) {
		String key = fm.hashCode() + "," + text.hashCode();
		Integer val = fmWidthBuf.get(key);
		int width;
		if (val == null) {
			width = fm.stringWidth(text);
			val = new Integer(width);
			fmWidthBuf.put(key, val);
		} else {
			width = val.intValue();
		}
		return width;
	}

	/**
	 * 折行
	 * 
	 * @param text
	 *            要折行的文本
	 * @param fm
	 *            FontMetrics
	 * @param w
	 *            宽度
	 * @param maxRow
	 *            最大行数(超过此行数的就不要了)
	 * @return
	 */
	public static ArrayList<String> wrapString(String text, FontMetrics fm,
			int w, int maxRow) {
		if (!StringUtils.isValidString(text) || w < 1) {
			return emptyArrayList;
		}
		boolean isExp = text != null && text.startsWith("=");
		String hashKey = text.hashCode() + "" + fm.hashCode() + w + maxRow;
		ArrayList<String> wrapedString = wrapStringBuffer.get(hashKey);
		if (wrapedString == null) {
			if (isExp) {
				wrapedString = StringUtils2.wrapExpString(text, fm, w, false,
						maxRow);
			} else {
				// String \n do not break lines, only line breaks char is
				// allowed
				// text = StringUtils.replace(text, "\\n", "\n");
				text = StringUtils.replace(text, "\t", "        ");

				if (text.indexOf('\n') < 0 && stringWidth(fm, text) < w) {
					wrapedString = new ArrayList<String>();
					wrapedString.add(text);
					if (maxRow > 0 && wrapedString.size() > maxRow) {
						wrapStringBuffer.put(hashKey, wrapedString);
						return wrapedString;
					}
				} else {
					// 在jdk6，New Times Roman字体使用LineBreakMeasurer出现jvm退出异常。
					// 搞不清楚最早是什么时候使用的LineBreakMeasurer，暂时替换成报表的折行方法。wunan
					// 2018-05-29
					wrapedString = StringUtils2.wrapString(text, fm, w, false,
							maxRow);
				}
			}
			wrapStringBuffer.put(hashKey, wrapedString);
		}
		return wrapedString;
	}
}
