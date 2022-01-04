package com.scudata.expression.mfn.canvas;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.CanvasFunction;
import com.scudata.resources.EngineMessage;

public class HLink extends CanvasFunction {
	public Object calculate(Context ctx) {
		if (param != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hlink" + mm.getMessage("function.invalidParam"));
		}
		
		return canvas.getHtmlLinks();
	}
}
