package com.library.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Category name is required")
    @Column(unique = true)
    private String name;

    private String description;

    private String color;  // hex color for UI badge

    // Getters
    public Long getId()            { return id; }
    public String getName()        { return name; }
    public String getDescription() { return description; }
    public String getColor()       { return color; }

    // Setters
    public void setId(Long id)                   { this.id = id; }
    public void setName(String name)             { this.name = name; }
    public void setDescription(String desc)      { this.description = desc; }
    public void setColor(String color)           { this.color = color; }
}
