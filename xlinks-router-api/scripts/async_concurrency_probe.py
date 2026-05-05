#!/usr/bin/env python3
"""
并发调用 chat/completions 或 responses 接口的简单压测脚本。

用途：
1. 验证路由层并发控制是否生效
2. 对比流式/非流式请求在会话级 permit 控制下的表现
3. 观察接口吞吐、成功率、超时与平均耗时

依赖：
    pip install aiohttp

示例：
    python scripts/async_concurrency_probe.py \
      --base-url http://localhost:8081 \
      --api chat \
      --token sk-xxx \
      --model gpt-5.4 \
      --total 20 \
      --concurrency 5

    python scripts/async_concurrency_probe.py \
      --base-url http://localhost:8081 \
      --api responses \
      --token sk-xxx \
      --model gpt-5.4 \
      --total 10 \
      --concurrency 2 \
      --stream
"""

from __future__ import annotations

import argparse
import asyncio
import json
import statistics
import time
from dataclasses import dataclass
from typing import Any

import aiohttp


@dataclass
class RequestResult:
    index: int
    success: bool
    status: int | None
    elapsed_ms: float
    response_size: int
    error: str | None = None


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="异步并发调用 chat/completions 或 responses 接口")
    parser.add_argument("--base-url", required=True, help="服务地址，例如 http://localhost:8081")
    parser.add_argument(
        "--api",
        choices=["chat", "responses"],
        default="chat",
        help="调用的接口类型",
    )
    parser.add_argument("--token", required=True, help="客户令牌，等价于 Authorization: Bearer xxx")
    parser.add_argument("--model", default="gpt-5.4", help="模型名称")
    parser.add_argument("--prompt", default="请简要介绍一下并发控制。", help="用户提示词")
    parser.add_argument("--total", type=int, default=20, help="总请求数")
    parser.add_argument("--concurrency", type=int, default=5, help="并发数")
    parser.add_argument("--timeout", type=float, default=120.0, help="单请求超时时间，单位秒")
    parser.add_argument("--connect-timeout", type=float, default=10.0, help="连接超时时间，单位秒")
    parser.add_argument("--stream", action="store_true", help="是否使用流式请求")
    parser.add_argument("--max-tokens", type=int, default=1024, help="最大 token 数")
    parser.add_argument("--temperature", type=float, default=0.7, help="采样温度")
    parser.add_argument("--request-interval-ms", type=int, default=0, help="每次发起请求前的额外延迟")
    parser.add_argument("--headers-json", default="", help="额外请求头 JSON，例如 '{\"X-Test\":\"1\"}'")
    parser.add_argument("--payload-json", default="", help="额外请求体 JSON，会合并到默认请求体")
    parser.add_argument("--print-body", action="store_true", help="打印非流式响应体摘要")
    return parser.parse_args()


def build_url(base_url: str, api_kind: str) -> str:
    normalized = base_url.rstrip("/")
    if api_kind == "chat":
        return f"{normalized}/v1/chat/completions"
    return f"{normalized}/v1/responses"


def build_payload(args: argparse.Namespace) -> dict[str, Any]:
    if args.api == "chat":
        payload: dict[str, Any] = {
            "model": args.model,
            "messages": [
                {
                    "role": "user",
                    "content": args.prompt,
                }
            ],
            "max_tokens": args.max_tokens,
            "temperature": args.temperature,
            "stream": args.stream,
        }
    else:
        payload = {
            "model": args.model,
            "input": args.prompt,
            "max_output_tokens": args.max_tokens,
            "temperature": args.temperature,
            "stream": args.stream,
        }

    if args.payload_json:
        payload.update(json.loads(args.payload_json))
    return payload


def build_headers(args: argparse.Namespace) -> dict[str, str]:
    headers = {
        "Authorization": f"Bearer {args.token}",
        "Content-Type": "application/json",
        "Accept": "*/*",
    }
    if args.headers_json:
        headers.update(json.loads(args.headers_json))
    return headers


async def consume_stream(response: aiohttp.ClientResponse) -> tuple[int, str]:
    total_bytes = 0
    chunks: list[bytes] = []
    async for chunk in response.content.iter_chunked(1024):
        if not chunk:
            continue
        total_bytes += len(chunk)
        if len(chunks) < 8:
            chunks.append(chunk)
    preview = b"".join(chunks).decode("utf-8", errors="ignore")
    return total_bytes, preview


async def consume_normal(response: aiohttp.ClientResponse) -> tuple[int, str]:
    text = await response.text()
    return len(text.encode("utf-8", errors="ignore")), text


