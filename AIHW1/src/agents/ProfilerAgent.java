package agents;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SubscriptionInitiator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Artifact;

/**
 *
 * @author M&M
 */
public class ProfilerAgent extends Agent {
    // list of artifacts' description sent by curator
    private List<Artifact> viewed;
    // map of artifacts to visit and corresponding curator
    private Map<String, AID> toVisit;
   // the list of known tourguide agent
    private AID[] tourGuideList;
    private int i=0;
    private int j=0;
    private int k=0;
    /**
         * since the Profiler agent contains user information, 
         * we assume that the retrieved information from the tourguide is based on only user's age, and prefered style.  
        */
    private int age;
    private String style;
    
    // put agent initialization hear
    @Override
    protected void setup() {
        toVisit = new HashMap<>();
        viewed=new ArrayList<>();
        
        Object[] args = getArguments();
        if (args != null && args.length > 1) {
            age = Integer.parseInt((String) args[0]);
            style = (String) args[1];
        } else {
            doDelete();
        }

        System.out.println("Hello! Profiler agent " + getAID().getName() + " is ready...");
        // search virtualTour guides
            DFAgentDescription temp = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("virtual tour");
            temp.addServices(sd);
                    addBehaviour(new SubscriptionInitiator(this, DFService.createSubscriptionMessage(this, getDefaultDF(), temp, null)){
                @Override
                protected void handleInform(ACLMessage inform){
                    try {
                        DFAgentDescription [] dfds= DFService.decodeNotification(inform.getContent());
                        System.out.println(myAgent.getAID().getName() + " found the following tourguide agents:");
                        tourGuideList = new AID[dfds.length];
                        k=tourGuideList.length;
                        for (int i = 0; i < dfds.length; ++i) {
                            tourGuideList[i] = dfds[i].getName();
                            System.out.println("\t" + tourGuideList[i].getName());
                        }
                    } catch (FIPAException ex) {
                        Logger.getLogger(ProfilerAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    try {
                        if (k>0){
                        addBehaviour(new PABehaviour(myAgent));
                    }
                    } catch (NullPointerException e){
                        
                    }
                }
            });
                    
                    
    }

    // Put agent clean-up operations here
    @Override
    protected void takeDown() {
        // print out a dismissal message
        System.out.println("Profiler agent" + getAID().getName() + " terminating...");
    }

    private class PABehaviour extends SequentialBehaviour {
        
        private MessageTemplate template;
        

        PABehaviour(final Agent agent) {
            super(agent);

            
            
            
            ///////////////////////////////////////////////////////////test
//            addSubBehaviour(new OneShotBehaviour(agent) {
//                @Override
//                public void action() {
//
//                    System.out.println(myAgent.getAID().getName() + " is searching for tour-guides...");
//                    DFAgentDescription template = new DFAgentDescription();
//                    ServiceDescription sd = new ServiceDescription();
//                    sd.setType("virtual tour");
//                    template.addServices(sd);
//                    try {
//                        DFAgentDescription[] result = DFService.search(agent, template);
//                        if (result.length == 0) {
//                            System.out.println(myAgent.getAID().getName() + ": ERROR - No tour-guides found...");
//                            myAgent.doDelete();
//                        }
//
//                        System.out.println(myAgent.getAID().getName() + " has found the next tour-guides:");
//                       tourGuideList = new AID[result.length];
//                        for (int i = 0; i < result.length; ++i) {
//                            tourGuideList[i] = result[i].getName();
//                            System.out.println("\t" + tourGuideList[i].getName());
//                        }
//                    } catch (FIPAException fe) {
//                        fe.printStackTrace();
//                    }
//                }
//            });
            


            // sending request for  virtualTour to tourguid agents
            addSubBehaviour(new WakerBehaviour(agent, 10000) {
                @Override
                protected void onWake() {
                    try{
                    System.out.println(myAgent.getAID().getName() + " start to send request for virtual tour...");
                    // Send the request to all tourguides
                    ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                    i=tourGuideList.length;
                    for (int i = 0; i < tourGuideList.length; ++i) {
                        req.addReceiver(tourGuideList[i]);
                        System.out.println("sending request to: " + tourGuideList[i].getName());
                    }

                    req.setContent(age + "," + style);
                    req.setConversationId("tour request");
                    myAgent.send(req);
                   // prepare template to receive reply from tourguide agent
                    template = MessageTemplate.MatchConversationId("tour request");
                    } catch (NullPointerException e){
                        
                    }
                }
            });

            // receive  virtualTour from tourguide agent
            addSubBehaviour(new Behaviour(agent) {
                int response=0;

                @Override
                public void action() {

                    ACLMessage reply = myAgent.receive(template);
                    if (reply != null) {
                        if(reply.getPerformative()==ACLMessage.PROPOSE){
                          
                            try {
                                toVisit = (Map<String, AID>) reply.getContentObject();
                                System.out.println(myAgent.getAID().getName() + ": virtual tour received from: " + reply.getSender().getName());
                            } catch (UnreadableException ex) {
                                Logger.getLogger(ProfilerAgent.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        else if(reply.getPerformative()==ACLMessage.REFUSE){
                            System.out.println(reply.getSender().getName()+" says: "+ reply.getContent());
                        }
                     response++;   
                    } 
                    else {
                        block();
                    }
                }
                @Override
                public boolean done(){

                    return response== i;

                }

            });

            // retrieved description of artifacts in virtual tour from corresponding curator
            addSubBehaviour(new OneShotBehaviour(agent) {
                @Override
                public void action() {

                    System.out.println(myAgent.getAID().getName() + " start to retrieve artifacts description from curator agent(s)...");
                    
                    if(!toVisit.isEmpty()){
                      for (Entry<String, AID> e : toVisit.entrySet()) {
                        String artName = e.getKey();
                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                        msg.addReceiver(e.getValue());
                        msg.setContent(artName);
                        msg.setConversationId("Artifact-info");
                        myAgent.send(msg);
                        System.out.println(myAgent.getAID().getName() + " send a message to curator agent "+e.getValue().getName());
                      }  
                    } 
                    j=toVisit.size();
                    toVisit.clear();
                    // Prepare the template to get proposals
                    template = MessageTemplate.MatchConversationId("Artifact-info");
                }
            });

            // receive reply from curator agent
            addSubBehaviour(new Behaviour(agent) {
                int receivedDescription=0;

                @Override
                public void action() {
                    ACLMessage msg = myAgent.receive(template);
                    if (msg != null) {
                        if (msg.getPerformative()==ACLMessage.INFORM){
                          System.out.println(myAgent.getAID().getName() + ": Artifact description received from: " + msg.getSender().getName());
                                Artifact art;
                        try {
                            art = (Artifact) msg.getContentObject();
                            // storing the artifact description if it is not already exist.
                            if(viewed.isEmpty()){
                                viewed.add(art);       
                            } else{
                              for(int i=0; i<viewed.size(); i++){
                                if(viewed.contains(art)){
                                  viewed.add(art);  
                                }
                            }  
                            }
                            
                            
                            System.out.println("Received details information for artifact: " + art.getName());
                        } catch (UnreadableException ex) {
                            Logger.getLogger(ProfilerAgent.class.getName()).log(Level.SEVERE, null, ex);
                        }  
                        }
                        // 
                        else if (msg.getPerformative()==ACLMessage.NOT_UNDERSTOOD){
                            System.out.println(msg.getSender().getName()+" says: "+msg.getContent());                          
                        } 
                        receivedDescription++;
                                
                    } else {
                        block();
                    }
                }

                @Override
                public boolean done() {
                   return receivedDescription== j;
                }

            });
        }
    }
}