package nub.wi1helm.server;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum ServerTeam {
    AQUA_CREEPERS("Aqua Creepers", TextColor.fromHexString("#17abac")),
    PURPLE_SWORDERS("Purple Sworders", TextColor.fromHexString("#B250E1"));


    private final String displayName;
    private final TextColor color;
    private final Component component;


    ServerTeam(String displayName, TextColor color) {
        this.displayName = displayName;
        this.color = color;
        this.component = Component.text(displayName, color);
    }

    public String displayName() {
        return displayName;
    }

    public TextColor color() {
        return color;
    }

    public Component component() {
        return component;
    }

    public static ServerTeam fromString(String teamName) {
        if (teamName == null) return null;

        if (teamName.equalsIgnoreCase("AQUA_CREEPERS")){
            return AQUA_CREEPERS;
        }
        if (teamName.equalsIgnoreCase("PURPLE_SWORDERS")) {
            return PURPLE_SWORDERS;
        }
        return null;
    }
}