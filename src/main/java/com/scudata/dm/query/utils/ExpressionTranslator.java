package com.scudata.dm.query.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.scudata.common.Logger;

public class ExpressionTranslator //只能处理中间过程中的表达式，不能直接用来处理SQL语句
{
	public static List<String> marksList = new ArrayList<String>();
	public static List<String> funcsList = new ArrayList<String>();

	public static String translateExp(String exp, Map<String, String> trMap)
	{
		if(exp == null || exp.isEmpty() || trMap == null || trMap.isEmpty())
		{
			return exp;
		}
		Logger.debug("translateExp beg : "+ exp);		
		exp = moveOutMarks(exp);//保护可能包含字段名的字符串""
		exp = moveOutFuncs(exp, trMap);//保护与字段同名的函数fun(
		
		//长的在前，短的在后，防止长的包含短的
		String[] keyBuf = new String[trMap.size()];
		trMap.keySet().toArray(keyBuf);
		Arrays.sort(keyBuf, new NameComprator()); 
		
		List<String> keyList = new ArrayList<String>();
		List<String> valueList = new ArrayList<String>();
		for(String key : keyBuf)
		{
			String value = trMap.get(key);
			keyList.add(key);
			valueList.add(value);
		}
		
		String specials = "^$.[]*\\?+{}|()";//正则表达式的特殊字符
		
		//对付大小写同时存在的情况
		Map<String, List<String>> ignoreCaseMap = new LinkedHashMap<String, List<String>>();
		for(int i = 0; i < keyList.size(); i++)
		{
			String key = keyList.get(i);
			List<String> ignoreCaseList = ignoreCaseMap.get(key.toLowerCase());
			if(ignoreCaseList != null)
			{
				ignoreCaseList.add(key);
			}
			else
			{
				ignoreCaseList = new ArrayList<String>();
				ignoreCaseList.add(key);
				ignoreCaseMap.put(key.toLowerCase(), ignoreCaseList);
			}
		}
		
		//先用不可见标识替换所有key，防止中途替换的value中包含后面的key
		for(Map.Entry<String, List<String>> ignoreCaseEntry : ignoreCaseMap.entrySet())
		{
			List<String> ignoreCaseList = ignoreCaseEntry.getValue();
			for(String sameKeyIgnoreCase : ignoreCaseList)
			{
				int index = keyList.indexOf(sameKeyIgnoreCase);
				StringBuffer sb = new StringBuffer();
				for(int k = 0; k < sameKeyIgnoreCase.length(); k++)
				{
					char ch = sameKeyIgnoreCase.charAt(k);
					if(specials.indexOf(ch) != -1)
					{
						sb.append('\\');//正则表达式的特殊字符要加\
						sb.append(ch);
					}
					else
					{
						sb.append(ch);
					}
				}
				sameKeyIgnoreCase = sb.toString();
				exp = exp.replace(sameKeyIgnoreCase, "" + (char)30 + (index + 1) + (char)31);
			}
			
			String sameKeyIgnoreCase = ignoreCaseList.get(0);
			int index = keyList.indexOf(sameKeyIgnoreCase);
			StringBuffer sb = new StringBuffer();
			for(int k = 0; k < sameKeyIgnoreCase.length(); k++)
			{
				char ch = sameKeyIgnoreCase.charAt(k);
				if(specials.indexOf(ch) != -1)
				{
					sb.append('\\');//正则表达式的特殊字符要加\
					sb.append(ch);
				}
				else
				{
					sb.append(ch);
				}
			}
			sameKeyIgnoreCase = sb.toString();
			exp = exp.replaceAll("(?i)" + sameKeyIgnoreCase, "" + (char)30 + (index + 1) + (char)31);
		}
		
		//最后用value替换所有不可见标识
		for(int j = 0; j < valueList.size(); j++)
		{
			String value = valueList.get(j);
			exp = exp.replace("" + (char)30 + (j + 1) + (char)31, value);
		}
		
		exp = moveInMarks(exp);
		exp = moveInFuncs(exp);

		Logger.debug("translateExp end : "+ exp);		

		return exp;
	}
	
