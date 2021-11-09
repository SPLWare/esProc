package com.raqsoft.lib.hbase.function;

import java.math.BigDecimal;

import org.apache.hadoop.hbase.filter.BigDecimalComparator;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.BinaryComponentComparator;
import org.apache.hadoop.hbase.filter.BinaryPrefixComparator;
import org.apache.hadoop.hbase.filter.BitComparator;
import org.apache.hadoop.hbase.filter.BitComparator.BitwiseOp;
import org.apache.hadoop.hbase.filter.ByteArrayComparable;
import org.apache.hadoop.hbase.filter.NullComparator;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.SubstringComparator;
import org.apache.hadoop.hbase.filter.LongComparator;
import org.apache.hadoop.hbase.util.Bytes;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.dm.Sequence;

public class Comparator extends Function {
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {	
		try{
			IParam param = this.param;
			String option = getOption();
			// 1. nullComparator, param is null
			if (option!=null && option.equals("n")){
				return new NullComparator();
			}
			
			if (param == null ) {
				throw new RQException( "comparator param is null ");
			}
			
			int size = param.getSubSize();
			//System.out.println("comparator option= " + option+" size="+size);
			// 无子节点
			if (size == 0){
				Object obj = param.getLeafExpression().calculate(ctx);
				//System.out.println("comparator Object= " +obj);
				if (option==null){
					return new BinaryComparator(((String)obj).getBytes());
				}else if (option.compareTo("p")==0){ //BinaryPrefixComparator
					return new BinaryPrefixComparator(((String)obj).getBytes()); 
				}else if (option.compareTo("s")==0){ //SubstringComparator
					return new SubstringComparator((String)obj); 
				}else if (option.compareTo("l")==0){ //LongComparator
					return new LongComparator(ImUtils.objectToLong(obj)); 
				}else if (option.compareTo("r")==0){ //RegexStringComparator
					return new RegexStringComparator((String)obj); 
				}else if (option.compareTo("d")==0){ //RegexStringComparator
					if (obj instanceof BigDecimal) {
		                return new BigDecimalComparator((BigDecimal) obj);
					}else if(obj instanceof Integer){
						BigDecimal bd = new BigDecimal(((Integer)obj).toString());
						return new BigDecimalComparator(bd);
					}else if(obj instanceof String){
						BigDecimal bd = new BigDecimal((String)obj);
						return new BigDecimalComparator(bd);
					}else{
						throw new RQException("BigDecimalComparator param type is not BigDecimal"); 
					}
					
				}else{ //BinaryComparator
					throw new RQException( "comparator "+option+" param is not support");
				}
			}
			
			if (size==2){			
				Object obj0 = null ,obj1 = null;
				if (option.equals("r")){ //RegexStringComparator
					if (((obj0=ImUtils.checkValidDataType(param.getSub(0), ctx, "String"))==null)|| 
						((obj1=ImUtils.checkValidDataType(param.getSub(1), ctx, "String"))==null) ){
						MessageManager mm = EngineMessage.get();
						throw new RQException("comparator param1 " + mm.getMessage("function.invalidParam"));
					}
					int nPatten = ImUtils.getRegexPattern((String)obj1);
					return new RegexStringComparator((String)obj0, nPatten);
				}else if (option.equals("b")){ //BitComparator
					//System.out.println("BitComparator ................");
					if (((obj1=ImUtils.checkValidDataType(param.getSub(1), ctx, "String"))==null)){
						throw new RQException("bitcomparator param 2 function.invalidParam");
					}
					
					try {
						if (((obj0=ImUtils.checkValidDataType(param.getSub(0), ctx, "Sequence"))!=null)){
							int []nt = ((Sequence)obj0).toIntArray();
							byte[] bt = new byte[nt.length];
							for(int j=0; j<nt.length; j++){					
								bt[j]=(byte)nt[j];
								//System.out.println(" data222 = " +nt[j] + " bt="+bt[j]);
							}
							return new BitComparator(bt, ImUtils.GetBitwiseOp((String)obj1));
						}else if(((obj0=ImUtils.checkValidDataType(param.getSub(0), ctx, "String"))!=null)){
							String s = (String)obj0;
							return new BitComparator(Bytes.toBytes(s), ImUtils.GetBitwiseOp((String)obj1));
						}					
					} catch (Exception e) {
						e.printStackTrace();
					}
				}else if (option.equals("c")){ //BinaryComponentComparator
					byte[] bt  = null;
					int offset = -1;
					
					obj0 = param.getSub(0).getLeafExpression().calculate(ctx);
					obj1 = param.getSub(1).getLeafExpression().calculate(ctx);				
					if (obj0 instanceof String) {
						bt = Bytes.toBytes((String)obj0);
					}else if (obj0 instanceof Integer) {
						bt = Bytes.toBytes((Integer)obj1);
					}
					
					if (obj1 instanceof Integer) {
						offset =(Integer)obj1;
					}
				 
					if (bt==null || offset == -1){
						throw new RQException("BinaryComponentComparator params error."); 
					}
					
					return new BinaryComponentComparator(bt, offset);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static ByteArrayComparable getComparator(String comparatorName, Object param)
	{
		ByteArrayComparable comp = null;
		if (comparatorName.compareToIgnoreCase("RegexStringComparator")==0){
			comp = new RegexStringComparator((String)param);
		}else if (comparatorName.compareToIgnoreCase("SubstringComparator")==0){
			comp = new SubstringComparator((String)param);
		}else if (comparatorName.compareToIgnoreCase("BinaryPrefixComparator")==0){
			comp = new BinaryPrefixComparator(((String)param).getBytes());
		}else if (comparatorName.compareToIgnoreCase("BinaryComparator")==0){
			comp = new BinaryComparator(((String)param).getBytes());
		}else if (comparatorName.compareToIgnoreCase("LongComparator")==0){
			if (param instanceof Long){
				comp = new LongComparator((Long)param);
			}
		}else if (comparatorName.compareToIgnoreCase("NullComparator")==0){
			comp = new NullComparator(); //ignore param
		}else if (comparatorName.compareToIgnoreCase("BitComparator")==0){			
			comp = new BitComparator(((String)param).getBytes(), BitwiseOp.AND);
		}else if (comparatorName.compareToIgnoreCase("BigDecimalComparator")==0){		
			if (param instanceof BigDecimal) {
                comp = new BigDecimalComparator((BigDecimal) param);
			}else if(param instanceof String){
				BigDecimal bd = new BigDecimal((String)param);
				comp = new BigDecimalComparator(bd);
			}else{
				throw new RQException("BigDecimalComparator param type is not BigDecimal"); 
			}
		}
//		else if (comparatorName.compareToIgnoreCase("BinaryComponentComparator")==0){		
//			if (param instanceof Integer) {
//                comp = new BigDecimalComparator((BigDecimal) param);
//			}else if(param instanceof String){
//				BigDecimal bd = new BigDecimal((String)param);
//				comp = new BigDecimalComparator(bd);
//			}else{
//				throw new RQException("BigDecimalComparator param type is not BigDecimal"); 
//			}
//		}
		return comp; 
	}
}
