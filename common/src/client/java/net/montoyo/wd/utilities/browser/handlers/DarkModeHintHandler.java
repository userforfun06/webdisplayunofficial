package net.montoyo.wd.utilities.browser.handlers;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.handler.CefResourceRequestHandlerAdapter;
import org.cef.misc.BoolRef;
import org.cef.network.CefRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * CEF never sends the Sec-CH-Prefers-Color-Scheme client hint, even when a site opts in with
 * Accept-CH. Sites that pick their theme server-side (Google's homepage, for example) then serve
 * their light variant on the first navigation and only learn the preference from a cookie on the
 * next one. CEF's command line already forces dark mode, so advertise that preference on the very
 * first main-frame request as well.
 */
public class DarkModeHintHandler extends CefRequestHandlerAdapter {

    public static final DarkModeHintHandler INSTANCE = new DarkModeHintHandler();

    private static final String HINT = "Sec-CH-Prefers-Color-Scheme";

    private static final CefResourceRequestHandler RESOURCE_HANDLER =
            new CefResourceRequestHandlerAdapter() {
                @Override
                public boolean onBeforeResourceLoad(CefBrowser browser, CefFrame frame, CefRequest request) {
                    Map<String, String> headers = new HashMap<>();
                    request.getHeaderMap(headers);

                    for (String name : headers.keySet())
                        if (name.equalsIgnoreCase(HINT))
                            return false;

                    headers.put(HINT, "dark");
                    request.setHeaderMap(headers);
                    return false;
                }
            };

    @Override
    public CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser, CefFrame frame,
                                                               CefRequest request, boolean isNavigation,
                                                               boolean isDownload, String requestInitiator,
                                                               BoolRef disableDefaultHandling) {
        if (request == null || request.getResourceType() != CefRequest.ResourceType.RT_MAIN_FRAME)
            return null;

        String url = request.getURL();
        if (url == null || !url.startsWith("https://"))
            return null;

        return RESOURCE_HANDLER;
    }
}
