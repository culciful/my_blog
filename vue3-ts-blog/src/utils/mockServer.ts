import fs from 'fs';
import Mock from 'mockjs';
import path from 'path';

// 读取json文件
function getJsonFile(filePath) {
    // 读取指定json文件
    const json = fs.readFileSync(path.join(__dirname,  '../mock' + filePath + '.json'), 'utf-8');
    // 解析并返回
    return JSON.parse(json);
}

const jsonContentType = 'application/json;charset=UTF-8';
export const mockServer = () => ({
    name: 'configure-server',
    configureServer(server) {
        server.middlewares.use((req, res, next) => {
            // 自定义请求处理...
            const type = req.headers['content-type'];
            if(type === jsonContentType) {
                const pathname = req.url.split('?')[0];
                const json = getJsonFile(pathname); // mock数据
                // 将json传入 Mock.mock 方法中，生成的数据返回给浏览器
                res.writeHead(200, {'content-type': jsonContentType})
                    .end(JSON.stringify(Mock.mock(json)));
            } else {
                next();
            }
        });
    },
});