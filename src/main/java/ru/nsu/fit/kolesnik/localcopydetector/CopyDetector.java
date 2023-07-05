package ru.nsu.fit.kolesnik.localcopydetector;

import ru.nsu.fit.kolesnik.localcopydetector.message.GroupMessage;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CopyDetector {

    private static final int MULTICAST_SOCKET_READ_TIMEOUT = 1000;
    private static final long ALIVE_TIMEOUT_MILLIS = 5000;

    private boolean working;
    private final UUID currentInstanceUuid;
    private Map<UUID, Long> copiesTimeouts;
    private Map<UUID, InetAddress> copiesInetAddresses;
    private boolean aliveCopiesListChanged;
    private final MulticastSocket multicastSocket;
    private final SocketAddress groupSocketAddress;
    private final NetworkInterface groupNetworkInterface;

    public CopyDetector(String groupHostName, int port, String groupNetworkInterfaceName) {
        working = true;
        currentInstanceUuid = UUID.randomUUID();
        copiesTimeouts = new HashMap<>();
        copiesInetAddresses = new HashMap<>();
        aliveCopiesListChanged = false;
        try {
            multicastSocket = new MulticastSocket(port);
            multicastSocket.setSoTimeout(MULTICAST_SOCKET_READ_TIMEOUT);
            groupNetworkInterface = NetworkInterface.getByName(groupNetworkInterfaceName);
            groupSocketAddress = new InetSocketAddress(groupHostName, port);
        } catch (IOException e) {
            throw new CopyDetectorException("CopyDetector instantiation failed", e);
        }
    }

    public void start() {
        try {
            multicastSocket.joinGroup(groupSocketAddress, groupNetworkInterface);
            GroupMessage outgoingGroupMessage = new GroupMessage(currentInstanceUuid, InetAddress.getLocalHost());
            System.out.println("Detecting started!");
            while (working) {
                sendGroupMessage(outgoingGroupMessage);
                handleIncomingGroupMessages();
            }
            System.out.println("Detecting stopped!");
            terminate();
        } catch (IOException e) {
            throw new CopyDetectorException("Failed to start detecting copies", e);
        }
    }

    private void sendGroupMessage(GroupMessage message) {
        try {
            byte[] messageBytes = message.getBytes();
            DatagramPacket outgoingDatagramPacket = new DatagramPacket(messageBytes, messageBytes.length,
                    groupSocketAddress);
            multicastSocket.send(outgoingDatagramPacket);
        } catch (IOException e) {
            throw new CopyDetectorException("Failed to send group message", e);
        }
    }

    private void handleIncomingGroupMessages() {
        DatagramPacket incomingDatagramPacket = new DatagramPacket(new byte[GroupMessage.SIZE_BYTES],
                GroupMessage.SIZE_BYTES, groupSocketAddress);
        try {
            while (true) {
                multicastSocket.receive(incomingDatagramPacket);
                Long incomingDatagramPacketReceiveTimeMillis = System.currentTimeMillis();
                GroupMessage incomingGroupMessage = new GroupMessage(incomingDatagramPacket.getData());
                if (!currentInstanceUuid.equals(incomingGroupMessage.getUuid())) {
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
            throw new CopyDetectorException("Failed to receive group message", e);
        }
        if (aliveCopiesListChanged) {
            printAliveCopiesList();
            aliveCopiesListChanged = false;
        } else {
            System.out.println("Alive copies list has not changed!");
        }
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

    private void printAliveCopiesList() {
        System.out.println("New alive copies list:");
        copiesTimeouts.forEach((uuid, millis) -> System.out.println(copiesInetAddresses.get(uuid).getHostAddress()));
    }

    public void stop() {
        working = false;
    }

    private void terminate() {
        try {
            multicastSocket.leaveGroup(groupSocketAddress, groupNetworkInterface);
        } catch (IOException e) {
            throw new CopyDetectorException("CopyDetector termination failed", e);
        }
    }

}
