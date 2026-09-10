<template>
<div class="a-full">
    <custom-header @keyword-change="handleKeywordChange"></custom-header>
    <main class="a-full a-bg-gray a-pt-header a-p-h-10">
        <div class="main-container">
            <article-list :keyword="keyword" :tag="tag"></article-list>
        </div>
    </main>
</div>
</template>

<script lang="ts" setup>
import CustomHeader from '@/components/customHeader/index.vue';
import ArticleList from '@/pages/article/components/articleList.vue';
import {ref, onMounted} from 'vue';
import {  useRoute } from 'vue-router';

const route = useRoute();

const keyword = ref('');
const tag = ref('');

const handleKeywordChange = (val) => {
    keyword.value = val;
};

onMounted(() => {
    keyword.value = (route.query?.keyword as string) ?? '';
    tag.value = (route.query?.tag as string) ?? '';
});

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
