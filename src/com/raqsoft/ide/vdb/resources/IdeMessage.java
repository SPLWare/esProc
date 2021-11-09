package com.raqsoft.ide.vdb.resources;

import java.util.Locale;

import com.raqsoft.common.MessageManager;

public class IdeMessage {
	private IdeMessage() {
	}

	public static MessageManager get() {
		return MessageManager.getManager("com.raqsoft.ide.vdb.resources.Ide");
	}

	public static MessageManager get(Locale locale) {
		return MessageManager.getManager("com.raqsoft.ide.vdb.resources.Ide", locale);
	}
}
