var DEFAULT_COOKIE = "";

window.BiliPaiPlugin = {
  id: "live.huya",
  title: "虎牙直播",
  version: "1.0.0",
  author: "BiliPai",
  description: "虎牙直播分区浏览与播放。分区参数选择后加载房间列表，点按房间自动解析带 Referer/Cookie 的 FLV 播放地址。签名 anticode 采用 MD5+base64，与 pure_live_TV huya_site 一致。",
  permissions: ["NETWORK", "PLUGIN_STORAGE", "EXTERNAL_MEDIA_PLAYBACK"],
  modules: [
    {
      id: "rooms",
      title: "虎牙房间",
      description: "按分区/推荐/搜索加载房间列表。",
      functionName: "loadRooms",
      params: [
        {
          name: "mode",
          title: "加载方式",
          type: "enum",
          defaultValue: "recommend",
          options: [
            { title: "推荐", value: "recommend" },
            { title: "分区", value: "category" },
            { title: "搜索", value: "search" }
          ]
        },
        {
          name: "areaId",
          title: "分区 ID（mode=category 时生效）",
          type: "text",
          defaultValue: "1"
        },
        {
          name: "keyword",
          title: "搜索关键词（mode=search 时生效）",
          type: "text",
          defaultValue: ""
        },
        {
          name: "page",
          title: "页码",
          type: "text",
          defaultValue: "1"
        },
        {
          name: "cookie",
          title: "虎牙 Cookie（可选，用于高清/关注）",
          type: "text",
          defaultValue: DEFAULT_COOKIE
        }
      ]
    },
    {
      id: "stream",
      title: "虎牙播放地址",
      description: "根据房间 ID 解析可播放的 FLV 流地址（带 Referer/Cookie/UA）。",
      functionName: "loadStream",
      params: [
        {
          name: "roomId",
          title: "房间 ID",
          type: "text",
          defaultValue: ""
        },
        {
          name: "qualityId",
          title: "清晰度 bitRate（0=原画，2000=高清等，留空=原画）",
          type: "text",
          defaultValue: ""
        },
        {
          name: "cookie",
          title: "虎牙 Cookie（可选）",
          type: "text",
          defaultValue: DEFAULT_COOKIE
        }
      ]
    }
  ],
  loadRooms: loadRooms,
  loadStream: loadStream
};

var WEB_UA = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36";
var PLAYER_UA = WEB_UA;

async function loadRooms(params) {
  var mode = stringParam(params, "mode", "recommend");
  var page = parseInt(stringParam(params, "page", "1"), 10) || 1;
  var cookie = stringParam(params, "cookie", DEFAULT_COOKIE);
  var items;

  if (mode === "category") {
    var areaId = stringParam(params, "areaId", "1").trim() || "1";
    items = await fetchCategoryRooms(areaId, page, cookie);
  } else if (mode === "search") {
    var keyword = stringParam(params, "keyword", "").trim();
    if (!keyword) throw new Error("请填写搜索关键词");
    items = await fetchSearchRooms(keyword, page, cookie);
  } else {
    items = await fetchRecommendRooms(page, cookie);
  }

  BiliPai.log("虎牙加载房间 " + items.length + " 个 (mode=" + mode + ", page=" + page + ")");
  return items;
}

async function loadStream(params) {
  var roomId = stringParam(params, "roomId", "").trim();
  if (!roomId) throw new Error("请填写房间 ID");
  var qualityId = stringParam(params, "qualityId", "").trim();
  var bitRate = parseInt(qualityId, 10) || 0;
  var cookie = stringParam(params, "cookie", DEFAULT_COOKIE);

  var detail = await fetchRoomDetail(roomId, cookie);
  if (!detail.live || detail.lines.length === 0) {
    throw new Error("房间未开播或无可用线路: " + roomId);
  }

  var headers = {
    Referer: "https://www.huya.com/",
    Origin: "https://www.huya.com",
    "User-Agent": PLAYER_UA
  };
  if (cookie) headers["Cookie"] = cookie;

  var streams = await Promise.all(detail.lines.map(async function(line) {
    var anti = await buildAnticode(line.streamName, line.presenterUid, line.flvAntiCode);
    var url = line.flvUrl + "/" + line.streamName + ".flv?" + anti + "&codec=264";
    if (bitRate > 0) url += "&ratio=" + bitRate;
    return {
      id: line.cdnType || "line",
      title: line.cdnType || "线路",
      url: url,
      contentType: "video/x-flv",
      headers: headers
    };
  }));

  var qualities = detail.bitRates.map(function(b) {
    return { id: String(b.bitRate), title: b.name };
  });

  BiliPai.log("虎牙解析播放地址 " + streams.length + " 条 (roomId=" + roomId + ", bitRate=" + bitRate + ")");
  return {
    id: roomId,
    title: detail.title || roomId,
    description: detail.nick || "",
    coverUrls: detail.cover ? [detail.cover] : [],
    type: "video",
    videoUrl: streams.length > 0 ? streams[0].url : "",
    streams: streams,
    childItems: qualities.map(function(q) {
      return {
        id: q.id,
        title: q.title,
        type: "video",
        videoUrl: "",
        streams: streams.map(function(s) {
          var url = s.url;
          if (q.id !== "0" && q.id !== "") {
            var br = parseInt(q.id, 10) || 0;
            if (br > 0) {
              if (url.indexOf("ratio=") < 0) url += "&ratio=" + br;
            }
          }
          return { id: s.id, title: q.title + " - " + s.title, url: url, contentType: s.contentType, headers: s.headers };
        })
      };
    })
  };
}

