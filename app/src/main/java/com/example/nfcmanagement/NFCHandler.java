package com.example.nfcmanagement;

import android.content.Context;
import android.icu.util.Output;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.util.Log;
import android.widget.Toast;

import com.example.nfcmanagement.blippUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

class NFCTagHeader {

    public byte[] UID;
    public String [] techList;
    public boolean isMifareClassic = false;
    public String type = "Unknown";
    public Integer size = 0;
    public Integer sectorCount = 0;
    public Integer blockCount = 0;



    public NFCTagHeader(Tag tag) {

        UID = tag.getId();
        techList = tag.getTechList();

        for (String tech : techList) {
            if (tech.equals(MifareClassic.class.getName())) {
                isMifareClassic = true;
                MifareClassic mifareTag = MifareClassic.get(tag);
                switch (mifareTag.getType()) {
                    case MifareClassic.TYPE_CLASSIC:
                        type = "Classic";
                        break;
                    case MifareClassic.TYPE_PLUS:
                        type = "Plus";
                        break;
                    case MifareClassic.TYPE_PRO:
                        type = "Pro";
                        break;
                }

                size = mifareTag.getSize();
                sectorCount = mifareTag.getSectorCount();
                blockCount = mifareTag.getBlockCount();
                try {
                    mifareTag.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }





    }

    public String getUID() {
        return blippUtilities.toReversedHex(UID);
    }





}

public class NFCHandler {

    byte[] encryptedKey = {(byte) 0x69, (byte) 0xAD, (byte) 0xBB, (byte) 0xAE, (byte) 0x69, (byte) 0x69};

    public NfcAdapter createAdapter(Context context) {

        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);

        // On fail, show error
        if (nfcAdapter == null) {
            Toast.makeText(context, "NO NFC CAPABILITIES", Toast.LENGTH_SHORT).show();
        }
        return nfcAdapter;
    }


    public byte [][] getKeys(MifareClassic mifareTag, Context context) throws IOException {

        InputStream inputStream = null;

        try {
            inputStream = context.getAssets().open("keyMapping.txt");
        } catch (IOException e) {
            Log.e("fail", "Failed to open asset keyMapping.txt");
        }

        byte [][] Keys = blippUtilities.readKeyFile(inputStream);

        return Keys;
    }

    public byte[] readTag(MifareClassic mifareTag, Context context) throws IOException {

        byte [][] data = new byte[64][];

        byte [][] Keys = getKeys(mifareTag, context);

        mifareTag.connect();

        for (int sector = 0; sector < 16; sector++) {

            // Authenticate. If fail:
            if (!mifareTag.authenticateSectorWithKeyA(sector, Keys[sector*2])) {
                if (!mifareTag.authenticateSectorWithKeyA(sector, encryptedKey)) {
                    Log.e("AuthFail", "Authentication on sector " + sector + " failed with key " + blippUtilities.toReversedHex(Keys[sector*2]) + " during reading");
                    return null;
                }
                Keys[sector*2] = encryptedKey;
                Keys[(sector*2)+1] = encryptedKey;
            }

            for (int block = sector*4; block < (sector+1)*4; block++) {
                byte[] payload = mifareTag.readBlock(block);

                // Manually map keyA|AccessConditions|keyB to 3rd block in every sector (3 7 11 ..)
                if ( (block+1) % 4 == 0) {

                    // Map keyA to first 6 bytes of block
                    for (int i = 0; i < 6; i++) {
                        payload[i] = Keys[sector*2][i];
                    }

                    // Map keyB to last 6 bytes of block
                    for (int i = 0; i < 6; i++) {
                        payload[i+10] = Keys[(sector*2)+1][i];
                    }
                }
                data[block] = payload;
            }
        }

        mifareTag.close();
        byte[] Output = blippUtilities.flattenByteArray(data);

        Log.v("data72", blippUtilities.toReversedHex(Output));

        return Output;

    }

    public boolean writeTag(MifareClassic mifareTag, Context context, boolean uidOnly, byte[] data) throws IOException {

        int sectorLimit = 16;
        byte [] defAC = {(byte) 0x78, (byte) 0x77, (byte) 0x88, (byte) 0x41};
        if (!uidOnly) {

            byte [] dataUID = Arrays.copyOfRange(data, 0, 4);
            byte [] tagUID = mifareTag.getTag().getId();
            if (!Arrays.equals(tagUID, dataUID)) {
                Log.e("uids", "1st: " + blippUtilities.toReversedHex(tagUID) + ", 2nd: " + blippUtilities.toReversedHex(dataUID));
                Toast.makeText(context, "Rollover required!", Toast.LENGTH_SHORT).show();
                return false;
            }

        }
        else {
            sectorLimit = 1;
        }


        byte [][] Keys = getKeys(mifareTag, context);

        byte[][] groupedData = blippUtilities.groupByteArray(data, 16);

        mifareTag.connect();

        // Write sector 0

        if (!mifareTag.authenticateSectorWithKeyB(0, Keys[1])) {
            if (!mifareTag.authenticateSectorWithKeyB(0, encryptedKey)) {
                Log.e("Authfail", "Authentication on sector 0 failed with key " + blippUtilities.toReversedHex(Keys[1]));
                return false;
            }
        }

        if (uidOnly) {
            byte [] arr = new byte[16];
            System.arraycopy(encryptedKey, 0, arr, 0, 6);
            System.arraycopy(defAC, 0, arr, 6, 4);
            System.arraycopy(encryptedKey, 0, arr, 10, 6);
            try {
                mifareTag.writeBlock(3, arr);
            } catch (Exception e) {
                Log.e("AC fix failed", "Failed fixing AC before rollover with error " + e.toString());
            }

            for (int block = 0; block < 4; block++) {
                try {
                    mifareTag.writeBlock(block, groupedData[block]);
                } catch (Exception e) {
                    Log.e("Write Failed", "Writing to block " + block + " failed with error" + e.toString());
                }
            }
            mifareTag.close();
            return true;
        }

        for (int sector = 1; sector < sectorLimit; sector++) {

            // Authenticate. If fail:
            if (!mifareTag.authenticateSectorWithKeyB(sector, Keys[(sector*2)+1])) {
                if (!mifareTag.authenticateSectorWithKeyB(sector, encryptedKey)) {
                    Log.e("AuthFail", "Authentication on sector " + sector + " failed with key " + blippUtilities.toReversedHex(Keys[(sector*2)+1]) + " during writing");
                    return false;
                }
            }

            for (int block = sector*4; block < (sector+1)*4; block++) {

                try {
                    //Log.v("write", "Writing to block " + Integer.toString(block) + " with data: " + blippUtilities.toReversedHex(groupedData[block]));
                    mifareTag.writeBlock(block, groupedData[block]);
                } catch (Exception e) {
                    Log.e("Write Failed", "Writing to block " + block + " failed with error" + e.toString());
                }


            }

        }

        mifareTag.close();

        if (uidOnly) {
            return true;
        }

        byte[] writtenData = readTag(mifareTag, context);

        byte[] datacropped = new byte[64*16];

        System.arraycopy(data, 0, datacropped, 0, 64*16);

        if (!Arrays.equals(datacropped, writtenData)){
            Log.e("fullWriteFailed", "Complete write failed");
            /*
            byte[][] x = blippUtilities.groupByteArray(data, 16);
            int i = 0;
            for(byte[] n: x) {
                Log.e("data" + Integer.toString(i), blippUtilities.toReversedHex(n));
                i++;
            }
            */
            Log.e("datacropped", datacropped.length + " " + blippUtilities.toReversedHex(datacropped));
            Log.e("writtenData", writtenData.length + " " + blippUtilities.toReversedHex(writtenData));
            return false;
        }

        return true;


    }

    public boolean tagIsConnected(Tag tag) {

        if (tag != null) {
            NFCTagHeader tagHeader = new NFCTagHeader(tag);
            if (tagHeader.isMifareClassic) {

                MifareClassic mifareTag = MifareClassic.get(tag);
                try {
                    mifareTag.connect();
                } catch (IOException e) {
                    return false;
                }

                try {
                    mifareTag.close();
                } catch (IOException e) {
                    Log.e("Closing error", "Failed to close: " + e.getMessage());
                    return false;
                }

                return true;

            }
        }
        return false;
    }


}
