package com.paanini.jiffy.vfs.schema;

import com.paanini.jiffy.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Property;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A class represents definition of the field in avro generated class,
 * Definition includes,
 *      1)What property to read from JackRabbit
 *      2)If property is to be encrypted
 *      3)If migration is necessary
 *<p>
 *     FieldDef are meta class, information from this class is used for reading
 *     or writing fields from java classes to Jackrabbit stores
 *</p>
 * Examples :
 *   1) Simple property:
 *     {name: "color"}
 *   2) JCR inbuild property
 *      {name: "color", alias: ["@JCR_TITLE"]}
 *   3) Nested property
 *      {name: "color", alias: ["@node:@JCR_CONTENT/color"]}
 *   3) Nested JCR property
 *      {name: "color", alias: ["@node:@JCR_CONTENT/file/@JCR_CONTENT/color"]}
 *   4) Encrypted property
 *      {name: "color", alias:["@node:@JCR_CONTENT/@enc(color)"]}
 *   4) Migratable property
 *      {
 *          name: "color",
 *          alias:[
 *              "@node:@JCR_CONTENT/color",
 *              "@migrate(@node:@JCR_CONTENT/old_color, package.class)"
 *          ]
 *      }
 *
 *  @author  Priyanka Bhoir
 *  @date 29/7/19
 */
public class FieldDef {
    private static final String NODE_PREFIX = "@node:";
    private static final String MIGRATE_PREFIX = "@migrate";
    private static final String SER_DES_PREFIX = "@serdes";
    private static final String ENC_PREFIX = "@enc";
    private static final String ENCRYPT = "@encrypt";
    private static final String NODE_A_Z_A_Z = "^(@node:(@?[a-zA-Z-_]+\\/)+)";
    private static final String ENCRYPTION_REGX = NODE_A_Z_A_Z + "*@enc\\" +
            "(@?[a-zA-Z\\.-_]+\\)$";
    private static final String MIGRATE_REGX = NODE_A_Z_A_Z + "*@migrate\\" +
            "(@?[a-zA-Z-_]+,[a-zA-Z0-9\\.]+\\)$";
    private static final String SERDES_REGX = NODE_A_Z_A_Z + "*@serdes\\" +
            "(@?[a-zA-Z-_]+,@?[a-zA-Z-_]+\\)$";


    private final boolean encrypted;
    private final boolean encryptedV2;
    private final boolean migrationRequired;
    private final boolean serealizable;
    private String name;
    private String originalProp;
    private List<String> path;
    private static Logger logger = LoggerFactory.getLogger(FieldDef.class);
    private Optional<String> versionProp;
    private Optional<String> encryptionClass;
    private Optional<String> encryptionKeyProp;


    private FieldDef(String name) {
        String encryptionRegx = ENCRYPTION_REGX;
        String migrateRegx = MIGRATE_REGX;

        String serdesRegx = SERDES_REGX;



        this.encrypted = name.matches(encryptionRegx);
        this.encryptedV2 = name.contains(ENCRYPT);
        this.migrationRequired = name.matches(migrateRegx);
        this.serealizable = name.matches(serdesRegx);
        this.path = simplifyNodeNames(name);
        this.originalProp = name;
        this.name = simplifyPropertyName(name);

    }

    /**
     * Remove all Prefixes, annotation and return property name
     * @param propName
     * @return simplified property name
     */
    private String simplifyPropertyName(String propName) {
        if(propName.startsWith("@")){
            if(propName.startsWith(NODE_PREFIX)) {
                //take only property part
                propName = propName.substring(propName.lastIndexOf("/") + 1);
            }
            propName = replaceMigrate(propName);
            propName = replaceSerDes(propName);

            propName = replaceEncPrefix(propName)
                    .replaceAll("@", "");

            try {
                return Property.class.getField(propName)
                        .get(propName)
                        .toString();
            } catch (IllegalAccessException | NoSuchFieldException e) {
                logger.debug("Property is not available in Jackrabbit {}", propName);
            }

        }
        return propName;
    }

    private String replaceEncPrefix(String propName) {
        if(!isEncrypted() && !isEncryptedV2()) return propName;
        if(this.encryptedV2){
            String prop = propName.replace(ENCRYPT+"(","")
                            .replace(")","");
            String[] split = prop.split(",");
            //this.encryptionClass = Optional.ofNullable(split[0]);
            this.encryptionKeyProp = Optional.ofNullable(split[0]);
            return split[1];
        }
        return propName.replace(ENC_PREFIX + "(", "")
                .replace(")", "");
    }

    private String replaceMigrate(String propName) {
        if(!migrationRequired || !propName.startsWith(MIGRATE_PREFIX)) {
            return propName;
        }

        String prop = propName.replace(MIGRATE_PREFIX+ "(", "");

        propName = prop.substring(0, prop.indexOf(","));
        return propName;
    }

    private String replaceSerDes(String propName) {
        if(!serealizable || !propName.startsWith(SER_DES_PREFIX)) {
            return propName;
        }

        String prop = propName.replace(SER_DES_PREFIX+ "(", "");

        propName = prop.substring(0, prop.indexOf(","));
        this.versionProp = Optional.of(
                prop.substring(prop.indexOf(",") + 1, prop.length() - 1));
        return propName;
    }

    private List<String> simplifyNodeNames(String propName) {
        List<String> path = new ArrayList<>();
        if(!propName.startsWith(NODE_PREFIX)) return path;

        String[] nodeNames = getNodeNames(propName);

        for(String nodeName : nodeNames) {
            path.add(simplifyPropertyName(nodeName));
        }

        return path;
    }

    private String[] getNodeNames(String propName) {
        final String propPath = propName
                .substring(propName.indexOf(NODE_PREFIX) + NODE_PREFIX.length());

        return propPath
                .substring(0, propPath.lastIndexOf("/"))
                .split("/");
    }

    public String getName() {
        return name;
    }

    public List<String> getPath() {
        return path;
    }

    public boolean isEncrypted(){
        return encrypted;
    }


    public boolean isMigrationRequired(){
        return migrationRequired;
    }

    public boolean isSerealizable(){
        if(serealizable && !versionProp.isPresent()) {
                throw new ProcessingException("Serializable property should " +
                        "have a schema version");
        }

        return serealizable;
    }

    public String getSerializationVersion(){
        if(serealizable) return versionProp.get();
        throw new ProcessingException("Only Serializable property can have " +
                "version");
    }

    public String getMigrationClass(){
        return originalProp.substring(
                        originalProp.lastIndexOf(",") + 1,
                originalProp.length() - 1);
    }

    public static class FieldDefBuilder {
         static String validPropRegx = "^(@node:(@?[a-zA-Z-_]+/)+)?" +
                "(@migrate\\(@?[a-zA-Z-_]+,[a-zA-Z0-9\\\\.]+\\)" +
                "|@serdes\\(@?[a-zA-Z-_]+,[a-zA-Z0-9\\\\._-]+\\)" +
                "|@enc\\(@?[a-zA-Z\\\\.-_]+\\)" +
                "|@?[A-Za-z\\\\._-]*)$";

        public static FieldDef from(String property){
            if(property.matches(validPropRegx) || property.contains("encrypt")) {
                return new FieldDef(property);
            }

            throw new ProcessingException("Invalid property :" + property);
        }


    }

    public boolean isEncryptedV2() {
        return encryptedV2;
    }

    public Optional<String> getEncryptionClass() {
        return encryptionClass;
    }

    public Optional<String> getEncryptionKeyProp() {
        return encryptionKeyProp;
    }



}
