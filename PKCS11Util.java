//
// inspired from: https://community.smartbear.com/t5/SoapUI-Pro/PKCS-11-Keystore-Support-Smartcard/td-p/15660
//
// added soft cert token support via jks
// added hard cert token support via opensc
//

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.Security;
import java.security.KeyStore.CallbackHandlerProtection;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Properties;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.callback.CallbackHandler;

import sun.security.pkcs11.SunPKCS11;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class PKCS11Util {

  static private KeyStore.PasswordProtection _protParam = null;
  static private Pattern _SubjectDNpattern = null;

  public static String convertToMultiline(String orig) {
    return "<html>" + orig.replaceAll("\n", "<br>");
  }

  public static String convertFromMultiline(String orig) {
    String t;
    // with the help from
    // http://www.regexplanet.com/advanced/java/index.html
    // and
    // https://docs.oracle.com/javase/tutorial/essential/regex/pre_char_classes.html
    System.out.println ("T: '" + orig + "'");
    t = orig.replaceAll("<br>[\\S|\\s]*", "");
    System.out.println ("T: '" + t + "'");
    t = t.replaceAll("<html>", "");
    System.out.println ("T: '" + t + "'");
    return t;
  }

  private static Provider createPkcs11Provider(Properties config) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      config.store(baos, null);
      SunPKCS11 result = new SunPKCS11(new ByteArrayInputStream(baos.toByteArray()));
      //SunPKCS11 result = new SunPKCS11("c:\\opensc-java.cfg");
      if (result.getService("KeyStore", "PKCS11") == null) {
        throw new RuntimeException(
          "No PKCS#11 Service available. Probably Security Token (Smartcard) not inserted");
      }

      // Register the Provider
      if (Security.getProvider(result.getName()) != null) {
        Security.removeProvider(result.getName());
      }

      Security.addProvider(result);
      return result;
    } catch (IOException e) {
      throw new RuntimeException("Failed to install SUN PKCS#11 Provider", e);
    }
  }

  public static final String WINDOWS = "win";
  public static final String OS_X= "mac";
  public static final String LINUX = "lin";
	
  public static KeyStore loadPKCS11Keystore(String dll,
                          CallbackHandler passwordCallbackHandler) {

    Properties config = new Properties();

    String osname = System.getProperty("os.name").toLowerCase();
    System.out.println ("OS Name is: " + osname);

    if (osname.startsWith(WINDOWS)) {
      //config.put("library", "C:\\WINDOWS\\System32\\opensc-pkcs11.dll");
      //config.put("library", "C:\\Windows\\System32\\opensc-pkcs11.dll");
      //config.put("library", "C:\\SEI\\Tools\\src\\afsmart\\workspace\\afsmart-ws\\build\\classes\\opensc-pkcs11.dll");
      //config.put("library", "C:\\SEI\\Tools\\src\\afsmart\\workspace\\afsmart-ws\\build\\classes\\opensc-pkcs11.dll");
      //config.put("library", "C:\\opensc-pkcs11.dll");
      //System.out.println ("putting to config: " + "'" + config.get("library")+ "'");
      //config.put("library", "C:\\SEI\\Tools\\src\\afsmart-ws\\build\\classes\\opensc-pkcs11.dll");
    }
    else if (osname.startsWith(LINUX)) {
      config.put("library", "/usr/lib/x86_64-linux-gnu/opensc-pkcs11.so");
      config.put("slot", "1");
    }
    else if (osname.startsWith(OS_X)) {
      config.put("library", "/usr/local/lib/opensc-pkcs11.so");			
      config.put("slot", "0");
    }
    else {
      System.out.println ("cannot determine OS: " + osname);
    }

    System.out.println ("OS Name is: " +
      osname + " lib: " + config.getProperty("library"));
    config.put("name", "OpenSC");
    config.put("showInfo", "true");
    config.put("description", "SunPKCS11 w/ OpenSC Smart card Framework");
    return loadPKCS11Keystore(config, passwordCallbackHandler);
  }

  public static KeyStore loadPKCS11Keystore(Properties config,
                           CallbackHandler passwordCallbackHandler) {
    try {
      Provider pkcs11Prov = createPkcs11Provider(config);
      CallbackHandlerProtection pwCallbackProt =
        new CallbackHandlerProtection(passwordCallbackHandler);
      KeyStore.Builder builder =
        KeyStore.Builder.newInstance("PKCS11", pkcs11Prov, pwCallbackProt);
      return builder.getKeyStore();
    } catch (KeyStoreException e) {
      throw new RuntimeException("Failed to load PKCS#11 Keystore", e);
    }
  }

  public static KeyStore.PasswordProtection getKeyStorePassword() {
    return _protParam;
  }
	
  public static KeyStore loadJKSKeystore(String path,
                           char[] keystorePassword) {
    String selectedFile = path;

    if (selectedFile == null) { // TBD: should work in windowed and tty env
      javax.swing.JFileChooser fileChooser =
        new javax.swing.JFileChooser();
      javax.swing.filechooser.FileNameExtensionFilter filter =
        new javax.swing.filechooser.FileNameExtensionFilter("Java KeyStore", "jks");
      // fileChooser.addChoosableFileFilter(filter);
      fileChooser.setFileFilter(filter);
      if (fileChooser.showOpenDialog(null) ==
            javax.swing.JFileChooser.APPROVE_OPTION) {
        selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
      } else {
        System.out.println("cannot continue without an ECA certificate from a Java Keystore");
        System.exit(0);
      }
    }

    if (keystorePassword == null) { // TBD: should work in windowed and tty env
      javax.swing.JLabel label =
        new javax.swing.JLabel("Please enter your password:");
      javax.swing.JPasswordField jpf =
        new javax.swing.JPasswordField();
      int option =
        JOptionPane.showConfirmDialog(null,
          new Object[] { label, jpf },
          "Java KeyStore Password",
          JOptionPane.OK_CANCEL_OPTION);
      // System.out.println ("you entered: " + option + " with: " + new
      // String(jpf.getPassword()));
      // System.out.println ("YES: " + JOptionPane.YES_OPTION + " CANCEL:
      // " + JOptionPane.CANCEL_OPTION);
      if (option == JOptionPane.YES_OPTION) {
        keystorePassword = jpf.getPassword();
      } else if (option == JOptionPane.CANCEL_OPTION) {
        System.out.println
          ("assuming there is no password for the Java Keystore");
      } else {
        System.out.println
          ("will not continue without a password for the Java Keystore");
        System.exit(0);
      }
    }

    KeyStore keyStore = null;
    try {
      keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(new FileInputStream(selectedFile), keystorePassword);
      _protParam = new KeyStore.PasswordProtection(keystorePassword);
      System.out.println ("KeyStore.PasswordProtection SET");
      return keyStore;
    } catch (KeyStoreException e) {
      throw new RuntimeException("Failed to load JKS Keystore", e);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Failed to find file: " + path, e);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file: " + path, e);
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new RuntimeException("unsupported algorithm for JKS Keystore", e);
    } catch (java.security.cert.CertificateException e) {
      throw new RuntimeException("unsupported certificate for JKS Keystore", e);
    }
  }

  public static void main(String[] args) throws Exception {
    // Test for the DataKey 330 Smartcard
    // (dskck201.dll is installed with CIP Utilities from DataKey)
    boolean useCAC = false;
    boolean useECA = true;
    String cardImage = " ";
    String tokenPrompt = " ";

    if (args.length > 0) {
      if (args[0].equalsIgnoreCase("cac")) {
        useCAC = true;
        useECA = false;
        cardImage = "CAC_Card-small.jpg";
        tokenPrompt = "Please select your PKCS11 (CAC/Smartcard) Token";
      }
      if (args[0].equalsIgnoreCase("eca")) {
        useCAC = false;
        useECA = true;
        cardImage = "java2sLogo.GIF";
        tokenPrompt = "Please select your Java KeyStore (ECA/Softcard) Token";
      }
    }

    KeyStore ks = null;
    Pattern pattern = null;
    char[] keystorePassword = null;

    pattern = Pattern.compile("CN=(.*?)[,]");

    if (useCAC) {
      ks = loadPKCS11Keystore("ignored-here-in-this-example-dkck201.dll",
        new SwingPasswordCallbackHandler());
      keystorePassword = null;
    }

    if (useECA) {
      String selectedFile = " ";
      javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
      javax.swing.filechooser.FileNameExtensionFilter filter =
        new javax.swing.filechooser.FileNameExtensionFilter(
                                                    "Java KeyStore", "jks");
      // fileChooser.addChoosableFileFilter(filter);
      fileChooser.setFileFilter(filter);
      if (fileChooser.showOpenDialog(null) ==
           javax.swing.JFileChooser.APPROVE_OPTION) {
         selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
      } else {
         System.out.println("cannot continue without an ECA certificate from a Java Keystore");
         System.exit(0);
      }

      javax.swing.JLabel label = new javax.swing.JLabel("Please enter your password:");
      javax.swing.JPasswordField jpf =
        new javax.swing.JPasswordField();
      int option =
        JOptionPane.showConfirmDialog(null,
          new Object[]{label, jpf},
          "Java KeyStore Password",
          JOptionPane.OK_CANCEL_OPTION);
      //System.out.println ("you entered: " + option + " with: " + new String(jpf.getPassword()));
      //System.out.println ("YES: " + JOptionPane.YES_OPTION + " CANCEL: " + JOptionPane.CANCEL_OPTION);
      if (option == JOptionPane.YES_OPTION) {
        keystorePassword = jpf.getPassword();
      } else if (option == JOptionPane.CANCEL_OPTION) {
        System.out.println(
          "assuming there is no password for the Java Keystore");
      } else {
        System.out.println(
          "will not continue without a password for the Java Keystore");
        System.exit(0);
      }

      ks = loadJKSKeystore(selectedFile, keystorePassword);
    }

    System.out.println("ks type: " + ks.getType());
    System.out.println("ks provider: " + ks.getProvider());
    System.out.println("ks default type: " + ks.getDefaultType());
    // System.out.println ("ks contains aliases: " + ks.containsAlias());
    System.out.println("ks number of entries: " + ks.size()); 

    List<String> certAliases = new ArrayList<String>();
    certAliases.add(" ");

    for (Enumeration<String> aliases =
         ks.aliases(); aliases.hasMoreElements();) {
      String alias = aliases.nextElement();
      System.out.println(alias);

      // print certifcate
      Certificate cert = ks.getCertificate(alias);
      if (cert != null) {
        // System.out.print(" Certificate found. type="+cert.getType());
        if (cert instanceof X509Certificate) {
          X509Certificate x509 = (X509Certificate) cert;
          System.out.print(
            " SubjectDN="+x509.getSubjectDN()+" IssuerDN="+x509.getIssuerDN());
          Key pk = null;
          if (useCAC) {
            // private key is accessed without password
            pk = ks.getKey(alias, null);
          }
          if (useECA) {
            //pk = ks.getKey(alias, "changeme".toCharArray());
            pk = ks.getKey(alias, keystorePassword);
          }
          if (pk != null) {
            System.out.println(
              " Private key found. algorithm=" + pk.getAlgorithm());
            Matcher matcher = pattern.matcher(x509.getSubjectDN().toString());
            while (matcher.find()) {
              certAliases.add(
                "<html>" + alias + "<br>SubjectDN=" + matcher.group(1));
              // certAliases.add(matcher.group(1));
            }
            /********
            if (pk instanceof PrivateKey) {
              PrivateKey PvtKey = (PrivateKey) pk;
              System.out.println("algorithm: " + PvtKey.getAlgorithm());
              byte[] b = PvtKey.getEncoded();
              if (b == null) {
                System.out.println("length: " + "PrivateKey cannot be encoded");
              } else {
                System.out.println("format: " + PvtKey.getFormat());
                System.out.println("length: " + b.length);
                System.out.println("bytes : " + getHexString(b));
              }
            }
            ********/
          }
        }
        // System.out.println();
      }
      System.out.println();
    }

    for (String s : certAliases) {
      System.out.println(s);
    }

    String input = (String) JOptionPane.showInputDialog(
        new JFrame(),
        tokenPrompt, "Authentication Credentials",
        JOptionPane.INFORMATION_MESSAGE, new ImageIcon(cardImage),
        certAliases.toArray(new String[certAliases.size()]),
        "Certificate for PIV Authentication");

    System.out.println("User's input raw: " + input);
    System.out.println("User's input    : " + convertFromMultiline(input));
    System.exit(0);
  }

}
