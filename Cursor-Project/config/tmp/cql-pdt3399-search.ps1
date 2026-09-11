$ErrorActionPreference = 'Stop'
$script = Join-Path $PSScriptRoot '..\confluence\search-confluence-cql-rest.ps1'
$outDir = Join-Path $PSScriptRoot 'pdt3399-cql'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$queries = @(
    @{ Name = 'exclude-liabilities'; Cql = 'text ~ "Exclude liabilities by amount" AND type = page' },
    @{ Name = 'less-greater-channel'; Cql = 'text ~ "less than" AND text ~ "greater than" AND text ~ "collection channel" AND type = page' },
    @{ Name = 'allowed-symbols-less'; Cql = 'text ~ "Allowed symbols" AND text ~ "Less than" AND type = page' },
    @{ Name = 'phase2-cc-children'; Cql = 'ancestor = 585697540 AND type = page' },
    @{ Name = 'export-liabilities'; Cql = 'title ~ "Export Liabilities" AND type = page' },
    @{ Name = 'exclude-by-amount'; Cql = 'text ~ "exclude liabilities by amount" AND type = page' },
    @{ Name = 'greater-than-optional'; Cql = 'text ~ "Greater than" AND text ~ "Optional" AND text ~ "collection" AND type = page' }
)

foreach ($q in $queries) {
    Write-Host ""
    Write-Host "======== $($q.Name) ========"
    Write-Host $q.Cql
    $out = Join-Path $outDir "$($q.Name).json"
    & $script -Cql $q.Cql -Limit 40 -OutFile $out
}
