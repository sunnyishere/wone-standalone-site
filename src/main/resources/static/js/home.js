
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
            let targetLangUrl = this.getAttribute('data-lang-url');
            if (!targetLangUrl){
                targetLangUrl = "";
            }

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

    (function () {
        var section = document.querySelector('.home-news-blog-section');
        if (!section) {
            return;
        }

        var tabs = section.querySelectorAll('[data-news-blog-tab]');
        var panels = section.querySelectorAll('[data-news-blog-panel]');
        var actions = section.querySelectorAll('[data-news-blog-action]');

        function activate(type) {
            var i;
            for (i = 0; i < tabs.length; i++) {
                var active = tabs[i].getAttribute('data-news-blog-tab') === type;
                tabs[i].classList.toggle('is-active', active);
                tabs[i].setAttribute('aria-selected', active ? 'true' : 'false');
            }
            for (i = 0; i < panels.length; i++) {
                var visible = panels[i].getAttribute('data-news-blog-panel') === type;
                panels[i].hidden = !visible;
            }
            for (i = 0; i < actions.length; i++) {
                actions[i].hidden = actions[i].getAttribute('data-news-blog-action') !== type;
            }
        }

        for (var i = 0; i < tabs.length; i++) {
            tabs[i].addEventListener('click', function () {
                activate(this.getAttribute('data-news-blog-tab'));
            });
        }

        activate('news');
    })();

    (function () {
        var form = document.getElementById('homeRequestForm');
        if (!form) {
            return;
        }

        var feedback = form.querySelector('.home-rfq-feedback');
        var submit = form.querySelector('.home-rfq-submit');
        var uploadArea = form.querySelector('#homeUploadArea');
        var fileInput = form.querySelector('#home-attachment');
        var uploadText = form.querySelector('#homeUploadText');
        var defaultUploadText = uploadText ? uploadText.textContent : 'Click to upload file';
        var uploadItems = form.querySelector('#homeUploadItems');
        var fileNameInput = form.querySelector('#home-fileName');
        var fileUrlInput = form.querySelector('#home-fileUrl');
        var countryInput = form.querySelector('#home-input-country');
        var countryTrigger = form.querySelector('#home-country-trigger');
        var countryText = form.querySelector('#home-country-text');
        var countryOptions = form.querySelector('#home-country-options');
        var countryDropdown = form.querySelector('#home-country-dropdown');
        var countrySearch = form.querySelector('#home-country-search');
        var countryEmpty = form.querySelector('#home-country-empty');
        var phonePrefix = form.querySelector('#home-phone-prefix');
        var uploadedFiles = [];
        var homeCountryInitialized = false;
        var allowedExtensions = ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx',
            '.txt', '.csv', '.zip', '.jpg', '.jpeg', '.png'];

        function getValidationMessage(key, fallback) {
            return typeof i18n !== 'undefined' && i18n[key] ? i18n[key] : fallback;
        }

        function clearFieldError(field) {
            var wrapper = field && field.closest('.home-rfq-field');
            if (!wrapper) {
                return;
            }
            wrapper.classList.remove('has-error');
            var oldError = wrapper.querySelector('.home-rfq-error');
            if (oldError) {
                oldError.remove();
            }
            field.removeAttribute('aria-invalid');
        }

        function showFieldError(field, message) {
            var wrapper = field && field.closest('.home-rfq-field');
            if (!wrapper) {
                return;
            }
            clearFieldError(field);
            wrapper.classList.add('has-error');
            field.setAttribute('aria-invalid', 'true');
            var error = document.createElement('div');
            error.className = 'home-rfq-error';
            error.textContent = message;
            wrapper.appendChild(error);
        }

        function setHomeCountry(name, data) {
            if (!countryInput || !countryText || !data) {
                return;
            }
            countryInput.value = data.iso2;
            countryInput.setAttribute('data-phone', data.phone);
            countryText.textContent = name;
            if (phonePrefix) {
                phonePrefix.textContent = '+' + data.phone;
            }
            if (countryOptions) {
                Array.prototype.forEach.call(countryOptions.querySelectorAll('.home-rfq-country-option'), function (option) {
                    var selected = option.getAttribute('data-iso2') === data.iso2;
                    option.classList.toggle('is-selected', selected);
                    option.setAttribute('aria-selected', selected ? 'true' : 'false');
                });
            }
            clearFieldError(countryTrigger);
        }

        function closeHomeCountryOptions() {
            if (!countryDropdown || !countryTrigger) {
                return;
            }
            countryDropdown.hidden = true;
            countryTrigger.setAttribute('aria-expanded', 'false');
        }

        function filterHomeCountries() {
            var query = countrySearch.value.trim().toLowerCase();
            var matches = 0;
            Array.prototype.forEach.call(countryOptions.querySelectorAll('.home-rfq-country-option'), function (option) {
                option.hidden = option.getAttribute('data-search').indexOf(query) === -1;
                if (!option.hidden) matches++;
            });
            countryEmpty.hidden = matches > 0;
            countryOptions.scrollTop = 0;
        }

        function initHomeCountrySelect() {
            if (homeCountryInitialized || !window.RFQ_COUNTRY_DATA || !countryOptions || !countryTrigger) {
                return;
            }
            homeCountryInitialized = true;
            Object.keys(window.RFQ_COUNTRY_DATA).forEach(function (name) {
                var data = window.RFQ_COUNTRY_DATA[name];
                var option = document.createElement('button');
                option.type = 'button';
                option.className = 'home-rfq-country-option';
                option.setAttribute('role', 'option');
                option.setAttribute('data-iso2', data.iso2);
                option.setAttribute('data-search', (name + ' ' + data.iso2 + ' +' + data.phone).toLowerCase());
                option.textContent = name;
                option.addEventListener('click', function () {
                    setHomeCountry(name, data);
                    closeHomeCountryOptions();
                    countryTrigger.focus();
                });
                countryOptions.appendChild(option);
            });
            setHomeCountry('China', window.RFQ_COUNTRY_DATA.China || {iso2: 'CN', phone: '86'});
        }

        if (countryTrigger && countryDropdown && countrySearch) {
            countryTrigger.addEventListener('click', function () {
                var opening = countryDropdown.hidden;
                countryDropdown.hidden = !opening;
                countryTrigger.setAttribute('aria-expanded', opening ? 'true' : 'false');
                if (opening) {
                    countrySearch.value = '';
                    filterHomeCountries();
                    countrySearch.focus();
                }
            });
            countrySearch.addEventListener('input', filterHomeCountries);
            form.querySelector('#home-country-select').addEventListener('keydown', function (event) {
                if (event.key === 'Escape') {
                    event.preventDefault();
                    closeHomeCountryOptions();
                    countryTrigger.focus();
                } else if (event.target === countrySearch && (event.key === 'Enter' || event.key === 'ArrowDown')) {
                    event.preventDefault();
                    var firstOption = countryOptions.querySelector('.home-rfq-country-option:not([hidden])');
                    if (firstOption) {
                        if (event.key === 'Enter') firstOption.click();
                        else firstOption.focus();
                    }
                }
            });
            form.querySelector('#home-country-select').addEventListener('focusout', function (event) {
                if (!this.contains(event.relatedTarget)) closeHomeCountryOptions();
            });
            document.addEventListener('click', function (event) {
                if (!form.querySelector('#home-country-select').contains(event.target)) {
                    closeHomeCountryOptions();
                }
            });
        }
        document.addEventListener('rfq-country-data-ready', initHomeCountrySelect);
        initHomeCountrySelect();

        Array.prototype.forEach.call(form.querySelectorAll('input[name="name"], input[name="email"], input[name="phone"], input[name="orgName"]'), function (field) {
            field.addEventListener('input', function () {
                clearFieldError(field);
            });
        });

        function syncUploadFields() {
            fileNameInput.value = uploadedFiles.filter(function (item) {
                return item.url;
            }).map(function (item) {
                return item.name;
            }).join(',');
            fileUrlInput.value = uploadedFiles.filter(function (item) {
                return item.url;
            }).map(function (item) {
                return item.url;
            }).join(',');
        }

        function selectUploadFile() {
            if (fileInput) {
                fileInput.click();
            }
        }

        function createUploadItem(file) {
            var record = {name: file.name, url: '', item: null};
            var item = document.createElement('div');
            item.className = 'home-rfq-upload-item';

            var name = document.createElement('div');
            name.className = 'home-rfq-upload-item-name';
            name.textContent = file.name;

            var progress = document.createElement('div');
            progress.className = 'home-rfq-upload-progress';
            var progressBar = document.createElement('div');
            progressBar.className = 'home-rfq-upload-progress-bar';
            progress.appendChild(progressBar);

            var status = document.createElement('div');
            status.className = 'home-rfq-upload-status';
            status.textContent = getValidationMessage('uploading', 'Uploading...');
            status.style.color = '#5c6675';

            var remove = document.createElement('button');
            remove.type = 'button';
            remove.className = 'home-rfq-upload-delete';
            remove.textContent = '¡Á';
            remove.title = 'Remove file';
            remove.setAttribute('aria-label', 'Remove ' + file.name);
            remove.addEventListener('click', function () {
                uploadedFiles = uploadedFiles.filter(function (uploaded) {
                    return uploaded !== record;
                });
                item.remove();
                syncUploadFields();
            });

            item.appendChild(name);
            item.appendChild(progress);
            item.appendChild(status);
            item.appendChild(remove);
            record.item = item;
            uploadedFiles.push(record);
            uploadItems.appendChild(item);
            return {record: record, progressBar: progressBar, status: status};
        }

        function uploadFile(file) {
            var extension = '.' + (file.name.split('.').pop() || '').toLowerCase();
            if (file.size > 20 * 1024 * 1024 || allowedExtensions.indexOf(extension) === -1) {
                feedback.textContent = 'File type or size is not supported: ' + file.name;
                feedback.style.color = '#c62828';
                return;
            }

            var uploadItem = createUploadItem(file);
            var uploadData = new FormData();
            uploadData.append('file', file);
            var xhr = new XMLHttpRequest();

            xhr.upload.onprogress = function (event) {
                if (event.lengthComputable) {
                    uploadItem.progressBar.style.width = ((event.loaded / event.total) * 100) + '%';
                }
            };

            xhr.onload = function () {
                var result;
                try {
                    result = JSON.parse(xhr.responseText);
                } catch (error) {
                    result = null;
                }

                if (result && (result.code === 200 || result.success) && result.url) {
                    uploadItem.record.url = result.url;
                    uploadItem.progressBar.style.width = '100%';
                    uploadItem.status.textContent = getValidationMessage('uploadSuccess', 'Upload successful!');
                    uploadItem.status.style.color = '#0dcb67';
                    syncUploadFields();
                } else {
                    uploadItem.status.textContent = getValidationMessage('uploadError', 'Upload failed');
                    uploadItem.status.style.color = '#c62828';
                }
            };

            xhr.onerror = function () {
                uploadItem.status.textContent = getValidationMessage('uploadError', 'Upload failed');
                uploadItem.status.style.color = '#c62828';
            };

            xhr.open('POST', '/upload', true);
            xhr.send(uploadData);
        }

        if (uploadArea && fileInput) {
            uploadArea.addEventListener('click', selectUploadFile);
            uploadArea.addEventListener('keydown', function (event) {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    selectUploadFile();
                }
            });
            fileInput.addEventListener('change', function () {
                var files = Array.prototype.slice.call(fileInput.files || []);
                fileInput.value = '';
                if (files.length) {
                    uploadText.textContent = defaultUploadText;
                }
                files.forEach(uploadFile);
            });
        }

        form.addEventListener('submit', function (event) {
            event.preventDefault();

            var nameField = form.querySelector('[name="name"]');
            var emailField = form.querySelector('[name="email"]');
            var phoneField = form.querySelector('[name="phone"]');
            var orgNameField = form.querySelector('[name="orgName"]');
            var name = nameField.value.trim();
            var email = emailField.value.trim();
            var phone = phoneField.value.trim();
            var firstInvalid = null;

            [nameField, emailField, phoneField, orgNameField, countryTrigger].forEach(clearFieldError);
            feedback.textContent = '';

            if (!name) {
                showFieldError(nameField, getValidationMessage('ruleName', 'Please enter your name'));
                firstInvalid = firstInvalid || nameField;
            }
            if (!email) {
                showFieldError(emailField, getValidationMessage('ruleEmail', 'Please enter your email'));
                firstInvalid = firstInvalid || emailField;
            } else if (!/^\S+@\S+\.\S+$/.test(email)) {
                showFieldError(emailField, getValidationMessage('ruleEmailFormat', 'Please enter a valid email address'));
                firstInvalid = firstInvalid || emailField;
            }
            if (!countryInput || !countryInput.value) {
                showFieldError(countryTrigger, getValidationMessage('ruleCountry', 'Please select your country'));
                firstInvalid = firstInvalid || countryTrigger;
            }

            if (firstInvalid) {
                firstInvalid.focus();
                return;
            }

            var params = new URLSearchParams();
            Array.prototype.forEach.call(form.querySelectorAll('[name]'), function (field) {
                if (field.type !== 'file') {
                    params.append(field.name, field.value);
                }
            });
            params.set('phone', phone ? (phonePrefix ? phonePrefix.textContent.trim() : '+86') + phone.replace(/^\+/, '') : '');

            submit.disabled = true;
            feedback.textContent = 'Sending...';
            feedback.style.color = '#5c6675';

            fetch('/leaveMessage', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'},
                body: params.toString()
            }).then(function (response) {
                return response.json();
            }).then(function (result) {
                if (result && result.code === 200) {
                    form.reset();
                    uploadedFiles = [];
                    if (fileNameInput) fileNameInput.value = '';
                    if (fileUrlInput) fileUrlInput.value = '';
                    if (uploadText) uploadText.textContent = defaultUploadText;
                    if (uploadItems) uploadItems.textContent = '';
                    setHomeCountry('China', (window.RFQ_COUNTRY_DATA && window.RFQ_COUNTRY_DATA.China) || {iso2: 'CN', phone: '86'});
                    feedback.textContent = 'Thank you. Your inquiry has been sent.';
                    feedback.style.color = '#168a42';
                } else {
                    throw new Error((result && result.msg) || 'Submit failed');
                }
            }).catch(function () {
                feedback.textContent = 'Unable to send your inquiry. Please try again.';
                feedback.style.color = '#c62828';
            }).finally(function () {
                submit.disabled = false;
            });
        });
    })();

