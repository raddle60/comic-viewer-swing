/**
 * 
 */
package com.raddle.comic.engine;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;
import org.slf4j.LoggerFactory;

import com.raddle.comic.LogWrapper;

/**
 * @author raddle
 *
 */
public class HttpHelper {
	private static final LogWrapper logger = new LogWrapper(LoggerFactory.getLogger(HttpHelper.class));
	/** 默认的读取超时（秒） */
	public static final int DEFAULT_SO_TIMEOUT_SEC = 5;
	/** 建立连接的超时（毫秒） */
	private static final int CONNECT_TIMEOUT_MS = 5 * 1000;
	/** 从连接池里取连接的等待时间（毫秒）。同时下好几页会排队等连接，等不到就直接失败，所以要留够时间。 */
	private static final int CONNECTION_REQUEST_TIMEOUT_MS = 10 * 1000;
	/** 同一个站点最多同时用几条连接。站点本身往往限制并发，开太多会被掐，所以保持小一点。 */
	private static final int MAX_CONN_PER_ROUTE = 4;
	/** 连接池里一共最多保留几条连接（不同站点加起来） */
	private static final int MAX_CONN_TOTAL = 20;
	/** 默认的请求配置 */
	private static final RequestConfig DEFAULT_REQUEST_CONFIG = RequestConfig.custom().setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
			.setSocketTimeout(DEFAULT_SO_TIMEOUT_SEC * 1000).setConnectTimeout(CONNECT_TIMEOUT_MS).build();
	private final static CloseableHttpClient httpclient;
	static {
		// 显式建连接池。HttpClient 自己也会建池，但默认每个站点只允许 2 条并发连接，
		// 池子满了以后取连接要排队，排队超时只有 1 秒，很容易就报 "Timeout waiting for connection from pool"，
		// 看起来就像是被站点限流了。
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
		connManager.setMaxTotal(MAX_CONN_TOTAL);
		connManager.setDefaultMaxPerRoute(MAX_CONN_PER_ROUTE);
		// 池子里放了超过 2 秒的连接，拿出来用之前先探一下是不是已经被服务器关掉了，
		// 是的话丢掉重连。这样既不会用坏连接，也不用每次都重新握手。
		connManager.setValidateAfterInactivity(2000);
		HttpClientBuilder custom = HttpClients.custom();
		custom.setUserAgent("Mozilla/5.0 (Windows NT 6.1; rv:25.0) Gecko/20100101 Firefox/25.0");
		custom.setConnectionManager(connManager);
		custom.setDefaultRequestConfig(DEFAULT_REQUEST_CONFIG);
		// keep-alive 是开着的：HTTP/1.1 默认就是持久连接，响应读完以后连接放回池子里，下一个请求接着用。
		// 但服务器会主动关掉空闲连接，池子里留着这种已经断掉的连接，下次拿去用就会失败。
		// 定期把闲置超过 30 秒的连接清掉，保证取出来的都是活连接，不用反复重建。
		custom.evictIdleConnections(30, TimeUnit.SECONDS);
		custom.evictExpiredConnections();
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
		if (timeoutSec == DEFAULT_SO_TIMEOUT_SEC) {
			return DEFAULT_REQUEST_CONFIG;
		}
		// 单次请求会整个替换掉默认配置，所以要基于默认配置改，不要重新从头建
		return RequestConfig.copy(DEFAULT_REQUEST_CONFIG).setSocketTimeout(timeoutSec * 1000).build();
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
				throw new HttpStatusException(response.getStatusLine().getStatusCode(), response.getStatusLine().toString(), url);
			}
		} finally {
			response.close();
		}
	}

	/**
	 * 用 POST 方式访问远程网页，使用默认超时。
	 *
	 * @param url 地址
	 * @param charset 编码
	 * @param headers 请求头
	 * @param body 请求体
	 * @return 网页内容
	 */
	public static String postRemotePage(String url, String charset, Map<Object, Object> headers, String body) throws IOException {
		return postRemotePage(url, charset, headers, body, null);
	}

	/**
	 * 用 POST 方式访问远程网页。
	 *
	 * @param url 地址
	 * @param charset 编码
	 * @param headers 请求头
	 * @param body 请求体
	 * @param soTimeoutSec 读取超时秒数，传数字表示本次请求的超时时间；不传（null / undefined）或者小于等于 0 时使用默认超时
	 * @return 网页内容
	 */
	public static String postRemotePage(String url, String charset, Map<Object, Object> headers, String body, Object soTimeoutSec) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		httpPost.setConfig(requestConfig(soTimeoutSec));
		// 有些站点（顶漫画）用 POST + json 取章节列表，Content-Type 必须是 application/json
		httpPost.setEntity(new StringEntity(body == null ? "" : body, ContentType.APPLICATION_JSON));
		if (headers != null) {
			for (Map.Entry<Object, Object> entry : headers.entrySet()) {
				httpPost.addHeader(entry.getKey() + "", entry.getValue() + "");
			}
		}
		CloseableHttpResponse response = httpclient.execute(httpPost);
		try {
			HttpEntity entity1 = response.getEntity();
			if (response.getStatusLine().getStatusCode() == 200) {
				return EntityUtils.toString(entity1, charset);
			} else {
				EntityUtils.consume(entity1);
				throw new HttpStatusException(response.getStatusLine().getStatusCode(), response.getStatusLine().toString(), url);
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
	 * 写一张"图片不存在"的图到缓存目录。
	 *
	 * 渠道脚本判断这一页是真的下不下来了（不是网络问题），
	 * 就可以调用它存一张占位图，不用再一个一个图片服务器去试。
	 * 存完以后这一页在本地就有了，下次不会再去远程下载。
	 *
	 * 缓存文件名要跟下载图片时用的名字一样，不然界面找不到。
	 *
	 * @param channel 渠道名
	 * @param comicId 漫画 id
	 * @param sectionId 章节 id
	 * @param imageUrl 图片地址，缓存文件名没给的时候用它算
	 * @param cacheFileName 缓存文件名
	 */
	public static void writeNotFoundImage(String channel, String comicId, String sectionId, String imageUrl, String cacheFileName)
			throws IOException {
		File cacheFile = new File(System.getProperty("user.home") + "/.comic-view/cache/img/" + channel + "/" + comicId + "/" + sectionId + "/"
				+ StringUtils.defaultIfBlank(cacheFileName, FilenameUtils.getName(imageUrl)));
		writeNotFoundImage(cacheFile);
	}

	/**
	 * 判断这个异常是不是"图片不存在"（服务器返回了404）。
	 *
	 * 渠道脚本判断这一页是真的下不来了（而不是网络问题），
	 * 想写一张占位图不用再反复试的时候，用它来判断。
	 *
	 * @param e 捕获到的异常
	 * @return true表示服务器返回的是404
	 */
	public static boolean isNotFound(Object e) {
		Throwable cause = e instanceof Throwable ? (Throwable) e : null;
		if (cause instanceof WrappedException) {
			// 脚本里catch到的是Rhino包了一层的异常
			cause = ((WrappedException) cause).getWrappedException();
		}
		if (cause instanceof HttpStatusException) {
			return ((HttpStatusException) cause).getStatusCode() == 404;
		}
		// 兜底：异常消息里有状态码，比如"获得内容失败:HTTP/1.1 404 Not Found"
		String message = messageOf(e);
		return message != null && message.contains(" 404 ");
	}

	/** 取异常的消息，脚本里catch到的异常也能取到 */
	private static String messageOf(Object e) {
		if (e instanceof Throwable) {
			return ((Throwable) e).getMessage();
		}
		if (e instanceof Scriptable) {
			Object message = ((Scriptable) e).get("message", (Scriptable) e);
			return message == null ? null : Context.toString(message);
		}
		return null;
	}

	/**
	 * 造一张"图片不存在"的图，保存到指定文件。
	 *
	 * @param file 保存的文件
	 */
	public static void writeNotFoundImage(File file) throws IOException {
		int width = 600;
		int height = 200;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		try {
			g.setColor(new Color(60, 60, 60));
			g.fillRect(0, 0, width, height);
			g.setColor(new Color(210, 210, 210));
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
			// 字写在中间
			String text = "图片不存在";
			int textWidth = g.getFontMetrics().stringWidth(text);
			int textHeight = g.getFontMetrics().getHeight();
			g.drawString(text, (width - textWidth) / 2, (height - textHeight) / 2 + g.getFontMetrics().getAscent());
		} finally {
			g.dispose();
		}
		File parentFile = file.getParentFile();
		if (parentFile != null && !parentFile.exists()) {
			parentFile.mkdirs();
		}
		ImageIO.write(image, imageFormat(file), file);
	}

	/**
	 * 缓存文件用什么格式存。jpg 就存 jpg，别的存 png。
	 * 文件名后缀和内容对不上也没关系，读图片的时候是看内容判断格式的。
	 */
	private static String imageFormat(File file) {
		String name = FilenameUtils.getExtension(file.getName()).toLowerCase();
		if ("jpg".equals(name) || "jpeg".equals(name)) {
			return "jpg";
		}
		return "png";
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
	/**
	 * 把下好的临时文件挪到正式位置。
	 */
	private static void moveToTarget(File tmpFile, File file) throws IOException {
		if (file.exists() && !file.delete()) {
			logger.log("删除旧文件失败 {}", file);
		}
		if (!tmpFile.renameTo(file)) {
			// 改名失败就退回直接写过去
			OutputStream os = new FileOutputStream(file);
			try {
				FileInputStream in = new FileInputStream(tmpFile);
				try {
					IOUtils.copy(in, os);
					os.flush();
				} finally {
					in.close();
				}
			} finally {
				os.close();
			}
			if (tmpFile.exists() && !tmpFile.delete()) {
				logger.log("删除临时文件失败 {}", tmpFile);
			}
		}
	}

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
				// 先下到临时文件，下完整了再改名。
				// 直接写目标文件的话，中途断掉会在缓存里留下半个坏文件，
				// 以后每次读它都失败，而且没法自动恢复。
				File tmpFile = new File(file.getAbsolutePath() + "." + System.nanoTime() + ".tmp");
				try {
					OutputStream os = new FileOutputStream(tmpFile);
					try {
						IOUtils.copy(entity1.getContent(), os);
						os.flush();
					} finally {
						os.close();
					}
					EntityUtils.consume(entity1);
					moveToTarget(tmpFile, file);
				} catch (Exception e) {
					// 没下完，临时文件删掉，别留坏文件
					if (tmpFile.exists() && !tmpFile.delete()) {
						logger.log("删除临时文件失败 {}", tmpFile);
					}
					throw e;
				}
			} else {
				EntityUtils.consume(entity1);
				throw new HttpStatusException(response.getStatusLine().getStatusCode(), response.getStatusLine().toString(), url);
			}
		} finally {
			response.close();
		}
	}
}
