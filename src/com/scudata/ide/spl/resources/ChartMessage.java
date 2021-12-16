package com.scudata.ide.spl.resources;

import java.util.*;

import com.scudata.common.*;

public class ChartMessage {

	private ChartMessage() {}

	public static MessageManager get() {
		return get(Locale.getDefault());
	}

	public static MessageManager get( Locale locale ) {
		return MessageManager.getManager( "com.scudata.ide.spl.resources.chart", locale );
	}

}
