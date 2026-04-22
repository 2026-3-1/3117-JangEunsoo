package com.jes.devlearn.domain.course.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private String title;

    @Column(name = "order_num")
    private Integer orderNum;

    public Section(Long courseId, String title, Integer orderNum) {
        this.courseId = courseId;
        this.title = title;
        this.orderNum = orderNum;
    }

    public void update(String title, Integer orderNum) {
        this.title = title;
        if (orderNum != null) {
            this.orderNum = orderNum;
        }
    }

    public void changeOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }
}
