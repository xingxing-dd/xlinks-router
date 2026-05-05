#!/usr/bin/env python3
"""
分析 xlinks-router-api 请求链路日志。

目标：
1. 从大日志文件中流式提取 RequestChainLogCollector 输出的完整链路块
2. 汇总路由、并发令牌、降级、重试、SSE 转发等关键行为
3. 标出可疑链路，便于后续人工复核策略是否正确

示例：
    python scripts/analyze_forwarding_log.py ^
      --log logs/xlinks-router-api.log

    python scripts/analyze_forwarding_log.py ^
      --log logs/xlinks-router-api.log ^
      --protocol responses ^
      --stream true ^
      --show-suspicious 20
"""

from __future__ import annotations

import argparse
import json
import re
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable


TIMESTAMP_RE = re.compile(r"^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3} ")
REQUEST_LOGGER_RE = re.compile(r"RequestChainLogCollector\s-\s")
HEADER_STATUS_RE = re.compile(r"(?:HTTP状态|httpStatus)=(\d+)")
HEADER_TRACE_ID_RE = re.compile(r"traceId=([A-Za-z0-9-]+)")
HEADER_REQUEST_ID_RE = re.compile(r"requestId=([A-Za-z0-9-]+)")
HEADER_RESULT_RE = re.compile(r"(?:结果|result)=([^|]+)")
HEADER_ELAPSED_RE = re.compile(r"(?:总耗时|elapsedMs)=(\d+)ms")
SUMMARY_PROTOCOL_RE = re.compile(r"(?:protocol)=([A-Za-z0-9_-]+)")
SUMMARY_STREAM_RE = re.compile(r"(?:stream)=([A-Za-z]+)")
NODE_RE = re.compile(r"^\s*-\s*\[(\d+)ms\]\[(.*?)\]\s*(.*)$")
PROVIDER_NAME_RE = re.compile(r"服务商=([^，|]+)")
PROVIDER_TOKEN_RE = re.compile(r"服务商令牌=([^，|]+)")
PERMIT_ID_RE = re.compile(r"permitId=([A-Za-z0-9]+)")
ATTEMPT_INDEX_RE = re.compile(r"第\s*(\d+)\s*次尝试")
ROUTE_CANDIDATE_COUNT_RE = re.compile(r"候选服务商数量=(\d+)")
ACTION_RE = re.compile(r"后续动作[:=]?\s*([^，|]+)")
HTTP_CODE_IN_MESSAGE_RE = re.compile(r"状态码[=:]?(\d+)")


def normalize_bool(raw: str | None) -> bool | None:
    if raw is None:
        return None
    value = raw.strip().lower()
    if value in {"true", "1", "yes"}:
        return True
    if value in {"false", "0", "no"}:
        return False
    return None


def match_any(text: str, *keywords: str) -> bool:
    return any(keyword in text for keyword in keywords if keyword)


def safe_int(raw: str | None) -> int | None:
    if raw is None or not raw.strip():
        return None
    try:
        return int(raw)
    except ValueError:
        return None


@dataclass
class NodeEvent:
    elapsed_ms: int
    stage: str
    message: str


