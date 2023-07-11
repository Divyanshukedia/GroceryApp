package com.app.fresy.model;

import java.io.Serializable;
import java.util.List;

public class Variation implements Serializable {

    public Long id;
    public Product parent;
    // price
    public String price;
    public String sale_price;
    public String regular_price;

    // stock
    public Boolean manage_stock;
    public String stock_status;
    public Long stock_quantity;

    // others
    public Image image;
    public List<Attribute> attributes;

    public Product getProduct() {
        Product p = new Product();
        p.id = id;
        p.parent_id_ = parent.id;
        p.name = parent.name;

        for (Attribute a : attributes) {
            p.short_description = p.short_description + a.name + " : " + a.option + ", ";
        }
        if (p.short_description.endsWith(", ")) {
            p.short_description = p.short_description.substring(0, p.short_description.length() - 2);
        }

        // price
        p.price = price;
        p.sale_price = sale_price;
        p.regular_price = regular_price;

        // stock
        p.manage_stock = manage_stock;
        p.stock_status = stock_status;
        p.stock_quantity = stock_quantity;

        p.images = parent.images;

        return p;
    }

}
