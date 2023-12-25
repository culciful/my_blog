import {createRouter, createWebHistory} from 'vue-router';
import type { RouterOptions } from 'vue-router';
import Index from '../pages/index/index.vue';
import Login from '../pages/login/index.vue';

const routes = [
  {
    path: '/',
    name: 'home',
    component: Login
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../pages/login/index.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../pages/register/index.vue')
  }
];

const router = createRouter<RouterOptions>({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
} as RouterOptions);

export default router;
