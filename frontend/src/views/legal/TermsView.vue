<template>
  <div class="legal-page">
    <div class="legal-card">
      <el-button text @click="$router.back()" style="margin-bottom: 16px">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h1>用户协议</h1>
      <p class="update-date">更新日期：2026年3月30日 &nbsp;|&nbsp; 生效日期：2026年3月30日</p>

      <h2>一、总则</h2>
      <p>欢迎使用 NutriAI 智能健康饮食助手（以下简称"本应用"）。本应用由开发者个人运营，旨在为用户提供AI辅助的饮食参考信息。在使用本应用前，请您仔细阅读本协议的全部内容。使用本应用即表示您同意接受本协议的约束。</p>

      <h2>二、服务说明</h2>
      <p>本应用提供以下功能：AI饮食建议参考、食物营养信息查询、饮食记录管理、社区交流等。<strong>本应用所提供的所有信息仅供参考，不构成任何形式的医疗建议、诊断或治疗方案。</strong></p>

      <h2>三、用户责任</h2>
      <ol>
        <li>用户应确保注册信息的真实性和准确性。</li>
        <li>用户不得利用本应用从事任何违法违规活动。</li>
        <li>用户在社区发布的内容应遵守中华人民共和国相关法律法规，不得发布违法、虚假、侵权内容。</li>
        <li>用户应妥善保管账号信息，因账号泄露导致的损失由用户自行承担。</li>
      </ol>

      <h2>四、免责声明</h2>
      <ol>
        <li><strong>本应用提供的AI营养建议、饮食计划、食物识别结果等均为算法生成，仅供参考，不能替代专业医生、营养师或其他医疗保健专业人员的建议。</strong></li>
        <li>如您有任何健康问题或特殊饮食需求，请务必咨询有资质的专业医疗人员。</li>
        <li>本应用不对因使用AI生成内容而造成的任何直接或间接损失承担责任。</li>
        <li>社区中用户发布的内容不代表本应用立场，本应用不对用户生成内容的准确性负责。</li>
        <li>商城中展示的产品信息来源于第三方，本应用不对产品质量做任何担保。</li>
      </ol>

      <h2>五、知识产权</h2>
      <p>本应用的所有内容（包括但不限于文字、图片、软件、音频、视频等）的知识产权归开发者所有，受中华人民共和国著作权法及相关法律保护。</p>

      <h2>六、隐私保护</h2>
      <p>我们重视用户的隐私保护，具体请参阅<router-link to="/legal/privacy">《隐私政策》</router-link>。</p>

      <h2>七、协议修改</h2>
      <p>开发者有权根据需要修改本协议内容。修改后的协议将在应用内公布，继续使用本应用即视为接受修改后的协议。</p>

      <h2>八、适用法律</h2>
      <p>本协议的订立、执行和解释及争议的解决均应适用中华人民共和国法律。</p>

      <h2>九、联系方式</h2>
      <p>如有任何问题或建议，请通过应用内"意见反馈"功能联系我们。</p>
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
  a { color: #409eff; text-decoration: none; }
}
</style>
