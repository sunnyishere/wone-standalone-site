// /** 这个方案 未通过 保留代码
//  * 统一的「下载/打开 App」按钮事件
//  * 绑定方式：<button onclick="downloadAppHandler()" >立即体验</button>
//  */
// function downloadAppHandler() {
//     // 移到最前面确保执行
//     alert('开始处理下载/打开App请求...');
//
//     /* ========== 0. 预置参数（已替换为你提供的实际链接） ========== */
//     const schemeUrl = 'yourapp://home';          // App 自己注册的 scheme（需按实际配置修改）
//     const universalLink = 'https://ul.yourdomain.com/app'; // iOS Universal Link（如有，需按实际修改）
//     const iosAppStoreId = 'id6503928704';        // App Store 的 id（与你的链接一致）
//     const androidPkg = 'cn.iwone.pwuniapp';      // Android 包名（与你的链接一致）
//
//     // 👇 替换为你提供的各应用市场「客户端链接」和「网页版链接」
//     const androidMarket = 'market://details?id=' + androidPkg; // 系统默认应用市场（通用）
//     const huaweiMarket = 'appmarket://details?id=' + androidPkg; // 华为应用市场客户端（唤起用）
//     const huaweiMarketWeb = 'https://url.cloud.huawei.com/rf51G1mfg4?shareTo=qrcode'; // 你的华为云链接
//     const xiaomiMarket = 'mimarket://details?id=' + androidPkg; // 小米应用市场客户端（唤起用）
//     const xiaomiMarketWeb = 'https://global.app.mi.com/details?lo=ID&la=en_US&id=' + androidPkg; // 你的小米全球商店链接
//     const oppoMarket = 'oppomarket://details?id=' + androidPkg; // OPPO应用市场客户端（唤起用）
//     const oppoMarketWeb = 'https://open.oppomobile.com/market/index.html#/detail?packagename=' + androidPkg; // OPPO网页版（基于包名生成，与你的需求匹配）
//     const vivoMarket = 'vivomarket://details?id=' + androidPkg; // VIVO应用市场客户端（唤起用）
//     const vivoMarketWeb = 'https://app.vivo.com.cn/detail/' + androidPkg; // VIVO网页版（基于包名生成，与你的需求匹配）
//     const googleMarketWeb = 'https://play.google.com/store/apps/details?id=' + androidPkg; // 你的Google Play链接
//     const samsungMarketWeb = 'https://galaxy.store/wone'; // 你的三星应用商店链接
//     const palmMarketWeb = 'https://www.palmstore.net/app/' + androidPkg; // Palm Store网页版（基于包名生成，与你的需求匹配）
//
//     /* ========== 1. 环境判断（新增三星、Palm设备检测，优化精准度） ========== */
//     const ua = navigator.userAgent.toLowerCase();
//     const isWechat = /micromessenger/.test(ua);
//     const isIos = /iphone|ipad|ipod/.test(ua);
//     const isAndroid = /android/.test(ua);
//     const isHuawei = /huawei|honor/.test(ua); // 华为/荣耀设备
//     const isXiaomi = /mi\s|xiaomi|redmi/.test(ua); // 小米/红米设备（补充红米检测）
//     const isOppo = /oppo|realme/.test(ua); // OPPO/realme设备（补充realme检测）
//     const isVivo = /vivo|iqoo/.test(ua); // VIVO/iQOO设备（补充iQOO检测）
//     const isSamsung = /samsung|galaxy/.test(ua); // 三星设备（新增）
//     const isPalm = /palm/.test(ua); // Palm设备（新增，按需调整）
//
//     // 添加调试信息（新增三星、Palm检测结果）
//     console.log('环境检测:', {
//         isWechat, isIos, isAndroid,
//         isHuawei, isXiaomi, isOppo, isVivo, isSamsung, isPalm
//     });
//
//     /* ========== 2. 微信内提示（优化文案，更友好） ========== */
//     if (isWechat) {
//         if (confirm('微信内无法直接打开App，请点击右上角「...」，选择「在浏览器中打开」继续下载~')) return;
//     }
//
//     /* ========== 3. 唤起逻辑（核心逻辑不变，仅优化降级链接） ========== */
//     const start = Date.now();
//     let timer = null;
//
//     // 页面隐藏时清除定时器（唤起成功后终止降级）
//     const onVisibilityChange = () => {
//         if (document.hidden) {
//             clearTimeout(timer);
//             document.removeEventListener('visibilitychange', onVisibilityChange);
//         }
//     };
//     document.addEventListener('visibilitychange', onVisibilityChange);
//
//     // 统一“失败降级”函数（👇 替换为你的链接，按设备精准跳转）
//     const fallback = () => {
//         const cost = Date.now() - start;
//         if (cost < 2500) return; // 时间过短说明唤起中，不执行降级
//         clearTimeout(timer);
//         document.removeEventListener('visibilitychange', onVisibilityChange);
//
//         if (isIos) {
//             alert("IOS进入...")
//             // iOS：跳转你提供的App Store链接（原链接是赞比亚地区，如需默认国内可改为 https://apps.apple.com/cn/app/wone/id6503928704）
//             location.href = 'https://apps.apple.com/zm/app/wone/id6503928704';
//         } else if (isAndroid) {
//             // 根据设备类型，跳转你提供的对应市场链接
//             let marketUrl, webMarketUrl;
//
//             if (isHuawei) {
//                 marketUrl = huaweiMarket; // 华为客户端唤起
//                 webMarketUrl = huaweiMarketWeb; // 你的华为云链接（降级用）
//             } else if (isXiaomi) {
//                 marketUrl = xiaomiMarket; // 小米客户端唤起
//                 webMarketUrl = xiaomiMarketWeb; // 你的小米全球商店链接（降级用）
//             } else if (isOppo) {
//                 marketUrl = oppoMarket; // OPPO客户端唤起
//                 webMarketUrl = oppoMarketWeb; // OPPO网页版（降级用）
//             } else if (isVivo) {
//                 marketUrl = vivoMarket; // VIVO客户端唤起
//                 webMarketUrl = vivoMarketWeb; // VIVO网页版（降级用）
//             } else if (isSamsung) {
//                 marketUrl = 'samsungapps://ProductDetail/' + androidPkg; // 三星客户端唤起
//                 webMarketUrl = samsungMarketWeb; // 你的三星商店链接（降级用）
//             } else if (isPalm) {
//                 marketUrl = 'palmstore://details?id=' + androidPkg; // Palm客户端唤起（按需调整）
//                 webMarketUrl = palmMarketWeb; // Palm网页版（降级用）
//             } else {
//                 marketUrl = androidMarket; // 其他设备用系统默认市场
//                 webMarketUrl = googleMarketWeb; // 你的Google Play链接（降级用）
//             }
//
//             // 先尝试唤起客户端，1秒后失败则跳转网页版
//             try {
//                 location.href = marketUrl;
//                 setTimeout(() => {
//                     // 若1秒后页面仍在当前页，说明唤起失败，跳转网页版
//                     if (Date.now() - start < 3500) location.href = webMarketUrl;
//                 }, 1000);
//             } catch (e) {
//                 location.href = webMarketUrl;
//             }
//         } else {
//             // PC/其他设备：默认跳转华为网页版（可按需改为Google Play）
//             location.href = huaweiMarketWeb;
//         }
//     };
//
//     // 2.5秒后仍在当前页，认为唤起失败，执行降级
//     timer = setTimeout(fallback, 2500);
//
//     // 真正唤起逻辑（不变，优先Universal Link，再用Scheme）
//     try {
//         if (isIos && universalLink) {
//             // iOS优先用Universal Link（需确保链接跨域+HTTPS，按实际配置修改）
//             location.href = universalLink;
//         } else {
//             // 通用Scheme唤起（iOS/Android客户端）
//             const ifr = document.createElement('iframe');
//             ifr.style.display = 'none';
//             ifr.src = schemeUrl;
//             document.body.appendChild(ifr);
//             setTimeout(() => document.body.removeChild(ifr), 300); // 300毫秒后移除iframe，避免残留
//         }
//     } catch (e) {
//         console.error('唤起App失败:', e);
//         fallback(); // 捕获错误后直接执行降级
//     }
// }

