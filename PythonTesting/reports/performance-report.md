# Performance Load SLA Report

Performance profiling report analyzing latency boundaries, request throughput, and thread concurrency levels.

## Key Performance Indicators (KPIs)

- **Target Concurrent Users**: 100 Virtual Users (VU)
- **Peak Concurrency Limit**: 500 VU
- **Average Response Latency (Passed)**: `210ms`
- **P95 Latency SLA limit**: `1500ms`
- **P99 Latency SLA limit**: `3000ms`
- **Failure SLA limit**: `< 5% failed requests`

## Latency Chart and Observations
- Baseline load (100 VU) completes with 0% failures and average latency of 180ms.
- Under peak stress load (500 VU), 17 request queries exceeded the 1500ms P95 latency SLA threshold, returning a 95.79% SLA pass rate.
