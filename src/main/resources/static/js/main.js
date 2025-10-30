// main.js
(function($) {
    // 图片懒加载初始化
    function initLazyLoad() {
        if (typeof echo !== 'undefined') {
            echo.init({
                offset: 100,
                throttle: 250,
                unload: false,
                callback: function(element, op) {
                    console.log(element, 'has been', op + 'ed');
                }
            });
        }
    }

    // 选项卡切换功能
    function initTabs() {
        $('.category ul li').click(function() {
            var indexC = $(this).index();
            $(this).addClass('pc-active').siblings().removeClass('pc-active');
            $('.cont').eq(indexC).addClass('pc-active').siblings().removeClass('pc-active');
        });
    }

    // 分类滚动控制
    function initCategoryScroll() {
        var catew = $('.category').width() - 150;
        var cateLiw = 0;
        $('.category ul li').each(function() {
            cateLiw += $(this).outerWidth();
        });
        var i = 0;

        $('.next').click(function() {
            $('.category ul').animate({
                "margin-left": -catew + 'px'
            }, 500);
            i++;
            if ((catew + 150) * i + (catew + 150) >= cateLiw) {
                $('.prev').show();
                $('.next').hide();
            }
        });

        $('.prev').click(function() {
            $('.category ul').animate({
                "margin-left": 0 + 'px'
            }, 500);
            $(this).hide();
            $('.next').show();
        });
    }

    // 生成二维码
    function generateQRCode() {
        var str = "http://www.saitolia.com";
        $("#code").qrcode({
            render: "table",
            width: 100,
            height: 100,
            text: str
        });
    }

    // 返回顶部按钮
    function initBackToTop() {
        var $backTop = $('<div class="m-top-cbbfixed">' +
            '<a class="m-top-go m-top-cbbtn">' +
            '<span class="m-top-goicon"></span>' +
            '</a>' +
            '</div>');

        $('body').append($backTop);

        $backTop.click(function() {
            $("html, body").animate({
                scrollTop: 0
            }, 800);
        });

        var timer = null;
        $(window).scroll(function() {
            var d = $(document).scrollTop();
            if (d > 0) {
                $backTop.css("bottom", "10px");
            } else {
                $backTop.css("bottom", "-90px");
            }

            clearTimeout(timer);
            timer = setTimeout(function() {
                clearTimeout(timer);
            }, 100);
        });
    }

    // 页面加载完成后初始化所有功能
    $(document).ready(function() {
        initLazyLoad();
        initTabs();
        initCategoryScroll();
        generateQRCode();
        initBackToTop();
    });

})(jQuery);