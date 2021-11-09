package com.raqsoft.ide.dfx.resources;

import com.raqsoft.common.*;
import java.util.*;

public class ChartMessage {

	private ChartMessage() {}

	public static MessageManager get() {
		return get(Locale.getDefault());
	}

	public static MessageManager get( Locale locale ) {
		return MessageManager.getManager( "com.raqsoft.ide.dfx.resources.chart", locale );
	}

}
