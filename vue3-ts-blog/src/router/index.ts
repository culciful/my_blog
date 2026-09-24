import type {RouterOptions} from 'vue-router';
import {createRouter, createWebHistory} from 'vue-router';
import Index from '../pages/index/index.vue';
import {LOGIN_STATE} from '@/utils/localStoreItem';

const routes = [
    {
        path: '/',
        name: 'home',
        component: Index,
        alias: '/index'
    },
    {
        path: '/login',
        name: 'login',
        component: () => import('../pages/login/login.vue')
    },
    {
        path: '/register',
        name: 'register',
        component: () => import('../pages/login/register.vue')
    },
    {
        path: '/forgetPassword',
        name: 'forgetPassword',
        component: () => import('../pages/login/forgetPassword.vue')
    },
    {
        path: '/article/:articleId',
        name: 'article',
        component: () => import('../pages/article/index.vue'),
        props: true
    },
    {
        path: '/edit/:articleId',
        name: 'edit',
        component: () => import('../pages/article/addArticle.vue'),
        props: true
    },
    {
        path: '/write',
        name: 'write',
        component: () => import('../pages/article/addArticle.vue')
    },
    {
        path: '/draft/:articleId',
        name: 'editDraft',
        component: () => import('../pages/article/addArticle.vue'),
        props: true
    },
    {
        path: '/user',
        component: () => import('../pages/user/index.vue'),
        children: [
            {
                path: '',
                name: 'viewUser',
                component: () => import('../pages/user/manageContent.vue')
            },
            {
                path: 'userCenter',
                name: 'userCenter',
                component: () => import('../pages/user/userCenter.vue')
            },
            {
                path: 'manageContent',
                name: 'manageContent',
                component: () => import('../pages/user/manageContent.vue')
            },
            {
                path: 'following',
                name: 'following',
                component: () => import('../pages/user/follow.vue')
            },
            {
                path: 'followers',
                name: 'followers',
                component: () => import('../pages/user/follow.vue')
            }
        ]
    },
    {
        path: '/message',
        name: 'message',
        component: () => import('../pages/message/index.vue')
    }
];

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes
} as RouterOptions);

export const needLoginPathReg = /^\/(user\/\S+|message|write|edit|draft)/;
router.beforeEach((to, from) => {
    if (needLoginPathReg.test(to.path)) {
        const hasLogin = !!localStorage.getItem(LOGIN_STATE);
        if (!hasLogin) return {name: 'login'};
    }
    return true;
});

export default router;
