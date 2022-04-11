package com.scudata.lib.influx.function;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class InfluxRest extends Function {
	
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
			MessageManager mm = EngineMessage.get();
			throw new RQException("influx rest param error" + mm.getMessage(Integer.toString(param.getSubSize())));
		}		
		
		try {
			return downLoadFromUrl(url,charset,method,content,headers);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
    public static Table  downLoadFromUrl(String urlStr,String charset,String method,String content, ArrayList<String> headers) throws IOException{
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

}
