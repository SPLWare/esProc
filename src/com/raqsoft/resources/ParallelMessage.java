package com.raqsoft.resources;

import java.util.Locale;
import com.raqsoft.common.MessageManager;

/**
 * 并行相关代码的资源类
 * @author Joancy
 *
 */
public class ParallelMessage {

	private ParallelMessage() {}

	public static MessageManager get() {
		return MessageManager.getManager("com.raqsoft.resources.parallelMessage");
	}

	public static MessageManager get(Locale locale) {
		return MessageManager.getManager("com.raqsoft.resources.parallelMessage", locale);
	}

}
