package com.scudata.expression.fn;

import com.scudata.common.*;
import com.scudata.dm.*;
import com.scudata.expression.*;
import com.scudata.resources.EngineMessage;

/**
 * 生成画布对象 canvas()
 * 集算器中定义画布，直接在单元格中使用canvas()函数，
 * 在其后的绘图程序中可以直接用单元格名称调用画布对象，设定绘图参数或者绘图。
 * @author runqian
 *
 */
public class CreateCanvas extends Function {

	public Object calculate(Context ctx) {
		if (param != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("canvas" + mm.getMessage("function.invalidParam"));
		}

		return new Canvas();
	}

	public Node optimize(Context ctx) {
		return this;
	}
}
