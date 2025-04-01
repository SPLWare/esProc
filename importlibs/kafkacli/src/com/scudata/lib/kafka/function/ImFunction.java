package com.scudata.lib.kafka.function;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class ImFunction extends Function {
	protected int m_paramSize = 0; // 参数个数
	protected ImConnection m_conn = null;
	protected String m_model;
	protected Context m_ctx;
	private static SimpleDateFormat m_sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		m_ctx = ctx;

		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("olap" + mm.getMessage("function.missingParam"));
		}
		option = getOption();
		int size = param.getSubSize();
		// System.out.println("baseSize = " + size);
		if (size == 0) {
			Object o = param.getLeafExpression().calculate(ctx);
			if ((o instanceof ImConnection)) {
				m_conn = (ImConnection) o;
				return doQuery(null);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ali_close" + mm.getMessage("function.paramTypeError"));
			}
		}

		Object cli = new Object();
		Object objs[] = new Object[size - 1];
		for (int i = 0; i < size; i++) {
			if (param.getSub(i) == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("client" + mm.getMessage("function.invalidParam"));
			}

			if (i == 0) {
				cli = param.getSub(i).getLeafExpression().calculate(ctx);
				if ((cli instanceof ImConnection)) {
					m_conn = (ImConnection) cli;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("client" + mm.getMessage("function.paramTypeError"));
				}
			} else {
				objs[i - 1] = param.getSub(i).getLeafExpression().calculate(ctx);
			}
		}

		if (m_conn == null) {
			throw new RQException("connect is null");
		}
		if (objs.length < 1) {
			throw new RQException("olap_param is empty");
		} else if (option != null && option.indexOf("f") > -1) {// 最近一个参数可能是文件，转换成对应的文本.
			try {
				if (objs.length > 1) {
					String s = objs[1].toString();
					FileObject f = new FileObject(s);
					if (f.isExists()) {
						objs[1] = f.read(0, -1, "v");
					}
				}
			} catch (IOException e) {
				Logger.error(e.getStackTrace());
			}
		}

		return doQuery(objs);
	}

	public Object doQuery(Object[] objs) {

		return null;
	}

	public static Table toTable(String[] cols, List<Object[]> rows) {
		if (rows == null)
			return null;
		Table table = new Table(cols);

		for (Object[] row : rows) {
			table.newLast(row);
		}

		return table;
	}

	public static List<Object[]> getData(ImConnection conn, long timeout) {
		Object okey, oval;
		List<Object[]> ls = new ArrayList<Object[]>();
		try {
			ConsumerRecords<Object, Object> records = null;
			for (int i = 0; i < 5; i++) {
				records = conn.m_consumer.poll(Duration.ofMillis(timeout));				
				if (records.isEmpty()) {
					System.out.println("No Data ...");
					Thread.sleep(300);
					continue;
				}else{
					break;
				}
			}
			
			for (ConsumerRecord<Object, Object> record : records) {
				Object[] lines = new Object[5];
				lines[0] = record.partition();
				lines[1] = record.offset();
				okey = record.key();
				oval = record.value();
				lines[2] = okey;
				if (oval instanceof byte[]) {
					byte[] bt = (byte[]) oval;
					if (bt.length > 12 && bt[0] == '#' && bt[1] == '#') { // Sequence
						Sequence seq = new Sequence();
						byte[] byteNew = new byte[bt.length - 2];
						System.arraycopy(bt, 2, byteNew, 0, bt.length - 2);
						bt = null;
						seq.fillRecord(byteNew);
						lines[3] = seq;
					} else {
						lines[3] = new String(bt);
					}
				} else {
					lines[3] = oval;
				}
				lines[4] = m_sdf.format(new Date(record.timestamp() ));
				ls.add(lines);
			}
			//conn.m_consumer.commitSync();
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		
		return ls;
	}

	public static List<Object[]> getClusterData(ImConnection conn, long timeout, int partitionSize) {
		Object okey, oval;
		List<Object[]> ls = new ArrayList<Object[]>();
		try {
			if(partitionSize<1) partitionSize = 1;
			int nTotal = partitionSize;

			boolean bHad = false; //对后来分区获取不再等待
			ConsumerRecords<Object, Object> consumerRecords = null;
			for (int i = 0; i < partitionSize; i++) {
				for(int j=0; j<6; j++){
					consumerRecords = conn.m_consumer.poll(Duration.ofMillis(timeout));
					if (consumerRecords.isEmpty()) {
						System.out.println("No Data2 ...");						
						if (bHad){
							break;
						}else{
							Thread.sleep(200);
							continue;
						}
					}else{
						bHad = true;
						break;						
					}
				}
				
				 //获取每个分区
	            Set<TopicPartition> partitions = consumerRecords.partitions();
	            //遍历每个分区
	            for (TopicPartition partition : partitions) {
	                //获取分区的数据的载体
	                List<ConsumerRecord<Object, Object>> records = consumerRecords.records(partition);
	                //获取每个数据
	                for (ConsumerRecord<Object, Object> record : records) {
						Object[] lines = new Object[5];
						lines[0] = record.partition();
						lines[1] = record.offset();						
						okey = record.key();
						oval = record.value();
						lines[2] = okey;
						if (oval instanceof byte[]) {
							byte[] bt = (byte[]) oval;
							if (bt.length > 12 && bt[0] == '#' && bt[1] == '#') { // Sequence
								Sequence seq = new Sequence();
								byte[] byteNew = new byte[bt.length - 2];
								System.arraycopy(bt, 2, byteNew, 0, bt.length - 2);
								bt = null;
								seq.fillRecord(byteNew);
								lines[3] = seq;
							} else {
								lines[3] = new String(bt);
							}
						} else {
							lines[3] = oval;
						}
						
						lines[4] = m_sdf.format(new Date(record.timestamp() ));
						ls.add(lines);
					}
	            }
	            //达到总分区数时则退出
	            nTotal -= partitions.size();
	            if (nTotal<=0){
	            	break;
	            }
	            //手动提交0ffset
	            //conn.m_consumer.commitSync();
			}			
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}

		return ls;
	}
}