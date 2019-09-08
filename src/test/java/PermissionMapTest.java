import blue.endless.jankson.JsonGrammar;
import io.github.indicode.fabric.permissions.Permission;
import io.github.indicode.fabric.permissions.PermissionMap;
import io.github.indicode.fabric.permissions.PlayerPermissionManager;

import java.util.HashMap;
import java.util.UUID;

/**
 * @author Indigo Amann
 */
public class PermissionMapTest {
    public static void main(String[] args) {
        PermissionMap map = new PermissionMap();
        Permission parent = new Permission("parent");
        Permission child1 = new Permission("child1", parent);
        Permission child2 = new Permission("child2", parent);

        Permission child1_a = new Permission("perm_a", child1);
        Permission child1_b = new Permission("perm_b", child1);
        Permission child2_z = new Permission("perm_z", child2).inherit(child1_a);
        Permission child2_x = new Permission("perm_x", child2_z).inherit(child1_a);

        Permission child1_a_subperm_d = new Permission("subperm_d", child1_a);
        Permission child1_b_subperm_e = new Permission("subperm_e", child1_a);
        //map.getPlayer(UUID.randomUUID()).permission(child1_a_subperm_d).permission(child2_z);
        //map.getPlayer(UUID.randomUUID()).permission(child1_b).permission(child2);
        //map.getPlayer(UUID.randomUUID()).permission(child1_a_subperm_d).permission(child1);
        map.addGroup(parent);
        System.out.println(map.permissionsToJson().toJson(JsonGrammar.STRICT));
        PermissionMap readMap = new PermissionMap();
        readMap.permissionsFromJson(map.permissionsToJson());
        System.out.println(readMap);
        System.out.println(readMap.permissionsToJson().toJson(JsonGrammar.STRICT));
        PermissionMap readMap2 = new PermissionMap();
        readMap2.permissionsFromJson(readMap.permissionsToJson());
        System.out.println(readMap2);
        System.out.println(readMap2.permissionsToJson().toJson(JsonGrammar.STRICT));
        PermissionMap readMap3 = new PermissionMap();
        readMap3.permissionsFromJson(readMap2.permissionsToJson());
        System.out.println(readMap3);
        System.out.println(readMap3.permissionsToJson().toJson(JsonGrammar.STRICT));
        PermissionMap readMap4 = new PermissionMap();
        readMap4.permissionsFromJson(readMap3.permissionsToJson());
        System.out.println(readMap4);
        System.out.println(readMap4.permissionsToJson().toJson(JsonGrammar.STRICT));
        PermissionMap readMap5 = new PermissionMap();
        readMap5.permissionsFromJson(readMap4.permissionsToJson());
        System.out.println(readMap5);
        System.out.println(readMap5.permissionsToJson().toJson(JsonGrammar.STRICT));
        PermissionMap readMap6 = new PermissionMap();
        readMap6.permissionsFromJson(readMap5.permissionsToJson());
        System.out.println(readMap6);
        System.out.println(readMap6.permissionsToJson().toJson(JsonGrammar.STRICT));
    }
}
