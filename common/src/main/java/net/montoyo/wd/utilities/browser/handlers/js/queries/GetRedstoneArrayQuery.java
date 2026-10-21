package net.montoyo.wd.utilities.browser.handlers.js.queries;

import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.utilities.browser.WDBrowser;
import net.montoyo.wd.utilities.browser.handlers.js.JSQueryHandler;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;

import java.util.StringJoiner;

public class GetRedstoneArrayQuery extends JSQueryHandler {
    public GetRedstoneArrayQuery() {
        super("GetRedstoneArray");
    }

    @Override
    public boolean handle(CefBrowser browser, CefFrame frame, JsonObject data, boolean persistent, CefQueryCallback callback) {
        if (!(browser instanceof WDBrowser wdBrowser) || wdBrowser.getSide() == null || wdBrowser.getBe() == null) {
            callback.failure(404, "Screen has been removed.");
            return true;
        }

        if (!wdBrowser.getBe().hasUpgrade(wdBrowser.getSide(), DefaultUpgrade.REDSTONE_INPUT)) {
            callback.failure(403, "Missing upgrade");
            return true;
        }

        var side = wdBrowser.getSide();
        var be = wdBrowser.getBe();
        var level = be.getLevel();
        if (level == null) {
            callback.failure(500, "No level");
            return true;
        }

        Direction facing = Direction.values()[side.reverse().ordinal()];
        StringJoiner resp = new StringJoiner(",", "{\"levels\":[", "]}");

        be.forEachScreenBlocks(side, bp -> {
            BlockState bs = level.getBlockState(bp);
            if (bs.getValue(ScreenBlock.emitting)) {
                resp.add("0");
            } else {
                resp.add("" + level.getSignal(bp, facing));
            }
        });

        callback.success(resp.toString());
        return true;
    }
}
