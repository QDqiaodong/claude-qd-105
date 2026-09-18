import { createRouter, createWebHistory } from 'vue-router'
import Chutes from '../views/Chutes.vue'
import Batches from '../views/Batches.vue'
import Plans from '../views/Plans.vue'
import Bags from '../views/Bags.vue'
import Exceptions from '../views/Exceptions.vue'

const routes = [
  { path: '/', redirect: '/chutes' },
  { path: '/chutes', component: Chutes, meta: { title: '格口台账' } },
  { path: '/batches', component: Batches, meta: { title: '分拣批次' } },
  { path: '/plans', component: Plans, meta: { title: '装车发运' } },
  { path: '/bags', component: Bags, meta: { title: '中转袋台账' } },
  { path: '/exceptions', component: Exceptions, meta: { title: '异常件' } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
