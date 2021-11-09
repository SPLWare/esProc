package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

// ZRANGEBYSCORE key min max [WITHSCORES] [LIMIT offset count]
// redis_zrangebyscore(key, min, max, offset, n, [withscores])
public class RedisSortSetRangeByScore extends RedisBase {
	boolean m_bDouble2 = false; //参数2 否为double类型
	boolean m_bDouble3 = false;  //参数23否为double类型
	boolean m_bReverse = false;	//倒排序
	boolean m_bWithscores = false;
	protected String m_paramTypes2[];
	public Node optimize(Context ctx) { //skip param:WITHSCORES
		m_paramTypes = new String[]{"string","string","string","int","int"};
		m_paramTypes2= new String[]{"string","int","int","int","int"};
		return super.optimize(ctx);
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("redis_client" + mm.getMessage("function.missingParam"));
		}

		String option = getOption();
		// 1. nullComparator, param is null
		if (option!=null && option.equals("z")){
			m_bReverse = true;
		}
		
		int size = param.getSubSize();
		Object cli = new Object();
		Object objs[] = new Object[size-1];

		for(int i=0; i<size; i++){
			if (param.getSub(i) == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("redis_client" + mm.getMessage("function.invalidParam"));
			}
			
			if (i==0){
				cli = param.getSub(i).getLeafExpression().calculate(ctx);
				if (!(cli instanceof RedisTool)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("redis_client" + mm.getMessage("function.paramTypeError"));
				}else{
					m_jedisTool = (RedisTool)cli;					
				}
			}else{
				if (m_paramTypes.length>i-1){
					if ((objs[i-1] = Utils.checkValidDataTypeWithoutPrompt(param.getSub(i), ctx, m_paramTypes[i-1]))==null){
						if ((objs[i-1] = Utils.checkValidDataTypeWithoutPrompt(param.getSub(i), ctx, m_paramTypes2[i-1]))==null){
							if ((objs[i-1] = Utils.checkValidDataTypeWithoutPrompt(param.getSub(i), ctx, "string"))==null){
								throw new RQException("redis_client param " + objs[i-1] + " type is not " + m_paramTypes2[i-1]);
							}else{
								// for check last param : withscores
								if ( "withscores".equalsIgnoreCase(objs[i-1].toString()) ){
									if ((i-1)!=m_paramTypes.length-2){
										throw new RQException("redis_client param " + objs[i-1] + " type is not " + m_paramTypes2[i-1]);
									}
								}
							}
						}else{
							if (i==2){ //[string, int],同时为double才调用double对应的接口
								m_bDouble2 = true;
							}else if (i==3){
								m_bDouble3 = true;
							}
						}			
					}
				}else{ 
					if ((objs[i-1] = Utils.checkValidDataType(param.getSub(i), ctx, "string"))==null){
						throw new RQException("redis_client param " + objs[i-1] + " type is not " + m_paramTypes[i-1]);
					}
				}
			}
		}

		if (m_jedisTool == null){
			throw new RQException("redis_connect is null");
		}
		if (objs.length<1){
			throw new RQException("redis_param is empty");
		}
		
		if ("withscores".equalsIgnoreCase(objs[objs.length-1].toString())){
			m_bWithscores = true;
		}
	
