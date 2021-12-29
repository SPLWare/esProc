package com.scudata.chart;

import org.w3c.dom.*;

import com.scudata.app.common.*;
import com.scudata.chart.edit.*;
import com.scudata.chart.element.*;
import com.scudata.common.*;
import com.scudata.dm.*;
import com.scudata.expression.*;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

/**
 * 绘图引擎
 */
public class Engine {
	private ArrayList<IElement> elements;
	private ArrayList<TickAxis> axisList = new ArrayList<TickAxis>(); // 轴
	private ArrayList<ICoor> coorList = new ArrayList<ICoor>(); // 坐标系
	private ArrayList<DataElement> dataList = new ArrayList<DataElement>(); // 数据图元
	private ArrayList<TimeAxis> timeList = new ArrayList<TimeAxis>(); // 时间轴

	private transient ArrayList<Shape> allShapes = new ArrayList<Shape>();
	private transient ArrayList<String> allLinks = new ArrayList<String>();
	private transient ArrayList<String> allTitles = new ArrayList<String>();
	private transient ArrayList<String> allTargets = new ArrayList<String>();
	private transient int w, h;
	private transient Graphics2D g;
	private transient ArrayList textAreas = new ArrayList(); // 文本区域已经占用的空间
	private transient StringBuffer html;

	private transient double t_maxDate = 0, t_minDate = Long.MAX_VALUE;// 所有时间轴的最大最小时间点

	/**
	 * 构造绘图引擎
	 */
	public Engine() {
	}

	/**
	 * 设置图形元素列表
	 * 
	 * @param ies
	 *            绘图元素
	 */
	public void setElements(ArrayList<IElement> ies) {
		elements = ies;
	}

	/**
	 * 设置刻度轴列表
	 * 
	 * @param tas
	 *            刻度轴列表
	 */
	public void setAxisList(ArrayList<TickAxis> tas) {
		axisList = tas;
	}

	/**
	 * 设置坐标系列表
	 * 
	 * @param ics
	 *            坐标系列表
	 */
	public void setCoorList(ArrayList<ICoor> ics) {
		coorList = ics;
	}

	/**
	 * 设置数据图元列表
	 * 
	 * @param des
	 *            数据图元
	 */
	public void setDataList(ArrayList<DataElement> des) {
		dataList = des;
	}

	/**
	 * 设置时间轴
	 * 
	 * @param tas
	 *            时间轴列表
	 */
	public void setTimeList(ArrayList<TimeAxis> tas) {
		timeList = tas;
	}

	/**
	 * 获取时间轴
	 * 
	 * @param name
	 *            轴名称
	 * @return 时间轴对象
	 */
	public TimeAxis getTimeAxis(String name) {
		for (int i = 0; i < timeList.size(); i++) {
			TimeAxis axis = timeList.get(i);
			if (axis.getName().equals(name)) {
				return axis;
			}
		}
		return null;
	}

	/**
	 * 获取所有图元对应的形状列表
	 * 
	 * @return
	 */
	public ArrayList<Shape> getShapes() {
		return allShapes;
	}

	/**
	 * 获取按照总帧数frameCount的第frameIndex帧图像的计算引擎
	 * @param frameCount;总帧数
	 * @param frameIndex;第几帧，从0开始
	 * @return
	 */
	public Engine getFrameEngine(int frameCount, int frameIndex){
		Engine e = clone();

		ArrayList<DataElement> des = new ArrayList<DataElement>();
		double timeSlice = (t_maxDate-t_minDate)*1f/frameCount;//时间跨度按照总帧数切分
		double frameTime = t_minDate+ timeSlice*frameIndex;//计算出当前帧所处时间点
		
		for(DataElement de:dataList){
			String timeAxis = de.getAxisTimeName(); 
			TimeAxis ta = e.getTimeAxis(timeAxis);
			if(ta.displayMark){
//				加上时间戳
				des.add( ta.getMarkElement(frameTime) );
			}
			if( StringUtils.isValidString(timeAxis) ){
				e.elements.remove(de);//elements已经包含所有图元，此时产生新的数据图元后，需要将老的数据图元剔除
				des.add(de.getFrame( frameTime ) );
			}else{
				des.add(de);
			}
		}
		e.setDataList(des);
		return e;
	}

