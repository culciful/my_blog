const moduleName = 'api';
const makeUrl = (str) => {
    return `/${moduleName}/${str}`;
};

const urlMap = {
    getConf: 'getConf'
};

export default {
    url: {
        getConf: makeUrl(urlMap.getConf)
    }
};