package com.raqsoft.lib.datastax;

import java.net.InetSocketAddress;
import java.util.List;

import com.datastax.driver.core.Cluster.Initializer;

public abstract class InitializerProxy{

	public abstract Initializer buildInitializer();
	
	public List<String> addresses;
	public int port;
	public String username;
	public String password;
	public String compressor;//"lz4/snappy"
	
}
