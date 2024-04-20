<template>
<div class="login a-full center-tl">
    <div class="bg a-full"></div>

    <div class="icon-translation">
        <translate-icon color="#409eff"></translate-icon>
    </div>

    <div class="login-box inner a-ta-c a-p-h-xxl a-pb-xxl a-bs-b">
        <div class="ta-c"><img class="logo-img a-d-ib" src="/static/img/logo-no-background.png"></div>
        <p class="a-font-title-1 a-mt-lg">{{$t('label.'+steps[active])}}</p>
        <el-form
            ref="forgetPwdRef"
            :model="forgetPwdForm"
            :rules="rules"
            size="large"
            class="a-mt-xxl">
            <template v-if="active===0">
                <el-form-item prop="email">
                    <el-input v-model="forgetPwdForm.email" type="email" :placeholder="$t('label.email')" />
                </el-form-item>
                <el-form-item prop="verificationCode" class="verification-code">
                    <el-input
                        v-model="forgetPwdForm.verificationCode"
                        :placeholder="$t('label.verificationCode')" />
                    <el-button plain @click="sendVeriCode" :disabled="hasSendCode">
                        {{hasSendCode?($t('label.resend')+timeToGet+'s'):$t('label.getVerificationCode')}}
                    </el-button>
                </el-form-item>
            </template>
            <template v-else>
                <el-form-item prop="newPassword">
                    <el-input
                        type="password"
                        show-password
                        maxlength="64"
                        minlength="6"
                        v-model="forgetPwdForm.newPassword"
                        :disabled="isQuerying"
                        :placeholder="$t('label.newPassword')" />
                </el-form-item>
                <el-form-item prop="confirmNewPwd">
                    <el-input
                        type="password"
                        show-password
                        maxlength="64"
                        minlength="6"
                        v-model="forgetPwdForm.confirmNewPwd"
                        :disabled="isQuerying"
                        :placeholder="$t('label.confirmNewPwd')" />
                </el-form-item>
            </template>
            <el-form-item>
                <el-button
                    :loading="isQuerying"
                    class="a-w-f"
                    type="primary"
                    round
                    @click="clickBtn">
                    {{active===0?$t('label.nextStep'):$t('label.confirm')}}
                </el-button>
            </el-form-item>
        </el-form>
    </div>
</div>
</template>

<script lang="ts" setup name="ForgetPassword">
import {reactive, ref, getCurrentInstance} from 'vue';
import TranslateIcon from '@/components/translateIcon/index.vue';
import type { FormInstance, FormRules } from 'element-plus';
import {globalRules} from '@/utils/validate';
import { useRouter } from 'vue-router';
import Constant from '@/model/user/constant';
import {useUserStore} from '@/stores/user';
import i18n from '@/language/i18n';

interface ForgetPwdForm {
    email: string,
    verificationCode: string,
    newPassword: string,
    confirmNewPwd: string,
}

const router = useRouter();
const userStore = useUserStore();
const { t } = i18n.global as any;
const { proxy }: any = getCurrentInstance();
const steps = ['authentication', 'resetPwd'];
const active = ref(0);
const forgetPwdRef = ref<FormInstance>();
const forgetPwdForm = reactive<ForgetPwdForm>({
    email: '',
    verificationCode: '',
    newPassword: '',
    confirmNewPwd: ''
});
let isQuerying = ref(false);
const validatePass2 = (rule: any, value: any, callback: any) => {
    if (value !== forgetPwdForm.newPassword) {
        callback(new Error(t('inputMessage.inputNotMatch')));
    } else callback();
};
const rules = reactive<FormRules<ForgetPwdForm>>({
    email: globalRules.email,
    verificationCode: globalRules.required,
    newPassword: globalRules.password,
    confirmNewPwd: [{ validator: validatePass2, trigger: 'blur' }]
});

const hasSendCode = ref(false);
const timeToGet = ref(60);
let interval = null;
const sendVeriCode = () => {
    forgetPwdRef.value.validateField('email', valid => {
        if(valid) {
            proxy.$request.post(Constant.url.checkEmailExist, {
                [Constant.email]: forgetPwdForm.email
            }).then(res => {
                if(res.result[Constant.isExisted]) send();
            });
        }
    });
};
const send = () => {
    hasSendCode.value = true;
    setCountdownTimer();
    proxy.$request.post(Constant.url.sendEmailCode, {
        [Constant.email]: forgetPwdForm.email
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

const clickBtn = () => {
    if(active.value === 0) {
        forgetPwdRef.value.validateField(['email', 'verificationCode'], valid => {
            if(valid) {
                isQuerying.value = true;
                proxy.$request.post(Constant.url.checkEmailCode, {
                    [Constant.email]: forgetPwdForm.email,
                    [Constant.verificationCode]: forgetPwdForm.verificationCode
                }).then(res => {
                    isQuerying.value = false;
                    active.value++;
                }).catch(err => {
                    isQuerying.value = false;
                });
            }
        });
    } else {
        forgetPwdRef.value.validateField(['newPassword', 'confirmNewPwd'], valid => {
            if(valid) {
                isQuerying.value = true;
                proxy.$request.post(Constant.url.updateUserInfo, {
                    [Constant.password]: forgetPwdForm.newPassword
                }).then(res => {
                    isQuerying.value = false;
                    userStore.clear();
                    router.replace({name: 'login'});
                }).catch(err => {
                    isQuerying.value = false;
                });
            }
        });
    }
};
</script>

<style scoped lang="scss">
@import "index";
</style>
