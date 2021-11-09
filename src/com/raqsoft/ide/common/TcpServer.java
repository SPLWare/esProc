package com.raqsoft.ide.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * To prevent starting multiple IDEs. Use TcpServer to check whether the IDE is
 * started.
 *
 */
public class TcpServer extends Thread {
	/**
	 * IDE frame
	 */
	private IAppFrame frame;

	/**
	 * Server port
	 */
	private int port;

	/**
	 * Constructor
	 * 
	 * @param port
	 *            Server port
	 * @param frame
	 *            IDE frame
	 */
	public TcpServer(int port, IAppFrame frame) {
		this.port = port;
		this.frame = frame;
	}

	/**
	 * Server run
	 */
	public void run() {
		ServerSocket ss = null;
		try {
			String str = "127.0.0.1";
			String[] ipStr = str.split("\\.");
			byte[] ipBuf = new byte[4];
			for (int i = 0; i < 4; i++) {
				ipBuf[i] = (byte) (Integer.parseInt(ipStr[i]) & 0xFF);
			}
			InetAddress add = InetAddress.getByAddress(ipBuf);
			ss = new ServerSocket(port, 10, add);
			while (true) {
				try {
					Socket s = ss.accept();
					InputStream is = s.getInputStream();
					byte[] buffer = new byte[1024];
					int len = is.read(buffer);
					String file = new String(buffer, 0, len);

					if (file.equals("GetWindowTitle")) {
						OutputStream os = s.getOutputStream();
						String wTitle = ((JFrame) frame).getTitle();
						os.write(wTitle.getBytes());
					} else {
						if (file.startsWith("\"")) {
							file = file.substring(1, file.length() - 1);
						}
						final String sfile = file;
						SwingUtilities.invokeLater(new Thread() {
							public void run() {
								try {
									frame.openSheetFile(sfile);
								} catch (Exception e) {
									GM.showException(e);
								}
							}
						});
					}
					s.close();
				} catch (Throwable x) {
				}
			}
		} catch (Exception e) {
			final String error = e.getMessage();
			SwingUtilities.invokeLater(new Thread() {
				public void run() {
					JOptionPane.showMessageDialog(null, "Socket port: " + port
							+ " creation failed: " + error);
					System.exit(0);
				}
			});
		} finally {
			if (ss != null)
				try {
					ss.close();
				} catch (IOException e) {
				}
		}
	}

}
