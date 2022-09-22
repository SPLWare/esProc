package com.scudata.cellset;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.imageio.ImageIO;

import com.scudata.chart.Utils;
import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.ICloneable;
import com.scudata.common.IRecord;
import com.scudata.common.Logger;
import com.scudata.common.StringUtils;

/**
 * 背景图配置
 * 
 * @author Joancy
 *
 */
public class BackGraphConfig implements Externalizable, ICloneable, Cloneable,
		IRecord {
	private static final long serialVersionUID = 1L;

	/** 背景图配置类型：URL */
	public final static byte TYPE_URL = (byte) 0;
	/** 背景图配置类型：表达式 */
	public final static byte TYPE_EXP = (byte) 1;

	/** 背景图显示方式：显示 */
	public final static byte DISP_NONE = (byte) 10;
	/** 背景图显示方式：每页显示 */
	public final static byte DISP_PER_PAGE = (byte) 11;

	public static final byte SOURCE_NONE = 0; // 无
	public static final byte SOURCE_PICTURE = 1; // 图片
	public static final byte SOURCE_TEXT = 2; // 文字,水印背景图

	public static final byte MODE_NONE = 0; // 缺省
	public static final byte MODE_FILL = 1; // 填充
	public static final byte MODE_TILE = 2; // 平铺

	public static final byte TEXT_NORMAL = 0; // 正常
	public static final byte TEXT_TILT = 1; // 倾斜

	private byte type = TYPE_URL;
	private String value;
	private byte disp = DISP_PER_PAGE;

	// 生成图片的方式
	private byte imgSource = SOURCE_PICTURE;
	// 布局
	private byte mode = MODE_NONE;
	private String fontName = "Dialog";
	private int fontSize = 12;
	private int textColor = Color.LIGHT_GRAY.getRGB();
	private int textGap = 40;
	private int textTransparency = 30;

	private byte[] imageBytes = null;

	public transient byte[] tmpImageBytes = null;
	private transient String waterMark = null;

	/**
	 * 缺省构造函数
	 */
	public BackGraphConfig() {
	}

	/**
	 * 构造函数
	 *
	 * @param type
	 *            指定配置类型，可取值为TYPE_URL、TYPE_EXP
	 * @param value
	 *            参数type为TYPE_URL时此参数表示URL， 为TYPE_EXP时此参数表示表达式串
	 * @param dispMode
	 *            显示模式，可取值为DISP_DEFAULT、DISP_PER_PAGE
	 */
	public BackGraphConfig(byte type, String value, byte dispMode) {
		this.type = type;
		this.value = value;
		this.disp = dispMode;
	}

	/**
	 * 设置背景图片类型 TYPE_URL，TYPE_EXP
	 * 
	 * @param type
	 *            byte
	 */
	public void setType(byte type) {
		this.type = type;
	}

	/**
	 * 获得背景图片类型
	 * 
	 * @return byte
	 */
	public byte getType() {
		return this.type;
	}

	/**
	 * 设置ULR或表达式串，参数type为TYPE_URL时此参数表示URL，为TYPE_EXP时此参数表示表达式串
	 * 
	 * @param urlOrClassName
	 *            String
	 */
	public void setValue(String value) {
		this.value = value;
		this.tmpImageBytes = null;
	}

	/**
	 * 取得ULR或表达式串
	 * 
	 * @return String
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * 设置显示模式 DISP_DEFAULT，DISP_PER_PAGE
	 * 
	 * @param dispMode
	 *            byte
	 */
	public void setDispMode(byte dispMode) {
		this.disp = dispMode;
	}

	/**
	 * 取得显示方式
	 * 
	 * @return byte
	 */
	public byte getDispMode() {
		return this.disp;
	}

	/**
	 * 取背景图
	 */
	public byte[] getImageBytes() {
		return this.imageBytes;
	}

	/**
	 * 根据宽高，按照背景图配置，生成背景图
	 * 
	 * @param w
	 * @param h
	 * @return
	 */
	public Image getBackImage(int w, int h) {
		return getBackImage(w, h, 1.0f);
	}

	private int imageWidth = -1, imageHeight = -1;
	private int lastW = -1, lastH = -1; // 这是设计尺寸，没有scale的
	private Image lastImage = null;

	/**
	 * 该方法用于直接将背景图往g输出，从而文本绘制清晰
	 * @param g 导出PDF或者打印的图形设备
	 * @param w 宽度
	 * @param h 高度
	 * @param scale 绘制比例
	 */
	public void drawImage(Graphics g, int w, int h, float scale) {
		drawImage(g, w, h, scale, 0, 0, w, h);
	}

	/**
	 * 增加了绘制区域x1,y1,x2,y2
	 * @param g
	 * @param w
	 * @param h
	 * @param scale
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	public void drawImage(Graphics g, int w, int h, float scale, int x1,
			int y1, int x2, int y2) {
		switch (imgSource) {
		case SOURCE_PICTURE:
			// 将ImageIcon改为BufferedImage，并缓存调整好尺寸的Image
			// 图片尺寸发生变化时，重新获取图片
			boolean graphChanged = lastImage == null || w != lastW
					|| h != lastH;
			int iw,
			ih;// 图片尺寸
			Image image;
			if (graphChanged) {
				BufferedImage bimage = null;
				try {
					ByteArrayInputStream bis = new ByteArrayInputStream(
							imageBytes);
					bimage = ImageIO.read(bis);
				} catch (IOException e) {
					Logger.error(e);
					return;
				}
				iw = bimage.getWidth(null);
				ih = bimage.getHeight(null);
				if (iw * ih <= 0) {
					return;
				}
				iw = (int) (iw * scale);
				ih = (int) (ih * scale);
				if (scale != 1.0f) {
					image = bimage.getScaledInstance(iw, ih,
							java.awt.Image.SCALE_SMOOTH);
				} else {
					image = bimage;
				}
				lastW = w;
				lastH = h;
				lastImage = image;
				imageWidth = iw;
				imageHeight = ih;
			} else {
				image = lastImage;
				iw = imageWidth;
				ih = imageHeight;
			}
			// 用setClip替代cutImage来减少绘制时间 wunan 2022-09-22
			Shape oldClip = g.getClip();
			switch (mode) {
			case MODE_NONE:
				try {
					g.setClip(x1, y1, x2 - x1, y2 - y1); // 图片不能超出绘制范围
					g.drawImage(image, 0, 0, iw, ih, null);
				} finally {
					g.setClip(oldClip);
				}
				// image = ImageUtils.drawAndReturnFixImage(g, image, 0, 0, w,
				// h);
				break;
			case MODE_FILL:
				try {
					g.setClip(x1, y1, x2 - x1, y2 - y1); // 图片不能超出绘制范围
					g.drawImage(image, 0, 0, w, h, null);
				} finally {
					g.setClip(oldClip);
				}
				break;
			case MODE_TILE:
				try {
					int x = 0, y = 0;
					while (x < x2) {
						if (x + iw <= x1) { // 没有到绘制范围
							x += iw;
							continue;
						}
						y = 0;
						while (y < y2) {
							if (y + ih <= y1) { // 没有到绘制范围
								y += ih;
								continue;
							}
							int clipx = Math.max(x, x1);
							int clipy = Math.max(y, y1);
							g.setClip(clipx, clipy, Math.min(iw, x2 - clipx),
									Math.min(ih, y2 - clipy));
							g.drawImage(image, x, y, iw, ih, null);
							y += ih;
						}
						x += iw;
					}
				} finally {
					g.setClip(oldClip);
				}
				break;
			}
			break;
		case SOURCE_TEXT:
			if (!StringUtils.isValidString(waterMark))
				return;
			// 疑难杂症：不知道的原因，设置了setComposite方法会导致
			// 1：打印内容缺失
			// 2：itext没有实现，导出PDF时，没有不透明状态
			// 解决办法：将透明度设为100，也即不透明，此时不调用setComposite，同时设置字体颜色为浅色

			Composite old = setTransparent((Graphics2D) g,
					textTransparency / 100f);
			Color c1 = new Color(textColor);
			int textAngle = 0;
			if (mode == TEXT_TILT) {
				textAngle = -45;
			}
			int fSize = StringUtils.getScaledFontSize(fontSize, scale);
			Rectangle textRect = getTextRect(fontName, fSize, waterMark);
			iw = textRect.width;
			ih = textRect.height;
			int x = 0,
			y = 0;
			Font font = getFont(fontName, 0, fSize);
			Color c = c1;

			int row = 0,
			col = 0;
			while (x < w) {
				y = 0;
				row = 0;
				while (y < h) {
					row++;
					int mod = (row + col) % 2;
					if (mod == 1) {
						drawText(waterMark, x + iw / 2, y + ih / 2, font, c,
								textAngle, (Graphics2D) g);
					}
					y += ih + textGap;
				}
				x += iw + textGap;
				col++;
			}
			if (old != null) {
				((Graphics2D) g).setComposite(old);
			}
		}
	}

	/**
	 * 将当前背景图计算为图片对象
	 * @param w 宽度
	 * @param h 高度
	 * @param scale 绘制比例
	 * @return 图像对象
	 */
	public Image getBackImage(int w, int h, float scale) {
		if (imgSource == SOURCE_NONE) {
			return null;
		}
		if (imgSource == SOURCE_PICTURE && imageBytes == null) {
			return null;
		}
		if (imgSource == SOURCE_TEXT && waterMark == null) {
			return null;
		}
		BufferedImage bimage = null;
		bimage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics g;
		g = bimage.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, w, h);
		drawImage(g, w, h, scale);
		return bimage;
	}

	/**
	 * 设置背景图数据，一般用于图片需要不断刷新，而又没必要每次都重画时
	 * 用于提高界面绘制性能
	 * @param b 图像数据
	 */
	public void setImageBytes(byte[] b) {
		this.imageBytes = b;
		waterMark = null;
	}

	/**
	 * 获取计算过后的水印文字
	 * @return 水印文字
	 */
	public String getWaterMark() {
		return waterMark;
	}

	/**
	 * 设置计算后的水印文字，用于提高绘图性能
	 * @param wm 水印文字
	 */
	public void setWaterMark(String wm) {
		this.waterMark = wm;
		imageBytes = null;
	}

	/**
	 * 获取水印或者背景图的填充样式
	 * 三种方式，左上角， 拉伸填充，平铺
	 * 值为： BackGraphConfig.MODE_???
	 * @return 填充样式
	 */
	public byte getMode() {
		return mode;
	}

	/**
	 * 设置填充样式
	 * @param m 样式
	 */
	public void setMode(byte m) {
		mode = m;
	}

	/**
	 * 获取水印文本平铺时的文本间隔
	 * @return 间隔值
	 */
	public int getTextGap() {
		return textGap;
	}

	/**
	 * 设置水印文本的平铺时的文本间隔
	 * @param g 间隔值
	 */
	public void setTextGap(int g) {
		textGap = g;
	}

	/**
	 * 设置水印文本的透明度，值为0到100
	 * @param tran 透明值
	 */
	public void setTransparency(int tran) {
		textTransparency = tran;
	}

	/**
	 * 获取水印文本的透明度
	 * @return 透明值
	 */
	public int getTransparency() {
		return textTransparency;
	}

	/**
	 * 获取背景图的生成来源
	 * 有两种来源，用图形生成，以及水印文字生成
	 * 值为： BackGraphConfig.SOURCE_???
	 * @return 来源样式
	 */
	public byte getImageSource() {
		return imgSource;
	}

	/**
	 * 设置背景图的来源样式
	 * @param src 样式值
	 */
	public void setImageSource(byte src) {
		imgSource = src;
	}

	/**
	 * 获取水印文字的字体名称
	 * @return 字体名
	 */
	public String getFontName() {
		return fontName;
	}

	/**
	 * 设置水印文字的字体名
	 * @param fn 字体名
	 */
	public void setFontName(String fn) {
		fontName = fn;
	}

	/**
	 * 获取水印文字的字号
	 * @return 字号
	 */
	public int getFontSize() {
		return fontSize;
	}

	/**
	 * 设置水印文字的字号
	 * @param size 字号
	 */
	public void setFontSize(int size) {
		fontSize = size;
	}

	/**
	 * 获取水印文字的颜色值
	 * @return 颜色值
	 */
	public int getTextColor() {
		return textColor;
	}

	/**
	 * 设置水印文字的颜色
	 * @param c 颜色值
	 */
	public void setTextColor(int c) {
		textColor = c;
	}

	/**
	 * 序列化输出本类
	 * 
	 * @param out 对象输出流
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(3); // version
		out.writeByte(type);
		out.writeObject(value);
		out.writeByte(disp);
		out.writeObject(imageBytes);
		// 版本2， 增加布局模式
		out.writeByte(imgSource);
		out.writeByte(mode);
		out.writeObject(fontName);
		out.writeInt(fontSize);
		out.writeInt(textColor);
		out.writeInt(textGap);
		out.writeInt(textTransparency);
	}

	/**
	 * 序列化输入本类
	 * 
	 * @param in 对象输入流
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		byte version = in.readByte();
		type = in.readByte();
		value = (String) in.readObject();
		disp = in.readByte();
		imageBytes = (byte[]) in.readObject();
		if (version > 1) {
			imgSource = in.readByte();
			mode = in.readByte();
			fontName = (String) in.readObject();
			fontSize = in.readInt();
			textColor = in.readInt();
			textGap = in.readInt();
		}
		if (version > 2) {
			textTransparency = in.readInt();
		}
	}

	/**
	 * 序列化输出本类
	 * 
	 * @throws IOException
	 */
	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeByte(type);
		out.writeString(value);
		out.writeByte(disp);
		out.writeBytes(imageBytes);

		out.writeByte(imgSource);
		out.writeByte(mode);
		out.writeString(fontName);
		out.writeInt(fontSize);
		out.writeInt(textColor);
		out.writeInt(textGap);
		out.writeInt(textTransparency);

		return out.toByteArray();
	}

	/**
	 * 序列化输入本类
	 * 
	 * @param in
	 *            byte[]input
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void fillRecord(byte[] buf) throws IOException,
			ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		type = in.readByte();
		value = in.readString();
		disp = in.readByte();
		imageBytes = in.readBytes();
		if (in.available() > 0) {
			imgSource = in.readByte();
			mode = in.readByte();
			fontName = in.readString();
			fontSize = in.readInt();
			textColor = in.readInt();
			textGap = in.readInt();
		}
		if (in.available() > 0) {
			textTransparency = in.readInt();
		}
	}

	/**
	 * 克隆本类
	 * 
	 * @return Object
	 */
	public Object deepClone() {
		BackGraphConfig bgc = new BackGraphConfig();
		bgc.setType(type);
		bgc.setValue(value);
		bgc.setDispMode(disp);
		bgc.setImageBytes(imageBytes);

		bgc.setImageSource(imgSource);
		bgc.setMode(mode);
		bgc.setFontName(fontName);
		bgc.setFontSize(fontSize);
		bgc.setTextColor(textColor);
		bgc.setTextGap(textGap);
		bgc.setTransparency(textTransparency);

		return bgc;
	}

	/**
	 * 设置图形透明度
	 * @param g 图形设备
	 * @param transparent 透明图，范围0到100， 100时不透明，0为全透明
	 * @return 旧的Composite，用于调用该方法后的恢复
	 */
	public static Composite setTransparent(Graphics2D g, float transparent) {
		if (transparent >= 1) {
			return null;
		} else if (transparent < 0) {
			transparent = 0f;
		}
		Composite old = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				transparent));
		return old;
	}

	/**
	 * 获取文本绘制的占用空间
	 * @param textFont 文字字体
	 * @param textSize 文字大小
	 * @param text 文本值
	 * @return 宽高描述的文本空间
	 */
	public static Rectangle getTextRect(String textFont, int textSize,
			String text) {
		Font font = new Font(textFont, Font.PLAIN, textSize);
		Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
				.createGraphics();
		FontMetrics fm = g.getFontMetrics(font);
		int txtHeight = fm.getHeight();
		g.dispose();
		return new Rectangle(0, 0, fm.stringWidth(text), txtHeight);
	}

	/**
	 * 构造字体对象
	 * @param fontName 字体名称
	 * @param fontStyle 文本风格
	 * @param fontSize 字号
	 * @return 字体对象
	 */
	public synchronized static Font getFont(String fontName, int fontStyle,
			int fontSize) {
		if (fontName == null || fontName.trim().length() < 1) {
			fontName = "dialog";
		}

		Font f = new Font(fontName, fontStyle, fontSize);
		return f;
	}

	/**
	 * 获取文本的水平正常输出时的空间
	 * @param text 文本值
	 * @param g 图形设备
	 * @param font 字
	 * @return 占用空间
	 */
	public static Rectangle getHorizonArea(String text, java.awt.Graphics g,
			Font font) {
		Rectangle area = new Rectangle();
		FontMetrics fm = g.getFontMetrics(font);
		int hw = fm.stringWidth(text);
		int hh = fm.getAscent() + fm.getDescent(); // .getAscent();
		area.width = hw;
		area.height = hh;
		return area;
	}

	/**
	 * 获取文本旋转输出后的占用空间
	 * @param text 文本
	 * @param g 图形设备
	 * @param angle 旋转角度
	 * @param font 字体
	 * @return 占用空间
	 */
	public static Rectangle getRotationArea(String text, java.awt.Graphics g,
			int angle, Font font) {
		Rectangle area = new Rectangle();
		angle = angle % 360;
		if (angle < 0) {
			angle += 360;
		}
		Rectangle area0 = getTextSize(text, g, 0, font);
		double sin = Math.sin(angle * Math.PI / 180);
		double cos = Math.cos(angle * Math.PI / 180);
		if (sin < 0) {
			sin = -sin;
		}
		if (cos < 0) {
			cos = -cos;
		}
		int aw = (int) (area0.height * sin + area0.width * cos);
		int ah = (int) (area0.width * sin + area0.height * cos);
		area.width = aw;
		area.height = ah;
		return area;
	}

	/**
	 * 自动根据旋转角度，调用相关方法计算文本的占用空间
	 * @param text 文本
	 * @param g 图形设备
	 * @param angle 旋转角度
	 * @param font 字体
	 * @return 占用空间
	 */
	public static Rectangle getTextSize(String text, java.awt.Graphics g,
			int angle, Font font) {
		if (text == null) {
			return new Rectangle();
		}
		Rectangle rect = null;
		if (angle == 0) {
			rect = getHorizonArea(text, g, font);
		} else {
			rect = getRotationArea(text, g, angle, font);
		}
		if (rect.width < 0) {
			rect.width = -rect.width;
		}
		if (rect.height < 0) {
			rect.height = -rect.height;
		}
		return rect;
	}

	/**
	 * isImage时，要变换为左上角， 文本时变换为左下角；因为g在左下角绘制文本， 左上角绘制图形。
	 * @param posDesc
	 * @return
	 */
	public static Point getRealDrawPoint(Rectangle posDesc) {
		Rectangle rect = posDesc;
		// 绘图中心点
		int xloc = rect.x;
		int yloc = rect.y;

		yloc += rect.height / 2;
		// 所给参考点在中间，需要求到左下角x坐标
		xloc -= rect.width / 2;
		return new Point(xloc, yloc);
	}

	// protected static void setGraphAntiAliasingOff(Graphics2D g) {
	// g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	// RenderingHints.VALUE_ANTIALIAS_OFF);
	// }
	//
	// protected static void setGraphAntiAliasingOn(Graphics2D g) {
	// g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	// RenderingHints.VALUE_ANTIALIAS_ON);
	// }

	/**
	 * 在指定位置处输出文本
	 * @param txt 文本
	 * @param dx 横坐标
	 * @param dy 纵坐标
	 * @param font 字体
	 * @param c 颜色
	 * @param angle 旋转角度
	 * @param g 图形设备
	 */
	public static void drawText(String txt, double dx, double dy, Font font,
			Color c, int angle, Graphics2D g) {
		if (txt == null || txt.trim().length() < 1 || font.getSize() == 0) {
			return;
		}
		int x = (int) dx;
		int y = (int) dy;

		// 文字不重叠
		Rectangle rect = getTextSize(txt, g, angle, font);
		rect.x = x;
		rect.y = y;

		g.setFont(font);
		g.setColor(c);

		Point drawPoint = getRealDrawPoint(rect);
		int xloc = drawPoint.x;
		int yloc = drawPoint.y;

		Utils.setGraphAntiAliasingOff(g);

		// 非竖排文字
		if (angle != 0) {
			AffineTransform at = g.getTransform();
			Rectangle rect2 = getTextSize(txt, g, 0, font);
			rect2.setLocation(xloc, yloc - rect2.height);
			int delx = 0, dely = 0;
			angle = angle % 360;
			if (angle < 0) {
				angle += 360;
			}
			if (angle >= 0 && angle < 90) {
				delx = 0;
				dely = (int) (rect2.width * Math.sin(angle * Math.PI / 180));
			} else if (angle < 180) {
				dely = rect.height;
				delx = (int) (rect2.width * Math.cos(angle * Math.PI / 180));
			} else if (angle < 270) {
				delx = -rect.width;
				dely = (int) (-rect2.height * Math.sin(angle * Math.PI / 180));
			} else {
				dely = 0;
				delx = (int) (rect2.height * Math.sin(angle * Math.PI / 180));
			}
			AffineTransform at1 = AffineTransform.getRotateInstance(angle
					* Math.PI / 180, xloc - delx, yloc - dely);
			g.transform(at1);

			g.setColor(c);
			g.drawString(txt, xloc - delx, yloc - dely);

			g.setTransform(at);
		} else {
			g.setColor(c);
			g.drawString(txt, xloc, yloc);
		}
		Utils.setGraphAntiAliasingOn(g);
	}

}
