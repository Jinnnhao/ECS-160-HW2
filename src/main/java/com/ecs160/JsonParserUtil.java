package com.ecs160;       
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonParserUtil {
    private static int count = 0;

    public static Post parsePost(JsonObject postObject) {
        try {
            // Extract post content from the record
            JsonObject record = postObject.has("record") ? postObject.getAsJsonObject("record") : null;
            String text = record != null && record.has("text") ? record.get("text").getAsString() : "";
            
            // Create new post with auto-incremented 
            Post post = new Post(++count, text);

            // System.out.println("Parsed post: " + post.getpostId());
 
            return post;

        } catch (Exception e) {
            System.err.println("Error parsing post: " + e.getMessage());
            return null;
        }
    }

    private static void parseReplies(JsonArray repliesArray, Post parentPost) {
        for (JsonElement replyElement : repliesArray) {
            try {
                JsonObject replyThreadView = replyElement.getAsJsonObject();
                if (replyThreadView.has("post")) {
                    JsonObject replyPost = replyThreadView.getAsJsonObject("post");
                    Post reply = parsePost(replyPost);
                    if (reply != null) {
                        parentPost.addReply(reply);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing reply: " + e.getMessage());
            }
        }
    }

    public static List<Thread> parseFeed(JsonArray feedArray) {
        List<Thread> threads = new ArrayList<>();
        if (feedArray == null) {
            System.err.println("Error: Feed array is null");
            return threads;
        }

       // System.out.println("Processing " + feedArray.size() + " feed items...");

        for (JsonElement feedElement : feedArray) {
            try {
                JsonObject feedObject = feedElement.getAsJsonObject();
                
                if (!feedObject.has("thread")) {
                    //System.err.println("Skipping feed item: Missing 'thread' key.");
                    continue;
                }
                
                JsonObject threadObject = feedObject.getAsJsonObject("thread");
                if (!threadObject.has("post")) {
                    // System.err.println("Skipping thread: Missing 'post' key.");
                    continue;
                }

                JsonObject postObject = threadObject.getAsJsonObject("post");
                Post rootPost = parsePost(postObject);
                
                if (threadObject.has("replies")) {
                    parseReplies(threadObject.getAsJsonArray("replies"), rootPost);
                }

                if (rootPost != null) {
                    threads.add(new Thread(rootPost));
                }
            } catch (Exception e) {
                System.err.println("Error processing feed item: " + e.getMessage());
                e.printStackTrace();
            }
        }

        //System.out.println("Successfully parsed " + threads.size() + " threads");
        return threads;
    }
}