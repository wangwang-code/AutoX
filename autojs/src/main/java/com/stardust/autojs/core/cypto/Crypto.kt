package com.stardust.autojs.core.cypto

import java.lang.IllegalArgumentException
import java.lang.NullPointerException
import java.lang.StringBuilder

class Crypto {
    companion object {
        const val A: Byte = 97
        const val F: Byte = 102
        val HEX_DIGITS: CharArray = "01234567890abcdef".toCharArray()
        val INSTANCE = Crypto()
        const val NINE: Byte = 57
        const val ZERO: Byte = 48

        @JvmStatic
        fun singleHexToNumber(paramChar: Char): Byte {
            var b = paramChar.lowercaseChar().code
            if (b in 48..57) {
                b -= 48
                return b.toByte()
            } else if (b in 97..102) {
                b -= 97
                return b.toByte()
            }
            val stringBuilder = "char: $paramChar"
            throw IllegalArgumentException(stringBuilder)
        }

        @JvmStatic
        fun fromHex(paramString: String?): ByteArray {
            if (paramString != null) {
                val result = ByteArray(paramString.length / 2)
                val len = paramString.length
                var index = 0
                while (index <= len - 1) {
                    val subString = paramString.substring(index, index + 2)
                    val intValue = subString.toInt(16)
                    result[index / 2] = intValue.toByte()
                    index += 2
                }
                return result
            }
            throw NullPointerException()
        }

        @JvmStatic
        fun toHex(paramArrayOfByte: ByteArray?): String {
            if (paramArrayOfByte != null) {
                val result = StringBuilder()
                var index = 0
                val len = paramArrayOfByte.size
                while (index <= len - 1) {
                    val char1: Int = (paramArrayOfByte[index].toInt()) shr 4 and 0xF
                    val chara1 = Character.forDigit(char1, 16)
                    val char2: Int = (paramArrayOfByte[index].toInt()) and 0xF
                    val chara2 = Character.forDigit(char2, 16)
                    result.append(chara1)
                    result.append(chara2)
                    index += 1
                }
                return result.toString()
            }
            throw NullPointerException()
        }
    }

}