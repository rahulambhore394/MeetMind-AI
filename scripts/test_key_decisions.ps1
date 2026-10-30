# scripts/test_key_decisions.ps1
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " MeetMind AI: Key Decision & Agreement Tracking Real-time " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$randomId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$email = "decision_tester_$randomId@meetmind.ai"
$password = "password123"

# Step 1: Register and Login
Write-Host "`n[1/6] Registering tester ($email)..." -ForegroundColor Yellow
$regBody = @{
    name = "Decision Tester"
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
Write-Host "`n[2/6] Creating meeting for Key Decision & Agreement Tracking..." -ForegroundColor Yellow
$meetingPayload = @{
    title = "Architecture & Roadmap Consensus Review"
    scheduledAt = [DateTime]::UtcNow.AddHours(1).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

$meeting = Invoke-RestMethod -Uri "$baseUrl/api/meetings" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $meetingPayload
$meetingId = $meeting.id
Write-Host "Meeting created: ID=$meetingId ($($meeting.title))" -ForegroundColor Green

# Step 3: Insert transcript with consensus moments
Write-Host "`n[3/6] Inserting transcript with consensus cues ('We settled on', 'Approved', 'Let''s go with Option B')..." -ForegroundColor Yellow
$transcriptText = "Dave said: We settled on PostgreSQL 15 over MongoDB for the meeting intelligence persistence layer. Host said: Everyone approved a bi-weekly release sprint schedule starting November 1st. Alice said: Let's go with Option B for the high-availability failover strategy."

$escapedTranscript = $transcriptText.Replace("'", "''")
$insertTranscriptSql = "INSERT INTO transcripts (meeting_id, language, status, full_text, created_at) VALUES ($meetingId, 'en', 'COMPLETED', '$escapedTranscript', NOW()) RETURNING id;"
$transcriptIdRaw = docker exec -i meetmind_ai-postgres-1 psql -U postgres -d meetmind -t -A -c "$insertTranscriptSql"
$transcriptId = [regex]::Match($transcriptIdRaw, "\d+").Value

$insertSegmentsSql = "INSERT INTO transcript_segments (transcript_id, segment_index, speaker, text, start_ms, end_ms, confidence) VALUES ($transcriptId, 0, 'Dave', 'We settled on PostgreSQL 15 over MongoDB for the meeting intelligence persistence layer.', 0, 15000, 0.98), ($transcriptId, 1, 'Host', 'Everyone approved a bi-weekly release sprint schedule starting November 1st.', 15000, 35000, 0.96), ($transcriptId, 2, 'Alice', 'Let''s go with Option B for the high-availability failover strategy.', 35000, 55000, 0.97);"
docker exec -i meetmind_ai-postgres-1 psql -U postgres -d meetmind -c "$insertSegmentsSql" | Out-Null
Write-Host "Transcript segments (ID=$transcriptId) attached to meeting $meetingId." -ForegroundColor Green

# Step 4: Trigger Intelligence Generation (Automated Extraction)
Write-Host "`n[4/6] Triggering Intelligence generation & consensus detection..." -ForegroundColor Yellow
$genPayload = @{ transcriptId = [long]$transcriptId } | ConvertTo-Json
$summary = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/intelligence/generate" -Method Post -Headers $headers -ContentType "application/json" -Body $genPayload

Write-Host "Intelligence Generated Successfully!" -ForegroundColor Green
Write-Host "Executive Brief: $($summary.summary)" -ForegroundColor Gray
Write-Host "`nExtracted Key Decisions & Agreements ($($summary.decisions.Count)):" -ForegroundColor Cyan

if ($summary.decisions.Count -lt 2) {
    Write-Error "FAILED: Expected at least 2 key decisions, but got $($summary.decisions.Count)"
}

$i = 1
foreach ($decision in $summary.decisions) {
    Write-Host "  [$i] $decision" -ForegroundColor White
    $i++
}

# Step 5: Test GET /api/meetings/{id}/decisions
Write-Host "`n[5/6] Testing GET /api/meetings/{id}/decisions..." -ForegroundColor Yellow
$getDecisionsResp = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/decisions" -Method Get -Headers $headers
Write-Host "Retrieved $($getDecisionsResp.Count) official decisions from REST endpoint." -ForegroundColor Green

# Step 6: Test Real-time Interactive Management (POST & DELETE)
Write-Host "`n[6/6] Testing Interactive Management (POST & DELETE /api/meetings/{id}/decisions)..." -ForegroundColor Yellow

# 6a. Record new manual agreement
$manualAgreement = "Signed off on zero-downtime database migration strategy on November 5th."
Write-Host "Recording new agreement: '$manualAgreement'..." -ForegroundColor Yellow
$postDecisionPayload = @{ decision = $manualAgreement } | ConvertTo-Json
$postDecisionsResp = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/decisions" -Method Post -Headers $headers -ContentType "application/json" -Body $postDecisionPayload

if (-not ($postDecisionsResp -contains $manualAgreement)) {
    Write-Error "FAILED: Expected posted agreement to be present in decision list."
}
Write-Host "Agreement successfully recorded! Total decisions now: $($postDecisionsResp.Count)" -ForegroundColor Green

# 6b. Delete the agreement by index
$lastIndex = $postDecisionsResp.Count - 1
Write-Host "Deleting decision at index $lastIndex..." -ForegroundColor Yellow
$deleteDecisionsResp = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/decisions/$lastIndex" -Method Delete -Headers $headers

if ($deleteDecisionsResp -contains $manualAgreement) {
    Write-Error "FAILED: Expected deleted agreement to be removed from decision list."
}
Write-Host "Agreement successfully deleted! Total decisions now: $($deleteDecisionsResp.Count)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host " ALL KEY DECISION & AGREEMENT TRACKING CHECKS PASSED!     " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
