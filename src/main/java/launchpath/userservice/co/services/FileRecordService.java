package launchpath.userservice.co.services;

import launchpath.userservice.co.entities.FileRecord;
import launchpath.userservice.co.entities.User;
import launchpath.userservice.co.enums.FileType;
import launchpath.userservice.co.exception.ResourceNotFoundException;
import launchpath.userservice.co.exception.UnauthorizedAccessException;
import launchpath.userservice.co.repository.FileRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileRecordService {

    private final FileRecordRepository fileRecordRepository;
    private final UserService userService;

    // ══════════════════════════════════════════════════════════
    // CREATE
    // ══════════════════════════════════════════════════════════

    // Called AFTER Cloudinary confirms upload success
    // Never save record if Cloudinary upload failed
    @Transactional
    public FileRecord saveFileRecord(Long userId,
                                     String cloudinaryId,
                                     String url,
                                     FileType fileType,
                                     Long sizeBytes) {
        log.info("Saving file record - userId: {}, type: {}", userId, fileType);

        User user = userService.getUserById(userId);

        FileRecord record = FileRecord.builder()
                .user(user)
                .cloudinaryId(cloudinaryId)
                .url(url)
                .fileType(fileType)
                .sizeBytes(sizeBytes)
                .build();

        FileRecord saved = fileRecordRepository.save(record);
        log.info("File record saved, id: {}", saved.getId());
        return saved;
    }

    // ══════════════════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<FileRecord> getAllUserFiles(Long userId) {
        log.debug("Fetching all files for userId: {}", userId);
        return fileRecordRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<FileRecord> getUserFilesByType(Long userId, FileType fileType) {
        log.debug("Fetching files - userId: {}, type: {}", userId, fileType);
        return fileRecordRepository.findByUserIdAndFileType(userId, fileType);
    }

    @Transactional(readOnly = true)
    public FileRecord getByCloudinaryId(String cloudinaryId) {
        return fileRecordRepository.findByCloudinaryId(cloudinaryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "File not found: " + cloudinaryId
                ));
    }

    @Transactional(readOnly = true)
    public FileRecord getById(Long fileId) {
        return fileRecordRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "File record not found: " + fileId
                ));
    }

    @Transactional(readOnly = true)
    public long countUserFilesByType(Long userId, FileType fileType) {
        return fileRecordRepository.countByUserIdAndFileType(userId, fileType);
    }

    // ══════════════════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════════════════

    // Deletes DB record only
    // Actual Cloudinary deletion added in Phase 3 via RabbitMQ event
    @Transactional
    public void deleteFileRecord(Long fileId, Long requestingUserId) {
        log.info("Delete file - fileId: {}, by: {}", fileId, requestingUserId);

        FileRecord record = getById(fileId);

        if (!record.getUser().getId().equals(requestingUserId)) {
            log.warn("Unauthorized file delete - fileId: {} by userId: {}",
                    fileId, requestingUserId);
            throw new UnauthorizedAccessException(
                    "You can only delete your own files"
            );
        }

        fileRecordRepository.delete(record);
        log.info("File record deleted: {}", fileId);
        // TODO Phase 3: publish Cloudinary delete event to RabbitMQ
    }

    @Transactional
    public void deleteAllUserFiles(Long userId) {
        log.info("Deleting all files for userId: {}", userId);
        List<FileRecord> records = fileRecordRepository.findByUserId(userId);
        fileRecordRepository.deleteAll(records);
        log.info("Deleted {} file records for userId: {}", records.size(), userId);
        // TODO Phase 3: publish batch Cloudinary delete events
    }
}