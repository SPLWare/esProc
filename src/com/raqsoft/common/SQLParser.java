package com.raqsoft.common;

public final class SQLParser {

  public static final int SQL_UNKNOWN = 100;
  public static final int SQL_SELECT = 101;
  public static final int SQL_INSERT = 102;
  public static final int SQL_DELETE = 103;
  public static final int SQL_UPDATE = 104;

  public static final int KEY_SELECT = 0;
  public static final int KEY_FROM = 1;
  public static final int KEY_WHERE = 2;
  public static final int KEY_GROUPBY = 3;
  public static final int KEY_HAVING = 4;
  public static final int KEY_ORDERBY = 5;

  public static final int KEY_INSERT = 0;
  public static final int KEY_VALUES = 1;

  public static final int KEY_DELETE = 0;

  public static final int KEY_UPDATE = 0;
  public static final int KEY_SET = 1;

  private int type;
  private String sql;

  private static final String[] selectKeys = {
      "SELECT", "FROM", "WHERE", "GROUP", "HAVING", "ORDER"};
  private static final String[] insertKeys = {
      "INTO", "VALUES"};
  private static final String[] deleteKeys = {
      "FROM", "WHERE", "WHERE"};
  private static final String[] updateKeys = {
      "UPDATE", "SET", "WHERE"};

  /**
   * 构造函数
   * @param sql 需要分析的SQL语句
   */
  public SQLParser(String sql) {
    this.sql = sql;
    type = parseSQLType(sql);
  }

  private static String
      getClause(String sql, int key, String[] keyValues) {
    if (key < 0 || key > keyValues.length)return null;
    String keyValue = keyValues[key];

    int p = Sentence.phraseAt(sql, keyValue, 0, Sentence.IGNORE_CASE);
    if (p < 0)return null;

    switch (key) {
      case KEY_GROUPBY:
      case KEY_ORDERBY:
        p = Sentence.phraseAt(sql, "BY", p, Sentence.IGNORE_CASE);
        if (p < 0)return null;
        p += 2;
        break;
      case KEY_SELECT:
        int q = Sentence.phraseAt(sql, "DISTINCT", p, Sentence.IGNORE_CASE);
        if (q >= 0) {
          p = q + 8;
          break;
        }
      default:
        p += keyValue.length();
        break;
    }
    int q = -1;
    while ( (q < 0) && (key < keyValues.length - 1)) {
      q = Sentence.phraseAt(sql, keyValues[++key], p, Sentence.IGNORE_CASE);
    }
    if (q < 0)
      return sql.substring(p).trim();
    else
      return sql.substring(p, q).trim();
  }

  /**
   * 获取SQL中的子句
   * @param sql 需要分析的SQL语句
   * @param key 检索子句对应的关键字
   * @return 返回检索到的子句
   */
  public static String getClause(String sql, int key) {
    switch (parseSQLType(sql)) {
      case SQL_SELECT:
        return getClause(sql, key, selectKeys);
      case SQL_INSERT:
        return getClause(sql, key, insertKeys);
      case SQL_DELETE:
        return getClause(sql, key, deleteKeys);
      case SQL_UPDATE:
        return getClause(sql, key, updateKeys);
    }
    return null;
  }

  /**
   * 获取SQL中的子句
   * @param key 检索子句对应的关键字
   * @return 返回检索到的子句
   */
  public String getClause(int key) {
    switch (type) {
      case SQL_SELECT:
        return getClause(sql, key, selectKeys);
      case SQL_INSERT:
        return getClause(sql, key, insertKeys);
      case SQL_DELETE:
        return getClause(sql, key, deleteKeys);
      case SQL_UPDATE:
        return getClause(sql, key, updateKeys);
    }
    return null;
  }

  private static String
      modify(String sql, int key, String clause, String[] keyValues) {
    if (key < 0 || key > keyValues.length)return sql;
    int p = -1, i = key;
    StringBuffer dst = new StringBuffer(2 * sql.length());
    while (p < 0) {
      p = Sentence.phraseAt(sql, keyValues[i], 0, Sentence.IGNORE_CASE);
      i++;
      if (i == keyValues.length)break;
    }
    if (p > 0) { //找到自己或后面的关键字
      dst.append(sql.substring(0, p));
    }
    else if (p < 0) {
      dst.append(sql);
      dst.append(' ');
    }
    if (! (clause == null || clause.trim().length() == 0)) {
      dst.append(keyValues[key]);
      if (key == KEY_SELECT) {
        if (Sentence.phraseAt(sql, "DISTINCT", 0, Sentence.IGNORE_CASE) > 0)
          dst.append(" DISTINCT");
      }
      else if (key == KEY_GROUPBY || key == KEY_ORDERBY) {
        dst.append(" BY");
      }
      dst.append(' ');
      dst.append(clause);
    }
    if (p < 0)return dst.toString();
    if (i == key + 1) { //若找到自己,则需要继续找下一个关键字
      int q = -1;
      while (q < 0 && i < keyValues.length) {
        q = Sentence.phraseAt(sql, keyValues[i], p, Sentence.IGNORE_CASE);
        if (q >= 0)break;
        i++;
      }
      p = q; //p记录下一个关键字位置
    }
    if (p >= 0) {
      dst.append(' ');
      dst.append(sql.substring(p));
    }
    return dst.toString();
  }

  /**
   * 修改指定SQL语句中指定的子句
   * @param sql 需要分析的SQL语句
   * @param key 需要修改子句的关键字
   * @param clause 用于替换的子句
   * @return 修改后的SQL语句
   */
  public static String modify(String sql, int key, String clause) {
    switch (parseSQLType(sql)) {
      case SQL_UNKNOWN:
      case SQL_SELECT:
        return modify(sql, key, clause, selectKeys);
      case SQL_INSERT:
        return modify(sql, key, clause, insertKeys);
      case SQL_DELETE:
        return modify(sql, key, clause, deleteKeys);
      case SQL_UPDATE:
        return modify(sql, key, clause, updateKeys);
    }
    return sql;
  }

