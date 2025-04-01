package com.scudata.ide.spl.chart.box;

import java.util.*;

import com.scudata.app.common.*;
import com.scudata.cellset.graph.config.GraphTypes;
import com.scudata.cellset.graph.config.IGraphProperty;
import com.scudata.chart.*;
import com.scudata.common.MessageManager;
import com.scudata.ide.common.*;
import com.scudata.ide.common.swing.*;
import com.scudata.ide.spl.resources.*;

/**
 * 编辑风格的各种下拉对象创建工具类
 * 
 * @author Joancy
 *
 */
public class EditStyles {
	static MessageManager mm = ChartMessage.get();
	/**
	 * 获得字体下拉框
	 * @return JComboBoxEx 字体下拉列表
	 */
	public static JComboBoxEx getFontBox() {
		Vector<String> fontV = new Section( GM.getFontNames()).toVector();
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData(fontV, fontV);
		return box;
	}

	/**
	 * 获得刻度线下拉框
	 * @return JComboBoxEx 刻度线下拉列表
	 */
	public static JComboBoxEx getTicksBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( Consts.TICK_RIGHTUP ) );
		code.add( new Integer( Consts.TICK_LEFTDOWN ) );
		code.add( new Integer( Consts.TICK_CROSS ) );
		code.add( new Integer( Consts.TICK_NONE ) );
		disp.add( mm.getMessage( "options.ticks1" ) );  //"靠右或上" );
		disp.add( mm.getMessage( "options.ticks2" ) );  //"靠左或下" );
		disp.add( mm.getMessage( "options.ticks3" ) );  //"压轴" );
		disp.add( mm.getMessage( "options.ticks4" ) );  //"无刻度线" );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获得坐标单位下拉列表
	 * @return JComboBoxEx 坐标单位下拉列表
	 */
	public static JComboBoxEx getUnitBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( Consts.NUNIT_NONE ) );
		code.add( new Integer( Consts.NUNIT_HUNDREDS ) );
		code.add( new Integer( Consts.NUNIT_THOUSANDS ) );
		code.add( new Integer( Consts.NUNIT_TEN_THOUSANDS ) );
		code.add( new Integer( Consts.NUNIT_HUNDRED_THOUSANDS ) );
		code.add( new Integer( Consts.NUNIT_MILLIONS ) );
		code.add( new Integer( Consts.NUNIT_TEN_MILLIONS ) );
		code.add( new Integer( Consts.NUNIT_HUNDRED_MILLIONS ) );
		code.add( new Integer( Consts.NUNIT_THOUSAND_MILLIONS ) );
		code.add( new Integer( Consts.NUNIT_BILLIONS ) );
		disp.add( mm.getMessage( "options.unit1" ) );  //"无" );
		disp.add( mm.getMessage( "options.unit2" ) );  //"百" );
		disp.add( mm.getMessage( "options.unit3" ) );  //"千" );
		disp.add( mm.getMessage( "options.unit4" ) );  //"万" );
		disp.add( mm.getMessage( "options.unit5" ) );  //"十万" );
		disp.add( mm.getMessage( "options.unit6" ) );  //"百万" );
		disp.add( mm.getMessage( "options.unit7" ) );  //"千万" );
		disp.add( mm.getMessage( "options.unit8" ) );  //"亿" );
		disp.add( mm.getMessage( "options.unit9" ) );  //"十亿" );
		disp.add( mm.getMessage( "options.unit10" ) );  //"万亿" );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获得坐标系下拉列表
	 * @return JComboBoxEx 坐标系下拉列表
	 */
	public static JComboBoxEx getCoordinateBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( Consts.COORDINATES_CARTESIAN ) );
		code.add( new Integer( Consts.COORDINATES_POLAR ) );
		code.add( new Integer( Consts.COORDINATES_CARTE_3D ) );
		code.add( new Integer( Consts.COORDINATES_CARTE_VIRTUAL_3D ) );
		code.add( new Integer( Consts.COORDINATES_POLAR_3D ) );
		code.add( new Integer( Consts.COORDINATES_POLAR_VIRTUAL_3D ) );
		code.add( new Integer( Consts.COORDINATES_LEGEND ) );
		code.add( new Integer( Consts.COORDINATES_FREE ) );
		disp.add( mm.getMessage( "options.coord1" ) );  //"直角坐标系" );
		disp.add( mm.getMessage( "options.coord2" ) );  //"极坐标系" );
		disp.add( mm.getMessage( "options.coord3" ) );  //"立体展现直角坐标系" );
		disp.add( mm.getMessage( "options.coord4" ) );  //"立体效果的平面直角坐标系" );
		disp.add( mm.getMessage( "options.coord5" ) );  //"立体展现极坐标系" );
		disp.add( mm.getMessage( "options.coord6" ) );  //"立体效果的平面极坐标系" );
		disp.add( mm.getMessage( "options.coord7" ) );  //"图例坐标系" );
		disp.add( mm.getMessage( "options.coord8" ) );  //"自由坐标系" );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获得坐标轴位置下拉列表
	 * @return JComboBoxEx 坐标轴位置下拉列表
	 */
	public static JComboBoxEx getAxisBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( Consts.AXIS_LOC_H ) );
		code.add( new Integer( Consts.AXIS_LOC_V ) );
		code.add( new Integer( Consts.AXIS_LOC_POLAR ) );
		code.add( new Integer( Consts.AXIS_LOC_ANGLE ) );
		disp.add( mm.getMessage( "options.axis1" ) );  //"横轴" );
		disp.add( mm.getMessage( "options.axis2" ) );  //"纵轴" );
		disp.add( mm.getMessage( "options.axis3" ) );  //"极轴" );
		disp.add( mm.getMessage( "options.axis4" ) );  //"角轴" );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获得柱型下拉列表
	 * @return JComboBoxEx 柱子下拉列表
	 */
	public static JComboBoxEx getColumnStyleBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( Consts.COL_COBOID ) );
		code.add( new Integer( Consts.COL_CUBE ) );
		code.add( new Integer( Consts.COL_CYLINDER ) );
		disp.add( mm.getMessage( "options.bar1" ) );  //"方柱" );
		disp.add( mm.getMessage( "options.bar2" ) );  //"立体方柱" );
		disp.add( mm.getMessage( "options.bar3" ) );  //"圆柱" );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获得水平对齐下拉列表
	 * @return JComboBoxEx 水平对齐下拉列表
	 */
	public static JComboBoxEx getHAlignBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( Consts.HALIGN_LEFT ) );
		code.add( new Integer( Consts.HALIGN_CENTER ) );
		code.add( new Integer( Consts.HALIGN_RIGHT ) );
		disp.add( mm.getMessage( "options.align1" ) );  //"左对齐" );
		disp.add( mm.getMessage( "options.align2" ) );  //"中对齐" );
		disp.add( mm.getMessage( "options.align3" ) );  //"右对齐" );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获得垂直对齐下拉列表
	 * @return JComboBoxEx 垂直对齐下拉列表
	 */
	public static JComboBoxEx getVAlignBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( Consts.VALIGN_TOP ) );
		code.add( new Integer( Consts.VALIGN_MIDDLE ) );
		code.add( new Integer( Consts.VALIGN_BOTTOM ) );
		disp.add( mm.getMessage( "options.valign1" ) );  //"靠上" );
		disp.add( mm.getMessage( "options.valign2" ) );  //"居中" );
		disp.add( mm.getMessage( "options.valign3" ) );  //"靠下" );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获取背景图填充模式下拉列表
	 * @return 背景图填充模式下拉列表
	 */
	public static JComboBoxEx getImageMode() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( Consts.MODE_NONE ) );
		code.add( new Integer( Consts.MODE_FILL ) );
		code.add( new Integer( Consts.MODE_TILE ) );
		disp.add( mm.getMessage( "options.modenone" ) );  //"左上" );
		disp.add( mm.getMessage( "options.modefill" ) );  //"填充" );
		disp.add( mm.getMessage( "options.modetile" ) );  //"平铺" );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}
	
	/**
	 * 获取图例的图标形状下拉列表
	 * @return 图例的图标形状下拉列表
	 */
	public static JComboBoxEx getLegendIconBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( Consts.LEGEND_RECT ) );
		code.add( new Integer( Consts.LEGEND_POINT ) );
		code.add( new Integer( Consts.LEGEND_LINE ) );
		code.add( new Integer( Consts.LEGEND_LINEPOINT ) );
		code.add( new Integer( Consts.LEGEND_NONE ) );
		disp.add( mm.getMessage( "options.legendicon1" ) );  //"矩形" );
		disp.add( mm.getMessage( "options.legendicon2" ) );  //"点形" );
		disp.add( mm.getMessage( "options.legendicon3" ) );  //"线形" );
		disp.add( mm.getMessage( "options.legendicon4" ) );  //"点线" );
		disp.add( mm.getMessage( "options.legendicon5" ) );  //"无" );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获取日期单位下拉列表
	 * @return 日期单位下拉列表
	 */
	public static JComboBoxEx getDateUnitBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( Consts.DATEUNIT_YEAR ) );
		code.add( new Integer( Consts.DATEUNIT_MONTH ) );
		code.add( new Integer( Consts.DATEUNIT_DAY ) );
		code.add( new Integer( Consts.DATEUNIT_HOUR ) );
		code.add( new Integer( Consts.DATEUNIT_MINUTE ) );
		code.add( new Integer( Consts.DATEUNIT_SECOND ) );
		code.add( new Integer( Consts.DATEUNIT_MILLISECOND ) );
		disp.add( mm.getMessage( "options.dateunit1" ) );  //"年" );
		disp.add( mm.getMessage( "options.dateunit2" ) );  //"月" );
		disp.add( mm.getMessage( "options.dateunit3" ) );  //"日" );
		disp.add( mm.getMessage( "options.dateunit4" ) );  //"时" );
		disp.add( mm.getMessage( "options.dateunit5" ) );  //"分" );
		disp.add( mm.getMessage( "options.dateunit6" ) );  //"秒" );
		disp.add( mm.getMessage( "options.dateunit7" ) );  //"毫秒" );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获取URL目标值下拉列表
	 * @return URL目标值下拉列表
	 */
	public static JComboBoxEx getUrlTargetBox() {
		Vector<String> code = new Vector<String>();
		Vector<String> disp = new Vector<String>();
		code.add( "_self" );
		code.add( "_blank" );
		code.add( "_parent" );
		code.add( "_top" );
		disp.add( mm.getMessage( "options.urltarget1" ) );  //"本窗口" );
		disp.add( mm.getMessage( "options.urltarget2" ) );  //"新窗口" );
		disp.add( mm.getMessage( "options.urltarget3" ) );  //"父窗口" );
		disp.add( mm.getMessage( "options.urltarget4" ) );  //"顶层窗口" );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		box.setEditable( true );
		return box;
	}

	/**
	 * 获得数值变换类型下拉列表
	 * @return JComboBoxEx 数值变换类型下拉列表
	 */
	public static JComboBoxEx getTransformBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( Consts.TRANSFORM_NONE ) );
		code.add( new Integer( Consts.TRANSFORM_SCALE ) );
		code.add( new Integer( Consts.TRANSFORM_LOG ) );
		code.add( new Integer( Consts.TRANSFORM_EXP ) );
		disp.add( mm.getMessage( "options.transform1" ) );  //"不变换" );
		disp.add( mm.getMessage( "options.transform2" ) );  //"比例" );
		disp.add( mm.getMessage( "options.transform3" ) );  //"对数" );
		disp.add( mm.getMessage( "options.transform4" ) );  //"指数" );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获取堆积图类型下拉列表
	 * @return 堆积图类型下拉列表
	 */
	public static JComboBoxEx getStackTypeBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( Consts.STACK_NONE ) );
		code.add( new Integer( Consts.STACK_PERCENT ) );
		code.add( new Integer( Consts.STACK_VALUE ) );
		disp.add( mm.getMessage( "options.stackNone" ) );  //"不堆积" );
		disp.add( mm.getMessage( "options.stackPercent" ) );  //"百分比堆积" );
		disp.add( mm.getMessage( "options.stackValue" ) );  //"原值堆积" );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获取数值显示类型下拉列表
	 * @return 数值显示类型下拉列表
	 */
	public static JComboBoxEx getDisplayDataBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( IGraphProperty.DISPDATA_NONE) );
		code.add( new Integer( IGraphProperty.DISPDATA_PERCENTAGE) );
		code.add( new Integer( IGraphProperty.DISPDATA_VALUE) );
		code.add( new Integer( IGraphProperty.DISPDATA_NAME_PERCENTAGE) );
		code.add( new Integer( IGraphProperty.DISPDATA_NAME_VALUE) );
		disp.add( mm.getMessage( "options.dispDataNone" ) );  //"不显示
		disp.add( mm.getMessage( "options.dispDataPercent" ) );  //"百分比显示
		disp.add( mm.getMessage( "options.dispDataValue" ) );  //"原值显示
		disp.add( mm.getMessage( "options.dispDataNamePercent" ) );  //名称和百分比
		disp.add( mm.getMessage( "options.dispDataNameValue" ) );  //名称和值
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获取图例方位下拉列表
	 * @return 图例方位下拉列表
	 */
	public static JComboBoxEx getLegendLocationBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( IGraphProperty.LEGEND_NONE) );
		code.add( new Integer( IGraphProperty.LEGEND_TOP) );
		code.add( new Integer( IGraphProperty.LEGEND_BOTTOM) );
		code.add( new Integer( IGraphProperty.LEGEND_LEFT) );
		code.add( new Integer( IGraphProperty.LEGEND_RIGHT) );
		disp.add( mm.getMessage( "options.dispDataNone" ) );  //"不显示
		disp.add( mm.getMessage( "options.top" ) );
		disp.add( mm.getMessage( "options.bottom" ) );
		disp.add( mm.getMessage( "options.left" ) );
		disp.add( mm.getMessage( "options.right" ) );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}
	
	/**
	 * 获取条形码类型下拉列表
	 * @return 条形码类型下拉列表
	 */
	public static JComboBoxEx getBarcodeType() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( Consts.TYPE_NONE) );
		code.add( new Integer( Consts.TYPE_CODABAR) );
		code.add( new Integer( Consts.TYPE_CODE128) );
		code.add( new Integer( Consts.TYPE_CODE128A) );
		code.add( new Integer( Consts.TYPE_CODE128B) );
		code.add( new Integer( Consts.TYPE_CODE128C) );
		code.add( new Integer( Consts.TYPE_CODE39) );
		code.add( new Integer( Consts.TYPE_EAN13) );
		code.add( new Integer( Consts.TYPE_EAN8) );
		code.add( new Integer( Consts.TYPE_ITF) );
		code.add( new Integer( Consts.TYPE_PDF417) );
		code.add( new Integer( Consts.TYPE_UPCA) );
		code.add( new Integer( Consts.TYPE_QRCODE) );
		
		disp.add( mm.getMessage( "options.dispDataNone" ) );  //"不显示
		disp.add( "Codabar" );
		disp.add( "Code128Auto" );
		disp.add( "Code128A" );
		disp.add( "Code128B" );
		disp.add( "Code128C" );
		disp.add( "Code39" );
		disp.add( "Ean13" );
		disp.add( "Ean8" );
		disp.add( "ITF" );
		disp.add( "PDF417" );
		disp.add( "UPCA" );
		disp.add( "QRCode" );
		
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}
	
	/**
	 * 获取字符集下拉列表
	 * @return 字符集下拉列表
	 */
	public static JComboBoxEx getCharSet() {
		Vector<String> code = new Vector<String>();
		code.add( "" );
		code.add( "UTF-8" );
		code.add( "GBK" );
		code.add( "iso-8859-1" );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, code );
		return box;
	}

	/**
	 * 获取二维码容错率下拉列表
	 * @return 二维码容错率下拉列表
	 */
	public static JComboBoxEx getRecError() {
		Vector<String> code = new Vector<String>();//L(7%)、M(15%)、Q(25%)、H(30%)
	    Vector<String> disp = new Vector<String>();
	    code.add("L");
	    code.add("M");
	    code.add("Q");
	    code.add("H");

	    disp.add("L(7%)");
	    disp.add("M(15%)");
	    disp.add("Q(25%)");
	    disp.add("H(30%)");
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获取柱状图下拉列表
	 * @return 柱状图下拉列表
	 */
	public static JComboBoxEx getInputColumnTypeBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( GraphTypes.GT_COL) );
		code.add( new Integer( GraphTypes.GT_COL3D) );
		code.add( new Integer( GraphTypes.GT_COL3DOBJ) );
		code.add( new Integer( GraphTypes.GT_COLSTACKED) );
		code.add( new Integer( GraphTypes.GT_COLSTACKED3DOBJ) );
		code.add( new Integer( GraphTypes.GT_BAR) );
		code.add( new Integer( GraphTypes.GT_BAR3DOBJ) );
		code.add( new Integer( GraphTypes.GT_BARSTACKED) );
		code.add( new Integer( GraphTypes.GT_BARSTACKED3DOBJ) );
		disp.add( mm.getMessage("graphType.1") );
		disp.add( mm.getMessage("graphType.2") );
		disp.add( mm.getMessage("graphType.3") );
		disp.add( mm.getMessage("graphType.4") );
		disp.add( mm.getMessage("graphType.5") );
		disp.add( mm.getMessage("graphType.12") );
		disp.add( mm.getMessage("graphType.14") );
		disp.add( mm.getMessage("graphType.15") );
		disp.add( mm.getMessage("graphType.16") );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获取线形图下拉列表
	 * @return 线形图下拉列表
	 */
	public static JComboBoxEx getInputLineTypeBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( GraphTypes.GT_LINE) );
		code.add( new Integer( GraphTypes.GT_LINE3DOBJ) );
		code.add( new Integer( GraphTypes.GT_CURVE) );
		code.add( new Integer( GraphTypes.GT_RADAR) );
		disp.add( mm.getMessage("graphType.8") );
		disp.add( mm.getMessage("graphType.9") );
		disp.add( mm.getMessage("graphType.29") );
		disp.add( mm.getMessage("graphType.22") );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获取饼状图下拉列表
	 * @return 饼状图下拉列表
	 */
	public static JComboBoxEx getInputPieTypeBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( GraphTypes.GT_PIE) );
		code.add( new Integer( GraphTypes.GT_PIE3DOBJ) );
		disp.add( mm.getMessage("graphType.6") );
		disp.add( mm.getMessage("graphType.7") );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}

	/**
	 * 获取双轴图形下拉列表
	 * @return 双轴图形下拉列表
	 */
	public static JComboBoxEx getInput2AxisTypeBox() {
		Vector<Integer> code = new Vector<Integer>();
		Vector<String> disp = new Vector<String>();
		code.add( new Integer( GraphTypes.GT_2YCOLLINE) );
		code.add( new Integer( GraphTypes.GT_2Y2LINE) );
		disp.add( mm.getMessage("graphType.20") );
		disp.add( mm.getMessage("graphType.21") );
		JComboBoxEx box = new JComboBoxEx();
		box.x_setData( code, disp );
		return box;
	}
}

