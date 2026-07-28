package net.montoyo.wd.utilities.browser;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;

public class WDBrowserHelper {
    public static MCEFBrowser createBrowser(String url, boolean transparent) {
        WDClientBrowser browser = new WDClientBrowser(MCEF.getClient(), url, transparent);
        browser.setCloseAllowed();
        browser.createImmediately();
        WDBrowser.registerQueries(browser);
        // return browser.getBrowser();
        return browser; // Temporary fix - may need to return WDClientBrowser directly
    }

    public static void registerQueries(WDClientBrowser browser) {
        WDBrowser.registerQueries(browser);
    }
}
