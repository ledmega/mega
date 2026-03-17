$apiKey = "AIzaSyAONj4zxweXYV0bwtXwFxDSeg3IuHIQofE"
$body = '{"model": "gemini-2.5-flash", "messages": [{"role": "user", "content": "hi"}]}'

$url = "https://generativelanguage.googleapis.com/v1beta/openai/v1/chat/completions"
Write-Host "Testing Gemini 2.5 Flash: $url"
try {
    $headers = @{ "Authorization" = "Bearer $apiKey" }
    $resp = Invoke-WebRequest -Method Post -Uri $url -ContentType "application/json" -Body $body -Headers $headers
    Write-Host "SUCCESS!" -ForegroundColor Green
    $resp.Content
} catch {
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host "FAILED!" -ForegroundColor Red
    $reader.ReadToEnd()
}
