package com.scudata.dm;

import java.io.*;

import com.scudata.chart.*;
import com.scudata.common.*;

/**
 * 画布类
 * 
 * @author Joancy
 *
 */
public class Canvas implements ICloneable, Externalizable, IRecord {
  private String name;
  private transient Sequence chartElements = new Sequence();

  private transient String htmlLinks=null;
  
  /**
   * 构造一个缺省画布
   */
  public Canvas() {
  }

  /**
   * 获取图画里面的html超链接
   * @return 超链接
   */
  public String getHtmlLinks(){
	  //必须先调用计算图形，因为要获取相应的w，h，所以没计算过时要报错。
	  if( htmlLinks==null) throw new RQException("You should call G.draw(w,h) before call G.hlink().");
	  return htmlLinks;
  }
  
  /**
   * 设置画布名称
   * @param name
   */
  public void setName(String name) {
	this.name = name;
  }

  /**
   * 取画布名称
   * @return
   */
  public String getName() {
	return name;
  }

  /**
   * 增加一个图元
   * @param elem  图元的序列表示
   */
  public void addChartElement(Sequence elem) {
	chartElements.add(elem);
  }

  /**
   * 获取所有图元
   * @return 图元序列表示的序列
   */
  public Sequence getChartElements(){
    return chartElements;
  }

  /**
   * 设置所有图元序列
   * @param elements 图元序列
   */
  public void setChartElements(Sequence elements){
    chartElements=elements;
  }

  /**
   * 清除图元
   */
  public void clear() {
	chartElements.clear();
  }

  private byte[] getImageBytes(int w, int h, byte fmt) {
	Engine e = new Engine(this.getChartElements());
	byte[] bytes = e.calcImageBytes(w, h, fmt);
	htmlLinks = e.getHtmlLinks();
	if( htmlLinks==null ) htmlLinks="";//null表示没有计算，空表示计算了，但是没有定义超链接
	return bytes;
  }

  /**
   * 将画布生成svg格式数据
   * @param w 宽度
   * @param h 高度
   * @return svg图形字节数据
   */
  public byte[] toSVG(int w, int h) { //Utf-8
	return getImageBytes(w, h, Consts.IMAGE_SVG);
  }

  /**
   * 将画布生成jpg格式数据
   * @param w 宽度
   * @param h 高度
   * @return jpg图形字节数据
   */
  public byte[] toJpg(int w, int h) {
	return getImageBytes(w, h, Consts.IMAGE_JPG);
  }

  /**
   * 将画布生成png格式数据
   * @param w 宽度
   * @param h 高度
   * @return png图形字节数据
   */
  public byte[] toPng(int w, int h) {
	return getImageBytes(w, h, Consts.IMAGE_PNG);
  }

  /**
   * 将画布生成gif格式数据
   * @param w 宽度
   * @param h 高度
   * @return gif图形字节数据
   */
  public byte[] toGif(int w, int h) {
	return getImageBytes(w, h, Consts.IMAGE_GIF);
  }

  /**
   * 实现toString的文本描述
   */
  public String toString(){
	  StringBuffer sb = new StringBuffer();
	  if(name!=null) sb.append(name+":");
	  if(chartElements!=null) sb.append( chartElements.length()+" elements.");
	  return sb.toString();
  }
  
  /**
   * 克隆画布对象
   * @return 克隆的画布
   */
  public Object deepClone(){
	  Canvas canvas = new Canvas();
	  canvas.name = name;
	  return canvas;
  }

  /**
   * 写内容到流
   * @param out ObjectOutput 输出流
   * @throws IOException
   */
  public void writeExternal(ObjectOutput out) throws IOException {
	  out.writeByte(1);
	  out.writeObject(name);
  }

  /**
   * 从流中读内容
   * @param in ObjectInput 输入流
   * @throws IOException
   * @throws ClassNotFoundException
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	  in.readByte();
	  name = (String)in.readObject();
  }

  /**
   * 实现IRecord接口
   */
  public byte[] serialize() throws IOException{
	  ByteArrayOutputRecord out = new ByteArrayOutputRecord();
	  out.writeString(name);
	  return out.toByteArray();
  }

  /**
   * 实现IRecord接口
   */
  public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
	  ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
	  name = in.readString();
  }
}
