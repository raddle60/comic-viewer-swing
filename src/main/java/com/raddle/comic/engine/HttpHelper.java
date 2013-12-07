/**
 * 
 */
package com.raddle.comic.engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * @author raddle
 * 
 */
public class HttpHelper {
	public static String getRemotePage(String url, String charset) throws IOException {
		CloseableHttpClient httpclient = HttpClients.createMinimal();
		HttpGet httpGet = new HttpGet(url);
		CloseableHttpResponse response = httpclient.execute(httpGet);
		try {
			HttpEntity entity1 = response.getEntity();
			if (response.getStatusLine().getStatusCode() == 200) {
				return EntityUtils.toString(entity1, charset);
			} else {
				EntityUtils.consume(entity1);
				throw new RuntimeException("获得内容失败:" + response.getStatusLine());
			}
		} finally {
			response.close();
		}
	}

	public static void saveRemotePage(String url, File file) throws IOException {
		CloseableHttpClient httpclient = HttpClients.createMinimal();
		HttpGet httpGet = new HttpGet(url);
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
				throw new RuntimeException("获得内容失败:" + response.getStatusLine());
			}
		} finally {
			response.close();
		}
	}
}
