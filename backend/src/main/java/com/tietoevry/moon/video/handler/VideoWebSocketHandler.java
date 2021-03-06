package com.tietoevry.moon.video.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tietoevry.moon.classroom.ClassroomService;
import com.tietoevry.moon.classroom.model.Classroom;
import com.tietoevry.moon.user.UserMapper;
import com.tietoevry.moon.user.UserService;
import com.tietoevry.moon.user.model.dto.ActiveUserDto;
import com.tietoevry.moon.user.model.dto.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class VideoWebSocketHandler extends TextWebSocketHandler {
    @Autowired
    ClassroomService classroomService;
    @Autowired
    UserService userService;
    private static final List<WebSocketSession> webSocketSessions = new ArrayList<>();

    public void handleTextMessage(WebSocketSession session, TextMessage message)
        throws InterruptedException, IOException {
        Map messageContent = new Gson().fromJson(message.getPayload(), Map.class);
        System.out.println("message received");
        String type = String.valueOf(messageContent.get("type"));
        if (type.equals("question")) {
            handleQuestion(message, session);
        } else if (type.equals("activeUsers")) {
            activeUsers(message, session);
        } else if (type.equals("answer")) {
            handleAnswer(message, session);
        } else if (type.equals("points")) {
            handlePoints(message, session);
        }

    }

    private void handleAnswer(TextMessage message, WebSocketSession session) throws IOException {
        Map messageContent = new Gson().fromJson(message.getPayload(), Map.class);
        String name = String.valueOf(messageContent.get("teacherUsername"));
        UserDto teacher = userService.findByUsername(name);
        for (WebSocketSession webSocketSession : webSocketSessions) {
            if (webSocketSession.getPrincipal().getName().equals(name)) {//  if (webSocketSession != session) {
                // System.out.println(teacher.getUsername());
                webSocketSession.sendMessage(message);
            }//}
        }

    }

    private void handleQuestion(TextMessage message, WebSocketSession session) throws IOException {
        Map messageContent = new Gson().fromJson(message.getPayload(), Map.class);
        String classroomName = String.valueOf(messageContent.get("classroom"));
        String username = session.getPrincipal().getName();
        //  System.out.println(username);
        Classroom classroom = classroomService.findClassroomByName(classroomName);
        for (WebSocketSession webSocketSession : webSocketSessions) {
            //  if (webSocketSession != session) {
            if (classroom
                .getUser()
                .stream()
                .anyMatch(student -> student
                    .getUsername()
                    .contains(webSocketSession.getPrincipal().getName()))) {
                webSocketSession.sendMessage(message);
                //}
            }
        }
    }

    private void activeUsers(TextMessage message, WebSocketSession session) throws IOException {

        Map messageContent = new Gson().fromJson(message.getPayload(), Map.class);
        String classroomName = String.valueOf(messageContent.get("classroom"));
        Classroom classroom = classroomService.findClassroomByName(classroomName);
        String include = String.valueOf(messageContent.get("include"));
        String teacherUsername = String.valueOf(messageContent.get("teacherUsername"));
        List<String> users = webSocketSessions.stream().map(w -> w.getPrincipal().getName()).collect(Collectors.toList());
        List<ActiveUserDto> studentsOfAClass = classroom
            .getUser()
            .stream().map(UserMapper::mapUserActiveDto)
            .map(s -> {
                if (users.contains(s.getUsername())) {
                    s.setActive(true);
                } else {
                    s.setActive(false);
                }
                return s;
            }).collect(Collectors.toList());
        List<UserDto> checkUsers = classroom.getUser().stream().map(UserMapper::mapUserDto).collect(Collectors.toList());
        if (include.equals("true")) {
            checkUsers = checkUsers.stream().filter(cu -> cu.getUsername() != session.getPrincipal().getName()).collect(Collectors.toList());
        }
        checkUsers.add(userService.findByUsername(teacherUsername));
        for (WebSocketSession webSocketSession : webSocketSessions) {
            if (checkUsers
                .stream()
                .anyMatch(student -> student
                    .getUsername()
                    .contains(webSocketSession.getPrincipal().getName()))) {
                session.sendMessage(new TextMessage(new Gson().toJson(studentsOfAClass).toString()));
            }

        }
    }


    private void handlePoints(TextMessage message, WebSocketSession session) throws IOException {

        Map messageContent = new Gson().fromJson(message.getPayload(), Map.class);
        double points = (double) messageContent.get("points");
        String user = String.valueOf(messageContent.get("user"));

        String type = String.valueOf(messageContent.get("type"));

        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty("points", points);
        userService.updateUserPoints(user, points);
        for (WebSocketSession webSocketSession : webSocketSessions) {
            if (user.equals(webSocketSession.getPrincipal().getName())) {

                webSocketSession.sendMessage(new TextMessage(new Gson().toJson(json)));
            }
        }


    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("Remove session called");
        webSocketSessions.remove(session);

    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        webSocketSessions.add(session);
        //    System.out.println(session.getPrincipal().getName());
        //    System.out.println("Something happened");
    }
}

