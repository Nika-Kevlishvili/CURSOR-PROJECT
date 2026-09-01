#!/usr/bin/env python3
"""Upload 40 POD mass-import xlsx files one by one to Dev2.

Waits until each PROCESS_POD_MASS_IMPORT reaches COMPLETED or CANCELED
before starting the next upload, to avoid stacking XSSFWorkbook heap load.
"""
from __future__ import annotations

import json
import os
import ssl
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ENV_PATH = Path("/Users/lukachrikishvili/Desktop/CURSOR-PROJECT/Cursor-Project/.env")
SRC_DIR = Path("/Users/lukachrikishvili/Desktop/mass imports/2 million pod")
PROGRESS_PATH = SRC_DIR / "upload_progress.json"
BASE = "https://devapps.energo-pro.bg/backend/phoenix2-dev"
UPLOAD_URL = f"{BASE}/mass-import/PODS/files/upload"
PROCESS_LIST_URL = f"{BASE}/process"
FILE_COUNT = 200
START_PART = 26
UPLOAD_TIMEOUT_S = 600
POLL_INTERVAL_S = 30
PROCESS_TIMEOUT_S = 4 * 60 * 60
WAIT_FOR_PROCESS_COMPLETE = True
UPLOAD_RETRIES = 6
TERMINAL = {"COMPLETED", "CANCELED"}
CTX = ssl._create_unverified_context()


def load_env() -> None:
    for line in ENV_PATH.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.strip().startswith("#"):
            k, v = line.split("=", 1)
            os.environ.setdefault(k.strip(), v.strip().strip('"').strip("'"))


def login() -> str:
    body = json.dumps(
        {"user": os.environ["PORTAL_USER"], "password": os.environ["PASSWORD"]}
    ).encode()
    req = urllib.request.Request(
        os.environ["DEVAUTHAPI"],
        data=body,
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=30, context=CTX) as resp:
        payload = json.load(resp)
    token = payload.get("jwt")
    if not token:
        raise SystemExit("LOGIN_FAILED: no jwt")
    return token


def api_json(url: str, token: str, timeout: int = 60) -> tuple[int, object]:
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(req, timeout=timeout, context=CTX) as resp:
            raw = resp.read()
            data = json.loads(raw.decode()) if raw else None
            return resp.status, data
    except urllib.error.HTTPError as e:
        raw = e.read().decode(errors="replace")
        try:
            data = json.loads(raw) if raw else raw
        except json.JSONDecodeError:
            data = raw
        return e.code, data


def list_pod_processes(token: str, size: int = 30) -> list[dict]:
    qs = urllib.parse.urlencode(
        {
            "page": 0,
            "size": size,
            "sortBy": "CREATE_DATE",
            "sortDirection": "DESC",
            "prompt": "PROCESS_POD_MASS_IMPORT",
            "searchBy": "NAME",
        }
    )
    status, data = api_json(f"{PROCESS_LIST_URL}?{qs}", token)
    if status != 200:
        raise SystemExit(f"PROCESS_LIST_FAIL {status} {data}")
    content = data.get("content") if isinstance(data, dict) else None
    if content is None:
        raise SystemExit(f"PROCESS_LIST_UNEXPECTED {data}")
    return content


def get_process(token: str, process_id: int) -> dict:
    status, data = api_json(f"{PROCESS_LIST_URL}/{process_id}", token)
    if status != 200:
        raise SystemExit(f"PROCESS_GET_FAIL id={process_id} {status} {data}")
    return data


