#!/usr/bin/env bash
# 一次性脚本：遍历 Rovniced/bilibili-skin 仓库，生成 rovniced-skin-catalog.json 索引。
#
# 仓库已停止更新（冻结快照），只需运行一次。产物供 BiliPai 在线装扮浏览页使用。
#
# 索引字段（每个主题）：
#   id              —— 目录名（稳定身份）
#   name            —— 主题显示名（直接使用仓库目录名；目录名始终为 UTF-8）
#   previewUrl      —— preview.jpg 的 GitHub raw https 直链
#   packageZipUrl   —— <主题名>_package.zip 的 GitHub raw https 直链（优先下载源）
#   packageUrlCdn   —— 个性装扮.json 里的官方 CDN package_url（http/https，回退源）
#   themeMetadataUrl—— 个性装扮.json 的 GitHub raw https 直链
#   colorMode       —— light / dark
#   color           —— 主色
#   colorSecondPage —— 副色
#   tailColor       —— 底栏饰面色
#   capabilities    —— 资源能力位（底栏、顶部、侧栏、个人页、发布入口与动效）
#
# 用法：
#   ./scripts/generate_rovniced_skin_catalog.sh [输出路径]
#   默认输出到 app/src/main/assets/rovniced-skin-catalog.json
#
# 依赖：curl、grep、sed。python3/perl 可选（用于 URL 编码，缺失时降级）。
# 重复运行时已缓存的个性装扮.json 不会重新下载。

set -euo pipefail

