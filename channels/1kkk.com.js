/**
 * 来源渠道描述
 */
var channel = {
	name : "1kkk极速慢画",
	home : "http://www.1kkk.com/",
	desc : "http://www.1kkk.com/manhua432/\nmanhua432是漫画Id\n"+
	"http://www.1kkk.com/ch51-145964/\nhttp://www.1kkk.com/t148129/\n" +
	"ch51-145964,t148129是章节Id",
	index : 20
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
	var chaptUrlJs = content.match(new RegExp("eval\\(function\\(p.*,0,\\{\\}\\)\\)"));
	var chaptUrls = engine.eval({}, "eval(" + chaptUrlJs + ");");
	var url = "";
	if(chaptUrls.CHAPTERURL != null){
		url = chaptUrls.CHAPTERURL.get(sectionId.substring(1));
	}
	if (url.indexOf("ac.qq.com") != -1) {
		var qqcontent = httpclient.getRemotePage(url, "utf-8", {});
		var dataJs = qqcontent.match(new RegExp("var DATA = '(.+)',"));
		var images = getImageUrls(dataJs[1]);
		for ( var i = 0; i < images.length; i++) {
			pages.push({
					pageNo : i + 1,
					filename : (i + 1) + ".jpg",
					pageUrl : images[i].url
			});
		}
	} else if (url.indexOf("qzone.qq.com") != -1) {
		var ids = url.match(new RegExp("http://user.qzone.qq.com/(\\d+)/blog/(\\d+)"));
		var blogUrl = "http://b11.qzone.qq.com/cgi-bin/blognew/blog_output_data?uin="+ids[1]+"&blogid="+ids[2]+"&styledm=ctc.qzonestyle.gtimg.cn&imgdm=ctc.qzs.qq.com&bdm=b.qzone.qq.com&mode=2&numperpage=15&timestamp="+(new Date().getTime()/1000)+"&dprefix=&blogseed=0.06768302366351653&inCharset=utf-8&outCharset=utf-8&ref=qzone&entertime="+new Date().getTime();
		var qqcontent = httpclient.getRemotePage(blogUrl, "utf-8", {
			"Referer" : url
		});
		var imageContent = qqcontent.substring(qqcontent.indexOf("id=\"blogDetailDiv\""),qqcontent.indexOf("id=\"paperPicArea1\""));
		var imageDivs = qqcontent.match(new RegExp("<br/><img[^<>]+src=\"[^\"]+\"[^<>]+/><br/>","g"));
		for ( var i = 0; i < imageDivs.length; i++) {
			var url = imageDivs[i].match(new RegExp("<br/><img[^<>]+src=\"([^\"]+)\"[^<>]+/><br/>"))[1];
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

function getImageUrls(DATA){
	!function () {
	    eval(function (p, a, c, k, e, r) {
	        e = function (c) {
	            return (c < a ? '' : e(parseInt(c / a))) + ((c = c % a) > 35 ? String.fromCharCode(c + 29)  : c.toString(36))
	        };
	        if (!''.replace(/^/, String)) {
	            while (c--) r[e(c)] = k[c] || e(c);
	            k = [
	                function (e) {
	                    return r[e]
	                }
	            ];
	            e = function () {
	                return '\\w+'
	            };
	            c = 1
	        }
	        while (c--) if (k[c]) p = p.replace(new RegExp('\\b' + e(c) + '\\b', 'g'), k[c]);
	        return p
	    }('p s(){i="C+/=";H.q=p(c){o a="",b,d,h,f,g,e=0;z(c=c.J(/[^A-L-M-9\\+\\/\\=]/g,"");e<c.r;)b=i.l(c.k(e++)),d=i.l(c.k(e++)),f=i.l(c.k(e++)),g=i.l(c.k(e++)),b=b<<2|d>>4,d=(d&t)<<4|f>>2,h=(f&3)<<6|g,a+=5.7(b),w!=f&&(a+=5.7(d)),w!=g&&(a+=5.7(h));n a=y(a)};y=p(c){z(o a="",b=0,d=D=8=0;b<c.r;)d=c.j(b),E>d?(a+=5.7(d),b++):F<d&&G>d?(8=c.j(b+1),a+=5.7((d&I)<<6|8&m),b+=2):(8=c.j(b+1),x=c.j(b+2),a+=5.7((d&t)<<K|(8&m)<<6|x&m),b+=3);n a}}o B=v s;u=(v N("n "+B.q(u.O(1))))();', 51, 51, '|||||String||fromCharCode|c2||||||||||_keyStr|charCodeAt|charAt|indexOf|63|return|var|function|decode|length|Base|15|DATA|new|64|c3|_utf8_decode|for|||ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789|c1|128|191|224|this|31|replace|12|Za|z0|Function|substring'.split('|'), 0, {
	    }))
	}();
	return DATA.picture;
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
			httpclient.getRemotePage("http://www.1kkk.com/userinfo.ashx?d="+encodeURIComponent(new Date()+""), "utf-8",{
				"Referer" :"http://www.1kkk.com/" + sectionId + "/"
			});
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