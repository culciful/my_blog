import axios from 'axios';
import mockConfig from '/mock';
import i18n from '@/language/i18n';
import {ElMessage} from 'element-plus';
import { useRouter } from 'vue-router';
import {LOGIN_STATE} from '@/utils/localStoreItem';
import UserConstant from '@/model/user/constant';
import {encrypt} from './encrypt';

const { t } = i18n.global as any;
const isProdEnv = import.meta.env.PROD;
const baseURL = (!isProdEnv && !mockConfig.useMock) ? mockConfig.baseURL : '';
const Error_Code = {
    networkError: -10000,
    timeoutError: -10001,
    serverError: -10002
};

// todo 代理配置
const defaultConfig = {
    baseURL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json;charset=UTF-8'
    },
    withCredentials: true
};
const axiosInstance = axios.create(defaultConfig);

const encryptUrls = [
    UserConstant.url.login,
    UserConstant.url.register,
    UserConstant.url.updateUserInfo,
    UserConstant.url.checkPassword
];

// 添加请求拦截
axiosInstance.interceptors.request.use(
    config => {
        if(encryptUrls.includes(<string>config.url)) {
            config.headers['Content-Type'] = 'text/plain;charset=UTF-8';
            config.data = encrypt(config.data);
        }
        return config;
    },
    error => {
        return Promise.reject({
            message: error.message
        });
    }
);

// 添加响应拦截器
axiosInstance.interceptors.response.use(
    response => {
        
        return response;
    },
    error => {
        const router = useRouter();
        if (error && error.response) {
            // 1.公共错误处理
            // 2.根据响应码具体处理
            switch (error.response.status) {
            case 401:   // 客户端出错
                localStorage.removeItem(LOGIN_STATE);
                router.push('/login');
                break;
            case 500:
                error.errorCode = Error_Code.serverError; // 服务器端出错
                break;
            default:
                error.errorCode = Error_Code.networkError;
            }
        } else {
            // 超时处理
            if (JSON.stringify(error).includes('timeout')) {
                error.errorCode = Error_Code.timeoutError;
            } else {
                error.errorCode = Error_Code.networkError;
            }
        }
        ElMessage.error(t('errorCode.' + error.errorCode));
        return Promise.reject(error);
    }
);

function get(url, params = {}, config = null) {
    return new Promise((resolve, reject) => {
        if(config) Object.assign(defaultConfig, {params}, config);
        axiosInstance.get(url, config).then( res => {
            const errorCode = (res.data || {}).errorCode;
            if(errorCode === 0) resolve(res.data);
            else {
                ElMessage.error(t('errorCode.' + errorCode));
                reject(res.data);
            }
        }).catch(error => {
            reject(error);
        });
    });
}

function post(url, data = {}, config = null) {
    return new Promise((resolve, reject) => {
        if(config) Object.assign(defaultConfig, config);
        axiosInstance.post(url, data, config).then( res => {
            const errorCode = (res.data || {}).errorCode;
            if(errorCode === 0) resolve(res.data);
            else {
                ElMessage.error(t('errorCode.' + errorCode));
                reject(res.data);
            }
        }).catch( error => {
            reject(error);
        });
    });
}


export default {get, post, axiosInstance};