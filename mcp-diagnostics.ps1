# MCP Diagnostics Script
# ეს სკრიპტი ამოწმებს MCP-ის სწორი მუშაობისთვის საჭირო ყველა კომპონენტს

Write-Host "`n=== MCP Diagnostics ===" -ForegroundColor Cyan
Write-Host "MCP სისტემის დიაგნოსტიკა`n" -ForegroundColor Gray

$issues = @()
$warnings = @()

# 1. Node.js და NPX
Write-Host "1. Node.js და NPX შემოწმება..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version 2>&1
    if ($nodeVersion -match "v(\d+)") {
        $majorVersion = [int]$matches[1]
        if ($majorVersion -ge 18) {
            Write-Host "   ✅ Node.js: $nodeVersion (საჭირო: v18+)" -ForegroundColor Green
        } else {
            Write-Host "   ⚠️  Node.js: $nodeVersion (საჭირო: v18+)" -ForegroundColor Yellow
            $warnings += "Node.js ვერსია ძველია, განაახლეთ v18+"
        }
    } else {
        Write-Host "   ❌ Node.js არ არის დაყენებული" -ForegroundColor Red
        $issues += "Node.js არ არის დაყენებული"
    }
} catch {
    Write-Host "   ❌ Node.js არ მოიძებნა" -ForegroundColor Red
    $issues += "Node.js არ მოიძებნა"
}

try {
    $npxVersion = npx --version 2>&1
    if ($npxVersion -match "\d+\.\d+") {
        Write-Host "   ✅ NPX: $npxVersion" -ForegroundColor Green
    } else {
        Write-Host "   ❌ NPX არ მუშაობს" -ForegroundColor Red
        $issues += "NPX არ მუშაობს"
    }
} catch {
    Write-Host "   ❌ NPX არ მოიძებნა" -ForegroundColor Red
    $issues += "NPX არ მოიძებნა"
}

# 2. MCP კონფიგურაცია
Write-Host "`n2. MCP კონფიგურაციის შემოწმება..." -ForegroundColor Yellow
$mcpPath = "$env:APPDATA\Cursor\mcp.json"
if (Test-Path $mcpPath) {
    Write-Host "   ✅ ფაილი არსებობს: $mcpPath" -ForegroundColor Green
    
    try {
        $configContent = Get-Content $mcpPath -Raw -ErrorAction Stop
        $config = $configContent | ConvertFrom-Json -ErrorAction Stop
        
        Write-Host "   ✅ JSON syntax სწორია" -ForegroundColor Green
        
        $serverCount = 0
        if ($config.mcpServers) {
            $serverCount = $config.mcpServers.PSObject.Properties.Count
            Write-Host "   📋 MCP სერვერების რაოდენობა: $serverCount" -ForegroundColor Cyan
            
            # შეამოწმეთ თითოეული სერვერი
            foreach ($serverName in $config.mcpServers.PSObject.Properties.Name) {
                $server = $config.mcpServers.$serverName
                Write-Host "      - $serverName" -ForegroundColor Gray
                
                if ($server.command -eq "npx") {
                    Write-Host "        ✅ command: npx" -ForegroundColor Green
                } else {
                    Write-Host "        ⚠️  command: $($server.command)" -ForegroundColor Yellow
                }
            }
        } else {
            Write-Host "   ⚠️  mcpServers სექცია არ მოიძებნა" -ForegroundColor Yellow
            $warnings += "mcpServers სექცია არ მოიძებნა კონფიგურაციაში"
        }
    } catch {
        Write-Host "   ❌ JSON syntax შეცდომა: $_" -ForegroundColor Red
        $issues += "MCP კონფიგურაციის JSON syntax შეცდომა: $_"
    }
} else {
    Write-Host "   ❌ MCP კონფიგურაცია არ მოიძებნა: $mcpPath" -ForegroundColor Red
    Write-Host "   💡 გადაიტანეთ mcp-config.json ამ მისამართზე" -ForegroundColor Yellow
    $issues += "MCP კონფიგურაცია არ მოიძებნა: $mcpPath"
}

# 3. PATH Environment Variable
Write-Host "`n3. PATH Environment Variable შემოწმება..." -ForegroundColor Yellow
$nodeInPath = $env:PATH -split ';' | Where-Object { 
    $_ -like "*node*" -and (Test-Path "$_\node.exe" -ErrorAction SilentlyContinue)
}
if ($nodeInPath) {
    Write-Host "   ✅ Node.js PATH-შია: $($nodeInPath[0])" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Node.js PATH-ში არ არის" -ForegroundColor Yellow
    $warnings += "Node.js PATH environment variable-ში არ არის"
}

