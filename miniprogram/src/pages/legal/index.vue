<template>
  <view class="container">
    <view class="legal-page">
      <view class="legal-title">{{ pageTitle }}</view>
      <view class="legal-date">更新日期：2026年3月30日</view>

      <!-- 用户协议 -->
      <view v-if="type === 'terms'">
        <view class="section">
          <view class="section-title">一、总则</view>
          <view class="section-text">欢迎使用 NutriAI 智能健康饮食助手（以下简称"本应用"）。本应用由开发者个人运营，旨在为用户提供AI辅助的饮食参考信息。使用本应用即表示您同意接受本协议的约束。</view>
        </view>
        <view class="section">
          <view class="section-title">二、服务说明</view>
          <view class="section-text">本应用提供AI饮食建议参考、食物营养信息查询、饮食记录管理、社区交流等功能。本应用所提供的所有信息仅供参考，不构成任何形式的医疗建议、诊断或治疗方案。</view>
        </view>
        <view class="section">
          <view class="section-title">三、用户责任</view>
          <view class="section-text">1. 用户应确保注册信息的真实性和准确性。
2. 用户不得利用本应用从事任何违法违规活动。
3. 用户在社区发布的内容应遵守中华人民共和国相关法律法规。
4. 用户应妥善保管账号信息。</view>
        </view>
        <view class="section">
          <view class="section-title">四、免责声明</view>
          <view class="section-text highlight">1. AI营养建议、饮食计划、食物识别结果等均为算法生成，仅供参考，不能替代专业医生、营养师的建议。
2. 如您有任何健康问题，请务必咨询有资质的专业医疗人员。
3. 本应用不对因使用AI生成内容而造成的任何损失承担责任。
4. 社区中用户发布的内容不代表本应用立场。
5. 商城中展示的产品信息来源于第三方，本应用不对产品质量做任何担保。</view>
        </view>
        <view class="section">
          <view class="section-title">五、知识产权</view>
          <view class="section-text">本应用的所有内容的知识产权归开发者所有，受中华人民共和国著作权法保护。</view>
        </view>
        <view class="section">
          <view class="section-title">六、适用法律</view>
          <view class="section-text">本协议的订立、执行和解释及争议的解决均应适用中华人民共和国法律。</view>
        </view>
        <view class="section">
          <view class="section-title">七、联系方式</view>
          <view class="section-text">如有任何问题或建议，请通过应用内"意见反馈"功能联系我们。</view>
        </view>
      </view>

      <!-- 隐私政策 -->
      <view v-if="type === 'privacy'">
        <view class="section">
          <view class="section-title">一、信息收集</view>
          <view class="section-text">为提供服务，我们可能收集以下信息：
1. 注册信息：用户名、邮箱地址、手机号码（选填）。
2. 健康档案信息：身高、体重、年龄等（用户自愿填写）。
3. 饮食记录：用户主动记录的每日饮食信息。
4. 设备信息：设备型号、操作系统版本。
5. 使用数据：功能使用频率、页面访问记录。</view>
        </view>
        <view class="section">
          <view class="section-title">二、信息用途</view>
          <view class="section-text">我们收集的信息仅用于：
1. 提供和改善AI饮食建议的个性化服务。
2. 维护账号安全和应用正常运行。
3. 向用户发送服务相关通知。
4. 统计和分析以改善用户体验（数据已匿名化）。</view>
        </view>
        <view class="section">
          <view class="section-title">三、信息安全</view>
          <view class="section-text">1. 用户密码采用 BCrypt 加密存储。
2. 数据传输全程采用 HTTPS 加密协议。
3. 数据库存储于安全认证的云服务器中。
4. 定期审查安全策略，防止数据泄露。</view>
        </view>
        <view class="section">
          <view class="section-title">四、信息共享</view>
          <view class="section-text highlight">我们不会向任何第三方出售、出租或共享您的个人信息，除非：
