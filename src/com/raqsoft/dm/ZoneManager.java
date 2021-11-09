package com.raqsoft.dm;

import java.util.List;

import com.raqsoft.parallel.HostManager;
import com.raqsoft.parallel.Request;
import com.raqsoft.parallel.Response;
import com.raqsoft.server.unit.UnitServer;

/**
 * 内存加载区管理器
 * @author Joancy
 *
 */
public class ZoneManager {
	public static Response execute(Request req) {
		Response res = new Response();
		switch (req.getAction()) {
		case Request.ZONE_INITDFX:
			List args = (List) req.getAttr(Request.EXECDFX_ArgList);
			res = UnitServer.init((Integer)args.get(0), (Integer)args.get(1), (String)args.get(2));
			break;
		}
		Exception x = res.getException();
		if(x!=null){
			HostManager hm = HostManager.instance();
			String msg = "["+hm+"] ";
			String causemsg = x.getMessage();
			if(causemsg.startsWith("[")){
				msg = causemsg;
			}else{
				msg += causemsg;
			}
			x = new Exception( msg, x );
			res.setException(x);
		}
		
		return res;
	}

}
