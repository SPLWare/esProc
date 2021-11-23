package com.scudata.ide.vdb.config;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.scudata.common.StringUtils;

class ConfigOptionsHandler extends DefaultHandler {
	private StringBuffer buf = new StringBuffer();
	private int version = 1;

	public ConfigOptionsHandler() {
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		buf.setLength(0);
		if (qName.equalsIgnoreCase(ConfigFile.ROOT)) {
			String sVersion = attributes.getValue(ConfigFile.VERSION);
			try {
				version = Integer.parseInt(sVersion);
			} catch (Exception ex) {
			}
			return;
		} else if (qName.equalsIgnoreCase(ConfigFile.OPTION)) {
			String name = attributes.getValue(ConfigFile.NAME);
			String value = attributes.getValue(ConfigFile.VALUE);
			ConfigOptions.options.put(name, value);
		} else if (qName.equalsIgnoreCase(ConfigFile.DIMENSION)) {
			String name = attributes.getValue(ConfigFile.NAME);
			String value = attributes.getValue(ConfigFile.VALUE);
			ConfigOptions.dimensions.put(name, value);
		} else if (qName.equalsIgnoreCase(ConfigFile.CONNECTION)) {
			String name = attributes.getValue(ConfigFile.NAME);
			String value = attributes.getValue(ConfigFile.VALUE);
			if(StringUtils.isValidString(value)){
				ConfigOptions.connections.put(name, value);
			}
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
	}

	// <dateFormat>yyyy-MM-dd</dateFormat>，获取其中的yyyy-MM-dd
	public void characters(char[] ch, int start, int length) throws SAXException {
		buf.append(ch, start, length);
	}

}
