# theme_simplification_inventory.ps1
# Phase 0 baseline audit: repeatable inventory command for the frontend
# architecture & theme simplification plan (FRONTEND_ARCHITECTURE_THEME_SIMPLIFICATION_PLAN.md).
#
# Usage:
#   powershell -File scripts/theme_simplification_inventory.ps1
#     -> prints per-category file counts and import occurrences, writes docs/theme_simplification_inventory/*.csv
#   powershell -File scripts/theme_simplification_inventory.ps1 -DumpLines
#     -> additionally dumps every matched import line (path, line, text)
#
# Scope: production Kotlin sources under app/src/main and design-system/src/main.
# Categories mirror the static gates described in plan section 6.4.

[CmdletBinding()]
param(
    [string]$RepoRoot,
    [switch]$DumpLines,
    [string]$OutDir = "docs/theme_simplification_inventory"
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
}
$RepoRoot = [IO.Path]::GetFullPath($RepoRoot)

function Get-RelativePath {
    param([string]$Path)
    $root = $RepoRoot.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    $full = [IO.Path]::GetFullPath($Path)
    if (-not $full.StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Path is outside RepoRoot: $full"
    }
    return $full.Substring($root.Length).Replace('\', '/')
}

function Get-KotlinFiles {
    param([string]$Root)
    if (-not (Test-Path -LiteralPath $Root -PathType Container)) { return @() }
    return @(Get-ChildItem -LiteralPath $Root -Recurse -File -Filter "*.kt")
}

# category name -> regex (applied line-wise to imports / symbols)
$categoryNames = @(
    "cupertino_all",
    "cupertino_icons",
    "cupertino_adaptive",
    "material_icons",
    "miuix_icons",
    "ios_symbol",
    "theme_enum",
    "legacy_theme_key"
)
$categoryPatterns = @{
    "cupertino_all"      = '^\s*import\s+io\.github\.alexzhirkevich\.cupertino\S*'
    "cupertino_icons"    = '^\s*import\s+io\.github\.alexzhirkevich\.cupertino\.icons\S*'
    "cupertino_adaptive" = '^\s*import\s+io\.github\.alexzhirkevich\.cupertino\.adaptive\S*'
    "material_icons"     = '^\s*import\s+androidx\.compose\.material\.icons\S*'
    "miuix_icons"        = '^\s*import\s+top\.yukonga\.miuix\.kmp\.icon\S*'
    "ios_symbol"         = '\b(?:iOS|Ios)[A-Z][A-Za-z0-9_]*'
    "theme_enum"         = '\b(?:UiPreset|AndroidNativeVariant|UiStyle|AppThemeSelection|PresetPrimitiveRenderer)\b'
    "legacy_theme_key"   = '\b(?:KEY_UI_PRESET|KEY_ANDROID_NATIVE_VARIANT|ui_preset|android_native_variant_v1)\b'
}

$sourceRoots = @(
    (Join-Path $RepoRoot "app/src/main"),
    (Join-Path $RepoRoot "design-system/src/main")
)

$allFiles = @()
foreach ($root in $sourceRoots) {
    $allFiles += @(Get-KotlinFiles $root)
}

# per-file per-category counts: path -> hashtable(category -> count)
$fileHits = @{}
$lineDump = [Collections.Generic.List[pscustomobject]]::new()

foreach ($file in $allFiles) {
    $rel = Get-RelativePath $file.FullName
    $text = Get-Content -Raw -LiteralPath $file.FullName
    if ([string]::IsNullOrWhiteSpace($text)) { continue }
    $perCategory = @{}
    foreach ($name in $categoryNames) {
        $matches = [regex]::Matches($text, $categoryPatterns[$name], [Text.RegularExpressions.RegexOptions]::Multiline)
        $perCategory[$name] = $matches.Count
        if ($DumpLines -and $matches.Count -gt 0) {
            $lineNo = 0
            foreach ($line in ($text -split "`r?`n")) {
                $lineNo++
                if ($line -match $categoryPatterns[$name]) {
                    $lineDump.Add([pscustomobject]@{
                        category = $name
                        path     = $rel
                        line     = $lineNo
                        text     = $line.Trim()
                    })
                }
            }
        }
    }
    if (($perCategory.Values | Measure-Object -Sum).Sum -gt 0) {
        $fileHits[$rel] = $perCategory
    }
}

# summary
Write-Output "=== THEME_SIMPLIFICATION_INVENTORY ==="
Write-Output ("SCANNED_FILES={0}" -f @($allFiles).Count)
foreach ($name in $categoryNames) {
    $files = @($fileHits.GetEnumerator() | Where-Object { $_.Value[$name] -gt 0 })
    $lines = 0
    foreach ($kv in $files) { $lines += $kv.Value[$name] }
    Write-Output ("{0}: FILES={1} OCCURRENCES={2}" -f $name, $files.Count, $lines)
}

# per-category CSV (path, occurrences)
$outDirPath = Join-Path $RepoRoot $OutDir
if (-not (Test-Path -LiteralPath $outDirPath -PathType Container)) {
    New-Item -ItemType Directory -Path $outDirPath | Out-Null
}
foreach ($name in $categoryNames) {
    $rows = @($fileHits.GetEnumerator() | Where-Object { $_.Value[$name] -gt 0 } | ForEach-Object {
        [pscustomobject]@{ path = $_.Key; occurrences = $_.Value[$name] }
    } | Sort-Object path)
    $csvPath = Join-Path $outDirPath ("{0}.csv" -f $name)
    $rows | Export-Csv -LiteralPath $csvPath -NoTypeInformation -Encoding utf8
    Write-Output ("WROTE {0}" -f (Get-RelativePath $csvPath))
}

if ($DumpLines) {
    $dumpPath = Join-Path $outDirPath "import_lines.csv"
    $lineDump | Export-Csv -LiteralPath $dumpPath -NoTypeInformation -Encoding utf8
    Write-Output ("WROTE {0} ({1} lines)" -f (Get-RelativePath $dumpPath), $lineDump.Count)
}

Write-Output "INVENTORY_DONE"
