package com.scudata.ide.common;

import java.util.Vector;

/**
 * Look and feel manager
 */
public class LookAndFeelManager {
	/**
	 * Office style silver
	 */
	public static final byte LNF_OFFICE_SILVER = 1;
	/**
	 * Office style blue
	 */
	public static final byte LNF_OFFICE_BLUE = 2;

	/**
	 * Look and feel code value list
	 * 
	 * @return
	 */
	public static Vector<Byte> listLNFCode() {
		Vector<Byte> v = new Vector<Byte>();
		v.add(new Byte(LNF_OFFICE_SILVER));
		v.add(new Byte(LNF_OFFICE_BLUE));
		return v;
	}

	/**
	 * Look and feel display value list
	 * 
	 * @return
	 */
	public static Vector<String> listLNFDisp() {
		Vector<String> v = new Vector<String>();
		v.add("Silver");
		v.add("Blue");
		return v;
	}

	/**
	 * Get Look and feel code value.If the wrong code value is configured, the
	 * default setting is used.
	 * 
	 * @param lnf
	 * @return
	 */
	public static byte getValidLookAndFeel(Byte lnf) {
		if (lnf != null) {
			switch (lnf.byteValue()) {
			case LNF_OFFICE_SILVER:
			case LNF_OFFICE_BLUE:
				return lnf;
			}
		}
		return new Byte(LNF_OFFICE_SILVER);
	}

	/**
	 * Get look and feel name
	 * 
	 * @return
	 */
	public static String getLookAndFeelName() {
		switch (ConfigOptions.iLookAndFeel.byteValue()) {
		case LNF_OFFICE_BLUE:
			return "org.pushingpixels.substance.api.skin.SubstanceOfficeBlue2007LookAndFeel";
		default:
			return "org.pushingpixels.substance.api.skin.SubstanceOfficeSilver2007LookAndFeel";
		}
	}
}
