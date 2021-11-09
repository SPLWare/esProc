package com.raqsoft.expression.mfn.canvas;

import java.awt.Toolkit;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.CanvasFunction;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;


public class Draw extends CanvasFunction {
	public Object calculate(Context ctx) {
		int w = 800;
		int h = 600;
		if (param != null) {
			int subSize = param.getSubSize();
			if (subSize < 2) {
				throw new RQException("G.draw( w, h ), absence of params.");
			}
			IParam p = param.getSub(0);
			Expression exp = p.getLeafExpression();

			double dw = ((Number) exp.calculate(ctx)).doubleValue();
			p = param.getSub(1);
			exp = p.getLeafExpression();
			double dh = ((Number) exp.calculate(ctx)).doubleValue();
			if (dw * dh < 0) {
				throw new RQException("w or h can not be negative value.");
			}
			if (dw < 1) {
				dw = Toolkit.getDefaultToolkit().getScreenSize().getWidth()
						* dw;
			}
			if (dh < 1) {
				dh = Toolkit.getDefaultToolkit().getScreenSize().getHeight()
						* dh;
			}

			w = (int) dw;
			h = (int) dh;
		}
		if (option != null) {
			if (option.indexOf('j') != -1) {
				return canvas.toJpg(w, h);
			}
			if (option.indexOf('p') != -1) {
				return canvas.toPng(w, h);
			}
			if (option.indexOf('g') != -1) {
				return canvas.toGif(w, h);
			}
		}
		return canvas.toSVG(w, h);
	}
}
