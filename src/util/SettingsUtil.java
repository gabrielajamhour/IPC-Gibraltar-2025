package util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class SettingsUtil {
    private static SettingsUtil instance = new SettingsUtil();

    private BooleanProperty usarColorSolido = new SimpleBooleanProperty(false);

    private SettingsUtil() {}

    public static SettingsUtil getInstance() {
        return instance;
    }

    public BooleanProperty usarColorSolidoProperty() {
        return usarColorSolido;
    }

    public boolean isUsarColorSolido() {
        return usarColorSolido.get();
    }

    public void setUsarColorSolido(boolean valor) {
        usarColorSolido.set(valor);
    }
    
    
}

