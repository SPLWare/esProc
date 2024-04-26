import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

public class JDBCTest {
	private static final int CONNECTION_COUNT = 10; // 线程
	private static final int STATEMENT_COUNT = 5; // 顺序

	private static final String DRIVER_ESPROC = "com.esproc.jdbc.InternalDriver";
	private static final String DRIVER_ESPROCQ = "com.esproc.jdbc.QDriver";
	private static final String DRIVER_QJDBC = "com.scudata.ecloud.jdbc.CloudDriver";
	private static final String DRIVER_DATALOGIC = "com.datalogic.jdbc.LogicDriver";
	private static final String DRIVER_DQL = "com.esproc.dql.jdbc.DQLDriver";

	private static final String URL_ESPROC = "jdbc:esproc:local://?onlyServer=true";
	private static final String URL_ESPROCQ = "jdbc:esproc:q:local://?onlyServer=true";
	private static final String URL_AWS = "jdbc:scudata:ecloud://54.223.221.124:8080/qvs?verify=aws-admin&waittime=30&ideltime=600";
	private static final String URL_LOCAL_QVS = "jdbc:scudata:ecloud://?verify=demoqvs&waittime=30&ideltime=600&qvsconfig=z:/work/test/classes/qvsConfig.xml";
	private static final String URL_WINHONG = "jdbc:scudata:ecloud://192.168.199.126:8090/qvs?verify=demoqvs";
	private static final String URL_DATALOGIC = "jdbc:datalogic://127.0.0.1:3366/datalogic";
	private static final String URL_DQL = "jdbc:esproc:dql://127.0.0.1:3368/?dqlmode=dql&user=root&password=root";

	private static final String DRIVER = DRIVER_ESPROC;
	private static final String URL = URL_ESPROC;

