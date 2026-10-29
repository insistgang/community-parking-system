"""Authentication regressions; all database access stays in a task-local database."""

import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

PROJECT = Path(__file__).resolve().parents[2]
(PROJECT / ".tmp").mkdir(exist_ok=True)
TEST_DIR = tempfile.TemporaryDirectory(prefix="auth-test-", dir=PROJECT / ".tmp")
os.environ["DATABASE_URL"] = f"sqlite:///{TEST_DIR.name}/parking.db"
for flag in ("PARKING_ENABLE_DEMO_RESET", "PARKING_ENABLE_DEMO_SMS", "PARKING_ENABLE_DEMO_ADMIN_REGISTRATION"):
    os.environ.pop(flag, None)
sys.path.insert(0, str(PROJECT / "backend"))

from fastapi.testclient import TestClient
from main import app
from routers import dev


class AuthenticationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.client = TestClient(app)

    @classmethod
    def tearDownClass(cls):
        cls.client.close()
        from database import engine
        engine.dispose()
        TEST_DIR.cleanup()

    def login(self, identifier="13800000001"):
        response = self.client.post("/api/auth/login", json={
            "identifier": identifier, "password": "123456", "login_type": "PASSWORD",
        })
        self.assertEqual(response.status_code, 200, response.text)
        return response.json()["data"]

    def headers(self, identifier="13800000001"):
        return {"Authorization": f"Bearer {self.login(identifier)['token']}"}

    def test_forged_user_id_without_token_cannot_access_business_routes(self):
        for path in ("/api/user/home", "/api/parking/queue", "/api/evidence/list", "/api/property/stats"):
            with self.subTest(path=path):
                response = self.client.get(path, headers={"X-User-Id": "4"})
                self.assertEqual(response.status_code, 401, response.text)

    def test_owner_token_works_and_cannot_be_elevated_by_user_id_header(self):
        headers = self.headers()
        self.assertEqual(self.client.get("/api/user/home", headers=headers).status_code, 200)
        self.assertEqual(self.client.get("/api/evidence/list", headers=headers).status_code, 200)
        headers["X-User-Id"] = "4"
        self.assertEqual(self.client.get("/api/property/stats", headers=headers).status_code, 403)

    def test_admin_password_login_allows_property_business(self):
        response = self.client.get("/api/property/stats", headers=self.headers("13900000001"))
        self.assertEqual(response.status_code, 200, response.text)

    def test_tampered_token_is_rejected(self):
        token = self.login()["token"]
        token = token[:-1] + ("a" if token[-1] != "a" else "b")
        response = self.client.get("/api/user/home", headers={"Authorization": f"Bearer {token}"})
        self.assertEqual(response.status_code, 401)

    def test_old_predictable_token_is_rejected(self):
        response = self.client.get("/api/user/home", headers={
            "Authorization": "Bearer token-4-anything", "X-User-Id": "4",
        })
        self.assertEqual(response.status_code, 401)

    def test_expired_token_is_rejected(self):
        with patch("time.time", return_value=1):
            token = self.login()["token"]
        response = self.client.get("/api/user/home", headers={"Authorization": f"Bearer {token}"})
        self.assertEqual(response.status_code, 401)

    def test_unknown_login_type_cannot_skip_password(self):
        response = self.client.post("/api/auth/login", json={
            "identifier": "13900000001", "login_type": "BYPASS",
        })
        self.assertEqual(response.status_code, 422)

    def test_fixed_sms_code_and_sending_demo_code_are_disabled_by_default(self):
        response = self.client.post("/api/auth/login", json={
            "identifier": "13900000001", "login_type": "SMS", "verify_code": "888888",
        })
        self.assertEqual(response.status_code, 403)
        self.assertEqual(self.client.post("/api/auth/send-sms", json={"phone": "13900000001"}).status_code, 403)

    def test_demo_sms_requires_requested_unexpired_one_use_code(self):
        with patch.dict(os.environ, {"PARKING_ENABLE_DEMO_SMS": "true"}):
            login = {"identifier": "13800000002", "login_type": "SMS", "verify_code": "888888"}
            self.assertEqual(self.client.post("/api/auth/login", json=login).status_code, 400)
            sent = self.client.post("/api/auth/send-sms", json={"phone": login["identifier"]})
            self.assertEqual(sent.status_code, 200)
            login["verify_code"] = sent.json()["data"]["demo_code"]
            self.assertEqual(self.client.post("/api/auth/login", json=login).status_code, 200)
            self.assertEqual(self.client.post("/api/auth/login", json=login).status_code, 400)

    def test_public_registration_cannot_create_property_admin(self):
        response = self.client.post("/api/auth/register", json={
            "district": "徐汇区", "subdistrict": "龙华街道", "community_name": "龙华新村",
            "real_name": "Test Admin", "id_card": "test-admin-id", "phone": "13955550000",
            "password": "test-password", "role": "PROPERTY_ADMIN",
        })
        self.assertEqual(response.status_code, 403)

    def test_owner_registration_issues_usable_token(self):
        response = self.client.post("/api/auth/register", json={
            "district": "徐汇区", "subdistrict": "龙华街道", "community_name": "龙华新村",
            "real_name": "Test Owner", "id_card": "test-owner-id", "phone": "13855550000",
            "password": "test-password", "role": "OWNER",
        })
        self.assertEqual(response.status_code, 200, response.text)
        token = response.json()["data"]["token"]
        self.assertEqual(self.client.get("/api/user/home", headers={"Authorization": f"Bearer {token}"}).status_code, 200)

    def test_reset_is_disabled_even_for_authenticated_admin(self):
        with patch.object(dev, "init_db_and_seed") as reset:
            response = self.client.post("/api/dev/reset-seed", headers=self.headers("13900000001"))
            self.assertEqual(response.status_code, 403)
            reset.assert_not_called()

    def test_enabled_reset_requires_admin_and_calls_seed_only_after_auth(self):
        with patch.dict(os.environ, {"PARKING_ENABLE_DEMO_RESET": "true"}), patch.object(dev, "init_db_and_seed") as reset:
            response = self.client.post("/api/dev/reset-seed", headers={"X-User-Id": "4"})
            self.assertEqual(response.status_code, 401)
            response = self.client.post("/api/dev/reset-seed", headers=self.headers())
            self.assertEqual(response.status_code, 403)
            reset.assert_not_called()
            response = self.client.post("/api/dev/reset-seed", headers=self.headers("13900000001"))
            self.assertEqual(response.status_code, 200, response.text)
            reset.assert_called_once_with(force=True)


if __name__ == "__main__":
    unittest.main()
