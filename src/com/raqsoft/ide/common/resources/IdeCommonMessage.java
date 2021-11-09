package com.raqsoft.ide.common.resources;

import java.util.Locale;
import com.raqsoft.common.*;

public class IdeCommonMessage {

	private IdeCommonMessage() {
	}

	public static MessageManager get() {
//		return MessageManager
//				.getManager("com.raqsoft.ide.common.resources.ideCommonMessage");
	  return get(Locale.getDefault());
	}

	public static MessageManager get(Locale locale) {
		return MessageManager.getManager(
				"com.raqsoft.ide.common.resources.ideCommonMessage", locale);
	}

}
