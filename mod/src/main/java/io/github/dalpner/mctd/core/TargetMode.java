package io.github.dalpner.mctd.core;

/** How a tower picks its target among the creeps in range. */
public enum TargetMode {
    FIRST("first"),
    LAST("last"),
    STRONGEST("strongest"),
    WEAKEST("weakest"),
    CLOSEST("closest");

    private final String id;

    TargetMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "mctd.target." + id;
    }

    public TargetMode next() {
        TargetMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static TargetMode byId(String id) {
        for (TargetMode m : values()) {
            if (m.id.equals(id)) {
                return m;
            }
        }
        return FIRST;
    }
}
