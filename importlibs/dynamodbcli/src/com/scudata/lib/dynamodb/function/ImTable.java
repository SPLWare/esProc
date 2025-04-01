package com.scudata.lib.dynamodb.function;

import java.util.Arrays;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.scudata.common.Logger;
import com.scudata.common.RQException;

//conn.create(fp, tableName, partitionKey, type, sortKey, type)
public class ImTable extends ImFunction {
	
	public Object doExec(Object[] objs){
		boolean bRet = true;
		try {
			com.amazonaws.services.dynamodbv2.document.Table table = null;
			if (m_db==null){
				throw new RQException("dyna_create paramTypeError2");
			}
			
			if (option!=null && option.indexOf("d")!=-1){
				if (objs.length<1){
					throw new RQException("drop table missingParam");
				}
				table = m_db.m_dynamoDB.getTable(objs[0].toString());
				table.delete();
				table.waitForDelete();
			}else{
				if (objs.length<2 ){
					throw new RQException("create table missingParam");
				}
				
				//check type of params
				for(int i=0; i<objs.length; i++){
					if (objs[i] instanceof String){
	
					}else{
						throw new RQException("dyna_create paramTypeError");
					}
				}
				
				int i=0;	
				Object[] vs = null;
				
				// 不指定type为"S"
				// 1.param: name, partKey
				if (objs.length==2){ 
					vs = new Object[3];
					for(Object o:objs){
						vs[i++] = o;
					}
					vs[objs.length] = "S";
				}else if (objs.length==3){ 			//2.param: name, partKey, ?[type or sortkey]
					String s = objs[2].toString();
					if (s.equals("S") || s.equals("N") ||s.equals("B") ){	//2.1 param: name, partKey, type]
						vs = new Object[3];
						for(Object o:objs){
							vs[i++] = o;
						}
					}else{ 							//2.2 param: name, partKey, sortkey]
						vs = new Object[5];
						vs[0]=objs[0]; vs[1]=objs[1];
						vs[3]=objs[2]; vs[2]=vs[4]="S";
					}
				}else {	// 3.objs.length==4/5 (param: name, partKey, type, sortkey[,type])				
					vs = new Object[5];
					for(Object o:objs){
						vs[i++] = o;
					}
					if (objs.length==4){ 			
						vs[objs.length] = "S";
					}					
				}
				
				if (vs.length==5){
					// 声明分区键和排序键
					table = m_db.m_dynamoDB.createTable(vs[0].toString(),	                
		                Arrays.asList(new KeySchemaElement(vs[1].toString(), KeyType.HASH),                                                      
		                    new KeySchemaElement(vs[3].toString(), KeyType.RANGE)), 
		                Arrays.asList(new AttributeDefinition(vs[1].toString(), vs[2].toString()),
		                    new AttributeDefinition(vs[3].toString(), vs[4].toString()) ),
		                new ProvisionedThroughput(10L, 10L));		            
				}else if (vs.length==3){
					table = m_db.m_dynamoDB.createTable(vs[0].toString(),	               
		                Arrays.asList(new KeySchemaElement(vs[1].toString(), KeyType.HASH)), 
		                Arrays.asList(new AttributeDefinition(vs[1].toString(), vs[2].toString()) ),
		                new ProvisionedThroughput(10L, 10L));
				}
				
				table.waitForActive();
			}
		}catch(Exception e) {
			bRet = false;
			Logger.error(e.getMessage());
		}
		
		return bRet;
	}
	
	
}
