package com.scudata.resources;

import java.util.Locale;

import com.scudata.common.MessageManager;

public class ManageMsg {

	private ManageMsg() {}

	public static MessageManager get() {
		return MessageManager.getManager("com.scudata.resources.manage");
	}

	public static MessageManager get(Locale locale) {
		return MessageManager.getManager("com.scudata.resources.manage", locale);
	}

}
