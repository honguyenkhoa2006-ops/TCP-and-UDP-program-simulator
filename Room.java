import java.util.concurrent.ConcurrentHashMap;

public class Room {
    private String roomName;
    private ConcurrentHashMap<Integer, String> members; // clientId -> username

    public Room(String roomName) {
        this.roomName = roomName;
        this.members = new ConcurrentHashMap<>();
    }

    public String getRoomName() {
        return roomName;
    }

    public void addMember(int clientId, String username) {
        members.put(clientId, username);
    }

    public void removeMember(int clientId) {
        members.remove(clientId);
    }

    public boolean isMember(int clientId) {
        return members.containsKey(clientId);
    }

    public ConcurrentHashMap<Integer, String> getMembers() {
        return members;
    }

    public int getMemberCount() {
        return members.size();
    }
}
