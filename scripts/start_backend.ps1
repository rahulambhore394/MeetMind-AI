$pgPort = 5432
$conn5433 = Get-NetTCPConnection -LocalPort 5433 -State Listen -ErrorAction SilentlyContinue
if ($conn5433) {
    $pgPort = 5433
}
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:$pgPort/meetmind"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "password"
$env:SPRING_REDIS_HOST = "localhost"

Set-Location -Path "d:\MeetMind_AI\meetmind-backend"
.\mvnw.cmd spring-boot:run
