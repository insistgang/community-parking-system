"""Short-lived signed sessions for the local demonstration API."""

import base64
import binascii
import hashlib
import hmac
import json
import os
import secrets
import time

from fastapi import Depends, Header, HTTPException
from sqlalchemy.orm import Session

from database import get_db
from models import User

_configured_secret = os.getenv("PARKING_AUTH_SECRET", "")
if _configured_secret and len(_configured_secret.encode("utf-8")) < 32:
    raise RuntimeError("PARKING_AUTH_SECRET 至少需要 32 字节")
_SIGNING_KEY = _configured_secret.encode("utf-8") if _configured_secret else secrets.token_bytes(32)
SESSION_TTL_SECONDS = 8 * 60 * 60


def demo_enabled(name: str) -> bool:
    """Demo-only capabilities require an explicit true environment setting."""
    return os.getenv(name, "").strip().lower() == "true"


def issue_token(user_id: int) -> str:
    payload = {"sub": user_id, "exp": int(time.time()) + SESSION_TTL_SECONDS,
               "nonce": secrets.token_hex(8)}
    body = base64.urlsafe_b64encode(json.dumps(payload, separators=(",", ":")).encode()).decode().rstrip("=")
    signed = f"v1.{body}"
    signature = hmac.new(_SIGNING_KEY, signed.encode(), hashlib.sha256).hexdigest()
    return f"{signed}.{signature}"


def _unauthorized() -> HTTPException:
    return HTTPException(status_code=401, detail="登录状态无效或已过期，请重新登录",
                         headers={"WWW-Authenticate": "Bearer"})


def get_current_user(authorization: str | None = Header(default=None),
                     db: Session = Depends(get_db)) -> User:
    """Only the verified Bearer subject determines the caller's identity."""
    if not authorization or len(authorization) > 2048:
        raise _unauthorized()
    try:
        scheme, token = authorization.split(" ", 1)
        version, body, signature = token.split(".")
        if scheme.lower() != "bearer" or version != "v1":
            raise ValueError("invalid scheme")
        expected = hmac.new(_SIGNING_KEY, f"{version}.{body}".encode(), hashlib.sha256).hexdigest()
        if not hmac.compare_digest(signature, expected):
            raise ValueError("invalid signature")
        raw = base64.b64decode(body + "=" * (-len(body) % 4), altchars=b"-_", validate=True)
        payload = json.loads(raw)
        user_id, expires = payload["sub"], payload["exp"]
        if type(user_id) is not int or user_id <= 0 or type(expires) is not int or expires <= time.time():
            raise ValueError("invalid or expired session")
    except (ValueError, TypeError, KeyError, UnicodeError, binascii.Error):
        raise _unauthorized() from None
    user = db.query(User).filter(User.id == user_id).first()
    if user is None:
        raise _unauthorized()
    return user


def get_property_admin(user: User = Depends(get_current_user)) -> User:
    if user.role != "PROPERTY_ADMIN":
        raise HTTPException(status_code=403, detail="仅限物业管理员权限访问")
    return user