@dataclass
class RequestTrace:
    trace_id: str = "-"
    request_id: str = "-"
    protocol: str = "-"
    stream: bool | None = None
    result: str = "-"
    http_status: int | None = None
    elapsed_ms: int | None = None
    nodes: list[NodeEvent] = field(default_factory=list)

    attempts: list[int] = field(default_factory=list)
    attempt_providers: list[str] = field(default_factory=list)
    attempt_provider_tokens: list[str] = field(default_factory=list)
    permit_acquired: int = 0
    permit_released: int = 0
    permit_failed: int = 0
    retryable_failures: int = 0
    provider_switches: int = 0
    route_candidate_count: int | None = None
    timeout_events: int = 0
    direct_success: bool = False
    stream_success: bool = False
    stream_failure: bool = False
    release_missing: bool = False
    suspicious_reasons: list[str] = field(default_factory=list)
    final_provider: str = "-"
    final_provider_token: str = "-"
    retry_actions: list[str] = field(default_factory=list)
    status_codes_seen: list[int] = field(default_factory=list)
    exception_categories: list[str] = field(default_factory=list)
    stream_error_reasons: list[str] = field(default_factory=list)

    def analyze(self) -> None:
        last_attempt_provider = None
        for node in self.nodes:
            stage = node.stage
            message = node.message
            compact_stage = stage.replace(" ", "")

            candidate_match = ROUTE_CANDIDATE_COUNT_RE.search(message)
            if candidate_match:
                self.route_candidate_count = safe_int(candidate_match.group(1))

            attempt_match = ATTEMPT_INDEX_RE.search(message)
            if attempt_match and match_any(stage, "转发尝试", "Forward Attempt"):
                attempt_no = safe_int(attempt_match.group(1))
                if attempt_no is not None:
                    self.attempts.append(attempt_no)
                provider_name = extract_first(PROVIDER_NAME_RE, message)
                provider_token = extract_first(PROVIDER_TOKEN_RE, message)
                self.attempt_providers.append(provider_name or "-")
                self.attempt_provider_tokens.append(provider_token or "-")
                self.final_provider = provider_name or self.final_provider
                self.final_provider_token = provider_token or self.final_provider_token
                if last_attempt_provider is not None and provider_name and provider_name != last_attempt_provider:
                    self.provider_switches += 1
                if provider_name:
                    last_attempt_provider = provider_name

            if (
                match_any(compact_stage, "并发令牌")
                and not match_any(compact_stage, "续约", "释放", "诊断")
                and match_any(message, "获取到并发令牌", "获取成功", "acquired")
            ):
                self.permit_acquired += 1

            if match_any(compact_stage, "并发令牌获取失败") or (
                    match_any(compact_stage, "并发令牌")
                    and not match_any(compact_stage, "续约", "释放", "诊断")
                    and match_any(message, "未拿到并发令牌", "获取失败")):
                self.permit_failed += 1

            if match_any(compact_stage, "并发令牌释放") and match_any(message, "释放成功", "released"):
                self.permit_released += 1

            if match_any(message, "可重试失败", "retryable"):
                self.retryable_failures += 1
                action = extract_first(ACTION_RE, message)
                if action:
                    self.retry_actions.append(action)

            if (
                match_any(compact_stage, "上游超时", "异步超时")
                or (
                    match_any(message, "超时", "timeout")
                    and not match_any(message, "Permit等待=", "非流式超时=", "流式首包超时=", "流式空闲超时=")
                )
            ):
                self.timeout_events += 1

            if match_any(stage, "流式转发完成", "STREAMING_RESPONSE_SUCCESS"):
                self.stream_success = True
            if match_any(stage, "流式转发失败", "SSE透传失败", "STREAMING_RESPONSE_ERROR"):
                self.stream_failure = True
                self.stream_error_reasons.append(message)
            if match_any(stage, "响应完成", "DIRECT_RESPONSE_SUCCESS") and match_any(message, "成功", "succeeded"):
                self.direct_success = True

            category = classify_exception(stage, message)
            if category:
                self.exception_categories.append(category)

            code_match = HTTP_CODE_IN_MESSAGE_RE.search(message)
            if code_match:
                code = safe_int(code_match.group(1))
                if code is not None:
                    self.status_codes_seen.append(code)

        if self.permit_acquired != self.permit_released:
            self.release_missing = True
            self.suspicious_reasons.append(
                f"并发令牌获取/释放不平衡: acquired={self.permit_acquired}, released={self.permit_released}"
            )

        unique_attempts = sorted(set(self.attempts))
        if self.retryable_failures > 0 and len(unique_attempts) <= 1:
            self.suspicious_reasons.append("出现可重试失败，但没有看到后续新的转发尝试")

        if self.permit_failed > 0 and self.permit_acquired == 0 and self.http_status and self.http_status < 500:
            self.suspicious_reasons.append("出现 permit 获取失败，但没有后续成功获取，且请求非 5xx 结束")

        if self.stream_success and self.stream_failure:
            self.suspicious_reasons.append("同一请求同时出现流式成功和流式失败日志")

        if self.stream is True and not self.stream_success and self.http_status == 200:
            self.suspicious_reasons.append("流式请求返回 200，但未看到流式完成日志")

        if self.route_candidate_count == 0:
            self.suspicious_reasons.append("候选服务商数量为 0")


