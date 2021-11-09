package com.raqsoft.ide.custom;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.raqsoft.common.DBConfig;

/**
 * Server interface. Used to log in, log out, open files, connect to data
 * sources, upload files, etc.
 */
public interface Server {
	/**
	 * Get server name
	 * 
	 * @return Server name
	 */
	public String getName();

	/**
	 * Set server name
	 * 
	 * @param name
	 *            Server name
	 */
	public void setName(String name);

	/**
	 * Get URL
	 * 
	 * @return URL
	 */
	public String getUrl();

	/**
	 * Set URL
	 * 
	 * @param url
	 *            URL
	 */
	public void setUrl(String url);

	/**
	 * Login
	 * 
	 * @param user
	 *            User name
	 * @param pwd
	 *            Password
	 * @return Whether the login is successful
	 */
	public boolean login(String user, String pwd);

	/**
	 * Open file input stream
	 * 
	 * @param fileName
	 *            Remote file name
	 * @return File InputStream
	 */
	public InputStream open(String fileName);

	/**
	 * Save the file to the server
	 * 
	 * @param fileName
	 *            Remote file name
	 * @param fileBytes
	 *            The byte array of the file
	 */
	public void save(String fileName, byte[] fileBytes);

	/**
	 * Upload local file to the server
	 * 
	 * @param fileName
	 *            Remote file name
	 * @param localFile
	 *            Locale file
	 */
	public void save(String fileName, File localFile);

	/**
	 * Get file informations in the specified directory
	 * 
	 * @param path
	 *            Remote file path. When path is null or "/", it means root.
	 * @return List of file informations in the specified directory
	 */
	public List<FileInfo> listFiles(String path);

	/**
	 * Get the data source configuration available to the current user.
	 * 
	 * @return Data source list (the user have permission to access)
	 */
	public List<DBConfig> getDBConfigList();

	/**
	 * Logout
	 */
	public void logout();
}