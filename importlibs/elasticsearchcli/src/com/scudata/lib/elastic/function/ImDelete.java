package com.scudata.lib.elastic.function;

import java.io.IOException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import com.scudata.common.RQException;
import com.scudata.common.Logger;

/*
 * ImDelete(index, type, doc)
 * 
 */
public class ImDelete extends ImFunction {	 
	public Object doQuery(Object[] objs) {
		try {
			if (objs.length<1)
				throw new RQException("params should be endpoint entity");
			
			String method = "DELETE";
			String endpoint = objs[0].toString();
			int subModel = getSubModel(method, endpoint);

			
			Response response = doRun( method, endpoint, objs);
			if(response!=null){
				String result = EntityUtils.toString(response.getEntity());
				return parseResponse(result, subModel);	
			}			
		} catch (IOException e) {
			Logger.error(e.getMessage());
		}
		return null;
	}
	
}