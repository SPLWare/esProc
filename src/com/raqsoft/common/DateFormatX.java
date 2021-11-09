package com.raqsoft.common;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class DateFormatX extends SimpleDateFormat {
    static final long serialVersionUID = 1L;
    
    //private final static int TAG_QUOTE_ASCII_CHAR	= 100;
    private final static int TAG_QUOTE_CHARS = 101;
    
    // 支持的模式，如果包含其它的则调用基类处理
    private static final String  patternChars = "yMdHmsS";
    
    // Map index into pattern character string to Calendar field number
    private static final int[] PATTERN_INDEX_TO_CALENDAR_FIELD = {
        Calendar.YEAR, Calendar.MONTH, Calendar.DATE,
        Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND};

    // 如果空则调用基类处理
    private char[] compiledPattern;


    public DateFormatX() {
    	super();
    }

    public DateFormatX(String pattern) {
        this(pattern, Locale.getDefault());
    }

    public DateFormatX(String pattern, Locale locale) {
    	super(pattern, locale);
    	compiledPattern = compile(pattern);
    }

    public DateFormatX(String pattern, DateFormatSymbols formatSymbols) {
    	super(pattern, formatSymbols);
    	compiledPattern = compile(pattern);
    }

    /**
     * Encodes the given tag and length and puts encoded char(s) into buffer.
     */
    private static boolean encode(int tag, int length, StringBuilder buffer) {
    	if (tag == 0) { // y
    		if (length != 4) {
    			return false;
    		}
    	} else if (tag >= 1 && tag <= 6) { // MdHms
    		if (length != 2) {
    			return false;
    		}
    	} else if (tag == 7) { // S
    	}
    	
	    buffer.append((char)PATTERN_INDEX_TO_CALENDAR_FIELD[tag]);
	    return true;
    }

    private static char[] compile(String pattern) {
		int length = pattern.length();
		StringBuilder compiledPattern = new StringBuilder(length * 2);
		int count = 0;
		int lastTag = -1;
		boolean hasQuote = false;
		
		for (int i = 0; i < length; i++) {
		    char c = pattern.charAt(i);
		    if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z')) {
				if (count != 0) {
				    if (!encode(lastTag, count, compiledPattern)) {
				    	return null;
				    }
				    
				    lastTag = -1;
				    count = 0;
				}
				
				compiledPattern.append((char)TAG_QUOTE_CHARS);
				compiledPattern.append(c);
				
				hasQuote = true;
				continue;
		    } else {
			    int tag;
			    if ((tag = patternChars.indexOf(c)) == -1) {
			    	return null;
			    }
			    
			    if (lastTag == -1 || lastTag == tag) {
					lastTag = tag;
					count++;
					continue;
			    }
			    
			    if (!encode(lastTag, count, compiledPattern)) {
			    	return null;
			    }
			    
			    lastTag = tag;
			    count = 1;
		    }
		}

		// 不对20181028格式的日期做优化
		if (!hasQuote) {
			return null;
		}
		
		if (count != 0) {
		    if (!encode(lastTag, count, compiledPattern)) {
		    	return null;
		    }
		}
	
		// Copy the compiled pattern to a char array
		int len = compiledPattern.length();
		char[] r = new char[len];
		compiledPattern.getChars(0, len, r, 0);
		return r;
    }

    public Date parse(String text) {
    	char[] compiledPattern = this.compiledPattern;
    	if (compiledPattern == null) {
    		ParsePosition pos = new ParsePosition(0);
    		Date date = parse(text, pos);
    		if (date == null) {
    			return null;
    		}
    		
    		int textLength = text.length();
    		for (int i = pos.getIndex(); i < textLength; ++i) {
            	if (!Character.isWhitespace(text.charAt(i))) {
            		return null;
            	}
    		}
    		
    		return date;
    	}
    	
        int start = 0;
        int textLength = text.length();
        calendar.clear(); // Clears all the time fields

        for (int i = 0; i < compiledPattern.length; ) {
            int tag = compiledPattern[i++];
		    if (tag == TAG_QUOTE_CHARS){
				if (start >= textLength || text.charAt(start) != compiledPattern[i++]) {
				    return null;
				}
				
				start++;
		    } else {
				// Peek the next pattern to determine if we need to
				// obey the number of pattern letters for
				// parsing. It's required when parsing contiguous
				// digit text (e.g., "20010704") with a pattern which
				// has no delimiters between fields, like "yyyyMMdd".
				//boolean obeyCount = false;
				start = subParse(text, start, tag);
				if (start < 0) {
					// 由父类处理
		    		ParsePosition pos = new ParsePosition(0);
		    		Date date = parse(text, pos);
		    		if (date == null) {
		    			return null;
		    		}
		    		
		    		for (i = pos.getIndex(); i < textLength; ++i) {
		            	if (!Character.isWhitespace(text.charAt(i))) {
		            		return null;
		            	}
		    		}
		    		
		    		return date;
				}
		    }
	    }

        // 检查日期后面是否还有多余的非空白字符
        for (;start < textLength; ++start) {
        	if (!Character.isWhitespace(text.charAt(start))) {
        		return null;
        	}
        }
        
        try {
            return calendar.getTime();
        } catch (IllegalArgumentException e) {
	        // An IllegalArgumentException will be thrown by Calendar.getTime()
	        // if any fields are out of range, e.g., MONTH == 17.
            return null;
        }
    }

    /**
     * Private member function that converts the parsed date strings into
     * timeFields. Returns -start (for ParsePosition) if failed.
     * @param text the time text to be parsed.
     * @param start where to start parsing.
     * @param field the date field text to be parsed.
     * @return the new start position if matching succeeded; -1 indicating
     * matching failure, otherwise. In case matching failure occurred,
     * an error index is set to origPos.errorIndex.
     */
    private int subParse(String text, int start, int field) {
        int value = 0;
        int end = start;
    	int len = text.length();
    	
    	// 暂时不支持带有空格
    	for (; end < len; ++end) {
    		char c = text.charAt(end);
    		if (c >= '0' && c <= '9') {
    			value = value * 10 + (c - '0');
    		} else {
    			break;
    		}
    	}

    	if (end == start) {
    		return -1;
    	}

    	if (field == Calendar.MONTH) {
    		calendar.set(field, value - 1);
    	} else {
    		calendar.set(field, value);
    	}
    	
	    return end;
    }

    /**
     * Applies the given pattern string to this date format.
     *
     * @param pattern the new date and time pattern for this date format
     * @exception NullPointerException if the given pattern is null
     * @exception IllegalArgumentException if the given pattern is invalid
     */
    public void applyPattern (String pattern) {
    	super.applyPattern(pattern);
    	compiledPattern = compile(pattern);
    }

    /**
     * Applies the given localized pattern string to this date format.
     *
     * @param pattern a String to be mapped to the new date and time format
     *        pattern for this format
     * @exception NullPointerException if the given pattern is null
     * @exception IllegalArgumentException if the given pattern is invalid
     */
    public void applyLocalizedPattern(String pattern) {
    	super.applyLocalizedPattern(pattern);
    	compiledPattern = compile(pattern);
    }
    
    public static void test(int count) throws ParseException {
    	DateFormatX df = new DateFormatX("yyyy-MM-dd");
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    	String date = "2018-12-21";
    	
    	long time1 = System.currentTimeMillis();
    	for (int i = 0; i < count; ++i) {
    		df.parse(date);
    	}
    	
    	long time2 = System.currentTimeMillis();
    	for (int i = 0; i < count; ++i) {
    		sdf.parse(date);
    	}
    	
    	long time3 = System.currentTimeMillis();
    	System.out.println(time2 - time1);
    	System.out.println(time3 - time2);
    }
}
