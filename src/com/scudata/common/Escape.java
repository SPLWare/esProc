package com.scudata.common;

public final class Escape {

/**
*	相当于调用 add(str, '\\')
*/
	public static String add( String str ) {
		return add( str, '\\' );
	}

/**
*   为给定的字符(\t,\r,\n,\',\"及定义的escapeChar)串增加指定的转义符，并返回新产生的新串
*   @param str 需要增加转义符的字符串
*   @param escapeChar 转义符
*   @return 新串
*/
	public static String add( String str, char escapeChar ) {
		if ( str == null )
			return null;

		int len = str.length();
		char[] sb = new char[ 2 * len + 2 ];   //str.length=1
		int j = 0;
		for ( int i = 0; i < len; i ++ ) {
			char ch = str.charAt( i );
			switch ( ch ) {
				case '\t':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = 't';
					break;
				case '\r':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = 'r';
					break;
				case '\n':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = 'n';
					break;
				case '\'':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = '\'';
					break;
				case '\"':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = '\"';
					break;
				default:
					if ( ch == escapeChar ) {
						sb[ j++ ] = escapeChar;
					}
					sb[ j++ ] = ch;
			}
		}
		return new String( sb, 0, j );
	}


/**
*   为给定的字符串增加指定的转义符，并返回新产生的新串
*
*   @param str 需要增加转义符的字符串
*	@param escapedChars 需要被转义的字符
*   @return 新串
*/
	public static String add( String str, String escapedChars ) {
		return add( str, escapedChars, '\\');
	}

/**
*   为给定的字符串增加指定的转义符，并返回新产生的新串
*
*   @param str 需要增加转义符的字符串
*	@param escapedChars 需要被转义的字符
*   @param escapeChar 转义符
*   @return 新串
*/
	public static String add( String str, String escapedChars, char escapeChar ) {
		if ( str == null )
			return null;

		int len = str.length();
		char[] sb = new char[ 2 * len + 2 ];   //str.length=1
		int j = 0;
		for ( int i = 0; i < len; i ++ ) {
			char ch = str.charAt( i );
			switch ( ch ) {
				case '\t':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = 't';
					break;
				case '\r':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = 'r';
					break;
				case '\n':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = 'n';
					break;
				case '\'':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = '\'';
					break;
				case '\"':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = '\"';
					break;
				default:
					if ((ch == escapeChar) ||
						(escapedChars != null && escapedChars.indexOf(ch) >= 0))
						sb[ j++ ] = escapeChar;
					sb[ j++ ] = ch;
			}
		}
		return new String( sb, 0, j );
	}

	public static String addEscAndQuote( String str, String escapedChars, char escapeChar ) {
		return addEscAndQuote( str, true, escapedChars, escapeChar );
	}

