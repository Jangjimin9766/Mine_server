package com.mine.api.event;

import com.mine.api.domain.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class UserSignupEvent extends ApplicationEvent {
    private final User user;
    private final List<String> interests;

    public UserSignupEvent(Object source, User user, List<String> interests) {
        super(source);
        this.user = user;
        this.interests = interests;
    }
}
