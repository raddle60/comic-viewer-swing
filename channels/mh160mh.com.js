/**
 * 来源渠道描述
 *
 * 漫画160（m.mh160mh.com），用的是晴天 cms（qingtiancms）。
 *
 * 漫画地址：https://m.mh160mh.com/kanmanhua/{漫画id}/
 * 章节地址：https://m.mh160mh.com/kanmanhua/{漫画id}/{章节id}.html
 * 漫画id就是 kanmanhua 后面的那一段，拼音（mohuangdaguanjia）或者数字（94）都行
 * 章节id就是章节地址里的那个数字
 */
var channel = {
	name : "漫画160",
	home : "https://m.mh160mh.com/",
	desc : "https://m.mh160mh.com/kanmanhua/mohuangdaguanjia/\n" +
	"mohuangdaguanjia 是漫画id（kanmanhua 后面那一段，拼音或者数字都行）\n" +
	"https://m.mh160mh.com/kanmanhua/mohuangdaguanjia/16120934.html\n" +
	"16120934 是章节id（章节地址里的数字）",
	index : 60
}

/** 站点根目录 */
var SITE = "https://m.mh160mh.com";
/** 漫画、章节都在这个目录下面 */
var COMIC_DIR = "/kanmanhua/";
/**
 * 图片服务器，站点自己的脚本也是从这几个里面随机挑一个用的，
 * 哪个抽风了就换下一个
 */
var IMAGE_HOSTS = [ "xwdf.tgmhfc.uk", "mhreswhm.tgmhfc.uk", "qwe123.tgmhfc.uk", "resmhpic.tgmhfc.uk", "reszxc.tgmhfc.uk" ];
/** 章节页面里存图片列表的脚本变量，一个章节可能分成好几组 */
var MURL_VARS = [ "qTcms_S_m_murl_e", "qTcms_S_m_murl_e2", "qTcms_S_m_murl_e3" ];
/** 图片列表里每一项之间的分隔符 */
var MURL_SEPARATOR = "$qingtiandy$";

/**
 * 漫画页面地址
 */
function comicUrl(comicId) {
	return SITE + COMIC_DIR + comicId + "/";
}

/**
 * 章节页面地址
 */
function sectionUrl(comicId, sectionId) {
	return SITE + COMIC_DIR + comicId + "/" + sectionId + ".html";
}

/**
 * 获得章节列表
 *
 * 章节列表全在漫画页面的 ul#chapterList_ul_1 里，
 * 页面上的“查看更多”只是把这段列表展开，章节本身是全的。
 * 章节是新的排在前面，和站点顺序一致。
 *
 * @param comicId 漫画id
 */
function getSections(comicId) {
	var comicName = "";
	var sections = [];
	var content = null;
	try {
		content = String(httpclient.getRemotePage(comicUrl(comicId), "utf-8", {
			"referer" : SITE + "/"
		}, 10));
	} catch (e) {
		log.info("打开漫画页面失败 comicId[" + comicId + "]", e);
	}
	if (content != null) {
		var doc = parseHtml(content);
		sections = parseSections(doc);
		comicName = parseComicName(doc, content);
	}
	return {
		comicName : comicName,
		sections : sections
	};
}

/**
 * 从漫画页面里取出章节列表
 */
function parseSections(doc) {
	var sections = [];
	var links = doc.select("#chapterList_ul_1 li a");
	if (links.length == 0) {
		links = doc.select("div.chapters ul li a");
	}
	for (var i = 0; i < links.length; i++) {
		var href = links[i].attr("href");
		if (href == null) {
			continue;
		}
		// 地址形如 /kanmanhua/mohuangdaguanjia/16120934.html
		var matched = href.match(/(\d+)\.html/);
		if (matched == null) {
			continue;
		}
		sections.push({
			sectionId : matched[1],
			name : links[i].text()
		});
	}
	return sections;
}

/**
 * 取漫画名称，页面上是 p.txtItme.h1，取不到就用脚本里的标题变量
 */
function parseComicName(doc, content) {
	var name = "";
	try {
		var node = doc.query("p.txtItme.h1");
		if (node != null) {
			name = node.text();
		}
	} catch (e) {
		// 忽略，取不到名字不影响看漫画
	}
	if (!name) {
		var matched = content.match(/qingtiancms_Details=\{[^}]*title:"([^"]*)"/);
		if (matched != null) {
			name = matched[1];
		}
	}
	if (!name) {
		var title = doc.title();
		if (title != null) {
			name = title.split(",")[0];
		}
	}
	return name;
}

