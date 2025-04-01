package com.scudata.lib.elastic.function;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class Http4SPL {
	
    public static Table  downLoadFromHttp(String urlStr,String charset,String method,String content, ArrayList<String> headers) throws IOException{
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();

        conn.setConnectTimeout(3*1000);
        conn.setRequestMethod(method);

        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        for (int i=0; i<headers.size(); i++) {
        	String ss[] = headers.get(i).split(":");
        	conn.setRequestProperty(ss[0].trim(), ss[1].trim());
        }
        if (content!=null) {
            conn.setDoOutput(true);
        	conn.getOutputStream().write(content.getBytes());
        }
        


        InputStream inputStream = conn.getInputStream();

        byte[] getData = readInputStream(inputStream);

        ArrayList<String> names = new ArrayList<String>();
        ArrayList<String> al = new ArrayList<String>();
        
        names.add("Message");
        al.add(conn.getResponseMessage());
        Map<String, List<String>> map = conn.getHeaderFields();
        
		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			//rm += "\n"+("Key : " + entry.getKey() + " ,Value : " + entry.getValue());
	        if (entry.getKey() == null || entry.getKey().length() == 0) continue;
			names.add(entry.getKey());
	        String ss = "";
	        for (int i=0; i<entry.getValue().size(); i++) {
	        	if (i>0) ss += ";";
	        	ss += entry.getValue().get(i);
	        }
	        al.add(ss);
		}

		if(inputStream!=null){
            inputStream.close();
        }

		names.add("Content");
        al.add(new String(getData,charset));
        
        Table t = new Table(names.toArray(new String[names.size()]));
        t.insert(0, al.toArray(new String[names.size()]));
        
        return t;

    }

	
    public static Table  downLoadFromHttps(String urlStr,String charset,String method,String content, ArrayList<String> headers) throws Exception{
        ignore();
    	
    	URL url = new URL(urlStr);
        HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();

        conn.setConnectTimeout(3*1000);
        conn.setRequestMethod(method);

        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        for (int i=0; i<headers.size(); i++) {
        	String ss[] = headers.get(i).split(":");
        	conn.setRequestProperty(ss[0].trim(), ss[1].trim());
        }
        if (content!=null) {
            conn.setDoOutput(true);
        	conn.getOutputStream().write(content.getBytes());
        }
        
        conn.connect();
        


        InputStream inputStream = conn.getInputStream();

        byte[] getData = readInputStream(inputStream);

        ArrayList<String> names = new ArrayList<String>();
        ArrayList<String> al = new ArrayList<String>();
        
        names.add("Message");
        al.add(conn.getResponseMessage());
        Map<String, List<String>> map = conn.getHeaderFields();
        
		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			//rm += "\n"+("Key : " + entry.getKey() + " ,Value : " + entry.getValue());
	        if (entry.getKey() == null || entry.getKey().length() == 0) continue;
			names.add(entry.getKey());
	        String ss = "";
	        for (int i=0; i<entry.getValue().size(); i++) {
	        	if (i>0) ss += ";";
	        	ss += entry.getValue().get(i);
	        }
	        al.add(ss);
		}

		if(inputStream!=null){
            inputStream.close();
        }

		names.add("Content");
        al.add(new String(getData,charset));
        
        Table t = new Table(names.toArray(new String[names.size()]));
        t.insert(0, al.toArray(new String[names.size()]));
        
        return t;

    }

    
    /**
     * 
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

    
    public static void ignore() throws NoSuchAlgorithmException, KeyManagementException {
        // 自定义证书校验器
        TrustManager[] trustAllCerts = new TrustManager[] {
          new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, 
                                           String s) throws CertificateException {}
            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, 
                                           String s) throws CertificateException {}
            @Override
            public X509Certificate[] getAcceptedIssuers() { 
                   return new X509Certificate[0]; 
            }
          }
        };
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // 自定义hostname校验器
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

}
