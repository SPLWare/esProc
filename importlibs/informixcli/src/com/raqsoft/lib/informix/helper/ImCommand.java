package com.raqsoft.lib.informix.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

//uage: ImCommand.exeCmd("sh /tmp/crt_ext.sh lineitem");  
public class ImCommand extends Thread {
	private String[] m_commandStr;
	private static String OS = System.getProperty("os.name").toLowerCase();

	public ImCommand(String[] cmd) {
		m_commandStr = cmd;
	}

	public static void exeCmd(String[] commandStr) {
		BufferedReader br = null;
		try {
			Process p = Runtime.getRuntime().exec(commandStr);
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}
			System.out.println(sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static Process exeShellCmd(String[] commandStr) {
		try {
			return Runtime.getRuntime().exec(commandStr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String readFileContent(File file) {
		StringBuilder result = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));// 构造一个BufferedReader类来读取文件
			String s = null;
			while ((s = br.readLine()) != null) {// 使用readLine方法，一次读一行
				result.append(System.lineSeparator() + s);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result.toString();
	}

	public static void main(String[] args) {
		String[] commandStr = new String[] { "ping", "www.taobao.com" };

		ImCommand.exeCmd(commandStr);
	}

	public void run() {
		exeCmd(m_commandStr);
	}

	// kill -9 $(ps -ef|grep java|gawk '$0 !~/grep/ {print $2}' |tr -s '\n' ' ')
	public static void killShell(String dbname, String pipeName) {
		String cmd[] = new String[] { "sh", "/informix/vsettan/clearsplit.sh", pipeName, dbname };
		exeShellCmd(cmd);
	}

	public static boolean isLinuxOS() {
		return OS.equalsIgnoreCase("Linux");
	}
}