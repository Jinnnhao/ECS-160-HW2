package com.ecs160.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;

// Assumption - only support int/long/and string values
public class Session {

    private Jedis jedisSession;
    private List<Object> pendingObjects;

    public Session() {
        jedisSession = new Jedis("localhost", 6379);
        pendingObjects = new ArrayList<>();
    }


    public void add(Object obj) {
        // check if the object is annotated with @Persistable
        Class<?> objClass = obj.getClass();
        if(!objClass.isAnnotationPresent(Persistable.class)) {
            throw new IllegalArgumentException("Object must be annotated with @Persistable");
        }
        
        pendingObjects.add(obj);
    }

    // Persist all pending objects with valid annotations in the database 
    public void persistAll() throws IllegalAccessException {
        // Loop all the objects in the pendingObjects list and create a map for each object 
        for(Object obj : pendingObjects) {
            Class<?> objClass = obj.getClass();
            Map<String, String> objectMap = new HashMap<>();  // Map <Key, Value>

            if(!objClass.isAnnotationPresent(Persistable.class)) {
                throw new IllegalArgumentException("Object must be annotated with @Persistable");
            }

            for(Field field : objClass.getDeclaredFields()) {
                field.setAccessible(true);  // make the private field accessible 
                if(field.isAnnotationPresent(PersistableId.class)) {
                    // Put the "postId" map with the actual Id number of the object in the post map
                    objectMap.put(field.getName(), String.valueOf(field.get(obj)));
                }
                // if the current field is a content 
                if(field.isAnnotationPresent(PersistableField.class)) {
                    // Put the "postContent" map with the actual content of the object in the post map
                    objectMap.put(field.getName(), String.valueOf(field.get(obj)));  
                }
                // if the current field is a list of objects
                if(field.isAnnotationPresent(PersistableListField.class)) {
                    // Put the "replies" map with the actual replies of the object in the post map
                    List<?> replies = (List<?>) field.get(obj);  // get the actual list of replies of the object
                    List<String> replyIds = new ArrayList<>();   // The list of String to store all the IDs of the replies
                    
                    for(Object reply : replies) {
                        // Get all the ids of the replies in to a String and separate them by commas
                        // The list of String to store all the IDs of the replie 
                        for(Field replyField : reply.getClass().getDeclaredFields()) {
                            replyField.setAccessible(true);
                            if(replyField.isAnnotationPresent(PersistableId.class)) {
                                // Get the actual ID  and convert it to a String and store it in the list
                                replyIds.add(replyField.get(reply).toString());
                            }
                        }
                    }
                    // Put the "replies" map with the actual replies of the object in the post map
                    objectMap.put(field.getName(), String.join(",", replyIds));
                }
            }

            // put all the objectMap into the database
            // The postID will also act as the key to access each post map in the database
            // Get the postId from the map and put the objectMap into the database
            jedisSession.hmset(objectMap.get("postId"), objectMap);
        }
    }



