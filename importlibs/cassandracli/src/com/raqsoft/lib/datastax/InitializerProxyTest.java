package com.raqsoft.lib.datastax;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.Cluster.Initializer;
import com.datastax.driver.core.Configuration;
import com.datastax.driver.core.Host.StateListener;
import com.datastax.driver.core.ProtocolOptions;

public class InitializerProxyTest extends InitializerProxy{

	public Initializer buildInitializer(){
		final InitializerProxy curr = this;
		Initializer init = new Initializer(){

			@Override
			public String getClusterName() {
				return "c"+System.currentTimeMillis();
			}

			@Override
			public Configuration getConfiguration() {
				Builder bu = Cluster.builder();
	    		bu.addContactPoints(curr.addresses.toArray(new String[curr.addresses.size()]));
			    if (curr.port > 0) bu.withPort(curr.port);
			    if (curr.username != null) bu.withCredentials(curr.username, curr.password);
			    if ("lz4".equalsIgnoreCase(curr.compressor)) {
			    	bu.withCompression(ProtocolOptions.Compression.LZ4);	    	
			    } else if ("snappy".equalsIgnoreCase(curr.compressor)) {
			    	bu.withCompression(ProtocolOptions.Compression.SNAPPY);	    	
			    }
			    Cluster cluster = bu.build();
			    return cluster.getConfiguration();
			}

			@Override
			public List<InetSocketAddress> getContactPoints() {
				List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
				for (int i=0; i<curr.addresses.size(); i++) {
					addresses.add(new InetSocketAddress(curr.addresses.get(i),port));
				}
				return addresses;
			}

			@Override
			public Collection<StateListener> getInitialListeners() {
				// TODO Auto-generated method stub
				return new ArrayList<StateListener>();
			}
			
		};
		return init;
	}
	
}
