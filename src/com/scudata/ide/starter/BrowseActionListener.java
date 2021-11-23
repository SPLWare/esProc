package com.scudata.ide.starter;

import java.awt.event.ActionEvent;

import com.scudata.common.StringUtils;
import com.scudata.ide.common.ConfigMenuAction;
import com.scudata.ide.common.GM;

public class BrowseActionListener extends ConfigMenuAction {

	public void actionPerformed(ActionEvent arg0) {
		String url = this.getConfigArgument();
		if (!StringUtils.isValidString(url)) {
			return;
		}
		try {
			url = url.trim();
			GM.browse(url);
		} catch (Exception e) {
			GM.showException(e);
		}

	}

}