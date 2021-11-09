package com.raqsoft.ide.starter;

import java.awt.event.ActionEvent;

import com.raqsoft.app.config.ConfigUtil;
import com.raqsoft.ide.common.ConfigMenuAction;
import com.raqsoft.ide.common.GM;

public class OpenActionListener extends ConfigMenuAction {

	public void actionPerformed(ActionEvent event) {
		try {
			String path = this.getConfigArgument();
			String startHome = System.getProperty("start.home");// 获得系统根目录
			path = ConfigUtil.getPath(startHome, path);
			Runtime.getRuntime().exec("cmd /c \"" + path + "\"");
		} catch (Exception e) {
			GM.showException(e);
		}

	}

}
