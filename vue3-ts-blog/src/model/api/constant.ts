const moduleName = 'api';
const makeUrl = (str) => {
    return `/${moduleName}/${str}`;
};

const urlMap = {
    getTags: 'getTags',
    getConf: 'getConf',
};

export default {
    url: {
        getTags: makeUrl(urlMap.getTags),
        getConf: makeUrl(urlMap.getConf),
    },
};