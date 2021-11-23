package com.scudata.common;

import java.util.Locale;

public class CommonMessage {

	private CommonMessage() {}

	public static MessageManager get() {
		return MessageManager.getManager( "com.scudata.common.resources.commonMessage" );
	}

	public static MessageManager get( Locale locale ) {
		return MessageManager.getManager( "com.scudata.common.resources.commonMessage", locale );
	}

}
