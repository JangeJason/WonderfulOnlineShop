import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import HomeLayout from '../views/HomeLayout.vue'
import ProductsView from '../views/ProductsView.vue'
import QuoteDemoView from '../views/QuoteDemoView.vue'

function isAuthed() {
  return !!localStorage.getItem('tokenValue')
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/products' },
    { path: '/login', component: LoginView },
    {
      path: '/',
      component: HomeLayout,
      children: [
        { path: 'products', component: ProductsView },
        { path: 'quote', component: QuoteDemoView }
      ]
    }
  ]
})

router.beforeEach((to: any) => {
  if (to.path === '/login') return true
  if (!isAuthed()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
