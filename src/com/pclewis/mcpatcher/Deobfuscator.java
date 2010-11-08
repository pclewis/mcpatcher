package com.pclewis.mcpatcher;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Manage mappings between obfuscated names and friendlier ones. Several 'friendly' names may refer to the same
 * obfuscated name.
 */
public class Deobfuscator {

    /**
     * A mapping between an obfuscated name and a list of friendly names.
     */
    private static class ObfuscatedItem {
        private String obfuscatedName;
        private List<String> friendlyNames = new LinkedList<String>();

        public ObfuscatedItem(String obfuscatedName) {
            this.obfuscatedName = obfuscatedName;
        }

        public void addName(String name) {
            if(!friendlyNames.contains(name))
                friendlyNames.add(name);
        }

        public String getObfuscatedName() {
            return obfuscatedName;
        }

        public List<String> getFriendlyNames() {
            return friendlyNames;
        }
    }

    /**
     * An ObfuscatedItem that additionally contains other ObfuscatedItems. Fields and methods are treated the same
     * because they have different type descriptors to identify them.
     */
    private static class ObfuscatedClass extends ObfuscatedItem {
        private Map<String, ObfuscatedItem> members = new HashMap<String, ObfuscatedItem>();

        public ObfuscatedClass(String name) {
            super(name);
        }

        private String memberName(String name, String type) {
            return name + ":" + type;
        }

        public void addMember(String name, String type) {
            members.put(memberName(name, type), new ObfuscatedItem(name));
        }

        public ObfuscatedItem getMember(String name, String type) {
            ObfuscatedItem item = members.get(memberName(name, type));
            if(item == null) {
                throw new IllegalArgumentException("No such member: " + memberName(name, type));
            }
            return item;
        }

        public void addMemberName(String name, String type, String newName) {
            ObfuscatedItem item = getMember(name, type);

            if(members.containsKey(memberName(newName, type))) {
                throw new IllegalArgumentException("Already existing member: " + memberName(name, type));
            }

            members.put(memberName(newName, type), item);
            item.addName(newName);
        }
    }

    private Version minecraftVersion;
    private Map<String, ObfuscatedClass> classes = new HashMap<String, ObfuscatedClass>();

    public Deobfuscator(Version minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    public void addClass(String name) {
        classes.put(name, new ObfuscatedClass(name));
    }

    public void addClassName(String name, String newName) {
        ObfuscatedClass oc = getClass(name);
        if(classes.containsKey(newName) && classes.get(newName) != oc) {
            throw new IllegalArgumentException("There is already a class named '" + newName +
                    "' associated with '" + classes.get(newName).getObfuscatedName() + "'");
        }
        classes.put(newName, oc);
        oc.addName(newName);
    }

    /**
     * Add a field or method to a class.
     *
     * @param className   Obfuscated or friendly class name.
     * @param memberName  New obfuscated member name.
     * @param memberType  New member type descriptor.
     */
    public void addMember(String className, String memberName, String memberType) {
        getClass(className).addMember(memberName, memberType);
    }

    /**
     * Add a friendly name to a field or method on a class.
     * 
     * @param className  Obfuscated or friendly class name.
     * @param memberName Obfuscated or friendly member name.
     * @param memberType Member type descriptor.
     * @param newName    New friendly name to associate with member.
     */
    public void addMemberName(String className, String memberName, String memberType, String newName) {
        getClass(className).addMemberName(memberName,memberType,newName);
    }

    public String getClassName(String name) {
        return getClass(name).getObfuscatedName();
    }

    public List<String> getClassFriendlyNames(String name) {
        return getClass(name).getFriendlyNames();
    }

    public String getMemberName(String className, String memberName, String memberType) {
        return getClass(className).getMember(memberName, memberType).getObfuscatedName();
    }

    public Version getMinecraftVersion() {
        return minecraftVersion;
    }

    private ObfuscatedClass getClass(String name) {
        ObfuscatedClass oc = classes.get(name);
        if(oc == null) {
            throw new IllegalArgumentException("No class named: " +  name);
        }
        return oc;
    }
}
