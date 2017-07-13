package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Artifact;


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author M&M
 */
public class CuratorAgent extends Agent {
    private List<Artifact> listOfArtifacts;

    @Override
    protected void setup() {
        
       listOfArtifacts = new ArrayList<>();
       // find all files ends with .ser and save them in a list
               File dir=new File(".");
               List <String> list=Arrays.asList(dir.list(
                       new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".ser");
                    }
               
               }));
               // read all found .ser files into a list of type artifact
               String[] s=(String []) list.toArray();
               for(int i=0; i<s.length; i++){
                    FileInputStream fileIn = null;
                    ObjectInputStream in = null;
                    try {
                        fileIn = new FileInputStream(s[i]);
                        in = new ObjectInputStream(fileIn);
                        listOfArtifacts.add((Artifact)in.readObject());
                    } 
                    
                    catch (ClassNotFoundException | IOException ex) {
                        Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        try {
                            fileIn.close();
                            in.close();
                        } catch (IOException ex) {
                            Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
               }
        

        //Register the curator service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Atifact-information");
        sd.setName("curator");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println("Hello! Curator " + getAID().getName() + " is ready...");
        } catch (FIPAException ex) {
            Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
            


        addBehaviour(new CuratorBehaviours(this));
    }

    @Override
    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException ex) {
            Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        // Printout a dismissal message
        System.out.println("curator "+getAID().getName()+" terminating...");
    }
    
    private class CuratorBehaviours extends ParallelBehaviour{
        public CuratorBehaviours(Agent a){
            super (a, ParallelBehaviour.WHEN_ALL);
            
            // reply to Profiler Agent
            addSubBehaviour(new CyclicBehaviour(a) {

                @Override
                public void action() {
           // prepare the template to get message from profiler
           MessageTemplate mt= MessageTemplate.MatchConversationId("Artifact-info");
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                if(msg.getPerformative()== ACLMessage.REQUEST){
                    String request=msg.getContent();
                System.out.println(getAID().getName() + ": "+msg.getSender().getName()+ "profiler is asking for details of "+request);
                
                // request message recevied. process it
                ACLMessage reply=msg.createReply();
                
                    Artifact toReply = null;
                    // find the requested artifact from the List of artifacts
                    for(int i=0; i<listOfArtifacts.size();i++){
                        Artifact a=listOfArtifacts.get(i);
                        if(a.getName().equals(request)){
                            toReply=a;
                            reply.setPerformative(ACLMessage.INFORM);
                        try {
                            reply.setContentObject((Artifact) toReply);
                            myAgent.send(reply);
                            System.out.println(getAID().getName() + " is sending response...");
                        } catch (IOException ex) {
                            Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                            break;
                        }
                        else{
                            System.out.println("curator "+getAID().getName()+": The artifact "+ request+" could not be found! ");
                            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                            reply.setContent("The artifact "+ request+" could not be found!");
                        }
                    }
                        
                }
                        
                
                    

//                        FileInputStream fileIn;
//                        try {
//                            fileIn = new FileInputStream(request+".ser");
//                            ObjectInputStream in=new ObjectInputStream(fileIn);
//                            toReply=(Artifact)in.readObject();
//                            in.close();
//                            fileIn.close();
//                            reply.setPerformative(ACLMessage.INFORM);
//                            reply.setContentObject((Serializable) toReply);
//                        } catch (FileNotFoundException ex) {
//                            System.out.println("curator "+getAID().getName()+": The artifact could not be found! ");
//                            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
//                            reply.setContent("The artifact could not be found!");
//                        }catch(IOException e){
//                            Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, null, e);
//                        }catch(ClassNotFoundException c){
//                            Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, null, c);
//                        }
                        
                }
            else{
                block();
            }
                }
            });
            
            // reply to Tourguide Agent
            addSubBehaviour(new CyclicBehaviour(a){

                @Override
                public void action() {
                    // prepare the template to get message from Tourguide
           MessageTemplate mt=MessageTemplate.and(MessageTemplate.MatchConversationId("Artifact-list"),MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
           ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                System.out.println(getAID().getName() + ": "+msg.getSender().getName()+ "Tourguide is asking for list of artifacts");
                // request message recevied. process it
                ACLMessage reply= msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
               
                        try {
                            reply.setContentObject((Serializable)listOfArtifacts);
                        } catch (IOException ex) {
                            Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, null, ex);
                        }
               myAgent.send(reply);
                }
            
            else{
                block();
            }
                }  
            });
        }
    }
}