import os
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from database import engine, Base
from seed import init_db_and_seed
from routers import auth, parking, property, evidence, dev

# 确保启动时表存在
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="老旧小区停车位综合管理平台 API",
    description="面向老旧小区车位供需失衡的公平排位、三证合一准入与虚拟区块链存证系统",
    version="3.0.0"
)

# 允许跨域请求
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
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

@app.get("/")
def root():
    return {
        "system": "老旧小区停车位管理系统 API",
        "version": "v3.0",
        "status": "online",
        "docs_url": "/docs"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
