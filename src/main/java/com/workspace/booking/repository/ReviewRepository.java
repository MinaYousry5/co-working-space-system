package com.workspace.booking.repository;

import com.workspace.booking.entity.engagement.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByUserId(Long userId);

    Page<Review> findAllByOrderByCreatedOnDesc(Pageable pageable);

    Page<Review> findByRatingOrderByCreatedOnDesc(Double rating, Pageable pageable);

    @Query("select coalesce(avg(r.rating), 0), count(r.id) from Review r")
    Object[] getAverageRatingAndTotal();

    @Query("""
            select cast(r.rating as integer), count(r.id)
            from Review r
            group by cast(r.rating as integer)
            order by cast(r.rating as integer)
            """)
    List<Object[]> countReviewsByRating();
}
