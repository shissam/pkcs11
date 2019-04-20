# Welcome to PKCS11Util

A little suit of Java programs cobbled together using inspiration from a number of sites across the web, my own trial and error, and need to use a DoD CAC to access sites using soft and hard token certificates.

# Linux Install OpenSC (optional for hard token smartcards)

```
sudo apt-get install opensc
```

## References

# OSX Install OpenSC (optional for hard token smartcards)

- [ ] To Do

# Windows Install OpenSC (optional for hard token smartcards)

- [ ] To Do

# PKCS11Util

This program test to ensure that the keystore containing soft and hard tokens is accessible by either Java Keystore (soft) or OpenSC (hard) Keystore.

```
javac PKCS11Util.java
java PKCS11Util eca  # (soft)
java PKCS11Util cac  # (hard CAC)
```

# Usage

# Warning

# Files Created

# Example

# Dependencies

- `opensc`: to parse the plaintext password input file
- `Oracle Java 7 or OpenJDK 7`: to generate a plaintext password temporary file

# Task Lists
- [ ] OSX
- [x] Debian/Ubuntu
- [ ] CentOS/Fedora (untested)
- [ ] Windows Command Prompt

