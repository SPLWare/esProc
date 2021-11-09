package com.raqsoft.parallel;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.JobSpace;
import com.raqsoft.dm.JobSpaceManager;

/**
 * 节点机集群
 * @author RunQian
 *
 */
public class Cluster implements Externalizable {
	private static final long serialVersionUID = 1L;
	
	private String []hosts; // ip数组
	private int []ports; // 端口数组
	private Context ctx; // 计算上下文
	
	// 序列化是使用
	public Cluster() {
	}
	
	/**
	 * 构建节点机集群
	 * @param hosts ip数组
	 * @param ports 端口数组
	 * @param ctx 计算上下文
	 */
	public Cluster(String[] hosts, int[] ports, Context ctx) {
		this.hosts = hosts;
		this.ports = ports;
		this.ctx = ctx;
		for(int i=0;i<hosts.length;i++){
			ctx.getJobSpace().addHosts(hosts[i], ports[i]);
		}
	}
	
	public boolean isEquals(Cluster other) {
		String []h1 = hosts;
		String []h2 = other.hosts;
		int count = h1.length;
		if (count == h2.length) {
			int []p1 = ports;
			int []p2 = other.ports;
			for (int i = 0; i < count; ++i) {
				if (!h1[i].equals(h2[i]) || p1[i] != p2[i]) {
					return false;
				}
			}
			
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 取节点机数量
	 * @return int
	 */
	public int getUnitCount() {
		return hosts.length;
	}
	
	public void setHosts(String[] hosts, int[] ports) {
		this.hosts = hosts;
		this.ports = ports;
	}

	public String[] getHosts() {
		return hosts;
	}
	
	//根据序号取分机
	public String getHost(int unit) {
		return hosts[unit];
	}
	
	public int[] getPorts() {
		return ports;
	}
	
	public int getPort(int unit) {
		return ports[unit];
	}
	
	public Context getContext() {
		return ctx;
	}
	
	public String getJobSpaceId() {
		return ctx.getJobSpace().getID();
	}
	
	// 取本机进程构成的集群，如果没有配置分进程则返回null
	public static Cluster getHostCluster() {
		return null;
	}
	
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(hosts);
		out.writeObject(ports);
		out.writeObject(getJobSpaceId());
	}
	
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		hosts = (String [])in.readObject();
		ports = (int [])in.readObject();
		String jobSpaceID = (String)in.readObject();
		JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
		ctx = new Context();
		ctx.setJobSpace(js);
	}
}