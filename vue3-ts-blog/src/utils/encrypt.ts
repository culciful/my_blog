import request from '@/utils/request';
import Constant from '@/model/api/constant.ts';
import JSEncrypt from 'jsencrypt';

const templateKey = '-----BEGIN PUBLIC KEY-----{key}-----END PUBLIC KEY-----';
const key = new JSEncrypt();
let hasKey = false;

function getKey() {
    return request.get(Constant.url.getConf).then(res => {
        hasKey = true;
        key.setPublicKey(templateKey.replace('{key}', res.result.data));
    });
}

function encrypt(data) {
    return key.encrypt(JSON.stringify(data));
}

export {hasKey, getKey, encrypt};