<template>
<i v-bind="$attrs" :style="{ color: color, fontSize: addUnit(size)}" :class="svgClass">
    <svg class="svg-icon">
        <use :href="iconName"></use>
    </svg>
</i>
</template>

<script setup lang="ts" name="SvgIcon">
import { computed } from 'vue';
import {addUnit} from '@/utils/utils';
const props = defineProps({
    name: {
        type: String,
        required: true,
    },
    color: {
        type: String,
        default: '',
    },
    size: {
        type: [Number, String],
    },
});

const iconName = computed(() => `#icon-${props.name}`);
const svgClass = computed(() => `icon-${props.name}`);
</script>

<style scoped>
i {
    display: inline-flex;
    align-items: center;
}
.svg-icon {
    width: 1em;
    height: 1em;
}
</style>