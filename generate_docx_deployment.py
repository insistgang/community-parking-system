import os
import sys
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement
from docx.oxml.ns import qn, nsdecls
from docx.oxml import parse_xml

sys.stdout.reconfigure(encoding='utf-8')

OUTPUT_DOCX_NAME = "慧停管家_老旧小区停车位管理系统_部署说明文档.docx"
OUTPUT_DIR = r"E:\parking-management-java"
OUTPUT_PATH = os.path.join(OUTPUT_DIR, OUTPUT_DOCX_NAME)

# 样式工具函数
def set_cell_background(cell, fill_hex):
    shading_elm = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    cell._tc.get_or_add_tcPr().append(shading_elm)

def set_cell_margins(cell, top=120, bottom=120, left=180, right=180):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement('w:tcMar')
    for m, val in [('w:top', top), ('w:bottom', bottom), ('w:left', left), ('w:right', right)]:
        node = OxmlElement(m)
        node.set(qn('w:w'), str(val))
        node.set(qn('w:type'), 'dxa')
        tcMar.append(node)
    tcPr.append(tcMar)

def set_table_borders(table, color="CBD5E1", sz="4", val="single"):
    tblPr = table._tbl.tblPr
    borders = parse_xml(
        f'<w:tblBorders {nsdecls("w")}>'
        f'  <w:top w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'  <w:bottom w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'  <w:left w:val="none"/>'
        f'  <w:right w:val="none"/>'
        f'  <w:insideH w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'  <w:insideV w:val="none"/>'
        f'</w:tblBorders>'
    )
    tblPr.append(borders)

def set_run_font(run, font_name="微软雅黑", size_pt=10.5, bold=False, color_rgb=(51, 65, 85)):
    run.font.name = font_name
    run._element.rPr.rFonts.set(qn('w:eastAsia'), font_name)
    run.font.size = Pt(size_pt)
    run.font.bold = bold
    run.font.color.rgb = RGBColor(*color_rgb)

def add_p(doc, text="", font_name="微软雅黑", size_pt=10.5, bold=False, color_rgb=(51, 65, 85),
          align=WD_ALIGN_PARAGRAPH.LEFT, space_before=2, space_after=4, line_spacing=1.25):
    p = doc.add_paragraph()
    p.alignment = align
    p.paragraph_format.space_before = Pt(space_before)
    p.paragraph_format.space_after = Pt(space_after)
    p.paragraph_format.line_spacing = line_spacing
    if text:
        run = p.add_run(text)
        set_run_font(run, font_name=font_name, size_pt=size_pt, bold=bold, color_rgb=color_rgb)
    return p

