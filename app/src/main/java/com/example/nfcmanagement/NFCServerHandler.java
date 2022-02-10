package com.example.nfcmanagement;
import android.util.Log;

import org.json.JSONObject;
import org.json.*;
import java.net.*;

import java.net.ServerSocket;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

interface NFCServerCallback
{
    JSONObject HandleRequest(JSONObject RequestObject);
}

public class NFCServerHandler implements Runnable
{
    public void run()
    {
        p_ListeningLoop();
    }


    NFCServerCallback m_CallbackToUse = null;
    Thread m_ListeningThread = null;

    void p_ListeningLoop()
    {
        try
        {
            Log.i("NFCServer","Listening loop engaged");
            ServerSocket ListeningSocket = new ServerSocket(13378);

            Socket ClientConnection = ListeningSocket.accept();

            Log.i("NFCServer","Connection establishe!");

            InputStream ClientInput = ClientConnection.getInputStream();
            OutputStream ClientOutput = ClientConnection.getOutputStream();
            while(true)
            {
                JSONObject  ClientRequest = new JSONObject((new Scanner(ClientInput, "UTF-8").nextLine()));

                JSONObject Response = m_CallbackToUse.HandleRequest(ClientRequest);

                ClientOutput.write(Response.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        catch (Exception e)
        {
            Log.d("NFCServerError",e.getMessage());
        }
    }

    public NFCServerHandler(NFCServerCallback Callback)
    {
        m_CallbackToUse = Callback;
        m_ListeningThread = new Thread(this);
        m_ListeningThread.start();
    }

}
