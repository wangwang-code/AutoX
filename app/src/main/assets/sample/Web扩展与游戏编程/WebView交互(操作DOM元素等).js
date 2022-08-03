"ui";
ui.layout(
    <vertical>
        <horizontal bg="#c7edcc" gravity="center" h="auto">
            <button text="后退" id="backBtn" style="Widget.AppCompat.Button.Colored" w="64" />
            <button text="主页" id="homePageBtn" style="Widget.AppCompat.Button.Colored" w="64" />
            <button text="刷新" id="reloadBtn" style="Widget.AppCompat.Button.Colored" w="64" />
            <button text="PC模式" id="desktopModeBtn" style="Widget.AppCompat.Button.Colored" w="72" />
            <button text="日志" id="logBtn" style="Widget.AppCompat.Button.Colored" w="64" />
        </horizontal>
        <vertical h="*" w="*">
            <webview id="webView" layout_below="title" w="*" h="*" />
        </vertical>
    </vertical>
);

var isDesktopMode = false
let url = "https://0x3.com/"
ui.webView.loadUrl(url)
// 延时执行，必须等待页面加载完成后才能正常获取页面元素内容
setTimeout(function () {
    ui.webView.evaluateJavascript("javascript:function a(){return document.title};a();", new JavaAdapter(android.webkit.ValueCallback, {
        onReceiveValue: (val) => {
            toastLog("当前网页标题：" + val)
        }
    }));
}, 3 * 1000);
ui.reloadBtn.on("click", () => {
    ui.webView.reload()
});
ui.desktopModeBtn.on('click', () => {
    isDesktopMode = !isDesktopMode
    // 设置 强制缩放 的两个方法：setEnabledRescale()，getEnabledRescale()。
    ui.webView.setEnabledRescale(isDesktopMode)
    // 设置 web控制台 的两个方法：setEnabledConsole()，getEnabledConsole()。
    ui.webView.setEnabledConsole(isDesktopMode)
    // 设置UA
    if (isDesktopMode) {
        ui.webView.getSettings().setUserAgentString("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_4), AppleWebKit/537.36 (KHTML, like Gecko), Chrome/99.0.4844.51 Safari/537.36")
    } else {
        ui.webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; U; Android 8.1.0; zh-CN; MI 5X Build/OPM1.171019.019) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/99.0.4844.51 MQQBrowser/6.2 TBS/045130 Mobile Safari/537.36")
    }
    ui.webView.reload();
});
ui.logBtn.on("click", () => {
    app.startActivity("console");
});
ui.backBtn.on("click", () => {
    if (ui.webView.canGoBack()) {
        ui.webView.goBack();
    } else {
        exit();
    }
});
ui.homePageBtn.on("click", () => {
    ui.webView.loadUrl("https://0x3.com/")
});



