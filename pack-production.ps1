$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$jar = Join-Path $root 'backend/target/parking-java-1.0.0.jar'
$dist = Join-Path $root 'frontend/dist/index.html'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Backend jar missing; run mvn test package first.' }
if (-not (Test-Path -LiteralPath $dist)) { throw 'Frontend dist missing; run npm run build first.' }

$artifactDir = Join-Path $root 'artifacts'
New-Item -ItemType Directory -Force -Path $artifactDir | Out-Null
$archive = Join-Path $artifactDir 'parking-produce-release.tar.gz'
$files = @(
  'docker-compose.production.yml',
  'docker-compose.production.internal.yml',
  'docker-compose.production.https.yml',
  '.env.production.example',
  'PRODUCTION_DEPLOYMENT.md',
  'backup-production.sh',
  'backend/Dockerfile',
  'backend/.dockerignore',
  'backend/target/parking-java-1.0.0.jar',
  'frontend/Dockerfile.production',
  'frontend/Dockerfile.production.https',
  'frontend/.dockerignore',
  'frontend/nginx.production.conf',
  'frontend/nginx.production.https.conf',
  'frontend/dist'
)
& tar -czf $archive -C $root @files
if ($LASTEXITCODE -ne 0) { throw 'Archive creation failed.' }
Get-FileHash -Algorithm SHA256 -LiteralPath $archive | Select-Object Path,Hash
