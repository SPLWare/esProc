package com.scudata.resources;

import java.util.Locale;

import com.scudata.common.MessageManager;

public class ParseMessage {

	private ParseMessage() {}

	public static MessageManager get() {
		return MessageManager.getManager("com.raqsoft.resources.parseMessage");
	}

	public static MessageManager get(Locale locale) {
		return MessageManager.getManager("com.raqsoft.resources.parseMessage", locale);
	}

}
