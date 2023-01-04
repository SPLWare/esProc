package com.scudata.lib.hdfs.function;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

//import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.BaseRecord;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.JSONUtil;

public class WebHdfs extends Function {
	protected String url = null;
	protected String localFile = null;
	protected String op = null;
	protected Context m_ctx;
	
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		m_ctx = ctx;
		return this;
	}
	
	protected void setParamSize(int paramSize){
		//m_paramSize = paramSize;
	}

	private static final Logger logger = LoggerFactory.getLogger(WebHdfs.class);

    /**
     * @param webhdfs
     * @param stream       the InputStream of file to upload
     * @param hdfsFilePath
     * @param op
     * @param parameters
     * @param method
     * @throws IOException
     */
    public static void uploadFile(String webhdfs, InputStream stream, String method) throws IOException {
        HttpURLConnection con;
        try {
            con = getConnection(webhdfs, method, "application/octet-stream");

            byte[] bytes = new byte[1024];
            int rc = 0;
            while ((rc = stream.read(bytes, 0, bytes.length)) > 0)
                con.getOutputStream().write(bytes, 0, rc);
            con.getInputStream();
            con.disconnect();
        } catch (IOException e) {
            logger.info(e.getMessage());
            e.printStackTrace();
        }
        stream.close();
    }
    




    /**
     * @param strurl     webhdfs like http://ip:port/webhdfs/v1 ,port usually 50070 or 14000
     * @param path       hdfs path + hdfs filename  eg:/user/razor/readme.txt
     * @param op         the operation for hdfsFile eg:GETFILESTATUS,OPEN,MKDIRS,CREATE etc.
     * @param parameters other parameter if you need
     * @param method     method eg: GET POST PUT etc.
     * @return
     */
    public static HttpURLConnection getConnection(String strurl, String method, String contentType) {
        URL url = null;
        HttpURLConnection con = null;
        StringBuffer sb = new StringBuffer();
        try {
            sb.append(strurl);
            url = new URL(sb.toString());
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(method);
            con.setRequestProperty("accept", "*/*");
            con.setRequestProperty("connection", "Keep-Alive");
            String s = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0)";
            String s1 = "ozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)";
            con.setRequestProperty("User-Agent", s1);
            con.setRequestProperty("Content-Type", contentType);
//            con.setRequestProperty("Accept-Encoding", "gzip");
//            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
        } catch (IOException e) {
            logger.error("eeee", e);
        }
        return con;
    }

    
    public static void  downLoadFromUrl(String urlStr,String file) throws IOException{
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        //设置超时间为3秒
        conn.setConnectTimeout(3*1000);
        conn.setRequestMethod("GET");
        //防止屏蔽程序抓取而返回403错误
        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        conn.setRequestProperty("lfwywxqyh_token","lfwywxqyh_token");
        String ct = "application/octet-stream";
        if (urlStr.toLowerCase().indexOf(".txt")>=0) {
        	ct = "text/plain";
        } else if (urlStr.toLowerCase().indexOf(".xls")>=0) {
        	ct = "Application/msexcel";
        } else if (urlStr.toLowerCase().indexOf(".btx")>=0) {
        	
        } 
        conn.setRequestProperty("Content-Type",ct);

        //得到输入流
        InputStream inputStream = conn.getInputStream();
        //获取自己数组
        byte[] getData = readInputStream(inputStream);

        //文件保存位置
        File f = new File(file);
        if(!f.getParentFile().exists()){
        	f.getParentFile().mkdir();
        }
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(getData);
        if(fos!=null){
            fos.close();
        }
        if(inputStream!=null){
            inputStream.close();
        }


        System.out.println("info:"+url+" download success");

    }


    
    public static byte[]  downLoadFromUrl(String urlStr) throws IOException{
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        //设置超时间为3秒
        conn.setConnectTimeout(3*1000);
        conn.setRequestMethod("GET");
        //防止屏蔽程序抓取而返回403错误
        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        conn.setRequestProperty("lfwywxqyh_token","lfwywxqyh_token");
        conn.setRequestProperty("Content-Type","application/octet-stream");

        //得到输入流
        InputStream inputStream = conn.getInputStream();
        //获取自己数组
        byte[] getData = readInputStream(inputStream);

        if(inputStream!=null){
            inputStream.close();
        }


        return getData;

    }

    /**
     * 从输入流中获取字节数组
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static  byte[] readInputStream(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int len = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while((len = inputStream.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        bos.close();
        return bos.toByteArray();
    }
    
    /**
     * 向指定URL发送GET方法的请求
     * 
     * @param url
     *            发送请求的URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String stringResult(String url, String method) {
        String result = "";
        BufferedReader in = null;
        try {
        	URL realUrl = new URL(url);
            // 打开和URL之间的连接
        	HttpURLConnection connection = (HttpURLConnection)(realUrl.openConnection());
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.setRequestProperty("Content-Type", "text/plain");
            
            connection.setRequestMethod(method);
            // 建立实际的连接
            connection.connect();
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }

    
    public static void main(String args[]) {
    	try {
//			WebHdfs w = new WebHdfs();
//			InputStream stream = new FileInputStream(new File("d:/emps.txt"));
//			Map<String, String> m = new HashMap<String,String>();
//			m.put("user.name", "root");
//			m.put("overwrite", "true");
//			//w.uploadFile("http://localhost:50070/webhdfs/v1", stream, "/spl2/emps20211112.txt", "APPEND", m, "POST");
//			
//			m.clear();
//			m.put("user.name", "root");
			//w.get("http://localhost:50070/webhdfs/v1/?op=LISTSTATUS",m);
			String url = "http://localhost:50070/webhdfs/v1/user/root/orders4.btx";
			String params = "&user.name=root";
			String s = WebHdfs.stringResult(url+"?op=DELETE"+params,"DELETE");
			Object o = JSONUtil.parseJSON(s.toCharArray(), 0, s.length()-1);
			if (o!=null) {
				BaseRecord rc = (BaseRecord)o;
				Object o2 = rc.getFieldValue("boolean");
				System.out.println(o2);
			}
			System.out.println(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    
    /**
     * 
     * webhdfs(url,localFile)
     * 
     * GET OPEN,GETFILESTATUS,LISTSTATUS,LISTSTATUS_BATCH,GETCONTENTSUMMARY,GETQUOTAUSAGE,GETFILECHECKSUM,GETHOMEDIRECTORY,GETDELEGATIONTOKEN,GETTRASHROOT,GETXATTRS,GETXATTRS,GETXATTRS,LISTXATTRS,CHECKACCESS,GETALLSTORAGEPOLICY,GETSTORAGEPOLICY,GETSNAPSHOTDIFF,GETSNAPSHOTTABLEDIRECTORYLIST,GETFILEBLOCKLOCATIONS,GETECPOLICY
     * 
     * 
     * 
     * 
     */
    
    public static String[] GETS = null;
    public static String[] PUTS = null;
    public static String[] POSTS = null;
    public static String[] DELETES = null;
    static {
    	GETS = "OPEN,GETFILESTATUS,LISTSTATUS,LISTSTATUS_BATCH,GETCONTENTSUMMARY,GETQUOTAUSAGE,GETFILECHECKSUM,GETHOMEDIRECTORY,GETDELEGATIONTOKEN,GETTRASHROOT,GETXATTRS,GETXATTRS,GETXATTRS,LISTXATTRS,CHECKACCESS,GETALLSTORAGEPOLICY,GETSTORAGEPOLICY,GETSNAPSHOTDIFF,GETSNAPSHOTTABLEDIRECTORYLIST,GETFILEBLOCKLOCATIONS,GETECPOLICY".split(",");
    	PUTS = "CREATE,MKDIRS,CREATESYMLINK,RENAME,SETREPLICATION,SETOWNER,SETPERMISSION,SETTIMES,RENEWDELEGATIONTOKEN,CANCELDELEGATIONTOKEN,ALLOWSNAPSHOT,DISALLOWSNAPSHOT,CREATESNAPSHOT,RENAMESNAPSHOT,SETXATTR,REMOVEXATTR,SETSTORAGEPOLICY,SATISFYSTORAGEPOLICY,ENABLEECPOLICY,DISABLEECPOLICY,SETECPOLICY".split(",");
    	POSTS = "APPEND,CONCAT,TRUNCATE,UNSETSTORAGEPOLICY,UNSETECPOLICY".split(",");
    	DELETES = "DELETE,DELETESNAPSHOT".split(",");
    }
    
	@Override
	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if (param == null) {
			throw new RQException("webhdfs" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		//System.out.println("baseSize = " + size);

		Object o = new Object();
		

		if (param.isLeaf()) {
			o = param.getLeafExpression().calculate(ctx);
			if (o instanceof String) {
				this.url = ((String)o);
			}else{
				throw new RQException("webhdfs" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			this.url = (String)param.getSub(0).getLeafExpression().calculate(ctx);
			this.localFile = (String)param.getSub(1).getLeafExpression().calculate(ctx);
		}
		
		String method = "";
		for (int i=GETS.length-1; i>=0; i--) {
			if (url.indexOf(GETS[i])>=0) {
				op = GETS[i];
				method = "GET";
				break;
			}
		}
		for (int i=PUTS.length-1; i>=0; i--) {
			if (url.indexOf(PUTS[i])>=0) {
				op = PUTS[i];
				method = "PUT";
				break;
			}
		}
		for (int i=POSTS.length-1; i>=0; i--) {
			if (url.indexOf(POSTS[i])>=0) {
				op = POSTS[i];
				method = "POST";
				break;
			}
		}
		for (int i=DELETES.length-1; i>=0; i--) {
			if (url.indexOf(DELETES[i])>=0) {
				op = DELETES[i];
				method = "DELETE";
				break;
			}
		}
		if (op == null) {
			throw new RQException("webhdfs" + mm.getMessage("function.paramTypeError"));
		}
		
		try {
			if ("CREATE".equals(op)) {
				if (localFile == null) throw new RQException("webhdfs need second param [localFile]" + mm.getMessage("function.paramTypeError"));
				FileObject fo = new FileObject(localFile,null,m_ctx);
				uploadFile(url, fo.getInputStream(), method);
				return "Upload success : " + localFile;
			} else if ("OPEN".equals(op)) {
				if (localFile == null) throw new RQException("webhdfs need second param [localFile]" + mm.getMessage("function.paramTypeError"));
				downLoadFromUrl(url,localFile);
				return "Download success : " + localFile;
			} else {
				return stringResult(url,method);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RQException("webhdfs : " + e.getMessage());
		}

	}
}

