package com.raqsoft.lib.elastic.function;

import java.io.IOException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import com.raqsoft.common.RQException;
/*
 * ImPost(index, type, doc)
 * 
 */
public class ImPost extends ImFunction {	 
	public Object doQuery(Object[] objs) {
		try {
			if (objs.length < 1){
				throw new RQException("elastic function.paramTypeError");
			}
			// check param: cursor
			boolean bCursor = false;
			if (option!=null){
				if (option.indexOf("c")!=-1){
					bCursor = true;
				}
			}
			
			String method = "POST";
			String endpoint = objs[0].toString();
			int subModel = getSubModel(method, endpoint);
			
			if (bCursor){
				return new ImCursor(m_ctx, m_restConn, objs);
			}else{
				Response response = doRun( method, endpoint, objs);
				if(response!=null){
					String result = EntityUtils.toString(response.getEntity());
					return parseResponse(result, subModel);	
				}
			}		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	
}