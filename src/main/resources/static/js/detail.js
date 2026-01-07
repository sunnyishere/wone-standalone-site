// 全局变量存储 CKEditor 实例，避免重复创建
let editorInstance = null;

// 初始化函数（确保只执行一次）
async function initCKEditor() {
    // 如果已有实例，先销毁
    if (editorInstance) {
        await editorInstance.destroy();
    }

    try {
        editorInstance = await ClassicEditor.create(document.querySelector('#editor'), {
            readOnly: true,
            toolbar: [],
            plugins: [
                'HorizontalRule',
                'GeneralHtmlSupport'
            ],
            htmlSupport: {
                allow: [{
                    name: /.*/,
                    attributes: true,
                    classes: true,
                    styles: true,
                    allowEmpty: true
                }]
            }
        });
        console.log('CKEditor 初始化成功');
    } catch (error) {
        // 只忽略「插件未找到」的误报，其他错误正常打印
        if (error.name === 'CKEditorError') {
            if (error.message.includes('plugin-not-found')) {
                console.log('忽略插件声明警告（功能正常）：', error.plugin);
            } else if (error.message.includes('duplicated-modules')) {
                console.error('CKEditor 模块重复加载，请检查脚本引入是否重复：', error);
            } else {
                console.error('CKEditor 初始化失败：', error);
            }
        }
    }
}

// 确保 DOM 加载完成后再初始化（避免找不到 #editor 元素）
document.addEventListener('DOMContentLoaded', initCKEditor);