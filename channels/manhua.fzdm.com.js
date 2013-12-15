/**
 * 来源渠道描述
 */
var channel = {
	name : "风之动漫",
	home : "http://manhua.fzdm.com/",
	desc : "http://manhua.fzdm.com/1/\n1是漫画Id\n"+
	"http://manhua.fzdm.com/1/658/\n1是漫画Id\n658是章节Id",
	index : 6
}

/**
 * 获得章节列表
 * 
 * @param comicId
 */
function getSections(comicId) {
	var content = httpclient.getRemotePage("http://manhua.fzdm.com/" + comicId + "/", "utf-8", {});
	var comicNameRegex = new RegExp("<h2>([^\\s]+)");
	var comicName = "";
	var comicMatched = content.match(comicNameRegex);
	if(comicMatched != null){
		comicName = comicMatched[1];
	}
	var sectionRegex =  new RegExp("<li><a href=\"\\d+/\"[^<>]+>[^<>]+</a></li>","g");
	var matched = content.match(sectionRegex);
	var sections = [];
	if(matched != null && matched.length > 0){
		for ( var i = 0; i < matched.length; i++) {
			var sectionId = matched[i].match(new RegExp("\"(\\d+)/"))[1];
			var sectionName = matched[i].match(/>([^<>]+)</)[1];
			sections.push({
				sectionId : sectionId,
				name : sectionName
			});
		}
	}
	return {
		comicName : comicName,
		sections : sections.reverse()
	};
}

/**
 * 获得章节的页数
 * 
 * @param SectionId
 */
function getPages(comicId, sectionId) {
	var pageCount = 0;
	var hasNext = true;
	var maxCount = 50;
	var startPageIndex = 0;
	for ( var i = 0; i < maxCount; i++) {
		var content = httpclient.getRemotePage("http://manhua.fzdm.com/" + comicId + "/" + sectionId + "/index_"+startPageIndex+".html", "utf-8", {});
		var pageUrls = content.match(new RegExp("href=\"index_\\d+\\.html\"","g"));
		var pageIndex = pageUrls[pageUrls.length - 1].match(new RegExp("index_(\\d+).html"))[1];
		startPageIndex = pageIndex;
		if(content.indexOf("最后一页了") !=-1){
			pageCount = parseInt(pageIndex) + 1;
			break;
		}
	}
	var pages = [];
	for ( var i = 0; i < pageCount; i++) {
		pages[i] = {
			pageNo : i + 1,
			pageUrl : "http://manhua.fzdm.com/" + (i + 1) +".jpg"
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
	var content = httpclient.getRemotePage("http://manhua.fzdm.com/" + comicId + "/" + sectionId + "/index_"+(parseInt(pageNo)-1)+".html", "utf-8", {});
	var pageUrlMatched = content.match(new RegExp("<img src=\"([^\"]+)\" id=\"mhpic\""));
	httpclient.saveRemoteImage(channel.name,comicId,sectionId,pageUrlMatched[1],pageNo + ".jpg",{});
}