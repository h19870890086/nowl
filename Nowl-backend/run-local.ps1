# ==================== 本地开发启动脚本（Windows）====================

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  🚀 本地后端开发环境启动" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

# 加载环境变量
Write-Host "📋 加载环境变量..." -ForegroundColor Yellow

$envVars = Get-Content .env.local | Where-Object { $_ -notmatch '^#' -and $_ -match '=' }
foreach ($line in $envVars) {
    $parts = $line.Split('=', 2)
    if ($parts.Length -eq 2) {
        [System.Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
    }
}

Write-Host "✅ 环境变量加载完成" -ForegroundColor Green
Write-Host ""
Write-Host "🔧 配置信息：" -ForegroundColor Cyan
Write-Host "   MySQL: localhost:3306" -ForegroundColor White
Write-Host "   Redis: localhost:6379" -ForegroundColor White
Write-Host "   Elasticsearch: localhost:9200" -ForegroundColor White
Write-Host ""
Write-Host "🚀 启动应用..." -ForegroundColor Yellow
Write-Host ""

# 使用 Maven 启动
mvn spring-boot:run -pl unimarket-web -am -Dspring-boot.run.profiles=dev