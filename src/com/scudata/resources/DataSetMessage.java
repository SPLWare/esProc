package com.scudata.resources;

import java.util.Locale;

import com.scudata.common.MessageManager;

public class DataSetMessage {

	private DataSetMessage() {}

	public static MessageManager get() {
		return MessageManager.getManager("com.raqsoft.resources.dataSetMessage");
	}

	public static MessageManager get(Locale locale) {
		return MessageManager.getManager("com.raqsoft.resources.dataSetMessage", locale);
	}

}
