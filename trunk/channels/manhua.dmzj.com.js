/**
 * 来源渠道描述
 */
var channel = {
	name : "动漫之家",
	home : "http://manhua.dmzj.com/",
	desc : "http://manhua.dmzj.com/mowangnaiba/\nmowangnaiba是漫画Id\n"+
	"http://manhua.dmzj.com/mowangnaiba/21483.shtml\nmowangnaiba是漫画Id\n21483是章节Id\n只支持腾讯漫画最后一章",
	index : 4
}

/**
 * 获得章节列表
 * 
 * @param comicId
 */
function getSections(comicId) {
	var content = httpclient.getRemotePage("http://manhua.dmzj.com/" + comicId + "/", "utf-8", {});
	var comicNameRegex = new RegExp("<h1>([^<>]+)</h1>");
	var comicName = "";
	var comicMatched = content.match(comicNameRegex);
	if(comicMatched != null){
		comicName = comicMatched[1];
	}
	var sectionRegex =  new RegExp("<li><a[^<>]+href=\"/"+comicId+"/\\d+.shtml\"[^<>]+>[^<>]+</a></li>","g");
	var matched = content.match(sectionRegex);
	var sections = [];
	if(matched != null && matched.length > 0){
		for ( var i = 0; i < matched.length; i++) {
			var sectionId = matched[i].match(new RegExp("/" + comicId + "/(\\d+).shtml"))[1];
			var sectionName = matched[i].match(/>([^<>]+)</)[1];
			sections.push({
				sectionId : sectionId,
				name : sectionName
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
 * @param SectionId
 */
function getPages(comicId, sectionId) {
	var content = httpclient.getRemotePage("http://manhua.dmzj.com/" + comicId + "/" + sectionId + ".shtml", "utf-8", {});
	var pageContent = content.match(new RegExp("eval\\(function\\(.+0,\\{\\}\\)\\)"));
	var pagesStr = engine.eval({},pageContent);
	eval("var pageArray = " + pagesStr.pages);
	var pages = [];
	for ( var i = 0; i < pageArray.length; i++) {
		pages[i] = {
			pageNo : i + 1,
			pageUrl : "http://imgfast.dmzj.com/"+pageArray[i]
		}
	}
	return pages;
}

/**
 * 加载图片
 * @param comicId
 * @param sectionId
 * @param imageUrl
 */
function loadRemoteImage(comicId, sectionId, pageNo, imageUrl) {
	if(imageUrl.indexOf("dmzj") != -1){
		httpclient.saveRemoteImage(channel.name,comicId,sectionId,imageUrl, null ,{});
	} else {
		httpclient.saveRemoteImage(channel.name,comicId,sectionId,imageUrl,pageNo+".jpg",{});
	}
}