package com.raqsoft.lib.sap.function;


import com.sap.conn.jco.ext.JCoSessionReference;

import java.util.concurrent.atomic.AtomicInteger;

public class RfcSessionReference implements JCoSessionReference {
	static AtomicInteger atomicInt = new AtomicInteger(0);
	private String id = "session-" + String.valueOf(atomicInt.addAndGet(1));;

	public void contextFinished() {
	}

	public void contextStarted() {
	}

	public String getID() {
		return id;
	}

}