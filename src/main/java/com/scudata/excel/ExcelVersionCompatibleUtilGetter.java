package com.scudata.excel;

import com.scudata.common.Logger;

/**
 * 获取Poi5.0.0或Poi3.17版本的导出工具实现类
 * 类的名称一致，但根据版本不同，存放于不同的包中。为了区分开引入，分别放到了poi-5.0.0.jar中和poi-3.17.jar中。
 * */
public class ExcelVersionCompatibleUtilGetter {
	private static ExcelVersionCompatibleUtilInterface e = null;
	public static String version5 = "com.scudata.excel.ExcelVersionCompatibleUtil5";
	public static String version3 = "com.raqsoft.report.util.ExcelVersionCompatibleUtil3";
	public static String version = null;
	
	public static ExcelVersionCompatibleUtilInterface getInstance(){
		if(version == null || version.length() == 0) {
			loadCompatibleUtilClassName(version5);
		}
		return getInstance(version);
	}
	
	public static void loadCompatibleUtilClassName(String cls) {
		version = cls;
		try {
			e = (ExcelVersionCompatibleUtilInterface) Class.forName(version).newInstance();
		} catch (Exception e1) {
			Logger.debug(e1.getMessage());
			try {
				e = (ExcelVersionCompatibleUtilInterface) Class.forName(version5).newInstance();
			} catch (InstantiationException e2) {
				e2.printStackTrace();
			} catch (IllegalAccessException e2) {
				e2.printStackTrace();
			} catch (ClassNotFoundException e2) {
				e2.printStackTrace();
			}
		}
	}
	
	public static ExcelVersionCompatibleUtilInterface getInstance(String cls){
		if(e == null) {
			loadCompatibleUtilClassName(cls);
		}
		return e;
	}
}
