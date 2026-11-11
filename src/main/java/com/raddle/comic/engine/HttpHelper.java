/**
 * 
 */
package com.raddle.comic.engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * @author raddle
 *
 */
public class HttpHelper {
	/** 默认的读取超时（秒） */
	public static final int DEFAULT_SO_TIMEOUT_SEC = 5;
	private final static CloseableHttpClient httpclient;
	static {
		HttpClientBuilder custom = HttpClients.custom();
		custom.setUserAgent("Mozilla/5.0 (Windows NT 6.1; rv:25.0) Gecko/20100101 Firefox/25.0");
		RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(1 * 1000).setSocketTimeout(DEFAULT_SO_TIMEOUT_SEC * 1000)
				.setConnectTimeout(1 * 1000).build();
		custom.setDefaultRequestConfig(config);
		httpclient = custom.build();
	}

	/**
	 * 生成单次请求使用的配置：传入的读取超时（秒）大于 0 时覆盖默认超时，否则使用默认超时。
	 *
	 * @param soTimeoutSec 读取超时秒数，可以为 null
	 * @return 本次请求的配置
	 */
	private static RequestConfig requestConfig(Object soTimeoutSec) {
		int timeoutSec = DEFAULT_SO_TIMEOUT_SEC;
		if (soTimeoutSec instanceof Number) {
			int sec = ((Number) soTimeoutSec).intValue();
			if (sec > 0) {
				timeoutSec = sec;
			}
		}
		return RequestConfig.custom().setConnectionRequestTimeout(1 * 1000).setSocketTimeout(timeoutSec * 1000).setConnectTimeout(1 * 1000).build();
	}

	/**
	 * 访问远程网页，使用默认超时。
	 *
	 * @param url 地址
	 * @param charset 编码
	 * @param headers 请求头
	 * @return 网页内容
	 */
	public static String getRemotePage(String url, String charset, Map<Object, Object> headers) throws IOException {
		return getRemotePage(url, charset, headers, null);
	}

	/**
	 * 访问远程网页。
	 *
	 * @param url 地址
	 * @param charset 编码
	 * @param headers 请求头
	 * @param soTimeoutSec 读取超时秒数，传数字表示本次请求的超时时间；不传（null / undefined）或者小于等于 0 时使用默认超时
	 * @return 网页内容
	 */
	public static String getRemotePage(String url, String charset, Map<Object, Object> headers, Object soTimeoutSec) throws IOException {
		HttpGet httpGet = new HttpGet(url);
		httpGet.setConfig(requestConfig(soTimeoutSec));
		if (headers != null) {
			for (Map.Entry<Object, Object> entry : headers.entrySet()) {
				httpGet.addHeader(entry.getKey() + "", entry.getValue() + "");
			}
		}
		CloseableHttpResponse response = httpclient.execute(httpGet);
		try {
			HttpEntity entity1 = response.getEntity();
			if (response.getStatusLine().getStatusCode() == 200) {
				return EntityUtils.toString(entity1, charset);
			} else {
				EntityUtils.consume(entity1);
				throw new RuntimeException("获得内容失败:" + response.getStatusLine() + " , " + url);
			}
		} finally {
			response.close();
		}
	}

	/**
	 * 下载远程图片到缓存目录，使用默认超时。
	 *
	 * @param channel 渠道名
	 * @param comicId 漫画 id
	 * @param sectionId 章节 id
	 * @param imageUrl 图片地址
	 * @param cacheFileName 缓存文件名
	 * @param headers 请求头
	 */
	public static void saveRemoteImage(String channel, String comicId, String sectionId, String imageUrl, String cacheFileName,
			Map<Object, Object> headers) throws IOException {
		saveRemoteImage(channel, comicId, sectionId, imageUrl, cacheFileName, headers, null);
	}

	/**
	 * 下载远程图片到缓存目录。
	 *
	 * @param channel 渠道名
	 * @param comicId 漫画 id
	 * @param sectionId 章节 id
	 * @param imageUrl 图片地址
	 * @param cacheFileName 缓存文件名
	 * @param headers 请求头
	 * @param soTimeoutSec 读取超时秒数，传数字表示本次请求的超时时间；不传（null / undefined）或者小于等于 0 时使用默认超时
	 */
	public static void saveRemoteImage(String channel, String comicId, String sectionId, String imageUrl, String cacheFileName,
			Map<Object, Object> headers, Object soTimeoutSec) throws IOException {
		File cacheFile = new File(System.getProperty("user.home") + "/.comic-view/cache/img/" + channel + "/" + comicId + "/" + sectionId + "/"
				+ StringUtils.defaultIfBlank(cacheFileName, FilenameUtils.getName(imageUrl)));
		saveRemotePage(imageUrl, headers, cacheFile, soTimeoutSec);
	}

	/**
	 * 下载远程内容到文件，使用默认超时。
	 *
	 * @param url 地址
	 * @param headers 请求头
	 * @param file 保存的文件
	 */
	public static void saveRemotePage(String url, Map<Object, Object> headers, File file) throws IOException {
		saveRemotePage(url, headers, file, null);
	}

	/**
	 * 下载远程内容到文件。
	 *
	 * @param url 地址
	 * @param headers 请求头
	 * @param file 保存的文件
	 * @param soTimeoutSec 读取超时秒数，传数字表示本次请求的超时时间；不传（null / undefined）或者小于等于 0 时使用默认超时
	 */
	public static void saveRemotePage(String url, Map<Object, Object> headers, File file, Object soTimeoutSec) throws IOException {
		File parentFile = file.getParentFile();
		if (parentFile != null && !parentFile.exists()) {
			parentFile.mkdirs();
		}
		HttpGet httpGet = new HttpGet(url);
		httpGet.setConfig(requestConfig(soTimeoutSec));
		if (headers != null) {
			for (Map.Entry<Object, Object> entry : headers.entrySet()) {
				httpGet.addHeader(entry.getKey() + "", entry.getValue() + "");
			}
		}
		CloseableHttpResponse response = httpclient.execute(httpGet);
		try {
			HttpEntity entity1 = response.getEntity();
			if (response.getStatusLine().getStatusCode() == 200) {
				OutputStream os = new FileOutputStream(file);
				IOUtils.copy(entity1.getContent(), os);
				os.flush();
				os.close();
				EntityUtils.consume(entity1);
			} else {
				EntityUtils.consume(entity1);
				throw new RuntimeException("获得内容失败:" + response.getStatusLine() + " , " + url);
			}
		} finally {
			response.close();
		}
	}
}