(function () {
    'use strict';
// Keep the accepted URL formats aligned with YoutubeVideoUtils on the backend.
function youtubeVideoId(value) {
  const text = (value || '').trim()
  if (!text || text.length > 1000 || /[\s\\<>"{}|^`]/.test(text) || /%(?![0-9a-f]{2})/i.test(text)) return ''
  try {
    const parts = /^https:\/\/[^/?#]+([^?#]*)(?:\?([^#]*))?(?:#.*)?$/i.exec(text)
    if (!parts) return ''
    const url = new URL(text)
    if (url.protocol !== 'https:' || url.username || url.password || url.port) return ''
    const host = url.hostname.toLowerCase()
    const path = parts[1]
    let id = ''
    if (host === 'youtu.be' || host === 'www.youtu.be') {
      if (/^\/[A-Za-z0-9_-]{11}\/?$/.test(path)) id = path.slice(1, 12)
    } else if (['youtube.com', 'www.youtube.com', 'm.youtube.com'].includes(host)) {
      if (path === '/watch') {
        const values = ((parts[2] || '').match(/(?:^|&)v=[^&]*/g) || []).map(part => part.replace(/^&?v=/, ''))
        if (values.length !== 1) return ''
        id = values[0]
      } else if (/^\/(embed|shorts|live)\/[A-Za-z0-9_-]{11}\/?$/.test(path)) {
        id = path.split('/')[2]
      }
    } else if (['youtube-nocookie.com', 'www.youtube-nocookie.com'].includes(host) && /^\/embed\/[A-Za-z0-9_-]{11}\/?$/.test(path)) {
      id = path.split('/')[2]
    }
    return /^[A-Za-z0-9_-]{11}$/.test(id) ? id : ''
  } catch (e) {
    return ''
  }
}

function youtubeThumbnailUrl(id, level = 0) {
  if (!/^[A-Za-z0-9_-]{11}$/.test(id)) return ''
  const sizes = ['maxresdefault', 'hqdefault', 'mqdefault']
  return 'https://i.ytimg.com/vi/' + id + '/' + sizes[Math.min(2, Math.max(0, level))] + '.jpg'
}

function youtubeEmbedUrl(id) {
  return /^[A-Za-z0-9_-]{11}$/.test(id) ? 'https://www.youtube-nocookie.com/embed/' + id : ''
}

    function initialize() {
        var section = document.querySelector('.home-strength-section');
        if (!section) return;
        var video = section.querySelector('[data-strength-video]');
        if (!video) return;
        var id = youtubeVideoId(video.getAttribute('data-strength-video'));
        if (!id) { video.hidden = true; return; }
        var image = video.querySelector('.home-strength-cover');
        var placeholder = video.querySelector('.home-strength-cover-placeholder');
        var level = 0;
        function nextCover() {
            image.hidden = true;
            placeholder.hidden = false;
            if (level < 2) {
                level++;
                image.src = youtubeThumbnailUrl(id, level);
            }
        }
        image.addEventListener('error', nextCover);
        image.addEventListener('load', function () {
            if (image.naturalWidth <= 120) { nextCover(); return; }
            image.hidden = false;
            placeholder.hidden = true;
        });
        image.src = youtubeThumbnailUrl(id);
        var play = video.querySelector('[data-strength-play]');
        var dialog = section.querySelector('[data-strength-dialog]');
        var player = dialog.querySelector('[data-strength-player]');
        play.addEventListener('click', function (event) {
            // Older browsers retain the normal YouTube link as a fallback.
            if (typeof dialog.showModal !== 'function') return;
            event.preventDefault();
            if (dialog.open) return;
            var frame = document.createElement('iframe');
            frame.src = youtubeEmbedUrl(id) + '?autoplay=1';
            frame.title = 'Manufacturing video';
            frame.allow = 'autoplay; encrypted-media; picture-in-picture';
            frame.allowFullscreen = true;
            player.appendChild(frame);
            dialog.showModal();
        });
        dialog.querySelector('[data-strength-close]').addEventListener('click', function () { dialog.close(); });
        dialog.addEventListener('click', function (event) {
            if (event.target === dialog) dialog.close();
        });
        dialog.addEventListener('close', function () {
            while (player.firstChild) player.removeChild(player.firstChild);
            play.focus();
        });
    }
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initialize);
    else initialize();
})();

;
(function () {
    document.querySelectorAll('[data-strength-gallery]').forEach(function (gallery) {
        var track = gallery.querySelector('[data-gallery-track]');
        var items = track.querySelectorAll('.home-strength-gallery-item');
        var controls = gallery.querySelector('[data-gallery-controls]');
        var dots = gallery.querySelector('[data-gallery-dots]');
        var previous = gallery.querySelector('[data-gallery-prev]');
        var next = gallery.querySelector('[data-gallery-next]');
        var targets = [];
        var page = 0;
        var frame;

        function update() {
            var nearest = 0;
            targets.forEach(function (target, index) {
                if (Math.abs(track.scrollLeft - target) < Math.abs(track.scrollLeft - targets[nearest])) nearest = index;
            });
            page = nearest;
            previous.disabled = page === 0;
            next.disabled = page === targets.length - 1;
            Array.prototype.forEach.call(dots.children, function (dot, index) {
                dot.setAttribute('aria-current', index === page ? 'true' : 'false');
            });
        }

        function go(index) {
            var targetPage = Math.max(0, Math.min(targets.length - 1, index));
            track.scrollTo({ left: targets[targetPage], behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' });
        }

        function layout() {
            var step = window.matchMedia('(max-width: 768px)').matches ? 1 : 4;
            var max = Math.max(0, track.scrollWidth - track.clientWidth);
            var origin = items[0].getBoundingClientRect().left;
            targets = [];
            for (var i = 0; i < items.length; i += step) {
                var target = Math.min(max, items[i].getBoundingClientRect().left - origin);
                if (!targets.length || target > targets[targets.length - 1] + 1) targets.push(target);
            }
            dots.textContent = '';
            targets.forEach(function (_, index) {
                var dot = document.createElement('button');
                dot.type = 'button';
                dot.className = 'home-strength-gallery-dot';
                dot.setAttribute('aria-label', 'Gallery page ' + (index + 1));
                dot.addEventListener('click', function () { go(index); });
                dots.appendChild(dot);
            });
            controls.hidden = targets.length < 2;
            update();
        }

        if (!items.length) return;
        previous.addEventListener('click', function () { go(page - 1); });
        next.addEventListener('click', function () { go(page + 1); });
        track.addEventListener('keydown', function (event) {
            var destination;
            if (event.key === 'ArrowRight') destination = page + 1;
            if (event.key === 'ArrowLeft') destination = page - 1;
            if (event.key === 'Home') destination = 0;
            if (event.key === 'End') destination = targets.length - 1;
            if (destination !== undefined) { event.preventDefault(); go(destination); }
        });
        track.addEventListener('scroll', function () {
            cancelAnimationFrame(frame);
            frame = requestAnimationFrame(update);
        }, { passive: true });
        if (typeof ResizeObserver !== 'undefined') new ResizeObserver(layout).observe(track);
        else window.addEventListener('resize', layout);
        layout();
    });
})();