async def execute_one(
    session: aiohttp.ClientSession,
    semaphore: asyncio.Semaphore,
    args: argparse.Namespace,
    index: int,
    url: str,
    headers: dict[str, str],
    payload: dict[str, Any],
) -> RequestResult:
    async with semaphore:
        if args.request_interval_ms > 0:
            await asyncio.sleep(args.request_interval_ms / 1000.0)

        started = time.perf_counter()
        try:
            async with session.post(url, headers=headers, json=payload) as response:
                if args.stream:
                    response_size, body_preview = await consume_stream(response)
                else:
                    response_size, body_preview = await consume_normal(response)

                elapsed_ms = (time.perf_counter() - started) * 1000
                ok = 200 <= response.status < 300
                prefix = "成功" if ok else "失败"
                print(
                    f"[{index:04d}] {prefix} status={response.status} elapsed={elapsed_ms:.1f}ms size={response_size}"
                )
                if args.print_body and body_preview:
                    preview = body_preview.strip().replace("\n", "\\n")
                    print(f"[{index:04d}] body={preview[:300]}")
                return RequestResult(
                    index=index,
                    success=ok,
                    status=response.status,
                    elapsed_ms=elapsed_ms,
                    response_size=response_size,
                    error=None if ok else body_preview[:300],
                )
        except Exception as exc:  # noqa: BLE001
            elapsed_ms = (time.perf_counter() - started) * 1000
            print(f"[{index:04d}] 异常 elapsed={elapsed_ms:.1f}ms error={exc}")
            return RequestResult(
                index=index,
                success=False,
                status=None,
                elapsed_ms=elapsed_ms,
                response_size=0,
                error=str(exc),
            )


def print_summary(results: list[RequestResult], wall_elapsed_s: float) -> None:
    success_results = [item for item in results if item.success]
    failed_results = [item for item in results if not item.success]
    elapsed_values = [item.elapsed_ms for item in results]

    print("\n========== 汇总 ==========")
    print(f"总请求数: {len(results)}")
    print(f"成功数: {len(success_results)}")
    print(f"失败数: {len(failed_results)}")
    print(f"总耗时: {wall_elapsed_s:.2f}s")
    print(f"吞吐: {len(results) / wall_elapsed_s:.2f} req/s" if wall_elapsed_s > 0 else "吞吐: -")
    print(f"平均耗时: {statistics.mean(elapsed_values):.1f}ms" if elapsed_values else "平均耗时: -")
    print(f"P50耗时: {statistics.median(elapsed_values):.1f}ms" if elapsed_values else "P50耗时: -")
    if len(elapsed_values) >= 2:
        print(f"最大耗时: {max(elapsed_values):.1f}ms")
        print(f"最小耗时: {min(elapsed_values):.1f}ms")

    status_counts: dict[str, int] = {}
    for item in results:
        key = str(item.status) if item.status is not None else "EXCEPTION"
        status_counts[key] = status_counts.get(key, 0) + 1

    print("状态分布:")
    for status, count in sorted(status_counts.items(), key=lambda x: x[0]):
        print(f"  {status}: {count}")

    if failed_results:
        print("失败样例:")
        for item in failed_results[:10]:
            print(f"  [{item.index:04d}] status={item.status} error={item.error}")


async def async_main(args: argparse.Namespace) -> None:
    url = build_url(args.base_url, args.api)
    headers = build_headers(args)
    payload = build_payload(args)

    timeout = aiohttp.ClientTimeout(
        total=args.timeout,
        connect=args.connect_timeout,
        sock_connect=args.connect_timeout,
        sock_read=args.timeout,
    )
    connector = aiohttp.TCPConnector(limit=0, ssl=False)
    semaphore = asyncio.Semaphore(args.concurrency)

    print("========== 压测参数 ==========")
    print(f"URL: {url}")
    print(f"接口类型: {args.api}")
    print(f"模型: {args.model}")
    print(f"总请求数: {args.total}")
    print(f"并发数: {args.concurrency}")
    print(f"是否流式: {args.stream}")
    print(f"超时: {args.timeout}s")
    print("==============================\n")

    started = time.perf_counter()
    async with aiohttp.ClientSession(timeout=timeout, connector=connector) as session:
        tasks = [
            asyncio.create_task(execute_one(session, semaphore, args, i + 1, url, headers, payload))
            for i in range(args.total)
        ]
        results = await asyncio.gather(*tasks)
    wall_elapsed_s = time.perf_counter() - started

    print_summary(results, wall_elapsed_s)


def main() -> None:
    args = parse_args()
    asyncio.run(async_main(args))


if __name__ == "__main__":
    main()
