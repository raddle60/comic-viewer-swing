# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

漫画查看器 — a Swing desktop comic reader. The Java app is a thin viewer; **all site-specific scraping lives in JavaScript plugins** under `channels/*.js` that are executed by the Rhino JS engine.

## Build & Run

Maven is on PATH (`mvn`, Apache Maven 3.3.9):

```bash
mvn -o package          # builds target/comic-viewer-swing-1.0.0-jar-with-dependencies.jar
mvn -o test             # JUnit 4
mvn -o test -Dtest=RhinoTest   # single test
```

Dependencies are already in the local repo (`d:/maven/repository` per `~/.m2/settings.xml`), so `-o` (offline) works.

The plugin versions are pinned in `pom.xml` (compiler 3.11.0, resources 3.3.1, surefire 3.2.5, jar 3.3.0, assembly 2.2.1) so the build does not depend on the Maven version's defaults — Maven 3.2.5 and newer all work. Don't let a newer Maven bump these to versions that require Maven 3.6.3+; the locally installed one is 3.3.9.

Run the app **from the repo root** — the `channels/` directory is resolved as a relative path (`new File("channels")`), so the jar must be launched with the repo root as the working directory:

```bash
java -jar target/comic-viewer-swing-1.0.0-jar-with-dependencies.jar
```

- JDK 21 is installed; `pom.xml` compiles with `source/target 1.8`.
- `defaultGoal` is `package`; the assembly plugin builds `jar-with-dependencies` with main class `com.raddle.comic.swing.ComicViewer`.
- UI strings and code comments are in Chinese — match that style.
- Source files are CRLF; keep it that way (a stray LF-only rewrite makes the whole file show up in `git diff`).

## Architecture

```
com.raddle.comic
├── engine/            plugin host + model
│   ├── ComicPluginEngine   loads a .js file into a Rhino scope, invokes its functions
│   ├── HttpHelper          static HTTP fetch/save, exposed to JS as `httpclient`
│   ├── ChannelInfo         channel metadata (name/home/desc/index/scriptFile)
│   ├── ComicInfo           comicName + List<SectionInfo>
│   ├── SectionInfo         sectionId + name
│   ├── PageInfo            pageNo + filename + pageUrl
│   └── dom/               jsoup wrappers used by the plugin scripts
│       ├── DomDocument         parseHtml(html) → document with select()/query()
│       └── DomNode            node wrapper: text(), attr(), select(), hasClass() ...
├── swing/
│   ├── ComicViewer        main window: menus, keybindings, paging, fullscreen, scroll
│   ├── ContinuousScrollModel  continuous-scroll model: scroll position + page cache + preloading
│   ├── OpenComicDialog    pick channel → comic id → section → page
│   └── RecentViewMenuItem recent-list menu entry (shows "有更新" in red)
├── RecentViewHelper    ~/.comic-view/recent-view.xml (last 30 reads)
├── RecentViewInfo     one entry of the recent-view list
├── ViewConfigHelper    ~/.comic-view/view-config.xml (continuous-scroll, seamless, continuous-download, fit-height, zoom flags)
└── LogWrapper          slf4j wrapper with `{}` placeholder substitution
```

`ComicViewer` holds all viewer state as fields (current channel/comic/section/page) and drives `ComicPluginEngine`; the dialog is a separate JDialog that talks to its own short-lived engine instance.

### Continuous scroll (连续显示)

`ContinuousScrollModel` owns the scroll position and the page cache; `ComicViewer` only draws what it hands back.

- The scroll position is **page number + offset within that page**, never an offset into a stitched image. That is what makes it stable: rebuilding the image, a page finishing its download, or switching chapters cannot move the content.
- Pages are drawn one by one (`getVisiblePages`), not stitched into one tall bitmap. A page that is not loaded yet is simply not drawn and the model keeps the position, so it drops in when it arrives without a jump.
- `preload(viewportHeight)` downloads pages **in order** (forward pages first, then backward ones). Loading them in parallel let them finish out of order, and the first gap in the middle hid every page after it — that showed up as a blank band under the current page.
- `getAdjacentSection` finds the neighbouring chapter. The chapter list is **newest-first** (site order), so the next chapter is the one *before* it in the list. The viewer takes the list from `OpenComicDialog.getSections()` so both use the same order. The newest chapter is therefore the **first** element of the list — `OpenComicDialog` (default chapter, "最后章节" label) and the startup update check all use `sections.get(0)`.
- Switching chapters only happens after the new pages are fetched successfully; on failure the viewer stays put and can retry.

### Plugin contract (`channels/*.js`)

Each file defines a global `channel` object and four functions. `ComicPluginEngine.init()` evaluates the file in a fresh Rhino scope with three globals injected:

| Global | What it is |
|---|---|
| `httpclient` | `HttpHelper` — `getRemotePage(url, charset, headers)`, `postRemotePage(url, charset, headers, body)`, `saveRemoteImage(channel, comicId, sectionId, imageUrl, cacheFileName, headers)`, `saveRemotePage(url, headers, file)`。这几个方法都另有一个末尾多带 `soTimeoutSec` 的重载：传数字表示本次请求的读取超时秒数，不传（`null` / `undefined`）或小于等于 0 时用默认超时 5 秒。`postRemotePage` 的请求体是字符串，`Content-Type` 固定是 `application/json` |
| `engine` | the `ComicPluginEngine` itself — used as `engine.eval(scope, script)` to run obfuscated/packed JS snippets scraped from a site and read the resulting object; `engine.parseHtml(html)` returns a DOM document |
| `log` | slf4j logger named `ChannelJs` |
| `parseHtml` | global function injected by `src/main/resources/dom.js` — `parseHtml(html)` parses HTML and returns a DOM document (same as `engine.parseHtml`) |

