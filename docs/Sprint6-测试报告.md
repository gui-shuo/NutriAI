# Sprint 6 会员中心API测试报告

**测试时间**: 2025-12-03 14:26  
**测试人员**: AI Assistant  
**后端状态**: ✅ 运行中 (Flyway已启用)

---

## 🔴 测试结果：失败

### 错误信息
```
HTTP 500 Internal Server Error
Message: 获取会员信息失败: 用户不存在
```

### 根本原因
数据库中 `members` 表没有admin用户的会员记录。

---

## 🔧 立即修复方案

### 方案1：使用SQL快速修复（推荐）

在MySQL中执行以下SQL：

```sql
USE nutriai;

-- 1. 检查会员等级是否存在
SELECT * FROM member_levels;

-- 2. 如果没有数据，插入会员等级
INSERT IGNORE INTO member_levels (level_code, level_name, level_order, growth_required, benefits, icon_url, color) VALUES
('ROOKIE', '新手会员', 1, 0, 
 '{"features": ["基础营养记录", "AI咨询(3次/天)", "基础数据分析"], "maxAiQueries": 3, "maxFoodRecords": 10}',
 '/icons/level-rookie.png', '#95a5a6'),
('BRONZE', '青铜会员', 2, 100,
 '{"features": ["营养记录无限", "AI咨询(10次/天)", "高级数据分析", "自定义目标"], "maxAiQueries": 10, "maxFoodRecords": -1}',
 '/icons/level-bronze.png', '#cd7f32'),
('SILVER', '白银会员', 3, 500,
 '{"features": ["所有青铜权益", "AI咨询(30次/天)", "专属营养报告", "优先客服"], "maxAiQueries": 30, "maxFoodRecords": -1}',
 '/icons/level-silver.png', '#c0c0c0'),
('GOLD', '黄金会员', 4, 2000,
 '{"features": ["所有白银权益", "AI咨询无限", "个性化食谱", "健康顾问", "数据导出"], "maxAiQueries": -1, "maxFoodRecords": -1}',
 '/icons/level-gold.png', '#ffd700'),
('PLATINUM', '铂金会员', 5, 5000,
 '{"features": ["所有黄金权益", "专属营养师", "线下活动", "合作商家折扣", "终身成长值加成"], "maxAiQueries": -1, "maxFoodRecords": -1, "growthBonus": 1.5}',
 '/icons/level-platinum.png', '#e5e4e2');

-- 3. 为admin用户创建会员记录
INSERT INTO members (user_id, level_id, invitation_code, activated_at)
SELECT 
    u.id,
    (SELECT id FROM member_levels WHERE level_code = 'ROOKIE' LIMIT 1),
    CONCAT('INV', LPAD(u.id, 6, '0'), SUBSTRING(MD5(CONCAT(u.id, NOW())), 1, 6)),
    NOW()
FROM users u
WHERE u.username = 'admin'
AND NOT EXISTS (SELECT 1 FROM members m WHERE m.user_id = u.id);

-- 4. 验证
SELECT m.*, u.username, ml.level_name 
FROM members m
JOIN users u ON m.user_id = u.id
JOIN member_levels ml ON m.level_id = ml.id
WHERE u.username = 'admin';
```

### 方案2：使用命令行

```bash
cd d:\ProgrammingLanguage\Java\Projects\ai-based-healthy-diet
mysql -u root -p nutriai < init-members.sql
```

---

## ✅ 修复后测试命令

执行SQL后，运行以下PowerShell命令测试：

```powershell
# 完整测试
$body = @{username='admin';password='Admin123456'} | ConvertTo-Json
$r = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body $body
$h = @{Authorization="Bearer $($r.data.accessToken)"}

Write-Host "`n=== 1. 获取会员信息 ===" -ForegroundColor Cyan
$info = Invoke-RestMethod -Uri 'http://localhost:8080/api/member/info' -Headers $h
$info.data | Format-List

Write-Host "`n=== 2. 生成邀请链接 ===" -ForegroundColor Cyan
$invite = Invoke-RestMethod -Uri 'http://localhost:8080/api/member/invitation/generate' -Headers $h
$invite.data | Format-List

Write-Host "`n=== 3. 获取成长值记录 ===" -ForegroundColor Cyan
$growth = Invoke-RestMethod -Uri 'http://localhost:8080/api/member/growth-records?page=0&size=5' -Headers $h
Write-Host "总记录数: $($growth.data.totalElements)"

Write-Host "`n=== 4. 获取邀请记录 ===" -ForegroundColor Cyan
$invitations = Invoke-RestMethod -Uri 'http://localhost:8080/api/member/invitation/records?page=0&size=5' -Headers $h
Write-Host "总记录数: $($invitations.data.totalElements)"
```

---

## 📊 预期结果

### 1. 会员信息
```json
{
  "username": "admin",
  "currentLevel": {
    "levelName": "新手会员",
    "levelCode": "ROOKIE",
    "growthRequired": 0
  },
  "totalGrowth": 0,
  "currentGrowth": 0,
  "invitationCount": 0,
  "invitationCode": "INV000001XXXXXX"
}
```

### 2. 邀请链接
```json
{
  "invitationCode": "INV000001XXXXXX",
  "invitationLink": "http://localhost:3000/register?code=INV000001XXXXXX",
  "invitationText": "邀请你加入AI健康饮食规划助手！..."
}
```

### 3. 成长值记录
- 初始状态应该为空（0条记录）

### 4. 邀请记录
- 初始状态应该为空（0条记录）

---

## 🎯 测试检查清单

执行SQL修复后，验证以下项：

- [ ] `SELECT * FROM member_levels;` 返回5条等级数据
- [ ] `SELECT * FROM members WHERE user_id = 1;` 返回admin的会员记录
- [ ] API `/api/member/info` 返回200状态
- [ ] API `/api/member/invitation/generate` 返回邀请码
- [ ] API `/api/member/growth-records` 返回空列表
- [ ] API `/api/member/invitation/records` 返回空列表

---

## 📝 注意事项

1. **必须先执行SQL初始化**，否则所有API都会返回500错误
2. Flyway已启用，但V6迁移可能需要手动标记为成功
3. 如果重复执行SQL，使用 `INSERT IGNORE` 避免重复数据错误
4. 邀请码格式：`INV + 6位用户ID + 6位随机MD5`

---

## 🔗 相关文档

- `Sprint6-修复指南.md` - 完整修复步骤
- `init-members.sql` - 数据库初始化脚本
- `Sprint6-后端API开发完成.md` - API开发文档
- `Sprint6-前端开发完成.md` - 前端开发文档

---

**下一步**: 请执行上述SQL后，重新运行测试命令！
