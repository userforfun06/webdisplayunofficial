package net.montoyo.wd.utilities.browser.handlers.js.queries;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.utilities.browser.WDBrowser;
import net.montoyo.wd.utilities.browser.handlers.js.JSQueryHandler;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;

public class IsOwnerQuery extends JSQueryHandler {
    public IsOwnerQuery() {
        super("IsOwner");
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

                Player player = WebDisplays.PROXY.getLocalPlayer();
                boolean res = scr.owner != null && player != null && scr.owner.uuid.equals(player.getGameProfile().getId());
                callback.success("{\"isOwner\":" + res + "}");
                return true;
            }
        }
        callback.failure(404, "Screen has been removed.");
        return true;
    }
}
