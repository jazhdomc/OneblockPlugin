package mc.jazhdo;

public abstract class OneblockBlock {
    protected enum Types {BLOCK, CHEST, LOOT};
    protected Types type;

    public OneblockBlock(Types type) {
        this.type = type;
    }

    public Types getType() {
        return type;
    }
}
