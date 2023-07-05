# 📡 Local Copy Detector

This is a simple CLI application that runs on the local network and detects its own copies.

---

## ✨ Description

The project's aim is to learn what is **_multicasting_** and how it can be used within socket programming.

---

## 🧰 Technologies

- Java 17

---

## 🚀 Run

Run the following command from the project root directory:

```
./gradle run --args="MULTICAST_ADDRESS MULTICAST_PORT MULTICAST_NETWORK_INTERFACE"
```

where:

- `MULTICAST_ADDRESS` is an IPv4 address in range from 224.0.0.0 to 239.255.255.255
- `MULTICAST_PORT` is a port which the application will be listening on
- `MULTICAST_NETWORK_INTERFACE` is an interface to receive multicast datagram packets

Example:

```
./gradle run --args="230.1.1.1 12000 wifi0"
```

---

## 💡 Usage

Press `Ctrl+C` to stop the application.