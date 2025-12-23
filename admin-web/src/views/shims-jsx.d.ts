import type { VNodeChild } from 'vue'

declare global {
  namespace JSX {
    interface IntrinsicElements {
      [elem: string]: any
    }
    type Element = VNodeChild
  }
}

export {}
