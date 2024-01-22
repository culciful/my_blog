const fs = require('fs');

let idPrefix = '';
const svgTitle = /<svg([^>]*)>/;
const clearHeightWidth = / (width|height)=['"]([^'">]*)['"]/g;
const clearVersion = / version=['"][^'">]*['"]/;
const resetColor = /fill=['"]([^'">]*)['"]/;
const hasViewBox = /(viewBox=['"][^'">]*['"])/;
const clearReturn = /(\r)|(\n)/g;

// 查找svg文件
function svgFind(e) {
    const arr = [];
    const dirents = fs.readdirSync(e, { withFileTypes: true });
    for (const dirent of dirents) {
        if (dirent.isDirectory()) arr.push(...svgFind(e + dirent.name + '/'));
        else {
            const svg = fs.readFileSync(e + dirent.name)
                .toString()
                .replace(clearReturn, '')
                .replace(clearVersion, '')
                .replace(resetColor, ($1, $2) => {
                    if($2 === 'none') return $1;
                    return 'fill="currentColor"';
                })
                .replace(svgTitle, ($1, $2) => {

                    let width = 0,
                        height = 0,
                        content = $2.replace(clearHeightWidth, (s1, s2, s3) => {
                            if (s2 === 'width') width = s3;
                            else if (s2 === 'height') height = s3;
                            return '';
                        });

                    if (!hasViewBox.test(content)) {
                        content += ` viewBox="0 0 ${width} ${height}"`;
                    }
                    return `<symbol id="${idPrefix}-${dirent.name.replace('.svg', '')}"${content}>`;
                }).replace('</svg>', '</symbol>');
            arr.push(svg);
        }
    }
    return arr;
}

// 生成svg
const createSvg = (path, prefix = 'icon') => {
    if (path === '') return;
    idPrefix = prefix;
    const res = svgFind(path);
    const content = `<svg xmlns="http://www.w3.org/2000/svg" style="position: absolute; width: 0; height: 0">\n\t${res.join('\n\t')}\n</svg>`
    fs.writeFileSync('./public/static/img/icons.svg', content);
    console.log('成功生成svg文件！')
};

const iconsDir = './src/assets/img/icons/';
createSvg(iconsDir);
