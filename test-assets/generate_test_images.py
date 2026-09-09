#!/usr/bin/env python3
"""生成演示用 mock 证件图片 (全部为虚构样证数据，仅供本系统上传测试)"""
import os
from PIL import Image, ImageDraw, ImageFont

OUT_DIR = os.path.dirname(os.path.abspath(__file__))
FONT_HEI = "/System/Library/Fonts/STHeiti Medium.ttc"
FONT_SONG = "/System/Library/Fonts/Hiragino Sans GB.ttc"

def font(size, bold=False):
    return ImageFont.truetype(FONT_HEI if bold else FONT_SONG, size)

def save(img, name):
    path = os.path.join(OUT_DIR, name)
    img.save(path)
    print(f"  ✓ {name} ({os.path.getsize(path)//1024}KB)")

# ============ 1. 居民身份证 (人像面) 样证 ============
def gen_id_card():
    W, H = 960, 600
    img = Image.new("RGB", (W, H), "#eef4ee")
    d = ImageDraw.Draw(img)
    # 底纹与边框
    d.rectangle([8, 8, W-8, H-8], outline="#3a7d44", width=4)
    d.rectangle([16, 16, W-16, H-16], outline="#7db387", width=2)
    d.text((W//2, 44), "中华人民共和国居民身份证", font=font(34, True), fill="#2d5f35", anchor="mm")
    d.line([60, 78, W-60, 78], fill="#7db387", width=2)

    fields = [
        ("姓    名", "张欣欣"),
        ("性    别", "女            民族  汉"),
        ("出    生", "1992 年 01 月 01 日"),
        ("住    址", "上海市徐汇区龙华街道龙华新村12号楼302室"),
    ]
    y = 120
    for label, value in fields:
        d.text((60, y), label, font=font(26), fill="#555555")
        d.text((220, y), value, font=font(28, True), fill="#1a1a1a")
        y += 64
    d.text((60, y+10), "公民身份号码", font=font(26), fill="#555555")
    d.text((260, y+6), "310104199201011111", font=font(34, True), fill="#1a1a1a")

    # 照片占位框
    d.rectangle([W-260, 110, W-70, 400], fill="#cfd8dc", outline="#78909c", width=3)
    d.ellipse([W-215, 150, W-115, 250], fill="#90a4ae")
    d.rectangle([W-225, 260, W-105, 390], fill="#90a4ae")
    d.text((W-165, 430), "照片", font=font(22), fill="#607d8b", anchor="mm")
    d.text((W//2, H-50), "样证 · 仅供系统测试使用", font=font(20), fill="#9e9e9e", anchor="mm")
    save(img, "身份证_样证_张欣欣.png")

# ============ 2. 不动产权证书 (房产证) 样证 ============
def gen_property_cert():
    W, H = 960, 660
    img = Image.new("RGB", (W, H), "#fdf6f0")
    d = ImageDraw.Draw(img)
    d.rectangle([8, 8, W-8, H-8], outline="#b91c1c", width=5)
    d.rectangle([18, 18, W-18, H-18], outline="#e8a33d", width=2)
    d.text((W//2, 52), "不动产权证书", font=font(40, True), fill="#b91c1c", anchor="mm")
    d.text((W//2, 92), "中华人民共和国自然资源部监制", font=font(18), fill="#8a6d3b", anchor="mm")
    d.line([60, 112, W-60, 112], fill="#e8a33d", width=2)

    d.text((60, 140), "沪 (2022) 徐字 不动产权第 019823 号", font=font(28, True), fill="#7f1d1d")
    fields = [
        ("权  利  人", "张欣欣"),
        ("坐        落", "徐汇区龙华街道龙华新村12号楼302室"),
        ("权利类型", "国有建设用地使用权/房屋所有权"),
        ("用        途", "城镇住宅用地/住宅"),
        ("面        积", "建筑面积 68.50 平方米"),
        ("使用期限", "2002年03月15日 起 2072年03月14日 止"),
    ]
    y = 200
    for label, value in fields:
        d.text((60, y), label, font=font(24), fill="#6b5d4f")
        d.text((250, y), value, font=font(26, True), fill="#1a1a1a")
        y += 58
    # 红色公章占位
    d.ellipse([W-240, H-220, W-90, H-70], outline="#dc2626", width=5)
    d.text((W-165, H-145), "登记专用章", font=font(18, True), fill="#dc2626", anchor="mm")
    d.text((60, H-70), "登记机构: 上海市徐汇区不动产登记事务中心      2022年05月20日", font=font(20), fill="#6b5d4f")
    d.text((W//2, H-36), "样证 · 仅供系统测试使用", font=font(18), fill="#b0a08e", anchor="mm")
    save(img, "不动产权证_样证_张欣欣.png")

# ============ 3. 营业执照 (物业) 样证 ============
def gen_business_license():
    W, H = 1100, 780
    img = Image.new("RGB", (W, H), "#fffdf5")
    d = ImageDraw.Draw(img)
    d.rectangle([10, 10, W-10, H-10], outline="#c9a227", width=6)
    d.rectangle([22, 22, W-22, H-22], outline="#c9a227", width=2)
    d.text((W//2, 70), "营 业 执 照", font=font(52, True), fill="#8a1c1c", anchor="mm")
    d.text((W//2, 118), "(副 本)", font=font(22), fill="#8a1c1c", anchor="mm")
    d.line([80, 145, W-80, 145], fill="#c9a227", width=2)

    d.text((80, 170), "统一社会信用代码  91310104MA1FL3800X", font=font(26, True), fill="#1a1a1a")
    fields = [
        ("名        称", "上海安康苑物业管理有限公司"),
        ("类        型", "有限责任公司 (自然人投资或控股)"),
        ("法定代表人", "王建国"),
        ("注册资本", "人民币 500.0000 万元整"),
        ("成立日期", "2016 年 04 月 18 日"),
        ("住        所", "徐汇区龙华街道龙华新村物业楼 101 室"),
        ("经营范围", "物业管理; 停车场库经营; 房屋维修养护"),
    ]
    y = 230
    for label, value in fields:
        d.text((80, y), label, font=font(26), fill="#4a4a4a")
        d.text((290, y), value, font=font(28, True), fill="#1a1a1a")
        y += 64
    # 公章占位 (右下，避开文字)
    d.ellipse([W-235, H-235, W-75, H-75], outline="#dc2626", width=6)
    d.text((W-155, H-155), "登记机关", font=font(20, True), fill="#dc2626", anchor="mm")
    d.text((W-155, H-52), "2023年01月10日", font=font(18), fill="#4a4a4a", anchor="mm")
    d.text((80, H-85), "登记机关: 上海市徐汇区市场监督管理局", font=font(22), fill="#4a4a4a")
    d.text((W//2, H-40), "样证 · 仅供系统测试使用", font=font(18), fill="#b8ab8a", anchor="mm")
    save(img, "营业执照_样证_物业公司.png")

if __name__ == "__main__":
    print("生成 mock 测试证件图片:")
    gen_id_card()
    gen_property_cert()
    gen_business_license()
    print("完成 →", OUT_DIR)
