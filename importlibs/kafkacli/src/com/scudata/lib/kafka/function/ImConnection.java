package com.scudata.lib.kafka.function;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.IResource;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.TopicPartition;

public class ImConnection implements IResource {
	private String m_topic;
	public static String m_prjName = "Kafka"; 
	public boolean m_bClose = false;
	public String[] m_cols = new String[]{"partition", "offset", "key", "value", "timestamp"};
	private Properties m_properties;
	//private List<KafkaConsumer<Object, Object>> m_listConsumers;
	public KafkaProducer<Object, Object> m_producer = null;
	public KafkaConsumer<Object, Object> m_consumer = null;
	private ClassLoader  m_classLoader=null;
	
	public ImConnection(Context ctx, String fileName,String topics,int nPartitionSize) {
		ctx.addResource(this);
		doProperty(fileName);		
		init(ctx, topics,nPartitionSize);
	}
	
	public ImConnection(Context ctx, Properties property, String topics,int nPartitionSize) {
		ctx.addResource(this);
		m_properties = property;
		init(ctx, topics,nPartitionSize);		
	}
	
	
	public Properties getProperties(){
		return m_properties;
	}
	
	public String getTopic(){
		return m_topic;
	}
	
	private void init(Context ctx, String topic, int nPartitionSize){
		try{
			m_topic = topic;
			m_classLoader = Thread.currentThread().getContextClassLoader();
			ClassLoader loader = ImConnection.class.getClassLoader();
			Thread.currentThread().setContextClassLoader(loader);
			
			//创建主题
			AdminClient client = KafkaAdminClient.create(m_properties);//创建操作客户端
			ListTopicsResult dr = client.listTopics();
			if (!isExistedTopic(dr, topic)){
				 NewTopic tmp = new NewTopic(m_topic, nPartitionSize, (short) 1); 
				 CreateTopicsResult cr = client.createTopics(Arrays.asList(tmp));
				 cr.all().get();
			}
			client.close();//关闭

		}catch(Exception e){
			Logger.error(e.getMessage());
		}
	}
	
	private boolean isExistedTopic(ListTopicsResult result, String topic){
		try {
			Set<String> names = result.names().get();
			for (String s : names){
				if (s.equals(topic)){
					return true;
				}
			}
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}
		
		return false;
	}
	public void close() {
		try {
			if(m_producer!=null){
				m_producer.flush();;
				m_producer.close();
				m_producer = null;
			}
			
			if(m_consumer!=null){
				m_consumer.unsubscribe();;
				m_consumer.close();
				m_consumer = null;
			}
			
			m_bClose = true;
			
			if (m_classLoader!=null){
				Thread.currentThread().setContextClassLoader(m_classLoader);
				m_classLoader=null;
			}
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
	}

	private void doProperty(String fileName){
    	m_properties = new Properties();  
    	String path = fileName;
    	if (fileName==null || fileName.isEmpty()){
    		path = ImConnection.class.getResource("/").getFile().toString() + "kafka.properties";
    	}else if (fileName.indexOf("/")<0 && fileName.indexOf("\\")<0){
    		path = ImConnection.class.getResource("/").getFile().toString() + "fileName";  
    	}else{
    		;
    	}
        File f = new File(path);
        if (!f.exists()){
        	throw new RQException(path + " is not existed");
        }else{
	        //System.out.println("path:" + path);  
	        try {  
	            FileInputStream fis = new FileInputStream(new File(path));  
	            m_properties.load(fis);  
	        } catch (Exception e) {  
	        	Logger.error(e.getMessage());
	        }  
        }
	}	
	
	public void setPropertyBrokers(String brokers){
		m_properties.setProperty("bootstrap.servers", brokers);
	}
	
	public String getPropertyValue(String key){
		return m_properties.getProperty(key);
	}
	
	public KafkaConsumer<Object, Object> initConsumer(){
		final KafkaConsumer<Object, Object> consumer = new KafkaConsumer<Object, Object>(m_properties);
		consumer.subscribe(Arrays.asList(m_topic));
		if(m_consumer!=null){
			m_consumer.unsubscribe();;
			m_consumer.close();
			m_consumer = null;
		}

		m_consumer = consumer;
		return m_consumer;
	}
	
	public KafkaConsumer<Object, Object> initConsumerCluster(List<Integer> partitions){
		final KafkaConsumer<Object, Object> consumer = new KafkaConsumer<Object, Object>(m_properties);

		// 指定分区消费
		List<TopicPartition> ls = new ArrayList<TopicPartition>();		
		if (partitions.size()>0){
			for(Integer part : partitions){
				TopicPartition partition = new TopicPartition(m_topic, part);
				ls.add(partition);
			}
			// 绑定topics
	        consumer.assign(ls);
		}else{
			consumer.subscribe(Arrays.asList(m_topic));
		}
		if(m_consumer!=null){
			m_consumer.unsubscribe();;
			m_consumer.close();
			m_consumer = null;
		}
		m_consumer = consumer;
		return m_consumer;
	}

}