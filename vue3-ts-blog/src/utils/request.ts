import axios from 'axios';
import Mock from 'mockjs';
import { readFileSync, existsSync } from 'fs';

const isProdEnv = import.meta.env.PROD;
const baseURL = isProdEnv
    ? 'https://production.com'
    : '';
const defaultConfig = {
    baseURL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json;charset=UTF-8',
    }
 };
const axiosInstance = axios.create(defaultConfig);

// 添加请求拦截
axiosInstance.interceptors.request.use(
    config => {
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
        return Promise.reject({
            message: error.message
        });
    }
);

function mockData(url) {

    return {errorCode: -12345};
}

function get(url, params = {}, config) {
    return new Promise((resolve, reject) => {
        console.log('GET', url, params);
        if(config) Object.assign(defaultConfig, {params}, config);
        axiosInstance.get(url, config).then( res => {
            const errorCode = (res.data || {}).errorCode;
            if(errorCode === 0) resolve(res.data);
            else reject(res.data);
        }).catch(error => {
            reject(error);
        });
    });
}

function post(url, data = {}, config) {
    return new Promise((resolve, reject) => {
        if(config) Object.assign(defaultConfig, config);
        if(!isProdEnv) resolve(mockData(url));
        axiosInstance.post(url, data, config).then( res => {
            const errorCode = (res.data || {}).errorCode;
            if(errorCode === 0) resolve(res.data);
            else reject(res.data);
        }).catch( error => {
            reject(error);
        });
    });
}


export default {get, post};