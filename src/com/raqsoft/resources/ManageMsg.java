package com.raqsoft.resources;

import java.util.Locale;
import com.raqsoft.common.MessageManager;

public class ManageMsg {

	private ManageMsg() {}

	public static MessageManager get() {
		return MessageManager.getManager("com.raqsoft.resources.manage");
	}

	public static MessageManager get(Locale locale) {
		return MessageManager.getManager("com.raqsoft.resources.manage", locale);
	}

}
