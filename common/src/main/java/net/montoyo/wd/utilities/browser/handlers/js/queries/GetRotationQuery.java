package net.montoyo.wd.utilities.browser.handlers.js.queries;

import com.google.gson.JsonObject;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.utilities.browser.WDBrowser;
import net.montoyo.wd.utilities.browser.handlers.js.JSQueryHandler;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;

public class GetRotationQuery extends JSQueryHandler {
    public GetRotationQuery() {
        super("GetRotation");
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
                callback.success("{\"rotation\":" + scr.rotation.ordinal() + "}");
                return true;
            }
        }
        callback.failure(404, "Screen has been removed.");
        return true;
    }
}
