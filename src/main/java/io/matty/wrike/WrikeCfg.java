package io.matty.wrike;

import java.io.FileInputStream;
import java.util.Properties;

public class WrikeCfg {

    public static String clientID = "";
    public static String clientSecret = "";
    public static String callbackUri = "";

    public WrikeCfg() {
        // get local.properties with your secret stuff
        // https://developers.wrike.com/getting-started/
        try {
            Properties propeties = new Properties();
            FileInputStream in = null;
            in = new FileInputStream("local.properties");
            propeties.load(in);
            this.clientID = propeties.getProperty("clientID");
            this.callbackUri = propeties.getProperty("callbackUri");
            this.clientSecret = propeties.getProperty("clientSecret");
            in.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }


}