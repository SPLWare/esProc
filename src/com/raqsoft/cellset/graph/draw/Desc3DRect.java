package com.raqsoft.cellset.graph.draw;

import java.awt.*;
import com.raqsoft.chart.ChartColor;

/**
 * 封装三维立体柱的属性
 * @author Joancy
 *
 */
public class Desc3DRect {
  public double x,y,w,h,coorShift;
  public int borderStyle;
  public Color borderColor;
  public float borderWeight,transparent;
  public boolean drawShade,convexEdge,isVertical;
  public ChartColor fillColor;
  
}
