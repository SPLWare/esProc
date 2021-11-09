package com.raqsoft.lib.hbase.function;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.hbase.filter.BitComparator.BitwiseOp;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.filter.ByteArrayComparable;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.filter.Filter;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.IParam;

public class ImUtils {
	public static CompareOperator fromSymbol(String v){
		CompareOperator op = CompareOperator.EQUAL;
		
		if (v == null || "".equals(v)){
			op = CompareOperator.NO_OP;
		}else if (v.equals("<") || v.equals("lt")){
			op = CompareOperator.LESS;
		}else if (v.equals("<=") || v.equals("le")){
			op = CompareOperator.LESS_OR_EQUAL;
		}else if (v.equals("=") || v.equals("eq")){
			op = CompareOperator.EQUAL;
		}else if (v.equals("!=") || v.equals("ne")|| v.equals("neq")){
			op = CompareOperator.NOT_EQUAL;
		}else if (v.equals(">=") || v.equals("ge")){
			op = CompareOperator.GREATER_OR_EQUAL;
		}else if (v.equals(">") || v.equals("gt")){
			op = CompareOperator.GREATER;
		}else if (v.equals("<>") || v.equals("nop")){
			op = CompareOperator.NO_OP;
		}else {
			op = CompareOperator.EQUAL;
		}
		
		return op;
	}
	
	//java.util.regex.Pattern
	//i=case insensitive, m=multiline, d=dotall, u=unicode case, q=canon_eq, x=unix_lines, l=literal, c=comments
	public static int getRegexPattern(String s){
		int nRet = 0;
		for(int i=0; i<s.length(); i++){
			if (s.charAt(i)=='i'){
				nRet |= Pattern.CASE_INSENSITIVE; 
			}else if(s.charAt(i)=='m'){
				nRet |= Pattern.MULTILINE; 
			}else if(s.charAt(i)=='d'){
				nRet |= Pattern.DOTALL; 
			}else if(s.charAt(i)=='u'){
				nRet |= Pattern.UNICODE_CASE; 
			}else if(s.charAt(i)=='q'){
				nRet |= Pattern.CANON_EQ; 
			}else if(s.charAt(i)=='x'){
				nRet |= Pattern.UNIX_LINES; 
			}else if(s.charAt(i)=='l'){
				nRet |= Pattern.LITERAL; 
			}else if(s.charAt(i)=='c'){
				nRet |= Pattern.COMMENTS; 
			}else if(s.charAt(i)=='n'){
				nRet |= Pattern.UNICODE_CHARACTER_CLASS; 
			}else {
				throw new RQException("regex param: " + s.charAt(i) + " not existed" );
			}
		}
		return nRet;
	}
	
	// 缺省用AND
	public static BitwiseOp GetBitwiseOp(String v) {
		if ("and".equalsIgnoreCase(v)) {
			return BitwiseOp.AND;
		} else if ("or".equalsIgnoreCase(v)) {
			return BitwiseOp.OR;
		} else if ("xor".equalsIgnoreCase(v)) {
			return BitwiseOp.XOR;
		}else {
			return BitwiseOp.AND;
		}
	}
	
	public static Object checkValidDataTypeWithoutPrompt(IParam sub, Context ctx, String type){
		Object retObj = null;
		do{
			if (sub == null ) {
				break;
			}
			//System.out.println("checkValidDataType::IParam= "+sub);
			Object o = sub.getLeafExpression().calculate(ctx);
			if (type.equalsIgnoreCase("Compator")){
				if (!(o instanceof ByteArrayComparable)) {
					break;
				}
			}else if (type.equalsIgnoreCase("CompateOp")){
				if (!(o instanceof CompareOp)) {
					break;
				}
			}else if (type.equalsIgnoreCase("Filter")){
				if (!(o instanceof Filter)) {
					break;
				}
			}else if (type.equalsIgnoreCase("int")){
				if (!(o instanceof Integer)) {
					break;
				}
			}else if (type.equalsIgnoreCase("long")){
				if (!(o instanceof Long)) {
					break;
				}
			}else if (type.equalsIgnoreCase("bool")){
				if (!(o instanceof Boolean)) {
					break;
				}
			}else if (type.equalsIgnoreCase("decimal")){
				if (!(o instanceof BigDecimal)) {
					break;
				}				
			}else if (type.equalsIgnoreCase("double")){
				if (!(o instanceof Double)) {
					break;
				}
			}else if (type.equalsIgnoreCase("byte")){
				if (!(o instanceof byte[])) {
					break;
				}
			}else{
				if (o==null){
					break;
				}else if (!(o instanceof String)) {
					break;
				}
			}
			
			retObj = o;
		}while(false);
		
		return retObj;
	}
	 
