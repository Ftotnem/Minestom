package nub.wi1helm.server;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;

public enum ServerTeam {
    AQUA_CREEPERS("Aqua Creepers", TextColor.fromHexString("#17abac"), new Pos(-24.0,-60,8.0,-90,0), new PlayerSkin("ewogICJ0aW1lc3RhbXAiIDogMTYzMDQxNjQyNjY0NSwKICAicHJvZmlsZUlkIiA6ICJiNzQ3OWJhZTI5YzQ0YjIzYmE1NjI4MzM3OGYwZTNjNiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTeWxlZXgiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjQ2ZjFiOTllODE2NzY5NjAzNTMxMmI4N2I3NWU3YjkxMzc1M2U4ODZhMTI1OWM2MWYwY2NjZjVlOWQ2OTlhNyIKICAgIH0KICB9Cn0=","jwLbu2f8CUaHVda/fOdOJsjWbaqAaAITHYNBVzrOC7RGrYyuCp/jSoe1dyQHt3MQC9RHKvLhkV2f8ga2xIdh4C0nHAoQb4rnrjYViZCLOnm1Kmff/dGUAdM3bFXKBmhwUenHlp668PJBDcCzrmfDFnkG9upiK4IatGPKAjjY/GqY4QbwdLATwBAh1Ldu7wS13oc3OwnBF5abo9G5AMiRMGJjmLGZia+bE1hl+mORqWOaozZ5/NDcgLscqMp7GV2RU02z/rkErA4DOU+izUbj7jDmF84KJYS0GLEIQcBnc2LRwWfURWfUXUVpBmC+IJJv28aAbQUD0SH6n/cfONV6Z0J3jJHURrpkPw9rFUWisJfzwaUwm2Uhy3BpeVKTHGNE55UMChW2jHPKkAKVD3IbNiV4iHCjgYcRQ72HoKI22mbbiMNBK8Oa4IdffFnMYC2Oc9X0xrFKWxkf3vjhKgBvUC2OrRJ7SoO+J2A1X5raaz1gwPCC8/MN5Mpm/x+2r0h3Dg2syo0WIMZBBW7QB+rG4tOBvOxEYZ2dXNSeI1x6arzfKO2DsLtfjRyacFmWFitrnnbtAsrQR4XNp9uhnhGG9pVsMmKClOVAYBaSZ8xKvktzBpKz6/O6UsJVrZotreUmT8MGKcJFi/NOUm5cZ3EHZDSp6PZcJuPZkT4k8WZVoJQ=")),
    PURPLE_AXOLOTLS("Purple Axolotls", TextColor.fromHexString("#B250E1"), new Pos(40.0,-60,8.0,90,0), new PlayerSkin("ewogICJ0aW1lc3RhbXAiIDogMTYyNzY2ODU0Njk5OSwKICAicHJvZmlsZUlkIiA6ICI2OTBkMDM2OGM2NTE0OGM5ODZjMzEwN2FjMmRjNjFlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ5emZyXzciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGE1YmNjNGU4N2M1MDkxZWM3ZmQ4MDI3YjQxMWYyOTdiYWFkNmYzY2QwNGNiZWMxYWM4NmY1MDA2NzFmNzU5YSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9","HG+ZyjSvvvph6ZKqLywDOJ+gSmNiIY2WzUdlxr7liYL7vPxxNing3rqk8F1E/KmmogmVAo7d28OacotqY65yYwgdx3rBSNYm3SzgBIy9Net3Md96WMxYbpK1ySTYuWje3M0EiQQUKAms0gkN80Jwrh5dOqC3Q1depIQTc34drE0hc7CUl3wZ86VM0s1HIEAm3a3+ImLlbmnHmxFX99HzYnbveBttZULLwjgaYPaJbcN+YnQAs91N4gQ21nqAye4Ab5ktsGv6+HW5wGAE7Nqq9Gx44q+70YpFOQQD1Z2uhTefEmLSK2fJQ5Tz0OdljBDCE5oUxosMe1dOJsS/1p64UwKnyAvCBRTUaB3rGyaf/nFUzhQS5TW3VVcSeZVqYKNNuGmJpWH3oKRHqJ3jCAxqwJBF7q7UsjtTYg6Ojrcd9rLNVH/6LlLEcEcPDGLxOyCd2DRSzQjgLnHFYaJmKT68KVVHZFEsRYPVlfilh1iTgvDpywutekLMtpKjB0x9iYzJ5TtQ+DEle3oN5N7ownHFCDIFPdP4snrXaC5GaUMqMgsj+34XaTKYTSqutdq+hzcGg65KSkwu9r/J4Hw3wNYbcmF6g2A3zjz+k+Qzz5e0liAyESW5dUKcTJREBopsuzyMFh4wV1Z5ENBh1xxd4+LkJPlkfmTFfRwwKhTjPBj2n/k=")),
    LIMBO("",TextColor.color(128,128,128), new Pos(0,-60,0), new PlayerSkin("ewogICJ0aW1lc3RhbXAiIDogMTc1MDM3NzU4MzQzMCwKICAicHJvZmlsZUlkIiA6ICI0YWU5MTM5MzZhOGU0MWU0YWNlMTYyYjI4YmM0MzMwMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ6ZXJ2YXRpb24iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmZiZGQ4OGU5ZWY1ZmZmNTJkYjdhOTJhNzI5ZGVmNDI5NWM5MzE1ZDRkMWZlZTlkM2Y3NTQwNTEwZGEwNWRmMCIKICAgIH0KICB9Cn0=", "NWGRVmUlDrFNkqBbsPlFQclPXJeLt8r+1mlWx59CH65vtAgmQaKZCdxh/j/QKn1DvPMUR+dDwxo4i7F4ofZSbu+MGxRKNesj9Z0fKni28fHN8c5PUi6BUQhOjVv8I0q3qXrDTF0gNmGxFdFO9nsVGb0PYz6rXrqNQw4RYvbvEZ0XS6SsisEnRrg5bg2NCo/k08fjn2jRvSjGFZTHEyLOSqBBuLCr31B977zq5APCV32+r8qTCoad94tBd/f1d4jgbTn6dUW7ZU3Ppcr4gtoXcxv01G7jmTvRNR7zc+yDNKTbqqDJeuyPuWPaUjnI6/bQ6eob5XpeIoF5DRieZcHs+mm1qqiM51D2bOuQnjShWxBBYZAxL2EHAPUHQj0nNFawT7YiKh2tX23tNuCjrNZ4jM+aiHMh7gum4h5pVRlnNWR03igz1ZlIyqnKypnoN8NPYD4OXbEzWQFqULIuOxXuYrbPgZqbFoaS8k9r2liUE9jFYZK5B9zG8gt4167m2dvS9SRbTuDP5jmhS3UisjhVq1QOEFG8THhuVMImspCkSbHG5ggfd4qiSWaqKuZXpv7uVkEKNwxl21c1Mx++85cBNR44Ygw0HSZeq7XqHiiLxHSCqxmCplkvUJeCaSrNG7B9HrAbQz64+Tm/8DgGp6rQH/FRKJhvfYSIjPpajwWdI1Y="));


    private final String displayName;
    private final TextColor color;
    private final Component component;
    private final Pos pos;
    private final PlayerSkin skin;

    ServerTeam(String displayName, TextColor color, Pos pos, PlayerSkin skin) {
        this.displayName = displayName;
        this.color = color;
        this.component = Component.text(displayName, color);
        this.pos = pos;
        this.skin = skin;
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

    public Pos getPos() {
        return pos;
    }

    public PlayerSkin getSkin() { return skin;}

    public static ServerTeam fromString(String teamName) {
        if (teamName == null) return LIMBO;

        if (teamName.equalsIgnoreCase("AQUA_CREEPERS")){
            return AQUA_CREEPERS;
        }
        if (teamName.equalsIgnoreCase("PURPLE_AXOLOTLS")) {
            return PURPLE_AXOLOTLS;
        }
        return LIMBO;
    }
}