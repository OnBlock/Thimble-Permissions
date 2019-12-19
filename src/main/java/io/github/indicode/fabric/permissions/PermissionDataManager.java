package io.github.indicode.fabric.permissions;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PermissionDataManager {
    protected Date expiry = null;
    protected HashMap<String, String> data = new HashMap<>();

    public Date getExpiryDate() {
        return expiry;
    }
    public void setExpiryDate(Date expiry) {
        this.expiry = expiry;
    }
    public boolean isExpired() {
        return getExpiryDate() != null && getExpiryDate().after(new Date());
    }

    public void clearData() {
        data.clear();
    }
    public void setData(String key, String value) {
        if (data != null) data.put(key, value);
        else removeData(key);
    }
    public String getData(String key) {
        if (data != null) return data.get(key);
        else return null;
    }
    public boolean dataExists(String key) {
        return data.containsKey(key);
    }
    public void removeData(String key) {
        data.remove(key);
    }
    public HashMap<String, String> getRawDataMap() {
        return data;
    }

    public void toNBT(CompoundTag tag) {
        if (expiry != null) {
            tag.putLong("expires", expiry.getTime());
        }
        if (!data.isEmpty()) {
            CompoundTag dataTag = new CompoundTag();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (entry.getValue() != null) dataTag.putString(entry.getKey(), entry.getValue());
            }
            tag.put("data", dataTag);
        }
    }
    public void fromTag(CompoundTag tag) {
        if (tag.contains("expires")) {
            expiry = new Date(tag.getLong("expires"));
        }
        if (tag.contains("data")) {
            CompoundTag dataTag = (CompoundTag) tag.get("data");
            for (String key : dataTag.getKeys()) {
                String value = dataTag.getString(key);
                if (value != null) {
                    data.put(key, value);
                }
            }
        }
    }
    public void fromJson(JsonObject json) {
        if (json.containsKey("data")) {
            JsonElement dataE = json.get("data");
            if (dataE instanceof JsonObject) {
                JsonObject dataO = (JsonObject) dataE;
                for (Map.Entry<String, JsonElement> entry : dataO.entrySet()) {
                    if (entry.getValue() instanceof JsonPrimitive) {
                        data.put(entry.getKey(), ((JsonPrimitive) entry.getValue()).asString());
                    }
                }
            }
        }
    }
}
