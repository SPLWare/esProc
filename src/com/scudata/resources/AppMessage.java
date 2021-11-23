package com.scudata.resources;

import java.util.Locale;

import com.scudata.common.MessageManager;

/**
 * 应用相关的资源类
 *
 */
public class AppMessage {

	private AppMessage() {
	}

	public static MessageManager get() {
		return MessageManager.getManager("com.raqsoft.resources.appMessage");
	}

	public static MessageManager get(Locale locale) {
		return MessageManager.getManager("com.raqsoft.resources.appMessage",
				locale);
	}

}
