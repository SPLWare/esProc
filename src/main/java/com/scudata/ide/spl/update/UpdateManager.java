package com.scudata.ide.spl.update;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.scudata.common.StringUtils;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.dialog.DialogCheckUpdate;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.spl.GMSpl;

/**
 * 检查更新管理器
 */
public class UpdateManager {
	/**
	 * 检查更新
	 * 
	 * @param isAuto true自动更新，false手动更新（菜单动作）
	 * @throws Exception
	 */
	public static void checkUpdate(final boolean isAuto) throws Exception {
		if (!canUpdate())
			return;
		String installPath = System.getProperty("start.home");
		if (!StringUtils.isValidString(installPath)) {
			return;
		}
		File updateDir = new File(installPath, "update");
		if (!updateDir.exists())
			return;
		File serverFile = new File(updateDir, "server.txt");
		if (!serverFile.exists())
			return;
		HashMap<String, String> map = readProperties(new FileInputStream(serverFile));
		String downloadUrl = map.get("downloadUrl");
		String localVersion = (String) map.get("version");
		String serverVersion;
		try {
			Class clz = Class.forName("com.scudata.ide.spl.update.GetVersion");
			Method m = clz.getMethod("getVersion");
			Object version = m.invoke(clz.newInstance());
			serverVersion = version == null ? null : (String) version;
		} catch (final Exception e) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					GM.showException(e, true, GMSpl.getLogoImage(true),
							IdeCommonMessage.get().getMessage("updatemanager.premessage"), GV.appFrame);
				}
			});
			return;
		}
		if (serverVersion != null && localVersion != null && serverVersion.compareTo(localVersion) > 0) { // 有新版本
			final String du = downloadUrl;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					dialogUpdate(du);
				}
			});
		} else {
			if (!isAuto) {
				SwingUtilities.invokeLater(new Thread() {
					public void run() {
						// 当前版本已是最新版本！
						JOptionPane.showMessageDialog(GV.appFrame,
								IdeCommonMessage.get().getMessage("update.islatest"));
					}
				});
			}
		}
	}

	/**
	 * 是否可以检查更新
	 * 
	 * @return true可以，false不可以
	 */
	public static boolean canUpdate() {
		try {
			Class clz = Class.forName("com.scudata.ide.spl.update.GetVersion");
			Method m = clz.getMethod("getVersion");
			return m != null;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 读取配置文件
	 * 
	 * @param is 文件输入流
	 * @return
	 * @throws IOException
	 */
	public static HashMap<String, String> readProperties(InputStream is) throws IOException {
		HashMap<String, String> map = new HashMap<String, String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.trim().length() == 0)
					continue;
				int pos = line.indexOf("=");
				if (pos < 0)
					continue;
				String key = line.substring(0, pos).trim();
				String value = line.substring(pos + 1).trim();
				map.put(key, value);
			}
		} finally {
			try {
				is.close();
				br.close();
			} catch (Exception e) {
			}
		}
		return map;
	}

	/**
	 * 检查更新对话框
	 * 
	 * @param downloadUrl 下载地址
	 * @return
	 */
	private static boolean dialogUpdate(String downloadUrl) {
		DialogCheckUpdate dcu = new DialogCheckUpdate(GV.appFrame);
		if (GV.appFrame == null) {
			ImageIcon ii = GM.getLogoImage(true);
			if (ii != null)
				dcu.setIconImage(ii.getImage());
		}
		dcu.setVisible(true);
		int option = dcu.getOption();
		if (option == JOptionPane.OK_OPTION) {
			try {
				GM.browse(downloadUrl);
			} catch (final Throwable t) {
				GM.showException(t);
			}
		}
		return false;
	}

}
