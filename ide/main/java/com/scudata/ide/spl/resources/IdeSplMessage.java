package com.scudata.ide.spl.resources;

import java.util.Locale;

import com.scudata.common.MessageManager;

/**
 * 集算器资源
 *
 */
public class IdeSplMessage {

	/**
	 * 私有构造函数
	 */
	private IdeSplMessage() {
	}

	/**
	 * 获取资源管理器对象
	 * 
	 * @return
	 */
	public static MessageManager get() {
		return get(Locale.getDefault());
	}

	/**
	 * 获取指定区域的资源管理器对象
	 * 
	 * @param locale 区域
	 * @return
	 */
	public static MessageManager get(Locale locale) {
		return MessageManager.getManager("com.scudata.ide.spl.resources.ideSplMessage", locale);
	}

}
