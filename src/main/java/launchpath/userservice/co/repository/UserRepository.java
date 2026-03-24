package launchpath.userservice.co.repository;

import launchpath.userservice.co.entities.User;
import launchpath.userservice.co.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository <User, Long>{

    // Derived — Spring generates: SELECT * FROM users WHERE email = ?
    // Optional → forces caller to handle "not found" case explicitly
    // Never return null from repo — Optional is the contract
    Optional<User> findByEmail(String email);

    // Derived — used during OAuth login to find existing OAuth user
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    // Derived — simple existence check, cheaper than findByEmail (no object mapping)
    boolean existsByEmail(String email);

    // @Modifying → tells Spring this is an UPDATE/DELETE, not a SELECT
    // @Transactional needed on service method that calls this
    // JPQL — "u" is alias for User entity, field names are Java field names not column names
    @Modifying
    @Query("UPDATE User u SET u.isVerified = true WHERE u.id = :id")
    void verifyUser(@Param("id") Long id);

    // Native SQL — when you need exact column names
    // Use case: admin dashboard, count users per provider
    @Query(value = "SELECT provider, COUNT(*) as count FROM users GROUP BY provider",
            nativeQuery = true)
    java.util.List<Object[]> countByProvider();
}

