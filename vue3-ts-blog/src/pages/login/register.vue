<template>
<div class="login a-full center-tl">
    <div class="bg a-full"></div>

    <div class="icon-translation">
        <translate-icon color="#409eff"></translate-icon>
    </div>

    <div class="login-box inner a-p-h-xxl a-pb-xxl a-bs-b">
        <img class="logo-img" src="/static/img/logo-no-background.png">
        <el-form
            :model="registerForm"
            :rules="rules"
            ref="registerFormRef"
            size="large" 
            class="a-mt-xxl">
            <el-form-item prop="email">
                <el-input type="email" v-model="registerForm.email" :placeholder="$t('label.email')" />
            </el-form-item>
            <el-form-item prop="verificationCode" class="verification-code">
                <el-input
                    v-model="registerForm.verificationCode"
                    :placeholder="$t('label.verificationCode')" />
                <el-button plain @click="sendVeriCode" :disabled="hasSendCode">
                    {{hasSendCode?($t('label.resend')+timeToGet+'s'):$t('label.getVerificationCode')}}
                </el-button>
            </el-form-item>
            <el-form-item prop="username">
                <el-input v-model="registerForm.username"
                          :placeholder="$t('label.username')"
                          maxlength="16"
                          minlength="1" />
            </el-form-item>
            <el-form-item prop="password">
                <el-input
                        type="password"
                        show-password
                        maxlength="64"
                        minlength="6"
                        v-model="registerForm.password"
                        :placeholder="$t('label.password')" />
            </el-form-item>
            <el-form-item>
                <el-button
                    :loading="isQuerying"
                    class="a-w-f"
                    type="primary"
                    round
                    @click="onSubmit(registerFormRef)">
                    {{$t('label.register')}}
                </el-button>
            </el-form-item>
        </el-form>
    </div>
</div>
</template>

<script lang="ts" setup name="Register">
import {reactive, getCurrentInstance, ref} from 'vue';
import TranslateIcon from '@/components/translateIcon/index.vue';
import type { FormInstance, FormRules } from 'element-plus';
import {globalRules} from '@/utils/validate';
import { useRouter } from 'vue-router';
import Constant from '@/model/user/constant';
import {ElMessage} from 'element-plus';
import i18n from '@/language/i18n';

interface RegisterForm {
    email: string,
    username: string,
    password: string,
    verificationCode: string
}
const router = useRouter();
const { t } = i18n.global as any;
const { proxy }: any = getCurrentInstance();
const registerFormRef = ref<FormInstance>();
const registerForm = reactive<RegisterForm>({
    email: '',
    username: '',
    password: '',
    verificationCode: ''
});
let isQuerying = ref(false);

const rules = reactive<FormRules<RegisterForm>>({
    username: globalRules.username,
    password: globalRules.password,
    email: globalRules.email,
    verificationCode: globalRules.required
});

const hasSendCode = ref(false);
const timeToGet = ref(60);
let interval = null;
const sendVeriCode = () => {
    registerFormRef.value.validateField('email', valid => {
        if(valid) {
            proxy.$request.post(Constant.url.checkEmailExist, {
                [Constant.email]: registerForm.email
            }).then(res => {
                if(res.result[Constant.isExisted]) {
                    ElMessage.error(t('infoMessage.emailExisted'));
                }
                else send();
            });
        }
    });
};
const send = () => {
    hasSendCode.value = true;
    setCountdownTimer();
    proxy.$request.post(Constant.url.sendEmailCode, {
        [Constant.email]: registerForm.email
    }).catch(err => {
        hasSendCode.value = false;
        if(interval) clearInterval(interval);
    });
};
const setCountdownTimer = () => {
    timeToGet.value = 60;
    interval = setInterval(() => {
        timeToGet.value = timeToGet.value - 1;
        if(timeToGet.value === 0) {
            hasSendCode.value = false;
            clearInterval(interval);
        }
    }, 1000);
};

const onSubmit = async (formEl: FormInstance | undefined) => {
    if (!formEl) return;
    await formEl.validate((valid) => {
        if (valid) {
            isQuerying.value = true;
            proxy.$request.post(Constant.url.register, {
                [Constant.email]: registerForm.email,
                [Constant.username]: registerForm.username,
                [Constant.password]: registerForm.password
            }).then(res => {
                isQuerying.value = false;
                router.push({ path: '/login' });
            }).catch(err => {
                isQuerying.value = false;
            });
        }
    });
};

</script>

<style scoped lang="scss">
@import "index";
</style>
