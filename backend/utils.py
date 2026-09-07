from datetime import datetime, timedelta

BJ_OFFSET = timedelta(hours=8)

def fmt_bj(dt) -> str:
    """数据库存储为 UTC 时间，对外展示统一转换为北京时间 (UTC+8)"""
    if not dt:
        return "--"
    return (dt + BJ_OFFSET).strftime("%Y-%m-%d %H:%M:%S")
