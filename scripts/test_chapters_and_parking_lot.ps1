# scripts/test_chapters_and_parking_lot.ps1
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " MeetMind AI: Topic Chapters & Parking Lot Verification   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$randomId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$email = "chapters_tester_$randomId@meetmind.ai"
$password = "password123"

# Step 1: Register and Login
Write-Host "`n[1/7] Registering tester ($email)..." -ForegroundColor Yellow
$regBody = @{
    name = "Chapter & Parking Tester"
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
Write-Host "`n[2/7] Creating meeting for Topic Chapters & Parking Lot testing..." -ForegroundColor Yellow
$meetingPayload = @{
    title = "Q3 Infrastructure & Architecture Review"
    scheduledAt = [DateTime]::UtcNow.AddHours(1).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

$meeting = Invoke-RestMethod -Uri "$baseUrl/api/meetings" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $meetingPayload
$meetingId = $meeting.id
Write-Host "Meeting created: ID=$meetingId ($($meeting.title))" -ForegroundColor Green

# Step 3: Insert transcript segments with agenda chapters, unanswered questions, and parking lot items
Write-Host "`n[3/7] Inserting multi-chapter transcript segments with timestamps..." -ForegroundColor Yellow

$tText = "Alice said: Welcome everyone to our quarterly budget and cloud cost review. Our compute usage increased by 25 percent so we should shift to savings plans. Bob said: Moving on to technical architecture discussion. Who has access to the production AWS billing console? Charlie said: Let's table the database migration to next sprint and put it in the parking lot for now."
$escapedText = $tText.Replace("'", "''")

$insertTranscriptSql = "INSERT INTO transcripts (meeting_id, language, status, full_text, created_at) VALUES ($meetingId, 'en', 'COMPLETED', '$escapedText', NOW()) RETURNING id;"
$transcriptIdRaw = docker exec -i meetmind_ai-postgres-1 psql -U postgres -d meetmind -t -A -c "$insertTranscriptSql"
$transcriptId = [regex]::Match($transcriptIdRaw, "\d+").Value

$insertSegmentsSql = "INSERT INTO transcript_segments (transcript_id, segment_index, speaker, text, start_ms, end_ms, confidence) VALUES " +
    "($transcriptId, 0, 'Alice (Finance)', 'Welcome everyone to our quarterly budget and cloud cost review. Our compute usage increased by 25 percent so we should shift to savings plans.', 0, 315000, 0.98), " +
    "($transcriptId, 1, 'Bob (DevOps)', 'Moving on to technical architecture discussion. Who has access to the production AWS billing console?', 316000, 1110000, 0.96), " +
    "($transcriptId, 2, 'Charlie (Tech Lead)', 'Let''s table the database migration to next sprint and put it in the parking lot for now.', 1111000, 1500000, 0.97);"

docker exec -i meetmind_ai-postgres-1 psql -U postgres -d meetmind -c "$insertSegmentsSql" | Out-Null
Write-Host "Transcript segments (ID=$transcriptId) attached to meeting $meetingId." -ForegroundColor Green

# Step 4: Generate Intelligence
Write-Host "`n[4/7] Generating meeting intelligence summary..." -ForegroundColor Yellow
$generateResp = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/intelligence/generate" -Method Post -Headers $headers
Write-Host "Intelligence generated! Executive Brief: $($generateResp.summary)" -ForegroundColor Green

# Step 5: Test GET /api/meetings/{id}/chapters
Write-Host "`n[5/7] Testing GET /api/meetings/$meetingId/chapters..." -ForegroundColor Yellow
$chapters = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/chapters" -Method Get -Headers $headers
Write-Host "Retrieved $($chapters.Count) Topic Chapters:" -ForegroundColor Green
foreach ($ch in $chapters) {
    Write-Host " - [$($ch.timeRange)] $($ch.title) (Keywords: $($ch.keywords -join ', '))" -ForegroundColor Cyan
}
if ($chapters.Count -eq 0) {
    throw "Verification failed: No topic chapters found!"
}

# Step 6: Test GET /api/meetings/{id}/parking-lot
Write-Host "`n[6/7] Testing GET /api/meetings/$meetingId/parking-lot..." -ForegroundColor Yellow
$parkingItems = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/parking-lot" -Method Get -Headers $headers
Write-Host "Retrieved $($parkingItems.Count) Parking Lot / Question Items:" -ForegroundColor Green
foreach ($item in $parkingItems) {
    Write-Host " - [$($item.status)] $($item.question) (Asked by: $($item.askedBy) @ $($item.timeFormatted))" -ForegroundColor Cyan
}

# Step 7: Test Real-time POST, PATCH, and DELETE on Parking Lot
Write-Host "`n[7/7] Testing Real-time Interactive Management (POST, PATCH, DELETE)..." -ForegroundColor Yellow

# POST: Add new question
$newQuestionPayload = @{
    question = "Do we have rollback automated scripts for Friday cutover?"
    askedBy = "Diana (QA Lead)"
    status = "UNANSWERED"
} | ConvertTo-Json

Write-Host "Adding new question via POST /api/meetings/$meetingId/parking-lot..." -ForegroundColor Yellow
$afterPost = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/parking-lot" -Method Post -Headers $headers -Body $newQuestionPayload
Write-Host "Question added! Total items: $($afterPost.Count)" -ForegroundColor Green

$addedIdx = $afterPost.Count - 1
Write-Host "Newly added item: index=$addedIdx, question='$($afterPost[$addedIdx].question)', status='$($afterPost[$addedIdx].status)'" -ForegroundColor Cyan

# PATCH: Resolve question
$patchPayload = @{
    status = "RESOLVED"
    notes = "Confirmed by Bob - rollback runbook verified in staging"
} | ConvertTo-Json

Write-Host "Resolving question via PATCH /api/meetings/$meetingId/parking-lot/$addedIdx..." -ForegroundColor Yellow
$afterPatch = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/parking-lot/$addedIdx" -Method Patch -Headers $headers -Body $patchPayload
Write-Host "Status updated to: $($afterPatch[$addedIdx].status) (Notes: $($afterPatch[$addedIdx].notes))" -ForegroundColor Green

# DELETE: Remove question
Write-Host "Deleting item via DELETE /api/meetings/$meetingId/parking-lot/$addedIdx..." -ForegroundColor Yellow
$afterDelete = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/parking-lot/$addedIdx" -Method Delete -Headers $headers
Write-Host "Item removed! Final count: $($afterDelete.Count)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host " ALL TOPIC CHAPTER & PARKING LOT TESTS PASSED SUCCESSFULLY! " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
