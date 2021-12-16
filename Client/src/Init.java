import java.io.Serializable;
import java.security.PublicKey;

public class Init implements Serializable {

    private String key;
    private int index;
    private String hashedTag;
    private byte[] salt;

    public Init(String k,int index, String hashedTag, byte[] salt){
        this.key=k;
        this.index=index;
        this.hashedTag=hashedTag;
        this.salt=salt;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getHashedTag() {
        return hashedTag;
    }

    public void setHashedTag(String hashedTag) {
        this.hashedTag = hashedTag;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