# 4. Network კავშირი (PostgreSQL)
Write-Host "`n4. Network კავშირის შემოწმება (PostgreSQL)..." -ForegroundColor Yellow
$testHosts = @(
    @{Host="10.236.20.24"; Port=5432; Name="PostgreSQLTest"},
    @{Host="10.236.20.21"; Port=5432; Name="PostgreSQLDev"},
    @{Host="10.236.20.22"; Port=5432; Name="PostgreSQLDev2"},
    @{Host="10.236.20.76"; Port=5432; Name="PostgreSQLPreProd"}
)

foreach ($test in $testHosts) {
    try {
        $connection = Test-NetConnection -ComputerName $test.Host -Port $test.Port -WarningAction SilentlyContinue -InformationLevel Quiet
        if ($connection) {
            Write-Host "   ✅ $($test.Name): $($test.Host):$($test.Port) - კავშირი მუშაობს" -ForegroundColor Green
        } else {
            Write-Host "   ❌ $($test.Name): $($test.Host):$($test.Port) - კავშირი არ მუშაობს" -ForegroundColor Red
            $warnings += "$($test.Name) სერვერთან კავშირი არ მუშაობს"
        }
    } catch {
        Write-Host "   ⚠️  $($test.Name): შემოწმება ვერ შესრულდა" -ForegroundColor Yellow
    }
}

# 5. NPX პაკეტის ტესტი
Write-Host "`n5. NPX პაკეტის ტესტი..." -ForegroundColor Yellow
try {
    Write-Host "   ⏳ mcp-postgres-server პაკეტის შემოწმება (შეიძლება დრო დასჭირდეს)..." -ForegroundColor Gray
    $npxTest = npx -y mcp-postgres-server --help 2>&1 | Select-Object -First 3
    if ($LASTEXITCODE -eq 0 -or $npxTest) {
        Write-Host "   ✅ mcp-postgres-server პაკეტი ხელმისაწვდომია" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  პაკეტის ჩამოტვირთვა შეიძლება დასჭირდეს" -ForegroundColor Yellow
        $warnings += "mcp-postgres-server პაკეტის ჩამოტვირთვა შეიძლება დასჭირდეს"
    }
} catch {
    Write-Host "   ❌ პაკეტის ტესტი ვერ შესრულდა: $_" -ForegroundColor Red
    $warnings += "NPX პაკეტის ტესტი ვერ შესრულდა"
}

# 6. Cursor Logs
Write-Host "`n6. Cursor Logs მდებარეობა..." -ForegroundColor Yellow
$cursorLogs = "$env:APPDATA\Cursor\logs"
if (Test-Path $cursorLogs) {
    $latestLog = Get-ChildItem $cursorLogs -Recurse -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($latestLog) {
        Write-Host "   📋 ბოლო log ფაილი: $($latestLog.FullName)" -ForegroundColor Cyan
        Write-Host "   💡 შეამოწმეთ logs MCP შეცდომებისთვის" -ForegroundColor Gray
    }
} else {
    Write-Host "   ⚠️  Logs ფოლდერი არ მოიძებნა" -ForegroundColor Yellow
}

# შეჯამება
Write-Host "`n=== შეჯამება ===" -ForegroundColor Cyan

if ($issues.Count -eq 0 -and $warnings.Count -eq 0) {
    Write-Host "✅ ყველაფერი კარგადაა! MCP სისტემა მუშაობს." -ForegroundColor Green
} else {
    if ($issues.Count -gt 0) {
        Write-Host "`n❌ კრიტიკული პრობლემები ($($issues.Count)):" -ForegroundColor Red
        foreach ($issue in $issues) {
            Write-Host "   - $issue" -ForegroundColor Red
        }
    }
    
    if ($warnings.Count -gt 0) {
        Write-Host "`n⚠️  გაფრთხილებები ($($warnings.Count)):" -ForegroundColor Yellow
        foreach ($warning in $warnings) {
            Write-Host "   - $warning" -ForegroundColor Yellow
        }
    }
    
    Write-Host "`n💡 რეკომენდაციები:" -ForegroundColor Cyan
    Write-Host "   1. გადატვირთეთ Cursor სრულად" -ForegroundColor Gray
    Write-Host "   2. შეამოწმეთ MCP_TROUBLESHOOTING.md დეტალური ინსტრუქციებისთვის" -ForegroundColor Gray
    Write-Host "   3. თუ პრობლემა გრძელდება, გადაინსტალირეთ Node.js" -ForegroundColor Gray
}

Write-Host "`n=== დიაგნოსტიკა დასრულდა ===`n" -ForegroundColor Cyan

