package com.raddle.comic.engine.dom;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * DOM 节点的包装类，暴露给 Rhino 脚本使用。
 *
 * jsoup 的 Element 本身也能直接用，但它返回的子节点集合是 java.util.List，
 * 在 JS 里只能 size()/get(i)，没有 length 和下标访问，所以这里统一包一层，
 * 让 select()/children() 返回数组。
 */
public class DomNode {

	protected final Element element;

	public DomNode(Element element) {
		this.element = element;
	}

	/** 节点内的全部文本 */
	public String text() {
		return element.text();
	}

	/** 节点自身的文本（不含子节点的文本） */
	public String ownText() {
		return element.ownText();
	}

	/** 节点内部的 HTML */
	public String html() {
		return element.html();
	}

	/** 节点自身的 HTML */
	public String outerHtml() {
		return element.outerHtml();
	}

	/** 标签名，如 a、div */
	public String tag() {
		return element.tagName();
	}

	/** id 属性 */
	public String id() {
		return element.id();
	}

	/** class 属性 */
	public String className() {
		return element.className();
	}

	/** 是否包含指定的 class */
	public boolean hasClass(String className) {
		return element.hasClass(className);
	}

	/** 取属性值，不存在时返回空字符串 */
	public String attr(String name) {
		return element.attr(name);
	}

	/** 设置属性值 */
	public void attr(String name, String value) {
		element.attr(name, value);
	}

	/** 属性的绝对地址，文档里有 base 标签时才能把相对地址补全 */
	public String absUrl(String name) {
		return element.absUrl(name);
	}

	/** 按 CSS 选择器查找后代节点 */
	public DomNode[] select(String css) {
		Elements elements = element.select(css);
		DomNode[] nodes = new DomNode[elements.size()];
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = new DomNode(elements.get(i));
		}
		return nodes;
	}

	/** 按 CSS 选择器查找第一个匹配的后代节点，没有时返回 null */
	public DomNode query(String css) {
		Element found = element.selectFirst(css);
		return found == null ? null : new DomNode(found);
	}

	/** 直接子节点 */
	public DomNode[] children() {
		Elements elements = element.children();
		DomNode[] nodes = new DomNode[elements.size()];
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = new DomNode(elements.get(i));
		}
		return nodes;
	}

	/** 父节点 */
	public DomNode parent() {
		Element parent = element.parent();
		return parent == null ? null : new DomNode(parent);
	}

	/** 下一个兄弟节点 */
	public DomNode next() {
		Element next = element.nextElementSibling();
		return next == null ? null : new DomNode(next);
	}

	/** 原始的 jsoup 元素，需要用 jsoup 特有功能时使用 */
	public Element element() {
		return element;
	}

	@Override
	public String toString() {
		return element.outerHtml();
	}
}
