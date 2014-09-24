/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package websocket.chat;

/**
 *
 * @author Manohar
 */
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.PathParam;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import java.util.*;
import java.io.*;
import javax.json.spi.JsonProvider;


@ServerEndpoint(value = "/chat/{client-id}")
public class Chat {
    
    private static final Log log = LogFactory.getLog(Chat.class);
    static Map<String, Session> hm = new HashMap<String, Session>( );
   
  
    
    private static final Set<Chat> connections = new CopyOnWriteArraySet<Chat>();

    private String nickname;
    private Session session;
    JsonProvider provider = JsonProvider.provider();
    public Chat() {
	    
        
    }


    @OnOpen
    public void start(Session session,@PathParam("client-id") String name) {
        this.session = session;
        connections.add(this);
	nickname  = name;
	hm.put(nickname, session);
       
        JsonObject connected = provider.createObjectBuilder()
                    .add("action", "connected")
                    .add("name", nickname)
                    .build();
        
        broadcast_all(connected);
        //let send about existing users.
        
        for (Map.Entry<String, Session> entry : hm.entrySet()) {
            
            if(entry.getKey().equals(nickname)){
            
            } else {
                
                JsonObject   conn = provider.createObjectBuilder()
                        .add("action", "connected")
                        .add("name",entry.getKey() )
                        .build();
                
                broadcastex(conn, nickname);  
            }      
	}
    
    }


    @OnClose
    public void end() {
        connections.remove(this);
        JsonObject dis = provider.createObjectBuilder()
                    .add("action", "disconnected")
                    .add("name", nickname)
                    .build();
        broadcast_all(dis);
    }


    @OnMessage
    public void incoming(String message) {
	   
                JsonReader reader = Json.createReader(new StringReader(message));
		JsonObject jsonMessage = reader.readObject();
		
		broadcast( jsonMessage.getString("message"), jsonMessage.getString("to"), nickname);
               
    }




    @OnError
    public void onError(Throwable t) throws Throwable {
        log.error("Chat Error: " + t.toString(), t);
    }


    private static void broadcast_all(JsonObject  msg) {
        for (Chat client : connections) {
            try {
                synchronized (client) {
                    client.session.getBasicRemote().sendText(msg.toString());
                }
            } catch (IOException e) {
                log.debug("Chat Error: Failed to send message to client", e);
                connections.remove(client);
                try {
                    client.session.close();
                } catch (IOException e1) {
                    // Ignore
                }
                
                 JsonProvider provider = JsonProvider.provider();
                 JsonObject removed = provider.createObjectBuilder()
                       .add("action", "disconnected")
                       .add("name", client.nickname)
                       .build();
        
                 broadcast_all(removed);
            
            }
        }
    }
	
	
    private static void broadcast(String msg, String to, String name) {
	
	       Session jk =   hm.get(to);
	       JsonProvider provider = JsonProvider.provider();
               JsonObject msg1 = provider.createObjectBuilder()
                       .add("action", "frndmessage")
                       .add("message", msg )
                       .add("to", to)
                       .add("from", name)
                       .build();
            try {
              
                   jk.getBasicRemote().sendText(msg1.toString());
               
              } catch (IOException e) {
                log.debug("Chat Error: Failed to send message to client", e);
      
             }
      
    }

    private static void broadcastex(JsonObject conn, String to) {
        
        Session jk =   hm.get(to);
        
         try {
                 jk.getBasicRemote().sendText(conn.toString());
               
              } catch (IOException e) {
                log.debug("Chat Error: Failed to send message to client", e);
      
             }
    
    }
    
    
    
}
