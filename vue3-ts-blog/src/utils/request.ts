import axios, { type AxiosRequestConfig } from 'axios';
import i18n from '@/language/i18n';
import {ElMessage} from 'element-plus';
import {LOGIN_STATE} from '@/utils/localStoreItem';
import ApiConstant from '@/model/api/constant';
import UserConstant from '@/model/user/constant';
import {encrypt} from './encrypt';

const { t } = i18n.global as any;
const isProdEnv = import.meta.env.PROD;
const useMock = import.meta.env.VITE_USE_MOCK === 'true';
const envApiBase = (import.meta.env.VITE_API_BASE || '').replace(/\/+$/, '');
/** 开发+mock：相对路径；开发+真接口：VITE_API_BASE 或空串走代理；生产：VITE_API_BASE 或同源 */
const baseURL = isProdEnv ? envApiBase : useMock ? '' : envApiBase;
const Error_Code = {
    networkError: -10000,
    timeoutError: -10001,
    serverError: -10002,
    loginValidError: -10004
};

/** 代理目标见 vite.config 中 loadEnv 的 VITE_PROXY_TARGET；API 根见 VITE_API_BASE */
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
    ApiConstant.url.authLogin,
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
        if (error && error.response) {
            // 1.公共错误处理
            // 2.根据响应码具体处理
            switch (error.response.status) {
            case 401:
                error.errorCode = Error_Code.loginValidError;
                localStorage.removeItem(LOGIN_STATE);
                window.location.href = '/login';
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

function get(
    url: string,
    params: Record<string, unknown> = {},
    config: AxiosRequestConfig | null = null
  ) {
    return new Promise((resolve, reject) => {
      const mergedParams = { ...(config?.params ?? {}), ...params };
      const axiosConfig: AxiosRequestConfig = { ...(config ?? {}), params: mergedParams };
  
      axiosInstance.get(url, axiosConfig).then((res) => {
        const errorCode = (res.data || {}).errorCode;
        if (errorCode === 0) resolve(res.data);
        else {
          ElMessage.error(t('errorCode.' + errorCode));
          reject(res.data);
        }
      }).catch(error => reject(error));
    });
  }

function post(
    url: string,
    data: unknown = {},
    config: AxiosRequestConfig | null = null
) {
    return new Promise((resolve, reject) => {
        axiosInstance.post(url, data, config ?? undefined).then( res => {
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

function put(
    url: string,
    data: unknown = {},
    config: AxiosRequestConfig | null = null
) {
    return new Promise((resolve, reject) => {
        axiosInstance.put(url, data, config ?? undefined).then(res => {
            const errorCode = (res.data || {}).errorCode;
            if (errorCode === 0) resolve(res.data);
            else {
                ElMessage.error(t('errorCode.' + errorCode));
                reject(res.data);
            }
        }).catch(error => {
            reject(error);
        });
    });
}

function patch(
    url: string,
    data: unknown = {},
    config: AxiosRequestConfig | null = null
) {
    return new Promise((resolve, reject) => {
        axiosInstance.patch(url, data, config ?? undefined).then(res => {
            const errorCode = (res.data || {}).errorCode;
            if (errorCode === 0) resolve(res.data);
            else {
                ElMessage.error(t('errorCode.' + errorCode));
                reject(res.data);
            }
        }).catch(error => {
            reject(error);
        });
    });
}

function del(
    url: string,
    config: AxiosRequestConfig | null = null
) {
    return new Promise((resolve, reject) => {
        axiosInstance.delete(url, config ?? undefined).then(res => {
            const errorCode = (res.data || {}).errorCode;
            if (errorCode === 0) resolve(res.data);
            else {
                ElMessage.error(t('errorCode.' + errorCode));
                reject(res.data);
            }
        }).catch(error => {
            reject(error);
        });
    });
}

export default {get, post, put, patch, delete: del, axiosInstance};
