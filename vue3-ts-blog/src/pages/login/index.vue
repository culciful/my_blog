<template>
<div class="login a-full center-tl">
    <div class="bg a-full"></div>

    <div class="icon-translation">
        <translate-icon></translate-icon>
    </div>

    <div class="login-box inner a-ta-c a-p-h-xxl a-pb-xxl a-bs-b">
        <img class="ta-c logo-img" src="/static/img/logo-no-background.png">
        <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="rules"
            status-icon
            size="large"
            class="a-mt-xxl">
            <el-form-item prop="username">
                <el-input v-model="loginForm.username" :placeholder="$t('label.username')" />
            </el-form-item>
            <el-form-item prop="password">
                <el-input
                    type="password"
                    show-password
                    v-model="loginForm.password"
                    :placeholder="$t('label.password')" />
            </el-form-item>
            <el-form-item>
                <el-button class="a-w-f" type="primary" round @click="onSubmit(loginFormRef)">{{$t('label.login')}}</el-button>
            </el-form-item>
        </el-form>
        <div class="register-btn">
            <el-button link type="primary" @click="gotoRegister">{{$t('label.register')}}
                <el-icon class="a-ml-xxs"><Right /></el-icon>
            </el-button>
        </div>
    </div>
</div>
</template>

<script lang="ts" setup name="Login">
import {reactive, ref, getCurrentInstance} from 'vue';
import TranslateIcon from '@/components/translateIcon/index.vue';
import type { FormInstance, FormRules } from 'element-plus';
import { useI18n } from 'vue-i18n';
import {regExps} from '@/utils/validate';

interface LoginForm {
    username: string,
    password: string
}
const { t } = useI18n();
const { proxy }: any = getCurrentInstance();
const loginFormRef = ref<FormInstance>();
const loginForm = reactive<LoginForm>({
    username: '',
    password: ''
});
const validatePass = (rule: any, value: any, callback: any) => {
    if (!regExps.enabledChar.test(value)) {
        callback(new Error(t('inputMessage.passwordFormat')));
    }
    callback();
};

const rules = reactive<FormRules<LoginForm>>({
    username: [
        { required: true, message: t('inputMessage.inputUsername'), trigger: 'blur' },
        { min: 4, max: 16, message: t('inputMessage.usernameFormat'), trigger: 'blur' },
    ],
    password: [
        {required: true, message: t('inputMessage.inputPassword'), trigger: 'change',},
        { min: 6, max: 32, message: t('inputMessage.passwordFormat'), trigger: 'blur' },
        { validator: validatePass, trigger: 'blur' }
    ]
});

const onSubmit = async (formEl: FormInstance | undefined) => {
    if (!formEl) return;
    await formEl.validate((valid, fields) => {
        if (valid) {
            console.log('submit!');
            proxy.$request.post('/user/login', {
                username: loginForm.username,
                password: loginForm.password
            }).then(res => {
                console.log(res);
            }).catch(err => {
                console.log(err);
            });
        } else {
            console.log('error submit!', fields);
        }
    });
};

const gotoRegister = () => {
    console.log('gotoRegister');
};

</script>

<style scoped lang="scss">
.bg {
    background-image: url("/static/img/bg.png");
    background-size: cover;
    background-repeat: no-repeat;
    background-position: center;
    opacity: 0.3;
    filter: blur(3px);
}
.icon-translation {
    position: absolute;
    right: 24px;
    top: 24px;
}
.logo-img {
    height: 40px;
    margin-top: 64px;
}

.login-box {
    width: 400px;
    height: 600px;
    background-color: $white;
    border-radius: 12px;
}

.register-btn {
    position: absolute;
    right: 40px;
    bottom: 40px;
}
</style>
