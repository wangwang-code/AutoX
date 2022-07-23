package com.stardust.autojs.runtime.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.baidu.paddle.lite.demo.ocr.OcrResult;
import com.baidu.paddle.lite.demo.ocr.Predictor;
import com.stardust.app.GlobalAppContext;
import com.stardust.autojs.core.image.ImageWrapper;

import java.util.Collections;
import java.util.List;

public class Paddle {
    private Predictor mPredictor = new Predictor();

    public boolean initOcr(Context context) {
        return mPredictor.init(context);
    }

    public boolean initOcr(Context context, int cpuThreadNum) {
        return mPredictor.init(context, cpuThreadNum);
    }

    public boolean initOcr(Context context, int cpuThreadNum, Boolean useSlim, Boolean useOpencl) {
        return mPredictor.init(context, cpuThreadNum, useSlim, useOpencl);
    }

    public boolean initOcr(Context context, int cpuThreadNum, String myModelPath, Boolean useOpencl) {
        return mPredictor.init(context, cpuThreadNum, myModelPath, useOpencl);
    }

    public List<OcrResult> ocr(ImageWrapper image, int cpuThreadNum, String myModelPath, Boolean useOpencl,
                               int detLongSize, float scoreThreshold) {
        if (image == null) {
            return Collections.emptyList();
        }
        Bitmap bitmap = image.getBitmap();
        if (bitmap == null || bitmap.isRecycled()) {
            return Collections.emptyList();
        }
        if (!mPredictor.isLoaded()) {
            mPredictor.init(GlobalAppContext.get(), cpuThreadNum, myModelPath, useOpencl, detLongSize, scoreThreshold);
        }
        return mPredictor.ocr(bitmap);
    }

    public List<OcrResult> ocr(ImageWrapper image, int cpuThreadNum, Boolean useSlim, Boolean useOpencl,
                               int detLongSize, float scoreThreshold) {
        if (image == null) {
            return Collections.emptyList();
        }
        Bitmap bitmap = image.getBitmap();
        if (bitmap == null || bitmap.isRecycled()) {
            return Collections.emptyList();
        }
        if (!mPredictor.isLoaded()) {
            mPredictor.init(GlobalAppContext.get(), cpuThreadNum, useSlim, useOpencl, detLongSize, scoreThreshold);
        }
        return mPredictor.ocr(bitmap);
    }

    public List<OcrResult> ocr(ImageWrapper image, int cpuThreadNum, Boolean useSlim, Boolean useOpencl) {
        if (image == null) {
            return Collections.emptyList();
        }
        Bitmap bitmap = image.getBitmap();
        if (bitmap == null || bitmap.isRecycled()) {
            return Collections.emptyList();
        }
        if (!mPredictor.isLoaded()) {
            mPredictor.init(GlobalAppContext.get(), cpuThreadNum, useSlim, useOpencl);
        }
        return mPredictor.ocr(bitmap);
    }

    public List<OcrResult> ocr(ImageWrapper image, int cpuThreadNum, String myModelPath, Boolean useOpencl) {
        if (image == null) {
            return Collections.emptyList();
        }
        Bitmap bitmap = image.getBitmap();
        if (bitmap == null || bitmap.isRecycled()) {
            return Collections.emptyList();
        }
        if (!mPredictor.isLoaded()) {
            mPredictor.init(GlobalAppContext.get(), cpuThreadNum, myModelPath, useOpencl);
        }
        return mPredictor.ocr(bitmap);
    }

    public List<OcrResult> ocr(ImageWrapper image, int cpuThreadNum, String myModelPath) {
        return ocr(image, cpuThreadNum, myModelPath, false);
    }

    public List<OcrResult> ocr(ImageWrapper image) {
        return ocr(image, 4, true, false);
    }

    public String[] ocrText(ImageWrapper image, int cpuThreadNum, Boolean useSlim, Boolean useOpencl) {
        if (!mPredictor.isLoaded()) {
            mPredictor.init(GlobalAppContext.get(), cpuThreadNum, useSlim, useOpencl);
        }
        List<OcrResult> words_result = ocr(image, cpuThreadNum, useSlim, useOpencl);
        String[] outputResult = new String[words_result.size()];
        for (int i = 0; i < words_result.size(); i++) {
            outputResult[i] = words_result.get(i).words;
            Log.i("outputResult", outputResult[i].toString()); // show LOG in Logcat panel
        }
        return outputResult;
    }

    public String[] ocrText(ImageWrapper image, int cpuThreadNum, String myModelPath, Boolean useOpencl) {
        List<OcrResult> words_result = ocr(image, cpuThreadNum, myModelPath, useOpencl);
        String[] outputResult = new String[words_result.size()];
        for (int i = 0; i < words_result.size(); i++) {
            outputResult[i] = words_result.get(i).words;
            Log.i("outputResult", outputResult[i].toString()); // show LOG in Logcat panel
        }
        return outputResult;
    }

    public String[] ocrText(ImageWrapper image, int cpuThreadNum, Boolean useSlim) {
        return ocrText(image, cpuThreadNum, useSlim, false);
    }

    public String[] ocrText(ImageWrapper image, int cpuThreadNum, String myModelPath) {
        return ocrText(image, cpuThreadNum, myModelPath, false);
    }

    public String[] ocrText(ImageWrapper image) {
        return ocrText(image, 4, true, false);
    }

    public String[] ocrText(ImageWrapper image, int cpuThreadNum) {
        return ocrText(image, cpuThreadNum, true, false);
    }

    public void releaseOcr() {
        mPredictor.releaseModel();
    }

    public void release() {
        mPredictor.releaseModel();
    }
}
