package com.scudata.chart;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

import com.scudata.cellset.graph.config.Palette;
import com.scudata.chart.element.IMapAxis;
import com.scudata.common.Logger;
import com.scudata.dm.*;
import com.scudata.util.Variant;

/**
 * 需要定义多个数据的顺序属性，且属性可以循环对应时，使用该类
 *
 */
public class Para {
//	名字和legendProperty没必要序列化，都在变量初始值里面
	private transient String name;
	private transient byte legendProperty=0;//当前属性对应到图例里面的哪个属性，绘制图例实现了3种属性
	
	private Object value = null;
	private String axis = null;

	private transient Engine e = null;
	private transient static ArrayList<Color> defPalette = null;

	/**
	 * 缺省构造函数
	 */
	public Para() {
	}

	/**
	 * 使用初始值构造参数
	 * @param value
	 */
	public Para(Object value) {
		this.value = value;
	}
	
	/**
	 * 使用对应图例属性类型构造参数
	 * @param legendProperty 对应图例属性值
	 */
	public Para(byte legendProperty) {
		this.legendProperty = legendProperty;
	}

	/**
	 * 使用初始值以及对应图例属性类型构造参数
	 * @param value 初始属性值
	 * @param legendProperty 对应图例属性
	 */
	public Para(Object value,byte legendProperty) {
		this.value = value;
		this.legendProperty = legendProperty;
	}
	
	public Para(Object value, String axis, String name) {
		this.name = name;
		Object tmp = value;
		if (tmp instanceof Sequence) {
			Sequence seq = (Sequence) tmp;
			tmp = Utils.sequenceToChartColor(seq);
			if (tmp == null) { // 如果不是ChartColor,仍然赋值为该序列
				tmp = value;
			}
		}

		this.value = tmp;
		this.axis = axis;
	}

	/**
	 * 设置对应到图例的属性值
	 * @param p 图例属性值
	 */
	public void setLegendProperty(byte p){
		this.legendProperty = p;
	}
	
	/**
	 * 获取图例属性值
	 * @return 属性值
	 */
	public byte getLegendProperty(){
		return legendProperty;
	}
	
	/**
	 * 获取属性值
	 * @return 属性值
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * 设置属性值
	 * @param value 属性值
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 设置环境引擎
	 * @param e 引擎对象
	 */
	public void setEngine(Engine e) {
		this.e = e;
	}

	/**
	 * 如果值为序列，获取序列的值个数
	 * @return 参数值个数
	 */
	public int getLength(){
		return sequenceValue().length();
	}
	
	/**
	 * 如果属性值为填充颜色类ChartColor，或者序列属性值中包含有ChartColor
	 * 获取填充颜色类是否定义了渐变颜色
	 * @return 如果有返回true，否则返回false
	 */
	public boolean hasGradientColor() {
		if (value == null || !(value instanceof Sequence))
			return chartColorValue().isGradient();
		Sequence seq = (Sequence) value;
		int len = seq.length();
		for (int i = 1; i <= len; i++) {
			ChartColor cc = chartColorValue(i);
			if (cc.isGradient())
				return true;
		}
		return false;
	}

	/**
	 * 如果逻辑数据值为序列时，对应逻辑值循环获取该属性值
	 * 比如定义了 红绿蓝 3种颜色， 而逻辑数据有 张王李赵 4个逻辑值时，则 赵 会循环对应为第1个红色
	 * @param pos 逻辑数据的位置序号
	 * @return 根据序号循环对应相应参数值
	 */
	public Object objectValue(int pos) {
		Object val;
		if (value instanceof Sequence) {
			Sequence seq = (Sequence) value;
			pos = pos % seq.length();
			if (pos == 0) {
				pos = seq.length();
			}
			val = seq.get(pos);
		} else if (value instanceof Color) { // 为了不必要的转换，程序中赋值直接用Color对象，但从编辑窗口获取的都是整数
			val = new Integer(((Color) value).getRGB());
		} else {
			val = value;
		}
		// 轴为null时，即直接取设置参数的值，又由于在设置没有Axis的参数时，跟engine无关
		// 所以通常下述两个条件是同时满足的，也即取没有轴的参数值时，e也肯定是null
		if (axis == null || e == null) {
			return val;
		}
		IMapAxis im = e.getMapAxisByName(axis);
		if (im == null) {
			return val;
		}
		if(legendProperty==0){
			throw new RuntimeException("Property "+name+" does not support legend mapping.");
		}
		return im.getMapValue(val, legendProperty);
	}

	/**
	 * 由上层代码事先知道对应的数据类型，如果为整形，取第一个整数
	 * @return 第一个整数值
	 */
	public int intValue() {
		return intValue(0);
	}

	/**
	 * 按照位置获取对应参数的整数值，位置比参数长度要大时，将循环取值
	 * @param pos 参数对应位置
	 * @return 参数相应整数值
	 */
	public int intValue(int pos) {
		Object val = objectValue(pos);
		if (val == null) {
			return 0;
		}
		if (val instanceof Number) {
			return ((Number) val).intValue();
		}
		return Integer.parseInt(val.toString());
	}