	public static Object checkValidDataType(IParam sub, Context ctx, String type){
		Object retObj = null;
		do{
			if (sub == null ) {
				break;
			}
			Object o = sub.getLeafExpression().calculate(ctx);
			if (type.equalsIgnoreCase("Compator")){
				if (!(o instanceof ByteArrayComparable)) {
					System.out.println("instanceof = " + o + " is not " + type);
					break;
				}
			}else if (type.equalsIgnoreCase("CompateOp")){
				if (!(o instanceof CompareOp)) {
					System.out.println("instanceof = " + o + " is not " + type);
					break;
				}
			}else if (type.equalsIgnoreCase("Filter")){
				if (!(o instanceof Filter)) {
					System.out.println("instanceof = " + o + " is not " + type);
					break;
				}
			}else if (type.equalsIgnoreCase("int")){
				if (!(o instanceof Integer)) {
					System.out.println("instanceof = " + o + " is not " + type);
					break;
				}
			}else if (type.equalsIgnoreCase("long")){
				if (!(o instanceof Long)) {
					System.out.println("instanceof = " + o + " is not " + type);
					break;
				}
			}else if (type.equalsIgnoreCase("bool")){
				if (!(o instanceof Boolean)) {
					System.out.println("instanceof = " + o + " is not " + type);
					break;
				}
			
			}else if (type.equalsIgnoreCase("double")){
				if (!(o instanceof Double)) {
					System.out.println("instanceof = " + o + " is not " + type);
					break;
				}
			}else if (type.equalsIgnoreCase("byte")){
				if (!(o instanceof byte[])) {
					System.out.println("instanceof = " + o + " is not " + type);
					break;
				}
			}else if (type.equalsIgnoreCase("Sequence")){
				if (!(o instanceof Sequence)) {
					System.out.println("instanceof = " + o + " is not " + type);
					break;
				}
			}else{
				if (o==null){
					break;
				}else if (!(o instanceof String)) {
					System.out.println("instanceof = " + o + " is not String");
					break;
				}
			}
			
			retObj = o;
		}while(false);
		
		return retObj;
	}

	// 不支持float, 只支持double;
	public static long objectToLong(Object Obj)  
	{  
	    //return Long.valueOf(String.valueOf(Obj)); 
		double d = Double.valueOf(String.valueOf(Obj));
		return (long)d;
	} 
	
	public static float objectToFloat(Object Obj)  
	{  
	    return Float.valueOf(String.valueOf(Obj));  
	} 
	
	public static double objectToDouble(Object Obj)  
	{  
	    return Double.valueOf(String.valueOf(Obj));  
	} 
 
	/*
	 * bool、int、long、float(64位)、decimal、string，
	 * 以后再补充,不认识的类型返回byte[]，用户可以写函数转换
	 */
	public static boolean checkColumnType(TableInfo tableInfo, String colFullName, String type){
		boolean bRet = false;
		if (type.compareToIgnoreCase("string")==0 ||
			type.compareToIgnoreCase("long")==0   ||
			type.compareToIgnoreCase("float")==0  ||
			type.compareToIgnoreCase("decimal")==0 ){
			bRet = true;
			tableInfo.setColumnType(colFullName, type.toLowerCase());
		}else if (type.compareToIgnoreCase("int")==0 ||
				  type.compareToIgnoreCase("integer")==0){
			bRet = true;
			tableInfo.setColumnType(colFullName, "int");
		}else{
			bRet = true;
			tableInfo.setColumnType(colFullName, "bytes");
		}
		
		return bRet;
	}
	
