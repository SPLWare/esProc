package com.raqsoft.lib.ftp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import com.raqsoft.common.Logger;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.IResource;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;

public class FtpClientImpl extends Table implements IResource {
//	package com.raqsoft.lib.ftp;
//	public class FtpClient implements IResource
//	{
//		public FtpClient(Context, String url, int mode);
//		public FtpClient(Context ctx, String url, int port, int mode);	//注意连接成功后要ctx.addResource,mode=0表示主动，1表示被动
//		public boolean login(String user, String pwd);		//返回是否成功，如果有更详细的错误代码，可返回int之类
//		public boolean changeRemoteDir(String dir);/
//		public boolean put(String remoteFileName, InputStream in, boolean overwrited);
//		public boolean get(String remoteFileName, FileObject localFile, boolean overwrited);
//		public boolean mkdir(String path);
//		public void close();
//	}
//
//	package com.raqsoft.lib.ftp.function;
	/*函数
	ftp_client(url:port, user, pwd) 返回FtpClient, port可省略，@d被动模式
	ftp_cd(client, path)
	ftp_put(client, remoteFileName, localFileName或FileObject)   @f覆盖， 返回false表示不成功
	ftp_get(client, remoteFileName, localFileName或FileObject)   @f覆盖， 返回false表示不成功
	ftp_mkdir(client, remotePath)
	ftp_close(client);

	ftp_dir(client, path) 列出文件，path支持通配符吗？
	@d  只列子目录
	@p  完整路径名
	@m  创建目录		//ftp_mkdir文档中不再出现
	@r  删除目录
	
	如果支持mget，那在原ftp_get提供一个@m选项，也就是目录到目录
	或者是带通配符的路径到本地目录

	ftp_mput(client,remoteFolder,localFolder,带通配符的localFiles;)   @f覆盖，@t跳过已存在文件， 返回false表示不成功
	ftp_mget(client,remoteFolder,localFolder,带通配符的remoteFiles)   @f覆盖，@t跳过已存在文件， 返回false表示不成功
	ftp_dir(client, 带通配符的remoteFiles) 列出文件，path支持通配符吗？

	*/
	public String toString() {
		return info;
	}
	public FTPClient ftp = null;
	private Context ctx = null;
	private String info = "";
	private String dir = "/";
	private int maxFileNum = 10000;
	
	
	/** 本地字符编码 */
	private static String LOCAL_CHARSET = "GBK";
	// FTP协议里面，规定文件名编码为iso-8859-1
	private static String SERVER_CHARSET = "ISO-8859-1";
	
	public FtpClientImpl(Context ctx, String url, int port, int mode) throws Exception {
        this.ctx = ctx;
        info = url+":"+port;
		ftp = new FTPClient();
        if (mode == 0) {
            ftp.enterLocalActiveMode();
        } else {
            ftp.enterLocalPassiveMode();
        }
        FTPClientConfig config = new FTPClientConfig();
        ftp.setDataTimeout(10000);
        ftp.setAutodetectUTF8(true);
        ftp.setControlEncoding("UTF-8");
        ftp.configure(config);
        //ftp.enterLocalPassiveMode(); 
		//由于apache不支持中文语言环境，通过定制类解析中文日期类型
		//ftp.configure(new FTPClientConfig("org.apache.commons.net.ftp.parser.UnixFTPEntryParser")); //com.zznode.tnms.ra.c11n.nj.resource.ftp
        if (port != 0) ftp.connect(url, port);
        else ftp.connect(url);
        
     // 开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就使用本地编码（GBK）.
        if (FTPReply.isPositiveCompletion(ftp.sendCommand("OPTS UTF8", "ON"))) {
        	//LOCAL_CHARSET = "UTF-8";
		}
        Logger.debug("LOCAL_CHARSET ： " + LOCAL_CHARSET);
        //ftp.setControlEncoding("UTF-8");
		//ftp.setControlEncoding(LOCAL_CHARSET);
	}
	
	public boolean login(String user, String password) throws Exception {
		//ftp.logout();
		info += "ftp:/"+user+":"+password+":"+info;
		boolean r = ftp.login(user, password);
		ftp.setFileType(FTPClient.BINARY_FILE_TYPE);			
		Logger.debug("login : " + r);
		
		ArrayList<String> patterns = new ArrayList<String>();
		patterns.add(getRegPath("**"));
		//loadRemote(patterns);
		return r;
	}

