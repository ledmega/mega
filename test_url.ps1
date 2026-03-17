$apiKey = "AIzaSyBqYISC0yG6ZyiRyFtHLFQtg9lssj-MDTA"
$body = '{"model": "gemini-1.5-flash", "messages": [{"role": "user", "content": "hi"}]}'

$url = "https://generativelanguage.googleapis.com/v1beta/chat/completions"
Write-Host "Testing URL: $url (Gemini Suggestion)"
try {
    $headers = @{ "Authorization" = "Bearer $apiKey" }
    $resp = Invoke-WebRequest -Method Post -Uri $url -ContentType "application/json" -Body $body -Headers $headers
    Write-Host "SUCCESS!"
    $resp.Content
} catch {
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $reader.ReadToEnd()
}
