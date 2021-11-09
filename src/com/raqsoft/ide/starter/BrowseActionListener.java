package com.raqsoft.ide.starter;

import java.awt.event.ActionEvent;

import com.raqsoft.common.StringUtils;
import com.raqsoft.ide.common.ConfigMenuAction;
import com.raqsoft.ide.common.GM;

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