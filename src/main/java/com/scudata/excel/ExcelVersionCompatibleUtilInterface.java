package com.scudata.excel;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;


/**
 * 公用poi版本方法兼容工具接口，实现类手动添加到poi-5.0.0.jar、poi-3.17.jar 
 * com/scudata/excel/ExcelVersionCompatibleUtil.class
 * 根据版本不同，方法实现有区别
*/
public interface ExcelVersionCompatibleUtilInterface {
	
	/**
	 * @return xlsx方块形状右下角坐标计算换算比例
	 */
	public int getXSSFShape_EMU_PER_PIXEL() ;

	/**
	 * 获取单元格类型
	 * @param c 目标单元格
	 * @return 单元格类型枚举
	 */
	public CellType getCellType(Cell c);
	
	/**
	 * 
	 * @return xls透明色short值
	 */
	public short getHSSFColor_AUTOMATIC_Index();

	/**
	 * xls水平位置常量
	 * @param style
	 * @return
	 */
	public HorizontalAlignment getHSSFAlignmentEnum(HSSFCellStyle style);

	/**
	 * xls垂直位置常量
	 * @param style
	 * @return
	 */
	public VerticalAlignment getHSSFVerticalAlignmentEnum(HSSFCellStyle style);

	/**
	 * xls左边框常量
	 * @param style
	 * @return
	 */
	public short getHSSFBorderLeft(HSSFCellStyle style);
	/**
	 * xls右边框常量
	 * @param style
	 * @return
	 */
	public short getHSSFBorderRight(HSSFCellStyle style);
	/**
	 * xls上边框常量
	 * @param style
	 * @return
	 */
	public short getHSSFBorderTop(HSSFCellStyle style);
	/**
	 * xls下边框常量
	 * @param style
	 * @return
	 */
	public short getHSSFBorderBottom(HSSFCellStyle style);
	/**
	 * xlsx水平位置常量
	 * @param style
	 * @return
	 */
	public HorizontalAlignment getXSSFAlignmentEnum(XSSFCellStyle style);
	/**
	 * xlsx垂直位置常量
	 * @param style
	 * @return
	 */
	public VerticalAlignment getXSSFVerticalAlignmentEnum(XSSFCellStyle style);
	/**
	 * xlsx左边框常量
	 * @param style
	 * @return
	 */
	public short getXSSFBorderLeft(XSSFCellStyle style);
	/**
	 * xlsx右边框常量
	 * @param style
	 * @return
	 */
	public short getXSSFBorderRight(XSSFCellStyle style);
	/**
	 * xlsx上边框常量
	 * @param style
	 * @return
	 */
	public short getXSSFBorderTop(XSSFCellStyle style);
	/**
	 * xlsx下边框常量
	 * @param style
	 * @return
	 */
	public short getXSSFBorderBottom(XSSFCellStyle style);

	/**
	 * 获取xlsx颜色对象的ARGB值
	 * @param xc
	 * @param defColor 如果xc为null，则返回此Color的ARGB
	 * @return
	 */
	public int getColor(XSSFColor xc, Color defColor);
	
	/**
	 * 二进制转int数字
	 * @param b
	 * @return
	 */
	public int byteToInt(byte b);
	
	/**
	 * 判断cell是否为公式格
	 * @param cell
	 * @return
	 */
	public boolean isCellTypeFomula(Cell cell);
	/**
	 * 插入xlsx背景图
	 * @param wbp
	 * @param img 背景图
	 * @param s 要添加背景图的sheet页对象
	 * @throws IOException
	 */
	public void addWaterRemarkToExcel(Workbook wbp, BufferedImage img, Sheet s) throws IOException;
	
	/** 获得边框样式
	 * @param borderStyle 润乾报表中定义的边框样式
	 * @param borderWidth 边框宽度
	 * @return Excel的边框样式
	 */
	public short getBorderStyle( byte borderStyle, float width );
	

	/**
	 * 获得边框样式
	 * 
	 * @param borderStyle
	 *            润乾报表中定义的边框样式
	 * @param borderWidth
	 *            边框宽度
	 * @return Excel的边框样式
	 */
	public short getISheetBorderStyle(byte borderStyle);

	/**
	 * 获取单元格类型
	 * @param value 目标单元格值
	 * @return 单元格类型枚举
	 */
	public CellType getCellType(CellValue value);
	
	/**
	 * 获取sheet中某一个富文本对象
	 * @param sst
	 * @param idx
	 * @return
	 */
	public RichTextString getItemAt(SharedStrings sst, int idx);
	/**
	 * 获取sheet页里的图片集合
	 * @param sheet
	 * @param graphMap
	 */
	public void getSheetPictures(XSSFSheet sheet, Map<String, byte[]> graphMap);
	/**
	 * 获取指定font的编号
	 * @param font
	 * @return 统一返回int
	 */
	public int getFontIndex(Font font);
	/**
	 * 获取指定单元格样式对象里的font的编号
	 * @param style
	 * @return 统一返回int
	 */
	public int getFontIndex(CellStyle style);
	/**
	 * 引用Workbook.getNumberOfFonts()
	 * @param wb
	 * @return 统一返回int
	 */
	public int getNumberOfFonts(Workbook wb);
	/**
	 * 引用Workbook.getNumberOfSheets()
	 * @param wb
	 * @return 统一返回int
	 */
	public int getNumberOfSheets(Workbook wb);

	/**
	 * 获取指定编号的字体Font对象
	 * @param wb
	 * @param index
	 * @return
	 */
	public Font getFontAt(Workbook wb, Number index);
	
	/**
	 * 返回单元格公式计算结果类型
	 * @param cell
	 * @return
	 */
	public CellType getCachedFormulaResultType(Cell cell) ;
	
	public XSSFColor getXSSFColor(int color);

	public SharedStrings readSharedStrings(XSSFReader xssfReader);
}
