package com.raqsoft.dm.sql;

/**
 * SQL语句分词后得到的词或者符号
 * @author RunQian
 *
 */
public final class Token {
	private String id;
	private int pos;
	private char type;

	public Token(char type, String id, int pos) {
		this.type = type;
		this.id = id;
		this.pos = pos;
	}

	public boolean isKeyWord() {
		return type == Tokenizer.KEYWORD;
	}

	public int getPos() {
		return pos;
	}

	public void setType(char type) {
		this.type = type;
	}
	
	public char getType() {
		return type;
	}

	public void setString(String str) {
		id = str;
	}

	public String getString() {
		return id;
	}

	public String toString() {
		return id;
	}

	public boolean equals(Token other) {
		return id.equals(other.id);
	}

	public boolean equals(String str) {
		return id.equals(str);
	}

	// 判断表达式节点是否可以有右节点
	// 操作符或操作符关键字可以有右表达式
	public boolean canHaveRightExp() {
		if (type == Tokenizer.OPERATOR || type == Tokenizer.DOT) { //type == Tokenizer.TABLEMARK || LEVELMARK
			return true;
		}

		return type == Tokenizer.KEYWORD && Tokenizer.isOperatorKeyWord(id);
	}

	public boolean isKeyWord(String str) {
		return type == Tokenizer.KEYWORD && id.equals(str);
	}

	public boolean isMergeKeyWord() {
		if (type != Tokenizer.KEYWORD) return false;
		return id.equals("UNION") || id.equals("INTERSECT") || id.equals("EXCEPT") || id.equals("MINUS");
	}

	// #L
	public String getLevelName() {
		return id.substring(1);
	}

	// @s
	public String getTableName() {
		return id.substring(1);
	}
}
