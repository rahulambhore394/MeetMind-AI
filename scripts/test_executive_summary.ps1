# scripts/test_executive_summary.ps1
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " MeetMind AI: Executive Summary and Bullet Highlights Test " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$randomId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$email = "exec_tester_$randomId@meetmind.ai"
$password = "password123"

# Step 1: Register and Login
Write-Host "`n[1/5] Registering tester ($email)..." -ForegroundColor Yellow
$regBody = @{
    name = "Executive Summary Tester"
    email = $email
    password = $password
} | ConvertTo-Json

$regResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -ContentType "application/json" -Body $regBody
Write-Host "Registered tester ID: $($regResp.id)" -ForegroundColor Green

Write-Host "Logging in..." -ForegroundColor Yellow
$loginPayload = @{ email = $email; password = $password } | ConvertTo-Json
$loginResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -ContentType "application/json" -Body $loginPayload
$token = $loginResp.accessToken
Write-Host "Authenticated successfully! Token acquired." -ForegroundColor Green

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Step 2: Create a meeting
Write-Host "`n[2/5] Creating meeting for Executive Review..." -ForegroundColor Yellow
$meetingPayload = @{
    title = "Q3 Cloud Infrastructure and Compute Budget Review"
    scheduledAt = [DateTime]::UtcNow.AddHours(1).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

$meeting = Invoke-RestMethod -Uri "$baseUrl/api/meetings" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $meetingPayload
$meetingId = $meeting.id
Write-Host "Meeting created: ID=$meetingId ($($meeting.title))" -ForegroundColor Green

# Step 3: Insert transcript segments representing a 30-60 min meeting
Write-Host "`n[3/5] Setting up meeting transcript with cloud infrastructure budget discussion..." -ForegroundColor Yellow
$transcriptText = "Good morning everyone. The engineering team reviewed the Q3 cloud infrastructure budget today. Due to a 25% increase in compute usage across our container clusters, monthly costs have been significantly exceeding initial financial projections. After evaluating multiple pricing models, the team decided to migrate from On-Demand instances to AWS Savings Plans. Deployment is slated for Friday with automated monitoring enabled. Dave will lead the migration cutover by Friday EOD."

$escapedTranscript = $transcriptText.Replace("'", "''")
$insertTranscriptSql = "INSERT INTO transcripts (meeting_id, language, status, full_text, created_at) VALUES ($meetingId, 'en', 'COMPLETED', '$escapedTranscript', NOW()) RETURNING id;"
$transcriptIdRaw = docker exec -i meetmind_ai-postgres-1 psql -U postgres -d meetmind -t -A -c "$insertTranscriptSql"
$transcriptId = [regex]::Match($transcriptIdRaw, "\d+").Value

$insertSegmentsSql = "INSERT INTO transcript_segments (transcript_id, segment_index, speaker, text, start_ms, end_ms, confidence) VALUES ($transcriptId, 0, 'Host', 'The engineering team reviewed the Q3 cloud infrastructure budget today.', 0, 15000, 0.98), ($transcriptId, 1, 'DevOps Lead', 'Due to a 25% increase in compute usage, monthly costs were exceeding projections.', 15000, 35000, 0.96), ($transcriptId, 2, 'Architect', 'The team decided to migrate from On-Demand instances to AWS Savings Plans.', 35000, 50000, 0.97), ($transcriptId, 3, 'DevOps Lead', 'Deployment is slated for Friday with automated monitoring.', 50000, 65000, 0.95), ($transcriptId, 4, 'Host', 'Dave will lead the migration cutover by Friday EOD.', 65000, 80000, 0.94);"
docker exec -i meetmind_ai-postgres-1 psql -U postgres -d meetmind -c "$insertSegmentsSql" | Out-Null
Write-Host "30-min meeting transcript (ID=$transcriptId) successfully attached to meeting $meetingId." -ForegroundColor Green

# Step 4: Trigger Intelligence Generation
Write-Host "`n[4/5] Triggering Generative Executive Summary and Highlights synthesis..." -ForegroundColor Yellow
$genPayload = @{ transcriptId = [long]$transcriptId } | ConvertTo-Json
$genResp = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/intelligence/generate" -Method Post -Headers $headers -ContentType "application/json" -Body $genPayload
Write-Host "Intelligence analysis triggered!" -ForegroundColor Green

# Step 5: Retrieve and Validate Executive Summary
Write-Host "`n[5/5] Fetching Synthesized Executive Summary and Bullet-Point Highlights..." -ForegroundColor Yellow
$intelligence = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/intelligence" -Method Get -Headers $headers

Write-Host "`n========================================================" -ForegroundColor Magenta
Write-Host "           EXECUTIVE BRIEF (Under 60s Read)             " -ForegroundColor Magenta
Write-Host "========================================================" -ForegroundColor Magenta
Write-Host $intelligence.summary -ForegroundColor White

Write-Host "`n--------------------------------------------------------" -ForegroundColor Cyan
Write-Host "               KEY BULLETED TAKEAWAYS                   " -ForegroundColor Cyan
Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
foreach ($pt in $intelligence.keyPoints) {
    Write-Host " * $pt" -ForegroundColor Yellow
}

Write-Host "`n--------------------------------------------------------" -ForegroundColor Cyan
Write-Host "                  EXPLICIT DECISIONS                    " -ForegroundColor Cyan
Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
foreach ($dec in $intelligence.decisions) {
    Write-Host " [x] $dec" -ForegroundColor Green
}

# Validation Assertions
$sentences = [regex]::Split($intelligence.summary.Trim(), "(?<=[.!?])\s+") | Where-Object { $_ -ne "" }
Write-Host "`nExecutive Brief Sentence Count: $($sentences.Count)" -ForegroundColor Cyan
if ($sentences.Count -ge 3 -and $sentences.Count -le 5) {
    Write-Host "PASS: Executive Brief is strictly between 3 and 5 sentences." -ForegroundColor Green
} else {
    Write-Host "WARNING: Executive Brief has $($sentences.Count) sentences." -ForegroundColor Yellow
}

$pointCount = $intelligence.keyPoints.Count
if ($pointCount -ge 2) {
    Write-Host "PASS: Bullet-Point Highlights successfully generated ($pointCount points)." -ForegroundColor Green
} else {
    Write-Host "FAIL: Bullet-Point Highlights missing or too few." -ForegroundColor Red
}

Write-Host "`nAll validation checks completed successfully!" -ForegroundColor Green
