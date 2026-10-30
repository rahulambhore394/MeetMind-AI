$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
while ($true) {
    try {
        & $adb reverse tcp:8080 tcp:8080 2>$null
    } catch {
        # ignore
    }
    Start-Sleep -Seconds 2
}