	/**
	 * 用法同intValue函数，相应参数参考它
	 * @return 浮点数值
	 */
	public float floatValue() {
		return floatValue(0);
	}

	/**
	 * 用法同intValue函数，相应参数参考它
	 * @param pos 
	 * @return
	 */
	public float floatValue(int pos) {
		Object val = objectValue(pos);
		if (val == null) {
			return 0;
		}
		if (val instanceof Number) {
			return ((Number) val).floatValue();
		}
		return Float.parseFloat(val.toString());
	}

	
	/**
	 * 用法同intValue函数，相应参数参考它
	 * @return 实数值
	 */
	public double doubleValue() {
		return doubleValue(0);
	}

	/**
	 * 用法同intValue函数，相应参数参考它
	 * @param pos 参数位置
	 * @return 实数值
	 */
	public double doubleValue(int pos) {
		Object val = objectValue(pos);
		if (val == null) {
			return 0;
		}
		if (val instanceof Number) {
			return ((Number) val).doubleValue();
		}
		return Double.parseDouble(val.toString());
	}

	/**
	 * 用法同intValue函数，相应参数参考它
	 * @return 日期值
	 */
	public Date dateValue() {
		return dateValue(0);
	}

	/**
	 * 用法同intValue函数，相应参数参考它
	 * @param pos  参数位置
	 * @return 日期值
	 */
	public Date dateValue(int pos) {
		Object val = objectValue(pos);
		if (val == null) {
			return null;
		}
		if (val instanceof Date) {
			return (Date) val;
		}
		val = Variant.parseDate(val.toString());
		if (val instanceof Date) {
			return (Date) val;
		}
		return null;
	}

	/**
	 * 用法同intValue函数，相应参数参考它
	 * @return 布尔值
	 */
	public boolean booleanValue() {
		return booleanValue(0);
	}

	/**
	 * 用法同intValue函数，相应参数参考它
	 * @param pos  参数位置
	 * @return 布尔值
	 */
	public boolean booleanValue(int pos) {
		Object val = objectValue(pos);
		if (val == null) {
			return false;
		}
		if (val instanceof Boolean) {
			return ((Boolean) val).booleanValue();
		}
		return Boolean.valueOf(val.toString()).booleanValue();
	}

	/**
	 * 用法同intValue函数，相应参数参考它
	 * @return 填充颜色值
	 */
	public ChartColor chartColorValue() {
		return chartColorValue(1);
	}

	/**
	 * 用法同intValue函数，相应参数参考它
	 * @param pos  参数位置
	 * @return 填充颜色值
	 */
	public ChartColor chartColorValue(int pos) {
		Object val = objectValue(pos);
		if (val instanceof Sequence) {
			val = Utils.sequenceToChartColor((Sequence) val);
		}
		if (val == null) {
			val = defColorValue(pos);
		}

		ChartColor cc;
		if (val instanceof ChartColor) {
			cc = ((ChartColor) val).deepClone();
		} else if (val instanceof Color) {
			cc = new ChartColor((Color) val);
		} else {
			cc = new ChartColor(Integer.parseInt(val.toString()));
		}
		return cc;
	}

	
	/**
	 * 用法同intValue函数，相应参数参考它
	 * @return 字符串值
	 */
	public String stringValue() {
		return stringValue(0);
	}

	/**
	 * 用法同intValue函数，相应参数参考它
	 * @param pos  参数位置
	 * @return 字符串值
	 */
	public String stringValue(int pos) {
		Object val = objectValue(pos);
		if (val == null) {
			return null;
		}
		return Variant.toString(val);
	}

	/**
	 * 使用缺省的颜色调色板获取颜色循环值
	 * @param pos 逻辑数据对应坐标
	 * @return 颜色值
	 */
	public static Color defColorValue(int pos) {
		pos--; 
		ArrayList<Color> palette = getHexPalette();

		pos = pos % palette.size();
		return palette.get(pos);
	}

	public static Object cycleValue(ArrayList values, int pos) {
		pos = pos % values.size();
		return values.get(pos);
	}

	/**
	 * 方法用在颜色不设置时，返回调色板中颜色
	 * @param pos 位置
	 * @return 颜色
	 */
	public Color colorValueNullAsDef(int pos) {
		Color c = colorValue(pos);
		if( c==null ) return defColorValue(pos);
		return c;
	}
	
	/**
	 * 有部分参数需要解释为没有定义颜色时，即返回null时，用其他属性的颜色 所以，
	 * 并不是所有没定义的颜色都使用系统调色板，故是否使用调色板的颜色
	 * 由上层程序决定，因此该函数仍然会返回null值
	 * 不能返回null的颜色使用colorValueNullAsDef方法。
	 * @param pos int 位置
	 * @return Color 颜色
	 */
	public Color colorValue(int pos) {
		Object val = objectValue(pos);
		if (val == null) {
			return null;
		}

		if (val instanceof Color) {
			return (Color) val;
		} else if (val instanceof ChartColor) {
			return ((ChartColor) val).getColor1();
		} else if (val instanceof Sequence) {
			ChartColor cc = ChartColor.getInstance((Sequence) val);
			return cc.getColor1();
		}
		int ci = Integer.parseInt(val.toString());
		if( ci ==16777215) return null;//透明色时，返回null对象；
		return new Color(ci);
	}

