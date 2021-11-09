package com.raqsoft.lib.kafka.function;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import com.raqsoft.common.Logger;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.Sequence;

/*
 * ImSend(fd, topic, value)
 * ImSend(fd, topic, key, value)
 * ImSend(fd, topic, part, key, value)
 */
public class ImSend extends ImFunction {
	private KafkaProducer<Object, Object> m_producer = null;

	public Object doQuery(Object[] objs) {
		try {
			if (m_conn.m_bClose){
				return null;
			}
			boolean bCluster = m_conn.isCluster();
			
			//Cluster :part, [key,] value;
			//Nocluster: [key,] value;
			if (bCluster){
				if (objs.length!=3){
					throw new RQException(ImConnection.m_prjName +" Cluster function.missingParam");
				}
				if (!(objs[0] instanceof Integer)){
					throw new RQException(ImConnection.m_prjName +" Cluster partition");
				}
			}else if (objs.length<1){
				throw new RQException(ImConnection.m_prjName +" function.missingParam");
			}
			
			m_producer = new KafkaProducer<Object, Object>(m_conn.getProperties());
			
			if (objs[0] instanceof FileObject){
				doProducerRecordFile((FileObject)objs[0]);
			}else{
				ProducerRecord<Object, Object> record = null; 
				if (bCluster){
					record = doProducerRecordCluster(objs);
				}else{
					record = doProducerRecord(objs);
				}
				// ·¢ËÍÏûÏ¢
				m_producer.send(record, new Callback() {
					@Override
					public void onCompletion(RecordMetadata recordMetadata, Exception e) {
						if (null != e) {
							Logger.info("send error" + e.getMessage());
						} else {
							System.out.println(String.format("offset:%s,partition:%s", recordMetadata.offset(),
									recordMetadata.partition()));
						}
					}
				});
			}
			m_producer.close();
		} catch (Exception e) {
			e.printStackTrace();
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
				m_producer.send(new ProducerRecord<Object, Object>(m_conn.getTopic(), tempString));
				Thread.sleep(10);
			}
			reader.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			e.printStackTrace();
		}
		
		return record;
	}
	
	// for Cluster;
	private ProducerRecord<Object, Object> doProducerRecordCluster(Object[] objs){
		ProducerRecord<Object, Object> record = null;
		try {			
			String val = (String) m_conn.getProperties().get("value.serializer");
			int nPart = Integer.parseInt(objs[0].toString());
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
			e.printStackTrace();
		}
		
		return record;
	}
}