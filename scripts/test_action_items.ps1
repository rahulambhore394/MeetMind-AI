# scripts/test_action_items.ps1
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " MeetMind AI: Automated Action Item Extraction & Tracking " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$randomId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$email = "action_tester_$randomId@meetmind.ai"
$password = "password123"

# Step 1: Register and Login
Write-Host "`n[1/5] Registering tester ($email)..." -ForegroundColor Yellow
$regBody = @{
    name = "Action Items Tester"
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
Write-Host "`n[2/5] Creating meeting for Action Item Extraction..." -ForegroundColor Yellow
$meetingPayload = @{
    title = "Sprint Planning & Architecture Sync"
    scheduledAt = [DateTime]::UtcNow.AddHours(1).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

$meeting = Invoke-RestMethod -Uri "$baseUrl/api/meetings" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $meetingPayload
$meetingId = $meeting.id
Write-Host "Meeting created: ID=$meetingId ($($meeting.title))" -ForegroundColor Green

# Step 3: Insert transcript segments containing linguistic cues
Write-Host "`n[3/5] Inserting spoken transcript with linguistic cues ('I will prepare', 'assigned to', 'due next Monday')..." -ForegroundColor Yellow
$transcriptText = "Dave said: I will prepare the production deployment checklist by Friday. Project Manager said: The task is assigned to Alice to finalize the security audit due next Monday. Bob said: I will coordinate the load testing with the DevOps team by tomorrow."

$escapedTranscript = $transcriptText.Replace("'", "''")
$insertTranscriptSql = "INSERT INTO transcripts (meeting_id, language, status, full_text, created_at) VALUES ($meetingId, 'en', 'COMPLETED', '$escapedTranscript', NOW()) RETURNING id;"
$transcriptIdRaw = docker exec -i meetmind_ai-postgres-1 psql -U postgres -d meetmind -t -A -c "$insertTranscriptSql"
$transcriptId = [regex]::Match($transcriptIdRaw, "\d+").Value

$insertSegmentsSql = "INSERT INTO transcript_segments (transcript_id, segment_index, speaker, text, start_ms, end_ms, confidence) VALUES ($transcriptId, 0, 'Dave', 'I will prepare the production deployment checklist by Friday.', 0, 15000, 0.98), ($transcriptId, 1, 'Project Manager', 'The task is assigned to Alice to finalize the security audit due next Monday.', 15000, 35000, 0.96), ($transcriptId, 2, 'Bob', 'I will coordinate the load testing with the DevOps team by tomorrow.', 35000, 55000, 0.97);"
docker exec -i meetmind_ai-postgres-1 psql -U postgres -d meetmind -c "$insertSegmentsSql" | Out-Null
Write-Host "Transcript segments (ID=$transcriptId) attached to meeting $meetingId." -ForegroundColor Green

# Step 4: Trigger Intelligence Generation (Extraction)
$genPayload = @{ transcriptId = [long]$transcriptId } | ConvertTo-Json
$summary = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/intelligence/generate" -Method Post -Headers $headers -ContentType "application/json" -Body $genPayload

Write-Host "Intelligence Generated Successfully!" -ForegroundColor Green
Write-Host "Executive Brief: $($summary.summary)" -ForegroundColor Gray
Write-Host "`nExtracted Action Items ($($summary.actionItems.Count)):" -ForegroundColor Cyan

if ($summary.actionItems.Count -eq 0) {
    Write-Error "FAILED: Expected action items to be extracted from linguistic cues, but got 0."
}

foreach ($item in $summary.actionItems) {
    Write-Host "  - [ID: $($item.id)] $($item.description)" -ForegroundColor White
    Write-Host "      * Assignee : $($item.assignedUser)" -ForegroundColor Cyan
    Write-Host "      * Deadline : $($item.dueDate)" -ForegroundColor Yellow
    Write-Host "      * Status   : $($item.status)" -ForegroundColor Magenta
    Write-Host "      * Conf     : $([Math]::Round($item.confidence * 100))%" -ForegroundColor DarkGray
}

# Step 5: Interactive Management (PATCH status transitions)
Write-Host "`n[5/5] Testing Interactive Management via PATCH /api/meetings/{id}/action-items/{id}..." -ForegroundColor Yellow
$testItem = $summary.actionItems[0]
$itemId = $testItem.id

# Transition 1: PENDING -> IN_PROGRESS
Write-Host "Transitioning item $itemId to IN_PROGRESS..." -ForegroundColor Yellow
$patchBody1 = @{ status = "IN_PROGRESS" } | ConvertTo-Json
$patchResp1 = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/action-items/$itemId" -Method Patch -Headers $headers -ContentType "application/json" -Body $patchBody1
if ($patchResp1.status -ne "IN_PROGRESS") {
    Write-Error "FAILED: Expected status IN_PROGRESS, got $($patchResp1.status)"
}
Write-Host "Successfully updated item $itemId to status: $($patchResp1.status)" -ForegroundColor Green

# Transition 2: IN_PROGRESS -> COMPLETED
Write-Host "Transitioning item $itemId to COMPLETED..." -ForegroundColor Yellow
$patchBody2 = @{ status = "COMPLETED" } | ConvertTo-Json
$patchResp2 = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/action-items/$itemId" -Method Patch -Headers $headers -ContentType "application/json" -Body $patchBody2
if ($patchResp2.status -ne "COMPLETED") {
    Write-Error "FAILED: Expected status COMPLETED, got $($patchResp2.status)"
}
Write-Host "Successfully checked off completed task $itemId status: $($patchResp2.status)" -ForegroundColor Green

# Verify via Summary GET
Write-Host "Verifying intelligence report reflects updated task status..." -ForegroundColor Yellow
$verifySummary = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/intelligence" -Method Get -Headers $headers
$updatedItem = $verifySummary.actionItems | Where-Object { $_.id -eq $itemId }
if ($updatedItem.status -ne "COMPLETED") {
    Write-Error "FAILED: Intelligence actionItem status not updated in database. Got: $($updatedItem.status)"
}
Write-Host "Verification PASSED! Updated item $itemId status in database is $($updatedItem.status)" -ForegroundColor Green

# Verify via Action Items List GET
Write-Host "Verifying dedicated action items endpoint /api/meetings/$meetingId/action-items..." -ForegroundColor Yellow
$actionItemsList = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/action-items" -Method Get -Headers $headers
$listedItem = $actionItemsList | Where-Object { $_.id -eq $itemId }
if ($listedItem.status -ne "COMPLETED") {
    Write-Error "FAILED: Action item status in list is not COMPLETED. Got: $($listedItem.status)"
}
Write-Host "Verification PASSED! Dedicated action-items list reflects $($listedItem.status)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host " ALL TESTS PASSED: Action Item Extraction & Tracking Ready! " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
