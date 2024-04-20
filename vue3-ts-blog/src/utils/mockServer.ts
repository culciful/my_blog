import fs from 'fs';
import Mock from 'mockjs';
import path from 'path';

// 读取json文件
function getJsonFile(filePath) {
    const fullPath = path.join(__dirname,  '../mock' + filePath + '.json');
    try {
        // 读取指定json文件
        const json = fs.readFileSync(fullPath, 'utf-8');
        // 解析并返回
        return JSON.parse(json);
    } catch (e) {
        return {errorCode: 0, msg: '未找到json文件, 或json解析失败\n' + fullPath};
    }
}

const jsonContentType = /(application\/json|text\/plain)/;
export const mockServer = () => ({
    name: 'configure-server',
    configureServer(server) {
        server.middlewares.use((req, res, next) => {
            // 自定义请求处理...
            const type = req.headers['accept'];
            if(jsonContentType.test(type)) {
                const pathname = req.url.split('?')[0];
                const json = getJsonFile(pathname);
                // 将json传入 Mock.mock 方法中，生成的数据返回给浏览器
                res.writeHead(200, {'content-type': 'application/json;charset=UTF-8'})
                    .end(JSON.stringify(Mock.mock(json)));
            } else {
                next();
            }
        });
    }
});