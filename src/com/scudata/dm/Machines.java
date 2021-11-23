package com.scudata.dm;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.parallel.UnitClient;

public class Machines implements Externalizable{
	private String []hosts;
	private int []ports;
	
	public Machines() {
	}
	
	public boolean set(Object o) {
		if (o == null) {
			//MessageManager mm = EngineMessage.get();
			//throw new RQException(errorMsg + mm.getMessage("function.invalidParam"));
		} else if (o instanceof String) {
			String str = (String)o; // ip:port
			int index = str.lastIndexOf(':');
			if (index == -1) {
				return false;
			}

			hosts = new String[]{str.substring(0, index)};
			ports = new int[]{Integer.parseInt(str.substring(index + 1))};
		} else if (o instanceof Sequence) {
			Sequence seq = (Sequence)o;
			int count = seq.length();
			if (count == 0) {
				return false;
			}

			hosts = new String[count];
			ports = new int[count];
			for (int i = 0; i < count; ++i) {
				Object obj = seq.get(i + 1);
				if(obj==null){
					continue;
				}
				if (obj instanceof String) {
					String str = (String)obj; // ip:port
					int index = str.lastIndexOf(':');
					if (index == -1) {
						return false;
					}

					hosts[i] = str.substring(0, index);
					ports[i] = Integer.parseInt(str.substring(index + 1));
				}else if(obj instanceof UnitClient){
					UnitClient uc = (UnitClient)obj;
					hosts[i] = uc.getHost();
					ports[i] = uc.getPort();
				}else{
					//不认识的对象
					return false;
				}

			}
		} else {
			return false;
		}

		return true;
	}
	
	public int size(){
		if(hosts==null) return 0;
		return hosts.length;
	}
	
	public String getHost(int i) {
		return hosts[i];
	}
	
	public String[] getHosts() {
		return hosts;
	}
	
	public int getPort(int i) {
		return ports[i];
	}
	
	public int[] getPorts() {
		return ports;
	}

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		hosts = (String[])in.readObject();
		ports = (int[])in.readObject();
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(hosts);
		out.writeObject(ports);
	}
}