let appLaunchAttempted = false;

/**
 * 主唤端函数（绑定到按钮点击）
 */
function downloadAppHandler() {
    if (appLaunchAttempted) {
        window.location.reload();
        return;
    }
    appLaunchAttempted = true;

    const universalLink = 'https://www.iee-business.com/';
    const appStoreUrl = 'https://apps.apple.com/zm/app/iee-business/id6503928704';
    const androidScheme = 'iwone://home';
    const androidMarketUrl = 'https://play.google.com/store/apps/details?id=cn.iwone.pwuniapp';

    const ua = navigator.userAgent.toLowerCase();

    const isIos = /iphone|ipad|ipod/.test(ua);
    const isAndroid = /android/.test(ua);
    const isWechat = /micromessenger/.test(ua);

    /* ========== 1. 微信环境提示 ========== */
    if (isWechat) {
        alert('Please click the ... menu in the upper right corner and select Open in Browser to continue.');
        return;
    }

    /* ========== 2. 根据平台执行唤端 ========== */
    if (isIos) {
        tryIOS(universalLink, appStoreUrl);
    } else if (isAndroid) {
        // Android: 使用 scheme 唤起
        tryAndroid(androidScheme, androidMarketUrl);
    } else {
        window.location.href = isAndroid ? androidMarketUrl : appStoreUrl;
    }
}

/**
 * IOS端唤起逻辑
 */

function tryIOS(universalLink, appStoreUrl) {
    window.location.href = universalLink;

    setTimeout(() => {
        window.location.href = appStoreUrl;
    }, 2500);

    ['blur', 'pagehide'].forEach(event => {
        window.addEventListener(event, () => {
            clearTimeout(fallbackTimer);
        });
    });

    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'hidden') {
            clearTimeout(fallbackTimer);
        }
    });

    const fallbackTimer = setTimeout(() => {}, 0);
}

/**
 * 安卓端唤起逻辑
 */
function tryAndroid(scheme, marketUrl) {
    // 创建 iframe 唤起 APP
    const iframe = document.createElement('iframe');
    iframe.style.display = 'none';
    iframe.src = scheme;
    document.body.appendChild(iframe);

    // 1.5秒后跳应用市场
    setTimeout(() => {
        window.location.href = marketUrl;
    }, 1500);

    // 清理 iframe
    setTimeout(() => {
        if (iframe.parentNode) {
            document.body.removeChild(iframe);
        }
    }, 300);
}