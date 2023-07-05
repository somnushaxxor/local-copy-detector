package ru.nsu.fit.kolesnik.localcopydetector;

public class Main {

    public static void main(String[] args) {
        checkArgs(args);
        String groupHostName = args[0];
        int port = Integer.parseInt(args[1]);
        String groupNetworkInterfaceName = args[2];
        CopyDetector copyDetector = new CopyDetector(groupHostName, port, groupNetworkInterfaceName);
        Runtime.getRuntime().addShutdownHook(new Thread(copyDetector::stop));
        copyDetector.start();
    }

    private static void checkArgs(String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException("Wrong number of arguments! Must be 3!");
        }
    }

}
