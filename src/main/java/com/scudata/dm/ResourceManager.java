package com.scudata.dm;

import java.util.ArrayList;

import com.scudata.parallel.IProxy;

public final class ResourceManager {
	private ArrayList<IResource> resources = new ArrayList<IResource>();
	private ArrayList<IProxy> proxys = new ArrayList<IProxy>();

	public void add(IResource resource) {
		synchronized(resources) {
			resources.add(resource);
		}
	}

	public void remove(IResource resource) {
		synchronized(resources) {
			resources.remove(resource);
		}
	}

	public void closeResource() {
		ArrayList<IResource> resources = this.resources;
		synchronized(resources) {
			for (int i = resources.size() - 1; i >= 0; --i) {
				try {
					IResource resource = resources.get(i);
					resource.close();
				} catch (Exception e) {
				}
			}

			resources.clear();
		}
	}
	
	public void addProxy(IProxy proxy) {
		synchronized(proxys) {
			proxys.add(proxy);
		}
	}
	
	public IProxy getProxy(int proxyId) {
		ArrayList<IProxy> proxys = this.proxys;
		synchronized(proxys) {
			for (IProxy proxy : proxys) {
				if (proxy.getProxyId() == proxyId) {
					return proxy;
				}
			}
		}
		
		return null;
	}
	
	public void closeProxy() {
		ArrayList<IProxy> proxys = this.proxys;
		synchronized(proxys) {
			for (IProxy proxy : proxys) {
				proxy.close();
			}
			
			proxys.clear();
		}
	}

	public boolean closeProxy(int proxyId) {
		ArrayList<IProxy> proxys = this.proxys;
		synchronized(proxys) {
			for (int i = proxys.size() - 1; i >= 0; --i) {
				IProxy proxy = proxys.get(i);
				if (proxy.getProxyId() == proxyId) {
					proxy.close();
					proxys.remove(i);
					return true;
				}
			}
		}
		
		return false;
	}
	
	public void close() {
		closeProxy();
		closeResource();
	}
}
