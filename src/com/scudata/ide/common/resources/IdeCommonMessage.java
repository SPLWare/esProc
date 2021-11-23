package com.scudata.ide.common.resources;

import java.util.Locale;

import com.scudata.common.*;

public class IdeCommonMessage {

	private IdeCommonMessage() {
	}

	public static MessageManager get() {
	  return get(Locale.getDefault());
	}

	public static MessageManager get(Locale locale) {
		return MessageManager.getManager(
				"com.scudata.ide.common.resources.ideCommonMessage", locale);
	}

}
