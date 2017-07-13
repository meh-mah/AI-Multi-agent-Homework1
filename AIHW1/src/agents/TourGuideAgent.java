package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
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
public class TourGuideAgent extends Agent {
    
    private AID [] curatorAgents;
    // map is used since it cannot contain dupplicated key
    private Map<Artifact, AID> artifactsAndCurators = new HashMap<Artifact, AID>();

    @Override
    protected void setup() {

        //Register the service in DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("virtual tour");
        sd.setName("tourguide");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println("Hello! TourGuide " + getAID().getName() + " is ready...");
        } catch (FIPAException ex) {
            Logger.getLogger(TourGuideAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
          
        addBehaviour(new TourGuideBehaviour(this));
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
        System.out.println("tourguide "+getAID().getName()+" terminating...");
    }

    private class TourGuideBehaviour extends ParallelBehaviour {
        public TourGuideBehaviour(Agent a) {
            super(a, ParallelBehaviour.WHEN_ALL);

            //reply to ProfilerAgent
            addSubBehaviour(new CyclicBehaviour(a) {
                @Override
                public void action() {
                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        ACLMessage reply = msg.createReply();

                        StringTokenizer st = new StringTokenizer(msg.getContent(), ",");
                        int age = Integer.parseInt(st.nextToken());
                        String style = st.nextToken();

                        System.out.println(myAgent.getAID().getName() + " received request for tour from"+ msg.getSender());

                       // create a map contain the name of listOfArtifacts and their curator, which are match profiler's interest
                        Map<String, AID> replyMap = new HashMap<>();
                        for(Artifact inMap: artifactsAndCurators.keySet()){
                            if (inMap.getStyle().equals(style) && (inMap.getMinAge() <= age) && (inMap.getMaxAge() >= age)){
                                String nameOfArtifact=inMap.getName();
                                AID curator=artifactsAndCurators.get(inMap);
                                replyMap.put(nameOfArtifact, curator);
                            }
                        }
                        
                        if (!replyMap.isEmpty()){
                            try {
                            reply.setContentObject((Serializable) replyMap);
                            reply.setPerformative(ACLMessage.PROPOSE);
                            System.out.println(myAgent.getAID().getName() + " sending tour...");
                        } catch (IOException ex) {
                            Logger.getLogger(TourGuideAgent.class.getName()).log(Level.SEVERE, null, ex);
                        } 
                        }
                        else{
                            try {
                                reply.setContentObject("No match found");
                                reply.setPerformative(ACLMessage.REFUSE);
                                System.out.println(myAgent.getAID().getName() + " no match found");
                            } catch (IOException ex) {
                                Logger.getLogger(TourGuideAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                        }
                        myAgent.send(reply);
                    } else {
                        block();
                    }
                }
            });

            //search for Curators and their listOfArtifacts
            addSubBehaviour(new TickerBehaviour(a, 60000) {
                @Override
                protected void onTick() {

                    //search for curators
                    System.out.println(myAgent.getAID().getName() + " start to search for curators...");

                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("Atifact-information");

                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        
                            System.out.println("found the following curator(s):");
                            curatorAgents=new AID [result.length];
                            for(int i=0; i<result.length; i++){
                                curatorAgents[i]=result[i].getName();
                                System.out.println(curatorAgents[i].getName());
                            }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    //ask for listOfArtifacts
                    ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                    for(int i=0; i<curatorAgents.length; i++){
                        req.addReceiver(curatorAgents[i]);
                        req.setConversationId("Artifact-list");
                        System.out.println(myAgent.getAID().getName() + " is sending message to curator " + curatorAgents[i].getName());
                    }
                        myAgent.send(req);
                }
            });

            //receive listOfArtifacts
            addSubBehaviour(new CyclicBehaviour(a) {
                @Override
                public void action() {
                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        AID curator = msg.getSender();
                        System.out.println(myAgent.getAID().getName() + " received message from the curator " + curator.getName() + "!");
                        List<Artifact> listOfArtifacts;
                        try {
                            listOfArtifacts = (List<Artifact>) msg.getContentObject();
                            for (Artifact art : listOfArtifacts) {
                                    artifactsAndCurators.put(art, curator);
                            }
                        } catch (UnreadableException ex) {
                            Logger.getLogger(TourGuideAgent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                            
                    } else {
                        block();
                    }
                }
            });
        }
    }
}