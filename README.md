# Welcome to PKCS11Util

A little suit of Java programs cobbled together using inspiration from a number of sites across the web, my own trial and error, and need to use a DoD CAC to access sites using soft and hard token certificates.

# Linux Install OpenSC (optional for hard token smartcards)

```
sudo apt-get install opensc
```

## References

# OSX Install OpenSC (optional for hard token smartcards)

see: [macOS Quick Start](https://github.com/OpenSC/OpenSC/wiki/macOS-Quick-Start)

- [x] To Do

# Windows Install OpenSC (optional for hard token smartcards)

- [ ] To Do

# PKCS11Util

This program tests to ensure that the keystore containing soft and hard tokens
is accessible by either Java Keystore (soft) or OpenSC (hard) Keystore.

## Usage
```
javac PKCS11Util.java
java PKCS11Util eca  # (soft)
java PKCS11Util cac  # (hard CAC)
```

## Note

The slot the smartcard is in will vary from machine to machine. For me, under **OSX** it was **_0_** while under **Ubuntu** it was **_1_**, on one machine with an internal card reader and **_5_**, on another machine with an external card reader. For now, please change the code to set the proper slot number in the code for the **OS** used in ```PKCS11Util.java``` near:
```
else if (osname.startsWith(LINUX)) {
  config.put("library", "/usr/lib/x86_64-linux-gnu/opensc-pkcs11.so");
  config.put("slot", "1");
}
else if (osname.startsWith(OS_X)) {
  config.put("library", "/usr/local/lib/opensc-pkcs11.so");
  config.put("slot", "0");
}
``` 
# Files Created

# Example

# Dependencies

- [`opensc`](https://github.com/OpenSC/OpenSC): to read a smartcard
- `Oracle Java 7 or OpenJDK 7`: to generate a plaintext password temporary file

# Task Lists
- [ ] OSX
- [x] Debian/Ubuntu
- [ ] CentOS/Fedora (untested)
- [ ] Windows Command Prompt
- [ ] PKSC11Util: Make the hard token slot a command line parameter

