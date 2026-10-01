#!/usr/bin/env bash
set -euo pipefail

bootstrap_server="kafka:19092"

topics=(
  request-submitted
  intake-result
  review-escalated
  decision-made
  appeal-submitted
  authorization-expired
)

for topic in "${topics[@]}"; do
  echo "Creating topic: $topic"

  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "$bootstrap_server" \
    --create \
    --if-not-exists \
    --topic "$topic" \
    --partitions 3 \
    --replication-factor 1 \
    --config retention.ms=604800000
done