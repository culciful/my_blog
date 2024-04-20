<template>
<div class="a-full">
    <custom-header hide-search></custom-header>
    <main class="a-full a-pt-header a-bg-gray">
        <router-view />
    </main>
</div>
</template>

<script lang="ts" setup name="User">
import CustomHeader from '@/components/customHeader/index.vue';
</script>

<style scoped lang="scss">

</style>