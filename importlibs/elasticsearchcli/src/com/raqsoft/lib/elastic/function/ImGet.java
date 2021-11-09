package com.raqsoft.lib.elastic.function;

import java.io.IOException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import com.raqsoft.common.RQException;

/*
 * ImGet(index, type, doc)
 * 
 */
public class ImGet extends ImFunction {	 
	public Object doQuery(Object[] objs) {
		try {
			if (objs.length < 1){
				throw new RQException("es_get function.missingParam");
			}
			
			boolean bCursor = false;
			if (option!=null){
				if (option.indexOf("c")!=-1){
					bCursor = true;
				}
			}
			
			String method = "GET";
			String endpoint = objs[0].toString();
			String info = endpoint.replace("?v", "");
			info = info.replaceFirst("/", "");
			String []vals = info.split("/");
			
			int subModel = 0;
			if(vals.length==2){
				subModel = getSubModel(vals[0], vals[1]);
			}else if(vals.length==4){
				subModel = getSubModel(method, vals[3]);
			}else{
				subModel = getSubModel(method, endpoint);
			}

			if (bCursor){
				return new ImCursor(m_ctx, m_restConn, objs);
			}else{
				Response response = doRun( method, endpoint, objs);
				if(response!=null){
					String result = EntityUtils.toString(response.getEntity());
					Object o = parseResponse(result, subModel);
					return o;//parseResponse(result, subModel);	
				}
			}		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
}