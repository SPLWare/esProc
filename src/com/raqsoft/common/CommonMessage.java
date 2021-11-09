package com.raqsoft.common;

import java.util.Locale;

public class CommonMessage {

	private CommonMessage() {}

	public static MessageManager get() {
		return MessageManager.getManager( "com.raqsoft.common.resources.commonMessage" );
	}

	public static MessageManager get( Locale locale ) {
		return MessageManager.getManager( "com.raqsoft.common.resources.commonMessage", locale );
	}

}
