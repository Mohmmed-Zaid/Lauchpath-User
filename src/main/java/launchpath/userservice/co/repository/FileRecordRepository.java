package launchpath.userservice.co.repository;

import launchpath.userservice.co.entities.FileRecord;
import launchpath.userservice.co.enums.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {


    // All files belonging to a user — for dashboard/file manager
    List<FileRecord> findByUserId(Long userId);

    // Specific file type for a user
    // e.g. findByUserIdAndFileType(1L, FileType.AVATAR) → get user's avatar
    List<FileRecord> findByUserIdAndFileType(Long userId, FileType fileType);

    // Find by cloudinary ID — needed when deleting file from Cloudinary
    // (Cloudinary delete API requires public_id, not our internal id)
    Optional<FileRecord> findByCloudinaryId(String cloudinaryId);

    // Count files per type per user — enforce storage limits on free tier
    @Query("SELECT COUNT(f) FROM FileRecord f WHERE f.user.id = :userId " +
            "AND f.fileType = :fileType")
    long countByUserIdAndFileType(@Param("userId") Long userId,
                                  @Param("fileType") FileType fileType);
}

