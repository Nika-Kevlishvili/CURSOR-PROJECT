# Enable Windows virtualization stack required by Docker Desktop (WSL2).
# Requires Administrator. Does not reboot automatically.

$ErrorActionPreference = "Continue"
$log = Join-Path $PSScriptRoot "docker-virt-fix.log"

function Write-Log([string]$msg) {
    $line = "[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $msg
    Add-Content -Path $log -Value $line
    Write-Host $line
}

Write-Log "=== Docker virtualization fix started ==="
Write-Log ("User={0} Elevated={1}" -f $env:USERNAME, ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator))
Write-Log ("OS={0}" -f (Get-CimInstance Win32_OperatingSystem).Caption)

if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Log "ERROR: Must run as Administrator."
    exit 1
}

$features = @(
    "Microsoft-Windows-Subsystem-Linux",
    "VirtualMachinePlatform",
    "HypervisorPlatform"
)

$rebootNeeded = $false
foreach ($name in $features) {
    try {
        $before = Get-WindowsOptionalFeature -Online -FeatureName $name
        Write-Log ("Feature {0} state={1}" -f $name, $before.State)
        if ($before.State -ne "Enabled") {
            Write-Log ("Enabling {0} ..." -f $name)
            $result = Enable-WindowsOptionalFeature -Online -FeatureName $name -All -NoRestart
            Write-Log ("Enable {0} RestartNeeded={1}" -f $name, $result.RestartNeeded)
            if ($result.RestartNeeded) { $rebootNeeded = $true }
        }
    } catch {
        Write-Log ("ERROR enabling {0}: {1}" -f $name, $_.Exception.Message)
    }
}

try {
    $bcd = & bcdedit /enum "{current}" 2>&1 | Out-String
    Write-Log ("bcdedit current:`n{0}" -f $bcd.Trim())
    & bcdedit /set "{current}" hypervisorlaunchtype auto | Out-String | ForEach-Object { Write-Log ("bcdedit hypervisorlaunchtype auto: {0}" -f $_.Trim()) }
} catch {
    Write-Log ("ERROR bcdedit: {0}" -f $_.Exception.Message)
}

try {
    Write-Log "Installing WSL (no Linux distro)..."
    $wslOut = & wsl.exe --install --no-distribution --web-download 2>&1 | Out-String
    Write-Log ("wsl --install exit={0}`n{1}" -f $LASTEXITCODE, $wslOut.Trim())
} catch {
    Write-Log ("ERROR wsl --install: {0}" -f $_.Exception.Message)
}

try {
    & wsl.exe --set-default-version 2 2>&1 | Out-String | ForEach-Object { Write-Log ("wsl default version: {0}" -f $_.Trim()) }
} catch {
    Write-Log ("ERROR wsl --set-default-version: {0}" -f $_.Exception.Message)
}

Write-Log ("HypervisorPresent after change (may still be False until reboot)={0}" -f (Get-CimInstance Win32_ComputerSystem).HypervisorPresent)
Write-Log ("REBOOT_REQUIRED={0}" -f $rebootNeeded)
Write-Log "=== Done. Reboot Windows, then start Docker Desktop. ==="

if ($rebootNeeded) { exit 3010 } else { exit 0 }
