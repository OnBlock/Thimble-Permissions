import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.impl.SyntaxError;
import io.github.cottonmc.jankson.JanksonFactory;
import io.github.indicode.fabric.permissions.Permission;
import io.github.indicode.fabric.permissions.PermissionMap;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;
import io.github.indicode.fabric.tinyconfig.JanksonLoader;

import java.util.UUID;

/**
 * @author Indigo Amann
 */
public class PermissionReadTest {
    public static void main(String[] args) {
        try {
            DefaultedJsonObject json = DefaultedJsonObject.of(JanksonFactory.createJankson().load("{\n" +
                    "            \"minecraft\": {\n" +
                    "                \"command\": {\n" +
                    "                    \"fly\": null,\n" +
                    "                    \"echest\": null,\n" +
                    "                    \"bench\": null,\n" +
                    "                    \"potato\": null,\n" +
                    "                    \"heal\": null,\n" +
                    "                    \"feed\": null\n" +
                    "                }\n" +
                    "            },\n" +
                    "            \"group\": {\n" +
                    "                \"donator\": {\n" +
                    "                    \"best\": {\n" +
                    "                        \"inherits\": [\n" +
                    "                            \"group.donator.better\",\n" +
                    "                            \"minecraft.command.fly\",\n" +
                    "                            \"minecract.command.heal\",\n" +
                    "                            \"minecraft.command.feed\"\n" +
                    "                        ]\n" +
                    "                    },\n" +
                    "                    \"better\": {\n" +
                    "                        \"inherits\": [\n" +
                    "                            \"group.donator.worst\",\n" +
                    "                            \"minecraft.command.potato\"\n" +
                    "                        ]\n" +
                    "                    }\n" +
                    "                    \"worst\": {\n" +
                    "                        \"inherits\": [\n" +
                    "                            \"minecraft.command.echest\",\n" +
                    "                            \"minecraft.command.bench\",\n" +
                    "                        ]\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            }\n" +
                    "            }"));
            PermissionMap readMap = new PermissionMap();
            readMap.permissionsFromJson(json);
            System.out.println(readMap.getPermission("group.donator.best"));
            System.out.println(readMap.getPermission("group.donator.bestistist"));
            System.out.println(readMap.getPermission("newtree.foo.bar"));
            System.out.println(readMap);
            System.out.println(readMap.toJson().toJson(JsonGrammar.STRICT));
        } catch (SyntaxError syntaxError) {
            syntaxError.printStackTrace();
        }

    }
}