	/**
	 * 图元有超链接的话，获取超链接列表
	 * 
	 * @return
	 */
	public ArrayList<String> getLinks() {
		return allLinks;
	}

	/**
	 * 返回所有图形的HTML超链接代码
	 * 
	 * @return
	 */
	public String getHtmlLinks() {
		return generateHyperLinks(true);
	}

	/**
	 * 获取任意形状shape的边界多边形
	 * 
	 * @param shape
	 * @return 多边形的坐标点
	 */
	private ArrayList<Point> getShapeOutline(Shape shape) {
		// 获取形状shape的平滑路径
		PathIterator iter = new FlatteningPathIterator(
				shape.getPathIterator(new AffineTransform()), 1);
		ArrayList<Point> points = new ArrayList<Point>();
		float[] coords = new float[6];
		while (!iter.isDone()) {
			iter.currentSegment(coords);
			int x = (int) coords[0];
			int y = (int) coords[1];
			points.add(new Point(x, y));
			iter.next();
		}
		return points;
	}

	private String getRectCoords(Rectangle rect) {
		int x, y, w, h;
		x = rect.x;
		y = rect.y;
		w = rect.width;
		h = rect.height;
		// 超连接处理
		int minimum = 10;
		if (w < minimum) {
			w = minimum;
		}
		if (h < minimum) {
			h = minimum;
		}
		String coords = x + "," + y + "," + (x + w) + "," + (y + h);
		return coords;
	}

	private String getPolyCoords(Shape shape) {
		StringBuffer buf = new StringBuffer();
		ArrayList<Point> polyPoints = getShapeOutline(shape);
		for (int i = 0; i < polyPoints.size(); i++) {
			Point p = polyPoints.get(i);
			if (i > 0) {
				buf.append(",");
			}
			buf.append(p.getX() + "," + p.getY());
		}
		return buf.toString();
	}

	private String dealSpecialChar(String str) {
		// 特殊符号改为上层处理，此处不再处理
		return str;
	}

	private String getLinkHtml(String link, String shape, String coords,
			String title, Object target) {
		StringBuffer sb = new StringBuffer(128);
		sb.append("<area shape=\"").append(shape).append("\" coords=\"");
		sb.append(coords);
		if (StringUtils.isValidString(link)) {
			link = dealSpecialChar(link);
			sb.append("\" href=\"").append(link).append("\" target=\"")
					.append(target);
		}

		if (StringUtils.isValidString(title)) {
			title = dealSpecialChar(title);
			sb.append("\" title=\"").append(title);
		}
		sb.append("\">\n");
		return sb.toString();
	}

	// svg没有提示信息，所以这里的title属性无效。
	private String getLinkSvg(String link, String shape, String coords,
			String title, Object target) {
		StringBuffer sb = new StringBuffer(128);
		link = dealSpecialChar(link);
		sb.append("<a xlink:href=\"").append(link);
		sb.append("\" target=\"");
		sb.append(target);
		sb.append("\">\n");

		sb.append("<");
		sb.append(shape);
		sb.append(" ");
		sb.append(coords);
		sb.append("/>\n");

		sb.append("</a>");

		return sb.toString();
	}

	/**
	 * 根据isHtml生成Html的还是svg的超链接
	 * 
	 * @param isHtml
	 *            ，真返回html的超链接串，否则为svg图形的链接串
	 * @return
	 */
	private String generateHyperLinks(boolean isHtml) {
		if (allLinks.isEmpty())
			return null;

		StringBuffer buf = new StringBuffer();
		String link, shape, coords, target, title;
		for (int i = 0; i < allShapes.size(); i++) {
			link = allLinks.get(i);
			Shape s = allShapes.get(i);
			target = allTargets.get(i);
			title = allTitles.get(i);
			if (isHtml) {
				if (s instanceof Rectangle) {
					shape = "rect";
					coords = getRectCoords((Rectangle) s);
				} else {// if(s instanceof Polygon){
					shape = "poly";
					coords = getPolyCoords(s);
				}
				buf.append(getLinkHtml(link, shape, coords, title, target));
			} else {// svg
				String style = " style=\"fill-opacity:0;stroke-width:0\"";
				if (s instanceof Rectangle) {
					Rectangle r = (Rectangle) s;
					shape = "rect";
					coords = "x=\"" + r.x + "\" y=\"" + r.y + "\" width=\""
							+ r.width + "\" height=\"" + r.height + "\""
							+ style;
				} else {// if(s instanceof Polygon){
					shape = "polygon";
					coords = "points=\"" + getPolyCoords(s) + "\"" + style;
				}
				buf.append(getLinkSvg(link, shape, coords, title, target));
			}
		}
		return buf.toString();
	}

