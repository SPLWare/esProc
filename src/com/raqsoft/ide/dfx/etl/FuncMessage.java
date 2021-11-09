package com.raqsoft.ide.dfx.etl;

import java.util.Locale;
import com.raqsoft.common.*;

/**
 * 辅助函数的资源编辑类
 * @author Joancy
 *
 */
public class FuncMessage {

	private FuncMessage() {
	}

	public static MessageManager get() {
	  return get(Locale.getDefault());
	}

	public static MessageManager get(Locale locale) {
		return MessageManager.getManager(
				"com.raqsoft.ide.dfx.etl.funcMessage", locale);
	}

}
