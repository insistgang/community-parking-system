Write-Output "JAVA"
Get-Process java -ErrorAction SilentlyContinue | Format-Table Id,StartTime -AutoSize | Out-String
Write-Output "NODE"
Get-Process node -ErrorAction SilentlyContinue | Format-Table Id,StartTime -AutoSize | Out-String
Write-Output "LISTEN"
netstat -ano | findstr "LISTENING" | findstr ":8080 :5174"
Write-Output "LOGTAIL"
Get-Content E:\parking-management-java\data\backend.log -Tail 25
Write-Output "ERRTAIL"
Get-Content E:\parking-management-java\data\backend.err -Tail 40
Write-Output "FE"
Get-Content E:\parking-management-java\data\frontend.log -Tail 20
Write-Output "FEERR"
Get-Content E:\parking-management-java\data\frontend.err -Tail 20
