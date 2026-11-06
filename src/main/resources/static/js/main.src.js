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
            '<div class="m-top-go m-top-cbbtn">' +
            '<span class="m-top-goicon"></span>' +
            '</div>' +
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
$(function () {
    $(document).on('click', '#search-button', function (event) {
        event.preventDefault();
        event.stopPropagation();

        const searchInput = $('.search-input');
        const searchTerm = searchInput.val().trim();
        if (!searchTerm) {
            alert('Please enter keywords');
            searchInput.focus();
            return;
        }

        const encodedTerm = encodeURIComponent(searchTerm);
        // 携带当前语种前缀（en 为空串，其他语种形如 /de）；
        // 隐藏域缺失时（旧缓存页面）按当前页 URL 首段解析语种前缀
        let langPrefix = ($('.search-lang').val() || '').trim();
        if (!langPrefix) {
            const firstSeg = window.location.pathname.split('/')[1] || '';
            langPrefix = /^[a-z]{2,3}$/.test(firstSeg) ? '/' + firstSeg : '';
        }
        langPrefix = langPrefix.replace(/\/+$/, '');
        // 搜索 URL 统一为查询参数形态，关键词原样编码不再折叠空格为短横线
        const targetUrl = `${langPrefix}/search?keyword=${encodedTerm}`;

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

document.addEventListener('DOMContentLoaded', function() {
    const themeColor = getComputedStyle(document.documentElement)
        .getPropertyValue('--theme-wone').trim() || '#2c6cb7';

    const form = document.querySelector('#doc-modal-1 #requestForm');
    if (!form) return;

    form.querySelectorAll('*').forEach(el => {
        el.style.outline = 'none';
    });

    form.querySelectorAll('input, textarea').forEach(el => {
        el.style.borderColor = '#d1d5db';
        el.style.transition = 'border-color 0.2s ease, box-shadow 0.2s ease';
        el.addEventListener('focus', function() {
            this.style.borderColor = themeColor;
            this.style.boxShadow = `0 0 0 3px ${hexToRgba(themeColor, 0.1)}`;
        });
        el.addEventListener('blur', function() {
            this.style.borderColor = '#d1d5db';
            this.style.boxShadow = 'none';
        });
    });

    const originalSelect = document.getElementById('input-country');
    if (originalSelect && !originalSelect.dataset.customized) {
        replaceCountrySelectWithIcon(originalSelect, themeColor);
    }
});

function hexToRgba(hex, alpha) {
    const h = hex.replace('#', '');
    if (h.length === 3) {
        return hexToRgba('#' + h.split('').map(c => c + c).join(''), alpha);
    }
    const r = parseInt(h.slice(0, 2), 16);
    const g = parseInt(h.slice(2, 4), 16);
    const b = parseInt(h.slice(4, 6), 16);
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

function replaceCountrySelectWithIcon(selectEl, themeColor) {
    selectEl.dataset.customized = 'true';

    const inputWithIcon = selectEl.closest('.input-with-icon');
    if (!inputWithIcon) {
        console.warn('未找到 .input-with-icon 容器，回退到原生 select');
        return;
    }

    selectEl.style.display = 'none';

    const displayBox = document.createElement('div');
    displayBox.tabIndex = 0;
    displayBox.style.position = 'relative';
    displayBox.style.width = '100%';
    displayBox.style.padding = '12px 12px 12px 32px';
    displayBox.style.border = '1px solid #d1d5db';
    displayBox.style.borderRadius = '8px';
    displayBox.style.backgroundColor = 'white';
    displayBox.style.cursor = 'pointer';
    displayBox.style.color = '#333';
    displayBox.style.fontFamily = 'inherit';
    displayBox.style.fontSize = '15px';
    displayBox.style.transition = 'border-color 0.2s ease, box-shadow 0.2s ease';
    displayBox.style.zIndex = '1';
    displayBox.style.textAlign = 'left';

    const arrowSvg = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#777" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>`;
    displayBox.style.backgroundImage = `url("data:image/svg+xml;charset=US-ASCII,${encodeURIComponent(arrowSvg)}")`;
    displayBox.style.backgroundRepeat = 'no-repeat';
    displayBox.style.backgroundPosition = 'right 12px center';
    displayBox.style.backgroundSize = '16px';
    displayBox.style.appearance = 'none';

    const textSpan = document.createElement('span');
    textSpan.textContent = '';
    displayBox.appendChild(textSpan);

    const dropdown = document.createElement('ul');
    Object.assign(dropdown.style, {
        position: 'absolute',
        top: 'calc(100% + 4px)',
        left: '0',
        right: '0',
        margin: '0',
        padding: '0',
        listStyle: 'none',
        backgroundColor: 'white',
        border: '1px solid #e5e7eb',
        borderRadius: '8px',
        boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)',
        zIndex: '1001',
        maxHeight: '200px',
        overflowY: 'auto',
        display: 'none'
    });

    function syncDisplay() {
        const selected = selectEl.options[selectEl.selectedIndex];
        textSpan.textContent = selected ? selected.text : '请选择';
        textSpan.style.color = '#333';
        textSpan.style.fontWeight = 'normal';
    }
    syncDisplay();

    Array.from(selectEl.options).forEach((option, index) => {
        const li = document.createElement('li');
        li.style.padding = '10px 12px';
        li.style.cursor = 'pointer';
        li.style.color = '#333';
        li.textContent = option.text;
        li.style.transition = 'background-color 0.2s ease, color 0.2s ease';

        if (index === selectEl.selectedIndex && option.value) {
            li.style.backgroundColor = themeColor;
            li.style.color = 'white';
            li.style.fontWeight = '600';
        }

        li.addEventListener('click', () => {
            Array.from(dropdown.querySelectorAll('li')).forEach(item => {
                item.style.backgroundColor = '';
                item.style.color = '#333';
                item.style.fontWeight = '';
            });
            li.style.backgroundColor = themeColor;
            li.style.color = 'white';
            li.style.fontWeight = '600';

            selectEl.selectedIndex = index;
            selectEl.dispatchEvent(new Event('change', { bubbles: true }));
            syncDisplay();
            dropdown.style.display = 'none';
            displayBox.focus();
        });

        li.addEventListener('mouseenter', () => {
            if (!li.style.backgroundColor) {
                li.style.backgroundColor = '#f3f4f6';
            }
        });

        li.addEventListener('mouseleave', () => {
            if (!li.style.backgroundColor) {
                li.style.backgroundColor = '';
            }
        });

        dropdown.appendChild(li);
    });

    displayBox.addEventListener('click', (e) => {
        e.stopPropagation();
        dropdown.style.display = dropdown.style.display === 'block' ? 'none' : 'block';
    });

    displayBox.addEventListener('focus', () => {
        displayBox.style.borderColor = themeColor;
        displayBox.style.boxShadow = `0 0 0 3px ${hexToRgba(themeColor, 0.1)}`;
        const focusedArrow = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="${themeColor}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>`;
        displayBox.style.backgroundImage = `url("data:image/svg+xml;charset=US-ASCII,${encodeURIComponent(focusedArrow)}")`;
    });

    displayBox.addEventListener('blur', () => {
        setTimeout(() => {
            dropdown.style.display = 'none';
            displayBox.style.borderColor = '#d1d5db';
            displayBox.style.boxShadow = 'none';
            displayBox.style.backgroundImage = `url("data:image/svg+xml;charset=US-ASCII,${encodeURIComponent(arrowSvg)}")`;
        }, 150);
    });

    document.addEventListener('click', () => {
        dropdown.style.display = 'none';
        displayBox.blur();
    });

    const inputIcon = inputWithIcon.querySelector('.input-icon');
    if (inputIcon) {
        inputIcon.style.zIndex = '1002';
        inputIcon.style.left = '12px';
        inputIcon.style.transform = 'translateY(-50%)';
        inputWithIcon.insertBefore(displayBox, inputIcon.nextSibling);
    } else {
        inputWithIcon.appendChild(displayBox);
    }
    inputWithIcon.appendChild(dropdown);
}


<!--leave msg -->

$(document).ready(function () {
    'use strict';
    $(document).on('click', 'a[data-trigger="modal-1"], div[data-trigger="modal-1"]', function (e) {
        $('.form-control').each(function() {
            clearError($(this));
        });
        e.preventDefault();
        const bizId = $(this).data('biz-id');
        $('#input-biz-id').val(bizId);
        $('#param-value').text(bizId);
        resetUploadUI();

        // 只在打开模态框时调用IP查询
        setCountryByIP();
    });

    const showError = ($input, errorMsg) => {
        $input.addClass('error');

        $input.css({
            'border-color': '#ef4444',
            'border': '1px solid #ef4444',
            'background-color': '#fef2f2'
        });

        $input.closest('.form-group').find('.form-error').remove();

        const $error = $('<div class="form-error" style="color: #ef4444; margin-top: 5px; font-size: 12px;">' + errorMsg + '</div>');
        $input.closest('.form-group').append($error);
    };

    const clearError = ($input) => {
        $input.removeClass('error');
        $input.closest('.form-group').find('.form-error').remove();

        $input.css({
            'border-color': '#d1d5db',
            'border': '1px solid #d1d5db',
            'background-color': '#fff'
        });
    };

    const showNativeMsg = (content, duration = 2000) => {
        const oldMsg = document.getElementById('native-msg');
        if (oldMsg) oldMsg.remove();

        const msgElem = document.createElement('div');
        msgElem.id = 'native-msg';
        Object.assign(msgElem.style, {
            position: 'fixed',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            padding: '12px 24px',
            background: 'rgba(0, 0, 0, 0.7)',
            color: '#fff',
            borderRadius: '6px',
            fontSize: '14px',
            zIndex: '9999',
            opacity: '1',
            transition: 'opacity 0.3s ease'
        });
        msgElem.textContent = content;
        document.body.appendChild(msgElem);

        setTimeout(() => {
            msgElem.style.opacity = '0';
            setTimeout(() => msgElem.remove(), 300);
        }, duration);
    };

    const getFileExtension = (filename) => {
        return filename.lastIndexOf('.') > 0
            ? filename.slice(filename.lastIndexOf('.')).toLowerCase()
            : '';
    };

    const resetUploadUI = () => {
        const uploadText = document.getElementById('uploadText');
        const uploadArea = document.getElementById('uploadArea');
        const uploadProgress = document.getElementById('uploadProgress');
        const fileUrlInput = document.getElementById('fileUrl');
        const attachmentInput = document.getElementById('attachment');
        const fileNameInput = document.getElementById('fileName');
        const uploadStatus = document.getElementById('uploadStatus');
        const progressFill = document.getElementById('progressFill');

        if (typeof window.clearUploadItems === 'function') {
            window.clearUploadItems();
        }

        // 1. 清空输入框值
        if (fileNameInput) fileNameInput.value = '';
        if (fileUrlInput) fileUrlInput.value = '';

        // 2. 仅清空value，不克隆（克隆逻辑移到上传点击事件）
        if (attachmentInput) {
            attachmentInput.value = '';
        }

        // 3. 重置上传区域样式 + 恢复上传提示文本
        if (uploadArea) {
            uploadArea.classList.remove('uploading', 'success', 'error');
            uploadArea.style.borderColor = '#d1d5db';
            uploadArea.style.backgroundColor = '#fafafa';
        }
        if (uploadText) {
            uploadText.textContent = (typeof i18n !== 'undefined' && i18n.uploadFile)
                ? i18n.uploadFile + '(.pdf, .doc, .docx, .xls, .xlsx, .jpg)'
                : 'Upload file (.pdf, .doc, .docx, .xls, .xlsx, .jpg)';
        }

        // 4. 重置进度条
        if (uploadProgress) {
            uploadProgress.style.display = 'none';
        }
        if (uploadStatus) {
            uploadStatus.textContent = (typeof i18n !== 'undefined' && i18n.uploading) ? i18n.uploading : 'Uploading...';
            uploadStatus.className = 'upload-status';
        }
        if (progressFill) {
            progressFill.classList.remove('uploading', 'success', 'error');
            progressFill.style.width = '0%';
            progressFill.style.backgroundColor = '#0DCB67';
        }
    };

    const COUNTRY_DATA = {
        'Afghanistan': { iso2: 'AF', phone: '93' },
        'Albania': { iso2: 'AL', phone: '355' },
        'Algeria': { iso2: 'DZ', phone: '213' },
        'American Samoa': { iso2: 'AS', phone: '1684' },
        'Andorra': { iso2: 'AD', phone: '376' },
        'Angola': { iso2: 'AO', phone: '244' },
        'Anguilla': { iso2: 'AI', phone: '1264' },
        'Antarctica': { iso2: 'AQ', phone: '672' },
        'Antigua and Barbuda': { iso2: 'AG', phone: '1268' },
        'Argentina': { iso2: 'AR', phone: '54' },
        'Armenia': { iso2: 'AM', phone: '374' },
        'Aruba': { iso2: 'AW', phone: '297' },
        'Australia': { iso2: 'AU', phone: '61' },
        'Austria': { iso2: 'AT', phone: '43' },
        'Azerbaijan': { iso2: 'AZ', phone: '994' },
        'Bahamas': { iso2: 'BS', phone: '1242' },
        'Bahrain': { iso2: 'BH', phone: '973' },
        'Bangladesh': { iso2: 'BD', phone: '880' },
        'Barbados': { iso2: 'BB', phone: '1246' },
        'Belarus': { iso2: 'BY', phone: '375' },
        'Belgium': { iso2: 'BE', phone: '32' },
        'Belize': { iso2: 'BZ', phone: '501' },
        'Benin': { iso2: 'BJ', phone: '229' },
        'Bermuda': { iso2: 'BM', phone: '1441' },
        'Bhutan': { iso2: 'BT', phone: '975' },
        'Bolivia': { iso2: 'BO', phone: '591' },
        'Bosnia and Herzegovina': { iso2: 'BA', phone: '387' },
        'Botswana': { iso2: 'BW', phone: '267' },
        'Bouvet Island': { iso2: 'BV', phone: '47' },
        'Brazil': { iso2: 'BR', phone: '55' },
        'British Indian Ocean Territory': { iso2: 'IO', phone: '246' },
        'Brunei Darussalam': { iso2: 'BN', phone: '673' },
        'Bulgaria': { iso2: 'BG', phone: '359' },
        'Burkina Faso': { iso2: 'BF', phone: '226' },
        'Burundi': { iso2: 'BI', phone: '257' },
        'Cambodia': { iso2: 'KH', phone: '855' },
        'Cameroon': { iso2: 'CM', phone: '237' },
        'Canada': { iso2: 'CA', phone: '1' },
        'Cape Verde': { iso2: 'CV', phone: '238' },
        'Cayman Islands': { iso2: 'KY', phone: '1345' },
        'Central African Republic': { iso2: 'CF', phone: '236' },
        'Chad': { iso2: 'TD', phone: '235' },
        'Chile': { iso2: 'CL', phone: '56' },
        'China': { iso2: 'CN', phone: '86' },
        'Christmas Island': { iso2: 'CX', phone: '61' },
        'Cocos (Keeling) Islands': { iso2: 'CC', phone: '61' },
        'Colombia': { iso2: 'CO', phone: '57' },
        'Comoros': { iso2: 'KM', phone: '269' },
        'Congo': { iso2: 'CG', phone: '242' },
        'Cook Islands': { iso2: 'CK', phone: '682' },
        "Cote d'Ivoire": { iso2: 'CI', phone: '225' },
        'Croatia': { iso2: 'HR', phone: '385' },
        'Cuba': { iso2: 'CU', phone: '53' },
        'Cyprus': { iso2: 'CY', phone: '357' },
        'Czech Republic': { iso2: 'CZ', phone: '420' },
        'Denmark': { iso2: 'DK', phone: '45' },
        'Djibouti': { iso2: 'DJ', phone: '253' },
        'Dominica': { iso2: 'DM', phone: '1767' },
        'Dominican Republic': { iso2: 'DO', phone: '1809' },
        'East Timor': { iso2: 'TL', phone: '670' },
        'Ecuador': { iso2: 'EC', phone: '593' },
        'Egypt': { iso2: 'EG', phone: '20' },
        'El Salvador': { iso2: 'SV', phone: '503' },
        'Equatorial Guinea': { iso2: 'GQ', phone: '240' },
        'Eritrea': { iso2: 'ER', phone: '291' },
        'Estonia': { iso2: 'EE', phone: '372' },
        'Ethiopia': { iso2: 'ET', phone: '251' },
        'Falkland Islands (Malvinas)': { iso2: 'FK', phone: '500' },
        'Faroe Islands': { iso2: 'FO', phone: '298' },
        'Fiji': { iso2: 'FJ', phone: '679' },
        'Finland': { iso2: 'FI', phone: '358' },
        'France, Metropolitan': { iso2: 'FR', phone: '33' },
        'French Guiana': { iso2: 'GF', phone: '594' },
        'French Polynesia': { iso2: 'PF', phone: '689' },
        'French Southern Territories': { iso2: 'TF', phone: '262' },
        'Gabon': { iso2: 'GA', phone: '241' },
        'Gambia': { iso2: 'GM', phone: '220' },
        'Georgia': { iso2: 'GE', phone: '995' },
        'Germany': { iso2: 'DE', phone: '49' },
        'Ghana': { iso2: 'GH', phone: '233' },
        'Gibraltar': { iso2: 'GI', phone: '350' },
        'Greece': { iso2: 'GR', phone: '30' },
        'Greenland': { iso2: 'GL', phone: '299' },
        'Grenada': { iso2: 'GD', phone: '1473' },
        'Guadeloupe': { iso2: 'GP', phone: '590' },
        'Guam': { iso2: 'GU', phone: '1671' },
        'Guatemala': { iso2: 'GT', phone: '502' },
        'Guinea': { iso2: 'GN', phone: '224' },
        'Guinea-Bissau': { iso2: 'GW', phone: '245' },
        'Guyana': { iso2: 'GY', phone: '592' },
        'Haiti': { iso2: 'HT', phone: '509' },
        'Heard and Mc Donald Islands': { iso2: 'HM', phone: '672' },
        'Honduras': { iso2: 'HN', phone: '504' },
        'Hungary': { iso2: 'HU', phone: '36' },
        'Iceland': { iso2: 'IS', phone: '354' },
        'India': { iso2: 'IN', phone: '91' },
        'Indonesia': { iso2: 'ID', phone: '62' },
        'Iran (Islamic Republic of)': { iso2: 'IR', phone: '98' },
        'Iraq': { iso2: 'IQ', phone: '964' },
        'Ireland': { iso2: 'IE', phone: '353' },
        'Israel': { iso2: 'IL', phone: '972' },
        'Italy': { iso2: 'IT', phone: '39' },
        'Jamaica': { iso2: 'JM', phone: '1876' },
        'Japan': { iso2: 'JP', phone: '81' },
        'Jordan': { iso2: 'JO', phone: '962' },
        'Kazakhstan': { iso2: 'KZ', phone: '7' },
        'Kenya': { iso2: 'KE', phone: '254' },
        'Kiribati': { iso2: 'KI', phone: '686' },
        'North Korea': { iso2: 'KP', phone: '850' },
        'South Korea': { iso2: 'KR', phone: '82' },
        'Kuwait': { iso2: 'KW', phone: '965' },
        'Kyrgyzstan': { iso2: 'KG', phone: '996' },
        'Laos': { iso2: 'LA', phone: '856' },
        'Latvia': { iso2: 'LV', phone: '371' },
        'Lebanon': { iso2: 'LB', phone: '961' },
        'Lesotho': { iso2: 'LS', phone: '266' },
        'Liberia': { iso2: 'LR', phone: '231' },
        'Libyan Arab Jamahiriya': { iso2: 'LY', phone: '218' },
        'Liechtenstein': { iso2: 'LI', phone: '423' },
        'Lithuania': { iso2: 'LT', phone: '370' },
        'Luxembourg': { iso2: 'LU', phone: '352' },
        'FYROM': { iso2: 'MK', phone: '389' },
        'Madagascar': { iso2: 'MG', phone: '261' },
        'Malawi': { iso2: 'MW', phone: '265' },
        'Malaysia': { iso2: 'MY', phone: '60' },
        'Maldives': { iso2: 'MV', phone: '960' },
        'Mali': { iso2: 'ML', phone: '223' },
        'Malta': { iso2: 'MT', phone: '356' },
        'Marshall Islands': { iso2: 'MH', phone: '692' },
        'Martinique': { iso2: 'MQ', phone: '596' },
        'Mauritania': { iso2: 'MR', phone: '222' },
        'Mauritius': { iso2: 'MU', phone: '230' },
        'Mayotte': { iso2: 'YT', phone: '262' },
        'Mexico': { iso2: 'MX', phone: '52' },
        'Micronesia, Federated States of': { iso2: 'FM', phone: '691' },
        'Moldova, Republic of': { iso2: 'MD', phone: '373' },
        'Monaco': { iso2: 'MC', phone: '377' },
        'Mongolia': { iso2: 'MN', phone: '976' },
        'Montserrat': { iso2: 'MS', phone: '1664' },
        'Morocco': { iso2: 'MA', phone: '212' },
        'Mozambique': { iso2: 'MZ', phone: '258' },
        'Myanmar': { iso2: 'MM', phone: '95' },
        'Namibia': { iso2: 'NA', phone: '264' },
        'Nauru': { iso2: 'NR', phone: '674' },
        'Nepal': { iso2: 'NP', phone: '977' },
        'Netherlands': { iso2: 'NL', phone: '31' },
        'Netherlands Antilles': { iso2: 'AN', phone: '599' },
        'New Caledonia': { iso2: 'NC', phone: '687' },
        'New Zealand': { iso2: 'NZ', phone: '64' },
        'Nicaragua': { iso2: 'NI', phone: '505' },
        'Niger': { iso2: 'NE', phone: '227' },
        'Nigeria': { iso2: 'NG', phone: '234' },
        'Niue': { iso2: 'NU', phone: '683' },
        'Norfolk Island': { iso2: 'NF', phone: '672' },
        'Northern Mariana Islands': { iso2: 'MP', phone: '1670' },
        'Norway': { iso2: 'NO', phone: '47' },
        'Oman': { iso2: 'OM', phone: '968' },
        'Pakistan': { iso2: 'PK', phone: '92' },
        'Palau': { iso2: 'PW', phone: '680' },
        'Panama': { iso2: 'PA', phone: '507' },
        'Papua New Guinea': { iso2: 'PG', phone: '675' },
        'Paraguay': { iso2: 'PY', phone: '595' },
        'Peru': { iso2: 'PE', phone: '51' },
        'Philippines': { iso2: 'PH', phone: '63' },
        'Pitcairn': { iso2: 'PN', phone: '64' },
        'Poland': { iso2: 'PL', phone: '48' },
        'Portugal': { iso2: 'PT', phone: '351' },
        'Puerto Rico': { iso2: 'PR', phone: '1787' },
        'Qatar': { iso2: 'QA', phone: '974' },
        'Reunion': { iso2: 'RE', phone: '262' },
        'Romania': { iso2: 'RO', phone: '40' },
        'Russian Federation': { iso2: 'RU', phone: '7' },
        'Rwanda': { iso2: 'RW', phone: '250' },
        'Saint Kitts and Nevis': { iso2: 'KN', phone: '1869' },
        'Saint Lucia': { iso2: 'LC', phone: '1758' },
        'Saint Vincent and the Grenadines': { iso2: 'VC', phone: '1784' },
        'Samoa': { iso2: 'WS', phone: '685' },
        'San Marino': { iso2: 'SM', phone: '378' },
        'Sao Tome and Principe': { iso2: 'ST', phone: '239' },
        'Saudi Arabia': { iso2: 'SA', phone: '966' },
        'Senegal': { iso2: 'SN', phone: '221' },
        'Seychelles': { iso2: 'SC', phone: '248' },
        'Sierra Leone': { iso2: 'SL', phone: '232' },
        'Singapore': { iso2: 'SG', phone: '65' },
        'Slovak Republic': { iso2: 'SK', phone: '421' },
        'Slovenia': { iso2: 'SI', phone: '386' },
        'Solomon Islands': { iso2: 'SB', phone: '677' },
        'Somalia': { iso2: 'SO', phone: '252' },
        'South Africa': { iso2: 'ZA', phone: '27' },
        'South Georgia & South Sandwich Islands': { iso2: 'GS', phone: '500' },
        'Spain': { iso2: 'ES', phone: '34' },
        'Sri Lanka': { iso2: 'LK', phone: '94' },
        'St. Helena': { iso2: 'SH', phone: '290' },
        'St. Pierre and Miquelon': { iso2: 'PM', phone: '508' },
        'Sudan': { iso2: 'SD', phone: '249' },
        'Suriname': { iso2: 'SR', phone: '597' },
        'Svalbard and Jan Mayen Islands': { iso2: 'SJ', phone: '47' },
        'Swaziland': { iso2: 'SZ', phone: '268' },
        'Sweden': { iso2: 'SE', phone: '46' },
        'Switzerland': { iso2: 'CH', phone: '41' },
        'Syrian Arab Republic': { iso2: 'SY', phone: '963' },
        'Tajikistan': { iso2: 'TJ', phone: '992' },
        'Tanzania, United Republic of': { iso2: 'TZ', phone: '255' },
        'Thailand': { iso2: 'TH', phone: '66' },
        'Togo': { iso2: 'TG', phone: '228' },
        'Tokelau': { iso2: 'TK', phone: '690' },
        'Tonga': { iso2: 'TO', phone: '676' },
        'Trinidad and Tobago': { iso2: 'TT', phone: '1868' },
        'Tunisia': { iso2: 'TN', phone: '216' },
        'Turkey': { iso2: 'TR', phone: '90' },
        'Turkmenistan': { iso2: 'TM', phone: '993' },
        'Turks and Caicos Islands': { iso2: 'TC', phone: '1649' },
        'Tuvalu': { iso2: 'TV', phone: '688' },
        'Uganda': { iso2: 'UG', phone: '256' },
        'Ukraine': { iso2: 'UA', phone: '380' },
        'United Arab Emirates': { iso2: 'AE', phone: '971' },
        'United Kingdom': { iso2: 'GB', phone: '44' },
        'United States': { iso2: 'US', phone: '1' },
        'United States Minor Outlying Islands': { iso2: 'UM', phone: '1' },
        'Uruguay': { iso2: 'UY', phone: '598' },
        'Uzbekistan': { iso2: 'UZ', phone: '998' },
        'Vanuatu': { iso2: 'VU', phone: '678' },
        'Vatican City State (Holy See)': { iso2: 'VA', phone: '39' },
        'Venezuela': { iso2: 'VE', phone: '58' },
        'Viet Nam': { iso2: 'VN', phone: '84' },
        'Virgin Islands (British)': { iso2: 'VG', phone: '1284' },
        'Virgin Islands (U.S.)': { iso2: 'VI', phone: '1340' },
        'Wallis and Futuna Islands': { iso2: 'WF', phone: '681' },
        'Western Sahara': { iso2: 'EH', phone: '212' },
        'Yemen': { iso2: 'YE', phone: '967' },
        'Democratic Republic of Congo': { iso2: 'CD', phone: '243' },
        'Zambia': { iso2: 'ZM', phone: '260' },
        'Zimbabwe': { iso2: 'ZW', phone: '263' },
        'Montenegro': { iso2: 'ME', phone: '382' },
        'Serbia': { iso2: 'RS', phone: '381' },
        'Aaland Islands': { iso2: 'AX', phone: '358' },
        'Bonaire, Sint Eustatius and Saba': { iso2: 'BQ', phone: '599' },
        'Curacao': { iso2: 'CW', phone: '599' },
        'Palestinian Territory, Occupied': { iso2: 'PS', phone: '970' },
        'South Sudan': { iso2: 'SS', phone: '211' },
        'St. Barthelemy': { iso2: 'BL', phone: '590' },
        'St. Martin (French part)': { iso2: 'MF', phone: '590' },
        'Canary Islands': { iso2: 'IC', phone: '34' },
        'Ascension Island (British)': { iso2: 'AC', phone: '247' },
        'Kosovo, Republic of': { iso2: 'XK', phone: '383' },
        'Isle of Man': { iso2: 'IM', phone: '44' },
        'Tristan da Cunha': { iso2: 'TA', phone: '290' },
        'Guernsey': { iso2: 'GG', phone: '44' },
        'Jersey': { iso2: 'JE', phone: '44' }
    };

    // 缓存选项元素
    let countryOptions = [];

    const initCountrySelect = () => {
        const phonePrefix = document.getElementById('phonePrefix');
        const phoneContainer = document.getElementById('phone-container');
        const originalSelect = document.getElementById('input-country');

        if (!originalSelect) return;

        // 替换原生select为自定义容器
        const selectContainer = document.createElement('div');
        originalSelect.replaceWith(selectContainer);
        selectContainer.id = 'custom-country-select';
        selectContainer.style.cssText = `
                position: relative;
                width: 100%;
                border: 1px solid #ddd;
                border-radius: 6px;
                background: #fff;
                cursor: pointer;
                font-size: 15px;
            `;

        // 1. 选中项显示区域
        const selectedDisplay = document.createElement('div');
        selectedDisplay.style.cssText = `
                padding: 12px 18px;
                display: flex;
                align-items: center;
                gap: 8px;
            `;
        const selectedIcon = document.createElement('span');
        selectedIcon.className = 'input-icon p-icon-a-232';
        selectedIcon.style.color = '#777';
        const selectedText = document.createElement('span');
        selectedDisplay.appendChild(selectedIcon);
        selectedDisplay.appendChild(selectedText);
        selectContainer.appendChild(selectedDisplay);

        // 2. 下拉箭头
        const arrow = document.createElement('div');
        arrow.style.cssText = `
                position: absolute;
                right: 12px;
                top: 50%;
                transform: translateY(-50%);
                color: #777;
            `;
        arrow.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#777" stroke-width="2" stroke-linecap="round"><polyline points="6 9 12 15 18 9"></polyline></svg>';
        selectContainer.appendChild(arrow);

        // 3. 下拉面板（默认隐藏）
        const dropdownPanel = document.createElement('div');
        dropdownPanel.style.cssText = `
                position: absolute;
                top: 100%;
                left: 0;
                right: 0;
                max-height: 200px;
                overflow-y: auto;
                background: #fff;
                border: 1px solid #ddd;
                border-top: none;
                border-radius: 0 0 6px 6px;
                z-index: 9999;
                display: none;
            `;
        selectContainer.appendChild(dropdownPanel);

        // 4. 隐藏的input用于表单提交
        const hiddenInput = document.createElement('input');
        hiddenInput.type = 'hidden';
        hiddenInput.name = 'country';
        hiddenInput.id = 'input-country-hidden';
        hiddenInput.value = '';
        selectContainer.appendChild(hiddenInput);

        // 初始化默认选项
        selectedText.textContent = 'China';
        selectedText.style.color = '#000';
        hiddenInput.value = 'CN';
        if (phonePrefix) {
            phonePrefix.textContent = '+86';
        }

        // 5. 填充国家选项
        const countryList = Object.entries(COUNTRY_DATA);

        // 保存所有选项引用
        countryOptions = [];

        // 设置选中样式的函数
        const setOptionSelected = (option, isSelected) => {
            if (isSelected) {
                option.classList.add('dropdown-option-selected');
            } else {
                option.classList.remove('dropdown-option-selected');
            }
        };

        // 清空所有选项的选中状态
        const clearAllSelections = () => {
            countryOptions.forEach(opt => {
                setOptionSelected(opt.optionElement, false);
            });
        };

        // 选择国家
        const selectCountry = (countryName, iso2, phone) => {
            // 更新显示文本
            selectedText.textContent = countryName;
            selectedText.style.color = '#000';

            // 更新隐藏input
            hiddenInput.value = iso2;

            // 更新电话前缀
            if (phonePrefix) {
                phonePrefix.textContent = '+' + phone;
            }

            // 设置选中状态
            clearAllSelections();
            const selectedOption = countryOptions.find(opt => opt.iso2 === iso2);
            if (selectedOption) {
                setOptionSelected(selectedOption.optionElement, true);
            }
        };

        // 创建并添加选项
        countryList.forEach(([name, data]) => {
            const option = document.createElement('div');
            option.dataset.iso2 = data.iso2;
            option.dataset.countryName = name;
            option.dataset.phone = data.phone;
            option.style.cssText = `
                    padding: 12px 18px;
                    text-align: left;
                    cursor: pointer;
                    transition: all 0.2s ease;
                    width: 100%;
                    box-sizing: border-box;
                `;
            option.textContent = name;

            // 默认选中中国
            if (data.iso2 === 'CN') {
                setOptionSelected(option, true);
            }

            option.addEventListener('click', () => {
                selectCountry(name, data.iso2, data.phone);
                dropdownPanel.style.display = 'none';
            });

            option.addEventListener('mouseenter', () => {
                if (option.dataset.iso2 !== hiddenInput.value) {
                    option.style.backgroundColor = '#f5f5f5';
                }
            });

            option.addEventListener('mouseleave', () => {
                if (option.dataset.iso2 !== hiddenInput.value) {
                    option.style.backgroundColor = '';
                }
            });

            option.className = 'dropdown-option';
            countryOptions.push({
                optionElement: option,
                iso2: data.iso2,
                name: name,
                phone: data.phone
            });
            dropdownPanel.appendChild(option);
        });

        // 6. 切换下拉面板显示/隐藏
        selectedDisplay.addEventListener('click', (e) => {
            e.stopPropagation();
            dropdownPanel.style.display = dropdownPanel.style.display === 'none' ? 'block' : 'none';
        });

        // 点击外部关闭下拉面板
        document.addEventListener('click', (e) => {
            if (!selectContainer.contains(e.target)) {
                dropdownPanel.style.display = 'none';
            }
        });

        // 设置默认选中
        selectCountry('China', 'CN', '86');
        initializeRfqCountrySearch(selectContainer, selectedDisplay, dropdownPanel, originalSelect);
    };

    const setCountryByIP = async () => {
        try {
            console.log('开始IP查询...');
            const response = await fetch('https://ipapi.co/json/');
            if (!response.ok) {
                console.log('IP查询失败:', response.status);
                throw new Error('Network error');
            }

            const data = await response.json();
            console.log('IP查询结果:', data);

            if (data && data.country_code) {
                const userCountryISO2 = data.country_code.toUpperCase();
                console.log('国家代码:', userCountryISO2);

                // 查找对应的国家
                const countryEntry = Object.entries(COUNTRY_DATA).find(
                    ([name, countryData]) => countryData.iso2 === userCountryISO2
                );

                if (countryEntry) {
                    const [countryName, countryData] = countryEntry;
                    console.log('找到国家:', countryName, '电话前缀:', countryData.phone);

                    // 设置国家
                    const customSelect = document.getElementById('custom-country-select');
                    if (customSelect) {
                        const selectedText = customSelect.querySelector('span:not(.input-icon)');
                        const hiddenInput = document.getElementById('input-country-hidden');
                        const phonePrefix = document.getElementById('phonePrefix');

                        if (selectedText) {
                            selectedText.textContent = countryName;
                            selectedText.style.color = '#000';
                        }

                        if (hiddenInput) {
                            hiddenInput.value = userCountryISO2;
                        }

                        if (phonePrefix) {
                            phonePrefix.textContent = '+' + countryData.phone;
                        }

                        // 更新选中状态
                        clearAllSelections();
                        const selectedOption = countryOptions.find(opt => opt.iso2 === userCountryISO2);
                        if (selectedOption) {
                            selectedOption.optionElement.classList.add('dropdown-option-selected');
                        }

                        console.log('国家已设置为:', countryName);
                    } else {
                        console.log('自定义选择器未找到');
                    }
                } else {
                    console.log('未找到对应国家:', userCountryISO2);
                }
            } else {
                console.log('未获取到国家代码');
            }
        } catch (error) {
            console.log('IP查询失败，使用默认国家:', error.message);
        }
    };

    // 清除所有选中状态的函数
    const clearAllSelections = () => {
        if (!countryOptions.length) return;

        countryOptions.forEach(opt => {
            opt.optionElement.style.backgroundColor = '';
            opt.optionElement.style.color = '';
            opt.optionElement.style.fontWeight = '';
        });
    };

    const initFileUpload = () => {
        const uploadArea = document.getElementById('uploadArea');
        const uploadText = document.getElementById('uploadText');
        const fileUrlInput = document.getElementById('fileUrl');
        const fileNameInput = document.getElementById('fileName');

        if (!uploadArea) return;

        const UPLOAD_CONFIG = {
            allowedMimeTypes: new Set([
                'application/pdf',
                'application/msword',
                'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                'application/vnd.ms-excel',
                'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
                'application/vnd.ms-powerpoint',
                'application/vnd.openxmlformats-officedocument.presentationml.presentation',
                'text/plain',
                'text/csv',
                'application/zip',
                'image/jpeg',
                'image/png'
            ]),
            allowedExtensions: new Set([
                '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx',
                '.txt', '.csv', '.zip', '.jpg', '.jpeg', '.png'
            ]),
            maxFileSize: 20 * 1024 * 1024 // 20MB
        };

        let uploadContainer = document.getElementById('uploadItemsContainer');
        if (!uploadContainer) {
            uploadContainer = document.createElement('div');
            uploadContainer.id = 'uploadItemsContainer';
            uploadContainer.style.marginTop = '12px';
            uploadArea.parentNode.appendChild(uploadContainer);
        }

        let uploadedFiles = [];

        const updateHiddenFields = () => {
            if (fileUrlInput) fileUrlInput.value = uploadedFiles.map(f => f.url).join(',');
            if (fileNameInput) fileNameInput.value = uploadedFiles.map(f => f.name).join(',');
        };

        const createUploadItem = (fileName) => {
            const item = document.createElement('div');
            item.className = 'upload-item';
            item.style.cssText = `
                    margin-top: 12px;
                    padding: 12px;
                    border: 1px solid #e5e7eb;
                    border-radius: 6px;
                    background: #f9fafb;
                    position: relative;
                `;

            const fileNameSpan = document.createElement('div');
            fileNameSpan.textContent = fileName;
            fileNameSpan.style.cssText = 'font-size: 14px; color: #374151; margin-bottom: 6px;';

            const progressBar = document.createElement('div');
            progressBar.style.cssText = 'width: 100%; height: 6px; background-color: #e5e7eb; border-radius: 3px; overflow: hidden;';
            const progressFill = document.createElement('div');
            progressFill.style.cssText = 'height: 100%; background-color: #0DCB67; width: 0%; transition: width 0.3s ease;';
            progressBar.appendChild(progressFill);

            const statusDiv = document.createElement('div');
            statusDiv.style.cssText = 'font-size: 12px; color: #6b7280; margin-top: 4px;';
            statusDiv.textContent = i18n?.uploading || 'Uploading...';

            const deleteBtn = document.createElement('button');
            deleteBtn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>';
            deleteBtn.style.cssText = `
                    position: absolute;
                    top: 8px;
                    right: 8px;
                    background: none;
                    border: none;
                    cursor: pointer;
                    color: #ef4444;
                    opacity: 0.7;
                `;
            deleteBtn.addEventListener('click', () => {
                uploadedFiles = uploadedFiles.filter(f => f.name !== fileName);
                item.remove();
                updateHiddenFields();
            });

            item.appendChild(fileNameSpan);
            item.appendChild(progressBar);
            item.appendChild(statusDiv);
            item.appendChild(deleteBtn);

            return { element: item, progressFill, statusDiv };
        };

        uploadArea.addEventListener('click', () => {
            const tempInput = document.createElement('input');
            tempInput.type = 'file';
            tempInput.accept = '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv,.zip,.jpg,.jpeg,.png';
            tempInput.style.display = 'none';

            tempInput.onchange = async () => {
                const files = tempInput.files;
                if (files.length === 0) return;

                const file = files[0];

                if (file.size > UPLOAD_CONFIG.maxFileSize) {
                    showNativeMsg(i18n.uploadSize || 'File too large (max 20MB)');
                    return;
                }

                const extension = '.' + (file.name.split('.').pop() || '').toLowerCase();
                const isValidType = UPLOAD_CONFIG.allowedMimeTypes.has(file.type) ||
                    UPLOAD_CONFIG.allowedExtensions.has(extension);

                if (!isValidType) {
                    showNativeMsg(i18n.uploadFormat + '(.pdf, .doc, .docx, .xls, .xlsx, .jpg)');
                    return;
                }

                const { element: uploadItem, progressFill, statusDiv } = createUploadItem(file.name);
                uploadContainer.appendChild(uploadItem);

                const formData = new FormData();
                formData.append('file', file);

                try {
                    const xhr = new XMLHttpRequest();

                    xhr.upload.onprogress = (e) => {
                        if (e.lengthComputable) {
                            const percent = (e.loaded / e.total) * 100;
                            progressFill.style.width = `${percent}%`;
                        }
                    };

                    xhr.onload = () => {
                        let result;
                        try {
                            result = JSON.parse(xhr.responseText);
                        } catch {
                            throw new Error('Invalid server response');
                        }

                        if (result && (result.code === 200 || result.success) && result.url) {
                            progressFill.style.width = '100%';
                            statusDiv.textContent = i18n.uploadSuccess || 'Uploaded';
                            statusDiv.style.color = '#10b981';

                            uploadedFiles.push({ url: result.url, name: file.name });
                            updateHiddenFields();
                            showNativeMsg(i18n.uploadSuccess || 'Upload successful!');
                        } else {
                            throw new Error(result.msg || 'Upload failed');
                        }
                    };

                    xhr.onerror = () => {
                        progressFill.style.backgroundColor = '#ef4444';
                        statusDiv.textContent = i18n.uploadError || 'Failed';
                        statusDiv.style.color = '#ef4444';
                        showNativeMsg('Upload failed');
                    };

                    xhr.open('POST', '/upload', true);
                    xhr.send(formData);

                } catch (error) {
                    progressFill.style.backgroundColor = '#ef4444';
                    statusDiv.textContent = i18n.uploadError || 'Error';
                    statusDiv.style.color = '#ef4444';
                    showNativeMsg('Error: ' + error.message);
                }
            };

            document.body.appendChild(tempInput);
            tempInput.click();
            tempInput.remove();
        });

        window.clearUploadItems = function() {
            uploadedFiles = [];
            if (uploadContainer) {
                uploadContainer.innerHTML = '';
            }
            if (fileUrlInput) fileUrlInput.value = '';
            if (fileNameInput) fileNameInput.value = '';
        };
    };

    let isLeaveMessageSubmitting = false;
    let lastLeaveMessageTouchAt = 0;

    const setLeaveMessageSubmittingState = function (submitting) {
        isLeaveMessageSubmitting = submitting;
        $('.submit-btn').prop('disabled', submitting).css({
            pointerEvents: submitting ? 'none' : '',
            opacity: submitting ? '0.7' : '',
            backgroundColor: submitting ? '#9ca3af' : 'var(--theme-wone)',
            cursor: submitting ? 'not-allowed' : ''
        }).attr('aria-busy', submitting ? 'true' : 'false');
    };

    const handleLeaveMessageSubmit = function () {
        if (isLeaveMessageSubmitting) {
            return;
        }
        $('.form-error').remove();
        const phonePrefix = $('#phonePrefix').text().trim();
        const phoneNumber = $('#input-phone').val().trim();
        const fullPhone = phoneNumber ? phonePrefix + phoneNumber : '';

        const formData = {
            bizId: $('#input-biz-id').val(),
            name: $('#input-name').val().trim(),
            phone: fullPhone,
            email: $('#input-email').val().trim(),
            country: $('#input-country-hidden').val().trim(),
            message: $('#input-message').val().trim(),
            orgName: $('#input-orgName').val().trim(),
            title: $('#input-title').val().trim(),
            attachFile: $('#fileName').val().trim(),
            attachUrl: $('#fileUrl').val().trim()
        };

        let hasError = false;

        // === 验证逻辑 ===
        if (!formData.name) {
            showError($('#input-name'), i18n.ruleName);
            hasError = true;
        }

        if (!formData.email) {
            showError($('#input-email'), i18n.ruleEmail);
            hasError = true;
        } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
            showError($('#input-email'), i18n.ruleEmailFormat);
            hasError = true;
        }

        if (!formData.country) {
            showError($('#input-country-hidden'), i18n.ruleCountry);
            hasError = true;
        }


        if (hasError) {
            return;
        }

        // 提交数据到后端
        setLeaveMessageSubmittingState(true);
        const xhr = new XMLHttpRequest();
        xhr.open('POST', "/leaveMessage", true);
        xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');

        xhr.onload = function () {
            setLeaveMessageSubmittingState(false);
            if (xhr.status >= 200 && xhr.status < 300) {
                try {
                    const result = JSON.parse(xhr.responseText);
                    if (result && result.code === 200) {
                        showNativeMsg(i18n.ruleSubmit);
                        setTimeout(function () {
                            // 关闭模态框并重置表单
                            $('#doc-modal-1').modal('close');
                            $('#requestForm')[0].reset();
                            resetUploadUI();
                            // location.reload(); // 可选：是否刷新页面
                        }, 500);
                    } else {
                        showNativeMsg(result?.msg || i18n.ruleSubmitError);
                    }
                } catch (e) {
                    showNativeMsg(i18n.ruleSubmitError);
                }
            } else {
                showNativeMsg(i18n.ruleSubmitError);
            }
        };

        xhr.onerror = function () {
            setLeaveMessageSubmittingState(false);
            showNativeMsg(i18n.ruleNetwork);
        };

        // 构建请求参数
        const formDataParams = Object.keys(formData).map(key =>
            encodeURIComponent(key) + '=' + encodeURIComponent(formData[key])
        ).join('&');

        console.log('提交的参数：', formDataParams);
        xhr.send(formDataParams);
    };

    $('.submit-btn').on('touchend', function (e) {
        e.preventDefault();
        lastLeaveMessageTouchAt = Date.now();
        handleLeaveMessageSubmit();
    });

    $('.submit-btn').on('click', function (e) {
        e.preventDefault();
        if (Date.now() - lastLeaveMessageTouchAt < 700) {
            return;
        }
        handleLeaveMessageSubmit();
    });

    // 页面加载时初始化，但不调用setCountryByIP
    $(function() {
        initCountrySelect();
        initFileUpload();

        $('.close-btn').on('click', function() {
            $('#doc-modal-1').modal('close');
        });
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
    $(document).on('click', '.chat-btn-brand', function() {
        const details = $(this).data('details');

        if (isMobile()) {
            const schemeUrl = 'iwone://chat?id=' + encodeURIComponent(details);
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
            // e.preventDefault();
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

function initializeRfqCountrySearch(container, trigger, panel, originalSelect) {
    const searchArea = document.createElement('div');
    searchArea.style.cssText = 'position:sticky;top:0;padding:8px;background:#fff;z-index:1;';
    const search = document.createElement('input');
    search.type = 'search';
    search.placeholder = originalSelect.getAttribute('data-search-placeholder') || 'Search country';
    search.setAttribute('aria-label', search.placeholder);
    search.autocomplete = 'off';
    search.style.cssText = 'display:block;box-sizing:border-box;width:100%;height:38px;padding:8px 10px;border:1px solid #ddd;border-radius:4px;background:#fff;color:#111827;font:inherit;';
    searchArea.appendChild(search);
    panel.insertBefore(searchArea, panel.firstChild);
    const empty = document.createElement('div');
    empty.textContent = originalSelect.getAttribute('data-empty-message') || 'No countries found';
    empty.setAttribute('role', 'status');
    empty.style.cssText = 'display:none;padding:12px 18px;color:#777;';
    panel.appendChild(empty);
    trigger.tabIndex = 0;
    trigger.setAttribute('role', 'button');
    trigger.setAttribute('aria-expanded', 'false');
    const options = Array.from(panel.querySelectorAll('.dropdown-option'));
    const filter = () => {
        const query = search.value.trim().toLowerCase();
        let matches = 0;
        options.forEach(option => {
            const text = (option.dataset.countryName + ' ' + option.dataset.iso2 + ' +' + option.dataset.phone).toLowerCase();
            const visible = text.includes(query);
            option.style.display = visible ? '' : 'none';
            if (visible) matches++;
        });
        empty.style.display = matches ? 'none' : 'block';
        panel.scrollTop = 0;
    };
    search.addEventListener('input', filter);
    trigger.addEventListener('click', () => {
        const open = panel.style.display !== 'none';
        trigger.setAttribute('aria-expanded', String(open));
        if (open) {
            search.value = '';
            filter();
            search.focus();
        }
    });
    options.forEach(option => {
        option.tabIndex = 0;
        option.addEventListener('click', () => {
            trigger.setAttribute('aria-expanded', 'false');
            trigger.focus();
        });
        option.addEventListener('keydown', event => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                option.click();
            }
        });
    });
    container.addEventListener('keydown', event => {
        if (event.key === 'Escape') {
            event.preventDefault();
            panel.style.display = 'none';
            trigger.setAttribute('aria-expanded', 'false');
            trigger.focus();
        } else if (event.target === search && (event.key === 'Enter' || event.key === 'ArrowDown')) {
            event.preventDefault();
            const first = options.find(option => option.style.display !== 'none');
            if (first) {
                if (event.key === 'Enter') first.click();
                else first.focus();
            }
        } else if (event.target === trigger && (event.key === 'Enter' || event.key === ' ')) {
            event.preventDefault();
            trigger.click();
        }
    });
    // Keep native scrollbar interaction inside the dropdown's focus boundary.
    panel.tabIndex = -1;
    let interactingWithPanel = false;
    panel.addEventListener('pointerdown', () => {
        interactingWithPanel = true;
    });
    const finishPanelInteraction = () => {
        setTimeout(() => { interactingWithPanel = false; }, 0);
    };
    document.addEventListener('pointerup', finishPanelInteraction);
    document.addEventListener('pointercancel', finishPanelInteraction);
    container.addEventListener('focusout', event => {
        // Native scrollbar interaction can blur the search input without a new focus target.
        // Close for focus moving outside; outside clicks are handled separately.
        if (!interactingWithPanel && event.relatedTarget && !container.contains(event.relatedTarget)) {
            panel.style.display = 'none';
            trigger.setAttribute('aria-expanded', 'false');
        }
    });
    document.addEventListener('click', event => {
        if (!container.contains(event.target)) trigger.setAttribute('aria-expanded', 'false');
    });
}

;
// List infinite scroll
(function () {
    'use strict';

    // Keep the next-page link usable if the browser cannot enhance the list.
    if (!window.fetch || !window.IntersectionObserver || !window.AbortController) return;
    var list = document.querySelector('[data-scroll-list]');
    var status = document.querySelector('[data-list-load-status]');
    if (!list || !status || !list.getAttribute('data-next-url')) return;
    var nextLink = status.querySelector('[data-list-load-next]');

    var chinese = /^zh/i.test(document.documentElement.lang);
    var labels = chinese ? {
        ready: '继续下滑加载更多', loading: '正在加载…',
        done: '已全部加载', error: '加载失败，请重试', retry: '重试'
    } : {
        ready: 'Scroll for more', loading: 'Loading…',
        done: 'All items loaded', error: 'Unable to load. Please retry.', retry: 'Retry'
    };
    // Translations are rendered into the static HTML from the advanced dictionary.
    Object.keys(labels).forEach(function (key) {
        var translated = status.getAttribute('data-label-' + key);
        if (translated && translated.trim()) labels[key] = translated;
    });
    var message = status.querySelector('[data-list-load-message]');
    var retry = status.querySelector('[data-list-load-retry]');
    var nextUrl = list.getAttribute('data-next-url');
    var currentPage = Number(list.getAttribute('data-page'));
    var loading = false;
    var failed = false;
    retry.textContent = labels.retry;

    function show(text) { message.textContent = text; }

    function loadNext() {
        if (loading || failed || !nextUrl) return;
        var requestedUrl = new URL(nextUrl, window.location.href);
        if (requestedUrl.origin !== window.location.origin) return;
        loading = true;
        retry.hidden = true;
        list.setAttribute('aria-busy', 'true');
        show(labels.loading);
        var controller = new AbortController();
        var timeout = window.setTimeout(function () { controller.abort(); }, 20000);
        fetch(requestedUrl.href, { signal: controller.signal, credentials: 'same-origin' })
            .then(function (response) {
                if (!response.ok) throw new Error('List page request failed');
                return response.text();
            })
            .then(function (html) {
                var page = new DOMParser().parseFromString(html, 'text/html');
                var incoming = page.querySelector('[data-scroll-list]');
                // Only an explicitly marked empty next page is a completed list.
                var pageInfo = page.querySelector('meta[name="list-scroll-page"]');
                var canonical = page.querySelector('link[rel="canonical"]');
                if (pageInfo && canonical && pageInfo.getAttribute('data-list-empty') === 'true' &&
                    Number(pageInfo.getAttribute('data-page')) === currentPage + 1 &&
                    new URL(canonical.getAttribute('href'), requestedUrl.href).pathname === requestedUrl.pathname) {
                    nextUrl = '';
                    list.setAttribute('data-next-url', '');
                    show(labels.done);
                    observer.disconnect();
                    return;
                }
                // Reject error pages, stale first pages and other unexpected responses.
                if (!incoming || Number(incoming.getAttribute('data-page')) !== currentPage + 1 ||
                    !incoming.children.length) throw new Error('Invalid list page');
                var following = incoming.getAttribute('data-next-url') || '';
                if (following) {
                    var followingUrl = new URL(following, requestedUrl.href);
                    if (followingUrl.origin !== window.location.origin || followingUrl.href === requestedUrl.href)
                        throw new Error('Invalid next page link');
                    following = followingUrl.href;
                }
                var fragment = document.createDocumentFragment();
                Array.prototype.forEach.call(incoming.children, function (card) {
                    // Resolve relative links against the fetched page, not the first page.
                    Array.prototype.forEach.call(card.querySelectorAll('[href], [src]'), function (element) {
                        ['href', 'src'].forEach(function (attribute) {
                            var value = element.getAttribute(attribute);
                            if (value) element.setAttribute(attribute, new URL(value, requestedUrl.href).href);
                        });
                    });
                    fragment.appendChild(document.importNode(card, true));
                });
                list.appendChild(fragment);
                currentPage += 1;
                nextUrl = following;
                list.setAttribute('data-page', currentPage);
                list.setAttribute('data-next-url', nextUrl);
                show(nextUrl ? labels.ready : labels.done);
                if (!nextUrl) observer.disconnect();
            })
            .catch(function () {
                failed = true;
                show(labels.error);
                retry.hidden = false;
            })
            .then(function () {
                window.clearTimeout(timeout);
                loading = false;
                list.setAttribute('aria-busy', 'false');
                // Recheck after the new cards lay out, even if the sentinel stayed visible.
                if (!failed && nextUrl) window.requestAnimationFrame(checkBottom);
            });
    }

    function checkBottom() {
        var bounds = status.getBoundingClientRect();
        if (bounds.top < window.innerHeight && bounds.bottom > 0) loadNext();
    }

    var observer = new IntersectionObserver(function (entries) {
        if (entries[0].isIntersecting) loadNext();
    }, { rootMargin: '0px' });
    retry.addEventListener('click', function () {
        failed = false;
        loadNext();
    });
    if (nextLink) nextLink.hidden = true;
    status.hidden = false;
    show(labels.ready);
    observer.observe(status);
}());
/**
	 * jQuery MD5 hash algorithm function
	 *
	 * 	<code>
	 * 		Calculate the md5 hash of a String
	 * 		String $.md5 ( String str )
	 * 	</code>
	 *
	 * Calculates the MD5 hash of str using the » RSA Data Security, Inc. MD5 Message-Digest Algorithm, and returns that hash.
	 * MD5 (Message-Digest algorithm 5) is a widely-used cryptographic hash function with a 128-bit hash value. MD5 has been employed in a wide variety of security applications, and is also commonly used to check the integrity of data. The generated hash is also non-reversable. Data cannot be retrieved from the message digest, the digest uniquely identifies the data.
	 * MD5 was developed by Professor Ronald L. Rivest in 1994. Its 128 bit (16 byte) message digest makes it a faster implementation than SHA-1.
	 * This script is used to process a variable length message into a fixed-length output of 128 bits using the MD5 algorithm. It is fully compatible with UTF-8 encoding. It is very useful when u want to transfer encrypted passwords over the internet. If you plan using UTF-8 encoding in your project don't forget to set the page encoding to UTF-8 (Content-Type meta tag).
	 * This function orginally get from the WebToolkit and rewrite for using as the jQuery plugin.
	 *
	 * Example
	 * 	Code
	 * 		<code>
	 * 			$.md5("I'm Persian.");
	 * 		</code>
	 * 	Result
	 * 		<code>
	 * 			"b8c901d0f02223f9761016cfff9d68df"
	 * 		</code>
	 *
	 * @alias Muhammad Hussein Fattahizadeh < muhammad [AT] semnanweb [DOT] com >
	 * @link http://www.semnanweb.com/jquery-plugin/md5.html
	 * @see http://www.webtoolkit.info/
	 * @license http://www.gnu.org/licenses/gpl.html [GNU General Public License]
	 * @param {jQuery} {md5:function(string))
	 * @return string
	 */

	(function($){

		var rotateLeft = function(lValue, iShiftBits) {
			return (lValue << iShiftBits) | (lValue >>> (32 - iShiftBits));
		}

		var addUnsigned = function(lX, lY) {
			var lX4, lY4, lX8, lY8, lResult;
			lX8 = (lX & 0x80000000);
			lY8 = (lY & 0x80000000);
			lX4 = (lX & 0x40000000);
			lY4 = (lY & 0x40000000);
			lResult = (lX & 0x3FFFFFFF) + (lY & 0x3FFFFFFF);
			if (lX4 & lY4) return (lResult ^ 0x80000000 ^ lX8 ^ lY8);
			if (lX4 | lY4) {
				if (lResult & 0x40000000) return (lResult ^ 0xC0000000 ^ lX8 ^ lY8);
				else return (lResult ^ 0x40000000 ^ lX8 ^ lY8);
			} else {
				return (lResult ^ lX8 ^ lY8);
			}
		}

		var F = function(x, y, z) {
			return (x & y) | ((~ x) & z);
		}

		var G = function(x, y, z) {
			return (x & z) | (y & (~ z));
		}

		var H = function(x, y, z) {
			return (x ^ y ^ z);
		}

		var I = function(x, y, z) {
			return (y ^ (x | (~ z)));
		}

		var FF = function(a, b, c, d, x, s, ac) {
			a = addUnsigned(a, addUnsigned(addUnsigned(F(b, c, d), x), ac));
			return addUnsigned(rotateLeft(a, s), b);
		};

		var GG = function(a, b, c, d, x, s, ac) {
			a = addUnsigned(a, addUnsigned(addUnsigned(G(b, c, d), x), ac));
			return addUnsigned(rotateLeft(a, s), b);
		};

		var HH = function(a, b, c, d, x, s, ac) {
			a = addUnsigned(a, addUnsigned(addUnsigned(H(b, c, d), x), ac));
			return addUnsigned(rotateLeft(a, s), b);
		};

		var II = function(a, b, c, d, x, s, ac) {
			a = addUnsigned(a, addUnsigned(addUnsigned(I(b, c, d), x), ac));
			return addUnsigned(rotateLeft(a, s), b);
		};

		var convertToWordArray = function(string) {
			var lWordCount;
			var lMessageLength = string.length;
			var lNumberOfWordsTempOne = lMessageLength + 8;
			var lNumberOfWordsTempTwo = (lNumberOfWordsTempOne - (lNumberOfWordsTempOne % 64)) / 64;
			var lNumberOfWords = (lNumberOfWordsTempTwo + 1) * 16;
			var lWordArray = Array(lNumberOfWords - 1);
			var lBytePosition = 0;
			var lByteCount = 0;
			while (lByteCount < lMessageLength) {
				lWordCount = (lByteCount - (lByteCount % 4)) / 4;
				lBytePosition = (lByteCount % 4) * 8;
				lWordArray[lWordCount] = (lWordArray[lWordCount] | (string.charCodeAt(lByteCount) << lBytePosition));
				lByteCount++;
			}
			lWordCount = (lByteCount - (lByteCount % 4)) / 4;
			lBytePosition = (lByteCount % 4) * 8;
			lWordArray[lWordCount] = lWordArray[lWordCount] | (0x80 << lBytePosition);
			lWordArray[lNumberOfWords - 2] = lMessageLength << 3;
			lWordArray[lNumberOfWords - 1] = lMessageLength >>> 29;
			return lWordArray;
		};

		var wordToHex = function(lValue) {
			var WordToHexValue = "", WordToHexValueTemp = "", lByte, lCount;
			for (lCount = 0; lCount <= 3; lCount++) {
				lByte = (lValue >>> (lCount * 8)) & 255;
				WordToHexValueTemp = "0" + lByte.toString(16);
				WordToHexValue = WordToHexValue + WordToHexValueTemp.substr(WordToHexValueTemp.length - 2, 2);
			}
			return WordToHexValue;
		};

		var uTF8Encode = function(string) {
			string = string.replace(/\x0d\x0a/g, "\x0a");
			var output = "";
			for (var n = 0; n < string.length; n++) {
				var c = string.charCodeAt(n);
				if (c < 128) {
					output += String.fromCharCode(c);
				} else if ((c > 127) && (c < 2048)) {
					output += String.fromCharCode((c >> 6) | 192);
					output += String.fromCharCode((c & 63) | 128);
				} else {
					output += String.fromCharCode((c >> 12) | 224);
					output += String.fromCharCode(((c >> 6) & 63) | 128);
					output += String.fromCharCode((c & 63) | 128);
				}
			}
			return output;
		};

		$.extend({
			md5: function(string) {
				var x = Array();
				var k, AA, BB, CC, DD, a, b, c, d;
				var S11=7, S12=12, S13=17, S14=22;
				var S21=5, S22=9 , S23=14, S24=20;
				var S31=4, S32=11, S33=16, S34=23;
				var S41=6, S42=10, S43=15, S44=21;
				string = uTF8Encode(string);
				x = convertToWordArray(string);
				a = 0x67452301; b = 0xEFCDAB89; c = 0x98BADCFE; d = 0x10325476;
				for (k = 0; k < x.length; k += 16) {
					AA = a; BB = b; CC = c; DD = d;
					a = FF(a, b, c, d, x[k+0],  S11, 0xD76AA478);
					d = FF(d, a, b, c, x[k+1],  S12, 0xE8C7B756);
					c = FF(c, d, a, b, x[k+2],  S13, 0x242070DB);
					b = FF(b, c, d, a, x[k+3],  S14, 0xC1BDCEEE);
					a = FF(a, b, c, d, x[k+4],  S11, 0xF57C0FAF);
					d = FF(d, a, b, c, x[k+5],  S12, 0x4787C62A);
					c = FF(c, d, a, b, x[k+6],  S13, 0xA8304613);
					b = FF(b, c, d, a, x[k+7],  S14, 0xFD469501);
					a = FF(a, b, c, d, x[k+8],  S11, 0x698098D8);
					d = FF(d, a, b, c, x[k+9],  S12, 0x8B44F7AF);
					c = FF(c, d, a, b, x[k+10], S13, 0xFFFF5BB1);
					b = FF(b, c, d, a, x[k+11], S14, 0x895CD7BE);
					a = FF(a, b, c, d, x[k+12], S11, 0x6B901122);
					d = FF(d, a, b, c, x[k+13], S12, 0xFD987193);
					c = FF(c, d, a, b, x[k+14], S13, 0xA679438E);
					b = FF(b, c, d, a, x[k+15], S14, 0x49B40821);
					a = GG(a, b, c, d, x[k+1],  S21, 0xF61E2562);
					d = GG(d, a, b, c, x[k+6],  S22, 0xC040B340);
					c = GG(c, d, a, b, x[k+11], S23, 0x265E5A51);
					b = GG(b, c, d, a, x[k+0],  S24, 0xE9B6C7AA);
					a = GG(a, b, c, d, x[k+5],  S21, 0xD62F105D);
					d = GG(d, a, b, c, x[k+10], S22, 0x2441453);
					c = GG(c, d, a, b, x[k+15], S23, 0xD8A1E681);
					b = GG(b, c, d, a, x[k+4],  S24, 0xE7D3FBC8);
					a = GG(a, b, c, d, x[k+9],  S21, 0x21E1CDE6);
					d = GG(d, a, b, c, x[k+14], S22, 0xC33707D6);
					c = GG(c, d, a, b, x[k+3],  S23, 0xF4D50D87);
					b = GG(b, c, d, a, x[k+8],  S24, 0x455A14ED);
					a = GG(a, b, c, d, x[k+13], S21, 0xA9E3E905);
					d = GG(d, a, b, c, x[k+2],  S22, 0xFCEFA3F8);
					c = GG(c, d, a, b, x[k+7],  S23, 0x676F02D9);
					b = GG(b, c, d, a, x[k+12], S24, 0x8D2A4C8A);
					a = HH(a, b, c, d, x[k+5],  S31, 0xFFFA3942);
					d = HH(d, a, b, c, x[k+8],  S32, 0x8771F681);
					c = HH(c, d, a, b, x[k+11], S33, 0x6D9D6122);
					b = HH(b, c, d, a, x[k+14], S34, 0xFDE5380C);
					a = HH(a, b, c, d, x[k+1],  S31, 0xA4BEEA44);
					d = HH(d, a, b, c, x[k+4],  S32, 0x4BDECFA9);
					c = HH(c, d, a, b, x[k+7],  S33, 0xF6BB4B60);
					b = HH(b, c, d, a, x[k+10], S34, 0xBEBFBC70);
					a = HH(a, b, c, d, x[k+13], S31, 0x289B7EC6);
					d = HH(d, a, b, c, x[k+0],  S32, 0xEAA127FA);
					c = HH(c, d, a, b, x[k+3],  S33, 0xD4EF3085);
					b = HH(b, c, d, a, x[k+6],  S34, 0x4881D05);
					a = HH(a, b, c, d, x[k+9],  S31, 0xD9D4D039);
					d = HH(d, a, b, c, x[k+12], S32, 0xE6DB99E5);
					c = HH(c, d, a, b, x[k+15], S33, 0x1FA27CF8);
					b = HH(b, c, d, a, x[k+2],  S34, 0xC4AC5665);
					a = II(a, b, c, d, x[k+0],  S41, 0xF4292244);
					d = II(d, a, b, c, x[k+7],  S42, 0x432AFF97);
					c = II(c, d, a, b, x[k+14], S43, 0xAB9423A7);
					b = II(b, c, d, a, x[k+5],  S44, 0xFC93A039);
					a = II(a, b, c, d, x[k+12], S41, 0x655B59C3);
					d = II(d, a, b, c, x[k+3],  S42, 0x8F0CCC92);
					c = II(c, d, a, b, x[k+10], S43, 0xFFEFF47D);
					b = II(b, c, d, a, x[k+1],  S44, 0x85845DD1);
					a = II(a, b, c, d, x[k+8],  S41, 0x6FA87E4F);
					d = II(d, a, b, c, x[k+15], S42, 0xFE2CE6E0);
					c = II(c, d, a, b, x[k+6],  S43, 0xA3014314);
					b = II(b, c, d, a, x[k+13], S44, 0x4E0811A1);
					a = II(a, b, c, d, x[k+4],  S41, 0xF7537E82);
					d = II(d, a, b, c, x[k+11], S42, 0xBD3AF235);
					c = II(c, d, a, b, x[k+2],  S43, 0x2AD7D2BB);
					b = II(b, c, d, a, x[k+9],  S44, 0xEB86D391);
					a = addUnsigned(a, AA);
					b = addUnsigned(b, BB);
					c = addUnsigned(c, CC);
					d = addUnsigned(d, DD);
				}
				var tempValue = wordToHex(a) + wordToHex(b) + wordToHex(c) + wordToHex(d);
				return tempValue.toLowerCase();
			}
		});
	})(jQuery);

;
(function () {
    'use strict';
    document.addEventListener('DOMContentLoaded', function () {
        var template = document.getElementById('site-account-template');
        if (!template || document.getElementById('site-account-dialog')) return;
        document.body.appendChild(template.content.cloneNode(true));
        var dialog = document.getElementById('site-account-dialog');
        var form = document.getElementById('site-account-form');
        var message = dialog.querySelector('.site-account-status');
        var user = null;
        var busy = false;
        var lastTrigger;

        function status(text) { message.textContent = text || ''; }
        function render(nextUser) {
            var before = user ? String(user.id || user.name) : '';
            var after = nextUser ? String(nextUser.id || nextUser.name) : '';
            user = nextUser;
            form.hidden = !!user;
            document.querySelectorAll('[data-account-entry]').forEach(function (entry) {
                var label = user ? user.name : dialog.dataset.loginLabel;
                entry.querySelector('.site-account-label').textContent = label;
                entry.title = label;
                entry.setAttribute('aria-label', label);
                entry.closest('.site-account-menu').classList.toggle('is-signed-in', !!user);
                entry.setAttribute('aria-haspopup', user ? 'true' : 'dialog');
                entry.setAttribute('aria-expanded', 'false');
                if (user) entry.removeAttribute('aria-controls');
                else entry.setAttribute('aria-controls', 'site-account-dialog');
            });
            closeMenus();
            if (before !== after) window.dispatchEvent(new CustomEvent('site-account-change', { detail: { user: user } }));
        }
        function setBusy(value) {
            busy = value;
            dialog.querySelectorAll('.site-account-submit').forEach(function (button) { button.disabled = value; });
            document.querySelectorAll('.site-account-dropdown button').forEach(function (button) { button.disabled = value; });
            form.setAttribute('aria-busy', String(value));
        }
        function request(path, body) {
            return fetch('/account/' + path, {
                method: 'POST',
                credentials: 'same-origin', cache: 'no-store',
                headers: { 'Content-Type': 'application/json', 'X-Site-Request': '1' },
                body: JSON.stringify(body)
            }).then(function (response) {
                if (!response.ok) {
                    var error = new Error('Account request failed');
                    error.status = response.status;
                    throw error;
                }
                return response.json().then(function (result) {
                    if (!result || result.code !== 200) {
                        var error = new Error(result && result.msg || 'Account request failed');
                        error.status = result && result.code;
                        throw error;
                    }
                    return result.data;
                });
            });
        }
        function refresh() {
            var name = document.cookie.split('; ').find(function (cookie) {
                return cookie.indexOf('site_account_name=') === 0;
            });
            try { name = name ? decodeURIComponent(name.substring('site_account_name='.length)) : ''; }
            catch (error) { name = ''; }
            // 名称只是展示缓存；后续写操作始终由后端检查 HttpOnly token。
            render(name ? (user && user.name === name ? user : { name: name }) : null);
        }
        function open() {
            status('');
            refresh();
            if (!dialog.open) dialog.showModal();
            if (!user) form.elements.account.focus();
        }
        function close() {
            dialog.close();
        }
        function closeMenus() {
            document.querySelectorAll('.site-account-menu').forEach(function (menu) {
                menu.querySelector('.site-account-dropdown').hidden = true;
                menu.querySelector('[data-account-entry]').setAttribute('aria-expanded', 'false');
            });
        }
        document.addEventListener('click', function (event) {
            var entry = event.target.closest('[data-account-entry]');
            if (entry) {
                lastTrigger = entry;
                var dropdown = entry.closest('.site-account-menu').querySelector('.site-account-dropdown');
                var expanded = !dropdown.hidden;
                refresh();
                if (user) {
                    dropdown.hidden = expanded;
                    entry.setAttribute('aria-expanded', String(!expanded));
                } else open();
            } else if (event.target.closest('.site-account-dropdown [data-account-logout]')) {
                logout();
            } else {
                closeMenus();
            }
        });
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') closeMenus();
        });
        dialog.querySelector('[data-account-close]').addEventListener('click', close);
        dialog.addEventListener('close', function () {
            form.elements.password.value = '';
            if (lastTrigger) lastTrigger.focus();
        });
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            if (busy || !form.reportValidity()) return;
            var account = form.elements.account.value.trim();
            if (!account || !form.elements.password.value.trim()) return;
            if (!window.jQuery || typeof window.jQuery.md5 !== 'function') {
                status(dialog.dataset.network);
                return;
            }
            var password = window.jQuery.md5(form.elements.password.value).toLowerCase();
            setBusy(true);
            status('');
            request('login', { account: account, password: password })
                .then(function (result) {
                    render(result);
                    form.reset();
                    close();
                }).catch(function (error) {
                    status(error.status === 401 ? dialog.dataset.rejected : dialog.dataset.network);
                }).finally(function () { setBusy(false); });
        });
        function logout() {
            if (busy) return;
            setBusy(true);
            status('');
            request('logout', {}).then(function () {
                render(null);
                form.reset();
                closeMenus();
                close();
                if (lastTrigger) lastTrigger.focus();
            }).catch(function () { open(); status(dialog.dataset.network); })
                .finally(function () { setBusy(false); });
        }
        // 后续评论组件可打开同一登录框并监听 site-account-change，正文无需刷新。
        window.SiteAccount = {
            open: open,
            refresh: refresh,
            getUser: function () { return user; },
            invalidate: function () {
                document.cookie = 'site_account_name=; Path=/; Max-Age=0; SameSite=Lax' + (location.protocol === 'https:' ? '; Secure' : '');
                render(null);
            }
        };
        window.addEventListener('pageshow', function (event) {
            if (event.persisted) refresh();
        });
        window.addEventListener('focus', function () {
            if (!busy) refresh();
        });
        refresh();
    });
}());

;
(function () {
    'use strict';
    document.addEventListener('DOMContentLoaded', function () {
        var root = document.getElementById('site-comments');
        if (!root) return;
        var words = root.querySelector('.site-comment-labels').dataset;
        var list = root.querySelector('.site-comment-list');
        var status = root.querySelector('.site-comments-status');
        var more = root.querySelector('.site-comments-more');
        var cursor = 0, hasMore = true, loading = false, failed = false, identityGeneration = 0;
        var seen = new Set(), replyTarget = 0, currentUserId = null;
        var scrollBox = root.querySelector('.site-comments-pagination');
        var retry = root.querySelector('.site-comments-retry');
        var loadStatus = root.querySelector('.site-comment-load-status');
        var automatic = typeof window.IntersectionObserver === 'function';
        var mainForm = root.querySelector('[data-comment-form]');
        var mainText = mainForm.querySelector('textarea');
        var resize = window.ResizeObserver ? new ResizeObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.target._measure) entry.target._measure();
            });
        }) : null;
        var share = document.querySelector('.site-share');
        var shareToggle = document.querySelector('[data-share-toggle]');
        var menu = document.getElementById('site-share-menu');
        var toast = share && share.querySelector('.site-share-status');
        var toastTimer;

        function element(tag, className, text) {
            var node = document.createElement(tag);
            if (className) node.className = className;
            // 评论正文及昵称按纯文本显示，防止 HTML 执行；接口保留原文，勿重复转义或改用 innerHTML。
            if (text !== undefined) node.textContent = text;
            return node;
        }
        function button(className, text) {
            var node = element('button', className, text);
            node.type = 'button';
            return node;
        }
        function total(value) {
            document.querySelectorAll('[data-comment-total]').forEach(function (node) { node.textContent = value; });
        }
        function isLiked(value) { return value === true || value === 1 || value === '1'; }
        function signedIn() { return window.SiteAccount && window.SiteAccount.getUser(); }
        function login() { if (window.SiteAccount) window.SiteAccount.open(); }
        function api(action, body) {
            var payload = Object.assign({ type: root.dataset.type, id: Number(root.dataset.id) }, body || {});
            var controller = window.AbortController ? new AbortController() : null;
            var timeout = controller ? setTimeout(function () { controller.abort(); }, action === 'like' || action === 'states' ? 15000 : 20000) : null;
            return fetch('/comments/' + action, {
                signal: controller ? controller.signal : undefined,
                method: 'POST', credentials: 'same-origin', cache: 'no-store',
                headers: { 'Content-Type': 'application/json', 'X-Site-Request': '1' },
                body: JSON.stringify(payload)
            }).then(function (response) {
                if (response.status === 401) {
                    if (window.SiteAccount && window.SiteAccount.invalidate) window.SiteAccount.invalidate();
                    login();
                }
                if (!response.ok) throw new Error('Request failed');
                return response.json();
            }).then(function (result) {
                if (!result || result.code !== 200) {
                    if (result && result.code === 401) {
                        if (window.SiteAccount && window.SiteAccount.invalidate) window.SiteAccount.invalidate();
                        login();
                    }
                    throw new Error(result && result.msg || 'Request failed');
                }
                return result.data;
            }).finally(function () { clearTimeout(timeout); });
        }
        function expandText(text, container) {
            var toggle = button('site-comment-expand', words.expand);
            toggle.hidden = true;
            toggle.setAttribute('aria-expanded', 'false');
            container.appendChild(toggle);
            toggle.addEventListener('click', function () {
                var expanded = text.classList.toggle('is-expanded');
                toggle.textContent = expanded ? words.collapse : words.expand;
                toggle.setAttribute('aria-expanded', String(expanded));
            });
            var measure = function () {
                if (!text.isConnected || text.classList.contains('is-expanded')) return;
                toggle.hidden = text.scrollHeight <= text.clientHeight + 1;
            };
            requestAnimationFrame(measure);
            if (resize) { text._measure = measure; resize.observe(text); }
        }
        function syncAuth() {
            var account = signedIn();
            mainText.readOnly = !account;
            mainForm.querySelector('button[type=submit], button[type=button]').type = account ? 'submit' : 'button';
            list.querySelectorAll('[data-parent-comment]').forEach(function (node) {
                if (node._syncReply) node._syncReply();
            });
        }
        function bindMainForm() {
            var submit = mainForm.querySelector('button');
            var message = mainForm.querySelector('.site-comment-form-status');
            var busy = false;
            mainForm.addEventListener('click', function () { if (!signedIn()) login(); });
            mainText.addEventListener('input', function () { if (!mainText.value) replyTarget = 0; });
            mainForm.addEventListener('submit', function (event) {
                event.preventDefault();
                if (busy || !signedIn()) { if (!signedIn()) login(); return; }
                var content = mainText.value.trim();
                if (!content || mainText.value.length > 1000) return;
                busy = true; submit.disabled = true; message.textContent = '';
                mainForm.setAttribute('aria-busy', 'true');
                api('add', { parentId: replyTarget, content: content }).then(function () {
                    // 与 back 一致，发表及回复成功后刷新，统一恢复列表、总数和回复状态。
                    window.location.reload();
                }).catch(function () { message.textContent = words.submitFailed; })
                    .finally(function () {
                        // 与 back 一致，请求结束清除回复目标，避免下一条主评论沿用旧 parentId。
                        replyTarget = 0; busy = false; submit.disabled = false; mainForm.setAttribute('aria-busy', 'false');
                    });
            });
            syncAuth();
        }
        function replyTo(row) {
            if (!signedIn()) { login(); return; }
            replyTarget = Number(row.id);
            var prefix = row.name ? '@' + row.name + ' ' : '';
            mainText.value = prefix;
            mainForm.querySelector('.site-comment-form-status').textContent = '';
            requestAnimationFrame(function () {
                var header = document.querySelector('.m-hd .am-sticky');
                var offset = header && getComputedStyle(header).position === 'fixed' ? header.offsetHeight : 0;
                window.scrollTo({ top: Math.max(0, window.scrollY + mainForm.getBoundingClientRect().top - offset - 20), behavior: 'smooth' });
                mainText.focus({ preventScroll: true });
                mainText.setSelectionRange(prefix.length, prefix.length);
            });
        }
        function displayDate(value) {
            var match = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(value || ''));
            return match ? match[2] + '/' + match[3] + '/' + match[1] : String(value || '');
        }
        function advancesCursor(next, previous) {
            next = String(next); previous = String(previous || 0);
            return /^[1-9]\d*$/.test(next) && (previous === '0' || next.length < previous.length || (next.length === previous.length && next < previous));
        }
        function validPage(page, before) {
            if (!page || !Array.isArray(page.items) || typeof page.hasMore !== 'boolean'
                    || (page.hasMore && (!page.items.length || !advancesCursor(page.nextCursor, before)))) {
                throw new Error('Invalid comment page');
            }
        }
        function comment(row, child) {
            var node = element('article', 'site-comment'); node.dataset.commentId = row.id;
            node._likeVersion = 0;
            if (child) node.classList.add('site-comment-child');
            var card = child ? node : element('div', 'site-comment-card');
            if (!child) node.appendChild(card);
            var rowNode = element('div', 'site-comment-row'); card.appendChild(rowNode);
            var avatar = element('span', 'site-comment-avatar', String(row.name || 'U').charAt(0));
            if (row.avatar) {
                try {
                    var url = new URL(row.avatar, location.origin);
                    if (url.protocol === 'https:' || url.protocol === 'http:') {
                        var img = element('img', 'site-comment-avatar'); img.src = url.href; img.alt = '';
                        img.loading = 'lazy'; img.referrerPolicy = 'no-referrer';
                        img.addEventListener('error', function () { img.replaceWith(avatar); });
                        rowNode.appendChild(img);
                    } else rowNode.appendChild(avatar);
                } catch (error) { rowNode.appendChild(avatar); }
            } else rowNode.appendChild(avatar);
            var body = element('div', 'site-comment-body'); rowNode.appendChild(body);
            var meta = element('div', 'site-comment-meta');
            meta.appendChild(element('span', 'site-comment-name', row.name || 'User'));
            var time = element('time', '', displayDate(row.created)); meta.appendChild(time); body.appendChild(meta);
            var text = element('p', 'site-comment-text', row.content || ''); body.appendChild(text); expandText(text, body);
            var actions = element('div', 'site-comment-actions'); body.appendChild(actions);
            var like = button('site-comment-action', '');
            var icon = element('span', 'am-icon-thumbs-o-up'); icon.setAttribute('aria-hidden', 'true');
            like.appendChild(icon);
            var count = element('span', '', ' ' + (row.likeNum || 0)); like.appendChild(count);
            like.setAttribute('aria-label', words.like); like.setAttribute('aria-pressed', String(isLiked(row.isLiked)));
            node._setLike = function (state) {
                like.setAttribute('aria-pressed', String(isLiked(state.isLiked)));
                count.textContent = ' ' + state.likeNum;
            };
            actions.appendChild(like);
            like.addEventListener('click', function () {
                if (!signedIn()) { login(); return; }
                if (like.disabled) return;
                var next = like.getAttribute('aria-pressed') !== 'true';
                var identity = identityGeneration;
                node._likeVersion++;
                node._likePending = true;
                like.disabled = true;
                api('like', { commentId: Number(row.id), liked: next }).then(function (result) {
                    if (identity === identityGeneration) node._setLike(result);
                }).catch(function () { /* 点赞失败时保留原状态。 */ })
                    .finally(function () {
                        node._likeVersion++; node._likePending = false; like.disabled = false;
                        if (identity !== identityGeneration) refreshStates();
                    });
            });
            if (!child) {
                node.setAttribute('data-parent-comment', '');
                var reply = button('site-comment-action', '');
                var replyIcon = element('span', 'am-icon-comment-o'); replyIcon.setAttribute('aria-hidden', 'true');
                reply.appendChild(replyIcon); reply.appendChild(element('span', '', words.reply));
                actions.appendChild(reply);
                node._syncReply = function () {
                    reply.hidden = currentUserId !== null && String(currentUserId) === String(row.uid);
                };
                node._syncReply();
                reply.addEventListener('click', function () { replyTo(row); });
                var thread = element('div', 'site-comment-thread'); node.appendChild(thread);
                var replies = element('div', 'site-comment-replies'); thread.appendChild(replies);
                var page = row.replies || { items: [], hasMore: false, nextCursor: 0 };
                var replyIds = new Set();
                function append(items) {
                    (items || []).forEach(function (item) {
                        var key = String(item.id);
                        if (!replyIds.has(key)) { replies.appendChild(comment(item, true)); replyIds.add(key); }
                    });
                }
                append(page.items);
                var replyMore = button('site-replies-more', '');
                var label = element('span', '', words.moreReplies);
                var arrow = element('i', 'site-replies-icon'); arrow.setAttribute('aria-hidden', 'true');
                replyMore.appendChild(label); replyMore.appendChild(arrow);
                replyMore.hidden = !page.hasMore; thread.appendChild(replyMore);
                thread.hidden = !replies.children.length && !page.hasMore;
                replyMore.addEventListener('click', function () {
                    if (replyMore.disabled) return;
                    replyMore.disabled = true; replyMore.setAttribute('aria-busy', 'true');
                    replyMore.classList.add('is-loading'); label.textContent = words.loading; loadStatus.textContent = words.loading;
                    var identity = identityGeneration;
                    api('replies', { parentId: Number(row.id), before: page.nextCursor }).then(function (result) {
                        validPage(result, page.nextCursor);
                        append(result.items); page = result; replyMore.hidden = !page.hasMore; loadStatus.textContent = '';
                        if (identity !== identityGeneration) refreshStates();
                    }).catch(function () { loadStatus.textContent = words.failed; })
                        .finally(function () {
                            replyMore.disabled = false; replyMore.removeAttribute('aria-busy');
                            replyMore.classList.remove('is-loading'); label.textContent = words.moreReplies;
                        });
                });
            }
            return node;
        }
        function showLoadState(text) {
            status.textContent = text;
            scrollBox.hidden = !text && !hasMore;
            retry.hidden = !failed;
            more.hidden = automatic || failed || !hasMore;
        }
        function checkBottom() {
            if (!automatic || loading || failed || !hasMore || scrollBox.hidden) return;
            var box = scrollBox.getBoundingClientRect();
            if (box.top < window.innerHeight && box.bottom > 0) load();
        }
        function load() {
            if (loading || !hasMore) return;
            var identity = identityGeneration;
            var before = cursor;
            loading = true; failed = false; more.disabled = true;
            list.setAttribute('aria-busy', 'true'); showLoadState(words.loading);
            api('list', { before: before }).then(function (result) {
                validPage(result, before);
                (result.items || []).forEach(function (row) {
                    var key = String(row.id);
                    if (!seen.has(key)) { list.appendChild(comment(row, false)); seen.add(key); }
                });
                cursor = result.nextCursor; hasMore = result.hasMore;
                currentUserId = result.currentUserId == null ? null : result.currentUserId;
                syncAuth(); total(result.totalCount);
                showLoadState(hasMore ? words.ready : (before > 0 ? words.end : ''));
                if (!hasMore && observer) observer.disconnect();
                if (identity !== identityGeneration) refreshStates();
            }).catch(function () {
                failed = true; showLoadState(words.failed);
                if (!seen.size) total('—');
            }).finally(function () {
                loading = false; more.disabled = false; more.textContent = words.more;
                list.setAttribute('aria-busy', 'false');
                if (!failed && hasMore) requestAnimationFrame(checkBottom);
            });
        }
        bindMainForm();
        more.textContent = words.more; retry.textContent = words.retry;
        more.addEventListener('click', load);
        retry.addEventListener('click', function () { failed = false; load(); });
        var observer = automatic ? new IntersectionObserver(function (entries) {
            if (entries.some(function (entry) { return entry.isIntersecting; })) checkBottom();
        }, { rootMargin: '0px' }) : null;
        if (observer) observer.observe(scrollBox);
        function refreshStates() {
            var nodes = Array.from(list.querySelectorAll('[data-comment-id]'));
            var identity = identityGeneration;
            // 原地刷新个人状态，保留已加载页、展开的回复和未提交正文。
            nodes.forEach(function (node) { if (node._setLike && !node._likePending) node._setLike({ isLiked: false, likeNum: node.querySelector('.site-comment-action span:last-child').textContent.trim() }); });
            for (var offset = 0; offset < nodes.length; offset += 100) {
                (function (batch) {
                    var versions = {};
                    batch.forEach(function (node) { versions[node.dataset.commentId] = node._likeVersion; });
                    api('states', { ids: batch.map(function (node) { return Number(node.dataset.commentId); }) }).then(function (result) {
                        if (identity !== identityGeneration) return;
                        (result.items || []).forEach(function (state) {
                            var node = list.querySelector('[data-comment-id="' + state.id + '"]');
                            if (node && node._setLike && !node._likePending && node._likeVersion === versions[state.id]) node._setLike(state);
                        });
                        currentUserId = result.currentUserId == null ? null : result.currentUserId; syncAuth();
                    }).catch(function () { loadStatus.textContent = words.failed; });
                })(nodes.slice(offset, offset + 100));
            }
        }
        function identityChanged() {
            identityGeneration++;
            var account = signedIn(); currentUserId = account && account.id != null ? account.id : null;
            syncAuth(); refreshStates();
        }
        window.addEventListener('site-account-change', identityChanged);
        window.addEventListener('focus', identityChanged);

        function closeShare(focus) {
            if (!menu) return;
            menu.hidden = true; shareToggle.setAttribute('aria-expanded', 'false');
            if (focus) shareToggle.focus();
        }
        function fitOverlay(node) {
            node.style.transform = '';
            var box = node.getBoundingClientRect();
            var shift = box.right > window.innerWidth - 16 ? window.innerWidth - 16 - box.right : 0;
            if (box.left + shift < 16) shift += 16 - box.left - shift;
            if (shift) node.style.transform = 'translateX(' + shift + 'px)';
        }
        function notify(message) {
            clearTimeout(toastTimer); toast.textContent = message; toast.hidden = false;
            fitOverlay(toast);
            toastTimer = setTimeout(function () { toast.hidden = true; }, 2000);
        }
        function fallbackCopy(url) {
            var area = element('textarea'); area.value = url;
            area.style.position = 'fixed'; area.style.opacity = '0'; document.body.appendChild(area); area.select();
            var success = false;
            try { success = document.execCommand('copy'); } catch (error) { success = false; }
            area.remove(); shareToggle.focus();
            notify(success ? words.copied : words.copyFailed + ' ' + url);
        }
        if (menu && shareToggle) {
            var url = location.href.split('#')[0], title = root.dataset.title || document.title;
            var encoded = encodeURIComponent(url), encodedTitle = encodeURIComponent(title);
            var links = {
                linkedin: 'https://www.linkedin.com/sharing/share-offsite/?url=' + encoded,
                whatsapp: 'https://wa.me/?text=' + encodeURIComponent(title + ' ' + url),
                facebook: 'https://www.facebook.com/sharer/sharer.php?u=' + encoded,
                x: 'https://twitter.com/intent/tweet?url=' + encoded + '&text=' + encodedTitle,
                email: 'mailto:?subject=' + encodedTitle + '&body=' + encoded
            };
            menu.querySelectorAll('a[data-share]').forEach(function (link) { link.href = links[link.dataset.share]; });
            shareToggle.addEventListener('click', function () {
                menu.hidden = !menu.hidden; shareToggle.setAttribute('aria-expanded', String(!menu.hidden));
                if (!menu.hidden) fitOverlay(menu);
            });
            window.addEventListener('resize', function () {
                if (!menu.hidden) fitOverlay(menu);
                if (!toast.hidden) fitOverlay(toast);
            });
            menu.querySelector('[data-share=copy]').addEventListener('click', function () {
                if (navigator.clipboard && navigator.clipboard.writeText) {
                    navigator.clipboard.writeText(url).then(function () { notify(words.copied); })
                        .catch(function () { fallbackCopy(url); });
                } else fallbackCopy(url);
            });
            menu.querySelectorAll('a').forEach(function (link) { link.addEventListener('click', function () { closeShare(false); }); });
            document.addEventListener('click', function (event) { if (!share.contains(event.target)) closeShare(false); });
            document.addEventListener('keydown', function (event) { if (event.key === 'Escape' && !menu.hidden) closeShare(true); });
            toast.addEventListener('mouseenter', function () { clearTimeout(toastTimer); });
            toast.addEventListener('mouseleave', function () { toastTimer = setTimeout(function () { toast.hidden = true; }, 2000); });
        }
        load();
    });
}());
