/**
 * 来源渠道描述
 */
var channel = {
	name : "dm5动漫屋",
	home : "http://www.dm5.com/",
	desc : "http://www.dm5.com/manhua-haizeiwang-onepiece/\nmanhua-haizeiwang-onepiece是漫画Id\n"+
	"http://www.dm5.com/m148306/\nhttp://www.dm5.com/t148760/\n" +
	"m148306,t148760是章节Id",
	index : 40
}

/**
 * 获得章节列表
 * 
 * @param comicId
 */
function getSections(comicId) {
	var content = httpclient.getRemotePage("http://www.dm5.com/" + comicId + "/", "utf-8", {});
	var comicNameRegex = new RegExp("var DM5_COMIC_MNAME=\"([^\"]+)\";");
	var comicName = "";
	var comicMatched = content.match(comicNameRegex);
	if(comicMatched != null){
		comicName = comicMatched[1];
	}
	var sectionContent = content.substring(content.indexOf("id=\"cbc_1\""));
	var sectionRegex =  new RegExp("<li style=\"[^\"]+\"><a class=\"tg\"[^<>]+href=\"/[^\"]+/\"[^<>]+>[^<>]+</a>","g");
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
		sections :  sections.reverse()
	};
}

/**
 * 获得章节的页数
 * 
 * @param SectionId
 */
function getPages(comicId, sectionId) {
	var pages = [];
	var content = httpclient.getRemotePage("http://www.dm5.com/" + sectionId + "/", "utf-8", {});
	var qqzone = content.match(new RegExp(">location.href=\"([^\"<>]+)\"<"));
	if (content.indexOf("qq.com") != -1 && qqzone != null && qqzone.length > 0) {
		var ids = qqzone[1].match(new RegExp("http://user.qzone.qq.com/(\\d+)/blog/(\\d+)"));
		var blogUrl = "http://b11.qzone.qq.com/cgi-bin/blognew/blog_output_data?uin="+ids[1]+"&blogid="+ids[2]+"&styledm=ctc.qzonestyle.gtimg.cn&imgdm=ctc.qzs.qq.com&bdm=b.qzone.qq.com&mode=2&numperpage=15&timestamp="+(new Date().getTime()/1000)+"&dprefix=&blogseed=0.06768302366351653&inCharset=utf-8&outCharset=utf-8&ref=qzone&entertime="+new Date().getTime();
		var qqcontent = httpclient.getRemotePage(blogUrl, "utf-8", {});
		var imageContent = qqcontent.substring(qqcontent.indexOf("id=\"blogDetailDiv\""),qqcontent.indexOf("id=\"paperPicArea1\""));
		var imageDivs = qqcontent.match(new RegExp("<div><img[^<>]+src=\"[^\"]+\"[^<>]+/></div>","g"));
		for ( var i = 0; i < imageDivs.length; i++) {
			var url = imageDivs[i].match(new RegExp("<div><img[^<>]+src=\"([^\"]+)\"[^<>]+/></div>"))[1];
			if(url != null) {
				pages.push({
						pageNo : i + 1,
						filename : (i + 1) + ".jpg",
						pageUrl : url
				});
			}
		}
	} else {
		var totalCount = content.match(new RegExp("总<span>(\\d+)</span>页"))[1];
		for ( var i = 0; i < totalCount; i++) {
			pages[i] = {
				pageNo : i + 1,
				pageUrl : "http://www.dm5.com/" + (i + 1) + ".jpg" // 后面会重新获取，这里只需要http开头和文件名
			}
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
	if(imageUrl.indexOf("dm5") != -1){
		var content = httpclient.getRemotePage("http://www.dm5.com/" + sectionId + "/", "utf-8", {});
		var midMatched = content.match(new RegExp("var DM5_MID=[^<>]+var DM5_IMAGE_COUNT=\\d+;"));
		var m5kkeyMatched = content.match(new RegExp("eval\\(function\\(.+0,\\{\\}\\)\\)"));
		var dm5_key = "";
		if(m5kkeyMatched != null){
			var result = engine.eval({
				$: function(){
					return {
						val:function(v){
							dm5_key = v;
						}
					};
				}
			},m5kkeyMatched);
		}
		if(midMatched != null){
			httpclient.getRemotePage("http://www.dm5.com/userinfo.ashx?d="+encodeURIComponent(new Date()+""), "utf-8",{
				"Referer" :"http://www.dm5.com/" + sectionId + "/"
			});
			var cidObj = engine.eval({}, midMatched);
			var urlContent = httpclient.getRemotePage("http://www.dm5.com/" + sectionId + "/chapterimagefun.ashx?cid=" + cidObj.DM5_CID + "&page="
					+ pageNo + "&key=" + dm5_key + "&language=1", "utf-8", {
				"Referer" :"http://www.dm5.com/" + sectionId + "/"
			});
			var result = engine.eval({}, "eval(" + urlContent + ");");
			httpclient.saveRemoteImage(channel.name,comicId,sectionId,result.d[0],pageNo+".jpg",{
				"Referer" :"http://www.dm5.com/" + sectionId + "/"
			});
		}
	} else {
		httpclient.saveRemoteImage(channel.name,comicId,sectionId,imageUrl,pageNo+".jpg",{});
	}
	
}