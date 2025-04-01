package com.scudata.lib.webservice.function;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import com.scudata.common.Logger;

public class Http {
    public static String httpsRequest(String requestUrl, String requestMethod, String outputStr) {    
        try {    
            URL url = new URL(requestUrl);    
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();    
              
            conn.setDoOutput(true);    
            conn.setDoInput(true);    
            conn.setUseCaches(false);    
            // 设置请求方式（GET/POST）    
            conn.setRequestMethod(requestMethod);    
            conn.setRequestProperty("content-type", "text/xml; charset=UTF-8");
            // 当outputStr不为null时向输出流写数据    
            if (null != outputStr) {    
                OutputStream outputStream = conn.getOutputStream();    
                // 注意编码格式    
                outputStream.write(outputStr.getBytes("UTF-8"));    
                outputStream.close();    
            }    
            // 从输入流读取返回内容    
            InputStream inputStream = conn.getInputStream();    
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");    
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);    
            String str = null;  
            StringBuffer buffer = new StringBuffer();    
            while ((str = bufferedReader.readLine()) != null) {    
                buffer.append(str);    
            }    
            // 释放资源    
            bufferedReader.close();    
            inputStreamReader.close();    
            inputStream.close();    
            inputStream = null;    
            conn.disconnect();    
            return buffer.toString();    
        } catch (Exception e) {    
            System.out.println("http请求异常：{}"+ e);
            Logger.error(e.getMessage());
        }    
        return null;    
      }    


//    public synchronized static String accessService(String wsdl,String content,String contentType)throws Exception{    
//        //拼接参数    
//        String soapResponseData = "";    
//        //拼接SOAP    
//        PostMethod postMethod = new PostMethod(wsdl);    
//        // 然后把Soap请求数据添加到PostMethod中    
//        byte[] b=null;    
//        InputStream is=null;    
//        try {    
//            b = content.getBytes("utf-8");     
//            is = new ByteArrayInputStream(b, 0, b.length);    
//            RequestEntity re = new InputStreamRequestEntity(is, b.length,contentType);    
//            postMethod.setRequestEntity(re);    
//            HttpClient httpClient = new HttpClient();    
//            //methods
//            int status = httpClient.executeMethod(postMethod);    
//            System.out.println("status:"+status);    
//            if(status==200){ 
//            	return postMethod.getResponseBodyAsString();
//            }    
//        } catch (Exception e) {    
//            Logger.error(e.getMessage());    
//        } finally{    
//            if(is!=null){    
//                is.close();    
//            }    
//        }    
//        return soapResponseData;    
//    }    

}
