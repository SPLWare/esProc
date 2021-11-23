package com.scudata.expression.mfn.canvas;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.CanvasFunction;
import com.scudata.expression.ChartParam;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;

public class Plot extends CanvasFunction {
	public Object calculate(Context ctx) {
	  if (param == null) {
	    return null;
	  }
	  Sequence s = new Sequence();
	  if( param.isLeaf() ){
	    ChartParam cp = calcSubParam(param, ctx);
	    s.add(cp);
	  }else{
	    int subSize = param.getSubSize();
	    for (int i = 0; i < subSize; i++) {
	      IParam p = param.getSub(i);
	      ChartParam cp = calcSubParam(p, ctx);
	      s.add(cp);
	    }
	  }
	  canvas.addChartElement(s);
	  return s;
	}

	private Object calcExp(Expression exp, Context ctx) {
	  if (exp == null) {
	    return null;
	  }
	  Object val = exp.calculate(ctx);
	  return val;
	}

	private ChartParam calcSubParam(IParam p, Context ctx) {
	  String name = null, axis = null;
	  Object value = null;
	  Object tmp;
	  if (p.isLeaf()) {
	    Expression exp = p.getLeafExpression();
	    tmp = calcExp(exp, ctx);
	    name = (tmp == null) ? null : tmp.toString();
	  }
	  else {
	    int size = p.getSubSize();
	    for (int i = 0; i < size; i++) {
	      IParam subParam = p.getSub(i);
	      Expression exp = subParam.getLeafExpression();
	      tmp = calcExp(exp, ctx);
	      if (i == 0) {
		name = (tmp == null) ? null : tmp.toString();
	      }
	      if (i == 1) {
		value = tmp;
	      }
	      if (i == 2) {
		axis = (tmp == null) ? null : tmp.toString();
	      }
	    }
	  }
	  return new ChartParam(name, value, axis);
	}
}
