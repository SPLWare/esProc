package com.scudata.resources;

import java.util.Locale;

import com.scudata.common.MessageManager;

public class EngineMessage {

	private EngineMessage() {}

	public static MessageManager get() {
		return MessageManager.getManager("com.raqsoft.resources.engineMessage");
	}

	public static MessageManager get(Locale locale) {
		return MessageManager.getManager("com.raqsoft.resources.engineMessage", locale);
	}

}
