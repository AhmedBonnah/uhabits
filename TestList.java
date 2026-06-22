import java.util.ArrayList;
import java.util.List;

public class TestList {
    static class Habit {
        Long id;
        Long groupId;
        String name;

        Habit(Long id, Long groupId, String name) {
            this.id = id;
            this.groupId = groupId;
            this.name = name;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Habit)) return false;
            Habit h = (Habit) other;
            if (id != null ? !id.equals(h.id) : h.id != null) return false;
            if (groupId != null ? !groupId.equals(h.groupId) : h.groupId != null) return false;
            return name != null ? name.equals(h.name) : h.name == null;
        }
    }

    public static void main(String[] args) {
        Habit h1 = new Habit(1L, null, "Test");
        List<Habit> list = new ArrayList<>();
        list.add(h1);
        
        System.out.println("List contains h1 initially: " + list.contains(h1));
        
        h1.groupId = 5L;
        
        System.out.println("List contains h1 after mutation: " + list.contains(h1));
        
        boolean removed = list.remove(h1);
        
        System.out.println("Removed: " + removed + ", list size: " + list.size());
    }
}
