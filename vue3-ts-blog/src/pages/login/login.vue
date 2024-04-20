<template>
<div class="login a-full center-tl">
    <div class="bg a-full"></div>

    <div class="icon-translation">
        <translate-icon color="#409eff"></translate-icon>
    </div>

    <div class="login-box inner a-p-h-xxl a-pb-xxl a-bs-b">
        <img class="logo-img" src="/static/img/logo-no-background.png">
        <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="rules"
            size="large"
            class="a-mt-xxl">
            <el-form-item prop="username">
                <el-input v-model="loginForm.username" :placeholder="$t('label.usernameOrEmail')" maxlength="64" minlength="1" />
            </el-form-item>
            <el-form-item prop="password">
                <el-input
                    type="password"
                    show-password
                    v-model="loginForm.password"
                    minlength="6"
                    maxlength="64"
                    :placeholder="$t('label.password')" />
            </el-form-item>
            <el-form-item>
                <el-button
                    :loading="isQuerying"
                    class="a-w-f"
                    type="primary"
                    round
                    @click="onSubmit(loginFormRef)">
                    {{$t('label.login')}}
                </el-button>
            </el-form-item>
        </el-form>
        <div class="extra-btn a-ta-r">
            <el-link class="a-mb-xs" type="primary" :underline="false" href="/forgetPassword">{{$t('label.forgetPwd')}}</el-link>
            <el-link :underline="false" type="primary" href="/register">
                {{$t('label.register')}}
                <svg-icon class="a-ml-xxs" name="right"></svg-icon>
            </el-link>
        </div>
    </div>
</div>
</template>

<script lang="ts" setup name="Login">
import {reactive, ref, getCurrentInstance} from 'vue';
import TranslateIcon from '@/components/translateIcon/index.vue';
import type { FormInstance, FormRules } from 'element-plus';
import {globalRules} from '@/utils/validate';
import { useRouter } from 'vue-router';
import LoginConstant from '@/model/user/constant';
import {useUserStore} from '@/stores/user';
import {LOGIN_STATE} from '@/utils/localStoreItem';

interface LoginForm {
    username: string,
    password: string
}

const userStore = useUserStore();
const router = useRouter();
const { proxy }: any = getCurrentInstance();
const loginFormRef = ref<FormInstance>();
const loginForm = reactive<LoginForm>({
    username: '',
    password: ''
});
let isQuerying = ref(false);

const rules = reactive<FormRules<LoginForm>>({
    username: globalRules.username,
    password: globalRules.password
});

const onSubmit =  (formEl: FormInstance | undefined) => {
    if (!formEl) return;
    formEl.validate((valid) => {
        if (valid) {
            isQuerying.value = true;
            proxy.$request.post(LoginConstant.url.login, {
                [LoginConstant.username]: loginForm.username,
                [LoginConstant.password]: loginForm.password
            }).then(res => {
                isQuerying.value = false;
                localStorage.setItem(LOGIN_STATE, '1');
                userStore.setUserData(res.result);
                router.push({ path: '/' });
            }).catch(err => {
                localStorage.removeItem(LOGIN_STATE);
                isQuerying.value = false;
            });
        }
    });
};

</script>

<style scoped lang="scss">
@import "index";

.extra-btn {
    position: absolute;
    right: 40px;
    bottom: 40px;
    .el-link {
        display: block;
    }
}
</style>
