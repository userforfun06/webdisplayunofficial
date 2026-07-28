package net.montoyo.wd.utilities.browser.handlers.js;

/**
 * Stub JSField class for JavaScript field handling.
 * This is a placeholder for Fabric 1.21 port.
 */
public class JSField {
    private String name;
    private Object value;
    
    public JSField(String name, Object value) {
        this.name = name;
        this.value = value;
    }
    
    public String getName() {
        return name;
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
}
