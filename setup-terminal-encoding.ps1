# Cursor IDE 터미널에서 한글 인코딩 설정 스크립트
# 이 스크립트를 터미널에서 실행하거나 PowerShell 프로파일에 추가하세요

# UTF-8 인코딩 설정
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

Write-Host "Terminal encoding set to UTF-8" -ForegroundColor Green
Write-Host "한글 인코딩이 설정되었습니다." -ForegroundColor Green

