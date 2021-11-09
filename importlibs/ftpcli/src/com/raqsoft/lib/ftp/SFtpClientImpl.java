package com.raqsoft.lib.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.raqsoft.common.Logger;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.IResource;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;

public class SFtpClientImpl extends Table implements IResource {
//	package com.raqsoft.lib.ftp;
//	public class FtpClient implements IResource
//	{
//		public FtpClient(Context, String url, int mode);
//		public FtpClient(Context ctx, String url, int port, int mode);	//注意连接成功后要ctx.addResource,mode=0表示主动，1表示被动
//		public boolean login(String user, String pwd);		//返回是否成功，如果有更详细的错误代码，可返回int之类
//		public void changeRemoteDir(String dir);
//		public void put(String remoteFileName, InputStream in);
//		public void get(String remoteFileName, FileObject localFile);
//		public void close();
//	}
//
//	package com.raqsoft.lib.ftp.function;
	/*函数
	ftp_client(url:port, user, pwd) 返回FtpClient, port可省略，@d被动模式
	ftp_cd(client, path)
	ftp_put(client, remoteFileName, localFileName或FileObject)
	ftp_get(client, remoteFileName, localFileName或FileObject)
	ftp_close(client);

	*/
    private Session session = null;  
    private Channel channel = null;
    private Context ctx = null;
    private String url = null;
    private int port = 0;
    private int mode = 0;
    private String ftpPath = "/";
    private ChannelSftp chSftp = null;  
	public SFtpClientImpl(Context ctx, String url, int port, int mode) throws Exception {
        this.ctx = ctx;
        this.url = url;
        this.port = port;
        this.mode = mode;
	}
	
	public boolean login(String user, String password) throws Exception {
		//ftp.logout();
        JSch jsch = new JSch();  
        session = jsch.getSession(user, url, port);  
        session.setPassword(password);
        session.setTimeout(100000);  
        Properties config = new Properties();  
        config.put("StrictHostKeyChecking", "no");  
        session.setConfig(config);  
        session.connect();  
  
        channel = session.openChannel("sftp");  
        channel.connect();  
        chSftp = (ChannelSftp) channel;  
		Logger.debug("login : " + true);
		return true;
	}
	
	public boolean changeRemoteDir(String dir) throws Exception {
		ftpPath = dir;
		return true;		
	}
	
	public boolean put(String remoteFileName, String localPath) throws Exception {
		Logger.debug("put ["+remoteFileName+"] begin");

        ChannelSftp chSftp = (ChannelSftp) channel;  
        
        String ftpFilePath = ftpPath + "/" + remoteFileName;  
        
        try {  
            chSftp.put(localPath,ftpFilePath);
    		Logger.debug("put ["+remoteFileName+"] success");
            return true;
        } catch (Exception e) {  
            e.printStackTrace();  
    		Logger.debug("put ["+remoteFileName+"] failed");
            return false;
        }  
	}
	
	
	public Sequence dirList(String path, ArrayList<String> patterns,boolean onlyDir, boolean fullPath) {
		Sequence s = new Sequence();
//		for (int i=0; i<paths.size(); i++) {
//			if (!paths.get(i).startsWith("/")) {
//				paths.set(i, dir + paths.get(i));
//			}
//			paths.set(i, getRegPath(paths.get(i)));
//		}
//		for (int i=0; i<files.size(); i++) {
//			for (int j=0; j<paths.size(); j++) {
//				if (files.get(i).matches(paths.get(j))) {
//					if (onlyDir && isDir.get(i)) {
//						s.add(fullPath?files.get(i):files.get(i).substring(dir.length()));
//						break;
//					}
//				}
//			}
//		}
		return s;
	}

	public boolean get(String remoteFileName, String localPath) throws Exception {
		Logger.debug("get ["+remoteFileName+"] begin");

        
        String ftpFilePath = ftpPath + "/" + remoteFileName;  
        
        try {  
            chSftp.get(ftpFilePath, localPath);
    		Logger.debug("get ["+remoteFileName+"] success");
            return true;
        } catch (Exception e) {  
            e.printStackTrace();  
    		Logger.debug("get ["+remoteFileName+"] failed");
            return false;
        }  
	}

	@Override
	public void close() {
		try {
            chSftp.quit();  
            channel.disconnect();  
            session.disconnect();  
			ctx.removeResource(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]) {
		try {
			//FtpUtil.downloadSftpFile("123.57.218.190", "root", "Carrygame888", 22, "/var/log/", "d:/test/ftp1.txt", "secure");

			SFtpClientImpl fc = new SFtpClientImpl(null, "123.57.218.190", 22, 1);
			fc.login("root", "Carrygame888");
			fc.changeRemoteDir("/");

			//fc.close();

//			FileInputStream fis = new FileInputStream("d:/aa1.txt");
//			fc.put("a2.txt", fis);
//			fis.close();

			//fc.get("/var/log/secure", "d:/ftp/secure");
			fc.put("/var/secure", "d:/ftp/secure");
			fc.close();
			//fc.put("aa.txt", FileUtil.);
			
		} catch (Exception e) {
			Logger.error("", e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
