from typing import Optional, List, Any
from datetime import datetime, date
from pydantic import BaseModel, Field

class ResponseModel(BaseModel):
    code: int = 200
    message: str = "success"
    data: Optional[Any] = None

class SendSmsRequest(BaseModel):
    phone: str

class LoginRequest(BaseModel):
    identifier: str # 手机号或身份证号
    password: Optional[str] = None
    verify_code: Optional[str] = None
    login_type: str = "PASSWORD" # PASSWORD 或 SMS

class RegisterRequest(BaseModel):
    district: str
    subdistrict: str
    community_name: str
    real_name: str
    id_card: str
    phone: str
    password: str

class ApplyParkingRequest(BaseModel):
    property_no: str
    plate_no: str
    is_spouse_car: bool = False
    property_cert_no: Optional[str] = None # 不动产权证号 (OCR 提取)
    marriage_cert_no: Optional[str] = None

class CancelApplyRequest(BaseModel):
    reason: Optional[str] = "个人原因自愿撤销"

class MaintenanceRequest(BaseModel):
    space_id: int
    action: str # SET_MAINTENANCE 或 RESTORE
    reason: Optional[str] = "地砖检修与设施保养"
    estimated_days: Optional[int] = 3

class ConfirmDeliveryRequest(BaseModel):
    space_id: int
    user_id: int
    months: Optional[int] = 6 # 签约周期，默认半年(6个月)，可选1/3/6/12等


class ForceTerminateRequest(BaseModel):
    space_id: int
    reason: str
    attachment_url: Optional[str] = "proof_document_mock.jpg"

class RenewRequest(BaseModel):
    months: Optional[int] = 6 # 默认半年(6个月)，支持1、3、6、12等周期

