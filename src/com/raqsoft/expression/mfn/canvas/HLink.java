package com.raqsoft.expression.mfn.canvas;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.CanvasFunction;
import com.raqsoft.resources.EngineMessage;

public class HLink extends CanvasFunction {
	public Object calculate(Context ctx) {
		if (param != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hlink" + mm.getMessage("function.invalidParam"));
		}
		
		return canvas.getHtmlLinks();
	}
}
