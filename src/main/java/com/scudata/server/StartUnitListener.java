package com.scudata.server;

public interface StartUnitListener {
	public void serverStarted(int atPort);
	public void serverStartFail();
	public void doStop();
}
