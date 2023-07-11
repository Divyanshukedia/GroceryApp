package com.app.fresy.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Product implements Serializable {

    public Long id;
    public Long parent_id_ = -1L;
    public String name;
    public String short_description = "";
    public String description;
    public String type;

    // price
    public String price;
    public String price_html;
    public String sale_price;
    public String regular_price;

    // stock
    public Boolean manage_stock;
    public String stock_status;
    public Long stock_quantity;

    // others
    public List<Image> images;
    public List<Long> related_ids;

    public List<Attribute> attributes;

    public Map<String, Variation> variationsMap = new HashMap<>();

}
