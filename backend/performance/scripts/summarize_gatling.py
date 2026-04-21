#!/usr/bin/env python3
import json
import sys
from pathlib import Path


def latest_stats_json(gatling_dir: Path) -> Path | None:
    candidates = sorted(gatling_dir.glob("**/js/stats.json"), key=lambda p: p.stat().st_mtime, reverse=True)
    return candidates[0] if candidates else None


def extract(stats: dict) -> dict:
    total = stats.get("stats", {})
    pct = total.get("percentiles4", {})
    return {
        "status": "OK",
        "simulation": stats.get("name", "unknown"),
        "responseTimeMs": {
            "p50": total.get("percentiles1", {}).get("total"),
            "p95": total.get("percentiles3", {}).get("total"),
            "p99": pct.get("total"),
        },
        "successRatePct": total.get("group4", {}).get("percentage"),
        "throughputPerSec": total.get("meanNumberOfRequestsPerSecond", {}).get("total"),
    }


def write_markdown(summary: dict, md_path: Path) -> None:
    rt = summary.get("responseTimeMs", {})
    md = f"""# DISP-106 Gatling Summary

- Simulation: `{summary.get('simulation')}`
- Success rate: `{summary.get('successRatePct')}%`
- Throughput: `{summary.get('throughputPerSec')} req/s`

## Response time percentiles

- p50: `{rt.get('p50')} ms`
- p95: `{rt.get('p95')} ms`
- p99: `{rt.get('p99')} ms`
"""
    md_path.parent.mkdir(parents=True, exist_ok=True)
    md_path.write_text(md, encoding="utf-8")


def main():
    if len(sys.argv) != 4:
        print("Usage: summarize_gatling.py <gatlingTargetDir> <outJson> <outMd>")
        sys.exit(1)

    gatling_dir = Path(sys.argv[1])
    out_json = Path(sys.argv[2])
    out_md = Path(sys.argv[3])

    stats_file = latest_stats_json(gatling_dir)
    if stats_file is None:
        print("No Gatling stats.json found")
        sys.exit(2)

    data = json.loads(stats_file.read_text(encoding="utf-8"))
    summary = extract(data)

    out_json.parent.mkdir(parents=True, exist_ok=True)
    out_json.write_text(json.dumps(summary, indent=2), encoding="utf-8")
    write_markdown(summary, out_md)
    print(f"Summary written to {out_json} and {out_md}")


if __name__ == "__main__":
    main()