async function fetchRecommendRooms(page, cookie) {
  var url = "https://www.huya.com/cache.php?m=LiveList&do=getLiveListByPage&tagAll=0&page=" + page;
  var body = await httpGet(url, recommendHeaders(cookie));
  return parseRoomList(body);
}

async function fetchCategoryRooms(areaId, page, cookie) {
  var url = "https://www.huya.com/cache.php?m=LiveList&do=getLiveListByPage&tagAll=0&gameId=" + encodeURIComponent(areaId) + "&page=" + page;
  var body = await httpGet(url, recommendHeaders(cookie));
  return parseRoomList(body);
}

async function fetchSearchRooms(keyword, page, cookie) {
  var start = (page - 1) * 20;
  var url = "https://search.cdn.huya.com/?m=Search&do=getSearchContent&q=" + encodeURIComponent(keyword) + "&uid=0&v=4&typ=-5&livestate=0&rows=20&start=" + start;
  var body = await httpGet(url, apiHeaders(cookie));
  return parseSearchRooms(body);
}

async function fetchRoomDetail(roomId, cookie) {
  var url = "https://mp.huya.com/cache.php?m=Live&do=profileRoom&roomid=" + encodeURIComponent(roomId) + "&showSecret=1";
  var body = await httpGet(url, detailHeaders(cookie));
  return parseRoomDetail(roomId, body);
}

function recommendHeaders(cookie) {
  var h = apiHeaders(cookie);
  h["Origin"] = "https://www.huya.com";
  h["Referer"] = "https://www.huya.com/";
  return h;
}

function detailHeaders(cookie) {
  var h = apiHeaders(cookie);
  h["Accept"] = "*/*";
  h["Origin"] = "https://www.huya.com";
  h["Referer"] = "https://www.huya.com/";
  h["Sec-Fetch-Dest"] = "empty";
  h["Sec-Fetch-Mode"] = "cors";
  h["Sec-Fetch-Site"] = "same-site";
  return h;
}

function apiHeaders(cookie) {
  var h = { "User-Agent": WEB_UA };
  if (cookie) h["Cookie"] = cookie;
  return h;
}

async function httpGet(url, headers) {
  var resp = BiliPai.http.get(url, headers || {});
  if (resp.code < 200 || resp.code >= 300) {
    throw new Error("虎牙请求失败: HTTP " + resp.code + " @ " + url);
  }
  return resp.body || "";
}

function parseRoomList(body) {
  var root = JSON.parse(String(body || "{}"));
  var data = root.data || {};
  var datas = data.datas || [];
  var items = [];
  for (var i = 0; i < datas.length; i++) {
    var item = datas[i];
    items.push(toRoomItem(item));
  }
  var hasMore = (data.page || 0) < (data.totalPage || 0);
  return items;
}

function parseSearchRooms(body) {
  var root = JSON.parse(String(body || "{}"));
  var response = root.response || {};
  var queryList = (response["3"] && response["3"].docs) || [];
  var responseList = (response["1"] && response["1"].docs) || [];
  var items = [];
  for (var i = 0; i < queryList.length; i++) {
    var doc = queryList[i];
    var roomId = findRoomId(responseList, doc.uid, doc.yyid) || String(doc.room_id || "");
    items.push({
      id: roomId,
      title: doc.game_introduction || doc.game_roomName || "未知",
      description: doc.game_nick || "",
      coverUrls: [normalizeCover(doc.game_screenshot || "")],
      type: "video",
      videoUrl: "",
      streams: [],
      _roomId: roomId
    });
  }
  return items;
}

