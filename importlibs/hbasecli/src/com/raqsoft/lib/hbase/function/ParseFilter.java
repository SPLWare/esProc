package com.raqsoft.lib.hbase.function;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.filter.MultiRowRangeFilter.RowRange;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Pair;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;

import com.raqsoft.expression.IParam;

// 只处理多参数的Filter
public class ParseFilter {
	private Context ctx;
	private IParam param;
	public ParseFilter(Context c, IParam p)
	{
		ctx = c;
		param = p;
	}
	
	public Object calculate() {		
		int size = param.getSubSize(); //funcName + params(size = 1 + n)
		Object val[]= new Object[size];
		if ((val[0]=ImUtils.checkValidDataType(param.getSub(0), ctx, "String"))==null){
			throw new RQException("Filter Param: " + val[0] + " not unvalid");
		}
		
		String filterName = (String)val[0];
		System.out.println("filter = " + filterName + " size="+size);
		if (filterName.compareToIgnoreCase("singleColumnValueFilter") == 0){ //1.
			if (size!=5){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doSingleColumnValueFilter(val);
			}			
		}else if(filterName.compareToIgnoreCase("SingleColumnValueExcludeFilter") == 0){ //2.
			if (size!=5){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doSingleColumnValueExcludeFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("FamilyFilter") == 0){ //3.
			if (size!=3){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doFamilyFilter(val);
			}			
		}else if(filterName.compareToIgnoreCase("qualifierFilter") == 0){ // 4.
			if (size!=3){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doQualifierFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("ColumnPrefixFilter") == 0){ // 5.
			if (size!=2){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doColumnPrefixFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("MultipleColumnPrefixFilter") == 0){ //6
			if (size<2){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doMultipleColumnPrefixFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("ColumnRangeFilter") == 0){ //7.
			if (size!=5){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doColumnRangeFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("DependentColumnFilter") == 0){ //8
			if (size<3){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doDependentColumnFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("rowFilter") == 0){ // 9
			if (size!=3){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doRowFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("RandomRowFilter") == 0){ //10
			if (size!=2){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doRandomRowFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("SkipFilter") == 0){ //11
			if (size!=2){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doSkipFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("WhileMatchFilter") == 0){ //12
			if (size!=2){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doWhileMatchFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("PageFilter") == 0){ //13 skip 14 firstOnlyFilter
			if (size!=2){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doPageFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("FirstKeyValueMatchingQualifiersFilter") == 0){ //15. skip 16 oneFilter
			if (size<2){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doFirstKeyValueMatchingQualifiersFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("InclusiveStopFilter") == 0){//17
			if (size!=2){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doInclusiveStopFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("ColumnPaginationFilter") == 0){//18
			if (size!=3){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doColumnPaginationFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("ColumnCountGetFilter") == 0){//19
			if (size!=2){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doColumnCountGetFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("FuzzyRowFilter") == 0){//20
			if (size<3){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else if (size%2!=1){
				throw new RQException("FilterName: " + filterName + " param size is not 2N");
			}else{
				return doFuzzyRowFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("PrefixFilter") == 0){//21
			if (size!=2){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doPrefixFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("TimestampsFilter") == 0){//22
			if (size<=2){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doTimestampsFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("ValueFilter") == 0){//23
			if (size!=3){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else{
				return doValueFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("MultiRowRangeFilter") == 0){//24
			if (size<5){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else if ((size-1) % 4!=0){
				throw new RQException("FilterName: " + filterName + " param size is 4 * n");
			}else{
				return doMultiRowRangeFilter(val);
			}
		}else if(filterName.compareToIgnoreCase("ColumnValueFilter") == 0){//25
			if (size<5){
				throw new RQException("FilterName: " + filterName + " param size is " + (size-1));
			}else if ((size-1) % 4!=0){
				throw new RQException("FilterName: " + filterName + " param size is 4 * n");
			}else{
				return doColumnValueFilter(val);
			}	
			
		}else{
			throw new RQException("FilterName: " + filterName + " is not existed");
		}
	}	
	
	///////////////// Filter base Opration start ///////////////////////
	// 1. 列值过滤器
	// FilterIdx: 01 singleColumnValueFilter
	public Filter doSingleColumnValueFilter( Object val[]){		
		// 1. family, qualifier, compareOp
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "String"))==null) ||
			((val[2]=ImUtils.checkValidDataType(param.getSub(2), ctx, "String"))==null) ){
			throw new RQException("Filter "+val[0]+" Param: family, qualifier, compareOp");
		}
		
		val[3]=ImUtils.checkValidDataType(param.getSub(3), ctx, "String"); 
		CompareOperator op = ImUtils.fromSymbol((String)val[3]);
		
		ByteArrayComparable comp = null;
		// 2. XXFilter(abc) //可能为String,Fiilter
		if (((val[4]=ImUtils.checkValidDataTypeWithoutPrompt(param.getSub(4), ctx, "String"))!=null )){
			String regExp = "(\\w+Filter)\\((.*)\\)";
			Pattern p=Pattern.compile(regExp);
			Matcher m=p.matcher((String)val[4]);
			//System.out.println("subStr = " + val[3] + "mgrp = "+m.groupCount());
			if (m.matches() && m.groupCount()==2){
				String compatorName = m.group(0);
				Object compatorParam = m.group(1);
				comp = Comparator.getComparator(compatorName, compatorParam);
				return singleColumnValueFilter((String)val[1], (String)val[2],op, comp);
			}else{
				return singleColumnValueFilter((String)val[1], (String)val[2],op, (String)val[4]);
			}
		}else{
			if (((val[4]=ImUtils.checkValidDataType(param.getSub(4), ctx, "Compator"))==null)){
				throw new RQException("Filter "+val[0]+" Param: family, qualifier, compareOp compator");
			}else{
				return singleColumnValueFilter((String)val[1], (String)val[2], op, (ByteArrayComparable)val[4]);
			}					
		}		
	}
	
	///////////////// Filter base Opration start ///////////////////////
	// 1. 列值过滤器
	// FilterIdx: 01 singleColumnValueFilter
	public static Filter singleColumnValueFilter(String family, String qualifier, CompareOperator op, String value){
//		System.out.println("SingleColumnValueFilter family=" + family + 
//			" column="+qualifier + " val="+value + " op=" + op);
		return new SingleColumnValueFilter(family.getBytes(), qualifier.getBytes(), op, value.getBytes());	
	}
	
	public static Filter singleColumnValueFilter(String family, String qualifier, CompareOperator op, ByteArrayComparable comparator){
		return new SingleColumnValueFilter(family.getBytes(), qualifier.getBytes(), op, comparator);	
	}
	
	// FilterIdx: 02 singleColumnValueExcludeFilter
	public Filter doSingleColumnValueExcludeFilter( Object val[]){
		// 1. family, qualifier, compareOp
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "String"))==null) ||
			((val[2]=ImUtils.checkValidDataType(param.getSub(2), ctx, "String"))==null) ){
			throw new RQException("Filter "+val[0]+" Param: family, qualifier, compareOp");
		}
		
		val[3]=ImUtils.checkValidDataType(param.getSub(3), ctx, "String"); 
		CompareOperator op = ImUtils.fromSymbol((String)val[3]);
		
		//System.out.println("subStr = " + param.getSub(4).getLeafExpression().calculate(ctx));
		// 2. XXFilter(abc)
		if (((val[4]=ImUtils.checkValidDataType(param.getSub(4), ctx, "String"))!=null )){
			return singleColumnValueExcludeFilter((String)val[1], (String)val[2], op, (String)val[4]);
		}else if (((val[4]=ImUtils.checkValidDataType(param.getSub(4), ctx, "Compator"))!=null)){
			return singleColumnValueExcludeFilter((String)val[1], (String)val[2], op, (ByteArrayComparable)val[4]);
		}else{
			throw new RQException("Filter "+val[0]+" Param: family, qualifier, compareOp compator");
		}
	}
	
	// FilterIdx: 02 singleColumnValueExcludeFilter
	public static Filter singleColumnValueExcludeFilter(String family, String qualifier, CompareOperator op, String value){
		return new SingleColumnValueExcludeFilter(family.getBytes(), qualifier.getBytes(), 
			op, value.getBytes());	
	}

	public static Filter singleColumnValueExcludeFilter(String family, String qualifier, CompareOperator op, ByteArrayComparable comparator){
		return new SingleColumnValueExcludeFilter(family.getBytes(), qualifier.getBytes(), op, comparator);	
	}
	
	// 2. 键值元数据过滤器
	// FilterIdx: 03 
	// new FamilyFilter(CompareOperator.EQUAL, new BinaryComparator(Bytes.toBytes("my-family")));
	public Filter doFamilyFilter( Object val[]){
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "CompareOperator"))==null) ||
			((val[2]=ImUtils.checkValidDataType(param.getSub(2), ctx, "Compator"))==null) ){
			throw new RQException("Filter "+val[0]+" Param compareOp Comparator");
		}else{
			return new FamilyFilter(ImUtils.fromSymbol((String)val[1]), (ByteArrayComparable)val[2]);
		}	
	}
	// FilterIdx: 04
	// new QualifierFilter(CompareOperator.EQUAL, new BinaryComparator(Bytes.toBytes("my-column"))); 
	public Filter doQualifierFilter(  Object val[]){
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "String"))==null) ||
			((val[2]=ImUtils.checkValidDataType(param.getSub(2), ctx, "Compator"))==null) ){
			throw new RQException("Filter "+val[0]+" Param compareOp Comparator");
		}else{
			return new QualifierFilter(ImUtils.fromSymbol((String)val[1]), (ByteArrayComparable)val[2]);
		}	
	}
	
	// FilterIdx: 05
	// new ColumnPrefixFilter(Bytes.toBytes("my-prefix"));
	public Filter doColumnPrefixFilter (Object val[]){
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "string"))==null) ){
			throw new RQException("Filter "+val[0]+" Param byte[]");
		}else{			
			return new ColumnPrefixFilter(((String)val[1]).getBytes());
		}			
	}
	
	// FilterIdx: 06
	/*
	 * byte[][] prefixes = new byte[][]{Bytes.toBytes("my-prefix-1"), Bytes.toBytes("my-prefix-2")};
	 * MultipleColumnPrefixFilter filter = new MultipleColumnPrefixFilter(prefixes); 
	 */	
	public Filter doMultipleColumnPrefixFilter (Object[] vals){
		int i = 0, j=0;
		byte [][] prefixes = new byte[vals.length-1][];
		for(Object v:vals){
			if (i==0) {//skip funcName
				i++;
				continue; 
			}
			if (((v=ImUtils.checkValidDataType(param.getSub(i), ctx, "string"))==null) ){
				throw new RQException("Filter "+vals[0]+ " index:" + i +" Param byte[]");
			}else{
				prefixes[j++] = ((String)v).getBytes();	
			}
			i++;
		}
		return new MultipleColumnPrefixFilter(prefixes);
	}
	
	// FilterIdx: 07
	/*
	 * boolean minColumnInclusive = true;
		boolean maxColumnInclusive = true;
		ColumnRangeFilter filter = new ColumnRangeFilter(Bytes.toBytes("minColumn"), 
		minColumnInclusive, Bytes.toBytes("maxColumn"), maxColumnInclusive);
	 */	
	public Filter doColumnRangeFilter (Object[] val){
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "String"))==null) ||
			((val[2]=ImUtils.checkValidDataType(param.getSub(2), ctx, "bool"))==null) ||
			((val[3]=ImUtils.checkValidDataType(param.getSub(3), ctx, "String"))==null) ||
			((val[4]=ImUtils.checkValidDataType(param.getSub(4), ctx, "bool"))==null) ){
				throw new RQException("Filter "+val[0]+"Param type: minColumn minColumnInclusive "+
						" maxColumn maxColumnInclusive");
		}else{
			boolean minColumnInclusive = (boolean)val[2];
			boolean maxColumnInclusive = (boolean)val[4];
			return new ColumnRangeFilter(((String)val[1]).getBytes(), minColumnInclusive, 
					((String)val[3]).getBytes(), maxColumnInclusive);
		}		
	}
	// FilterIdx: 08
	/*new DependentColumnFilter(Bytes.toBytes("family"), Bytes.toBytes("qualifier"));
	 *  public DependentColumnFilter(final byte [] family, final byte[] qualifier,
      final boolean dropDependentColumn, final CompareOperator valueCompareOp,
      final ByteArrayComparable valueComparator)*/
	public Filter doDependentColumnFilter (Object val[]){
		//System.out.println("DependentColumnFilter = " + val.length);
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "String"))==null) ||
			((val[2]=ImUtils.checkValidDataType(param.getSub(2), ctx, "String"))==null) ){
			throw new RQException("Filter "+val[0]+"Param family qualifier");
		}

		if (val.length==3){
			return new DependentColumnFilter(((String)val[1]).getBytes(), ((String)val[2]).getBytes());
		}else if(val.length>3){
			if (((val[3]=ImUtils.checkValidDataType(param.getSub(3), ctx, "bool"))==null)){
				throw new RQException("Filter "+val[0]+"Param family qualifier true/false");
			}
			if (val.length==4){
				//System.out.println("DependentColumnFilter = " + val[3]);
				return new DependentColumnFilter(((String)val[1]).getBytes(), 
					((String)val[2]).getBytes(),(boolean)val[3]);
			}else if(val.length==6){
				if (((val[4]=ImUtils.checkValidDataType(param.getSub(4), ctx, "String"))==null) ||
					((val[5]=ImUtils.checkValidDataType(param.getSub(5), ctx, "Compator"))==null) ){
						throw new RQException("Filter "+val[0]+"Param family qualifier");
				}
				return new DependentColumnFilter(((String)val[1]).getBytes(), 
						((String)val[2]).getBytes(),(boolean)val[3],
						ImUtils.fromSymbol((String)val[4]),(ByteArrayComparable)val[5]);
			}
		}
		
		return null;
	}
	
	// 3. 行键过滤器
	// FilterIdx: 09
	// RowFilter filter = new RowFilter(CompareOperator.EQUAL, new BinaryComparator(Bytes.toBytes("my-row-1")));
	public Filter doRowFilter( Object val[] ){
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "String"))==null) ||
			((val[2]=ImUtils.checkValidDataType(param.getSub(2), ctx, "Compator"))==null) ){
			throw new RQException("Filter "+val[0]+" Param compareOp Comparator");
		}else{
			return new RowFilter(ImUtils.fromSymbol((String)val[1]), (ByteArrayComparable)val[2]);
		}			
	}
	
	// FilterIdx: 10
	// new RandomRowFilter(chance); 
	public Filter doRandomRowFilter( Object val[] ){
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "double"))==null) ){
			throw new RQException("Filter "+val[0]+" Param chance");
		}else{
			return new RandomRowFilter(ImUtils.objectToFloat(val[1]));
		}	
	}
	
	//FilterIdx: 11
	// new SkipFilter(new ValueFilter(CompareOperator.NOT_EQUAL, new BinaryComparator(Bytes.toBytes(0)));
	public Filter doSkipFilter(Object val[] ){
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "Filter"))==null) ){
			throw new RQException("Filter "+val[0]+" Param filter");
		}else{
			return new SkipFilter((Filter)val[1]);
		}
	}
	//FilterIdx: 12
	// new WhileMatchFilter(new PageFilter(120))
	public Filter doWhileMatchFilter(Object val[] ){
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "Filter"))==null) ){
			throw new RQException("Filter "+val[0]+" Param filter");
		}else{
			return new WhileMatchFilter((Filter)val[1]);
		}
	}
	
	// 4.  功能过滤器
	//FilterIdx: 13
	//PageFilter filter = new PageFilter(pageSize);
	public Filter doPageFilter(  Object val[] ){
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "int"))==null) ){
			throw new RQException("Filter "+val[0]+" Param pageSize");
		}else{
			return new PageFilter((int)val[1]);
		}	
	}
	
	//FilterIdx: 14
	// new FirstKeyOnlyFilter();	
	public Filter firstKeyOnlyFilter( ){
		return new FirstKeyOnlyFilter();	
	}
	//FilterIdx: 15
	/*
	Set<byte []> qualifiers = new TreeSet<byte[]>(Bytes.BYTES_COMPARATOR);
	if (qualifiers.size() == 0) {
	    scan.setFilter(new FirstKeyOnlyFilter());
	  } else {
	    scan.setFilter(new FirstKeyValueMatchingQualifiersFilter(qualifiers));
	  }
	*/
	public Filter doFirstKeyValueMatchingQualifiersFilter(Object vals[] ){
		int i = 0;
		Set<byte []> qualifiers = new HashSet<byte []>();
		for(Object v:vals){
			if (i==0) {//skip funcName
				i++;
				continue; 
			}
			if (((v=ImUtils.checkValidDataType(param.getSub(i), ctx, "string"))==null) ){
				throw new RQException("Filter "+vals[0]+ " index:" + i +" Param byte[]");
			}else{
				qualifiers.add(((String)v).getBytes());	
			}
			i++;
		}
		return new FirstKeyValueMatchingQualifiersFilter(qualifiers);
	}
	
	//FilterIdx: 16
	// new KeyOnlyFilter();
	public Filter keyOnlyFilter( ){
		return new KeyOnlyFilter();	
	}
	//FilterIdx: 17
	//  new InclusiveStopFilter(Bytes.toBytes("stopRowKey"));
	public Filter doInclusiveStopFilter (Object val[]){
		if ((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "String"))==null){
			throw new RQException("Filter " + val[0] + " Param: stopRowKey");
		}else{
			//System.out.println("doInclusiveStopFilter" + val[1]);
			return new InclusiveStopFilter(((String)val[1]).getBytes());
		}
	}
	//FilterIdx: 18
	// new ColumnPaginationFilter(limit, columnOffset);
	public Filter doColumnPaginationFilter (Object val[]){
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "int"))==null)){
			throw new RQException("Filter "+val[0]+" Param limit columnOffset");
		}else{
			 if (((val[2]=ImUtils.checkValidDataType(param.getSub(2), ctx, "int"))!=null) ){
				 return new ColumnPaginationFilter((Integer)val[1], (Integer)val[2]);
			 }else if (((val[2]=ImUtils.checkValidDataType(param.getSub(2), ctx, "string"))!=null) ){
				 return new ColumnPaginationFilter((Integer)val[1], ((String)val[2]).getBytes());
			 }
		}
		return null;
	}
	//FilterIdx: 19
	// new ColumnCountGetFilter(2);
	public Filter doColumnCountGetFilter (Object val[]){
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "int"))==null) ){
			throw new RQException("Filter "+val[0]+" Param count");
		}else{			
			return new ColumnCountGetFilter( (Integer)val[1] );
		}
	}
	
	//FilterIdx: 20
	/* FuzzyRowFilter rowFilter = new FuzzyRowFilter(
		 Arrays.asList(new Pair<byte[], byte[]>(
		    Bytes.toBytesBinary("\x00\x00\x00\x00_login_"),
		    new byte[] {1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0})));

		\x00表示?
		1表示不需要匹配
		跳过对应的????(相当于通配符)，只要能匹配上"_login_"(对应为7个0)
	 */
	public Filter doFuzzyRowFilter (Object vals[]){
		try {
			List<Pair<byte[], byte[]>> fuzzyKeysData = new LinkedList<Pair<byte[], byte[]>>();
	
			for(int i = 1; i<vals.length; i+=2){
				if (((vals[1]=ImUtils.checkValidDataType(param.getSub(i), ctx, "string"))==null) ||
					((vals[2]=ImUtils.checkValidDataType(param.getSub(i+1), ctx, "sequence"))==null)){
					throw new RQException("Filter "+vals[0]+ " index:" + i +" Param long");
				}

				int []nt = ((Sequence)vals[2]).toIntArray();
				byte[] bt = new byte[nt.length];
				for(int j=0; j<nt.length; j++){					
					bt[j]=(byte)nt[j];
					//System.out.println(" data222 = " +nt[j] + " bt="+bt[j]);
				}
				Pair<byte[], byte[]> data = new Pair<byte[], byte[]>(Bytes.toBytesBinary((String)vals[1]), bt);
				fuzzyKeysData.add(data);				
			}
			
//			for(int i=0; i<fuzzyKeysData.size();i++){
//				Pair<byte[], byte[]> data = fuzzyKeysData.get(i);
//				System.out.println("xxxxxxxxxxxxx fuzzy = " + Bytes.toString(data.getFirst()) + 
//						" seq=" +  Bytes.toString(data.getSecond()));
//				byte[] ss = data.getSecond();
//				for(int j=0; j<ss.length; j++){
//					System.out.println(" data = " +ss[j]);
//				}
//			}
			return new FuzzyRowFilter( fuzzyKeysData );
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	//FilterIdx: 21
	// new PrefixFilter(Bytes.toBytes("testRowOne"))
	public Filter doPrefixFilter (Object val[]){
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "string"))==null) ){
			throw new RQException("Filter "+val[0]+" Param string");
		}else{			
			return new PrefixFilter( ((String)val[1]).getBytes() );
		}
	}
	
	//FilterIdx: 22
	// new TimestampsFilter(timestamps, false)
	public Filter doTimestampsFilter (Object vals[]){
		int i = 0;
		List<Long> list = new LinkedList<Long>();
		try{
			for(i = 1; i<vals.length-1; i++){
				if (((vals[0]=ImUtils.checkValidDataType(param.getSub(i), ctx, "long"))==null) ){
					throw new RQException("Filter "+vals[0]+ " index:" + i +" Param type is not long");
				}else{
					list.add(ImUtils.objectToLong(vals[0]));	
				}
			}
					
			if (((vals[1]=ImUtils.checkValidDataType(param.getSub(vals.length-1), ctx, "bool"))==null) ){
				throw new RQException("Filter "+vals[0]+ " index:" + i +" Param byte[]");
			}
			return new TimestampsFilter( list, (boolean)vals[1] );
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	//FilterIdx: 23
	//new ValueFilter(CompareOperator.EQUAL, new SubstringComparator(".4") ); 
	public Filter doValueFilter ( Object val[]){
		if (((val[1]=ImUtils.checkValidDataType(param.getSub(1), ctx, "String"))==null) ||
			((val[2]=ImUtils.checkValidDataType(param.getSub(2), ctx, "Compator"))==null) ){
			throw new RQException("Filter "+val[0]+" Param compareOp Comparator");
		}else{
			return new ValueFilter(ImUtils.fromSymbol((String)val[1]), (ByteArrayComparable)val[2]);
		}
	}
	
	//FilterIdx: 24
	/* 暂时不用它，比较繁杂
	 * MultiRowRangeFilter filter = new MultiRowRangeFilter(Arrays.asList(
                new MultiRowRangeFilter.RowRange(key1Start, true, key1End, false),
                new MultiRowRangeFilter.RowRange(key2Start, true, key2End, false)
        ));
        filter.filterRowKey(badKey, 0, 1);
	 */
	private byte[] doMultiRowRangeVal(int i, Object[] vals, Context ctx){
		byte [] bt = null;
		if (((vals[1]=ImUtils.checkValidDataType(param.getSub(i), ctx, "String"))!=null) ){
			bt = Bytes.toBytes(vals[1].toString());
		}else if (((vals[1]=ImUtils.checkValidDataType(param.getSub(i), ctx, "int"))!=null)){
			int n = Integer.parseInt(String.valueOf(vals[1]));
			bt = Bytes.toBytes(n);
		}else if (((vals[1]=ImUtils.checkValidDataType(param.getSub(i), ctx, "decimal"))!=null)){
			BigDecimal n = ImUtils.getBigDecimal(vals[1]);
			bt = Bytes.toBytes(n);
		}
		return bt;
	}
	public Filter doMultiRowRangeFilter (Object vals[]){
		Filter filter = null;
		
		try {
			int i = 0;
			List<RowRange> range = new ArrayList<RowRange>();;
			//System.out.println("MultiRowRangeFilter = " + vals.length);
			byte []low = null;
			byte []high = null;
			for(i = 1; i<vals.length; i+=4){				
				low  = doMultiRowRangeVal(i+0, vals, ctx);
				high = doMultiRowRangeVal(i+2, vals, ctx);
				if (((vals[2]=ImUtils.checkValidDataType(param.getSub(i+1), ctx, "bool"))==null) ||
				    ((vals[4]=ImUtils.checkValidDataType(param.getSub(i+3), ctx, "bool"))==null) ){
					throw new RQException("Filter "+vals[0]+ " index:" + (i+1) + " or "
							+ (i+3) +" Param is boolean");
				}else{
					range.add(new MultiRowRangeFilter.RowRange(low, (boolean)vals[2], high, (boolean)vals[4]));	
				}
			}			
			
			filter = new MultiRowRangeFilter( range );
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return filter;
	}
	
	public Filter doColumnValueFilter(Object vals[]){
		Filter filter = null;
		
		try {
			byte[] bval = null;
			vals[4]=param.getSub(4).getLeafExpression().calculate(ctx);
			if (vals[4] instanceof String){
				bval =Bytes.toBytes((String)vals[4]) ;
			}else if(vals[4] instanceof Integer){
				bval =Bytes.toBytes((Integer)vals[4]) ;
			}
			for(int i=0; i<vals.length-2; i++){
				if (((vals[i+1]=ImUtils.checkValidDataType(param.getSub(i+1), ctx, "string"))==null)){
					throw new RQException("Filter "+vals[0]+ " index:" + (i+1) +" Param is String");
				}
			}
			byte[] family = ((String)vals[1]).getBytes();
			byte[] qualifier = ((String)vals[2]).getBytes();
			CompareOperator op = ImUtils.fromSymbol((String)vals[3]);
			
			filter = new ColumnValueFilter( family, qualifier, op, bval );
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return filter;
	}
	
///////////////// Filter base Opration end ///////////////////////
}
