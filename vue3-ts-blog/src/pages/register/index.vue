<template>
<div class="login a-full center-tl">
    <div class="bg a-full"></div>

    <div class="icon-translation">
        <translate-icon></translate-icon>
    </div>

    <div class="login-box inner a-ta-c a-p-h-xxl a-pb-xxl a-bs-b">
        <img class="ta-c logo-img" src="/static/img/logo-no-background.png">
        <el-form :model="loginForm" size="large" class="a-mt-xxl">
            <el-form-item>
                <el-input v-model="loginForm.username" :placeholder="$t('label.username')" />
            </el-form-item>
            <el-form-item>
                <el-input
                        type="password"
                        show-password
                        v-model="loginForm.password"
                        :placeholder="$t('label.password')" />
            </el-form-item>
            <el-form-item>
                <el-button class="a-w-f" type="primary" round @click="onSubmit">{{$t('label.register')}}</el-button>
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

<script lang="ts" setup name="Register">
import {reactive} from 'vue';
import TranslateIcon from '@/components/translateIcon/index.vue';
const loginForm = reactive({
    username: '',
    password: ''
});
const onSubmit = () => {
    console.log(loginForm.username);
    console.log(loginForm.password);
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