    public Object load(Object object) throws IllegalAccessException, NoSuchMethodException, 
            InstantiationException, InvocationTargetException, ClassNotFoundException, Exception {
        if(object == null) {
            return null;
        }
        // Get the Id of the object 
        Class<?> objClass = object.getClass();
        Map<String, String> objectMap = new HashMap<>();
        String objectId;
        // Get the objectMap from the database
        for(Field field : objClass.getDeclaredFields()) {
            field.setAccessible(true);
            if(field.isAnnotationPresent(PersistableId.class)) {
                objectId = field.get(object).toString();
                objectMap = jedisSession.hgetAll(objectId);   
            }
        }
      
        for (Field field : objClass.getDeclaredFields()) {
            field.setAccessible(true); 
            if(field.isAnnotationPresent(PersistableField.class)) {
                // field.getName() is the name of field in the class 
                // objectMap.get(field.getName()) is the value of the field in the database
                // field.set(result, objectMap.get(field.getName())) is to set the value of the field into the result object
                field.set(object, objectMap.get(field.getName()));  // set the test content into the result object
            }
            if(field.isAnnotationPresent(PersistableListField.class)) {
                String ids = objectMap.get(field.getName()); // use the field name "replies" to get the ids of the replies from the map
                    
                    if (ids != null && !ids.isEmpty()) {
                        PersistableListField annotation = field.getAnnotation(PersistableListField.class); // get the annotation of the field in specific class
                        // annotation.className() is the name of the class of the object in the list
                        Class<?> replyClass = Class.forName(annotation.className()); // The type of the object in the list

                        List<Object> list = new ArrayList<>(); // The list of Object to store all the objects
                        
                        // Loop through all the reply ids and create a map for each replay 
                        for (String itemId : ids.split(",")) {
                            Map<String, String> replayMap = jedisSession.hgetAll(itemId); 

                            Object replyObj = replyClass.getDeclaredConstructor().newInstance(); // Create a new instance of the reply object, which is a Post 
                            
                            // Set the ID field and the content field of the reply object
                           for(Field replyField : replyClass.getDeclaredFields()) {
                                replyField.setAccessible(true);
                                if(replyField.isAnnotationPresent(PersistableId.class)) {
                                    replyField.set(replyObj, Integer.parseInt(itemId));  // set the ID of the reply object to the its ID field 
                                }
                                if(replyField.isAnnotationPresent(PersistableField.class)) {
                                    replyField.set(replyObj, replayMap.get(replyField.getName())); // set the content of the reply into the reply object
                                }
                                //At each reply object, add it to the list 
                            }  
                             
                            list.add(replyObj);
                        
                        }
                    field.set(object, list);
                    
                }
            }
            
         }
         return object;
        }


        // For Lazy Loading 
        public Object LazyLoad(Object object) throws IllegalAccessException, NoSuchMethodException, 
            InstantiationException, InvocationTargetException, ClassNotFoundException, Exception {
        if(object == null) {
            return null;
        }
        // Get the Id of the object 
        Class<?> objClass = object.getClass();
        Map<String, String> objectMap = new HashMap<>();
        String objectId;
        // Get the objectMap from the database
        for(Field field : objClass.getDeclaredFields()) {
            field.setAccessible(true);
            if(field.isAnnotationPresent(PersistableId.class)) {
                objectId = field.get(object).toString();
                objectMap = jedisSession.hgetAll(objectId);   
            }
        }
      
        for (Field field : objClass.getDeclaredFields()) {
            field.setAccessible(true); 
            if(field.isAnnotationPresent(PersistableField.class)) {
                // field.getName() is the name of field in the class 
                // objectMap.get(field.getName()) is the value of the field in the database
                // field.set(result, objectMap.get(field.getName())) is to set the value of the field into the result object
                field.set(object, objectMap.get(field.getName()));  // set the test content into the result object
            }
            if(field.isAnnotationPresent(PersistableListField.class) && 
            field.isAnnotationPresent(LazyLoad.class)) {
                String ids = objectMap.get(field.getName()); // use the field name "replies" to get the ids of the replies from the map
                    
                    if (ids != null && !ids.isEmpty()) {
                        PersistableListField annotation = field.getAnnotation(PersistableListField.class); // get the annotation of the field in specific class
                        // annotation.className() is the name of the class of the object in the list
                        Class<?> replyClass = Class.forName(annotation.className()); // The type of the object in the list

                        List<Object> list = new ArrayList<>(); // The list of Object to store all the objects
                        
                        // Loop through all the reply ids and create a map for each replay 
                        for (String itemId : ids.split(",")) {
                            //Map<String, String> replayMap = jedisSession.hgetAll(itemId); 

                            Object replyObj = replyClass.getDeclaredConstructor().newInstance(); // Create a new instance of the reply object, which is a Post 
                            
                            // Set the ID field and the content field of the reply object
                           for(Field replyField : replyClass.getDeclaredFields()) {
                                replyField.setAccessible(true);
                                if(replyField.isAnnotationPresent(PersistableId.class)) {
                                    replyField.set(replyObj, Integer.parseInt(itemId));  // set the ID of the reply object to the its ID field 
                                }
                            }  
                             
                            list.add(replyObj);
                        
                        }
                    field.set(object, list);
                    
                }
            }
            
         }
         return object;
        }
    }
