/**
 * 
 */
package com.raddle.comic.engine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.LoggerFactory;

import com.raddle.comic.LogWrapper;
import com.raddle.comic.engine.dom.DomDocument;

/**
 * @author raddle
 *
 */
public class ComicPluginEngine {
	private static LogWrapper logger = new LogWrapper(LoggerFactory.getLogger(ComicPluginEngine.class));
	/** DOM 辅助脚本，提供 parseHtml 等全局函数 */
	private static final String DOM_SCRIPT = "/dom.js";
	private Scriptable topScope;
	private File pluginFile;

	public void init(File pluginFile) throws IOException {
		if (topScope != null) {
			throw new IllegalStateException("ComicPluginEngine was initialized ");
		}
		Context context = Context.getCurrentContext();
		if (context == null) {
			context = Context.enter();
		}
		topScope = context.initStandardObjects();
		ScriptableObject.putProperty(topScope, "log", LoggerFactory.getLogger(pluginFile.getName()));
		ScriptableObject.putProperty(topScope, "httpclient", new HttpHelper());
		ScriptableObject.putProperty(topScope, "engine", this);
		loadDomScript(context);
		this.pluginFile = pluginFile;
		context.evaluateString(topScope, FileUtils.readFileToString(pluginFile, "utf-8"), "<" + pluginFile.getName() + ">", 1, null);
	}

