import os
from sqlalchemy import create_engine, event
from sqlalchemy.orm import declarative_base, sessionmaker

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DEFAULT_DB_PATH = os.path.join(BASE_DIR, "parking.db")
DATABASE_URL = os.getenv("DATABASE_URL", f"sqlite:///{DEFAULT_DB_PATH}")

# 若为 SQLite 且目录不存在，自动创建目录
if DATABASE_URL.startswith("sqlite:///"):
    db_file_path = DATABASE_URL.replace("sqlite:///", "")
    parent_dir = os.path.dirname(db_file_path)
    if parent_dir and not os.path.exists(parent_dir):
        os.makedirs(parent_dir, exist_ok=True)

engine = create_engine(
    DATABASE_URL, connect_args={"check_same_thread": False}
)

@event.listens_for(engine, "connect")
def _enable_sqlite_foreign_keys(dbapi_conn, connection_record):
    # SQLite 默认不强制外键约束，显式开启保证引用完整性
    cursor = dbapi_conn.cursor()
    cursor.execute("PRAGMA foreign_keys=ON")
    cursor.close()

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

def run_migrations():
    """轻量自动迁移: 为已有 SQLite 表动态补齐缺失的列"""
    if not DATABASE_URL.startswith("sqlite"):
        return
    with engine.connect() as conn:
        # 1. communities 表
        try:
            res = conn.exec_driver_sql("PRAGMA table_info(communities)")
            cols = [row[1] for row in res.fetchall()]
            if "audit_mode" not in cols:
                conn.exec_driver_sql("ALTER TABLE communities ADD COLUMN audit_mode VARCHAR(30) DEFAULT 'THREE_CERT_AUTO'")
        except Exception as e:
            print(f"[Migration Warning] communities check: {e}")

        # 2. queue_applications 表
        try:
            res = conn.exec_driver_sql("PRAGMA table_info(queue_applications)")
            cols = [row[1] for row in res.fetchall()]
            if "audit_mode" not in cols:
                conn.exec_driver_sql("ALTER TABLE queue_applications ADD COLUMN audit_mode VARCHAR(30) DEFAULT 'THREE_CERT_AUTO'")
            if "reject_reason" not in cols:
                conn.exec_driver_sql("ALTER TABLE queue_applications ADD COLUMN reject_reason TEXT")
            if "attachments_json" not in cols:
                conn.exec_driver_sql("ALTER TABLE queue_applications ADD COLUMN attachments_json TEXT")
            if "audit_time" not in cols:
                conn.exec_driver_sql("ALTER TABLE queue_applications ADD COLUMN audit_time DATETIME")
            if "auditor_id" not in cols:
                conn.exec_driver_sql("ALTER TABLE queue_applications ADD COLUMN auditor_id INTEGER")
        except Exception as e:
            print(f"[Migration Warning] queue_applications check: {e}")

        # 3. users 表
        try:
            res = conn.exec_driver_sql("PRAGMA table_info(users)")
            cols = [row[1] for row in res.fetchall()]
            if "audit_mode" not in cols:
                conn.exec_driver_sql("ALTER TABLE users ADD COLUMN audit_mode VARCHAR(30) DEFAULT NULL")
        except Exception as e:
            print(f"[Migration Warning] users check: {e}")

        conn.commit()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

