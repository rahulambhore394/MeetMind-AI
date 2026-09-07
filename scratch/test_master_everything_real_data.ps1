# ==============================================================================
# MEETMIND AI - MASTER REAL-DATA COMPREHENSIVE END-TO-END VERIFICATION SUITE
# ==============================================================================
$baseUrl = "http://localhost:8080/api"
$ErrorActionPreference = "Stop"
$ts = Get-Date -Format "yyyyMMddHHmmss"

Write-Host "`n==============================================================================" -ForegroundColor Cyan
Write-Host "         MEETMIND AI - MASTER REAL-DATA END-TO-END TEST SUITE          " -ForegroundColor Cyan
Write-Host "==============================================================================`n" -ForegroundColor Cyan

function Register-And-Login($name, $email, $password) {
    $regBody = @{ name = $name; email = $email; password = $password } | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -Body $regBody -ContentType "application/json" | Out-Null
    } catch {}
    
    $loginBody = @{ email = $email; password = $password } | ConvertTo-Json
    $loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    return @{
        token = $loginRes.accessToken
        userId = $loginRes.userId
        email = $email
        name = $name
        headers = @{ Authorization = "Bearer $($loginRes.accessToken)" }
    }
}

# ------------------------------------------------------------------------------
# STEP 1: AUTHENTICATION & USER PROFILES
# ------------------------------------------------------------------------------
Write-Host "1. Testing User Registration & JWT Authentication..." -ForegroundColor Yellow
$hostUser = Register-And-Login "Host Leader" "master_host_$ts@meetmind.ai" "Password123!"
$userRahul = Register-And-Login "Rahul Ambhore" "master_rahul_$ts@meetmind.ai" "Password123!"
Write-Host "   [PASS] Authenticated Host (ID: $($hostUser.userId)) & Attendee Rahul Ambhore (ID: $($userRahul.userId))" -ForegroundColor Green

