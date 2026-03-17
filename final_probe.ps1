$apiKey = "AIzaSyBqYISC0yG6ZyiRyFtHLFQtg9lssj-MDTA"
$models = @("gemini-1.5-flash", "models/gemini-1.5-flash")
$endpoints = @(
    "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
    "https://generativelanguage.googleapis.com/v1beta/openai/v1/chat/completions",
    "https://generativelanguage.googleapis.com/v1beta/chat/completions",
    "https://generativelanguage.googleapis.com/v1/chat/completions"
)

foreach ($ep in $endpoints) {
    foreach ($m in $models) {
        $url = "$ep?key=$apiKey"
        $body = @{
            model = $m
            messages = @(
                @{ role = "user"; content = "hello" }
            )
        } | ConvertTo-Json
        
        Write-Host "`n[TEST] URL: $ep | Model: $m" -ForegroundColor Cyan
        try {
            $response = Invoke-RestMethod -Method Post -Uri $url -ContentType "application/json" -Body $body
            Write-Host "[SUCCESS]" -ForegroundColor Green
            $response | ConvertTo-Json
            exit 0 # Stop at first success
        } catch {
            Write-Host "[FAILED] STATUS: $($_.Exception.Response.StatusCode) | MESSAGE: $($_.Exception.Message)" -ForegroundColor Red
            if ($_.Exception.Response) {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                Write-Host "Error Body: $($reader.ReadToEnd())"
            }
        }
    }
}
