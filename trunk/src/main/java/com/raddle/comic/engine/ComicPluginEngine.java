/**
 * 
 */
package com.raddle.comic.engine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
			if (channelObj.get("index", topScope) != null && channelObj.get("index", topScope) != Scriptable.NOT_FOUND) {
				channelInfo.setIndex(((Number) channelObj.get("index", topScope)).intValue());
			}
		}
		if (channelInfo.getName() != null) {
			return channelInfo;
		} else {
			logger.log("missing var channel in file[{}]", pluginFile);
		}
		return null;
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
					if (o.get("sectionId", topScope) != null && o.get("sectionId", topScope) != Scriptable.NOT_FOUND) {
						info.setSectionId(o.get("sectionId", topScope) + "");
					}
					if (o.get("name", topScope) != null && o.get("name", topScope) != Scriptable.NOT_FOUND) {
						info.setName(o.get("name", topScope) + "");
					}
					sectionInfos.add(info);
				}
			}
			comicInfo.setSections(sectionInfos);
		}
		if (result.get("comicName") != null && result.get("comicName") != Scriptable.NOT_FOUND) {
			comicInfo.setComicName((String) result.get("comicName"));
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
						info.setFilename(o.get("filename", topScope) + "");
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
