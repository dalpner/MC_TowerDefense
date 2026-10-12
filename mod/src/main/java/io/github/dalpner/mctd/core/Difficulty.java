package io.github.dalpner.mctd.core;

public enum Difficulty {
    EASY("easy", 0.75, 50, 220),
    NORMAL("normal", 1.1, 30, 160),
    HARD("hard", 1.4, 20, 150);

    private final String id;
    private final double hpMultiplier;
    private final int lives;
    private final int startGold;

    Difficulty(String id, double hpMultiplier, int lives, int startGold) {
        this.id = id;
        this.hpMultiplier = hpMultiplier;
        this.lives = lives;
        this.startGold = startGold;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "mctd.difficulty." + id;
    }

    public double hpMultiplier() {
        return hpMultiplier;
    }

    public int lives() {
        return lives;
    }

    public int startGold() {
        return startGold;
    }

    public static Difficulty byId(String id) {
        for (Difficulty d : values()) {
            if (d.id.equalsIgnoreCase(id)) {
                return d;
            }
        }
        return null;
    }
}