REPO="Rovniced/bilibili-skin"
BRANCH="main"
RAW_BASE="https://raw.githubusercontent.com/${REPO}/${BRANCH}"
BRANCH_API="https://api.github.com/repos/${REPO}/branches/${BRANCH}"
TREE_CACHE="${TMPDIR:-/tmp}/rovniced_tree.json"
JSON_CACHE_DIR="${TMPDIR:-/tmp}/rovniced_json_cache"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUTPUT="${1:-app/src/main/assets/rovniced-skin-catalog.json}"
if [[ "${OUTPUT}" != /* ]]; then
  OUTPUT="${PROJECT_ROOT}/${OUTPUT}"
fi

mkdir -p "$(dirname "${OUTPUT}")"
mkdir -p "${JSON_CACHE_DIR}"

# ---------- 1. 获取仓库文件树 ----------
if [[ ! -s "${TREE_CACHE}" ]] || ! grep -q '"tree"' "${TREE_CACHE}" 2>/dev/null; then
  echo ">> 下载仓库文件树..."
  tree_sha=$(curl -sfL "${BRANCH_API}" | awk '
    /"tree": *\{/ { in_tree = 1; next }
    in_tree && /"sha":/ {
      value = $0
      sub(/^.*"sha": *"/, "", value)
      sub(/".*$/, "", value)
      print value
      exit
    }
  ')
  if [[ -z "${tree_sha}" ]]; then
    echo "!! 无法解析 ${BRANCH} 分支的 tree SHA" >&2
    exit 1
  fi
  curl -sfL "https://api.github.com/repos/${REPO}/git/trees/${tree_sha}?recursive=1" -o "${TREE_CACHE}"
fi

TRUNCATED=$(grep -oE '"truncated": *(true|false)' "${TREE_CACHE}" | head -1 | grep -oE '(true|false)' || echo "false")
if [[ "${TRUNCATED}" == "true" ]]; then
  echo "!! 警告：GitHub tree API 返回被截断，索引将不完整。" >&2
fi

PATHS_FILE="${TMPDIR:-/tmp}/rovniced_paths.txt"
grep -oE '"path": *"[^"]*"' "${TREE_CACHE}" | sed 's/"path": *"//;s/"$//' > "${PATHS_FILE}"

# 顶层目录（path 不含 /，排除非主题文件）
TOP_DIRS=$(grep -v '/' "${PATHS_FILE}" | grep -vE '^README|^LICENSE|^\.' | sort -u)
TOP_COUNT=$(echo "${TOP_DIRS}" | grep -c . || true)
echo ">> 发现 ${TOP_COUNT} 个顶层目录"

# ---------- 辅助函数 ----------
url_encode() {
  python3 -c "import urllib.parse,sys;print(urllib.parse.quote(sys.argv[1]))" "$1" 2>/dev/null \
    || perl -MURI::Escape -e 'print URI::Escape::uri_escape($ARGV[0])' "$1" 2>/dev/null \
    || printf '%s' "$1" | sed 's/ /%20/g;s/#/%23/g;s/\$/%24/g;s/&/%26/g;s/;/%3B/g;s/?/%3F/g'
}

md5_name() {
  printf '%s' "$1" | md5sum 2>/dev/null | cut -d' ' -f1 || md5 -q "$1" 2>/dev/null || printf '%s' "$1" | cksum | tr -d ' '
}

extract_field() {
  grep -oE "\"$2\":\"[^\"]*\"" <<<"$1" | head -1 | sed "s/\"$2\":\"//;s/\"$//"
}

jq_escape() {
  printf '"%s"' "${1//\"/\\\"}"
}

# ---------- 2. 遍历目录，提取元数据 ----------
echo ">> 开始提取主题元数据（串行 + 缓存）..."
ENTRIES=()
COUNT=0
SKIPPED=0

while IFS= read -r dir; do
  enc_dir=$(url_encode "${dir}")

  # preview.jpg 与 _package.zip 的 raw 直链
  preview_url=""
  pkg_zip_url=""
  if grep -qx "${dir}/preview.jpg" "${PATHS_FILE}"; then
    preview_url="${RAW_BASE}/${enc_dir}/preview.jpg"
  fi
  pkg_zip_name="${dir}_package.zip"
  if grep -qx "${dir}/${pkg_zip_name}" "${PATHS_FILE}"; then
    pkg_zip_url="${RAW_BASE}/${enc_dir}/$(url_encode "${pkg_zip_name}")"
  fi

  # 从个性装扮.json 提取元数据（带缓存）
  json_url="${RAW_BASE}/${enc_dir}/$(url_encode '个性装扮.json')"
  cache_file="${JSON_CACHE_DIR}/$(md5_name "${dir}").json"
  json=""
  if [[ -s "${cache_file}" ]]; then
    json=$(cat "${cache_file}")
  else
    json=$(curl -sfL --max-time 15 "${json_url}" 2>/dev/null || true)
    if [[ -n "${json}" ]]; then
      printf '%s' "${json}" > "${cache_file}"
    fi
  fi

  # 仓库中的部分旧版个性装扮.json 使用 GB18030/GBK，而 GitHub 路径始终是
  # UTF-8。直接拼接 JSON 内的 name 会生成混合编码 catalog，Android 解码后
  # 显示为 �。README 明确规定顶层目录就是主题名称，因此以目录名为准。
  name="${dir}"
  pkg_cdn=$(extract_field "${json}" "package_url" || true)
  color_mode=$(extract_field "${json}" "color_mode" || true)
  color=$(extract_field "${json}" "color" || true)
  color_second=$(extract_field "${json}" "color_second_page" || true)
  tail_color=$(extract_field "${json}" "tail_color" || true)

  caps_bottom="false"; caps_trim="false"; caps_profile="false"; caps_profile_video="false"
  caps_top="false"; caps_top_tab="false"; caps_side="false"; caps_drawer_trim="false"
  caps_publish="false"; caps_animated="false"
  echo "${json}" | grep -qE '"tail_icon_main":|"tail_icon_mode":"color"|"tail_icon_color":' && caps_bottom="true"
  echo "${json}" | grep -q '"tail_bg":' && caps_trim="true"
  echo "${json}" | grep -q '"head_myself_bg":' && caps_profile="true"
  echo "${json}" | grep -q '"head_myself_mp4_bg":' && caps_profile_video="true"
  echo "${json}" | grep -qE '"head_bg":|"head_tab_bg":' && caps_top="true"
  echo "${json}" | grep -q '"head_tab_bg":' && caps_top_tab="true"
  echo "${json}" | grep -q '"side_bg":' && caps_side="true"
  echo "${json}" | grep -q '"side_bg_bottom":' && caps_drawer_trim="true"
  echo "${json}" | grep -qE '"tail_icon_pub_btn_bg":|"pub_btn_plus_color":|"pub_btn_shade_color_top":' && caps_publish="true"
  echo "${json}" | grep -q '"tail_icon_ani":"true"' && caps_animated="true"

  # 跳过既无本地 zip 也无 CDN url 的目录
  if [[ -z "${pkg_zip_url}" && -z "${pkg_cdn}" ]]; then
    SKIPPED=$((SKIPPED + 1))
    continue
  fi

  entry="  {\"id\":$(jq_escape "${dir}"),\"name\":$(jq_escape "${name}"),\"previewUrl\":$(jq_escape "${preview_url}")"
  [[ -n "${pkg_zip_url}" ]] && entry="${entry},\"packageZipUrl\":$(jq_escape "${pkg_zip_url}")"
  [[ -n "${pkg_cdn}" ]] && entry="${entry},\"packageUrlCdn\":$(jq_escape "${pkg_cdn}")"
  [[ -n "${json}" ]] && entry="${entry},\"themeMetadataUrl\":$(jq_escape "${json_url}")"
  [[ -n "${color_mode}" ]] && entry="${entry},\"colorMode\":$(jq_escape "${color_mode}")"
  [[ -n "${color}" ]] && entry="${entry},\"color\":$(jq_escape "${color}")"
  [[ -n "${color_second}" ]] && entry="${entry},\"colorSecondPage\":$(jq_escape "${color_second}")"
  [[ -n "${tail_color}" ]] && entry="${entry},\"tailColor\":$(jq_escape "${tail_color}")"
  entry="${entry},\"capabilities\":{\"bottomBarIcons\":${caps_bottom},\"bottomBarTrim\":${caps_trim},\"profileBackground\":${caps_profile},\"profileVideo\":${caps_profile_video},\"topAtmosphere\":${caps_top},\"topTabBackground\":${caps_top_tab},\"sideBackground\":${caps_side},\"drawerBottomTrim\":${caps_drawer_trim},\"publishIcon\":${caps_publish},\"animatedIcons\":${caps_animated}}}"
  ENTRIES+=("${entry}")
  COUNT=$((COUNT + 1))
  [[ $((COUNT % 50)) -eq 0 ]] && echo "   ...已处理 ${COUNT} 个"
done <<<"${TOP_DIRS}"

# ---------- 3. 写出索引 ----------
{
  printf '{\n  "catalogVersion": 2,\n  "sourceRepo": "%s",\n  "sourceBranch": "%s",\n  "frozen": true,\n  "themes": [\n' "${REPO}" "${BRANCH}"
  for i in "${!ENTRIES[@]}"; do
    [[ $i -gt 0 ]] && printf ',\n'
    printf '%s' "${ENTRIES[$i]}"
  done
  printf '\n  ]\n}\n'
} > "${OUTPUT}"

echo ">> 完成：生成 ${COUNT} 个主题条目（跳过 ${SKIPPED} 个无资源目录）"
echo ">> 输出：${OUTPUT}"
echo ">> 大小：$(wc -c < "${OUTPUT}") 字节"

if command -v python3 >/dev/null 2>&1; then
  python3 "${SCRIPT_DIR}/augment_skin_catalog_effects.py" "${OUTPUT}"
else
  echo "!! 未找到 python3，已跳过加载／点赞／进度条能力合并。" >&2
fi
