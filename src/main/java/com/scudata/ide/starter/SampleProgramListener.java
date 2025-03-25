package com.scudata.ide.starter;

import java.awt.event.ActionEvent;

import com.scudata.ide.common.ConfigMenuAction;
import com.scudata.ide.common.dialog.DialogDemoFiles;

/**
 * 打开样例程序
 * 
 * @author wunan
 *
 */
public class SampleProgramListener extends ConfigMenuAction {

	public void actionPerformed(ActionEvent arg0) {
		new DialogDemoFiles().setVisible(true);
	}
}