// config.js
const Config = {
    themeColor: '#FF5656',
    init() {
        document.documentElement.style.setProperty('--theme-wone', this.themeColor);
    },
    setThemeColor(color) {
        this.themeColor = color;
        document.documentElement.style.setProperty('--theme-wone', color);
    }
};

export default Config;