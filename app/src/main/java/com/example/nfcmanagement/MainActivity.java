package com.example.nfcmanagement;

import androidx.appcompat.app.AppCompatActivity;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback{

    NFCHandler nfcHandler = new NFCHandler();
    NfcAdapter nfcAdapter;
    NFCTagHeader tagHeader;
    Tag tag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nfcAdapter = nfcHandler.createAdapter(this);
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

        Log.v("here", "here");
        tag = localTag;
        assert tag != null;
        tagHeader = new NFCTagHeader(tag);
        //Toast.makeText(this, "New card with UID " + tagHeader.getUID() + " detected", Toast.LENGTH_SHORT).show();
    }

}