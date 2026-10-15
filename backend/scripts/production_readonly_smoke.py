"""Read-only production API smoke check. Login/logout create only session audit records.

Usage:
  python production_readonly_smoke.py --base-url http://HOST:PORT/api --credentials account-book.xlsx
"""

import argparse
import json
import urllib.error
import urllib.request
from pathlib import Path

from openpyxl import load_workbook


def call(base_url, method, route, token=None, body=None, binary=False):
    headers = {}
    if token:
        headers["Authorization"] = "Bearer " + token
    if body is not None:
        headers["Content-Type"] = "application/json"
    payload = None if body is None else json.dumps(body, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(base_url + route, data=payload, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            raw = response.read()
            return response.status, raw if binary else json.loads(raw)
    except urllib.error.HTTPError as error:
        raw = error.read()
        return error.code, raw if binary else json.loads(raw)


def require(base_url, method, route, token=None, body=None, status=200, binary=False):
    actual, data = call(base_url, method, route, token, body, binary)
    if actual != status:
        raise AssertionError(f"{method} {route}: expected HTTP {status}, got {actual}")
    return data


def login(base_url, phone, password, admin=False):
    route = "/admin/login" if admin else "/auth/login"
    field = "username" if admin else "identifier"
    response = require(base_url, "POST", route, body={field: phone, "password": password})
    return response["data"]["token"]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--credentials", type=Path, required=True)
    args = parser.parse_args()
    base_url = args.base_url.rstrip("/")
    workbook = load_workbook(args.credentials, read_only=True, data_only=True)
    rows = list(workbook.worksheets[0].iter_rows(min_row=9, values_only=True))
    accounts = {str(row[2]): row for row in rows if row[2]}
    required_phones = {"13900000000", "18616871739", "13900000001", "13901922616"}
    if not required_phones.issubset(accounts):
        raise AssertionError("Account workbook lacks a required role")
    rental = next(row for row in rows if row[0] == "在租台账")
    sessions = []
    checked = 0
    try:
        require(base_url, "GET", "/health")
        checked += 1
        require(base_url, "GET", "/common/districts")
        checked += 1
        admin = login(base_url, "13900000000", accounts["13900000000"][3], True)
        manager = login(base_url, "18616871739", accounts["18616871739"][3])
        owner = login(base_url, "13900000001", accounts["13900000001"][3])
        queued = login(base_url, "13901922616", accounts["13901922616"][3])
        rented = login(base_url, str(rental[2]), rental[3])
        sessions.extend((admin, manager, owner, queued, rented))

        for route in ("/admin/stats", "/admin/users", "/admin/login-logs",
                      "/admin/operation-logs", "/admin/external-api-logs"):
            require(base_url, "GET", route, admin)
            checked += 1
        for route in ("/property/stats", "/property/spaces", "/property/audit-config",
                      "/property/community-users", "/property/applications"):
            require(base_url, "GET", route, manager)
            checked += 1
        property_stats = require(base_url, "GET", "/property/stats", manager)["data"]
        if property_stats["legacy_import_count"] == 167:
            if property_stats["total_spaces"] != 167 or property_stats["rented_spaces"] != 167:
                raise AssertionError("Pilot parking inventory counts changed")
            if property_stats["occupancy_rate"] != "100%":
                raise AssertionError("Recorded pilot occupancy is not 100%")
        checked += 1
        require(base_url, "GET", "/property/export-spaces", manager, binary=True)
        checked += 1
        for route in ("/user/home", "/parking/audit-mode", "/parking/queue",
                      "/parking/my-timeline", "/notifications", "/evidence/list"):
            require(base_url, "GET", route, owner)
            checked += 1
        queue_home = require(base_url, "GET", "/user/home", queued)["data"]
        if queue_home["view_state"] != "WAITING" or queue_home["my_queue"]["current_rank"] != 1:
            raise AssertionError("Historical queue position changed")
        if queue_home["my_queue"]["identity_pending"] is not True:
            raise AssertionError("Historical identity status changed")
        checked += 1
        rental_home = require(base_url, "GET", "/user/home", rented)["data"]
        if rental_home["view_state"] != "IDENTITY_PENDING" or rental_home["my_space"] is not None:
            raise AssertionError("Unverified rental account has linked lease")
        checked += 1
        evidence = require(base_url, "GET", "/evidence/list", queued)["data"]
        init_record = next((item for item in evidence if item["event_type"] == "PILOT_IMPORT"), None)
        if init_record is None or init_record["external_status"] != "NOT_CONNECTED":
            raise AssertionError("Initialization audit record missing or mislabeled")
        result = require(base_url, "GET", "/evidence/verify/" + init_record["evidence_id"], queued)["data"]
        if result["is_valid"] is not True or result["verification_status"] != "LOCAL_INTEGRITY_VERIFIED":
            raise AssertionError("Local audit chain verification failed")
        checked += 2
        require(base_url, "GET", "/property/stats", owner, status=403)
        require(base_url, "GET", "/admin/users", manager, status=403)
        checked += 2
    finally:
        for token in sessions:
            try:
                call(base_url, "POST", "/auth/logout", token, {})
            except Exception:
                pass
    print(json.dumps({"result": "PASS", "checked_api_calls": checked,
                      "accounts_logged_in": len(sessions)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
