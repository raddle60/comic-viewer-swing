/**
 * DOM 辅助脚本，在渠道脚本之前注入到 scope 中。
 *
 * 提供全局函数 parseHtml(html)，返回可以按 CSS 选择器查询的文档对象。
 *
 * Rhino 1.7R4 里 Java 方法返回的 String 会被包成 NativeJavaObject，
 * typeof 是 "object"，.length 之类的属性不能用，返回给 Java 时也会转型失败，
 * 所以这里统一在 JS 层包一层，把值都转成真正的 JS 字符串和数组。
 */

function parseHtml(html) {
	return __domDocument(__comicEngine.parseHtml(html));
}

/**
 * 定义一个不可枚举的方法，免得污染 for-in 循环
 */
function __domDefine(obj, name, fn) {
	try {
		if (typeof Object.defineProperty === 'function') {
			Object.defineProperty(obj, name, {
				value : fn,
				enumerable : false,
				writable : true,
				configurable : true
			});
			return;
		}
	} catch (e) {
		// Rhino 部分版本不支持，退回普通赋值
	}
	obj[name] = fn;
}

/**
 * Java 的 String 转成 JS 的 String
 * 末尾的 toString 是为了把 Rhino 拼接出来的 ConsString 摊平成普通字符串
 */
function __domText(value) {
	return value == null ? null : String(value).toString();
}

/**
 * 包一个节点
 */
function __domNode(node) {
	if (node == null) {
		return null;
	}
	return {
		/** 节点内的全部文本 */
		text : function () {
			return __domText(node.text());
		},
		/** 节点自身的文本 */
		ownText : function () {
			return __domText(node.ownText());
		},
		/** 节点内部的 HTML */
		html : function () {
			return __domText(node.html());
		},
		/** 节点自身的 HTML */
		outerHtml : function () {
			return __domText(node.outerHtml());
		},
		/** 标签名，如 a、div */
		tag : function () {
			return __domText(node.tag());
		},
		/** id 属性 */
		id : function () {
			return __domText(node.id());
		},
		/** class 属性 */
		className : function () {
			return __domText(node.className());
		},
		/** 是否包含指定的 class */
		hasClass : function (className) {
			return node.hasClass(String(className));
		},
		/** 取属性值 */
		attr : function (name) {
			return __domText(node.attr(String(name)));
		},
		/** 设置属性值 */
		setAttr : function (name, value) {
			node.attr(String(name), String(value));
		},
		/** 属性的绝对地址 */
		absUrl : function (name) {
			return __domText(node.absUrl(String(name)));
		},
		/** 按 CSS 选择器查找后代节点 */
		select : function (css) {
			return __domList(node.select(String(css)));
		},
		/** 按 CSS 选择器查找第一个匹配的后代节点，没有时返回 null */
		query : function (css) {
			return __domNode(node.query(String(css)));
		},
		/** 直接子节点 */
		children : function () {
			return __domList(node.children());
		},
		/** 父节点 */
		parent : function () {
			return __domNode(node.parent());
		},
		/** 下一个兄弟节点 */
		next : function () {
			return __domNode(node.next());
		},
		/** 原始的 jsoup 元素 */
		element : function () {
			return node.element();
		}
	};
}

/**
 * 包一个文档
 */
function __domDocument(doc) {
	if (doc == null) {
		return null;
	}
	var wrapped = __domNode(doc);
	/** 页面标题 */
	wrapped.title = function () {
		return __domText(doc.title());
	};
	/** 文档位置，也就是 base URI */
	wrapped.location = function () {
		return __domText(doc.location());
	};
	return wrapped;
}

/**
 * 包一组节点，返回真正的 JS 数组，并加上几个 jQuery 风格的快捷方法
 */
function __domList(nodes) {
	var list = [];
	for (var i = 0; i < nodes.length; i++) {
		list[list.length] = __domNode(nodes[i]);
	}
	/** 遍历，函数收到两个参数：下标、节点 */
	__domDefine(list, 'each', function (fn) {
		for (var i = 0; i < this.length; i++) {
			fn(i, this[i]);
		}
		return this;
	});
	/** 第一个节点，没有节点时返回 null */
	__domDefine(list, 'first', function () {
		return this.length > 0 ? this[0] : null;
	});
	/** 最后一个节点，没有节点时返回 null */
	__domDefine(list, 'last', function () {
		return this.length > 0 ? this[this.length - 1] : null;
	});
	/** 所有节点的文本拼在一起 */
	__domDefine(list, 'text', function () {
		var text = '';
		for (var i = 0; i < this.length; i++) {
			text = text + this[i].text();
		}
		return text.toString();
	});
	/** 取第一个节点的属性值，没有节点时返回 null */
	__domDefine(list, 'attr', function (name) {
		return this.length > 0 ? this[0].attr(name) : null;
	});
	/** 用函数处理每个节点，返回结果数组 */
	__domDefine(list, 'map', function (fn) {
		var result = [];
		for (var i = 0; i < this.length; i++) {
			result[result.length] = fn(i, this[i]);
		}
		return result;
	});
	/** 所有节点的子节点拼在一起 */
	__domDefine(list, 'children', function () {
		var result = [];
		for (var i = 0; i < this.length; i++) {
			var children = this[i].children();
			for (var j = 0; j < children.length; j++) {
				result[result.length] = children[j];
			}
		}
		return result;
	});
	return list;
}

/**
 * 用门面包一下 engine，engine.parseHtml 的结果和全局的 parseHtml 保持一致，
 * 其余方法（比如 eval）照常转发
 */
var __comicEngine = engine;
engine = {
	/** 解析 HTML，返回文档对象 */
	parseHtml : function (html) {
		return __domDocument(__comicEngine.parseHtml(html));
	},
	/** 执行混淆过的脚本 */
	eval : function (scope, script) {
		return __comicEngine.eval(scope, script);
	}
};
