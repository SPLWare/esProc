package com.scudata.lib.dynamodb.function;

import java.util.ArrayList;
import java.util.List;
import com.scudata.common.RQException;

public class ImSqlUtil {
	private String[] m_columns;
	private static ImSqlUtil instance = new ImSqlUtil() ;
	
	public static ImSqlUtil getInstance(){
		return instance;
	}

	
    public String trim(String str){
        String values=values(str);

        values=values.substring(values.indexOf("("));

        StringBuffer sb=new StringBuffer(values);
        boolean delete=true;
        boolean flag=true;
        int start=0;
        while (flag){
            char c=sb.charAt(start);
            if(compare(c,'\'')){
                delete=!delete;
            }
            if((compare(c,'\n')&&delete)
                    ||(compare(c,' ')&&delete)){
                sb.deleteCharAt(start);
            }else{
                start++;
            }
            if(start>=sb.length()){
                break;
            }
        }
        return sb.toString();
    }

    private String values(String str) {
        String ls=str.toLowerCase();
        int valueIndex=ls.indexOf("values");

        String values=str.substring(valueIndex);
        return values;
    }
    
    public String tableName(String str) {
    	String s=str.replace("(", " ");
    	String[] ss = s.split(" ");
    	String value="";
    	int n = 0;
        for(String item: ss){
        	if (item.trim().isEmpty()) continue;
        	else{
        		n++;
        	}
        	if (n==3){
        		value = item;
        		break;
        	}
        }
              
        return value.trim();
    }

    public String[] columns(String str) {
    	int valueIndex=str.toLowerCase().indexOf("values");
    	str=str.substring(0, valueIndex);
        
        int start=str.indexOf("(");
        int end=str.indexOf(")");
        if (start<0 || end<0){
        	return null;
        }

        String values=str.substring(start+1,end);
        values=values.replaceAll("\n","")
                	 .replaceAll(" ","");
        
        return values.split(",");
    }

    public List<String> params(String sql){
        String values=trim(sql);
        boolean flag=true;
        int startIndex=0;
        int endIndex=0;
        boolean param=false;
        boolean fh=false;
        int start=1;
        int t=0;
        boolean str=false;
        List<String> params=new ArrayList<>();
        while(flag){
            char pri=values.charAt(start-1);
            char c=values.charAt(start);

            if(!param&&(compare(pri,'(')||compare(pri,','))){
                if(compare(c,'\'')){
                    //startIndex=start+1;
                	startIndex=start;
                    str=true;
                    fh=true;
                }else{
                    startIndex=start;
                    str=false;
                    fh=false;
                }
                param=true;
            }
            //System.out.println(c+"   "+startIndex+"    "+param);
            if(param){
                if(t==0&&(compare(c,',')||compare(c,')'))){
                    if(compare(pri,'\'')&&fh){
                        //endIndex=start-1;
                    	endIndex=start;
                        String v=values.substring(startIndex,endIndex);
                        params.add(v);
                        param=false;
                        //System.out.println("v "+v);
                    }else if(notCompare(pri,'\'')&&!fh){
                        endIndex=start;
                        String v=values.substring(startIndex,endIndex);
                        params.add(v);
                        param=false;
                        //System.out.println("v "+v);
                    }
                }

                if(compare(c,'(')&&!str){
                    t++;
                }

                if(compare(c,')')&&!str&&t>0){
                    t--;
                }
            }

            start++;
            if(start>=values.length()){
                break;
            }
        }
        return params;
    }

    public boolean compare(char c,char c1){
        return c==c1;
    }

    public boolean notCompare(char c,char c1){
        return c!=c1;
    }
    
    public static String convertSqlJson(String insert) {    	
    	ImSqlUtil cls = getInstance();
       
        // tableName
        String tbl = cls.tableName(insert);
        //返回列
        String[] cols=cls.columns(insert);
        
        //返回参数
        List<String> list=cls.params(insert);
        //System.out.println(Arrays.toString(cols)+" 列数量 "+cols.length+" 参数数量 "+list.size());

        int i=0;
        String sql = "insert into "+tbl+" value {";
        if (cols==null){
//			colums按字符排序了，与值对应不上        	
//        	ExecuteStatementResult rst = ImUtils.executeStatementRequest(client, "select * from "+tbl, null);
//        	if(rst.getSdkHttpMetadata().getHttpStatusCode()==200){
//        		List<Map<String, AttributeValue>> ls = rst.getItems();
//        		Set<String> set =  ls.get(0).keySet();
//        		cols = new String[set.size()];
//        		set.toArray(cols);
//        	}
        	throw new RQException("Table columns is not set");
        }

        if (list.size()==cols.length){
	        for(String s:list){        	
	        	sql += "'"+cols[i++]+"':"+s+", "; 
	        }
        }else {
        	for(String s:cols){
        		if (i<list.size()){
        			sql += "'"+s+"':"+list.get(i++)+", "; 
        		}else{
        			sql += "'"+s+"':?, "; 
        		}
	        }
        }
        
        cls.m_columns = cols;
        sql = sql.substring(0, sql.length()-2) + " }";
        System.out.println(sql);
 
        return sql;
    }  
    
    public static String[] getCols(){
    	return getInstance().m_columns;
    }
    
    
    public static void main(String[] args) {
    	//写一个测试sql
    	String sql="insert  into   test (col1, col2 , col3, \n col4,col5,col6,col7,col8) " +
                "values('',123,'测试(彩色',123,'',dateformat(sdfsdf,'erter',\n test(test(111,test('222')))),'测试2\n彩色',now('test',1123))";
    	sql="insert  into test" +
                "values('',123,'测试(彩色',123,'',dateformat(sdfsdf,'erter',\n test(test(111,test('222')))),'测试2\n彩色',now('test',1123))";
    	convertSqlJson(sql);
    }
}