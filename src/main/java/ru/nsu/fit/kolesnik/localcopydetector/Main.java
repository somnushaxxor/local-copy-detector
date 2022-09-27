package ru.nsu.fit.kolesnik.localcopydetector;

public class Main {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Wrong number of arguments! Must be 2!");
            return;
        }
        String groupHostName = args[0];
        String groupNetworkInterfaceName = args[1];
        CopyDetector copyDetector = new CopyDetector(groupHostName, groupNetworkInterfaceName);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> copyDetector.stop()));
        copyDetector.start();
    }

}
