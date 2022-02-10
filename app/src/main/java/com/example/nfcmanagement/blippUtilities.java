package com.example.nfcmanagement;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class blippUtilities {

    static public byte[] toBytes(String s) {
        int len = s.length();
        assert len%2 == 0;
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    static public String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = bytes.length - 1; i >= 0; --i) {
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
            if (i > 0) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    static public String toReversedHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            if (i > 0) {
                sb.append(" ");
            }
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }

    static public byte[][] readKeyFile(InputStream stream) throws  IOException {

        byte[][] keyArray = new byte[32][];

        String tContents = "";

        try {

            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            tContents = new String(buffer);
        } catch (Exception e) {
            Log.e("readFail", "Failed while reading keyMapping.txt..", e);
        }

        String[] Keys = tContents.split("[|\n]|\r\n");

        for (int i = 0; i < Keys.length; i++) {
            keyArray[i] = toBytes(Keys[i]);
        }

        return keyArray;

    }

    static public byte[][] groupByteArray(byte[] bytes, int dataPerBlock) {

        int  amountOfBlocks = 0;
        if (bytes.length % dataPerBlock == 0) {

            amountOfBlocks = bytes.length / dataPerBlock;

        }
        else {
            Log.e("Grouping Error", "Data could not be grouped evenly");
            return null;
        }

        byte[][] Output = new byte[amountOfBlocks][];
        Output[0] = new byte[dataPerBlock];
        int data = 0;
        int Block = 0;

        for (byte n: bytes) {

            if (data == dataPerBlock) {
                Block++;
                data = 0;
                Output[Block] = new byte[dataPerBlock];
            }
            Output[Block][data] = n;
            data++;
        }

        return Output;

    }


    static public byte[] flattenByteArray(byte[][] bytes) {

        int arraySize = 0;

        for (byte[] n: bytes) {
            arraySize += n.length;
        }

        byte[] Output = new byte[arraySize];
        int Offset = 0;

        for (byte[] arr: bytes) {
            for (byte x: arr) {
                Output[Offset] = x;
                Offset++;
            }
        }

        return Output;

    }

}
