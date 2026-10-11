package com.example.xclient;

public class Setting {
    public final String name;
    public final double min, max, step;
    public double value;

    public Setting(String name, double min, double max, double step, double value) {
        this.name = name; this.min = min; this.max = max; this.step = step; this.value = value;
    }
}
