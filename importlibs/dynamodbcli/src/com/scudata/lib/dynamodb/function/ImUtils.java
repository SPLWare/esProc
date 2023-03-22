package com.scudata.lib.dynamodb.function;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchExecuteStatementRequest;
import com.amazonaws.services.dynamodbv2.model.BatchExecuteStatementResult;
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementRequest;
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementResult;
import com.scudata.common.Logger;

public class ImUtils {
	public static void writeString(String file, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(file);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			bos.write(content.getBytes(), 0, content.getBytes().length);
			bos.flush();
			bos.close();
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
	}

	public static String ReadStringOfFile(String file) {
		try {
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			String content = "";

			byte[] buffer = new byte[512];
			int flag = 0;
			while ((flag = bis.read(buffer)) != -1) {
				content += new String(buffer, 0, flag);
			}

			bis.close();
			return content;
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}

		return null;
	}

	// 输出修改时间，如2009-08-17
	public static String getModifiedTime(String fname) {
		File f = new File(fname);
		Calendar cal = Calendar.getInstance();
		long time = f.lastModified();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		cal.setTimeInMillis(time);
		return formatter.format(cal.getTime());
	}

	public static String getTempTodateFile(String mod) {
		try {
			// create a temp file
			File temp = File.createTempFile("temp-file-name", ".tmp");

			// Get tempropary file path
			String absolutePath = temp.getAbsolutePath();
			String tempFilePath = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
			String OS = System.getProperty("os.name").toLowerCase();
			if (OS.indexOf("windows") >= 0) {
				String s = tempFilePath.replace("Temp", "raqsoft");
				File f = new File(s);
				if (!f.exists()) {
					f.mkdir();
				}
				s = String.format("%s%sjq_%s_token.txt", s, File.separator, mod);
				return s;
			} else {
				return String.format("%s%sraqsoft%sjq_%s_token.txt", tempFilePath, File.separator, File.separator, mod);
			}
		} catch (IOException e) {
			Logger.error(e.getMessage());
		}
		return null;
	}

	// return 0:not json,1:jsonObj, 2:jsonArray
	public static int getJsonTypeByStr(String jsonStr) {
		int nRet = 0;
		try{
			Object object = JSON.parse(jsonStr);
			if (object instanceof JSONObject) {
				nRet = 1;
			} else if (object instanceof JSONArray) {
				nRet = 2;
			} else {
			    //System.out.println("Neither jsonobject nor jsonarray is jsonStr");
			}
		}catch(Exception e){
			
		}

		return nRet;
	}
	
	public static ExecuteStatementResult executeStatementRequest(AmazonDynamoDB client, String statement, List<AttributeValue> parameters ) {
        ExecuteStatementRequest request = new ExecuteStatementRequest();
        request.setStatement(statement);
        if (parameters!=null){
        	request.setParameters(parameters);
        }
        return client.executeStatement(request);
    }
	
	public static BatchExecuteStatementResult batchExecuteStatementRequest(AmazonDynamoDB client, String[] statements ) {
		BatchExecuteStatementRequest requests = new BatchExecuteStatementRequest();
		List<String> ls = Arrays.asList(statements);  
        requests.setStatements((Collection)ls);
       
        return client.batchExecuteStatement(requests);
    }
}
