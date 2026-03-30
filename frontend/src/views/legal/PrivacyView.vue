<template>
  <div class="legal-page">
    <div class="legal-card">
      <el-button text @click="$router.back()" style="margin-bottom: 16px">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h1>隐私政策</h1>
      <p class="update-date">更新日期：2026年3月30日 &nbsp;|&nbsp; 生效日期：2026年3月30日</p>

      <h2>一、信息收集</h2>
      <p>为提供服务，我们可能收集以下信息：</p>
      <ol>
        <li><strong>注册信息</strong>：用户名、邮箱地址、手机号码（选填）。</li>
        <li><strong>健康档案信息</strong>：身高、体重、年龄、性别、健康目标、过敏原等（用户自愿填写）。</li>
        <li><strong>饮食记录</strong>：用户主动记录的每日饮食信息。</li>
        <li><strong>设备信息</strong>：设备型号、操作系统版本、唯一设备标识符。</li>
        <li><strong>使用数据</strong>：功能使用频率、页面访问记录。</li>
      </ol>

      <h2>二、信息用途</h2>
      <p>我们收集的信息仅用于：</p>
      <ol>
        <li>提供和改善AI饮食建议的个性化服务。</li>
        <li>维护账号安全和应用正常运行。</li>
        <li>向用户发送服务相关通知（如反馈回复）。</li>
        <li>统计和分析以改善用户体验（数据已匿名化处理）。</li>
      </ol>

      <h2>三、信息安全</h2>
      <p>我们采取以下措施保护您的个人信息：</p>
      <ol>
        <li>用户密码采用 BCrypt 加密存储，任何人（包括开发者）无法查看明文密码。</li>
        <li>数据传输全程采用 HTTPS 加密协议。</li>
        <li>数据库存储于经过安全认证的云服务器中，实施访问控制和防火墙保护。</li>
        <li>定期审查安全策略，防止数据泄露。</li>
      </ol>

      <h2>四、信息共享</h2>
      <p><strong>我们不会向任何第三方出售、出租或共享您的个人信息</strong>，但以下情况除外：</p>
      <ol>
        <li>获得您的明确授权同意。</li>
        <li>根据中华人民共和国法律法规的要求、行政或司法机关的强制要求。</li>
        <li>为保护其他用户或公众的安全和权益所必需。</li>
      </ol>

      <h2>五、第三方服务</h2>
      <p>本应用使用以下第三方服务：</p>
      <ol>
        <li><strong>AI 服务</strong>：调用第三方大语言模型API以提供智能对话功能。我们仅传递对话内容，不传递您的身份信息。</li>
        <li><strong>对象存储服务</strong>：用于存储用户上传的头像等文件。</li>
        <li><strong>邮件服务</strong>：用于发送验证码和反馈通知邮件。</li>
      </ol>

      <h2>六、用户权利</h2>
      <p>您享有以下权利：</p>
      <ol>
        <li><strong>查看权</strong>：您可以随时查看和导出您的个人信息。</li>
        <li><strong>更正权</strong>：您可以随时修改您的个人资料和健康档案。</li>
        <li><strong>删除权</strong>：您可以要求删除您的账号及所有相关数据，请通过意见反馈功能联系我们。</li>
        <li><strong>撤回同意权</strong>：您可以随时停止使用本应用来撤回对本隐私政策的同意。</li>
      </ol>

      <h2>七、未成年人保护</h2>
      <p>本应用不面向14周岁以下的未成年人。如果您是未成年人的监护人，发现您的孩子未经同意使用了本应用，请联系我们以删除相关信息。</p>

      <h2>八、政策更新</h2>
      <p>本隐私政策可能根据法律法规变化或服务调整而更新。更新后将在应用内发布通知。</p>

      <h2>九、联系我们</h2>
      <p>如有任何关于隐私保护的问题，请通过应用内"意见反馈"功能联系我们。</p>
    </div>
  </div>
</template>

<script setup>
import { ArrowLeft } from '@element-plus/icons-vue'
</script>

<style scoped lang="scss">
.legal-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 40px 20px;
}
.legal-card {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  h1 { font-size: 28px; margin-bottom: 8px; }
  h2 { font-size: 18px; margin: 24px 0 12px; color: #333; }
  p, li { font-size: 14px; line-height: 1.8; color: #555; }
  ol { padding-left: 20px; }
  .update-date { color: #999; font-size: 13px; margin-bottom: 24px; }
  strong { color: #333; }
}
</style>
