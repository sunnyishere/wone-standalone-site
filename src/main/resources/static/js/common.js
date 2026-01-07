$(function () {
    $(document).on('click', '#search-button', function (event) {
        event.preventDefault();
        event.stopPropagation();

        const searchInput = $('.search-input');
        const searchTerm = searchInput.val().trim().replace(/\s+/g, '-');
        if (!searchTerm) {
            alert('Please enter keywords');
            searchInput.focus();
            return;
        }

        const encodedTerm = encodeURIComponent(searchTerm);
        const targetUrl = `/search-${encodedTerm}`;

        console.log(`搜索关键词: ${searchTerm}`);
        console.log(`跳转URL: ${targetUrl}`);

        window.location.href = targetUrl;

        searchInput.val('').attr('placeholder', 'Search Products...');
        setTimeout(() => {
            searchInput.attr('placeholder', 'Search Products...');
        }, 1000);
    });

    $(document).on('keypress', '.search-input', function (e) {
        if (e.key === 'Enter') {
            console.log('【输入框回车】事件触发');
            e.preventDefault();
            $('#search-button').trigger('click');
        }
    });
    $(document).on('click', 'a[data-trigger="modal-1"], div[data-trigger="modal-1"]', function (e) {
        e.preventDefault();
        // 获取传递的bizId值
        const bizId = $(this).data('biz-id');
        // 设置到隐藏字段
        $('#input-biz-id').val(bizId);
        // 在表单中显示（可选）
        $('#param-value').text(bizId);
    });
});

<!--   rfq 提交 -->

jQuery(function ($) {
    function showError($input, errorMsg) {
        const $error = $('<div class="form-error" style="color: red; margin-top: 5px; font-size: 12px;">' + errorMsg + '</div>');
        $input.closest('.am-form-group').append($error);
    }

    function showNativeMsg(content) {
        const oldMsg = document.getElementById('native-msg');
        if (oldMsg) oldMsg.remove();

        const msgElem = document.createElement('div');
        msgElem.id = 'native-msg';
        msgElem.style.cssText = `
                position: fixed;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                padding: 12px 24px;
                background: rgba(0, 0, 0, 0.7);
                color: #fff;
                border-radius: 6px;
                font-size: 14px;
                z-index: 9999; /* 确保在最上层 */
                transition: opacity 0.3s ease;
            `;
        msgElem.textContent = content;

        document.body.appendChild(msgElem);

        setTimeout(() => {
            msgElem.style.opacity = '0';
            setTimeout(() => msgElem.remove(), 300);
        }, 2000);
    }

    $('#doc-modal-1 button[style*="background-color: var(--theme-wone)"]').on('click', function () {
        $('.form-error').remove();

        // 获取表单数据
        const formData = {
            bizId: $('#input-biz-id').val(),
            name: $('#input-name').val(),
            phone: $('#input-phone').val(),
            email: $('#input-email').val(),
            country: $('#input-country').val(),
            message: $('#input-message').val()
        };

        let hasError = false;

        if (!formData.name.trim()) {
            showError($('#input-name'), 'Please enter your name');
            hasError = true;
        }

        if (!formData.phone.trim()) {
            showError($('#input-phone'), 'Please enter your phone number');
            hasError = true;
        }

        if (!formData.email.trim()) {
            showError($('#input-email'), 'Please enter your email');
            hasError = true;
        } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
            showError($('#input-email'), 'Please enter a valid email address');
            hasError = true;
        }

        if (!formData.country.trim()) {
            showError($('#input-country'), 'Please enter your country');
            hasError = true;
        }

        if (!formData.message.trim()) {
            showError($('#input-message'), 'Please enter your message');
            hasError = true;
        }

        if (hasError) {
            return;
        }

        const xhr = new XMLHttpRequest();
        xhr.open('POST', "/leaveMessage", true);
        xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');

        xhr.onload = function () {
            if (xhr.status >= 200 && xhr.status < 300) {
                try {
                    const result = JSON.parse(xhr.responseText);
                    if (result && result.code === 200) {
                        showNativeMsg("Submission successful!");
                        setTimeout(function () {
                            location.reload();
                        }, 500);
                    } else {
                        showNativeMsg(result?.msg || "Failed to submit the form. Please try again later.");
                    }
                } catch (e) {
                    showNativeMsg("Invalid response from server");
                }
            } else {
                showNativeMsg("Server error: " + xhr.statusText);
            }
        };

        xhr.onerror = function () {
            showNativeMsg("Network error, please try again.");
        };

        const formDataParams = Object.keys(formData).map(key =>
            encodeURIComponent(key) + '=' + encodeURIComponent(formData[key])
        ).join('&');

        xhr.send(formDataParams);
    });
});


