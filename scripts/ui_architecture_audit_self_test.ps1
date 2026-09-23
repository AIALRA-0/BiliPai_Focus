[CmdletBinding()]
param(
    [string]$RepoRoot
)

$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
}
$audit = Join-Path $RepoRoot "scripts/ui_architecture_audit.ps1"
$probeRoot = Join-Path ([IO.Path]::GetTempPath()) ("bilipai-ui-audit-" + [guid]::NewGuid().ToString("N"))

function Invoke-Audit {
    param(
        [string]$SyntheticFile,
        [switch]$DesignSystemBuild
    )

    $auditArguments = @("-NoProfile", "-File", $audit, "-RepoRoot", $RepoRoot)
    if ($SyntheticFile) {
        if ($DesignSystemBuild) {
            $auditArguments += @("-SyntheticDesignSystemBuildFile", $SyntheticFile)
        } else {
            $auditArguments += @("-SyntheticFeatureFile", $SyntheticFile)
        }
    }

    $ErrorActionPreference = "Continue"
    $output = & powershell @auditArguments 2>&1
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = "Stop"
    return [pscustomobject]@{
        ExitCode = $exitCode
        Text = @($output) -join [Environment]::NewLine
    }
}

function Get-AuditMetric {
    param(
        [string]$AuditText,
        [string]$Metric
    )

    $match = [regex]::Match(
        $AuditText,
        ("(?im)\b{0}=(\d+)" -f [regex]::Escape($Metric))
    )
    if (-not $match.Success) {
        throw "Audit output has no metric: $Metric"
    }
    return [int]$match.Groups[1].Value
}

function Assert-AuditRejectsProbe {
    param(
        [string]$Name,
        [string]$Source,
        [string]$Metric,
        [string[]]$ExpectedPatterns = @(),
        [switch]$DesignSystemBuild
    )

    $extension = if ($DesignSystemBuild) { ".gradle.kts" } else { ".kt" }
    $probe = Join-Path $probeRoot ("$Name$extension")
    $Source | Set-Content -LiteralPath $probe -Encoding utf8

    $auditResult = Invoke-Audit -SyntheticFile $probe -DesignSystemBuild:$DesignSystemBuild
    if ($auditResult.ExitCode -eq 0) {
        throw "$Name probe unexpectedly passed.`n$($auditResult.Text)"
    }
    $expectedMetricValue = $baselineMetrics[$Metric] + 1
    $expectedGatePattern = "(?i)\b$Metric=$expectedMetricValue\s+exceeds baseline maximum"
    if ($auditResult.Text -notmatch $expectedGatePattern) {
        throw "$Name probe failed without the expected gate error '$expectedGatePattern'.`n$($auditResult.Text)"
    }
    foreach ($expectedPattern in $ExpectedPatterns) {
        if ($auditResult.Text -notmatch $expectedPattern) {
            throw "$Name probe failed without expected evidence '$expectedPattern'.`n$($auditResult.Text)"
        }
    }
    Write-Output ("SELF_TEST_{0}_OK" -f $Name.ToUpperInvariant())
}

