from fastapi import APIRouter, Depends, HTTPException
from schemas import ResponseModel
from seed import init_db_and_seed
from models import User
from security import demo_enabled, get_property_admin

router = APIRouter(prefix="/api/dev", tags=["Developer Tools"])

@router.post("/reset-seed", response_model=ResponseModel)
def reset_seed(admin: User = Depends(get_property_admin)):
    """
    一键秒级重置演示数据矩阵:
    将张欣欣、李雷、韩梅梅、王建国与 50 个车位资产完全恢复至初始完美演示状态！
    """
    if not demo_enabled("PARKING_ENABLE_DEMO_RESET"):
        raise HTTPException(status_code=403, detail="演示数据重置默认关闭；仅限隔离演示环境显式开启")
    init_db_and_seed(force=True)
    return ResponseModel(code=200, message="演示数据已完全一键重置为初始状态！")
