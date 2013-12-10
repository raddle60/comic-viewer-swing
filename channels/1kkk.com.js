/**
 * 来源渠道描述
 */
var channel = {
	name : "1kkk极速慢画",
	home : "http://www.1kkk.com/",
	desc : "http://www.1kkk.com/manhua432/\nmanhua432是漫画Id\n"+
	"http://www.1kkk.com/ch51-145964/\nhttp://www.1kkk.com/t148129/\n" +
	"ch51-145964,t148129是章节Id",
	index : 2
}

/**
 * 获得章节列表
 * 
 * @param comicId
 */
function getSections(comicId) {
	var content = httpclient.getRemotePage("http://www.1kkk.com/" + comicId + "/", "utf-8", {});
	var comicNameRegex = new RegExp("var mname=\"([^\"]+)\";");
	var comicName = "";
	var comicMatched = content.match(comicNameRegex);
	if(comicMatched != null){
		comicName = comicMatched[1];
	}
	var sectionContent = content.substring(content.indexOf("全部章节列表"));
	var sectionRegex =  new RegExp("<li><a href=\"/[^\"]+/\" class=\"tg\">[^<>]+</a>","g");
	var matched = sectionContent.match(sectionRegex);
	var sections = [];
	if(matched != null && matched.length > 0){
		for ( var i = 0; i < matched.length; i++) {
			var sectionId = matched[i].match(new RegExp("href=\"/([^\"]+)/\""))[1];
			var sectionName = matched[i].match(/>([^<>]+)</)[1];
			sections.push({
				sectionId : sectionId,
				name : sectionName
			});
		}
	}
	return {
		comicName : comicName,
		sections :  sections
	};
}

/**
 * 获得章节的页数
 * 
 * @param SectionId
 */
function getPages(comicId, sectionId) {
	var content = httpclient.getRemotePage("http://www.1kkk.com/" + sectionId + "/", "utf-8", {});
	var totalCount = content.match(new RegExp("总<span>(\\d+)</span>页"))[1];
	var pages = [];
	for ( var i = 0; i < totalCount; i++) {
		pages[i] = {
			pageNo : i + 1,
			pageUrl : "http://www.1kkk.com/" + i + ".jpg" //后面会重新获取，这里只需要http开头和文件名
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
	var content = httpclient.getRemotePage("http://www.1kkk.com/" + sectionId + "/", "utf-8", {});
	var midMatched = content.match(new RegExp("var mid=[^<>]+imagecount;"));
	var m5kkeyMatched = content.match(new RegExp("id=\"dm5_key\" value=\"([^\"]*)\""));
	if(midMatched != null && m5kkeyMatched != null && m5kkeyMatched.length > 0){
		var cidObj = engine.eval({}, midMatched);
		var urlContent = httpclient.getRemotePage("http://www.1kkk.com/" + sectionId + "/chapterimagefun.ashx?cid=" + cidObj.cid + "&page="
				+ pageNo + "&key=" + m5kkeyMatched[1] + "&maxcount=10", "utf-8", {});
		log.info(urlContent.substring(5,urlContent.length()-1));
		var result = engine.eval({}, urlContent.substring(5,urlContent.length()));
		log.info(result);
		//httpclient.saveRemoteImage(channel.name,comicId,sectionId,imageUrl,null,{});
	}
	// getimage() 通过ajax获取
	//content.match(new RegExp("<a href=\"/manhua13672/\">\\s+<img src=\"http://mhfm2.tel.cdndm5.com/14/13672/13672_c.jpg\" />""))[1];
	
}