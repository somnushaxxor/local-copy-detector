package ru.nsu.fit.kolesnik.localcopydetector;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CopyDetector {

    private static final int MULTICAST_SOCKET_READ_TIMEOUT = 1000;
    private static final long ALIVE_TIMEOUT_MILLIS = 5000;

    private boolean working;
    private final UUID uuid;
    private Map<UUID, Long> copiesTimeouts;
    private Map<UUID, InetAddress> copiesInetAddresses;
    private boolean aliveCopiesListChanged;
    private final MulticastSocket multicastSocket;
    private final SocketAddress groupSocketAddress;
    private final NetworkInterface groupNetworkInterface;

    public CopyDetector(String groupHostName, int port, String groupNetworkInterfaceName) {
        working = true;
        uuid = UUID.randomUUID();
        copiesTimeouts = new HashMap<>();
        copiesInetAddresses = new HashMap<>();
        aliveCopiesListChanged = false;
        try {
            multicastSocket = new MulticastSocket(port);
            multicastSocket.setSoTimeout(MULTICAST_SOCKET_READ_TIMEOUT);
            groupNetworkInterface = NetworkInterface.getByName(groupNetworkInterfaceName);
            groupSocketAddress = new InetSocketAddress(groupHostName, port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        try {
            GroupMessage outgoingGroupMessage = new GroupMessage(uuid, InetAddress.getLocalHost());
            multicastSocket.joinGroup(groupSocketAddress, groupNetworkInterface);
            System.out.println("Detecting started!");
            while (working) {
                sendGroupMessage(outgoingGroupMessage);
                handleIncomingGroupMessages();
            }
            System.out.println("Detecting stopped!");
            terminate();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        working = false;
    }

    private void sendGroupMessage(GroupMessage message) {
        try {
            byte[] messageBytes = message.getBytes();
            DatagramPacket outgoingDatagramPacket = new DatagramPacket(messageBytes, messageBytes.length, groupSocketAddress);
            multicastSocket.send(outgoingDatagramPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleIncomingGroupMessages() {
        DatagramPacket incomingDatagramPacket = new DatagramPacket(new byte[GroupMessage.SIZE_BYTES], GroupMessage.SIZE_BYTES, groupSocketAddress);
        try {
            while (true) {
                multicastSocket.receive(incomingDatagramPacket);
                Long incomingDatagramPacketReceiveTimeMillis = System.currentTimeMillis();
                GroupMessage incomingGroupMessage = new GroupMessage(incomingDatagramPacket.getData());
                if (!uuid.equals(incomingGroupMessage.getUuid())) {
                    if (!copiesTimeouts.containsKey(incomingGroupMessage.getUuid())) {
                        aliveCopiesListChanged = true;
                    }
                    copiesTimeouts.put(incomingGroupMessage.getUuid(), incomingDatagramPacketReceiveTimeMillis);
                    copiesInetAddresses.put(incomingGroupMessage.getUuid(), incomingGroupMessage.getInetAddress());
                }
            }
        } catch (SocketTimeoutException e) {
            checkTimeouts();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (aliveCopiesListChanged) {
            printAliveCopiesList();
            aliveCopiesListChanged = false;
        } else {
            System.out.println("Alive copies list has not changed!");
        }
    }

    private void printAliveCopiesList() {
        System.out.println("New alive copies list:");
        copiesTimeouts.forEach((uuid, millis) -> System.out.println(copiesInetAddresses.get(uuid).getHostAddress()));
    }

    private void checkTimeouts() {
        HashMap<UUID, Long> newCopiesTimeouts = new HashMap<>();
        HashMap<UUID, InetAddress> newCopiesInetAddresses = new HashMap<>();
        copiesTimeouts.forEach((uuid, millis) -> {
            if (System.currentTimeMillis() - millis > ALIVE_TIMEOUT_MILLIS) {
                aliveCopiesListChanged = true;
            } else {
                newCopiesTimeouts.put(uuid, millis);
                newCopiesInetAddresses.put(uuid, copiesInetAddresses.get(uuid));
            }
        });
        copiesTimeouts = newCopiesTimeouts;
        copiesInetAddresses = newCopiesInetAddresses;
    }

    private void terminate() {
        try {
            multicastSocket.leaveGroup(groupSocketAddress, groupNetworkInterface);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
