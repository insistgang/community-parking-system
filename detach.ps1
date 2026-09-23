$java = 'cmd /c cd /d E:\parking-management-java && java -jar target\parking-java-1.0.0.jar > data\run-backend.log 2>&1'
$web = 'cmd /c cd /d E:\parking-management-java\frontend && npm run dev -- --host 0.0.0.0 --port 5174 > ..\data\run-frontend.log 2>&1'
$wmi = [wmiclass]'Win32_Process'
$a = $wmi.Create($java)
$b = $wmi.Create($web)
"JAVA_PID $($a.ProcessId) RC $($a.ReturnValue)"
"WEB_PID $($b.ProcessId) RC $($b.ReturnValue)"
Start-Sleep -Seconds 14
"---- java log ----"
Get-Content E:\parking-management-java\data\run-backend.log -Tail 8 -ErrorAction SilentlyContinue
"---- web log ----"
Get-Content E:\parking-management-java\data\run-frontend.log -Tail 12 -ErrorAction SilentlyContinue
