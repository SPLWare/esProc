package com.raqsoft.lib.elastic.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.IResource;
import net.sf.json.JSONObject;

public class RestConn implements IResource {
	public String m_version="1.50";
	public RestClient m_restClient = null;

	public RestConn(Context ctx, Map<String, Object> map) {
		ctx.addResource(this);
		if (map.containsKey("user")) {
			getRestClientWithUser(map);
		} else {
			getRestClient(map);
		}
		
		getServerVer();
	}

	public void close() {
		// TODO Auto-generated method stub
		try {
			if (m_restClient!=null){
				m_restClient.close();
				m_restClient = null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void getRestClientWithUser(Map<String, Object> map) {
		String user = map.get("user").toString();
		String pwd = map.get("passwd").toString();
		
		final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user,pwd));
		HttpHost[] hosts = getHosts(map);
		m_restClient = RestClient.builder(hosts)
           .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {  
               @Override  
               public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {  
                   return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);  
               }  
           }).build();  
	}

	private void getRestClient(Map<String, Object> map) {
		HttpHost[] hosts = getHosts(map);
		m_restClient = RestClient.builder(hosts).build();
	}
	
	private void getHostByUrl( String url, List<HttpHost> ls, boolean bScheme){
		if (url==null) return;
		String host = url.replace("https://", "");
		host = host.replace("http://", "");
		String kv[] = host.split(":");
		
		int port = 9200;
		if (kv.length==2){
			port = Integer.parseInt(kv[1]);
		}
		String scheme = bScheme?"https":"http";
		HttpHost h = new HttpHost(kv[0], port, scheme);
		ls.add(h);
	}
	
	private HttpHost[] getHosts( Map<String, Object> map){
		String s = "host";
		boolean bScheme = false;
		List<HttpHost> ls = new ArrayList<HttpHost>();

		//String scheme = "http";
		if (map.containsKey("scheme")){
			 bScheme = true;
		}
		for(int i=0; i<map.size(); i++){
			String key = s+(i+1);
			if (map.containsKey(key)){
				String url = map.get(key).toString();
				//System.out.println(url);
				if (!bScheme && url.indexOf("https")>-1){ //for https
					getHostByUrl(url, ls, true);
				}else if (bScheme && url.indexOf("http")>-1){ //for https
					getHostByUrl(url, ls, false);
				}else if (bScheme){//for https
					getHostByUrl(url, ls, bScheme);
				}else{//for http
					getHostByUrl(url, ls, false);
				}
			}
		}
		
		HttpHost []hosts = new HttpHost[ls.size()];
		ls.toArray(hosts);
		
		return hosts;
	}

	private void getServerVer() {
		try {
			Request request = new Request("GET",  "/");   
			Response response = m_restClient.performRequest(request);
			String ret = EntityUtils.toString(response.getEntity());  
			JSONObject job = new JSONObject(ret);
			String ver = job.getJSONObject("version").getString("number");
			m_version = ver.substring(0, ver.lastIndexOf("."));
			//System.out.println(ret);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}