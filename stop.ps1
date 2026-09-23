$root = "E:\parking-management-java"
Get-CimInstance Win32_Process | Where-Object {
  $_.CommandLine -and (
    $_.CommandLine -like "*parking-java-1.0.0.jar*" -or
    $_.CommandLine -like "*parking-management-java*"
  )
} | ForEach-Object {
  Write-Output "STOP $($_.ProcessId) $($_.Name)"
  Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
}
Start-Sleep -Seconds 1
Write-Output "---- listen ----"
netstat -ano | findstr "LISTENING" | findstr ":8080 :5174"
if (-not $?) { Write-Output "8080 and 5174 are not listening" }
