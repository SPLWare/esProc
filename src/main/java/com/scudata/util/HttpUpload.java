package com.scudata.util;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.scudata.common.RQException;

//向服务器端的url上载文件
public class HttpUpload {
	
	//上载文件的url
	private String url;
	
	//上载时要传送的参数
	private Hashtable<String,String> params;
	
	//上载的文件参数名
	private ArrayList<String> fileArgs;
	
	//上载的文件路径
	private ArrayList<String> filePaths;
	
	//上载的文件字节
	private ArrayList<byte[]> fileBytes;
	
	//上载后返回结果内容的编码
	private String resultEncoding = "UTF-8";  
	
	//上载时要传送的Header参数
	private Hashtable<String,String> headers;

	public HttpUpload( String url ) {
		this.url = url;
		params = new Hashtable<String,String>();
		fileArgs = new ArrayList<String>();
		filePaths = new ArrayList<String>();
		fileBytes = new ArrayList<byte[]>();
		headers = new Hashtable<String,String>();
	}
	
	/**
	 * 设置返回内容的编码
	 * @param encoding
	 */
	public void setResultEncoding( String encoding ) {
		this.resultEncoding = encoding;
	}
	
	/**
	 * 添加一个参数
	 * @param paramName
	 * @param paramValue
	 */
	public void addParam( String paramName, String paramValue ) {
		params.put( paramName, paramValue );
	}
	
	/**
	 * 设置一个Header参数
	 * @param headerName
	 * @param headerValue
	 */
	public void setHeader( String headerName, String headerValue ) {
		headers.put( headerName, headerValue );
	}
	
	/**
	 * 添加一个上载文件
	 * @param fileArg    文件参数名
	 * @param filePath   文件路径
	 */
	public void addFile( String fileArg, String filePath ) {
		fileArgs.add( fileArg );
		filePaths.add( filePath );
		fileBytes.add( null );
	}
	
	/**
	 * 添加一个上载文件
	 * @param fileArg   文件参数名
	 * @param b         文件字节
	 */
	public void addFile( String fileArg, byte[] b ) {
		fileArgs.add( fileArg );
		filePaths.add( "noname" );
		fileBytes.add( b );
	}
	
	/**
	 * 实现文件上载
	 * @return    返回上载结果
	 * @throws Throwable
	 */
	public String upload() {
		CloseableHttpClient httpClient = null;
		HttpPost httpPost;
		String result = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			httpPost = new HttpPost( url );
			Enumeration<String> emh = headers.keys();
			while( emh.hasMoreElements() ) {
				String header = emh.nextElement();
				String hv = headers.get( header );
				httpPost.setHeader( header, hv );
			}
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setCharset( Charset.forName( "UTF-8" ) );//设置请求的编码格式
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);//设置浏览器兼容模式
			for( int i = 0; i < fileArgs.size(); i++ ) {
				byte[] b = fileBytes.get( i );
				if( b == null ) {
					builder.addBinaryBody( fileArgs.get( i ), new File( filePaths.get( i ) ) );
				}
				else {
					builder.addBinaryBody( fileArgs.get( i ), b, ContentType.create( HTTP.CONTENT_TYPE ), "noname" );
				}
			}
			Enumeration<String> em = params.keys();
			while( em.hasMoreElements() ) {
				String arg = em.nextElement();
				builder.addPart( arg, new StringBody( params.get( arg ), ContentType.create( HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8) ) );//设置请求参数
			}
			HttpEntity entity = builder.build();// 生成 HTTP POST 实体  	
			httpPost.setEntity( entity ); //设置请求实体
			HttpResponse response = httpClient.execute(httpPost);
			if( null != response && response.getStatusLine().getStatusCode() == 200) {
				HttpEntity resEntity = response.getEntity();
				if( null != resEntity ) {
					result = EntityUtils.toString( resEntity, resultEncoding );
				}
			}
		}
		catch( Throwable t ) {
			throw new RQException( t );
		}
		finally {
			try{ httpClient.close(); }catch( Exception e ){}
		}
		return result;
	}
	
}
