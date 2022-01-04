package com.scudata.ide.common;

import java.util.Vector;

import com.scudata.common.Types;
import com.scudata.ide.common.resources.IdeCommonMessage;

/**
 * Code value and display value mapping for data type
 */
public class TypesEx extends Types {
	/**
	 * List of code values for data types
	 * 
	 * @param showCursor
	 *            Whether to include the cursor type
	 * @return
	 */
	public static Vector<Byte> listCodeTypes(boolean showCursor) {
		Vector<Byte> v = new Vector<Byte>();
		v.add(new Byte(DT_INT));
		v.add(new Byte(DT_LONG));
		v.add(new Byte(DT_SHORT));
		v.add(new Byte(DT_BIGINT));
		v.add(new Byte(DT_FLOAT));
		v.add(new Byte(DT_DOUBLE));
		v.add(new Byte(DT_DECIMAL));
		v.add(new Byte(DT_DATE));
		v.add(new Byte(DT_TIME));
		v.add(new Byte(DT_DATETIME));
		v.add(new Byte(DT_BOOLEAN));
		v.add(new Byte(DT_STRING));

		v.add(new Byte(DT_INT_SERIES));
		v.add(new Byte(DT_LONG_SERIES));
		v.add(new Byte(DT_SHORT_SERIES));
		v.add(new Byte(DT_BIGINT_SERIES));
		v.add(new Byte(DT_FLOAT_SERIES));
		v.add(new Byte(DT_DOUBLE_SERIES));
		v.add(new Byte(DT_DECIMAL_SERIES));
		v.add(new Byte(DT_DATE_SERIES));
		v.add(new Byte(DT_TIME_SERIES));
		v.add(new Byte(DT_DATETIME_SERIES));
		v.add(new Byte(DT_STRING_SERIES));
		v.add(new Byte(DT_DEFAULT));
		v.add(new Byte(DT_AUTOINCREMENT));
		if (showCursor)
			v.add(new Byte(DT_CURSOR));
		return v;
	}

	/**
	 * List of display values for data types
	 * 
	 * @param showCursor
	 *            Whether to include the cursor type
	 * @return
	 */
	public static Vector<String> listDispTypes(boolean showCursor) {
		Vector<String> v = new Vector<String>();
		v.add(IdeCommonMessage.get().getMessage("type.int"));
		v.add(IdeCommonMessage.get().getMessage("type.long"));
		v.add(IdeCommonMessage.get().getMessage("type.short"));
		v.add(IdeCommonMessage.get().getMessage("type.bigint"));
		v.add(IdeCommonMessage.get().getMessage("type.float"));
		v.add(IdeCommonMessage.get().getMessage("type.double"));
		v.add(IdeCommonMessage.get().getMessage("type.decimal"));
		v.add(IdeCommonMessage.get().getMessage("type.date"));
		v.add(IdeCommonMessage.get().getMessage("type.time"));
		v.add(IdeCommonMessage.get().getMessage("type.datetime"));
		v.add(IdeCommonMessage.get().getMessage("type.boolean"));
		v.add(IdeCommonMessage.get().getMessage("type.string"));

		v.add(IdeCommonMessage.get().getMessage("type.intarr"));
		v.add(IdeCommonMessage.get().getMessage("type.longarr"));
		v.add(IdeCommonMessage.get().getMessage("type.shortarr"));
		v.add(IdeCommonMessage.get().getMessage("type.bigintarr"));
		v.add(IdeCommonMessage.get().getMessage("type.floatarr"));
		v.add(IdeCommonMessage.get().getMessage("type.doublearr"));
		v.add(IdeCommonMessage.get().getMessage("type.decimalarr"));
		v.add(IdeCommonMessage.get().getMessage("type.datearr"));
		v.add(IdeCommonMessage.get().getMessage("type.timearr"));
		v.add(IdeCommonMessage.get().getMessage("type.datetimearr"));
		v.add(IdeCommonMessage.get().getMessage("type.stringarr"));
		v.add(IdeCommonMessage.get().getMessage("type.default"));
		v.add(IdeCommonMessage.get().getMessage("type.auto"));
		if (showCursor)
			v.add(IdeCommonMessage.get().getMessage("type.cursor"));
		return v;
	}

	/**
	 * Get the display value according to the code value of the data type
	 * 
	 * @param type
	 *            Data type
	 * @return
	 */
	public static String getDataTypeName(byte type) {
		Vector<Byte> code = listCodeTypes(true);
		Vector<String> disp = listDispTypes(true);
		for (int i = 0; i < code.size(); i++) {
			Byte bType = (Byte) code.get(i);
			if (bType.byteValue() == type) {
				return (String) disp.get(i);
			}
		}
		return "Error";
	}

	/**
	 * Get the code value according to the displayed value of the data type
	 * 
	 * @param dispValue
	 *            Display value
	 * @return
	 */
	public static byte getDataTypeCode(String dispValue) {
		Vector<Byte> code = listCodeTypes(true);
		Vector<String> disp = listDispTypes(true);
		for (int i = 0; i < disp.size(); i++) {
			String sType = (String) disp.get(i);
			if (sType.equals(dispValue)) {
				return ((Byte) code.get(i)).byteValue();
			}
		}
		return 0;
	}

}
