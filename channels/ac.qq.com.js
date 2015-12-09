/**
 * 来源渠道描述
 */
var channel = {
	name : "腾讯动漫",
	home : "http://ac.qq.com/",
	desc : "http://ac.qq.com/Comic/comicInfo/id/17114\n17114是漫画Id\n"+
	"http://ac.qq.com/ComicView/index/id/17114/cid/163\n" +
	"163是章节Id",
	index : 10
}

/**
 * 获得章节列表
 * 
 * @param comicId
 */
function getSections(comicId) {
	var content = httpclient.getRemotePage("http://ac.qq.com/Comic/comicInfo/id/" + comicId + "/", "utf-8", {
		"Referer" : "http://ac.qq.com/"
	});
	var comicName = "";
	if(content.indexOf("works-intro-title ui-left")!=-1){
		var comicNameStrong = content.substring(content.indexOf("works-intro-title ui-left"),content.indexOf("works-score clearfix"));
		var comicNameRegex = new RegExp("<strong>([^<>]+)</strong>");
		var comicMatched = comicNameStrong.match(comicNameRegex);
		if(comicMatched != null){
			comicName = comicMatched[1];
		}
	} else {
		var pageTitleRegex = new RegExp("<title>([^<>]+)</title>");
		var comicMatched = content.match(pageTitleRegex);
		if(comicMatched != null){
			comicName = comicMatched[1];
		}
	}
	var sections = [];
	if(content.indexOf("chapter-page-all works-chapter-list") != -1){
		var sectionContent = content.substring(content.indexOf("chapter-page-all works-chapter-list"));
		sectionContent = sectionContent.substring(0,sectionContent.indexOf("</ol>"));
		var sectionRegex =  new RegExp(" href=\"/ComicView/index/id/\\d+/cid/(\\d+)\">[^<>]+</a>","g");
		var matched = sectionContent.match(sectionRegex);
		if(matched != null && matched.length > 0){
			for ( var i = 0; i < matched.length; i++) {
				var sectionId = matched[i].match(new RegExp("href=\"/ComicView/index/id/\\d+/cid/(\\d+)\""))[1];
				var sectionName = matched[i].match(/\">([^<>]+)</)[1];
				sections.push({
					sectionId : sectionId,
					name : sectionName
				});
			}
		}
	} else if(content.indexOf("chapter-list-content") != -1){
		var sectionContent = content.substring(content.indexOf("chapter-list-content"));
		sectionContent = sectionContent.substring(0,sectionContent.indexOf("foot-wrap"));
		var sectionRegex =  new RegExp(" href=\"/ComicView/index/id/\\d+/cid/(\\d+)\"><span>[^<>]+</span>","g");
		var matched = sectionContent.match(sectionRegex);
		if(matched != null && matched.length > 0){
			for ( var i = 0; i < matched.length; i++) {
				var sectionId = matched[i].match(new RegExp("href=\"/ComicView/index/id/\\d+/cid/(\\d+)\""))[1];
				var sectionName = matched[i].match(/<span>([^<>]+)<\/span>/)[1];
				sections.push({
					sectionId : sectionId,
					name : sectionName
				});
			}
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
		var qqcontent = httpclient.getRemotePage("http://ac.qq.com/ComicView/index/id/"+comicId+"/cid/"+sectionId, "utf-8", {
			"Referer" : "http://ac.qq.com/Comic/comicInfo/id/"+comicId
		});
		var dataJs = qqcontent.match(new RegExp("var DATA\\s+= '(.+)',"));
		var images = getImageUrls(dataJs[1]);
		for ( var i = 0; i < images.length; i++) {
			pages.push({
					pageNo : i + 1,
					filename : (i + 1) + ".jpg",
					pageUrl : images[i].url
			});
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
	httpclient.saveRemoteImage(channel.name,comicId,sectionId,imageUrl,pageNo+".jpg",{});
}