package ru.nsu.fit.kolesnik.localcopydetector;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class GroupMessage {

    private final UUID uuid;
    private final InetAddress inetAddress;
    public final static int SIZE_BYTES = 32;

    public GroupMessage(UUID uuid, InetAddress inetAddress) {
        this.uuid = uuid;
        this.inetAddress = inetAddress;
    }

    public GroupMessage(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        final int UUID_SIZE_BYTES = 16;
        final int INET_ADDRESS_SIZE_BYTES = 16;
        byte[] uuidBytes = new byte[UUID_SIZE_BYTES];
        byte[] inetAddressBytes = new byte[INET_ADDRESS_SIZE_BYTES];
        bb.get(uuidBytes, 0, uuidBytes.length);
        bb.get(inetAddressBytes, 0, inetAddressBytes.length);
        uuid = UUIDUtils.convertBytesToUUID(uuidBytes);
        try {
            inetAddress = InetAddress.getByAddress(inetAddressBytes);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getBytes() {
        byte[] uuidBytes = UUIDUtils.convertUUIDToBytes(uuid);
        byte[] inetAddressBytes = inetAddress.getAddress();
        return ByteBuffer.allocate(SIZE_BYTES).put(uuidBytes).put(inetAddressBytes).array();
    }

    public UUID getUuid() {
        return uuid;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

}
