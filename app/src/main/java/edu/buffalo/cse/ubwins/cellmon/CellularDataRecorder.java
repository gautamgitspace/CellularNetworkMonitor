/**
 *   Created by Gautam on 6/18/16.
 *   MBP111.0138.B16
 *   agautam2@buffalo.edu
 *   University at Buffalo, The State University of New York.
 *   Copyright © 2016 Gautam. All rights reserved.
 */

package edu.buffalo.cse.ubwins.cellmon;

import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellIdentityCdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;



public class CellularDataRecorder
{

    static final String TAG = "[CELNETMON-CDR]";

    public Long getLocalTimeStamp()
    {

        Long timeStamp = System.currentTimeMillis();
        return timeStamp;
    }

    public int getCurrentDataState(TelephonyManager telephonyManager)
    {
        int state = telephonyManager.getDataState();
        int dataState;

        if (state < 0 || state  >3 ){
            dataState = -1;
        }
        else{
            dataState =state;
        }
        return dataState;
    }

    public int getCurrentDataActivity(TelephonyManager telephonyManager)
    {
        int activity = telephonyManager.getDataActivity();
        int dataActivity;

        if (activity < 0 || activity  > 4 ){
            dataActivity = -1;
        }
        else{
            dataActivity =activity;
        }
        return dataActivity;
    }

    public int getMobileNetworkType(TelephonyManager telephonyManager)
    {
        int networkType = telephonyManager.getNetworkType();

            return networkType;
    }

    public String getCellularInfo(TelephonyManager telephonyManager)
    {
        String cellularInfo = "";
        String log = "";
        if(telephonyManager.getAllCellInfo()==null) {
            Log.v(TAG, "getAllCellInfo returned null");
        }
        else {
            for (final CellInfo info : telephonyManager.getAllCellInfo()) {
                if (info instanceof CellInfoGsm) {
                    log += "GSM@";
                    CellIdentityGsm gsm_cell = ((CellInfoGsm) info).getCellIdentity();
                    log += gsm_cell.getCid() + "#" + gsm_cell.getLac() + "#" + gsm_cell.getMcc() + "#" + gsm_cell.getMnc() + "_";

                    final CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                    log += gsm.getDbm() + "#" + gsm.getLevel()+"#"+gsm.getAsuLevel()+":";
                } else if (info instanceof CellInfoCdma) {
                    log += "CDMA@";
                    CellIdentityCdma cdma_cell = ((CellInfoCdma) info).getCellIdentity();
                    log += cdma_cell.getBasestationId() + "#" + cdma_cell.getNetworkId() + "#" + cdma_cell.getSystemId() + "#" + cdma_cell.getSystemId() + "_";

                    final CellSignalStrengthCdma cdma = ((CellInfoCdma) info).getCellSignalStrength();
                    log += cdma.getDbm() + "#" + cdma.getLevel()+"#"+cdma.getAsuLevel()+":";
                } else if (info instanceof CellInfoLte) {
                    log += "LTE@";
                    CellIdentityLte lte_cell = ((CellInfoLte) info).getCellIdentity();
                    log += lte_cell.getCi() + "#" + lte_cell.getPci() + "#" + lte_cell.getMcc() + "#" + lte_cell.getMnc() + "_";

                    final CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                    log += lte.getDbm() + "#" + lte.getLevel()+"#"+lte.getAsuLevel()+":";
                } else if (info instanceof CellInfoWcdma) {
                    log += "WCDMA@";
                    CellIdentityWcdma wcdma_cell = ((CellInfoWcdma) info).getCellIdentity();
                    log += wcdma_cell.getCid() + "#" + wcdma_cell.getLac() + "#" + wcdma_cell.getMcc() + "#" + wcdma_cell.getMnc() + "_";

                    final CellSignalStrengthWcdma wcdma = ((CellInfoWcdma) info).getCellSignalStrength();
                    log += wcdma.getDbm() + "#" + wcdma.getLevel()+"#"+wcdma.getAsuLevel()+":";
                } else {
                    Log.v(TAG, "Unknown Network Type");
                }
            }
        }
        cellularInfo = log;
        return cellularInfo;
    }
}
