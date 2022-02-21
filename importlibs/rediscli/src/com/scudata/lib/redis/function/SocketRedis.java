package com.scudata.lib.redis.function;

import java.util.ArrayList;
import java.util.List;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.Node;

public class SocketRedis extends Function {
	SocketRedisOpen sj = null;
	
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		try {
			if (param != null && !param.isLeaf() && param.getSubSize()==2) {
				sj = (SocketRedisOpen)(param.getSub(0).getLeafExpression().calculate(ctx));
				Sequence c = (Sequence)param.getSub(1).getLeafExpression().calculate(ctx);
				Object r = null;
				switch(c.length()){
			    case 1 :
			    	r = sj.redis.call(c.get(1));
			    	break;
			    case 2 :
			    	r = sj.redis.call(c.get(1),c.get(2));
			    	break;
			    case 3 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3));
			    	break;
			    case 4 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4));
			    	break;
			    case 5 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5));
			    	break;
			    case 6 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5),c.get(6));
			    	break;
			    case 7 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5),c.get(6),c.get(7));
			    	break;
			    case 8 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5),c.get(6),c.get(7),c.get(8));
			    	break;
			    case 9 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5),c.get(6),c.get(7),c.get(8),c.get(9));
			    	break;
			    case 10 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5),c.get(6),c.get(7),c.get(8),c.get(9),c.get(10));
			    	break;
			    case 11 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5),c.get(6),c.get(7),c.get(8),c.get(9),c.get(10),c.get(11));
			    	break;
			    case 12 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5),c.get(6),c.get(7),c.get(8),c.get(9),c.get(10),c.get(11),c.get(12));
			    	break;
			    case 13 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5),c.get(6),c.get(7),c.get(8),c.get(9),c.get(10),c.get(11),c.get(12),c.get(13));
			    	break;
			    case 14 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5),c.get(6),c.get(7),c.get(8),c.get(9),c.get(10),c.get(11),c.get(12),c.get(13),c.get(14));
			    	break;
			    case 15 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5),c.get(6),c.get(7),c.get(8),c.get(9),c.get(10),c.get(11),c.get(12),c.get(13),c.get(14),c.get(15));
			    	break;
			    case 16 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5),c.get(6),c.get(7),c.get(8),c.get(9),c.get(10),c.get(11),c.get(12),c.get(13),c.get(14),c.get(15),c.get(16));
			    	break;
			    case 17 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5),c.get(6),c.get(7),c.get(8),c.get(9),c.get(10),c.get(11),c.get(12),c.get(13),c.get(14),c.get(15),c.get(16),c.get(17));
			    	break;
			    case 18 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5),c.get(6),c.get(7),c.get(8),c.get(9),c.get(10),c.get(11),c.get(12),c.get(13),c.get(14),c.get(15),c.get(16),c.get(17),c.get(18));
			    	break;
			    case 19 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5),c.get(6),c.get(7),c.get(8),c.get(9),c.get(10),c.get(11),c.get(12),c.get(13),c.get(14),c.get(15),c.get(16),c.get(17),c.get(18),c.get(19));
			    	break;
			    case 20 :
			    	r = sj.redis.call(c.get(1),c.get(2),c.get(3),c.get(4),c.get(5),c.get(6),c.get(7),c.get(8),c.get(9),c.get(10),c.get(11),c.get(12),c.get(13),c.get(14),c.get(15),c.get(16),c.get(17),c.get(18),c.get(19),c.get(20));
			    	break;
			    default :
			    	throw new RQException("redis_redis, not support more than 20 parameters");
				}
				return parseRedisResult(r);
			}
	    	throw new RQException("redis_redis, parameter not correct");
		} catch (Exception e) {
	    	throw new RQException("redis_redis, execute error",e);
		}

	}
	
	
	public static Object parseRedisResult(Object o) throws Exception {
		if (o instanceof byte[]) {
			return new String((byte[])o,"UTF-8");
		} if (o instanceof List) {
			Sequence seq = new Sequence();
			List l = (List)o;
			for (int i=0; i<l.size(); i++) {
				seq.add(parseRedisResult(l.get(i)));
			}
			return seq;
		}
		return o;
		
	}
}
