#!/bin/bash

BASE_URL="http://localhost:8080/api/magazines/search"
KEYWORD="test"
ITERATIONS=5

echo "========================================"
echo "Performance Test (Search API)"
echo "Target: < 0.1s (100ms)"
echo "========================================"

total_time=0

for i in $(seq 1 $ITERATIONS); do
    # curl output format: time_total in seconds
    duration=$(curl -s -w "%{time_total}" -o /dev/null "$BASE_URL?keyword=$KEYWORD&page=0&size=10")
    
    # Check if curl failed (duration might be empty or exit code)
    if [ $? -ne 0 ]; then
        echo "Request $i Failed"
        continue
    fi

    # Convert seconds to ms
    duration_ms=$(echo "$duration * 1000" | bc)
    
    echo "Request $i: ${duration_ms} ms"
    total_time=$(echo "$total_time + $duration_ms" | bc)
done

avg_time=$(echo "$total_time / $ITERATIONS" | bc)

echo ""
echo "Average Response Time: ${avg_time} ms"

if (( $(echo "$avg_time < 100" | bc -l) )); then
    echo "✅ PERFORMANCE GOAL MET (< 100ms)"
else
    echo "⚠️ PERFORMANCE GOAL NOT MET (> 100ms)"
fi
