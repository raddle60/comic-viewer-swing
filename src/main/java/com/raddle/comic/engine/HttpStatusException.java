package com.raddle.comic.engine;

import java.io.IOException;

/**
 * 服务器返回了不是200的状态码。
 *
 * 渠道脚本想知道是不是"图片不存在"（404）的时候，
 * 捕获到异常以后用 httpclient.isNotFound(e) 判断。
 */
public class HttpStatusException extends IOException {

	private static final long serialVersionUID = 1L;

	/** 服务器返回的状态码 */
	private final int statusCode;
	/** 服务器返回的状态行，比如"HTTP/1.1 404 Not Found" */
	private final String statusLine;

	public HttpStatusException(int statusCode, String statusLine, String url) {
		super("获得内容失败:" + statusLine + " , " + url);
		this.statusCode = statusCode;
		this.statusLine = statusLine;
	}

	/** 服务器返回的状态码 */
	public int getStatusCode() {
		return statusCode;
	}

	/** 服务器返回的状态行，比如"HTTP/1.1 404 Not Found" */
	public String getStatusLine() {
		return statusLine;
	}
}
