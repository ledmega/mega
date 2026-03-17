$apiKey = "AIzaSyBqYISC0yG6ZyiRyFtHLFQtg9lssj-MDTA"
$body = @{
    contents = @(
        @{ parts = @( @{ text = "hi" } ) }
    )
} | ConvertTo-Json

$url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

Write-Host "Testing Native Gemini URL: $url"
try {
    $response = Invoke-RestMethod -Method Post -Uri $url -ContentType "application/json" -Body $body
    Write-Host "SUCCESS (Native API Works)!"
    $response.candidates[0].content.parts[0].text
} catch {
    Write-Host "FAILED: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errBody = $reader.ReadToEnd()
        Write-Host "Error Response: $errBody"
    }
}
