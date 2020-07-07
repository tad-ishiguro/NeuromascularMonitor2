package com.adtex.NeuromusclarMonitor;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AESObj
{

	public byte[] m_source;
	public byte[] m_encrypt;
	public byte[] m_CommonKey;
	public Key		m_Key;
	public AESObj()
	{
		m_source = new byte[16];
		m_encrypt = new byte[16];
		m_CommonKey = new byte[16];
	}

	public void SetCommonKey(byte[] CommonKey)	//AES　暗号キーをセット
	{
		int		i;
		for(i = 0; i < 16; i++)
			m_CommonKey[i] = CommonKey[i];
		m_Key = new SecretKeySpec(m_CommonKey, "AES");
	}

	public void MakeRandKey(byte[] RandKey)	//16byteの乱数データ配列を作る
	{
		byte		nRand;
		int			i;
		for(i = 0; i < 16; i++)
		{
			nRand = (byte)(256 * Math.random() - 128);
			RandKey[i] = nRand;
		}
	}

	public void SetEncrypt(byte[] code)	//乱数配列を暗号キーで暗号化する
	{
		int			i;
		for(i = 0; i < 16; i++)
			m_source[i] = code[i];
		byte[] encrypt = encode1(m_source, m_Key);
		for(i = 0; i < 16; i++)
			m_encrypt[i] = encrypt[i];
	}
	/**
	 * 暗号化
	 */
	public static byte[] encode1(byte[] src, Key skey) {	//乱数配列を暗号キーで暗号化する
		try {
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, skey);
			return cipher.doFinal(src);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 復号化
	 */
	public static byte[] decode1(byte[] src, Key skey) {//暗号化配列を暗号キーで複合化する　使用していない
		try {
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, skey);
			return cipher.doFinal(src);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean CheckByteArray(byte[] value1, byte[] value2)	//二つの１６byte配列が一致しているか調べる
	{
		int		i;
		boolean	bRet = true;
		for(i = 0; i < 16; i++)
		{
			if(value1[i] != value2[i])
			{
				bRet = false;
				break;
			}
		}
		return bRet;
	}
}

