package it.unibo.mobile.d2dchat.device;

import android.net.wifi.p2p.WifiP2pInfo;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import it.unibo.mobile.d2dchat.Constants;
import it.unibo.mobile.d2dchat.messagesManager.Message;
import it.unibo.mobile.d2dchat.messagesManager.MessageManager;

/**
 * Created by asig on 1/3/17.
 */

public class Client extends Peer {
    private Socket server;
    private MessageManager messageManager;
    private DeviceManager deviceManager;
    private ArrayList<ArrayList<Message>> goQueue;
    private int discarded = 0;
    private static final String TAG = "Client";

    public Client(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
        goQueue = new ArrayList<>(deviceManager.GOlist.size());
        for (int i = 0; i < deviceManager.GOlist.size(); i++) {
            goQueue.add(new ArrayList<Message>(20));
        }
    }

    @Override
    public void onConnect(WifiP2pInfo info) {
        // every time a new wifi connection is established we need to create a new socket
        server = new Socket();
        try {
            server.bind(null);
            server.connect(new InetSocketAddress(info.groupOwnerAddress.getHostAddress(),
                    Constants.SERVER_PORT), 5000);
            // stop old instance because it's using an old socket
            if (messageManager != null)
                messageManager.keepRunning = false;
            messageManager = new MessageManager(server, this);
           messageManager.start();
        } catch (IOException e) {
            //La connessione non è stata posssibile! Forse non sta usando la nostra applicazione?
            e.printStackTrace();
            try {
                server.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        Log.d(TAG, "onConnect() created new socket");
        sendQueued();
    }

    @Override
    public void onDisconnect() {
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        discarded += goQueue.get(deviceManager.currentGO).size();
        goQueue.get(deviceManager.currentGO).clear();
        messageManager.keepRunning = false;
    }

    @Override
    public void writeMessage(Message message) {
        messageManager.write(message);
    }

    @Override
    public void receiveMessage(Message message, MessageManager messageManager) {
        goQueue.get(deviceManager.currentGO).add(message);
    }

    public void sendQueued() {
        int dest;
        dest = 1 - deviceManager.currentGO; // we assume only 2 GOs
        while (!goQueue.get(dest).isEmpty()) {
            Message message = goQueue.get(dest).get(0);
            goQueue.get(dest).remove(0);
            messageManager.write(message);
        }
        Log.d(TAG, "Sent all queued messages.");
    }
}
