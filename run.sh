#!/usr/bin/env bash
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$ROOT/data"
cd "$ROOT"
if [ ! -x /tmp/maven-dl/apache-maven-3.9.9/bin/mvn ]; then
  echo "需要本机 Maven。可先执行: brew install maven"
  exit 1
fi
/tmp/maven-dl/apache-maven-3.9.9/bin/mvn -q -DskipTests package
java -jar target/parking-java-1.0.0.jar &
echo $! > "$ROOT/data/backend.pid"
cd "$ROOT/frontend"
npm run dev -- --host 0.0.0.0 --port 5174
