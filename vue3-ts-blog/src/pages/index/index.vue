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
import ArticleList from '@/pages/article/components/list.vue';
import {ref, onMounted} from 'vue';
import {  useRoute } from 'vue-router';

const route = useRoute();

const keyword = ref('');
const tag = ref('');

const handleKeywordChange = (val) => {
    keyword.value = val;
};

onMounted(() => {
    keyword.value = route.query?.keyword as string;
    tag.value = route.query?.tag as string;
});

</script>

<style scoped lang="scss">
.main-container {
    margin: 10px auto;
    padding: 8px 24px 16px;
    max-width: 1000px;
    background: $--color-white;
}
</style>
