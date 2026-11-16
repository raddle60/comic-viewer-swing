package com.raddle.comic.engine;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import javax.imageio.ImageIO;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * 渠道脚本能调用 httpclient.writeNotFoundImage 写一张"图片不存在"的图。
 * 脚本判断这一页下不下来了，就写一张占位图，不用再反复试。
 */
public class NotFoundImageTest {

	/** 测试专用的渠道名，写完就删掉 */
	private static final String TEST_CHANNEL = "test-notfound";

	@Test
	public void 脚本能写notfound图片() throws Exception {
		File cacheDir = new File(System.getProperty("user.home") + "/.comic-view/cache/img/" + TEST_CHANNEL);
		try {
			Context context = Context.enter();
			try {
				Scriptable topScope = context.initStandardObjects();
				ScriptableObject.putProperty(topScope, "httpclient", new HttpHelper());
				context.evaluateString(topScope, "httpclient.writeNotFoundImage('" + TEST_CHANNEL + "', 'c1', 's1', 'http://test.local/1.jpg', '1.png');",
						"<test>", 1, null);
			} finally {
				Context.exit();
			}

			File file = new File(cacheDir, "c1/s1/1.png");
			Assert.assertTrue("脚本应该把not found的图片写到缓存里", file.exists());
			Assert.assertNotNull("写下来的要是一张能读的图片", ImageIO.read(file));
			Assert.assertEquals(600, ImageIO.read(file).getWidth());
			Assert.assertEquals(200, ImageIO.read(file).getHeight());
		} finally {
			deleteDir(cacheDir);
		}
	}

	/** 删掉测试写的目录 */
	private static void deleteDir(File dir) {
		if (!dir.exists()) {
			return;
		}
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					deleteDir(file);
				} else {
					file.delete();
				}
			}
		}
		dir.delete();
	}

	/**
	 * 脚本里捕获到异常以后，用 httpclient.isNotFound(e) 判断是不是图片不存在。
	 * 这里起一个真的返回404的服务器来试。
	 *
	 * 脚本里直接调用（插件就是这种写法），异常会被Rhino包一层，
	 * 包在js函数里调用又会多包一层，两种都要能判断出来。
	 */
	@Test
	public void 脚本能判断是不是404() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				exchange.sendResponseHeaders(404, -1);
				exchange.close();
			}
		});
		server.start();
		File cacheDir = new File(System.getProperty("user.home") + "/.comic-view/cache/img/" + TEST_CHANNEL);
		try {
			String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/1.png";
			// 脚本里直接调用
			Assert.assertEquals("404应该判断成图片不存在", Boolean.TRUE, check404(url, false));
			// 包在js函数里调用，异常会被Rhino再多包一层
			Assert.assertEquals("404应该判断成图片不存在", Boolean.TRUE, check404(url, true));
		} finally {
			server.stop(0);
			deleteDir(cacheDir);
		}
	}

	/**
	 * 在脚本里下载一个一定返回404的地址，catch到异常以后用 isNotFound 判断。
	 *
	 * @param inFunction true表示把调用包在js函数里
	 * @return 脚本判断的结果
	 */
	private boolean check404(String url, boolean inFunction) throws Exception {
		Context context = Context.enter();
		try {
			Scriptable topScope = context.initStandardObjects();
			ScriptableObject.putProperty(topScope, "httpclient", new HttpHelper());
			String call = "httpclient.saveRemoteImage('" + TEST_CHANNEL + "', 'c2', 's1', '" + url + "', '1.png', {}, 5);";
			String script;
			if (inFunction) {
				script = "var notFound = false;\n"
						+ "function tryDownload() {\n"
						+ "	" + call + "\n"
						+ "}\n"
						+ "try {\n"
						+ "	tryDownload();\n"
						+ "} catch (e) {\n"
						+ "	notFound = httpclient.isNotFound(e);\n"
						+ "}\n";
			} else {
				script = "var notFound = false;\n"
						+ "try {\n"
						+ "	" + call + "\n"
						+ "} catch (e) {\n"
						+ "	notFound = httpclient.isNotFound(e);\n"
						+ "}\n";
			}
			context.evaluateString(topScope, script, "<test>", 1, null);
			Object result = context.evaluateString(topScope, "notFound", "<test>", 1, null);
			return Context.toBoolean(result);
		} finally {
			Context.exit();
		}
	}
}
