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
	var pages = [];
	var content = httpclient.getRemotePage("http://www.1kkk.com/" + sectionId + "/", "utf-8", {});
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
				pageUrl : "http://www.1kkk.com/" + (i + 1) + ".jpg" // 后面会重新获取，这里只需要http开头和文件名
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
	if(imageUrl.indexOf("1kkk") != -1){
		var content = httpclient.getRemotePage("http://www.1kkk.com/" + sectionId + "/", "utf-8", {});
		var midMatched = content.match(new RegExp("var mid=[^<>]+imagecount;"));
		var m5kkeyMatched = content.match(new RegExp("eval\\(function\\(.+0,\\{\\}\\)\\)"));
		var dm5_key = "";
		var result = engine.eval({
			$: function(){
				return {
					val:function(v){
						dm5_key = v;
					}
				};
			}
		},m5kkeyMatched);
		if(midMatched != null && m5kkeyMatched != null && m5kkeyMatched.length > 0){
			httpclient.getRemotePage("http://www.1kkk.com/" + sectionId + "/wxh.js?pt=4&va=10&v=20131213150158&key=", "utf-8",{
				"Referer" :"http://www.1kkk.com/" + sectionId + "/"
			});
			httpclient.getRemotePage("http://www.1kkk.com/showstatus.ashx?d="+encodeURIComponent(new Date()+""), "utf-8",{
				"Referer" :"http://www.1kkk.com/" + sectionId + "/"
			});
			httpclient.getRemotePage("http://www.1kkk.com/userinfo.ashx?d="+encodeURIComponent(new Date()+""), "utf-8",{
				"Referer" :"http://www.1kkk.com/" + sectionId + "/"
			});
			httpclient.getRemotePage("http://www.1kkk.com/wxhfm.html?cid=160&v=20131213150158&a=10", "utf-8",{
				"Referer" :"http://www.1kkk.com/" + sectionId + "/"
			});
			httpclient.getRemotePage("http://www.1kkk.com/" + sectionId + "/wxh.js?pt=4&va=10&v=20131213150158&key=", "utf-8",{});
			var cidObj = engine.eval({}, midMatched);
			var urlContent = httpclient.getRemotePage("http://www.1kkk.com/" + sectionId + "/chapterimagefun.ashx?cid=" + cidObj.cid + "&page="
					+ pageNo + "&key=" + dm5_key + "&maxcount=10", "utf-8", {
				"Referer" :"http://www.1kkk.com/" + sectionId + "/"
			});
			var result = engine.eval({}, "eval(" + urlContent + ");");
			httpclient.saveRemoteImage(channel.name,comicId,sectionId,result.d[0],pageNo+".jpg",{
				"Referer" :"http://www.1kkk.com/" + sectionId + "/"
			});
		}
	} else {
		httpclient.saveRemoteImage(channel.name,comicId,sectionId,imageUrl,pageNo+".jpg",{});
	}
	
}