function parseRoomDetail(roomId, body) {
  var root = JSON.parse(String(body || "{}"));
  var status = root.status || 0;
  var data = root.data || {};
  var stream = data.stream || {};
  if (status !== 200 || !stream.baseSteamInfoList) {
    return { live: false, lines: [], bitRates: [], title: "", nick: "", cover: "" };
  }
  var baseList = stream.baseSteamInfoList || [];
  var flvLines = (stream.flv && stream.flv.multiLine) || [];
  var lines = [];
  for (var i = 0; i < flvLines.length; i++) {
    var flv = flvLines[i];
    if (!flv.url) continue;
    var matchKey = flv.cdnType || flv.sCdnType || "";
    var base = null;
    for (var j = 0; j < baseList.length; j++) {
      if (baseList[j].sCdnType === matchKey) { base = baseList[j]; break; }
    }
    if (!base) continue;
    lines.push({
      flvUrl: base.sFlvUrl || "",
      streamName: base.sStreamName || "",
      flvAntiCode: base.sFlvAntiCode || "",
      presenterUid: asLong(base.lChannelId),
      cdnType: flv.sCdnType || matchKey
    });
  }
  lines = lines.filter(function(l) { return l.flvUrl && l.streamName; });

  var liveData = data.liveData || {};
  var bitRateInfoStr = liveData.bitRateInfo;
  var rateArray = null;
  if (bitRateInfoStr) {
    try { rateArray = JSON.parse(bitRateInfoStr); } catch (e) { rateArray = null; }
  }
  if (!rateArray && stream.flv && stream.flv.rateArray) rateArray = stream.flv.rateArray;
  var bitRates = [];
  if (rateArray) {
    for (var k = 0; k < rateArray.length; k++) {
      var name = rateArray[k].sDisplayName || "";
      if (name && !bitRates.some(function(b) { return b.name === name; })) {
        bitRates.push({ bitRate: rateArray[k].iBitRate || 0, name: name });
      }
    }
  }
  if (bitRates.length === 0) {
    bitRates.push({ bitRate: 0, name: "原画" });
    bitRates.push({ bitRate: 2000, name: "高清" });
  }

  var liveStatus = data.liveStatus || "";
  var live = liveStatus === "ON" || liveStatus === "REPLAY";
  var profile = data.profileInfo || {};
  return {
    live: live,
    lines: lines,
    bitRates: bitRates,
    title: liveData.introduction || "",
    nick: profile.nick || "",
    cover: liveData.screenshot || ""
  };
}

function toRoomItem(item) {
  var roomId = String(item.profileRoom || "");
  return {
    id: roomId,
    title: item.introduction || item.roomName || "未知",
    description: item.nick || "",
    coverUrls: [normalizeCover(item.screenshot || "")],
    type: "video",
    videoUrl: "",
    streams: [],
    _roomId: roomId
  };
}

function findRoomId(responseList, targetUid, targetYyid) {
  if (!targetUid && !targetYyid) return null;
  for (var i = 0; i < responseList.length; i++) {
    var o = responseList[i];
    if (String(o.uid) === String(targetUid) && String(o.yyid) === String(targetYyid)) {
      return String(o.room_id || "");
    }
  }
  return null;
}

function normalizeCover(cover) {
  if (!cover) return "";
  if (cover.indexOf("?") < 0) return cover + "?x-oss-process=style/w338_h190&";
  return cover;
}

function asLong(v) {
  if (typeof v === "number") return Math.floor(v);
  return parseInt(String(v), 10) || 0;
}