def upload_file(path: Path, token: str) -> int:
    last_err = None
    for attempt in range(1, UPLOAD_RETRIES + 1):
        boundary = f"----CursorBoundary{int(time.time() * 1000)}"
        filename = path.name
        file_bytes = path.read_bytes()
        parts = [
            f"--{boundary}\r\n".encode(),
            (
                f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'
                "Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n"
            ).encode(),
            file_bytes,
            b"\r\n",
            f"--{boundary}--\r\n".encode(),
        ]
        body = b"".join(parts)
        req = urllib.request.Request(
            UPLOAD_URL,
            data=body,
            method="POST",
            headers={
                "Authorization": f"Bearer {token}",
                "Content-Type": f"multipart/form-data; boundary={boundary}",
            },
        )
        try:
            with urllib.request.urlopen(req, timeout=UPLOAD_TIMEOUT_S, context=CTX) as resp:
                _ = resp.read()
                return resp.status
        except urllib.error.HTTPError as e:
            raw = e.read().decode(errors="replace")[:400]
            last_err = f"http={e.code} body={raw}"
            if e.code in (500, 502, 503, 504) and attempt < UPLOAD_RETRIES:
                wait_s = min(60, 10 * attempt)
                print(f"RETRY {path.name} attempt={attempt} {last_err} wait_s={wait_s}", flush=True)
                time.sleep(wait_s)
                continue
            raise SystemExit(f"UPLOAD_FAIL {path.name} {last_err}")
    raise SystemExit(f"UPLOAD_FAIL {path.name} {last_err}")


def save_progress(payload: dict) -> None:
    PROGRESS_PATH.write_text(json.dumps(payload, indent=2), encoding="utf-8")


def main() -> None:
    load_env()
    token = login()
    print("LOGIN_OK", flush=True)
    results = []
    known_ids = {
        item["id"]
        for item in list_pod_processes(token, size=80)
        if str(item.get("name", "")).startswith("PROCESS_POD_MASS_IMPORT_")
    }
    print(f"KNOWN_POD_PROCESSES {len(known_ids)} START_PART={START_PART}", flush=True)
    if PROGRESS_PATH.exists():
        prior = json.loads(PROGRESS_PATH.read_text(encoding="utf-8"))
        results = list(prior.get("completed") or [])
    else:
        results = []

    for part in range(START_PART, FILE_COUNT + 1):
        path = SRC_DIR / f"pod-mass-import-dev2-34493-part-{part:03d}-of-{FILE_COUNT:03d}.xlsx"
        if not path.exists():
            raise SystemExit(f"MISSING {path}")
        token = login()
        t0 = time.time()
        http_status = upload_file(path, token)
        upload_s = time.time() - t0
        print(f"UPLOADED {path.name} http={http_status} elapsed_s={upload_s:.1f}", flush=True)
        if http_status not in (200, 202, 204):
            raise SystemExit(f"UNEXPECTED_STATUS {http_status} for {path.name}")

        process_id = None
        for _ in range(15):
            time.sleep(2)
            for item in list_pod_processes(token, size=30):
                name = str(item.get("name", ""))
                pid = item.get("id")
                if name.startswith("PROCESS_POD_MASS_IMPORT_") and pid not in known_ids:
                    process_id = pid
                    break
            if process_id:
                break
        if not process_id:
            raise SystemExit(f"PROCESS_NOT_FOUND after upload of {path.name}")
        known_ids.add(process_id)
        print(f"PROCESS {process_id} started for {path.name}", flush=True)

        token = login()
        proc = get_process(token, process_id)
        last_status = proc.get("status")
        if WAIT_FOR_PROCESS_COMPLETE:
            deadline = time.time() + PROCESS_TIMEOUT_S
            while time.time() < deadline:
                token = login()
                proc = get_process(token, process_id)
                last_status = proc.get("status")
                print(f"POLL {path.name} process={process_id} status={last_status}", flush=True)
                if last_status in TERMINAL:
                    break
                time.sleep(POLL_INTERVAL_S)
            else:
                raise SystemExit(
                    f"PROCESS_TIMEOUT {path.name} process={process_id} last_status={last_status}"
                )
        else:
            print(f"SKIP_WAIT {path.name} process={process_id} status={last_status}", flush=True)
            time.sleep(15)

        row = {
            "file": path.name,
            "httpStatus": http_status,
            "uploadSeconds": round(upload_s, 1),
            "processId": process_id,
            "processStatus": last_status,
            "processName": f"PROCESS_POD_MASS_IMPORT_{process_id}",
        }
        results.append(row)
        save_progress({"completed": results, "nextPart": part + 1})
        print(
            f"FINISHED {path.name} process={process_id} status={last_status}",
            flush=True,
        )

    print(f"DONE files={len(results)}", flush=True)
    save_progress({"completed": results, "done": True})


if __name__ == "__main__":
    main()
