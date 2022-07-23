let img = images.read("./0.jpg")
let cpuThreadNum = 4
// 新增：自定义模型路径(必须是完整路径)
// 使用时请将自定义路径 myModelPath 改为拥有读写权限的目录，并在目录里放上三个必需的模型文件： cls_opt.nb det_opt.nb rec_opt.nb，否则可能引起程序崩溃。
let myModelPath = "/sdcard/models/ocr_v2_for_cpu";
let start = new Date()
// 识别图片中的文字，返回完整识别信息（兼容百度OCR格式）。
let result = paddle.ocr(img, cpuThreadNum, myModelPath)
log('OCR识别耗时：' + (new Date() - start) + 'ms')
toastLog("完整识别信息: " + JSON.stringify(result))
start = new Date()
// 识别图片中的文字，只返回文本识别信息（字符串列表）
const stringList = paddle.ocrText(img, cpuThreadNum, myModelPath)
log('OCR纯文本识别耗时：' + (new Date() - start) + 'ms')
toastLog("文本识别信息: " + JSON.stringify(stringList))

// 回收图片
img.recycle()
// 释放native内存，非必要，供万一出现内存泄露时使用
// paddle.release()

