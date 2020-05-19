package org.opencovidtrace.octrace.utils

import android.annotation.SuppressLint
import java.io.UnsupportedEncodingException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec


object AESEncryptor {
    private const val cipherInstance = "AES/ECB/NoPadding"

    @SuppressLint("GetInstance")
    fun encrypt(input: ByteArray, keyBytes: ByteArray): ByteArray? {
        try {
            val skey = SecretKeySpec(keyBytes, "AES")

            synchronized(Cipher::class.java) {
                val cipher = Cipher.getInstance(cipherInstance)
                cipher.init(Cipher.ENCRYPT_MODE, skey)
                return cipher.doFinal(input)
            }
        } catch (uee: UnsupportedEncodingException) {
            uee.printStackTrace()
        } catch (ibse: IllegalBlockSizeException) {
            ibse.printStackTrace()
        } catch (bpe: BadPaddingException) {
            bpe.printStackTrace()
        } catch (ike: InvalidKeyException) {
            ike.printStackTrace()
        } catch (nspe: NoSuchPaddingException) {
            nspe.printStackTrace()
        } catch (nsae: NoSuchAlgorithmException) {
            nsae.printStackTrace()
        } catch (e: ShortBufferException) {
            e.printStackTrace()
        }

        return null
    }

    @SuppressLint("GetInstance")
    fun decryptWithAES(input: ByteArray, keyBytes: ByteArray): ByteArray? {
        try {
            val skey = SecretKeySpec(keyBytes, "AES")

            synchronized(Cipher::class.java) {
                val cipher = Cipher.getInstance(cipherInstance)
                cipher.init(Cipher.DECRYPT_MODE, skey)
                return cipher.doFinal(input)
            }
        } catch (uee: UnsupportedEncodingException) {
            uee.printStackTrace()
        } catch (ibse: IllegalBlockSizeException) {
            ibse.printStackTrace()
        } catch (bpe: BadPaddingException) {
            bpe.printStackTrace()
        } catch (ike: InvalidKeyException) {
            ike.printStackTrace()
        } catch (nspe: NoSuchPaddingException) {
            nspe.printStackTrace()
        } catch (nsae: NoSuchAlgorithmException) {
            nsae.printStackTrace()
        } catch (e: ShortBufferException) {
            e.printStackTrace()
        }

        return null
    }

}