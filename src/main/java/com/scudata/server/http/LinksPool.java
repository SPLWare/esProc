package com.scudata.server.http;

public class LinksPool {
	
	private static int maxLinks = 20;
	private static int currLinks = 0;
	
	public synchronized static void addLink() {
		currLinks++;
	}

	public synchronized static void removeLink() {
		currLinks--;
	}

	public synchronized static int countLinks() {
		return currLinks;
	}
	
	public static void setMaxLinks( int maxLink ) {
		maxLinks = maxLink;
	}
	
	public synchronized static boolean canCreateLink() {
		return currLinks < maxLinks;
	}

}
