package com.app.fresy.model;

import java.io.Serializable;

public class Attribute implements Serializable {
    public Long id;
    public String name;
    public int position;
    public boolean visible;
    public boolean variation;
    public String[] options;
    public String option;
}
