/**
 * 
 */
package com.raddle.comic.engine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * @author raddle
 * 
 */
public class ComicPluginEngine {
	private Context context;
	private Scriptable topScope;
	private File pluginFile;

	public void init() throws IOException {
		if (context != null) {
			throw new IllegalStateException("ComicPluginEngine was initialized ");
		}
		context = Context.enter();
		topScope = context.initStandardObjects();
		ScriptableObject.putProperty(topScope, "out", System.out);
		ScriptableObject.putProperty(topScope, "httpclient", new HttpHelper());
		ScriptableObject.putProperty(topScope, "engine", this);
		pluginFile = new File("D:\\workspaces\\raddle\\playlist\\src\\main\\resources\\comic.sfacg.com.js");
		context.evaluateString(topScope, FileUtils.readFileToString(pluginFile, "utf-8"), "<" + pluginFile.getName() + ">", 1, null);
	}

	public List<PageInfo> getPages(String comicId, String sectionId) {
		List<PageInfo> pageInfos = new ArrayList<PageInfo>();
		Function getPages = (Function) topScope.get("getPages", topScope);
		Object result = getPages.call(context, topScope, topScope, new Object[] { comicId, sectionId });
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
		}
		return pageInfos;
	}

	public void close() {
		Context.exit();
	}

	public void eval(String script) {
		context.evaluateString(topScope, script, "<inner>", 1, null);
	}

}