	private void loadRemote(String path, ArrayList<String> patterns) throws Exception {


//		_remoteFiles.add("/");
//		_remoteIsDir.add(true);
//		_remoteLevels.add((byte)0);
		loadRemote(path,(byte)1,patterns);
	}
	private ArrayList<String> _remoteFiles = new ArrayList<String>();
	private ArrayList<Boolean> _remoteIsDir = new ArrayList<Boolean>();
	private ArrayList<Byte> _remoteLevels = new ArrayList<Byte>();
	
	private void loadRemote(String parent,byte level,ArrayList<String> patterns) throws Exception {
		String status = ftp.getStatus(parent);
		if (!parent.endsWith("/")) parent = parent + "/";
	
		if (status == null) return;
		String[] fs = status.split("\r\n");
		for (int i=1; i<fs.length-1; i++) {
			String name = fs[i].substring(fs[i].lastIndexOf(' ')+1);
			if (name.endsWith(".") || name.endsWith("..") ) continue;
			boolean dir = fs[i].indexOf("<DIR>")>=0;
			if (!dir){
				dir = fs[i].startsWith("d");
			}
			if (_remoteFiles.size()>=maxFileNum) {
				Logger.warn("ftp server has too many files, more than ["+maxFileNum+"]");
				return;
			}
			String fi = parent+name+(dir?"/":"");
			_remoteFiles.add(fi);
			_remoteIsDir.add(dir);
			_remoteLevels.add(level);
			//System.out.println(fi+" : "+dir + " : "+level);
			if (dir) {
				loadRemote(parent+name,(byte)(level+1),patterns);
			}
		}
		
	}
	