	public static void main(String[] args) {
		try {
			Driver d = (Driver) Class.forName(DRIVER).newInstance();
			DriverManager.registerDriver(d);
			for (int i = 0; i < CONNECTION_COUNT; i++) {
				Thread t = new Thread() {
					public void run() {
						try {
							executeConnection();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				t.start();
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private static void executeConnection() throws Exception {
		Connection con = null;
		try {
			Properties prop = new Properties();
			// prop.setProperty("user", "root");
			// prop.setProperty("password", "root");
			Driver d = DriverManager.getDriver(URL);
			con = d.connect(URL, prop);
			for (int i = 0; i < STATEMENT_COUNT; i++)
				executeStatement(con);
		} finally {
			if (con != null)
				try {
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
	}

	private static void executeStatement(Connection con) throws Exception {
		CallableStatement cst = null;
		PreparedStatement pst = null;
		ResultSet rs;
		try {
			// DatabaseMetaData dbmd = con.getMetaData();
			// rs = dbmd.getProcedures(null, null, "%");
			// printResult(rs);

			// rs = dbmd.getProcedureColumns(null, null, "%", null);
			// printResult(rs);

			// rs = dbmd.getTables(null, null, "%", null);
			// printResult(rs);

			// rs = dbmd.getColumns(null, null, "%", null);
			// printResult(rs);

			// pst = con.prepareStatement("=to(10)");
			// rs = pst.executeQuery();
			// printResult(rs);
			// pst.close();

			// Statement st = con.createStatement();
			// st.execute(">arg1=1"); // 这种没用了

			// pst = con
			// .prepareStatement("==Qfile(\"un-bucket100/Employee.btx\").import@b()\treturn A1");
			// rs = pst.executeQuery();
			// printResult(rs);
			// pst.close();

			// cst = con.prepareCall("call un-bucket100/24点.splx()");
			// rs = cst.executeQuery();
			// printResult(rs);

			// cst = con
			// .prepareCall("call un-bucket100/employee-cursor.splx(?,?)");
			// cst.setString("department", "R&D"); // finance
			// cst.setInt(2, 5000);
			// cst.setFetchSize(5);
			// rs = cst.executeQuery();
			// printResult(rs);

			// cst = con
			// .prepareCall("calls un-bucket100/employee-calls.splx(?2,?1)");
			// final String[] DEPTS = new String[] { "R&D", "Sales", "Marketing"
			// };
			// final int[] SALARYS = new int[] { 12000, 14000, 10000 };
			// for (int i = 0; i < DEPTS.length; i++) {
			// cst.setString(2, DEPTS[i]); // department
			// cst.setInt(1, SALARYS[i]); // salary
			// cst.addBatch();
			// }
			// rs = cst.executeQuery();

			String spl;
			// spl = "select * from 产品"; // "call outputarg(?,?)"; //call
			// Qconnect()
			spl = "call test()";
			cst = con.prepareCall(spl);
			// cst.setString("arg2", "abc");
			// cst.setInt(1, 123);
			cst.execute();
			rs = cst.getResultSet();
			printResult(rs);
			while (cst.getMoreResults()) {
				rs = cst.getResultSet();
				printResult(rs);
			}

			// pst =
			// con.prepareStatement("SELECT 订单编码,客户,雇员,签单日期,发货日期,总额 FROM 订单表");
			// rs = pst.executeQuery();
			// printResult(rs);
			// pst.close();
			//

			// cst = con.prepareCall("calls callstest(?,15000)");
			// for (int i = 0; i < DEPTS.length; i++) {
			// cst.setString(1, DEPTS[i]);
			// cst.addBatch();
			// }
			// rs = cst.executeQuery();
			// printResult(rs);
			//
			// cst = con.prepareCall("calls callstest(?,?)");
			// cst.setString(1, "R&D");
			// cst.setInt(2, 15000);
			// rs = cst.executeQuery();
			// printResult(rs);
			//

			// CallableStatement cst =
			// con.prepareCall("select * from Employee.csv");
			// rs = cst.executeQuery();
			// printResult(rs);

			// CallableStatement cst = con.prepareCall("call 24点()");
			// rs = cst.executeQuery();
			// printResult(rs);

			// CallableStatement cst = con.prepareCall("call c2x");
			// rs = cst.executeQuery();
			// printResult(rs);

			// cst = con.prepareCall("call demo_calls_dfx(?,?)");
			// cst.setInt("arg1", 1);
			// cst.setInt("arg2", 2);
			// rs = cst.executeQuery();
			// printResult(rs);

			// String sql =
			// "=[{file:\"customer.ctx\"}]	=env(customer,pseudo(A1))	=B1.import()	return C1.select(C_ID==?2 && C_CITY_ID==?1)";
			// PreparedStatement pst = con.prepareStatement(sql);
			// pst.setInt(1, 50101);
			// pst.setString(2, "BOLID");
			// rs = pst.executeQuery();
			// printResult(rs);

			// cst = con
			// .prepareCall("select * from testcase2.csv where ID < ?2 and Activity < ?1");
			// cst.setInt(1, 3);
			// cst.setInt(2, 4);
			// rs = cst.executeQuery();
			// printResult(rs);

			// cst = con
			// .prepareCall("$(demo)select * from EMPLOYEE where EID < ? and STATE = ?");
			// cst.setInt(1, 10);
			// cst.setString(2, "Texas");
			// cst = con.prepareCall("=return 1");
			// rs = cst.executeQuery();
			// printResult(rs);

		} finally {
			if (pst != null)
				try {
					pst.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if (cst != null)
				try {
					cst.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}

		}
	}

	private static void printResult(java.sql.ResultSet rs) throws SQLException {
		if (rs == null) {
			System.out.println("ResultSet is null.");
			return;
		}
		System.out.println("ResultSet======================");
		java.sql.ResultSetMetaData rsmd = rs.getMetaData();
		int cc = rsmd.getColumnCount();
		for (int i = 0; i < cc; i++) {
			if (i > 0)
				System.out.print(",");
			System.out.print(rsmd.getColumnLabel(i + 1));
		}
		System.out.println();
		while (rs.next()) {
			for (int i = 0; i < cc; i++) {
				if (i > 0)
					System.out.print(",");
				int columnType = rsmd.getColumnType(i + 1);
				switch (columnType) {
				case Types.BOOLEAN:
					System.out.print(rs.getBoolean(i + 1));
					break;
				case Types.DATE:
					System.out.print(rs.getDate(i + 1));
					break;
				case Types.TIME:
					System.out.print(rs.getTime(i + 1));
					break;
				case Types.TINYINT:
					System.out.print(rs.getByte(i + 1));
					break;
				case Types.SMALLINT:
					System.out.print(rs.getShort(i + 1));
					break;
				case Types.INTEGER:
					System.out.print(rs.getInt(i + 1));
					break;
				case Types.DOUBLE:
					System.out.print(rs.getDouble(i + 1));
					break;
				case Types.FLOAT:
					System.out.print(rs.getFloat(i + 1));
					break;
				case Types.DECIMAL:
					System.out.print(rs.getBigDecimal(i + 1));
					break;
				case Types.TIMESTAMP:
					System.out.print(rs.getTimestamp(i + 1));
					break;
				case Types.VARCHAR:
					System.out.print(rs.getString(i + 1));
					break;
				case Types.BIGINT:
					System.out.print(rs.getBigDecimal(i + 1));
					break;
				default:
					System.out.print(rs.getObject(i + 1));
					break;
				}
			}
			System.out.println();
		}
		rs.close();
	}

}