Functions the engine calls:

- `getSections(comicId)` → `{ comicName: String, sections: [{sectionId, name}] }`
- `getPages(comicId, sectionId)` → array of `{ pageNo, pageUrl, filename? }`
- `loadRemoteImage(comicId, sectionId, pageNo, imageUrl)` → downloads the image into the cache
- `channel` → `{ name, home, desc, index }`; `index` orders the channel dropdown

`getPages` may return placeholder `pageUrl`s (e.g. `http://site/1.jpg`) that `loadRemoteImage` later resolves to the real URL — the real download happens in `loadRemoteImage`, not `getPages`.

顶漫画（`channels/dingmanhua.com.js`）的章节列表页面上只列最近的一小部分（26 个），完整的章节列表要用它自己的 ajax 接口取：`POST https://www.dingmanhua.com/comic/{comicId}`，请求体 `{}`，返回 `{"code":0,"data":{"chapters":[{"id":…,"chapterName":…}]}}`。这个接口对请求头敏感，**必须带 `referer`**，否则返回的 json 是坏的。

### DOM 查询（jsoup）

`parseHtml(html)`（全局函数，等价于 `engine.parseHtml(html)`）用 jsoup 解析 HTML，返回一个文档对象，可以像 jQuery 一样按 CSS 选择器查节点：

```js
var doc = parseHtml(content);
var links = doc.select(".cbc-list li a");   // 返回 JS 数组
var title = doc.select("h1.title").text();  // 集合的 text() 是拼接
var href = links.attr("href");              // 取第一个节点的属性
var first = links.first();                  // 第一个节点，没有时 null
```

节点上的方法：`text()`、`ownText()`、`html()`、`outerHtml()`、`tag()`、`id()`、`className()`、`hasClass(cls)`、`attr(name)`、`setAttr(name, value)`、`absUrl(name)`、`select(css)`、`query(css)`、`children()`、`parent()`、`next()`、`element()`；文档额外有 `title()`、`location()`。

集合（数组）上的快捷方法：`each(fn)`（fn 收到下标和节点）、`first()`、`last()`、`text()`、`attr(name)`、`map(fn)`、`children()`。

实现分两层：Java 侧 `com.raddle.comic.engine.dom.DomDocument` / `DomNode` 包装 jsoup；`src/main/resources/dom.js` 在渠道脚本之前注入，负责把 Java 返回值转成真正的 JS 字符串和数组。**这一步不能省**：Rhino 1.7R4 里 Java 方法返回的 String 会被包成 `NativeJavaObject`（`typeof` 是 `"object"`、`.length` 不可用），Rhino 拼接出来的 `ConsString` 也不是 `java.lang.String`，直接强转会 `ClassCastException`。引擎侧取 JS 字符串统一走 `ComicPluginEngine.asString()`。

jsoup 不执行 JS，页面内容如果是前端渲染出来的就抓不到。

### Image caching

`httpclient.saveRemoteImage` writes to
`~/.comic-view/cache/img/<channelName>/<comicId>/<sectionId>/<filename>`.

`ComicViewer.loadImage` checks that path first, then calls `picEngine.loadRemoteImage(...)` to populate it. A `pageUrl` that looks like a local path (no `:` or a `X:` drive prefix) is read straight off disk with `ImageIO` instead.

Images are read with `ImageIO.read()`. The JDK only supports jpeg/png/gif/bmp/wbmp/tiff, so the project pulls in `com.twelvemonkeys.imageio:imageio-webp` — some sites (e.g. dingmanhua) only serve `.webp`, and without that plugin the reader returns null and the viewer shows "加载图片失败".

Logs go to stdout and `~/.comic-view/logs/comic-view.log` (logback, `src/main/resources/logback.xml`).

### Rhino threading notes

`Context` is thread-local. `ComicPluginEngine.close()` calls `Context.exit()` — always close the engine in a `finally` block, and note that `init()` throws `IllegalStateException` if called twice on the same engine. `loadRemoteImage` may be called from a background thread and does `Context.enter()` itself.

The startup update check spawns a thread that re-reads each recent comic's last section id so the "最近打开" menu can flag `有更新`.

## Viewer controls

Arrows/space/Enter scroll (auto page-advance at the bottom), ←/→/PageUp/PageDown page, Home/End jump to first/last, Ctrl+arrows pan, Ctrl+Enter fullscreen, Esc exit fullscreen, Ctrl+G goto page, F5 refresh, M hide menu bar.

The mouse wheel scrolls 150px per notch and is animated (the target accumulates, a 16ms timer eases towards it) instead of jumping. Dragging cancels that animation.

View menu (查看):

| Item | What it does |
|---|---|
| 连续显示 | continuous scroll — pages are drawn one below the next, scrolling across page boundaries |
| 无缝显示 | remove the gap between pages (continuous mode only) |
| 连续下载 | after a page is downloaded, keep going until the chapter is done or it fails |
| 缩放 | zoom 0–150% in 25% steps, 150% at the top; combines with "适合高度" |
| 适合高度 | scale each page to the pane height |

Scrolling to the end of a chapter asks whether to continue to the next one; if the next chapter cannot be fetched the viewer stays where it is and can be retried.
