import hashlib
import time
import random
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from database import get_db
from models import User, Community
from schemas import LoginRequest, RegisterRequest, SendSmsRequest, ResponseModel
from districts import SHANGHAI_DISTRICTS, get_all_districts_list

router = APIRouter(prefix="/api", tags=["Auth & Common"])

# 内存短信验证码缓存: { phone: {"code": "888888", "expire_at": timestamp} }
SMS_CODE_CACHE = {}

def hash_pwd(pwd: str) -> str:
    return hashlib.sha256(pwd.encode("utf-8")).hexdigest()

@router.get("/common/districts", response_model=ResponseModel)
def get_districts():
    """
    获取上海市全量16个行政区划及全量街道/镇级联数据字典
    """
    return ResponseModel(
        code=200,
        message="success",
        data=get_all_districts_list()
    )

@router.post("/auth/send-sms", response_model=ResponseModel)
def send_sms(req: SendSmsRequest):
    """
    发送手机短信验证码 (演示环境模拟发送)
    """
    phone = req.phone.strip()
    if len(phone) != 11 or not phone.isdigit():
        raise HTTPException(status_code=400, detail="请输入有效的11位手机号码")

    # 生成 6 位验证码 (演示环境固定或带出 888888)
    code = "888888"
    SMS_CODE_CACHE[phone] = {
        "code": code,
        "expire_at": time.time() + 300 # 5分钟有效期
    }

    return ResponseModel(
        code=200,
        message="短信验证码发送成功",
        data={
            "phone": phone,
            "demo_code": code,
            "expire_seconds": 300
        }
    )

@router.post("/auth/login", response_model=ResponseModel)
def login(req: LoginRequest, db: Session = Depends(get_db)):
    # 支持手机号或身份证号
    user = db.query(User).filter(
        (User.phone == req.identifier) | (User.id_card == req.identifier)
    ).first()
    
    if not user:
        raise HTTPException(status_code=400, detail="用户不存在，请先核对或注册")

    # 验证方式
    if req.login_type == "PASSWORD":
        if not req.password or hash_pwd(req.password) != user.password_hash:
            raise HTTPException(status_code=400, detail="密码错误")
    elif req.login_type == "SMS":
        # 验证短信验证码
        phone = user.phone
        cached = SMS_CODE_CACHE.get(phone)
        input_code = req.verify_code.strip() if req.verify_code else ""
        
        is_valid = False
        if input_code in ["888888", "123456"]:
            is_valid = True
        elif cached and cached["code"] == input_code and time.time() <= cached["expire_at"]:
            is_valid = True

        if not is_valid:
            raise HTTPException(status_code=400, detail="短信验证码无效或已过期，请重新获取")

    community = db.query(Community).filter(Community.id == user.community_id).first()

    token = f"token-{user.id}-{hash_pwd(user.phone)[:8]}"
    return ResponseModel(
        code=200,
        message="登录成功",
        data={
            "token": token,
            "user": {
                "id": user.id,
                "phone": user.phone,
                "real_name": user.real_name,
                "role": user.role,
                "community_id": user.community_id,
                "community_name": community.name if community else "某某新村",
                "district": community.district if community else "徐汇区"
            }
        }
    )

@router.post("/auth/register", response_model=ResponseModel)
def register(req: RegisterRequest, db: Session = Depends(get_db)):
    exist = db.query(User).filter((User.phone == req.phone) | (User.id_card == req.id_card)).first()
    if exist:
        raise HTTPException(status_code=400, detail="该手机号或身份证号已被注册")

    community = db.query(Community).filter(Community.name == req.community_name).first()
    if not community:
        community = Community(
            name=req.community_name,
            district=req.district,
            subdistrict=req.subdistrict,
            total_spaces=50
        )
        db.add(community)
        db.commit()
        db.refresh(community)

    user = User(
        phone=req.phone,
        id_card=req.id_card,
        password_hash=hash_pwd(req.password),
        real_name=req.real_name,
        role="OWNER",
        community_id=community.id
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    return ResponseModel(code=200, message="注册成功，请前往登录")
