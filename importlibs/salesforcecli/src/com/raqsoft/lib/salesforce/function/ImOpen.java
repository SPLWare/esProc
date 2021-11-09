package com.raqsoft.lib.salesforce.function;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import com.alibaba.fastjson.JSON;
import com.raqsoft.common.RQException;

public class ImOpen extends ImFunction {
	private String m_loginurl = "https://login.salesforce.com";
	private String m_granttype = "/services/oauth2/token?grant_type=password";
	private String m_clientid = "3MVG9fe4g9fhX0E66lsoDv6kDPNDZPhVxCzgBevUKqP3w5701AUnOfb9xYfSpHL_IOxphtw1qr4yl2rHAmXTX";
	private String m_clientsecret = "4D3DBDFD2E7F7B169EC6DD6BFAF5CAAB3C219D62263AB531FCBD43077B52ECAC";
	private String m_userid = "";
	private String m_password = "";
	private String m_accesstoken = "access_token";
	private String m_instanceurl = "instance_url";

	public String m_instanceUrl;
	public Header m_oAuthHeader;
	public Header m_printHeader = new BasicHeader("X-PrettyPrint", "1");
	public HttpPost m_httpPost = null;

	public com.alibaba.fastjson.JSONObject getParamsFromFile(String fileName) {
		com.alibaba.fastjson.JSONObject ret = null;
		try {
			// 1. userconfig.xml
			File f = null;			
			if (fileName==null || fileName.isEmpty()) {
				String appHome = "";
				URL url = ImOpen.class.getClassLoader().getResource("");
				f = new File(url.toURI());
				String fName = f.getPath();

				if (fName.endsWith((File.separator+"bin"))) {
					appHome = fName;
				}else if(fName.endsWith("esProc")) {
					appHome= fName + File.separator+ "extlib/SalesforceCli";
				}
				
				fileName = appHome + File.separator+ "user.json";
				//System.out.println("fName2 = "+fileName);
			}

			f = new File(fileName);
			if (!f.exists()){
				throw new RQException("user.json is not in SalesforceCli");
			}
			String jsonContent = ImUtils.readFile(fileName);	
			Object o = JSON.parse(jsonContent);
			//String name = o.getClass().getName();
			if (o instanceof com.alibaba.fastjson.JSONObject) {
				ret = (com.alibaba.fastjson.JSONObject)o;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public void doParams(String fileName) {
		com.alibaba.fastjson.JSONObject json = getParamsFromFile(fileName);
		if (json.containsKey("LOGINURL")){
			m_loginurl = json.getString("LOGINURL");
		}
		if (json.containsKey("GRANTTYPE")){
			m_granttype = json.getString("GRANTTYPE");
		}
		if (json.containsKey("CLIENTID")){
			m_clientid = json.getString("CLIENTID");
		}
		if (json.containsKey("CLIENTSECRET")){
			m_clientsecret = json.getString("CLIENTSECRET");
		}
		if (json.containsKey("USERID")){
			m_userid = json.getString("USERID");
		}
		if (json.containsKey("PASSWORD")){
			m_password = json.getString("PASSWORD");
		}
		if (json.containsKey("ACCESSTOKEN")){
			m_accesstoken = json.getString("ACCESSTOKEN");
		}
		
		if (json.containsKey("INSTANCEURL")){
			m_instanceurl = json.getString("INSTANCEURL");
		}
		// System.out.println(json);
	}
	
	public void init(String fileName) {
		doParams(fileName);
		HttpClient httpclient = HttpClientBuilder.create().build();
		
		String loginURL = m_loginurl + m_granttype + "&client_id=" + m_clientid + "&client_secret=" + m_clientsecret
						  + "&username=" + m_userid + "&password=" + m_password;
		//System.out.println(loginURL);
		m_httpPost = new HttpPost(loginURL);
		HttpResponse httpResponse = null;

		try {
			httpResponse = httpclient.execute(m_httpPost);
		} catch (ClientProtocolException clientProtocolException) {
			clientProtocolException.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		final int statusCode = httpResponse.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			System.out.println("Error authenticating to Salesforce.com platform: " + statusCode);
			return;
		}

		String httpMessage = null;
		try {
			httpMessage = EntityUtils.toString(httpResponse.getEntity());
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

		JSONObject jsonObject = null;
		String accessToken = null;
		try {
			jsonObject = (JSONObject) new JSONTokener(httpMessage).nextValue();
			accessToken = jsonObject.getString(m_accesstoken);
			m_instanceUrl = jsonObject.getString(m_instanceurl);
			//System.out.println("accessToken:" + accessToken);
			//System.out.println("instanceUrl:" + m_instanceUrl);
		} catch (JSONException jsonException) {
			jsonException.printStackTrace();
		}

		m_oAuthHeader = new BasicHeader("Authorization", "OAuth " + accessToken);
	}

	public Object doQuery(Object[] objs){
		String fileName = null;
		if (objs==null){
			
		}else if(objs.length==1 && objs[1] instanceof String) {
			fileName = objs[0].toString();
		}
		
		init(fileName);
		
		return this;
	}
}
