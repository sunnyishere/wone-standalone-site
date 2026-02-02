document.addEventListener("DOMContentLoaded", function () {
    const videoModal = document.getElementById('videoModal');
    const closeButton = document.getElementById('closeVideo');
    const iframeContainer = document.getElementById('videoIframeContainer');
    const playButtonPC = document.getElementById('playVideoBtn');
    const playButtonMobile = document.getElementById('playVideoBtnMobile');

    function handlePlayVideo(button) {
        let iframeCode = button.getAttribute('data-video-iframe');
        if (!iframeCode) {
            alert('未获取到视频播放代码，请联系管理员');
            return;
        }

        iframeCode = iframeCode
            .replace(/&amp;/g, '&')
            .replace(/&lt;/g, '<')
            .replace(/&gt;/g, '>')
            .replace(/&quot;/g, '"')
            .replace(/&#39;/g, "'")
            .replace(/&nbsp;/g, ' ');

        if (!iframeCode.startsWith('<iframe') || !iframeCode.endsWith('</iframe>')) {
            console.error('data-video-iframe 中的值不是完整的 iframe 代码：', iframeCode);
            alert('视频播放代码格式错误');
            return;
        }

        iframeContainer.innerHTML = iframeCode;

        const iframe = iframeContainer.querySelector('iframe');
        if (iframe) {
            iframe.style.width = '100%';
            iframe.style.height = '100%';
            iframe.style.border = 'none';
            iframe.style.borderRadius = '8px';
            iframe.allow = "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share";
            iframe.allowFullScreen = true;
        }

        videoModal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    if (playButtonPC) {
        playButtonPC.addEventListener('click', function () {
            handlePlayVideo(this);
        });
    }

    if (playButtonMobile) {
        playButtonMobile.addEventListener('click', function () {
            handlePlayVideo(this);
        });
    }

    function closeVideoModal() {
        videoModal.style.display = 'none';
        iframeContainer.innerHTML = '';
        document.body.style.overflow = 'auto';
    }

    closeButton.addEventListener('click', closeVideoModal);

    videoModal.addEventListener('click', function (event) {
        if (event.target === videoModal) {
            closeVideoModal();
        }
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && videoModal.style.display === 'flex') {
            closeVideoModal();
        }
    });
});

function scrollToTarget(event) {
    event.preventDefault();
    var anchor = event.currentTarget;
    var targetId = anchor.getAttribute('href').substring(1);
    var offset = parseInt(anchor.dataset.offset, 10);
    var target = document.getElementById(targetId);
    window.scrollTo({
        top: target.offsetTop + offset,
        behavior: 'smooth'
    });
}

document.addEventListener('DOMContentLoaded', function() {
    // 查找所有符合条件的前一个按钮
    var prevButtons = document.querySelectorAll('.am-direction-nav .am-prev');
    // 查找所有符合条件的后一个按钮
    var nextButtons = document.querySelectorAll('.am-direction-nav .am-next');

    // 为前一个按钮添加内容
    prevButtons.forEach(function(button) {
        var span = document.createElement('span');
        span.textContent = 'Previous Slide';
        span.style.display = 'none';
        button.appendChild(span);
        button.setAttribute('aria-label', 'Previous Page');
    });

    // 为后一个按钮添加内容
    nextButtons.forEach(function(button) {
        var span = document.createElement('span');
        span.textContent = 'Next Slide';
        span.style.display = 'none';
        button.appendChild(span);
        button.setAttribute('aria-label', 'Next Page');
    });
});
const stickyBar = document.querySelector('.sticky-bar');
const bottomBlock = document.querySelector('.sticky-bottom');

function handleScroll() {
    const bottomBlockTop = bottomBlock.getBoundingClientRect().top;

    const stickyBarHeight = stickyBar.offsetHeight;

    if (bottomBlockTop <= stickyBarHeight) {
        stickyBar.style.top = `-${stickyBarHeight - bottomBlockTop}px`;
    } else {
        stickyBar.style.top = '0';
    }
}

handleScroll();
window.addEventListener('scroll', handleScroll);