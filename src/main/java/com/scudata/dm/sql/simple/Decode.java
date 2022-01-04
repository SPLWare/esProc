package com.scudata.dm.sql.simple;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.JobSpace;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.query.utils.SQLValueComprator;
import com.scudata.expression.Expression;

public class Decode implements IFunction
{
	public static Context ctx = null;
	
	public String getFormula(String[] params)
	{
		boolean hasDefault = false;
		if(params.length % 2 == 0)
		{
			hasDefault = true;
		}
		
		TreeMap<Object, String> treeMap = new TreeMap<Object, String>(new SQLValueComprator());
		int itemLength = (hasDefault ? params.length - 2 : params.length - 1) / 2;
		boolean canotUseTable = false;
		if(itemLength > 10)
		{
			for(int i = 0; i < itemLength; i++)
			{
				if(!canotUseTable)
				{
					try
					{
						Object obj = new Expression(params[2 * i + 1]).calculate(new Context());
						if(!treeMap.containsKey(obj))
						{
							treeMap.put(obj, String.format("%s,%s", params[2 * i + 1], params[2 * i + 2]));
						}
					}
					catch(Exception ex)
					{
						canotUseTable = true;
					}
				}
			}
			
			if(hasDefault)
			{
				try
				{
					new Expression(params[params.length - 1]).calculate(new Context());
				}
				catch(Exception ex)
				{
					canotUseTable = true;
				}
			}
			
			if(!canotUseTable)
			{
				Iterator<Map.Entry<Object, String>> iter = treeMap.entrySet().iterator();
				
				StringBuffer sb = new StringBuffer();
				while(iter.hasNext())
				{
					if(sb.length() > 0)
					{
						sb.append(",");
					}
					else
					{
						sb.append("create(#_1,_2).record([");
					}
					sb.append(iter.next().getValue()); 
					
				}
				sb.append("])");
				
				String tempVar = newEnvJVar(ctx);
				if(hasDefault)
				{
					return String.format("ifn(if(ifv(%s),%s,env@j(%s,%s)).find@b(%s).(#2),%s)", tempVar, tempVar, tempVar, sb.toString(), params[0], params[params.length - 1]);
				}
				else
				{
					return String.format("ifn(if(ifv(%s),%s,env@j(%s,%s)).find@b(%s).(#2))", tempVar, tempVar, tempVar, sb.toString(), params[0]);
				}
			}
		}

		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < itemLength; i++)
		{
			if(i > 0)
			{
				sb.append(",");
			}
			
			sb.append(params[2 * i + 1] + ":" + params[2 * i + 2]);
		}
		
		if(hasDefault)
		{
			return String.format("case(%s,%s;%s)", params[0], sb.toString(), params[params.length - 1]);
		}
		else
		{
			return String.format("case(%s,%s)", params[0], sb.toString());
		} 
	}
	
	private static long varIndex = 1L;
	public static String newEnvJVar(Context context)
	{
		Boolean contain = null;
		String varName = null;
		do
		{
			contain = false;
			
			varName =  "$_" + varIndex;
			varIndex++;
			
			if(context != null)
			{
				JobSpace job = context.getJobSpace();
				if(job != null)
				{
					Param param = job.getParam(varName);
					if(param != null && param.getName().equalsIgnoreCase(varName))
					{
						contain = true;
						break;
					}
				}
				
				if(!contain)
				{
					ParamList paramList = context.getParamList();
					if(paramList != null)
					{
						for(int p = 0; p < paramList.count(); p++)
						{
							Param param = paramList.get(p);
							if(param != null && param.getName().equalsIgnoreCase(varName))
							{
								contain = true;
								break;
							}
						}
					}
				}
			}

			if(!contain)
			{
				ParamList paramList = Env.getParamList();
				if(paramList != null)
				{
					for(int p = 0; p < paramList.count(); p++)
					{
						Param param = paramList.get(p);
						if(param != null && param.getName().equalsIgnoreCase(varName))
						{
							contain = true;
							break;
						}
					}
				}
			}
		}
		while(contain);
		
		return varName;
	}

}
