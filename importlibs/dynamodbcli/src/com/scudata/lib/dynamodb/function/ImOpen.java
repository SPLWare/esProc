package com.scudata.lib.dynamodb.function;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Node;
import com.amazonaws.util.StringUtils;

//fp=dyan_open(url, region, AccessKey, SecretKey)
public class ImOpen extends ImFunction {
	public AmazonDynamoDB m_client = null;
	public DynamoDB m_dynamoDB = null;
	public Table m_table = null;
	
	public static AmazonDynamoDB getAmazonDynamoDBClient(String url, String region,
														 String accessKeyId, String secretKey) {
		if (url==null && region!=null){
			return AmazonDynamoDBClient.builder().withRegion(region).build();
		}else if ( (accessKeyId==null || accessKeyId.isEmpty()) && (secretKey==null || secretKey.isEmpty()) ){
			return AmazonDynamoDBClientBuilder.standard()
					.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(url, region)).build();
		}else{
			BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, secretKey);
			return AmazonDynamoDBClientBuilder.standard()
					.withCredentials(new AWSStaticCredentialsProvider(awsCreds))
					.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(url, region)).build();
		}
	}
	
	public Node optimize(Context ctx) {
		return this;
	}

	public Table getTableByName(String tblName){
		return m_dynamoDB.getTable(tblName);
	}
	
	public Object doExec(Object[] objs){
		try {
			String url = null;
			if (objs.length<2){
				throw new RQException("dyna_open missingParam");
			}
			if (objs[0]==null){
				//skip;
			}else if (!(objs[0] instanceof String && objs[1] instanceof String)){
				throw new RQException("dyna_open paramTypeError");
			}else{
				url = objs[0].toString();
			}
			
			String accessKeyId=null;
			String secretKey=null;
			if (objs.length>=3){
				if (objs[2] instanceof String){
					accessKeyId = objs[2].toString();
				}
			}
			if (objs.length>=4){
				if (objs[2] instanceof String){
					secretKey = objs[3].toString();
				}
			}
			
			m_client = getAmazonDynamoDBClient(url, objs[1].toString(), accessKeyId, secretKey);
			m_dynamoDB = new DynamoDB(m_client);
			
			return this;
		}catch(Exception e) {
			Logger.error(e.getMessage());
		}
		
		return null;
	}
	
	public void close(){
		if (m_dynamoDB!=null){
			m_dynamoDB.shutdown();
			m_dynamoDB = null;
		}
		
		if (m_client!=null){
			m_client.shutdown();
			m_client = null;
		}
	}
}
