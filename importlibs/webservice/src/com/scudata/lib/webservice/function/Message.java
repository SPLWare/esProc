package com.scudata.lib.webservice.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Message {
	public String name;
	public Map<String,Part> parts = new HashMap<String,Part>();
	public ArrayList<String> partNames = new ArrayList<String>();
}
