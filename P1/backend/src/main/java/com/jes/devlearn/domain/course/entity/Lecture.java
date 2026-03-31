package com.jes.devlearn.domain.course.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lectures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(nullable = false)
    private String title;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "order_num")
    private Integer orderNum;

    public Lecture(Long sectionId, String title, String videoUrl, Integer orderNum) {
        this.sectionId = sectionId;
        this.title = title;
        this.videoUrl = videoUrl;
        this.orderNum = orderNum;
    }
}
