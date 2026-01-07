# 터미널 한글 인코딩 설정 가이드

## 문제
PowerShell 터미널에서 한글이 깨져서 보이는 경우 (예: `ġ`, `` 등)

## 해결 방법

### 방법 1: PowerShell 프로파일 자동 설정 (권장)

프로젝트 루트에서 다음 스크립트를 실행하세요:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup-powershell-profile.ps1
```

이 스크립트는 PowerShell 프로파일에 UTF-8 인코딩 설정을 자동으로 추가합니다.

**적용 방법:**
1. Cursor IDE를 완전히 종료
2. Cursor IDE를 다시 시작
3. 새 터미널을 열면 자동으로 UTF-8 인코딩이 적용됩니다

### 방법 2: 현재 터미널에서 즉시 설정

현재 열려있는 터미널에서 다음 명령을 실행하세요:

```powershell
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null
$PSDefaultParameterValues['*:Encoding'] = 'utf8'
```

### 방법 3: 수동으로 PowerShell 프로파일 설정

1. PowerShell 프로파일 위치 확인:
```powershell
$PROFILE
```

2. 프로파일이 없으면 생성:
```powershell
if (-not (Test-Path $PROFILE)) {
    New-Item -ItemType File -Path $PROFILE -Force
}
```

3. 프로파일 편집:
```powershell
notepad $PROFILE
```

4. 다음 내용을 추가:
```powershell
# UTF-8 인코딩 설정 (한글 출력을 위해)
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null
$PSDefaultParameterValues['*:Encoding'] = 'utf8'
```

5. 저장 후 PowerShell 재시작

## Cursor IDE 설정

`.vscode/settings.json` 파일에 이미 터미널 인코딩 설정이 포함되어 있습니다:

```json
{
    "terminal.integrated.encoding": "utf8",
    "terminal.integrated.profiles.windows": {
        "PowerShell": {
            "source": "PowerShell",
            "args": [
                "-NoExit",
                "-Command",
                "$OutputEncoding = [System.Text.Encoding]::UTF8; [Console]::OutputEncoding = [System.Text.Encoding]::UTF8; [Console]::InputEncoding = [System.Text.Encoding]::UTF8; chcp 65001 | Out-Null; $PSDefaultParameterValues['*:Encoding'] = 'utf8'"
            ]
        }
    }
}
```

## 확인 방법

터미널에서 다음 명령을 실행하여 한글이 제대로 표시되는지 확인하세요:

```powershell
Write-Host "한글 테스트: 안녕하세요" -ForegroundColor Green
```

또는:

```powershell
echo "한글 테스트: 안녕하세요"
```

## 추가 설정

### Gradle 빌드 시 한글 출력

`gradlew.bat` 파일에 이미 UTF-8 인코딩 설정이 포함되어 있습니다:
- `chcp 65001` - 코드 페이지를 UTF-8로 변경
- JVM 옵션에 `-Dfile.encoding=UTF-8` 포함

### Java 애플리케이션 로그

`logback-spring.xml` 파일에 UTF-8 인코딩이 설정되어 있습니다:
```xml
<encoder>
    <charset>UTF-8</charset>
</encoder>
```

## 문제 해결

### 여전히 한글이 깨지는 경우

1. **Cursor IDE 완전 재시작**
   - 모든 창을 닫고 Cursor IDE를 완전히 종료
   - 다시 시작

2. **PowerShell 프로파일 확인**
   ```powershell
   Get-Content $PROFILE
   ```
   UTF-8 인코딩 설정이 있는지 확인

3. **현재 인코딩 확인**
   ```powershell
   [Console]::OutputEncoding
   [Console]::InputEncoding
   $OutputEncoding
   ```
   모두 UTF-8이어야 합니다.

4. **수동으로 코드 페이지 변경**
   ```powershell
   chcp 65001
   ```

## 참고

- Windows PowerShell 기본 인코딩은 보통 CP949 (EUC-KR)입니다
- UTF-8로 변경하면 한글 출력이 정상적으로 표시됩니다
- 프로파일에 설정을 추가하면 매번 자동으로 적용됩니다

