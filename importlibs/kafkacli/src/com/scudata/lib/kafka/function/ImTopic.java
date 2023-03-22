package com.scudata.lib.kafka.function;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import com.scudata.common.Logger;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/*
 * ImTopic(fd)
 * 
 */
public class ImTopic extends ImFunction {	
	
	public Object doQuery(Object[] objs) {
		try {			
			if (m_conn.m_bClose) return null;
			Properties props = new Properties();
			String url = m_conn.getPropertyValue("bootstrap.servers");
		    props.put("bootstrap.servers", url);
//		    String url = m_conn.getPropertyValue("zookeeper.connect");
//		    props.put("zookeeper.connect", url);		    
		    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		    
			final KafkaConsumer<Object, Object> consumer = new KafkaConsumer<Object, Object>(props);
			Set<String> ss = consumer.listTopics().keySet();
			List<Object[]> ls = new ArrayList<Object[]>();
			Object[] os = null;
			for (String s : ss){
				os = new Object[1];
				os[0] = s;
				ls.add(os);
			}
			consumer.close();
	        return ImFunction.toTable(new String[]{"title"}, ls);
	        
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		return null;
	}
	
}