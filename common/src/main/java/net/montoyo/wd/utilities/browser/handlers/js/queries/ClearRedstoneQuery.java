package net.montoyo.wd.utilities.browser.handlers.js.queries;

import com.google.gson.JsonObject;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.server_bound.C2SMessageRedstoneOutput;
import net.montoyo.wd.utilities.browser.WDBrowser;
import net.montoyo.wd.utilities.browser.handlers.js.JSQueryHandler;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;

public class ClearRedstoneQuery extends JSQueryHandler {
    public ClearRedstoneQuery() {
        super("ClearRedstone");
    }

    @Override
    public boolean handle(CefBrowser browser, CefFrame frame, JsonObject data, boolean persistent, CefQueryCallback callback) {
        if (!(browser instanceof WDBrowser wdBrowser) || wdBrowser.getSide() == null || wdBrowser.getBe() == null) {
            callback.failure(404, "Screen has been removed.");
            return true;
        }

        if (!wdBrowser.getBe().hasUpgrade(wdBrowser.getSide(), DefaultUpgrade.REDSTONE_OUTPUT)) {
            callback.failure(403, "Missing upgrade");
            return true;
        }

        boolean clearAll = data != null && data.has("all") && data.get("all").getAsBoolean();

        WDNetworkRegistry.sendToServer(new C2SMessageRedstoneOutput(
                wdBrowser.getBe().getBlockPos(), wdBrowser.getSide(), clearAll
        ));

        callback.success("{\"status\":\"pending\"}");
        return true;
    }
}
