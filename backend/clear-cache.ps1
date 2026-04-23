# IntelliJ IDEA 및 Gradle 캐시 정리 스크립트
# 실행 전 IntelliJ IDEA를 완전히 종료하세요!

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "IDE 캐시 정리 스크립트" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# IntelliJ IDEA 프로세스 확인
$ideaProcesses = Get-Process | Where-Object { $_.ProcessName -like "*idea*" }
if ($ideaProcesses) {
    Write-Host "⚠️  경고: IntelliJ IDEA가 실행 중입니다!" -ForegroundColor Yellow
    Write-Host "다음 프로세스를 종료하세요:" -ForegroundColor Yellow
    $ideaProcesses | ForEach-Object { Write-Host "  - $($_.ProcessName) (PID: $($_.Id))" -ForegroundColor Yellow }
    Write-Host ""
    $continue = Read-Host "계속하시겠습니까? (y/n)"
    if ($continue -ne "y") {
        Write-Host "취소되었습니다." -ForegroundColor Red
        exit
    }
}

Write-Host "삭제할 폴더 목록:" -ForegroundColor Yellow
Write-Host ""

# 1. Gradle 캐시
$gradleCache = "$env:USERPROFILE\.gradle\caches"
if (Test-Path $gradleCache) {
    $size = (Get-ChildItem -Path $gradleCache -Recurse -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum / 1MB
    Write-Host "  [1] Gradle 캐시: $gradleCache" -ForegroundColor White
    Write-Host "      크기: $([math]::Round($size, 2)) MB" -ForegroundColor Gray
} else {
    Write-Host "  [1] Gradle 캐시: 없음" -ForegroundColor Gray
}

# 2. IntelliJ 프로젝트 설정
$ideaFolder = "$PSScriptRoot\.idea"
if (Test-Path $ideaFolder) {
    $size = (Get-ChildItem -Path $ideaFolder -Recurse -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum / 1MB
    Write-Host "  [2] IntelliJ 설정: $ideaFolder" -ForegroundColor White
    Write-Host "      크기: $([math]::Round($size, 2)) MB" -ForegroundColor Gray
} else {
    Write-Host "  [2] IntelliJ 설정: 없음" -ForegroundColor Gray
}

# 3. 빌드 결과물
$buildFolder = "$PSScriptRoot\build"
if (Test-Path $buildFolder) {
    $size = (Get-ChildItem -Path $buildFolder -Recurse -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum / 1MB
    Write-Host "  [3] 빌드 결과물: $buildFolder" -ForegroundColor White
    Write-Host "      크기: $([math]::Round($size, 2)) MB" -ForegroundColor Gray
} else {
    Write-Host "  [3] 빌드 결과물: 없음" -ForegroundColor Gray
}

Write-Host ""
$confirm = Read-Host "위 폴더들을 삭제하시겠습니까? (y/n)"

if ($confirm -ne "y") {
    Write-Host "취소되었습니다." -ForegroundColor Red
    exit
}

Write-Host ""
Write-Host "삭제 중..." -ForegroundColor Yellow

# 삭제 실행
$deleted = @()

# 1. Gradle 캐시 삭제
if (Test-Path $gradleCache) {
    try {
        Remove-Item -Path $gradleCache -Recurse -Force -ErrorAction Stop
        Write-Host "  ✓ Gradle 캐시 삭제 완료" -ForegroundColor Green
        $deleted += "Gradle 캐시"
    } catch {
        Write-Host "  ✗ Gradle 캐시 삭제 실패: $_" -ForegroundColor Red
    }
}

# 2. IntelliJ 설정 삭제
if (Test-Path $ideaFolder) {
    try {
        Remove-Item -Path $ideaFolder -Recurse -Force -ErrorAction Stop
        Write-Host "  ✓ IntelliJ 설정 삭제 완료" -ForegroundColor Green
        $deleted += "IntelliJ 설정"
    } catch {
        Write-Host "  ✗ IntelliJ 설정 삭제 실패: $_" -ForegroundColor Red
    }
}

# 3. 빌드 결과물 삭제
if (Test-Path $buildFolder) {
    try {
        Remove-Item -Path $buildFolder -Recurse -Force -ErrorAction Stop
        Write-Host "  ✓ 빌드 결과물 삭제 완료" -ForegroundColor Green
        $deleted += "빌드 결과물"
    } catch {
        Write-Host "  ✗ 빌드 결과물 삭제 실패: $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($deleted.Count -gt 0) {
    Write-Host "삭제 완료!" -ForegroundColor Green
    Write-Host "삭제된 항목: $($deleted -join ', ')" -ForegroundColor Green
} else {
    Write-Host "삭제할 항목이 없습니다." -ForegroundColor Yellow
}
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "다음 단계:" -ForegroundColor Cyan
Write-Host "1. IntelliJ IDEA 실행" -ForegroundColor White
Write-Host "2. Open → backend 폴더 선택" -ForegroundColor White
Write-Host "3. 'Import Gradle Project' 클릭" -ForegroundColor White
Write-Host "4. Gradle 동기화 완료 대기" -ForegroundColor White
Write-Host ""
