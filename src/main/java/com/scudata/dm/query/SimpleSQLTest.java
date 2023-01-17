package com.scudata.dm.query;

import com.scudata.app.common.AppUtil;
import com.scudata.app.config.ConfigUtil;
import com.scudata.dm.Context;

public class SimpleSQLTest {
	public static void main(String args[]){
		System.out.println(333);
		try {
			//ConfigUtil.load("d:\\esProcData\\raqsoftConfig.xml");
			System.out.println(444);
			Context ctx = new Context();
			ctx.setParamValue("intofile", "d:/test/intof1,d:/test/intof2,d:/test/intof3");
			Object o = null;
			System.out.println(111);
			//o = AppUtil.executeSql("select * from d:/test/emps.txt", null, ctx);
			System.out.println(222);
			//			o = AppUtil.executeSql("select _size,first_name,gender,_file from d:/test/emps1*.txt", null, ctx);
			//o = AppUtil.executeSql("select gender,first_name from d:/test/emps1*.txt", null, ctx);
			//			o = AppUtil.executeSql("select _size,first_name,gender,_file from d:/test/emps1*.xlsx", null, ctx);
//			o = AppUtil.executeSql("select _file,_size,* from d:/test/emps1*.csv", null, ctx);
//			o = AppUtil.executeSql("select _file,_size,gender from d:/test/emps1*.xlsx", null, ctx);
//			o = AppUtil.executeSql("select _file from d:/test/emps1*.xlsx", null, ctx);
//			o = AppUtil.executeSql("select gender into d:/empresult2.txt from d:/emps.txt group by gender", null, ctx);			
//			o = AppUtil.executeSql("select gender into ${intofile}.txt from d:/emps.txt group by gender", null, ctx);			
			o = AppUtil.executeSql("select * from {T(\"D:/雇员.csv\")} as a join {T(\"D:/雇员.csv\")} as b on a.雇员ID=b.雇员ID", null, ctx);			
//			o = AppUtil.executeSql("select * from D:/雇员.csv as a join D:/雇员.csv as b on a.雇员ID=b.雇员ID", null, ctx);			
//				o = AppUtil.executeSql("select * from {T(\"D:/雇员.csv\")} as a", null, ctx);			
			
			//Object o = AppUtil.executeSql("select gender,max(emp_no) from d:/emps.txt group by gender", null, ctx);
			//Object o = AppUtil.executeSql("select gender,max(hire_date) from d:/emps.txt group by gender", null, ctx);
//			Object o = AppUtil.executeSql("select max(hire_date) maxhire,gender from d:/emps.txt group by gender", null, ctx);
//			Object o = AppUtil.executeSql("select ee.DEPT,max(ee.BIRTHDAY),avg(ee.SALARY) from d:/emp8.btx as ee join d:/dep8.btx as dd on dd.DEPT = ee.DEPT group by ee.DEPT having avg(ee.SALARY) > 8000", null, ctx);
//			Object o = AppUtil.executeSql("select ee.DEPT,max(ee.BIRTHDAY),avg(ee.SALARY) from d:/emp8.btx as ee join d:/dep8.btx as dd on dd.DEPT = ee.DEPT group by ee.DEPT having avg(ee.SALARY) > 8000", null, ctx);
//			Object o = AppUtil.executeSql("select ee.DEPT,max(ee.BIRTHDAY),avg(ee.SALARY) from d:/emp8.btx as ee join d:/dep8.btx as dd on dd.DEPT = ee.DEPT group by ee.DEPT having avg(ee.SALARY) > 8000", null, ctx);
//			Object o = AppUtil.executeSql("select ee.DEPT,max(ee.BIRTHDAY),avg(ee.SALARY) from d:/emp8.btx as ee join d:/dep8.btx as dd on dd.DEPT = ee.DEPT group by ee.DEPT having avg(ee.SALARY) > 8000", null, ctx);
//			Object o = AppUtil.executeSql("select ee.DEPT,max(ee.BIRTHDAY) bir,avg(ee.SALARY) sal from d:/emp8.btx as ee join d:/dep8.btx as dd on dd.DEPT = ee.DEPT group by ee.DEPT having avg(ee.SALARY) > 8000", null, ctx);
//			Object o = AppUtil.executeSql("select * from d:/emp8.btx as ee join d:/dep8.btx as dd on dd.DEPT = ee.DEPT", null, ctx);
//			System.out.println(o);
//			Object o = AppUtil.executeSql("select * from D:/esProcData/员工.ctx where 1 = 2", null, ctx, false);
			//			Object o = AppUtil.executeSql("select top 1 * from D:/esProcData/员工.ctx", null, ctx, false);
			if (o != null) {
				System.out.println(o);
				if (o instanceof com.scudata.dm.cursor.ICursor) {
					System.out.println(((com.scudata.dm.cursor.ICursor)o).fetch());
				} else if (o instanceof com.scudata.dw.Cursor) {
					System.out.println(((com.scudata.dw.Cursor)o).fetch());
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