	// 转换成Table前将object转换成对应的类型
	public static Object getDataType(TableInfo tableInfo, String colFullName, Object data){
		Object oRet = null;
		
		String type = tableInfo.m_columnType.get(colFullName);
		//System.out.println("d=" + data + " type="+type + " key="+colFullName);
		if (type.compareToIgnoreCase("string")==0){
			oRet = (String)data;
		}else if (type.compareToIgnoreCase("int")==0){
			oRet = Integer.parseInt(String.valueOf(data));
		}else if (type.compareToIgnoreCase("long")==0){
			oRet = ImUtils.objectToLong(data);
		}else if (type.compareToIgnoreCase("float")==0){
			oRet = Float.valueOf(String.valueOf(data));  
		}else if (type.compareToIgnoreCase("decimal")==0){
			oRet = getBigDecimal(data);
		}else{
			oRet = (String) data;
		}
		
		return oRet;
	}
	
	 public static BigDecimal getBigDecimal( Object value ) {  
        BigDecimal ret = null;  
        if( value != null ) {  
            if( value instanceof BigDecimal ) {  
                ret = (BigDecimal) value;  
            } else if( value instanceof String ) {  
                ret = new BigDecimal( (String) value );  
            } else if( value instanceof BigInteger ) {  
                ret = new BigDecimal( (BigInteger) value );  
            } else if( value instanceof Number ) {  
                ret = new BigDecimal( ((Number)value).doubleValue() );  
            } else {  
                throw new ClassCastException("Not possible to coerce ["+value+"] from class "+value.getClass()+" into a BigDecimal.");  
            }  
        }  
        return ret;  
    }
	 
	public static boolean isRegExpMatch(String strSrc, String regExp, Matcher[] retMatch)
	{
		if (strSrc==null || strSrc.isEmpty()){
			throw new RQException("hive isMatch strUrl is empty");
		}
		
		if (regExp==null || regExp.isEmpty()){
			throw new RQException("hive isMatch regExp is empty");
		}
		
		Pattern p=Pattern.compile(regExp);
		retMatch[0] = p.matcher(strSrc);
		
		//groupCount=N由regExp来决定，暂时用2
		return retMatch[0].matches() && (retMatch[0].groupCount()==2); 
	}
	 
	 /**   
     * 对象转数组   
     * @param obj   
     * @return   
     */    
    public static byte[] toByteArray (Object obj) {        
        byte[] bytes = null;        
              
        try {  
        	if (obj==null) return null;
        	ByteArrayOutputStream bos = new ByteArrayOutputStream();  
            ObjectOutputStream oos = new ObjectOutputStream(bos);           
            oos.writeObject(obj);          
            oos.flush();           
            bytes = bos.toByteArray ();        
            oos.close();           
            bos.close();          
        } catch (IOException ex) {          
            ex.printStackTrace();     
        }        
        return bytes;      
    }     
         
    /**   
     * 数组转对象   
     * @param bytes   
     * @return   
     */    
    public static Object toObject (byte[] bytes) {        
        Object obj = null;        
        try {
        	if (obj==null) return null;   
            ByteArrayInputStream bis = new ByteArrayInputStream (bytes);          
            ObjectInputStream ois = new ObjectInputStream (bis);          
            obj = ois.readObject();        
            ois.close();     
            bis.close();     
        } catch (IOException ex) {          
            ex.printStackTrace();     
        } catch (ClassNotFoundException ex) {          
            ex.printStackTrace();     
        }        
        return obj;      
    }     
    
    public static boolean checkFunctionExisted(Class<?> cls, String funcName, Method[] m) {
    	boolean bExisted = false;
		Method[] methods = cls.getMethods();
		
		for (int idx = 0; idx < methods.length; idx++) {
			//System.out.println("func = " + methods[idx].getName());
			if (methods[idx].getName().equals(funcName)) {
				bExisted = true;
				m = new Method[1];
				m[0] = methods[idx]; 		
				break;
			}
		}
		
		return bExisted;
	}
	
    public static void format(Result result){        
    	List<Cell> cells = result.listCells();
        for (Cell c : cells) {
        	String rowkey = Bytes.toString(CellUtil.cloneRow(c));
            String family= Bytes.toString(CellUtil.cloneFamily(c));
            String qualifier= Bytes.toString(CellUtil.cloneQualifier(c));
            System.out.println("rowkey->"+rowkey+" family->"+family
            		+" qualifier->"+qualifier+" val->"+Bytes.toString(CellUtil.cloneValue(c)));
        }
    }
}