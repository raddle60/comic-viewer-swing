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
	var content = httpclient.getRemotePage("http://www.1kkk.com/" + "/" + sectionId + "/", "utf-8", {});
	return null;
}

/**
 * 加载图片
 * @param comicId
 * @param sectionId
 * @param imageUrl
 */
function loadRemoteImage(comicId, sectionId, pageNo, imageUrl) {
	httpclient.saveRemoteImage(channel.name,comicId,sectionId,imageUrl,{});
}