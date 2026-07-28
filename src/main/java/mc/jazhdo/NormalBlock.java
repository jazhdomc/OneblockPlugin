package mc.jazhdo;

public class NormalBlock extends OneblockBlock {
    private final int id;
    private final byte data;

    public NormalBlock(int id, byte data) {
        super(Types.BLOCK);
        
        this.id = id;
        this.data = data;
    }

    public byte getData() {
        return data;
    }

    public int getId() {
        return id;
    }
}
