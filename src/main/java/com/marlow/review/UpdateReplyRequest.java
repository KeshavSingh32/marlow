package com.marlow.review;

public record UpdateReplyRequest(
        String draftReply,
        ReplyStatus replyStatus
) {
}
