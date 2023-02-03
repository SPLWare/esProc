package com.scudata.dm;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONObject;

import com.scudata.common.RQException;

public class HttpFile implements IFile {
	private String url;
	private String params;
	private String paramsEncoding = "UTF-8";
	private String requestContentType = null;
	private HashMap headers;
	private Map<String,List<String>> responseHeaders;
	private int timeout = -1;   //读取超时时间，以秒为单位
	private String sendParamMethod = "POST";    //传送参数的方法
	
	static {
        HostnameVerifier ignoreHostnameVerifier = new HostnameVerifier() {
            public boolean verify( String s, SSLSession sslsession) {
                return true;
            }  
        };
        HttpsURLConnection.setDefaultHostnameVerifier(ignoreHostnameVerifier);
	}
	
	static {
		System.setProperty( "sun.net.client.defaultConnectTimeout", "30000" );
		System.setProperty( "sun.net.client.defaultReadTimeout", "0" );
	}

	public HttpFile( String url ) {
		this.url = url;
	}

	public void setFileName( String url ) {
		this.url = url;
	}
	
	/**
	 * 设置post方法传送的参数
	 * @param params 参数，格式为: key1=value1&key2=value2......
	 * @param paramsEncoding  参数字符集,如GBK，UTF-8
	 * @param method  传送参数的方法，post或get
	 */
	public void setPostParams( String params, String paramsEncoding, String method ) {
		if( params != null && params.trim().length() > 0 ) {
			this.params = params.trim();
		}
		if( paramsEncoding != null && paramsEncoding.trim().length() > 0 ) {
			this.paramsEncoding = paramsEncoding.trim();
		}
		if( method != null && method.trim().length() > 0 ) {
			this.sendParamMethod = method.trim();
		}
	}
	
	public void addRequestHeader( String propName, String value ) {
		if( headers == null ) {
			headers = new HashMap();
		}
		headers.put( propName, value );
	}
	
	public void setRequestContentType( String type ) {
		this.requestContentType = type;
	}
	
	public void setReadTimeout( int timeout ) {
		this.timeout = timeout;
	}

	public InputStream getInputStream() {
		try {
			if( params == null && headers == null ) {
				URL u = new URL( url );
				URLConnection conn = u.openConnection();
				if( timeout > 0 ) conn.setReadTimeout( timeout * 1000 );
				if( url.toLowerCase().startsWith( "https" ) ) {
		            TrustManager[] tm = { new MyX509TrustManager() };
		            SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
		            sslContext.init(null, tm, new java.security.SecureRandom());
		            // 从上述SSLContext对象中得到SSLSocketFactory对象SSLSocketFactory
		            SSLSocketFactory ssf = sslContext.getSocketFactory();
		            ((HttpsURLConnection)conn).setSSLSocketFactory(ssf);
				}
				conn.setRequestProperty( "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 Safari/537.36" );
				responseHeaders = conn.getHeaderFields();
				return conn.getInputStream();
			}
			else {
				URL u = new URL( url );
				URLConnection conn = u.openConnection();
				if( timeout > 0 ) conn.setReadTimeout( timeout * 1000 );
				if( url.toLowerCase().startsWith( "https" ) ) {
		            TrustManager[] tm = { new MyX509TrustManager() };
		            SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
		            sslContext.init(null, tm, new java.security.SecureRandom());
		            // 从上述SSLContext对象中得到SSLSocketFactory对象SSLSocketFactory
		            SSLSocketFactory ssf = sslContext.getSocketFactory();
		            ((HttpsURLConnection)conn).setSSLSocketFactory(ssf);
				}
				conn.setDoOutput(true);  
	            conn.setDoInput(true);  
	            conn.setUseCaches(false);  
				conn.setRequestProperty( "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 Safari/537.36" );
	            conn.setRequestProperty("connection", "Keep-Alive");  
	            if( requestContentType != null ) {
	            	conn.setRequestProperty( "Content-Type", requestContentType );
	            }
	            /*conn.setRequestProperty("charset", paramsEncoding );
	            PrintWriter out = new PrintWriter( conn.getOutputStream() );  
	            out.print( params );  
	            out.flush();*/
	            if( headers != null ) {
	            	Iterator it = headers.keySet().iterator();
	            	while( it.hasNext() ) {
	            		String propName = (String)it.next();
	            		String propValue = (String) headers.get( propName );
	            		conn.setRequestProperty( propName, propValue );
	            	}
	            }
	            if( params != null ) {
		            OutputStream os = conn.getOutputStream();
		            //jdk中调用conn.getOutputStream()方法时，会强行将传参方法改为post，要使用get方法传参，则要使用下面这段反射程序将conn的方法修改为get
		            if( "GET".equalsIgnoreCase( sendParamMethod ) ) {
		            	List<Field> fields = new ArrayList<Field>() ;
		                Class tempClass = conn.getClass();
		                while( tempClass != null ) {//当父类为null的时候说明到达了最上层的父类(Object类).
		                    fields.addAll(Arrays.asList(tempClass.getDeclaredFields()));
		                    tempClass = tempClass.getSuperclass(); //得到父类,然后赋给自己
		                }
		                for (Field field : fields) {
		                    if("method".equals(field.getName())){
		                        field.setAccessible(true);
		                        field.set(conn,"GET");
		                    }
		                }
		            }
		            os.write( params.getBytes( paramsEncoding ) );
		            os.flush();
	            }
				responseHeaders = conn.getHeaderFields();
	            return conn.getInputStream();
			}
		}
		catch( Throwable t ) {
			throw new RQException( t );
		}
	}
	
