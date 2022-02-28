package com.scudata.lib.ftp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.scudata.common.Logger;
import com.scudata.dm.Context;
import com.scudata.dm.IResource;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;

public class SFtpClientImpl extends Table implements IResource {
//	package com.scudata.lib.ftp;
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
//	package com.scudata.lib.ftp.function;
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
    ChannelSftp chSftp = null;  
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
		return this.put(remoteFileName, localPath, 1);
	}
	/**
	 * 
	 * @param remoteFileName
	 * @param localPath
	 * @param mode 1 default;  2 覆盖;   3追加
	 * @return
	 * @throws Exception
	 */
	public boolean put(String remoteFileName, String localPath, int mode) throws Exception {
		Logger.debug("put ["+remoteFileName+"] begin");
        String ftpFilePath = ftpPath + "/" + remoteFileName;  
        boolean exists = false;
        try {  
        	Vector fs = chSftp.ls(remoteFileName);
        	if (fs.size() == 1 && !((LsEntry)fs.get(0)).getAttrs().isDir()) exists = true;
        } catch (Exception e) {  
        }  
        
        try { 
        	if (mode == 2) {
    			chSftp.put(localPath,remoteFileName,ChannelSftp.OVERWRITE);
        	} else if (mode == 3) {
    			chSftp.put(localPath,remoteFileName,ChannelSftp.APPEND);
        	} else {
        		if (exists) {
	        		Logger.debug("put ["+remoteFileName+"] failed, file has exist");
	        		return false;
        		} else {
        			chSftp.put(localPath,remoteFileName);
        		}
        	}
        	
    		Logger.debug("put ["+remoteFileName+"] success");
            return true;
        } catch (Exception e) {  
            e.printStackTrace();  
    		Logger.debug("put ["+remoteFileName+"] failed");
            return false;
        }  
	}
	
	
	public Sequence mkdir(ArrayList<String> dirs) throws Exception {
		Table result = new Table(new String[]{"folder","result","cause"});
		for (int i=0; i<dirs.size(); i++) {
			
			String diri = dirs.get(i);
			boolean r = true;
			String msg = "mkdir success : " + diri;
			try {
				chSftp.mkdir(diri);
			} catch (Exception e) {
				r = false;
				msg = e.getMessage();
				e.printStackTrace();
			}
			
			Record r2 = result.insert(0);
			r2.set("folder",dirs.get(i));
			r2.set("result",r?"success":"fail");
			r2.set("cause",msg);
		}
		return result;
	}

	
	public Sequence deldir(ArrayList<String> dirs) throws Exception {
		Table result = new Table(new String[]{"folder","result","cause"});
		for (int i=0; i<dirs.size(); i++) {
			String dir2 = dirs.get(i);//new String(dir.getBytes(LOCAL_CHARSET),"iso-8859-1");
			boolean r = true;
			String msg = "";
			try {
				chSftp.rmdir(dir2);
			} catch (Exception e) {
				r = false;
				msg = e.getMessage();
				e.printStackTrace();
			}
			Record r2 = result.insert(0);
			r2.set("folder",dirs.get(i));
			r2.set("result",r?"success":"fail");
			r2.set("cause",msg);
		}
		return result;
		//FTPFile fs[] = ftp.listDirectories();
		//Logger.debug(fs.length);
	}
	
	private String getFullPath(String p) {
		if (p.equals("./")) return ftpPath;
		if (!p.startsWith("/")) return ftpPath + "/" + p;
		return p;
	}
	
	private String getShortPath(String p) {
		if (!p.startsWith("/")) return p;
		return p.replaceFirst(ftpPath, "");
	}

	public String getCurrentDir(){
		return ftpPath;
		
//		try {
//			return chSftp.pwd();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		return "";
	}

	
	private ArrayList<String> _remoteFiles = new ArrayList<String>();
	private ArrayList<Boolean> _remoteIsDir = new ArrayList<Boolean>();
	private ArrayList<Byte> _remoteLevels = new ArrayList<Byte>();

	private ArrayList<String> _localFiles = new ArrayList<String>();
	private ArrayList<Boolean> _localIsDir = new ArrayList<Boolean>();
	private ArrayList<Byte> _localLevels = new ArrayList<Byte>();
	
	
	public Sequence dirList(String path, ArrayList<String> regex,boolean onlyDir, boolean fullPath) throws Exception {
		Sequence s = new Sequence();
		ArrayList<String> ps = new ArrayList<String>();
		
		if (!path.endsWith("/")){
			path+="/";
		}
				
		List<String> paths = new ArrayList<String>();
		String reg = null;
		for (int i=0; i<regex.size(); i++) {
			reg = regex.get(i);
			if (reg.startsWith("/") ){
				
			}else{
				reg = getFullPath(path+reg);
			}
			if (reg.indexOf("*")!=-1 || reg.indexOf("?")!=-1 ){
				paths.add("/");
			}else{
				paths.add(reg.lastIndexOf("/")==0? reg : reg.substring(0, reg.lastIndexOf("/")));
			}
			regex.set(i, getRegPath(reg));			
			ps.add(regex.get(i));
		}
		
		_remoteFiles.clear();
		_remoteIsDir.clear();
		_remoteLevels.clear();
		for(int i=0; i<paths.size(); i++){
			ArrayList<String> psi = new ArrayList<String>();
			psi.add(ps.get(i));
			loadRemote(paths.get(i), psi);
		}
		for (int i=0; i<_remoteFiles.size(); i++) {
			//System.out.println(_remoteFiles.get(i));
			for (int j=0; j<regex.size(); j++) {
				if (_remoteFiles.get(i).matches(regex.get(j)) || _remoteFiles.get(i).startsWith(regex.get(j))) {
					if (onlyDir && !_remoteIsDir.get(i)) {
					} else {
						//s.add(fullPath?_remoteFiles.get(i):getShortPath(_remoteFiles.get(i)));
						s.add(_remoteFiles.get(i));
					}
					break;
				}
			}
		}
		
		return s;
	}

	public Sequence ls(String path, ArrayList<String> regex,boolean onlyDir, boolean fullPath) throws Exception {
		ArrayList<String> regex2 = new ArrayList<String>();
		for (int i=0; i<regex.size(); i++) {
			regex2.add(i, getRegPath(regex.get(i)));			
		}
		
		Sequence s = new Sequence();
		_ls(path, regex2, onlyDir, fullPath, s);
		
		return s;
	}

	public void _ls(String path, ArrayList<String> regex,boolean onlyDir, boolean fullPath, Sequence result) throws Exception {
		System.out.println(path);
		Vector fs = chSftp.ls(path);
		
		for (int i=0; i<fs.size(); i++) {
			LsEntry fsi = (LsEntry)fs.get(i);
			String name = fsi.getFilename();
			if ("..".equals(name) || ".".equals(name)) continue; 
			String n = getName(path,fsi,regex,onlyDir,fullPath);
			if (n != null && result.length()<maxFileNum) result.add(n);
		}
		
		
		for (int i=0; i<fs.size(); i++) {			
			LsEntry fsi = (LsEntry)fs.get(i);
			String name = fsi.getFilename();
			if ("..".equals(name) || ".".equals(name)) continue; 
			if (fsi.getAttrs().isDir() && result.length()<maxFileNum) {
				_ls(path+"/"+fsi.getFilename(), regex, onlyDir, fullPath, result);
			}
		}

	}
	
	private String getName(String parent,LsEntry f, ArrayList<String> regex,boolean onlyDir, boolean fullPath) throws Exception {
		String fullname = parent + "/" + f.getFilename();
		if  (regex.size()>0) {
			boolean match = false;
			for (int i=0; i<regex.size(); i++) {
				//System.out.println(fullname + "--" + regex.get(i));
				if (fullname.toLowerCase().replaceAll("/", " ").matches(regex.get(i).toLowerCase())) {
					match = true;
					break;
				}
			}
			if (!match) return null;
		}
		
		if (onlyDir && !f.getAttrs().isDir()) return null;
		
		return fullPath ? parent + "/" + f.getFilename() : f.getFilename();

	}

	private void loadRemote(String path, ArrayList<String> patterns) throws Exception {
		loadRemote(path,(byte)1,patterns);
	}

	private void loadRemote(String parent,byte level,ArrayList<String> patterns) throws Exception {
		if (!parent.endsWith("/")) parent = parent + "/";
		Vector fs = chSftp.ls(parent);
		System.out.println(parent+", "+fs.size());
//		String status = ftp.getStatus(parent);
	
//		if (status == null) return;
//		String[] fs = status.split("\r\n");
		for (int i=0; i<fs.size(); i++) {
			LsEntry fsi = (LsEntry)fs.get(i);
			String name = fsi.getFilename();

			System.out.println(fsi.getFilename());
			
			if (name.endsWith(".") || name.endsWith("..") ) continue;
			boolean dir = fsi.getAttrs().isDir();

			if (_remoteFiles.size()>=maxFileNum) {
				Logger.warn("ftp server has too many files, more than ["+maxFileNum+"]");
				return;
			}
			String fi = parent+name+(dir?"/":"");
			if (_remoteFiles.indexOf(fi)==-1) {
				_remoteFiles.add(fi);
				_remoteIsDir.add(dir);
				_remoteLevels.add(level);
			}
			//System.out.println(fi+" : "+dir + " : "+level);
			if (dir) {
				loadRemote(parent+name,(byte)(level+1),patterns);
			}
		}
		
	}
	
	public void cd(String path) throws Exception {
		chSftp.cd(path);
		ftpPath = path;
	}

	private int maxFileNum = 100;
	
    /**
     * ？代表单个字符，*代表任意字符，**代表任意字符和目录 
     * 将通配符表达式转化为正则表达式
     *  
     * @param path 
     * @return 
     */  
    public static String getRegPath(String path) {  
        char[] chars = path.toCharArray();  
        int len = chars.length;  
        StringBuilder sb = new StringBuilder();  
        boolean preX = false;  
        for(int i=0;i<len;i++){  
            if (chars[i] == '*'){//遇到*字符  
                if (preX){//如果是第二次遇到*，则将**替换成.*  
                    sb.append(".*");  
                    preX = false;  
                }else if(i+1 == len){//如果是遇到单星，且单星是最后一个字符，则直接将*转成[^/]*  
                    sb.append("[^/]*");  
                }else{//否则单星后面还有字符，则不做任何动作，下一把再做动作  
                    preX = true;  
                    continue;  
                }  
            }else{//遇到非*字符  
                if (preX){//如果上一把是*，则先把上一把的*对应的[^/]*添进来  
                    sb.append("[^/]*");  
                    preX = false;  
                }  
                if (chars[i] == '?'){//接着判断当前字符是不是?，是的话替换成.  
                    sb.append('.');  
                }else{//不是?的话，则就是普通字符，直接添进来  
                    sb.append(chars[i]);  
                }
            }
        }
        //System.out.println(sb.toString());
        return sb.toString();  
    } 

	public boolean get(String remoteFileName, String localPath) throws Exception {
		return this.get(remoteFileName, localPath,1);
	}
    /**
     * 
     * @param remoteFileName
     * @param localPath
     * @param mode 1:不能覆盖  2覆盖
     * @return
     * @throws Exception
     */
	public boolean get(String remoteFileName, String localPath, int mode) throws Exception {
		Logger.debug("get ["+remoteFileName+"] begin");
        
        try {
        	File f = new File(localPath);
        	if (mode == 2) {
        		
        	} else {
            	if (f.exists()) {
            		chSftp.get(remoteFileName, localPath);
            	} else {
                	if (f.exists()) {
                		Logger.debug("get ["+remoteFileName+"] failed, file has exist");
                		return false;
                	} else {
                		chSftp.get(remoteFileName, localPath);
                	}

            	}
        		
        	}
            
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
        } catch (Exception e) {
			Logger.warn(e);
		}
	}
	
	public static void main(String args[]) {
		try {
			//FtpUtil.downloadSftpFile("123.57.218.190", "root", "Carrygame888", 22, "/var/log/", "d:/test/ftp1.txt", "secure");

			SFtpClientImpl fc = new SFtpClientImpl(null, "47.94.18.233", 22, 1);
			fc.login("root", "Carrygame888");
			//fc.changeRemoteDir("/raqsoft");
			//fc.cd("/raqsoft");
			fc.cd("/ftp");
			
			
//			System.out.println("/ftp/file9.csv".replaceAll("/", " ").matches("[^/]*"));
//			System.out.println("file9.csv".matches("[^/]*"));
//			System.out.println(" ftp file9.csv".matches("[^/]*"));
			
//			Vector fs = fc.chSftp.ls("/ftp");
//			for (int i=0; i<fs.size(); i++) {
//				LsEntry fsi = (LsEntry)fs.get(i);
//				String name = fsi.getFilename();
//				System.out.println(fsi.getAttrs().isDir());
//				System.out.println(name);
//			}
//			//			fc.chSftp.mkdir("folder3");
//			fc.chSftp.rmdir("folder3");
			
			ArrayList<String> regex = new ArrayList<String>();
			regex.add("**");
			Sequence seq = fc.ls("/ftp", regex, false, false);
			System.out.println(seq);
			
			//fc.chSftp.get("file9.csv", "d:/ftp/file999.csv");
//			fc.chSftp.put("d:/ftp/file8.csv","/ftp/file9.csv");
//			fc.chSftp.put("d:/ftp/file8.csv","/ftp/file8.csv",ChannelSftp.OVERWRITE);
//			fc.chSftp.put("d:/ftp/file8.csv","/ftp/file8.csv",ChannelSftp.APPEND);
			//System.out.println(fc.chSftp.pwd());

			
			//System.out.println(fc.chSftp.pwd());
			
//			ArrayList<String> regex = new ArrayList<String>();
//			regex.add("*");
//			Sequence seq = fc.dirList("/ftp", regex,false,false);
//			System.out.println(seq);

			//fc.close();

//			FileInputStream fis = new FileInputStream("d:/aa1.txt");
//			fc.put("a2.txt", fis);
//			fis.close();

			//fc.get("/var/log/secure", "d:/ftp/secure");
//			fc.put("/var/secure", "d:/ftp/secure");
			fc.close();
			//fc.put("aa.txt", FileUtil.);
			
		} catch (Exception e) {
			Logger.error("", e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
