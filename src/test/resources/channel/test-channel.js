/**
 * 测试用的渠道脚本，不访问网络，只返回固定的数据。
 *
 * 章节数由漫画id决定，方便测试章节多的时候列表怎么滚。
 * 章节列表是倒序的，最新的一话在最前面。
 */
var channel = {
	name : "测试渠道",
	home : "http://test.local/",
	desc : "单元测试用",
	index : 999
}

function getSections(comicId) {
	var count = 2;
	var num = parseInt(comicId);
	if (!isNaN(num) && num > 0) {
		count = num;
	}
	var sections = [];
	for (var i = 0; i < count; i++) {
		sections.push({ sectionId : String(count - i), name : "第" + (count - i) + "章" });
	}
	return {
		comicName : "测试漫画",
		sections : sections
	};
}

/**
 * 每一章都是两页
 */
function getPages(comicId, sectionId) {
	return [
		{ pageNo : 1, pageUrl : "http://test.local/" + sectionId + "/1.jpg", filename : "1.jpg" },
		{ pageNo : 2, pageUrl : "http://test.local/" + sectionId + "/2.jpg", filename : "2.jpg" }
	];
}

/**
 * 测试里不真的下载图片
 */
function loadRemoteImage(comicId, sectionId, pageNo, imageUrl) {
}
