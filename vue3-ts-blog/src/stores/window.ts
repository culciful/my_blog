import { defineStore } from 'pinia';
import {ref} from 'vue';

export const useWindowStore = defineStore('windowProps', () => {
    const screenWidth = ref(0);
    const screenHeight = ref(0);

    function setScreenSize(width, height) {
        screenWidth.value = width;
        screenHeight.value = height;
    }

    return {screenWidth, screenHeight, setScreenSize};
});