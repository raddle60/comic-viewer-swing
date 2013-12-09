/**
 * 来源渠道描述
 */
var channel = {
	name : "SF互动传媒网",
	home : "http://comic.sfacg.com/",
	desc : "http://comic.sfacg.com/HTML/OnePiece/730/\nOnePiece是漫画Id\n730是章节Id\n"+
	"http://comic.sfacg.com/HTML/YJDWB/SP/SP1/\nYJDWB是漫画Id\nSP/SP1是章节Id"
}

/**
 * 获得章节列表
 * 
 * @param comicId
 */
function getSections(comicId) {
	var content = httpclient.getRemotePage("http://comic.sfacg.com/HTML/" + comicId + "/", "utf-8", {});
	var sectionRegex =  new RegExp("<li><a href=\"/HTML/"+comicId+"/[^/]+/\" target=\"_blank\">(([^<>]+)|(<font color=red>([^<>]+)</font>))</a></li>","g");
	var matched = content.match(sectionRegex);
	var sections = [];
	if(matched != null && matched.length > 0){
		for ( var i = 0; i < matched.length; i++) {
			var sectionId = matched[i].match(new RegExp("/" + comicId + "/(\\w+)/"))[1];
			var sectionName = matched[i].match(/>([^<>]+)</)[1];
			sections.push({
				sectionId : sectionId,
				name : sectionName
			});
		}
	}
	var extSectionRegex =  new RegExp("<li><a href=\"/HTML/"+comicId+"/[^/]+/[^/]+/\" target=\"_blank\">(([^<>]+)|(<font color=red>([^<>]+)</font>))</a></li>","g");
	var extMatched = content.match(extSectionRegex);
	if(extMatched != null && extMatched.length > 0){
		for ( var i = 0; i < extMatched.length; i++) {
			var sectionId = extMatched[i].match(new RegExp("/" + comicId + "/(\\w+/\\w+)/"))[1];
			var sectionName = extMatched[i].match(/>([^<>]+)</)[1];
			sections.push({
				sectionId : sectionId,
				name : sectionName
			});
		}
	}
	return sections.reverse();
}

/**
 * 获得章节的页数
 * 
 * @param SectionId
 */
function getPages(comicId, sectionId) {
	var content = httpclient.getRemotePage("http://comic.sfacg.com/HTML/" + comicId + "/" + sectionId + "/", "utf-8", {});
	var jsUrl = content.match(/\/Utility\/\w+\/(\w+\/)?\w+.js/);
	if(jsUrl != null && jsUrl.length > 0){
		var contentjs = httpclient.getRemotePage("http://comic.sfacg.com/" +jsUrl[0] , "utf-8", {});
		var evalResult = engine.eval({}, contentjs);
		var pages = [];
		for ( var i = 0; i < evalResult.picAy.length; i++) {
			pages[i] = {
				pageNo : i + 1,
				pageUrl : evalResult.hosts[0] + evalResult.picAy[i]
			}
		}
		return pages;
	}
	return null;
}

/**
 * 加载图片
 * @param comicId
 * @param sectionId
 * @param imageUrl
 */
function loadRemoteImage(comicId, sectionId, imageUrl) {
	httpclient.saveRemoteImage(channel.name,comicId,sectionId,imageUrl,{});
}