	/**
	 * 获得url的内容字符串
	 * @param charset 内容的字符集, 如GBK，UTF-8
	 * @return
	 */
	public String getContentString( String charset ) {
		try {
			if( charset == null || charset.length() == 0 ) charset = "UTF-8";
			InputStream is = getInputStream();
			BufferedReader br = new BufferedReader( new InputStreamReader( is, charset ) );
			StringBuffer res = new StringBuffer();
			String tmp = null;
			while( ( tmp = br.readLine() ) != null ) {
				if( res.length() > 0 ) res.append( "\n" );
				res.append( tmp ); 
			}
			return res.toString();
		}
		catch( Throwable t ) {
			throw new RQException( t );
		}
	}
	
	public String getResponseHeader( String key ) {
		if( responseHeaders == null ) return null;
		List<String> vs = responseHeaders.get(key);
		if( vs == null ) return null;
		String value = "";
		for( int i = 0; i < vs.size(); i++ ) {
			if( value.length() > 0 ) value += ";";
			value += vs.get( i );
		}
		return value;
	}

	public JSONObject getResponseHeaders() {
		JSONObject jo = new JSONObject(); 
		if( responseHeaders == null ) return jo;
		Iterator it = responseHeaders.keySet().iterator();
		while( it.hasNext() ) {
			String key = (String)it.next();
			try {
				jo.put( key, getResponseHeader( key ) );
			}catch( Throwable t){}
		}
		return jo;
	}
	
	public OutputStream getOutputStream(boolean isAppend) {
		throw new RQException( "httpfile is readonly!" );
	}

	public boolean exists() {
		try {
			size();
			return true;
		}
		catch( Throwable t ) {
			throw new RQException( t );
		}
	}

	public long size() {
		try {
			URL u = new URL( url );
			return u.openConnection().getContentLength();
		}
		catch( Throwable t ) {
			throw new RQException( t );
		}
	}

	public long lastModified() {
		return 0;
	}

	public boolean delete() {
		return false;
	}

	public boolean move(String path, String opt) {
		return false;
	}

	public String createTempFile(String prefix) {
		return null;
	}

	public RandomOutputStream getRandomOutputStream(boolean isAppend) {
		// TODO Auto-generated method stub
		throw new RQException( "不能写http文件" );
		//return null;
	}
	
	private class MyX509TrustManager implements X509TrustManager {
	    // 检查客户端证书
	    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	    }

	    // 检查服务器端证书
	    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	    }

	    // 返回受信任的X509证书数组
	    public X509Certificate[] getAcceptedIssuers() {
	        return null;
	    }
	}

	public static void main( String[] args ) {
		try {
			HttpFile hf = new HttpFile( "http://localhost:7080/raqsoftReport/reportJsp/showReport.jsp?rpx=2.rpx" );
			//hf.setPostParams( "arg1=中国&arg2=真行啊！", "UTF-8" );
			System.out.println( hf.getContentString( "UTF-8" ) );
		}
		catch( Throwable t ) {
			t.printStackTrace();
		}
	}

	public boolean deleteDir() {
		return true;
	}

	/**
	 * 取随机访问文件对象，如果不支持则返回null
	 * @return RandomAccessFile
	 */
	public RandomAccessFile getRandomAccessFile() {
		return null;
	}
}
