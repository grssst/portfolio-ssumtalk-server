package com.hottalk.hottalkserver.dto;

import java.time.LocalDateTime;

public class UserProfileResponseDTO {
    private UserProfileDTO myUser;
    private UserProfileDTO otherUser;


    public UserProfileDTO getMyUser() {
        return myUser;
    }

    public void setMyUser(UserProfileDTO myUser) {
        this.myUser = myUser;
    }

    public UserProfileDTO getOtherUser() {
        return otherUser;
    }

    public void setOtherUser(UserProfileDTO otherUser) {
        this.otherUser = otherUser;
    }




}