const APPCONFIG = {
    android: 'https://play.google.com/store/apps/details?id=cn.iwone.pwuniapp',
    ios: 'https://apps.apple.com/zm/app/wone/id6503928704',
    scheme: 'iwone://chat?id=#(brand.id)'
};

function goConfirmAddr() {
    let {isAndroid} = judgePhoneType();
    window.location.href = !isAndroid ? APPCONFIG.ios : APPCONFIG.android;
}

function isWeiXin() {
    return /micromessenger/i.test(navigator.userAgent.toLowerCase()) || typeof navigator.wxuserAgent !== 'undefined'
}

function judgePhoneType() {
    let isAndroid = false, isIOS = false, isIOS9 = false, version,
        u = navigator.userAgent,
        ua = u.toLowerCase();
    //Android系统
    if (u.indexOf('Android') > -1 || u.indexOf('Linux') > -1) {   //android终端或者uc浏览器
        isAndroid = true
    }
    //ios
    if (ua.indexOf("like mac os x") > 0) {
        let regStr_saf = /os [\d._]*/gi;
        let verinfo = ua.match(regStr_saf);
        version = (verinfo + "").replace(/[^0-9|_.]/ig, "").replace(/_/ig, ".");
    }
    let version_str = version + "";
    // ios9以上
    if (version_str !== "undefined" && version_str.length > 0) {
        version = parseInt(version);
        if (version >= 8) {
            isIOS9 = true
        } else {
            isIOS = true
        }
    }
    return {isAndroid, isIOS, isIOS9};
}

function openApp(url, callback) {
    let {isAndroid, isIOS, isIOS9} = judgePhoneType();
    if (isWeiXin()) {
        alert("Please open it in the system's built-in browser.");
        return;
    }

    if (isAndroid || isIOS) {
        let hasApp = true, t = 1000,
            t1 = Date.now(),
            ifr = document.createElement("iframe");
        setTimeout(function () {
            if (!hasApp) {
                callback && callback()
            }
            document.body.removeChild(ifr);
        }, 2000);

        ifr.setAttribute('src', url);
        ifr.setAttribute('style', 'display:none');
        document.body.appendChild(ifr);

        setTimeout(function () { //启动app时间较长处理
            let t2 = Date.now();
            if (t2 - t1 < t + 100) {
                hasApp = false;
            }
        }, t);
    }
    if (isIOS9) {
        location.href = url;
        setTimeout(function () {
            callback && callback()
        }, 250);
        setTimeout(function () {
            location.reload();
        }, 1000);
    }
}

function isMobile() {
    const ua = navigator.userAgent;
    return /Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(ua);
}

jQuery(function () {
    $(".vivo").click(function () {
        layer.tips("Search by using 'wone' in the app store!", $(this), {tips: [1, '#07C160 ']});
    });
    $(".oppo").click(function () {
        layer.tips("Please anticipate!", $(this), {tips: [1, '#07C160 ']});
    });
    $(".palm").click(function () {
        layer.tips("Search by using 'wone' in the app store!", $(this), {tips: [1, '#07C160 ']});
    });

    $("#chatBtn").click(function () {
        openApp(CONFIG.scheme, goConfirmAddr);
    });

    $(document).on('click', '.chat-btn', function () {
        const details = $(this).data('details');

        if (isMobile()) {
            const schemeUrl = 'iwone://c2cChat?id=' + encodeURIComponent(details);
            openApp(schemeUrl, goConfirmAddr);
        } else {
            $('#doc-modal-2').modal('open');

        }
    });
});

/**
 * phone
 */
document.addEventListener('DOMContentLoaded', function() {
    const langDropdown = document.querySelector('.lang-dropdown');
    const sidebar = document.getElementById('r-nav');
    const status = document.getElementById('status');

    // 语言下拉菜单切换
    langDropdown.addEventListener('click', function(e) {
        e.stopPropagation();
        langDropdown.classList.toggle('active');
    });

    // 点击侧边栏外部区域关闭下拉菜单
    document.addEventListener('click', function(e) {
        if (!langDropdown.contains(e.target)) {
            langDropdown.classList.remove('active');
        }
    });



    // 语言选项点击事件
    const langOptions = document.querySelectorAll('.lang-list a');
    langOptions.forEach(option => {
        option.addEventListener('click', function(e) {
            e.preventDefault();
            const selectedLang = this.textContent;
            document.querySelector('.lang-trigger .am-padding-left-0').textContent = selectedLang;

            // 更新激活状态
            langOptions.forEach(opt => opt.classList.remove('am-active'));
            this.classList.add('am-active');

            // 关闭下拉菜单
            langDropdown.classList.remove('active');
        });
    });
});


