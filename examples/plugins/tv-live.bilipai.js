var DEFAULT_SOURCE_URL = "";
var DEFAULT_LOGO_BASE_URL = "https://live.fanmingming.com/tv/";

window.BiliPaiPlugin = {
  id: "tv.live",
  title: "电视直播",
  version: "1.1.0",
  author: "BiliPai",
  description: "解析 M3U/M3U Plus/TXT/JSON 电视直播源，对齐 pure_live_TV 风格的 group-title/tvg-id/tvg-logo/EPG 字段，返回可播放频道和备用线路。",
  permissions: ["NETWORK", "PLUGIN_STORAGE", "EXTERNAL_MEDIA_PLAYBACK"],
  modules: [
    {
      id: "channels",
      title: "电视频道",
      description: "从用户填写的数据源加载频道。",
      functionName: "loadChannels",
      params: [
        {
          name: "dataSource",
          title: "数据源 URL",
          type: "text",
          defaultValue: DEFAULT_SOURCE_URL
        },
        {
          name: "category",
          title: "分类",
          type: "enum",
          defaultValue: "all",
          options: [
            { title: "全部", value: "all" },
            { title: "央视", value: "cctv" },
            { title: "卫视", value: "stv" },
            { title: "地方", value: "ltv" },
            { title: "港澳台", value: "hk" },
            { title: "体育", value: "sports" },
            { title: "影视", value: "movie" },
            { title: "纪录", value: "doc" },
            { title: "少儿", value: "kids" },
            { title: "音乐", value: "music" },
            { title: "新闻", value: "news" },
            { title: "海外", value: "overseas" }
          ]
        },
        {
          name: "logoBaseUrl",
          title: "图标基础 URL",
          type: "text",
          defaultValue: DEFAULT_LOGO_BASE_URL
        },
        {
          name: "iconLibraryUrl",
          title: "自定义图标库 JSON",
          type: "text",
          defaultValue: ""
        },
        {
          name: "iconProxyTemplate",
          title: "图标代理模板",
          type: "text",
          defaultValue: ""
        },
        {
          name: "useGeneratedLogoFallback",
          title: "按频道名猜测台标",
          type: "enum",
          defaultValue: "false",
          options: [
            { title: "否", value: "false" },
            { title: "是", value: "true" }
          ]
        },
        {
          name: "preferGroupCategory",
          title: "优先源分类(group-title)",
          type: "enum",
          defaultValue: "true",
          options: [
            { title: "是", value: "true" },
            { title: "否", value: "false" }
          ]
        }
      ]
    }
  ],
  loadChannels: loadChannels
};

async function loadChannels(params) {
  var sourceUrl = stringParam(params, "dataSource", DEFAULT_SOURCE_URL).trim();
  if (!sourceUrl) {
    throw new Error("请先填写 M3U/TXT/JSON 数据源 URL");
  }

  var response = BiliPai.http.get(sourceUrl);
  if (response.code < 200 || response.code >= 300) {
    throw new Error("数据源请求失败: HTTP " + response.code);
  }

  var content = response.body || "";
  var category = stringParam(params, "category", "all");
  var logoBaseUrl = stringParam(params, "logoBaseUrl", DEFAULT_LOGO_BASE_URL);
  var iconLibraryUrl = stringParam(params, "iconLibraryUrl", "").trim();
  var iconLibrary = loadIconLibrary(iconLibraryUrl);
  var iconProxyTemplate = stringParam(params, "iconProxyTemplate", "").trim();
  var useGeneratedLogoFallback = stringParam(params, "useGeneratedLogoFallback", "false") === "true";
  var preferGroupCategory = stringParam(params, "preferGroupCategory", "true") === "true";
  var format = detectFormat(content);
  var items;

  if (format === "m3u") {
    items = parseM3uSource(content, logoBaseUrl, iconLibrary, iconProxyTemplate, useGeneratedLogoFallback, sourceUrl, preferGroupCategory);
  } else if (format === "txt") {
    items = parseTxtSource(content, logoBaseUrl, iconLibrary, iconProxyTemplate, useGeneratedLogoFallback, preferGroupCategory);
  } else if (format === "json") {
    items = parseJsonSource(content, iconLibrary, iconProxyTemplate, preferGroupCategory);
  } else {
    throw new Error("无法识别内容格式，请确认源返回的是 M3U/TXT/JSON");
  }

  if (category !== "all") {
    items = items.filter(function(item) { return item._category === category; });
  }
  items.forEach(function(item) { delete item._category; });
  BiliPai.storage.set("lastSourceUrl", sourceUrl);
  BiliPai.log("加载频道 " + items.length + " 个");
  return items;
}

