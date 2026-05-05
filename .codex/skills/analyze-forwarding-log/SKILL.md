---
name: analyze-forwarding-log
description: Analyze xlinks router forwarding logs from a given log file path to judge whether routing, concurrency limiting, downgrade, retry, token hotspot, exception classification, and session latency behave as expected. Use when the user provides a large log file path and wants an evidence-based analysis of forwarding strategy correctness.
---

# Analyze Forwarding Log

## Workflow

1. Take the user-provided log file path and run `D:\project\xlinks-router\xlinks-router-api\scripts\analyze_forwarding_log.py`.
2. Prefer `--protocol responses --stream true` when the user is checking streaming forwarding; otherwise analyze the full file first and then narrow if needed.
3. Use `--json` when you need structured output for a precise conclusion.
4. Judge the result against these expectations:
   - routing selects sensible providers and can switch providers on retryable failure
   - permit acquisition/release is balanced
   - downgrade only happens in the intended stage
   - retry happens only for retryable failures
   - token usage is not heavily concentrated on a single token unless concurrency is exhausted
   - exceptions are grouped correctly
   - session latency is within an acceptable range and no outliers dominate

## Output

Summarize:
- whether the strategy is normal or abnormal
- what is wrong if abnormal
- which request traces are representative
- whether token hotspots exist
- whether timeout / 4xx / 5xx / SSE failures are classified correctly
- whether the end-to-end latency profile is acceptable

## Notes

- The log file may be large, so do not load it all into memory.
- Always prefer the project script output over manual log inspection.
- If the user only gives a path, treat that path as the primary input and analyze it directly.
