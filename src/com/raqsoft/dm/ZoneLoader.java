package com.raqsoft.dm;

import java.util.ArrayList;

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.parallel.UnitClient;
import com.raqsoft.resources.ParallelMessage;

/**
 * 内存区加载器
 * hosts(;j)	取出本分机上任务j的内存区号，j可省略
 * hosts(i;j)	设置本分机上任务j的内存区号，i=0表示清除任务j的内存区号
 * hosts(n,hs;j)	在hs中找出hosts(;j)返回值为1,…,n的可用分机序列。有缺失者则在hosts()为空的分机用相应缺失值执行初始化(init.dfx)，找不到足够多分机返回空
 * n==0时返回可用分机，不可用分机的位置填成null
 * 
 * @author Joancy
 */
public class ZoneLoader {
	private Integer I = null;
	private String J = null;
	
	private int N = 0;
	private Machines hs = null;
	

	// 已经找到了加载区的机器，需要执行dfx加载的机器不能包含已经找到加载区的机器，但是重复指定同一ip端口的机器确要算是不同的机器。
	private transient ArrayList<UnitClient> dispatchedNodes = new ArrayList<UnitClient>();
	static MessageManager mm = ParallelMessage.get();

	/**
	 * 构造一个内存区加载器
	 */
	public ZoneLoader() {
	}

	/**
	 * 设置参数
	 * @param i 设置任务j的内存区号，i=0表示清除任务j的内存区号
	 * @param j 任务名，省略用null
	 */
	public void setArgs(Integer i, String j) {
		this.I = i;
		this.J = j;
	}

	/**
	 * 设置参数
	 * @param n n==0时返回可用分机，不可用分机的位置填成null
	 * @param hs 分机组
	 * @param j  任务名
	 */
	public void setArgs(Integer n, Machines hs, String j) {
		if(n!=null && n>0){
			this.N = n;
		}
		this.hs = hs;
		this.J = j;
	}
	
	/**
	 * 连接指定的分机客户端
	 * @param nodes 分机客户端列表
	 * @throws Exception 连接出错抛出异常
	 */
	public static void connectNodes(ArrayList<UnitClient> nodes)
			throws Exception {
		for (int i = 0; i < nodes.size(); i++) {
			UnitClient uc = (UnitClient) nodes.get(i);
			uc.connect();
		}
	}

	/**
	 * 关掉分机客户端的连接
	 * @param nodes 分机客户端列表
	 */
	public static void closeNodes(ArrayList<UnitClient> nodes) {
		for (int i = 0; i < nodes.size(); i++) {
			UnitClient uc = (UnitClient) nodes.get(i);
			uc.close();
		}
	}
	
	/**
	 * 从指定的分机名称中找出启动了的机器
	 * @param deadAsNull 没有启动的分机使用null对象加入
	 * @return 分机客户端列表
	 * @throws Exception 列出过程出错抛出异常
	 */
	public ArrayList<UnitClient> listLiveClients( boolean deadAsNull) throws Exception {
		ArrayList<UnitClient> liveNodes = new ArrayList<UnitClient>();
		StringBuffer reason = new StringBuffer();
		for (int i = 0; i < hs.size(); i++) {
			UnitClient uc = new UnitClient(hs.getHost(i), hs.getPort(i));
			if (uc.isAlive(reason)) {
				liveNodes.add(uc);
			}else if( deadAsNull ){
				liveNodes.add(null);
			}
		}
		return liveNodes;
	}
	
	/**
	 * 成功返回与zone对应的分机序列，否则返回null
	 * @return 分机序列
	 */
	public Object execute() {
		if(hs!=null){
			return getMachines();
		}
		if(I==null){
//			取出本分机上任务j的内存区号，j可省略
			return Env.getAreaNo(J);
		}
		Env.setAreaNo( J, I );
		return true;
	}
	
	private Sequence getMachines(){ 
		ArrayList<UnitClient> liveNodes = null;
		try {
			Sequence nodes = new Sequence();
//			n=0，返回所有活动的分机,不活动的分机用null占位
			if( N==0 ){
				ArrayList<UnitClient> stateNodes = listLiveClients(true);
				for(UnitClient uc:stateNodes){
					String desc = uc==null?null:uc.toString();
					nodes.add( desc );
				}
				return nodes;
			}
			
			liveNodes = listLiveClients( false );
			if (liveNodes.isEmpty()) {
				Logger.debug(new Exception(mm.getMessage("ZoneLoader.noAlives")));
				return null;
			}
			if(liveNodes.size()<N){
				Logger.debug( new Exception(mm.getMessage("ZoneLoader.notEnoughAlives",liveNodes.size())));
				return null;
			}
			
			// 对于分区的均衡不根据机器本身的执行dfx的任务来均衡，而是根据当前任务，平均分配到活动的分机上
			connectNodes(liveNodes);
			
			ArrayList<Integer> areaNos = new ArrayList<Integer>();
			for (int i = 0; i < liveNodes.size(); i++) {
				UnitClient uc = liveNodes.get(i);
				Integer areaNo = uc.getAreaNo(J);
				areaNos.add( areaNo );
			}
			
//				先按内存区顺序，将已经加载过的分机找出来
			boolean lackZone = false;
			for (int i = 1; i <= N; i++) {
				Integer zone = i;
				int index = areaNos.indexOf( zone );
				if( index==-1 ){
					nodes.add(null);
					Logger.debug("Data zone: "+i+" is not found.");
					lackZone = true;
				}else{
					UnitClient uc = liveNodes.get(index); 
					nodes.add( uc );
					Logger.debug("Found zone: "+i+" on "+uc);
					dispatchedNodes.add(uc);
				}
			}
//				再补上中间区号缺失的分机，之所以要等上面步骤完成，是要将已经使用的分机剔除
			if(lackZone){
				for (int i = 1; i <= N; i++) {
					UnitClient uc = (UnitClient)nodes.get(i);
					if(uc==null){
						for(int n=0;n<liveNodes.size();n++){
							uc = liveNodes.get(n);
							if(!dispatchedNodes.contains(uc)){
								uc.initNode(i, N, J);
								nodes.set(i, uc);
								dispatchedNodes.add(uc);
								break;
							}
						}
					}
				}
			}
			return nodes;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception x) {
			throw new RuntimeException(x.getMessage(), x);
		} finally {
			if (liveNodes != null) {
				closeNodes(liveNodes);
			}
		}
	}

}
