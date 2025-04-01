package com.scudata.lib.kafka.function;

import java.util.Map;
import java.util.HashMap;
import com.scudata.common.Logger;
import com.scudata.dm.Sequence;

/* //pX为分区号, tX为分区的offset,
 * ImOffset(fd, offset, [p1,p2,...]) 
 * ImOffset(fd, [t1,t2,...]) 
 * 只有一个topic,统一转换成map<partSN, partOffset>格式.
 */
public class ImOffset extends ImFunction {	
	
	public Object doQuery(Object[] objs) {
		try {			
			if (m_conn.m_bClose) {
				Logger.warn("connect is close.");
				return false;
			}
			if (objs==null ){
				Logger.warn("paramSize is not zero");
				return false;
			}
			
			int nOffset = 0;
			Sequence seq = null;
			Map<Integer, Integer> map = new HashMap<Integer, Integer>();
			
			//ImOffset(fd, [t1,t2,...]) 
			if ( objs.length==1 && objs[0] instanceof Sequence ){
				seq = (Sequence)objs[0];
				for(int i=1; i<=seq.length(); i++){
					map.put(i-1, seq.get(i)==null?-1:(Integer)seq.get(i));
				}				
			}else{ //ImOffset(fd, offset, [p1,p2,...]) 				
				if ( objs.length>=1 && objs[0] instanceof Integer ){
					nOffset = (Integer)objs[0];
				}else{
					Logger.error("ParamType is not Integer");
				}
				
				if (objs.length>=2 && objs[1] instanceof Sequence){
					seq = (Sequence)objs[1];					
			        for(int i=1; i<=seq.length(); i++){
			        	map.put((Integer)seq.get(i), nOffset);
			        }
				}else if(objs.length>=2){
					Logger.error("ParamType is not Sequence");
				}
			}
			
			if (map.size()>0){
				m_ctx.setParamValue("offset_val", map);
			}else{
				m_ctx.setParamValue("offset_val", nOffset);
			}
			
	        return true;	        
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		
		return false;
	}
	
}