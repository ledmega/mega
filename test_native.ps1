$apiKey = "AIzaSyBqYISC0yG6ZyiRyFtHLFQtg9lssj-MDTA"
$body = '{"contents": [{"parts": [{"text": "hi"}]}]}'

$url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
Write-Host "Testing Native Gemini API: $url"
try {
    $resp = Invoke-WebRequest -Method Post -Uri $url -ContentType "application/json" -Body $body
    Write-Host "SUCCESS (Native API)!"
    $resp.Content
} catch {
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $reader.ReadToEnd()
}
