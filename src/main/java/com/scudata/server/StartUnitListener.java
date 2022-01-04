package com.scudata.server;

//import java.io.InputStream;

public interface StartUnitListener {
//	public void unitStarted(int port, InputStream[] is);
//	public void unitStopped(int port);
	public void serverStarted(int atPort);
	public void serverStartFail();
	public void doStop();
}
