#!/usr/bin/env node
/**
 * BiliPai JS 插件脚手架
 *
 * 用法：
 *   node create-plugin.js <插件ID> [输出目录]
 *
 * 示例：
 *   node create-plugin.js dev.example.my_plugin
 *   node create-plugin.js dev.example.my_plugin ./my-plugin
 *
 * 生成后打开输出目录里的 plugin.js，按 TODO 注释填写抓取逻辑即可。
 * 完整 API 文档见仓库 docs/PLUGIN_DEVELOPMENT.md 的「JS 插件」章节。
 */
'use strict';

const fs = require('fs');
const path = require('path');

const PLUGIN_ID_PATTERN = /^[A-Za-z0-9_.-]{1,64}$/;
const FUNCTION_NAME_PATTERN = /^[A-Za-z_$][A-Za-z0-9_$]{0,63}$/;

function fail(message) {
  console.error('错误: ' + message);
  process.exit(1);
}

function toPascalCase(pluginId) {
  const parts = pluginId.split(/[._-]+/).filter(Boolean);
  const tail = parts.slice(-2).join(' ');
  return tail
    .split(/\s+/)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join('');
}

function toFunctionName(pluginId) {
  const name = 'load' + toPascalCase(pluginId).replace(/[^A-Za-z0-9]/g, '');
  return FUNCTION_NAME_PATTERN.test(name) ? name : 'loadItems';
}

const pluginId = process.argv[2];
if (!pluginId) {
  fail('缺少插件 ID 参数。用法: node create-plugin.js <插件ID> [输出目录]');
}
if (!PLUGIN_ID_PATTERN.test(pluginId)) {
  fail('插件 ID 格式无效，仅支持字母数字/._-，长度 1-64: ' + pluginId);
}

const outputDir = path.resolve(process.argv[3] || pluginId.split('.').pop() || 'bilipai-plugin');
if (fs.existsSync(outputDir)) {
  fail('输出目录已存在: ' + outputDir);
}
const functionName = toFunctionName(pluginId);
const className = toPascalCase(pluginId) || 'MyPlugin';

fs.mkdirSync(outputDir, { recursive: true });

const pluginJs = `// ${pluginId} — BiliPai 原生 JS 插件
// 宿主会在 WebView 中执行本文件，可用的 API 见 docs/PLUGIN_DEVELOPMENT.md：
//   BiliPai.http.get(url, headers) / BiliPai.http.post(url, body, headers)
//     -> { code, body, headers }；headers 不传时使用默认 UA，传入 User-Agent 可覆盖
//   BiliPai.dom.parse(html) -> { title, body, select(selector), selectOne(selector) }
//     节点对象：text / html / attr(name) / select(selector) / selectOne(selector)
//   BiliPai.storage.get(key) / set(key, value) / remove(key)   // 插件私有持久化
//   BiliPai.log(message)                                       // logcat: BiliPaiJsPlugin
//
// 返回值必须是 BiliPaiJsMediaItem 数组：
//   { id, title, description, coverUrl, backdropUrl, posterPath, type: "video",
//     videoUrl 或 streams: [{ id, title, url, headers? }], childItems: [...] }

window.BiliPaiPlugin = {
  id: '${pluginId}',
  title: '${className}',
  version: '1.0.0',
  author: '',
  description: 'TODO: 一句话描述这个插件提供什么内容。',
  // 权限按需声明，安装时会展示给用户确认：
  // NETWORK / PLUGIN_STORAGE / EXTERNAL_MEDIA_PLAYBACK
  permissions: ['NETWORK', 'PLUGIN_STORAGE', 'EXTERNAL_MEDIA_PLAYBACK'],
  modules: [
    {
      id: 'main',
      title: '${className}',
      description: 'TODO: 模块说明。',
      functionName: '${functionName}',
      // cacheDuration: 600, // 可选：结果缓存秒数，0 或缺省表示不缓存
      params: [
        // 普通文本参数（自由输入）：
        // { name: 'sourceUrl', title: '数据源 URL', type: 'text', defaultValue: '' },
        // 枚举参数（显示为选项 chips）：
        // { name: 'category', title: '分类', type: 'enum', defaultValue: 'all',
        //   options: [{ title: '全部', value: 'all' }] },
        // 宿主驱动分页（不显示在表单里，滚动到底时自动递增调用）：
        // { name: 'page', title: '页码', type: 'page' }        // 从 1 开始
        // { name: 'offset', title: '偏移', type: 'offset' }    // 已加载条数
      ]
    }
  ],
  ${functionName}: ${functionName}
};

async function ${functionName}(params) {
  const sourceUrl = String((params && params.sourceUrl) || '').trim();
  if (!sourceUrl) {
    throw new Error('请在模块参数里填写数据源 URL');
  }

  const response = BiliPai.http.get(sourceUrl);
  if (response.code < 200 || response.code >= 300) {
    throw new Error('数据源请求失败: HTTP ' + response.code);
  }

  // TODO: 把 response.body 解析成媒体列表。
  // JSON 数据：JSON.parse(response.body)
  // HTML 页面：const doc = BiliPai.dom.parse(response.body); doc.select('a.item') ...
  const items = [];
  items.push({
    id: 'example-1',
    title: '示例条目',
    description: 'TODO: 替换成真实数据',
    coverUrl: '',
    type: 'video',
    videoUrl: 'https://example.com/stream.mp4'
  });

  BiliPai.log('${pluginId} 返回 ' + items.length + ' 条');
  return items;
}
`;

const readme = `# ${className}（${pluginId}）

BiliPai 原生 JS 插件。由 \`plugins/tools/create-plugin.js\` 脚手架生成。

## 安装到 BiliPai

1. 插件中心 → JS 插件 → 本地导入，选择 \`plugin.js\`（或把文件放到可访问的 URL 后用 URL 导入）。
2. 检查预览信息与权限，确认安装。
3. 在插件列表里启用插件，打开内容页验证。

## 开发

- 修改 \`plugin.js\` 后重新导入即可更新（会要求重新授权）。
- 宿主 API（http/dom/storage/log）与返回数据格式见仓库 \`docs/PLUGIN_DEVELOPMENT.md\`。
`;

fs.writeFileSync(path.join(outputDir, 'plugin.js'), pluginJs, 'utf8');
fs.writeFileSync(path.join(outputDir, 'README.md'), readme, 'utf8');

console.log('已生成 BiliPai JS 插件骨架:');
console.log('  ' + path.join(outputDir, 'plugin.js'));
console.log('  ' + path.join(outputDir, 'README.md'));
console.log('');
console.log('下一步: 打开 plugin.js，按 TODO 注释填写解析逻辑。');
