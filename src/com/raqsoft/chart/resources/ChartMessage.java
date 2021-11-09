package com.raqsoft.chart.resources;

import java.util.Locale;
import com.raqsoft.common.*;

public class ChartMessage {

	private ChartMessage() {
	}

	public static MessageManager get() {
	  return get(Locale.getDefault());
	}

	public static MessageManager get(Locale locale) {
		return MessageManager.getManager(
				"com.raqsoft.chart.resources.chartMessage", locale);
	}

}
