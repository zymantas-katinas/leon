package com.tietoevry.moon.lesson.model;

import com.tietoevry.moon.classroom.model.Classroom;
import com.tietoevry.moon.subject.model.Subject;
import com.tietoevry.moon.user.model.User;
import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne
    private Subject subject;
    @OneToOne
    private User teacher;
    @OneToOne
    private Classroom classroom;
    private String video;
    private String day;
    private int time;
    private int status;
}