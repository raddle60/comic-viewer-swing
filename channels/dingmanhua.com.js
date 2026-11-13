/**
 * 来源渠道描述
 */
var channel = {
	name : "顶漫画",
	home : "https://www.dingmanhua.com/",
	desc : "https://www.dingmanhua.com/comic/10261.html\n10261是漫画Id\n"+
	"https://www.dingmanhua.com/chapter/10261-2374867.html\n10261是漫画Id\n2374867是章节Id\n不支持腾讯漫画",
	index : 50
}

/**
 * 获得章节列表
 *
 * 页面上只列出最近的一小部分章节（顶漫画一页只有 26 个章节），完整的章节列表
 * 要用它自己的 ajax 接口取：
 * POST https://www.dingmanhua.com/comic/{comicId}，请求体是 {}
 * 返回 {"code":0,"data":{"chapters":[{"id":2385665,"chapterName":"第542话",...}]}}
 * 接口的章节顺序是新的在前，和页面上的顺序一致。
 * 这个接口对请求头比较敏感，不带 referer 的话返回的 json 是坏的。
 *
 * @param comicId
 */
function getSections(comicId) {
	var pageContent = null;
	try {
		pageContent = String(httpclient.getRemotePage("https://www.dingmanhua.com/comic/" + comicId + ".html", "utf-8", {
			"referer" : "https://www.dingmanhua.com/"
		}, 30));
	} catch (e) {
		log.info("打开漫画页面失败 comicId[" + comicId + "]", e);
	}

	var sections = getSectionsFromApi(comicId);

	var comicName = "";
	if (pageContent != null) {
		var doc = parseHtml(pageContent);
		var comicTitleH1 = doc.query("#mangaTitle");
		if (comicTitleH1 != null) {
			comicName = comicTitleH1.text();
		}
	}
	return {
		comicName : comicName,
		sections : sections
	};
}

/**
 * 用 ajax 接口取全部章节
 *
 * @param comicId
 * @returns 章节数组，接口不通时返回空数组
 */
function getSectionsFromApi(comicId) {
	var sections = [];
	try {
		var headers = {
			"accept" : "application/json, text/plain, */*",
			"origin" : "https://www.dingmanhua.com",
			"referer" : "https://www.dingmanhua.com/comic/" + comicId + ".html"
		};
		var content = String(httpclient.postRemotePage("https://www.dingmanhua.com/comic/" + comicId, "utf-8", headers, "{}", 30));
		var result = JSON.parse(content);
		var chapters = result.data.chapters;
		for (var i = 0; i < chapters.length; i++) {
			sections.push({
				sectionId : "" + chapters[i].id,
				name : "" + chapters[i].chapterName
			});
		}
	} catch (e) {
		log.info("调用章节接口失败 comicId[" + comicId + "]", e);
		sections = [];
	}
	return sections;
}

/**
 * 获得章节的页数
 *
 * 图片地址是页面里的脚本动态拼出来的，脚本内容是：
 * var num =eval("103")
 * var pasd = "https://image1.dingmanhua.com/mh/xxx/yyy/"
 * 循环里用 pasd + i + '.webp' 拼出每一页的图片地址
 * 所以把这两个变量取出来，图片地址就能算出来了
 *
 * @param SectionId
 */
function getPages(comicId, sectionId) {
	var content = httpclient.getRemotePage("https://www.dingmanhua.com/chapter/" + comicId + "-" + sectionId + ".html", "utf-8", {});
	var doc = parseHtml(content);
	var script = "";
	doc.select("script").each(function (i, s) {
		script = script + s.html() + "\n";
	});
	// num 可能是 eval("103")，也可能是直接的数字
	var numMatch = script.match(/var\s+num\s*=\s*(?:eval\(\s*["'](\d+)["']\s*\)|(\d+))/);
	var pasdMatch = script.match(/var\s+pasd\s*=\s*["']([^"']+)["']/);
	if (numMatch == null || pasdMatch == null) {
		log.info("没有找到图片脚本 comicId[{}] sectionId[{}]", comicId, sectionId);
		return [];
	}
	var num = parseInt(numMatch[1] != null ? numMatch[1] : numMatch[2]);
	var pasd = pasdMatch[1];
	var pages = [];
	for (var i = 1; i <= num; i++) {
		pages.push({
			pageNo : i,
			pageUrl : pasd + i + ".webp",
			filename : i + ".webp"
		});
	}
	return pages;
}

/**
 * 加载图片
 * 图片地址在 getPages 里已经算出来了，直接下载缓存
 * @param comicId
 * @param sectionId
 * @param imageUrl
 */
function loadRemoteImage(comicId, sectionId, pageNo, imageUrl) {
	httpclient.saveRemoteImage(channel.name, comicId, sectionId, imageUrl, pageNo + ".webp", {}, 30);
}