	public static String translateExp2(String exp, Map<String, String> trMap)
	{
		if(exp == null || exp.isEmpty() || trMap == null || trMap.isEmpty())
		{
			return exp;
		}
		
		//长的在前，短的在后，防止长的包含短的
		String[] keyBuf = new String[trMap.size()];
		trMap.keySet().toArray(keyBuf);
		Arrays.sort(keyBuf, new NameComprator()); 
		
		List<String> keyList = new ArrayList<String>();
		List<String> valueList = new ArrayList<String>();
		for(String key : keyBuf)
		{
			String value = trMap.get(key);
			keyList.add(key);
			valueList.add(value);
		}
		
		String specials = "^$.[]*\\?+{}|()";//正则表达式的特殊字符
		
		//对付大小写同时存在的情况
		Map<String, List<String>> ignoreCaseMap = new LinkedHashMap<String, List<String>>();
		for(int i = 0; i < keyList.size(); i++)
		{
			String key = keyList.get(i);
			List<String> ignoreCaseList = ignoreCaseMap.get(key.toLowerCase());
			if(ignoreCaseList != null)
			{
				ignoreCaseList.add(key);
			}
			else
			{
				ignoreCaseList = new ArrayList<String>();
				ignoreCaseList.add(key);
				ignoreCaseMap.put(key.toLowerCase(), ignoreCaseList);
			}
		}		
				
		//先用不可见标识替换所有key，防止中途替换的value中包含后面的key
		for(Map.Entry<String, List<String>> ignoreCaseEntry : ignoreCaseMap.entrySet())
		{
			List<String> ignoreCaseList = ignoreCaseEntry.getValue();
			for(String sameKeyIgnoreCase : ignoreCaseList)
			{
				int index = keyList.indexOf(sameKeyIgnoreCase);
				StringBuffer sb = new StringBuffer();
				for(int k = 0; k < sameKeyIgnoreCase.length(); k++)
				{
					char ch = sameKeyIgnoreCase.charAt(k);
					if(specials.indexOf(ch) != -1)
					{
						sb.append('\\');//正则表达式的特殊字符要加\
						sb.append(ch);
					}
					else
					{
						sb.append(ch);
					}
				}
				sameKeyIgnoreCase = sb.toString();
				exp = exp.replace(sameKeyIgnoreCase, "" + (char)30 + (index + 1) + (char)31);
			}
			
			String sameKeyIgnoreCase = ignoreCaseList.get(0);
			int index = keyList.indexOf(sameKeyIgnoreCase);
			StringBuffer sb = new StringBuffer();
			for(int k = 0; k < sameKeyIgnoreCase.length(); k++)
			{
				char ch = sameKeyIgnoreCase.charAt(k);
				if(specials.indexOf(ch) != -1)
				{
					sb.append('\\');//正则表达式的特殊字符要加\
					sb.append(ch);
				}
				else
				{
					sb.append(ch);
				}
			}
			sameKeyIgnoreCase = sb.toString();
			exp = exp.replaceAll("(?i)" + sameKeyIgnoreCase, "" + (char)30 + (index + 1) + (char)31);
		}		
		
		//最后用value替换所有不可见标识
		for(int j = 0; j < valueList.size(); j++)
		{
			String value = valueList.get(j);
			exp = exp.replace("" + (char)30 + (j + 1) + (char)31, value);
		}
		
		return exp;
	}
	
	public static boolean containExp(String exp, Set<String> scSet)
	{
		if(exp == null || exp.isEmpty() || scSet == null || scSet.isEmpty())
		{
			return false;
		}
		
		for(String sc : scSet)
		{
			if(sc != null && !sc.isEmpty() && exp.toLowerCase().indexOf(sc.toLowerCase()) != -1)
			{
				return true;
			}
		}
		
		return false;
	}
	
	private static String moveOutMarks(String exp)
	{
		Pattern pattern = Pattern.compile("\"([\\s\\S]*?)\"");
		Matcher matcher = pattern.matcher(exp);
		while(matcher.find())
		{
			String marks = matcher.group(0);
			marksList.add(marks);
			int index = exp.indexOf(marks);
			exp = new StringBuffer(exp).replace(index, index + marks.length(), "" + (char)14 + marksList.size() + (char)15).toString();
		}
		return exp;
	}
	
	private static String moveInMarks(String exp)
	{
		for(int i = 0, sz = marksList.size(); i < sz; i++)
		{
			String marks = marksList.get(i);
			int index = exp.indexOf("" + (char)14 + (i + 1) + (char)15);
			exp = new StringBuffer(exp).replace(index, index + ("" + (char)14 + (i + 1) + (char)15).length(), marks).toString();
		}
		marksList.clear();
		return exp;
	}
	
	private static String moveOutFuncs(String exp, Map<String, String> trMap)
	{
		Next:
		for(String name : trMap.keySet())
		{
			for(int i = 0; i < name.length(); i++)
			{
				char ch = name.charAt(i);
				if(i == 0)
				{
					if(!Character.isJavaIdentifierStart(ch))
					{
						continue Next;
					}
				}
				else
				{
					if(!Character.isJavaIdentifierPart(ch))
					{
						continue Next;
					}
				}
			}

			Pattern pattern = Pattern.compile("(?i)" + name + "\\(");
			Matcher matcher = pattern.matcher(exp);
			while(matcher.find())
			{
				String func = matcher.group(0);
				funcsList.add(func);
				int index = exp.indexOf(func);
				exp = new StringBuffer(exp).replace(index, index + func.length(), "" + (char)16 + funcsList.size() + (char)17).toString();
			}
		}
		return exp;
	}
	
	private static String moveInFuncs(String exp)
	{
		for(int i = 0, sz = funcsList.size(); i < sz; i++)
		{
			String func = funcsList.get(i);
			int index = exp.indexOf("" + (char)16 + (i + 1) + (char)17);
			exp = new StringBuffer(exp).replace(index, index + ("" + (char)16 + (i + 1) + (char)17).length(), func).toString();
		}
		funcsList.clear();
		return exp;
	}
}
