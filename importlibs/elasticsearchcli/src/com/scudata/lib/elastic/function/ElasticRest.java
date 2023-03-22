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
import com.scudata.common.Logger;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class ElasticRest extends Function {
	
	protected String url;
	protected String charset="UTF-8";
	protected String method;
	protected String content;
	protected ArrayList<String> headers = new ArrayList<String>();
	protected Context m_ctx;
	
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}

		return this;
	}
	
	//url:charset,method,content;header1,header2....
	//hbase_rest("http://example. com:8000/table/scanner/","POST/GET/PUT/DELETE","UTF-8","xml or json","Accept: text/xml","Content-Type: text/xml")	
	public Object calculate(Context ctx) {
		m_ctx = ctx;
		IParam param = this.param;
		try {
			if (param.getType()==';'){	
				ArrayList<Expression> list1 = new ArrayList<Expression>();
				ArrayList<Expression> list2 = new ArrayList<Expression>();
				param.getSub(0).getAllLeafExpression(list1);
				param.getSub(1).getAllLeafExpression(list2);
				
				IParam param0 = param.getSub(0);
				IParam param00 = param0.getSub(0);
				if (param00.isLeaf()) {
					url = param00.getLeafExpression().calculate(ctx).toString();
				} else {
					url = param00.getSub(0).getLeafExpression().calculate(ctx).toString();
					charset = param00.getSub(1).getLeafExpression().calculate(ctx).toString();
				}
				method = param0.getSub(1).getLeafExpression().calculate(ctx).toString();
				if (param0.getSubSize()>2) content = param0.getSub(2).getLeafExpression().calculate(ctx).toString();
				
				IParam param1 = param.getSub(1);
				if (param1.isLeaf()) {
					headers.add(param1.getLeafExpression().calculate(ctx).toString());
				} else {
					for (int i=0; i<param1.getSubSize(); i++)
						headers.add(param1.getSub(i).getLeafExpression().calculate(ctx).toString());
				}
			}else if (param.getType()==','){
				IParam param00 = param.getSub(0);
				if (param00.isLeaf()) {
					url = param00.getLeafExpression().calculate(ctx).toString();
				} else {
					url = param00.getSub(0).getLeafExpression().calculate(ctx).toString();
					charset = param00.getSub(1).getLeafExpression().calculate(ctx).toString();
				}
				method = param.getSub(1).getLeafExpression().calculate(ctx).toString();
				if (param.getSubSize()>2) content = param.getSub(2).getLeafExpression().calculate(ctx).toString();
			}
		} catch (Exception e) {
			Logger.error(e.getMessage());
			MessageManager mm = EngineMessage.get();
			throw new RQException("influx rest param error" + mm.getMessage(Integer.toString(param.getSubSize())));
		}		
		
		try {
			if (url.indexOf("https:")>=0) return Http4SPL.downLoadFromHttps(url,charset,method,content,headers);
			else return Http4SPL.downLoadFromHttp(url,charset,method,content,headers);
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		return null;
	}
}
