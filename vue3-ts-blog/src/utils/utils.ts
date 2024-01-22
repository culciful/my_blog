/**
 * param 将要转为URL参数字符串的对象
 * key URL参数字符串的前缀
 * encode true/false 是否进行URL编码,默认为true
 * return URL参数字符串
 */

const urlEncode = function (param, key, encode) {
    if ( param === null) return '';
    let paramStr = '';
    const t = typeof (param);
    if (t === 'string' || t === 'number' || t === 'boolean') {
        paramStr += '&' + key + '=' + ((encode === null || encode) ? encodeURIComponent(param) : param);
    } else {
        for (const i in param) {
            const k = key === null ? i : key + (param instanceof Array ? '[' + i + ']' : '.' + i);
            paramStr += urlEncode(param[i], k, encode);
        }
    }
    return paramStr;
};

function addUnit(value, defaultUnit = 'px') {
    if (!value)
        return '';
    if (isNumber(value) || isStringNumber(value)) {
        return `${value}${defaultUnit}`;
    } else if (isString(value)) {
        return value;
    }
}

const isNumber = (val) => typeof val === 'number';
const isString = (val) => typeof val === 'string';

const isStringNumber = (val) => {
    if (!isString(val)) {
        return false;
    }
    return !Number.isNaN(Number(val));
};

const transferTimestamp = (val: number, formatVal: string = 'yyyy-MM-dd hh:mm') => {
    return new Date(val * 1000).format(formatVal);
};

const setReactiveData = (to, from) => {
    if(from == null) return;
    const props = Object.keys(from);
    props.forEach(prop => {
        to[prop] = from[prop];
    });
};

const deepCopy = (source) => {
    if(typeof source !== 'object' || source == null) {
        return source;
    }
    const result = Array.isArray(source) ? [] : {};
    for(const key in source) {
        result[key] = deepCopy(source[key]);
    }
    return result;
};

export {
    urlEncode,
    isNumber,
    isString,
    isStringNumber,
    addUnit,
    transferTimestamp,
    setReactiveData,
    deepCopy
};