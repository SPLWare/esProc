package com.scudata.common;

import java.util.Locale;

/**
 * Global Constants
 * 
 */
public class GCBase {
	/**
	 * Configuration file path
	 */
	public final static String PATH_CONFIG = "config";
	/**
	 * Chinese
	 */
	public static final byte ASIAN_CHINESE = 0;
	/**
	 * Traditional Chinese
	 */
	public static final byte ASIAN_CHINESE_TRADITIONAL = 1;
	/**
	 * Japanese
	 */
	public static final byte ASIAN_JAPANESE = 2;
	/**
	 * Korea
	 */
	public static final byte ASIAN_KOREA = 3;
	/**
	 * English
	 */
	public static final byte ENGLISH = 4;
	/**
	 * Current language
	 */
	public static byte LANGUAGE = ENGLISH;
	
	/**
	 * Initialize Locale
	 */
	static {
		initLocale();
	}

	/**
	 * Initialize Locale
	 */
	public static void initLocale() {
		Locale local = Locale.getDefault();
		if (local.equals(Locale.PRC) || local.equals(Locale.CHINA)
				|| local.equals(Locale.CHINESE)
				|| local.equals(Locale.SIMPLIFIED_CHINESE)
				|| local.getLanguage().equalsIgnoreCase("zh")) {
			LANGUAGE = ASIAN_CHINESE;
		} else if (local.equals(Locale.TAIWAN)
				|| local.equals(Locale.TRADITIONAL_CHINESE)
				|| local.getLanguage().equalsIgnoreCase("tw")) {
			LANGUAGE = ASIAN_CHINESE_TRADITIONAL;
		} else if (local.equals(Locale.JAPAN) || local.equals(Locale.JAPANESE)) {
			LANGUAGE = ASIAN_JAPANESE;
		} else if (local.equals(Locale.KOREA) || local.equals(Locale.KOREAN)) {
			LANGUAGE = ASIAN_KOREA;
		} else {
			LANGUAGE = ENGLISH;
		}
	}
}
