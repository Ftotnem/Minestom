package nub.wi1helm.server;

public enum ServerTeam {
    AQUA_CREEPERS,
    PURPLE_SWORDERS,
    UNKNOWN;

    public static ServerTeam fromString(String teamName) {
        if (teamName == null) return UNKNOWN;

        if (teamName.equalsIgnoreCase("AQUA_CREEPERS")){
            return AQUA_CREEPERS;
        }
        if (teamName.equalsIgnoreCase("PURPLE_SWORDERS")) {
            return PURPLE_SWORDERS;
        }
        return UNKNOWN;
    }
}