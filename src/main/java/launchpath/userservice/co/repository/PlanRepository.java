package launchpath.userservice.co.repository;

import launchpath.userservice.co.entities.Plan;
import launchpath.userservice.co.entities.Subscription;
import launchpath.userservice.co.enums.PlanName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository <Plan, Integer>{

    // Derived — most common lookup: "give me the FREE plan details"
    // Used when creating new user → assign FREE plan automatically
    Optional<Plan> findByName(PlanName name);

    // Existence check — used in seeding logic to avoid duplicate plan inserts
    boolean existsByName(PlanName name);
}
