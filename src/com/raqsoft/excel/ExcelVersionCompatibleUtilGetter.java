package com.raqsoft.excel;

/**
 * 获取Poi5.0.0或Poi3.17版本的导出工具实现类
 * 类的名称一致，但根据版本不同，存放于不同的包中。为了区分开引入，分别放到了poi-5.0.0.jar中和poi-3.17.jar中。
 * */
public class ExcelVersionCompatibleUtilGetter {
	private static ExcelVersionCompatibleUtilInterface e = null;
	public static ExcelVersionCompatibleUtilInterface getInstance(){
		if(e == null)
			try {
				e = (ExcelVersionCompatibleUtilInterface) Class.forName("com.raqsoft.excel.ExcelVersionCompatibleUtil").newInstance();
				//e = new ExcelVersionCompatibleUtil();
			} catch (Exception e1) {
				e1.printStackTrace();
//				try {
//					e = (ExcelVersionCompatibleUtilInterface) Class.forName("com.raqsoft.excel.ExcelVersionCompatibleUtil").newInstance();
//				} catch (InstantiationException e2) {
//					e2.printStackTrace();
//				} catch (IllegalAccessException e2) {
//					e2.printStackTrace();
//				} catch (ClassNotFoundException e2) {
//					e2.printStackTrace();
//				}
			}
		return e;
	}
}
