#!/bin/bash
set -e

echo "[flink-start] Starting Flink JobManager..."
/docker-entrypoint.sh jobmanager &
JM_PID=$!

echo "[flink-start] Waiting for JobManager RPC on localhost:6123..."
for i in {1..60}; do
  if (echo > /dev/tcp/localhost/6123) 2>/dev/null; then
    echo "[flink-start] JobManager RPC is up."
    break
  fi
  echo "[flink-start] JM not ready yet, retry $i..."
  sleep 2
done


#1)rich-job
echo "[flink-start] Submitting Rich Flink Consumer job..."
/opt/flink/bin/flink run -d \
  -c iot.data.platform.rich.flink.consumer.consumer.KafkaFlinkConsumer \
  /opt/flink/jobs/rich-flink-consumer-1.0.jar \
  || echo "[flink-start] WARNING: job submission failed, check logs in JM/TS."

echo "[flink-start] Job submission finished. Waiting for JobManager to exit..."
wait "$JM_PID"
echo "[flink-start] JobManager process finished. Exiting container."
