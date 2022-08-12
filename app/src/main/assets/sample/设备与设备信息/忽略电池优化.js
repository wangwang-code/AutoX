"ui";
ui.layout(
    <vertical>
        <text text="Auto.Js" textSize="24sp" w="*" gravity="center">
        </text>
        <button id="isIgnoringBatteryOptimizations" textSize="36sp">
            是否忽略电池优化
        </button>
        <button id="requestIgnoreBatteryOptimizations" textSize="36sp">
            申请忽略电池优化
        </button>
        <text id="instruction" line="18" />
    </vertical>
);
ui.instruction.setText("\n\n"
    + "  device.isIgnoringBatteryOptimizations()\n"
    + "  无参数（默认为本应用包名），返回值类型为 boolean\n"
    + "  \n"
    + "  device.requestIgnoreBatteryOptimizations()\n"
    + "  无参数（默认为本应用包名），无返回值\n"
    + "  \n"
    + "  device.isIgnoringBatteryOptimizations(packageName)\n"
    + "  参数packageName类型为字符串，内容为目标应用的包名，返回值类型为 boolean\n"
    + "  \n"
    + "  device.requestIgnoreBatteryOptimizations(packageName)\n"
    + "  参数packageName类型为字符串，内容为目标应用的包名，无返回值\n"
    + "  \n"
);
// isIgnoringBatteryOptimizations() 与 requestIgnoreBatteryOptimizations() 两个方法，不带参数，默认为本应用包名
ui.isIgnoringBatteryOptimizations.click(function () {
    if (device.isIgnoringBatteryOptimizations()) {
        toastLog("已忽略电池优化");
    } else {
        toastLog("未忽略电池优化");
    }
})
// isIgnoringBatteryOptimizations(packageName) 与 requestIgnoreBatteryOptimizations(packageName) 两个方法，参数类型为字符串，内容为目标应用的包名
ui.requestIgnoreBatteryOptimizations.click(function () {
    device.requestIgnoreBatteryOptimizations()
})
