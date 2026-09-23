[CmdletBinding()]
param(
    [string]$RepoRoot,
    [string]$BaselinePath = "docs/UI_ARCHITECTURE_BASELINE.json",
    [string]$RegistryPath = "docs/UI_COMPONENT_REGISTRY.csv",
    [string]$ExceptionsPath = "docs/UI_COMPONENT_EXCEPTIONS.csv",
    [string]$SyntheticFeatureFile,
    [string]$SyntheticDesignSystemBuildFile,
    [switch]$UpdateRegistry
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
}

function Resolve-RepoPath {
    param([string]$Path)

    if ([IO.Path]::IsPathRooted($Path)) {
        return [IO.Path]::GetFullPath($Path)
    }
    return [IO.Path]::GetFullPath((Join-Path $RepoRoot $Path))
}

function Get-RelativePath {
    param([string]$Path)

    $root = [IO.Path]::GetFullPath($RepoRoot).TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    $full = [IO.Path]::GetFullPath($Path)
    if (-not $full.StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Path is outside RepoRoot: $full"
    }
    return $full.Substring($root.Length).Replace('\', '/')
}

function Get-KotlinSources {
    param([string]$Root)

    if (-not (Test-Path -LiteralPath $Root -PathType Container)) {
        return @()
    }

    return @(Get-ChildItem -LiteralPath $Root -Recurse -File -Filter "*.kt" | ForEach-Object {
        [pscustomobject]@{
            Path = Get-RelativePath $_.FullName
            Text = Get-Content -Raw -LiteralPath $_.FullName
        }
    })
}

function Get-FirstMatch {
    param(
        [string]$Text,
        [string]$Pattern
    )

    $match = [regex]::Match($Text, $Pattern, [Text.RegularExpressions.RegexOptions]::Multiline)
    if ($match.Success) { return $match.Value.Trim() }
    return $null
}

function Get-SourceBody {
    param([string]$Text)

    $body = [regex]::Replace($Text, '(?m)^\s*(?:package|import)\s+[^\r\n]*\r?$', '')
    $body = [regex]::Replace($body, '(?m)^\s*//[^\r\n]*\r?$', '')
    return [regex]::Replace(
        $body,
        '(?m)^(\s*(?:(?:public|private|internal|protected|override|suspend|inline|operator|infix|tailrec|external)\s+)*fun\s+)[A-Za-z_][A-Za-z0-9_]*',
        '${1}__DECLARATION__'
    )
}

function Test-ExecutableReference {
    param(
        [string]$Text,
        [string]$Evidence,
        [switch]$RequireCall
    )

    if ([string]::IsNullOrWhiteSpace($Evidence)) { return $false }
    $body = Get-SourceBody $Text
    $escaped = [regex]::Escape($Evidence)
    $pattern = if ($RequireCall) { "\b$escaped\s*\(" } else { "\b$escaped\b" }
    return $body -cmatch $pattern
}

function Get-VendorComponentEvidence {
    param([pscustomobject]$Source)

    $materialComponents = @(
        'AlertDialog', 'BasicAlertDialog', 'AssistChip', 'Badge', 'Button', 'Card',
        'CenterAlignedTopAppBar', 'Checkbox', 'CircularProgressIndicator',
        'DropdownMenu', 'DropdownMenuItem', 'ElevatedButton', 'ElevatedCard',
        'FilledIconButton', 'FilledTonalButton', 'FilterChip', 'FloatingActionButton',
        'HorizontalDivider', 'Icon', 'IconButton', 'LinearProgressIndicator', 'ListItem', 'LoadingIndicator', 'ModalBottomSheet',
        'ModalNavigationDrawer', 'NavigationBar', 'NavigationBarItem',
        'NavigationDrawerItem', 'NavigationRail', 'NavigationRailItem', 'OutlinedButton',
        'OutlinedCard', 'OutlinedIconButton', 'OutlinedTextField', 'PrimaryScrollableTabRow',
        'PrimaryTabRow', 'PullToRefreshBox', 'RadioButton', 'Scaffold', 'ScrollableTabRow', 'SegmentedButton',
        'SingleChoiceSegmentedButtonRow', 'Slider', 'SmallFloatingActionButton', 'Snackbar', 'SnackbarHost', 'Surface',
        'SuggestionChip', 'Switch', 'Tab', 'Text', 'TextButton', 'TextField', 'TopAppBar'
    )
    $cupertinoComponents = @(
        'CupertinoActivityIndicator', 'CupertinoAlertDialog', 'CupertinoButton',
        'CupertinoCheckbox', 'CupertinoNavigationBar', 'CupertinoScaffold',
        'CupertinoSlider', 'CupertinoSwitch', 'CupertinoTextField'
    )
    $miuixComponents = @(
        'AlertDialog', 'Badge', 'Button', 'Checkbox', 'FloatingActionButton',
        'NavigationBar', 'NavigationBarItem', 'NavigationRail', 'NavigationRailItem',
        'Scaffold', 'Slider', 'Switch', 'TabRow', 'TextField'
    )

    $body = Get-SourceBody $Source.Text
    $evidence = [Collections.Generic.List[string]]::new()
    $imports = [regex]::Matches($Source.Text, '(?m)^\s*import\s+([^\r\n]+)\r?$')
    $materialWildcard = $false
    foreach ($importMatch in $imports) {
        $importText = $importMatch.Groups[1].Value.Trim()
        if ($importText -eq 'androidx.compose.material3.*') {
            $materialWildcard = $true
            continue
        }

        $materialMatch = [regex]::Match(
            $importText,
            '^androidx\.compose\.material3(?:\.pulltorefresh)?\.([A-Za-z][A-Za-z0-9]*)(?:\s+as\s+([A-Za-z][A-Za-z0-9]*))?$'
        )
        if ($materialMatch.Success -and $materialMatch.Groups[1].Value -in $materialComponents) {
            $symbol = $materialMatch.Groups[1].Value
            $localName = if ($materialMatch.Groups[2].Success) {
                $materialMatch.Groups[2].Value
            } else {
                $symbol
            }
            if ($body -cmatch ("\b{0}\s*(?:\(|\{{)" -f [regex]::Escape($localName))) {
                $evidence.Add("Material3.$symbol")
            }
            continue
        }

        $cupertinoMatch = [regex]::Match(
            $importText,
            '^io\.github\.alexzhirkevich\.cupertino\.([A-Za-z][A-Za-z0-9]*)(?:\s+as\s+([A-Za-z][A-Za-z0-9]*))?$'
        )
        if ($cupertinoMatch.Success -and $cupertinoMatch.Groups[1].Value -in $cupertinoComponents) {
            $symbol = $cupertinoMatch.Groups[1].Value
            $localName = if ($cupertinoMatch.Groups[2].Success) {
                $cupertinoMatch.Groups[2].Value
            } else {
                $symbol
            }
            if ($body -cmatch ("\b{0}\s*(?:\(|\{{)" -f [regex]::Escape($localName))) {
                $evidence.Add("Cupertino.$symbol")
            }
            continue
        }

        $miuixMatch = [regex]::Match(
            $importText,
            '^top\.yukonga\.miuix\.kmp\.(?:basic|extra|overlay)\.([A-Za-z][A-Za-z0-9]*)(?:\s+as\s+([A-Za-z][A-Za-z0-9]*))?$'
        )
        if ($miuixMatch.Success -and $miuixMatch.Groups[1].Value -in $miuixComponents) {
            $symbol = $miuixMatch.Groups[1].Value
            $localName = if ($miuixMatch.Groups[2].Success) {
                $miuixMatch.Groups[2].Value
            } else {
                $symbol
            }
            if ($body -cmatch ("\b{0}\s*(?:\(|\{{)" -f [regex]::Escape($localName))) {
                $evidence.Add("Miuix.$symbol")
            }
        }
    }

    if ($materialWildcard) {
        foreach ($symbol in $materialComponents) {
            if ($body -cmatch ("\b{0}\s*(?:\(|\{{)" -f [regex]::Escape($symbol))) {
                $evidence.Add("Material3.$symbol")
            }
        }
    }

    foreach ($symbol in $materialComponents) {
        if ($body -cmatch ("\bandroidx\.compose\.material3(?:\.pulltorefresh)?\.{0}\s*(?:\(|\{{)" -f [regex]::Escape($symbol))) {
            $evidence.Add("Material3.$symbol")
        }
    }

    foreach ($symbol in $cupertinoComponents) {
        if ($body -cmatch ("\bio\.github\.alexzhirkevich\.cupertino\.{0}\s*(?:\(|\{{)" -f [regex]::Escape($symbol))) {
            $evidence.Add("Cupertino.$symbol")
        }
    }

    foreach ($symbol in $miuixComponents) {
        if ($body -cmatch ("\btop\.yukonga\.miuix\.kmp\.(?:basic|extra|overlay)\.{0}\s*(?:\(|\{{)" -f [regex]::Escape($symbol))) {
            $evidence.Add("Miuix.$symbol")
        }
    }

    return @($evidence | Sort-Object -Unique)
}

function Get-RegistryRow {
    param(
        [pscustomobject]$Source,
        [hashtable]$Exceptions
    )

    $body = Get-SourceBody $Source.Text
    $appComponent = Get-FirstMatch $body '\bApp[A-Z][A-Za-z0-9_]*(?=\s*\()'
    if ($appComponent) {
        return [pscustomobject]@{
            path = $Source.Path
            mapping_kind = "APP_COMPONENT"
            evidence = $appComponent
            implementation_status = "CURRENT"
        }
    }

    $sharedToken = Get-FirstMatch $body '\b(?:App[A-Za-z0-9_]*(?:Tokens|Metrics|Typography|Spacing|Shapes|Icons|Motion)|LocalApp[A-Za-z0-9_]+|MaterialTheme\.(?:colorScheme|typography|shapes))\b'
    if ($sharedToken) {
        return [pscustomobject]@{
            path = $Source.Path
            mapping_kind = "SHARED_TOKEN"
            evidence = $sharedToken
            implementation_status = "CURRENT"
        }
    }

    if ($Exceptions.ContainsKey($Source.Path)) {
        return [pscustomobject]@{
            path = $Source.Path
            mapping_kind = "EXCEPTION"
            evidence = $Exceptions[$Source.Path]
            implementation_status = "REVIEWED_EXCEPTION"
        }
    }

    $targetComponent = switch -Regex ($Source.Path) {
        '(?:TextField|Input)' { "AppTextField"; break }
        '(?:Switch)' { "AppSwitch"; break }
        '(?:Slider)' { "AppSlider"; break }
        '(?:Segmented)' { "AppSegmentedControl"; break }
        '(?:Refresh)' { "AppPullRefresh"; break }
        '(?:Progress|Loading)' { "AppProgress"; break }
        '(?:Dialog|Sheet|Menu)' { "AppSheet/AppDialog/AppMenu"; break }
        '(?:Button|Action|Controls)' { "AppButton/AppIconButton"; break }
        '(?:Screen|Host|Content|Layout)' { "AppScaffold/AppSurface"; break }
        default { "AppSurface/AppText/AppIcon" }
    }

    return [pscustomobject]@{
        path = $Source.Path
        mapping_kind = "APP_COMPONENT"
        evidence = "planned:$targetComponent"
        implementation_status = "PLANNED"
    }
}

function Get-CountByPattern {
    param(
        [object[]]$Sources,
        [string]$Pattern
    )

    return @($Sources | Where-Object { $_.Text -match $Pattern }).Count
}

$RepoRoot = [IO.Path]::GetFullPath($RepoRoot)
$baselineFile = Resolve-RepoPath $BaselinePath
$registryFile = Resolve-RepoPath $RegistryPath
$exceptionsFile = Resolve-RepoPath $ExceptionsPath

if (-not (Test-Path -LiteralPath $baselineFile -PathType Leaf)) {
    throw "Baseline not found: $baselineFile"
}

$baseline = Get-Content -Raw -LiteralPath $baselineFile | ConvertFrom-Json
$exceptions = @{}
$vendorComponentExceptions = [Collections.Generic.HashSet[string]]::new(
    [StringComparer]::OrdinalIgnoreCase
)
if (Test-Path -LiteralPath $exceptionsFile -PathType Leaf) {
    foreach ($row in @(Import-Csv -LiteralPath $exceptionsFile)) {
        if ([string]::IsNullOrWhiteSpace($row.path)) { continue }
        if ([string]::IsNullOrWhiteSpace($row.rationale)) {
            throw "Exception has no rationale: $($row.path)"
        }
        $normalizedExceptionPath = $row.path.Replace('\', '/')
        $exceptions[$normalizedExceptionPath] = "$($row.scope): $($row.rationale)"
        if ($row.scope -eq 'VENDOR_COMPONENT') {
            [void]$vendorComponentExceptions.Add($normalizedExceptionPath)
        }
    }
}

$productionRoot = Join-Path $RepoRoot "app/src/main/java"
$featureRoot = Join-Path $RepoRoot "app/src/main/java/com/android/purebilibili/feature"
$testRoots = @(
    (Join-Path $RepoRoot "app/src/test"),
    (Join-Path $RepoRoot "design-system/src/test")
)
$coreUiRoot = Join-Path $RepoRoot "app/src/main/java/com/android/purebilibili/core/ui"
$designSystemRoot = Join-Path $RepoRoot "design-system"
$designSystemBuildFile = Join-Path $designSystemRoot "build.gradle.kts"

$production = Get-KotlinSources $productionRoot
$features = @($production | Where-Object { $_.Path -like "app/src/main/java/com/android/purebilibili/feature/*" })
$tests = @($testRoots | ForEach-Object { Get-KotlinSources $_ })
$coreUi = Get-KotlinSources $coreUiRoot
$designSystem = Get-KotlinSources $designSystemRoot
$designSystemBuildText = if (Test-Path -LiteralPath $designSystemBuildFile -PathType Leaf) {
    Get-Content -Raw -LiteralPath $designSystemBuildFile
} else {
    ''
}

if ($SyntheticDesignSystemBuildFile) {
    $syntheticBuildPath = Resolve-RepoPath $SyntheticDesignSystemBuildFile
    if (-not (Test-Path -LiteralPath $syntheticBuildPath -PathType Leaf)) {
        throw "Synthetic design-system build file not found: $syntheticBuildPath"
    }
    $designSystemBuildText += "`n" + (Get-Content -Raw -LiteralPath $syntheticBuildPath)
}

if ($SyntheticFeatureFile) {
    $syntheticPath = Resolve-RepoPath $SyntheticFeatureFile
    if (-not (Test-Path -LiteralPath $syntheticPath -PathType Leaf)) {
        throw "Synthetic feature file not found: $syntheticPath"
    }
    $synthetic = [pscustomobject]@{
        Path = "app/src/main/java/com/android/purebilibili/feature/__audit_probe__/IllegalStyleProbe.kt"
        Text = Get-Content -Raw -LiteralPath $syntheticPath
    }
    $production = @($production) + $synthetic
    $features = @($features) + $synthetic
}

$featureUi = @($features | Where-Object {
    $_.Text -match '(?m)^\s*@Composable\b|^\s*import\s+androidx\.compose\.'
} | Sort-Object Path)

$registryRows = @($featureUi | ForEach-Object { Get-RegistryRow $_ $exceptions })
if ($UpdateRegistry) {
    $registryDirectory = Split-Path -Parent $registryFile
    if (-not (Test-Path -LiteralPath $registryDirectory -PathType Container)) {
        New-Item -ItemType Directory -Path $registryDirectory | Out-Null
    }
    $registryRows | Export-Csv -LiteralPath $registryFile -NoTypeInformation -Encoding utf8
    Write-Output "REGISTRY_UPDATED=$RegistryPath"
}

$registered = @()
if (Test-Path -LiteralPath $registryFile -PathType Leaf) {
    $registered = @(Import-Csv -LiteralPath $registryFile)
}
$scannedPaths = @($registryRows.path)
$registeredPaths = @($registered.path)
$unregisteredPaths = @($scannedPaths | Where-Object { $_ -notin $registeredPaths })
$stalePaths = @($registeredPaths | Where-Object { $_ -notin $scannedPaths })
$featureByPath = @{}
foreach ($source in $features) { $featureByPath[$source.Path] = $source }
$registryEvidenceViolations = [Collections.Generic.List[string]]::new()
foreach ($row in $registered) {
    if (-not $featureByPath.ContainsKey($row.path)) { continue }
    $source = $featureByPath[$row.path]
    if ($row.mapping_kind -eq 'APP_COMPONENT' -and $row.implementation_status -eq 'CURRENT') {
        if (-not (Test-ExecutableReference $source.Text $row.evidence -RequireCall)) {
            $registryEvidenceViolations.Add(
                "$($row.path) does not call declared component $($row.evidence)"
            )
        }
    } elseif ($row.mapping_kind -eq 'SHARED_TOKEN' -and $row.implementation_status -eq 'CURRENT') {
        if (-not (Test-ExecutableReference $source.Text $row.evidence)) {
            $registryEvidenceViolations.Add(
                "$($row.path) does not use declared token $($row.evidence)"
            )
        }
    } elseif ($row.mapping_kind -eq 'EXCEPTION') {
        if ($row.implementation_status -ne 'REVIEWED_EXCEPTION') {
            $registryEvidenceViolations.Add(
                "$($row.path) exception is not explicitly REVIEWED_EXCEPTION"
            )
        }
    }
}

$stylePattern = '\b(?:UiPreset|AndroidNativeVariant|UiStyle)\b'
$localPattern = '\b(?:LocalUiPreset|LocalAndroidNativeVariant|LocalUiStyle)\b'
$iosPattern = '\b(?:IOSSectionTitle|IOSGroup|IOSSwitchItem|IOSSliderPreference|IOSClickableItem|IOSDivider|IOSGridItem|IOSSearchBar|IOSAdaptiveTextField|IOSAlertDialog|IOSDialogAction|IOSModalBottomSheet|IOSDragHandle|IOSSlidingSegmentedControl|IOSSlidingSegmentedSetting)\b'
$rendererPattern = '(?m)^\s*import\s+[^\r\n]*\.renderer(?:\.|\b)|\bresolvePresetPrimitiveRenderer\b|\bLocalAppRenderers\b|\bAppRenderers\b|\b(?:Md3|Miuix|IOS|Ios|Material3?|Cupertino|Preset|AppUi|AppTheme)[A-Za-z0-9_]*Renderer\b'
$reverseDependencyPattern = '(?m)^\s*import\s+com\.android\.purebilibili\.(?:feature(?:\.|\b)|core\.store(?:\.|\b))'
$relatedTestPattern = 'UiPreset|AndroidNativeVariant|UiStyle|PresetPrimitiveRenderer|supportsIndependentLiquidGlass|Adaptive(?:Scaffold|TopAppBar|Navigation|Loading|PullToRefresh|ListVisual)|IOS(?:Group|SwitchItem|SliderPreference|AlertDialog|ModalBottomSheet|AdaptiveTextField)|App(?:Surface|Shape|Motion|Typography|Icon).*Token|App(?:PlayerChromeProfile|EffectCapability|TopTabPresentation|SemanticVisual|SemanticIcon|Preference|SegmentedControl|SearchField|SearchEntry|PullRefresh)'
$designSystemBoundaryPattern = '(?m)^\s*import\s+com\.android\.purebilibili\.(?:feature|core\.store|data|network|plugin)(?:\.|\b)'
$designSystemGradleBoundaryPattern = '(?m)\b(?:api|implementation|compileOnly|runtimeOnly|[A-Za-z][A-Za-z0-9]*(?:Api|Implementation|CompileOnly|RuntimeOnly))\s*\(\s*project\s*\(\s*(?:path\s*=\s*)?["'']:(?:app|settings(?:-core)?|network(?:-core)?|plugin[^"'']*|data[^"'']*)["'']\s*\)'

$vendorComponentOffenders = @($features | ForEach-Object {
    $evidence = @(Get-VendorComponentEvidence $_)
    if ($evidence.Count -gt 0 -and -not $vendorComponentExceptions.Contains($_.Path)) {
        [pscustomobject]@{
            Path = $_.Path
            Evidence = $evidence -join ', '
        }
    }
})
$vendorComponentExceptionCount = @($features | Where-Object {
    $vendorComponentExceptions.Contains($_.Path) -and @(Get-VendorComponentEvidence $_).Count -gt 0
}).Count

$relatedTests = @($tests | Where-Object { $_.Text -match $relatedTestPattern })
$relatedTestCases = 0
foreach ($test in $relatedTests) {
    $relatedTestCases += [regex]::Matches($test.Text, '(?m)^\s*@Test\s*(?:\r?\n|$)').Count
}

$metrics = [ordered]@{
    production_kotlin = @($production).Count
    style_production = Get-CountByPattern $production $stylePattern
    style_feature = Get-CountByPattern $features $stylePattern
    local_feature = Get-CountByPattern $features $localPattern
    ios_feature_callers = Get-CountByPattern $features $iosPattern
    renderer_feature = Get-CountByPattern $features $rendererPattern
    vendor_component_feature = @($vendorComponentOffenders).Count
    vendor_component_exceptions = $vendorComponentExceptionCount
    core_ui_reverse_dependencies = Get-CountByPattern $coreUi $reverseDependencyPattern
    design_system_kotlin_boundary_violations = Get-CountByPattern $designSystem $designSystemBoundaryPattern
    design_system_gradle_boundary_violations = [regex]::Matches(
        $designSystemBuildText,
        $designSystemGradleBoundaryPattern
    ).Count
    related_test_files = @($relatedTests).Count
    related_test_cases = $relatedTestCases
    feature_ui_files = @($featureUi).Count
    registry_rows = @($registered).Count
    registry_missing = @($registered | Where-Object { $_.mapping_kind -eq "MISSING" }).Count
    registry_evidence_violations = $registryEvidenceViolations.Count
}
$metrics["design_system_boundary_violations"] =
    $metrics.design_system_kotlin_boundary_violations +
    $metrics.design_system_gradle_boundary_violations

$failures = [Collections.Generic.List[string]]::new()
function Assert-Maximum {
    param([string]$Name, [int]$Actual, [int]$Maximum)
    if ($Actual -gt $Maximum) { $failures.Add("$Name=$Actual exceeds baseline maximum $Maximum") }
}
function Assert-Minimum {
    param([string]$Name, [int]$Actual, [int]$Minimum)
    if ($Actual -lt $Minimum) { $failures.Add("$Name=$Actual is below baseline minimum $Minimum") }
}

Assert-Maximum "style_production" $metrics.style_production $baseline.gates.style_production_max
Assert-Maximum "style_feature" $metrics.style_feature 0
Assert-Maximum "local_feature" $metrics.local_feature 0
Assert-Maximum "ios_feature_callers" $metrics.ios_feature_callers 0
Assert-Maximum "renderer_feature" $metrics.renderer_feature 0
Assert-Maximum "vendor_component_feature" $metrics.vendor_component_feature ([int]$baseline.gates.vendor_component_feature_max)
Assert-Maximum "core_ui_reverse_dependencies" $metrics.core_ui_reverse_dependencies $baseline.gates.core_ui_reverse_dependencies_max
Assert-Maximum "design_system_boundary_violations" $metrics.design_system_boundary_violations 0
Assert-Maximum "registry_missing" $metrics.registry_missing $baseline.gates.registry_missing_max
Assert-Maximum "registry_evidence_violations" $metrics.registry_evidence_violations 0
Assert-Minimum "related_test_files" $metrics.related_test_files $baseline.gates.related_test_files_min
Assert-Minimum "related_test_cases" $metrics.related_test_cases $baseline.gates.related_test_cases_min

if ($unregisteredPaths.Count -gt 0) {
    $failures.Add("registry has $($unregisteredPaths.Count) unregistered scanned file(s): $($unregisteredPaths -join ', ')")
}
if ($stalePaths.Count -gt 0) {
    $failures.Add("registry has $($stalePaths.Count) stale file(s): $($stalePaths -join ', ')")
}
if ($registeredPaths.Count -ne (@($registeredPaths | Sort-Object -Unique).Count)) {
    $failures.Add("registry contains duplicate paths")
}
$allowedMappingKinds = @("APP_COMPONENT", "SHARED_TOKEN", "EXCEPTION")
foreach ($row in $registered) {
    if ($row.mapping_kind -notin $allowedMappingKinds) {
        $failures.Add("registry has invalid mapping_kind for $($row.path): $($row.mapping_kind)")
    }
    if ([string]::IsNullOrWhiteSpace($row.evidence)) {
        $failures.Add("registry has empty evidence for $($row.path)")
    }
    if ([string]::IsNullOrWhiteSpace($row.implementation_status)) {
        $failures.Add("registry has empty implementation_status for $($row.path)")
    }
}
Write-Output ("R1 PRODUCTION_KOTLIN={0} STYLE_PRODUCTION={1} STYLE_FEATURE={2} LOCAL_FEATURE={3}" -f $metrics.production_kotlin, $metrics.style_production, $metrics.style_feature, $metrics.local_feature)
Write-Output ("R2 FEATURE_FILES={0} REGISTERED={1} MISSING={2} UNREGISTERED={3} STALE={4}" -f $metrics.feature_ui_files, $metrics.registry_rows, $metrics.registry_missing, $unregisteredPaths.Count, $stalePaths.Count)
Write-Output ("R3 IOS_FEATURE_CALLERS={0} RENDERER_FEATURE={1}" -f $metrics.ios_feature_callers, $metrics.renderer_feature)
Write-Output ("R4 CORE_UI_REVERSE_DEPENDENCIES={0}" -f $metrics.core_ui_reverse_dependencies)
Write-Output ("R5 DESIGN_SYSTEM_BOUNDARY_VIOLATIONS={0} DESIGN_SYSTEM_KOTLIN_BOUNDARY_VIOLATIONS={1} DESIGN_SYSTEM_GRADLE_BOUNDARY_VIOLATIONS={2}" -f $metrics.design_system_boundary_violations, $metrics.design_system_kotlin_boundary_violations, $metrics.design_system_gradle_boundary_violations)
Write-Output ("R6 RELATED_TEST_FILES={0} RELATED_TEST_CASES={1}" -f $metrics.related_test_files, $metrics.related_test_cases)
Write-Output ("R7 FEATURE_STYLE_TARGET_GAP={0} FEATURE_LOCAL_TARGET_GAP={1} IOS_TARGET_GAP={2}" -f $metrics.style_feature, $metrics.local_feature, $metrics.ios_feature_callers)
Write-Output ("R8 REGISTRY_TARGET_GAP={0}" -f $metrics.registry_missing)
Write-Output ("R9 VENDOR_COMPONENT_FEATURE={0} VENDOR_COMPONENT_EXCEPTIONS={1}" -f $metrics.vendor_component_feature, $metrics.vendor_component_exceptions)
Write-Output ("R10 REGISTRY_EVIDENCE_VIOLATIONS={0}" -f $metrics.registry_evidence_violations)
Write-Output ("GATE STYLE_FEATURE_MAX=0 LOCAL_FEATURE_MAX=0 IOS_FEATURE_CALLERS_MAX=0 RENDERER_FEATURE_MAX=0 VENDOR_COMPONENT_FEATURE_MAX={0}" -f $baseline.gates.vendor_component_feature_max)

if ($failures.Count -gt 0) {
    foreach ($offender in $vendorComponentOffenders) {
        Write-Output ("VENDOR_COMPONENT_VIOLATION path={0} evidence={1}" -f $offender.Path, $offender.Evidence)
    }
    foreach ($violation in $registryEvidenceViolations) {
        Write-Output ("REGISTRY_EVIDENCE_VIOLATION {0}" -f $violation)
    }
    foreach ($failure in $failures) { Write-Output ("AUDIT_ERROR {0}" -f $failure) }
    Write-Output "AUDIT_FAILED"
    exit 1
}

Write-Output "AUDIT_OK"
