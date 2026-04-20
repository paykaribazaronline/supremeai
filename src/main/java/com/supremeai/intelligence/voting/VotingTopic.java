package com.supremeai.intelligence.voting;

public class VotingTopic {
    private String topicId;
    private String category; // e.g., "ARCHITECTURE", "SECURITY", "PERFORMANCE"
    private String context;
    private String questionToAsk;

    public VotingTopic(String topicId, String category, String context, String questionToAsk) {
        this.topicId = topicId;
        this.category = category;
        this.context = context;
        this.questionToAsk = questionToAsk;
    }

    public String getTopicId() { return topicId; }
    public String getCategory() { return category; }
    public String getContext() { return context; }
    public String getQuestionToAsk() { return questionToAsk; }
}