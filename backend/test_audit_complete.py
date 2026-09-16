import sys
from fastapi.testclient import TestClient
from main import app

def test_full_audit_lifecycle():
    client = TestClient(app)
    
    # 0. 重置种子数据
    print(">>> 0. 重置种子数据...")
    res = client.post("/api/dev/reset-seed")
    assert res.status_code == 200, res.text
    print("Seed reset ok.")

    # 1. 物业管理员王建国登录 (13900000001 / 123456)
    prop_login = client.post("/api/auth/login", json={
        "identifier": "13900000001",
        "password": "123456",
        "login_type": "PASSWORD"
    })
    assert prop_login.status_code == 200, prop_login.text
    prop_user = prop_login.json()["data"]["user"]
    prop_token = prop_login.json()["data"]["token"]
    prop_headers = {
        "Authorization": f"Bearer {prop_token}",
        "X-User-Id": str(prop_user["id"])
    }
    print(f">>> 1. 物业管理员登录成功 (User ID: {prop_user['id']})！")

    # 2. 验证初始默认审核模式为 THREE_CERT_AUTO
    res = client.get("/api/parking/audit-mode", headers=prop_headers)
    assert res.status_code == 200
    assert res.json()["data"]["audit_mode"] == "THREE_CERT_AUTO", res.json()
    print(">>> 2. 默认审核方式为 THREE_CERT_AUTO 验证通过！")

    # 3. 物业将审核方式调整为 FLEXIBLE_HYBRID
    mode_change = client.post("/api/property/audit-config", json={
        "audit_mode": "FLEXIBLE_HYBRID"
    }, headers=prop_headers)
    assert mode_change.status_code == 200, mode_change.text
    assert mode_change.json()["data"]["audit_mode"] == "FLEXIBLE_HYBRID"
    print(">>> 3. 物业成功配置审核模式为 FLEXIBLE_HYBRID！")

    # 4. 验证存量申请不受影响 (存量单据保持原有审核模式不变)
    apps_res = client.get("/api/property/applications", headers=prop_headers)
    assert apps_res.status_code == 200
    apps = apps_res.json()["data"]
    for a in apps:
        assert a["audit_mode"] == "THREE_CERT_AUTO", f"存量单据模式应为原有模式: {a}"
    print(f">>> 4. 存量 {len(apps)} 笔申请单据保持原有审核模式不变，验证通过！")

    # 5. 业主张欣欣登录 (13800000001 / 123456)
    owner_login = client.post("/api/auth/login", json={
        "identifier": "13800000001",
        "password": "123456",
        "login_type": "PASSWORD"
    })
    assert owner_login.status_code == 200, owner_login.text
    owner_user = owner_login.json()["data"]["user"]
    owner_token = owner_login.json()["data"]["token"]
    owner_headers = {
        "Authorization": f"Bearer {owner_token}",
        "X-User-Id": str(owner_user["id"])
    }
    print(f">>> 5. 业主张欣欣登录成功 (User ID: {owner_user['id']})！")

    # 6. 张欣欣查看主页: 初始未申请状态 (NO_APPLY)
    home_res = client.get("/api/user/home", headers=owner_headers)
    assert home_res.status_code == 200
    assert home_res.json()["data"]["view_state"] == "NO_APPLY"
    print(">>> 6. 张欣欣主页初始状态为 NO_APPLY！")

    # 7. 张欣欣在 FLEXIBLE_HYBRID 模式下发起申请，上传多张必要证明附件
    sample_attachments = [
        {"title": "机动车行驶证正副页彩色扫描件", "file_name": "行驶证_沪A12345.jpg", "type": "LICENSE"},
        {"title": "不动产权证书坐落与权利人页", "file_name": "不动产权证_019823.jpg", "type": "PROPERTY"},
        {"title": "老旧小区车位合规停放承诺书", "file_name": "承诺书_张欣欣.pdf", "type": "COMMITMENT"}
    ]
    apply_res = client.post("/api/parking/apply", json={
        "property_no": "12号302室",
        "plate_no": "沪A·12345",
        "is_spouse_car": False,
        "car_relationship": "SELF",
        "attachments": sample_attachments
    }, headers=owner_headers)
    assert apply_res.status_code == 200, apply_res.text
    apply_data = apply_res.json()["data"]
    assert apply_data["status"] == "PENDING_REVIEW"
    assert apply_data["audit_mode"] == "FLEXIBLE_HYBRID"
    print(">>> 7. 业主在灵活配置模式下提交申请成功，状态直接流转至 PENDING_REVIEW！")

    # 8. 张欣欣再次查看主页: view_state 为 PENDING_REVIEW
    home_res = client.get("/api/user/home", headers=owner_headers)
    assert home_res.status_code == 200
    home_data = home_res.json()["data"]
    assert home_data["view_state"] == "PENDING_REVIEW"
    assert len(home_data["my_queue"]["attachments"]) == 3
    print(">>> 8. 业主主页实时展示 PENDING_REVIEW 与 3 份附件清单！")

    # 9. 物业管理员查看统计看板与待办单据
    stats_res = client.get("/api/property/stats", headers=prop_headers)
    assert stats_res.status_code == 200
    assert stats_res.json()["data"]["pending_audit_count"] == 1
    assert stats_res.json()["data"]["audit_mode"] == "FLEXIBLE_HYBRID"

    pending_apps = client.get("/api/property/applications?status=PENDING_REVIEW", headers=prop_headers)
    assert pending_apps.status_code == 200
    p_list = pending_apps.json()["data"]
    assert len(p_list) == 1
    target_app_id = p_list[0]["id"]
    print(">>> 9. 物业中台待办列表中精确包含该待审申请！")

    # 10. 物业驳回申请并填写理由
    reject_reason = "行驶证正副页反光模糊，无法清楚识别所有人姓名与车辆识别代号"
    reject_res = client.post("/api/property/audit-application", json={
        "app_id": target_app_id,
        "action": "REJECT",
        "reject_reason": reject_reason
    }, headers=prop_headers)
    assert reject_res.status_code == 200, reject_res.text
    print(">>> 10. 物业成功驳回申请并记录驳回理由！")

    # 11. 业主张欣欣查看主页: view_state 变为 REJECTED，并展示驳回理由与站内通知
    home_res = client.get("/api/user/home", headers=owner_headers)
    assert home_res.status_code == 200
    home_data = home_res.json()["data"]
    assert home_data["view_state"] == "REJECTED"
    assert home_data["my_queue"]["reject_reason"] == reject_reason

    notifs = client.get("/api/notifications", headers=owner_headers)
    assert notifs.status_code == 200
    assert any("驳回" in n["title"] for n in notifs.json()["data"])
    print(">>> 11. 业主端主页与消息通知均精准接收到驳回理由！")

    # 12. 业主张欣欣调整材料后重新发起申报
    sample_attachments_v2 = [
        {"title": "机动车行驶证正副页彩色扫描件(重拍高清版)", "file_name": "行驶证_沪A12345_高清.jpg", "type": "LICENSE"},
        {"title": "不动产权证书坐落与权利人页", "file_name": "不动产权证_019823.jpg", "type": "PROPERTY"},
        {"title": "老旧小区车位合规停放承诺书", "file_name": "承诺书_张欣欣.pdf", "type": "COMMITMENT"}
    ]
    reapply_res = client.post("/api/parking/apply", json={
        "property_no": "12号302室",
        "plate_no": "沪A·12345",
        "is_spouse_car": False,
        "car_relationship": "SELF",
        "attachments": sample_attachments_v2
    }, headers=owner_headers)
    assert reapply_res.status_code == 200, reapply_res.text
    new_app_id = reapply_res.json()["data"]["id"]
    print(">>> 12. 业主成功重新提交申请！")

    # 13. 物业人工审核通过
    approve_res = client.post("/api/property/audit-application", json={
        "app_id": new_app_id,
        "action": "APPROVE"
    }, headers=prop_headers)
    assert approve_res.status_code == 200, approve_res.text
    print(">>> 13. 物业人工审核通过！")

    # 14. 验证单据正式进入排位序列
    home_res = client.get("/api/user/home", headers=owner_headers)
    assert home_res.status_code == 200
    home_data = home_res.json()["data"]
    assert home_data["view_state"] == "WAITING"
    assert home_data["my_queue"]["current_rank"] > 0
    print(f">>> 14. 单据成功进入 WAITING 排位队列，排位位次: 第【{home_data['my_queue']['current_rank']}】位！")

    # 15. 演示重置恢复
    client.post("/api/dev/reset-seed")
    print(">>> 15. 演示种子数据重置恢复完成！全流程闭环 100% 验收通过！")

if __name__ == "__main__":
    test_full_audit_lifecycle()
