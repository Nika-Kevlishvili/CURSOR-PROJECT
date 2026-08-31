#Requires -RunAsAdministrator
$ErrorActionPreference = 'Continue'
Write-Host 'Fixing chicken-and-egg: VPN hostname must resolve on PUBLIC DNS before connect.'

# Remove NRPT that forces ALL energo-pro.bg to unreachable corp DNS while VPN is down
Get-DnsClientNrptRule -ErrorAction SilentlyContinue |
  Where-Object { $_.DisplayName -like 'Cursor-VPN-*' } |
  ForEach-Object {
    Write-Host "Removing NRPT $($_.DisplayName) namespace=$($_.Namespace -join ',')"
    Remove-DnsClientNrptRule -Name $_.Name -Force
  }

# Keep only .domain.internal on corp DNS (not needed before VPN; harmless if VPN down for those names)
# Do NOT add .energo-pro.bg NRPT — that breaks vpncorp.energo-pro.bg pre-connect.

$hostsPath = "$env:SystemRoot\System32\drivers\etc\hosts"
$markerStart = '# CURSOR-VPN-DNS-FIX'
$markerEnd = '# END CURSOR-VPN-DNS-FIX'
$block = @"
$markerStart
# Public VPN gateway (must resolve WITHOUT VPN)
176.116.144.90 vpncorp.energo-pro.bg
# Internal apps (only reachable AFTER VPN connects)
10.236.19.126 apps.energo-pro.bg
10.236.19.102 git.domain.internal
$markerEnd
"@

$raw = [System.IO.File]::ReadAllText($hostsPath)
$cleaned = [regex]::Replace($raw, "(?ms)$([regex]::Escape($markerStart)).*?$([regex]::Escape($markerEnd))\r?\n?", '')
$newText = $cleaned.TrimEnd() + "`r`n`r`n" + $block + "`r`n"
$tmp = "$env:TEMP\hosts-cursor-fix2.txt"
[System.IO.File]::WriteAllText($tmp, $newText, [System.Text.Encoding]::ASCII)
Copy-Item $tmp $hostsPath -Force
Write-Host 'hosts updated (vpncorp public IP + internal hosts)'

# Optional: keep .domain.internal NRPT only AFTER noting it needs VPN — skip for safety until connected
Clear-DnsClientCache
ipconfig /flushdns | Out-Null

Write-Host ''
Write-Host '=== Verify VPN gateway resolves (no VPN needed) ==='
nslookup vpncorp.energo-pro.bg 2>&1 | Select-Object -Last 8
ping -n 1 vpncorp.energo-pro.bg 2>&1 | Select-Object -First 4
Write-Host ''
Write-Host '=== PPTP 1723 to gateway ==='
Test-NetConnection 176.116.144.90 -Port 1723 -WarningAction SilentlyContinue |
  Select-Object ComputerName,RemotePort,TcpTestSucceeded,PingSucceeded | Format-List
Write-Host 'Done. Cancel stuck VPN, then Connect again to profile VPN (vpncorp.energo-pro.bg).'
pause
