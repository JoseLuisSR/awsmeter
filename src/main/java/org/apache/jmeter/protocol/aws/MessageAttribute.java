package org.apache.jmeter.protocol.aws;

/**
 * Message Attribute class.
 * @author JoseLuisSR
 * @since 01/27/2021
 * @see "https://github.com/JoseLuisSR/awsmeter"
 */
public class MessageAttribute {

    /**
     * Message attribute name.
     */
    private String name;

    /**
     * Message attribute type.
     */
    private String type;

    /**
     * Message attribute value.
     */
    private String value;

    /**
     * Get Message Attribute Name.
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set Message Attribute Name.
     * @param name
     *        Message Attribute Name.
     */
    public void setName(String name){
        this.name = name;
    }

    /**
     * Get Message Attribute Type.
     * @return type.
     */
    public String getType() {
        return type;
    }

    /**
     * Set Message Attribute Type.
     * @param type
     *        Message Attribute Type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Message Attribute Value.
     * @return value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Message Attribute Value.
     * @param value
     *        Message Attribute Value.
     */
    public void setValue(String value) {
        this.value = value;
    }
}