async function buildAnticode(streamName, presenterUid, antiCode, nowOverride, randomOverride) {
  var map = parseQuery(antiCode);
  if (!map.fm) return antiCode;
  var ctype = map.ctype || "huya_pc_exe";
  var platformId = parseInt(map.t, 10) || 0;
  var isWap = platformId === 103;
  var now = (nowOverride != null ? nowOverride : Date.now());
  var rnd = (randomOverride != null ? randomOverride : Math.random());
  var seqId = presenterUid + now;
  var secretHash = await md5Hex(seqId + "|" + ctype + "|" + platformId);
  var convertUid = rotl64(presenterUid);
  var calcUid = isWap ? presenterUid : convertUid;
  var fm = map.fm || "";
  var secretPrefix = atobUtf8(fm).split("_")[0];
  var wsTime = map.wsTime || "";
  var wsSecret = await md5Hex(secretPrefix + "_" + calcUid + "_" + streamName + "_" + secretHash + "_" + wsTime);
  var wsTimeLong = parseInt(wsTime, 16) || 0;
  var ct = (wsTimeLong + Math.floor(rnd * 1000)) * 1000;
  var uuid = (((ct % 10000000000) + Math.floor(rnd * 1000)) * 1000 % 0xffffffff).toString();

  var parts = [];
  parts.push(["wsSecret", wsSecret]);
  parts.push(["wsTime", wsTime]);
  parts.push(["seqid", seqId]);
  parts.push(["ctype", ctype]);
  parts.push(["ver", "1"]);
  parts.push(["fs", map.fs || ""]);
  parts.push(["fm", encodeURIComponent(map.fm || "")]);
  parts.push(["t", platformId]);
  if (isWap) {
    parts.push(["uid", presenterUid]);
    parts.push(["uuid", uuid]);
  } else {
    parts.push(["u", convertUid]);
  }
  return parts.map(function(p) { return p[0] + "=" + p[1]; }).join("&");
}

function rotl64(t) {
  var low = t & 0xffffffff;
  return ((low << 8) | (low >>> 24)) & 0xffffffff;
}

function parseQuery(query) {
  var map = {};
  if (!query) return map;
  var body = query.indexOf("?") >= 0 ? query.substring(query.indexOf("?") + 1) : query;
  body.split("&").forEach(function(pair) {
    if (!pair) return;
    var eq = pair.indexOf("=");
    if (eq < 0) return;
    var k = decodeURIComponent(pair.substring(0, eq));
    var v = decodeURIComponent(pair.substring(eq + 1));
    if (k) map[k] = v;
  });
  return map;
}

async function md5Hex(input) {
  var bytes = utf8Encode(input);
  var subtle = globalThis.crypto && globalThis.crypto.subtle;
  if (!subtle || !subtle.digest) {
    throw new Error("当前环境不支持 crypto.subtle.digest，无法计算虎牙 anticode");
  }
  var buf = await subtle.digest("MD5", new Uint8Array(bytes).buffer);
  var u8 = new Uint8Array(buf);
  var hex = "";
  for (var i = 0; i < u8.length; i++) {
    var v = u8[i];
    hex += (v < 16 ? "0" : "") + v.toString(16);
  }
  return hex;
}

function atobUtf8(b64) {
  var bin = atob(b64);
  var bytes = [];
  for (var i = 0; i < bin.length; i++) bytes.push(bin.charCodeAt(i) & 0xff);
  return utf8Decode(bytes);
}

function utf8Encode(str) {
  str = String(str || "");
  var bytes = [];
  for (var i = 0; i < str.length; i++) {
    var c = str.charCodeAt(i);
    if (c < 0x80) bytes.push(c);
    else if (c < 0x800) { bytes.push(0xc0 | (c >> 6), 0x80 | (c & 0x3f)); }
    else if (c >= 0xd800 && c <= 0xdbff) {
      var c2 = str.charCodeAt(++i);
      var cp = 0x10000 + ((c & 0x3ff) << 10) + (c2 & 0x3ff);
      bytes.push(0xf0 | (cp >> 18), 0x80 | ((cp >> 12) & 0x3f), 0x80 | ((cp >> 6) & 0x3f), 0x80 | (cp & 0x3f));
    } else { bytes.push(0xe0 | (c >> 12), 0x80 | ((c >> 6) & 0x3f), 0x80 | (c & 0x3f)); }
  }
  return bytes;
}

function utf8Decode(bytes) {
  var str = "";
  var i = 0;
  while (i < bytes.length) {
    var b = bytes[i++];
    if (b < 0x80) str += String.fromCharCode(b);
    else if (b < 0xe0) { str += String.fromCharCode(((b & 0x1f) << 6) | (bytes[i++] & 0x3f)); }
    else if (b < 0xf0) {
      var c = ((b & 0x0f) << 12) | ((bytes[i++] & 0x3f) << 6) | (bytes[i++] & 0x3f);
      str += String.fromCharCode(c);
    } else {
      var cp = ((b & 0x07) << 18) | ((bytes[i++] & 0x3f) << 12) | ((bytes[i++] & 0x3f) << 6) | (bytes[i++] & 0x3f);
      cp -= 0x10000;
      str += String.fromCharCode(0xd800 + (cp >> 10), 0xdc00 + (cp & 0x3ff));
    }
  }
  return str;
}

function stringParam(params, name, fallback) {
  return params && params[name] != null ? String(params[name]) : fallback;
}
