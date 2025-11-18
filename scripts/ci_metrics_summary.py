#!/usr/bin/env python3
"""
Summarize Maven QA metrics (tests, Jacoco coverage, PITest results,
Dependency-Check counts) and append them to the GitHub Actions job summary.

The script is defensive: if a report is missing (often because a gate was
skipped), we record that fact instead of failing the workflow.
"""

from __future__ import annotations

import json
import os
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional


# Repo root + Maven `target/` folder.
ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "target"


def percent(part: float, whole: float) -> float:
    """Return percentage helper rounded to 0.1 with zero guard."""
    if whole == 0:
        return 0.0
    return round((part / whole) * 100, 1)


def load_jacoco() -> Optional[Dict[str, float]]:
    """Parse JaCoCo XML and return a dict with line-level coverage."""
    report = TARGET / "site" / "jacoco" / "jacoco.xml"
    if not report.exists():
        return None
    try:
        tree = ET.parse(report)
    except ET.ParseError:
        return None

    root = tree.getroot()
    counters = root.findall("./counter")
    if not counters:
        counters = root.iter("counter")
    for counter in counters:
        if counter.attrib.get("type") == "LINE":
            covered = int(counter.attrib.get("covered", "0"))
            missed = int(counter.attrib.get("missed", "0"))
            total = covered + missed
            return {
                "covered": covered,
                "missed": missed,
                "total": total,
                "pct": percent(covered, total),
            }
    return None


def load_pitest() -> Optional[Dict[str, float]]:
    """Parse PITest mutations.xml for kill/survive counts."""
    report = TARGET / "pit-reports" / "mutations.xml"
    if not report.exists():
        return None
    try:
        tree = ET.parse(report)
    except ET.ParseError:
        return None

    mutations = list(tree.getroot().iter("mutation"))
    total = len(mutations)
    if total == 0:
        return {"total": 0, "killed": 0, "pct": 0.0}

    killed = sum(1 for m in mutations if m.attrib.get("status") == "KILLED")
    survived = sum(1 for m in mutations if m.attrib.get("status") == "SURVIVED")
    detected = sum(1 for m in mutations if m.attrib.get("detected") == "true")
    return {
        "total": total,
        "killed": killed,
        "survived": survived,
        "detected": detected,
        "pct": percent(killed, total),
    }


SEVERITY_ORDER = ["CRITICAL", "HIGH", "MEDIUM", "LOW", "UNKNOWN"]
SEVERITY_LABELS = {
    "CRITICAL": "🟥 Critical",
    "HIGH": "🟧 High",
    "MEDIUM": "🟨 Medium",
    "LOW": "🟩 Low",
    "UNKNOWN": "⬜ Unknown",
}


def load_dependency_check() -> Optional[Dict[str, object]]:
    """Parse Dependency-Check JSON for vulnerability counts."""
    report = ROOT / "target" / "dependency-check-report.json"
    if not report.exists():
        return None
    try:
        data = json.loads(report.read_text())
    except json.JSONDecodeError:
        return None

    dependencies = data.get("dependencies", [])
    dep_count = len(dependencies)
    vulnerable_deps = 0
    vuln_total = 0
    severity_counts = defaultdict(int)
    for dep in dependencies:
        vulns = dep.get("vulnerabilities") or []
        if vulns:
            vulnerable_deps += 1
            vuln_total += len(vulns)
            for vuln in vulns:
                severity = (vuln.get("severity") or "UNKNOWN").upper()
                if severity not in SEVERITY_ORDER:
                    severity = "UNKNOWN"
                severity_counts[severity] += 1

    for key in SEVERITY_ORDER:
        severity_counts[key] = severity_counts.get(key, 0)

    return {
        "dependencies": dep_count,
        "vulnerable_dependencies": vulnerable_deps,
        "vulnerabilities": vuln_total,
        "severity": dict(severity_counts),
    }


def load_surefire() -> Optional[Dict[str, float]]:
    """Aggregate JUnit results from Surefire XML reports."""
    report_dir = TARGET / "surefire-reports"
    if not report_dir.exists():
        return None

    total = failures = errors = skipped = 0
    times: List[float] = []

    for xml_path in report_dir.glob("TEST-*.xml"):
        try:
            tree = ET.parse(xml_path)
        except ET.ParseError:
            continue
        root = tree.getroot()
        total += int(root.attrib.get("tests", "0"))
        failures += int(root.attrib.get("failures", "0"))
        errors += int(root.attrib.get("errors", "0"))
        skipped += int(root.attrib.get("skipped", "0"))
        times.append(float(root.attrib.get("time", "0")))

    if total == 0 and failures == 0 and errors == 0:
        return None

    return {
        "tests": total,
        "failures": failures,
        "errors": errors,
        "skipped": skipped,
        "time": round(sum(times), 2),
    }