def extract_first(pattern: re.Pattern[str], text: str) -> str | None:
    match = pattern.search(text)
    if not match:
        return None
    return match.group(1).strip()


def decode_lines(path: Path, encoding: str) -> Iterable[str]:
    with path.open("r", encoding=encoding, errors="replace") as handle:
        for line in handle:
            yield line.rstrip("\n")


def collect_request_blocks(path: Path, encoding: str) -> Iterable[list[str]]:
    current: list[str] | None = None
    for line in decode_lines(path, encoding):
        if TIMESTAMP_RE.match(line):
            if current is not None:
                yield current
            if REQUEST_LOGGER_RE.search(line):
                current = [line]
            else:
                current = None
            continue
        if current is not None:
            current.append(line)
    if current is not None:
        yield current


def parse_trace(block: list[str]) -> RequestTrace | None:
    if not block:
        return None
    header = block[0]
    trace = RequestTrace(
        trace_id=extract_first(HEADER_TRACE_ID_RE, header) or "-",
        request_id=extract_first(HEADER_REQUEST_ID_RE, header) or "-",
        result=(extract_first(HEADER_RESULT_RE, header) or "-").strip(),
        http_status=safe_int(extract_first(HEADER_STATUS_RE, header)),
        elapsed_ms=safe_int(extract_first(HEADER_ELAPSED_RE, header)),
    )

    for line in block[1:]:
        if "protocol=" in line:
            protocol = extract_first(SUMMARY_PROTOCOL_RE, line)
            if protocol:
                trace.protocol = protocol
            stream_raw = extract_first(SUMMARY_STREAM_RE, line)
            trace.stream = normalize_bool(stream_raw)
            continue

        node_match = NODE_RE.match(line)
        if node_match:
            trace.nodes.append(
                NodeEvent(
                    elapsed_ms=int(node_match.group(1)),
                    stage=node_match.group(2).strip(),
                    message=node_match.group(3).strip(),
                )
            )

    trace.analyze()
    return trace