		return doQuery(objs);
	}
	
	public Object doQuery( Object[] objs){
		if (objs.length == 3){
			Set<String> set=null;
			if(m_bDouble2 && m_bDouble3){
				if (m_bReverse){					
					set = m_jedisTool.zReverseRangeByScore(objs[0].toString(), Utils.objectToDouble(objs[1]), Utils.objectToDouble(objs[2]));
					//System.out.println("zzz = " + set.size());
				}else{
					set = m_jedisTool.zRangeByScore(objs[0].toString(), Utils.objectToDouble(objs[1]), Utils.objectToDouble(objs[2]));
				}				
			}else{
				if (m_bReverse){
					set = m_jedisTool.zReverseRangeByScore(objs[0].toString(), Utils.objectToLong(objs[1]), Utils.objectToLong(objs[2]));
				}else{
					set = m_jedisTool.zRangeByScore(objs[0].toString(), Utils.objectToDouble(objs[1]), Utils.objectToDouble(objs[2]));
				}				
			}
			if (set==null) return null;
			return toTableData(set, objs);
			//throw new RQException("redis strlen param size " + objs.length + " is not 1");
		}else if (objs.length==4){
			if (m_bWithscores){				
				Set<TypedTuple<String>> set;
				if(m_bDouble2 && m_bDouble3){
					if (m_bReverse){
						set = m_jedisTool.zReverseRangeByScoreWithScores(objs[0].toString(), Utils.objectToDouble(objs[1]), Utils.objectToDouble(objs[2]));
					}else{
						set = m_jedisTool.zRangeByScoreWithScores(objs[0].toString(), Utils.objectToDouble(objs[1]), Utils.objectToDouble(objs[2]));
					}
				}else{
					if (m_bReverse){
						set = m_jedisTool.zReverseRangeByScoreWithScores(objs[0].toString(), Utils.objectToDouble(objs[1]), Utils.objectToDouble(objs[2]));
					}else{
						set = m_jedisTool.zRangeByScoreWithScores(objs[0].toString(), Utils.objectToDouble(objs[1]), Utils.objectToDouble(objs[2]));
					}
				}
				if (set==null) return null;	
				return toTableTypedTuple(set, objs);
			}
		}else if (objs.length==5){
			Set<String> set1=null;
			Set<TypedTuple<String>> set2=null;
			if(m_bDouble2 && m_bDouble3){
				if (m_bReverse){
					set1 = m_jedisTool.zReverseRangeByScore(objs[0].toString(), Utils.objectToDouble(objs[1]), 
							Utils.objectToDouble(objs[2]),
							Integer.parseInt(objs[3].toString()),
							Integer.parseInt(objs[4].toString()));
				}else{
					set2 = m_jedisTool.zRangeByScoreWithScores(objs[0].toString(), Utils.objectToDouble(objs[1]), 
							Utils.objectToDouble(objs[2]),
							Integer.parseInt(objs[3].toString()),
							Integer.parseInt(objs[4].toString()));
				}
			}else{
				if (m_bReverse){
					set1 = m_jedisTool.zReverseRangeByScore(objs[0].toString(), Utils.objectToDouble(objs[1]), 
							Utils.objectToDouble(objs[2]),
							Utils.objectToLong(objs[3].toString()),
							Utils.objectToLong(objs[4].toString()));
				}else{
					set2 = m_jedisTool.zRangeByScoreWithScores(objs[0].toString(), Utils.objectToDouble(objs[1]), 
							Utils.objectToDouble(objs[2]),
							Utils.objectToLong(objs[3].toString()),
							Utils.objectToLong(objs[4].toString()));
				}
			}
			if (set1!=null){
				return toTableData(set1, objs);
			}else if (set2!=null){
				return toTableTypedTuple(set2, objs);
			}
		}else if (objs.length==6){
			if (m_bWithscores){	
				Set<TypedTuple<String>> set = null;
				if(m_bDouble2 && m_bDouble3){
					if (m_bReverse){
						set = m_jedisTool.zReverseRangeByScoreWithScores(objs[0].toString(), Utils.objectToDouble(objs[1]), 
								Utils.objectToDouble(objs[2]),
								Utils.objectToLong(objs[3].toString()),
								Utils.objectToLong(objs[4].toString()));
					}else{
						set = m_jedisTool.zRangeByScoreWithScores(objs[0].toString(), Utils.objectToDouble(objs[1]), 
								Utils.objectToDouble(objs[2]),
								Utils.objectToLong(objs[3].toString()),
								Utils.objectToLong(objs[4].toString()));
					}
				}else{
					if (m_bReverse){
						set = m_jedisTool.zReverseRangeByScoreWithScores(objs[0].toString(), Utils.objectToDouble(objs[1]), 
								Utils.objectToDouble(objs[2]),
								Utils.objectToLong(objs[3].toString()),
								Utils.objectToLong(objs[4].toString()));
					}else{
						set = m_jedisTool.zRangeByScoreWithScores(objs[0].toString(), Utils.objectToDouble(objs[1]), 
								Utils.objectToDouble(objs[2]),
								Utils.objectToLong(objs[3].toString()),
								Utils.objectToLong(objs[4].toString()));
					}
				}
				if (set==null) return null;
				return toTableTypedTuple(set, objs);
			}
		}
		
		return null;
	}
	
	private Table toTableData(Set<String> set, Object objs[]){
		if (set.size()>0){
			Object os[] = new Object[1];
			os[0]=objs[0];
			m_colNames = new String[]{objs[0].toString()}; //for columns
			
			List<Object[]> list = new ArrayList<Object[]>();
			for( Iterator<String> it = set.iterator();  it.hasNext(); ){  
				os = new Object[1];
				os[0] = it.next().toString();
	            list.add(os);            
	        } 
			return toTable(list);
		}
		
		return null;
	}
	
	private Table toTableTypedTuple(Set<TypedTuple<String>> set, Object objs[]){
		if (set.size()>0){
			Object os[] = null;
			m_colNames = new String[]{objs[0]+"_key", objs[0]+"_val"}; //for columns
			
			List<Object[]> list = new ArrayList<Object[]>();
			for(TypedTuple<String> tp:set){
				os = new Object[2];
				os[0] = tp.getValue();
				os[1] = tp.getScore();
	            list.add(os);   
			}
			return toTable(list);
		}
		
		return null;
	}
	
}
