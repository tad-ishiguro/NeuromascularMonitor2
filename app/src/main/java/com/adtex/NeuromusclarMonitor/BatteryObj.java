package com.adtex.NeuromusclarMonitor;

import android.graphics.Point;

public class BatteryObj {
    int     m_nNoOfPoint[] = new int[3];
    float   m_fVolt[][] = new float[3][20];
    float   m_fLevel[][] = new float[3][20];
    int     m_nAverageTime = 10;
    float   m_fAveLevel;
    int         m_nAveCount = 0;
    void InitObj()
    {
        m_fAveLevel = 0.0F;

        m_fLevel[0][0] = 100.0F;
        m_fLevel[0][1] = 89.44F;
        m_fLevel[0][2] = 75.35F;
        m_fLevel[0][3] = 62.68F;
        m_fLevel[0][4] = 48.94F;
        m_fLevel[0][5] = 36.86F;
        m_fLevel[0][6] = 22.67F;
        m_fLevel[0][7] = 11.96F;
        m_fLevel[0][8] = 1.41F;
        m_fLevel[0][9] = 0.0F;

        m_fVolt[0][0] = 1.4F;
        m_fVolt[0][1] = 1.215F;
        m_fVolt[0][2] = 1.116F;
        m_fVolt[0][3] = 1.054F;
        m_fVolt[0][4] = 1.01F;
        m_fVolt[0][5] = 0.9725F;
        m_fVolt[0][6] = 0.9274F;
        m_fVolt[0][7] = 0.8909F;
        m_fVolt[0][8] = 0.7957F;
        m_fVolt[0][9] = 0.0F;
        m_nNoOfPoint[0] = 10;

        m_fLevel[1][0] = 100.0F;
        m_fLevel[1][1] = 96.26923F;
        m_fLevel[1][2] = 90.05128F;
        m_fLevel[1][3] = 80.72436F;
        m_fLevel[1][4] = 72.01923F;
        m_fLevel[1][5] = 66.42308F;
        m_fLevel[1][6] = 55.23077F;
        m_fLevel[1][7] = 33.68244F;
        m_fLevel[1][8] = 24.98506F;
        m_fLevel[1][9] = 17.92308F;
        m_fLevel[1][10] = 11.70513F;
        m_fLevel[1][11] = 6.60025F;
        m_fLevel[1][12] = 4.24359F;
        m_fLevel[1][13] = 3F;
        m_fLevel[1][14] = 0F;

        m_fVolt[1][0] = 1.3F;
        m_fVolt[1][1] = 1.23349F;
        m_fVolt[1][2] = 1.18916F;
        m_fVolt[1][3] = 1.16145F;
        m_fVolt[1][4] = 1.15313F;
        m_fVolt[1][5] = 1.15036F;
        m_fVolt[1][6] = 1.14482F;
        m_fVolt[1][7] = 1.13443F;
        m_fVolt[1][8] = 1.12508F;
        m_fVolt[1][9] = 1.10602F;
        m_fVolt[1][10] = 1.07831F;
        m_fVolt[1][11] = 1.02075F;
        m_fVolt[1][12] = 0.95084F;
        m_fVolt[1][13] = 0.84F;
        m_fVolt[1][14] = 0F;
        m_nNoOfPoint[1] = 15;

        m_fLevel[2][0] = 100.0F;
        m_fLevel[2][1] = 93.78882F;
        m_fLevel[2][2] = 86.02484F;
        m_fLevel[2][3] = 78.88199F;
        m_fLevel[2][4] = 73.91304F;
        m_fLevel[2][5] = 69.56522F;
        m_fLevel[2][6] = 59.62733F;
        m_fLevel[2][7] = 47.20497F;
        m_fLevel[2][8] = 31.62286F;
        m_fLevel[2][9] = 20.17509F;
        m_fLevel[2][10] = 8.07453F;
        m_fLevel[2][11] = 5.16925F;
        m_fLevel[2][12] = 3.10559F;
        m_fLevel[2][13] = 1.24224F;
        m_fLevel[2][14] = 0F;

        m_fVolt[2][0] = 1.35F;
        m_fVolt[2][1] = 1.26111F;
        m_fVolt[2][2] = 1.21667F;
        m_fVolt[2][3] = 1.19444F;
        m_fVolt[2][4] = 1.18333F;
        m_fVolt[2][5] = 1.17778F;
        m_fVolt[2][6] = 1.175F;
        m_fVolt[2][7] = 1.17222F;
        m_fVolt[2][8] = 1.16284F;
        m_fVolt[2][9] = 1.14914F;
        m_fVolt[2][10] = 1.10556F;
        m_fVolt[2][11] = 1.06125F;
        m_fVolt[2][12] = 1F;
        m_fVolt[2][13] = 0.9F;
        m_fVolt[2][14] = 0.8F;
        m_nNoOfPoint[2] = 15;
    }

    int GetBatteryLevel(float fVoltage, int nBatteryType, int nNoOfBattery)
    {
        if(nNoOfBattery <= 0)
            nNoOfBattery = 1;
        float   fVolt = fVoltage / nNoOfBattery;
        int     i, ret;

        if(m_fVolt[nBatteryType][0] <= fVolt)
            return 100;
        if(fVolt <= m_fVolt[nBatteryType][m_nNoOfPoint[nBatteryType] - 1])
            return 0;
        for(i = 0; i < m_nNoOfPoint[nBatteryType] - 1; i++)
        {
            if(m_fVolt[nBatteryType][i + 1] <= fVolt && fVolt < m_fVolt[nBatteryType][i])
            {
                ret = (int)GetBetweenLevel(fVolt, m_fVolt[nBatteryType][i + 1], m_fVolt[nBatteryType][i], m_fLevel[nBatteryType][i + 1], m_fLevel[nBatteryType][i]);
                return ret;
            }
        }
        return 0;
    }

    float GetBetweenLevel(float fVolt, float fVolt1, float fVolt2, float fLevel1, float fLevel2)
    {
        float   fLevel;
        if(fVolt2 == fVolt1)
            return 0.0F;
        fLevel = (fVolt - fVolt1) * (fLevel2 - fLevel1) / (fVolt2 - fVolt1) + fLevel1;
//        fLevel = CalcAve(fLevel);
        return fLevel;
    }

    //移動平均を計算する
    public float CalcAve(float fVal)
    {
        int		nNoOfAve;
        int		nAveTime = m_nAverageTime;
        m_nAveCount++;
        nNoOfAve = (m_nAveCount < nAveTime)? m_nAveCount : nAveTime;
        if(nNoOfAve == 0)
            nNoOfAve = 1;

        m_fAveLevel = (m_fAveLevel * (float)(nNoOfAve - 1) + fVal) / (float)nNoOfAve;
        return m_fAveLevel;
    }

}
