#Requires -RunAsAdministrator
<#
.SYNOPSIS
  Prefer corporate VPN DNS for internal hosts (split-DNS fix).
.NOTES
  Safe to re-run. Marker-based hosts entries can be removed with -Undo.
#>
param(
  [switch]$Undo
)

$ErrorActionPreference = 'Stop'
$hostsPath = "$env:SystemRoot\System32\drivers\etc\hosts"
$markerStart = '# CURSOR-VPN-DNS-FIX'
$markerEnd = '# END CURSOR-VPN-DNS-FIX'
$vpnDns = @('10.236.19.220', '10.236.17.13')

function Remove-HostsBlock {
  if (-not (Test-Path $hostsPath)) { return }
  $raw = Get-Content -Raw -Path $hostsPath
  $cleaned = [regex]::Replace($raw, "(?ms)$([regex]::Escape($markerStart)).*?$([regex]::Escape($markerEnd))\r?\n?", '')
  Set-Content -Path $hostsPath -Value $cleaned.TrimEnd() -Encoding ASCII -Force
}

if ($Undo) {
  Remove-HostsBlock
  Get-DnsClientNrptRule -ErrorAction SilentlyContinue |
    Where-Object { $_.DisplayName -like 'Cursor-VPN-*' } |
    ForEach-Object { Remove-DnsClientNrptRule -Name $_.Name -Force }
  try { Set-NetIPInterface -InterfaceAlias 'VPN' -InterfaceMetric 35 } catch {}
  try { Set-NetIPInterface -InterfaceAlias 'Ethernet' -InterfaceMetric 35 } catch {}
  Clear-DnsClientCache
  Write-Host 'UNDO complete (hosts NRPT metrics restored toward defaults).'
  exit 0
}

# 1) Interface metrics — VPN preferred
try { Set-NetIPInterface -InterfaceAlias 'VPN' -InterfaceMetric 1; Write-Host 'VPN metric = 1' } catch { Write-Host "WARN VPN metric: $_" }
try { Set-NetIPInterface -InterfaceAlias 'Ethernet' -InterfaceMetric 50; Write-Host 'Ethernet metric = 50' } catch { Write-Host "WARN Ethernet metric: $_" }

# 2) NRPT — ONLY .domain.internal (never .energo-pro.bg: that breaks vpncorp.* before VPN connects → RAS 868)
foreach ($namespace in @('.domain.internal')) {
  Get-DnsClientNrptRule -ErrorAction SilentlyContinue |
    Where-Object { $_.DisplayName -eq "Cursor-VPN-$namespace" -or $_.Namespace -contains $namespace } |
    ForEach-Object { Remove-DnsClientNrptRule -Name $_.Name -Force -ErrorAction SilentlyContinue }
  Add-DnsClientNrptRule -Namespace $namespace -NameServers $vpnDns -DisplayName "Cursor-VPN-$namespace"
  Write-Host "NRPT: $namespace -> $($vpnDns -join ', ')"
}
# Remove any prior Cursor NRPT for energo-pro.bg
Get-DnsClientNrptRule -ErrorAction SilentlyContinue |
  Where-Object { $_.DisplayName -like 'Cursor-VPN-*energo-pro*' -or ($_.Namespace -join ',') -match 'energo-pro' } |
  ForEach-Object {
    Write-Host "Removing unsafe NRPT $($_.DisplayName)"
    Remove-DnsClientNrptRule -Name $_.Name -Force
  }

# 3) hosts fallback (optional — NRPT is enough when it succeeds)
$hostsOk = $false
$block = @"
$markerStart
176.116.144.90 vpncorp.energo-pro.bg
10.236.19.126 apps.energo-pro.bg
10.236.19.102 git.domain.internal
$markerEnd
"@
for ($i = 1; $i -le 5; $i++) {
  try {
    Remove-HostsBlock
    $tmp = "$env:TEMP\hosts-cursor-write.txt"
    $raw = [System.IO.File]::ReadAllText($hostsPath)
    $cleaned = [regex]::Replace($raw, "(?ms)$([regex]::Escape($markerStart)).*?$([regex]::Escape($markerEnd))\r?\n?", '')
    [System.IO.File]::WriteAllText($tmp, ($cleaned.TrimEnd() + "`r`n`r`n" + $block + "`r`n"), [System.Text.Encoding]::ASCII)
    Copy-Item -Path $tmp -Destination $hostsPath -Force
    $hostsOk = $true
    Write-Host 'hosts: vpncorp (public) + apps + git added'
    break
  } catch {
    Write-Host "hosts attempt $i/5 locked or failed: $($_.Exception.Message)"
    Start-Sleep -Seconds 1
  }
}
if (-not $hostsOk) {
  Write-Host 'WARN: hosts skipped (file locked). NRPT + metrics already applied — usually enough.'
  Write-Host 'Close apps that edit hosts (antivirus, VPN client, Notepad) and re-run if needed.'
}

Clear-DnsClientCache
ipconfig /flushdns | Out-Null

Write-Host ''
Write-Host '=== Verify ==='
foreach ($n in @('git.domain.internal', 'apps.energo-pro.bg')) {
  try {
    $ip = (Resolve-DnsName $n -Type A -ErrorAction Stop | Where-Object Type -eq 'A' | Select-Object -First 1).IPAddress
    Write-Host "OK $n -> $ip"
  } catch {
    Write-Host "FAIL $n : $_"
  }
}
Write-Host ''
Write-Host '=== NRPT present? ==='
Get-DnsClientNrptRule -ErrorAction SilentlyContinue |
  Where-Object { $_.DisplayName -like 'Cursor-VPN-*' } |
  Select-Object DisplayName, Namespace, NameServers |
  Format-List | Out-String | Write-Host

Write-Host 'Done. Re-open browser tabs to apps.energo-pro.bg / git.domain.internal.'
Write-Host 'Undo later: powershell -ExecutionPolicy Bypass -File this-script.ps1 -Undo'
pause
