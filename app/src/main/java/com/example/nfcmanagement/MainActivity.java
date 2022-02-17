package com.example.nfcmanagement;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import  com.example.nfcmanagement.NFCServerHandler;
import  com.example.nfcmanagement.NFCServerCallback;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Base64;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback,NFCServerCallback{

    @RequiresApi(api = Build.VERSION_CODES.O)
    public JSONObject HandleRequest(JSONObject ClientRequest)
    {
        JSONObject ReturnValue = new JSONObject();

        Log.i("HandleRequest", "Request Received: " + ClientRequest.toString());


        try {
            if (tagHeader != null) {
                // UID
                if (ClientRequest.has("getUID")) {
                    ReturnValue.put("UID", tagHeader.getUID());
                }
                // ReadTag
                if (ClientRequest.has("readTag")) {
                    readCard();
                    if (cardData == null) {
                        ReturnValue.put("cardData", "");
                    }
                    ReturnValue.put("cardData", new String(Base64.getEncoder().encode(cardData)));
                    Log.i("size", Integer.toString(cardData.length));
                    cardData = null;
                }

                if (ReturnValue == null) {
                    ReturnValue.put("empty", "empty");
                }
            }
            else {
                ReturnValue.put("Empty", "empty");
            }
        }
        catch (Exception e)
        {
            Log.e("error", e.getMessage());
        }


        Log.i("returnData", ReturnValue.toString());
        return(ReturnValue);
    }
    NFCServerHandler m_ServerHandler = null;
    NFCHandler nfcHandler = new NFCHandler();
    NfcAdapter nfcAdapter;
    NFCTagHeader tagHeader;
    Tag tag;
    byte[] cardData = null;


    public void readCard() {

        if (!nfcHandler.tagIsConnected(tag)) {
            Toast.makeText(this, "No valid NFC tag detected", Toast.LENGTH_SHORT).show();
            return;
        }

        MifareClassic mifareTag = MifareClassic.get(tag);
        try {
            cardData = nfcHandler.readTag(mifareTag, getApplicationContext());
            if (cardData == null) {
                // TODO: Improve with actual error, i.e "Auth fail on sector0"
                Toast.makeText(this, "Failed while reading card data!", Toast.LENGTH_SHORT).show();
                return;
            }

        } catch (IOException e) {
            Toast.makeText(this, "Error reading card data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nfcAdapter = nfcHandler.createAdapter(this);

        m_ServerHandler = new NFCServerHandler(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        assert nfcAdapter != null;

        Bundle options = new Bundle();
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

        nfcAdapter.enableReaderMode(this,
                this,
                NfcAdapter.FLAG_READER_NFC_A |
                        NfcAdapter.FLAG_READER_NFC_B |
                        NfcAdapter.FLAG_READER_NFC_F |
                        NfcAdapter.FLAG_READER_NFC_V |
                        NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                options);
    }

    @Override
    protected void onPause() {
        super.onPause();

        nfcAdapter.disableReaderMode(this);

    }

    public void onTagDiscovered (Tag localTag) {

        Log.i("here", "here");
        tag = localTag;
        assert tag != null;
        tagHeader = new NFCTagHeader(tag);
        Log.i("tag", tagHeader.getUID());
        //Toast.makeText(this, "New card with UID " + tagHeader.getUID() + " detected", Toast.LENGTH_SHORT).show();
    }

}