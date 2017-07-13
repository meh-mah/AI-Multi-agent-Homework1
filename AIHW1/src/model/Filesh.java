/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import model.Artifact;

/**
 *
 * @author M&M
 */
public class Filesh {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        Artifact a= new Artifact("name3", "creator3", "descrption3", "style3", 31, 50, "museum1");
        try {
            FileOutputStream fileOut= new FileOutputStream("Artifact3.ser");
            ObjectOutputStream out= new ObjectOutputStream(fileOut);
            out.writeObject(a);
            out.close();
            fileOut.close();
        }
        catch (IOException i){
            
        }
    }
}