def color_block(pct: float) -> str:
    if pct >= 90:
        return "🟢"
    if pct >= 80:
        return "🟡"
    return "🔴"


def bar(pct: float, width: int = 20) -> str:
    filled = int(round((pct / 100) * width))
    filled = max(0, min(width, filled))
    return "█" * filled + "░" * (width - filled)


def section_header() -> str:
    """Identify the current matrix entry (os + JDK)."""
    matrix_os = os.environ.get("MATRIX_OS", "unknown-os")
    matrix_java = os.environ.get("MATRIX_JAVA", "unknown")
    return f"### QA Metrics ({matrix_os}, JDK {matrix_java})"


def format_row(metric: str, value: str, detail: str) -> str:
    """Helper for Markdown table rows."""
    return f"| {metric} | {value} | {detail} |"


def severity_summary(counts: Dict[str, int]) -> str:
    parts = []
    for level in SEVERITY_ORDER:
        parts.append(f"{SEVERITY_LABELS[level]}: {counts.get(level, 0)}")
    return ", ".join(parts)


def write_dashboard(
        tests: Optional[Dict[str, float]],
        jacoco: Optional[Dict[str, float]],
        pit: Optional[Dict[str, float]],
        dep: Optional[Dict[str, object]],
) -> None:
    """Generate an HTML dashboard with nicer styling."""
    dashboard_dir = TARGET / "site" / "qa-dashboard"
    dashboard_dir.mkdir(parents=True, exist_ok=True)
    html_path = dashboard_dir / "index.html"

    def progress_bar(pct: float) -> str:
        return f"""
            <div class="progress">
                <div class="progress-bar" style="width:{pct:.1f}%"></div>
            </div>
        """

    tests_primary = "No data"
    tests_secondary = "Surefire reports not found."
    if tests:
        passed = tests["tests"] - tests["failures"] - tests["errors"] - tests["skipped"]
        status = "✅ All passing" if tests["failures"] == 0 and tests["errors"] == 0 else "⚠️ Attention needed"
        extra = ""
        if tests["failures"] or tests["errors"] or tests["skipped"]:
            extra = f" (failures: {tests['failures']}, errors: {tests['errors']}, skipped: {tests['skipped']})"
        tests_primary = f"{status} — {passed}/{tests['tests']} tests green"
        tests_secondary = f"Runtime: {tests['time']}s{extra}"

    jacoco_primary = "No data"
    jacoco_secondary = "Jacoco XML report missing."
    jacoco_progress = ""
    if jacoco:
        jacoco_primary = f"{jacoco['pct']}% covered"
        jacoco_secondary = f"{jacoco['covered']} / {jacoco['total']} lines"
        jacoco_progress = progress_bar(jacoco["pct"])

    pit_primary = "No data"
    pit_secondary = "PITest report missing or skipped."
    pit_progress = ""
    if pit:
        pit_primary = f"{pit['pct']}% mutations killed"
        pit_secondary = f"{pit['killed']} / {pit['total']} mutants (survived {pit['survived']})"
        pit_progress = progress_bar(pit["pct"])

    dep_primary = "Not run"
    dep_secondary = "Dependency-Check report missing."
    severity_list = ""
    if dep:
        dep_primary = (
            f"{dep['vulnerable_dependencies']} vulnerable deps "
            f"({dep['vulnerabilities']} findings)"
        )
        dep_secondary = f"Scanned dependencies: {dep['dependencies']}"
        severity_items = "".join(
            f"<li>{SEVERITY_LABELS[level]} — {dep['severity'].get(level, 0)}</li>"
            for level in SEVERITY_ORDER
        )
        severity_list = f"<ul class='severity'>{severity_items}</ul>"

    timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")

    html_content = f"""<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>QA Dashboard</title>
    <style>
        body {{
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
            background: #0f172a;
            color: #e2e8f0;
            margin: 0;
            padding: 2rem;
        }}
        h1 {{
            margin-top: 0;
        }}
        .grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 1.5rem;
        }}
        .card {{
            background: rgba(30, 41, 59, 0.9);
            border-radius: 12px;
            padding: 1.25rem;
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.4);
        }}
        .label {{
            text-transform: uppercase;
            font-size: 0.9rem;
            letter-spacing: 0.08em;
            color: #94a3b8;
        }}
        .value {{
            font-size: 1.4rem;
            margin: 0.4rem 0;
            color: #38bdf8;
        }}
        .detail {{
            color: #cbd5f5;
            font-size: 0.95rem;
        }}
        .progress {{
            background: #1e293b;
            border-radius: 999px;
            height: 6px;
            margin-top: 0.75rem;
        }}
        .progress-bar {{
            background: linear-gradient(90deg, #22d3ee, #14b8a6);
            height: 100%;
            border-radius: inherit;
        }}
        .links {{
            margin-top: 2rem;
        }}
        .links a {{
            color: #38bdf8;
            margin-right: 1rem;
            text-decoration: none;
            border-bottom: 1px solid transparent;
        }}
        .links a:hover {{
            border-color: #38bdf8;
        }}
        .severity {{
            padding-left: 1.2rem;
            margin: 0.5rem 0 0;
            color: #e2e8f0;
        }}
        footer {{
            margin-top: 2rem;
            font-size: 0.85rem;
            color: #94a3b8;
        }}
    </style>
</head>
<body>
    <h1>QA Dashboard</h1>
    <p>Generated {timestamp}. Download the artifact from GitHub Actions for interactive viewing.</p>
    <div class="grid">
        <div class="card">
            <div class="label">Tests</div>
            <div class="value">{tests_primary}</div>
            <div class="detail">{tests_secondary}</div>
        </div>
        <div class="card">
            <div class="label">Line Coverage</div>
            <div class="value">{jacoco_primary}</div>
            <div class="detail">{jacoco_secondary}</div>
            {jacoco_progress}
        </div>
        <div class="card">
            <div class="label">Mutation Score</div>
            <div class="value">{pit_primary}</div>
            <div class="detail">{pit_secondary}</div>
            {pit_progress}
        </div>
        <div class="card">
            <div class="label">Dependency-Check</div>
            <div class="value">{dep_primary}</div>
            <div class="detail">{dep_secondary}</div>
            {severity_list}
        </div>
    </div>
    <div class="links">
        <strong>Detailed reports:</strong>
        <a href="../jacoco/index.html">JaCoCo</a>
        <a href="../spotbugs.html">SpotBugs</a>
        <a href="../../pit-reports/index.html">PITest</a>
        <a href="../dependency-check-report.html">Dependency-Check</a>
    </div>
    <footer>QA dashboard generated by scripts/ci_metrics_summary.py</footer>
</body>
</html>
"""

    html_path.write_text("\n".join(line.rstrip() for line in html_content.splitlines()), encoding="utf-8")


