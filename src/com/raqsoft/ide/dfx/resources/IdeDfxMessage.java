package com.raqsoft.ide.dfx.resources;

import java.util.Locale;

import com.raqsoft.common.MessageManager;

/**
 * 集算器资源
 *
 */
public class IdeDfxMessage {

	/**
	 * 私有构造函数
	 */
	private IdeDfxMessage() {
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
	 * @param locale
	 *            区域
	 * @return
	 */
	public static MessageManager get(Locale locale) {
		return MessageManager.getManager(
				"com.raqsoft.ide.dfx.resources.ideDfxMessage", locale);
	}

}
