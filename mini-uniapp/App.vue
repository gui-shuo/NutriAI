<script>
	import { useUserStore } from './stores/user'
	export default {
		onLaunch: function() {
			console.log('NutriAI App Launch')
			// 恢复登录态
			const userStore = useUserStore()
			userStore.restoreSession()
		},
		onShow: function() {
			console.log('App Show')
		},
		onHide: function() {
			console.log('App Hide')
		}
	}
</script>

<style lang="scss">
	@import 'uview-plus/index.scss';
	@import './styles/design-system.scss';

	/* 全局基础样式 */
	page {
		background-color: #ffffff;
		color: $on-surface;
		font-family: -apple-system, BlinkMacSystemFont, 'Inter', 'PingFang SC',
			'Microsoft YaHei', 'Helvetica Neue', sans-serif;
		font-size: $font-base;
		line-height: $leading-normal;
		box-sizing: border-box;
		width: 100%;
		overflow-x: hidden;
	}

	view, text, image, scroll-view {
		box-sizing: border-box;
		max-width: 100%;
	}

	/* 隐藏滚动条 */
	::-webkit-scrollbar {
		display: none;
		width: 0;
		height: 0;
	}

	/* 安全区域 */
	.safe-bottom {
		padding-bottom: constant(safe-area-inset-bottom);
		padding-bottom: env(safe-area-inset-bottom);
	}

	/* 文本省略 */
	.text-ellipsis {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.text-ellipsis-2 {
		display: -webkit-box;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: 2;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	/* 通用flex布局 */
	.flex-center {
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.flex-between {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.flex-col {
		display: flex;
		flex-direction: column;
	}
</style>
