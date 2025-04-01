package com.scudata.cellset;

public interface IStyle {
	/** 类型取值：文本 */
	public final static byte TYPE_TEXT = (byte) 0;
	/** 类型取值：图片 */
	public final static byte TYPE_PIC = (byte) 1;
	/** 类型取值：统计图 */
	public final static byte TYPE_CHART = (byte) 2;
	/** 数据类型取值：子报表 */
	public final static byte TYPE_SUBREPORT = (byte) 3;
	/** 数据类型取值：HTML */
	public final static byte TYPE_HTML = (byte) 4;
	/** 数据类型取值：条形码 */
	public final static byte TYPE_BARCODE = (byte) 5;
	/** 多功能文本类型取值：多功能文本 */
	public final static byte TYPE_RICHTEXT = (byte) 6;
	/** SVG */
	public final static byte TYPE_SVG = (byte) 7;
	/** 大对象 */
	public final static byte TYPE_BLOB = (byte) 8;
	/** 类型取值：自定义 */
	public final static byte TYPE_CUSTOM = (byte) 20;

	/**
	 * 水平对齐取值：左对齐 * / public final static byte HALIGN_LEFT = (byte) 0; /**
	 * 水平对齐取值：中对齐 * / public final static byte HALIGN_CENTER = (byte) 1; /**
	 * 水平对齐取值：右对齐 * / public final static byte HALIGN_RIGHT = (byte) 2;
	 * 
	 * 
	 * /** 垂直对齐取值：靠上 * / public final static byte VALIGN_TOP = (byte) 0; /**
	 * 垂直对齐取值：居中 * / public final static byte VALIGN_MIDDLE = (byte) 1; /**
	 * 垂直对齐取值：靠下 * / public final static byte VALIGN_BOTTOM = (byte) 2; /*
	 * 对齐定义改为如下，是为了跟图形的Consts统一且兼容过去的图形 xq 2014.9.23,注意：对齐的数值要运算，不能随意改动。
	 */
	/** ----------------------水平对齐取值---------------------------- */
	public final static byte HALIGN_LEFT = 0; // 左对齐
	public final static byte HALIGN_CENTER = 2; // 中对齐
	public final static byte HALIGN_RIGHT = 4; // 右对齐

	/** ----------------------垂直对齐取值---------------------------- */
	public final static byte VALIGN_TOP = 8; // 靠上
	public final static byte VALIGN_MIDDLE = 16; // 居中
	public final static byte VALIGN_BOTTOM = 32; // 靠下

	/** 导出方式取值：导出真实值 */
	public final static byte EXPORT_REAL = (byte) 0;
	/** 导出方式取值：导出显示值 */
	public final static byte EXPORT_DISP = (byte) 1;
	/** 导出到Excel的方式取值：导出公式 */
	public final static byte EXPORT_FORMULA = (byte) 2;

	/** 尺寸调整取值：按设计尺寸不变 */
	public final static byte ADJUST_FIXED = (byte) 0;
	/** 尺寸调整取值：按单元格内容扩大 */
	public final static byte ADJUST_EXTEND = (byte) 1;
	/** 尺寸调整取值：图片填满单元格 */
	public final static byte ADJUST_FILL = (byte) 2;
	/** 尺寸调整取值：缩小字体填充 */
	public final static byte ADJUST_SHRINK = (byte) 3;

	/**
	 * 边框样式取值：无边框 * / public final static byte LINE_NONE = (byte) 0; /**
	 * 边框样式取值：点线 * / public final static byte LINE_DOT = (byte)1; /** 边框样式取值：虚线
	 * * / public final static byte LINE_DASHED = (byte) 2; /** 边框样式取值：实线 * /
	 * public final static byte LINE_SOLID = (byte) 3; /** 边框样式取值：双线 * / public
	 * final static byte LINE_DOUBLE = (byte) 4; /** 边框样式取值：点划线 * / public final
	 * static byte LINE_DOTDASH = (byte) 5; /* 线定义改为如下，是为了跟图形的Consts统一且兼容过去的图形
	 * xq 2014.9.23
	 */
	public final static byte LINE_NONE = 0x0; // 无
	public final static byte LINE_SOLID = 0x1; // 实线
	public final static byte LINE_DASHED = 0x2; // 虚线
	public final static byte LINE_DOT = 0x3; // 点线
	public final static byte LINE_DOUBLE = 0x4; // 双实线
	public final static byte LINE_DOTDASH = 0x5; // 点划线

	/** 状态自动 */
	public final static byte STATE_AUTO = -1;
	/** 状态禁止 */
	public final static byte STATE_NO = 0;
	/** 状态允许 */
	public final static byte STATE_YES = 1;
}
