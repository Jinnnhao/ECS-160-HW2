package com.ecs160;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import com.ecs160.persistence.Session;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;  

public class MyApp {
    public static void main(String[] args) throws FileNotFoundException, URISyntaxException, NoSuchFieldException {
        // RedisDAO redisDAO = null;
        Session session = null;

        try {
            // Initialize Redis and Session
            //redisDAO = new RedisDAO();
            session = new Session();

            // Load JSON from resources
            InputStream inputStream = MyApp.class.getClassLoader().getResourceAsStream("input.json");
            if (inputStream == null) {
                throw new FileNotFoundException("input.json not found in resources directory");
            }

            // Parse JSON
            JsonObject root = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                                     .getAsJsonObject();
            if (!root.has("feed")) {
                throw new IllegalStateException("JSON does not contain 'feed' array");
            }

            JsonArray feedArray = root.getAsJsonArray("feed");
            //System.out.println("Found " + feedArray.size() + " items in feed");

            // Parse threads and persist posts
            List<Thread> threads = JsonParserUtil.parseFeed(feedArray);
            //System.out.println("Found " + threads.size() + " threads");

            // Persist all posts using the Session
            // int persistedCount = 0;
            for (Thread thread : threads) {
                Post rootPost = thread.getRootPost();
                session.add(rootPost);
                
                // Also persist all replies
                for (Post reply : rootPost.getReplies()) {
                    session.add(reply);
                }
            }
            
            // Persist all pending objects
            session.persistAll();
           
            while(true){
                Scanner scanner = new Scanner(System.in);
                System.out.println(" ");
                // Ask user for which mode they want to load the post
                System.out.println("Which loading mode do you want to use? (L (LazyLoad)/ N (Normal)): ");
                String modeAnswer = scanner.next();
                System.out.println(" ");
                System.out.println("Enter the postId to load: ");
                int postId = scanner.nextInt();
                System.out.println(" ");
                Post template = new Post(postId, null); // Create template with just ID
               
                if(modeAnswer.equals("L")){

                    Post loadedPost = (Post) session.LazyLoad(template);  // Load the post from the database
                    System.out.println("> Post --> "+loadedPost.getpostContent()); // Print the content of the post
                    System.out.println(" ");
                    
                    // Ask the user if they want to load the replies
                    System.out.println("Do you want to load the replies? (y/n): ");
                    String answer = scanner.next();
                    System.out.println(" ");
                
                    if(!answer.equals("n")){
                        // This will trigger the lazy loading of all replies
                        List<Post> replies = loadedPost.getReplies(); // Full data loaded here
                        
                        if(replies.isEmpty()){
                            System.out.println("No replies found");
                        }
                        else{
                            for(Post reply : replies) {
                                Post reply2 = (Post) session.LazyLoad(reply);
                                System.out.println("> Repies --> " + reply2.getpostContent());
                                System.out.println(" ");
                            }
                        }
                        
                    }
                }else if(modeAnswer.equals("N")){
                    Post loadedPost = (Post) session.load(template);  // Load the post from the database
                    System.out.println("> Post --> "+loadedPost.getpostContent()); // Print the content of the post
                    System.out.println(" ");

                    List<Post> replies = loadedPost.getReplies();
                    if(replies.isEmpty()){
                        System.out.println("No replies found");
                    }
                    else {
                        for(Post reply : replies) {
                            System.out.println("> Repies --> " + reply.getpostContent());
                            System.out.println(" ");
                        }
                    }
                        

                }
                
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
         } 
    }
}
