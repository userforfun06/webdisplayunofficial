package net.montoyo.wd.utilities.browser.handlers.js.queries;

import com.google.gson.JsonObject;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.server_bound.C2SMessageRedstoneOutput;
import net.montoyo.wd.utilities.browser.WDBrowser;
import net.montoyo.wd.utilities.browser.handlers.js.JSQueryHandler;
import net.montoyo.wd.utilities.data.BlockSide;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;

public class SetRedstoneAtQuery extends JSQueryHandler {
    public SetRedstoneAtQuery() {
        super("SetRedstoneAt");
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

        if (data == null || !data.has("x") || !data.has("y") || !data.has("state")) {
            callback.failure(400, "Wrong arguments");
            return true;
        }

        ScreenData scr = wdBrowser.getBe().getScreen(wdBrowser.getSide());
        if (scr == null) {
            callback.failure(404, "Screen side not found.");
            return true;
        }

        int x = data.get("x").getAsInt();
        int y = data.get("y").getAsInt();
        boolean state = data.get("state").getAsBoolean();

        if (x < 0 || x >= scr.size.x || y < 0 || y >= scr.size.y) {
            callback.failure(403, "Out of range");
            return true;
        }

        BlockSide side = wdBrowser.getSide();

        WDNetworkRegistry.sendToServer(new C2SMessageRedstoneOutput(
                wdBrowser.getBe().getBlockPos(), side, x, y, state
        ));

        callback.success("{\"status\":\"pending\"}");
        return true;
    }
}
