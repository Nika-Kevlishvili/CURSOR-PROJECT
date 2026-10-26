function Resolve-ScratchOutFile {
    param([Parameter(Mandatory = $true)][string]$RequestedPath)

    $scratch = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\tmp'))
    $full = [System.IO.Path]::GetFullPath($RequestedPath)
    if ($full.StartsWith($scratch, [System.StringComparison]::OrdinalIgnoreCase)) {
        $dir = Split-Path -Parent $full
        if (-not (Test-Path -LiteralPath $dir)) {
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
        }
        return $full
    }

    $name = [System.IO.Path]::GetFileName($RequestedPath)
    foreach ($c in [System.IO.Path]::GetInvalidFileNameChars()) {
        $name = $name.Replace([string]$c, '-')
    }
    $name = $name -replace '[~=]', '-'
    $name = $name.Trim()
    if ($name.Length -gt 60) { $name = $name.Substring(0, 60).Trim() }
    if (-not $name.EndsWith('.json')) { $name = "$name.json" }

    $destDir = Join-Path $scratch 'confluence'
    if (-not (Test-Path -LiteralPath $destDir)) {
        New-Item -ItemType Directory -Path $destDir -Force | Out-Null
    }
    return (Join-Path $destDir $name)
}