try {
    New-Item -ItemType Directory -Path $probeRoot | Out-Null
    $baselineAudit = Invoke-Audit
    $baselineMetrics = @{}
    foreach ($metric in @(
        'style_feature',
        'local_feature',
        'renderer_feature',
        'ios_feature_callers',
        'vendor_component_feature',
        'design_system_boundary_violations'
    )) {
        $baselineMetrics[$metric] = Get-AuditMetric -AuditText $baselineAudit.Text -Metric $metric
    }

    Assert-AuditRejectsProbe -Name "style" -Metric 'style_feature' -Source @"
package com.android.purebilibili.feature.auditprobe

val illegalStyle = UiStyle.IOS
"@

    Assert-AuditRejectsProbe -Name "local" -Metric 'local_feature' -Source @"
package com.android.purebilibili.feature.auditprobe

val illegalLocal = LocalUiPreset.current
"@

    Assert-AuditRejectsProbe -Name "renderer" -Metric 'renderer_feature' -Source @"
package com.android.purebilibili.feature.auditprobe

import com.android.purebilibili.core.ui.renderer.material3.Material3ButtonRenderer
"@

    Assert-AuditRejectsProbe -Name "ios" -Metric 'ios_feature_callers' -Source @"
package com.android.purebilibili.feature.auditprobe

fun illegalIos() {
    IOSGroup()
}
"@

    Assert-AuditRejectsProbe -Name "vendor_import" -Metric 'vendor_component_feature' -ExpectedPatterns @(
        'VENDOR_COMPONENT_VIOLATION path=app/src/main/java/com/android/purebilibili/feature/__audit_probe__/IllegalStyleProbe\.kt evidence=Material3\.Button'
    ) -Source @"
package com.android.purebilibili.feature.auditprobe

import androidx.compose.material3.Button

fun illegalVendor() {
    Button(onClick = {}) {}
}
"@

    Assert-AuditRejectsProbe -Name "vendor_qualified_material3" -Metric 'vendor_component_feature' -ExpectedPatterns @(
        'VENDOR_COMPONENT_VIOLATION path=app/src/main/java/com/android/purebilibili/feature/__audit_probe__/IllegalStyleProbe\.kt evidence=Material3\.Scaffold'
    ) -Source @"
package com.android.purebilibili.feature.auditprobe

fun illegalVendor() {
    androidx.compose.material3.Scaffold {}
}
"@

    Assert-AuditRejectsProbe -Name "vendor_qualified_cupertino" -Metric 'vendor_component_feature' -ExpectedPatterns @(
        'VENDOR_COMPONENT_VIOLATION path=app/src/main/java/com/android/purebilibili/feature/__audit_probe__/IllegalStyleProbe\.kt evidence=Cupertino\.CupertinoSlider'
    ) -Source @"
package com.android.purebilibili.feature.auditprobe

fun illegalVendor() {
    io.github.alexzhirkevich.cupertino.CupertinoSlider(value = 0f, onValueChange = {})
}
"@

    Assert-AuditRejectsProbe -Name "vendor_qualified_miuix" -Metric 'vendor_component_feature' -ExpectedPatterns @(
        'VENDOR_COMPONENT_VIOLATION path=app/src/main/java/com/android/purebilibili/feature/__audit_probe__/IllegalStyleProbe\.kt evidence=Miuix\.Button'
    ) -Source @"
package com.android.purebilibili.feature.auditprobe

fun illegalVendor() {
    top.yukonga.miuix.kmp.basic.Button(onClick = {}) {}
}
"@

    Assert-AuditRejectsProbe -Name "vendor_material_omissions" -Metric 'vendor_component_feature' -ExpectedPatterns @(
        'VENDOR_COMPONENT_VIOLATION path=app/src/main/java/com/android/purebilibili/feature/__audit_probe__/IllegalStyleProbe\.kt evidence=.*Material3\.HorizontalDivider',
        'VENDOR_COMPONENT_VIOLATION path=app/src/main/java/com/android/purebilibili/feature/__audit_probe__/IllegalStyleProbe\.kt evidence=.*Material3\.Icon',
        'VENDOR_COMPONENT_VIOLATION path=app/src/main/java/com/android/purebilibili/feature/__audit_probe__/IllegalStyleProbe\.kt evidence=.*Material3\.ListItem',
        'VENDOR_COMPONENT_VIOLATION path=app/src/main/java/com/android/purebilibili/feature/__audit_probe__/IllegalStyleProbe\.kt evidence=.*Material3\.ScrollableTabRow',
        'VENDOR_COMPONENT_VIOLATION path=app/src/main/java/com/android/purebilibili/feature/__audit_probe__/IllegalStyleProbe\.kt evidence=.*Material3\.Snackbar',
        'VENDOR_COMPONENT_VIOLATION path=app/src/main/java/com/android/purebilibili/feature/__audit_probe__/IllegalStyleProbe\.kt evidence=.*Material3\.SnackbarHost',
        'VENDOR_COMPONENT_VIOLATION path=app/src/main/java/com/android/purebilibili/feature/__audit_probe__/IllegalStyleProbe\.kt evidence=.*Material3\.Text'
    ) -Source @"
package com.android.purebilibili.feature.auditprobe

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text

fun illegalVendor() {
    HorizontalDivider()
    Icon(imageVector = TODO(), contentDescription = null)
    ListItem(headlineContent = {})
    ScrollableTabRow(selectedTabIndex = 0) {}
    Snackbar {}
    SnackbarHost(hostState = TODO())
    Text("illegal")
}
"@

    Assert-AuditRejectsProbe -Name "vendor_material_wildcard" -Metric 'vendor_component_feature' -ExpectedPatterns @(
        'VENDOR_COMPONENT_VIOLATION path=app/src/main/java/com/android/purebilibili/feature/__audit_probe__/IllegalStyleProbe\.kt evidence=.*Material3\.Icon',
        'VENDOR_COMPONENT_VIOLATION path=app/src/main/java/com/android/purebilibili/feature/__audit_probe__/IllegalStyleProbe\.kt evidence=.*Material3\.ScrollableTabRow',
        'VENDOR_COMPONENT_VIOLATION path=app/src/main/java/com/android/purebilibili/feature/__audit_probe__/IllegalStyleProbe\.kt evidence=.*Material3\.Text'
    ) -Source @"
package com.android.purebilibili.feature.auditprobe

import androidx.compose.material3.*

fun illegalVendor() {
    Icon(imageVector = TODO(), contentDescription = null)
    ScrollableTabRow(selectedTabIndex = 0) {}
    Text("illegal")
}
"@

    Assert-AuditRejectsProbe -Name "design_system_gradle" -DesignSystemBuild -Metric 'design_system_boundary_violations' -Source @"
dependencies {
    implementation(project(":app"))
}
"@

    $cleanupAudit = Invoke-Audit
    if ($cleanupAudit.ExitCode -ne $baselineAudit.ExitCode -or $cleanupAudit.Text -ne $baselineAudit.Text) {
        throw "Audit did not recover to its pre-self-test result."
    }
    Write-Output "SELF_TEST_CLEANUP_OK"
}
finally {
    if (Test-Path -LiteralPath $probeRoot) {
        Remove-Item -LiteralPath $probeRoot -Recurse -Force
    }
}