	public Sequence sequenceValue() {
		if (value instanceof Sequence) {
			return (Sequence) value;
		}
		Sequence seq = new Sequence();
		if(value!=null){
			seq.add(value);
		}
		return seq;
	}

	private static String[] hexColors = new String[] { "AFD8F8", "F6BD0F",
			"8BBA00", "FF8E46", "008E8E", "D64646", "8E468E", "588526",
			"B3AA00", "008ED6", "9D080D", "A186BE", "CC6600", "FDC689",
			"ABA000", "F26D7D", "FFF200", "0054A6", "F7941C", "CC3300",
			"006600", "663300", "6DCFF6" };
	private static ArrayList<Color> hexPalette = null;

	public static ArrayList<Color> getHexPalette() {
		if (hexPalette == null) {
			hexPalette = loadConfigFile();
			if(hexPalette!=null) return hexPalette;
			
			hexPalette = new ArrayList<Color>();
			for (int i = 0; i < hexColors.length; i++) {
				String tmp = hexColors[i];
				int r = Integer.parseInt(tmp.substring(0, 2), 16);
				int g = Integer.parseInt(tmp.substring(2, 4), 16);
				int b = Integer.parseInt(tmp.substring(4, 6), 16);
				hexPalette.add(new Color(r, g, b));
			}
		}
		return hexPalette;
	}

	public static ArrayList<Color> getDefPalette() {
		if (defPalette == null) {
			defPalette = loadConfigFile();
			if(defPalette!=null) return defPalette;

			defPalette = new ArrayList<Color>();
			defPalette.add(new Color(128, 128, 0, 255));
			defPalette.add(new Color(255, 128, 0, 255));
			defPalette.add(new Color(192, 255, 0, 255));
			defPalette.add(new Color(0, 0, 128, 255));
			defPalette.add(new Color(128, 0, 128, 255));
			defPalette.add(new Color(255, 0, 128, 255));
			defPalette.add(new Color(0, 128, 128, 255));
			defPalette.add(new Color(128, 128, 128, 255));
			defPalette.add(new Color(0, 255, 255, 255));
			defPalette.add(new Color(192, 192, 192, 255));
			defPalette.add(new Color(255, 128, 128, 255));
			defPalette.add(new Color(0, 255, 128, 255));
			defPalette.add(new Color(192, 255, 128, 255));
			defPalette.add(new Color(255, 255, 0, 255));
			defPalette.add(new Color(255, 255, 128, 255));
			defPalette.add(new Color(128, 0, 255, 255));
			defPalette.add(new Color(255, 0, 255, 255));
			defPalette.add(new Color(0, 128, 255, 255));
			defPalette.add(new Color(128, 128, 255, 255));
			defPalette.add(new Color(255, 128, 255, 255));
			defPalette.add(new Color(192, 255, 255, 255));
			defPalette.add(new Color(255, 0, 0, 255));
			defPalette.add(new Color(0, 255, 0, 255));
			defPalette.add(new Color(0, 0, 255, 255));
			defPalette.add(new Color(0, 128, 0, 255));
			defPalette.add(new Color(255, 255, 255, 255));
		}
		return defPalette;
	}
	
	private static ArrayList<Color> loadConfigFile() {
		try {
			Properties config = new Properties();
			InputStream is = null;
			String name = "/chartcolor.properties";
//			集算器图元会用到报表统计图，而统计图的配置文件为color.properties,且与集算器格式不同
			String relativePath = com.scudata.ide.common.GC.PATH_CONFIG
					+ name;
			File f = new File(
					com.scudata.ide.common.GM.getAbsolutePath(relativePath));
			if (f.exists()) {
				is = new FileInputStream(f);
			} else {
				is = Palette.class.getResourceAsStream(relativePath);
			}

			if (is == null) {
				is = Palette.class
						.getResourceAsStream("/config"+name);
			}
			if (is == null)
				return null;// 没有配色文件
			config.load(is);
			String obj = (String)config.getProperty("default");
			if(obj==null){
				return null;
			}
			if(obj.startsWith("[")){
				obj = obj.substring(1,obj.length()-1);
			}
			StringTokenizer st = new StringTokenizer(obj,",");
			ArrayList<Color> colors = new ArrayList<Color>();
			while (st.hasMoreElements()){
				String tmp = st.nextToken();
				int value = Integer.parseInt(tmp);
				Color c = new Color(value);
				colors.add(c);
			}
			Logger.debug("Load "+name+" OK.");
			return colors;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args){
		getHexPalette();	
	}
}
