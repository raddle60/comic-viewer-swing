package com.raddle.comic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.lang.reflect.Field;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import com.raddle.comic.engine.ComicPluginEngine;

public class DomTest {

	private static final String PLUGIN_JS = "var doc = parseHtml(\"\" +\n"
			+ "\"<html><head><title>测试漫画</title></head><body>\" +\n"
			+ "\"<div class='cbc-list'><ul><li><a class='tg' href='/m148306/'>第一章</a></li>\" +\n"
			+ "\"<li><a class='tg' href='/t148760/'>第二章</a></li></ul></div>\" +\n"
			+ "\"</body></html>\");\n" + "var links = doc.select(\".cbc-list li a\");\n" + "var result = {\n"
			+ "  title : doc.title(),\n" + "  count : links.length,\n" + "  href : links.attr(\"href\"),\n"
			+ "  firstText : links.first().text(),\n" + "  lastText : links.last().text(),\n"
			+ "  joinedText : links.text(),\n" + "  queryText : doc.query(\"title\").text(),\n"
			+ "  childCount : doc.select(\".cbc-list ul\").children().length,\n" + "  noMatch : doc.select(\"div.nope\").length,\n"
			+ "  emptyFirst : doc.select(\"div.nope\").first(),\n" + "  engineTitle : engine.parseHtml(\"<title>另一个</title>\").title(),\n"
			+ "  texts : links.map(function (i, a) { return a.text(); }),\n" + "  evalOk : typeof engine.eval(this, \"var a = 1;\"),\n" + "  sumLength : 0\n" + "};\n"
			+ "links.each(function (i, a) { result.sumLength += a.text().length; });\n";

	@Test
	public void testParseHtml() throws Exception {
		File pluginFile = File.createTempFile("dom-test", ".js");
		pluginFile.deleteOnExit();
		FileUtils.write(pluginFile, PLUGIN_JS, "utf-8");

		ComicPluginEngine engine = new ComicPluginEngine();
		engine.init(pluginFile);
		try {
			Field field = ComicPluginEngine.class.getDeclaredField("topScope");
			field.setAccessible(true);
			Scriptable scope = (Scriptable) field.get(engine);
			NativeObject result = (NativeObject) scope.get("result", scope);

			assertEquals("测试漫画", result.get("title", scope));
			assertEquals(2, ((Number) result.get("count", scope)).intValue());
			assertEquals("/m148306/", result.get("href", scope));
			assertEquals("第一章", result.get("firstText", scope));
			assertEquals("第二章", result.get("lastText", scope));
			assertEquals("第一章第二章", result.get("joinedText", scope));
			assertEquals("测试漫画", result.get("queryText", scope));
			assertEquals(2, ((Number) result.get("childCount", scope)).intValue());
			assertEquals(0, ((Number) result.get("noMatch", scope)).intValue());
			assertNull(result.get("emptyFirst", scope));
			assertEquals("另一个", result.get("engineTitle", scope));
			assertEquals("object", result.get("evalOk", scope));
			assertEquals(6, ((Number) result.get("sumLength", scope)).intValue());

			NativeArray texts = (NativeArray) result.get("texts", scope);
			assertEquals(2, texts.getLength());
			assertEquals("第一章", texts.get(0, scope));
			assertEquals("第二章", texts.get(1, scope));
		} finally {
			engine.close();
		}
	}
}
