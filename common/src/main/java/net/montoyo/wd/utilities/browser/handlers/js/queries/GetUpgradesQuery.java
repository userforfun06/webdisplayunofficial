package net.montoyo.wd.utilities.browser.handlers.js.queries;

import com.google.gson.JsonObject;
import net.montoyo.wd.core.IUpgrade;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.utilities.browser.WDBrowser;
import net.montoyo.wd.utilities.browser.handlers.js.JSQueryHandler;
import net.montoyo.wd.utilities.serialization.Util;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;

public class GetUpgradesQuery extends JSQueryHandler {
    public GetUpgradesQuery() {
        super("GetUpgrades");
    }

    @Override
    public boolean handle(CefBrowser browser, CefFrame frame, JsonObject data, boolean persistent, CefQueryCallback callback) {
        if (browser instanceof WDBrowser wdBrowser) {
            if (wdBrowser.getSide() != null && wdBrowser.getBe() != null) {
                ScreenData scr = wdBrowser.getBe().getScreen(wdBrowser.getSide());
                if (scr == null) {
                    callback.failure(404, "Screen side not found.");
                    return true;
                }

                StringBuilder sb = new StringBuilder("{\"upgrades\":[");
                for (int i = 0; i < scr.upgrades.size(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append('"');
                    sb.append(Util.addSlashes(((IUpgrade) scr.upgrades.get(i).getItem()).getJSName(scr.upgrades.get(i))));
                    sb.append('"');
                }
                callback.success(sb.append("]}").toString());
                return true;
            }
        }
        callback.failure(404, "Screen has been removed.");
        return true;
    }
}
