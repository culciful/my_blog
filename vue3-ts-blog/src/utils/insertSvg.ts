import { readFileSync } from 'fs';

export const insertSvg = () => {
    const svg = readFileSync('./public/static/img/icons.svg');
    return {
        name: 'svg-transform',
        transformIndexHtml(dom: String) {
            return dom.replace(
                '<body>',
                `<body>${svg}`
            );
        }
    };
};
