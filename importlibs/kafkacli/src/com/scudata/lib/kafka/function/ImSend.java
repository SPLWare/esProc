package com.scudata.lib.kafka.function;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import com.scudata.common.Logger;
import com.scudata.dm.FileObject;
import com.scudata.dm.Param;
import com.scudata.dm.Sequence;

/*
 * ImSend(fd, topic, [key,] value)
 * ImSend(fd, topic, part, key, value)
 * ImSend(fd, topic, [part,...], key, value)
 */
public class ImSend extends ImFunction {

	public Object doQuery(Object[] objs) {
		try {
			if (m_conn.m_bClose){
				return null;
			}
			//分区参数
			
			Param param = null;
			boolean bSameConn = false;
			param = m_ctx.getParam("kafka_conn");
			if (param!=null){
				ImConnection conn = (ImConnection)param.getValue(); 
				bSameConn = (conn==m_conn);
			}
			if (!bSameConn){
				if (m_conn.m_producer!=null){
					m_conn.m_producer.close();
				}
				KafkaProducer<Object, Object> producer = new KafkaProducer<Object, Object>(m_conn.getProperties());
				m_conn.m_producer = producer;
				m_ctx.setParamValue("kafka_conn", m_conn);
			}
			ProducerRecord<Object, Object> record = null; 
			List<Integer> ll = new ArrayList<Integer>();
			if (objs[0] instanceof Integer){
				ll.add((Integer)objs[0]);
			}else if (objs[0] instanceof Sequence){
				Sequence seq = (Sequence)objs[0];
				for(int i=1; i<=seq.length();i++){
					ll.add((Integer)seq.get(i));
				}
			}else if(objs[0] instanceof String){ //NoPartition: [key,] value;
				record = doProducerRecord(objs);
				m_conn.m_producer.send(record, new Callback() {
					@Override
					public void onCompletion(RecordMetadata recordMetadata, Exception e) {
						if (null != e) {
							Logger.info("send error:" + e.getMessage());
						} else {
							System.out.println(String.format("offset:%s,partition:%s", 
									recordMetadata.offset(),
									recordMetadata.partition()));
						}
					}
				});
				Thread.sleep(10);
			}else if (objs[0] instanceof FileObject){
				doProducerRecordFile((FileObject)objs[0]);
			}
			
			//Partition :part, [key,] value;
			for (Integer partSN: ll){
				record = doProducerRecordPartition(partSN, objs);
				m_conn.m_producer.send(record, new Callback() {
					@Override
					public void onCompletion(RecordMetadata recordMetadata, Exception e) {
						if (null != e) {
							Logger.info("send2 error " + e.getMessage());
						} else {
							System.out.println(String.format("offset:%s,partition:%s", 
									recordMetadata.offset(),
									recordMetadata.partition()));
						}
					}
				});
				Thread.sleep(10);
			}
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		
		return null;
	}
	
	// for FileObjec
	private void doProducerRecordFile(FileObject fo){
		FileReader freader;
		try {
			if (!fo.isExists()) return;
			
			freader = new FileReader(new File(fo.getFileName()));
			BufferedReader reader = new BufferedReader(freader);
			String tempString = null;
			while ((tempString = reader.readLine()) != null) {
				m_conn.m_producer.send(new ProducerRecord<Object, Object>(m_conn.getTopic(), tempString));
				Thread.sleep(10);
			}
			reader.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Logger.error(e.getMessage());
		}		
	}
	
	// for Normal;
	private ProducerRecord<Object, Object> doProducerRecord(Object[] objs){
		ProducerRecord<Object, Object> record = null;
		try {			
			String val = (String) m_conn.getProperties().get("value.serializer");
			Object oVal = null;
			if (objs.length==2){
				oVal = objs[1];
			}else{
				oVal = objs[0];
			}

			if (val!=null && val.endsWith("ByteArraySerializer")){
				if (oVal instanceof String){
					if (objs.length==2){
						record = new ProducerRecord<Object, Object>(m_conn.getTopic(), objs[0], oVal.toString().getBytes());
					}else{
						record = new ProducerRecord<Object, Object>(m_conn.getTopic(), oVal.toString().getBytes());
					}
				}else if(oVal instanceof Sequence){
					Sequence seq = (Sequence)oVal;
					byte[] bt = seq.serialize();
					byte[] byteNew = new byte[2+bt.length];
					byteNew[0]=byteNew[1]='#';
					System.arraycopy(bt, 0, byteNew, 2, bt.length);
					bt = null;
					if (objs.length==2){
						record = new ProducerRecord<Object, Object>(m_conn.getTopic(), objs[0], byteNew);
					}else{
						record = new ProducerRecord<Object, Object>(m_conn.getTopic(), byteNew);
					}
				}
			}else{
				if (objs.length==2){
					record = new ProducerRecord<Object, Object>(m_conn.getTopic(), objs[0], oVal);
				}else{
					record = new ProducerRecord<Object, Object>(m_conn.getTopic(), oVal);
				}
			}
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}
		
		return record;
	}
	
	// for Part;
	private ProducerRecord<Object, Object> doProducerRecordPartition(int nPart, Object[] objs){
		ProducerRecord<Object, Object> record = null;
		try {			
			String val = (String) m_conn.getProperties().get("value.serializer");
			Object oVal = objs[2];
			if (val!=null && val.trim().endsWith("ByteArraySerializer")){
				if (oVal instanceof String){
					record = new ProducerRecord<Object, Object>(m_conn.getTopic(), nPart, objs[1], oVal.toString().getBytes());
				}else if(oVal instanceof Sequence){
					Sequence seq = (Sequence)oVal;
					byte[] bt = seq.serialize();
					byte[] byteNew = new byte[2+bt.length];
					byteNew[0]=byteNew[1]='#';
					System.arraycopy(bt, 0, byteNew, 2, bt.length);
					bt = null;
					record = new ProducerRecord<Object, Object>(m_conn.getTopic(), nPart, objs[1], byteNew);
				}
			}else{
				record = new ProducerRecord<Object, Object>(m_conn.getTopic(),nPart, objs[1], oVal);
			}		
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		
		return record;
	}
}