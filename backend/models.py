from datetime import datetime
from sqlalchemy import Column, Integer, String, Boolean, DateTime, Date, ForeignKey, Text
from sqlalchemy.orm import relationship
from database import Base

class Community(Base):
    """小区基础表"""
    __tablename__ = "communities"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), nullable=False) # 某某新村
    district = Column(String(50), nullable=False) # 徐汇区
    subdistrict = Column(String(50), nullable=False) # 湖南路街道
    total_spaces = Column(Integer, default=50) # 总车位数 50

    users = relationship("User", back_populates="community")
    spaces = relationship("ParkingSpace", back_populates="community")

class User(Base):
    """用户账号表 (业主主账号 / 家庭副账号 / 物业管理员)"""
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    phone = Column(String(20), unique=True, index=True, nullable=False)
    id_card = Column(String(18), unique=True, index=True, nullable=False)
    password_hash = Column(String(128), nullable=False)
    real_name = Column(String(50), nullable=False)
    role = Column(String(20), default="OWNER") # OWNER(主申请人), FAMILY(家庭成员副账号), PROPERTY_ADMIN(物业)
    community_id = Column(Integer, ForeignKey("communities.id"), nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)

    community = relationship("Community", back_populates="users")
    applications = relationship("QueueApplication", back_populates="user")
    contracts = relationship("RentalContract", back_populates="user")
    notifications = relationship("Notification", back_populates="user")

class ParkingSpace(Base):
    """车位资产台账表"""
    __tablename__ = "parking_spaces"

    id = Column(Integer, primary_key=True, index=True)
    community_id = Column(Integer, ForeignKey("communities.id"), nullable=False)
    space_no = Column(String(30), unique=True, nullable=False) # A-001 ~ A-050
    area_type = Column(String(20), default="GROUND") # GROUND(地面), UNDERGROUND(地下)
    status = Column(String(30), default="VACANT") 
    # VACANT(空闲), ALLOCATED(已分配待确认48h), OFFLINE_PENDING(线下办理中), RENTED(已租), MAINTENANCE(维修停用)
    current_user_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    community = relationship("Community", back_populates="spaces")
    current_user = relationship("User")
    contract = relationship("RentalContract", back_populates="space", uselist=False)

class QueueApplication(Base):
    """车位排队申请单表"""
    __tablename__ = "queue_applications"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    community_id = Column(Integer, ForeignKey("communities.id"), nullable=False)
    property_no = Column(String(100), nullable=False) # 12号楼302室
    plate_no = Column(String(20), nullable=False) # 沪A·12345
    is_spouse_car = Column(Boolean, default=False)
    marriage_cert_no = Column(String(50), nullable=True) # 结婚证编号
    priority_level = Column(Integer, default=1) # 1: 家庭一车, 2: 家庭二车
    current_rank = Column(Integer, default=0) # 实时排位 (1, 2, 3...)
    status = Column(String(30), default="WAITING") 
    # WAITING(排队中), ALLOCATED(中签待确认48h), OFFLINE_PENDING(线下办理中), COMPLETED(租约生效), CANCELLED(主动撤销), TIMEOUT_CANCELLED(超时放弃)
    allocated_space_id = Column(Integer, ForeignKey("parking_spaces.id"), nullable=True)
    allocated_time = Column(DateTime, nullable=True) # 48h 倒计时起始点
    apply_time = Column(DateTime, default=datetime.utcnow, index=True) # FIFO 排序键

    user = relationship("User", back_populates="applications")
    allocated_space = relationship("ParkingSpace")

class RentalContract(Base):
    """车位租约履约表"""
    __tablename__ = "rental_contracts"

    id = Column(Integer, primary_key=True, index=True)
    contract_no = Column(String(64), unique=True, nullable=False) # AGY-CT-20260907-XXXX
    space_id = Column(Integer, ForeignKey("parking_spaces.id"), nullable=False)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    plate_no = Column(String(20), nullable=False) # 关联车牌号
    start_date = Column(Date, nullable=False)
    end_date = Column(Date, nullable=False) # 默认1年租期
    status = Column(String(30), default="ACTIVE") 
    # ACTIVE(生效中), EXPIRING(临期<=30天), OVERDUE(逾期宽限期7天), TERMINATED(已退租)
    created_at = Column(DateTime, default=datetime.utcnow)

    space = relationship("ParkingSpace", back_populates="contract")
    user = relationship("User", back_populates="contracts")

class ChainEvidence(Base):
    """虚拟区块链存证流水表"""
    __tablename__ = "chain_evidence"

    id = Column(Integer, primary_key=True, index=True)
    evidence_id = Column(String(64), unique=True, index=True, nullable=False) # AGY-EVID-xxx
    community_id = Column(Integer, nullable=False)
    user_id = Column(Integer, nullable=True)
    event_type = Column(String(32), nullable=False) # APPLY, CANCEL, ALLOCATE, OFFLINE_SIGN, RENEW, TERMINATE, INIT
    block_height = Column(Integer, nullable=False) # 虚拟自增区块高度
    tx_hash = Column(String(66), nullable=False) # 0x + 64位哈希
    prev_hash = Column(String(66), nullable=False) # 形成链式结构
    event_summary = Column(String(255), nullable=False) # 中文摘要
    raw_payload_json = Column(Text, nullable=False) # 业务快照数据
    timestamp = Column(String(32), nullable=False) # 毫秒级时间戳
    created_at = Column(DateTime, default=datetime.utcnow)

class Notification(Base):
    """消息通知表"""
    __tablename__ = "notifications"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    title = Column(String(100), nullable=False)
    content = Column(Text, nullable=False)
    type = Column(String(30), nullable=False) # RANK_CHANGE, ALLOCATION, EXPIRATION, OVERDUE, SYSTEM
    is_read = Column(Boolean, default=False)
    action_type = Column(String(30), nullable=True) # GO_CONFIRM, GO_QUEUE
    created_at = Column(DateTime, default=datetime.utcnow)

    user = relationship("User", back_populates="notifications")
