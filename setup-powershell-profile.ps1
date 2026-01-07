# PowerShell 프로파일에 UTF-8 인코딩 설정을 추가하는 스크립트
# 이 스크립트를 한 번 실행하면 PowerShell 프로파일에 자동으로 인코딩 설정이 추가됩니다.

$profilePath = $PROFILE.CurrentUserAllHosts
$profileDir = Split-Path -Parent $profilePath

# 프로파일 디렉토리가 없으면 생성
if (-not (Test-Path $profileDir)) {
    New-Item -ItemType Directory -Path $profileDir -Force | Out-Null
    Write-Host "PowerShell 프로파일 디렉토리 생성: $profileDir" -ForegroundColor Green
}

# 인코딩 설정 코드
$encodingConfig = @"

# UTF-8 인코딩 설정 (한글 출력을 위해)
`$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null
`$PSDefaultParameterValues['*:Encoding'] = 'utf8'

"@

# 프로파일이 존재하는지 확인
if (Test-Path $profilePath) {
    $content = Get-Content $profilePath -Raw -ErrorAction SilentlyContinue
    
    # 이미 설정이 있는지 확인
    if ($content -and $content -match "UTF-8 인코딩 설정") {
        Write-Host "이미 UTF-8 인코딩 설정이 프로파일에 있습니다." -ForegroundColor Yellow
        Write-Host "프로파일 위치: $profilePath" -ForegroundColor Cyan
    } else {
        # 기존 내용에 추가
        Add-Content -Path $profilePath -Value $encodingConfig -Encoding UTF8
        Write-Host "UTF-8 인코딩 설정을 프로파일에 추가했습니다." -ForegroundColor Green
        Write-Host "프로파일 위치: $profilePath" -ForegroundColor Cyan
    }
} else {
    # 새 프로파일 생성
    Set-Content -Path $profilePath -Value $encodingConfig -Encoding UTF8
    Write-Host "새 PowerShell 프로파일을 생성하고 UTF-8 인코딩 설정을 추가했습니다." -ForegroundColor Green
    Write-Host "프로파일 위치: $profilePath" -ForegroundColor Cyan
}

Write-Host "`n변경사항을 적용하려면 PowerShell을 재시작하거나 다음 명령을 실행하세요:" -ForegroundColor Yellow
Write-Host ". `$PROFILE" -ForegroundColor Cyan
Write-Host "`n또는 Cursor IDE를 재시작하세요." -ForegroundColor Yellow


