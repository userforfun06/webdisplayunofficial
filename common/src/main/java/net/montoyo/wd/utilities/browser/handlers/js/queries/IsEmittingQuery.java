package net.montoyo.wd.utilities.browser.handlers.js.queries;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.utilities.browser.WDBrowser;
import net.montoyo.wd.utilities.browser.handlers.js.JSQueryHandler;
import net.montoyo.wd.utilities.math.Vector3i;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;

public class IsEmittingQuery extends JSQueryHandler {
    public IsEmittingQuery() {
        super("IsEmitting");
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

        if (data == null || !data.has("x") || !data.has("y")) {
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

        if (x < 0 || x >= scr.size.x || y < 0 || y >= scr.size.y) {
            callback.failure(403, "Out of range");
            return true;
        }

        var side = wdBrowser.getSide();
        var be = wdBrowser.getBe();
        var level = be.getLevel();
        if (level == null) {
            callback.failure(500, "No level");
            return true;
        }

        BlockPos bpos = (new Vector3i(be.getBlockPos())).addMul(side.right, x).addMul(side.up, y).toBlock();
        BlockState bs = level.getBlockState(bpos);
        boolean emitting = bs.getValue(ScreenBlock.emitting);
        callback.success("{\"emitting\":" + emitting + "}");
        return true;
    }
}
