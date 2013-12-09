/**
 * 
 */
package com.raddle.comic.engine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.LoggerFactory;

import com.raddle.comic.LogWrapper;

/**
 * @author raddle
 * 
 */
public class ComicPluginEngine {
	private static LogWrapper logger = new LogWrapper(LoggerFactory.getLogger(ComicPluginEngine.class));
	private Context context;
	private Scriptable topScope;
	private File pluginFile;

	public void init(File pluginFile) throws IOException {
		if (context != null) {
			throw new IllegalStateException("ComicPluginEngine was initialized ");
		}
		context = Context.enter();
		topScope = context.initStandardObjects();
		ScriptableObject.putProperty(topScope, "log", LoggerFactory.getLogger("ChannelJs"));
		ScriptableObject.putProperty(topScope, "httpclient", new HttpHelper());
		ScriptableObject.putProperty(topScope, "engine", this);
		this.pluginFile = pluginFile;
		context.evaluateString(topScope, FileUtils.readFileToString(pluginFile, "utf-8"), "<" + pluginFile.getName() + ">", 1, null);
	}

	public ChannelInfo getChannelInfo() {
		NativeObject channelObj = (NativeObject) topScope.get("channel", topScope);
		ChannelInfo channelInfo = new ChannelInfo();
		if (channelObj != null && channelObj != Scriptable.NOT_FOUND) {
			if (channelObj.get("name", topScope) != null && channelObj.get("name", topScope) != Scriptable.NOT_FOUND) {
				channelInfo.setName((String) channelObj.get("name", topScope));
			}
			if (channelObj.get("home", topScope) != null && channelObj.get("home", topScope) != Scriptable.NOT_FOUND) {
				channelInfo.setHome((String) channelObj.get("home", topScope));
			}
			if (channelObj.get("desc", topScope) != null && channelObj.get("desc", topScope) != Scriptable.NOT_FOUND) {
				channelInfo.setDesc((String) channelObj.get("desc", topScope));
			}
		}
		if (channelInfo.getName() != null) {
			return channelInfo;
		} else {
			logger.log("missing var channel in file[{}]", pluginFile);
		}
		return null;
	}

	public List<SectionInfo> getSections(String comicId) {
		List<SectionInfo> sectionInfos = new ArrayList<SectionInfo>();
		Function getSections = (Function) topScope.get("getSections", topScope);
		Object result = getSections.call(context, topScope, context.newObject(topScope), new Object[] { comicId });
		if (result instanceof NativeArray) {
			NativeArray array = (NativeArray) result;
			for (Object object : array) {
				if (object instanceof NativeObject) {
					NativeObject o = (NativeObject) object;
					SectionInfo info = new SectionInfo();
					if (o.get("sectionId", topScope) != null && o.get("sectionId", topScope) != Scriptable.NOT_FOUND) {
						info.setSectionId(o.get("sectionId", topScope) + "");
					}
					if (o.get("name", topScope) != null && o.get("name", topScope) != Scriptable.NOT_FOUND) {
						info.setName(o.get("name", topScope) + "");
					}
					sectionInfos.add(info);
				}
			}
		} else if (result != null) {
			logger.log("getSections return unsupported result type :" + result.getClass());
		} else {
			logger.log("getSections can't find section comicId[{}]", comicId);
		}
		return sectionInfos;
	}

	public List<PageInfo> getPages(String comicId, String sectionId) {
		List<PageInfo> pageInfos = new ArrayList<PageInfo>();
		Function getPages = (Function) topScope.get("getPages", topScope);
		Object result = getPages.call(context, topScope, context.newObject(topScope), new Object[] { comicId, sectionId });
		if (result instanceof NativeArray) {
			NativeArray array = (NativeArray) result;
			for (Object object : array) {
				if (object instanceof NativeObject) {
					NativeObject o = (NativeObject) object;
					PageInfo info = new PageInfo();
					if (o.get("pageNo", topScope) != null && o.get("pageNo", topScope) != Scriptable.NOT_FOUND) {
						info.setPageNo(new Double(o.get("pageNo", topScope) + "").intValue());
					}
					if (o.get("pageUrl", topScope) != null && o.get("pageUrl", topScope) != Scriptable.NOT_FOUND) {
						info.setPageUrl(o.get("pageUrl", topScope) + "");
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

	public void loadRemoteImage(String comicId, String sectionId, String imageUrl) {
		Function loadRemoteImage = (Function) topScope.get("loadRemoteImage", topScope);
		loadRemoteImage.call(context, topScope, context.newObject(topScope), new Object[] { comicId, sectionId, imageUrl });
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
				} catch (IOException e) {
					logger.log(e.getMessage(), e);
				} finally {
					pluginEngine.close();
				}
			}
		} else {
			logger.log("dir[{}] not exist or not a dir", pluginDir);
			JOptionPane.showMessageDialog(null, LogWrapper.replacePlaceHolder("目录[{}]不存在或不是目录", pluginDir));
		}
		return list;
	}

	public void close() {
		Context.exit();
	}

	public Scriptable eval(Scriptable scope, String script) {
		ScriptableObject newObject = (ScriptableObject) context.newObject(scope);
		newObject.setPrototype(scope);
		newObject.setParentScope(null);
		Context.getCurrentContext().initStandardObjects(newObject);
		context.evaluateString(newObject, script, "<eval>", 1, null);
		return newObject;
	}

}