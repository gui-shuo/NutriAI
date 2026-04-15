/// <reference types="@dcloudio/types" />
/// <reference types="@types/wechat-miniprogram" />

declare module '*.vue' {
  import { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
