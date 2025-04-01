package com.scudata.ide.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.scudata.common.StringUtils;

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
	private String file;

	public static String GETWINDOWTITLE = "GetWindowTitle";
	public static String ACTIVATE = "ACTIVATE";
	public static String LOCALHOST = "127.0.0.1";

	/**
	 * Constructor
	 * 
	 * @param port
	 *            Server port
	 * @param frame
	 *            IDE frame
	 */
	public TcpServer(int port, IAppFrame frame, String file) {
		this(port, frame);
		this.file = file;
	}

	public TcpServer(int port, IAppFrame frame) {
		this.port = port;
		this.frame = frame;
	}

	/**
	 * 检查一下根据配置的端口port，有没有已经启动的实例
	 * 
	 * @param port
	 * @return 有了则返回true
	 */
	public static boolean checkExistInstance(int port) {
		return ask(LOCALHOST, GETWINDOWTITLE, port);
	}

	private boolean ask(String host, String cmd) {
		return ask(host, cmd, port);
	}

	private static boolean ask(String host, String cmd, int port) {
		int timeout = 2000;
		Socket s = new Socket();
		try {
			InetSocketAddress isa = new InetSocketAddress(host, port);
			s.connect(isa, timeout);
			OutputStream os = s.getOutputStream();
			os.write(cmd.getBytes());
			InputStream is = s.getInputStream();
			byte[] buffer = new byte[1024];
			int len = is.read(buffer);
			String res = new String(buffer, 0, len);
			if (StringUtils.isValidString(res)) {
				return true;
			}
		} catch (Exception x) {
		} finally {
			try {
				s.close();
			} catch (IOException e) {
			}
		}
		return false;
	}

	/**
	 * Server run
	 */
	public void run() {
		ServerSocket ss = null;
		try {
			boolean isExist = checkExistInstance(port);
			if (isExist) {
				ask(LOCALHOST, ACTIVATE);
				if (StringUtils.isValidString(file)) {
					ask(LOCALHOST, file);
				}
				exit();
			}
			String[] ipStr = LOCALHOST.split("\\.");
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
					if (file.equals(GETWINDOWTITLE)) {
						OutputStream os = s.getOutputStream();
						String wTitle = ((JFrame) frame).getTitle();
						os.write(wTitle.getBytes());
					} else if (file.equals(ACTIVATE)) {
						((JFrame) frame).toFront();
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
									GM.showException(GV.appFrame, e);
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
					GM.messageDialog(null, "Socket port: " + port
							+ " creation failed: " + error);
					exit();
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

	protected void exit() {
		System.exit(0);
	}
}
