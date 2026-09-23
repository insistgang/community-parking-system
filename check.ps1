$root = Invoke-RestMethod http://127.0.0.1:8080/
$root | ConvertTo-Json -Compress
$body = '{"identifier":"13800000001","password":"123456","login_type":"PASSWORD"}'
$login = Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/api/auth/login -ContentType "application/json" -Body $body
"LOGIN $($login.code) $($login.data.user.real_name) $($login.data.user.community_name)"
$web = Invoke-WebRequest -UseBasicParsing http://127.0.0.1:5174/
"WEB $($web.StatusCode)"