def add_heading_1(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(14)
    p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.keep_with_next = True
    run = p.add_run(text)
    set_run_font(run, font_name="微软雅黑", size_pt=15, bold=True, color_rgb=(15, 23, 42))
    return p

def add_heading_2(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(10)
    p.paragraph_format.space_after = Pt(4)
    p.paragraph_format.keep_with_next = True
    run = p.add_run(text)
    set_run_font(run, font_name="微软雅黑", size_pt=12.5, bold=True, color_rgb=(30, 41, 59))
    return p

def add_code_block(doc, code_text):
    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl.autofit = False
    tbl.columns[0].width = Inches(6.5)
    cell = tbl.cell(0, 0)
    set_cell_background(cell, "0F172A") # 深黑控制台背景
    set_cell_margins(cell, top=140, bottom=140, left=200, right=200)

    # 去除外边框
    tblPr = tbl._tbl.tblPr
    borders = parse_xml(
        f'<w:tblBorders {nsdecls("w")}>'
        f'  <w:top w:val="none"/>'
        f'  <w:bottom w:val="none"/>'
        f'  <w:left w:val="none"/>'
        f'  <w:right w:val="none"/>'
        f'  <w:insideH w:val="none"/>'
        f'  <w:insideV w:val="none"/>'
        f'</w:tblBorders>'
    )
    tblPr.append(borders)

    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.15
    run = p.add_run(code_text)
    set_run_font(run, font_name="Consolas", size_pt=9.0, bold=False, color_rgb=(226, 232, 240))
    doc.add_paragraph().paragraph_format.space_after = Pt(4)

def add_tip_box(doc, title, text, tip_type="INFO"):
    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl.autofit = False
    tbl.columns[0].width = Inches(6.5)
    cell = tbl.cell(0, 0)

    bg_color = "F0FDF4" if tip_type == "SUCCESS" else ("FEF2F2" if tip_type == "WARN" else "F8FAFC")
    border_color = "22C55E" if tip_type == "SUCCESS" else ("EF4444" if tip_type == "WARN" else "3B82F6")

    set_cell_background(cell, bg_color)
    set_cell_margins(cell, top=120, bottom=120, left=180, right=180)

    tblPr = tbl._tbl.tblPr
    borders = parse_xml(
        f'<w:tblBorders {nsdecls("w")}>'
        f'  <w:top w:val="none"/>'
        f'  <w:bottom w:val="none"/>'
        f'  <w:left w:val="single" w:sz="18" w:space="0" w:color="{border_color}"/>'
        f'  <w:right w:val="none"/>'
        f'  <w:insideH w:val="none"/>'
        f'  <w:insideV w:val="none"/>'
        f'</w:tblBorders>'
    )
    tblPr.append(borders)

    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(2)
    run_t = p.add_run(title)
    title_rgb = (21, 128, 61) if tip_type == "SUCCESS" else ((185, 28, 28) if tip_type == "WARN" else (29, 78, 216))
    set_run_font(run_t, font_name="微软雅黑", size_pt=10.0, bold=True, color_rgb=title_rgb)

    p2 = cell.add_paragraph()
    p2.paragraph_format.space_before = Pt(0)
    p2.paragraph_format.space_after = Pt(0)
    p2.paragraph_format.line_spacing = 1.2
    run_c = p2.add_run(text)
    set_run_font(run_c, font_name="微软雅黑", size_pt=9.5, bold=False, color_rgb=(71, 85, 105))
    doc.add_paragraph().paragraph_format.space_after = Pt(4)

def create_custom_table(doc, col_widths, headers, data):
    table = doc.add_table(rows=len(data) + 1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(table, color="CBD5E1", sz="4")

    # 表头
    for i, header_text in enumerate(headers):
        cell = table.cell(0, i)
        cell.width = col_widths[i]
        set_cell_background(cell, "F1F5F9")
        set_cell_margins(cell, top=100, bottom=100, left=140, right=140)
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(0)
        run = p.add_run(header_text)
        set_run_font(run, font_name="微软雅黑", size_pt=9.5, bold=True, color_rgb=(30, 41, 59))

    # 数据行
    for r_idx, row_data in enumerate(data):
        row = table.rows[r_idx + 1]
        bg = "FFFFFF" if r_idx % 2 == 0 else "F8FAFC"
        for c_idx, val in enumerate(row_data):
            cell = row.cells[c_idx]
            cell.width = col_widths[c_idx]
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=90, bottom=90, left=140, right=140)
            p = cell.paragraphs[0]
            p.paragraph_format.space_before = Pt(0)
            p.paragraph_format.space_after = Pt(0)
            p.paragraph_format.line_spacing = 1.15
            if c_idx == 0 or len(val) <= 6:
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            else:
                p.alignment = WD_ALIGN_PARAGRAPH.LEFT
            run = p.add_run(val)
            set_run_font(run, font_name="微软雅黑", size_pt=9.0, bold=False, color_rgb=(51, 65, 85))

    return table

def build_deployment_docx():
    doc = docx.Document()

    # 设置页边距
    for section in doc.sections:
        section.top_margin = Inches(1.0)
        section.bottom_margin = Inches(1.0)
        section.left_margin = Inches(1.0)
        section.right_margin = Inches(1.0)

    # ==================== 封面与标题 ====================
    add_p(doc, "慧停管家 · 老旧小区停车位管理系统", font_name="微软雅黑", size_pt=22, bold=True,
          color_rgb=(15, 23, 42), align=WD_ALIGN_PARAGRAPH.CENTER, space_before=10, space_after=6)
    add_p(doc, "系统生产部署与运维操作说明文档 (v1.0)", font_name="微软雅黑", size_pt=13, bold=False,
          color_rgb=(100, 116, 139), align=WD_ALIGN_PARAGRAPH.CENTER, space_before=0, space_after=18)

    # 简要元信息卡片
    meta_widths = [Inches(1.8), Inches(4.7)]
    meta_headers = ["配置属性", "详细信息"]
    meta_data = [
        ["目标部署路径", "/DATA/QC_yangjichang/liugang_xiaoliu"],
        ["生产访问端口", "http://192.168.0.147:39080/"],
        ["技术架构模式", "Docker Compose 双容器 (Java 17 Spring Boot + Vue 3 Nginx + SQLite)"],
        ["当前运行状态", "已全量容器化上线，健康检查 HTTP 200 OK，稳定运行中"]
    ]
    create_custom_table(doc, meta_widths, meta_headers, meta_data)
    doc.add_paragraph().paragraph_format.space_after = Pt(12)

    # ==================== 第一章：系统架构与技术选型 ====================
    add_heading_1(doc, "第一章 系统架构与部署方案概述")
    add_p(doc, "慧停管家系统采用前后端解耦的轻量化微服务化容器架构，为老旧小区停车位资源紧缺现状提供“阳光轮候、规范申报、公平公示、存证溯源、安全审计”的全流程数字化管理支撑：")

    add_heading_2(doc, "1.1 前后端技术栈组成")
    w1 = [Inches(1.2), Inches(2.2), Inches(3.1)]
    h1 = ["组件分层", "技术选型", "功能定位与说明"]
    d1 = [
        ["前端应用", "Vue 3 + Vite + Tailwind CSS + Pinia", "提供业主端 H5、物业管理端 PC、运维管理员后台，轻量自适应交互。"],
        ["前端网关", "Nginx (Alpine Linux)", "对外监听 80 端口，负责前端静态资源分发、SPA 路由重定向与 /api/ 反向代理。"],
        ["后端核心", "Java 17 + Spring Boot 3.4.4", "提供业务规则引擎、排队算法轮候、区块链存证生成、安全审计日志与外部接口中台。"],
        ["持久化数据库", "SQLite 3 (Embedded File DB)", "轻量单文件嵌入式数据库，零独立服务维护依赖，通过 Docker 数据卷持久化至宿主机。"]
    ]
    create_custom_table(doc, w1, h1, d1)
    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    # ==================== 第二章：部署环境要求 ====================
    add_heading_1(doc, "第二章 部署环境要求")

    add_heading_2(doc, "2.1 硬件配置指标")
    w2 = [Inches(1.2), Inches(1.4), Inches(1.6), Inches(2.3)]
    h2 = ["硬件项", "最低配置", "推荐配置", "资源开销说明"]
    d2 = [
        ["CPU", "2 核", "4 核及以上", "保证容器并发调度与业务排队轮候计算响应。"],
        ["内存", "4 GB", "8 GB 及以上", "Spring Boot 运行时占用约 500M~1G，Nginx 开销极低。"],
        ["磁盘空间", "可用 20 GB", "可用 50 GB 以上", "用于存放 Docker 基础镜像、应用依赖层与历史持久化数据。"]
    ]
    create_custom_table(doc, w2, h2, d2)
    doc.add_paragraph().paragraph_format.space_after = Pt(6)

    add_heading_2(doc, "2.2 操作系统与基础软件")
    add_p(doc, "• 操作系统：Linux 64 位（推荐 Ubuntu 20.04/22.04 LTS 或 CentOS 7.9+ / Debian 11+）。")
    add_p(doc, "• 容器环境：Docker Engine 20.10.x 及以上（推荐 24.0+），Docker Compose v2.x（或 docker-compose 1.29+）。")
    add_p(doc, "• 基础镜像要求：Java 镜像使用 docker.1panel.live/library/openjdk:17-jdk-slim；前端镜像使用 nginx:alpine。")

    add_heading_2(doc, "2.3 宿主机端口规划与防火墙规则")
    w3 = [Inches(1.1), Inches(0.8), Inches(2.2), Inches(2.4)]
    h3 = ["端口号", "协议", "映射与访问关系", "业务用途说明"]
    d3 = [
        ["39080", "TCP", "宿主机:39080 -> 前端容器:80", "【核心主端口】系统对外 Web 访问与所有 REST API 通信入口。"],
        ["39000", "TCP", "宿主机:39000 -> 后端容器:8080", "后端 Spring Boot 调试暴露端口（限内网或排查使用）。"],
        ["1024 / 22", "TCP", "宿主机 SSH 端口", "用于运维工程师远程登录管理服务器。"]
    ]
    create_custom_table(doc, w3, h3, d3)
    doc.add_paragraph().paragraph_format.space_after = Pt(6)
    add_tip_box(doc, "安全组注意提示", "如果服务器处于阿里云、腾讯云等公有云或企业专属内网，必须在网络安全组中将 TCP 39080 端口加入入方向放行列表，否则外部用户无法访问系统前端界面。", "WARN")

    # ==================== 第三章：部署位置与目录规范 ====================
    add_heading_1(doc, "第三章 部署位置与目录结构规范")
    add_p(doc, "系统在生产服务器上的部署绝对目录统一规范为：")
    add_code_block(doc, "/DATA/QC_yangjichang/liugang_xiaoliu")

    add_heading_2(doc, "3.1 服务器目录规范与文件清单")
    w4 = [Inches(2.2), Inches(1.3), Inches(3.0)]
    h4 = ["相对路径 / 文件名", "归属属性", "作用说明与管理规范"]
    d4 = [
        ["docker-compose.yml", "核心编排文件", "定义双容器集群配置、网络桥接以及目录挂载映射。"],
        [".env", "环境配置文件", "定义 PORT=39080 与 BACKEND_PORT=39000 等关键端口参数。"],
        ["DEPLOYMENT.md", "说明文档", "Markdown 格式的工程部署指南。"],
        ["backend/pom.xml", "后端 Maven 文件", "定义 Spring Boot 3 与各业务依赖坐标。"],
        ["backend/Dockerfile", "后端构建定义", "基于 OpenJDK 17 Slim 镜像，封装 Spring Boot Jar 包。"],
        ["backend/src/", "后端纯源码目录", "Java 控制层、服务层、实体层与 application.yml 配置。"],
        ["backend/target/*.jar", "后端运行产物", "由本地或 CI 构建生成的 parking-java-1.0.0.jar。"],
        ["frontend/Dockerfile", "前端构建定义", "基于 Nginx Alpine 镜像，加载 dist 静态资源及配置。"],
        ["frontend/nginx.conf", "网关反代配置", "支持 SPA 路由回退，并将 /api/ 请求反代至 Java 容器。"],
        ["data/parking.db", "核心数据库", "【高危核心】SQLite 数据文件，必须持久化，严禁随意删除！"]
    ]
    create_custom_table(doc, w4, h4, d4)
    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    # ==================== 第四章：详细部署安装步骤 ====================
    add_heading_1(doc, "第四章 详细部署与安装步骤")

    add_heading_2(doc, "步骤一：本地代码编译与构建（推荐）")
    add_p(doc, "为确保服务器资源开销最小化并实现秒级部署，建议在本地开发机先打包好 Jar 与前端静态文件：")
    add_code_block(doc, "# 1. 构建后端 Java 可执行 Jar 包\ncd backend\nmvn clean package -DskipTests\n\n# 2. 构建前端生产静态资产包\ncd ../frontend\nnpm install\nnpm run build")

    add_heading_2(doc, "步骤二：产物同步上传至服务器")
    add_p(doc, "通过 SFTP / SCP 将工程文件上传至目标服务器：")
    add_code_block(doc, "scp -P 1024 -r backend frontend data docker-compose.yml .env qc_yangjichang@192.168.0.147:/DATA/QC_yangjichang/liugang_xiaoliu/")

    add_heading_2(doc, "步骤三：数据目录读写权限赋予（关键操作）")
    add_p(doc, "SQLite 需要在运行时由容器进程创建 journal 或-wal 临时锁文件，必须赋予宿主机 data 目录完全读写权限：")
    add_code_block(doc, "ssh -p 1024 qc_yangjichang@192.168.0.147\ncd /DATA/QC_yangjichang/liugang_xiaoliu\nchmod -R 777 data")

    add_heading_2(doc, "步骤四：容器构建与后台启动")
    add_p(doc, "执行 Docker Compose 启动指令，一键拉起前后端双容器：")
    add_code_block(doc, "# 构建容器镜像并转入后台常驻运行\ndocker compose up -d --build")

    add_heading_2(doc, "步骤五：部署健康验证")
    add_p(doc, "通过命令行或浏览器核验系统各服务运行状态：")
    add_code_block(doc, "# 1. 检查容器状态\ndocker compose ps\n\n# 2. 检查后端健康状态\ncurl -s http://127.0.0.1:39080/api/health\n# 正常返回: {\"code\":200,\"message\":\"系统运行正常\",\"data\":{\"status\":\"UP\"}}")
    add_tip_box(doc, "部署成功标志", "当控制台 curl 返回 {\"status\":\"UP\"} 且浏览器能正常加载前端登录页时，代表生产部署已完全成功！", "SUCCESS")

    # ==================== 第五章：部署与运维核心注意事项 ====================
    add_heading_1(doc, "第五章 部署与运维核心注意事项")

    add_heading_2(doc, "5.1 数据持久化与定期备份安全规范")
    add_p(doc, "1. 挂载持久化原理：docker-compose.yml 中配置了 ./data:/app/data，容器内部的数据库读写均实时映射在宿主机的 data/ 目录下。即便容器执行 down 卸载删除，业务数据也不会丢失。")
    add_p(doc, "2. 备份操作规范：在进行版本升级或重大操作前，请先备份数据文件：")
    add_code_block(doc, "cp data/parking.db data/parking.db.$(date +%Y%m%d_%H%M%S).bak")

    add_heading_2(doc, "5.2 SQLite 并发写与连接池配置")
    add_p(doc, "SQLite 属于文件级事务锁机制，天然具备单写多读特性。为防止高并发下发生“database is locked”异常，后端 application.yml 强制设置了 HikariCP 连接池 maximum-pool-size: 1，由连接池排队串行化写入。切勿随意调大最大连接数。")

    add_heading_2(doc, "5.3 Nginx 路由与 SPA 回退机制")
    add_p(doc, "由于系统采用 Vue Router HTML5 History 路由模式，nginx.conf 中必须配置 try_files $uri $uri/ /index.html;，否则在浏览器端直接刷新页面（例如访问 /admin/login 或 /property/dashboard）时将导致 Nginx 报 404 错误。")

    add_heading_2(doc, "5.4 端口冲突应急调整机制")
    add_p(doc, "若宿主机 39080 端口发生冲突，切勿修改 Dockerfile，只需修改 .env 文件中的 PORT=新端口，然后执行 docker compose up -d，即可平滑完成端口切换。")

    add_heading_2(doc, "5.5 系统出厂账号与权限清单")
    w5 = [Inches(1.2), Inches(1.8), Inches(1.2), Inches(2.3)]
    h5 = ["角色类型", "访问入口路径", "默认账号", "默认密码与权限说明"]
    d5 = [
        ["系统管理员", "#/admin/login", "admin", "密码 admin123，最高权限，可查看日志与下发测试。"],
        ["物业管理员", "#/property/login", "13900000001", "密码 123456，负责车位轮候、审核、线下缴费流转。"],
        ["社区业主", "#/login", "13800000001", "密码 123456，张欣欣，负责申请车位与查看公示存证。"]
    ]
    create_custom_table(doc, w5, h5, d5)
    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    # ==================== 第六章：常用运维排错速查表 ====================
    add_heading_1(doc, "第六章 常用运维命令速查表")
    w6 = [Inches(2.2), Inches(4.3)]
    h6 = ["运维排错需求", "执行命令"]
    d6 = [
        ["查看容器运行与端口状态", "docker compose ps"],
        ["实时滚动查看后端 Java 日志", "docker logs -f parking-backend --tail 100"],
        ["实时滚动查看前端 Nginx 访问日志", "docker logs -f parking-frontend --tail 100"],
        ["一键重启系统全部容器", "docker compose restart"],
        ["单独重启 Java 后端服务", "docker compose restart backend"],
        ["前端构建后热重载（免重启）", "docker cp frontend/dist/. parking-frontend:/usr/share/nginx/html/ && docker exec parking-frontend nginx -s reload"],
        ["停止并卸载容器服务", "docker compose down"]
    ]
    create_custom_table(doc, w6, h6, d6)

    # 保存文档
    doc.save(OUTPUT_PATH)
    print(f"Deployment docx successfully created at: {OUTPUT_PATH}")

if __name__ == '__main__':
    build_deployment_docx()
