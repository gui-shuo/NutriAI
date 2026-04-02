<template>
  <div class="legal-page">
    <div class="legal-card">
      <el-button text @click="$router.back()" style="margin-bottom: 16px">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h1>免责声明</h1>
      <p class="update-date">更新日期：2026年3月30日 &nbsp;|&nbsp; 生效日期：2026年3月30日</p>

      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 24px">
        <template #title>
          <strong>重要提示：请在使用本应用前仔细阅读以下免责声明</strong>
        </template>
      </el-alert>

      <h2>一、AI 生成内容声明</h2>
      <p>本应用中的 AI 营养师对话、饮食计划生成、食物营养分析等功能均基于人工智能算法生成。</p>
      <ol>
        <li><strong>AI 生成的所有内容仅供参考，不构成医疗建议、诊断或治疗方案。</strong></li>
        <li>AI 生成内容可能存在不准确、不完整或过时的情况。</li>
        <li>用户不应将 AI 建议作为制定饮食方案的唯一依据。</li>
        <li>对于患有糖尿病、高血压、肾病、食物过敏等疾病的用户，<strong>请务必遵循专业医生的指导</strong>。</li>
      </ol>

      <h2>二、食物识别与营养数据声明</h2>
      <ol>
        <li>食物识别功能基于第三方图像识别 API，识别结果可能存在误差。</li>
        <li>营养成分数据来源于公开数据库，数据的准确性和时效性不作保证。</li>
        <li>不同品牌、产地、烹饪方式的食物营养成分可能存在较大差异。</li>
        <li>用户不应将本应用提供的营养数据作为精确计量的依据。</li>
      </ol>

      <h2>三、医疗相关声明</h2>
      <p><strong>本应用不是医疗器械或医疗服务平台，不具备任何医疗资质。</strong></p>
      <ol>
        <li>本应用不能用于疾病的预防、诊断、治疗或监测。</li>
        <li>如您出现任何身体不适，请立即就医并遵从专业医生建议。</li>
        <li>孕妇、哺乳期女性、儿童、老年人及患有慢性疾病者在调整饮食前，应咨询专业医疗人员。</li>
        <li>本应用的体质档案功能仅供用户自行记录和参考，不具有任何医学诊断价值。</li>
      </ol>

      <h2>四、产品商城声明</h2>
      <ol>
        <li>商城中展示的营养产品信息（包括功效描述、营养成分等）来源于第三方，本应用不对其真实性和准确性做任何担保。</li>
        <li>本应用目前采用模拟交易模式，不涉及真实资金交易。</li>
        <li>营养补充剂不能替代正常饮食和药物治疗。</li>
        <li>用户在购买和使用任何营养产品前，建议咨询专业人员。</li>
      </ol>

      <h2>五、社区内容声明</h2>
      <ol>
        <li>社区中用户发布的内容（帖子、评论等）为用户个人观点，不代表本应用立场。</li>
        <li>本应用不对用户生成内容的真实性、准确性、完整性做任何保证。</li>
        <li>用户应自行判断社区内容的可靠性，因参考社区内容而导致的损失由用户自行承担。</li>
        <li>如发现违法违规内容，请通过举报功能或意见反馈联系我们。</li>
      </ol>

      <h2>六、数据与隐私声明</h2>
      <ol>
        <li>用户的体质档案、饮食记录等个人数据仅存储于安全的云服务器中。</li>
        <li>我们不会向任何第三方出售或共享用户的个人数据。</li>
        <li>AI 对话内容可能被用于改善服务质量，但会经过匿名化处理。</li>
        <li>详细的数据保护措施请参阅<router-link to="/legal/privacy">《隐私政策》</router-link>。</li>
      </ol>

      <h2>七、责任限制</h2>
      <p>在法律允许的最大范围内，本应用开发者不对以下情况承担责任：</p>
      <ol>
        <li>因使用或无法使用本应用而造成的任何直接、间接、偶然或后果性损失。</li>
        <li>因依赖本应用提供的AI建议而导致的身体问题或经济损失。</li>
        <li>因网络故障、服务器维护等导致的服务中断。</li>
        <li>因第三方服务（AI接口、图像识别等）异常导致的功能失效。</li>
      </ol>

      <h2>八、联系方式</h2>
      <p>如有任何疑问或建议，请通过应用内"意见反馈"功能联系我们。</p>
    </div>
  </div>
</template>

<script setup>
import { ArrowLeft } from '@element-plus/icons-vue'
</script>

<style scoped lang="scss">
.legal-page {
  min-height: 100vh;
  background: #FAFAFA;
  padding: 40px 20px;
  font-family: 'Inter', sans-serif;
}
.legal-card {
  max-width: 800px;
  margin: 0 auto;
  background: #FFFFFF;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1);
  border: 1px solid #E2E8F0;
  position: relative;

  h1 { font-size: 28px; margin-bottom: 8px; font-family: 'Calistoga', cursive; color: #0F172A; }
  h2 { font-size: 18px; margin: 24px 0 12px; color: #0F172A; font-family: 'Calistoga', cursive; border-bottom: 1px solid #E2E8F0; padding-bottom: 6px; }
  p, li { font-size: 14px; line-height: 1.8; color: #0F172A; font-family: 'Inter', sans-serif; }
  ol { padding-left: 20px; }
  .update-date { color: #0F172A; opacity: 0.5; font-size: 13px; margin-bottom: 24px; font-family: 'Inter', sans-serif; }
  strong { color: #10B981; }
  a { color: #10B981; text-decoration: none; &:hover { text-decoration: underline; } }
}
</style>