  /**
   * 修改指定的子句
   * @param key 需要修改子句的关键字
   * @param clause 用于替换的子句
   */
  public void modify(int key, String clause) {
    switch (type) {
      case SQL_UNKNOWN:
      case SQL_SELECT:
        sql = modify(sql, key, clause, selectKeys);
        break;
      case SQL_INSERT:
        sql = modify(sql, key, clause, insertKeys);
        break;
      case SQL_DELETE:
        sql = modify(sql, key, clause, deleteKeys);
        break;
      case SQL_UPDATE:
        sql = modify(sql, key, clause, updateKeys);
        break;
    }
  }

  private static String
      mergeWhere(String sql, String where, String[] keyValues) {
    if (where.equals("1") || where.equals("1=1"))return sql;
    int i = KEY_WHERE;
    StringBuffer dst = new StringBuffer(2 * where.length());
    int p = Sentence.phraseAt(sql, keyValues[i], 0, Sentence.IGNORE_CASE);
    if (p >= 0) p += 5;
    int q = -1;
    while (q < 0 && ++i < keyValues.length) {
      q = Sentence.phraseAt(sql, keyValues[i], 0, Sentence.IGNORE_CASE);
    }
    if (q < 0) q = sql.length();
    if (p >= 0) {
      dst.append(sql.substring(0, p));
      dst.append('(');
      if (!where.equals("1=0") && !where.equals("0=1")) {
        dst.append(sql.substring(p, q));
        dst.append(") AND (");
      }
      dst.append(where);
      dst.append(')');
      dst.append(sql.substring(q));
    }
    else {
      dst.append(sql.substring(0, q));
      dst.append(" WHERE ");
      dst.append(where);
      dst.append(' ');
      dst.append(sql.substring(q));
    }
    return dst.toString();
  }

  /**
   * 将条件表达式合并进SQL语句中的WHERE子句
   * @param sql 需要合并WHERE子句的SQL语句
   * @param where 要加入的WHERE条件
   * @return 合并后的SQL语句
   */
  public static String mergeWhere(String sql, String where) {
    if (where == null || where.trim().length() == 0)return sql;
    where = where.trim();
    switch (parseSQLType(sql)) {
      case SQL_SELECT:
        return mergeWhere(sql, where, selectKeys);
      case SQL_DELETE:
        return mergeWhere(sql, where, deleteKeys);
      case SQL_UPDATE:
        return mergeWhere(sql, where, updateKeys);
    }
    return sql;
  }

  /**
   * 将条件表达式合并进SQL语句中的WHERE子句
   * @param where 要加入的WHERE条件
   */
  public void mergeWhere(String where) {
    if (where == null || where.trim().length() == 0)return;
    where = where.trim();
    switch (type) {
      case SQL_SELECT:
        sql = mergeWhere(sql, where, selectKeys);
        break;
      case SQL_DELETE:
        sql = mergeWhere(sql, where, deleteKeys);
        break;
      case SQL_UPDATE:
        sql = mergeWhere(sql, where, updateKeys);
        break;
    }
  }

  /**
   * 返回当前的SQL语句
   */
  public String toString() {
    return this.sql;
  }

  /**
   * 分析指定SQL语句的DML(数据操纵)类型
   * @param sql SQL语句
   * @return 返回DML类型
   */
  public static int parseSQLType(String sql) {
    if (sql == null)return SQL_UNKNOWN;
    int i = 0, len = sql.length();
    while (i < len && Character.isWhitespace(sql.charAt(i))) i++;

    if ("SELECT".regionMatches(true, 0, sql, i, 6)) {
      return SQL_SELECT;
    }
    else if ("INSERT".regionMatches(true, 0, sql, i, 6)) {
      return SQL_INSERT;
    }
    else if ("DELETE".regionMatches(true, 0, sql, i, 6)) {
      return SQL_DELETE;
    }
    else if ("UPDATE".regionMatches(true, 0, sql, i, 6)) {
      return SQL_UPDATE;
    }
    else {
      return SQL_UNKNOWN;
    }
  }

  /**
   * 分析DML(数据操纵)类型
   * @return 返回DML类型
   */
  public int parseSQLType() {
    return type;
  }

  /**
   * 在SQL中搜索匹配的引号位置
   * @param sql SQL语句串
   * @param start 起始位置
   * @param quote 字符串所使用的引号字符(单/双)
   * @param 成功返回匹配的引号位置，否则返回-1
   */
  public static int scanQuotation(String sql, int start, char quote) {
    if (quote != sql.charAt(start))return -1;
    int idx = start + 1, len = sql.length();
    while (idx < len) {
      idx = sql.indexOf(quote, idx);
      if (idx < 0)break;
      if (idx + 1 < len) {
        if (sql.charAt(idx + 1) != quote)
          return idx;
        else
          idx++;
      }
      idx++;
    }
    return -1;
  }

  public static void main(String[] a) {
    String sss = "\t  insert into xu values('ddd')";
    System.out.println(SQLParser.parseSQLType(sss));
    System.out.println(SQLParser.modify(sss, SQLParser.KEY_ORDERBY, "dddd"));
    SQLParser sp = new SQLParser(
        "select abc, dsc, ab a from rt a where abc < ?");
    System.out.println(sp.getClause(SQLParser.KEY_FROM));
    System.out.println("*" +
        SQLParser.getClause("select abc, dsc, ab a from rt a where abc < ?",
        SQLParser.KEY_FROM) + "*");
  }
}
