package com.scudata.app.config;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.common.ICloneable;

/**
 * XML for Analysis configuration
 */
public class Xmla implements Cloneable, ICloneable, Externalizable {
	/**
	 * Xmla name
	 */
	private String name;
	/**
	 * Xmla type. SSAS/SAP/ESS
	 */
	private String type;
	/**
	 * Xmla URL
	 */
	private String url;
	/**
	 * Xmla catalog
	 */
	private String catalog;
	/**
	 * Xmla user
	 */
	private String user;
	/**
	 * Xmla password
	 */
	private String password;

	/**
	 * Constructor
	 */
	public Xmla() {
	}

	/**
	 * Get Xmla name
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set Xmla name
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get Xmla type. SSAS/SAP/ESS
	 * 
	 * @return
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set Xmla type. SSAS/SAP/ESS
	 * 
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Get Xmla URL
	 * 
	 * @return
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set Xmla URL
	 * 
	 * @param url
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Get Xmla catalog
	 * 
	 * @return
	 */
	public String getCatalog() {
		return catalog;
	}

	/**
	 * Set Xmla catalog
	 * 
	 * @param catalog
	 */
	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	/**
	 * Get xmla user
	 * 
	 * @return
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Set xmla user
	 * 
	 * @param user
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Get xmla password
	 * 
	 * @return
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set xmla password
	 * 
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Deep clone
	 * 
	 * @return
	 */
	public Xmla deepClone() {
		Xmla xmla = new Xmla();
		xmla.setName(name);
		xmla.setType(type);
		xmla.setUrl(url);
		xmla.setCatalog(catalog);
		xmla.setUser(user);
		xmla.setPassword(password);
		return xmla;
	}

	/**
	 * Realize serialization
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		/* Version number */
		out.writeByte(1);
		out.writeObject(name);
		out.writeObject(type);
		out.writeObject(url);
		out.writeObject(catalog);
		out.writeObject(user);
		out.writeObject(password);
	}

	/**
	 * Realize serialization
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		/* Version number */
		in.readByte();
		name = (String) in.readObject();
		type = (String) in.readObject();
		url = (String) in.readObject();
		catalog = (String) in.readObject();
		user = (String) in.readObject();
		password = (String) in.readObject();
	}
}
