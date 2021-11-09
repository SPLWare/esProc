package com.raqsoft.server.unit;

import java.util.List;

import com.raqsoft.app.common.AppUtil;
import com.raqsoft.app.common.Section;
import com.raqsoft.common.StringUtils;
import com.raqsoft.parallel.UnitClient;
import com.raqsoft.parallel.UnitContext;

/**
 * 通过命令远程关闭服务器
 * 
 * @author Joancy
 *
 */
public class ShutdownUnitServer {
	static String[] allHosts;
	static{
		allHosts = AppUtil.getLocalIps();
	}

	/**
	 * 从配置问读取所有分机信息，然后关闭所有分机
	 * @return 正确关机返回true，否则返回false
	 * @throws Exception
	 */
	public static boolean autoClose() throws Exception{
		String home = System.getProperty("start.home");

		if (!StringUtils.isValidString( home )) {
			throw new Exception("start.home is not specified!");
		}
		List<UnitContext.UnitInfo> hosts = UnitContext.listNodes();
		if(hosts==null || hosts.isEmpty()){
			System.out.println("No node server found under: "+home);
			return false;
		}
		for (int i = 0; i < hosts.size(); i++) {
			UnitContext.UnitInfo ui = hosts.get(i);
			String host = ui.getHost();
			if(!(AppUtil.isLocalIP(host))) continue;
			int port = ui.getPort();
			if (close(host, port)) {
			}
		}

		return true;
	}

	/**
	 * 关闭指定的服务器
	 * @param host 服务器IP
	 * @param port 端口号
	 * @return 成功关机返回true，否则返回false
	 */
	public static boolean close(String host, int port){
		if(!StringUtils.isValidString(host)){
			host = UnitContext.getDefaultHost();
		}
		UnitClient uc = new UnitClient(host,port);
		if(uc.isAlive()){
			uc.shutDown();
			System.out.println(uc+" is shut downed.");
			return true;
		}else{
			System.out.println(uc+" is not alive.");
			return false;
		}
	}
	
	public static void main(String[] args) {
		String host = null;
		int port = 0;
		String arg;
		if (args.length == 1) {
			arg = args[0].trim();
			if (arg.trim().indexOf(" ") > 0) {
				Section st = new Section(arg, ' ');
				args = st.toStringArray();
			}
		}

		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				arg = args[i].toLowerCase();
				// System.err.println("arg "+i+"="+arg);
				if (arg.equals("com.raqsoft.parallel.shutdownunitserver")) { // 用bat打开的文件，类名本身会是参数
					continue;
				}

				if (host == null) {
					host = arg;
				} else if (port == 0) {
					port = Integer.parseInt(arg);
				} else {
					break;
				}
			}
		}
		
		try {
			if (host == null) {
				autoClose();
				Thread.sleep(3000);
			}else{
				if(!close(host,port)){
					System.out.println(host+":"+port+" does not on running.");
				}
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		System.exit(0);
	}
	

}