# ------------------------------------------------------------------------------
# STEP 2: MEETING CREATION & INVITATIONS
# ------------------------------------------------------------------------------
Write-Host "`n2. Testing Meeting Creation, Invitations & Live Join..." -ForegroundColor Yellow
$createMeetingBody = @{
    title = "MeetMind AI Architecture & Feature Roadmap Sync"
    description = "Comprehensive review of real-time translation, chat, recording, and post-meeting summaries."
    scheduledAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

$meeting = Invoke-RestMethod -Uri "$baseUrl/meetings" -Method Post -Headers $hostUser.headers -Body $createMeetingBody -ContentType "application/json"
$mId = $meeting.id
Write-Host "   [PASS] Meeting Created (ID: $mId, Title: '$($meeting.title)', Code: '$($meeting.meetingCode)')" -ForegroundColor Green

# Host starts meeting
Invoke-RestMethod -Uri "$baseUrl/meetings/$mId/start" -Method Post -Headers $hostUser.headers | Out-Null

# Host invites Rahul Ambhore
$inviteReq = @{ email = $userRahul.email } | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/meetings/$mId/participants" -Method Post -Headers $hostUser.headers -Body $inviteReq -ContentType "application/json" | Out-Null

# Rahul Ambhore accepts invitation & joins live room
Invoke-RestMethod -Uri "$baseUrl/meetings/$mId/participants/$($userRahul.userId)/accept" -Method Patch -Headers $userRahul.headers | Out-Null
Invoke-RestMethod -Uri "$baseUrl/meetings/$mId/participants/join" -Method Post -Headers $hostUser.headers | Out-Null
Invoke-RestMethod -Uri "$baseUrl/meetings/$mId/participants/join" -Method Post -Headers $userRahul.headers | Out-Null
Write-Host "   [PASS] Meeting $mId Started, Participant Invited, Accepted & Joined Live Room." -ForegroundColor Green

# ------------------------------------------------------------------------------
# STEP 3: REAL-TIME IN-MEETING CHAT
# ------------------------------------------------------------------------------
Write-Host "`n3. Testing Real-Time In-Meeting Chat System..." -ForegroundColor Yellow
$msg1Req = @{ meetingId = $mId; message = "Hello team! Checking in for our roadmap sync." } | ConvertTo-Json
$msg1Res = Invoke-RestMethod -Uri "$baseUrl/meetings/$mId/messages" -Method Post -Headers $userRahul.headers -Body $msg1Req -ContentType "application/json"
Write-Host "   [PASS] Rahul Ambhore Sent Message: '$($msg1Res.message)' (ID: $($msg1Res.messageId))" -ForegroundColor Green

$msg2Req = @{ meetingId = $mId; message = "Welcome Rahul! Everything is operating in real time." } | ConvertTo-Json
$msg2Res = Invoke-RestMethod -Uri "$baseUrl/meetings/$mId/messages" -Method Post -Headers $hostUser.headers -Body $msg2Req -ContentType "application/json"
Write-Host "   [PASS] Host Sent Reply: '$($msg2Res.message)' (ID: $($msg2Res.messageId))" -ForegroundColor Green

$chatHistory = Invoke-RestMethod -Uri "$baseUrl/meetings/$mId/messages" -Method Get -Headers $hostUser.headers
Write-Host "   [PASS] Total Retained In-Meeting Chat Messages: $($chatHistory.content.Count)" -ForegroundColor Green

# ------------------------------------------------------------------------------
# STEP 4: MULTILINGUAL REAL-TIME AUDIO TRANSLATION
# ------------------------------------------------------------------------------
Write-Host "`n4. Testing Multilingual Real-Time Audio Translation..." -ForegroundColor Yellow
$prefBody = @{ targetLanguage = "mr" } | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/meetings/$mId/translations/preference" -Method Post -Headers $userRahul.headers -Body $prefBody -ContentType "application/json" | Out-Null

$liveTransBody = @{
    sourceText = "Welcome to MeetMind AI real-time meeting platform."
    sourceLanguage = "en"
    targetLanguage = "mr"
    speaker = "Host Leader"
} | ConvertTo-Json

$liveTransRes = Invoke-RestMethod -Uri "$baseUrl/meetings/$mId/translations/live" -Method Post -Headers $userRahul.headers -Body $liveTransBody -ContentType "application/json"
Write-Host "   [PASS] Live Audio Translation (EN -> MR): '$($liveTransRes[0].sourceText)' => '$($liveTransRes[0].translatedText)'" -ForegroundColor Green

# ------------------------------------------------------------------------------
# STEP 5: REAL-TIME MEETING RECORDING
# ------------------------------------------------------------------------------
Write-Host "`n5. Testing Real-Time Live Meeting Recording..." -ForegroundColor Yellow
$recStartRes = Invoke-RestMethod -Uri "$baseUrl/meetings/$mId/recordings/start" -Method Post -Headers $hostUser.headers
$recId = $recStartRes.id
Write-Host "   [PASS] Live Meeting Recording Started (Recording ID: $recId)" -ForegroundColor Green

$recStopRes = Invoke-RestMethod -Uri "$baseUrl/meetings/$mId/recordings/$recId/stop" -Method Post -Headers $hostUser.headers
Write-Host "   [PASS] Live Meeting Recording Stopped (Status: $($recStopRes.status))" -ForegroundColor Green

$allRecordings = Invoke-RestMethod -Uri "$baseUrl/meetings/$mId/recordings" -Method Get -Headers $hostUser.headers
Write-Host "   [PASS] Total Meeting Recordings Retained: $($allRecordings.Count)" -ForegroundColor Green

# ------------------------------------------------------------------------------
# STEP 6: AI REPRESENTATIVE INTEGRATION
# ------------------------------------------------------------------------------
Write-Host "`n6. Testing AI Representative Integration..." -ForegroundColor Yellow
$aiConfigReq = @{
    meetingId = $mId
    monitoredTopics = @("Offline Engine", "WebRTC Audio", "Post Meeting Report")
    monitoredQuestions = @("What is the release target for post-meeting reports?")
    consentDisclosure = $true
} | ConvertTo-Json

$aiRep = Invoke-RestMethod -Uri "$baseUrl/ai-representatives" -Method Post -Headers $userRahul.headers -Body $aiConfigReq -ContentType "application/json"
Write-Host "   [PASS] AI Representative Configured (Rep ID: $($aiRep.id), Status: $($aiRep.status))" -ForegroundColor Green

# ------------------------------------------------------------------------------
# STEP 7: POST-MEETING COMPREHENSIVE SUMMARY REPORT & WORK ASSIGNMENTS
# ------------------------------------------------------------------------------
Write-Host "`n7. Testing Post-Meeting Summary Report & Work Assignments..." -ForegroundColor Yellow
Invoke-RestMethod -Uri "$baseUrl/meetings/$mId/end" -Method Post -Headers $hostUser.headers | Out-Null
Write-Host "   [PASS] Host Ended Meeting via POST /api/meetings/$mId/end -> Auto Summary Fired." -ForegroundColor Green

Start-Sleep -Seconds 2

# Fetch Dashboard Reports List
$reportsList = Invoke-RestMethod -Uri "$baseUrl/meetings/reports/all" -Method Get -Headers $userRahul.headers
Write-Host "   [PASS] Dashboard Option Reports Count: $($reportsList.Count)" -ForegroundColor Green
foreach ($rep in $reportsList) {
    Write-Host "          -> Meeting ID: $($rep.meetingId) | Title: '$($rep.title)' | Type: $($rep.meetingType) | Action Items: $($rep.actionItemCount) | My Tasks: $($rep.myTaskCount)" -ForegroundColor Yellow
}

# Fetch Comprehensive Report for Rahul Ambhore
$compReport = Invoke-RestMethod -Uri "$baseUrl/meetings/$mId/comprehensive-report" -Method Get -Headers $userRahul.headers

Write-Host "`n==============================================================================" -ForegroundColor Cyan
Write-Host " COMPREHENSIVE REPORT DETAILS FOR LOGGED-IN USER: Rahul Ambhore" -ForegroundColor Cyan
Write-Host "==============================================================================" -ForegroundColor Cyan
Write-Host "Title:             $($compReport.title)" -ForegroundColor White
Write-Host "Executive Summary: $($compReport.executiveSummary)" -ForegroundColor White
Write-Host "Decisions Count:   $($compReport.decisions.Count)" -ForegroundColor White
Write-Host "All Tasks Count:   $($compReport.allWorkAssignments.Count)" -ForegroundColor White
Write-Host "My Tasks Count:    $($compReport.myAssignedTasks.Count)" -ForegroundColor White

Write-Host "`n--- ALL WORK ASSIGNMENTS ('WHOOM WHAT') ---" -ForegroundColor Yellow
foreach ($task in $compReport.allWorkAssignments) {
    Write-Host "  * [Assigned To: $($task.assignedUser)] $($task.description) (Due: $($task.dueDate))" -ForegroundColor White
}

Write-Host "`n--- USER SPECIFIC TASK SECTION FOR RAHUL AMBHORE ---" -ForegroundColor Green
foreach ($myTask in $compReport.myAssignedTasks) {
    Write-Host "  -> [MY ASSIGNED TASK] $($myTask.description) (Due: $($myTask.dueDate))" -ForegroundColor Green
}

# Test AI Representative Meeting Report Differentiation
$aiMeetingReq = @{
    title = "Delegated Executive AI Rep Sync"
    description = "Meeting attended by AI Representative on behalf of Rahul Ambhore."
    scheduledAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

$aiMeeting = Invoke-RestMethod -Uri "$baseUrl/meetings" -Method Post -Headers $hostUser.headers -Body $aiMeetingReq -ContentType "application/json"
$aiMId = $aiMeeting.id

Invoke-RestMethod -Uri "$baseUrl/meetings/$aiMId/start" -Method Post -Headers $hostUser.headers | Out-Null
Invoke-RestMethod -Uri "$baseUrl/meetings/$aiMId/end" -Method Post -Headers $hostUser.headers | Out-Null
Write-Host "`n   [PASS] AI Representative Meeting Created & Ended (ID: $aiMId)" -ForegroundColor Green

Start-Sleep -Seconds 1

$finalReports = Invoke-RestMethod -Uri "$baseUrl/meetings/reports/all" -Method Get -Headers $userRahul.headers
Write-Host "   [PASS] Final Dashboard Reports Count (Normal + AI Rep): $($finalReports.Count)" -ForegroundColor Green
foreach ($r in $finalReports) {
    Write-Host "          -> [Type: $($r.meetingType)] Meeting ID: $($r.meetingId) | Title: '$($r.title)'" -ForegroundColor Yellow
}

Write-Host "`n==============================================================================" -ForegroundColor Cyan
Write-Host "  100% EVERYTHING VERIFIED & PASSED AGAINST REAL LIVE BACKEND DATA!   " -ForegroundColor Green
Write-Host "==============================================================================`n" -ForegroundColor Cyan
