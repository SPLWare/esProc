package com.raqsoft.lib.informix.helper;

import java.util.ArrayList;
import java.util.List;

//分片规则
enum RULE_TYPE{
	RULE_UNKNOW,
	RULE_ROBIN, 	//R
	RULE_EXP,   	//E
	RULE_REMAINDER, //E
	RULE_LIST, 		//L
	RULE_INTERVAL 	//N
};

public class Fragment
{	
	private String	m_tableName;
	private String	m_fieldName;
	private String	m_fieldType;
	private List<String>	m_comparison; //比较符号，用","分隔,前>小后格式存放[>;<]
	private Object	m_minValue;
	private Object	m_maxValue;
	private int		m_nFieldType;
	private char	m_nRuleType;  //分片规则
	private List<Object> m_list;
	private int		m_interval;  //间隔值
	private int		m_segment[]; //查询时使用的分片起始范围
	private ORDER_TYPE m_orderby = ORDER_TYPE.ORDER_NORMAL;
	
	public enum ORDER_TYPE{
		ORDER_NORMAL, 			//对sql顺序不调整，保持原样
		ORDER_NO,     			//不要求有序，可加db参数PDQ
		ORDER_FORCE   			//要求有序
	}
	
	public Fragment(){
		m_segment = new int[2];
		m_segment[0] = m_segment[1];
		m_nRuleType = 'U';
		m_list = new ArrayList<Object>();
		m_comparison = new ArrayList<String>();
	}
	// 1. tableName,分片表
	public void setTableName(String tableName)				
	{
		m_tableName = tableName;
	}
	//分片表
	public String getTableName()				
	{
		return m_tableName;
	}
	
	// 2. maxValue
	public void setMaxValue(Object maxValue)	
	{
		m_maxValue = maxValue;
	}
	
	public Object getMaxValue()			
	{
		return m_maxValue;
	}
	
	// 3. minValue
	public void setMinValue(Object minValue)	
	{
		m_minValue = minValue;
	}
	
	public Object getMinValue()				
	{
		return m_minValue;
	}
	
	// 4. FieldName, 分片字段
	public void setFieldName(String fieldName)				
	{
		m_fieldName = fieldName;
	}
	
	public String getFieldName()				
	{
		return m_fieldName;
	}
	// 3 FieldType
	public void setFieldTypeStr(String fieldType)				
	{
		m_fieldType = fieldType;
	}
	
	//分片字段数据类型, 参见java.sql.Types
	public String getFieldTypeStr()					
	{
		return m_fieldType;
	}		
	
	public void setFieldType(int fieldType)			
	{
		m_nFieldType = fieldType;
	}
	//分片字段数据类型, 参见java.sql.Types
	public int getFieldType()						
	{
		return m_nFieldType;
	}		
	
	// 4. PartitionCount
	public void addPartition(Object partition)		
	{
		m_list.add(partition);
	}
	
	public void setPartitionVal(String sVal)		
	{
		m_list.clear();
		if (sVal==null) return;
		
		String[] ary = sVal.split(",");
		for(String s:ary){
			m_list.add(s);
		}	
	}
	
	public void setPartitionVal(int index, Object val)
	{
		if (val==null) return;
		if (index>m_list.size()) return;
		
		m_list.set(index, val);
	}
	//分片数
	public int getPartitionCount()					
	{
		return m_list.size();
	}
	
	public String getPartitionString()				
	{
		String sRet = "kkk";
		for(Object line: m_list){
			if (line == null){
				continue;
			}else{
				sRet += ","+line.toString();
			}
		}
		
		return sRet.replace("kkk,", "");
	}
	
	// 5. Partition
	public Object getPartition(int partition)		
	{
		if (m_list.size()<partition){
			return null;
		}else{
			return m_list.get(partition);
		}
	}
	
	public List<Object> getPartitionMap(){
		return m_list;
	}
	
	public void setSegment(int start, int end){
		m_segment[0] = start;
		m_segment[1] = end;
	}
	
	public int[] getSegment(){
		return m_segment;
	}
	
	public int getSegmentStart(){
		return m_segment[0];
	}
	
	public int getSegmentEnd(){
		return m_segment[1];
	}
	
	public int getInterval(){
		return m_interval;
	}
	
	public void setInterval(int n){
		m_interval = n;
	}
	
	public char getRuleType(){
		return m_nRuleType; 
	}
	
	public void setRuleType(char type){
		m_nRuleType = type;
	}
	
	public void addComparison(String val)
	{
		m_comparison.add(val);
	}
	
	public int getComparisonCount()
	{
		return m_comparison.size();
	}
	//比较符
	public String getComparison(int index)	
	{
		if (index>=m_comparison.size()) return null;
		return m_comparison.get(index);
	}
	
	public void setOrderby( ORDER_TYPE orderby) {
		m_orderby = orderby;
	}
	
	public ORDER_TYPE getOrderby() {
		return m_orderby;
	}
}