def main() -> int:
    summary_lines = [section_header(), "", "| Metric | Result | Details |", "| --- | --- | --- |"]

    tests = load_surefire()
    if tests:
        status_icon = "✅" if tests["failures"] == 0 and tests["errors"] == 0 else "⚠️"
        summary_lines.append(
            format_row(
                "Tests",
                f"{status_icon} {tests['tests']} executed",
                f"Runtime {tests['time']}s — failures: {tests['failures']}, errors: {tests['errors']}, skipped: {tests['skipped']}",
            )
        )
    else:
        summary_lines.append(format_row("Tests", "_no data_", "Surefire reports not found."))

    jacoco = load_jacoco()
    if jacoco:
        coverage_text = f"{color_block(jacoco['pct'])} {jacoco['pct']}% {bar(jacoco['pct'])}"
        detail = f"{jacoco['covered']} / {jacoco['total']} lines covered"
        summary_lines.append(format_row("Line coverage (JaCoCo)", coverage_text, detail))
    else:
        summary_lines.append(format_row("Line coverage (JaCoCo)", "_no data_", "Jacoco XML report missing."))

    pit = load_pitest()
    if pit:
        detail = f"{pit['killed']} killed, {pit['survived']} survived out of {pit['total']} mutations"
        summary_lines.append(
            format_row("Mutation score (PITest)", f"{color_block(pit['pct'])} {pit['pct']}% {bar(pit['pct'])}", detail)
        )
    else:
        summary_lines.append(
            format_row("Mutation score (PITest)", "_no data_", "PITest report not generated (likely skipped).")
        )

    dep = load_dependency_check()
    if dep:
        detail = (
            f"{dep['vulnerable_dependencies']} dependencies with issues "
            f"({dep['vulnerabilities']} vulnerabilities) out of {dep['dependencies']} scanned. "
            f"{severity_summary(dep['severity'])}"
        )
        summary_lines.append(format_row("Dependency-Check", "✅ scan complete", detail))
    else:
        summary_lines.append(
            format_row(
                "Dependency-Check",
                "_not run_",
                "Report missing (probably skipped when `NVD_API_KEY` was not provided).",
            )
        )

    summary_lines.append("")
    summary_lines.append(
        "Interactive dashboard: `target/site/qa-dashboard/index.html` (packaged in the `quality-reports-*` artifact)."
    )
    summary_lines.append("Artifacts: `target/site/`, `target/pit-reports/`, `target/dependency-check-report.*`.")
    summary_lines.append("")

    summary_text = "\n".join(summary_lines) + "\n"

    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as handle:
            handle.write(summary_text)
    else:
        print(summary_text)

    write_dashboard(tests, jacoco, pit, dep)
    return 0


if __name__ == "__main__":
    sys.exit(main())