def build_summary(traces: list[RequestTrace]) -> dict:
    summary: dict[str, object] = {}
    summary["total_requests"] = len(traces)
    summary["by_protocol"] = dict(Counter(trace.protocol for trace in traces))
    summary["by_stream"] = dict(Counter(
        "true" if trace.stream is True else "false" if trace.stream is False else "unknown"
        for trace in traces
    ))
    summary["by_http_status"] = dict(Counter(str(trace.http_status or "-") for trace in traces))
    summary["by_result"] = dict(Counter(trace.result for trace in traces))
    summary["requests_with_retry"] = sum(1 for trace in traces if len(set(trace.attempts)) > 1)
    summary["requests_with_provider_switch"] = sum(1 for trace in traces if trace.provider_switches > 0)
    summary["requests_with_permit_failure"] = sum(1 for trace in traces if trace.permit_failed > 0)
    summary["requests_with_retryable_failure"] = sum(1 for trace in traces if trace.retryable_failures > 0)
    summary["requests_with_timeout"] = sum(1 for trace in traces if trace.timeout_events > 0)
    summary["requests_with_suspicious_flags"] = sum(1 for trace in traces if trace.suspicious_reasons)
    summary["requests_with_release_mismatch"] = sum(1 for trace in traces if trace.release_missing)
    summary["stream_success"] = sum(1 for trace in traces if trace.stream_success)
    summary["stream_failure"] = sum(1 for trace in traces if trace.stream_failure)
    summary["direct_success"] = sum(1 for trace in traces if trace.direct_success)
    summary["latency"] = build_latency_summary(traces)

    attempt_distribution = Counter(len(set(trace.attempts)) for trace in traces if trace.attempts)
    summary["attempt_distribution"] = dict(sorted((str(k), v) for k, v in attempt_distribution.items()))

    provider_counter = Counter()
    provider_token_counter = Counter()
    retry_action_counter = Counter()
    suspicious_counter = Counter()
    upstream_status_counter = Counter()
    candidate_counter = Counter()
    provider_success_counter = Counter()
    provider_failure_counter = Counter()
    provider_token_success_counter = Counter()
    provider_token_failure_counter = Counter()
    exception_category_counter = Counter()

    for trace in traces:
        for provider in trace.attempt_providers:
            if provider and provider != "-":
                provider_counter[provider] += 1
        for provider_token in trace.attempt_provider_tokens:
            if provider_token and provider_token != "-":
                provider_token_counter[provider_token] += 1
        for action in trace.retry_actions:
            retry_action_counter[action] += 1
        for reason in trace.suspicious_reasons:
            suspicious_counter[reason] += 1
        for code in trace.status_codes_seen:
            upstream_status_counter[str(code)] += 1
        if trace.route_candidate_count is not None:
            candidate_counter[str(trace.route_candidate_count)] += 1
        if trace.final_provider != "-" and trace.http_status is not None and trace.http_status < 400:
            provider_success_counter[trace.final_provider] += 1
        elif trace.final_provider != "-":
            provider_failure_counter[trace.final_provider] += 1
        if trace.final_provider_token != "-" and trace.http_status is not None and trace.http_status < 400:
            provider_token_success_counter[trace.final_provider_token] += 1
        elif trace.final_provider_token != "-":
            provider_token_failure_counter[trace.final_provider_token] += 1
        for category in trace.exception_categories:
            exception_category_counter[category] += 1

    summary["top_attempt_providers"] = provider_counter.most_common(10)
    summary["top_attempt_provider_tokens"] = provider_token_counter.most_common(10)
    summary["top_success_providers"] = provider_success_counter.most_common(10)
    summary["top_failure_providers"] = provider_failure_counter.most_common(10)
    summary["top_success_provider_tokens"] = provider_token_success_counter.most_common(20)
    summary["top_failure_provider_tokens"] = provider_token_failure_counter.most_common(20)
    summary["provider_token_hotspots"] = build_hotspot_summary(provider_token_counter)
    summary["retry_actions"] = dict(retry_action_counter)
    summary["suspicious_reasons"] = dict(suspicious_counter)
    summary["upstream_status_codes"] = dict(upstream_status_counter)
    summary["route_candidate_distribution"] = dict(candidate_counter)
    summary["exception_categories"] = dict(exception_category_counter)
    summary["permit_leak_candidates"] = build_permit_leak_candidates(traces)
    summary["stream_error_candidates"] = build_stream_error_candidates(traces)

    return summary


def build_latency_summary(traces: list[RequestTrace]) -> dict[str, float | int]:
    values = sorted(trace.elapsed_ms for trace in traces if trace.elapsed_ms is not None)
    if not values:
        return {}
    return {
        "count": len(values),
        "min_ms": values[0],
        "p50_ms": percentile(values, 50),
        "p90_ms": percentile(values, 90),
        "p95_ms": percentile(values, 95),
        "p99_ms": percentile(values, 99),
        "max_ms": values[-1],
        "avg_ms": round(sum(values) / len(values), 2),
    }


def percentile(sorted_values: list[int], p: int) -> int:
    if not sorted_values:
        return 0
    if len(sorted_values) == 1:
        return sorted_values[0]
    rank = max(0, min(len(sorted_values) - 1, round((p / 100) * (len(sorted_values) - 1))))
    return sorted_values[rank]


def build_hotspot_summary(counter: Counter[str]) -> dict[str, object]:
    total = sum(counter.values())
    if total <= 0:
        return {"total_attempts": 0, "tokens": [], "hotspot_ratio": 0.0}
    tokens = []
    for token, count in counter.most_common(20):
        ratio = round((count / total) * 100, 2)
        tokens.append({"token": token, "count": count, "ratio_percent": ratio})
    hottest = tokens[0]["ratio_percent"] if tokens else 0.0
    return {
        "total_attempts": total,
        "tokens": tokens,
        "hotspot_ratio": hottest,
    }


def build_permit_leak_candidates(traces: list[RequestTrace]) -> list[dict[str, object]]:
    candidates = []
    for trace in traces:
        if not trace.release_missing:
            continue
        candidates.append(
            {
                "trace_id": trace.trace_id,
                "request_id": trace.request_id,
                "http_status": trace.http_status,
                "result": trace.result,
                "attempts": sorted(set(trace.attempts)),
                "providers": trace.attempt_providers,
                "provider_tokens": trace.attempt_provider_tokens,
                "permit_acquired": trace.permit_acquired,
                "permit_released": trace.permit_released,
                "exception_categories": trace.exception_categories,
            }
        )
    return candidates


