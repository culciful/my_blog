import fs from 'fs';
import Mock from 'mockjs';
import path from 'path';

const mockRoot = path.join(__dirname, '../mock');

/**
 * 接口为 RPC 扁平命名（/模块/动词），路径不含 id；这里仍顺手滤掉纯数字段做兜底。
 * 例：/comment/getComments → ['comment','getComments']
 */
function segmentsWithoutNumericIds(pathname: string): string[] {
    const clean = (pathname.split('?')[0] || '/').replace(/\/+/g, '/').replace(/\/$/, '') || '/';
    return clean
        .split('/')
        .filter((s) => s.length > 0 && !/^\d+$/.test(s));
}

/** 磁盘路径：mock/<段1>/.../<段n-1>/<接口名>.json（最后一段作为文件名） */
function resolveMockAbsolutePath(pathname: string): string {
    const parts = segmentsWithoutNumericIds(pathname);
    if (parts.length === 0) {
        return path.join(mockRoot, 'index.json');
    }
    const dirs = parts.slice(0, -1);
    const name = parts[parts.length - 1];
    return path.join(mockRoot, ...dirs, `${name}.json`);
}

function loadMockJson(pathname: string) {
    const abs = resolveMockAbsolutePath(pathname);
    try {
        const raw = fs.readFileSync(abs, 'utf-8');
        return JSON.parse(raw);
    } catch {
        return {
            errorCode: 0,
            msg: `未找到 mock 文件：${abs}\n规则：去掉路径中的数字 id 段后，对应 mock/<路径>/<接口名>.json`
        };
    }
}

const jsonContentType = /(application\/json|text\/plain)/;
export const mockServer = () => ({
    name: 'configure-server',
    configureServer(server: import('vite').ViteDevServer) {
        server.middlewares.use((req, res, next) => {
            const type = req.headers['accept'];
            if (jsonContentType.test(String(type))) {
                const pathname = (req.url || '').split('?')[0];
                const json = loadMockJson(pathname);
                res.writeHead(200, {'content-type': 'application/json;charset=UTF-8'})
                    .end(JSON.stringify(Mock.mock(json)));
            } else {
                next();
            }
        });
    }
});
