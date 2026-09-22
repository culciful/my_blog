// 关键路径 e2e：登录 → 发文 → 评论 → 关注。
// 跑在真实后端上（不是 mock），用项目文档里记录的种子账号
// testuser/Test1234（本地运行与联调.md）。默认视口太窄时用户主页的
// 关注按钮在侧栏里不渲染（`.aside` 响应式隐藏），所以这里显式给一个宽视口。
describe('关键路径：登录 → 发文 → 评论 → 关注', () => {
    beforeEach(() => {
        cy.viewport(1400, 900);
    });

    it('走完整个流程', () => {
        // ---------- 1. 登录 ----------
        cy.visit('/login');
        cy.get('input[placeholder="用户名或邮箱"]').type('testuser');
        cy.get('input[placeholder="密码"]').type('Test1234');
        cy.contains('button', '登录').click();
        cy.url().should('eq', Cypress.config().baseUrl + '/');

        // ---------- 2. 发文 ----------
        const title = `e2e-${Date.now()}`;
        const body = `e2e 正文内容 ${Date.now()}`;
        cy.visit('/write');
        cy.contains('.el-form-item', '标题').find('input').type(title);
        cy.get('textarea[placeholder="请使用markdown语法"]').type(body, { force: true });
        cy.contains('button', '发布').click();
        // 发布成功后跳到 /article/:aid，标题应该出现在详情页上
        cy.url().should('match', /\/article\/\d+$/);
        cy.contains(title);

        // ---------- 3. 评论 ----------
        const comment = `e2e-comment-${Date.now()}`;
        // 评论区顶部的新评论框是唯一一个此时可见的 el-textarea（每条已有评论的"回复"框
        // 要点了"回复"才会渲染，不会跟这个撞）
        cy.get('.el-textarea__inner:visible').first().type(comment);
        cy.contains('评论列表')
            .parent()
            .within(() => {
                cy.contains('button', '发布').click();
            });
        cy.contains(comment);

        // ---------- 4. 关注 ----------
        // testuser2 是项目文档里记录的另一个种子账号，id 直接查库拿的
        // （本地运行与联调.md：testuser/testuser2 默认互相关注）
        // 不假定 testuser 当前是否已关注 testuser2（种子数据是互相关注，但可能被之前跑过的
        // e2e/手工测试改动过）——先读一次真实状态，点一下，断言状态确实翻转了。不做"点两次
        // 恢复原状"：两次网络请求之间有竞态（第二次点击可能在第一次的响应/重渲染完成前打出
        // 去，命中过期的 DOM），退回去反而让测试变脆；关注关系是可重复跑的开发库数据，没必要
        // 强求恢复。
        const testuser2Id = '353900534664663040';
        cy.intercept('GET', '**/user/checkHasFollow*').as('checkFollow');
        cy.intercept('POST', '**/user/switchFollow').as('switchFollow');
        cy.visit(`/user?id=${testuser2Id}`);
        // 关键：先等 checkHasFollow 落地，再去读按钮文字。这个按钮挂载时先渲染默认态
        // "关注"（isFollowing 初始值 false），要等 checkHasFollow 的响应回来才会变成真实值——
        // cy.contains('button', /关注$/) 光凭文字匹配，这个默认态一样能匹配上，不会等到真实
        // 状态；不显式等这个请求，读到的经常是"关注"这个默认态而不是数据库里的真实关注状态
        // （之前就是踩在这——没等 checkFollow，导致 wasFollowing 读出来的值时对时错）。
        cy.wait('@checkFollow');
        // 按钮文字前面带个 ✓ 图标字符（"已关注"态才有），所以用"以关注结尾"匹配，
        // 不锚定开头；用是否含"已"判断当前状态
        cy.contains('button', /关注$/).then($btn => {
            const wasFollowing = $btn.text().includes('已');
            cy.wrap($btn).click();
            cy.wait('@switchFollow').its('response.body.errorCode').should('eq', 0);
            cy.contains('button', /关注$/, { timeout: 6000 })
                .should($after => {
                    expect($after.text().includes('已')).to.equal(!wasFollowing);
                });
        });
    });
});
