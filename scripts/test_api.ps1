$loginBody = @{
    email = "testuser_live@meetmind.ai"
    password = "Password123!"
} | ConvertTo-Json

try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    Write-Host "Login success! AccessToken: $($res.accessToken.Substring(0, 20))..."
    $headers = @{ Authorization = "Bearer $($res.accessToken)" }

    $now = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
    $createMeetingBody = @{
        title = "Live WebRTC Test"
        description = "Testing 2-way WebRTC"
        scheduledAt = $now
        invitedEmails = @()
    } | ConvertTo-Json

    $meeting = Invoke-RestMethod -Uri "http://localhost:8080/api/meetings" -Method Post -Headers $headers -Body $createMeetingBody -ContentType "application/json"
    Write-Host "SUCCESS! Created meeting: ID=$($meeting.id), Code=$($meeting.meetingCode)"

    $getMeetings = Invoke-RestMethod -Uri "http://localhost:8080/api/meetings" -Method Get -Headers $headers
    Write-Host "Total meetings for user: $($getMeetings.Count)"
} catch {
    Write-Host "Error: $($_.Exception.Message)"
}
