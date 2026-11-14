/**
 * 测试用的渠道脚本，不访问网络，只返回固定的数据。
 */
var channel = {
	name : "测试渠道",
	home : "http://test.local/",
	desc : "单元测试用",
	index : 999
}

/**
 * 章节列表是倒序的，最新的一话在最前面
 */
function getSections(comicId) {
	return {
		comicName : "测试漫画",
		sections : [
			{ sectionId : "2", name : "第二章" },
			{ sectionId : "1", name : "第一章" }
		]
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
