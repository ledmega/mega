$apiKey = "AIzaSyAONj4zxweXYV0bwtXwFxDSeg3IuHIQofE"

$tests = @(
    @{ name = "Native 1.5-flash"; url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"; body = '{"contents": [{"parts": [{"text": "hi"}]}]}' },
    @{ name = "Native gemini-flash-latest"; url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=$apiKey"; body = '{"contents": [{"parts": [{"text": "hi"}]}]}' }
)

foreach ($t in $tests) {
    Write-Host "`nTesting: $($t.name)" -ForegroundColor Cyan
    try {
        $resp = Invoke-WebRequest -Method Post -Uri $t.url -ContentType "application/json" -Body $t.body
        Write-Host "SUCCESS!" -ForegroundColor Green
        $resp.Content
        break
    } catch {
        Write-Host "FAILED!" -ForegroundColor Red
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.ReadToEnd()
    }
}