	/**
	 * 把 DOM 辅助脚本注入 scope，脚本里可以直接用 parseHtml(html)
	 */
	private void loadDomScript(Context context) {
		InputStream inputStream = ComicPluginEngine.class.getResourceAsStream(DOM_SCRIPT);
		if (inputStream == null) {
			logger.log("dom script not found: {}", DOM_SCRIPT);
			return;
		}
		try {
			context.evaluateReader(topScope, new InputStreamReader(inputStream, "utf-8"), "<dom.js>", 1, null);
		} catch (IOException e) {
			logger.log("load dom script error", e);
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				// 忽略
			}
		}
	}

	/**
	 * 解析 HTML，返回可以按 CSS 选择器查询的文档对象。
	 *
	 * @param html 网页内容
	 * @return 文档对象，用法：doc.select("div.list a")、doc.query("title")
	 */
	public DomDocument parseHtml(String html) {
		if (html == null) {
			return null;
		}
		return new DomDocument(Jsoup.parse(html));
	}


	public ChannelInfo getChannelInfo() {
		NativeObject channelObj = (NativeObject) topScope.get("channel", topScope);
		ChannelInfo channelInfo = new ChannelInfo();
		if (channelObj != null && channelObj != Scriptable.NOT_FOUND) {
			channelInfo.setName(asString(channelObj.get("name", topScope)));
			channelInfo.setHome(asString(channelObj.get("home", topScope)));
			channelInfo.setDesc(asString(channelObj.get("desc", topScope)));
			Object index = channelObj.get("index", topScope);
			if (index != null && index != Scriptable.NOT_FOUND) {
				channelInfo.setIndex(((Number) index).intValue());
			}
		}
		if (channelInfo.getName() != null) {
			return channelInfo;
		} else {
			logger.log("missing var channel in file[{}]", pluginFile);
		}
		return null;
	}

	/**
	 * 取 JS 里的字符串值。
	 * Rhino 拼接出来的字符串是 ConsString，不是 java.lang.String，直接强转会出错，
	 * 所以这里统一用 Context.toString 转换。
	 */
	private static String asString(Object value) {
		if (value == null || value == Scriptable.NOT_FOUND) {
			return null;
		}
		return Context.toString(value);
	}

	public ComicInfo getSections(String comicId) {
		ComicInfo comicInfo = new ComicInfo();
		Function getSections = (Function) topScope.get("getSections", topScope);
		NativeObject result = (NativeObject) getSections.call(Context.getCurrentContext(), topScope, Context.getCurrentContext().newObject(topScope),
				new Object[] { comicId });
		if (result == null) {
			logger.log("getSections can't find section comicId[{}]", comicId);
			return null;
		}
		if (result.get("sections") instanceof NativeArray) {
			List<SectionInfo> sectionInfos = new ArrayList<SectionInfo>();
			NativeArray array = (NativeArray) result.get("sections");
			for (Object object : array) {
				if (object instanceof NativeObject) {
					NativeObject o = (NativeObject) object;
					SectionInfo info = new SectionInfo();
					info.setSectionId(asString(o.get("sectionId", topScope)));
					info.setName(asString(o.get("name", topScope)));
					sectionInfos.add(info);
				}
			}
			comicInfo.setSections(sectionInfos);
		}
		if (result.get("comicName") != null && result.get("comicName") != Scriptable.NOT_FOUND) {
			comicInfo.setComicName(asString(result.get("comicName", topScope)));
		}
		return comicInfo;
	}

	public List<PageInfo> getPages(String comicId, String sectionId) {
		List<PageInfo> pageInfos = new ArrayList<PageInfo>();
		Function getPages = (Function) topScope.get("getPages", topScope);
		Object result = getPages.call(Context.getCurrentContext(), topScope, Context.getCurrentContext().newObject(topScope), new Object[] { comicId,
				sectionId });
		if (result instanceof NativeArray) {
			NativeArray array = (NativeArray) result;
			for (Object object : array) {
				if (object instanceof NativeObject) {
					NativeObject o = (NativeObject) object;
					PageInfo info = new PageInfo();
					if (o.get("pageNo", topScope) != null && o.get("pageNo", topScope) != Scriptable.NOT_FOUND) {
						info.setPageNo(new Double(o.get("pageNo", topScope) + "").intValue());
					}
					if (o.get("filename", topScope) != null && o.get("filename", topScope) != Scriptable.NOT_FOUND) {
						info.setFilename(asString(o.get("filename", topScope)));
					}
					if (o.get("pageUrl", topScope) != null && o.get("pageUrl", topScope) != Scriptable.NOT_FOUND) {
						info.setPageUrl(asString(o.get("pageUrl", topScope)));
					}
					pageInfos.add(info);
				}
			}
		} else if (result != null) {
			logger.log("getPages return unsupported result type :" + result.getClass());
		} else {
			logger.log("getPages can't find page comicId[{}] sectionId[{}]", comicId, sectionId);
		}
		return pageInfos;
	}

	public void loadRemoteImage(String comicId, String sectionId, Integer pageNo, String imageUrl) {
		if (Context.getCurrentContext() == null) {
			// 说明起了多线程
			Context.enter();
		}
		try {
			Function loadRemoteImage = (Function) topScope.get("loadRemoteImage", topScope);
			loadRemoteImage.call(Context.getCurrentContext(), topScope, Context.getCurrentContext().newObject(topScope), new Object[] { comicId,
					sectionId, pageNo, imageUrl });
		} finally {
			Context.exit();
		}
	}

	public static List<ChannelInfo> getChannelList(File pluginDir) {
		List<ChannelInfo> list = new ArrayList<ChannelInfo>();
		if (pluginDir.isDirectory()) {
			Collection<File> listFiles = FileUtils.listFiles(pluginDir, new String[] { "js" }, true);
			for (File file : listFiles) {
				ComicPluginEngine pluginEngine = new ComicPluginEngine();
				try {
					pluginEngine.init(file);
					ChannelInfo channelInfo = pluginEngine.getChannelInfo();
					if (channelInfo != null) {
						channelInfo.setScriptFile(file);
						list.add(channelInfo);
					}
					Collections.sort(list, new Comparator<ChannelInfo>() {

						@Override
						public int compare(ChannelInfo o1, ChannelInfo o2) {
							if (o1.getIndex() == null) {
								return 1;
							}
							if (o2.getIndex() == null) {
								return -1;
							}
							return o1.getIndex().compareTo(o2.getIndex());
						}

					});
				} catch (Exception e) {
					logger.log(e.getMessage(), e);
				} finally {
					pluginEngine.close();
				}
			}
		} else {
			logger.log("dir[{}] not exist or not a dir", pluginDir);
		}
		return list;
	}

	public void close() {
		if (Context.getCurrentContext() != null) {
			Context.exit();
		}
	}

	public Scriptable eval(Scriptable scope, String script) {
		ScriptableObject newObject = (ScriptableObject) Context.getCurrentContext().newObject(scope);
		newObject.setPrototype(scope);
		newObject.setParentScope(null);
		Context.getCurrentContext().initStandardObjects(newObject);
		Context.getCurrentContext().evaluateString(newObject, script, "<eval>", 1, null);
		return newObject;
	}

}
