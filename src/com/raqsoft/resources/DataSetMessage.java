package com.raqsoft.resources;

import java.util.Locale;
import com.raqsoft.common.MessageManager;

public class DataSetMessage {

	private DataSetMessage() {}

	public static MessageManager get() {
		return MessageManager.getManager("com.raqsoft.resources.dataSetMessage");
	}

	public static MessageManager get(Locale locale) {
		return MessageManager.getManager("com.raqsoft.resources.dataSetMessage", locale);
	}

}
