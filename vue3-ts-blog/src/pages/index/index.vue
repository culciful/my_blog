<template>
<div class="a-full">
    <custom-header @keyword-change="handleKeywordChange"></custom-header>
    <main class="a-full a-bg-gray a-pt-header a-p-h-10">
        <div class="main-container">
            <!-- 正在按标签过滤时显示当前标签，点 × 清除（标签只能从文章页点进来，首页本身没有别的入口） -->
            <div v-if="tag" class="tag-filter a-mt-md">
                <span class="a-font-body-1 a-mr-xs">{{$t('label.tag') + ': '}}</span>
                <el-tag closable @close="clearTag">{{tag}}</el-tag>
            </div>
            <article-list :keyword="keyword" :tag="tag" is-page-in-query></article-list>
        </div>
    </main>
</div>
</template>

<script lang="ts" setup>
import CustomHeader from '@/components/customHeader/index.vue';
import ArticleList from '@/pages/article/components/articleList.vue';
import {ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';

const route = useRoute();
const router = useRouter();

const queryString = (val: unknown): string => {
    const s = Array.isArray(val) ? val[0] : val;
    return typeof s === 'string' ? s : '';
};

// setup 里同步取，不能等 onMounted：子组件 articleList 的 onMounted 比这里先跑，
// 之后再改 keyword/tag 会触发它的 watch，把从 URL 还原出来的页码重置回 1
const keyword = ref(queryString(route.query.keyword));
const tag = ref(queryString(route.query.tag));

// 搜索词也写进 URL（同时清掉页码）：否则「搜索 → 点进文章 → 返回」页码是还原了，
// 搜索词却丢了，列表变成没过滤的第 N 页
const handleKeywordChange = (val: string) => {
    keyword.value = val;
    router.replace({ query: { ...route.query, keyword: val || undefined, page: undefined } });
};

const clearTag = () => {
    tag.value = '';
    router.replace({ query: { ...route.query, tag: undefined, page: undefined } });
};

</script>

<style scoped lang="scss">
// a-full 是 height:100%，内容超过一屏时 <main> 的灰底只到一屏高，下面露出白色 body
// 改成 min-height，让灰底随内容撑满
main {
    height: auto;
    min-height: 100%;
}
.main-container {
    margin: 24px auto;
    padding: 8px 24px 16px;
    max-width: 1000px;
    background: $--color-white;
}
</style>
