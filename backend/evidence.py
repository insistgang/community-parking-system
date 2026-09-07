import hashlib
import time
import uuid
import json
from sqlalchemy.orm import Session
from models import ChainEvidence

def create_evidence_record(db: Session, community_id: int, user_id: int | None, event_type: str, summary: str, payload: dict) -> ChainEvidence:
    """
    虚拟区块链存证核心算法:
    1. 查询链上最近一条存证的 tx_hash 作为 prev_hash 构建链式结构
    2. 计算当前虚拟区块高度 (自增)
    3. 基于事件类型、用户ID、时间戳、prev_hash 与快照数据计算 SHA-256 哈希 (0x...)
    4. 生成唯一存证凭据号: AGY-EVENT-时间戳后6位-4位随机UUID
    """
    last_record = db.query(ChainEvidence).order_by(ChainEvidence.id.desc()).first()
    prev_hash = last_record.tx_hash if last_record else "0x0000000000000000000000000000000000000000000000000000000000000000"
    last_height = last_record.block_height if last_record else 1829000

    timestamp_ms = str(int(time.time() * 1000))
    payload_str = json.dumps(payload, ensure_ascii=False, sort_keys=True)
    
    raw_str = f"{event_type}_{community_id}_{user_id}_{timestamp_ms}_{prev_hash}_{payload_str}"
    tx_hash = "0x" + hashlib.sha256(raw_str.encode('utf-8')).hexdigest()
    evidence_id = f"AGY-{event_type}-{timestamp_ms[-6:]}-{uuid.uuid4().hex[:4].upper()}"

    record = ChainEvidence(
        evidence_id=evidence_id,
        community_id=community_id,
        user_id=user_id,
        event_type=event_type,
        block_height=last_height + 1,
        tx_hash=tx_hash,
        prev_hash=prev_hash,
        event_summary=summary,
        raw_payload_json=payload_str,
        timestamp=timestamp_ms
    )
    db.add(record)
    db.commit()
    db.refresh(record)
    return record
