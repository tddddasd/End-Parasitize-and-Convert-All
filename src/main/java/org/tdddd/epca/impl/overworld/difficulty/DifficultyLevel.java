package org.tdddd.epca.impl.overworld.difficulty;

public enum DifficultyLevel {
    EASY("easy", 0),
    NORMAL("normal", 1),
    EXPERT("expert", 2),
    MASTER("master", 3),
    CUSTOM("custom", 4),
    LEGENDARY("legendary", 5);

    private final String name;
    private final int id;

    DifficultyLevel(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() { return name; }
    public int getId() { return id; }

    public DifficultyLevel next() {
        DifficultyLevel[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static DifficultyLevel fromId(int id) {
        for (DifficultyLevel level : values()) {
            if (level.id == id) return level;
        }
        return NORMAL;
    }
}