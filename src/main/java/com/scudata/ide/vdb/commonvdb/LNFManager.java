package com.scudata.ide.vdb.commonvdb;

import java.util.Vector;

import javax.swing.UIManager;

import com.scudata.ide.vdb.config.ConfigOptions;

/**
 * 界面样式
 * 
 * @author wunan
 *
 */
public class LNFManager {
	public static final byte LNF_SYSTEM = 1;
	public static final byte LNF_WINDOWS = 2;
	public static final byte LNF_NIMBUS = 3;

	public static Vector<Object> listLNFCode() {
		Vector<Object> list = new Vector<Object>();
		if (isNimbusEnabled())
			list.add(new Byte(LNF_NIMBUS));
		list.add(new Byte(LNF_WINDOWS));
		list.add(new Byte(LNF_SYSTEM));
		return list;
	}

	public static Vector<String> listLNFDisp() {
		Vector<String> list = new Vector<String>();
		if (isNimbusEnabled())
			list.add("Nimbus");
		list.add("Windows");
		list.add("System");
		return list;
	}

	public static Byte getValidLookAndFeel(Byte lnf) {
		if (lnf != null) {
			switch (lnf.byteValue()) {
			case LNF_SYSTEM:
			case LNF_WINDOWS:
				return lnf;
			}
		}
		return isNimbusEnabled() ? new Byte(LNF_NIMBUS) : new Byte(LNF_SYSTEM);
	}

	private static boolean isNimbusEnabled() {
		try {
			return Class.forName("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel") != null;
		} catch (Exception e) {
			return false;
		}
	}

	public static String getLookAndFeelName() {
		switch (ConfigOptions.iLookAndFeel.byteValue()) {
		case LNF_WINDOWS:
			return "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
		case LNF_SYSTEM:
			return UIManager.getSystemLookAndFeelClassName();
		default:
			if (isNimbusEnabled())
				return "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";
			else
				return UIManager.getSystemLookAndFeelClassName();
		}
	}
}