	private void loadLocal(ArrayList<String> patterns) throws Exception {
		_localFiles.clear();
		_localIsDir.clear();
		_localLevels.clear();
		
		for (int i=0; i<patterns.size(); i++) {
			String pi = patterns.get(i);
			//System.out.println(pi);
			int pos1 = pi.indexOf("*");
			//System.out.println(pos1);
			int pos2 = pi.indexOf("?");
			//System.out.println(pos2);
			pos1 = pos1==-1?pos2:pos1;
			String root = pi;
			if (pos1>=0) root = pi.substring(0, pos1);
			//System.out.println(root);
			pos1 = root.lastIndexOf("/");
			if (pos1>0 && !root.endsWith("/")) root = root.substring(0, pos1+1);

			//System.out.println(root);
			ArrayList<String> ps = new ArrayList<String>();
			ps.add(getRegPath(pi));
			_localFiles.add(root);
			_localIsDir.add(true);
			_localLevels.add((byte)0);
			loadLocal(root,(byte)1,ps);
		}
	}
	//private ArrayList<File> _localFiles = new ArrayList<File>();
	private ArrayList<String> _localFiles = new ArrayList<String>();
	private ArrayList<Boolean> _localIsDir = new ArrayList<Boolean>();
	private ArrayList<Byte> _localLevels = new ArrayList<Byte>();
	/**
	 * 
	 * @param parent
	 * @param level
	 * @param patterns 要求是绝对路径，盘符及路径是小写，分隔符是/
	 * @throws Exception
	 */
	private void loadLocal(String parent,byte level,ArrayList<String> patterns) throws Exception {
		parent = parent.replaceAll("\\\\", "/");
		if (!parent.endsWith("/")) parent = parent + "/";
		File f = new File(parent);
		
		File[] fs = f.listFiles();
		for (int i=0; i<fs.length; i++) {
			File fi = fs[i];
			if (fi.length()==0 && !fi.isDirectory()) {
				continue;
			}
//			if (fi.length() == 0) continue;
			String path = fi.getAbsolutePath().replaceAll("\\\\", "/");
			boolean useful2 = false;
			for (int j=0; j<patterns.size(); j++) {
				if (patterns.get(j).toLowerCase().startsWith(path.toLowerCase()) || path.toLowerCase().matches(patterns.get(j).toLowerCase())) {
					useful2 = true;
					break;
				}
			}
			if (!useful2) continue;;

			String pi = fi.getAbsolutePath().replaceAll("\\\\", "/");
			_localFiles.add(pi);
			_localIsDir.add(fi.isDirectory());
			_localLevels.add(level);
			//System.out.println(fi+" : "+fi.isDirectory() + " : "+level);
			if (fi.isDirectory()) {
				loadLocal(pi,(byte)(level+1),patterns);
			}
		}
	}
	
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
		for(String ss:paths){
			loadRemote(ss, ps);
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
	
	public Table mget(String localFolder,String remoteFolder,ArrayList<String> remoteFiles,boolean overwrite,boolean ignore) throws Exception {
		if (remoteFolder == null || remoteFolder.length() == 0) remoteFolder = dir;
		if (localFolder == null) localFolder = ""; 
		localFolder = localFolder.replaceAll("\\\\", "/");
		remoteFolder = remoteFolder.replaceAll("\\\\", "/");
		localFolder = new FileObject(localFolder).getLocalFile().file().getAbsolutePath();
		if (!localFolder.endsWith("/")) localFolder = localFolder+"/";
		if (!remoteFolder.endsWith("/")) remoteFolder = remoteFolder+"/";
		remoteFolder = getFullPath(remoteFolder);
		
		for (int i=0; i<remoteFiles.size(); i++) {
			remoteFiles.set(i, remoteFiles.get(i).replaceAll("\\\\", "/"));
			if (remoteFiles.get(i).startsWith("/")) remoteFiles.set(i, remoteFiles.get(i).substring(1));
			remoteFiles.set(i, getRegPath(remoteFolder + remoteFiles.get(i)));
		}
		
		_remoteFiles.clear();
		_remoteIsDir.clear();
		_remoteLevels.clear();
		loadRemote(remoteFolder, remoteFiles);
		
		ArrayList<String> remotes = new ArrayList<String>();
		ArrayList<String> locals = new ArrayList<String>();
		ArrayList<Boolean> dirs = new ArrayList<Boolean>(); //0
		ArrayList<Boolean> localExists = new ArrayList<Boolean>();
		int existCount = 0;
		
		Sequence s = new Sequence();
		
		for (int i=0; i<_remoteFiles.size(); i++) {
			for (int j=0; j<remoteFiles.size(); j++) {
				if (_remoteFiles.get(i).matches(remoteFiles.get(j))) {
					remotes.add(_remoteFiles.get(i));
					String fi = _remoteFiles.get(i).substring(remoteFolder.length());
					File f = new File(localFolder+fi);
					locals.add(f.getAbsolutePath());
					dirs.add(_remoteIsDir.get(i));
					localExists.add(f.exists());
					if (f.exists() && !_remoteIsDir.get(i)) {
						existCount++;
						s.add(f.getAbsolutePath());
					}
				}
			}
		}
		
		Table result = null;

		if (!ignore && !overwrite) {
			if (existCount>0) {
				result = new Table(new String[]{"errorCode","localExistFiles"});
				Record r = result.insert(0);
				r.set("errorCode", 1);
				Sequence seq = new Sequence();
				//for (int i=0; i<localExists.)
				//seq.addAll(localExists.toArray(new String[localExists.size()]));
				r.set("localExistFiles",s);
				return result;
			}
		}
		
		Sequence sucs = new Sequence();
		Sequence overwrites = new Sequence();
		Sequence ignores = new Sequence();
		Sequence fails = new Sequence();
		
		for (int i=0; i<remotes.size(); i++) {
			try {
				File locali = new File(locals.get(i));
				if (dirs.get(i)) {
					if (!locali.exists()) locali.mkdirs();
					continue;
				} else {
					if (locali.exists()) {
						if (overwrite) {
							locali.delete();
						} else if (ignore) {
							ignores.add(remotes.get(i));
							continue;
						}
					}
					OutputStream os = new FileObject(locali.getAbsolutePath()).getOutputStream(false);
					boolean r = ftp.retrieveFile(remotes.get(i), os);
					if (r) {
						sucs.add(locals.get(i));
					} else fails.add(remotes.get(i));
					os.flush();
					os.close();
					if (localExists.get(i) && overwrite) overwrites.add(locals.get(i));
				}
			} catch (IOException e) {
				fails.add(remotes.get(i));
				e.printStackTrace();
			}
		}

		result = new Table(new String[]{"success","overwrite","ignore","fail"});
		Record r = result.insert(0);
		r.set("success",sucs);
		r.set("overwrite",overwrites);
		r.set("ignore",ignores);
		r.set("fail",fails);
		
		return result;
	}
	
	public Table mput(String localFolder,String remoteFolder,ArrayList<String> localFiles,boolean overwrite,boolean ignore) throws Exception{
		if (remoteFolder == null || remoteFolder.length() == 0) remoteFolder = dir;
		if (localFolder == null) localFolder = ""; 

		remoteFolder = remoteFolder.replaceAll("\\\\", "/");
		localFolder = new FileObject(localFolder).getLocalFile().file().getAbsolutePath();
		localFolder = localFolder.replaceAll("\\\\", "/");
		if (!localFolder.endsWith("/")) localFolder = localFolder+"/";
		if (!remoteFolder.endsWith("/")) remoteFolder = remoteFolder+"/";
		remoteFolder = getFullPath(remoteFolder);

		String parents[] = remoteFolder.split("/");
		String curr = "/";
		for (int i=0; i<parents.length; i++) {
			if (parents[i].length() == 0) continue;
			curr += parents[i] + "/";
			if (ftp.getStatus(curr) == null) ftp.makeDirectory(curr);
		}
		
		Logger.debug("localFiles.size() = "+localFiles.size());
		for (int i=0; i<localFiles.size(); i++) {
			localFiles.set(i, localFiles.get(i).replaceAll("\\\\", "/"));
			if (localFiles.get(i).startsWith("/")) localFiles.set(i, localFiles.get(i).substring(1));
			localFiles.set(i, localFolder + localFiles.get(i));
			Logger.debug("localFiles.get(i) = "+localFiles.get(i));
		}
		
		loadLocal(localFiles);

		for (int i=0; i<localFiles.size(); i++) {
			localFiles.set(i, getRegPath(localFiles.get(i)));
		}

		
		ArrayList<String> remotes = new ArrayList<String>();
		ArrayList<String> locals = new ArrayList<String>();
		ArrayList<Boolean> dirs = new ArrayList<Boolean>(); //0
		ArrayList<Boolean> remoteExists = new ArrayList<Boolean>();
		int existCount = 0;
		
		Sequence s = new Sequence();
		
		for (int i=0; i<_localFiles.size(); i++) {
			for (int j=0; j<localFiles.size(); j++) {
				//Logger.debug("i,j = "+_localFiles.get(i)+" , "+localFiles.get(j));
				if (_localFiles.get(i).matches(localFiles.get(j))) {
					Logger.debug("i,j matched");
					locals.add(_localFiles.get(i));
					String fi = _localFiles.get(i).substring(localFolder.length());
					String st = ftp.getStatus(remoteFolder+fi);
					String[] sts = st.split("\r\n");
					remotes.add(remoteFolder+fi);
					dirs.add(_localIsDir.get(i));
					remoteExists.add(sts.length==3);
					if (sts.length==3 && !_localIsDir.get(i)) {
						existCount++;
						s.add(remoteFolder+fi);
					}
				}
			}
		}
		
		Table result = null;

		if (!ignore && !overwrite) {
			if (existCount>0) {
				result = new Table(new String[]{"errorCode","remoteExistFiles"});
				Record r = result.insert(0);
				r.set("errorCode", 1);
				Sequence seq = new Sequence();
				//for (int i=0; i<localExists.)
				//seq.addAll(localExists.toArray(new String[localExists.size()]));
				r.set("remoteExistFiles",s);
				return result;
			}
		}
		
		Sequence sucs = new Sequence();
		Sequence overwrites = new Sequence();
		Sequence ignores = new Sequence();
		Sequence fails = new Sequence();
		
		for (int i=0; i<locals.size(); i++) {
			try {
				File locali = new File(locals.get(i));
				String st = ftp.getStatus(remotes.get(i));
				String sts[] = st.split("\r\n");
				if (dirs.get(i)) {
					if (st == null) ftp.makeDirectory(remotes.get(i));
					continue;
				} else {
					if (sts.length == 3) {
						if (overwrite) {
							ftp.deleteFile(remotes.get(i));
						} else if (ignore) {
							ignores.add(locals.get(i));
							continue;
						}
					}
					
					InputStream is = new FileObject(locali.getAbsolutePath()).getInputStream();
					//System.out.println("r = " + is.available());
					//System.out.println("r = " + remotes.get(i));
					boolean r = ftp.appendFile(remotes.get(i), is);
					//System.out.println("r = " + r);
					is.close();
					sucs.add(remotes.get(i));
					if (remoteExists.get(i) && overwrite) overwrites.add(remotes.get(i));
				}
			} catch (IOException e) {
				fails.add(remotes.get(i));
				e.printStackTrace();
			}
		}

		result = new Table(new String[]{"success","overwrite","ignore","fail"});
		Record r = result.insert(0);
		r.set("success",sucs);
		r.set("overwrite",overwrites);
		r.set("ignore",ignores);
		r.set("fail",fails);
		
		return result;
	}

	public boolean changeRemoteDir(String dir) throws Exception {
		if (!dir.endsWith("/")) dir = dir+"/";
		String dir2 = dir;//new String(dir.getBytes(LOCAL_CHARSET),"iso-8859-1");
		boolean r = ftp.changeWorkingDirectory(dir2);
		if (r) {
			Logger.debug("changeRemoteDir success : " + dir);
			this.dir = dir;
		} else Logger.warn("changeRemoteDir “"+dir+"” failed, current remote dir is “"+this.dir+"”");
		return r;
		//FTPFile fs[] = ftp.listDirectories();
		//Logger.debug(fs.length);
	}

	public String getCurrentDir(){
		try {
			return ftp.printWorkingDirectory();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "";
	}
	
	public Sequence mkdir(ArrayList<String> dirs) throws Exception {
		Table result = new Table(new String[]{"folder","result","cause"});
		for (int i=0; i<dirs.size(); i++) {
			String dir2 = getShortPath(dirs.get(i));//new String(dir.getBytes(LOCAL_CHARSET),"iso-8859-1");
			boolean r = ftp.makeDirectory(dir2);
			String msg = "folder not exist or not empty";
			if (r) {
				Logger.debug("mkdir success : " + dir);
				//this.dir = dir;
			} else {
				String st = ftp.getStatus(dir2);
				String[] sts = st.split("\r\n");
				if (sts.length==4){
					msg = "folder had been existed";
				}
				Logger.warn("mkdir dir “"+dir+"” failed");
			}
			Record r2 = result.insert(0);
			r2.set("folder",this.getFullPath(dirs.get(i)));
			r2.set("result",r?"success":"fail");
			r2.set("cause",r?"":msg);
		}
		return result;
	}
	
	private String getFullPath(String p) {
		if (p.equals("./")) return dir;
		if (!p.startsWith("/")) return dir + p;
		return p;
	}
	
	private String getShortPath(String p) {
		if (!p.startsWith("/")) return p;
		return p.replaceFirst(dir, "");
	}
	
	public Sequence deldir(ArrayList<String> dirs) throws Exception {
		Table result = new Table(new String[]{"folder","result","cause"});
		for (int i=0; i<dirs.size(); i++) {
			String dir2 = getShortPath(dirs.get(i));//new String(dir.getBytes(LOCAL_CHARSET),"iso-8859-1");
			boolean r = ftp.removeDirectory(dir2);
			if (r) {
				Logger.debug("delete dir success : " + dir);
				//this.dir = dir;
			} else Logger.warn("delete dir “"+dir+"” failed");
			Record r2 = result.insert(0);
			r2.set("folder",this.getFullPath(dirs.get(i)));
			r2.set("result",r?"success":"fail");
			r2.set("cause",r?"":"folder not exist or not empty");
		}
		return result;
		//FTPFile fs[] = ftp.listDirectories();
		//Logger.debug(fs.length);
	}

	public boolean put(String remoteFileName, InputStream in, boolean overwrite) throws Exception {
		Logger.debug("put ["+remoteFileName+"] begin");
		String remoteFileName2 = remoteFileName;//new String(remoteFileName.getBytes(LOCAL_CHARSET),"iso-8859-1");
		String status = ftp.getStatus(remoteFileName2);
		if (status != null) {
			String[] ss = status.split("\r\n");
			if (ss.length==3) {
				if ( overwrite){
					ftp.deleteFile(remoteFileName2);
				} else {
					Logger.warn("remote file ["+dir+remoteFileName+"] has exist");
					return false;
				}
			}
		}

		boolean r = ftp.appendFile(remoteFileName2, in);
		return r;
	}
	
	public boolean get(String remoteFileName, FileObject localFile, boolean overwrite) throws Exception {
		Logger.debug("get ["+remoteFileName+"] begin");
		String remoteFileName2 = remoteFileName;//new String(remoteFileName.getBytes(LOCAL_CHARSET),"iso-8859-1");
		if (localFile.getLocalFile().file().exists()) {
			if (overwrite) localFile.getLocalFile().file().delete();
			else {
				Logger.warn("local file ["+localFile.getLocalFile().file()+"] has exist");
				return false;
			}
		}
		OutputStream os = localFile.getOutputStream(false);
		boolean r = ftp.retrieveFile(remoteFileName2, os);
		os.flush();
		os.close();
		Logger.debug("get ["+remoteFileName+"] to local ["+localFile.getLocalFile().file().getAbsolutePath()+"]" + (r?"success":"failed"));
		return r;
	}

	@Override
	public void close() {
		try {
			ftp.disconnect();
			ftp = null;
			ctx.removeResource(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

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

	public static void main(String args[]) {
		try {
			FtpClientImpl fc = new FtpClientImpl(null, "127.0.0.1", 21, 0);
			//FtpClientImpl fc = new FtpClientImpl(null, "192.168.0.175", 21, 1);
			fc.login("xingjinglong@hotmail.com", "Xiaogeda5");
			fc.changeRemoteDir("/");
//			
//			
//			ArrayList<String> ss1 = new ArrayList<String>();
//			ss1.add("a*2*");
//			Table t = fc.mget("d:/ftp2", "/", ss1, false, false);
//			System.out.println(t);

//			System.out.println(fc.ftp.getStatus("/a1"));
//			System.out.println(fc.ftp.getStatus("/b1"));
//			System.out.println(fc.ftp.getStatus("/aaa1"));
//			System.out.println(fc.ftp.getStatus("/aaa1/aaa1"));

//			ArrayList<String> ss2 = new ArrayList<String>();
//			ss2.add("a*2*");
//			Table t = fc.mput("d:/ftp2", "/t1/t2/t3/", ss2, true, false);
//			System.out.println(t);

			ArrayList<String> ss = new ArrayList<String>();
			//ss.add("*1**");
			ss.add("/p*/temp??.txt");
			Sequence seq = fc.dirList("/", ss,false,false);
			System.out.println(seq);

			
//			ArrayList<String> patterns = new ArrayList<String>();
//			patterns.add("d:/ftp/a1**");
//			fc.loadLocal(patterns);
			
			//fc.loadRemote("/",(byte)1);

//			System.out.println(fc.ftp.getStatus());
//			System.out.println(fc.ftp.getStatus("/"));
//			System.out.println(fc.ftp.getStatus("/a1/aa.txt"));
//			String[] fs = fc.ftp.listNames("/a1/aa.txt");//.listFiles("/");
//			if (fs != null) {
//				System.out.println(fs.length);
//				for (int i=0; i<fs.length; i++) {
//					System.out.println(fs[i]);
//				}
//			}

			
//			FTPFile[] fs2 = fc.ftp.listFiles("/");
//			System.out.println(fs2.length);
//			if (fs2 != null) {
//				for (int i=0; i<fs2.length; i++) {
//					System.out.println(fs2[i].getLink());
//					System.out.println(fs2[i].getName());
//				}
//			}

			//fc.close();

//			FileInputStream fis = new FileInputStream("d:/aa.txt");
//			fc.put("aa.txt", fis, false);
//			fis.close();
//			System.out.println("aabtxt".matches("a*"));
			
//			FileObject localFile = new FileObject("d:/aa2.txt");
//			fc.get("aa.txt", localFile, true);
			
			//fc.put("aa.txt", FileUtil.);
			System.out.println("/pre_1/temp11.txt".matches("/p[^/]*/temp...*"));
			
		} catch (Exception e) {
			Logger.error("", e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
}