1. 获得您的明确授权同意。
2. 根据法律法规或司法机关的强制要求。
3. 为保护其他用户或公众的安全和权益。</view>
        </view>
        <view class="section">
          <view class="section-title">五、用户权利</view>
          <view class="section-text">您享有查看、更正、删除个人信息和撤回同意的权利。如需删除账号及数据，请通过意见反馈功能联系我们。</view>
        </view>
        <view class="section">
          <view class="section-title">六、未成年人保护</view>
          <view class="section-text">本应用不面向14周岁以下的未成年人。如果您是未成年人的监护人，发现孩子未经同意使用了本应用，请联系我们删除相关信息。</view>
        </view>
        <view class="section">
          <view class="section-title">七、联系我们</view>
          <view class="section-text">如有任何关于隐私保护的问题，请通过应用内"意见反馈"功能联系我们。</view>
        </view>
      </view>

      <!-- 免责声明 -->
      <view v-if="type === 'disclaimer'">
        <view class="warning-box">
          ⚠️ 重要提示：请在使用本应用前仔细阅读以下免责声明
        </view>
        <view class="section">
          <view class="section-title">一、AI 生成内容声明</view>
          <view class="section-text highlight">1. AI生成的所有内容仅供参考，不构成医疗建议、诊断或治疗方案。
2. AI生成内容可能存在不准确、不完整或过时的情况。
3. 用户不应将AI建议作为制定饮食方案的唯一依据。
4. 对于患有糖尿病、高血压、肾病、食物过敏等疾病的用户，请务必遵循专业医生的指导。</view>
        </view>
        <view class="section">
          <view class="section-title">二、食物识别与营养数据声明</view>
          <view class="section-text">1. 食物识别功能基于第三方图像识别API，识别结果可能存在误差。
2. 营养成分数据来源于公开数据库，准确性和时效性不作保证。
3. 不同品牌、产地、烹饪方式的食物营养成分可能差异较大。</view>
        </view>
        <view class="section">
          <view class="section-title">三、健康与医疗声明</view>
          <view class="section-text highlight">本应用不是医疗器械或医疗服务平台，不具备任何医疗资质。
1. 不能用于疾病的预防、诊断、治疗或监测。
2. 如出现健康问题，请立即就医。
3. 孕妇、哺乳期女性、儿童、老年人及慢性疾病患者调整饮食前应咨询专业医疗人员。</view>
        </view>
        <view class="section">
          <view class="section-title">四、产品商城声明</view>
          <view class="section-text">1. 商城产品信息来源于第三方，不对其真实性做任何担保。
2. 营养补充剂不能替代正常饮食和药物治疗。
3. 购买使用营养产品前建议咨询专业人员。</view>
        </view>
        <view class="section">
          <view class="section-title">五、社区内容声明</view>
          <view class="section-text">社区中用户发布的内容为个人观点，不代表本应用立场。用户应自行判断内容可靠性。</view>
        </view>
        <view class="section">
          <view class="section-title">六、责任限制</view>
          <view class="section-text">在法律允许的最大范围内，本应用开发者不对因使用或无法使用本应用而造成的任何直接、间接损失承担责任。</view>
        </view>
        <view class="section">
          <view class="section-title">七、联系方式</view>
          <view class="section-text">如有任何疑问，请通过应用内"意见反馈"功能联系我们。</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const type = ref('terms')

const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    terms: '用户协议',
    privacy: '隐私政策',
    disclaimer: '免责声明'
  }
  return titles[type.value] || '用户协议'
})

onLoad((query) => {
  if (query?.type) type.value = query.type
  uni.setNavigationBarTitle({ title: pageTitle.value })
})
</script>

<style scoped>
.legal-page {
  padding: 20rpx 0;
}
.legal-title {
  font-size: 40rpx;
  font-weight: 700;
  text-align: center;
  margin-bottom: 10rpx;
}
.legal-date {
  font-size: 24rpx;
  color: #999;
  text-align: center;
  margin-bottom: 30rpx;
}
.section {
  margin-bottom: 28rpx;
}
.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 12rpx;
}
.section-text {
  font-size: 26rpx;
  color: #555;
  line-height: 1.8;
  white-space: pre-line;
}
.section-text.highlight {
  color: #c00;
}
.warning-box {
  background: #fff3cd;
  color: #856404;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  font-size: 26rpx;
  font-weight: 600;
  text-align: center;
  margin-bottom: 30rpx;
}
</style>
