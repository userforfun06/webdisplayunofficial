package net.montoyo.wd.utilities.browser;

import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.browser.handlers.js.queries.GetSizeQuery;
import net.montoyo.wd.utilities.browser.handlers.js.queries.GetUpgradesQuery;
import net.montoyo.wd.utilities.browser.handlers.js.queries.IsOwnerQuery;
import net.montoyo.wd.utilities.browser.handlers.js.queries.GetRotationQuery;
import net.montoyo.wd.utilities.browser.handlers.js.queries.GetSideQuery;
import net.montoyo.wd.utilities.browser.handlers.js.queries.GetRedstoneAtQuery;
import net.montoyo.wd.utilities.browser.handlers.js.queries.GetRedstoneArrayQuery;
import net.montoyo.wd.utilities.browser.handlers.js.queries.IsEmittingQuery;
import net.montoyo.wd.utilities.browser.handlers.js.queries.GetEmissionArrayQuery;
import net.montoyo.wd.utilities.browser.handlers.js.queries.SetRedstoneAtQuery;
import net.montoyo.wd.utilities.browser.handlers.js.queries.ClearRedstoneQuery;
import net.montoyo.wd.utilities.data.BlockSide;

public class InWorldQueries {
    private static final GetSizeQuery getSize = new GetSizeQuery();
    private static final GetUpgradesQuery getUpgrades = new GetUpgradesQuery();
    private static final IsOwnerQuery isOwner = new IsOwnerQuery();
    private static final GetRotationQuery getRotation = new GetRotationQuery();
    private static final GetSideQuery getSide = new GetSideQuery();
    private static final GetRedstoneAtQuery getRedstoneAt = new GetRedstoneAtQuery();
    private static final GetRedstoneArrayQuery getRedstoneArray = new GetRedstoneArrayQuery();
    private static final IsEmittingQuery isEmitting = new IsEmittingQuery();
    private static final GetEmissionArrayQuery getEmissionArray = new GetEmissionArrayQuery();
    private static final SetRedstoneAtQuery setRedstoneAt = new SetRedstoneAtQuery();
    private static final ClearRedstoneQuery clearRedstone = new ClearRedstoneQuery();

    public static void attach(ScreenBlockEntity blockEntity, BlockSide side, WDBrowser browser) {
        browser.setBe(blockEntity, side);
        browser.queryHandlers().put(getSize.getName(), getSize);
        browser.queryHandlers().put(getUpgrades.getName(), getUpgrades);
        browser.queryHandlers().put(isOwner.getName(), isOwner);
        browser.queryHandlers().put(getRotation.getName(), getRotation);
        browser.queryHandlers().put(getSide.getName(), getSide);
        browser.queryHandlers().put(getRedstoneAt.getName(), getRedstoneAt);
        browser.queryHandlers().put(getRedstoneArray.getName(), getRedstoneArray);
        browser.queryHandlers().put(isEmitting.getName(), isEmitting);
        browser.queryHandlers().put(getEmissionArray.getName(), getEmissionArray);
        browser.queryHandlers().put(setRedstoneAt.getName(), setRedstoneAt);
        browser.queryHandlers().put(clearRedstone.getName(), clearRedstone);
    }
}
