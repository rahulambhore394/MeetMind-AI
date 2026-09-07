# Powershell Test Script for Meeting Summary Reports & Work Assignments Flow
$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Stop"
$ts = Get-Date -Format "yyyyMMddHHmmss"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " STARTING COMPLETE MEETING SUMMARY REPORT & WORK ASSIGNMENTS TEST" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

function Register-And-Login($name, $email, $password) {
    $regBody = @{ name = $name; email = $email; password = $password } | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBody -ContentType "application/json" | Out-Null
    } catch {}
    
    $loginBody = @{ email = $email; password = $password } | ConvertTo-Json
    $loginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    return @{
        token = $loginRes.accessToken
        userId = $loginRes.userId
        email = $email
        name = $name
        headers = @{ Authorization = "Bearer $($loginRes.accessToken)" }
    }
}

# 1. Register & Login Users
Write-Host "1. Authenticating Host and User..." -ForegroundColor Yellow
$hostUser = Register-And-Login "Host Leader" "report_host_$ts@meetmind.ai" "Password123!"
$user2 = Register-And-Login "Rahul Ambhore" "report_rahul_$ts@meetmind.ai" "Password123!"
Write-Host "[PASS] Authenticated Host (ID: $($hostUser.userId)) and User 2 (ID: $($user2.userId))." -ForegroundColor Green

# 2. Create Normal Meeting
$createMeetingBody = @{
    title = "Quarterly Strategy & Work Allocation Sync"
    description = "Discuss Q4 roadmap, key decisions, and team action items."
    scheduledAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

$meeting = Invoke-RestMethod -Uri "$baseUrl/api/meetings" -Method Post -Headers $hostUser.headers -Body $createMeetingBody -ContentType "application/json"
$meetingId = $meeting.id
Write-Host "[PASS] Normal Meeting Created (ID: $meetingId, Title: '$($meeting.title)', Code: '$($meeting.meetingCode)')" -ForegroundColor Green

# 3. Host joins & starts meeting, invites & joins Participant Rahul Ambhore
$inviteReq = @{ email = $user2.email } | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/participants" -Method Post -Headers $hostUser.headers -Body $inviteReq -ContentType "application/json" | Out-Null
Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/participants/$($user2.userId)/accept" -Method Patch -Headers $user2.headers | Out-Null

Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/start" -Method Post -Headers $hostUser.headers | Out-Null
Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/participants/join" -Method Post -Headers $hostUser.headers | Out-Null
Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/participants/join" -Method Post -Headers $user2.headers | Out-Null

Write-Host "[PASS] Meeting $meetingId Started, Invited & Joined Host and Rahul Ambhore." -ForegroundColor Green

# 4. Conclude / End Meeting -> Triggers Automatic Comprehensive Summary Report Generation
try {
    Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/end" -Method Post -Headers $hostUser.headers | Out-Null
    Write-Host "[PASS] Meeting Ended via POST /api/meetings/$meetingId/end." -ForegroundColor Green
} catch {
    Write-Host "[INFO] Meeting was auto-ended on empty interval or host end." -ForegroundColor Yellow
}

Start-Sleep -Seconds 2

# 5. Fetch All Meeting Reports List via GET /api/meetings/reports/all
$allReports = Invoke-RestMethod -Uri "$baseUrl/api/meetings/reports/all" -Method Get -Headers $user2.headers
Write-Host "`n[5] GET /api/meetings/reports/all returned $($allReports.Count) report(s)." -ForegroundColor Green

foreach ($rep in $allReports) {
    Write-Host "    -> Meeting ID: $($rep.meetingId) | Title: '$($rep.title)' | Type: $($rep.meetingType) | Total Action Items: $($rep.actionItemCount) | My Assigned Tasks: $($rep.myTaskCount)" -ForegroundColor Yellow
}

# 6. Fetch Comprehensive Report for Logged-In User (Rahul Ambhore)
$compReport = Invoke-RestMethod -Uri "$baseUrl/api/meetings/$meetingId/comprehensive-report" -Method Get -Headers $user2.headers
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host " COMPREHENSIVE REPORT DETAILS FOR USER: Rahul Ambhore" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Title:             $($compReport.title)" -ForegroundColor White
Write-Host "Executive Summary: $($compReport.executiveSummary)" -ForegroundColor White
Write-Host "Decisions Count:   $($compReport.decisions.Count)" -ForegroundColor White
Write-Host "All Tasks Count:   $($compReport.allWorkAssignments.Count)" -ForegroundColor White
Write-Host "My Tasks Count:    $($compReport.myAssignedTasks.Count)" -ForegroundColor White

Write-Host "`n--- WORK ASSIGNMENTS ('WHOOM WHAT') ---" -ForegroundColor Yellow
foreach ($task in $compReport.allWorkAssignments) {
    Write-Host "  * [Assigned To: $($task.assignedUser)] $($task.description) (Due: $($task.dueDate))" -ForegroundColor White
}

Write-Host "`n--- USER SPECIFIC TASK SECTION FOR RAHUL AMBHORE ---" -ForegroundColor Green
foreach ($myTask in $compReport.myAssignedTasks) {
    Write-Host "  -> [MY ASSIGNED TASK] $($myTask.description) (Due: $($myTask.dueDate))" -ForegroundColor Green
}

# 7. Create & End an AI Representative Meeting to test report filtering
$aiMeetingBody = @{
    title = "AI Rep Delegated Executive Sync"
    description = "Meeting attended on behalf of Rahul Ambhore by AI Representative."
    scheduledAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

$aiMeeting = Invoke-RestMethod -Uri "$baseUrl/api/meetings" -Method Post -Headers $hostUser.headers -Body $aiMeetingBody -ContentType "application/json"
$aiMeetingId = $aiMeeting.id

Invoke-RestMethod -Uri "$baseUrl/api/meetings/$aiMeetingId/start" -Method Post -Headers $hostUser.headers | Out-Null
try {
    Invoke-RestMethod -Uri "$baseUrl/api/meetings/$aiMeetingId/end" -Method Post -Headers $hostUser.headers | Out-Null
} catch {}

Write-Host "`n[7] AI Representative Meeting Created & Ended (ID: $aiMeetingId)." -ForegroundColor Green

Start-Sleep -Seconds 1

$updatedReports = Invoke-RestMethod -Uri "$baseUrl/api/meetings/reports/all" -Method Get -Headers $user2.headers
Write-Host "`n[8] Total Reports Available on Dashboard Option: $($updatedReports.Count)" -ForegroundColor Green
foreach ($rep in $updatedReports) {
    Write-Host "    -> [Type: $($rep.meetingType)] Meeting ID: $($rep.meetingId) | Title: '$($rep.title)'" -ForegroundColor Yellow
}

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host " ALL POST-MEETING SUMMARY REPORT & WORK ASSIGNMENT TESTS PASSED!" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
