/**
 * 
 */
package com.raddle.comic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author raddle
 * 
 */
public class LogWrapper {
	private static final Pattern replace = Pattern.compile("\\{\\}");
	private Logger logger;

	public LogWrapper(Logger logger) {
		this.logger = logger;
	}

	/**
	 * 用参数替换消息展位符，展位符是{}比如<br>
	 * "url[{}]不正确 xxx"替换后是"url[xxx]不正确"
	 * 
	 * @param msg
	 * @param params
	 */
	public void log(String msg, Object... params) {
		logger.info(replacePlaceHolder(msg, params));
	}

	/**
	 * 用参数替换消息展位符，展位符是{}比如<br>
	 * "url[{}]不正确 xxx"替换后是"url[xxx]不正确"
	 * 
	 * @param msg
	 * @param t
	 * @param params
	 */
	public void log(String msg, Throwable t, Object... params) {
		logger.error(replacePlaceHolder(msg, params), t);
	}

	/**
	 * /** 用参数替换消息展位符，展位符是{}比如<br>
	 * "url[{}]不正确 xxx"替换后是"url[xxx]不正确"
	 * 
	 * @param msg
	 * @param params
	 * @return
	 */
	public static String replacePlaceHolder(String msg, Object... params) {
		if (msg == null) {
			return null;
		}
		Matcher matcher = replace.matcher(msg);
		StringBuffer sb = new StringBuffer();
		int i = -1;
		while (matcher.find()) {
			i++;
			String replacement = "";
			if (params != null && params.length > i) {
				replacement = ObjectUtils.toString(params[i], "");
			}
			matcher.appendReplacement(sb, replacement);
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	public static void main(String[] args) {
		Exception exception = new Exception("fff");
		LogWrapper w = new LogWrapper(LoggerFactory.getLogger(LogWrapper.class));
		w.log("sss{}ddd{}", exception, "111", "222");
	}
}
