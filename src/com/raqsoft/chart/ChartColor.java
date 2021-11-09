package com.raqsoft.chart;

import java.awt.*;

import com.raqsoft.dm.*;

/**
 * 用于描述填充颜色的颜色类
 * @author Joancy
 *
 */
public class ChartColor {
	public static Integer transparentColor = new Integer(0xffffff);
//	填充类型
	int type = Consts.PATTERN_DEFAULT;
	
//是否为渐变色
	boolean isGradient = true;

//	如果是渐变色，渐变色1；非渐变色时只使用颜色1
	Color color1 = (Color) Para.getDefPalette().get(0);
	
//	如果是渐变色，渐变色2
	Color color2 = color1;

	//渐变色的角度方向	
	int angle = 0;
	// 以color1为圆心，按角度angle向color2梯度
	// 颜色解释说明；
	// 1: 当isGradient＝false时；图块使用纯色color1填充；
	// 2: 当
	// isGradient＝true时；color2＝＝color1，则使用color1为基础的cubeColor
	// 否则使用从color1到color2的渐变色

	/**
	 * 构造一个缺省的填充颜色类
	 */
	public ChartColor() {
	}

	/**
	 * 使用单一颜色c构造一个简单的填充颜色类
	 */
	public ChartColor(Color c) {
		color1 = c;
		color2 = c;
	}

	/**
	 * 使用单一颜色c(RGB值)构造一个简单的填充颜色类
	 */
	public ChartColor(int c) {
		setColor1(c);
		setColor2(c);
	}
/**
 * 设置填充类型
 * @param type 类型参考Consts.PATTERN开头的常量，PATTERN_DEFAULT缺省模式采用颜色填充，其他模式用于黑白图形时的各种类型。
 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * 获取填充类型
	 * @return 填充类型
	 */
	public int getType() {
		return type;
	}

/**
 * 设置渐变色填充，渐变色时，color1一般不能跟color2相同，否则没效果
 * @param gradient 是否渐变颜色
 */
	public void setGradient(boolean gradient) {
		this.isGradient = gradient;
	}

	/**
	 * 是否为渐变色填充
	 * @return 使用渐变色时返回true，否则false
	 */
	public boolean isGradient() {
		//如果设置的透明色，没有炫和渐变效果
		if( color1==null ) return false;
		return isGradient;
	}

	/**
	 * 是否使用炫颜色，即使用CubeColor，炫颜色指柱形图的各个立面，使用不同饱和度的颜色构成立体柱，使得立体柱看起来比较炫。
	 * @return 使用炫颜色时返回true，否则false
	 */
	public boolean isDazzle(){
		if( color1==null ) return false;
		return isGradient && color1.equals(color2);
	}
	
	/**
	 * 设置颜色1
	 * @param color 颜色值
	 */
	public void setColor1(Color color) {
		this.color1 = color;
	}

	/**
	 * 使用RGB值设置颜色1
	 * @param color 颜色值
	 */
	public void setColor1(int color) {
		if(color==transparentColor){
			setColor1(null);
		}else{
			setColor1(new Color(color));
		}
	}

	/**
	 * 设置颜色2
	 * @param color 颜色值
	 */
	public void setColor2(Color color) {
		this.color2 = color;
	}

	/**
	 * 使用RGB值设置颜色2
	 * @param color 颜色值
	 */
	public void setColor2(int color) {
		if(color==transparentColor){
			setColor2(null);
		}else{
			setColor2(new Color(color));
		}
	}

	/**
	 * 获取颜色1
	 * @return Color颜色
	 */
	public Color getColor1() {
		return color1;
	}

	/**
	 * 获取颜色2
	 * @return Color颜色
	 */
	public Color getColor2() {
		return color2;
	}

	/**
	 * 设置渐变颜色的渐变角度
	 * @param angle 角度值
	 */
	public void setAngle(int angle) {
		this.angle = angle;
	}

	/**
	 * 获取渐变颜色的渐变角度
	 * @return 角度值
	 */
	public int getAngle() {
		return angle;
	}

	/**
	 * 将该类的属性设置生成plot函数的描述串 
	 * @return 符合SPL语法的串描述
	 */
	public String toPlotString() {
		Sequence seq = new Sequence();
		seq.add("ChartColor");
		seq.add(new Integer(type));
		seq.add(new Boolean(isGradient));
		if (color1 == null) {
			seq.add( transparentColor );
		} else {
			seq.add(new Integer(color1.getRGB()));
		}
		if (color2 == null) {
			seq.add(transparentColor);
		} else {
			seq.add(new Integer(color2.getRGB()));
		}
		seq.add(new Integer(angle));
		return seq.toString();
	}

	/**
	 * 生成文本串描述，同方法toPlotString
	 */
	public String toString() {
		return toPlotString();
	}

	/**
	 * SPL语法的串描述，形成序列后，再实例化为本类
	 * @param seq 颜色描述的序列
	 * @return 实例化后的本类对象
	 */
	public static ChartColor getInstance(Sequence seq) {
		ChartColor cc = new ChartColor();
		cc.setType(((Number) seq.get(2)).intValue());
		cc.setGradient(((Boolean) seq.get(3)).booleanValue());
		Object obj = seq.get(4);
		if (obj != null) {
			cc.setColor1(((Number) obj).intValue());
		}
		obj = seq.get(5);
		if (obj != null) {
			cc.setColor2(((Number) obj).intValue());
		}
		cc.setAngle(((Number) seq.get(6)).intValue());
		return cc;
	}

	/**
	 * 深度克隆一个ChartColor对象
	 * @return 克隆后的填充颜色类
	 */
	public ChartColor deepClone() {
		ChartColor cc = new ChartColor();
		cc.setType(type);
		cc.setGradient(isGradient);
		cc.setColor1(color1);
		cc.setColor2(color2);
		cc.setAngle(angle);
		return cc;
	}
}
