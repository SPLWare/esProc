package com.raqsoft.lib.kafka.function;

import java.util.ArrayList;
import java.util.List;

import com.raqsoft.dm.Sequence;
/*
 * ImPoll(fd, timeout)
 * 
 */
public class ImPoll extends ImFunction {	 
	public Object doQuery(Object[] objs) {
		try {			
			boolean bCursor = false;
			if (m_conn.m_bClose) return null;
			if (option!=null){
				if (option.indexOf("c")!=-1){
					bCursor = true;
				}
			}
			List<Integer> partitions = new ArrayList<Integer>();
			long timeout = 1000;
			if (objs!=null ){
				if (objs.length>1){
					if(objs[1] instanceof Integer){
						partitions.add((Integer)objs[1]);
					}else if(objs[1] instanceof Sequence){
						Sequence seq = (Sequence)objs[1];
						for(int i=0; i<seq.length(); i++){
							partitions.add((Integer)seq.get(i+1));
						}
					}
				}
				timeout = Integer.parseInt(objs[0].toString());
			}
			int nPartSize = -1;
			if (m_conn.isCluster()){
				nPartSize = partitions.size();
				m_conn.m_cols = new String[]{"partition", "offset", "key", "value"};
				m_conn.initConsumerCluster(partitions);
			}else{
				m_conn.initConsumer();
			}
			
			if (bCursor){
				return new ImCursor(m_ctx, m_conn, timeout, nPartSize);
			}else{
				List<Object[]> ls = null;
				if (m_conn.isCluster()){
					ls = ImFunction.getClusterData(m_conn, timeout, partitions.size());
				}else{
					ls = ImFunction.getData(m_conn, timeout);
				}
		        return ImFunction.toTable(m_conn.m_cols, ls);
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
}