def build_stream_error_candidates(traces: list[RequestTrace]) -> list[dict[str, object]]:
    candidates = []
    for trace in traces:
        if not trace.stream_failure and "请求异步处理异常" not in trace.result and "流式响应写出失败" not in trace.result:
            continue
        candidates.append(
            {
                "trace_id": trace.trace_id,
                "request_id": trace.request_id,
                "http_status": trace.http_status,
                "result": trace.result,
                "attempts": sorted(set(trace.attempts)),
                "providers": trace.attempt_providers,
                "provider_tokens": trace.attempt_provider_tokens,
                "stream_error_reasons": trace.stream_error_reasons,
                "exception_categories": trace.exception_categories,
            }
        )
    return candidates


def classify_exception(stage: str, message: str) -> str | None:
    text = f"{stage} {message}"
    compact_stage = stage.replace(" ", "")
    if match_any(text, "并发令牌", "permit") and match_any(text, "获取失败", "未拿到", "等待超时"):
        return "permit_acquire_failed"
    if match_any(text, "续约") and match_any(text, "失败", "failed"):
        return "permit_renew_failed"
    if match_any(text, "释放") and match_any(text, "失败", "failed"):
        return "permit_release_failed"
    if match_any(compact_stage, "上游超时", "异步超时") or (
            match_any(text, "超时", "timeout")
            and not match_any(text, "Permit等待=", "非流式超时=", "流式首包超时=", "流式空闲超时=")
    ):
        if match_any(text, "首包"):
            return "upstream_first_packet_timeout"
        if match_any(text, "空闲"):
            return "upstream_idle_timeout"
        if match_any(text, "上游"):
            return "upstream_timeout"
        return "timeout"
    if match_any(compact_stage, "上游响应", "上游失败", "响应完成", "协议异常", "全局异常") and match_any(text, "503", "502", "504"):
        return "upstream_5xx"
    if match_any(compact_stage, "上游响应", "上游失败", "响应完成", "协议异常", "全局异常") and match_any(text, "429"):
        return "upstream_rate_limited"
    if match_any(compact_stage, "上游响应", "上游失败", "响应完成", "协议异常", "全局异常") and match_any(text, "400"):
        return "upstream_4xx"
    if match_any(text, "协议异常", "全局异常", "未处理异常", "请求参数错误"):
        return "local_exception"
    if match_any(text, "SSE") and match_any(text, "失败", "failed"):
        return "sse_transfer_failed"
    return None


