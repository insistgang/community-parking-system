from fastapi import APIRouter
from schemas import ResponseModel
from seed import init_db_and_seed

router = APIRouter(prefix="/api/dev", tags=["Developer Tools"])

@router.post("/reset-seed", response_model=ResponseModel)
def reset_seed():
    """
    一键秒级重置演示数据矩阵:
    将张欣欣、李雷、韩梅梅、王建国与 50 个车位资产完全恢复至初始完美演示状态！
    """
    init_db_and_seed(force=True)
    return ResponseModel(code=200, message="演示数据已完全一键重置为初始状态！")
