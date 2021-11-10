package com.raqsoft.lib.webservice.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Binding {
	public String name;
	public PortType port;
	public String style; //
	public String transport;
	public Map<String,BindingOperation> bindingOperations = new HashMap<String,BindingOperation>();
	public ArrayList<String> names = new ArrayList<String>();
	
	//private Array
}