def print_human_summary(summary: dict, suspicious_traces: list[RequestTrace], show_suspicious: int) -> None:
    print("=== 日志分析摘要 ===")
    print(f"总请求数: {summary['total_requests']}")
    print(f"协议分布: {json.dumps(summary['by_protocol'], ensure_ascii=False)}")
    print(f"流式分布: {json.dumps(summary['by_stream'], ensure_ascii=False)}")
    print(f"HTTP 状态分布: {json.dumps(summary['by_http_status'], ensure_ascii=False)}")
    print(f"结果分布: {json.dumps(summary['by_result'], ensure_ascii=False)}")
    print(f"发生重试的请求数: {summary['requests_with_retry']}")
    print(f"发生服务商切换的请求数: {summary['requests_with_provider_switch']}")
    print(f"发生 permit 获取失败的请求数: {summary['requests_with_permit_failure']}")
    print(f"发生可重试失败的请求数: {summary['requests_with_retryable_failure']}")
    print(f"发生 timeout 的请求数: {summary['requests_with_timeout']}")
    print(f"permit 获取/释放不平衡请求数: {summary['requests_with_release_mismatch']}")
    print(f"带可疑标记的请求数: {summary['requests_with_suspicious_flags']}")
    print(f"会话耗时统计: {json.dumps(summary['latency'], ensure_ascii=False)}")
    print(f"尝试次数分布: {json.dumps(summary['attempt_distribution'], ensure_ascii=False)}")
    print(f"候选服务商数量分布: {json.dumps(summary['route_candidate_distribution'], ensure_ascii=False)}")
    print(f"重试动作分布: {json.dumps(summary['retry_actions'], ensure_ascii=False)}")
    print(f"上游状态码分布: {json.dumps(summary['upstream_status_codes'], ensure_ascii=False)}")
    print(f"异常分类分布: {json.dumps(summary['exception_categories'], ensure_ascii=False)}")
    print(f"尝试次数最多的服务商: {json.dumps(summary['top_attempt_providers'], ensure_ascii=False)}")
    print(f"尝试次数最多的服务商令牌: {json.dumps(summary['top_attempt_provider_tokens'], ensure_ascii=False)}")
    print(f"成功最多的服务商: {json.dumps(summary['top_success_providers'], ensure_ascii=False)}")
    print(f"失败最多的服务商: {json.dumps(summary['top_failure_providers'], ensure_ascii=False)}")
    print(f"成功最多的服务商令牌: {json.dumps(summary['top_success_provider_tokens'], ensure_ascii=False)}")
    print(f"失败最多的服务商令牌: {json.dumps(summary['top_failure_provider_tokens'], ensure_ascii=False)}")
    print(f"令牌热点分布: {json.dumps(summary['provider_token_hotspots'], ensure_ascii=False)}")
    print(f"permit 疑似泄漏请求数: {len(summary['permit_leak_candidates'])}")
    print(f"流式异常请求数: {len(summary['stream_error_candidates'])}")

    if not suspicious_traces or show_suspicious <= 0:
        return

    print()
    print("=== 可疑请求样本 ===")
    for trace in suspicious_traces[:show_suspicious]:
        print(
            f"- traceId={trace.trace_id} requestId={trace.request_id} protocol={trace.protocol} "
            f"stream={trace.stream} httpStatus={trace.http_status} attempts={sorted(set(trace.attempts)) or []}"
        )
        print(f"  result={trace.result}")
        print(f"  providers={trace.attempt_providers}")
        print(f"  providerTokens={trace.attempt_provider_tokens}")
        print(f"  exceptionCategories={trace.exception_categories}")
        if trace.stream_error_reasons:
            print(f"  streamErrorReasons={trace.stream_error_reasons}")
        print(f"  suspicious={trace.suspicious_reasons}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="分析 xlinks-router-api 请求链路日志")
    parser.add_argument("--log", required=True, help="日志文件路径")
    parser.add_argument("--encoding", default="utf-8", help="日志编码，默认 utf-8")
    parser.add_argument("--protocol", default="", help="仅分析指定协议，如 responses/chat_completions")
    parser.add_argument("--stream", default="", help="仅分析指定 stream，true/false")
    parser.add_argument("--show-suspicious", type=int, default=10, help="显示多少条可疑请求样本")
    parser.add_argument("--json", action="store_true", help="输出 JSON")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    log_path = Path(args.log)
    if not log_path.exists():
        raise SystemExit(f"日志文件不存在: {log_path}")

    protocol_filter = args.protocol.strip()
    stream_filter = normalize_bool(args.stream) if args.stream.strip() else None

    traces: list[RequestTrace] = []
    for block in collect_request_blocks(log_path, args.encoding):
        trace = parse_trace(block)
        if trace is None:
            continue
        if protocol_filter and trace.protocol != protocol_filter:
            continue
        if stream_filter is not None and trace.stream != stream_filter:
            continue
        traces.append(trace)

    summary = build_summary(traces)
    suspicious_traces = [trace for trace in traces if trace.suspicious_reasons]

    if args.json:
        payload = {
            "summary": summary,
            "suspicious": [
                {
                    "trace_id": trace.trace_id,
                    "request_id": trace.request_id,
                    "protocol": trace.protocol,
                    "stream": trace.stream,
                    "http_status": trace.http_status,
                    "result": trace.result,
                    "attempts": sorted(set(trace.attempts)),
                    "attempt_providers": trace.attempt_providers,
                    "attempt_provider_tokens": trace.attempt_provider_tokens,
                    "suspicious_reasons": trace.suspicious_reasons,
                }
                for trace in suspicious_traces[:args.show_suspicious]
            ],
        }
        print(json.dumps(payload, ensure_ascii=False, indent=2))
        return 0

    print_human_summary(summary, suspicious_traces, args.show_suspicious)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
