package com.jes.devlearn.domain.cart.service;

import com.jes.devlearn.domain.cart.dto.response.CartItemResponse;
import com.jes.devlearn.domain.cart.dto.response.CartResponse;
import com.jes.devlearn.domain.cart.entity.CartItem;
import com.jes.devlearn.domain.cart.error.CartErrorCode;
import com.jes.devlearn.domain.cart.repository.CartItemRepository;
import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.entity.PublishStatus;
import com.jes.devlearn.domain.course.error.CourseErrorCode;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public CartResponse get(Long userId) {
        List<CartItem> items = cartItemRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return CartResponse.of(toResponses(items));
    }

    @Transactional
    public CartItemResponse add(Long userId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        if (course.getPublishStatus() != PublishStatus.PUBLISHED) {
            throw new CustomException(CourseErrorCode.COURSE_NOT_FOUND);
        }
        if (course.isFree()) {
            throw new CustomException(CartErrorCode.COURSE_NOT_PURCHASABLE);
        }
        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new CustomException(CartErrorCode.ALREADY_ENROLLED);
        }
        if (cartItemRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new CustomException(CartErrorCode.CART_DUPLICATE);
        }
        CartItem item = cartItemRepository.save(new CartItem(userId, courseId));
        return CartItemResponse.of(item, course);
    }

    @Transactional
    public void remove(Long userId, Long courseId) {
        cartItemRepository.deleteByUserIdAndCourseId(userId, courseId);
    }

    @Transactional
    public void clear(Long userId) {
        cartItemRepository.deleteAllByUserId(userId);
    }

    private List<CartItemResponse> toResponses(List<CartItem> items) {
        if (items.isEmpty()) return List.of();
        List<Long> courseIds = items.stream().map(CartItem::getCourseId).distinct().toList();
        Map<Long, Course> courseMap = new HashMap<>();
        courseRepository.findAllById(courseIds).forEach(c -> courseMap.put(c.getId(), c));
        return items.stream()
                .filter(i -> courseMap.containsKey(i.getCourseId()))
                .map(i -> CartItemResponse.of(i, courseMap.get(i.getCourseId())))
                .toList();
    }
}
