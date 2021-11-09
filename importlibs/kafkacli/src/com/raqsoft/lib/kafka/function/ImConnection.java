package com.raqsoft.lib.kafka.function;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.raqsoft.common.Logger;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.IResource;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

public class ImConnection implements IResource {
	public static String m_prjName = "Kafka"; 
	public boolean m_bClose = false;
	public String[] m_cols = new String[]{"offset", "key", "value"};
	private int m_nPartitions = 0;
	private boolean m_bCluster = false;
	private Properties m_properties;
	private List<KafkaConsumer<Object, Object>> m_listConsumers;
	public KafkaConsumer<Object, Object> m_consumer = null;
	private List<String> m_topics; 	
	private ClassLoader  m_classLoader=null;
	
	public ImConnection(Context ctx, String fileName,List<String> topics,int nPartitionSize) {
		ctx.addResource(this);
		doProperty(fileName);		
		init(ctx, topics,nPartitionSize);
	}
	
	public ImConnection(Context ctx, Properties property, List<String> topics,int nPartitionSize) {
		ctx.addResource(this);
		m_properties = property;
		init(ctx, topics,nPartitionSize);		
	}
	
	public Properties getProperties(){
		return m_properties;
	}
	
	public String getTopic(){
		return m_topics.get(0);
	}
	
	public boolean isCluster(){
		return m_bCluster;
	}
	
	public int getPartitionSize(){
		return m_nPartitions;
	}
	
	private void init(Context ctx, List<String> topics, int nPartitionSize){
		try{
			m_nPartitions = nPartitionSize;
			m_bCluster = (m_nPartitions>0);
			m_topics = new ArrayList<String>(topics);
			m_listConsumers = new ArrayList<KafkaConsumer<Object, Object>>() ;
			m_classLoader = Thread.currentThread().getContextClassLoader();
			ClassLoader loader = ImConnection.class.getClassLoader();
			Thread.currentThread().setContextClassLoader(loader);
			
			//群集时创建主题
			if (m_bCluster){
				AdminClient client = KafkaAdminClient.create(m_properties);//创建操作客户端
				for(int i=0; i<topics.size(); i++){
					String topicName = topics.get(i);
					ListTopicsResult dr = client.listTopics();
					if (!isExistedTopic(dr, topicName)){
						 NewTopic topic = new NewTopic(topics.get(i), nPartitionSize, (short) 1); 
						 CreateTopicsResult cr = client.createTopics(Arrays.asList(topic));
						 cr.all().get();
					}
				}		
				client.close();//关闭
			}
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
			for(int i=0; i<m_listConsumers.size(); i++){
				 KafkaConsumer<Object, Object> c = m_listConsumers.get(i);
				 c.close();
				 c = null;
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
	
	public void setPropertyGroudId( String groupId){
	     m_properties.put("group.id", groupId);
	}
	
	public KafkaConsumer<Object, Object> initConsumer(){
		final KafkaConsumer<Object, Object> consumer = new KafkaConsumer<Object, Object>(m_properties);
       
		consumer.subscribe(m_topics, new ConsumerRebalanceListener() {
	        public void onPartitionsRevoked(Collection<TopicPartition> collection) {
	        	
	        }
	        public void onPartitionsAssigned(Collection<TopicPartition> collection) {
	            //将偏移设置到最开始
	            consumer.seekToBeginning(collection);
	        }
	    });
		
		m_listConsumers.add(consumer);
		m_consumer = consumer;

		return m_consumer;
	}
	
	public KafkaConsumer<Object, Object> initConsumerCluster(List<Integer> partitions){
		final KafkaConsumer<Object, Object> consumer = new KafkaConsumer<Object, Object>(m_properties);

		// 指定分区消费
		List<TopicPartition> ls = new ArrayList<TopicPartition>();
		String topic = getTopic();
		if (partitions.size()>0){
			for(Integer part : partitions){
				TopicPartition partition = new TopicPartition(topic, part);
				ls.add(partition);
			}
			// 绑定topics
	        consumer.assign(ls);
		}else{
			consumer.subscribe(Arrays.asList(topic));
		}
		
		m_listConsumers.add(consumer);
		m_consumer = consumer;

		return m_consumer;
	}



}