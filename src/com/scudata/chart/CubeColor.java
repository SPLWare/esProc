package com.scudata.chart;

import java.awt.*;
import java.util.ArrayList;

import com.scudata.dm.*;

/**
 * 柱子配色方案，为某一种颜色配置前面front，顶面top，右面right 共6色，
 * 其中颜色1为浅色，2为深色
 * CubeColor的亮度排序，从亮到暗：T1,F1,T2,F2,R1,R2
 * @author Joancy
 * 
 */
public class CubeColor {
	// 1为浅色，2为深色;缺省的配色为灰色
	Color f1 = new Color(204, 213, 194);// 正面颜色
	Color f2 = new Color(116, 126, 104);
	Color t1 = new Color(225, 230, 218);// 顶面颜色
	Color t2 = new Color(146, 158, 130);
	Color r1 = new Color(106, 115, 93);// 右侧面颜色
	Color r2 = new Color(83, 90, 73);
	
//	由原始颜色产生上述6个饱和度不同的颜色
	private Color origin=null;
	float f1s = 0.55f;
	float f2s = 0.65f;
	float t1s = 0.35f;
	float t2s = 0.85f;
	float r1s = 0.75f;
	float r2s = 0.35f;
	/**
	 * 构造一个缺省的立方体填充颜色类
	 */
	public CubeColor() {
	}

	/**
	 * 使用指定的颜色c1生成类防踢填充颜色类
	 * @param c1 柱子原始颜色
	 */
	public CubeColor(Color c1) {
		origin = c1;
		if(c1!=null){
			f1 = getLight( f1s );
			f2 = getDark( f2s );
			t1 = getLight( t1s );
			t2 = getDark( t2s );
			r1 = getDark( r1s );
			r2 = getDark( r2s );
		}else{
			f1 = null;
			f2 = null;
			t1 = null;
			t2 = null;
			r1 = null;
			r2 = null;
		}
	}

	/**
	 * 获取原始颜色
	 * @return 颜色值
	 */
	public Color getOrigin(){
		return origin;
	}
	
	/**
	 * 获取前面浅色
	 * @return 颜色值
	 */
	public Color getF1() {
		return f1;
	}
	
	/**
	 * 获取相对于定义好的颜色，取更亮几级的颜色，以后都同
	 * @param relative 相对颜色的名称，(F1,F2, T1,T2, R1,R2)
	 * @param degree，比F1颜色亮的级数
	 * @return 颜色值
	 */
	public Color getRelativeBrighter(String relative,int degree) {
		float deltaFactor = degree*0.05f;
		if( relative.equalsIgnoreCase("F1")){
			return getLight(f1s - deltaFactor);
		}
		if( relative.equalsIgnoreCase("T1")){
			return getLight(t1s - deltaFactor);
		}
		float tmpFactor;
		if( relative.equalsIgnoreCase("F2")){
			tmpFactor = f2s;
		}else if(relative.equalsIgnoreCase("T2")){
			tmpFactor = t2s;
		}else if(relative.equalsIgnoreCase("R1")){
			tmpFactor = r1s;
		}else{//(relative.equalsIgnoreCase("R2")){
			tmpFactor = r2s;
		}
		return getDark(tmpFactor + deltaFactor);
	}

	/**
	 * 参考getRelativeBrighter用法，获取深级别的颜色
	 * @param relative
	 * @param degree
	 * @return
	 */
	public Color getRelativeDarker(String relative, int degree) {
		float deltaFactor = degree*0.05f;
		if( relative.equalsIgnoreCase("F1")){
			return getLight(f1s + deltaFactor);
		}
		if( relative.equalsIgnoreCase("T1")){
			return getLight(t1s + deltaFactor);
		}
		float tmpFactor;
		if( relative.equalsIgnoreCase("F2")){
			tmpFactor = f2s;
		}else if(relative.equalsIgnoreCase("T2")){
			tmpFactor = t2s;
		}else if(relative.equalsIgnoreCase("R1")){
			tmpFactor = r1s;
		}else{//(relative.equalsIgnoreCase("R2")){
			tmpFactor = r2s;
		}
		return getDark(tmpFactor - deltaFactor);
	}

	/**
	 * 获取前面深色
	 * @return 颜色值
	 */
	public Color getF2() {
		return f2;
	}

	/**
	 * 获取顶面浅色
	 * @return 颜色值
	 */
	public Color getT1() {
		return t1;
	}

	/**
	 * 获取顶面深色
	 * @return 颜色值
	 */
	public Color getT2() {
		return t2;
	}

	/**
	 * 获取右面浅色
	 * @return 颜色值
	 */
	public Color getR1() {
		return r1;
	}

	/**
	 * 获取右面深色
	 * @return 颜色值
	 */
	public Color getR2() {
		return r2;
	}
	
	//系数越小，颜色越dark
	public Color getDark(float intensity) {
		return getDarkColor(origin,intensity);
	}

	/**
	 * 获取原始颜色的炫色彩，因为炫色要用到很多过渡色，
	 * 比如白色黑色等极端颜色不使用炫色，炫的效果体现不出来
	 * 用该方法获取相近的能使用炫颜色的颜色值
	 * @param origin 原始颜色
	 * @return 最相近的能使用炫颜色的颜色值
	 */
	public static Color getDazzelColor(Color origin){
		CubeColor cc = new CubeColor(origin);
		if( cc.getT1().equals(origin)) return cc.getT2();
		if( cc.getR2().equals(origin)) return cc.getR1();
		return origin;
	}
	

	public static Color getDarkColor(Color sourceHexColor, float intensity) {
		intensity = (((intensity > 1) || (intensity < 0)) ? 1 : (intensity));
		int _local2 = noAlphaRGB(sourceHexColor.getRGB());
		double _local3 = Math.floor(_local2 / 65536);
		double _local4 = Math.floor((_local2 - (_local3 * 65536)) / 256);
		double _local5 = (_local2 - (_local3 * 65536)) - (_local4 * 256);

		int r = (int) (_local3 * intensity);
		int g = (int) (_local4 * intensity);
		int b = (int) (_local5 * intensity);
		return new Color(r, g, b);
	}

	public static int noAlphaRGB(int rgba) {
		int tmp = 0x00FFFFFF;
		return tmp & rgba;
	}
	//系数越小，颜色越light
	public Color getLight(float intensity) {
		return getLightColor(origin,intensity);
	}
	
	public static Color getLightColor(Color sourceHexColor, float intensity) {
		intensity = (((intensity > 1) || (intensity < 0)) ? 1 : (intensity));
		int _local2 = noAlphaRGB(sourceHexColor.getRGB());
		double _local3 = Math.floor(_local2 / 65536);
		double _local4 = Math.floor((_local2 - (_local3 * 65536)) / 256);
		double _local5 = (_local2 - (_local3 * 65536)) - (_local4 * 256);
		int r = (int) (256 - (256 - _local3) * intensity);
		int g = (int) (256 - (256 - _local4) * intensity);
		int b = (int) (256 - (256 - _local5) * intensity);
		return new Color(r, g, b);
	}

	public static void main(String[] args) {
		CubeColor cc = new CubeColor(new Color(255, 255, 255));
		
		System.out.println(cc.getF1());
		System.out.println(cc.getF2());
		System.out.println(cc.getT1());
		System.out.println(cc.getT2());
		System.out.println(cc.getR1());
		System.out.println(cc.getR2());
	}
}
