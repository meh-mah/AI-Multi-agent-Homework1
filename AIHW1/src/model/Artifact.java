package model;


import java.io.Serializable;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author M&M
 */
public class Artifact implements Serializable {
    //private static long idCounter = 1;
   // private long id;
    private String name;
    private String creator;
    private String description;
    private String style;
    private int minAge;
    private int maxAge;
    private String museum;

    public Artifact(String name, String creator, String description, String style, int minAge, int maxAge, String museum) {
        //this.id = idCounter++;
        this.name = name;
        this.creator = creator;
        this.description = description;
        this.style = style;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.museum = museum;
    }

    public String getCreator() {
        return creator;
    }

    public String getDescription() {
        return description;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public int getMinAge() {
        return minAge;
    }

    public String getMuseum() {
        return museum;
    }

    public String getName() {
        return name;
    }

    public String getStyle() {
        return style;
    }

//    public long getId() {
//        return id;
//    }
}