function parseM3uSource(content, logoBaseUrl, iconLibrary, iconProxyTemplate, useGeneratedLogoFallback, sourceUrl, preferGroupCategory) {
  var lines = String(content).split(/\r?\n/);
  var channels = {};
  var currentName = "";
  var currentLogo = "";
  var currentGroup = "";
  var currentTvgId = "";

  lines.forEach(function(rawLine) {
    var line = rawLine.trim();
    if (line.indexOf("#EXTM3U") === 0) {
      var epgUrl = matchAttribute(line, "url-epg") || matchAttribute(line, "x-tvg-url");
      if (epgUrl && sourceUrl) {
        try { BiliPai.storage.set("epgUrl:" + sourceUrl, epgUrl); } catch (e) {}
      }
      return;
    }
    if (line.indexOf("#EXTINF:") === 0) {
      var attrs = parseExtInfAttributes(line);
      var nameMatch = line.match(/#EXTINF:.*?,(.+)$/);
      currentName = cleanLeadingDash(attrs["tvg-name"] || (nameMatch ? nameMatch[1].trim() : "") || "未知频道");
      currentLogo = attrs["tvg-logo"] || attrs["logo"] || "";
      currentGroup = attrs["group-title"] || "";
      currentTvgId = attrs["tvg-id"] || "";
      return;
    }
    if (line.indexOf("#EXTGRP:") === 0) {
      if (!currentGroup) currentGroup = line.substring("#EXTGRP:".length).trim();
      return;
    }
    if (!line || line.indexOf("#") === 0) return;
    if (!isValidStreamUrl(line)) return;
    addChannel(
      channels,
      currentName || "频道",
      line,
      currentLogo,
      logoBaseUrl,
      iconLibrary,
      iconProxyTemplate,
      useGeneratedLogoFallback,
      currentGroup,
      currentTvgId,
      preferGroupCategory
    );
    currentName = "";
    currentLogo = "";
    currentGroup = "";
    currentTvgId = "";
  });

  return channelMapToItems(channels);
}

function parseTxtSource(content, logoBaseUrl, iconLibrary, iconProxyTemplate, useGeneratedLogoFallback, preferGroupCategory) {
  var channels = {};
  String(content).split(/\r?\n/).forEach(function(rawLine) {
    var line = rawLine.trim();
    if (!line || line.indexOf("#") === 0) return;
    var splitIndex = line.indexOf(",");
    if (splitIndex < 0) splitIndex = line.indexOf(" ");
    var name = splitIndex > 0 ? line.substring(0, splitIndex).trim() : "频道";
    var url = splitIndex > 0 ? line.substring(splitIndex + 1).trim() : line;
    if (isValidStreamUrl(url)) {
      addChannel(
        channels,
        cleanLeadingDash(name),
        url,
        "",
        logoBaseUrl,
        iconLibrary,
        iconProxyTemplate,
        useGeneratedLogoFallback,
        "",
        "",
        preferGroupCategory
      );
    }
  });
  return channelMapToItems(channels);
}

function parseJsonSource(content, iconLibrary, iconProxyTemplate, preferGroupCategory) {
  var parsed = JSON.parse(String(content));
  var arrays = Array.isArray(parsed) ? { all: parsed } : parsed;
  var items = [];
  Object.keys(arrays).forEach(function(categoryKey) {
    var channels = arrays[categoryKey];
    if (!Array.isArray(channels)) return;
    channels.forEach(function(channel) {
      var url = channel.videoUrl || channel.url || channel.id;
      if (!url) return;
      var group = channel.groupTitle || channel.group || channel.category || categoryKey;
      var streams = normalizeStreams(channel.streams || channel.childItems || []);
      items.push({
        id: String(channel.tvgId || channel.id || url),
        title: cleanLeadingDash(channel.title || channel.name || "频道"),
        description: cleanLeadingDash(channel.description || channel.name || ""),
        coverUrls: buildIconCandidates(
          channel.coverUrl ||
            channel.logo ||
            channel.tvgLogo ||
            channel.backdrop_path ||
            resolveIconUrl(iconLibrary, channel.title || channel.name || ""),
          iconProxyTemplate
        ),
        backdropUrl: scaleIcon(channel.backdropUrl || channel.backdrop_path || "", iconProxyTemplate),
        type: "video",
        videoUrl: url,
        streams: streams,
        _category: guessCategory(channel.title || channel.name || categoryKey, group, preferGroupCategory)
      });
    });
  });
  return items;
}

function addChannel(
  channels,
  name,
  url,
  logoUrl,
  logoBaseUrl,
  iconLibrary,
  iconProxyTemplate,
  useGeneratedLogoFallback,
  group,
  tvgId,
  preferGroupCategory
) {
  var title = cleanLeadingDash(name || "频道");
  var key = tvgId ? tvgId : title;
  if (!channels[key]) {
    var cleanName = cleanChannelNameForLogo(title);
    var matchedIcon = logoUrl || resolveIconUrl(iconLibrary, title) || resolveIconUrl(iconLibrary, cleanName);
    var generatedIcon = useGeneratedLogoFallback && cleanName ? logoBaseUrl + cleanName + ".png" : "";
    channels[key] = {
      title: cleanName || title,
      tvgId: tvgId,
      description: title,
      logoUrl: matchedIcon || generatedIcon,
      iconProxyTemplate: iconProxyTemplate,
      group: group,
      category: guessCategory(title, group, preferGroupCategory),
      urls: []
    };
  }
  channels[key].urls.push(url);
}

function channelMapToItems(channels) {
  return Object.keys(channels).map(function(key) {
    var channel = channels[key];
    return {
      id: channel.tvgId || channel.title || channel.urls[0],
      title: channel.title,
      description: channel.description,
      coverUrls: buildIconCandidates(channel.logoUrl, channel.iconProxyTemplate),
      type: "video",
      videoUrl: channel.urls[0],
      streams: channel.urls.slice(1).map(function(url, index) {
        return {
          id: "backup-" + (index + 1),
          title: "备用线路 " + (index + 1),
          url: url
        };
      }),
      _category: channel.category
    };
  });
}

function normalizeStreams(streams) {
  return streams.map(function(stream, index) {
    if (typeof stream === "string") {
      return { id: "line-" + (index + 1), title: "线路 " + (index + 1), url: stream };
    }
    return {
      id: String(stream.id || "line-" + (index + 1)),
      title: stream.title || ("线路 " + (index + 1)),
      url: stream.url || stream.videoUrl || stream.id || ""
    };
  }).filter(function(stream) {
    return !!stream.url;
  });
}

function detectFormat(content) {
  var trimmed = String(content || "").trim();
  if (trimmed.indexOf("#EXTM3U") === 0) return "m3u";
  if (trimmed.indexOf("{") === 0 || trimmed.indexOf("[") === 0) return "json";
  if (/https?:\/\//i.test(trimmed)) return "txt";
  return "";
}

function parseExtInfAttributes(extInf) {
  var attrs = {};
  var regex = /(\S+?)=["']?([^"'\s,]+)["']?/g;
  var match;
  while ((match = regex.exec(extInf)) !== null) {
    attrs[match[1].toLowerCase()] = match[2];
  }
  return attrs;
}

function matchAttribute(line, key) {
  var regex = new RegExp(key + "=\"([^\"]+)\"", "i");
  var match = line.match(regex);
  return match ? match[1] : "";
}

function isValidStreamUrl(url) {
  return /^(https?|rtmps?|rtsp|udp|mms):\/\//i.test(url);
}

function stringParam(params, name, fallback) {
  return params && params[name] != null ? String(params[name]) : fallback;
}

function loadIconLibrary(iconLibraryUrl) {
  if (!iconLibraryUrl) return {};
  try {
    var cacheKey = "iconLibrary:" + iconLibraryUrl;
    var cached = BiliPai.storage.get(cacheKey);
    if (cached) return JSON.parse(cached);
    var response = BiliPai.http.get(iconLibraryUrl);
    if (response.code < 200 || response.code >= 300) {
      BiliPai.log("图标库请求失败: HTTP " + response.code);
      return {};
    }
    var library = parseIconLibrary(response.body || "");
    BiliPai.storage.set(cacheKey, JSON.stringify(library));
    return library;
  } catch (error) {
    BiliPai.log("图标库解析失败: " + error.message);
    return {};
  }
}

function parseIconLibrary(content) {
  var parsed = JSON.parse(String(content || "{}"));
  var icons = Array.isArray(parsed) ? parsed : parsed.icons;
  var map = {};
  if (!Array.isArray(icons)) return map;
  icons.forEach(function(icon) {
    var name = icon.name || icon.title || icon.id || "";
    var url = icon.url || icon.icon || icon.src || "";
    if (!name || !url) return;
    map[normalizeIconName(name)] = url;
  });
  return map;
}

function resolveIconUrl(iconLibrary, channelName) {
  if (!iconLibrary || !channelName) return "";
  return iconLibrary[normalizeIconName(channelName)] || "";
}

function normalizeIconName(name) {
  return cleanChannelNameForLogo(name).toLowerCase();
}

function buildIconCandidates(url, iconProxyTemplate) {
  if (!url) return [];
  var candidates = [url];
  var proxied = scaleIcon(url, iconProxyTemplate);
  if (proxied && proxied !== url) candidates.push(proxied);
  return candidates;
}

function scaleIcon(url, iconProxyTemplate) {
  if (!url) return "";
  if (!iconProxyTemplate) return url;
  if (iconProxyTemplate.indexOf("{url}") >= 0) {
    return iconProxyTemplate.replace("{url}", encodeURIComponent(url));
  }
  return iconProxyTemplate + url;
}

function cleanLeadingDash(str) {
  return String(str || "").replace(/^[-\s\._－]+/, "");
}

function cleanChannelNameForLogo(rawName) {
  return cleanLeadingDash(rawName)
    .replace(/\s*(高清|超清|蓝光|HD|FHD|4K|频道|直播)\s*/gi, "")
    .replace(/([a-z]+)(\d*)/i, function(match, p1, p2) { return p1.toUpperCase() + p2; })
    .replace(/\s+/g, " ")
    .replace(/[^\u4e00-\u9fa5a-zA-Z0-9+]/g, "")
    .trim();
}

function guessCategory(channelName, group, preferGroupCategory) {
  if (preferGroupCategory) {
    var fromGroup = guessCategoryFromGroup(group);
    if (fromGroup) return fromGroup;
  }
  var name = String(channelName || "").toLowerCase();
  if (name.indexOf("cctv") >= 0 || name.indexOf("央视") >= 0) return "cctv";
  if (name.indexOf("卫视") >= 0 && name.indexOf("地方") < 0) return "stv";
  if (name.indexOf("体育") >= 0 || name.indexOf("sports") >= 0) return "sports";
  if (name.indexOf("电影") >= 0 || name.indexOf("电视剧") >= 0 || name.indexOf("影院") >= 0) return "movie";
  if (name.indexOf("纪录") >= 0 || name.indexOf("纪实") >= 0 || name.indexOf("documentary") >= 0) return "doc";
  if (name.indexOf("少儿") >= 0 || name.indexOf("儿童") >= 0 || name.indexOf("卡通") >= 0) return "kids";
  if (name.indexOf("音乐") >= 0 || name.indexOf("music") >= 0) return "music";
  if (name.indexOf("新闻") >= 0 || name.indexOf("news") >= 0) return "news";
  if (name.indexOf("港") >= 0 || name.indexOf("澳") >= 0 || name.indexOf("台") >= 0) return "hk";
  if (name.indexOf("海外") >= 0 || name.indexOf("国际") >= 0 || name.indexOf("foreign") >= 0) return "overseas";
  return "ltv";
}

function guessCategoryFromGroup(group) {
  var g = String(group || "").toLowerCase();
  if (!g) return "";
  if (g.indexOf("cctv") >= 0 || g.indexOf("央视") >= 0) return "cctv";
  if (g.indexOf("卫视") >= 0) return "stv";
  if (g.indexOf("体育") >= 0 || g.indexOf("sport") >= 0) return "sports";
  if (g.indexOf("电影") >= 0 || g.indexOf("影") >= 0 || g.indexOf("movie") >= 0 || g.indexOf("剧场") >= 0) return "movie";
  if (g.indexOf("纪录") >= 0 || g.indexOf("纪实") >= 0 || g.indexOf("document") >= 0) return "doc";
  if (g.indexOf("少儿") >= 0 || g.indexOf("卡通") >= 0 || g.indexOf("动画") >= 0 || g.indexOf("kid") >= 0) return "kids";
  if (g.indexOf("音乐") >= 0 || g.indexOf("music") >= 0) return "music";
  if (g.indexOf("新闻") >= 0 || g.indexOf("资讯") >= 0 || g.indexOf("news") >= 0) return "news";
  if (g.indexOf("港") >= 0 || g.indexOf("澳") >= 0 || g.indexOf("台") >= 0) return "hk";
  if (g.indexOf("海外") >= 0 || g.indexOf("国际") >= 0) return "overseas";
  return "";
}
