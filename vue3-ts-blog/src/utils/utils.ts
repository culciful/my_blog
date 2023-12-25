/**
 * param 将要转为URL参数字符串的对象
 * key URL参数字符串的前缀
 * encode true/false 是否进行URL编码,默认为true
 * return URL参数字符串
 */
export const urlEncode = function (param, key, encode) {
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
