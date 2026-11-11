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
 * @param comicId
 */
function getSections(comicId) {
	var content = httpclient.getRemotePage("https://www.dingmanhua.com/comic/" + comicId + ".html", "utf-8", {});
	var doc = parseHtml(content)
	var comicNameRegex = new RegExp("<h1 class=\"manga-title\" id=\"mangaTitle\" style=\"white-space:pre-line;\" itemprop=\"name\">([^<>]+)</h1>");
	var comicName = "";
	var comicMatched = content.match(comicNameRegex);
	if(comicMatched != null){
		comicName = comicMatched[1];
	}
	var section_A_list = doc.select("a.pseudo.chaper-btn");
	var sections = [];
	if(section_A_list != null && section_A_list.length > 0){
		for ( var i = 0; i < section_A_list.length; i++) {
			var sectionId = section_A_list[i].attr("href").match(new RegExp("/" + comicId + "-(\\d+)"))[1];
			sections.push({
				sectionId : sectionId,
				name : section_A_list[i].text()
			});
		}
	}
	return {
		comicName : comicName,
		sections : sections
	};
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
	httpclient.saveRemoteImage(channel.name, comicId, sectionId, imageUrl, pageNo + ".webp", {});
}