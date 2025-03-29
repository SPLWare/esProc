package com.scudata.ide.vdb.control;

import com.scudata.common.SegmentSet;
import com.scudata.vdb.Library;
import com.scudata.vdb.VDB;

public class ConnectionConfig{
	private String name;
	
	private String url;
	private int port=0;
	private String user;
	private String password;
	private boolean reservePassword = true;

	private boolean isConnected = false;
	private Library lib = null;
	private VDB vdb = null;
	
	private boolean editChanged = false;
	private Library getLibrary(){
		if(lib==null){
			lib = new Library(url);
			lib.start();
		}
		return lib;
	}
	
	public String toString(){
		SegmentSet ss = new SegmentSet();
		ss.put("name", name);
		ss.put("url", url);
		ss.put("port", port+"");
		ss.put("user", user);
		ss.put("password", password);
		ss.put("reservePassword", Boolean.toString(reservePassword));
		return ss.toString();
	}
	
	public static ConnectionConfig fromString(String config){
		ConnectionConfig cc = new ConnectionConfig();
		SegmentSet ss = new SegmentSet(config);
		cc.name = ss.get("name");
		cc.url = ss.get("url");
		String buf = ss.get("port");
		try{
		cc.port=Integer.parseInt(buf);
		}catch(Exception x){
			return null;
		}
		
		cc.user = ss.get("user");
		cc.password = ss.get("password");
		
		buf = ss.get("reservePassword");
		cc.reservePassword= new Boolean(buf);
		return cc;
	}
	
	public VDB connect(){
		if(!isConnected){
			lib = getLibrary();
			vdb = lib.createVDB();
			vdb.begin();
		}
		isConnected = true;
		return vdb;
	}
	
	public void setEditChanged(){
		editChanged = true;
	}
	
	public boolean isEditChanged(){
		return editChanged;
	}
	
	public boolean commit() throws Exception{
		if(vdb!=null){
			int r = vdb.commit();
			if( r==0 ){
				editChanged = false;
				return true;
			}
			throw new Exception("提交数据库失败，错误码为："+r);
		}
		throw new Exception("提交数据库失败，数据库没有启动。");
	}
	
	public void rollback(){
		if(vdb!=null){
			vdb.rollback();
			editChanged = false;
		}
	}
		
	public void close(){
		if(vdb!=null){
			vdb.close();
			vdb = null;
		}
		if(lib!=null){
			lib.stop();
			lib = null;
		}
		isConnected = false;
	}
	
	public boolean isConnected(){
		return isConnected;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public boolean isReservePassword() {
		return reservePassword;
	}
	public void setReservePassword(boolean reservePassword) {
		this.reservePassword = reservePassword;
	}
	
}