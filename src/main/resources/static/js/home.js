
document.querySelectorAll('.am-slider-images').forEach(function (slider) {
    slider.addEventListener('click', function () {
        const link = this.querySelector('.banner-link');
        if (link && link.href) {
            window.location.href = link.href;
        }
    });
});
function loadImages(container) {
    const images = container.querySelectorAll('img[data-src]');
    if (images.length === 0) return;
    images.forEach(img => {
        const realSrc = img.getAttribute('data-src');
        if (realSrc && img.src.includes('/img/space.png')) {
            img.src = realSrc;
            img.removeAttribute('data-src');
        }
    });
}
document.addEventListener('DOMContentLoaded', function () {
    const topNavPageName = document.getElementById('topNavPageName').value || '';
    const cateItems = document.querySelectorAll('.category li');
    const allViewAllBtns = document.querySelectorAll('.view-all-btn');

    cateItems.forEach(cateItem => {
        cateItem.addEventListener('click', function () {
            cateItems.forEach(item => item.classList.remove('pc-active'));
            this.classList.add('pc-active');
            const tabIndex = this.getAttribute('data-tab-index')
            if (tabIndex){
                const targetCont = document.querySelector(`.cont[data-content-index="${tabIndex}"]`);
                requestAnimationFrame(() => {
                    loadImages(targetCont)
                });
            }
        });
    });

    allViewAllBtns.forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            const targetCateId = this.getAttribute('data-cate-id');
            const targetCateName = this.getAttribute('data-cate-name');
            const targetLangUrl = this.getAttribute('data-lang-url');

            const baseUrl = topNavPageName ? `/${topNavPageName}` : '';
            const jumpUrl = `${baseUrl}/${targetCateName}-${targetCateId}`;
            const finalUrl = jumpUrl.replace(/\/+/g, '/');

            window.location.href = targetLangUrl + finalUrl;
        });
    });

    const allCateItem = document.querySelector('.category li:first-child');
    if (allCateItem) {
        allCateItem.classList.add('pc-active');
    }
});