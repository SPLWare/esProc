package com.scudata.cellset.graph.draw;

import java.awt.geom.*;
import java.awt.*;
import java.util.*;

import com.scudata.chart.Utils;
import com.scudata.common.*;

/**
 * 图形字体视图
 * 封装了字体各类相关信息的字体视图
 * @author Joancy
 *
 */
public class GraphFontView {
	public static final byte FONT_TITLE = 0;
	public static final byte FONT_LEGEND = 1;
	public static final byte FONT_XLABEL = 2;
	public static final byte FONT_YLABEL = 3;
	public static final byte FONT_XTITLE = 4;
	public static final byte FONT_YTITLE = 5;
	public static final byte FONT_VALUE = 6;
	public static final byte TEXT_FIXED = 0; // 文字不作校准
	public static final byte TEXT_ON_TOP = 1; // 文字位于中心点上方
	public static final byte TEXT_ON_BOTTOM = 2; // 文字位于中心点下方
	public static final byte TEXT_ON_LEFT = 3; // 文字位于中心点左边
	public static final byte TEXT_ON_RIGHT = 4; // 文字位于中心点右边
	public static final byte TEXT_ON_CENTER = 5; // 文字位于中心点

	DrawBase db;
	public String text = "";
	public String text2 = ""; // 两轴图的Y2轴标题
	public Font font;
	public Color color;
	public boolean vertical = false;
	public int angle; // 文字旋转角度，注意当前仅支持旋转0到90度，多余的度数取90度的余

	// 文字的缺省对齐位置
	byte textPosition = TEXT_FIXED;
	private boolean allowIntersect = true; // 是否允许相邻的数值重叠输出

	// Rectangle PA = null; //上一个输出文本区域 PreArea
	ArrayList fontRects = new ArrayList(); // 所有输出过的矩形都hold，禁止重叠输出时要与所有已占区间比较，老办法是只比相邻区间

	/**
	 * 构造字体视图对象
	 * @param drawBase 图形绘制实例
	 */
	public GraphFontView(DrawBase drawBase) {
		this.db = drawBase;
		allowIntersect = drawBase.egp.isShowOverlapText();
	}

	/**
	 * 将字体对齐反向
	 * @param direction 对齐方式
	 * @return 反向对齐方式，不需要反向的仍然是当前对齐方式
	 */
	public static byte reverseDirection(byte direction) {
		switch (direction) {
		case TEXT_FIXED:
			return TEXT_FIXED;
		case TEXT_ON_CENTER:
			return TEXT_ON_CENTER;
		case TEXT_ON_TOP:
			return TEXT_ON_BOTTOM;
		case TEXT_ON_BOTTOM:
			return TEXT_ON_TOP;
		case TEXT_ON_LEFT:
			return TEXT_ON_RIGHT;
		case TEXT_ON_RIGHT:
			break;
		}
		return TEXT_ON_LEFT;
	}

	/**
	 * 设置字体对象
	 * @param font 字体对象
	 */
	public void setFont(Font font) {
		this.font = font;
	}

	/**
	 * 设置绘制文本时，是否允许跟别的文字重叠
	 * @param allowIntersect 允许重叠
	 */
	public void setIntersect(boolean allowIntersect) {
		this.allowIntersect = allowIntersect;
	}

	/**
	 * 设置颜色
	 * @param color 颜色
	 */
	public void setColor(Color color) {
		this.color = color;
	}

	/**
	 * 设置文字是否竖向排列
	 * @param vertical 竖向排列
	 */
	public void setVertical(boolean vertical) {
		this.vertical = vertical;
	}

	/**
	 * 设置文本的旋转角度
	 * @param angle 角度
	 */
	public void setAngle(int angle) {
		if(Math.abs(angle)>90) {
			int tmp = Math.abs(angle) % 90;
			Logger.warn("Rotate angle must between [0,90], "+tmp+" will be used instead of "+angle);
			this.angle = tmp;
		}else {
			this.angle = angle;
		}
	}

	/**
	 * 设置显示文本
	 * @param text 文本串
	 */
	public void setText(String text) {
		if (text == null) {
			return;
		}
		if (db.egp.is2YGraph()) {
			int pos = -1;
			//只准许用分号，考虑到还可以用分开的表达式
			if (pos < 0) {
				pos = text.indexOf(';');
			}
			if (pos < 0) {
				this.text = text;
			} else {
				this.text = text.substring(0, pos);
				this.text2 = text.substring(pos + 1);
			}
		} else {
			this.text = text;
		}
	}

