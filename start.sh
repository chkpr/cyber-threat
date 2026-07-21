#!/bin/bash
set -e

set -a
source .env 2>/dev/null || true
set +a



# --- Fonctions d'attente (health checks) ---

wait_for_postgres() {
  echo -n "  attente de PostgreSQL"
  for i in {1..30}; do
    if pg_isready -h localhost -p 5432 -q; then
      echo " ✓"
      return 0
    fi
    echo -n "."
    sleep 1
  done
  echo " ✗ (timeout)"
  return 1
}

wait_for_ollama() {
  echo -n "  attente d'Ollama"
  for i in {1..30}; do
    if curl -sf http://localhost:11434/api/tags > /dev/null 2>&1; then
      echo " ✓"
      return 0
    fi
    echo -n "."
    sleep 1
  done
  echo " ✗ (timeout)"
  return 1
}

# --- Nettoyage à l'arrêt ---

cleanup() {
  echo -e "\n▼ Arrêt en cours…"
  kill $BACK_PID $FRONT_PID 2>/dev/null
  kill $OLLAMA_PID 2>/dev/null
  sudo systemctl stop postgresql
  echo "✓ Tout est arrêté."
  exit 0
}
trap cleanup INT

# --- Démarrage ---

echo "▶ PostgreSQL…"
sudo systemctl start postgresql
wait_for_postgres || { echo "PostgreSQL n'a pas démarré, arrêt."; exit 1; }

echo "▶ Ollama…"
ollama serve > /tmp/ollama.log 2>&1 &
OLLAMA_PID=$!
wait_for_ollama || { echo "Ollama n'a pas démarré, arrêt."; cleanup; }

echo "▶ Backend…"
echo "DB_PASSWORD présent au moment de lancer le back : ${DB_PASSWORD:+oui}"
( cd cyth-backend && ./gradlew bootRun ) &
BACK_PID=$!

echo "▶ Frontend…"
( cd cyth-frontend && ng serve --open ) &
FRONT_PID=$!

echo "✓ Tout démarre. Ctrl+C pour tout arrêter proprement."
wait
