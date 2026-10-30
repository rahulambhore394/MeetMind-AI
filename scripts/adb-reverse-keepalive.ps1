# MeetMind AI - ADB Reverse Keep-Alive Daemon
$adb = "C:\Users\rahul\AppData\Local\Android\Sdk\platform-tools\adb.exe"
Write-Host "Starting ADB Reverse Keep-Alive service..."

while ($true) {
    try {
        $devices = & $adb devices
        if ($devices -match "device\b") {
            $reverseList = & $adb reverse --list 2>$null
            if ($reverseList -notmatch "tcp:8080 tcp:8080") {
                Write-Host "Re-establishing adb reverse tcp:8080 tcp:8080..."
                & $adb reverse tcp:8080 tcp:8080
            }
        }
    } catch {
        # Ignore transient USB poll errors
    }
    Start-Sleep -Seconds 5
}
