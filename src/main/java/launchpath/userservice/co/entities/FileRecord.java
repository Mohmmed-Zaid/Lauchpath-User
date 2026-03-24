package launchpath.userservice.co.entities;

import launchpath.userservice.co.entities.base.BaseEntity;
import launchpath.userservice.co.enums.FileType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "file_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FileRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Cloudinary's public_id — needed to delete/transform the file later
    @Column(name = "cloudinary_id", nullable = false, length = 512)
    private String cloudinaryId;

    // Full delivery URL from Cloudinary
    @Column(nullable = false, length = 1024)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 30)
    private FileType fileType; // RESUME_UPLOAD, RESUME_PDF, RESUME_DOCX, AVATAR

    @Column(name = "size_bytes")
    private Long sizeBytes;
}
