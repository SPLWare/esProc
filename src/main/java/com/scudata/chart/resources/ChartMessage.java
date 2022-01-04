package com.scudata.chart.resources;

import java.util.Locale;

import com.scudata.common.*;

public class ChartMessage {

	private ChartMessage() {
	}

	public static MessageManager get() {
	  return get(Locale.getDefault());
	}

	public static MessageManager get(Locale locale) {
		return MessageManager.getManager(
				"com.scudata.chart.resources.chartMessage", locale);
	}

}
