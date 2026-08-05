package org.example.hive.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.example.hive.config.AppEnums.ReactionType;

@Getter
@Setter
public class ReactToPostRequest {

    private ReactionType reactionType = ReactionType.LIKE;
}