	public static String addEscAndQuote( String str, boolean ifDblQuote, String escapedChars, char escapeChar ) {
		if ( str == null )
			return null;

		int len = str.length();
		char[] sb = new char[ 2 * len + 2 ];   //str.length=1
		sb[0] = ifDblQuote ? '\"' : '\'';
		int j = 1;
		for ( int i = 0; i < len; i ++ ) {
			char ch = str.charAt( i );
			switch ( ch ) {
				case '\t':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = 't';
					break;
				case '\r':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = 'r';
					break;
				case '\n':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = 'n';
					break;
				case '\'':
					if(!ifDblQuote)
						sb[ j++ ] = escapeChar;
					sb[ j++ ] = '\'';
					break;
				case '\"':
					if(ifDblQuote)
						sb[ j++ ] = escapeChar;
					sb[ j++ ] = '\"';
					break;
				default:
					if ((ch == escapeChar) ||
						(escapedChars != null && escapedChars.indexOf(ch) >= 0))
						sb[ j++ ] = escapeChar;
					sb[ j++ ] = ch;
			}
		}
		sb[j++] = ifDblQuote ? '\"' : '\'';
		return new String( sb, 0, j );
	}


/**
*	相当于调用 remove( str, '\\')
*/
	public static String remove( String str ) {
		return remove( str, '\\' );
	}

/**
*   将指定的字符串移去指定转义符，并返回新产生的新串
*
*   @param str 需要移去转义符的字符串
*   @param escapeChar 转义字符
*   @return 原串移去转义符后的新串
*/
	public static String remove( String str, char escapeChar ) {
		if ( str == null )
			return null;

		int len = str.length();
		if (len == 0) return str;
		char[] sb = new char[ len ];
		int i = 0, j = 0;
		char ch = str.charAt( i );
		for ( ; i < len; i++ ) {
			ch = str.charAt( i );
			if ( ch == escapeChar ) {
				i ++;
				if ( i == len )
					break;
				ch = str.charAt( i );
				switch ( ch ) {
					case 't':
						sb[ j++ ] = '\t';
						break;
					case 'r':
						sb[ j++ ] = '\r';
						break;
					case 'n':
						sb[ j++ ] = '\n';
						break;
					default:
						sb[ j++ ] = ch;
				}
			} else
				sb[ j++ ] =  ch;
		}
		return new String( sb, 0, j );
	}

/**
*	将使用旧转义符的字符串变换使用新转义符的字符串
*	@param str 需要变换转义符的串
*	@param oldEscapeChar 旧转义符
*	@param newEscapeChar 新转义符
*	@return 变换后的新串
*/
	public static String change( String str, char oldEscapeChar, char newEscapeChar ) {
		if ( str == null )
			return null;

		int len = str.length();
		if (len == 0) return str;
		char[] sb = new char[ len ];
		for (int i = 0 ; i < len; i++ ) {
			if ( str.charAt( i ) == oldEscapeChar ) {
				sb[ i++ ] = newEscapeChar;
				if ( i < len )
					sb[ i ] = str.charAt( i );
			} else
				sb[ i ] = str.charAt( i );
		}
		return new String( sb );
	}

/**
*	相当于调用 addEscAndQuote(str, true, '\\')
*/
	public static String addEscAndQuote( String str ) {
		return addEscAndQuote( str, true, '\\' );
	}

/**
*	相当于调用 addEscAndQuote(str, ifDblQuote, '\\')
*/
	public static String addEscAndQuote( String str, boolean ifDblQuote) {
		return addEscAndQuote( str, ifDblQuote, '\\' );
	}

/**
*	相当于调用 addEscapeAndQuote(str, true, '\\')
*/
	public static String addEscAndQuote( String str, char escapeChar) {
		return addEscAndQuote( str, true, escapeChar );
	}

/**
*   为给定的字符串增加指定的转义符，且前后补上引号，并返回新产生的新串
*
*   @param str 需要增加转义符的字符串
*	@param ifDblQuote 为true时加上双引号，否则加上单引号
*   @param escapeChar 转义符
*   @return 新串
*/
	public static String addEscAndQuote( String str, boolean ifDblQuote, char escapeChar ) {
		if ( str == null )
			return null;

		int len = str.length();
		char[] sb = new char[ 2 * len + 2 ];   //str.length=1
		sb[0] = ifDblQuote ? '\"' : '\'';
		int j = 1;
		for ( int i = 0; i < len; i ++ ) {
			char ch = str.charAt( i );
			switch ( ch ) {
				case '\t':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = 't';
					break;
				case '\r':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = 'r';
					break;
				case '\n':
					sb[ j++ ] = escapeChar;
					sb[ j++ ] = 'n';
					break;
				case '\'':
					if(!ifDblQuote)
						sb[ j++ ] = escapeChar;
					sb[ j++ ] = '\'';
					break;
				case '\"':
					if(ifDblQuote)
						sb[ j++ ] = escapeChar;
					sb[ j++ ] = '\"';
					break;
				default:
					if ( ch == escapeChar ) {
						sb[ j++ ] = escapeChar;
					}
					sb[ j++ ] = ch;
			}
		}
		sb[ j++ ] = ifDblQuote ? '\"' : '\'';
		return new String( sb, 0, j );
	}
	
	/**
	 * 使用excel标准添加双引号
	 * @param str
	 * @return
	 */
	public static String addExcelQuote(String str) {
		if ( str == null ) {
			return null;
		}
		
		int len = str.length();
		char[] sb = new char[ 2 * len + 2 ];   //str.length=1
		sb[0] = '"';
		int j = 1;
		for ( int i = 0; i < len; i ++ ) {
			char ch = str.charAt( i );
			if (ch == '"') {
				sb[ j++ ] = '"';
				sb[ j++ ] = '"';
			} else {
				sb[ j++ ] = ch;
			}
		}
		
		sb[ j++ ] = '\"';
		return new String( sb, 0, j );
	}

	/**
	*	相当于调用 removeEscAndQuote( str, '\\')
	*/
	public static String removeEscAndQuote( String str ) {
		return removeEscAndQuote( str, '\\' );
	}

	/**
	*   将指定的字符串移去最外边的引号及指定转义符，并返回新产生的新串
	*
	*   @param str 需要移去转义符的字符串
	*   @param escapeChar 转义字符
	*   @return 原串移去转义符后的新串
	*/
	public static String removeEscAndQuote( String str, char escapeChar ) {
		if ( str == null )
			return null;

		int len = str.length();
		if (len == 0) return str;
		char[] sb = new char[ len ];
		int i = 0, j = 0;
		char ch = str.charAt( i );
		
		if ((ch=='"' || ch=='\'') && str.charAt( len - 1 ) == ch) {
			 i++;
			 len--;
		}
		
		for ( ; i < len; i++ ) {
			ch = str.charAt( i );
			if ( ch == escapeChar ) {
				i ++;
				if ( i == len )
					break;
				ch = str.charAt( i );
				switch ( ch ) {
					case 't':
						sb[ j++ ] = '\t';
						break;
					case 'r':
						sb[ j++ ] = '\r';
						break;
					case 'n':
						sb[ j++ ] = '\n';
						break;
					default:
						sb[ j++ ] = ch;
				}
			} else {
				sb[ j++ ] =  ch;
			}
		}
		
		return new String( sb, 0, j );
	}

	public static void main(String[] args) {
		String s = "a=\"(1+1)\";b=\"（salary）+1\"";
		s = add(s, "()[]{}");
		System.out.println(s);
		s = remove(s);
		System.out.println(s);
		s = "\"'abc'\"+5";
		System.out.println( addEscAndQuote(s) );
	}
	
}
