package com.raddle.comic;

import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class RhinoTest {
	@Test
	public void testEval() throws IOException {
		Context context = Context.enter();
		Scriptable topScope = context.initStandardObjects();
		ScriptableObject.putProperty(topScope, "out", System.out);
		try {
			context.evaluateReader(topScope, new InputStreamReader(RhinoTest.class.getResourceAsStream("/eval.js"), "utf-8"), "<eval>", 1, null);
		} finally {
			Context.exit();
		}
	}
}