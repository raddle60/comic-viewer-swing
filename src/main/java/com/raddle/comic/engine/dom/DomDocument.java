package com.raddle.comic.engine.dom;

import org.jsoup.nodes.Document;

/**
 * 整篇 HTML 文档的包装类，parseHtml 的返回值。
 */
public class DomDocument extends DomNode {

	public DomDocument(Document document) {
		super(document);
	}

	private Document document() {
		return (Document) element;
	}

	/** 页面标题 */
	public String title() {
		return document().title();
	}

	/** 文档的位置，也就是 base URI */
	public String location() {
		return document().location();
	}

	/** 整页文本 */
	@Override
	public String text() {
		return document().text();
	}
}
