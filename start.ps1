$root = "E:\parking-management-java"
New-Item -ItemType Directory -Force -Path "$root\data" | Out-Null
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
Get-NetTCPConnection -LocalPort 5174 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
Start-Process -FilePath "java" -ArgumentList "-jar","$root\target\parking-java-1.0.0.jar" -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput "$root\data\backend.log" -RedirectStandardError "$root\data\backend.err"
Start-Process -FilePath "npm.cmd" -ArgumentList "run","dev","--","--host","0.0.0.0","--port","5174" -WorkingDirectory "$root\frontend" -WindowStyle Hidden -RedirectStandardOutput "$root\data\frontend.log" -RedirectStandardError "$root\data\frontend.err"
Start-Sleep -Seconds 8
Write-Output "---- backend err ----"
Get-Content "$root\data\backend.err" -Tail 30 -ErrorAction SilentlyContinue
Write-Output "---- frontend err ----"
Get-Content "$root\data\frontend.err" -Tail 30 -ErrorAction SilentlyContinue
Write-Output "---- listen ----"
netstat -ano | findstr ":8080 :5174"
