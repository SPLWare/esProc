package com.scudata.ide.vdb.commonvdb;

import java.util.Vector;

import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

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
	public static final byte LNF_DARK = 4;

	public static Vector<Object> listLNFCode() {
		Vector<Object> list = new Vector<Object>();
		if (isNimbusEnabled())
			list.add(new Byte(LNF_NIMBUS));
		list.add(new Byte(LNF_WINDOWS));
		list.add(new Byte(LNF_SYSTEM));
		list.add(new Byte(LNF_DARK));
		return list;
	}

	public static Vector<String> listLNFDisp() {
		Vector<String> list = new Vector<String>();
		if (isNimbusEnabled())
			list.add("Nimbus");
		list.add("Windows");
		list.add("System");
		list.add("Dark");
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
		case LNF_DARK:
			return UIManager.getSystemLookAndFeelClassName();
		case LNF_SYSTEM:
			return UIManager.getSystemLookAndFeelClassName();
		default:
			if (isNimbusEnabled())
				return "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";
			else
				return UIManager.getSystemLookAndFeelClassName();
		}
	}

	public static void applyDarkMode() {
		// Apply dark mode colors to UI components
		UIManager.put("Control", new ColorUIResource(50, 50, 50));
		UIManager.put("ControlText", new ColorUIResource(220, 220, 220));
		UIManager.put("Menu", new ColorUIResource(50, 50, 50));
		UIManager.put("MenuText", new ColorUIResource(220, 220, 220));
		UIManager.put("MenuItem", new ColorUIResource(50, 50, 50));
		UIManager.put("MenuBar", new ColorUIResource(40, 40, 40));
		UIManager.put("Button.background", new ColorUIResource(50, 50, 50));
		UIManager.put("Button.foreground", new ColorUIResource(220, 220, 220));
		UIManager.put("text", new ColorUIResource(220, 220, 220));
		UIManager.put("textText", new ColorUIResource(220, 220, 220));
		UIManager.put("Panel.background", new ColorUIResource(50, 50, 50));
		UIManager.put("Panel.foreground", new ColorUIResource(220, 220, 220));
		UIManager.put("EditorPane.background", new ColorUIResource(40, 40, 40));
		UIManager.put("EditorPane.foreground", new ColorUIResource(220, 220, 220));
		UIManager.put("TextPane.background", new ColorUIResource(40, 40, 40));
		UIManager.put("TextPane.foreground", new ColorUIResource(220, 220, 220));
		UIManager.put("TextArea.background", new ColorUIResource(40, 40, 40));
		UIManager.put("TextArea.foreground", new ColorUIResource(220, 220, 220));
		UIManager.put("TextField.background", new ColorUIResource(60, 60, 60));
		UIManager.put("TextField.foreground", new ColorUIResource(220, 220, 220));
		UIManager.put("Tree.background", new ColorUIResource(50, 50, 50));
		UIManager.put("Tree.foreground", new ColorUIResource(220, 220, 220));
		UIManager.put("Tree.textBackground", new ColorUIResource(50, 50, 50));
		UIManager.put("Table.background", new ColorUIResource(50, 50, 50));
		UIManager.put("Table.foreground", new ColorUIResource(220, 220, 220));
		UIManager.put("Table.gridColor", new ColorUIResource(80, 80, 80));
		UIManager.put("Window.background", new ColorUIResource(50, 50, 50));
		UIManager.put("Window.foreground", new ColorUIResource(220, 220, 220));
		UIManager.put("ComboBox.background", new ColorUIResource(60, 60, 60));
		UIManager.put("ComboBox.foreground", new ColorUIResource(220, 220, 220));
		UIManager.put("OptionPane.background", new ColorUIResource(50, 50, 50));
		UIManager.put("OptionPane.foreground", new ColorUIResource(220, 220, 220));
		UIManager.put("Dialog.background", new ColorUIResource(50, 50, 50));
		UIManager.put("Dialog.foreground", new ColorUIResource(220, 220, 220));
		UIManager.put("List.background", new ColorUIResource(50, 50, 50));
		UIManager.put("List.foreground", new ColorUIResource(220, 220, 220));
		UIManager.put("ScrollPane.background", new ColorUIResource(50, 50, 50));
		UIManager.put("ScrollPane.foreground", new ColorUIResource(220, 220, 220));
		UIManager.put("Separator.background", new ColorUIResource(80, 80, 80));
		UIManager.put("Separator.foreground", new ColorUIResource(80, 80, 80));
	}

	public static void applyLightMode() {
		// Reset to default light mode colors
		try {
			String lnf = getLookAndFeelName();
			UIManager.setLookAndFeel(lnf);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
