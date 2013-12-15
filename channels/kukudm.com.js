/**
 * 来源渠道描述
 */
var channel = {
	name : "kuku动漫(电信)",
	home : "http://kukudm.com/",
	desc : "http://kukudm.com/comiclist/346/\n346是漫画Id\n"+
	"http://kukudm.com/comiclist/346/4704/1.htm\n346是漫画Id\n4704是章节Id\n不支持腾讯漫画",
	index : 4
}

/**
 * 获得章节列表
 * 
 * @param comicId
 */
function getSections(comicId) {
	var content = httpclient.getRemotePage("http://kukudm.com/comiclist/" + comicId + "/", "gbk", {});
	var comicNameRegex = new RegExp("<td colspan='2'>([^<>]+)</td>");
	var comicName = "";
	var comicMatched = content.match(comicNameRegex);
	if(comicMatched != null){
		comicName = comicMatched[1];
	}
	var sectionRegex =  new RegExp("<dd><A href='/comiclist/"+comicId+"/\\d+/1.htm' target='_blank'>[^<>]+</A>","g");
	var matched = content.match(sectionRegex);
	var sections = [];
	if(matched != null && matched.length > 0){
		for ( var i = 0; i < matched.length; i++) {
			var sectionId = matched[i].match(new RegExp("/" + comicId + "/(\\d+)/"))[1];
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
	var content = httpclient.getRemotePage("http://kukudm.com/comiclist/" + comicId + "/" + sectionId + "/1.htm", "gbk", {});
	var pageContent = content.match(new RegExp("共(\\d+)页"))[1];
	var pages = [];
	for ( var i = 0; i < parseInt(pageContent); i++) {
		pages[i] = {
			pageNo : i + 1,
			pageUrl : "http://kukudm.com/"+(i + 1)+".jpg"
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
	var content = httpclient.getRemotePage("http://kukudm.com/comiclist/" + comicId + "/" + sectionId + "/"+pageNo+".htm", "gbk", {});
	var urlObj = content.match(new RegExp("document.write\\(\"<IMG srC='\"\\+(\\w+)\\+\"([^'\"]+)'>","i"));
	var serverVarName = urlObj[1];
	var imageUri = urlObj[2];
	if(serverVarName == "server"){
		var jscontent = httpclient.getRemotePage("http://kukudm.com/js2/js0.js", "gbk", {});
		var serverHost = jscontent.match(new RegExp("server='([^'\"]+)';"))[1];
		httpclient.saveRemoteImage(channel.name,comicId,sectionId,serverHost+imageUri,pageNo+".jpg",{});
	} else{
		var jsUri = content.match(new RegExp("/js2/js\\d+.js","g"))[1];
		var jscontent = httpclient.getRemotePage("http://kukudm.com/"+jsUri, "gbk", {});
		var serverHost = jscontent.match(new RegExp(serverVarName + "='([^'\"]+)';"))[1];
		httpclient.saveRemoteImage(channel.name,comicId,sectionId,serverHost+imageUri,pageNo+".jpg",{});
	}
}