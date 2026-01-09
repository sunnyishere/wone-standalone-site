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

$(document).ready(function () {
    $('.am-direction-nav .am-prev').append('<span style="display:none;">Previous Slide</span>');
    $('.am-direction-nav .am-next').append('<span style="display:none;">Next Slide</span>');
    $('.am-direction-nav .am-prev').attr('aria-label', 'Previous Page');
    $('.am-direction-nav .am-next').attr('aria-label', 'Next Page');
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