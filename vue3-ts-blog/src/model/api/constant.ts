/** 仅泛用配置与认证走 /api；业务模块见各 model 下的 constant。 */
const moduleName = 'api';
const authModuleName = 'auth';

const makeUrl = (str: string) => {
    return `/${moduleName}/${str}`;
};
const makeAuthUrl = (str: string) => {
    return `/${moduleName}/${authModuleName}/${str}`;
};

const urlMap = {
    getConf: 'getConf',
    authLogin: 'login',
    authLogout: 'logout'
};

export default {
    url: {
        getConf: makeUrl(urlMap.getConf),
        authLogin: makeAuthUrl(urlMap.authLogin),
        authLogout: makeAuthUrl(urlMap.authLogout)
    }
};
