<#
.SYNOPSIS
    Extracts semantic structure from draw.io diagrams in a format optimized for AI analysis.

.DESCRIPTION
    Creates AI-readable documentation from draw.io diagrams focusing on:
    - Clean process step listing
    - Decision points with their branches
    - Business rules and conditions
    - Readable flow relationships

.PARAMETER Path
    Path to diagram file.

.PARAMETER All
    Process all diagrams.
#>

param(
    [Parameter(ParameterSetName = "Single")]
    [string]$Path,
    
    [Parameter(ParameterSetName = "All")]
    [switch]$All
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Web

$DiagramsRoot = Join-Path $PSScriptRoot "..\..\Cursor-Project\config\Diagrams"
$DiagramsRoot = (Resolve-Path $DiagramsRoot).Path

function Clean-Label {
    param([string]$Text)
    if (-not $Text) { return "" }
    $clean = $Text -replace '<[^>]+>', ''
    $clean = [System.Web.HttpUtility]::HtmlDecode($clean)
    $clean = $clean -replace '\s+', ' '
    return $clean.Trim()
}

function Get-DiagramDomain {
    param([string]$FilePath)
    if ($FilePath -match "Bundle 4") { return "POD Management / Contracts" }
    if ($FilePath -match "Bundle 5") { return "Billing / Invoicing" }
    if ($FilePath -match "Bundle 6") { return "Payments / Receivables" }
    return "General"
}

function Extract-Content {
    param([string]$FilePath)
    
    $content = Get-Content $FilePath -Raw -Encoding UTF8
    $ext = [System.IO.Path]::GetExtension($FilePath).ToLower()
    
    if ($ext -eq ".drawio") {
        return $content
    }
    
    $match = [regex]::Match($content, 'content="([^"]+)"')
    if ($match.Success) {
        $decoded = [System.Web.HttpUtility]::HtmlDecode($match.Groups[1].Value)
        return [System.Web.HttpUtility]::HtmlDecode($decoded)
    }
    return $null
}

function Parse-Diagram {
    param([string]$XmlContent)
    
    $nodes = @{}
    $edges = @()
    $yesNoNodes = @{}
    
    # Extract cells
    $cellPattern = '<mxCell\s+([^>]+)(?:/>|>)'
    $matches = [regex]::Matches($XmlContent, $cellPattern)
    
    foreach ($m in $matches) {
        $attrs = $m.Groups[1].Value
        
        $id = if ($attrs -match 'id="([^"]*)"') { $Matches[1] } else { $null }
        $value = if ($attrs -match 'value="([^"]*)"') { Clean-Label $Matches[1] } else { "" }
        $style = if ($attrs -match 'style="([^"]*)"') { $Matches[1] } else { "" }
        $isVertex = $attrs -match 'vertex="1"'
        $isEdge = $attrs -match 'edge="1"'
        $source = if ($attrs -match 'source="([^"]*)"') { $Matches[1] } else { $null }
        $target = if ($attrs -match 'target="([^"]*)"') { $Matches[1] } else { $null }
        
        if (-not $id) { continue }
        
        # Track Yes/No nodes separately
        if ($isVertex -and $value -and $value.ToLower() -in @('yes', 'no', 'true', 'false')) {
            $yesNoNodes[$id] = $value
            continue
        }
        
        if ($isVertex -and $value) {
            # Determine type
            $type = "process"
            $valueLower = $value.ToLower()
            $styleLower = $style.ToLower()
            
            if ($styleLower -match 'rhombus|diamond') {
                $type = "decision"
            }
            elseif ($valueLower -match '\?$|^is |^are |^has |^does |^can |^should |^check |^verify |^validate ') {
                $type = "decision"  
            }
            elseif ($valueLower -match '^click|^open|^start|^begin|^trigger|^user ') {
                $type = "action"
            }
            elseif ($valueLower -match 'save |create |update |delete |insert |remove ') {
                $type = "save"
            }
            elseif ($valueLower -match 'error|fail|exception|reject|cancel') {
                $type = "error"
            }
            
            $nodes[$id] = @{
                Id = $id
                Label = $value
                Type = $type
                Style = $style
            }
        }
        elseif ($isEdge -and $source -and $target) {
            $edges += @{
                Source = $source
                Target = $target
                Label = $value
            }
        }
    }
    
    # Resolve Yes/No nodes - convert them to edge labels
    $resolvedEdges = @()
    foreach ($edge in $edges) {
        $newEdge = @{
            Source = $edge.Source
            Target = $edge.Target
            Label = $edge.Label
        }
        
        # If target is Yes/No, find what it leads to and use Yes/No as label
        if ($yesNoNodes.ContainsKey($edge.Target)) {
            $condition = $yesNoNodes[$edge.Target]
            $nextEdge = $edges | Where-Object { $_.Source -eq $edge.Target } | Select-Object -First 1
            if ($nextEdge) {
                $newEdge.Target = $nextEdge.Target
                $newEdge.Label = $condition
            }
        }
        
        # If source is Yes/No, find what leads to it
        if ($yesNoNodes.ContainsKey($edge.Source)) {
            $condition = $yesNoNodes[$edge.Source]
            $prevEdge = $edges | Where-Object { $_.Target -eq $edge.Source } | Select-Object -First 1
            if ($prevEdge) {
                $newEdge.Source = $prevEdge.Source
                if (-not $newEdge.Label) {
                    $newEdge.Label = $condition
                }
            }
        }
        
        # Only add if both source and target are real nodes
        if ($nodes.ContainsKey($newEdge.Source) -or $nodes.ContainsKey($newEdge.Target)) {
            $resolvedEdges += $newEdge
        }
    }
    
    return @{
        Nodes = $nodes
        Edges = $resolvedEdges
    }
}

function Build-DecisionTree {
    param($Nodes, $Edges)
    
    $decisions = @()
    
    foreach ($node in ($Nodes.Values | Where-Object { $_.Type -eq "decision" })) {
        $outEdges = $Edges | Where-Object { $_.Source -eq $node.Id }
        
        $branches = @()
        foreach ($edge in $outEdges) {
            $targetNode = $Nodes[$edge.Target]
            $targetLabel = if ($targetNode) { $targetNode.Label } else { "(continues...)" }
            $condition = if ($edge.Label) { $edge.Label } else { "then" }
            
            $branches += @{
                Condition = $condition
                LeadsTo = $targetLabel
            }
        }
        
        if ($branches.Count -gt 0) {
            $decisions += @{
                Question = $node.Label
                Branches = $branches
            }
        }
    }
    
    return $decisions
}

function Build-ProcessFlow {
    param($Nodes, $Edges)
    
    # Group nodes by type
    $actions = $Nodes.Values | Where-Object { $_.Type -eq "action" } | ForEach-Object { $_.Label }
    $processes = $Nodes.Values | Where-Object { $_.Type -eq "process" } | ForEach-Object { $_.Label }
    $saves = $Nodes.Values | Where-Object { $_.Type -eq "save" } | ForEach-Object { $_.Label }
    $errors = $Nodes.Values | Where-Object { $_.Type -eq "error" } | ForEach-Object { $_.Label }
    
    return @{
        Actions = $actions | Select-Object -Unique
        Processes = $processes | Select-Object -Unique
        SaveOperations = $saves | Select-Object -Unique
        ErrorStates = $errors | Select-Object -Unique
    }
}

function Build-Connections {
    param($Nodes, $Edges)
    
    $connections = @()
    
    foreach ($edge in $Edges) {
        $sourceNode = $Nodes[$edge.Source]
        $targetNode = $Nodes[$edge.Target]
        
        if ($sourceNode -and $targetNode) {
            $condition = if ($edge.Label) { " [$($edge.Label)]" } else { "" }
            $connections += "$($sourceNode.Label)$condition --> $($targetNode.Label)"
        }
    }
    
    return $connections | Select-Object -Unique
}

function Generate-SemanticMarkdown {
    param(
        [string]$SourceFile,
        [hashtable]$Nodes,
        [array]$Edges,
        [array]$Decisions,
        [hashtable]$ProcessFlow,
        [array]$Connections
    )
    
    $fileName = [System.IO.Path]::GetFileNameWithoutExtension($SourceFile)
    $title = $fileName -replace '\.drawio.*$', '' -replace '[-_\(\)\d]', ' ' -replace '\s+', ' '
    $title = (Get-Culture).TextInfo.ToTitleCase($title.Trim().ToLower())
    $domain = Get-DiagramDomain -FilePath $SourceFile
    
    $sb = [System.Text.StringBuilder]::new()
    
    # Header
    [void]$sb.AppendLine("# $title")
    [void]$sb.AppendLine()
    [void]$sb.AppendLine("**Domain:** $domain")
    [void]$sb.AppendLine("**Source:** $([System.IO.Path]::GetFileName($SourceFile))")
    [void]$sb.AppendLine("**Extracted:** $(Get-Date -Format 'yyyy-MM-dd')")
    [void]$sb.AppendLine()
    
    # Quick Stats
    [void]$sb.AppendLine("## Overview")
    [void]$sb.AppendLine()
    [void]$sb.AppendLine("| Metric | Count |")
    [void]$sb.AppendLine("|--------|-------|")
    [void]$sb.AppendLine("| Decision Points | $($Decisions.Count) |")
    [void]$sb.AppendLine("| User Actions | $($ProcessFlow.Actions.Count) |")
    [void]$sb.AppendLine("| Process Steps | $($ProcessFlow.Processes.Count) |")
    [void]$sb.AppendLine("| Save Operations | $($ProcessFlow.SaveOperations.Count) |")
    [void]$sb.AppendLine("| Error States | $($ProcessFlow.ErrorStates.Count) |")
    [void]$sb.AppendLine()
    
    # User Actions (Entry Points)
    if ($ProcessFlow.Actions.Count -gt 0) {
        [void]$sb.AppendLine("## User Actions (Entry Points)")
        [void]$sb.AppendLine()
        foreach ($action in $ProcessFlow.Actions) {
            [void]$sb.AppendLine("- $action")
        }
        [void]$sb.AppendLine()
    }
    
    # Decision Points - THE MOST IMPORTANT SECTION
    if ($Decisions.Count -gt 0) {
        [void]$sb.AppendLine("## Decision Points (Business Logic)")
        [void]$sb.AppendLine()
        [void]$sb.AppendLine("These are the branching points in the process. Each decision leads to different outcomes.")
        [void]$sb.AppendLine()
        
        $decNum = 1
        foreach ($decision in $Decisions) {
            [void]$sb.AppendLine("### $decNum. $($decision.Question)")
            [void]$sb.AppendLine()
            
            foreach ($branch in $decision.Branches) {
                [void]$sb.AppendLine("- **$($branch.Condition)** --> $($branch.LeadsTo)")
            }
            [void]$sb.AppendLine()
            $decNum++
        }
    }
    
    # Process Steps
    if ($ProcessFlow.Processes.Count -gt 0) {
        [void]$sb.AppendLine("## Process Steps")
        [void]$sb.AppendLine()
        foreach ($process in $ProcessFlow.Processes) {
            [void]$sb.AppendLine("- $process")
        }
        [void]$sb.AppendLine()
    }
    
    # Save Operations (Outcomes)
    if ($ProcessFlow.SaveOperations.Count -gt 0) {
        [void]$sb.AppendLine("## Save Operations (Outcomes)")
        [void]$sb.AppendLine()
        [void]$sb.AppendLine("These are the possible end states where data is persisted:")
        [void]$sb.AppendLine()
        foreach ($save in $ProcessFlow.SaveOperations) {
            [void]$sb.AppendLine("- $save")
        }
        [void]$sb.AppendLine()
    }
    
    # Error States
    if ($ProcessFlow.ErrorStates.Count -gt 0) {
        [void]$sb.AppendLine("## Error/Exception States")
        [void]$sb.AppendLine()
        foreach ($error in $ProcessFlow.ErrorStates) {
            [void]$sb.AppendLine("- $error")
        }
        [void]$sb.AppendLine()
    }
    
    # Flow Connections
    if ($Connections.Count -gt 0) {
        [void]$sb.AppendLine("## Flow Connections")
        [void]$sb.AppendLine()
        [void]$sb.AppendLine("Direct relationships between steps:")
        [void]$sb.AppendLine()
        foreach ($conn in ($Connections | Select-Object -First 50)) {
            [void]$sb.AppendLine("- $conn")
        }
        if ($Connections.Count -gt 50) {
            [void]$sb.AppendLine("- ... and $($Connections.Count - 50) more connections")
        }
        [void]$sb.AppendLine()
    }
    
    # AI Usage Guide
    [void]$sb.AppendLine("---")
    [void]$sb.AppendLine()
    [void]$sb.AppendLine("## How to Use This for Test Cases")
    [void]$sb.AppendLine()
    [void]$sb.AppendLine("1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations")
    [void]$sb.AppendLine("2. **Alternative Paths**: Follow 'No' branches to see different outcomes")
    [void]$sb.AppendLine("3. **Error Scenarios**: Check Error States section for exception cases")
    [void]$sb.AppendLine("4. **Preconditions**: User Actions show what triggers this process")
    [void]$sb.AppendLine()
    [void]$sb.AppendLine("*This is supplementary evidence - always verify against code and Confluence.*")
    
    return $sb.ToString()
}

function Process-File {
    param([string]$FilePath)
    
    $fileName = [System.IO.Path]::GetFileName($FilePath)
    Write-Host "Processing: $fileName" -ForegroundColor Cyan
    
    $xml = Extract-Content -FilePath $FilePath
    if (-not $xml) {
        Write-Warning "  Could not extract content"
        return $false
    }
    
    Write-Host "  Parsing..." -ForegroundColor Gray
    $parsed = Parse-Diagram -XmlContent $xml
    Write-Host "  Found $($parsed.Nodes.Count) nodes, $($parsed.Edges.Count) edges" -ForegroundColor Gray
    
    $decisions = Build-DecisionTree -Nodes $parsed.Nodes -Edges $parsed.Edges
    Write-Host "  Found $($decisions.Count) decision points" -ForegroundColor Gray
    
    $processFlow = Build-ProcessFlow -Nodes $parsed.Nodes -Edges $parsed.Edges
    $connections = Build-Connections -Nodes $parsed.Nodes -Edges $parsed.Edges
    
    $markdown = Generate-SemanticMarkdown `
        -SourceFile $FilePath `
        -Nodes $parsed.Nodes `
        -Edges $parsed.Edges `
        -Decisions $decisions `
        -ProcessFlow $processFlow `
        -Connections $connections
    
    $baseName = [System.IO.Path]::GetFileNameWithoutExtension($FilePath) -replace '\.drawio$', ''
    $outputPath = Join-Path ([System.IO.Path]::GetDirectoryName($FilePath)) "$baseName.semantic.md"
    
    $markdown | Out-File -FilePath $outputPath -Encoding UTF8
    Write-Host "  Created: $outputPath" -ForegroundColor Green
    
    return $true
}

# Main
Write-Host "`n=== Diagram Semantic Extractor ===" -ForegroundColor Yellow
Write-Host "Root: $DiagramsRoot`n"

$count = 0

if ($All) {
    $files = Get-ChildItem -Path $DiagramsRoot -Include "*.svg", "*.drawio" -Recurse
    foreach ($file in $files) {
        try {
            if (Process-File -FilePath $file.FullName) { $count++ }
        }
        catch {
            Write-Warning "Error processing $($file.Name): $_"
        }
        Write-Host ""
    }
}
elseif ($Path) {
    $fullPath = if ([System.IO.Path]::IsPathRooted($Path)) { $Path } else { Join-Path $DiagramsRoot $Path }
    if (Process-File -FilePath $fullPath) { $count++ }
}
else {
    Write-Host "Usage:"
    Write-Host '  .\extract-diagram-semantic.ps1 -Path "Bundle 4/POD activation_deactivation manual.drawio (3).svg"'
    Write-Host "  .\extract-diagram-semantic.ps1 -All"
}

Write-Host "`nProcessed: $count files" -ForegroundColor Green
