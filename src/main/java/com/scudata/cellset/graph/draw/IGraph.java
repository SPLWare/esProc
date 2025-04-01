package com.scudata.cellset.graph.draw;

/**
 * 统计图形接口
 * @author Joancy
 *
 */
public interface IGraph {
	/**
	 * 图形绘制方法
	 * @param htmlLink 超链接缓冲
	 */
  public void draw( StringBuffer htmlLink );
}

