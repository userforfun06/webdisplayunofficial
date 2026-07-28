package net.montoyo.wd.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.cinemamod.mcef.CefUtil", remap = false)
public class MCEFDarkModeMixin {

    private static String[] appendDarkModeArgs(String[] args) {
        String[] newArgs = java.util.Arrays.copyOf(args, args.length + 3);
        newArgs[args.length] = "--force-dark-mode";
        newArgs[args.length + 1] = "--enable-features=WebContentsForceDark";
        newArgs[args.length + 2] = "--blink-settings=darkMode=4";
        return newArgs;
    }

    @ModifyVariable(method = "init", at = @At("STORE"), remap = false)
    private static String[] addDarkModeToCefArgs(String[] args) {
        return appendDarkModeArgs(args);
    }
}
