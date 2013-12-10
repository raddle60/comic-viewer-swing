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
	private final static CloseableHttpClient httpclient;
	static {
		HttpClientBuilder custom = HttpClients.custom();
		custom.setUserAgent("Mozilla/5.0 (Windows NT 6.1; rv:25.0) Gecko/20100101 Firefox/25.0");
		httpclient = custom.build();
	}

	public static String getRemotePage(String url, String charset, Map<Object, Object> headers) throws IOException {
		HttpGet httpGet = new HttpGet(url);
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

	public static void saveRemoteImage(String channel, String comicId, String sectionId, String imageUrl, String cacheFileName,
			Map<Object, Object> headers) throws IOException {
		File cacheFile = new File(System.getProperty("user.home") + "/.comic-view/cache/img/" + channel + "/" + comicId + "/" + sectionId + "/"
				+ StringUtils.defaultIfBlank(cacheFileName, FilenameUtils.getName(imageUrl)));
		saveRemotePage(imageUrl, headers, cacheFile);
	}

	public static void saveRemotePage(String url, Map<Object, Object> headers, File file) throws IOException {
		HttpGet httpGet = new HttpGet(url);
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
