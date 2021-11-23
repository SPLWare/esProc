package com.scudata.dm;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.parallel.UnitClient;
import com.scudata.resources.ParallelMessage;

/**
 * 对应于callx的多机任务
 * @author Joancy
 *
 */
public class UnitTasks {
	ArrayList<UnitTask> uts = new ArrayList<UnitTask>();
	static MessageManager mm = ParallelMessage.get();
	
	/**
	 * 构造函数
	 * @param ucs 节点机列表
	 * @throws Exception 构造出错扔出异常
	 */
	public UnitTasks(ArrayList<UnitClient> ucs) throws Exception{
		int len = ucs.size();
		for(int i=0;i<len; i++){
			UnitClient uc = ucs.get(i);
			UnitTask ut = new UnitTask(uc);
			uts.add(ut);
		}
	}
	
	/**
	 * 构造函数
	 * @param nodes 节点机数组
	 * @throws Exception 出错时扔出异常
	 */
	public UnitTasks(UnitClient[] nodes) throws Exception{
		int len = nodes.length;
		for(int i=0;i<len; i++){
			UnitClient uc = nodes[i];
			String h = null;
			if(uc!=null){
				h = uc.getHost();
			}
			UnitTask ut;
			if(h==null){
				ut = new UnitTask(null);
			}else{
				if(!uc.isConnected()){
					ut = new UnitTask(null);
				}else{
					ut = new UnitTask(uc);
				}
			}
			uts.add(ut);
		}
	}
	/**
	 * 获取当前最大算力的节点机
	 * 该方法执行后，节点机假设分配了适合作业数，下一次直接最大算力节点机即可
	 * @return 最大算力节点机
	 */
	public UnitClient getMaxCapacityUC(){
		return getMaxCapacityUC(null);
	}
	
	/**
	 * 从指定序号的节点机中找出最大算力的节点机
	 * @param ucIndexes 指定序号位置的节点机
	 * @return 最大算力节点机
	 */
	public UnitClient getMaxCapacityUC(ArrayList<Integer> ucIndexes){
		UnitTask ut = getMaxCapacityUT(ucIndexes);
		int prefer = ut.preferredNum;
		ut.addTaskNum(prefer);
		return ut.getUnitClient();
	}
	
	/**
	 * 获取节点任务
	 * utIndex为 Sequence中引用的1开始的序号，数组引用要减去1
	 * @param utIndex
	 * @return
	 */
	public UnitTask getUnitTask(int utIndex){
		return uts.get(utIndex-1);
	}

	/**
	 * 实现串化接口
	 */
	public String toString(){
		StringBuffer sb = new StringBuffer();
		for(UnitTask ut:uts){
			if(sb.length()>0){
				sb.append(",");
			}
			sb.append(ut);
		}
		return sb.toString();
	}
	/**
	 * 获取在指定范围的机器内找出最大算力的节点任务
	 * 该方法返回节点任务后，不自动增加节点机的任务数，需要外层调用
	 * UnitTask.addTaskNum(n)来计数任务。
	 * @param ucIndexes，指定范围的分机序号列表
	 * @return 节点任务
	 */
	public UnitTask getMaxCapacityUT(ArrayList<Integer> ucIndexes){
		if(ucIndexes==null){
			return getMaxCapacityUT();
		}
		UnitTask ut = uts.get(ucIndexes.get(0));
		double c = ut.capacity();
		for(int i=1;i<ucIndexes.size();i++){
			UnitTask tmp = uts.get(ucIndexes.get(i));
			double dc = tmp.capacity();
			if(dc>c){
				c = dc;
				ut = tmp;
			}
		}
		return ut;
	}
	
	/**
	 * 在当前全部范围的机器内找出算力最大的
	 * @return 节点任务
	 */
	public UnitTask getMaxCapacityUT(){
		UnitTask ut = uts.get(0);
		double c = ut.capacity();
		for(int i=1;i<uts.size();i++){
			UnitTask tmp = uts.get(i);
			double dc = tmp.capacity();
			if(dc>c){
				c = dc;
				ut = tmp;
			}
		}
		return ut;
	}
	
	
	class UnitTask{
		int preferredNum = 0;
		int currentNum = 0;
		UnitClient uc;
		
		UnitTask(UnitClient uc) throws Exception{
			this.uc = uc;
			if(uc!=null){
				int[] tasks = uc.getTaskNums();
				preferredNum = tasks[0];
				currentNum = tasks[1];
			}
		}
		
		UnitClient getUnitClient(){
			return uc;
		}
		
		public String toString(){
			return uc.toString();
		}
		
		double capacity(){
			return preferredNum*1.0f/(currentNum+0.5f);
		}
		
		public void addTaskNum(int tasks){
			currentNum += tasks;
		}
	}

}
