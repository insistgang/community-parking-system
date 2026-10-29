import os
import asyncio
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from database import engine, Base, SessionLocal, run_migrations
from models import Community
from seed import init_db_and_seed
from scheduler import check_expired_allocations
from routers import auth, parking, property, evidence, dev

# 确保启动时表存在并执行轻量迁移
Base.metadata.create_all(bind=engine)
run_migrations()
try:
    init_db_and_seed(force=False)
except Exception as e:
    print(f"[Startup Warning] Seed initialization check skipped: {e}")

app = FastAPI(
    title="慧停管家 · 老旧小区停车位管理平台 API",
    description="面向老旧小区车位供需失衡的公平排位、三证合一准入与虚拟区块链存证系统",
    version="3.0.0"
)

# 允许跨域请求
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://127.0.0.1:5173", "http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 全局异常处理
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=500,
        content={"code": 500, "message": f"服务器内部异常: {str(exc)}", "data": None}
    )

# 挂载路由
app.include_router(auth.router)
app.include_router(parking.router)
app.include_router(property.router)
app.include_router(evidence.router)
app.include_router(dev.router)

async def _timeout_sweeper():
    """后台定时任务: 每分钟全量扫描所有小区，自动收回超过 48 小时未确认的中签车位"""
    while True:
        await asyncio.sleep(60)
        db = SessionLocal()
        try:
            for (cid,) in db.query(Community.id).all():
                check_expired_allocations(db, cid)
        except Exception:
            db.rollback()
        finally:
            db.close()

@app.on_event("startup")
async def start_timeout_sweeper():
    asyncio.create_task(_timeout_sweeper())

@app.get("/")
def root():
    return {
        "system": "慧停管家 API",
        "version": "v3.0",
        "status": "online",
        "docs_url": "/docs"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host=os.getenv("PARKING_HOST", "127.0.0.1"), port=8000, reload=True)
