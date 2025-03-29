package com.scudata.ide.spl;

import java.io.InputStream;
import java.util.Scanner;

/**
 * 等待线程，用于接受Process返回的信息
 */
public class ProcessWatchThread extends Thread {
	InputStream in;
	boolean over;

	public ProcessWatchThread(InputStream in) {
		this.in = in;
		over = false;
	}

	public void run() {
		if (in == null)
			return;
		Scanner br = null;
		try {
			br = new Scanner(in);
			while (true) {
				if (in == null || over)
					break;
				while (br.hasNextLine()) {
					String tempStream = br.nextLine();
					if (tempStream.trim() == null
							|| tempStream.trim().equals(""))
						continue;
					outputLine(tempStream);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				br.close();
			}
		}
	}

	protected void outputLine(String line) {
		System.out.println(line);
	}

	public void setOver(boolean over) {
		this.over = over;
	}
}
