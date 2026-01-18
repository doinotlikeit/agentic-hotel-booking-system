package com.hotel.booking.util;

import java.util.*;

/**
 * Utility class for building A2UI (Agent-to-UI) metadata
 * All agent responses should be formatted using A2UI components
 */
public class A2UIBuilder {
    
    private final List<Map<String, Object>> components;
    
    private A2UIBuilder() {
        this.components = new ArrayList<>();
    }
    
    public static A2UIBuilder create() {
        return new A2UIBuilder();
    }
    
    /**
     * Add a Card component
     */
    public A2UIBuilder addCard(String title, String subtitle, String content) {
        Map<String, Object> card = new HashMap<>();
        card.put("type", "card");
        card.put("title", title);
        if (subtitle != null) {
            card.put("subtitle", subtitle);
        }
        card.put("content", content);
        components.add(card);
        return this;
    }
    
    /**
     * Add a Text component
     */
    public A2UIBuilder addText(String content, String variant, String align) {
        Map<String, Object> text = new HashMap<>();
        text.put("type", "text");
        text.put("content", content);
        text.put("variant", variant != null ? variant : "body");
        text.put("align", align != null ? align : "left");
        components.add(text);
        return this;
    }
    
    /**
     * Add a heading text
     */
    public A2UIBuilder addHeading(String content) {
        return addText(content, "heading", "left");
    }
    
    /**
     * Add a subheading text
     */
    public A2UIBuilder addSubheading(String content) {
        return addText(content, "subheading", "left");
    }
    
    /**
     * Add body text
     */
    public A2UIBuilder addBody(String content) {
        return addText(content, "body", "left");
    }
    
    /**
     * Add caption text
     */
    public A2UIBuilder addCaption(String content) {
        return addText(content, "caption", "left");
    }
    
    /**
     * Add a Grid component
     */
    public A2UIBuilder addGrid(int columns, List<Map<String, Object>> items) {
        Map<String, Object> grid = new HashMap<>();
        grid.put("type", "grid");
        grid.put("columns", columns);
        grid.put("items", items);
        components.add(grid);
        return this;
    }
    
    /**
     * Add a Button component
     */
    public A2UIBuilder addButton(String label, String action, String variant) {
        Map<String, Object> button = new HashMap<>();
        button.put("type", "button");
        button.put("label", label);
        button.put("action", action);
        button.put("variant", variant != null ? variant : "primary");
        components.add(button);
        return this;
    }
    
    /**
     * Add a list component
     */
    public A2UIBuilder addList(List<String> items, boolean ordered) {
        Map<String, Object> list = new HashMap<>();
        list.put("type", "list");
        list.put("items", items);
        list.put("ordered", ordered);
        components.add(list);
        return this;
    }
    
    /**
     * Add a divider
     */
    public A2UIBuilder addDivider() {
        Map<String, Object> divider = new HashMap<>();
        divider.put("type", "divider");
        components.add(divider);
        return this;
    }
    
    /**
     * Add raw JSON data display
     */
    public A2UIBuilder addJsonData(String label, Object data) {
        Map<String, Object> jsonDisplay = new HashMap<>();
        jsonDisplay.put("type", "json");
        jsonDisplay.put("label", label);
        jsonDisplay.put("data", data);
        components.add(jsonDisplay);
        return this;
    }
    
    /**
     * Add a status message with icon
     */
    public A2UIBuilder addStatus(String message, String status) {
        Map<String, Object> statusMsg = new HashMap<>();
        statusMsg.put("type", "status");
        statusMsg.put("message", message);
        statusMsg.put("status", status); // success, error, warning, info
        components.add(statusMsg);
        return this;
    }
    
    /**
     * Build the final A2UI metadata structure
     */
    public Map<String, Object> build() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("format", "a2ui");
        metadata.put("version", "1.0");
        metadata.put("components", components);
        return metadata;
    }
    
    /**
     * Build and convert to JSON string
     */
    public String buildAsString() {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(build());
        } catch (Exception e) {
            return "{\"error\": \"Failed to serialize A2UI metadata\"}";
        }
    }
}
