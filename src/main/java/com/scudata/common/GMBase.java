package com.scudata.common;

import com.scudata.app.config.ConfigUtil;

/**
 * Global Method
 *
 */
public class GMBase {
	/**
	 * Get the absolute path relative to start.home.
	 * 
	 * @param path
	 *            Absolute path or relative path
	 * @return Return the absolute path of path
	 */
	public static String getAbsolutePath(String path) {
		String home = System.getProperty("start.home");
		return ConfigUtil.getPath(home, path);
	}

	
	
	
	
	
	
	
	
	
	
	
}