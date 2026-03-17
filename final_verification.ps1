$apiKey = "AIzaSyAONj4zxweXYV0bwtXwFxDSeg3IuHIQofE"

Write-Host "--- TEST: Native Gemini API ---"
$nativeBody = '{"contents": [{"parts": [{"text": "hi"}]}]}'
$nativeUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
try {
    $r = Invoke-WebRequest -Method Post -Uri $nativeUrl -ContentType "application/json" -Body $nativeBody
    Write-Host "Native API SUCCESS!" -ForegroundColor Green
    $r.Content
} catch {
    Write-Host "Native API FAILED"
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $reader.ReadToEnd()
}

Write-Host "`n--- TEST: OpenAI Endpoint with 'models/' prefix ---"
$oaBody = '{"model": "models/gemini-1.5-flash", "messages": [{"role": "user", "content": "hi"}]}'
$tests = @(
    @{ name = "v1beta/openai/v1/chat/completions WITH Bearer"; url = "https://generativelanguage.googleapis.com/v1beta/openai/v1/chat/completions"; headers = @{ "Authorization" = "Bearer $apiKey" } },
    @{ name = "v1beta/chat/completions WITH Bearer"; url = "https://generativelanguage.googleapis.com/v1beta/chat/completions"; headers = @{ "Authorization" = "Bearer $apiKey" } }
)

foreach ($t in $tests) {
    Write-Host "`nTesting: $($t.name)" -ForegroundColor Cyan
    try {
        $resp = Invoke-WebRequest -Method Post -Uri $t.url -ContentType "application/json" -Body $oaBody -Headers $t.headers
        Write-Host "SUCCESS!" -ForegroundColor Green
        $resp.Content
        break
    } catch {
        Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            Write-Host "Error Body: $($reader.ReadToEnd())"
        }
    }
}
