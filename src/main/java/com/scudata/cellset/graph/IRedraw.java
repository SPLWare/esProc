package com.scudata.cellset.graph;

import java.awt.Graphics2D;
/**
 * 重画接口用于导出PDF，打印时，可以将输出设备的g直接传给可重画对象
 * 使得ImageValue可以不使用已经画好的图形输出，而是将绘图过程直接向
 * g重画，解决了文字的清晰输出
 * 
 * 已经实现的有背景图 BackGraphConfig，统计图  ReportStatisticGraph
 * 
 * 条形码也可以有文本，但是条形码的绘制过程很复杂，有logo，旋转以及图形放大到跟格子一样大等各种图片生成
 * 实现g的直接重画有点麻烦，目前没实现
 * 条形码的清晰文本输出可以用下一个格子专门绘制文本
 * @author Joancy
 *
 */
public interface IRedraw{
	public void repaint(Graphics2D g, int w, int h);
}