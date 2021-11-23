package com.raqsoft.chart.element;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import com.raqsoft.app.common.StringUtils2;
import com.raqsoft.chart.Consts;
import com.raqsoft.chart.DataElement;
import com.raqsoft.chart.Para;
import com.raqsoft.chart.Utils;
import com.raqsoft.chart.edit.ParamInfo;
import com.raqsoft.chart.edit.ParamInfoList;
/**
 * 文本图元
 * 文本图元不光可以文本形式展现，还可以以条形码展示
 * @author Joancy
 *
 */
public class Text extends DataElement{
	//单行文本时，宽高无效，根据文字实际宽高来的；
//	多行文本时， 超出宽高的将截掉
//	条码时采用宽高来适配 条码尺寸
	public Para width=new Para(new Double(0));
	public Para height=new Para(new Double(0));//条码时，会根据宽高适配

	public Para text = new Para(null);
	public Para textFont = new Para("Dialog");//"宋体"
	public Para textStyle = new Para(new Integer(0));
	public Para textSize = new Para(new Integer(14));
	public Para textColor = new Para(Color.black);
	public Para backColor = new Para(null);
	
//	是否多行式文本，单行文本时，不采用width和height，x，y为文本中心点，背景也仅文字有背景
//	多行文本时，x,y为左上角，采用宽和高，忽略旋转和纵向属性，背景为w，h限定的矩形
	public boolean isMulti = false;

	// 旋转角度
	public int textAngle = 0;

	// 横向对齐
	public int hAlign = Consts.HALIGN_CENTER;

	// 纵向对齐
	public int vAlign = Consts.VALIGN_MIDDLE;
	
	// 条形码相关属性
	public int barType = Consts.TYPE_NONE;

	// 字符集 
	public String charSet = "UTF-8";
	
	// 条形码时显示文字
	public boolean dispText = false;

	// 容错率
	public String recError = "M";
	
	//logo图内容
	public Para logoValue = new Para();
	
	//logo占图形的百分比例
	public int logoSize=15;

	// logo背景框
	public boolean logoFrame = true;
	
	/**
	 * 是否为多行文本
	 * @return 如果是多行文本返回true，否则返回false
	 */
	public boolean isMulti() {
		return isMulti;
	}

	/**
	 * 绘图前准备工作，当前忽略该函数
	 */
	public void beforeDraw() {
	}

	/**
	 * 绘制中间层，忽略，无意义
	 */
	public void draw() {
	}

	/**
	 * 绘制背景层，忽略
	 */
	public void drawBack() {
	}

	/**
	 * 获取序号位置的文本所占宽度
	 * @param index 序号
	 * @return 像素宽度
	 */
	public int getWidth(int index){
		return (int)e.getXPixel(width.doubleValue(index));
	}
	
	/**
	 * 获取序号位置的文本所占搞度
	 * @param index 序号
	 * @return 像素高度
	 */
	public int getHeight(int index){
		return (int)e.getYPixel(height.doubleValue(index));
	}
	
	/**
	 * 绘制前景层
	 */
	public void drawFore() {
		if (!isVisible()) {
			return;
		}
		drawTexts();
	}
	
	private void drawTexts() {
		// 数据
		int size = pointSize();
		for (int i = 1; i <= size; i++) {
			Point2D p = getScreenPoint(i);
			Shape shape = drawAText(i, p);
			if(shape!=null){
				String title = getTipTitle(i);
				addLink(shape, htmlLink.stringValue(i), title,linkTarget.stringValue(i));
			}
		}
	}
	
	private Shape drawAText(int index, Point2D p) {
		double px = p.getX();
		double py = p.getY();
		String tf = textFont.stringValue(index);
		int ts = textStyle.intValue(index);
		int tsize = textSize.intValue(index);
		
		Font font = Utils.getFont(tf, ts, tsize);
		Color c = textColor.colorValue(index);
		Color backC = backColor.colorValue(index);
		String aText = text.stringValue(index);
		
		if(barType==Consts.TYPE_NONE){
			Rectangle shape;
			Graphics g = e.getGraphics();
			if(!isMulti){
//				单行文本时，不采用宽高，指定的x，y为文本中心点
				Utils.drawText(e, aText, px, py, font, c, backC, ts, textAngle, hAlign
						+ vAlign, true);
				shape = Utils.getTextSize(aText, g, ts, textAngle, font);
				shape.setLocation((int)px, (int)py);
			}else{
				FontMetrics fm = g.getFontMetrics(font);
				int ascent = fm.getAscent();
				int fheight = fm.getHeight();
				int w = getWidth(index);
				if(w<1){
					w = 60;//未设置时缺省宽度
				}
				ArrayList<String> wrapedString = StringUtils2.wrapString(aText, fm, w, false, -1);
				//	多行时，忽略垂直布局，旋转等特殊状态
				int h = getHeight(index);
				int lineH = StringUtils2.getTextRowHeight(fm);
				int lines = wrapedString.size();
				if(h<1){
					h = lineH*lines;//未设置时缺省宽度
				}
				shape = new Rectangle((int)px, (int)py, w, h);
				if(backC!=null){
					g.setColor(backC);
					g.fillRect((int)px, (int)py, w, h);
				}

				
				int yy = (int)py;
				if (vAlign == Consts.VALIGN_MIDDLE) {
					yy = (int)py + (h - lineH * lines) / 2;
				} else if (vAlign == Consts.VALIGN_BOTTOM) {
					yy = (int)py + h - lineH * lines;
				}
				if (yy < py) {
					yy = (int)py;
				}
				for (int i = 0; i < lines; i++) {
					if (i > 0 && yy + lineH > py + h) { // 第一行总是绘制，其余行如果在框外不画，否则遮盖别的格子文字
						break;
					}

					String wrapedText = (String) wrapedString.get(i);
					int fw = stringWidth(fm, wrapedText,g);
					int x1 = (int)px;
					if (hAlign == Consts.HALIGN_CENTER) {
						x1 = (int)px + (w - fw) / 2;
					} else if (hAlign == Consts.HALIGN_RIGHT) {
						x1 = (int)px + w - fw;
					}
					int y1 = yy + fheight-ascent;//+ ascent;// + 
					Utils.drawText(e, wrapedText, x1, y1, font, c, null, ts, 0,
							Consts.HALIGN_LEFT+Consts.VALIGN_MIDDLE
							, true);
					yy += lineH;
				}
			}
			return shape;
		}else{
			Graphics2D g = e.getGraphics();
			BufferedImage barcodeImg = Utils.calcBarcodeImage(this,index,c,backC);
			Rectangle posDesc = new Rectangle();
			posDesc.setBounds((int)px, (int)py, barcodeImg.getWidth(), barcodeImg.getHeight());
			Point drawPoint = Utils.getRealDrawPoint(posDesc, hAlign + vAlign,true);
			g.drawImage(barcodeImg, drawPoint.x, drawPoint.y, null);
//			g.drawImage(barcodeImg, (int)px, (int)py,null);曾经注释过坐标调整，图片还是需要根据对齐方式布置
			return posDesc;
		}
	}

