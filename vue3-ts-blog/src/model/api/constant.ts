/** 仅泛用配置与认证走 /api；业务模块见各 model 下的 constant。 */
const moduleName = 'api';
const authModuleName = 'auth';

const makeAuthUrl = (str: string) => {
    return `/${moduleName}/${authModuleName}/${str}`;
};

const urlMap = {
    authLogin: 'login',
    authLogout: 'logout'
};

export default {
    url: {
        authLogin: makeAuthUrl(urlMap.authLogin),
        authLogout: makeAuthUrl(urlMap.authLogout)
    }
};