	/**
	 * 设置文字在圆形布局时的方位，用于雷达图
	 * @param pos 方位
	 */
	public void setTextPosition(byte pos) {
		this.textPosition = pos;
	}
	
	/**
	 * 在坐标出输出当前文本
	 * @param x 横坐标
	 * @param y 纵坐标
	 */
	public void outText(double x, double y) {
		outText(x, y, text);
	}

	/**
	 * 该函数方便X轴标签根据interval决定是否画文字
	 * 
	 * @param x
	 *            int 横坐标
	 * @param y
	 *            int 纵坐标
	 * @param text
	 *            String 文本
	 * @param visible
	 *            boolean 是否可见
	 */
	public void outText(double x, double y, String text, boolean visible) {
		if (visible) {
			outText(x, y, text);
		}
	}

	/**
	 * 输出文本
	 * @param x 横坐标
	 * @param y 纵坐标
	 * @param text 文本
	 * @param visible 可见
	 * @param direction 方位
	 */
	public void outText(double x, double y, String text, boolean visible,
			byte direction) {
		if (visible) {
			outText(x, y, text, direction);
		}
	}

	/**
	 * 输出文本
	 * @param text 
	 *            String 文本
	 * @param x
	 *            double 文本输出时的左下角x
	 * @param y
	 *            double 文本输出时的左下角y
	 */
	public boolean outText(double x, double y, String text) {
		return outText(x, y, text, textPosition);
	}

	private Rectangle intersects(Rectangle newRect) {
		// 要从后往前找，找到最后一个重叠区域
		for (int i = fontRects.size() - 1; i >= 0; i--) {
			Rectangle rect = (Rectangle) fontRects.get(i);
			if (rect.intersects(newRect)) {
				return rect;
			}
		}
		return null;
	}

	/**
	 * 输出文本
	 * @param x 横坐标
	 * @param y 纵坐标
	 * @param text 文本
	 * @param tmpColor 颜色
	 * @return 绘制完成返回true，否则返回false
	 */
	public boolean outText(double x, double y, String text, Color tmpColor) {
		return outText(x, y, text, textPosition, tmpColor);
	}

	/**
	 * 输出文本
	 * @param x 横坐标
	 * @param y 纵坐标
	 * @param text 文本
	 * @param direction 方位
	 * @return 绘制完成返回true，否则返回false
	 */
	public boolean outText(double x, double y, String text, byte direction) {
		return outText(x, y, text, direction, color);
	}

	/**
	 * 输出文本
	 * @param x 横坐标
	 * @param y 纵坐标
	 * @param text 文本
	 * @param direction 方位 
	 * @param textColor 颜色
	 * @return 绘制了文本返回true，否则返回false
	 */

