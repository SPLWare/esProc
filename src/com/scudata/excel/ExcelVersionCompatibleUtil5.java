package com.scudata.excel;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PictureData;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTMarker;

import com.scudata.common.ImageUtils;

public class ExcelVersionCompatibleUtil5 implements ExcelVersionCompatibleUtilInterface{
	
	private static final String ROW_COL_SEP = "_";
	
	public int getXSSFShape_EMU_PER_PIXEL() {
		return 9525;
	}
	
	public CellType getCellType(Cell c){
		return c.getCellType();
	}
	
	public short getHSSFColor_AUTOMATIC_Index() {
		return HSSFColor.HSSFColorPredefined.AUTOMATIC.getIndex();
	}

	public HorizontalAlignment getHSSFAlignmentEnum(HSSFCellStyle style){
		return style.getAlignment();
	}

	public VerticalAlignment getHSSFVerticalAlignmentEnum(HSSFCellStyle style) {
		return style.getVerticalAlignment();
	}

	public short getHSSFBorderLeft(HSSFCellStyle style){
		return style.getBorderLeft().getCode();
	}
	
	public short getHSSFBorderRight(HSSFCellStyle style){
		return style.getBorderRight().getCode();
	}
	
	public short getHSSFBorderTop(HSSFCellStyle style){
		return style.getBorderTop().getCode();
	}
	
	public short getHSSFBorderBottom(HSSFCellStyle style){
		return style.getBorderBottom().getCode();
	}
	
	public HorizontalAlignment getXSSFAlignmentEnum(XSSFCellStyle style){
		return style.getAlignment();
	}

	public VerticalAlignment getXSSFVerticalAlignmentEnum(XSSFCellStyle style){
		return style.getVerticalAlignment();
	}

	public short getXSSFBorderLeft(XSSFCellStyle style){
		return style.getBorderLeft().getCode();
	}
	
	public short getXSSFBorderRight(XSSFCellStyle style){
		return style.getBorderRight().getCode();
	}
	
	public short getXSSFBorderTop(XSSFCellStyle style){
		return style.getBorderTop().getCode();
	}
	
	public short getXSSFBorderBottom(XSSFCellStyle style){
		return style.getBorderBottom().getCode();
	}

	public boolean isCellTypeFomula(Cell cell) {
		return cell.getCellType().compareTo(CellType.FORMULA) == 0;
	}



	public int getColor(XSSFColor xc, Color defColor) {
		Color c = defColor;
		if (xc != null) {
			byte[] argb = xc.getARGB();
			if (argb != null) {
				// edited by hhw 2012.2.8如果argb中没有a值则给它加上
				if (argb.length == 3) {
					byte[] tmp = new byte[4];
					tmp[0] = (byte) -1;
					tmp[1] = argb[0];
					tmp[2] = argb[1];
					tmp[3] = argb[2];
					argb = tmp;
				}
				c = new Color(byteToInt(argb[1]), byteToInt(argb[2]), byteToInt(argb[3]), byteToInt(argb[0]));
			}
		}
		return c.getRGB();
	}
	
	public int byteToInt(byte b) {
		if (b >= 0)
			return (int) b;
		return b + 256;
	}
	
	
	//use RqeditJar ooxml-schemas-1.3.jar
	public void addWaterRemarkToExcel(Workbook wbp, BufferedImage img, Sheet s) throws IOException{
		byte[] bytes = ImageUtils.writePNG( img );
		ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream(bytes.length);
		byteArrayOut.write(bytes);
		XSSFWorkbook xssfWb = null;
		if(wbp instanceof XSSFWorkbook) {
			xssfWb = ((XSSFWorkbook)wbp);
		}else if(wbp instanceof SXSSFWorkbook) {
			xssfWb = ((SXSSFWorkbook)wbp).getXSSFWorkbook();
		}
		Iterator<Sheet> poisheets = xssfWb.sheetIterator();
		while(poisheets.hasNext()) {
			XSSFSheet poisheet = (XSSFSheet) poisheets.next();//(XSSFSheet) s;
			int pictureIdx = wbp.addPicture( bytes, Workbook.PICTURE_TYPE_PNG); 
			if(poisheet.getCTWorksheet().isSetPicture()) continue;
			String rID = poisheet.addRelation(null, XSSFRelation.IMAGES, xssfWb.getAllPictures().get(pictureIdx)).getRelationship().getId();
			poisheet.getCTWorksheet().addNewPicture().setId(rID); 
		}
	 }
	
	/** 获得边框样式
	 * @param borderStyle 润乾报表中定义的边框样式
	 * @param borderWidth 边框宽度
	 * @return Excel的边框样式
	 */
	public short getBorderStyle( byte borderStyle, float width ) {
		try {
			Class clazz = Class.forName("com.raqsoft.report.view.ExportExcelUtil2");
			Object o = clazz.newInstance();
			Method m = clazz.getMethod("getBorderStyle", Byte.class, Float.class);
			return (Short) m.invoke(o, borderStyle, width);
		}catch(Exception e) {
			return BorderStyle.THIN.getCode();	
		}
	}
	

	/**
	 * 获得边框样式
	 * 
	 * @param borderStyle
	 *            润乾报表中定义的边框样式
	 * @param borderWidth
	 *            边框宽度
	 * @return Excel的边框样式
	 */
	public short getISheetBorderStyle(byte borderStyle) {
		try {
			Class clazz = Class.forName("com.raqsoft.report.view.ExportExcelUtil2");
			Object o = clazz.newInstance();
			Method m = clazz.getMethod("getISheetBorderStyle");
			return (Short) m.invoke(o, borderStyle);
		}catch(Exception e) {
			return BorderStyle.THIN.getCode();	
		}
	}
	
	public CellType getCellType(CellValue value){
		return value.getCellType();
	}

	public RichTextString getItemAt(SharedStringsTable sst, int idx){
		return sst.getItemAt(idx);
	}

	public void getSheetPictures(XSSFSheet sheet, Map<String, byte[]> graphMap) {
		List<POIXMLDocumentPart> list = sheet.getRelations();
		for (POIXMLDocumentPart part : list) {
			if (part instanceof XSSFDrawing) {
				XSSFDrawing drawing = (XSSFDrawing) part;
				List<XSSFShape> shapes = drawing.getShapes();
				for (XSSFShape shape : shapes) {
					XSSFPicture picture = (XSSFPicture) shape;
					PictureData pdata = picture
							.getPictureData();
					if (pdata != null) {
						XSSFClientAnchor anchor = picture
								.getPreferredSize();
						CTMarker marker = anchor.getFrom();
						String key = marker.getRow()
								+ ROW_COL_SEP + marker.getCol();
						graphMap.put(key, pdata.getData());
						if (pdata.getPictureType() != Workbook.PICTURE_TYPE_PNG) {
							picture.resize();
						}
					}
				}
			}
		}
	}
	
	public int getFontIndex(Font font) {
		return font.getIndex();
	}
	
	public int getFontIndex(CellStyle style) {
		return style.getFontIndex();
	}
	
	public int getNumberOfFonts(Workbook wb) {
		return wb.getNumberOfFonts();
	}
	
	public int getNumberOfSheets(Workbook wb) {
		return wb.getNumberOfSheets();
	}

	public Font getFontAt(Workbook wb, Number index) {
		return wb.getFontAt(index.intValue());
	}
}
