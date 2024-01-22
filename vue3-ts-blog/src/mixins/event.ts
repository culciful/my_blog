import { onMounted, onUnmounted } from 'vue';
import * as _ from 'lodash';
import {useWindowStore} from '@/stores/window';

// 按照惯例，组合式函数名以“use”开头
function useOnResize() {
    const windowStore = useWindowStore();

    windowStore.setScreenSize(window.innerWidth, window.innerHeight);
    function update(event) {
        windowStore.setScreenSize(event.target.innerWidth, event.target.innerHeight);
    }
    useEventListener(window, 'resize', _.debounce((event) =>{
        update(event);
    }, 400));
}

function useEventListener(target, event, callback) {
    // 如果你想的话，
    // 也可以用字符串形式的 CSS 选择器来寻找目标 DOM 元素
    onMounted(() => target.addEventListener(event, callback));
    onUnmounted(() => target.removeEventListener(event, callback));
}

export {
    useOnResize,
    useEventListener
};