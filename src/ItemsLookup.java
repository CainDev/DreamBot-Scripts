import java.util.HashMap;
import java.util.Map;

public enum ItemsLookup {
    COINS(995, "Coins"),
    BOW_STRING(1777, "Bow string"),

    YEW_LOGS_UNNOTED(1515, "Yew logs"),

    YEW_LONGBOW(855, "Yew longbow"),
    YEW_LONGBOW_NOTED(856, "Yew longbow");

    private final int id;
    private final String name;

    ItemsLookup(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName(){
        return name;
    }

    private static final Map<Integer, ItemsLookup> BY_ID = new HashMap<>();

    static {
        for (ItemsLookup d : values()) {
            BY_ID.put(d.id, d);
        }
    }

    public static ItemsLookup fromId(int id) {
        return BY_ID.get(id);
    }
}