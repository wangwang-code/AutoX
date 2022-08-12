package com.stardust.autojs.runtime.api

import android.content.Context
import android.content.Intent
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import android.speech.tts.TextToSpeech.EngineInfo
import android.os.Build
import android.speech.tts.Voice
import com.stardust.autojs.runtime.api.Speech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.util.*

class Speech(context: Context) : UtteranceProgressListener() {
    lateinit var mContext: Context
    lateinit var mTextToSpeech: TextToSpeech
    lateinit var mSpeech: Speech
    var isSuccess = true


    @JavascriptInterface
    fun getInstance(context: Context): Speech? {
        if (mSpeech == null) {
            synchronized(Speech::class.java) {
                if (mSpeech == null) {
                    mSpeech = Speech(context)
                }
            }
        }
        return mSpeech
    }

    init {
        mContext = context.applicationContext
        mTextToSpeech = TextToSpeech(mContext) { i: Int ->
            // TTS初始化
            if (i == TextToSpeech.SUCCESS) {
                var result = mTextToSpeech.setLanguage(Locale.CHINA)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    result = mTextToSpeech.setLanguage(Locale.getDefault())
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        isSuccess = false
                    }
                }
                mTextToSpeech.setOnUtteranceProgressListener(this@Speech)
            }
        }
    }

    @get:JavascriptInterface
    val engines: Array<String?>
        get() {
            val result = arrayOfNulls<String>(mTextToSpeech!!.engines.toTypedArray().size)
            var i = 0
            for (item in mTextToSpeech.engines) {
                result[i] = item.name
                i += 1
            }
            return result
        }

    @JavascriptInterface
    fun setEngine(enginePackageName: String?): Int {
        return mTextToSpeech!!.setEngineByPackageName(enginePackageName)
    }

    @get:JavascriptInterface
    val voices: Array<String?>
        get() {
            val result = arrayOfNulls<String>(mTextToSpeech!!.voices.toTypedArray().size)
            var i = 0
            for (item in mTextToSpeech.voices) {
                result[i] = item.name
                i += 1
            }
            return result
        }

    @JavascriptInterface
    fun setVoice(voiceName: String): Int {
        for (item in mTextToSpeech!!.voices) {
            if (voiceName == item.name) {
                return mTextToSpeech.setVoice(item)
            }
        }
        return -1
    }

    @get:JavascriptInterface
    val languages: Array<String?>
        get() {
            val result =
                arrayOfNulls<String>(mTextToSpeech!!.availableLanguages.toTypedArray().size)
            var i = 0
            for (item in mTextToSpeech.availableLanguages) {
                result[i] = item.displayName
                i += 1
            }
            return result
        }

    @JavascriptInterface
    fun setLanguage(language: String): Int {
        for (item in mTextToSpeech!!.availableLanguages) {
            if (language == item.displayName) {
                return mTextToSpeech.setLanguage(item)
            }
        }
        return -1
    }

    @JavascriptInterface
    fun speak(text: String?, pitch: Float?, speechRate: Float?, volume: Float) {
        if (!isSuccess) {
            Toast.makeText(mContext, "TTS暂不支持该语言", Toast.LENGTH_SHORT).show()
            return
        }
        if (mTextToSpeech != null) {
            mTextToSpeech.setPitch(pitch!!) // 设置音调，值越大声音越尖（女生），值越小则变成男声,1.0是常规
            mTextToSpeech.setSpeechRate(speechRate!!)
            //设置音量
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_VOLUME] = volume.toString()
            params[TextToSpeech.Engine.KEY_PARAM_STREAM] = "STREAM_MUSIC"
            mTextToSpeech.speak(text, TextToSpeech.QUEUE_ADD, params)
        } else {
            Log.e("Speech", "mTextToSpeech is null")
        }
    }

    @JavascriptInterface
    fun speak(text: String?) {
        speak(text, 1.0f, 1.0f, 0.8f)
    }

    @JavascriptInterface
    fun synthesizeToFile(
        text: String?,
        pitch: Float?,
        speechRate: Float?,
        volume: Float,
        fileName: String?
    ) {
        if (!isSuccess) {
            Toast.makeText(mContext, "TTS暂不支持该语言", Toast.LENGTH_SHORT).show()
            return
        }
        if (mTextToSpeech != null) {
            mTextToSpeech.setPitch(pitch!!) // 设置音调，值越大声音越尖（女生），值越小则变成男声,1.0是常规
            mTextToSpeech.setSpeechRate(speechRate!!)
            //设置音量
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_VOLUME] = volume.toString()
            mTextToSpeech.synthesizeToFile(text, params, fileName)
        } else {
            Log.e("Speech", "mTextToSpeech is null")
        }
    }

    @get:JavascriptInterface
    val isSpeaking: Boolean
        get() = mTextToSpeech!!.isSpeaking

    @JavascriptInterface
    fun gotoSettings() {
        var intent0 = Intent("com.android.settings.TTS_SETTINGS")
        intent0.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        mContext.startActivity(intent0)
    }

    @JavascriptInterface
    fun shutdown() {
        mTextToSpeech?.shutdown()
    }

    @JavascriptInterface
    fun stop() {
        mTextToSpeech?.stop()
    }

    @JavascriptInterface
    fun destroy() {
        stop()
        mTextToSpeech?.shutdown()
    }

    override fun onStart(s: String) {}
    override fun onDone(s: String) {}
    override fun onError(s: String) {}

}