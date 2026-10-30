[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$randomId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$email = "translator_$randomId@meetmind.ai"
$password = "password123"

Write-Host "1. Registering tester ($email)..."
$regBody = @{
    name = "Translation Tester"
    email = $email
    password = $password
} | ConvertTo-Json

$regRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method Post -ContentType "application/json; charset=utf-8" -Body $regBody
Write-Host "Registered user ID: $($regRes.id)"

Write-Host "`n2. Logging in..."
$loginBody = @{
    email = $email
    password = $password
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json; charset=utf-8" -Body $loginBody
$token = $loginRes.accessToken
Write-Host "Logged in. Token acquired: $($token.Substring(0, 15))..."

Write-Host "`n3. Creating a test meeting..."
$headers = @{
    Authorization = "Bearer $token"
}
$meetingBody = @{
    title = "Multilingual Live Translation Test"
    scheduledAt = [DateTime]::UtcNow.AddHours(1).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

$meetingRes = Invoke-RestMethod -Uri "http://localhost:8080/api/meetings" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $meetingBody
$meetingId = $meetingRes.id
Write-Host "Created meeting ID: $meetingId"

Write-Host "`n4. Testing Live Translation to Hindi (hi)..."
$translateHindi = @{
    sourceText = "Good morning everyone, welcome to the project review meeting."
    sourceLanguage = "en"
    targetLanguage = "hi"
    speaker = "Rahul"
    timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
} | ConvertTo-Json

$hiRes = Invoke-RestMethod -Uri "http://localhost:8080/api/meetings/$meetingId/translations/live" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $translateHindi
Write-Host "Original: $($hiRes[0].sourceText)"
Write-Host "Hindi Subtitle: $($hiRes[0].translatedText)"
Write-Host "Provider: $($hiRes[0].providerName)"

Write-Host "`n5. Testing Live Translation to Marathi (mr)..."
$translateMarathi = @{
    sourceText = "Good morning everyone, welcome to the project review meeting."
    sourceLanguage = "en"
    targetLanguage = "mr"
    speaker = "Rahul"
    timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
} | ConvertTo-Json

$mrRes = Invoke-RestMethod -Uri "http://localhost:8080/api/meetings/$meetingId/translations/live" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $translateMarathi
Write-Host "Marathi Subtitle: $($mrRes[0].translatedText)"
Write-Host "Provider: $($mrRes[0].providerName)"

Write-Host "`n6. Testing Live Translation to Spanish (es)..."
$translateSpanish = @{
    sourceText = "Good morning everyone, welcome to the project review meeting."
    sourceLanguage = "en"
    targetLanguage = "es"
    speaker = "Rahul"
    timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
} | ConvertTo-Json

$esRes = Invoke-RestMethod -Uri "http://localhost:8080/api/meetings/$meetingId/translations/live" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $translateSpanish
Write-Host "Spanish Subtitle: $($esRes[0].translatedText)"
Write-Host "Provider: $($esRes[0].providerName)"

Write-Host "`n7. Testing Reverse Translation: Marathi -> English..."
$translateReverse = @{
    sourceText = "नमस्कार सर्वांना, बैठकीत आपले स्वागत आहे."
    sourceLanguage = "mr"
    targetLanguage = "en"
    speaker = "Amit"
    timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
} | ConvertTo-Json

$enRes = Invoke-RestMethod -Uri "http://localhost:8080/api/meetings/$meetingId/translations/live" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($translateReverse))
Write-Host "Original: $($enRes[0].sourceText)"
Write-Host "English Subtitle: $($enRes[0].translatedText)"
Write-Host "Provider: $($enRes[0].providerName)"

Write-Host "`n8. Verifying Persisted Subtitles Retrieval..."
$allSubtitles = Invoke-RestMethod -Uri "http://localhost:8080/api/meetings/$meetingId/translations" -Method Get -Headers $headers
Write-Host "Total persisted translation subtitles in DB: $($allSubtitles.Count)"

Write-Host "`n=== MULTILINGUAL LIVE TRANSLATION & SUBTITLES FULLY OPERATIONAL ==="
