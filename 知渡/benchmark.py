#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
知渡（ZhiDu）接口实测（零三方依赖，标准库）
公平对比 /posts/{id} 详情接口：缓存命中 vs 缓存未命中
关键修正：命中/未命中在【相同并发度】下测量，避免并发竞争污染结论。
"""
import subprocess
import time
import threading
import urllib.request
import statistics

BASE = "http://127.0.0.1:8088/api"
POST_ID = 1
URL = f"{BASE}/posts/{POST_ID}"
REDIS_DEL = ["redis-cli", "-h", "127.0.0.1", "-p", "6379", "DEL", f"post:detail:{POST_ID}"]


def pctl(lat, p):
    if not lat:
        return 0.0
    s = sorted(lat)
    if len(s) == 1:
        return s[0]
    idx = (p / 100.0) * (len(s) - 1)
    lo = int(idx)
    hi = min(lo + 1, len(s) - 1)
    return s[lo] + (s[hi] - s[lo]) * (idx - lo)


def once():
    t0 = time.perf_counter()
    try:
        req = urllib.request.Request(URL, headers={"Accept": "application/json"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            resp.read()
            return (time.perf_counter() - t0) * 1000.0 if resp.status == 200 else None
    except Exception:
        return None


def clear_cache():
    try:
        subprocess.run(REDIS_DEL, capture_output=True, timeout=5)
    except Exception:
        pass


def run_serial(n, miss_each=False, warm=10):
    for _ in range(warm):
        once()
    lat = []
    for _ in range(n):
        if miss_each:
            clear_cache()
        r = once()
        if r is not None:
            lat.append(r)
    return lat


def run_concurrent(n, concurrency):
    for _ in range(10):
        once()  # warm
    lat = []
    barrier = threading.Barrier(concurrency)
    lock = threading.Lock()

    def worker():
        barrier.wait()
        r = once()
        if r is not None:
            with lock:
                lat.append(r)

    wall0 = time.perf_counter()
    for i in range(0, n, concurrency):
        batch = min(concurrency, n - i)
        ts = [threading.Thread(target=worker) for _ in range(batch)]
        for t in ts:
            t.start()
        for t in ts:
            t.join()
    wall = time.perf_counter() - wall0
    return lat, wall


def show(name, lat, wall=None, total=None):
    if not lat:
        print(f"[{name}] 无样本"); return
    print(f"\n=== {name} ===")
    print(f"  样本    : {len(lat)}")
    print(f"  平均    : {statistics.fmean(lat):7.2f} ms")
    print(f"  p50     : {pctl(lat,50):7.2f} ms")
    print(f"  p95     : {pctl(lat,95):7.2f} ms")
    print(f"  p99     : {pctl(lat,99):7.2f} ms")
    print(f"  min/max : {min(lat):.2f}/{max(lat):.2f} ms")
    if wall and total:
        print(f"  吞吐    : {total/wall:8.1f} QPS  (并发 40, 总 {total} 请求, 耗时 {wall*1000:.0f}ms)")


if __name__ == "__main__":
    print(f"接口: GET {URL}\n")

    print(">> [A] 缓存命中 串行(并发=1) ×300")
    hit_s = run_serial(300, miss_each=False)
    show("命中-串行", hit_s)

    print("\n>> [B] 缓存未命中 串行(并发=1) ×100 (每次先删 key)")
    miss_s = run_serial(100, miss_each=True)
    show("未命中-串行", miss_s)

    print("\n>> [C] 缓存命中 并发=40 ×2000 (吞吐)")
    hit_c, wall = run_concurrent(2000, 40)
    show("命中-并发40", hit_c, wall=wall, total=2000)

    if hit_s and miss_s:
        ah, am = statistics.fmean(hit_s), statistics.fmean(miss_s)
        ph, pm = pctl(hit_s, 99), pctl(miss_s, 99)
        print("\n=== 公平对比(同并发=1) ===")
        print(f"  平均: 未命中 {am:.2f}ms -> 命中 {ah:.2f}ms  ({(1-ah/am)*100:+.0f}%)")
        print(f"  p99 : 未命中 {pm:.2f}ms -> 命中 {ph:.2f}ms  ({(1-ph/pm)*100:+.0f}%)")
        print("  说明: 本地 H2 为内存库，DB 回源本身极快，故缓存延迟收益有限；")
        print("        切换 MySQL(网络/LAN)后，未命中需承担 DB 往返(通常 5~20ms+)，收益随之放大。")
