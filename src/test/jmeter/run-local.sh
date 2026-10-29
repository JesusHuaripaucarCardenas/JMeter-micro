#!/usr/bin/env bash
# Prueba de carga en local. Lo único que hace falta en la máquina es Docker:
# el servicio y JMeter corren cada uno en su contenedor, igual que en el job
# `jmeter` del pipeline.
#
#   ./src/test/jmeter/run-local.sh                 # levanta la imagen de Docker Hub y le mete carga
#   THREADS=50 LOOPS=20 ./src/test/jmeter/run-local.sh
#   IMAGE=ms-medical-history:dev ./src/test/jmeter/run-local.sh   # una imagen construida en local
#
# Contra un servicio que ya está corriendo (no levanta nada):
#
#   HOST=lab.vallegrande.edu.pe PORT=443 PROTOCOL=https BASE=/sigrc/gateway ./src/test/jmeter/run-local.sh
#
# El reporte HTML queda en target/jmeter/html/index.html.
set -euo pipefail

SERVICE=ms-medical-history
PLAN=medical-history-load.jmx
IMAGE=${IMAGE:-jesushuaripaucarcardenas/ms-medical-history:latest}
APP_PORT=9091

THREADS=${THREADS:-10}
RAMPUP=${RAMPUP:-5}
LOOPS=${LOOPS:-10}
PROTOCOL=${PROTOCOL:-http}
BASE=${BASE:-}

ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
OUT="$ROOT/target/jmeter"
NETWORK=jmeter-$SERVICE

rm -rf "$OUT" && mkdir -p "$OUT"

if [ -z "${HOST:-}" ]; then
  HOST=app
  PORT=$APP_PORT
  docker network create "$NETWORK" >/dev/null 2>&1 || true
  trap 'docker rm -f "$SERVICE-carga" >/dev/null 2>&1 || true; docker network rm "$NETWORK" >/dev/null 2>&1 || true' EXIT

  echo "==> Levantando $IMAGE..."
  docker rm -f "$SERVICE-carga" >/dev/null 2>&1 || true
  docker run -d --platform linux/amd64 --name "$SERVICE-carga" --network "$NETWORK" --network-alias app "$IMAGE" >/dev/null

  echo "==> Esperando a que el servicio responda..."
  for intento in $(seq 1 45); do
    if docker run --rm --network "$NETWORK" curlimages/curl:8.10.1 -fs -o /dev/null "http://app:$APP_PORT/actuator/health"; then
      echo "    listo en el intento $intento"; break
    fi
    if [ "$intento" = 45 ]; then
      echo "El servicio no levantó; últimas líneas del log:"; docker logs --tail 40 "$SERVICE-carga"; exit 1
    fi
    sleep 4
  done
  NETWORK_ARG=(--network "$NETWORK")
else
  PORT=${PORT:-$APP_PORT}
  NETWORK_ARG=()
fi

echo "==> JMeter: $THREADS usuarios x $LOOPS repeticiones contra $PROTOCOL://$HOST:$PORT$BASE"
docker run --rm --platform linux/amd64 ${NETWORK_ARG[@]+"${NETWORK_ARG[@]}"} \
  -v "$ROOT/src/test/jmeter:/plan:ro" -v "$OUT:/out" \
  --entrypoint /opt/apache-jmeter-5.5/bin/jmeter justb4/jmeter:5.5 \
  -n -t "/plan/$PLAN" \
  -Jprotocol="$PROTOCOL" -Jhost="$HOST" -Jport="$PORT" -Jbase="$BASE" \
  -Jthreads="$THREADS" -Jrampup="$RAMPUP" -Jloops="$LOOPS" \
  -l /out/resultados.jtl -j /out/jmeter.log -e -o /out/html

TOTAL=$(($(wc -l < "$OUT/resultados.jtl") - 1))
ERRORES=$(grep -c ',false,' "$OUT/resultados.jtl" || true)
echo "==> $TOTAL peticiones, $ERRORES con error"
echo "==> Reporte: $OUT/html/index.html"
