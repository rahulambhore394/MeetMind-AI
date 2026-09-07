$baseUrl = "http://localhost:8080/api"

$ts = Get-Date -Format "yyyyMMddHHmmss"
$hostBody = @{ name = "Host Leader"; email = "host_$ts@meetmind.ai"; password = "Password123!" } | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -Body $hostBody -ContentType "application/json" | Out-Null
$loginBody = @{ email = "host_$ts@meetmind.ai"; password = "Password123!" } | ConvertTo-Json
$hostRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"

$mBody = @{ title = "Debug Meeting"; scheduledAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss") } | ConvertTo-Json
$meeting = Invoke-RestMethod -Uri "$baseUrl/meetings" -Method Post -Headers @{ Authorization = "Bearer $($hostRes.accessToken)" } -Body $mBody -ContentType "application/json"

Write-Host "Created meeting ID: $($meeting.id)"

try {
    $report = Invoke-RestMethod -Uri "$baseUrl/meetings/$($meeting.id)/comprehensive-report" -Method Get -Headers @{ Authorization = "Bearer $($hostRes.accessToken)" }
    Write-Host "SUCCESS Report Title: $($report.title)"
} catch {
    Write-Host "ERROR EXCEPTION: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "RESPONSE BODY: $($reader.ReadToEnd())"
    }
}
