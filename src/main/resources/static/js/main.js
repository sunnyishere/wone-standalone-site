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
        let selectedIso2 = 'CN';

        // 保存所有选项引用
        countryOptions = [];

        // 设置选中样式的函数
        const setOptionSelected = (option, isSelected) => {
            if (isSelected) {
                option.style.backgroundColor = 'var(--theme-wone)';
                option.style.color = '#ffffff';
                option.style.fontWeight = '500';
            } else {
                option.style.backgroundColor = '';
                option.style.color = '';
                option.style.fontWeight = '';
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
                if (option.dataset.iso2 !== selectedIso2) {
                    option.style.backgroundColor = '#f5f5f5';
                }
            });

            option.addEventListener('mouseleave', () => {
                if (option.dataset.iso2 !== selectedIso2) {
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
                            const themeColor = getComputedStyle(document.documentElement)
                                .getPropertyValue('--theme-wone').trim() || '#4f46e5';
                            selectedOption.optionElement.style.backgroundColor = themeColor;
                            selectedOption.optionElement.style.color = '#ffffff';
                            selectedOption.optionElement.style.fontWeight = '500';
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

    $('.submit-btn').on('click', function () {
        $('.form-error').remove();
        const phonePrefix = $('#phonePrefix').text().trim();
        const phoneNumber = $('#input-phone').val().trim();
        const fullPhone = phonePrefix + phoneNumber;

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

        if (!formData.orgName) {
            showError($('#input-orgName'), i18n.ruleOrgName);
            hasError = true;
        }

        if (!phoneNumber) {
            showError($('#input-phone'), i18n.rulePhone);
            hasError = true;
        }

        if (hasError) {
            return;
        }

        // 提交数据到后端
        const xhr = new XMLHttpRequest();
        xhr.open('POST', "/leaveMessage", true);
        xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');

        xhr.onload = function () {
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
            showNativeMsg(i18n.ruleNetwork);
        };

        // 构建请求参数
        const formDataParams = Object.keys(formData).map(key =>
            encodeURIComponent(key) + '=' + encodeURIComponent(formData[key])
        ).join('&');

        console.log('提交的参数：', formDataParams);
        xhr.send(formDataParams);
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


