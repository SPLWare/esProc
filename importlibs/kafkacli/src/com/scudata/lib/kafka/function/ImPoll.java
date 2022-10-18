package com.scudata.lib.kafka.function;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import com.scudata.common.Logger;
import com.scudata.dm.Param;
import com.scudata.dm.Sequence;

/*
 * ImPoll@e(fd, timeout, [p1,p2,...])
 */
public class ImPoll extends ImFunction {
	private Set<TopicPartition> m_topicPartition;		// 指定位置进行消费
	
	@SuppressWarnings("unchecked")
	public Object doQuery(Object[] objs) {
		try {			
			boolean bCursor = false;
			if (m_conn.m_bClose) return null;
			if (option!=null){
				if (option.indexOf("c")!=-1){
					bCursor = true;
				}
			}

			long timeout = 1000;
			if (objs!=null){
				timeout = Integer.parseInt(objs[0].toString());
			}
			
			List<Integer> partitions = new ArrayList<Integer>();
			if (objs!=null && objs.length>1){
				if(objs[1] instanceof Integer){
					partitions.add((Integer)objs[1]);
				}else if(objs[1] instanceof Sequence){
					Sequence seq = (Sequence)objs[1];
					for(int i=0; i<seq.length(); i++){
						partitions.add((Integer)seq.get(i+1));
					}
				}
			}
	        
			//再次调用poll时不再初始化.
			Param param = null;
			boolean bPollRun = false;
			param = m_ctx.getParam("poll_run");
			if (param!=null){
				bPollRun = (Boolean)param.getValue(); 
			}
			
			int nPartSize = partitions.size();
			
			/********************************
			 * 由于seek([p1,p2...], off)后，对于主题poll()只能得到定位的分区数据，因此采用下面方式:
			 * 对于有offset参数且poll无parti参数时,则取所有的分区
			 * 也就是有offset参数时，按带poll带参数分区处理.
			 * 
			 **********************************/
			param = m_ctx.getParam("offset_val");
			if (nPartSize==0 && param!=null){
				partitions = getPaititionSize();
				nPartSize = partitions.size();
			}

			if (nPartSize>0){
				//分区修改后，则重新初始化，不管前面分区是否消费过。
				param = m_ctx.getParam("partition");
				if (param!=null){
					List<Integer> oldPorts = (List<Integer>)param.getValue(); 
					if (!oldPorts.equals(partitions)){
						bPollRun = false;
					}
				}
				
				if (!bPollRun){
					m_conn.initConsumerCluster(partitions);
					setComsumerOffset(partitions);
				}
				m_ctx.setParamValue("partition", partitions);
			}else{
				param = m_ctx.getParam("partition");
				if (param!=null){
					bPollRun = false;
					m_ctx.setParamValue("partition", null);
				}
				
				if (!bPollRun){
					m_conn.initConsumer();
				}
			}
			
			m_ctx.setParamValue("poll_run", true);
			if (bCursor){
				return new ImCursor(m_ctx, m_conn, timeout, nPartSize);
			}else{
				List<Object[]> ls = null;
				if (nPartSize==0 && !bPollRun){
					partitions = getPaititionSize();
					nPartSize = partitions.size();
				}
				ls = ImFunction.getClusterData(m_conn, timeout, nPartSize);
				
		        return ImFunction.toTable(m_conn.m_cols, ls);
			}		
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		
		return null;
	}
	
	private List<Integer> getPaititionSize(){
		KafkaConsumer<Object, Object> consumer = 
				new KafkaConsumer<Object, Object>(m_conn.getProperties());
		consumer.subscribe(Arrays.asList(m_conn.getTopic()));
		// 指定位置进行消费
		m_topicPartition = consumer.assignment();

        // 保证分区分配方案已经制定完毕
		// 网络中断可能是个死循环，循环过多则退出。
		int nCount = 0;
        while (m_topicPartition.size() == 0){
        	consumer.poll(Duration.ofSeconds(1));
        	m_topicPartition = consumer.assignment();
        	if (nCount++ > 10){
        		break;
        	}
        }
        List<Integer> partitions = new ArrayList<Integer>();
		for(TopicPartition t:m_topicPartition){
			partitions.add(t.partition());
		}
		
		consumer.unsubscribe();
		consumer.close();
		
		return partitions;
	}
	
	// 对不带分区的消费者的offset进行设置
	@SuppressWarnings("unchecked")
	private void setComsumerOffset(){		
		Param param = m_ctx.getParam("offset_val");
		if (param!=null){
			if (param.getValue() instanceof Map){
				Map<Integer, Integer> map = (Map<Integer, Integer>)param.getValue();
				for (TopicPartition topicPartition : m_topicPartition) {					
	        		if (map.containsKey(topicPartition.partition()) ){
	        			m_conn.m_consumer.seek(topicPartition, map.get(topicPartition.partition()));
	        		}
		        }
			}else if(param.getValue() instanceof Integer){
				Integer nOffset = (Integer)param.getValue();
				for (TopicPartition topicPartition : m_topicPartition) {
		        	m_conn.m_consumer.seek(topicPartition, nOffset);
		        }
			}
			m_ctx.setParamValue("offset_val", null);
		}
	}
	
	// 对带分区的消费者的offset进行设置
	@SuppressWarnings("unchecked")
	private void setComsumerOffset(List<Integer> partitions){	
		Param param = m_ctx.getParam("offset_val");
		if (param!=null){
			if (param.getValue() instanceof Map){
				Map<Integer, Integer> map = (Map<Integer, Integer>)param.getValue();
				
				for (Map.Entry<Integer, Integer> entry: map.entrySet()) {
					//找出两个分区的交集(kafka_offset, kafka_poll的分区参数)
					if (partitions.contains(entry.getKey()) && entry.getValue()>-1){ 
						TopicPartition topicPartition = new TopicPartition(m_conn.getTopic(), entry.getKey());
	        			m_conn.m_consumer.seek(topicPartition, entry.getValue());
					}
		        }

			}else if(param.getValue() instanceof Integer){
				Integer nOffset = (Integer)param.getValue();
				for (Integer partSN : partitions) {
					TopicPartition topicPartition = new TopicPartition(m_conn.getTopic(), partSN);
        			m_conn.m_consumer.seek(topicPartition, nOffset);
		        }
			}
			m_ctx.setParamValue("offset_val", null);
		}
	}
}