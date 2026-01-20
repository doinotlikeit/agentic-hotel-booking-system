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
     * Wrap plain text in A2UI format (for LLM responses)
     * This ensures all content is delivered in A2UI format
     */
    public static Map<String, Object> wrapText(String text) {
        return create()
                .addText(text, "body", "left")
                .build();
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
     * Add an Image component
     */
    public A2UIBuilder addImage(String src, String alt, String caption, Integer width, Integer height) {
        Map<String, Object> image = new HashMap<>();
        image.put("type", "image");
        image.put("src", src);
        if (alt != null) {
            image.put("alt", alt);
        }
        if (caption != null) {
            image.put("caption", caption);
        }
        if (width != null) {
            image.put("width", width);
        }
        if (height != null) {
            image.put("height", height);
        }
        components.add(image);
        return this;
    }

    /**
     * Add an Image Gallery component (grid of images)
     */
    public A2UIBuilder addImageGallery(List<String> imageUrls, String alt, Integer maxImages, Integer columns) {
        Map<String, Object> gallery = new HashMap<>();
        gallery.put("type", "image-gallery");
        gallery.put("images", imageUrls);
        if (alt != null) {
            gallery.put("alt", alt);
        }
        if (maxImages != null) {
            gallery.put("maxImages", maxImages);
        } else {
            gallery.put("maxImages", 6); // Default
        }
        if (columns != null) {
            gallery.put("columns", columns);
        } else {
            gallery.put("columns", 3); // Default
        }
        components.add(gallery);
        return this;
    }

    /**
     * Add an Image Gallery component with just images (using defaults)
     */
    public A2UIBuilder addImageGallery(List<String> imageUrls) {
        return addImageGallery(imageUrls, null, null, null);
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
     * Add JSON with tree view (expandable/collapsible)
     * 
     * @param label     Optional label for the JSON display
     * @param data      The JSON data to display
     * @param mode      Display mode: "raw", "tree", or "both"
     * @param collapsed Whether tree nodes should start collapsed (default true)
     */
    public A2UIBuilder addJsonTree(String label, Object data, String mode, boolean collapsed) {
        Map<String, Object> jsonDisplay = new HashMap<>();
        jsonDisplay.put("type", "json");
        if (label != null) {
            jsonDisplay.put("label", label);
        }
        jsonDisplay.put("data", data);
        jsonDisplay.put("mode", mode != null ? mode : "both"); // raw, tree, or both
        jsonDisplay.put("collapsed", collapsed);
        components.add(jsonDisplay);
        return this;
    }

    /**
     * Add JSON with tree view (collapsed by default, showing both modes)
     */
    public A2UIBuilder addJsonTree(String label, Object data) {
        return addJsonTree(label, data, "both", true);
    }

    /**
     * Add a table component with pagination
     * 
     * @param headers    Column headers
     * @param rows       Table rows (each row is a list of cell values)
     * @param pageSize   Number of rows per page (default 10)
     * @param sortable   Enable column sorting (default true)
     * @param filterable Enable table filtering (default false)
     * @param pagination Enable pagination (default true if rows > pageSize)
     */
    public A2UIBuilder addTable(List<String> headers, List<List<Object>> rows,
            Integer pageSize, Boolean sortable,
            Boolean filterable, Boolean pagination) {
        Map<String, Object> table = new HashMap<>();
        table.put("type", "table");
        table.put("headers", headers);
        table.put("rows", rows);
        if (pageSize != null) {
            table.put("pageSize", pageSize);
        }
        if (sortable != null) {
            table.put("sortable", sortable);
        }
        if (filterable != null) {
            table.put("filterable", filterable);
        }
        if (pagination != null) {
            table.put("pagination", pagination);
        }
        components.add(table);
        return this;
    }

    /**
     * Add a simple table with default settings (10 rows per page, sortable)
     */
    public A2UIBuilder addTable(List<String> headers, List<List<Object>> rows) {
        return addTable(headers, rows, 10, true, false, null);
    }

    /**
     * Add a table with custom page size
     */
    public A2UIBuilder addTable(List<String> headers, List<List<Object>> rows, int pageSize) {
        return addTable(headers, rows, pageSize, true, false, null);
    }

    /**
     * Add a searchable/filterable table
     */
    public A2UIBuilder addFilterableTable(List<String> headers, List<List<Object>> rows, int pageSize) {
        return addTable(headers, rows, pageSize, true, true, null);
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

        // Check if any component is a JSON tree - if so, mark for direct send to UI
        boolean hasJsonTree = components.stream()
                .anyMatch(c -> "json".equals(c.get("type")));
        if (hasJsonTree) {
            metadata.put("__a2ui_direct__", true);
        }

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
