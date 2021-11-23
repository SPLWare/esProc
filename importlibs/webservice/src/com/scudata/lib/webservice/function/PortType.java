package com.scudata.lib.webservice.function;

import java.util.HashMap;
import java.util.Map;

public class PortType {
	public String name;
	public Map<String, Operation> operations = new HashMap<String, Operation>();
}