	/**
	 * 计算指定字体下的文本占用宽度
	 * @param fm 字体度量
	 * @param text 文本值
	 * @param g 图形设备
	 * @return 占用宽度，单位为像素
	 */
	public static int stringWidth(FontMetrics fm, String text, Graphics g) {
		Graphics displayG = g;
		FontMetrics dispFm = displayG.getFontMetrics(fm.getFont());
		return dispFm.stringWidth(text);
	}
	
	/**
	 * 获取编辑参数信息列表
	 * @return 参数信息列表
	 */
	public ParamInfoList getParamInfoList() {
		ParamInfoList paramInfos = new ParamInfoList();
		ParamInfo.setCurrent(Text.class, this);

		paramInfos.add(new ParamInfo("text"));
		paramInfos.add(new ParamInfo("isMulti", Consts.INPUT_CHECKBOX));

		String group = "size";
		paramInfos.add(group, new ParamInfo("width", Consts.INPUT_DOUBLE));
		paramInfos.add(group, new ParamInfo("height", Consts.INPUT_DOUBLE));

		group = "text";
		paramInfos.add(group, new ParamInfo("textFont", Consts.INPUT_FONT));
		paramInfos.add(group,
				new ParamInfo("textStyle", Consts.INPUT_FONTSTYLE));
		paramInfos.add(group, new ParamInfo("textSize", Consts.INPUT_FONTSIZE));
		paramInfos.add(group, new ParamInfo("textColor", Consts.INPUT_COLOR));
		paramInfos.add(group, new ParamInfo("backColor", Consts.INPUT_COLOR));
		paramInfos.add(group, new ParamInfo("textAngle", Consts.INPUT_INTEGER));

		group = "align";
		paramInfos.add(group, new ParamInfo("hAlign", Consts.INPUT_HALIGN));
		paramInfos.add(group, new ParamInfo("vAlign", Consts.INPUT_VALIGN));
		
		group = "barcode";
		paramInfos.add(group, new ParamInfo("barType", Consts.INPUT_BARTYPE));
		paramInfos.add(group, new ParamInfo("charSet", Consts.INPUT_CHARSET));
		paramInfos.add(group, new ParamInfo("dispText", Consts.INPUT_CHECKBOX));
		paramInfos.add(group, new ParamInfo("recError", Consts.INPUT_RECERROR));
		paramInfos.add(group, new ParamInfo("logoValue"));
		paramInfos.add(group, new ParamInfo("logoSize", Consts.INPUT_INTEGER));
		paramInfos.add(group, new ParamInfo("logoFrame", Consts.INPUT_CHECKBOX));
		
		ParamInfoList superParams = super.getParamInfoList();
		superParams.delete("data","axisTime");
		superParams.delete("data","dataTime");
		paramInfos.addAll( superParams);
		
		return paramInfos;
	}

	/**
	 * 获取指定序号下的显示文本
	 * @param index 序号
	 * @return 文本内容
	 */
	public String getDispText(int index) {
		return text.stringValue(index);
	}
	
	protected String getText(int index) {
//		条码时， tip为当前文字
		if(barType!=Consts.TYPE_NONE){
			return text.stringValue(index);
		}
//		文本时，此处不返回， 底层返回坐标信息
		return null;
	}

	/**
	 * 是否包含渐变颜色
	 * 该函数无意义，返回false
	 */
	public boolean hasGradientColor() {
		return false;
	}

	/**
	 * 克隆文本内容
	 * @param t 另一个文本对象
	 */
	public void clone(Text t){
		super.clone(t);
	}
	
	/**
	 * 深度克隆一个文本图元
	 * @return 克隆后的文本图元
	 */
	public Object deepClone() {
		Text t = new Text();
		clone(t);
		return t;
	}
}
