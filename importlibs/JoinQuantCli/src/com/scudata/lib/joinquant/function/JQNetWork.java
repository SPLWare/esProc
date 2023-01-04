package com.scudata.lib.joinquant.function;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import net.sf.json.JSONObject;

public class JQNetWork{
	public static String mToken = null;
	public static String mExecurl = "https://dataapi.joinquant.com/apis"; // 请求URL
	
	public static String GetNetData( Map<String, Object> paramMap) throws Exception {
		if (paramMap==null || paramMap.size()<1) return null;
		return SentPostBody(mExecurl, paramMap);
	}

	public static String[] GetNetArrayData( Map<String, Object> paramMap) throws Exception {
		String ret = GetNetData(paramMap);
		if (ret!=null && ret.trim()!="") {
			return ret.split("\n");
		}
		return null;
	}
	
	public static String[] GetNetArrayData( String jsonStr) throws Exception {
		String ret = SentPostBody(mExecurl, jsonStr);
		if (ret!=null && ret.trim()!="") {
			return ret.split("\n");
		}
		return null;
	}
	
	public static String SentPostBody(String execurl, Map<String, Object> paramMap) {
		String s = JSONObject.fromObject(paramMap).toString();
		return SentPostBody(execurl, s);
	}
	
	public static String SentPostBody(String execurl, String jsonStr) {
		OutputStreamWriter out = null;
		InputStream is = null;
		try {
			URL url = new URL(execurl);// 创建连接
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setUseCaches(false);
			connection.setInstanceFollowRedirects(true);
			connection.setRequestMethod("POST"); // 设置请求方式
			connection.setRequestProperty("Accept", "application/json"); // 设置接收数据的格�?
			connection.setRequestProperty("Content-Type", "application/json"); // 设置发�?�数据的格式
			connection.setRequestProperty("Accept-Encoding", "identity");
			connection.setRequestProperty("User-Agent", " Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36");
			connection.connect();
			out = new OutputStreamWriter(connection.getOutputStream()); // utf-8编码
			out.append(jsonStr);
			out.flush();
			out.close();
			
			// 读取响应
			is = connection.getInputStream();
		
			byte[] data = null;
			int length = (int) connection.getContentLength();
			if (length == -1) {
				length = 1024*512;
			}
			
			data = new byte[length];
			byte[] temp = new byte[1024*10];
			int readLen = 0;
			int destPos = 0;
			while ((readLen = is.read(temp)) > 0) {
				if (destPos+readLen>length) {
					length = length*2;
					byte[] newData = new byte[length];
					System.arraycopy(data, 0, newData, 0, destPos);
					data = null;
					data = newData;
					System.out.println("length="+length);
				}
				System.arraycopy(temp, 0, data, destPos, readLen);
				destPos += readLen;
			}
			String result = new String(data, "utf-8");
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return "";
	}
}