	/**
	 * 为了防止图元重叠，绘制过程中每个画完的图元位置都会缓存 该方法用来判断指定的矩形位置rect是否跟已经画完的图形元素有相交
	 * 
	 * @param rect
	 *            指定的矩形位置
	 * @return 如果有相交时返回true，否则false
	 */
	public boolean intersectTextArea(Rectangle rect) {
		int size = textAreas.size();
		for (int i = 0; i < size; i++) {
			Rectangle tmp = (Rectangle) textAreas.get(i);
			if (tmp.intersects(rect)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 每画完一个图元后，需要将当前图元所处的位置信息添加到引擎缓存
	 * 
	 * @param rect
	 *            图元对应的矩形描述位置
	 */
	public void addTextArea(Rectangle rect) {
		textAreas.add(rect);
	}

	private IElement getElement(Sequence chartParams) {
		ChartParam cp = (ChartParam) chartParams.get(1);
		ElementInfo ei = ElementLib.getElementInfo(cp.getName());
		if (ei == null) {
			throw new RuntimeException("Unknown chart element: " + cp.getName());
		}
		ObjectElement oe = ei.getInstance();
		oe.loadProperties(chartParams);
		return oe;
	}

	/**
	 * 用定义好的图元序列构造绘图引擎
	 * 
	 * @param chartElements
	 *            图形元素序列
	 */
	public Engine(Sequence chartElements) {
		int size = chartElements.length();
		this.elements = new ArrayList<IElement>();
		for (int i = 1; i <= size; i++) {
			Sequence chartParams = (Sequence) chartElements.get(i);
			IElement e = getElement(chartParams);
			this.elements.add(e);
		}
		prepare();
	}

	/**
	 * 获取刻度轴列表
	 * 
	 * @return 刻度轴
	 */
	public ArrayList<TickAxis> getAxisList() {
		return axisList;
	}

	/**
	 * 获取数据图元列表
	 * 
	 * @return 数据图元
	 */
	public ArrayList<DataElement> getDataList() {
		return dataList;
	}

	/**
	 * 获取坐标系列表，一个画布里可以布置多个坐标系，以便绘制组合图形
	 * 
	 * @return 坐标系
	 */
	public ArrayList<ICoor> getCoorList() {
		return coorList;
	}

	/**
	 * 根据刻度轴的名称获取相应的刻度轴对象
	 * 
	 * @param name
	 *            刻度轴名称
	 * @return 刻度轴对象
	 */
	public TickAxis getAxisByName(String name) {
		for (int i = 0; i < axisList.size(); i++) {
			TickAxis axis = axisList.get(i);
			if (axis.getName().equals(name)) {
				return axis;
			}
		}
		return null;
	}

	public IMapAxis getMapAxisByName(String name) {
		for (int i = 0; i < elements.size(); i++) {
			IElement e = elements.get(i);
			if (e instanceof IMapAxis) {
				IMapAxis ma = (IMapAxis) e;
				if (ma.getName().equals(name)) {
					return ma;
				}
			}
		}
		return null;
	}

	private ArrayList<DataElement> getDataElementsOnAxis(String axis) {
		ArrayList<DataElement> al = new ArrayList<DataElement>();
		int size = dataList.size();
		for (int i = 0; i < size; i++) {
			DataElement de = dataList.get(i);
			if (de.isPhysicalCoor()) {
				continue;
			}

			if (de.getAxis1Name().equals(axis)
					|| de.getAxis2Name().equals(axis)) {
				al.add(de);
			}
		}
		return al;
	}

	private ArrayList<DataElement> getDataElementsOnTime(String axis) {
		ArrayList<DataElement> al = new ArrayList<DataElement>();
		int size = dataList.size();
		for (int i = 0; i < size; i++) {
			DataElement de = dataList.get(i);

			if (de.getAxisTimeName().equals(axis)) {
				al.add(de);
			}
		}
		return al;
	}

	private void prepare() {
		// 只考虑非组合图时，只有一个图例，允许不设置图例值，可以自动从枚举轴获取图例值。
		ArrayList<Legend> legends = new ArrayList<Legend>();
		// 挑出轴图元
		for (int i = 0; i < elements.size(); i++) {
			IElement e = elements.get(i);
			e.setEngine(this);
			if (e instanceof TickAxis) {
				if (axisList.contains(e)) {
					continue;
				}
				axisList.add((TickAxis) e);
			}
			if (e instanceof Legend) {
				legends.add((Legend) e);
			}
			if (e instanceof TimeAxis) {
				timeList.add((TimeAxis) e);
			}
		}
		// 挑出数据图元，构造用到的坐标系
		for (int i = 0; i < elements.size(); i++) {
			Object e = elements.get(i);
			if (!(e instanceof DataElement)) {
				continue;
			}
			DataElement de = (DataElement) e;
			dataList.add(de);
			if (de.isPhysicalCoor()) {
				continue;
			}

			String name1 = de.getAxis1Name();
			String deName = de.getClass().getName();
			int lastDot = deName.lastIndexOf(".");
			deName = "Data element " + deName.substring(lastDot + 1);
			if (!StringUtils.isValidString(name1)) {
				throw new RuntimeException(deName
						+ "'s property axis1 is not valid.");
			}
			String name2 = de.getAxis2Name();
			if (!StringUtils.isValidString(name2)) {
				throw new RuntimeException(deName
						+ "'s property axis2 is not valid.");
			}
			TickAxis axis1 = getAxisByName(name1);
			if (axis1 == null) {
				throw new RuntimeException(deName + "'s axis1: " + name1
						+ " is not defined.");
			}
			TickAxis axis2 = getAxisByName(name2);
			if (axis2 == null) {
				throw new RuntimeException(deName + "'s axis2: " + name2
						+ " is not defined.");
			}

			int L1 = axis1.getLocation();
			int L2 = axis2.getLocation();
			ICoor coor = null;
			RuntimeException re = new RuntimeException("Axis " + name1
					+ " and " + name2
					+ " can not construct a coordinate system.");
			switch (L1) {
			case Consts.AXIS_LOC_H:
				if (L2 != Consts.AXIS_LOC_V) {
					throw re;
				}
				coor = new CartesianCoor();
				break;
			case Consts.AXIS_LOC_V:
				if (L2 != Consts.AXIS_LOC_H) {
					throw re;
				}
				coor = new CartesianCoor();
				break;
			case Consts.AXIS_LOC_POLAR:
				if (L2 != Consts.AXIS_LOC_ANGLE) {
					throw re;
				}
				coor = new PolarCoor();
				break;
			case Consts.AXIS_LOC_ANGLE:
				if (L2 != Consts.AXIS_LOC_POLAR) {
					throw re;
				}
				coor = new PolarCoor();
				break;
			}
			coor.setAxis1(axis1);
			coor.setAxis2(axis2);

			if (!coorList.contains(coor)) {
				coorList.add(coor);
			}
		}

		// 数据图元绘图前准备工作,数据图元会调整数据，所以得在轴图元之前准备
		for (int i = 0; i < dataList.size(); i++) {
			DataElement de = dataList.get(i);
			de.prepare();
		}

		// 枚举轴图元先准备，因为后续的数值轴准备过程中涉及到累积时，会按照枚举值来累积计算最大值
		for (int i = 0; i < axisList.size(); i++) {
			TickAxis axis = axisList.get(i);
			if (axis instanceof EnumAxis) {
				ArrayList<DataElement> al = getDataElementsOnAxis(axis
						.getName());
				axis.prepare(al);
				if (legends.size() == 1) {
					Sequence seq = ((EnumAxis) axis).series;
					if (seq == null || seq.length() == 0) {
						seq = ((EnumAxis) axis).categories;
					}
					Legend legend = legends.get(0);
					if (legend.legendText.getLength() == 0) {
						legend.legendText.setValue(seq);
					}
				}

			}
		}

		// 轴图元绘图前准备工作
		for (int i = 0; i < axisList.size(); i++) {
			TickAxis axis = axisList.get(i);
			if (axis instanceof EnumAxis) {
				continue;
			}
			ArrayList<DataElement> al = getDataElementsOnAxis(axis.getName());
			axis.prepare(al);
		}

		// 时间轴准备工作
		for (int i = 0; i < timeList.size(); i++) {
			TimeAxis ta = timeList.get(i);
			ArrayList<DataElement> al = getDataElementsOnTime(ta.getName());
			ta.prepare(al);
			t_maxDate = Math.max(ta.getMaxDate(), t_maxDate);
			t_minDate = Math.min(ta.getMinDate(), t_minDate);
		}
	}

	/**
	 * 获取所有绘图元素列表
	 * 
	 * @return 全部绘图元素
	 */
	public ArrayList<IElement> getElements() {
		return elements;
	}

	/**
	 * 是否动画
	 * 
	 * @return 如果是动画返回true
	 */
	public boolean isAnimate() {
		return !timeList.isEmpty();
	}

	private byte[] generateSVG(int w, int h) throws Exception {
		Object batikDom = Class.forName(
				"org.apache.batik.dom.GenericDOMImplementation").newInstance();

		DOMImplementation domImpl = (DOMImplementation) AppUtil.invokeMethod(
				batikDom, "getDOMImplementation", new Object[] {});
		// org.apache.batik.dom.GenericDOMImplementation.getDOMImplementation();

		// Create an instance of org.w3c.dom.Document.
		String svgNS = "http://www.w3.org/2000/svg";
		Document document = domImpl.createDocument(svgNS, "svg", null);

		// Create an instance of the SVG Generator.
		Class cls = Class.forName("org.apache.batik.svggen.SVGGraphics2D");
		Constructor con = cls.getConstructor(new Class[] { Document.class });
		Object g2d = con.newInstance(new Object[] { document });

		// org.apache.batik.svggen.SVGGraphics2D ggd = new
		// org.apache.batik.svggen.SVGGraphics2D(document);

		draw((Graphics2D) g2d, 0, 0, w, h, null);

		// Finally, stream out SVG to the standard output using
		// UTF-8 encoding.
		boolean useCSS = true; // we want to use CSS style attributes
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Writer out = new OutputStreamWriter(baos, "UTF-8");
		// StringWriter out = new StringWriter();//直接写串出去，不转换字符集

		AppUtil.invokeMethod(g2d, "stream", new Object[] { out,
				new Boolean(useCSS) }, new Class[] { Writer.class,
				boolean.class });
		// g2d.stream(out, useCSS);

		out.flush();
		out.close();
		// return out.toString().getBytes();

		baos.close();

		byte[] bs = baos.toByteArray();
		String links = generateHyperLinks(false);
		if (links != null) {// 拼接上超链接
			String buf = new String(bs, "UTF-8");
			int n = buf.lastIndexOf("</svg");
			StringBuffer sb = new StringBuffer();
			sb.append(buf.substring(0, n));
			sb.append(links);
			sb.append("</svg>");
			bs = sb.toString().getBytes("UTF-8");
		}

		return bs;
	}

	/**
	 * 将画完的缓冲图像转换为相应格式的图形数据
	 * 
	 * @param bi
	 *            待转换的缓冲图像
	 * @param imageFmt
	 *            目标图片格式，Consts.IMAGE_XXX
	 * @return 转换为图片格式后的字节数组
	 */
	public static byte[] getImageBytes(BufferedImage bi, byte imageFmt)
			throws Exception {
		byte[] bytes = null;
		switch (imageFmt) {
		case Consts.IMAGE_GIF:
			bytes = ImageUtils.writeGIF(bi);
			break;
		case Consts.IMAGE_JPG:
			bytes = ImageUtils.writeJPEG(bi);
			break;
		case Consts.IMAGE_PNG:
			bytes = ImageUtils.writePNG(bi);
			break;
		}
		return bytes;
	}

	/**
	 * 根据指定宽高以及输出图形格式绘制缓冲图像
	 * 
	 * @param w
	 *            图形宽度，像素
	 * @param h
	 *            图形高度，像素
	 * @param imageFmt
	 *            待产生的图片格式
	 * @return 画完后的缓冲图像
	 */
	public BufferedImage calcBufferedImage(int w, int h, byte imageFmt) {
		BufferedImage bi = null;

		if (imageFmt == Consts.IMAGE_GIF || imageFmt == Consts.IMAGE_PNG) {
			bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		} else if (imageFmt == Consts.IMAGE_JPG) {
			bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		} else { // Flash
			imageFmt = Consts.IMAGE_PNG;
			bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		}
		Graphics2D lg = (Graphics2D) bi.getGraphics();

		Utils.setIsGif( imageFmt == Consts.IMAGE_GIF );
		if (imageFmt == Consts.IMAGE_PNG) {
		} else if (imageFmt == Consts.IMAGE_JPG) {
			// 缺省为白色背景，否则缺省黑色难看
			lg.setColor(Color.white);
			lg.fillRect(0, 0, w, h);
		} else { // Flash
		}

		draw(lg, 0, 0, w, h, null);
		return bi;
	}

	/**
	 * 作用同calcBufferedImage，参数参考前述方法
	 * 
	 * @param w
	 * @param h
	 * @param imageFmt
	 * @return 绘制完成后的图片字节数组
	 */
	public byte[] calcImageBytes(int w, int h, byte imageFmt) {
		Graphics2D lg = null;
		try {
			if (w + h == 0) {
				int[] wh = getBackOrginalWH();
				if (wh != null) {
					w = wh[0];
					h = wh[1];
				}
			}
			if (imageFmt == Consts.IMAGE_SVG) {
				return generateSVG(w, h);
			}

			BufferedImage bi = calcBufferedImage(w, h, imageFmt);
			return getImageBytes(bi, imageFmt);
		} catch (Exception x) {
			throw new RuntimeException(x);
		} finally {
			if (lg != null) {
				lg.dispose();
			}
		}
	}

	/**
	 * 获取背景图的原始宽高，背景图填充方式需要根据该信息相应填充或者拉伸
	 * 
	 * @return 宽和高构成的像素数组
	 */
	public int[] getBackOrginalWH() {
		int size = elements.size();
		for (int i = 0; i < size; i++) {
			IElement e = elements.get(i);
			if (e instanceof com.scudata.chart.element.BackGround) {
				BackGround bg = (BackGround) e;
				return bg.getOrginalWH();
			}
		}
		return null;
	}

	/**
	 * 执行画图动作，参数html为null时不生成超链接
	 * 
	 * @param g
	 *            图形设备
	 * @param x
	 *            x坐标
	 * @param y
	 *            y坐标
	 * @param w
	 *            图形宽度
	 * @param h
	 *            图形高度
	 * @param html
	 *            用于缓存图形中产生的超链接信息
	 */
	public void draw(Graphics2D g, int x, int y, int w, int h, StringBuffer html) {
		long b = System.currentTimeMillis();

		this.g = g;
		this.w = w;
		this.h = h;
		this.html = html;
		// 只考虑x，y为正数的情形
		if (x + y != 0) {
			g.translate(x, y);
		}
		Utils.setGraphAntiAliasingOn(g);
		textAreas.clear();
		ArrayList<IElement> bufElements = new ArrayList<IElement>();// 缓冲图元，图元分批绘制，先绘制过的，就从缓冲移除
		bufElements.addAll(elements);

		int size = bufElements.size();
		for (int i = 0; i < size; i++) {
			IElement e = (IElement) elements.get(i);
			e.beforeDraw();
		}

		// 第一步，先画背景图，背景一般只能有一个，此处不判断
		for (int i = 0; i < size; i++) {
			IElement e = elements.get(i);
			if (e instanceof com.scudata.chart.element.BackGround) {
				e.drawBack();
				bufElements.remove(e);// 已经绘制过的图元从缓冲去掉
			}
		}

		// 第二步，绘制轴图元
		drawElements(getAxisList());
		bufElements.removeAll(getAxisList());

		// 第三步，绘制数据图元
		drawElements(getDataList());
		ArrayList<DataElement> des = getDataList();
		for (int i = 0; i < des.size(); i++) {
			DataElement de = des.get(i);
			Object ss = de.getShapes();
			if (ss == null)
				continue;
			allShapes.addAll(de.getShapes());
			ArrayList<String> links = de.getLinks();
			allLinks.addAll(links);
			allTitles.addAll(de.getTitles());
			// for (int n = 0; n < links.size(); n++) {
			// allTargets.add(de.getTarget());// target跟link对齐
			// }
			allTargets.addAll(de.getTargets());
		}
		bufElements.removeAll(getDataList());

		// 第四步，绘制剩余的图元
		drawElements(bufElements);

		if (x + y != 0) {
			g.translate(-x, -y);
		}

		long e = System.currentTimeMillis();
		Logger.debug("Calc chart last time: " + (e - b) + " ms");
	}

	private void drawElements(ArrayList als) {
		int size = als.size();
		for (int i = 0; i < size; i++) {
			IElement e = (IElement) als.get(i);
			e.drawBack();
		}
		for (int i = 0; i < size; i++) {
			IElement e = (IElement) als.get(i);
			e.draw();
		}
		for (int i = 0; i < size; i++) {
			IElement e = (IElement) als.get(i);
			e.drawFore();
		}
	}

	/**
	 * 获取当前绘图设备
	 * 
	 * @return
	 */
	public Graphics2D getGraphics() {
		return g;
	}

	/**
	 * 获取待绘制的图形宽度，单位像素
	 * 
	 * @return 图形宽度
	 */
	public int getW() {
		return w;
	}

	/**
	 * 获取待绘制的图形高度，单位像素
	 * 
	 * @return 图形高度
	 */
	public int getH() {
		return h;
	}

	/**
	 * 计算val值位于图形中的像素位置，val大于1时，直接表示像素位置； 0<val<=1时，表示相对于当前图形宽度的比例位置；
	 * val<1时，表示相对于右下角时的反向位置
	 * 
	 * @param val
	 *            待转换的数值
	 * @return 转换后的横向图形绝对位置像素值，采用实数精度，避免累积误差
	 */
	public double getXPixel(double val) {
		return getPixel(val, getW());
	}

	/**
	 * 含义同getXPixel，参考相应内容
	 * 
	 * @param val
	 *            带转换的数值
	 * @return 纵向像素坐标
	 */
	public double getYPixel(double val) {
		return getPixel(val, getH());
	}

	private double getPixel(double val, double length) {
		if (val > 1) { // val大于1时表示绝对像素坐标
			return val;
		} else if (val >= 0) {
			// 否则表示相对图形length的比例坐标
			return length * val;
		} else {// 负数时相对右或者下的像素数
			if (val > -1) {
				val = length * val;
			}
			return length + val;
		}
	}

	public StringBuffer getHtml() {
		return html;
	}

	/**
	 * 克隆当前绘图引擎
	 */
	public Engine clone() {
		Engine e = new Engine();
		e.elements = (ArrayList<IElement>) elements.clone();
		e.axisList = (ArrayList<TickAxis>) axisList.clone();
		e.coorList = (ArrayList<ICoor>) coorList.clone();
		e.dataList = (ArrayList<DataElement>) dataList.clone();
		e.timeList = (ArrayList<TimeAxis>)timeList.clone();
		for (IElement ie : e.elements) {
			ie.setEngine(e);
		}

		return e;
	}

	public static void main(String[] args) {
		BufferedImage bi = new BufferedImage(633, 450,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bi.createGraphics();
		Utils.setGraphAntiAliasingOn(g);

		// Point2D.Double p1 = new Point2D.Double(10, 10);
		// Point2D.Double p2 = new Point2D.Double(100, 100);
		// Paint paint = new GradientPaint(50, 50, Color.red, 100, 50,
		// Color.blue,
		// true);
		// g.setPaint(paint);
		// g.fillRect((int) p1.getX(), (int) p1.getY(), 90, 90);
		Path2D p = new Path2D.Double();
		p.moveTo(10, 10);
		p.lineTo(20, 10);
		p.lineTo(20, 20);
		p.closePath();
		g.setColor(Color.black);
		g.draw(p);
		g.dispose();
		try {
			FileOutputStream os = new FileOutputStream("c:/test.png");
			com.scudata.common.ImageUtils.writePNG(bi, os);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		g.dispose();
		/**/
	}
}