	public boolean outText(double x, double y, String text, byte direction,
			Color textColor) {
		if (text == null || text.trim().length() == 0) {
			return false;
		}
		if (font.getSize() == 0) {
			return false;
		}
		Rectangle TA = getTextSize(text); // This Area
		if (vertical || angle == 0) {
			TA = getTextSize(text);
//		} else if(angle%90!=0){
//			vertical = true; // 文字有旋转角度的时候,得计算假设竖排文字是否相交
//			TA = getTextSize(text);
//			vertical = false;
		}
		FontMetrics fm = db.g.getFontMetrics(font);
		Point rop = getActualTextPoint((int)x, (int)y, direction, TA, fm, text); 
		TA.x = rop.x;
		TA.y = rop.y;
		if (textColor != color) { // 当输出颜色跟编辑的颜色不一致时，表示使用了系列的动态颜色来区分标签，该状态下
			// 不判断重叠，真有重叠时，让坐标稍微错下位，由于颜色不同，错位后，仍可以分辨出来
			Rectangle rect = intersects(TA);
			if (rect != null) {
				if (TA.y <= rect.y) {
					TA.y = rect.y - rect.height;
				} else {
					TA.y = rop.y + rect.height + db.VALUE_RADIUS + 2;
				}
			}
		} else {
			if (!allowIntersect && intersects(TA) != null) {
				return false;
			}
		}
		if (!fontRects.contains(TA)) {
			fontRects.add(TA);
		}

		db.g.setColor(textColor);
		db.g.setFont(font);
//		程序中禁止关闭anti，保证所有的线和文字都是平滑的
//		文字要求不平滑，看起来更清晰，所以，改成绘制文字时关闭锯齿，绘制完后恢复平滑
		Composite com = db.g.getComposite();
		Utils.setGraphAntiAliasingOff(db.g);
//		db.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//				RenderingHints.VALUE_ANTIALIAS_OFF);
		// 不能使用异或输出，两个原因： 1,透明背景时异或根本就画不出来；2，异或总有某个相近的颜色异或后仍然不清晰

		x = TA.x;
		y = TA.y;
		if (vertical) {
			for (int i = 0; i < text.length(); i++) {
				String ch = text.substring(i, i + 1);
				if ("()[]{}".indexOf(ch) >= 0) {
					AffineTransform at = db.g.getTransform();
					double yy = y + i * (fm.getAscent() + 2);
					if ("([{".indexOf(ch) >= 0) {
						yy -= fm.getAscent() / 2;
					} else {
						yy -= fm.getAscent();
					}
					AffineTransform at1 = AffineTransform.getTranslateInstance(
							x + 2, yy);
					db.g.transform(at1);
					double rotateAngle = Math.toRadians(90);
					AffineTransform at2 = AffineTransform.getRotateInstance(
							rotateAngle, 0, 0);
					db.g.transform(at2);
					db.g.setStroke(new BasicStroke(1f));
					db.g.drawString(ch, 0, 0);
					db.g.setTransform(at);
				} else {
					db.g.drawString(ch, (int)x, (int)(y + i * (fm.getAscent() + 2)));
				}
			}
		} else if (angle == 0) {
			db.g.drawString(text, (int)x, (int)y);
		} else {
			double rotateAngle = Math.toRadians(-angle);
			AffineTransform at = db.g.getTransform();
			AffineTransform at1 = AffineTransform.getRotateInstance(
					rotateAngle, x, y);
			db.g.transform(at1);
			db.g.setStroke(new BasicStroke(1f));
			db.g.drawString(text, (int)x, (int)y);

			db.g.setTransform(at);
		}
		
		Utils.setGraphAntiAliasingOn(db.g);

//		文字输出完毕，再恢复平滑
//		db.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//				RenderingHints.VALUE_ANTIALIAS_ON);

		db.g.setComposite(com);
		db.g.setStroke(new BasicStroke(0.00001f));
		return true;
	}