/**
 * 获得章节的页数
 *
 * 图片地址不在 img 标签里，而是在章节页面的脚本变量 qTcms_S_m_murl_e 里，
 * 这个变量是 base64 编码的，解开以后是一串用 $qingtiandy$ 分隔的相对路径，
 * 例如：
 * /mh160tuku/m/魔皇大管家_23400/第934话都是我的错_76320916/0001.jpg
 * 前面要拼上图片服务器，站点脚本里是从 IMAGE_HOSTS 里面随机挑一个的。
 *
 * @param comicId 漫画id
 * @param sectionId 章节id
 */
function getPages(comicId, sectionId) {
	var content = null;
	try {
		content = String(httpclient.getRemotePage(sectionUrl(comicId, sectionId), "utf-8", {
			"referer" : comicUrl(comicId)
		}, 10));
	} catch (e) {
		log.info("打开章节页面失败 comicId[" + comicId + "] sectionId[" + sectionId + "]", e);
		return [];
	}
	var paths = parseImagePaths(content);
	var pages = [];
	for (var i = 0; i < paths.length; i++) {
		pages.push({
			pageNo : i + 1,
			pageUrl : toImageUrl(paths[i]),
			filename : (i + 1) + ".webp"
		});
	}
	return pages;
}

/**
 * 从章节页面里取出图片路径列表
 */
function parseImagePaths(content) {
	var paths = [];
	for (var i = 0; i < MURL_VARS.length; i++) {
		var matched = content.match(new RegExp("var\\s+" + MURL_VARS[i] + "\\s*=\\s*\"([^\"]*)\""));
		if (matched == null) {
			continue;
		}
		var decoded = base64Decode(matched[1]);
		if (!decoded) {
			continue;
		}
		var items = decoded.split(MURL_SEPARATOR);
		for (var j = 0; j < items.length; j++) {
			var path = String(items[j]).trim();
			if (path != "") {
				paths.push(path);
			}
		}
	}
	return paths;
}

/**
 * base64 解码，解出来是 utf-8 的字符串（里面含中文）
 */
function base64Decode(data) {
	if (data == null || data == "") {
		return "";
	}
	try {
		var bytes = java.util.Base64.getDecoder().decode(String(data));
		return String(new java.lang.String(bytes, "UTF-8"));
	} catch (e) {
		log.info("base64 解码失败", e);
		return "";
	}
}

/**
 * 相对路径拼成图片地址，中文要转成 %xx 编码，不然 HttpClient 建不出请求
 */
function toImageUrl(path) {
	if (path.indexOf("http://") == 0 || path.indexOf("https://") == 0) {
		return path;
	}
	return "https://" + IMAGE_HOSTS[0] + encodeURI(path);
}

/**
 * 加载图片
 *
 * 图片服务器不带 referer 会返回 404，所以要带上章节页面的地址；
 * 一个服务器不行就换下一个。
 *
 * @param comicId 漫画id
 * @param sectionId 章节id
 * @param pageNo 页码
 * @param imageUrl 图片地址
 */
function loadRemoteImage(comicId, sectionId, pageNo, imageUrl) {
	var headers = {
		"referer" : sectionUrl(comicId, sectionId)
	};
	var path = imagePath(imageUrl);
	// 全是404才说明这一页是真的没有了，网络问题不算
	var allNotFound = true;
	for (var i = 0; i < IMAGE_HOSTS.length; i++) {
		var url = "https://" + IMAGE_HOSTS[i] + path;
		try {
			httpclient.saveRemoteImage(channel.name, comicId, sectionId, url, pageNo + ".webp", headers, 10);
			return;
		} catch (e) {
			allNotFound = allNotFound && httpclient.isNotFound(e);
			log.info("下载图片失败，换一个图片服务器重试 comicId[" + comicId + "] sectionId[" + sectionId + "] pageNo[" + pageNo
					+ "] host[" + IMAGE_HOSTS[i] + "] url[" + url + "] " + e);
		}
	}
	if (allNotFound) {
		// 所有图片服务器都说不存在，写一张not found的图存到本地，
		// 下次就不用再一个一个服务器去试了
		try {
			httpclient.writeNotFoundImage(channel.name, comicId, sectionId, imageUrl, pageNo + ".webp");
			log.info("这一页不存在，写了not found图片 comicId[" + comicId + "] sectionId[" + sectionId + "] pageNo[" + pageNo + "]");
			return;
		} catch (e) {
			log.info("写not found图片失败 comicId[" + comicId + "] sectionId[" + sectionId + "] pageNo[" + pageNo + "] " + e);
		}
	}
	log.info("所有图片服务器都没下载成功 comicId[" + comicId + "] sectionId[" + sectionId + "] pageNo[" + pageNo + "]");
}

/**
 * 从图片地址里取出路径部分
 */
function imagePath(url) {
	var protocol = url.indexOf("://");
	if (protocol < 0) {
		return url;
	}
	var start = url.indexOf("/", protocol + 3);
	if (start < 0) {
		return "/";
	}
	return url.substring(start);
}
