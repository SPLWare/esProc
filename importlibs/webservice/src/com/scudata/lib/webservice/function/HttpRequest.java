package com.scudata.lib.webservice.function;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import com.scudata.common.Logger;

public class HttpRequest {
    /**
     * 向指定URL发送GET方法的请求
     * 
     * @param url
     *            发送请求的URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, String param) {
        String result = "";
        BufferedReader in = null;
        try {
        	if (param != null && param != "") param += "?" + param;
            String urlNameString = url + param;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            connection.connect();
//            // 获取所有响应头字段
//            Map<String, List<String>> map = connection.getHeaderFields();
//            // 遍历所有的响应头字段
//            for (String key : map.keySet()) {
//                System.out.println(key + "--->" + map.get(key));
//            }
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            Logger.error(e.getMessage());
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

    /**
     * 向指定 URL 发送POST方法的请求
     * 
     * @param url
     *            发送请求的 URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param) {
    	OutputStreamWriter osw = null;
        InputStream in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            conn.setRequestProperty("Content-Type", "text/xml;charset:UTF-8");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            osw = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            // 发送请求参数
            osw.write(param);
            // flush输出流的缓冲
            osw.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = conn.getInputStream();
            byte bs[] = new byte[in.available()];
            in.read(bs);
            result = new String(bs,"UTF-8");
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            Logger.error(e.getMessage());
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(osw!=null){
                    osw.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }

    public static void main(String[] args) {
        //发送 GET 请求
//        String s=HttpRequest.sendGet("http://ip.taobao.com/service/getIpInfo.php?ip=223.72.74.217", "");
//        System.out.println(s);

    	for (int i=0; i<1000; i++) {
            String s=HttpRequest.sendGet("http://www.nedvr.com/c?action=2&oper=checkNed&deviceId=11", "");
            System.out.println(i + s);
    	}

//        //发送 POST 请求
//        String sr=HttpRequest.sendPost("http://localhost:8080/solr/lifetraces/update?commit=true", "<add><doc><field name=\"eid\">7</field><field name=\"pid\" update=\"set\">1</field><field name=\"tid\" update=\"set\">8</field><field name=\"type\" update=\"set\">1</field><field name=\"content\" update=\"set\">中国日本美国</field></doc></add>");
//        System.out.println(sr);
    	
      //发送 POST 请求
//      String sr=HttpRequest.sendPost("http://localhost:8008/solr/lifetraces/update?commit=true", "<delete><query>eid:*</query></delete>");
//      System.out.println(sr);
  	
//        //发送 POST 请求
//        try {
//        	String url = "http://localhost:8080/solr/lifetraces/select?wt=xml&indent=true&q=eid:7+tid:8";//+URLEncoder.encode("*", "UTF-8");
//        	System.out.println(url);
//			String sr2=HttpRequest.sendGet(url, "");
//			System.out.println(sr2);
//		} catch (Exception e) {
//			Logger.error(e.getMessage());
//		}

    }
}