	private Point getActualTextPoint(int x, int y, byte direction,
			Rectangle TA, FontMetrics fm, String text) {
		if (direction == TEXT_FIXED) {
			return new Point(x, y);
		}
		if (vertical) {
			switch (direction) {
			case TEXT_ON_BOTTOM:
				x -= TA.width / 2;
				break;
			case TEXT_ON_TOP:
				x -= TA.width / 2;
				y -= TA.height;
				break;
			case TEXT_ON_LEFT:
				x -= TA.width;
				y -= TA.height / 2;
				break;
			case TEXT_ON_RIGHT:
				y -= TA.height / 2;
				break;
			case TEXT_ON_CENTER:
				x -= TA.width / 2;
				y -= TA.height / 2;
				break;
			}
			y += fm.getAscent() + 2;
		} else if (angle == 0) {
			switch (direction) {
			case TEXT_ON_BOTTOM:
				x -= TA.width / 2;
				y += TA.height;
				break;
			case TEXT_ON_TOP:
				x -= TA.width / 2;
				break;
			case TEXT_ON_LEFT:
				x -= TA.width;
				y += TA.height / 2;
				break;
			case TEXT_ON_RIGHT:
				y += TA.height / 2;
				break;
			case TEXT_ON_CENTER:
				x -= TA.width / 2;
				y += TA.height / 2;
				break;
			}
		} else if(angle==90){
			switch (direction) {
			case TEXT_ON_BOTTOM:
				x += TA.width / 2;
				y += TA.height;
				break;
			case TEXT_ON_TOP:
				x += TA.width / 2;
				break;
			case TEXT_ON_LEFT:
				y += TA.height / 2;
				break;
			case TEXT_ON_RIGHT:
				x += TA.width;
				y += TA.height / 2;
				break;
			case TEXT_ON_CENTER:
				x += TA.width / 2;
				y += TA.height / 2;
				break;
			}
		}else {
//			旋转角度仅支持0-90度
			double rotateAngle = Math.toRadians(angle);
//			Rectangle tmpTA = getTextSize(text);
			
			FontMetrics tfm = db.g.getFontMetrics(font);
			int tw = tfm.stringWidth(text);
			int th = tfm.getAscent();
			double  dotLeft = th * Math.sin(rotateAngle );
			double  dotRight = tw * Math.cos(rotateAngle );
			double halfW = TA.width/2;
			double halfH = TA.height/2;
			switch (direction) {
			case TEXT_ON_BOTTOM:
				x -= dotRight;
				y += TA.height;
				break;
			case TEXT_ON_TOP:
//				x += halfW-dotLeft;
				break;
			case TEXT_ON_LEFT:
				x -= dotRight;
				y += halfH;
				break;
			case TEXT_ON_RIGHT:
				x += dotLeft;
				y += halfH;
				break;
			case TEXT_ON_CENTER:
				x -= halfW-dotLeft;
				y += halfH;
				break;
			}
		}

		int gap = 2;
		switch (direction) {
		case TEXT_ON_BOTTOM:
			y += gap;
			break;
		case TEXT_ON_TOP:
			y -= gap;
			break;
		case TEXT_ON_LEFT:
			x -= gap;
			break;
		case TEXT_ON_RIGHT:
			x += gap;
			break;
		}

		return new Point(x, y);
	}

	/**
	 * 获取当前文本所占区域
	 * @return 矩形描述文本区域
	 */
	public Rectangle getTextSize() {
		return getTextSize(text);
	}

	/**
	 * 获取指定文本再当前字体下的所占区域
	 * @param text 文本
	 * @return 矩形区域
	 */
	public Rectangle getTextSize(String text) {
		if (text == null) {
			return new Rectangle();
		}
		if (vertical) {
			return getVerticalArea(text);
		}
		if (angle % 180== 0) {
			return getHorizonArea(text);
		}
		return getRotationArea(text);
	}

	private Rectangle getVerticalArea(String text) {
		if (!StringUtils.isValidString(text)) {
			text = "A";
		}
		Rectangle area = new Rectangle();
		FontMetrics fm = db.g.getFontMetrics(font);
		int hh = fm.getAscent() + 2; // 竖排文字间有2个点的间隙
		area.width = fm.stringWidth(text.substring(0, 1));
		area.height = hh * text.length();
		return area;
	}

	private Rectangle getHorizonArea(String text) {
		Rectangle area = new Rectangle();
		FontMetrics fm = db.g.getFontMetrics(font);
		int hw = fm.stringWidth(text);
		int hh = fm.getAscent();
		area.width = hw;
		area.height = hh - fm.getLeading() - 2;// 按理来说Ascent就已经是文字基线上部空间了，但实测是多了，微调一下2
		return area;
	}

	private Rectangle getRotationArea(String text) {
		if (!StringUtils.isValidString(text)) {
			text = "A";
		}

		Rectangle area = new Rectangle();
		FontMetrics fm = db.g.getFontMetrics(font);
		int hw = fm.stringWidth(text);
		int hh = fm.getAscent();
		double djx =  Math.sqrt(hw * hw + hh * hh); // 对角线长度
		double textAngle = Math.atan(hh / (hw * 1.0f)), tmpAngle; // 文字本身的对角线与底边的角度
																	// 单位：弧度
		int aw, ah;
		// 文字是矩形，不能用hw旋转角度得到高度，得用对角线去旋转
		tmpAngle = textAngle + Math.toRadians(angle);
		ah = (int) (djx * Math.sin(tmpAngle));

		tmpAngle = Math.toRadians(angle) - textAngle;
		aw = (int) (djx * Math.cos(tmpAngle));

		if (aw == 0) {
			aw = fm.stringWidth(text.substring(0, 1));
		}
		if (ah == 0) {
			ah = hh;
		}
		area.width = Math.abs( aw );
		area.height = Math.abs( ah );
		return area;
	}

}
