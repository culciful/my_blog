import fs from 'fs';
import Mock from 'mockjs';
import path from 'path';

const mockRoot = path.join(__dirname, '../mock');

/**
 * 去掉 URL 路径里「整段为纯数字」的段（视作资源 id），其余段作为 mock 目录层级。
 * 例：/user/users/12/packages/3 → ['user','users','packages']
 */
function segmentsWithoutNumericIds(pathname: string): string[] {
    const clean = (pathname.split('?')[0] || '/').replace(/\/+/g, '/').replace(/\/$/, '') || '/';
    return clean
        .split('/')
        .filter((s) => s.length > 0 && !/^\d+$/.test(s));
}

/** 磁盘路径：mock/<段1>/<段2>/.../<method>.json */
function resolveMockAbsolutePath(method: string, pathname: string): string {
    const parts = segmentsWithoutNumericIds(pathname);
    const verb = (method || 'GET').toLowerCase();
    if (parts.length === 0) {
        return path.join(mockRoot, `${verb}.json`);
    }
    return path.join(mockRoot, ...parts, `${verb}.json`);
}

function loadMockJson(method: string, pathname: string) {
    const abs = resolveMockAbsolutePath(method, pathname);
    try {
        const raw = fs.readFileSync(abs, 'utf-8');
        return JSON.parse(raw);
    } catch {
        return {
            errorCode: 0,
            msg: `未找到 mock 文件：${abs}\n规则：去掉路径中的数字 id 段后，对应 mock/<路径>/<method>.json`
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
                const method = req.method || 'GET';
                const json = loadMockJson(method, pathname);
                res.writeHead(200, {'content-type': 'application/json;charset=UTF-8'})
                    .end(JSON.stringify(Mock.mock(json)));
            } else {
                next();
            }
        });
    }
});
