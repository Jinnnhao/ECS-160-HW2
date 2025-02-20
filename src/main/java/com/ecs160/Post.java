package com.ecs160;

import java.util.ArrayList;
import java.util.List;

import com.ecs160.persistence.LazyLoad;
import com.ecs160.persistence.Persistable;
import com.ecs160.persistence.PersistableField;
import com.ecs160.persistence.PersistableId;
import com.ecs160.persistence.PersistableListField;


@Persistable
public class Post {
    @PersistableId
    private Integer postId;
    
    @PersistableField
    private String postContent;
    
    @PersistableListField(className="com.ecs160.Post")
    @LazyLoad
    private List<Post> replies;

    // Update your constructors and methods accordingly
    // Default constructor
    public Post() {
        this.postId = null;
        this.postContent = null;
        this.replies = new ArrayList<>();
    }
    // Constructor
    public Post(Integer postId, String postContent) {
        this.postId = postId;
        this.postContent = postContent;
        this.replies = new ArrayList<>();
    }

    // Add a reply
    public void addReply(Post reply) {
        if (reply != null) {
            replies.add(reply);
        }
    }

    // Getters and Setters
 
    public Integer getpostId() {
        return postId;
    }


    public String getpostContent() {
        return postContent;
    }

    public List<Post> getReplies() {
        return replies;
    }

    public int getTotalReplies() {
        int total = replies.size();
        for (Post reply : replies) {
            total += reply.getTotalReplies();
        }
        return total;
    }
}
